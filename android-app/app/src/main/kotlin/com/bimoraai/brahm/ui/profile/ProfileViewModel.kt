package com.bimoraai.brahm.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.datastore.TokenDataStore
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.network.UpdateProfileRequest
import com.bimoraai.brahm.core.network.UserDto
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

    init {
        // Re-fetch on every profile screen open — startup fetch may have fired before token was ready
        userRepository.refresh()
    }

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState = _saveState.asStateFlow()

    // ── City search ──────────────────────────────────────────────────────────

    val cityQuery = MutableStateFlow("")

    val citySuggestions: StateFlow<List<City>> = cityQuery
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
                    val detail = res.errorBody()?.string() ?: ""
                    _saveState.value = SaveState.Error("Save failed (${res.code()}): $detail")
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
