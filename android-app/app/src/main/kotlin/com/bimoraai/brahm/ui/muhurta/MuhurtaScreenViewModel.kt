package com.bimoraai.brahm.ui.muhurta

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
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

@HiltViewModel
class MuhurtaScreenViewModel @Inject constructor(
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

    val activity = MutableStateFlow("Wedding")
    val dob      = MutableStateFlow("")
    val tob      = MutableStateFlow("")
    val pob      = MutableStateFlow("")
    val fromDate = MutableStateFlow("")
    val toDate   = MutableStateFlow("")
    val lat      = MutableStateFlow(0.0)
    val lon      = MutableStateFlow(0.0)
    val tz       = MutableStateFlow("5.5")

    init {
        viewModelScope.launch {
            userRepository.user
                .filterNotNull()
                .first()
                .let { u -> prefillFromProfile(u) }
        }
    }

    private fun prefillFromProfile(u: UserDto) {
        if (dob.value.isBlank() && u.date.isNotBlank()) dob.value = u.date
        if (tob.value.isBlank() && u.time.isNotBlank()) tob.value = u.time
        if (pob.value.isBlank() && u.place.isNotBlank()) pob.value = u.place
        if (lat.value == 0.0 && u.lat != 0.0) lat.value = u.lat
        if (lon.value == 0.0 && u.lon != 0.0) lon.value = u.lon
        if (u.tz != 0.0) tz.value = u.tz.toString()
    }

    fun calculate() {
        if (fromDate.value.isBlank()) {
            _error.value = "Please enter a start date"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val body = buildJsonObject {
                    put("activity",   JsonPrimitive(activity.value.lowercase()))
                    put("start_date", JsonPrimitive(fromDate.value))
                    put("days",       JsonPrimitive(7))
                    put("lat",        JsonPrimitive(lat.value))
                    put("lon",        JsonPrimitive(lon.value))
                    put("tz",         JsonPrimitive(tz.value.toDoubleOrNull() ?: 5.5))
                }
                val resp = api.getMuhurta(body)
                if (resp.isSuccessful) {
                    _result.value  = resp.body()
                    _hasData.value = true
                } else {
                    _error.value = "Calculation failed. Please check your inputs."
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
