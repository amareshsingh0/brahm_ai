package com.bimoraai.brahm.ui.panchang

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.components.brahmFieldColors
import com.bimoraai.brahm.core.data.CitySearchViewModel
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.util.Locale

// ─── Sutak rules for Eclipses tab ─────────────────────────────────────────────

private data class SutakRule(val title: String, val body: String, val items: List<String>)
private val SUTAK_RULES = listOf(
    SutakRule(
        "Sutak Kaal (सूतक काल)",
        "Sutak is the period of ritual impurity before a Grahan. During Sutak, cooked food becomes impure, temples are closed, and major auspicious activities are avoided.",
        listOf(
            "Solar eclipse (Surya Grahan): Sutak starts 12 hours before first contact",
            "Lunar eclipse (Chandra Grahan): Sutak starts 9 hours before first contact",
            "Penumbral lunar eclipse: No Sutak per most traditions",
            "Children, elderly, sick, and pregnant women are exempt",
        )
    ),
    SutakRule(
        "During Grahan",
        "From Sparsha (first contact) to Moksha (last contact) — the main eclipse window.",
        listOf(
            "Avoid eating or drinking (fast during eclipse)",
            "Chant mantras, do japa — amplified effect during eclipse",
            "Add Tulsi/Kusha grass to stored water and food",
            "Avoid looking at Sun during Solar Grahan",
        )
    ),
    SutakRule(
        "After Moksha",
        "Once the last contact (Moksha) is complete, purification begins.",
        listOf(
            "Take a bath immediately after Moksha",
            "Sprinkle Ganga jal in the house",
            "Recook or replace food prepared during Sutak",
            "Charity (Daan) yields 10× merit after eclipse",
        )
    ),
)

private val ECLIPSE_GUIDANCE = mapOf(
    "Total Solar"   to listOf("Chant Surya mantra 108 times", "Fast during eclipse", "Donate wheat and copper", "Perform Surya Tarpan after Moksha"),
    "Annular Solar" to listOf("Recite Aditya Hridaya Stotra", "Avoid new ventures", "Bathe before and after", "Donate to charity"),
    "Partial Solar" to listOf("Observe silence", "Chant Gayatri Mantra", "Avoid cooking", "Sprinkle Ganga jal"),
    "Total Lunar"   to listOf("Chant Vishnu Sahasranama", "Meditate near water", "Avoid eating during Sutak", "Take bath after eclipse"),
    "Partial Lunar" to listOf("Reflect and introspect", "Light a ghee lamp", "Recite mantras"),
    "Penumbral"     to listOf("Heightened meditation time", "Avoid major decisions", "Chant mantras"),
)

// ─── JSON helpers ─────────────────────────────────────────────────────────────

private fun JsonObject.str(key: String): String =
    this[key]?.jsonPrimitive?.contentOrNull ?: "—"

private fun JsonObject.bool(key: String): Boolean =
    this[key]?.jsonPrimitive?.booleanOrNull == true

private fun JsonObject.int(key: String): Int =
    this[key]?.jsonPrimitive?.intOrNull ?: 0

private fun JsonObject.arr(key: String) = this[key]?.jsonArray

// ─── Time helpers ──────────────────────────────────────────────────────────────

private fun fmt12(t: String?): String {
    if (t == null || t == "N/A" || t == "—") return t ?: "—"
    val parts = t.split(":"); if (parts.size < 2) return t
    val h = parts[0].toIntOrNull() ?: return t
    val m = parts[1].toIntOrNull() ?: return t
    val ampm = if (h >= 12) "PM" else "AM"
    return "${h % 12 + if (h % 12 == 0) 12 else 0}:${m.toString().padStart(2, '0')} $ampm"
}

