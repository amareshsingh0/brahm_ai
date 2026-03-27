package com.bimoraai.brahm.ui.rectification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.add
import javax.inject.Inject

@HiltViewModel
class RectificationScreenViewModel @Inject constructor(
    private val api: ApiService,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _result    = MutableStateFlow<JsonObject?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error     = MutableStateFlow<String?>(null)
    private val _hasData   = MutableStateFlow(false)

    val result:    StateFlow<JsonObject?> = _result
    val isLoading: StateFlow<Boolean>    = _isLoading
    val error:     StateFlow<String?>    = _error
    val hasData:   StateFlow<Boolean>    = _hasData

    val name      = MutableStateFlow("")
    val dob       = MutableStateFlow("")
    val approxTob = MutableStateFlow("")
    val pob       = MutableStateFlow("")
    val lat       = MutableStateFlow(0.0)
    val lon       = MutableStateFlow(0.0)
    val tz        = MutableStateFlow("5.5")

    val uncertainty = MutableStateFlow("±1 Hour")
    val event1Type  = MutableStateFlow("Marriage")
    val event1Date  = MutableStateFlow("")
    val event2Type  = MutableStateFlow("Career Change")
    val event2Date  = MutableStateFlow("")

    init {
        viewModelScope.launch {
            userRepository.user
                .filterNotNull()
                .first()
                .let { u -> prefillFromProfile(u) }
        }
    }

    private fun prefillFromProfile(u: UserDto) {
        if (name.value.isBlank() && u.name.isNotBlank()) name.value = u.name
        if (dob.value.isBlank() && u.date.isNotBlank()) dob.value = u.date
        if (approxTob.value.isBlank() && u.time.isNotBlank()) approxTob.value = u.time
        if (pob.value.isBlank() && u.place.isNotBlank()) pob.value = u.place
        if (lat.value == 0.0 && u.lat != 0.0) lat.value = u.lat
        if (lon.value == 0.0 && u.lon != 0.0) lon.value = u.lon
        if (u.tz != 0.0) tz.value = u.tz.toString()
    }

    fun calculate() {
        if (dob.value.isBlank() || approxTob.value.isBlank()) {
            _error.value = "Please enter date and approximate time of birth"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val uncertaintyMinutes = when {
                    uncertainty.value.contains("30") -> 30
                    uncertainty.value.contains("2")  -> 120
                    uncertainty.value.contains("3")  -> 180
                    else                             -> 60
                }
                val body = buildJsonObject {
                    put("date",                 JsonPrimitive(dob.value))
                    put("approx_time",          JsonPrimitive(approxTob.value))
                    put("uncertainty_minutes",  JsonPrimitive(uncertaintyMinutes))
                    put("lat",                  JsonPrimitive(lat.value))
                    put("lon",                  JsonPrimitive(lon.value))
                    put("tz",                   JsonPrimitive(tz.value.toDoubleOrNull() ?: 5.5))
                    put("events", buildJsonArray {
                        if (event1Date.value.isNotBlank()) add(buildJsonObject {
                            put("date", JsonPrimitive(event1Date.value))
                            put("type", JsonPrimitive(event1Type.value.lowercase().replace(" ", "_")))
                        })
                        if (event2Date.value.isNotBlank()) add(buildJsonObject {
                            put("date", JsonPrimitive(event2Date.value))
                            put("type", JsonPrimitive(event2Type.value.lowercase().replace(" ", "_")))
                        })
                    })
                }
                val resp = api.getRectification(body)
                if (resp.isSuccessful) {
                    _result.value  = resp.body()
                    _hasData.value = true
                } else {
                    _error.value = "Rectification failed. Please check inputs."
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
