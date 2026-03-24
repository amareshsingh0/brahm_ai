package com.bimoraai.brahm.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.PanchangRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val api: ApiService,
) : ViewModel() {

    private val _panchang = MutableStateFlow<Map<String, Any?>?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val panchang = _panchang.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val error = _error.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val today = LocalDate.now().toString()
                // Default: Delhi coordinates — will be replaced with user location
                val res = api.getPanchang(PanchangRequest(today, 28.6139, 77.2090, "Asia/Kolkata"))
                if (res.isSuccessful) {
                    _panchang.value = res.body()
                } else {
                    _error.value = "Failed to load Panchang"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