private fun festDateRange(date: String, dateEnd: String?): String {
    fun fmtDate(iso: String): String {
        val (y, m, d) = iso.split("-").map { it.toInt() }
        return java.time.LocalDate.of(y, m, d)
            .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
    }
    if (dateEnd == null || dateEnd == date) return fmtDate(date)
    val days = kotlin.runCatching {
        val (y1,m1,d1) = date.split("-").map{it.toInt()}
        val (y2,m2,d2) = dateEnd.split("-").map{it.toInt()}
        val s = java.time.LocalDate.of(y1,m1,d1)
        val e = java.time.LocalDate.of(y2,m2,d2)
        java.time.temporal.ChronoUnit.DAYS.between(s,e)
    }.getOrDefault(0L)
    if (days <= 1L) return fmtDate(date)
    val (y2,m2,d2) = dateEnd.split("-").map{it.toInt()}
    val eStr = java.time.LocalDate.of(y2,m2,d2)
        .format(java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
    return "${fmtDate(date)} – $eStr"
}

private fun tithiWindow(fest: JsonObject): String? {
    val ts = fest.str("tithi_start"); val te = fest.str("tithi_end")
    if (ts == "—" || ts == "N/A") return null
    if (ts == "00:00" && te == "23:59") return "All day"
    return "${fmt12(ts)} – ${fmt12(te)}"
}

private fun shortDate(iso: String): String {
    val (y, m, d) = iso.split("-").map { it.toInt() }
    return java.time.LocalDate.of(y, m, d)
        .format(java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
}

private fun isPastDate(iso: String): Boolean = try {
    val (y, m, d) = iso.split("-").map { it.toInt() }
    java.time.LocalDate.of(y, m, d).isBefore(LocalDate.now())
} catch (_: Exception) { false }

private fun toMonthKey(iso: String) = iso.take(7)   // "YYYY-MM"

private fun monthKeyLabel(key: String): String {
    val (y, m) = key.split("-").map { it.toInt() }
    return java.time.LocalDate.of(y, m, 1)
        .format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
}

private fun monthKeyShort(key: String): String {
    val (y, m) = key.split("-").map { it.toInt() }
    return java.time.LocalDate.of(y, m, 1)
        .format(java.time.format.DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH))
}

private data class TimelineItem(val kind: String, val date: String, val data: JsonObject)

// ─── Main Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanchangScreen(
    navController: NavController,
    vm: PanchangViewModel   = hiltViewModel(),
    cityVm: CitySearchViewModel = hiltViewModel(),
) {
    val festivals  by vm.festivals.collectAsState()
    val grahan     by vm.grahan.collectAsState()
    val isLoading  by vm.isLoading.collectAsState()
    val error      by vm.error.collectAsState()
    val cityName   by vm.cityName.collectAsState()
    val suggestions by cityVm.suggestions.collectAsState()
    val cityQuery   by cityVm.cityQuery.collectAsState()

    val year = LocalDate.now().year

    // Build unified sorted allItems list: festivals + eclipses
    val allItems: List<TimelineItem> = remember(festivals, grahan) {
        val list = mutableListOf<TimelineItem>()
        festivals?.arr("festivals")?.forEach { el ->
            val f = el.jsonObject
            list += TimelineItem("festival", f.str("date"), f)
        }
        grahan?.arr("eclipses")?.forEach { el ->
            val e = el.jsonObject
            list += TimelineItem("eclipse", e.str("date"), e)
        }
        list.sortedBy { it.date }
    }

    // Group by month
    val monthGroups: List<Pair<String, List<TimelineItem>>> = remember(allItems) {
        val map = linkedMapOf<String, MutableList<TimelineItem>>()
        allItems.forEach { item ->
            val key = toMonthKey(item.date)
            map.getOrPut(key) { mutableListOf() } += item
        }
        map.entries.map { it.key to it.value }
    }

    val curMonthKey = remember { toMonthKey(LocalDate.now().toString()) }
    val upcomingCount = remember(allItems) { allItems.count { !isPastDate(it.date) } }
    val pastCount = allItems.size - upcomingCount
    val conflictCount = remember(grahan) {
        grahan?.arr("eclipses")?.count { el ->
            (el.jsonObject.arr("festival_conflict")?.size ?: 0) > 0
        } ?: 0
    }

    var selectedTab   by remember { mutableIntStateOf(0) }
    var searchQuery   by remember { mutableStateOf("") }

    SwipeBackLayout(navController) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("🗓️ Panchang", fontWeight = FontWeight.Bold, color = BrahmGold)
                            Text("$year · ${cityName ?: "Ujjain"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // City search via AlertDialog (keeps keyboard visible)
                        var showCitySearch by remember { mutableStateOf(false) }
                        IconButton(onClick = { showCitySearch = true }) {
                            Icon(Icons.Default.LocationOn, contentDescription = "City", tint = BrahmGold)
                        }
                        if (showCitySearch) {
                            AlertDialog(
                                onDismissRequest = { showCitySearch = false; cityVm.cityQuery.value = "" },
                                containerColor = BrahmCard,
                                title = { Text("Change Location", fontWeight = FontWeight.SemiBold, color = BrahmForeground) },
                                text = {
                                    Column {
                                        OutlinedTextField(
                                            value = cityQuery,
                                            onValueChange = { cityVm.cityQuery.value = it },
                                            placeholder = { Text(cityName ?: "Search city…", fontSize = 13.sp) },
                                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            colors = brahmFieldColors(),
                                        )
                                        if (suggestions.isEmpty() && cityQuery.isBlank()) {
                                            Text(
                                                "Type a city name to search…",
                                                fontSize = 12.sp,
                                                color = BrahmMutedForeground,
                                                modifier = Modifier.padding(top = 8.dp),
                                            )
                                        }
                                        suggestions.forEach { city ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(city.label.ifBlank { city.name },
                                                        fontSize = 13.sp, maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                                },
                                                onClick = {
                                                    vm.loadForCity(city.lat, city.lon, city.tz, city.name)
                                                    cityVm.cityQuery.value = ""
                                                    showCitySearch = false
                                                },
                                            )
                                        }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = {
                                    TextButton(onClick = { showCitySearch = false; cityVm.cityQuery.value = "" }) {
                                        Text("Cancel", color = BrahmGold)
                                    }
                                },
                            )
                        }
                        IconButton(onClick = { vm.load() }, enabled = !isLoading) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = BrahmGold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BrahmBackground),
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(BrahmBackground),
            ) {
                // Tab row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = BrahmBackground,
                    contentColor = BrahmGold,
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("📅 Calendar", modifier = Modifier.padding(vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (selectedTab == 0) BrahmGold else BrahmMutedForeground,
                                fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                            ))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("🎉 Festivals", modifier = Modifier.padding(vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (selectedTab == 1) BrahmGold else BrahmMutedForeground,
                                fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                            ))
                    }
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(vertical = 10.dp)) {
                            Text("● Eclipses", style = MaterialTheme.typography.labelMedium.copy(
                                color = if (selectedTab == 2) BrahmGold else BrahmMutedForeground,
                                fontWeight = if (selectedTab == 2) FontWeight.SemiBold else FontWeight.Normal,
                            ))
                            if (conflictCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF59E0B).copy(0.15f),
                                ) {
                                    Text("$conflictCount",
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                        fontSize = 10.sp, color = Color(0xFFD97706))
                                }
                            }
                        }
                    }
                }

                when (selectedTab) {
                    0 -> com.bimoraai.brahm.ui.calendar.CalendarScreen(
                        navController = navController,
                        asTab = true,
                    )
                    1 -> FestivalsTab(
                        allItems = allItems,
                        monthGroups = monthGroups,
                        curMonthKey = curMonthKey,
                        upcomingCount = upcomingCount,
                        pastCount = pastCount,
                        eclipseCount = grahan?.arr("eclipses")?.size ?: 0,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        isLoading = isLoading,
                        error = error,
                        onRetry = { vm.load() },
                    )
                    2 -> EclipsesTab(
                        grahan = grahan,
                        isLoading = isLoading,
                        error = error,
                        onRetry = { vm.load() },
                    )
                }
            }
        }
    }
}

