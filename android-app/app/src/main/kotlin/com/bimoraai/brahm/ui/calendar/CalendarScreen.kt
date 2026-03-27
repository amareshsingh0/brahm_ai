package com.bimoraai.brahm.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.BrahmCard
import com.bimoraai.brahm.core.data.CitySearchViewModel
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.*

// ─── Constants ────────────────────────────────────────────────────────────────

private val WEEK_HEADERS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val MONTH_NAMES  = listOf(
    "January","February","March","April","May","June",
    "July","August","September","October","November","December",
)
private val TRADITIONS = listOf(
    "smarta" to "Smarta",
    "vaishnava" to "Vaishnava",
    "shakta" to "Shakta",
    "shaiva" to "Shaiva",
)
private val LUNAR_SYSTEMS = listOf("amanta" to "Amanta", "purnimanta" to "Purnimanta")

// ─── Color helpers ────────────────────────────────────────────────────────────

private fun dayBgColor(day: JsonObject, hasEclipse: Boolean): Color = when {
    hasEclipse        -> Color(0xFFB71C1C).copy(alpha = 0.15f)
    day.bool("has_festival") -> Color(0xFFF59E0B).copy(alpha = 0.10f)
    day.bool("is_purnima")   -> Color(0xFF3B82F6).copy(alpha = 0.10f)
    day.bool("is_amavasya")  -> Color(0xFF64748B).copy(alpha = 0.12f)
    day.bool("is_ekadashi")  -> Color(0xFF10B981).copy(alpha = 0.10f)
    day.bool("is_pradosh")   -> Color(0xFFA855F7).copy(alpha = 0.10f)
    day.bool("is_chaturthi") -> Color(0xFFF97316).copy(alpha = 0.08f)
    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)
}

private fun dayBorderColor(day: JsonObject, hasEclipse: Boolean): Color = when {
    hasEclipse        -> Color(0xFFEF4444).copy(alpha = 0.40f)
    day.bool("has_festival") -> Color(0xFFF59E0B).copy(alpha = 0.25f)
    day.bool("is_purnima")   -> Color(0xFF3B82F6).copy(alpha = 0.25f)
    day.bool("is_amavasya")  -> Color(0xFF64748B).copy(alpha = 0.25f)
    day.bool("is_ekadashi")  -> Color(0xFF10B981).copy(alpha = 0.25f)
    day.bool("is_pradosh")   -> Color(0xFFA855F7).copy(alpha = 0.25f)
    day.bool("is_chaturthi") -> Color(0xFFF97316).copy(alpha = 0.20f)
    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
}

private fun dayIcon(day: JsonObject, hasEclipse: Boolean, isSolarEclipse: Boolean): String? = when {
    hasEclipse           -> if (isSolarEclipse) "☀️🌑" else "🌕🌑"
    day.bool("is_purnima")  -> "🌕"
    day.bool("is_amavasya") -> "🌑"
    day.bool("is_ekadashi") -> "✦"
    day.bool("is_pradosh")  -> "☽"
    else -> null
}

private fun shortTithi(name: String): String {
    if (name == "N/A" || name == "—") return ""
    if (name.length <= 6) return name
    return name.take(5) + "."
}

// JSON helpers
private fun JsonObject.str(key: String) = this[key]?.jsonPrimitive?.contentOrNull ?: "—"
private fun JsonObject.bool(key: String) = this[key]?.jsonPrimitive?.booleanOrNull == true
private fun JsonObject.int(key: String) = this[key]?.jsonPrimitive?.intOrNull ?: 0
private fun JsonObject.arr(key: String) = this[key]?.jsonArray

