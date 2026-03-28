package com.bimoraai.brahm.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.ui.main.Route
import com.bimoraai.brahm.core.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    vm: ProfileViewModel = hiltViewModel(),
) {
    val user          by vm.user.collectAsState()
    val saveState     by vm.saveState.collectAsState()
    val sessionExpired by vm.sessionExpired.collectAsState()
    val suggestions   by vm.citySuggestions.collectAsState()

    // Session expired → force logout to login screen
    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            navController.navigate(Route.LOGIN) { popUpTo(0) }
        }
    }
    val context    = LocalContext.current
    val focusManager = LocalFocusManager.current
    var showLogoutDialog   by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showEditSheet      by remember { mutableStateOf(false) }

    val savedLang = remember {
        context.getSharedPreferences("brahm_prefs", Context.MODE_PRIVATE)
            .getString("language", null)
    }
    val currentLangLabel = when (savedLang) {
        "hi" -> "हिंदी (Hindi)"
        "en" -> "English"
        else -> "System Default"
    }

    val plan = user?.plan ?: "free"
    val isPaid    = plan != "free"
    val planColor = if (isPaid) BrahmGold else BrahmMutedForeground
    val planBg    = if (isPaid) BrahmGold.copy(alpha = 0.12f) else Color(0xFFE5E7EB)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrahmCard),
            )
        },
        containerColor = BrahmBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {

            // ── User card (top, like Grok) ──────────────────────────────────────
            Surface(color = BrahmCard) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Avatar circle
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(BrahmGold),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (user?.name?.firstOrNull() ?: "U").toString().uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user?.name ?: "User", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text(user?.phone ?: user?.email ?: "—", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    }
                    // Plan badge
                    Surface(shape = RoundedCornerShape(20.dp), color = planBg) {
                        Text(
                            text = plan.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium.copy(color = planColor, fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── ACCOUNT section ────────────────────────────────────────────────
            SettingsSectionLabel("Account")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Default.Person,
                    iconColor = Color(0xFF5C6BC0),
                    label = "Profile",
                    subtitle = "Name, birth details, gender",
                    onClick = { showEditSheet = true },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = BrahmBorder)
                SettingsItem(
                    icon = Icons.Default.CreditCard,
                    iconColor = BrahmGold,
                    label = "Billing",
                    subtitle = if (isPaid) "${plan.replaceFirstChar { it.uppercase() }} plan active" else "Free plan · Upgrade available",
                    onClick = { navController.navigate(Route.BILLING) },
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── PREFERENCES section ─────────────────────────────────────────────
            SettingsSectionLabel("Preferences")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    iconColor = Color(0xFF43A047),
                    label = "Notifications",
                    subtitle = "Manage app notifications",
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = BrahmBorder)
                SettingsItem(
                    icon = Icons.Default.Language,
                    iconColor = Color(0xFF7B1FA2),
                    label = "Language",
                    subtitle = currentLangLabel,
                    onClick = { showLanguageDialog = true },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = BrahmBorder)
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    iconColor = Color(0xFF37474F),
                    label = "Dark Mode",
                    subtitle = "Coming soon — light theme active",
                    onClick = { /* Phase 2 */ },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = BrahmBorder)
                SettingsItem(
                    icon = Icons.Default.Share,
                    iconColor = Color(0xFF00ACC1),
                    label = "Share App",
                    subtitle = "Invite friends to Brahm AI",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Try Brahm AI — Vedic Astrology App")
                            putExtra(Intent.EXTRA_TEXT, "Check out Brahm AI — AI-powered Vedic astrology! ☽✨\nhttps://brahmasmi.bimoraai.com")
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Brahm AI"))
                    },
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── CHATS section ────────────────────────────────────────────────────
            SettingsSectionLabel("Chats")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Default.Archive,
                    iconColor = Color(0xFF6D4C41),
                    label = "Archived Chats",
                    subtitle = "View your archived conversations",
                    onClick = { navController.navigate(com.bimoraai.brahm.ui.main.Route.ARCHIVED_CHATS) },
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── LEGAL section ───────────────────────────────────────────────────
            SettingsSectionLabel("Legal")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Default.PrivacyTip,
                    iconColor = Color(0xFF546E7A),
                    label = "Privacy Policy",
                    subtitle = "How we handle your data",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://brahmasmi.bimoraai.com/privacy"))
                        context.startActivity(intent)
                    },
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Sign Out ────────────────────────────────────────────────────────
            Surface(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFEBEE),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Text("Sign Out", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Inline Profile Edit Bottom Sheet ────────────────────────────────────
    if (showEditSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        // Form state pre-filled from current user
        var fullName   by remember(user) { mutableStateOf(user?.name ?: "") }
        var dob        by remember(user) { mutableStateOf(user?.date ?: "") }
        var birthTime  by remember(user) { mutableStateOf(user?.time ?: "") }
        var birthPlace by remember(user) { mutableStateOf(user?.place ?: "") }
        var gender     by remember(user) { mutableStateOf(user?.gender ?: "") }
        var selectedLat by remember(user) { mutableStateOf(user?.lat ?: 0.0) }
        var selectedLon by remember(user) { mutableStateOf(user?.lon ?: 0.0) }
        var selectedTz  by remember(user) { mutableStateOf(user?.tz ?: 5.5) }
        var cityConfirmed by remember(user) { mutableStateOf(user?.place?.isNotEmpty() == true) }
        var showDatePicker   by remember { mutableStateOf(false) }
        var showTimePicker   by remember { mutableStateOf(false) }
        var showCitySuggestions by remember { mutableStateOf(false) }
        var genderExpanded   by remember { mutableStateOf(false) }
        val genderOptions    = listOf("Male", "Female", "Other", "Prefer not to say")
        val datePickerState  = rememberDatePickerState(initialSelectedDateMillis = parseDateToMillis(dob))
        val (initHour, initMinute) = parseTime(birthTime)
        val timePickerState  = rememberTimePickerState(initialHour = initHour, initialMinute = initMinute, is24Hour = false)

        // Close sheet on save success
        LaunchedEffect(saveState) {
            if (saveState is SaveState.Success) {
                vm.resetSaveState()
                showEditSheet = false
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState  = sheetState,
            containerColor = BrahmBackground,
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Edit Profile", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                if (saveState is SaveState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = BrahmGold, strokeWidth = 2.dp)
                } else {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            vm.saveProfile(fullName, dob, birthTime, birthPlace, gender, selectedLat, selectedLon, selectedTz)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrahmGold),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    ) { Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
            }

            if (saveState is SaveState.Error) {
                Text(
                    (saveState as SaveState.Error).msg,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(16.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Full Name
                OutlinedTextField(
                    value = fullName, onValueChange = { fullName = it },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Full Name") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = BrahmGold) },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold, unfocusedBorderColor = BrahmBorder),
                )
                // DOB
                Box {
                    OutlinedTextField(
                        value = if (dob.isEmpty()) "" else formatDateDisplay(dob), onValueChange = {},
                        readOnly = true, modifier = Modifier.fillMaxWidth(), label = { Text("Date of Birth") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Default.CalendarToday, null, tint = BrahmGold) },
                        trailingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = BrahmMutedForeground) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold, unfocusedBorderColor = BrahmBorder),
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                }
                // Birth Time
                Box {
                    OutlinedTextField(
                        value = if (birthTime.isEmpty()) "" else formatTimeDisplay(birthTime), onValueChange = {},
                        readOnly = true, modifier = Modifier.fillMaxWidth(), label = { Text("Birth Time") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Schedule, null, tint = BrahmGold) },
                        trailingIcon = { Icon(Icons.Default.AccessTime, null, tint = BrahmMutedForeground) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold, unfocusedBorderColor = BrahmBorder),
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showTimePicker = true })
                }
                // Birth Place
                Column {
                    Box {
                        OutlinedTextField(
                            value = birthPlace,
                            onValueChange = { v -> birthPlace = v; cityConfirmed = false; vm.cityQuery.value = v; showCitySuggestions = v.length >= 2 },
                            modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) showCitySuggestions = false },
                            label = { Text("Birth Place") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = BrahmGold) },
                            trailingIcon = { if (cityConfirmed) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF43A047)) },
                            singleLine = true, shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold, unfocusedBorderColor = BrahmBorder),
                        )
                        DropdownMenu(
                            expanded = showCitySuggestions && suggestions.isNotEmpty(),
                            onDismissRequest = { showCitySuggestions = false },
                            offset = DpOffset(0.dp, 0.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            suggestions.forEach { city ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(if (city.country.isNotBlank()) "${city.name}, ${city.country}" else city.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("%.2f°N  %.2f°E  · UTC+%.1f".format(city.lat, city.lon, city.tz),
                                                style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = BrahmGold, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        birthPlace = city.name; selectedLat = city.lat; selectedLon = city.lon
                                        selectedTz = city.tz; cityConfirmed = true; showCitySuggestions = false
                                        vm.cityQuery.value = ""; focusManager.clearFocus()
                                    },
                                )
                                if (city != suggestions.last()) HorizontalDivider(color = BrahmBorder)
                            }
                        }
                    }
                    if (cityConfirmed && selectedLat != 0.0) {
                        Text("%.4f°N  %.4f°E  · TZ +%.1f".format(selectedLat, selectedLon, selectedTz),
                            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                    }
                }
                // Gender
                ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = it }) {
                    OutlinedTextField(
                        value = gender, onValueChange = {}, readOnly = true,
                        label = { Text("Gender") },
                        leadingIcon = { Icon(Icons.Default.People, null, tint = BrahmGold) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold, unfocusedBorderColor = BrahmBorder),
                    )
                    ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                        genderOptions.forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = { gender = opt; genderExpanded = false })
                        }
                    }
                }
            }
        }

        // Date Picker Dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = { TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        dob = "%04d-%02d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
                    }; showDatePicker = false
                }) { Text("OK", color = BrahmGold) } },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
                colors = DatePickerDefaults.colors(containerColor = BrahmCard),
            ) {
                DatePicker(state = datePickerState, colors = DatePickerDefaults.colors(
                    containerColor = BrahmCard, titleContentColor = BrahmMutedForeground,
                    headlineContentColor = BrahmForeground, weekdayContentColor = BrahmMutedForeground,
                    subheadContentColor = BrahmMutedForeground, navigationContentColor = BrahmForeground,
                    yearContentColor = BrahmForeground, currentYearContentColor = BrahmGold,
                    selectedYearContentColor = BrahmCard, selectedYearContainerColor = BrahmGold,
                    dayContentColor = BrahmForeground, todayContentColor = BrahmGold,
                    todayDateBorderColor = BrahmGold, selectedDayContentColor = BrahmCard,
                    selectedDayContainerColor = BrahmGold,
                ))
            }
        }
        // Time Picker Dialog
        if (showTimePicker) {
            Dialog(onDismissRequest = { showTimePicker = false }) {
                Surface(shape = RoundedCornerShape(16.dp), color = BrahmCard) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Select Birth Time", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        TimePicker(state = timePickerState, colors = TimePickerDefaults.colors(
                            clockDialColor = BrahmBackground, selectorColor = BrahmGold,
                            timeSelectorSelectedContainerColor = BrahmGold, timeSelectorSelectedContentColor = Color.White,
                            timeSelectorUnselectedContainerColor = Color(0xFFF0EAD6), timeSelectorUnselectedContentColor = BrahmForeground,
                        ))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { birthTime = "%02d:%02d".format(timePickerState.hour, timePickerState.minute); showTimePicker = false }) { Text("OK", color = BrahmGold) }
                        }
                    }
                }
            }
        }
    }

    // Language picker dialog
    if (showLanguageDialog) {
        val languages = listOf(
            null   to "System Default",
            "en"   to "English",
            "hi"   to "हिंदी (Hindi)",
        )
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Language", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)) },
            text = {
                Column {
                    languages.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLanguageDialog = false
                                    val prefs = context.getSharedPreferences("brahm_prefs", Context.MODE_PRIVATE)
                                    if (code == null) {
                                        prefs.edit().remove("language").apply()
                                    } else {
                                        prefs.edit().putString("language", code).apply()
                                    }
                                    // Restart activity to apply new locale
                                    (context as? Activity)?.recreate()
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(
                                selected = savedLang == code,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = BrahmGold),
                            )
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Cancel") }
            },
        )
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        vm.logout { navController.navigate(Route.LOGIN) { popUpTo(0) } }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Sign Out") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            },
        )
    }
}

