package com.bimoraai.brahm.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.datastore.TokenDataStore
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.GoogleAuthRequest
import com.bimoraai.brahm.core.network.SendOtpRequest
import com.bimoraai.brahm.core.network.VerifyOtpRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class OtpSent(val phone: String) : AuthState()
    object LoggedIn : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state = _state.asStateFlow()

    fun sendOtp(phone: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val res = api.sendOtp(SendOtpRequest(phone))
                if (res.isSuccessful && res.body()?.success == true) {
                    _state.value = AuthState.OtpSent(phone)
                } else {
                    _state.value = AuthState.Error("Failed to send OTP. Try again.")
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Network error")
            }
        }
    }

    fun verifyOtp(phone: String, otp: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val res = api.verifyOtp(VerifyOtpRequest(phone, otp))
                val body = res.body()
                if (res.isSuccessful && body != null) {
                    tokenDataStore.saveTokens(body.access_token, body.refresh_token)
                    tokenDataStore.saveUserId(body.user.id, body.user.plan)
                    _state.value = AuthState.LoggedIn
                } else {
                    _state.value = AuthState.Error("Invalid OTP. Please try again.")
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Network error")
            }
        }
    }

    fun googleLogin(idToken: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val res = api.googleAuth(GoogleAuthRequest(idToken))
                val body = res.body()
                if (res.isSuccessful && body != null) {
                    tokenDataStore.saveTokens(body.access_token, body.refresh_token)
                    tokenDataStore.saveUserId(body.user.id, body.user.plan)
                    _state.value = AuthState.LoggedIn
                } else {
                    _state.value = AuthState.Error("Google login failed.")
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Network error")
            }
        }
    }

    fun resetState() { _state.value = AuthState.Idle }
}
