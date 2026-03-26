package com.bimoraai.brahm.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val api: ApiService,
) : ViewModel() {

    val query      = MutableStateFlow("")
    private val _results   = MutableStateFlow<JsonObject?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error     = MutableStateFlow<String?>(null)

    val results:   StateFlow<JsonObject?> = _results
    val isLoading: StateFlow<Boolean>    = _isLoading
    val error:     StateFlow<String?>    = _error

    init {
        query
            .debounce(300)
            .filter { it.trim().length >= 2 }
            .distinctUntilChanged()
            .onEach { q -> search(q) }
            .launchIn(viewModelScope)
    }

    private fun search(q: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val resp = api.searchVedicLibrary(q)
                if (resp.isSuccessful) _results.value = resp.body()
                else _error.value = "Search failed"
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
