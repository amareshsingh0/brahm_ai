package com.bimoraai.brahm.ui.horoscope

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

private const val PREFS_NAME   = "brahm_horoscope_cache"
private const val TTL_MS       = 12 * 60 * 60 * 1000L  // 12 hours

@HiltViewModel
class HoroscopeScreenViewModel @Inject constructor(
    private val api: ApiService,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _result        = MutableStateFlow<JsonObject?>(null)
    private val _isLoading     = MutableStateFlow(false)
    private val _error         = MutableStateFlow<String?>(null)
    private val _selectedRashi = MutableStateFlow("Aries")
    private val _userMoonRashi = MutableStateFlow<String?>(null)
    private val _autoSelected  = MutableStateFlow(false)

    val result:        StateFlow<JsonObject?> = _result
    val isLoading:     StateFlow<Boolean>     = _isLoading
    val error:         StateFlow<String?>     = _error
    val selectedRashi: StateFlow<String>      = _selectedRashi
    val userMoonRashi: StateFlow<String?>     = _userMoonRashi
    val autoSelected:  StateFlow<Boolean>     = _autoSelected

    // In-process cache — instant re-navigation within same session
    companion object {
        val cache = mutableMapOf<String, JsonObject>()
        var lastSelectedRashi: String? = null
    }

    private val sanskritToEnglish = mapOf(
        "Mesha" to "Aries", "Vrishabha" to "Taurus", "Mithuna" to "Gemini",
        "Karka" to "Cancer", "Simha" to "Leo", "Kanya" to "Virgo",
        "Tula" to "Libra", "Vrischika" to "Scorpio", "Dhanu" to "Sagittarius",
        "Makara" to "Capricorn", "Kumbha" to "Aquarius", "Meena" to "Pisces",
    )

    // ── Persistent cache (SharedPreferences) ─────────────────────────────────
    private fun loadPersisted(rashi: String): JsonObject? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json  = prefs.getString("json_$rashi", null) ?: return null
        val saved = prefs.getLong("ts_$rashi", 0L)
        if (System.currentTimeMillis() - saved > TTL_MS) return null
        return try { Json.parseToJsonElement(json) as? JsonObject } catch (_: Exception) { null }
    }

    private fun persist(rashi: String, data: JsonObject) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("json_$rashi", data.toString())
            .putLong("ts_$rashi", System.currentTimeMillis())
            .apply()
    }

    init {
        val profileRashi = userRepository.user.value?.rashi?.takeIf { it.isNotBlank() }
        val janmaRashi   = profileRashi?.let { if (rashiNames.contains(it)) it else sanskritToEnglish[it] }
        val targetRashi  = janmaRashi ?: lastSelectedRashi ?: "Aries"

        _selectedRashi.value = targetRashi
        if (janmaRashi != null) { _userMoonRashi.value = janmaRashi; _autoSelected.value = true }

        // 1. In-process cache hit → instant
        val inProcess = cache[targetRashi]
        if (inProcess != null) {
            _result.value = inProcess
        } else {
            // 2. Persistent cache hit → instant display, no API call needed
            val persisted = loadPersisted(targetRashi)
            if (persisted != null) {
                cache[targetRashi] = persisted
                _result.value      = persisted
            } else {
                // 3. No cache — fetch from API
                loadForRashi(targetRashi)
            }
        }
    }

    fun loadForRashi(rashi: String) {
        _selectedRashi.value = rashi
        lastSelectedRashi    = rashi

        // In-process cache
        cache[rashi]?.let { _result.value = it; return }

        // Persistent cache
        loadPersisted(rashi)?.let {
            cache[rashi]   = it
            _result.value  = it
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val resp = api.getHoroscope(rashi = rashi)
                if (resp.isSuccessful) {
                    resp.body()?.let { result ->
                        cache[rashi]   = result
                        _result.value  = result
                        persist(rashi, result)
                    }
                } else {
                    _error.value = "Failed to load horoscope."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectRashi(rashi: String) {
        _autoSelected.value = false
        loadForRashi(rashi)
    }

    fun load() = loadForRashi(_selectedRashi.value)
}

val rashiNames = listOf(
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces",
)
