package com.bimoraai.brahm.ui.varshphal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import javax.inject.Inject

@HiltViewModel
class VarshpalScreenViewModel @Inject constructor(
    private val api: ApiService,
) : ViewModel() {

    private val _result = MutableStateFlow<Map<String, Any?>?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val result = _result.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val error = _error.asStateFlow()

    private var lastParams: Map<String, Any?> = emptyMap()

    fun submit(params: Map<String, Any?>) {
        lastParams = params
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val jsonBody = buildJsonObject {
                    lastParams.forEach { (k, v) ->
                        when (v) {
                            is String -> put(k, v)
                            is Number -> put(k, v.toDouble())
                            is Boolean -> put(k, v)
                            else -> if (v != null) put(k, v.toString())
                        }
                    }
                }
                val res = api.getVarshphal(jsonBody)
                if (res.isSuccessful) {
                    _result.value = res.body()?.entries?.associate { (k, v) ->
                        k to if (v is JsonPrimitive) v.contentOrNull else v.toString()
                    }
                } else _error.value = "Failed to load data"
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
