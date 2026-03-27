package com.bimoraai.brahm.ui.sky

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class SkyPlanetData(
    val name: String,
    val rashi: String,
    val degree: String,   // e.g. "15.4"
    val dms: String,      // e.g. "15°24'00\""
    val nakshatra: String,
    val pada: Int,
    val retro: Boolean,
    val visible: Boolean,
    val combust: Boolean,
)

data class SkyLagna(val rashi: String, val dms: String)

data class SkySnapshot(
    val grahas: Map<String, SkyPlanetData>,
    val lagna: SkyLagna?,
    val visibleCount: Int,
    val retroCount: Int,
    val dayProgress: Float,
    val secondsToMidnight: Int,
)

// Average daily motion (degrees/day), negative = retrograde motion
val PLANET_SPEED_PER_DAY = mapOf(
    "Surya" to 0.9856f, "Chandra" to 13.176f, "Mangal" to 0.524f,
    "Budh" to 1.383f, "Guru" to 0.083f, "Shukra" to 1.2f,
    "Shani" to 0.033f, "Rahu" to -0.053f, "Ketu" to -0.053f,
)

val GRAHA_ORDER = listOf("Surya", "Chandra", "Mangal", "Budh", "Guru", "Shukra", "Shani", "Rahu", "Ketu")

val RASHI_INDEX = mapOf(
    "Mesha" to 0, "Vrishabha" to 1, "Mithuna" to 2, "Karka" to 3,
    "Simha" to 4, "Kanya" to 5, "Tula" to 6, "Vrischika" to 7,
    "Dhanu" to 8, "Makara" to 9, "Kumbha" to 10, "Meena" to 11,
)

fun degToDms(deg: Float): String {
    val d = deg.toInt()
    val mFrac = (deg - d) * 60f
    val m = mFrac.toInt()
    val s = ((mFrac - m) * 60f).toInt()
    return "${d}°${m.toString().padStart(2,'0')}'"
}

@HiltViewModel
class SkyViewModel @Inject constructor(
    private val api: ApiService,
    val userRepository: UserRepository,
) : ViewModel() {

    private val _snapshot  = MutableStateFlow<SkySnapshot?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _error     = MutableStateFlow<String?>(null)
    // seconds tick for live clock
    private val _tick      = MutableStateFlow(0L)

    val snapshot:  StateFlow<SkySnapshot?> = _snapshot
    val isLoading: StateFlow<Boolean>      = _isLoading
    val error:     StateFlow<String?>      = _error
    val tick:      StateFlow<Long>         = _tick

    init {
        load()
        viewModelScope.launch {
            while (isActive) {
                delay(5 * 60_000)
                load(showLoading = false)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                _tick.value = System.currentTimeMillis() / 1000L
            }
        }
    }

    fun load(showLoading: Boolean = _snapshot.value == null) {
        viewModelScope.launch {
            if (showLoading) _isLoading.value = true
            _error.value = null
            try {
                val resp = api.getPlanetsNow()
                if (resp.isSuccessful) {
                    resp.body()?.let { _snapshot.value = parseSnapshot(it) }
                } else if (_snapshot.value == null) {
                    _error.value = "Failed to load sky data"
                }
            } catch (e: Exception) {
                if (_snapshot.value == null) _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parseSnapshot(json: JsonObject): SkySnapshot {
        val grahasRaw = try { json["grahas"]?.jsonObject } catch (_: Exception) { null }
        val grahas = mutableMapOf<String, SkyPlanetData>()
        grahasRaw?.forEach { (name, v) ->
            try {
                val obj = v.jsonObject
                val rashi    = obj["rashi"]?.jsonPrimitive?.contentOrNull ?: "—"
                val degRaw   = obj["degree"]?.jsonPrimitive?.contentOrNull ?: "0"
                val degF     = degRaw.toFloatOrNull() ?: 0f
                val dms      = obj["dms"]?.jsonPrimitive?.contentOrNull ?: degToDms(degF)
                val nakshatra = obj["nakshatra"]?.jsonPrimitive?.contentOrNull ?: "—"
                val pada     = obj["pada"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1
                val retro    = obj["retro"]?.jsonPrimitive?.booleanOrNull ?: false
                val visible  = obj["visible"]?.jsonPrimitive?.booleanOrNull ?: false
                val combust  = obj["combust"]?.jsonPrimitive?.booleanOrNull ?: false
                grahas[name] = SkyPlanetData(name, rashi, degRaw, dms, nakshatra, pada, retro, visible, combust)
            } catch (_: Exception) {}
        }

        val lagnaObj = try { json["lagna"]?.jsonObject } catch (_: Exception) { null }
        val lagna = lagnaObj?.let {
            val r    = it["rashi"]?.jsonPrimitive?.contentOrNull ?: "—"
            val dms  = it["dms"]?.jsonPrimitive?.contentOrNull
                ?: it["degree"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull()?.let { f -> degToDms(f) } ?: "—"
            SkyLagna(r, dms)
        }

        val now = LocalTime.now()
        val midnight = now.until(LocalTime.MIDNIGHT, ChronoUnit.SECONDS).let {
            if (it <= 0) it + 86400 else it
        }.toInt()
        val dayProgress = (now.toSecondOfDay().toFloat() / 86400f)

        return SkySnapshot(
            grahas         = grahas,
            lagna          = lagna,
            visibleCount   = grahas.values.count { it.visible },
            retroCount     = grahas.values.count { it.retro },
            dayProgress    = dayProgress,
            secondsToMidnight = midnight,
        )
    }
}