// ─── Festivals Tab ────────────────────────────────────────────────────────────

@Composable
private fun FestivalsTab(
    allItems: List<TimelineItem>,
    monthGroups: List<Pair<String, List<TimelineItem>>>,
    curMonthKey: String,
    upcomingCount: Int,
    pastCount: Int,
    eclipseCount: Int,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrahmGold)
        }
        return
    }
    if (error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("⚠️ $error", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BrahmGold)) {
                    Text("Retry", color = Color.White)
                }
            }
        }
        return
    }

    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 8.dp),
    ) {
        // Search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search festivals, deities, Hindi name…", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                trailingIcon = if (searchQuery.isNotEmpty()) {{ IconButton(onClick = { onSearchChange("") }) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                }}} else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                shape = RoundedCornerShape(14.dp),
                colors = brahmFieldColors(),
            )
        }

        // Month jump pills (only when not searching)
        if (searchQuery.isEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(monthGroups) { idx, (key, _) ->
                        val isCur  = key == curMonthKey
                        val isPast = key < curMonthKey
                        val keyStr = key as String
                        // items before monthGroups: search(0) + pills(1) + stats(2) = 3
                        val targetIndex = 3 + idx
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = when {
                                isCur  -> BrahmGold.copy(0.15f)
                                isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)
                                else   -> MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)
                            },
                            border = BorderStroke(
                                0.5.dp,
                                when {
                                    isCur  -> BrahmGold.copy(0.4f)
                                    else   -> MaterialTheme.colorScheme.outline.copy(0.2f)
                                }
                            ),
                            onClick = { scope.launch { listState.animateScrollToItem(targetIndex) } },
                        ) {
                            Text(
                                monthKeyShort(keyStr),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = if (isCur) FontWeight.SemiBold else FontWeight.Normal,
                                color = when {
                                    isCur  -> BrahmGold
                                    isPast -> MaterialTheme.colorScheme.onSurface.copy(0.4f)
                                    else   -> MaterialTheme.colorScheme.onSurface.copy(0.7f)
                                },
                            )
                        }
                    }
                }
            }
        }

        // Stats
        item {
            Text(
                buildString {
                    if (searchQuery.isNotEmpty()) {
                        // search results count handled below
                    } else {
                        append("$upcomingCount upcoming")
                        if (pastCount > 0) append(" · $pastCount past")
                        if (eclipseCount > 0) append(" · $eclipseCount eclipse${if (eclipseCount != 1) "s" else ""}")
                    }
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
            )
        }

        if (searchQuery.isNotEmpty()) {
            // Flat search results
            val q = searchQuery.trim().lowercase()
            val results = allItems.filter { item ->
                val d = item.data
                if (item.kind == "eclipse") d.str("type").lowercase().contains(q)
                else d.str("name").lowercase().contains(q) ||
                     d.str("hindi").contains(q) ||
                     d.str("deity").lowercase().contains(q)
            }
            if (results.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No results for \"$searchQuery\"", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    }
                }
            } else {
                items(results) { item ->
                    if (item.kind == "eclipse") GrahanCard(item.data)
                    else FestivalCard(item.data)
                }
            }
        } else {
            // Month-grouped sections
            items(monthGroups) { (key, groupItems) ->
                MonthSection(
                    monthKey = key,
                    items = groupItems,
                    isCurrent = key == curMonthKey,
                    isPastMonth = key < curMonthKey,
                )
            }
        }
    }
    } // Box
}

