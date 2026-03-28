package com.bimoraai.brahm.ui.yogas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.KundaliRequest
import com.bimoraai.brahm.core.network.UserDto
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

    val name  = MutableStateFlow("")
    val dob   = MutableStateFlow("")
    val tob   = MutableStateFlow("")
    val pob   = MutableStateFlow("")
    val lat   = MutableStateFlow(0.0)
    val lon   = MutableStateFlow(0.0)
    val tz    = MutableStateFlow("5.5")

    // ── Static cache — survives ViewModel recreation across navigation ──────────
    companion object {
        private var cachedResult: JsonObject? = null
    }

    init {
        // Restore from cache instantly — no network on re-navigation
        if (cachedResult != null) {
            _yogas.value   = cachedResult
            _hasData.value = true
        }

        viewModelScope.launch {
            val u = userRepository.user.value
            if (u != null && u.date.isNotBlank() && u.place.isNotBlank()) {
                prefillFromProfile(u)
            } else {
                userRepository.user
                    .filterNotNull()
                    .first { it.date.isNotBlank() && it.place.isNotBlank() }
                    .let { prefillFromProfile(it) }
            }
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
        if (!_hasData.value) calculate()
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
                // Prefer saved kundali — fast DB lookup, avoids heavy recalculation
                val savedResp = api.getSavedKundali()
                val result = if (savedResp.isSuccessful && savedResp.body() != null) {
                    savedResp.body()
                } else {
                    // Fallback: lightweight kundali (no divisional charts / dashas)
                    val resp = api.generateKundali(KundaliRequest(
                        name         = name.value.ifBlank { "User" },
                        date         = dob.value,
                        time         = tob.value,
                        place        = pob.value,
                        lat          = lat.value,
                        lon          = lon.value,
                        tz           = tz.value.toDoubleOrNull() ?: 5.5,
                        calc_options = emptyList(),
                    ))
                    if (resp.isSuccessful) resp.body() else null
                }

                if (result != null) {
                    _yogas.value   = result
                    cachedResult   = result
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

    fun load()  { if (!_hasData.value) calculate() }
    fun reset() { cachedResult = null; _hasData.value = false; _yogas.value = null; _error.value = null }
}
