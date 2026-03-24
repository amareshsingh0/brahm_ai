package com.bimoraai.brahm.ui.kundali

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.KundaliRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KundaliViewModel @Inject constructor(
    private val api: ApiService,
) : ViewModel() {

    private val _kundali = MutableStateFlow<Map<String, Any?>?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val kundali = _kundali.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val error = _error.asStateFlow()

    // Birth inputs
    private var name = ""; private var dob = ""; private var tob = ""
    private var pob = ""; private var lat = 0.0; private var lon = 0.0

    fun setInputs(name: String, dob: String, tob: String, pob: String, lat: Double, lon: Double) {
        this.name = name; this.dob = dob; this.tob = tob
        this.pob = pob; this.lat = lat; this.lon = lon
    }

    fun generate() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val res = api.generateKundali(KundaliRequest(name, dob, tob, pob, lat, lon, "Asia/Kolkata"))
                if (res.isSuccessful) _kundali.value = res.body()
                else _error.value = "Failed to generate Kundali"
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
