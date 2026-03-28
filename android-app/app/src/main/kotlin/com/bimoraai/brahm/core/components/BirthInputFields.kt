package com.bimoraai.brahm.core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.bimoraai.brahm.core.data.CitySearchViewModel
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.theme.*
import java.util.Calendar

/**
 * Shared birth data input fields used across all feature screens.
 * Provides DatePicker, TimePicker, and city autocomplete — no manual typing needed.
 *
 * @param cityVmKey  Pass a unique key when using two instances in the same screen (e.g. Compatibility).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthInputFields(
    dob: String,
    onDobChange: (String) -> Unit,
    tob: String,
    onTobChange: (String) -> Unit,
    pob: String,
    onPobChange: (String) -> Unit,
    onCitySelected: (City) -> Unit,
    name: String = "",
    onNameChange: (String) -> Unit = {},
    showName: Boolean = true,
    showDob: Boolean = true,
    showTob: Boolean = true,
    cityVmKey: String = "default",
    cityVm: CitySearchViewModel = hiltViewModel(key = cityVmKey),
) {
    val suggestions  by cityVm.suggestions.collectAsState()
    val focusManager = LocalFocusManager.current

    var showDatePicker      by remember { mutableStateOf(false) }
    var showTimePicker      by remember { mutableStateOf(false) }
    var showCitySuggestions by remember { mutableStateOf(false) }
    var cityConfirmed       by remember(pob) { mutableStateOf(pob.isNotBlank()) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = parseDateToMillis(dob))
    val (initHour, initMin) = parseTime(tob)
    val timePickerState = rememberTimePickerState(initialHour = initHour, initialMinute = initMin, is24Hour = false)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // ── Name ──────────────────────────────────────────────────────────────
        if (showName) {
            OutlinedTextField(
                value         = name,
                onValueChange = onNameChange,
                modifier      = Modifier.fillMaxWidth(),
                label         = { Text("Full Name") },
                leadingIcon   = { Icon(Icons.Default.Person, contentDescription = null, tint = BrahmGold) },
                singleLine    = true,
                shape         = RoundedCornerShape(10.dp),
                colors        = fieldColors(),
            )
        }

        // ── Date of Birth ─────────────────────────────────────────────────────
        if (showDob) Box {
            OutlinedTextField(
                value         = if (dob.isEmpty()) "" else formatDateDisplay(dob),
                onValueChange = {},
                readOnly      = true,
                modifier      = Modifier.fillMaxWidth(),
                label         = { Text("Date of Birth") },
                placeholder   = { Text("Select date", color = BrahmMutedForeground) },
                leadingIcon   = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BrahmGold) },
                trailingIcon  = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = BrahmMutedForeground) },
                singleLine    = true,
                shape         = RoundedCornerShape(10.dp),
                colors        = fieldColors(),
            )
            Box(Modifier.matchParentSize().clickable { showDatePicker = true })
        }

        // ── Time of Birth ─────────────────────────────────────────────────────
        if (showTob) Box {
            OutlinedTextField(
                value         = if (tob.isEmpty()) "" else formatTimeDisplay(tob),
                onValueChange = {},
                readOnly      = true,
                modifier      = Modifier.fillMaxWidth(),
                label         = { Text("Time of Birth") },
                placeholder   = { Text("Select time", color = BrahmMutedForeground) },
                leadingIcon   = { Icon(Icons.Default.Schedule, contentDescription = null, tint = BrahmGold) },
                trailingIcon  = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = BrahmMutedForeground) },
                singleLine    = true,
                shape         = RoundedCornerShape(10.dp),
                colors        = fieldColors(),
            )
            Box(Modifier.matchParentSize().clickable { showTimePicker = true })
        }

        // ── Place of Birth ────────────────────────────────────────────────────
        Column {
            // Suggestions card shown ABOVE the text field — no Popup, no clipping, keyboard stays up
            if (showCitySuggestions && suggestions.isNotEmpty()) {
                androidx.compose.material3.Card(
                    shape     = RoundedCornerShape(10.dp),
                    colors    = androidx.compose.material3.CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier  = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        suggestions.forEach { city ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onPobChange(city.name)
                                        onCitySelected(city)
                                        cityVm.cityQuery.value = ""
                                        cityConfirmed          = true
                                        showCitySuggestions    = false
                                        focusManager.clearFocus()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrahmGold, modifier = Modifier.size(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (city.country.isNotBlank()) "${city.name}, ${city.country}" else city.name,
                                        style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "%.2f°N  %.2f°E  · UTC+%.1f".format(city.lat, city.lon, city.tz),
                                        style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
                                    )
                                }
                            }
                            if (city != suggestions.last()) HorizontalDivider(color = BrahmBorder)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            OutlinedTextField(
                value         = pob,
                onValueChange = { v ->
                    onPobChange(v)
                    cityConfirmed = false
                    cityVm.cityQuery.value = v
                    showCitySuggestions = v.length >= 2
                },
                modifier      = Modifier.fillMaxWidth(),
                label         = { Text("Place of Birth") },
                placeholder   = { Text("Type city name…", color = BrahmMutedForeground) },
                leadingIcon   = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrahmGold) },
                trailingIcon  = {
                    if (cityConfirmed)
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF43A047))
                },
                singleLine    = true,
                shape         = RoundedCornerShape(10.dp),
                colors        = fieldColors(),
            )
        }
    }

    // ── Date Picker Dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        onDobChange("%04d-%02d-%02d".format(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH),
                        ))
                    }
                    showDatePicker = false
                }) { Text("OK", color = BrahmGold) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
            colors = DatePickerDefaults.colors(containerColor = BrahmCard),
        ) {
            DatePicker(
                state  = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor             = BrahmCard,
                    titleContentColor          = BrahmMutedForeground,
                    headlineContentColor       = BrahmForeground,
                    weekdayContentColor        = BrahmMutedForeground,
                    subheadContentColor        = BrahmMutedForeground,
                    navigationContentColor     = BrahmForeground,
                    yearContentColor           = BrahmForeground,
                    currentYearContentColor    = BrahmGold,
                    selectedYearContentColor   = BrahmCard,
                    selectedYearContainerColor = BrahmGold,
                    dayContentColor            = BrahmForeground,
                    todayContentColor          = BrahmGold,
                    todayDateBorderColor       = BrahmGold,
                    selectedDayContentColor    = BrahmCard,
                    selectedDayContainerColor  = BrahmGold,
                ),
            )
        }
    }

    // ── Time Picker Dialog ────────────────────────────────────────────────────
    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = BrahmCard) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Select Birth Time", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    TimePicker(
                        state  = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor                       = BrahmBackground,
                            selectorColor                        = BrahmGold,
                            timeSelectorSelectedContainerColor   = BrahmGold,
                            timeSelectorSelectedContentColor     = Color.White,
                            timeSelectorUnselectedContainerColor = Color(0xFFF0EAD6),
                            timeSelectorUnselectedContentColor   = BrahmForeground,
                        ),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            onTobChange("%02d:%02d".format(timePickerState.hour, timePickerState.minute))
                            showTimePicker = false
                        }) { Text("OK", color = BrahmGold) }
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = BrahmGold,
    unfocusedBorderColor = BrahmBorder,
)

private fun parseDateToMillis(date: String): Long? = try {
    val p = date.split("-")
    if (p.size != 3) null
    else Calendar.getInstance().apply {
        set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt(), 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
} catch (_: Exception) { null }

private fun parseTime(time: String): Pair<Int, Int> = try {
    val p = time.split(":")
    Pair(p[0].toInt(), p[1].toInt())
} catch (_: Exception) { Pair(6, 0) }

private fun formatDateDisplay(date: String): String = try {
    val p = date.split("-")
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    "${p[2].toInt()} ${months[p[1].toInt() - 1]} ${p[0]}"
} catch (_: Exception) { date }

private fun formatTimeDisplay(time: String): String = try {
    val p = time.split(":")
    val h = p[0].toInt(); val m = p[1].toInt()
    val ampm = if (h < 12) "AM" else "PM"
    val h12  = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
    "$h12:%02d $ampm".format(m)
} catch (_: Exception) { time }
