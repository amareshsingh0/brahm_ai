package com.bimoraai.brahm.ui.horoscope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {

    private val _result    = MutableStateFlow<JsonObject?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error     = MutableStateFlow<String?>(null)

    val result:    StateFlow<JsonObject?> = _result
    val isLoading: StateFlow<Boolean>    = _isLoading
    val error:     StateFlow<String?>    = _error

    val selectedRashi = MutableStateFlow("Aries")

    // In-memory cache: rashi → result (shared across rashi switches in same session)
    private val cache = mutableMapOf<String, JsonObject>()

    fun loadForRashi(rashi: String) {
        selectedRashi.value = rashi
        // Return instantly from cache if available
        cache[rashi]?.let { _result.value = it; return }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val resp = api.getHoroscope(rashi = rashi)
                if (resp.isSuccessful) {
                    resp.body()?.let { cache[rashi] = it; _result.value = it }
                } else {
                    _error.value = "Failed to load horoscope. Please try again."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun load() { loadForRashi(selectedRashi.value) }

    init { loadForRashi("Aries") }
}