@Composable
private fun MonthSection(
    monthKey: String,
    items: List<TimelineItem>,
    isCurrent: Boolean,
    isPastMonth: Boolean,
) {
    var open by remember(monthKey) { mutableStateOf(!isPastMonth) }
    val upcomingCount = items.count { !isPastDate(it.date) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Month header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when {
                        isCurrent  -> BrahmGold.copy(0.08f)
                        isPastMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(0.25f)
                        else        -> MaterialTheme.colorScheme.surfaceVariant.copy(0.18f)
                    }
                )
                .border(
                    0.5.dp,
                    when {
                        isCurrent  -> BrahmGold.copy(0.25f)
                        else        -> MaterialTheme.colorScheme.outline.copy(0.15f)
                    },
                    RoundedCornerShape(10.dp)
                )
                .clickable { open = !open }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    monthKeyLabel(monthKey),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (isPastMonth) MaterialTheme.colorScheme.onSurface.copy(0.5f)
                            else MaterialTheme.colorScheme.onSurface,
                )
                if (isCurrent) {
                    Surface(shape = RoundedCornerShape(4.dp), color = BrahmGold.copy(0.15f)) {
                        Text("THIS MONTH", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrahmGold)
                    }
                }
                if (upcomingCount > 0) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF59E0B).copy(0.10f)) {
                        Text("$upcomingCount upcoming", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp, color = Color(0xFFD97706))
                    }
                }
            }
            Icon(
                if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(0.5f),
            )
        }

        AnimatedVisibility(visible = open, enter = expandVertically(), exit = shrinkVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    if (item.kind == "eclipse") GrahanCard(item.data)
                    else FestivalCard(item.data)
                }
            }
        }
    }
}

