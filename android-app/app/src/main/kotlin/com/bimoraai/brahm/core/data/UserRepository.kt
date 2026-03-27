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
                if (res.isSuccessful && res.body() != null) {
                    _user.value = res.body()
                }
            } catch (e: Exception) {
                android.util.Log.w("UserRepository", "refresh failed: ${e.message}")
            }
        }
    }

    /** Called immediately after login — stores name/plan from auth response without a network round-trip */
    fun setFromAuth(name: String?, plan: String, phone: String?, email: String?) {
        val current = _user.value
        _user.value = UserDto(
            session_id = current?.session_id ?: "",
            name       = name?.takeIf { it.isNotBlank() } ?: current?.name ?: "",
            plan       = plan,
            phone      = phone ?: current?.phone,
            email      = email ?: current?.email,
            date       = current?.date ?: "",
            time       = current?.time ?: "",
            place      = current?.place ?: "",
            gender     = current?.gender ?: "",
            lat        = current?.lat ?: 0.0,
            lon        = current?.lon ?: 0.0,
            tz         = current?.tz ?: 5.5,
            rashi      = current?.rashi ?: "",
            nakshatra  = current?.nakshatra ?: "",
        )
        // Also do a full refresh in background to get complete profile
        refresh()
    }

    /** Returns true only if the user has complete birth data saved */
    fun hasBirthData(): Boolean {
        val u = _user.value ?: return false
        return u.date.isNotBlank() && u.place.isNotBlank()
    }
}
