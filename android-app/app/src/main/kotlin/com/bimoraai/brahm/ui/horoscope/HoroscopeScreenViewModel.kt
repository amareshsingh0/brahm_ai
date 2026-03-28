package com.bimoraai.brahm.ui.horoscope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

@HiltViewModel
class HoroscopeScreenViewModel @Inject constructor(
    private val api: ApiService,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _result        = MutableStateFlow<JsonObject?>(null)
    private val _isLoading     = MutableStateFlow(false)
    private val _error         = MutableStateFlow<String?>(null)
    private val _selectedRashi = MutableStateFlow("Aries")
    // null = no kundali saved; non-null = user's Janma Rashi (English)
    private val _userMoonRashi = MutableStateFlow<String?>(null)
    // true while the selection is still the auto-detected Janma Rashi
    private val _autoSelected  = MutableStateFlow(false)

    val result:        StateFlow<JsonObject?> = _result
    val isLoading:     StateFlow<Boolean>     = _isLoading
    val error:         StateFlow<String?>     = _error
    val selectedRashi: StateFlow<String>      = _selectedRashi
    val userMoonRashi: StateFlow<String?>     = _userMoonRashi
    val autoSelected:  StateFlow<Boolean>     = _autoSelected

    // ── Static cache — survives ViewModel recreation on re-navigation ──────────
    companion object {
        // Per-rashi result cache (persists across ViewModel recreations)
        val cache = mutableMapOf<String, JsonObject>()
        var lastSelectedRashi: String? = null
    }

    // Sanskrit → English mapping matching the website
    private val sanskritToEnglish = mapOf(
        "Mesha" to "Aries", "Vrishabha" to "Taurus", "Mithuna" to "Gemini",
        "Karka" to "Cancer", "Simha" to "Leo", "Kanya" to "Virgo",
        "Tula" to "Libra", "Vrischika" to "Scorpio", "Dhanu" to "Sagittarius",
        "Makara" to "Capricorn", "Kumbha" to "Aquarius", "Meena" to "Pisces",
    )

    init {
        // Determine which rashi to show — synchronous check first
        val profileRashi = userRepository.user.value?.rashi?.takeIf { it.isNotBlank() }
        val janmaRashi = profileRashi?.let { raw ->
            if (rashiNames.contains(raw)) raw else sanskritToEnglish[raw]
        }

        val targetRashi = janmaRashi ?: lastSelectedRashi ?: "Aries"
        _selectedRashi.value = targetRashi

        if (janmaRashi != null) {
            _userMoonRashi.value = janmaRashi
            _autoSelected.value  = true
        }

        // Restore from static cache — instant display on re-navigation
        val cached = cache[targetRashi]
        if (cached != null) {
            _result.value = cached
        } else {
            loadForRashi(targetRashi)
        }
    }

    fun loadForRashi(rashi: String) {
        _selectedRashi.value   = rashi
        lastSelectedRashi      = rashi
        // Serve from cache instantly
        cache[rashi]?.let { _result.value = it; return }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val resp = api.getHoroscope(rashi = rashi)
                if (resp.isSuccessful) {
                    resp.body()?.let { result ->
                        cache[rashi]   = result
                        _result.value  = result
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

// Canonical English rashi list in order
val rashiNames = listOf(
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces",
)
