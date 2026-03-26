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
import kotlinx.serialization.json.JsonPrimitive
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

    val panchang = _panchang.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val error = _error.asStateFlow()

    // Cache today's panchang — it won't change until midnight
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
        val today = LocalDate.now().toString()
        // Return instantly if we already have today's data
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

fun JsonObject.str(key: String): String {
    val el = this[key] ?: return "—"
    if (el !is JsonPrimitive) return "—"
    val s = el.content
    return if (s.isBlank() || s == "null") "—" else s
}

/** Read a primitive field from a nested object: obj.nested("tithi", "name") */
fun JsonObject.nested(outer: String, inner: String): String {
    val obj = this[outer]?.let {
        try { it.jsonObject } catch (_: Exception) { null }
    } ?: return "—"
    return obj.str(inner)
}
