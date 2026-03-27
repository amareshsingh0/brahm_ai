package com.bimoraai.brahm.core.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.City
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class CitySearchViewModel @Inject constructor(
    private val api: ApiService,
) : ViewModel() {

    val cityQuery = MutableStateFlow("")

    val suggestions: StateFlow<List<City>> = cityQuery
        .debounce(300)
        .map { q ->
            if (q.length < 2) return@map emptyList()
            try {
                val res = api.searchCities(q)
                if (res.isSuccessful) res.body()?.results?.take(6) ?: emptyList()
                else emptyList()
            } catch (_: Exception) { emptyList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
