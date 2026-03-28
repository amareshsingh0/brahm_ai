package com.bimoraai.brahm.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.BrahmCard
import com.bimoraai.brahm.core.components.ScrollToTopFab
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

@Composable
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

@Composable
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
    hasEclipse           -> if (isSolarEclipse) "☉●" else "○●"
    day.bool("is_purnima")  -> "○"
    day.bool("is_amavasya") -> "●"
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
    asTab: Boolean = false,
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
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

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

    val content: @Composable (PaddingValues) -> Unit = { padding ->
        val listState = rememberLazyListState()
        Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 10.dp),
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
                            Text("☽ Lunar:", fontSize = 11.sp,
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
                val btnShape  = RoundedCornerShape(8.dp)
                val btnBorder = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
                )
                val btnHeight = 40.dp
                val onSurface = MaterialTheme.colorScheme.onSurface

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    // Row 1: [< Month >] [Year] [Today] ··· [grid|list]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Month nav — compact, takes only what it needs
                        Surface(shape = btnShape, color = MaterialTheme.colorScheme.surface, border = btnBorder) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(btnHeight)) {
                                Box(
                                    modifier = Modifier.size(btnHeight).clickable { vm.prevMonth() },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(18.dp), tint = onSurface)
                                }
                                Text(
                                    MONTH_NAMES[month - 1].take(3), // "Mar" not "March" — saves space
                                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                    color = onSurface,
                                    modifier = Modifier.width(32.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                                Box(
                                    modifier = Modifier.size(btnHeight).clickable { vm.nextMonth() },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = onSurface)
                                }
                            }
                        }

                        // Year input — compact, same height as Month/Today buttons
                        OutlinedTextField(
                            value = yearInput,
                            onValueChange = { yearInput = it },
                            modifier = Modifier.width(68.dp).height(btnHeight + 16.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = onSurface,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { vm.setYear(yearInput.toIntOrNull() ?: year); keyboardController?.hide() }),
                            shape = btnShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrahmGold,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.30f),
                            ),
                        )

                        // Today button
                        Surface(
                            onClick = { vm.goToday() }, shape = btnShape,
                            color = MaterialTheme.colorScheme.surface, border = btnBorder,
                            modifier = Modifier.height(btnHeight),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
                                Icon(Icons.Default.Today, null, modifier = Modifier.size(14.dp), tint = BrahmGold)
                                Spacer(Modifier.width(4.dp))
                                Text("Today", fontSize = 12.sp, color = onSurface)
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Grid / List toggle
                        Surface(shape = btnShape, color = MaterialTheme.colorScheme.surface, border = btnBorder,
                            modifier = Modifier.height(btnHeight)) {
                            Row(modifier = Modifier.padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                listOf("grid" to Icons.Default.GridView, "list" to Icons.Default.List).forEach { (mode, icon) ->
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (viewMode == mode) BrahmGold.copy(alpha = 0.12f) else Color.Transparent)
                                            .clickable { viewMode = mode },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(icon, null, modifier = Modifier.size(16.dp),
                                            tint = if (viewMode == mode) BrahmGold else onSurface.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }

                    // Row 2: [Smarta ▾] [Amanta ▾]  (city search is in the TopAppBar location icon)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // Tradition dropdown
                        Box {
                            Surface(onClick = { showTradition = true }, shape = btnShape, color = MaterialTheme.colorScheme.surface, border = btnBorder, modifier = Modifier.height(btnHeight)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
                                    Text(TRADITIONS.find { it.first == tradition }?.second ?: "Smarta", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = onSurface)
                                }
                            }
                            DropdownMenu(expanded = showTradition, onDismissRequest = { showTradition = false }) {
                                TRADITIONS.forEach { (key, label) ->
                                    DropdownMenuItem(text = { Text(label, fontSize = 13.sp) }, onClick = { vm.setTradition(key); showTradition = false })
                                }
                            }
                        }

                        // Lunar system dropdown
                        Box {
                            Surface(onClick = { showLunar = true }, shape = btnShape, color = MaterialTheme.colorScheme.surface, border = btnBorder, modifier = Modifier.height(btnHeight)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
                                    Text(LUNAR_SYSTEMS.find { it.first == lunarSystem }?.second ?: "Amanta", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = onSurface)
                                }
                            }
                            DropdownMenu(expanded = showLunar, onDismissRequest = { showLunar = false }) {
                                LUNAR_SYSTEMS.forEach { (key, label) ->
                                    DropdownMenuItem(text = { Text(label, fontSize = 13.sp) }, onClick = { vm.setLunarSystem(key); showLunar = false })
                                }
                            }
                        }
                    }
                }
            }

            // ── Stats + Legend combined row ───────────────────────────────────
            item {
                val festCount   = days.count { it.bool("has_festival") }
                val purnimaDays = days.filter { it.bool("is_purnima") }.map { it.str("day") }
                val traditionLabel = TRADITIONS.find { it.first == tradition }?.second ?: "Smarta"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Stats line
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("📅 $traditionLabel", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        Text("🎉 $festCount festivals", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        if (purnimaDays.isNotEmpty())
                            Text("○ Day ${purnimaDays.first()}", fontSize = 11.sp, color = Color(0xFF3B82F6))
                    }
                    // Legend line
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        LegendChip(Color(0xFFF59E0B), "Festival")
                        LegendChip(Color(0xFF3B82F6), "Purnima")
                        LegendChip(Color(0xFF64748B), "Amavasya")
                        LegendChip(Color(0xFF10B981), "Ekadashi")
                        LegendChip(Color(0xFFEF4444), "Eclipse")
                    }
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
                        year = year,
                        month = month,
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
        ScrollToTopFab(listState, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp))
        } // Box
    }

    // Render with or without Scaffold depending on context
    if (asTab) {
        content(PaddingValues(0.dp))
    } else {
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
        ) { padding -> content(padding) }
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
    year: Int,
    month: Int,
    eclipseMap: Map<String, JsonObject>,
    onDayClick: (JsonObject) -> Unit,
) {
    // Days in the previous month
    val daysInPrevMonth = java.util.Calendar.getInstance().apply {
        set(year, month - 2, 1)  // month is 1-based; Calendar.MONTH is 0-based → month-2 = previous
    }.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        // Week headers
        Row(modifier = Modifier.fillMaxWidth()) {
            WEEK_HEADERS.forEach { h ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(h, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 2.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
        )

        // Build grid: null = ghost day (Int box stored separately), real day = JsonObject
        // Use a sealed-like approach: cells contain JsonObject? but ghost cells tracked by index
        val totalCells = 42
        val trailingCount = maxOf(0, totalCells - firstWeekday - days.size)

        // Leading ghost day numbers (prev month's end days)
        val leadingGhosts = List(firstWeekday) { i -> daysInPrevMonth - firstWeekday + 1 + i }
        // Trailing ghost day numbers (next month's start days)
        val trailingGhosts = List(trailingCount) { i -> i + 1 }

        val cells: List<Any?> = buildList {
            leadingGhosts.forEach { add(-it) }  // negative = ghost (leading)
            addAll(days)
            trailingGhosts.forEach { add(-(1000 + it)) }  // negative < -100 = ghost (trailing)
        }

        cells.chunked(7).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                row.forEach { cell ->
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            cell is JsonObject -> DayCell(
                                day = cell,
                                eclipse = eclipseMap[cell.str("date")],
                                onClick = { onDayClick(cell) },
                            )
                            cell is Int && cell < 0 -> {
                                // Ghost day cell
                                val ghostNum = if (cell > -1000) -cell else -(cell + 1000)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(84.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.15f))
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.08f), RoundedCornerShape(7.dp))
                                        .padding(5.dp),
                                    contentAlignment = Alignment.TopStart,
                                ) {
                                    Text(
                                        text = ghostNum.toString(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                    )
                                }
                            }
                            else -> Spacer(modifier = Modifier.fillMaxWidth().height(84.dp))
                        }
                    }
                }
            }
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(bgColor)
            .border(
                width = if (isToday) 2.dp else 0.5.dp,
                color = if (isToday) BrahmGold else borderColor,
                shape = RoundedCornerShape(7.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        // Day number row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                dayNum,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isToday -> BrahmGold
                    hasEclipse -> Color(0xFFF87171)
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            if (icon != null) {
                Text(icon, fontSize = 10.sp, lineHeight = 12.sp)
            }
        }
        // Tithi
        Text(
            "$paksha$tithiNum · ${shortTithi(tithiName)}",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 11.sp,
        )
        if (hasEclipse) {
            Text(
                if (isSolarEcl) "☉ Surya" else "○ Chandra",
                fontSize = 9.sp,
                color = Color(0xFFF87171),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 11.sp,
            )
        }
        val showCount = if (hasEclipse) 1 else 2
        festivals.take(showCount).forEach { el ->
            val f = el.jsonObject
            Text(
                "${f.str("icon")} ${f.str("name").split(" ").first()}",
                fontSize = 9.sp,
                color = Color(0xFFD97706),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 11.sp,
            )
        }
        if (festivals.size > showCount) {
            Text(
                "+${festivals.size - showCount}",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                lineHeight = 11.sp,
            )
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
                    "● ${if (isSolarEcl) "Solar" else "Lunar"} Eclipse",
                    fontSize = 11.sp, color = Color(0xFFF87171), fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                buildString {
                    append("★ ${day.str("nakshatra")}  ☉ ${day.str("sunrise")}")
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
        // Column + verticalScroll avoids the LazyColumn infinity-height crash inside ModalBottomSheet
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
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
                            day.str("lunar_month").let { if (it != "—") " · ☽ $it" else "" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }

            // Special badges
            val badges = buildList {
                if (eclipse != null) add("● ${eclipse.str("type")} Eclipse" to Color(0xFFEF4444))
                if (day.bool("is_purnima"))   add("○ Purnima"   to Color(0xFF3B82F6))
                if (day.bool("is_amavasya"))  add("● Amavasya"  to Color(0xFF64748B))
                if (day.bool("is_ekadashi"))  add("✦ Ekadashi"  to Color(0xFF10B981))
                if (day.bool("is_pradosh"))   add("☽ Pradosh"   to Color(0xFFA855F7))
                if (day.bool("is_chaturthi")) add("● Chaturthi" to Color(0xFFF97316))
                if (day.bool("is_ashtami"))   add("◉ Ashtami"   to Color(0xFFE11D48))
            }
            if (badges.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    badges.forEach { (label, color) ->
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

            // Panchang grid
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
                    SheetInfoRow("↑ Sunrise", day.str("sunrise"), highlight = false)
                    SheetInfoRow("🌇 Sunset", day.str("sunset"), highlight = false)
                    SheetInfoRow("☢ Rahu Kaal", day.str("rahu_kaal"), highlight = true)
                }
            }

            // Eclipse section
            if (eclipse != null) {
                Text("GRAHAN · ECLIPSE",
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = Color(0xFFEF4444).copy(alpha = 0.8f),
                )
                SheetEclipseCard(eclipse)
            }

            // Festivals
            val festivals = day.arr("festivals") ?: JsonArray(emptyList())
            if (festivals.isNotEmpty()) {
                Text("FESTIVALS & OBSERVANCES",
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                festivals.forEach { item ->
                    SheetFestivalCard(item.jsonObject)
                }
            } else {
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

// ─── Sheet helpers ────────────────────────────────────────────────────────────

@Composable
private fun SheetInfoRow(label: String, value: String, highlight: Boolean = false) {
    if (value == "—" || value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .padding(bottom = 0.dp),
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
            .border(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF5F5))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(if (isSolar) "☉●" else "○●", fontSize = 22.sp)
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
                        .background(Color(0xFFB71C1C).copy(alpha = 0.08f))
                        .border(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(time, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrahmGold)
                    Text(label, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(sub, fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                }
            }
        }

        // Duration
        val durationMin = eclipse.int("duration_minutes")
        if (durationMin > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFB71C1C).copy(alpha = 0.06f))
                    .border(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⏱", fontSize = 13.sp)
                Text("Duration: ", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text("$durationMin min", fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface)
            }
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
                        Text("● Grahan",
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
                Text("● Eclipse Conflict & Shift Reason",
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
                    if (paksha != "—" && paksha != "N/A") item {
                        TagChip("$paksha Paksha", MaterialTheme.colorScheme.surfaceVariant)
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color.copy(alpha = 0.7f))
        )
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
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

