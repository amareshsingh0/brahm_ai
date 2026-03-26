package com.bimoraai.brahm.ui.panchang

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PanchangViewModel @Inject constructor(
    private val api: ApiService,
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

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            val today = LocalDate.now()
            try {
                val pResp = api.getPanchang(
                    date = today.toString(),
                    lat  = 28.6139,
                    lon  = 77.2090,
                    tz   = 5.5,
                )
                if (pResp.isSuccessful) _panchang.value = pResp.body()

                val fResp = api.getFestivals(month = today.monthValue, year = today.year)
                if (fResp.isSuccessful) _festivals.value = fResp.body()

                val gResp = api.getGrahan()
                if (gResp.isSuccessful) _grahan.value = gResp.body()

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load panchang"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun JsonObject.str(key: String) = this[key]?.jsonPrimitive?.contentOrNull ?: "—"
}