// ── Reusable composables ─────────────────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(label: String) {
    Text(
        text = label.uppercase(),
        modifier = Modifier.padding(start = 20.dp, bottom = 6.dp),
        style = MaterialTheme.typography.labelSmall.copy(
            color = BrahmMutedForeground,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        ),
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = BrahmCard,
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrahmMutedForeground, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Helpers (shared with edit sheet) ─────────────────────────────────────────

private fun parseDateToMillis(date: String): Long? = try {
    val p = date.split("-")
    if (p.size != 3) null
    else Calendar.getInstance().apply { set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt(), 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
} catch (_: Exception) { null }

private fun parseTime(time: String): Pair<Int, Int> = try {
    val p = time.split(":"); Pair(p[0].toInt(), p[1].toInt())
} catch (_: Exception) { Pair(6, 0) }

private fun formatDateDisplay(date: String): String = try {
    val p = date.split("-")
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    "${p[2].toInt()} ${months[p[1].toInt() - 1]} ${p[0]}"
} catch (_: Exception) { date }

private fun formatTimeDisplay(time: String): String = try {
    val p = time.split(":"); val h = p[0].toInt(); val m = p[1].toInt()
    val ampm = if (h < 12) "AM" else "PM"
    val h12  = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
    "$h12:%02d $ampm".format(m)
} catch (_: Exception) { time }
