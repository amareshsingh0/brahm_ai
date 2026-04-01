package com.bimoraai.brahm.ui.gochar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.KundaliRequest
import com.bimoraai.brahm.core.network.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

private const val PREFS_NAME        = "brahm_gochar_cache"
private const val KEY_ANALYZE_JSON  = "analyze_json"
private const val KEY_ANALYZE_TIME  = "analyze_ts"
private const val ANALYZE_TTL_MS    = 6 * 60 * 60 * 1000L  // 6 hours

private data class KundaliExtract(
    val lagnaRashi: String,
    val moonRashi: String,
    val natalBav: JsonObject?,
)

@HiltViewModel
class GocharScreenViewModel @Inject constructor(
    private val api: ApiService,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _gocharData  = MutableStateFlow<JsonObject?>(null)
    private val _analyzeData = MutableStateFlow<JsonObject?>(null)
    private val _isLoading   = MutableStateFlow(false)
    private val _error       = MutableStateFlow<String?>(null)
    private val _hasData     = MutableStateFlow(false)

    val gocharData:  StateFlow<JsonObject?> = _gocharData
    val analyzeData: StateFlow<JsonObject?> = _analyzeData
    val isLoading:   StateFlow<Boolean>     = _isLoading
    val error:       StateFlow<String?>     = _error
    val hasData:     StateFlow<Boolean>     = _hasData

    val name = MutableStateFlow("")
    val dob  = MutableStateFlow("")
    val tob  = MutableStateFlow("")
    val pob  = MutableStateFlow("")
    val lat  = MutableStateFlow(0.0)
    val lon  = MutableStateFlow(0.0)
    val tz   = MutableStateFlow("5.5")

    // In-process cache — survives ViewModel recreation within same process
    companion object {
        private var cachedGochar:  JsonObject? = null
        private var cachedAnalyze: JsonObject? = null
    }

    // ── Persistent cache (SharedPreferences) — survives app reopen ────────────
    private fun loadPersistedAnalyze(): JsonObject? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json  = prefs.getString(KEY_ANALYZE_JSON, null) ?: return null
        val saved = prefs.getLong(KEY_ANALYZE_TIME, 0L)
        if (System.currentTimeMillis() - saved > ANALYZE_TTL_MS) return null
        return try { Json.parseToJsonElement(json) as? JsonObject } catch (_: Exception) { null }
    }

    private fun persistAnalyze(data: JsonObject) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ANALYZE_JSON, data.toString())
            .putLong(KEY_ANALYZE_TIME, System.currentTimeMillis())
            .apply()
    }

    init {
        // 1. Restore in-process cache (same process re-navigation — instant)
        val inProcess = cachedAnalyze
        // 2. Fallback: persistent cache from SharedPreferences (app reopen)
        val persisted = if (inProcess == null) loadPersistedAnalyze() else null

        val analyzeToRestore = inProcess ?: persisted
        if (cachedGochar != null || analyzeToRestore != null) {
            _gocharData.value  = cachedGochar
            _analyzeData.value = analyzeToRestore
            _hasData.value     = true
            if (inProcess == null && persisted != null) cachedAnalyze = persisted
        }

        // Always refresh sky positions in background (fast, real-time data)
        fetchSkyPositions()

        // Prefill birth data from profile; calculate full data only if no cache
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

    // Lightweight sky-positions-only fetch (no kundali calc, no analysis)
    private fun fetchSkyPositions() {
        viewModelScope.launch {
            try {
                val resp = api.getGocharNow()
                if (resp.isSuccessful && resp.body() != null) {
                    _gocharData.value = resp.body()
                    cachedGochar      = resp.body()
                    _hasData.value    = true
                }
            } catch (_: Exception) { /* silent — full calculate() will handle errors */ }
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
        // Only run full calculate (expensive) if we have no analyze data
        if (cachedAnalyze == null) calculate()
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
                // Step 1: Sky positions + saved kundali in parallel
                val skyDef       = async { api.getGocharNow() }
                val savedKundDef = async { api.getSavedKundali() }

                val skyResp = skyDef.await()
                if (skyResp.isSuccessful) {
                    _gocharData.value = skyResp.body()
                    cachedGochar      = skyResp.body()
                    _hasData.value    = true
                }

                // Step 2: Extract lagna + moon rashi (prefer saved kundali)
                val savedKundResp = savedKundDef.await()
                val extract = if (savedKundResp.isSuccessful && savedKundResp.body() != null) {
                    val body  = savedKundResp.body()!!
                    val found = body["found"]?.let {
                        (it as? JsonPrimitive)?.contentOrNull == "true" || it.toString() == "true"
                    } ?: false
                    if (found) {
                        val kundaliRow = try { body["kundali"]?.jsonObject } catch (_: Exception) { null }
                        val jsonStr    = kundaliRow?.get("kundali_json")?.let { (it as? JsonPrimitive)?.contentOrNull }
                        val parsed     = jsonStr?.let { s ->
                            try { Json.parseToJsonElement(s) as? JsonObject } catch (_: Exception) { null }
                        }
                        val lr  = parsed?.get("lagna")?.jsonObject?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                        val mr  = parsed?.get("grahas")?.jsonObject?.get("Chandra")?.jsonObject?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                        val bav = try { parsed?.get("ashtakavarga")?.jsonObject?.get("bav")?.jsonObject } catch (_: Exception) { null }
                        val nb  = bav?.let { bavObj ->
                            try { buildJsonObject { bavObj.forEach { (p, v) -> val pts = v.jsonObject["points"]?.jsonArray; if (pts != null) put(p, pts) } } } catch (_: Exception) { null }
                        }
                        KundaliExtract(lr, mr, nb)
                    } else null
                } else null

                val (lagnaRashi, moonRashi, natalBav) = extract ?: run {
                    val resp = api.generateKundali(KundaliRequest(
                        name  = name.value.ifBlank { "User" },
                        date  = dob.value, time  = tob.value, place = pob.value,
                        lat   = lat.value, lon   = lon.value,
                        tz    = tz.value.toDoubleOrNull() ?: 5.5,
                        calc_options = listOf("ashtakavarga"),
                    ))
                    if (resp.isSuccessful) {
                        val k  = resp.body()
                        val lr = k?.get("lagna")?.jsonObject?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                        val mr = k?.get("grahas")?.jsonObject?.get("Chandra")?.jsonObject?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                        val bav = try { k?.get("ashtakavarga")?.jsonObject?.get("bav")?.jsonObject } catch (_: Exception) { null }
                        val nb  = bav?.let { bavObj ->
                            try { buildJsonObject { bavObj.forEach { (p, v) -> val pts = v.jsonObject["points"]?.jsonArray; if (pts != null) put(p, pts) } } } catch (_: Exception) { null }
                        }
                        KundaliExtract(lr, mr, nb)
                    } else KundaliExtract("", "", null)
                }

                // Step 3: Personal transit analysis
                if (lagnaRashi.isNotBlank() && moonRashi.isNotBlank()) {
                    val body = buildJsonObject {
                        put("lagna_rashi", JsonPrimitive(lagnaRashi))
                        put("moon_rashi",  JsonPrimitive(moonRashi))
                        put("name",        JsonPrimitive(name.value.ifBlank { "User" }))
                        if (natalBav != null) put("natal_bav", natalBav)
                    }
                    val analyzeResp = api.analyzeGochar(body)
                    if (analyzeResp.isSuccessful && analyzeResp.body() != null) {
                        _analyzeData.value = analyzeResp.body()
                        cachedAnalyze      = analyzeResp.body()
                        persistAnalyze(analyzeResp.body()!!)
                    }
                }

                _hasData.value = _gocharData.value != null || _analyzeData.value != null
                if (!_hasData.value) _error.value = "Calculation failed. Please check your birth details."
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun load() { calculate() }

    fun refresh() {
        cachedGochar  = null
        cachedAnalyze = null
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        _hasData.value = false
        calculate()
    }
}
