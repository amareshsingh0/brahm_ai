package com.bimoraai.brahm.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.datastore.TokenDataStore
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.UpdateProfileRequest
import com.bimoraai.brahm.core.network.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore,
) : ViewModel() {

    private val _user = MutableStateFlow<UserDto?>(null)
    val user = _user.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState = _saveState.asStateFlow()

    init { loadUser() }

    private fun loadUser() {
        viewModelScope.launch {
            try {
                val res = api.getMe()
                if (res.isSuccessful) _user.value = res.body()
            } catch (_: Exception) {}
        }
    }

    fun saveProfile(
        name: String,
        date: String,
        time: String,
        place: String,
        gender: String,
    ) {
        viewModelScope.launch {
            _saveState.value = SaveState.Loading
            try {
                val current = _user.value
                val req = UpdateProfileRequest(
                    session_id = current?.session_id ?: "",
                    name       = name,
                    date       = date,
                    time       = time,
                    place      = place,
                    gender     = gender,
                    lat        = 0.0,
                    lon        = 0.0,
                    tz         = 5.5,
                    rashi      = current?.rashi ?: "",
                    nakshatra  = current?.nakshatra ?: "",
                    language   = "english",
                    plan       = current?.plan ?: "free",
                    phone      = current?.phone,
                    email      = current?.email,
                )
                val res = api.updateProfile(req)
                if (res.isSuccessful) {
                    res.body()?.let { _user.value = it }
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
