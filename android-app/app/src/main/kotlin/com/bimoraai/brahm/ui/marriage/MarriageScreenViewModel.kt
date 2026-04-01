package com.bimoraai.brahm.ui.marriage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

private const val PREFS_NAME    = "brahm_marriage_cache"
private const val KEY_JSON      = "marriage_json"
private const val KEY_TS        = "marriage_ts"
private const val TTL_MS        = 24 * 60 * 60 * 1000L  // 24 hours

@HiltViewModel
class MarriageScreenViewModel @Inject constructor(
    private val api: ApiService,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _result    = MutableStateFlow<JsonObject?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error     = MutableStateFlow<String?>(null)
    private val _hasData   = MutableStateFlow(false)

    val result:    StateFlow<JsonObject?> = _result
    val isLoading: StateFlow<Boolean>     = _isLoading
    val error:     StateFlow<String?>     = _error
    val hasData:   StateFlow<Boolean>     = _hasData

    val name   = MutableStateFlow("")
    val dob    = MutableStateFlow("")
    val tob    = MutableStateFlow("")
    val pob    = MutableStateFlow("")
    val lat    = MutableStateFlow(0.0)
    val lon    = MutableStateFlow(0.0)
    val tz     = MutableStateFlow("5.5")
    val gender = MutableStateFlow("Male")

    companion object {
        private var cached: JsonObject? = null
    }

    // ── Persistent cache ─────────────────────────────────────────────────────
    private fun loadPersisted(): JsonObject? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json  = prefs.getString(KEY_JSON, null) ?: return null
        val saved = prefs.getLong(KEY_TS, 0L)
        if (System.currentTimeMillis() - saved > TTL_MS) return null
        return try { Json.parseToJsonElement(json) as? JsonObject } catch (_: Exception) { null }
    }

    private fun persist(data: JsonObject) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_JSON, data.toString())
            .putLong(KEY_TS, System.currentTimeMillis())
            .apply()
    }

    init {
        val inProcess = cached
        val persisted = if (inProcess == null) loadPersisted() else null
        val toRestore = inProcess ?: persisted
        if (toRestore != null) {
            _result.value  = toRestore
            _hasData.value = true
            if (inProcess == null) cached = persisted
        }

        // Prefill birth details from profile
        viewModelScope.launch {
            val u = userRepository.user.value
            if (u != null && u.date.isNotBlank() && u.place.isNotBlank()) {
                prefillFromProfile(u)
            } else {
                userRepository.user
                    .filterNotNull()
                    .first { it.date.isNotBlank() && it.place.isNotBlank() }
                    .let { prefillFromProfile(it) }
            }
        }
    }

    private fun prefillFromProfile(u: UserDto) {
        if (name.value.isBlank() && u.name.isNotBlank()) name.value = u.name
        if (dob.value.isBlank() && u.date.isNotBlank()) dob.value = u.date
        if (tob.value.isBlank() && u.time.isNotBlank()) tob.value = u.time
        if (pob.value.isBlank() && u.place.isNotBlank()) pob.value = u.place
        if (lat.value == 0.0 && u.lat != 0.0) lat.value = u.lat
        if (lon.value == 0.0 && u.lon != 0.0) lon.value = u.lon
        if (u.tz != 0.0) tz.value = u.tz.toString()
        if (u.gender.isNotBlank()) gender.value = u.gender
    }

    fun calculate() {
        if (dob.value.isBlank() || tob.value.isBlank()) {
            _error.value = "Please enter date and time of birth"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val body = buildJsonObject {
                    put("name",   JsonPrimitive(name.value.ifBlank { "User" }))
                    put("date",   JsonPrimitive(dob.value))
                    put("time",   JsonPrimitive(tob.value))
                    put("place",  JsonPrimitive(pob.value))
                    put("lat",    JsonPrimitive(lat.value))
                    put("lon",    JsonPrimitive(lon.value))
                    put("tz",     JsonPrimitive(tz.value.toDoubleOrNull() ?: 5.5))
                    put("gender", JsonPrimitive(gender.value))
                }
                val resp = api.getMarriageAnalysis(body)
                if (resp.isSuccessful && resp.body() != null) {
                    cached         = resp.body()
                    _result.value  = resp.body()
                    _hasData.value = true
                    persist(resp.body()!!)
                } else {
                    _error.value = "Analysis failed. Please check your birth details."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun load() { calculate() }

    fun refresh() {
        cached         = null
        _hasData.value = false
        _result.value  = null
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        calculate()
    }
}