// ─── Eclipses Tab ─────────────────────────────────────────────────────────────

@Composable
private fun EclipsesTab(grahan: JsonObject?, isLoading: Boolean, error: String?, onRetry: () -> Unit) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrahmGold)
        }
        return
    }
    if (error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("⚠️ $error", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BrahmGold)) {
                    Text("Retry", color = Color.White)
                }
            }
        }
        return
    }

    val eclipses = grahan?.arr("eclipses")?.map { it.jsonObject } ?: emptyList()

    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 8.dp),
    ) {
        // Info + rules
        item {
            Text(
                "Eclipse timings are for visible location. Sutak rules may vary by tradition.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
            )
        }
        item { SutakRulesCard() }

        if (eclipses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No eclipses visible this year ★", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            }
        } else {
            items(eclipses) { eclipse -> GrahanCard(eclipse) }
        }
    }
    } // Box
}

@Composable
private fun SutakRulesCard() {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BrahmGold.copy(0.06f))
            .border(0.5.dp, BrahmGold.copy(0.2f), RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("📜 Sutak & Grahan Rules", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrahmGold)
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null, modifier = Modifier.size(18.dp), tint = BrahmGold,
            )
        }

        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SUTAK_RULES.forEach { rule ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(rule.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(rule.body, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                            lineHeight = 16.sp)
                        rule.items.forEach { item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("•", fontSize = 11.sp, color = BrahmGold)
                                Text(item, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                                    lineHeight = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Festival Card ─────────────────────────────────────────────────────────────

@Composable
private fun FestivalCard(fest: JsonObject) {
    var expanded by remember { mutableStateOf(false) }

    val doshNotes    = fest.arr("dosh_notes") ?: JsonArray(emptyList())
    val grahanNotes  = doshNotes.filter { el ->
        val s = el.jsonPrimitive.content.lowercase()
        s.contains("grahan") || s.contains("eclipse")
    }
    val otherNotes   = doshNotes.filter { el ->
        val s = el.jsonPrimitive.content.lowercase()
        !s.contains("grahan") && !s.contains("eclipse")
    }
    val hasGrahan    = grahanNotes.isNotEmpty()

    val cardBg    = if (hasGrahan) Color(0xFFB71C1C).copy(0.08f) else Color(0xFFF59E0B).copy(0.05f)
    val borderClr = if (hasGrahan) Color(0xFFEF4444).copy(0.25f) else Color(0xFFF59E0B).copy(0.18f)
    val nameClr   = if (hasGrahan) Color(0xFFEF4444)             else Color(0xFFD97706)

    val dateStr = festDateRange(fest.str("date"), fest.str("date_end").takeIf { it != "—" })
    val timing  = tithiWindow(fest)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(0.5.dp, borderClr),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Header
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(fest.str("icon"), fontSize = 26.sp)
                Column(Modifier.weight(1f)) {
                    Text(fest.str("name"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = nameClr)
                    val hindi = fest.str("hindi")
                    if (hindi != "—") Text(hindi, fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    Text(dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)) {
                        Text(fest.str("tithi_name"),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                    }
                    val tType = fest.str("tithi_type")
                    if (tType != "—" && tType != "normal") TithiTypeBadge(tType)
                    if (hasGrahan) {
                        Surface(shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFEF4444).copy(0.10f),
                            border = BorderStroke(0.5.dp, Color(0xFFEF4444).copy(0.3f))) {
                            Text("● Grahan",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp, color = Color(0xFFF87171))
                        }
                    }
                }
            }

            // Tithi timing
            if (timing != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("⏱", fontSize = 12.sp)
                    Text("Tithi: $timing", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
            }

            // Fast note
            val fastNote = fest.str("fast_note")
            if (fastNote != "—") {
                ExplainBox(
                    bg = Color(0xFFF97316).copy(0.07f),
                    border = Color(0xFFF97316).copy(0.2f),
                    boldLabel = "🍃 Fast: ",
                    text = fastNote,
                    textColor = Color(0xFFEA580C),
                )
            }

            // Grahan notes — always visible
            if (hasGrahan) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("● Eclipse Conflict", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF87171))
                    grahanNotes.forEach { el ->
                        ExplainBox(
                            bg = Color(0xFFB71C1C).copy(0.07f),
                            border = Color(0xFFEF4444).copy(0.15f),
                            boldLabel = "",
                            text = el.jsonPrimitive.content,
                            textColor = Color(0xFFF87171),
                        )
                    }
                }
            }

            // Expandable section
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val significance = fest.str("significance")
                    if (significance != "—") {
                        Text(significance, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.7f), lineHeight = 16.sp)
                    }
                    otherNotes.forEach { el ->
                        ExplainBox(
                            bg = Color(0xFFF59E0B).copy(0.07f),
                            border = Color(0xFFF59E0B).copy(0.15f),
                            boldLabel = "",
                            text = el.jsonPrimitive.content,
                            textColor = Color(0xFFD97706),
                        )
                    }
                    // Tags
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        val deity  = fest.str("deity");  val paksha = fest.str("paksha")
                        val deity_s  = deity.takeIf  { it != "—" }
                        val paksha_s = paksha.takeIf { it != "—" && it != "N/A" }
                        if (deity_s  != null) item { SmallTag("⚛ $deity_s") }
                        if (paksha_s != null) item { SmallTag("$paksha_s Paksha") }
                    }
                }
            }

            Text(if (expanded) "▲ Less" else "▼ Details",
                fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
        }
    }
}

