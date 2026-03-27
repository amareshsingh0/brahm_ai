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

    private val _festivals  = MutableStateFlow<JsonObject?>(null)
    private val _grahan     = MutableStateFlow<JsonObject?>(null)
    private val _isLoading  = MutableStateFlow(true)
    private val _error      = MutableStateFlow<String?>(null)
    val cityName = MutableStateFlow<String?>(null)

    val festivals: StateFlow<JsonObject?> = _festivals
    val grahan:    StateFlow<JsonObject?> = _grahan
    val isLoading: StateFlow<Boolean>    = _isLoading
    val error:     StateFlow<String?>    = _error

    private var overrideLat: Double? = null
    private var overrideLon: Double? = null
    private var overrideTz:  Double? = null

    init {
        load()
        viewModelScope.launch {
            userRepository.user
                .filterNotNull()
                .first { it.lat != 0.0 }
                .let {
                    if (overrideLat == null) {
                        cityName.value = it.place.takeIf { p -> p.isNotBlank() }
                        load()
                    }
                }
        }
    }

    fun loadForCity(lat: Double, lon: Double, tz: Double, name: String) {
        overrideLat = lat; overrideLon = lon; overrideTz = tz
        cityName.value = name
        load()
    }

    fun load() {
        val year  = LocalDate.now().year
        val lat: Double; val lon: Double; val tz: Double

        if (overrideLat != null) {
            lat = overrideLat!!; lon = overrideLon!!; tz = overrideTz!!
        } else {
            val u = userRepository.user.value
            lat = if (u != null && u.lat != 0.0) u.lat else 23.1765
            lon = if (u != null && u.lon != 0.0) u.lon else 75.7885
            tz  = if (u != null && u.tz  != 0.0) u.tz  else 5.5
            if (cityName.value == null) cityName.value = u?.place?.takeIf { it.isNotBlank() } ?: "Ujjain"
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val fDef = async { api.getFestivalsYear(year = year, lat = lat, lon = lon, tz = tz) }
                val gDef = async { api.getGrahan() }

                val fResp = fDef.await()
                val gResp = gDef.await()

                if (fResp.isSuccessful) _festivals.value = fResp.body()
                if (gResp.isSuccessful) _grahan.value    = gResp.body()
                if (!fResp.isSuccessful) _error.value = "Failed to load festivals (${fResp.code()})"
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun JsonObject.str(key: String) = this[key]?.jsonPrimitive?.contentOrNull ?: "—"
}
