package com.bimoraai.brahm.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.datastore.TokenDataStore
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.network.UpdateProfileRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore,
    private val userRepository: UserRepository,
) : ViewModel() {

    // Expose the shared repository's user so all screens see the same cached data
    val user: StateFlow<UserDto?> = userRepository.user

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState = _saveState.asStateFlow()

    // ── City search ──────────────────────────────────────────────────────────
    private var allCities: List<City> = emptyList()

    val cityQuery = MutableStateFlow("")

    val citySuggestions: StateFlow<List<City>> = cityQuery
        .debounce(200)
        .map { q ->
            if (q.length < 2) return@map emptyList()
            val lower = q.lowercase()
            val local = allCities.filter { it.name.lowercase().startsWith(lower) }
                .take(6)
            if (local.size >= 2) return@map local
            // fallback: geocode API for international cities
            try {
                val res = api.geocode(q)
                if (res.isSuccessful) res.body()?.let { listOf(it) } ?: local
                else local
            } catch (_: Exception) { local }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadCities()
    }

    private fun loadCities() {
        viewModelScope.launch {
            try {
                val res = api.getCities()
                if (res.isSuccessful) allCities = res.body()?.cities ?: emptyList()
            } catch (_: Exception) {}
        }
    }

    fun saveProfile(
        name: String,
        date: String,
        time: String,
        place: String,
        gender: String,
        lat: Double = 0.0,
        lon: Double = 0.0,
        tz: Double = 5.5,
    ) {
        viewModelScope.launch {
            _saveState.value = SaveState.Loading
            try {
                val current = userRepository.user.value
                val req = UpdateProfileRequest(
                    session_id = current?.session_id ?: "",
                    name       = name,
                    date       = date,
                    time       = time,
                    place      = place,
                    gender     = gender,
                    lat        = lat,
                    lon        = lon,
                    tz         = tz,
                    rashi      = current?.rashi ?: "",
                    nakshatra  = current?.nakshatra ?: "",
                    language   = "english",
                    plan       = current?.plan ?: "free",
                    phone      = current?.phone,
                    email      = current?.email,
                )
                val res = api.updateProfile(req)
                if (res.isSuccessful) {
                    userRepository.refresh()   // update shared cache
                    _saveState.value = SaveState.Success
                } else {
                    _saveState.value = SaveState.Error("Save failed (${res.code()})")
                }
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetSaveState() { _saveState.value = SaveState.Idle }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            try { api.logout() } catch (_: Exception) {}
            tokenDataStore.clear()
            onDone()
        }
    }
}

sealed class SaveState {
    object Idle    : SaveState()
    object Loading : SaveState()
    object Success : SaveState()
    data class Error(val msg: String) : SaveState()
}
