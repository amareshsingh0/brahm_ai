package com.bimoraai.brahm.ui.kundali

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.KundaliRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import com.bimoraai.brahm.core.network.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject

@HiltViewModel
class KundaliViewModel @Inject constructor(
    private val api: ApiService,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _kundali = MutableStateFlow<Map<String, Any?>?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val kundali = _kundali.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val error = _error.asStateFlow()

    // Expose saved profile so KundaliScreen can pre-fill the form
    val savedProfile: StateFlow<UserDto?> = userRepository.user

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
                if (res.isSuccessful) {
                    @Suppress("UNCHECKED_CAST")
                    _kundali.value = res.body()?.let { jsonObjectToMap(it) } as? Map<String, Any?>
                } else _error.value = "Failed to generate Kundali"
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

private fun jsonElementToAny(element: JsonElement): Any? = when (element) {
    is JsonPrimitive -> element.contentOrNull
    is JsonArray -> element.map { jsonElementToAny(it) }
    is JsonObject -> jsonObjectToMap(element)
    else -> null
}

private fun jsonObjectToMap(obj: JsonObject): Map<String, Any?> =
    obj.entries.associate { (k, v) -> k to jsonElementToAny(v) }
