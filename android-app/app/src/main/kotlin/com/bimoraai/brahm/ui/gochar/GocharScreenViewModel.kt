package com.bimoraai.brahm.ui.gochar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
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

    // Input fields
    val name = MutableStateFlow("")
    val dob  = MutableStateFlow("")
    val tob  = MutableStateFlow("")
    val pob  = MutableStateFlow("")
    val lat  = MutableStateFlow(0.0)
    val lon  = MutableStateFlow(0.0)
    val tz   = MutableStateFlow("5.5")

    init { prefillFromProfile() }

    private fun prefillFromProfile() {
        userRepository.user.value?.let { u ->
            if (name.value.isBlank() && u.name.isNotBlank()) name.value = u.name
            if (dob.value.isBlank() && u.date.isNotBlank()) dob.value = u.date
            if (tob.value.isBlank() && u.time.isNotBlank()) tob.value = u.time
            if (pob.value.isBlank() && u.place.isNotBlank()) pob.value = u.place
            if (u.date.isNotBlank() && u.place.isNotBlank()) calculate()
        }
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
                val body = buildJsonObject {
                    put("name", JsonPrimitive(name.value.ifBlank { "User" }))
                    put("dob",  JsonPrimitive(dob.value))
                    put("tob",  JsonPrimitive(tob.value))
                    put("pob",  JsonPrimitive(pob.value))
                    put("lat",  JsonPrimitive(lat.value))
                    put("lon",  JsonPrimitive(lon.value))
                    put("tz",   JsonPrimitive(tz.value))
                }
                val gocharResp = api.getGochar(body)
                if (gocharResp.isSuccessful) {
                    _gocharData.value = gocharResp.body()
                }

                val analyzeResp = api.analyzeGochar(body)
                if (analyzeResp.isSuccessful) {
                    _analyzeData.value = analyzeResp.body()
                }

                if (gocharResp.isSuccessful || analyzeResp.isSuccessful) {
                    _hasData.value = true
                } else {
                    _error.value = "Calculation failed. Please check your birth details."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun load() { if (hasData.value) calculate() }
}
