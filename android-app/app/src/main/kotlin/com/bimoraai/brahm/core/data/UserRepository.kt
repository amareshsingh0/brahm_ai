package com.bimoraai.brahm.core.data

import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.UserDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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

    private val _user        = MutableStateFlow<UserDto?>(null)
    private val _kundaliJson = MutableStateFlow<String?>(null)   // cached raw kundali_json

    val user:        StateFlow<UserDto?> = _user.asStateFlow()
    val kundaliJson: StateFlow<String?>  = _kundaliJson.asStateFlow()

    init { refresh() }

    fun refresh() {
        scope.launch { fetchAll() }
    }

    /** Fetches profile + kundali in parallel — use in login flows before navigation. */
    suspend fun refreshAndGetUser(): UserDto? {
        fetchAll()
        return _user.value
    }

    /** Clears cached user + kundali — call on logout. */
    fun clear() {
        _user.value        = null
        _kundaliJson.value = null
    }

    /** Pre-populate cached kundali JSON from KundaliViewModel after a fresh generate. */
    fun cacheKundaliJson(json: String) {
        _kundaliJson.value = json
    }

    private suspend fun fetchAll() {
        try {
            // Parallel fetch: profile + saved kundali
            val profileDeferred = scope.async { api.getMe() }
            val kundaliDeferred = scope.async { api.getSavedKundali() }

            val profileRes = profileDeferred.await()
            if (profileRes.isSuccessful && profileRes.body() != null) {
                _user.value = profileRes.body()
            }

            val kundaliRes = kundaliDeferred.await()
            if (kundaliRes.isSuccessful) {
                val body  = kundaliRes.body()
                val found = body?.get("found")?.let {
                    (it as? JsonPrimitive)?.contentOrNull == "true" || it.toString() == "true"
                } ?: false
                if (found) {
                    val kundaliObj = body?.get("kundali") as? JsonObject
                    val json = kundaliObj?.get("kundali_json")?.let { (it as? JsonPrimitive)?.contentOrNull }
                    if (!json.isNullOrBlank()) _kundaliJson.value = json
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("UserRepository", "fetchAll failed: ${e.message}")
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
