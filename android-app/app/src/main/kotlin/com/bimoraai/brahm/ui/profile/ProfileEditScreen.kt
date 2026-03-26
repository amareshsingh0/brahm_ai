package com.bimoraai.brahm.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    navController: NavController,
    vm: ProfileViewModel = hiltViewModel(),
) {
    val user       by vm.user.collectAsState()
    val saveState  by vm.saveState.collectAsState()
    val suggestions by vm.citySuggestions.collectAsState()
    val focusManager = LocalFocusManager.current

    // ── Form state — pre-filled from loaded profile ──────────────────────────
    var fullName   by remember(user) { mutableStateOf(user?.name ?: "") }
    var dob        by remember(user) { mutableStateOf(user?.date ?: "") }   // YYYY-MM-DD
    var birthTime  by remember(user) { mutableStateOf(user?.time ?: "") }   // HH:MM (24h)
    var birthPlace by remember(user) { mutableStateOf(user?.place ?: "") }
    var gender     by remember(user) { mutableStateOf(user?.gender ?: "") }

    // Selected city coords (updated when user picks from autocomplete)
    var selectedLat by remember { mutableStateOf(0.0) }
    var selectedLon by remember { mutableStateOf(0.0) }
    var selectedTz  by remember { mutableStateOf(5.5) }
    var cityConfirmed by remember { mutableStateOf(user?.place?.isNotEmpty() == true) }

    // ── Dialog visibility ────────────────────────────────────────────────────
    var showDatePicker   by remember { mutableStateOf(false) }
    var showTimePicker   by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCitySuggestions by remember { mutableStateOf(false) }

    val genderOptions  = listOf("Male", "Female", "Other", "Prefer not to say")
    var genderExpanded by remember { mutableStateOf(false) }

    // Navigate back on success
    LaunchedEffect(saveState) {
        if (saveState is SaveState.Success) {
            vm.resetSaveState()
            navController.popBackStack()
        }
    }

    // ── Date picker state ────────────────────────────────────────────────────
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = parseDateToMillis(dob),
    )

    // ── Time picker state ────────────────────────────────────────────────────
    val (initHour, initMinute) = parseTime(birthTime)
    val timePickerState = rememberTimePickerState(
        initialHour   = initHour,
        initialMinute = initMinute,
        is24Hour      = false,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (saveState is SaveState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(36.dp)
                                .padding(end = 16.dp),
                            color = BrahmGold,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        TextButton(onClick = {
                            focusManager.clearFocus()
                            vm.saveProfile(
                                name   = fullName,
                                date   = dob,
                                time   = birthTime,
                                place  = birthPlace,
                                gender = gender,
                                lat    = selectedLat,
                                lon    = selectedLon,
                                tz     = selectedTz,
                            )
                        }) {
                            Text("Save", color = BrahmGold, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrahmCard),
            )
        },
        containerColor = BrahmBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // Error banner
            if (saveState is SaveState.Error) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        (saveState as SaveState.Error).msg,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onErrorContainer),
                    )
                }
            }

            // ── Personal Info ──────────────────────────────────────────────────
            ProfileEditSection("Personal Information") {

                // Full Name
                BrahmTextField(
                    value         = fullName,
                    onValueChange = { fullName = it },
                    label         = "Full Name",
                    leadingIcon   = Icons.Default.Person,
                )

                // Date of Birth — opens DatePickerDialog
                ReadonlyField(
                    value       = if (dob.isEmpty()) "" else formatDateDisplay(dob),
                    label       = "Date of Birth",
                    placeholder = "Select date",
                    leadingIcon = Icons.Default.CalendarToday,
                    trailingIcon = Icons.Default.CalendarMonth,
                    onClick     = { showDatePicker = true },
                )

                // Birth Time — opens TimePickerDialog
                ReadonlyField(
                    value        = if (birthTime.isEmpty()) "" else formatTimeDisplay(birthTime),
                    label        = "Birth Time",
                    placeholder  = "Select time",
                    leadingIcon  = Icons.Default.Schedule,
                    trailingIcon = Icons.Default.AccessTime,
                    onClick      = { showTimePicker = true },
                )

                // Birth Place — city autocomplete
                Column {
                    OutlinedTextField(
                        value         = birthPlace,
                        onValueChange = { v ->
                            birthPlace = v
                            cityConfirmed = false
                            vm.cityQuery.value = v
                            showCitySuggestions = v.length >= 2
                        },
                        modifier      = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (!it.isFocused) showCitySuggestions = false },
                        label         = { Text("Birth Place") },
                        placeholder   = { Text("Type city name…", color = BrahmMutedForeground) },
                        leadingIcon   = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrahmGold) },
                        trailingIcon  = {
                            if (cityConfirmed)
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF43A047))
                        },
                        singleLine    = true,
                        shape         = RoundedCornerShape(10.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = BrahmGold,
                            unfocusedBorderColor = BrahmBorder,
                        ),
                    )

                    // Suggestions dropdown
                    if (showCitySuggestions && suggestions.isNotEmpty()) {
                        Surface(
                            modifier      = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            shape         = RoundedCornerShape(10.dp),
                            color         = BrahmCard,
                            shadowElevation = 4.dp,
                        ) {
                            Column {
                                suggestions.forEach { city ->
                                    CityRow(
                                        city    = city,
                                        onClick = {
                                            birthPlace        = city.name
                                            selectedLat       = city.lat
                                            selectedLon       = city.lon
                                            selectedTz        = city.tz
                                            cityConfirmed     = true
                                            showCitySuggestions = false
                                            vm.cityQuery.value  = ""
                                            focusManager.clearFocus()
                                        },
                                    )
                                    if (city != suggestions.last()) {
                                        HorizontalDivider(color = BrahmBorder)
                                    }
                                }
                            }
                        }
                    }

                    // Coords shown when confirmed
                    if (cityConfirmed && selectedLat != 0.0) {
                        Text(
                            "%.4f°N  %.4f°E  · TZ +%.1f".format(selectedLat, selectedLon, selectedTz),
                            style    = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        )
                    }
                }

                // Gender dropdown
                ExposedDropdownMenuBox(
                    expanded          = genderExpanded,
                    onExpandedChange  = { genderExpanded = it },
                ) {
                    OutlinedTextField(
                        value         = gender,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Gender") },
                        leadingIcon   = { Icon(Icons.Default.People, contentDescription = null, tint = BrahmGold) },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        modifier      = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape         = RoundedCornerShape(10.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = BrahmGold,
                            unfocusedBorderColor = BrahmBorder,
                        ),
                    )
                    ExposedDropdownMenu(
                        expanded          = genderExpanded,
                        onDismissRequest  = { genderExpanded = false },
                    ) {
                        genderOptions.forEach { option ->
                            DropdownMenuItem(
                                text    = { Text(option) },
                                onClick = { gender = option; genderExpanded = false },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Delete Account ───────────────────────────────────────────────
            Surface(
                onClick  = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                color    = Color(0xFFFFEBEE),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Column {
                        Text("Delete Account", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium))
                        Text("Permanently delete your account and all data", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)))
                    }
                }
            }
        }
    }

    // ── Date Picker Dialog ───────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        dob = "%04d-%02d-%02d".format(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH),
                        )
                    }
                    showDatePicker = false
                }) { Text("OK", color = BrahmGold) }
            },
            dismissButton    = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(
                state  = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor   = BrahmGold,
                    todayDateBorderColor        = BrahmGold,
                    selectedYearContainerColor  = BrahmGold,
                ),
            )
        }
    }

    // ── Time Picker Dialog ───────────────────────────────────────────────────
    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BrahmCard,
            ) {
                Column(
                    modifier              = Modifier.padding(24.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                    verticalArrangement   = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "Select Birth Time",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
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
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            // timePickerState.hour is already in 24h internally
                            birthTime = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) { Text("OK", color = BrahmGold) }
                    }
                }
            }
        }
    }

    // ── Delete Confirmation ──────────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon             = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title            = { Text("Delete Account?") },
            text             = { Text("This will permanently delete your account, Kundali data, and chat history. This action cannot be undone.") },
            confirmButton    = {
                TextButton(
                    onClick = { showDeleteDialog = false /* TODO: call delete API */ },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton    = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

// ── City row in dropdown ─────────────────────────────────────────────────────

@Composable
private fun CityRow(city: City, onClick: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrahmGold, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(city.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "%.4f°N  %.4f°E".format(city.lat, city.lon),
                style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
            )
        }
    }
}

