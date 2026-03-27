package com.bimoraai.brahm.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.UpdateProfileRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileSetupState {
    object Idle    : ProfileSetupState()
    object Loading : ProfileSetupState()
    object Success : ProfileSetupState()
    object Skipped : ProfileSetupState()
    data class Error(val message: String) : ProfileSetupState()
}

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val api: ApiService,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileSetupState>(ProfileSetupState.Idle)
    val state = _state.asStateFlow()

    fun saveProfile(
        name:   String,
        gender: String,
        date:   String,
        time:   String,
        place:  String,
        lat:    Double,
        lon:    Double,
        tz:     Double,
    ) {
        viewModelScope.launch {
            _state.value = ProfileSetupState.Loading
            try {
                val req = UpdateProfileRequest(
                    name     = name,
                    gender   = gender,
                    date     = date,
                    time     = time,
                    place    = place,
                    lat      = lat,
                    lon      = lon,
                    tz       = tz,
                    language = "english",
                )
                val res = api.updateProfile(req)
                if (res.isSuccessful) {
                    userRepository.refresh()
                    _state.value = ProfileSetupState.Success
                } else {
                    val errBody = res.errorBody()?.string()?.take(200) ?: "Save failed"
                    _state.value = ProfileSetupState.Error(errBody)
                }
            } catch (e: Exception) {
                _state.value = ProfileSetupState.Error(e.message ?: "Network error")
            }
        }
    }

    fun skip() {
        _state.value = ProfileSetupState.Skipped
    }
}
