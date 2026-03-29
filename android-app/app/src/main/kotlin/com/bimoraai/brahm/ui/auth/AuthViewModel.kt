package com.bimoraai.brahm.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.datastore.TokenDataStore
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.GoogleAuthRequest
import com.bimoraai.brahm.core.network.SendOtpRequest
import com.bimoraai.brahm.core.network.VerifyOtpRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

private fun friendlyNetworkError(e: Exception): String = when (e) {
    is SocketTimeoutException -> "Request timed out. Please check your internet and try again."
    is IOException            -> "Unable to connect. Please check your internet connection."
    else                      -> "Something went wrong. Please try again."
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class OtpSent(val phone: String) : AuthState()
    data class LoggedIn(val hasBirthData: Boolean) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state = _state.asStateFlow()

    fun sendOtp(phone: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val res = api.sendOtp(SendOtpRequest(phone))
                if (res.isSuccessful && res.body()?.sent == true) {
                    _state.value = AuthState.OtpSent(phone)
                } else {
                    val errBody = res.errorBody()?.string()?.take(300)
                    val msg = res.body()?.message
                        ?: errBody
                        ?: "Failed to send OTP (HTTP ${res.code()})"
                    _state.value = AuthState.Error(msg)
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error(friendlyNetworkError(e))
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
                    tokenDataStore.saveUserId(body.user_id, body.plan)
                    // Await full profile fetch so hasBirthData is accurate at navigation time
                    val user = userRepository.refreshAndGetUser()
                    _state.value = AuthState.LoggedIn(
                        hasBirthData = user?.date?.isNotBlank() == true && user.place.isNotBlank()
                    )
                } else {
                    _state.value = AuthState.Error("Invalid OTP. Please try again.")
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error(friendlyNetworkError(e))
            }
        }
    }

    fun googleLogin(idToken: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val res = api.googleAuth(GoogleAuthRequest(idToken, device_type = "android"))
                val body = res.body()
                if (res.isSuccessful && body != null) {
                    tokenDataStore.saveTokens(body.access_token, body.refresh_token)
                    tokenDataStore.saveUserId(body.user_id, body.plan)
                    val user = userRepository.refreshAndGetUser()
                    _state.value = AuthState.LoggedIn(
                        hasBirthData = user?.date?.isNotBlank() == true && user.place.isNotBlank()
                    )
                } else {
                    val errBody = res.errorBody()?.string()?.take(300)
                    _state.value = AuthState.Error(errBody ?: "Google login failed.")
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error(friendlyNetworkError(e))
            }
        }
    }

    fun resetState() { _state.value = AuthState.Idle }
    fun setError(msg: String) { _state.value = AuthState.Error(msg) }
}
