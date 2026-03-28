package com.bimoraai.brahm.ui.gochar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.KundaliRequest
import com.bimoraai.brahm.core.network.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

private data class KundaliExtract(
    val lagnaRashi: String,
    val moonRashi: String,
    val natalBav: JsonObject?,
)

@HiltViewModel
class GocharScreenViewModel @Inject constructor(
    private val api: ApiService,
    private val userRepository: UserRepository,
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

    // ── Static cache — survives ViewModel recreation across navigation ──────────
    companion object {
        private var cachedGochar:  JsonObject? = null
        private var cachedAnalyze: JsonObject? = null
    }

    init {
        // Restore from static cache immediately — instant display on re-navigation
        if (cachedGochar != null || cachedAnalyze != null) {
            _gocharData.value  = cachedGochar
            _analyzeData.value = cachedAnalyze
            _hasData.value     = true
        }

        viewModelScope.launch {
            val u = userRepository.user.value
            if (u != null && u.date.isNotBlank() && u.place.isNotBlank()) {
                prefillFromProfile(u)
            } else {
                // Wait for profile only if not yet loaded
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
        // Skip calculation if already loaded from cache
        if (!_hasData.value) calculate()
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
                // Step 1: Fetch current sky positions
                // Step 2: Try saved kundali first (fast DB fetch) for lagna/moon rashi
                //         Only fall back to full kundali calc if no saved data exists
                val skyDef        = async { api.getGocharNow() }
                val savedKundDef  = async { api.getSavedKundali() }

                val skyResp = skyDef.await()
                if (skyResp.isSuccessful) {
                    _gocharData.value = skyResp.body()
                    cachedGochar      = skyResp.body()
                    // Show sky positions IMMEDIATELY — don't wait for analysis
                    _hasData.value = true
                }

                // Get lagna + moon rashi + natal_bav (preferring saved kundali)
                val savedKundResp = savedKundDef.await()
                val extract = if (savedKundResp.isSuccessful && savedKundResp.body() != null) {
                    val body = savedKundResp.body()!!
                    val found = body["found"]?.let {
                        (it as? JsonPrimitive)?.contentOrNull == "true" || it.toString() == "true"
                    } ?: false
                    if (found) {
                        val kundaliRow = try { body["kundali"]?.jsonObject } catch (_: Exception) { null }
                        val jsonStr = kundaliRow?.get("kundali_json")?.let { (it as? JsonPrimitive)?.contentOrNull }
                        val parsed = jsonStr?.let { s ->
                            try { Json.parseToJsonElement(s) as? JsonObject } catch (_: Exception) { null }
                        }
                        val lr = parsed?.get("lagna")?.jsonObject?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                        val mr = parsed?.get("grahas")?.jsonObject?.get("Chandra")?.jsonObject?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                        // Build natal_bav: {planet: [12 ints]} from ashtakavarga.bav
                        val bav = try { parsed?.get("ashtakavarga")?.jsonObject?.get("bav")?.jsonObject } catch (_: Exception) { null }
                        val natalBav = bav?.let { bavObj ->
                            try {
                                buildJsonObject {
                                    bavObj.forEach { (planet, v) ->
                                        val pts = v.jsonObject["points"]?.jsonArray
                                        if (pts != null) put(planet, pts)
                                    }
                                }
                            } catch (_: Exception) { null }
                        }
                        KundaliExtract(lr, mr, natalBav)
                    } else null
                } else null

                val (lagnaRashi, moonRashi, natalBav) = extract ?: run {
                    // Saved kundali not found — fall back to lightweight calc
                    val resp = api.generateKundali(KundaliRequest(
                        name  = name.value.ifBlank { "User" },
                        date  = dob.value,
                        time  = tob.value,
                        place = pob.value,
                        lat   = lat.value,
                        lon   = lon.value,
                        tz    = tz.value.toDoubleOrNull() ?: 5.5,
                        calc_options = listOf("ashtakavarga"),
                    ))
                    if (resp.isSuccessful) {
                        val k = resp.body()
                        val lr = k?.get("lagna")?.jsonObject?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                        val mr = k?.get("grahas")?.jsonObject?.get("Chandra")?.jsonObject?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""
                        val bav = try { k?.get("ashtakavarga")?.jsonObject?.get("bav")?.jsonObject } catch (_: Exception) { null }
                        val nb = bav?.let { bavObj ->
                            try {
                                buildJsonObject {
                                    bavObj.forEach { (planet, v) ->
                                        val pts = v.jsonObject["points"]?.jsonArray
                                        if (pts != null) put(planet, pts)
                                    }
                                }
                            } catch (_: Exception) { null }
                        }
                        KundaliExtract(lr, mr, nb)
                    } else KundaliExtract("", "", null)
                }

                // Step 3: Personal transit analysis
                if (lagnaRashi.isNotBlank() && moonRashi.isNotBlank()) {
                    val analyzeBody = buildJsonObject {
                        put("lagna_rashi", JsonPrimitive(lagnaRashi))
                        put("moon_rashi",  JsonPrimitive(moonRashi))
                        put("name",        JsonPrimitive(name.value.ifBlank { "User" }))
                        if (natalBav != null) put("natal_bav", natalBav)
                    }
                    val analyzeResp = api.analyzeGochar(analyzeBody)
                    if (analyzeResp.isSuccessful) {
                        _analyzeData.value = analyzeResp.body()
                        cachedAnalyze      = analyzeResp.body()
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

    // Call when user explicitly wants a fresh fetch (pull-to-refresh)
    fun refresh() {
        cachedGochar  = null
        cachedAnalyze = null
        _hasData.value = false
        calculate()
    }
}
