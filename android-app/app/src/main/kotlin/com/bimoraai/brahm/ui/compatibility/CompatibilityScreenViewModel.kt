package com.bimoraai.brahm.ui.compatibility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

@HiltViewModel
class CompatibilityScreenViewModel @Inject constructor(
    private val api: ApiService,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _result    = MutableStateFlow<JsonObject?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error     = MutableStateFlow<String?>(null)
    private val _hasData   = MutableStateFlow(false)

    val result:    StateFlow<JsonObject?> = _result
    val isLoading: StateFlow<Boolean>    = _isLoading
    val error:     StateFlow<String?>    = _error
    val hasData:   StateFlow<Boolean>    = _hasData

    // Person A
    val name1 = MutableStateFlow("")
    val dob1  = MutableStateFlow("")
    val tob1  = MutableStateFlow("")
    val pob1  = MutableStateFlow("")

    // Person B
    val name2 = MutableStateFlow("")
    val dob2  = MutableStateFlow("")
    val tob2  = MutableStateFlow("")
    val pob2  = MutableStateFlow("")

    init {
        // Pre-fill Person A from saved profile
        userRepository.user.value?.let { u ->
            if (name1.value.isBlank() && u.name.isNotBlank()) name1.value = u.name
            if (dob1.value.isBlank() && u.date.isNotBlank()) dob1.value = u.date
            if (tob1.value.isBlank() && u.time.isNotBlank()) tob1.value = u.time
            if (pob1.value.isBlank() && u.place.isNotBlank()) pob1.value = u.place
        }
    }

    fun calculate() {
        if (dob1.value.isBlank() || tob1.value.isBlank() || dob2.value.isBlank() || tob2.value.isBlank()) {
            _error.value = "Please enter birth date and time for both persons"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val body = buildJsonObject {
                    put("name1", JsonPrimitive(name1.value.ifBlank { "Person A" }))
                    put("dob1",  JsonPrimitive(dob1.value))
                    put("tob1",  JsonPrimitive(tob1.value))
                    put("pob1",  JsonPrimitive(pob1.value))
                    put("lat1",  JsonPrimitive(0.0))
                    put("lon1",  JsonPrimitive(0.0))
                    put("tz1",   JsonPrimitive("5.5"))
                    put("name2", JsonPrimitive(name2.value.ifBlank { "Person B" }))
                    put("dob2",  JsonPrimitive(dob2.value))
                    put("tob2",  JsonPrimitive(tob2.value))
                    put("pob2",  JsonPrimitive(pob2.value))
                    put("lat2",  JsonPrimitive(0.0))
                    put("lon2",  JsonPrimitive(0.0))
                    put("tz2",   JsonPrimitive("5.5"))
                }
                val resp = api.getCompatibility(body)
                if (resp.isSuccessful) {
                    _result.value  = resp.body()
                    _hasData.value = true
                } else {
                    _error.value = "Calculation failed. Please check birth details."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun load() { if (hasData.value) calculate() }
}
