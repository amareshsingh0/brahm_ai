package com.bimoraai.brahm.core.data

import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.UserDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that loads and caches the logged-in user's profile once.
 * Inject this into any ViewModel that needs birth data pre-fill.
 */
@Singleton
class UserRepository @Inject constructor(
    private val api: ApiService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _user = MutableStateFlow<UserDto?>(null)
    val user: StateFlow<UserDto?> = _user.asStateFlow()

    init { refresh() }

    fun refresh() {
        scope.launch {
            try {
                val res = api.getMe()
                if (res.isSuccessful) _user.value = res.body()
            } catch (_: Exception) {}
        }
    }

    /** Returns true only if the user has complete birth data saved */
    fun hasBirthData(): Boolean {
        val u = _user.value ?: return false
        return u.date.isNotBlank() && u.place.isNotBlank()
    }
}