// ── Section wrapper ──────────────────────────────────────────────────────────

@Composable
private fun ProfileEditSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style    = MaterialTheme.typography.labelMedium.copy(color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = BrahmCard,
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content             = content,
        )
    }
}

// ── Readonly tappable field (Date / Time) ────────────────────────────────────
// Uses Box overlay so clicks reach the handler even with readOnly=true

@Composable
private fun ReadonlyField(
    value: String,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector,
    onClick: () -> Unit,
) {
    Box {
        OutlinedTextField(
            value         = value,
            onValueChange = {},
            readOnly      = true,
            modifier      = Modifier.fillMaxWidth(),
            label         = { Text(label) },
            placeholder   = { Text(placeholder, color = BrahmMutedForeground) },
            leadingIcon   = { Icon(leadingIcon, contentDescription = null, tint = BrahmGold) },
            trailingIcon  = { Icon(trailingIcon, contentDescription = null, tint = BrahmMutedForeground) },
            singleLine    = true,
            shape         = RoundedCornerShape(10.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = BrahmGold,
                unfocusedBorderColor = BrahmBorder,
            ),
        )
        // Transparent overlay captures the tap (readOnly fields block keyboard but pass clicks)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick),
        )
    }
}

// ── Normal editable field ────────────────────────────────────────────────────

@Composable
private fun BrahmTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = Modifier.fillMaxWidth(),
        label         = { Text(label) },
        leadingIcon   = { Icon(leadingIcon, contentDescription = null, tint = BrahmGold) },
        singleLine    = true,
        shape         = RoundedCornerShape(10.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = BrahmGold,
            unfocusedBorderColor = BrahmBorder,
        ),
    )
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/** "YYYY-MM-DD" → epoch millis for DatePickerState */
private fun parseDateToMillis(date: String): Long? {
    return try {
        val parts = date.split("-")
        if (parts.size != 3) return null
        val cal = Calendar.getInstance()
        cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    } catch (_: Exception) { null }
}

/** "HH:MM" → Pair(hour24, minute) */
private fun parseTime(time: String): Pair<Int, Int> {
    return try {
        val parts = time.split(":")
        Pair(parts[0].toInt(), parts[1].toInt())
    } catch (_: Exception) { Pair(6, 0) }
}

/** "YYYY-MM-DD" → "15 Jan 1990" */
private fun formatDateDisplay(date: String): String {
    return try {
        val parts = date.split("-")
        val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        "${parts[2].toInt()} ${months[parts[1].toInt() - 1]} ${parts[0]}"
    } catch (_: Exception) { date }
}

/** "HH:MM" (24h) → "2:30 PM" */
private fun formatTimeDisplay(time: String): String {
    return try {
        val parts  = time.split(":")
        val hour24 = parts[0].toInt()
        val minute = parts[1].toInt()
        val ampm   = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0  -> 12
            hour24 > 12  -> hour24 - 12
            else         -> hour24
        }
        "$hour12:%02d $ampm".format(minute)
    } catch (_: Exception) { time }
}
