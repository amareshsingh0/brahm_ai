package com.bimoraai.brahm.ui.gochar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.KundaliRequest
import com.bimoraai.brahm.core.network.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

@HiltViewModel
class GocharScreenViewModel @Inject constructor(
    private val api: ApiService,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _gocharData  = MutableStateFlow<JsonObject?>(null)
    private val _analyzeData = MutableStateFlow<JsonObject?>(null)
    private val _isLoading   = MutableStateFlow(false)
    private val _error       = MutableStateFlow<String?>(null)
    private val _hasData     = MutableStateFlow(false)

    val gocharData:  StateFlow<JsonObject?> = _gocharData
    val analyzeData: StateFlow<JsonObject?> = _analyzeData
    val isLoading:   StateFlow<Boolean>     = _isLoading
    val error:       StateFlow<String?>     = _error
    val hasData:     StateFlow<Boolean>     = _hasData

    val name = MutableStateFlow("")
    val dob  = MutableStateFlow("")
    val tob  = MutableStateFlow("")
    val pob  = MutableStateFlow("")
    val lat  = MutableStateFlow(0.0)
    val lon  = MutableStateFlow(0.0)
    val tz   = MutableStateFlow("5.5")

    init {
        viewModelScope.launch {
            userRepository.user
                .filterNotNull()
                .first { it.date.isNotBlank() && it.place.isNotBlank() }
                .let { u -> prefillFromProfile(u) }
        }
    }

    private fun prefillFromProfile(u: UserDto) {
        if (name.value.isBlank() && u.name.isNotBlank()) name.value = u.name
        if (dob.value.isBlank() && u.date.isNotBlank()) dob.value = u.date
        if (tob.value.isBlank() && u.time.isNotBlank()) tob.value = u.time
        if (pob.value.isBlank() && u.place.isNotBlank()) pob.value = u.place
        if (lat.value == 0.0 && u.lat != 0.0) lat.value = u.lat
        if (lon.value == 0.0 && u.lon != 0.0) lon.value = u.lon
        if (u.tz != 0.0) tz.value = u.tz.toString()
        calculate()
    }

    fun calculate() {
        if (dob.value.isBlank() || tob.value.isBlank()) {
            _error.value = "Please enter date and time of birth"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                // Step 1 + 2 in parallel: current sky AND kundali for lagna/moon rashi
                val skyDef = async { api.getGocharNow() }
                val kundaliDef = async {
                    api.generateKundali(KundaliRequest(
                        name  = name.value.ifBlank { "User" },
                        date  = dob.value,
                        time  = tob.value,
                        place = pob.value,
                        lat   = lat.value,
                        lon   = lon.value,
                        tz    = tz.value.toDoubleOrNull() ?: 5.5,
                    ))
                }

                val skyResp = skyDef.await()
                if (skyResp.isSuccessful) _gocharData.value = skyResp.body()

                // Step 3: extract lagna + moon rashi → call gochar/analyze
                val kundaliResp = kundaliDef.await()
                if (kundaliResp.isSuccessful) {
                    val k = kundaliResp.body()
                    val lagnaRashi = k?.get("lagna")?.jsonObject?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                    val moonRashi  = k?.get("grahas")?.jsonObject?.get("Chandra")?.jsonObject?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                    if (lagnaRashi.isNotBlank() && moonRashi.isNotBlank()) {
                        val analyzeBody = buildJsonObject {
                            put("lagna_rashi", JsonPrimitive(lagnaRashi))
                            put("moon_rashi",  JsonPrimitive(moonRashi))
                            put("name",        JsonPrimitive(name.value.ifBlank { "User" }))
                        }
                        val analyzeResp = api.analyzeGochar(analyzeBody)
                        if (analyzeResp.isSuccessful) _analyzeData.value = analyzeResp.body()
                    }
                }

                _hasData.value = _gocharData.value != null || _analyzeData.value != null
                if (!_hasData.value) _error.value = "Calculation failed. Please check your birth details."
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun load() { if (hasData.value) calculate() }
}
