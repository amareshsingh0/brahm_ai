package com.bimoraai.brahm.ui.prashna

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class PrashnaScreenViewModel @Inject constructor(
    private val api: ApiService,
) : ViewModel() {

    private val _result    = MutableStateFlow<JsonObject?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error     = MutableStateFlow<String?>(null)
    private val _hasData   = MutableStateFlow(false)

    val result:    StateFlow<JsonObject?> = _result
    val isLoading: StateFlow<Boolean>    = _isLoading
    val error:     StateFlow<String?>    = _error
    val hasData:   StateFlow<Boolean>    = _hasData

    val question      = MutableStateFlow("")
    val questionType  = MutableStateFlow("General")
    val pob           = MutableStateFlow("")

    fun calculate() {
        if (question.value.isBlank()) {
            _error.value = "Please enter your question"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val body = buildJsonObject {
                    put("question",      JsonPrimitive(question.value))
                    put("question_type", JsonPrimitive(questionType.value))
                    put("pob",           JsonPrimitive(pob.value))
                    put("lat",           JsonPrimitive(0.0))
                    put("lon",           JsonPrimitive(0.0))
                    put("tz",            JsonPrimitive("5.5"))
                }
                val resp = api.getPrashna(body)
                if (resp.isSuccessful) {
                    _result.value  = resp.body()
                    _hasData.value = true
                } else {
                    _error.value = "Failed to analyze question. Please try again."
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
