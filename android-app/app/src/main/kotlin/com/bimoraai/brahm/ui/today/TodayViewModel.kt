package com.bimoraai.brahm.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val api: ApiService,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _panchang = MutableStateFlow<JsonObject?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // User-derived state for Today screen personalization
    private val _userName = MutableStateFlow<String?>(null)
    private val _smartAlert = MutableStateFlow<SmartAlert?>(null)
    private val _hasBirthData = MutableStateFlow(false)

    val panchang = _panchang.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val error = _error.asStateFlow()
    val userName = _userName.asStateFlow()
    val smartAlert = _smartAlert.asStateFlow()
    val hasBirthData = _hasBirthData.asStateFlow()

    // Cache today's panchang — it won't change until midnight
    private var cachedDate: String? = null

    init {
        load()
        observeUser()
        // Reload once user location arrives (in case profile loads after first load)
        viewModelScope.launch {
            userRepository.user
                .filterNotNull()
                .first { it.lat != 0.0 }
                .let { cachedDate = null; load() }
        }
    }

    private fun observeUser() {
        viewModelScope.launch {
            userRepository.user.collect { user ->
                if (user != null) {
                    _userName.value = user.name.takeIf { it.isNotBlank() }
                    _hasBirthData.value = user.date.isNotBlank() && user.place.isNotBlank()
                    _smartAlert.value = deriveSmartAlert(user.nakshatra, user.rashi)
                }
            }
        }
    }

    /**
     * Derives a contextual alert based on user's birth nakshatra/rashi.
     * Saturn transits ~2.5 years per sign — detect if it's near the user's Moon sign.
     * This is a simplified approximation. Full logic requires current gochar data.
     */
    private fun deriveSmartAlert(nakshatra: String, rashi: String): SmartAlert? {
        if (rashi.isBlank()) return null
        // Sade Sati rashis are approximate for 2025-2026 (Saturn in Aquarius/Kumbha)
        val saturnRashis = setOf("Capricorn", "Makar", "Aquarius", "Kumbha", "Pisces", "Meen")
        if (saturnRashis.any { it.equals(rashi, ignoreCase = true) }) {
            return SmartAlert(
                icon = "⚠️",
                message = "Saturn transiting near your Moon sign ($rashi) — Sade Sati may be active",
                route = "sade_sati",
                actionLabel = "View Analysis →",
            )
        }
        return null
    }

    fun load() {
        val today = LocalDate.now().toString()
        if (cachedDate == today && _panchang.value != null) return

        val u = userRepository.user.value
        val lat = if (u != null && u.lat != 0.0) u.lat else 28.6139
        val lon = if (u != null && u.lon != 0.0) u.lon else 77.2090
        val tz  = if (u != null && u.tz  != 0.0) u.tz  else 5.5

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val res = api.getPanchang(date = today, lat = lat, lon = lon, tz = tz)
                if (res.isSuccessful && res.body() != null) {
                    _panchang.value = res.body()
                    cachedDate = today
                } else {
                    _error.value = res.errorBody()?.string()?.take(200) ?: "Failed to load Panchang (${res.code()})"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

data class SmartAlert(
    val icon: String,
    val message: String,
    val route: String,
    val actionLabel: String,
)

fun JsonObject.str(key: String): String {
    val s = try {
        this[key]?.jsonPrimitive?.contentOrNull
    } catch (_: Exception) { null }
    return if (s.isNullOrBlank() || s == "null") "—" else s
}

/** Read a primitive field from a nested object: obj.nested("tithi", "name") */
fun JsonObject.nested(outer: String, inner: String): String {
    val obj = try {
        this[outer]?.jsonObject
    } catch (_: Exception) { null } ?: return "—"
    return obj.str(inner)
}
