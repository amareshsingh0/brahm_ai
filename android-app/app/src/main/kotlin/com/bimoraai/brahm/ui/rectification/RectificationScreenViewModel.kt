package com.bimoraai.brahm.ui.rectification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bimoraai.brahm.core.data.UserRepository
import com.bimoraai.brahm.core.network.ApiService
import com.bimoraai.brahm.core.network.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.add
import javax.inject.Inject

data class LifeEvent(val date: String = "", val type: String = "marriage")

@HiltViewModel
class RectificationScreenViewModel @Inject constructor(
    private val api: ApiService,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _result    = MutableStateFlow<JsonObject?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error     = MutableStateFlow<String?>(null)
    private val _hasData   = MutableStateFlow(false)

    val result:    StateFlow<JsonObject?> = _result
    val isLoading: StateFlow<Boolean>    = _isLoading
    val error:     StateFlow<String?>    = _error
    val hasData:   StateFlow<Boolean>    = _hasData

    val name        = MutableStateFlow("")
    val dob         = MutableStateFlow("")
    val approxTob   = MutableStateFlow("")
    val pob         = MutableStateFlow("")
    val lat         = MutableStateFlow(0.0)
    val lon         = MutableStateFlow(0.0)
    val tz          = MutableStateFlow("5.5")
    val uncertainty = MutableStateFlow(60) // minutes

    val events = MutableStateFlow(listOf(LifeEvent(type = "marriage"), LifeEvent(type = "career_start")))

    init {
        viewModelScope.launch {
            userRepository.user
                .filterNotNull()
                .first()
                .let { u -> prefillFromProfile(u) }
        }
    }

    private fun prefillFromProfile(u: UserDto) {
        if (name.value.isBlank() && u.name.isNotBlank()) name.value = u.name
        if (dob.value.isBlank() && u.date.isNotBlank()) dob.value = u.date
        if (approxTob.value.isBlank() && u.time.isNotBlank()) approxTob.value = u.time
        if (pob.value.isBlank() && u.place.isNotBlank()) pob.value = u.place
        if (lat.value == 0.0 && u.lat != 0.0) lat.value = u.lat
        if (lon.value == 0.0 && u.lon != 0.0) lon.value = u.lon
        if (u.tz != 0.0) tz.value = u.tz.toString()
    }

    fun addEvent() {
        events.value = events.value + LifeEvent(type = "career_start")
    }

    fun removeEvent(index: Int) {
        if (events.value.size > 1) {
            events.value = events.value.filterIndexed { i, _ -> i != index }
        }
    }

    fun updateEventDate(index: Int, date: String) {
        events.value = events.value.mapIndexed { i, ev -> if (i == index) ev.copy(date = date) else ev }
    }

    fun updateEventType(index: Int, type: String) {
        events.value = events.value.mapIndexed { i, ev -> if (i == index) ev.copy(type = type) else ev }
    }

    fun calculate() {
        if (dob.value.isBlank() || approxTob.value.isBlank()) {
            _error.value = "Please enter date and approximate time of birth"
            return
        }
        if (lat.value == 0.0 || lon.value == 0.0) {
            _error.value = "Please select a birth place from the suggestions"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            try {
                val validEvents = events.value.filter { it.date.isNotBlank() && it.type.isNotBlank() }
                val body = buildJsonObject {
                    put("date",                JsonPrimitive(dob.value))
                    put("approx_time",         JsonPrimitive(approxTob.value))
                    put("uncertainty_minutes", JsonPrimitive(uncertainty.value))
                    put("lat",                 JsonPrimitive(lat.value))
                    put("lon",                 JsonPrimitive(lon.value))
                    put("tz",                  JsonPrimitive(tz.value.toDoubleOrNull() ?: 5.5))
                    put("events", buildJsonArray {
                        validEvents.forEach { ev ->
                            add(buildJsonObject {
                                put("date", JsonPrimitive(ev.date))
                                put("type", JsonPrimitive(ev.type))
                            })
                        }
                    })
                }
                val resp = api.getRectification(body)
                if (resp.isSuccessful) {
                    _result.value  = resp.body()
                    _hasData.value = true
                } else {
                    _error.value = "Rectification failed. Please check inputs."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun load() { if (hasData.value) calculate() }
    fun reset() { _hasData.value = false; _result.value = null; _error.value = null }
}
