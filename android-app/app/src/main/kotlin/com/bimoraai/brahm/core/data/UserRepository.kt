package com.bimoraai.brahm.core.data

import android.content.Context
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.UserDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that caches the logged-in user's profile.
 *
 * On app open the cached UserDto is emitted INSTANTLY from SharedPreferences
 * (same disk-first strategy as YouTube / ChatGPT / Grok), while a background
 * network refresh silently updates it.  This eliminates the "looks logged-out"
 * flash on app reopen.
 */
@Singleton
class UserRepository @Inject constructor(
    private val api: ApiService,
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // SharedPreferences for synchronous disk read — DataStore is async-only
    private val prefs = context.getSharedPreferences("brahm_user_cache", Context.MODE_PRIVATE)
    private val jsonCodec = Json { ignoreUnknownKeys = true }

    // Emit cached data immediately — no network required on first compose pass
    private val _user        = MutableStateFlow<UserDto?>(loadCachedUser())
    private val _kundaliJson = MutableStateFlow<String?>(prefs.getString("kundali_json", null))

    val user:        StateFlow<UserDto?> = _user.asStateFlow()
    val kundaliJson: StateFlow<String?>  = _kundaliJson.asStateFlow()

    init {
        // Cached data already emitted above — silently refresh from network
        refresh()
    }

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
        prefs.edit().clear().apply()
    }

    /** Pre-populate cached kundali JSON from KundaliViewModel after a fresh generate. */
    fun cacheKundaliJson(json: String) {
        _kundaliJson.value = json
        prefs.edit().putString("kundali_json", json).apply()
    }

    private suspend fun fetchAll() {
        try {
            val profileDeferred = scope.async { api.getMe() }
            val kundaliDeferred = scope.async { api.getSavedKundali() }

            val profileRes = profileDeferred.await()
            if (profileRes.isSuccessful && profileRes.body() != null) {
                val user = profileRes.body()!!
                _user.value = user
                persistUser(user)   // update disk cache for next cold start
            }

            val kundaliRes = kundaliDeferred.await()
            if (kundaliRes.isSuccessful) {
                val body  = kundaliRes.body()
                val found = body?.get("found")?.let {
                    (it as? JsonPrimitive)?.contentOrNull == "true" || it.toString() == "true"
                } ?: false
                if (found) {
                    val kundaliObj = body?.get("kundali") as? JsonObject
                    val raw: String? = (kundaliObj?.get("kundali_json") as? JsonPrimitive)?.contentOrNull
                    if (!raw.isNullOrBlank()) {
                        _kundaliJson.value = raw
                        prefs.edit().putString("kundali_json", raw).apply()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("UserRepository", "fetchAll failed: ${e.message}")
        }
    }

    /**
     * Called immediately after login — stores name/plan from auth response without
     * a network round-trip so the main screen shows user info instantly after login.
     */
    fun setFromAuth(name: String?, plan: String, phone: String?, email: String?) {
        val current = _user.value
        val updated = UserDto(
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
        _user.value = updated
        persistUser(updated)
        // Full refresh in background for complete profile
        refresh()
    }

    /** Returns true only if the user has complete birth data saved */
    fun hasBirthData(): Boolean {
        val u = _user.value ?: return false
        return u.date.isNotBlank() && u.place.isNotBlank()
    }

    // ── Disk cache ────────────────────────────────────────────────────────────

    private fun loadCachedUser(): UserDto? {
        val raw = prefs.getString("user_dto", null) ?: return null
        return try { jsonCodec.decodeFromString<UserDto>(raw) } catch (_: Exception) { null }
    }

    private fun persistUser(user: UserDto) {
        try {
            prefs.edit().putString("user_dto", jsonCodec.encodeToString(user)).apply()
        } catch (_: Exception) {}
    }
}
