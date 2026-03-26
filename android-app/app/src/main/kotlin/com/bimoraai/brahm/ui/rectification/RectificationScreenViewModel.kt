package com.bimoraai.brahm.ui.rectification

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
import kotlinx.serialization.json.put
import javax.inject.Inject

@HiltViewModel
class RectificationScreenViewModel @Inject constructor(
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

    val name        = MutableStateFlow("")
    val dob         = MutableStateFlow("")
    val approxTob   = MutableStateFlow("")
    val pob         = MutableStateFlow("")

    init {
        userRepository.user.value?.let { u ->
            if (name.value.isBlank() && u.name.isNotBlank()) name.value = u.name
            if (dob.value.isBlank() && u.date.isNotBlank()) dob.value = u.date
            if (approxTob.value.isBlank() && u.time.isNotBlank()) approxTob.value = u.time
            if (pob.value.isBlank() && u.place.isNotBlank()) pob.value = u.place
        }
    }
    val uncertainty = MutableStateFlow("±1 Hour")
    // Life events
    val event1Type  = MutableStateFlow("Marriage")
    val event1Date  = MutableStateFlow("")
    val event2Type  = MutableStateFlow("Career Change")
    val event2Date  = MutableStateFlow("")

    fun calculate() {
        if (dob.value.isBlank() || approxTob.value.isBlank()) {
            _error.value = "Please enter date and approximate time of birth"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val body = buildJsonObject {
                    put("name",        JsonPrimitive(name.value.ifBlank { "User" }))
                    put("dob",         JsonPrimitive(dob.value))
                    put("approx_tob",  JsonPrimitive(approxTob.value))
                    put("pob",         JsonPrimitive(pob.value))
                    put("uncertainty", JsonPrimitive(uncertainty.value))
                    put("lat",         JsonPrimitive(0.0))
                    put("lon",         JsonPrimitive(0.0))
                    put("tz",          JsonPrimitive("5.5"))
                    put("events",      buildJsonObject {
                        if (event1Date.value.isNotBlank()) put("event1", buildJsonObject {
                            put("type", JsonPrimitive(event1Type.value))
                            put("date", JsonPrimitive(event1Date.value))
                        })
                        if (event2Date.value.isNotBlank()) put("event2", buildJsonObject {
                            put("type", JsonPrimitive(event2Type.value))
                            put("date", JsonPrimitive(event2Date.value))
                        })
                    })
                }
                val resp = api.getRectification(body)
                if (resp.isSuccessful) {
                    _result.value  = resp.body()
                    _hasData.value = true
                } else {
                    _error.value = "Rectification failed. Please check inputs."
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
