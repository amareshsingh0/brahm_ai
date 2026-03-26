package com.bimoraai.brahm.ui.panchang

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PanchangViewModel @Inject constructor(
    private val api: ApiService,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _panchang   = MutableStateFlow<JsonObject?>(null)
    private val _festivals  = MutableStateFlow<JsonObject?>(null)
    private val _grahan     = MutableStateFlow<JsonObject?>(null)
    private val _isLoading  = MutableStateFlow(true)
    private val _error      = MutableStateFlow<String?>(null)

    val panchang:  StateFlow<JsonObject?> = _panchang
    val festivals: StateFlow<JsonObject?> = _festivals
    val grahan:    StateFlow<JsonObject?> = _grahan
    val isLoading: StateFlow<Boolean>    = _isLoading
    val error:     StateFlow<String?>    = _error

    // Cache key — invalidate at midnight by storing the date
    private var cachedDate: String? = null

    init {
        load()
        // Reload once user location arrives (in case profile loads after first load)
        viewModelScope.launch {
            userRepository.user
                .filterNotNull()
                .first { it.lat != 0.0 }
                .let { cachedDate = null; load() }
        }
    }

    fun load() {
        val today = LocalDate.now()
        val todayStr = today.toString()
        // Return instantly if today's data is already loaded
        if (cachedDate == todayStr && _panchang.value != null) return

        val u = userRepository.user.value
        val lat = if (u != null && u.lat != 0.0) u.lat else 28.6139
        val lon = if (u != null && u.lon != 0.0) u.lon else 77.2090
        val tz  = if (u != null && u.tz  != 0.0) u.tz  else 5.5

        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                // Fire all 3 requests in parallel
                val pDef = async { api.getPanchang(date = todayStr, lat = lat, lon = lon, tz = tz) }
                val fDef = async { api.getFestivals(month = today.monthValue, year = today.year) }
                val gDef = async { api.getGrahan() }

                val pResp = pDef.await()
                val fResp = fDef.await()
                val gResp = gDef.await()

                if (pResp.isSuccessful) _panchang.value  = pResp.body()
                if (fResp.isSuccessful) _festivals.value = fResp.body()
                if (gResp.isSuccessful) _grahan.value    = gResp.body()

                if (pResp.isSuccessful) cachedDate = todayStr

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load panchang"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun JsonObject.str(key: String) = this[key]?.jsonPrimitive?.contentOrNull ?: "—"
}
