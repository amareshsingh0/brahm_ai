package com.bimoraai.brahm.ui.sadesati

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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

@HiltViewModel
class SadeSatiScreenViewModel @Inject constructor(
    private val api: ApiService,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _shaniRashi    = MutableStateFlow("")
    private val _shaniDegree   = MutableStateFlow(0.0)
    private val _lagnaRashi    = MutableStateFlow("")
    private val _isLoading     = MutableStateFlow(true)
    private val _saturnError   = MutableStateFlow<String?>(null)

    val shaniRashi:  StateFlow<String>  = _shaniRashi
    val shaniDegree: StateFlow<Double>  = _shaniDegree
    val lagnaRashi:  StateFlow<String>  = _lagnaRashi
    val isLoading:   StateFlow<Boolean> = _isLoading
    val saturnError: StateFlow<String?> = _saturnError

    /** User-selected moon rashi — mutable from UI */
    val selectedMoonRashi = MutableStateFlow("")

    init {
        viewModelScope.launch {
            // Fetch Saturn position AND user profile in parallel
            val saturnDef = async { fetchSaturnPosition() }
            val profileDef = async {
                try {
                    userRepository.user
                        .filterNotNull()
                        .first()
                        .let { u -> prefillFromProfile(u) }
                } catch (_: Exception) { /* no profile */ }
            }
            saturnDef.await()
            profileDef.await()
            _isLoading.value = false
        }
    }

    private suspend fun fetchSaturnPosition() {
        try {
            val resp = api.getGocharNow()
            if (resp.isSuccessful) {
                val positions = resp.body()?.get("positions")?.jsonObject
                val shani = positions?.get("Shani")?.jsonObject
                _shaniRashi.value  = shani?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                _shaniDegree.value = shani?.get("degree")?.jsonPrimitive?.doubleOrNull ?: 0.0
            } else {
                _saturnError.value = "Could not load Saturn position."
            }
        } catch (e: Exception) {
            _saturnError.value = "Could not reach server."
        }
    }

    private suspend fun prefillFromProfile(u: UserDto) {
        if (u.date.isBlank() || u.time.isBlank()) return
        try {
            val resp = api.generateKundali(KundaliRequest(
                name  = u.name.ifBlank { "User" },
                date  = u.date,
                time  = u.time,
                place = u.place,
                lat   = u.lat,
                lon   = u.lon,
                tz    = u.tz,
            ))
            if (resp.isSuccessful) {
                val body = resp.body()
                val moonR  = body?.get("grahas")?.jsonObject?.get("Chandra")?.jsonObject?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                val lagnaR = body?.get("lagna")?.jsonObject?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                if (moonR.isNotBlank() && selectedMoonRashi.value.isBlank()) selectedMoonRashi.value = moonR
                if (lagnaR.isNotBlank()) _lagnaRashi.value = lagnaR
            }
        } catch (_: Exception) { /* ignore — user can select manually */ }
    }

    fun refreshSaturn() {
        viewModelScope.launch {
            _isLoading.value    = true
            _saturnError.value  = null
            fetchSaturnPosition()
            _isLoading.value    = false
        }
    }
}
