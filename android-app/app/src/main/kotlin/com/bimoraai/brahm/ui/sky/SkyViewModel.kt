package com.bimoraai.brahm.ui.sky

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

@HiltViewModel
class SkyViewModel @Inject constructor(
    private val api: ApiService,
) : ViewModel() {

    private val _planets   = MutableStateFlow<JsonObject?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _error     = MutableStateFlow<String?>(null)

    val planets:   StateFlow<JsonObject?> = _planets
    val isLoading: StateFlow<Boolean>    = _isLoading
    val error:     StateFlow<String?>    = _error

    init {
        load()
        // Auto-refresh every 60 seconds
        viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                load()
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val resp = api.getPlanetsNow()
                if (resp.isSuccessful) _planets.value = resp.body()
                else _error.value = "Failed to load sky data"
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
