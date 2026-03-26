package com.bimoraai.brahm.ui.yogas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.KundaliRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

@HiltViewModel
class YogasViewModel @Inject constructor(
    private val api: ApiService,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _yogas     = MutableStateFlow<JsonObject?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error     = MutableStateFlow<String?>(null)
    private val _hasData   = MutableStateFlow(false)

    val yogas:     StateFlow<JsonObject?> = _yogas
    val isLoading: StateFlow<Boolean>    = _isLoading
    val error:     StateFlow<String?>    = _error
    val hasData:   StateFlow<Boolean>    = _hasData

    // Input fields
    val name  = MutableStateFlow("")
    val dob   = MutableStateFlow("")
    val tob   = MutableStateFlow("")
    val pob   = MutableStateFlow("")
    val lat   = MutableStateFlow(0.0)
    val lon   = MutableStateFlow(0.0)
    val tz    = MutableStateFlow("5.5")

    init {
        viewModelScope.launch {
            userRepository.user
                .filterNotNull()
                .first { it.date.isNotBlank() && it.place.isNotBlank() }
                .let { u -> prefillFromProfile(u) }
        }
    }

    private fun prefillFromProfile(u: com.bimoraai.brahm.core.network.UserDto) {
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
                val resp = api.generateKundali(
                    KundaliRequest(
                        name = name.value.ifBlank { "User" },
                        dob  = dob.value,
                        tob  = tob.value,
                        pob  = pob.value,
                        lat  = lat.value,
                        lon  = lon.value,
                        tz   = tz.value,
                    )
                )
                if (resp.isSuccessful) {
                    _yogas.value  = resp.body()
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
}