// ─── Main Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavController,
    vm: CalendarViewModel   = hiltViewModel(),
    cityVm: CitySearchViewModel = hiltViewModel(),
) {
    val year        by vm.year.collectAsState()
    val month       by vm.month.collectAsState()
    val tradition   by vm.tradition.collectAsState()
    val lunarSystem by vm.lunarSystem.collectAsState()
    val cityName    by vm.cityName.collectAsState()
    val calendar    by vm.calendar.collectAsState()
    val grahan      by vm.grahan.collectAsState()
    val isLoading   by vm.isLoading.collectAsState()
    val error       by vm.error.collectAsState()

    val suggestions by cityVm.suggestions.collectAsState()
    val cityQuery   by cityVm.cityQuery.collectAsState()

    var viewMode       by remember { mutableStateOf("grid") }   // "grid" | "list"
    var selectedDay    by remember { mutableStateOf<JsonObject?>(null) }
    var showSheet      by remember { mutableStateOf(false) }
    var yearInput      by remember { mutableStateOf(year.toString()) }
    var showTradition  by remember { mutableStateOf(false) }
    var showLunar      by remember { mutableStateOf(false) }

    // Build eclipse map: date-string → eclipse JsonObject
    val eclipseMap: Map<String, JsonObject> = remember(grahan) {
        val m = mutableMapOf<String, JsonObject>()
        grahan?.arr("eclipses")?.forEach { el ->
            val e = el.jsonObject
            val d = e.str("date")
            if (d != "—") m[d] = e
        }
        m
    }

    val days: List<JsonObject> = remember(calendar) {
        calendar?.arr("days")?.map { it.jsonObject } ?: emptyList()
    }
    val firstWeekday  = calendar?.int("first_weekday") ?: 0
    val vikramSamvat  = calendar?.str("vikram_samvat")?.takeIf { it != "—" }
    val lunarMonths   = calendar?.arr("lunar_months")?.map { it.jsonPrimitive.content } ?: emptyList()
    val adhikNote     = calendar?.str("adhik_maas_note")?.takeIf { it != "—" }
    val kshayaNote    = calendar?.str("kshaya_maas_note")?.takeIf { it != "—" }

    // Sync yearInput when vm year changes (e.g., from goToday)
    LaunchedEffect(year) { yearInput = year.toString() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Vedic Calendar", fontWeight = FontWeight.Bold)
                        Text("Panchang · Festivals · Eclipses", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                )
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            // ── Vikram Samvat header ──────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Any year · any city",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                    if (vikramSamvat != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Vikram Samvat", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(vikramSamvat, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = BrahmGold)
                        }
                    }
                }
            }

            // ── Lunar months banner ───────────────────────────────────────────
            if (lunarMonths.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            Text("🌙 Lunar:", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        items(lunarMonths) { lm ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BrahmGold.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp, BrahmGold.copy(alpha = 0.3f)
                                ),
                            ) {
                                Text(
                                    lm,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 11.sp,
                                    color = BrahmGold,
                                )
                            }
                        }
                    }
                }
            }

            // ── Adhik / Kshaya banners ────────────────────────────────────────
            if (adhikNote != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.08f))
                            .border(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("⭐", fontSize = 14.sp)
                        Text(
                            "Adhik Maas $year: $adhikNote",
                            fontSize = 12.sp,
                            color = Color(0xFFD97706),
                        )
                    }
                }
            }
            if (kshayaNote != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE11D48).copy(alpha = 0.08f))
                            .border(0.5.dp, Color(0xFFE11D48).copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("⚠️", fontSize = 14.sp)
                        Text(
                            "Kshaya Maas $year: $kshayaNote",
                            fontSize = 12.sp,
                            color = Color(0xFFBE123C),
                        )
                    }
                }
            }

            // ── Controls row ──────────────────────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Row 1: Month nav + year + Today button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Month nav
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { vm.prevMonth() }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    MONTH_NAMES[month - 1],
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.widthIn(min = 80.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                                IconButton(onClick = { vm.nextMonth() }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Year input
                        OutlinedTextField(
                            value = yearInput,
                            onValueChange = { yearInput = it },
                            modifier = Modifier.width(72.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                vm.setYear(yearInput.toIntOrNull() ?: year)
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrahmGold,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            ),
                        )

                        // Today button
                        OutlinedButton(
                            onClick = { vm.goToday() },
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                        ) {
                            Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Today", fontSize = 12.sp)
                        }

                        Spacer(Modifier.weight(1f))

                        // Grid/List toggle
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                IconButton(
                                    onClick = { viewMode = "grid" },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (viewMode == "grid") MaterialTheme.colorScheme.surfaceVariant else Color.Transparent),
                                ) {
                                    Icon(Icons.Default.GridView, contentDescription = "Grid", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { viewMode = "list" },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (viewMode == "list") MaterialTheme.colorScheme.surfaceVariant else Color.Transparent),
                                ) {
                                    Icon(Icons.Default.List, contentDescription = "List", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Row 2: City search + Tradition + Lunar system
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // City search
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = cityQuery,
                                onValueChange = { cityVm.cityQuery.value = it },
                                placeholder = {
                                    Text(cityName, fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.LocationOn, contentDescription = null,
                                        modifier = Modifier.size(16.dp), tint = BrahmGold)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrahmGold,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                ),
                            )
                            if (suggestions.isNotEmpty()) {
                                Popup(alignment = Alignment.TopStart) {
                                    Surface(
                                        modifier = Modifier.width(220.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        shadowElevation = 8.dp,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                    ) {
                                        Column {
                                            suggestions.forEach { city ->
                                                Text(
                                                    text = city.label.ifBlank { city.name },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            vm.setCity(city)
                                                            cityVm.cityQuery.value = ""
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Tradition dropdown
                        Box {
                            OutlinedButton(
                                onClick = { showTradition = true },
                                modifier = Modifier.height(56.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                ),
                            ) {
                                Text(
                                    TRADITIONS.find { it.first == tradition }?.second ?: "Smarta",
                                    fontSize = 11.sp,
                                )
                            }
                            DropdownMenu(expanded = showTradition, onDismissRequest = { showTradition = false }) {
                                TRADITIONS.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label, fontSize = 12.sp) },
                                        onClick = { vm.setTradition(key); showTradition = false },
                                    )
                                }
                            }
                        }

                        // Lunar system dropdown
                        Box {
                            OutlinedButton(
                                onClick = { showLunar = true },
                                modifier = Modifier.height(56.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                ),
                            ) {
                                Text(
                                    LUNAR_SYSTEMS.find { it.first == lunarSystem }?.second ?: "Amanta",
                                    fontSize = 11.sp,
                                )
                            }
                            DropdownMenu(expanded = showLunar, onDismissRequest = { showLunar = false }) {
                                LUNAR_SYSTEMS.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label, fontSize = 12.sp) },
                                        onClick = { vm.setLunarSystem(key); showLunar = false },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Stats row ─────────────────────────────────────────────────────
            item {
                val festCount   = days.count { it.bool("has_festival") }
                val purnimaDays = days.filter { it.bool("is_purnima") }.map { it.str("day") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CalStatChip("📅", TRADITIONS.find { it.first == tradition }?.second ?: "Smarta")
                    CalStatChip("🎉", "$festCount festivals")
                    if (purnimaDays.isNotEmpty()) CalStatChip("🌕", "Day ${purnimaDays.first()}")
                }
            }

            // ── Legend ────────────────────────────────────────────────────────
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { LegendChip(Color(0xFFF59E0B).copy(0.15f), "Festival") }
                    item { LegendChip(Color(0xFF3B82F6).copy(0.15f), "🌕 Purnima") }
                    item { LegendChip(Color(0xFF64748B).copy(0.15f), "🌑 Amavasya") }
                    item { LegendChip(Color(0xFF10B981).copy(0.15f), "✦ Ekadashi") }
                    item { LegendChip(Color(0xFFA855F7).copy(0.15f), "☽ Pradosh") }
                    item { LegendChip(Color(0xFFB71C1C).copy(0.15f), "🌑 Eclipse") }
                }
            }

            // ── Loading / Error ───────────────────────────────────────────────
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrahmGold)
                    }
                }
            } else if (error != null) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("⚠️ $error", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error)
                        Button(onClick = { vm.load() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrahmGold)) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
            } else if (days.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center) {
                        Text("No calendar data available", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            } else if (viewMode == "grid") {
                // ── Grid view ─────────────────────────────────────────────────
                item {
                    CalendarGridView(
                        days = days,
                        firstWeekday = firstWeekday,
                        eclipseMap = eclipseMap,
                        onDayClick = { d -> selectedDay = d; showSheet = true },
                    )
                }
            } else {
                // ── List view ─────────────────────────────────────────────────
                items(days) { day ->
                    val eclipse = eclipseMap[day.str("date")]
                    DayListRow(
                        day = day,
                        eclipse = eclipse,
                        onClick = { selectedDay = day; showSheet = true },
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // ── Day detail bottom sheet ───────────────────────────────────────────────
    if (showSheet && selectedDay != null) {
        DayDetailSheet(
            day = selectedDay!!,
            eclipse = eclipseMap[selectedDay!!.str("date")],
            onClose = { showSheet = false },
        )
    }
}

// ─── Calendar Grid ───────────────────────────────────────────────────────────

@Composable
private fun CalendarGridView(
    days: List<JsonObject>,
    firstWeekday: Int,
    eclipseMap: Map<String, JsonObject>,
    onDayClick: (JsonObject) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        // Week headers
        Row(modifier = Modifier.fillMaxWidth()) {
            WEEK_HEADERS.forEach { h ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(h, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        // Build 6-row grid
        val totalCells = 42
        val cells: List<JsonObject?> = buildList {
            repeat(firstWeekday) { add(null) }
            addAll(days)
            while (size < totalCells) add(null)
        }

        cells.chunked(7).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                row.forEach { day ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (day == null) {
                            Spacer(modifier = Modifier.fillMaxWidth().height(72.dp))
                        } else {
                            DayCell(
                                day = day,
                                eclipse = eclipseMap[day.str("date")],
                                onClick = { onDayClick(day) },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
        }
    }
}

// ─── Day Cell ────────────────────────────────────────────────────────────────

@Composable
private fun DayCell(
    day: JsonObject,
    eclipse: JsonObject?,
    onClick: () -> Unit,
) {
    val hasEclipse    = eclipse != null
    val isSolarEcl    = eclipse?.str("type")?.contains("Solar") == true
    val isToday       = day.bool("is_today")
    val icon          = dayIcon(day, hasEclipse, isSolarEcl)
    val bgColor       = dayBgColor(day, hasEclipse)
    val borderColor   = dayBorderColor(day, hasEclipse)
    val dayNum        = day.str("day")
    val paksha        = day.str("paksha_short")
    val tithiNum      = day.str("tithi_num")
    val tithiName     = day.str("tithi")
    val festivals     = day.arr("festivals") ?: JsonArray(emptyList())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(
                width = if (isToday) 2.dp else 0.5.dp,
                color = if (isToday) BrahmGold else borderColor,
                shape = RoundedCornerShape(6.dp),
            )
            .clickable { onClick() }
            .padding(3.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    dayNum,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isToday -> BrahmGold
                        hasEclipse -> Color(0xFFF87171)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
                if (icon != null) {
                    Text(icon, fontSize = 9.sp)
                }
            }
            Text(
                "$paksha$tithiNum ${shortTithi(tithiName)}",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (hasEclipse) {
                Text(
                    if (isSolarEcl) "Surya Grahan" else "Chandra Grahan",
                    fontSize = 8.sp,
                    color = Color(0xFFF87171),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val showCount = if (hasEclipse) 1 else 2
            festivals.take(showCount).forEach { el ->
                val f = el.jsonObject
                Text(
                    "${f.str("icon")} ${f.str("name").split(" ").take(2).joinToString(" ")}",
                    fontSize = 8.sp,
                    color = Color(0xFFD97706),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (festivals.size > showCount) {
                Text(
                    "+${festivals.size - showCount} more",
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }
    }
}

// ─── Day List Row ─────────────────────────────────────────────────────────────

@Composable
private fun DayListRow(
    day: JsonObject,
    eclipse: JsonObject?,
    onClick: () -> Unit,
) {
    val hasEclipse = eclipse != null
    val isSolarEcl = eclipse?.str("type")?.contains("Solar") == true
    val isToday    = day.bool("is_today")
    val festivals  = day.arr("festivals") ?: JsonArray(emptyList())

    val bgColor = when {
        isToday    -> BrahmGold.copy(alpha = 0.08f)
        hasEclipse -> Color(0xFFB71C1C).copy(alpha = 0.12f)
        else       -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    }
    val borderColor = when {
        isToday    -> BrahmGold.copy(alpha = 0.4f)
        hasEclipse -> Color(0xFFEF4444).copy(alpha = 0.3f)
        else       -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Date block
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp),
        ) {
            Text(
                day.str("day"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isToday -> BrahmGold
                    hasEclipse -> Color(0xFFF87171)
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                day.str("weekday").take(3),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }

        VerticalDivider(
            modifier = Modifier.height(48.dp).width(1.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${day.str("paksha_short")}${day.str("tithi_num")} · ${day.str("tithi")}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                )
                if (hasEclipse) Text(
                    "🌑 ${if (isSolarEcl) "Solar" else "Lunar"} Eclipse",
                    fontSize = 11.sp, color = Color(0xFFF87171), fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                buildString {
                    append("🌟 ${day.str("nakshatra")}  ☀ ${day.str("sunrise")}")
                    val rahu = day.str("rahu_kaal")
                    if (rahu != "—") append("  ☢ $rahu")
                },
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            if (festivals.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(minOf(festivals.size, 3)) { i ->
                        val f = festivals[i].jsonObject
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.10f),
                        ) {
                            Text(
                                "${f.str("icon")} ${f.str("name")}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = Color(0xFFD97706),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (festivals.size > 3) {
                        item {
                            Text(
                                "+${festivals.size - 3}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Day Detail Bottom Sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailSheet(
    day: JsonObject,
    eclipse: JsonObject?,
    onClose: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null,
                        tint = BrahmGold, modifier = Modifier.size(20.dp))
                    Column {
                        Text(
                            day.str("date"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BrahmGold,
                        )
                        Text(
                            "${day.str("vara")} · ${day.str("weekday")}" +
                                day.str("lunar_month").let { if (it != "—") " · 🌙 $it" else "" },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            }

            // Special badges
            item {
                val badges = buildList {
                    if (eclipse != null) add("🌑 ${eclipse.str("type")} Eclipse" to Color(0xFFEF4444))
                    if (day.bool("is_purnima"))   add("🌕 Purnima"   to Color(0xFF3B82F6))
                    if (day.bool("is_amavasya"))  add("🌑 Amavasya"  to Color(0xFF64748B))
                    if (day.bool("is_ekadashi"))  add("✦ Ekadashi"  to Color(0xFF10B981))
                    if (day.bool("is_pradosh"))   add("☽ Pradosh"   to Color(0xFFA855F7))
                    if (day.bool("is_chaturthi")) add("● Chaturthi" to Color(0xFFF97316))
                    if (day.bool("is_ashtami"))   add("◉ Ashtami"   to Color(0xFFE11D48))
                }
                if (badges.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(badges) { (label, color) ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = color.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(0.3f)),
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    color = color,
                                )
                            }
                        }
                    }
                }
            }

            // Panchang grid
            item {
                BrahmCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Text("PANCHANG",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.height(6.dp))
                        SheetInfoRow("Vara / Day", day.str("vara"))
                        SheetInfoRow("Tithi",
                            "${day.str("paksha")} ${day.str("tithi")} (${day.str("paksha_short")}${day.str("tithi_num")})")
                        SheetInfoRow("Nakshatra", day.str("nakshatra"))
                        SheetInfoRow("Yoga", day.str("yoga"))
                        SheetInfoRow("Lunar Month", day.str("lunar_month"))
                        SheetInfoRow("🌅 Sunrise", day.str("sunrise"), highlight = false)
                        SheetInfoRow("🌇 Sunset", day.str("sunset"), highlight = false)
                        SheetInfoRow("☢ Rahu Kaal", day.str("rahu_kaal"), highlight = true)
                    }
                }
            }

            // Eclipse section
            if (eclipse != null) {
                item {
                    Text("GRAHAN · ECLIPSE",
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = Color(0xFFEF4444).copy(alpha = 0.8f),
                    )
                }
                item { SheetEclipseCard(eclipse) }
            }

            // Festivals
            val festivals = day.arr("festivals") ?: JsonArray(emptyList())
            if (festivals.isNotEmpty()) {
                item {
                    Text("FESTIVALS & OBSERVANCES",
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
                items(festivals.size) { i ->
                    SheetFestivalCard(festivals[i].jsonObject)
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No festivals today", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

// ─── Sheet helpers ────────────────────────────────────────────────────────────

@Composable
private fun SheetInfoRow(label: String, value: String, highlight: Boolean = false) {
    if (value == "—" || value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .border(
                width = Dp.Hairline,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                shape = RoundedCornerShape(0.dp)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.widthIn(max = 120.dp))
        Text(value, fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (highlight) Color(0xFFF97316) else MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f, fill = false))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
}

@Composable
private fun SheetEclipseCard(eclipse: JsonObject) {
    val isSolar   = eclipse.str("type").contains("Solar")
    val hasSutak  = eclipse.int("sutak_hours") > 0
    val conflicts = eclipse.arr("festival_conflict") ?: JsonArray(emptyList())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, Color(0xFFEF4444).copy(0.4f), RoundedCornerShape(12.dp))
            .background(Color(0xFFB71C1C).copy(0.10f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(if (isSolar) "☀️🌑" else "🌕🌑", fontSize = 22.sp)
            Column {
                Text("${eclipse.str("type")} Eclipse",
                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = Color(0xFFF87171))
                Text("${if (isSolar) "Surya Grahan" else "Chandra Grahan"} · ${eclipse.str("nakshatra")} (${eclipse.str("rashi")})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }

        // Sparsha / Madhya / Moksha
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Triple("Sparsha", "First contact", eclipse.str("sparsha")),
                Triple("Madhya",  "Maximum",       eclipse.str("madhya")),
                Triple("Moksha",  "Last contact",  eclipse.str("moksha")),
            ).forEach { (label, sub, time) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.20f))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(time, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrahmGold)
                    Text(label, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    Text(sub, fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }

        // Duration
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.10f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⏱", fontSize = 13.sp)
            Text("Duration: ", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text("${eclipse.int("duration_minutes")} min",
                fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
        }

        // Sutak
        if (hasSutak) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFB71C1C).copy(0.08f))
                    .border(0.5.dp, Color(0xFFEF4444).copy(0.2f), RoundedCornerShape(6.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("⏳ Sutak Kaal (${eclipse.int("sutak_hours")}h before Sparsha)",
                    fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    color = Color(0xFFF87171))
                Text(
                    "${eclipse.str("sutak_start")} → ${eclipse.str("sparsha")}",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFFF87171),
                )
                Text("Avoid cooking, eating, major activities. Temples closed.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            Text("ℹ️ No Sutak — penumbral eclipse not observed with Sutak in most traditions.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.10f))
                    .padding(10.dp))
        }

        // Festival conflicts
        if (conflicts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🎪 Nearby Festival Impact",
                    fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    color = Color(0xFFD97706))
                conflicts.forEach { el ->
                    Text(
                        el.jsonPrimitive.content,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF59E0B).copy(0.08f))
                            .border(0.5.dp, Color(0xFFF59E0B).copy(0.2f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        fontSize = 11.sp,
                        color = Color(0xFFD97706),
                    )
                }
            }
        }

        // Spiritual effect
        val spiritualEffect = eclipse.str("spiritual_effect")
        if (spiritualEffect != "—") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(BrahmGold.copy(0.06f))
                    .border(0.5.dp, BrahmGold.copy(0.15f), RoundedCornerShape(6.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text("🔮 Spiritual Significance",
                    fontSize = 11.sp, color = BrahmGold)
                Text(spiritualEffect, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun SheetFestivalCard(f: JsonObject) {
    var expanded by remember { mutableStateOf(false) }
    val doshNotes  = f.arr("dosh_notes") ?: JsonArray(emptyList())
    val grahanNotes = doshNotes.filter { el ->
        val s = el.jsonPrimitive.content.lowercase()
        s.contains("grahan") || s.contains("eclipse")
    }
    val otherNotes = doshNotes.filter { el ->
        val s = el.jsonPrimitive.content.lowercase()
        !s.contains("grahan") && !s.contains("eclipse")
    }
    val hasGrahan = grahanNotes.isNotEmpty()

    val cardBg     = if (hasGrahan) Color(0xFFB71C1C).copy(0.10f) else Color(0xFFF59E0B).copy(0.06f)
    val borderClr  = if (hasGrahan) Color(0xFFEF4444).copy(0.25f) else Color(0xFFF59E0B).copy(0.20f)
    val nameClr    = if (hasGrahan) Color(0xFFF87171) else Color(0xFFD97706)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(cardBg)
            .border(0.5.dp, borderClr, RoundedCornerShape(10.dp))
            .clickable { expanded = !expanded }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(f.str("icon"), fontSize = 18.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(f.str("name"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = nameClr)
                val hindi = f.str("hindi")
                if (hindi != "—") Text(hindi, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f),
                ) {
                    Text(f.str("tithi_name"),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp)
                }
                if (hasGrahan) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFEF4444).copy(0.10f),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp, Color(0xFFEF4444).copy(0.3f)
                        ),
                    ) {
                        Text("🌑 Grahan",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp, color = Color(0xFFF87171))
                    }
                }
            }
        }

        // Tithi timing
        val tithiStart = f.str("tithi_start"); val tithiEnd = f.str("tithi_end")
        if (tithiStart != "—" && tithiEnd != "—") {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("⏱", fontSize = 12.sp)
                Text("Tithi: ", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text("$tithiStart – $tithiEnd",
                    fontWeight = FontWeight.Medium, fontSize = 11.sp)
            }
        }

        // Grahan notes — always visible
        if (hasGrahan) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("🌑 Eclipse Conflict & Shift Reason",
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF87171))
                grahanNotes.forEach { el ->
                    Text(
                        el.jsonPrimitive.content,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFB71C1C).copy(0.08f))
                            .border(0.5.dp, Color(0xFFEF4444).copy(0.15f), RoundedCornerShape(4.dp))
                            .padding(6.dp),
                        fontSize = 11.sp, color = Color(0xFFF87171),
                    )
                }
            }
        }

        // Expandable section
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val significance = f.str("significance")
                if (significance != "—") {
                    Text(significance, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 16.sp)
                }
                otherNotes.forEach { el ->
                    Text(
                        el.jsonPrimitive.content,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF97316).copy(0.06f))
                            .border(0.5.dp, Color(0xFFF97316).copy(0.15f), RoundedCornerShape(4.dp))
                            .padding(6.dp),
                        fontSize = 11.sp,
                        color = Color(0xFFD97706),
                    )
                }
                val fastNote = f.str("fast_note")
                if (fastNote != "—") {
                    Text(
                        "🙏 $fastNote",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(BrahmGold.copy(0.05f))
                            .border(0.5.dp, BrahmGold.copy(0.12f), RoundedCornerShape(4.dp))
                            .padding(6.dp),
                        fontSize = 11.sp,
                        color = BrahmGold,
                    )
                }
                // Tags
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    val deity = f.str("deity"); val paksha = f.str("paksha")
                    val tithiType = f.str("tithi_type")
                    if (deity != "—") item {
                        TagChip("⚛ $deity", MaterialTheme.colorScheme.surfaceVariant)
                    }
                    if (paksha != "—") item {
                        TagChip("$paksha paksha", MaterialTheme.colorScheme.surfaceVariant)
                    }
                    if (tithiType != "—" && tithiType != "normal") item {
                        TagChip(tithiType, Color(0xFFF59E0B).copy(0.08f))
                    }
                }
            }
        }

        // Expand hint
        Text(
            if (expanded) "▲ Less" else "▼ More",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}

// ─── Small helpers ─────────────────────────────────────────────────────────────

@Composable
private fun CalStatChip(icon: String, label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, MaterialTheme.colorScheme.outline.copy(0.2f)
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(icon, fontSize = 12.sp)
            Text(label, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun TagChip(label: String, bg: Color) {
    Surface(shape = RoundedCornerShape(4.dp), color = bg) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

private val Dp.Companion.Hairline get() = 0.5.dp