@Composable
private fun TithiTypeBadge(type: String) {
    val (bg, border, text) = when (type.lowercase()) {
        "vridhi" -> Triple(Color(0xFF3B82F6).copy(0.10f), Color(0xFF3B82F6).copy(0.3f), Color(0xFF3B82F6))
        "ksheya" -> Triple(Color(0xFFEF4444).copy(0.10f), Color(0xFFEF4444).copy(0.3f), Color(0xFFEF4444))
        else     -> Triple(Color(0xFFF59E0B).copy(0.10f), Color(0xFFF59E0B).copy(0.3f), Color(0xFFF59E0B))
    }
    Surface(shape = RoundedCornerShape(4.dp), color = bg,
        border = BorderStroke(0.5.dp, border)) {
        Text(type.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            fontSize = 9.sp, color = text)
    }
}

// ─── Grahan Card ──────────────────────────────────────────────────────────────

@Composable
private fun GrahanCard(eclipse: JsonObject) {
    var expanded by remember { mutableStateOf(true) }
    val isSolar  = eclipse.str("type").contains("Solar")
    val hasSutak = eclipse.int("sutak_hours") > 0
    val conflicts = eclipse.arr("festival_conflict") ?: JsonArray(emptyList())
    val color    = if (isSolar) Color(0xFFF59E0B) else Color(0xFF3B82F6)
    val guidance = ECLIPSE_GUIDANCE[eclipse.str("type")] ?: emptyList()

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, color.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            // Top accent strip
            Box(Modifier.fillMaxWidth().height(4.dp).background(
                Brush.horizontalGradient(
                    if (isSolar) listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
                    else listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                )
            ))
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (isSolar) "☉●" else "○●", fontSize = 28.sp)
                    Column(Modifier.weight(1f)) {
                        Text("${eclipse.str("type")} Eclipse",
                            fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
                        Text(if (isSolar) "Surya Grahan" else "Chandra Grahan",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        Text("${eclipse.str("nakshatra")} · ${eclipse.str("rashi")}",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                    Surface(shape = RoundedCornerShape(16.dp),
                        color = color.copy(0.10f), border = BorderStroke(0.5.dp, color.copy(0.3f))) {
                        Text(eclipse.str("date"),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
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
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(time, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace, color = color)
                            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            Text(sub, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        }
                    }
                }

                // Duration
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("⏱", fontSize = 13.sp)
                    Text("Duration: ${eclipse.int("duration_minutes")} min", fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text("Visible in India: ${if (eclipse.bool("visible_in_india")) "✓" else "✗"}",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }

                // Sutak
                if (hasSutak) {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFB71C1C).copy(0.07f))
                            .border(0.5.dp, Color(0xFFEF4444).copy(0.2f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text("⏳ Sutak Kaal (${eclipse.int("sutak_hours")}h before Sparsha)",
                            fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFFEF4444))
                        Text("${eclipse.str("sutak_start")} → ${eclipse.str("sparsha")}",
                            fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color(0xFFEF4444))
                        Text("Avoid cooking, eating, major activities. Temples closed.",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                } else {
                    Text("ℹ️ No Sutak — penumbral eclipse not observed with Sutak in most traditions.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.12f))
                            .padding(10.dp))
                }

                // Festival conflicts
                if (conflicts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🎪 Festival Impact", fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                            color = Color(0xFFD97706))
                        conflicts.forEach { el ->
                            ExplainBox(Color(0xFFF59E0B).copy(0.07f), Color(0xFFF59E0B).copy(0.2f),
                                "", el.jsonPrimitive.content, Color(0xFFD97706))
                        }
                    }
                }

                // Spiritual guidance (collapsible)
                if (guidance.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(color.copy(0.05f))
                            .border(0.5.dp, color.copy(0.15f), RoundedCornerShape(8.dp))
                            .clickable { expanded = !expanded }
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()) {
                            Text("🔮 Spiritual Guidance", fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold, color = color)
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null, modifier = Modifier.size(16.dp), tint = color,
                            )
                        }
                        AnimatedVisibility(visible = expanded,
                            enter = expandVertically(), exit = shrinkVertically()) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                guidance.forEach { g ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("•", fontSize = 11.sp, color = color)
                                        Text(g, fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                                    }
                                }
                            }
                        }
                        val effect = eclipse.str("spiritual_effect")
                        if (effect != "—") {
                            Text(effect, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.6f), lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Small helpers ─────────────────────────────────────────────────────────────

@Composable
private fun ExplainBox(bg: Color, border: Color, boldLabel: String, text: String, textColor: Color) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp)).background(bg)
        .border(0.5.dp, border, RoundedCornerShape(7.dp)).padding(8.dp)) {
        Text(
            text = boldLabel + text,
            style = MaterialTheme.typography.bodySmall.copy(color = textColor, fontSize = 11.sp),
            lineHeight = 15.sp,
        )
    }
}

@Composable
private fun SmallTag(label: String) {
    Surface(shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)) {
        Text(label, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
    }
}

@Composable
private fun PRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
    }
}
