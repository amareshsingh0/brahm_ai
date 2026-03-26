package com.bimoraai.brahm.ui.varshphal

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
class VarshpalScreenViewModel @Inject constructor(
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

    val name       = MutableStateFlow("")
    val dob        = MutableStateFlow("")
    val tob        = MutableStateFlow("")
    val pob        = MutableStateFlow("")
    val targetYear = MutableStateFlow("")

    init {
        userRepository.user.value?.let { u ->
            if (name.value.isBlank() && u.name.isNotBlank()) name.value = u.name
            if (dob.value.isBlank() && u.date.isNotBlank()) dob.value = u.date
            if (tob.value.isBlank() && u.time.isNotBlank()) tob.value = u.time
            if (pob.value.isBlank() && u.place.isNotBlank()) pob.value = u.place
        }
    }

    fun calculate() {
        if (dob.value.isBlank() || tob.value.isBlank()) {
            _error.value = "Please enter date and time of birth"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val body = buildJsonObject {
                    put("name",        JsonPrimitive(name.value.ifBlank { "User" }))
                    put("dob",         JsonPrimitive(dob.value))
                    put("tob",         JsonPrimitive(tob.value))
                    put("pob",         JsonPrimitive(pob.value))
                    put("target_year", JsonPrimitive(targetYear.value.ifBlank { "2025" }))
                    put("lat",         JsonPrimitive(0.0))
                    put("lon",         JsonPrimitive(0.0))
                    put("tz",          JsonPrimitive("5.5"))
                }
                val resp = api.getVarshphal(body)
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
