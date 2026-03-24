package com.bimoraai.brahm.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.datastore.TokenDataStore
import com.bimoraai.brahm.core.network.ApiService
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

    init { loadUser() }

    private fun loadUser() {
        viewModelScope.launch {
            try {
                val res = api.getMe()
                if (res.isSuccessful) _user.value = res.body()
            } catch (_: Exception) {}
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            try { api.logout() } catch (_: Exception) {}
            tokenDataStore.clear()
            onDone()
        }
    }
}
