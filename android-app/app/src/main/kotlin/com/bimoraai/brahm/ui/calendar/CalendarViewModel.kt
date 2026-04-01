package com.bimoraai.brahm.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.City
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val api: ApiService,
    private val userRepository: UserRepository,
) : ViewModel() {

    private var lastKnownDate = LocalDate.now()

    val year        = MutableStateFlow(lastKnownDate.year)
    val month       = MutableStateFlow(lastKnownDate.monthValue)
    val tradition   = MutableStateFlow("smarta")
    val lunarSystem = MutableStateFlow("amanta")

    // Current city — default Ujjain, overridden from profile or user pick
    val cityName = MutableStateFlow("Ujjain")
    private var lat = 23.1765
    private var lon = 75.7885
    private var tz  = 5.5

    private val _calendar  = MutableStateFlow<JsonObject?>(null)
    private val _grahan    = MutableStateFlow<JsonObject?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _error     = MutableStateFlow<String?>(null)

    val calendar:  StateFlow<JsonObject?> = _calendar
    val grahan:    StateFlow<JsonObject?> = _grahan
    val isLoading: StateFlow<Boolean>    = _isLoading
    val error:     StateFlow<String?>    = _error

    init {
        // Pre-fill from profile
        val u = userRepository.user.value
        if (u != null && u.lat != 0.0) {
            lat = u.lat; lon = u.lon; tz = u.tz
            cityName.value = u.place.takeIf { it.isNotBlank() } ?: "Ujjain"
        }
        load()
    }

    fun setCity(city: City) {
        lat = city.lat; lon = city.lon; tz = city.tz
        cityName.value = city.name
        load()
    }

    fun prevMonth() {
        if (month.value == 1) { year.value -= 1; month.value = 12 }
        else month.value -= 1
        load()
    }

    fun nextMonth() {
        if (month.value == 12) { year.value += 1; month.value = 1 }
        else month.value += 1
        load()
    }

    fun goToday() {
        val t = LocalDate.now()
        year.value = t.year; month.value = t.monthValue
        load()
    }

    fun setYear(y: Int) {
        if (y in 1800..2200) { year.value = y; load() }
    }

    fun setTradition(t: String) { tradition.value = t; load() }
    fun setLunarSystem(s: String) { lunarSystem.value = s; load() }

    /** Call this from screen's OnResume/LaunchedEffect — refreshes if date changed at midnight */
    fun checkDateChange() {
        val today = LocalDate.now()
        if (today != lastKnownDate) {
            lastKnownDate = today
            year.value = today.year
            month.value = today.monthValue
            load()
        }
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val calDef = async {
                    api.getCalendar(
                        year = year.value, month = month.value,
                        lat = lat, lon = lon, tz = tz,
                        tradition = tradition.value,
                        lunarSystem = lunarSystem.value,
                    )
                }
                val grahanDef = async { api.getGrahan() }

                val calResp    = calDef.await()
                val grahanResp = grahanDef.await()

                if (calResp.isSuccessful) _calendar.value = calResp.body()
                else _error.value = "Failed to load calendar (${calResp.code()})"

                if (grahanResp.isSuccessful) _grahan.value = grahanResp.body()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load calendar"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun JsonObject.str(key: String) = this[key]?.jsonPrimitive?.contentOrNull ?: "—"
}
