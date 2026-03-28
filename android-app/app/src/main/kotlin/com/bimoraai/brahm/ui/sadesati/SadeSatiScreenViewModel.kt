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
    private val _isLoading     = MutableStateFlow(false)
    private val _saturnError   = MutableStateFlow<String?>(null)

    val shaniRashi:  StateFlow<String>  = _shaniRashi
    val shaniDegree: StateFlow<Double>  = _shaniDegree
    val lagnaRashi:  StateFlow<String>  = _lagnaRashi
    val isLoading:   StateFlow<Boolean> = _isLoading
    val saturnError: StateFlow<String?> = _saturnError

    /** User-selected moon rashi — mutable from UI */
    val selectedMoonRashi = MutableStateFlow("")

    // ── Static cache — survives ViewModel recreation across navigation ──────────
    companion object {
        private var cachedShaniRashi:  String? = null
        private var cachedShaniDegree: Double? = null
        private var cachedMoonRashi:   String? = null
        private var cachedLagnaRashi:  String? = null
    }

    init {
        // Restore from cache instantly — no network needed on re-navigation
        if (cachedShaniRashi != null) {
            _shaniRashi.value  = cachedShaniRashi!!
            _shaniDegree.value = cachedShaniDegree ?: 0.0
        }
        if (cachedMoonRashi != null && selectedMoonRashi.value.isBlank()) {
            selectedMoonRashi.value = cachedMoonRashi!!
        }
        if (cachedLagnaRashi != null) {
            _lagnaRashi.value = cachedLagnaRashi!!
        }

        // Only fetch if cache is empty
        if (cachedShaniRashi == null) {
            viewModelScope.launch {
                _isLoading.value = true
                val saturnDef = async { fetchSaturnPosition() }
                val profileDef = async { loadProfileRashis() }
                saturnDef.await()
                profileDef.await()
                _isLoading.value = false
            }
        } else {
            // Cache hit — still try to fill profile rashi if missing
            if (cachedMoonRashi == null) {
                viewModelScope.launch { loadProfileRashis() }
            }
        }
    }

    private suspend fun fetchSaturnPosition() {
        try {
            val resp = api.getGocharNow()
            if (resp.isSuccessful) {
                val positions = resp.body()?.get("positions")?.jsonObject
                val shani = positions?.get("Shani")?.jsonObject
                val rashi  = shani?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                val degree = shani?.get("degree")?.jsonPrimitive?.doubleOrNull ?: 0.0
                _shaniRashi.value  = rashi
                _shaniDegree.value = degree
                cachedShaniRashi  = rashi
                cachedShaniDegree = degree
            } else {
                _saturnError.value = "Could not load Saturn position."
            }
        } catch (e: Exception) {
            _saturnError.value = "Could not reach server."
        }
    }

    private suspend fun loadProfileRashis() {
        val u = userRepository.user.value
            ?: try { userRepository.user.filterNotNull().first() } catch (_: Exception) { return }
        if (u.date.isBlank() || u.time.isBlank()) return
        prefillFromProfile(u)
    }

    private suspend fun prefillFromProfile(u: UserDto) {
        try {
            // Prefer saved kundali — fast DB lookup, no heavy recalculation
            val savedResp = api.getSavedKundali()
            val (moonR, lagnaR) = if (savedResp.isSuccessful && savedResp.body() != null) {
                val k = savedResp.body()!!
                val mr = k["grahas"]?.jsonObject?.get("Chandra")?.jsonObject?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                val lr = k["lagna"]?.jsonObject?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                mr to lr
            } else {
                // Fallback: lightweight kundali (no divisional charts / dashas)
                val resp = api.generateKundali(KundaliRequest(
                    name         = u.name.ifBlank { "User" },
                    date         = u.date,
                    time         = u.time,
                    place        = u.place,
                    lat          = u.lat,
                    lon          = u.lon,
                    tz           = u.tz,
                    calc_options = emptyList(),
                ))
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val mr = body?.get("grahas")?.jsonObject?.get("Chandra")?.jsonObject?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                    val lr = body?.get("lagna")?.jsonObject?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                    mr to lr
                } else {
                    "" to ""
                }
            }

            if (moonR.isNotBlank() && selectedMoonRashi.value.isBlank()) {
                selectedMoonRashi.value = moonR
                cachedMoonRashi = moonR
            }
            if (lagnaR.isNotBlank()) {
                _lagnaRashi.value = lagnaR
                cachedLagnaRashi  = lagnaR
            }
        } catch (_: Exception) { /* user can select manually */ }
    }

    fun refreshSaturn() {
        cachedShaniRashi  = null
        cachedShaniDegree = null
        cachedMoonRashi   = null
        cachedLagnaRashi  = null
        viewModelScope.launch {
            _isLoading.value   = true
            _saturnError.value = null
            val saturnDef  = async { fetchSaturnPosition() }
            val profileDef = async { loadProfileRashis() }
            saturnDef.await()
            profileDef.await()
            _isLoading.value = false
        }
    }
}
