package com.bimoraai.brahm.ui.panchang

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PanchangViewModel @Inject constructor(
    private val api: ApiService,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _panchang   = MutableStateFlow<JsonObject?>(null)
    private val _festivals  = MutableStateFlow<JsonObject?>(null)
    private val _grahan     = MutableStateFlow<JsonObject?>(null)
    private val _isLoading  = MutableStateFlow(true)
    private val _error      = MutableStateFlow<String?>(null)
    private val _cityName   = MutableStateFlow<String?>(null)

    val panchang:  StateFlow<JsonObject?> = _panchang
    val festivals: StateFlow<JsonObject?> = _festivals
    val grahan:    StateFlow<JsonObject?> = _grahan
    val isLoading: StateFlow<Boolean>    = _isLoading
    val error:     StateFlow<String?>    = _error
    val cityName:  StateFlow<String?>    = _cityName

    // Override coordinates set when user picks a city from search
    private var overrideLat: Double? = null
    private var overrideLon: Double? = null
    private var overrideTz:  Double? = null

    // Cache key — invalidate at midnight or when city changes
    private var cachedDate: String? = null
    private var cachedCity: String? = null

    init {
        load()
        // Reload once user location arrives (in case profile loads after first load)
        viewModelScope.launch {
            userRepository.user
                .filterNotNull()
                .first { it.lat != 0.0 }
                .let {
                    if (overrideLat == null) { // only reload if user hasn't picked a custom city
                        _cityName.value = it.place.takeIf { p -> p.isNotBlank() }
                        cachedDate = null
                        load()
                    }
                }
        }
    }

    /** Load panchang for a custom city (overrides profile location). */
    fun loadForCity(lat: Double, lon: Double, tz: Double, cityName: String) {
        overrideLat = lat
        overrideLon = lon
        overrideTz  = tz
        _cityName.value = cityName
        cachedDate = null   // force refresh
        load()
    }

    fun load() {
        val today    = LocalDate.now()
        val todayStr = today.toString()
        val cityKey  = overrideLat?.toString() ?: userRepository.user.value?.place ?: "default"

        // Return instantly if same city + same day already loaded
        if (cachedDate == todayStr && cachedCity == cityKey && _panchang.value != null) return

        val lat: Double
        val lon: Double
        val tz:  Double

        if (overrideLat != null) {
            lat = overrideLat!!
            lon = overrideLon!!
            tz  = overrideTz!!
        } else {
            val u = userRepository.user.value
            lat = if (u != null && u.lat != 0.0) u.lat else 23.1765  // Ujjain default
            lon = if (u != null && u.lon != 0.0) u.lon else 75.7885
            tz  = if (u != null && u.tz  != 0.0) u.tz  else 5.5
            if (_cityName.value == null) {
                _cityName.value = u?.place?.takeIf { it.isNotBlank() } ?: "Ujjain"
            }
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val pDef = async { api.getPanchang(date = todayStr, lat = lat, lon = lon, tz = tz) }
                val fDef = async { api.getFestivals(month = today.monthValue, year = today.year) }
                val gDef = async { api.getGrahan() }

                val pResp = pDef.await()
                val fResp = fDef.await()
                val gResp = gDef.await()

                if (pResp.isSuccessful) { _panchang.value = pResp.body(); cachedDate = todayStr; cachedCity = cityKey }
                if (fResp.isSuccessful) _festivals.value = fResp.body()
                if (gResp.isSuccessful) _grahan.value    = gResp.body()
                if (!pResp.isSuccessful) _error.value = "Failed to load panchang (${pResp.code()})"

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load panchang"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun JsonObject.str(key: String) = this[key]?.jsonPrimitive?.contentOrNull ?: "—"
}
