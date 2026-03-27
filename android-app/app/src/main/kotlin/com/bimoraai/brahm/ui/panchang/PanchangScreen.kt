package com.bimoraai.brahm.ui.panchang

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.BrahmErrorView
import com.bimoraai.brahm.core.components.BrahmLoadingSpinner
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.data.CitySearchViewModel
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.theme.*
import com.bimoraai.brahm.ui.main.Route
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── Explain Maps ─────────────────────────────────────────────────────────────

private data class AngaExplain(val what: String, val why: String, val doWhat: String)
private val ANGA_EXPLAIN = mapOf(
    "Tithi" to AngaExplain(
        "Tithi is the lunar day — calculated from the angle between the Sun and Moon. Every 12° of difference = 1 tithi. There are 30 tithis in a lunar month.",
        "Each tithi has a ruling deity and energy. It determines which activities are naturally supported — fasting, worship, travel, business, or rest.",
        "Pratipada/Pratipad: good for starting new work. Panchami/Dashami: auspicious for learning. Ekadashi: fast day, Vishnu worship. Purnima: full moon — powerful for charity. Amavasya: ancestor rites.",
    ),
    "Nakshatra" to AngaExplain(
        "The 27 lunar mansions (star clusters) through which the Moon passes. The Moon stays in each nakshatra for about 1 day.",
        "The Moon's nakshatra affects your emotional state, intuition, and the type of energy available for activities. Each has a ruling deity and planet.",
        "Ashwini, Pushya, Hasta, Rohini, Mrigashirsha: highly auspicious for new starts. Moola, Jyeshtha, Ashlesha, Magha: avoid major beginnings. Check your birth nakshatra for personal alignment.",
    ),
    "Yoga" to AngaExplain(
        "Yoga is calculated by adding the longitudes of the Sun and Moon, then dividing by 13°20'. There are 27 yogas, each with a specific quality.",
        "Yoga reflects the combined energy of the day — whether the universe supports success, obstacles, progress, or caution.",
        "Auspicious yogas (Siddhi, Shubha, Amrita, Brahma, Indra): begin important work. Inauspicious yogas (Vishkambha, Atiganda, Shula, Ganda, Vyaghata): prefer spiritual practices, avoid big decisions.",
    ),
    "Karana" to AngaExplain(
        "Karana is half of a tithi — it changes approximately every 6 hours. There are 11 karanas, with Vishti (Bhadra) being the most inauspicious.",
        "Karana determines the quality of short time windows during the day. Important for choosing the right hour for specific tasks.",
        "Bhadra active: avoid starting journeys, new work, signing contracts, or sacred rituals. All other karanas: generally supportive. Bava, Balava, Kaulava: especially auspicious.",
    ),
    "Vara (Day)" to AngaExplain(
        "Vara is the weekday per the Hindu calendar, each ruled by a planet. Ravivara=Sun, Somavara=Moon, Mangalavara=Mars, Budhavara=Mercury, Guruvara=Jupiter, Shukravara=Venus, Shanivara=Saturn.",
        "The ruling planet of the day influences which activities are naturally supported and which deities are best worshipped.",
        "Sunday: Surya puja, avoid oil use. Monday: Shiva puja, fasting. Tuesday: Hanuman/Durga puja. Wednesday: Ganesha/Vishnu. Thursday: Guru puja, start learning. Friday: Lakshmi puja, beauty. Saturday: Shani puja, avoid non-veg.",
    ),
)

private data class TimingExplain(val what: String, val doWhat: String, val avoid: String? = null)
private val TIMING_EXPLAIN = mapOf(
    "Brahma Muhurta" to TimingExplain(
        "The sacred 'creator's hour' — 96 minutes before sunrise (roughly 4–5:30 AM). Brahma = the creator, Muhurta = auspicious time window.",
        "Meditation, yoga, pranayama, studying scriptures, chanting mantras. The mind is clearest and cosmic energy is at its purest at this hour.",
        "Sleeping through it (considered a missed spiritual opportunity).",
    ),
    "Sunrise" to TimingExplain(
        "Udaya — the moment the upper limb of the Sun crosses the horizon. All 5 Panchang elements are determined at this moment per the Udaya Tithi rule.",
        "Offer Surya Arghya (water to the Sun). Begin your day's important activities after sunrise.",
    ),
    "Abhijit Muhurta" to TimingExplain(
        "The most powerful muhurta of the day — a 48-minute window centered exactly at solar noon. 'Abhijit' means 'victorious'. One of the 30 muhurtas in a day.",
        "Start the most important task of your day here. Business meetings, signing agreements, beginning long journeys, important decisions.",
        "Not effective on Wednesdays. Avoid for north-facing activities.",
    ),
    "Rahu Kaal" to TimingExplain(
        "An inauspicious 90-minute period each day, ruled by Rahu (the shadow planet / north lunar node). Each weekday has a fixed 1/8th slot of daytime assigned to Rahu.",
        "Use this time for spiritual practice, reading, meditation, or routine work that doesn't need an auspicious start.",
        "Starting new work, travel, medical procedures, business deals, signing contracts, marriage negotiations.",
    ),
    "Yamagandam" to TimingExplain(
        "Inauspicious period governed by Yama — the deity of death and dharma. Like Rahu Kaal, it's a fixed segment of each weekday.",
        "Ancestor worship (Pitra Tarpan) is enhanced during this period. Routine tasks, spiritual practices.",
        "All auspicious activities: weddings, new business, travel, moving houses, medical operations.",
    ),
    "Gulika Kaal" to TimingExplain(
        "Period governed by Gulika (Manda) — the son of Saturn. Considered inauspicious like Rahu Kaal but slightly less severe.",
        "Tantra practices, Shani worship, studying difficult subjects. Administrative and discipline-related work.",
        "New beginnings, auspicious ceremonies, travel starts, signing important documents.",
    ),
    "Sunset" to TimingExplain(
        "The moment the lower limb of the Sun touches the horizon. Marks the transition from daytime to nighttime energy.",
        "Evening prayer (Sandhya Vandana), lighting a lamp (Diya), offering incense. A spiritually sensitive time of day.",
        "Starting new work, travel, or meals right at sunset in traditional practice.",
    ),
    "Pradosh Kaal" to TimingExplain(
        "The twilight period — 144 minutes starting from sunset. 'Pradosh' means 'at the beginning of the night'. Especially sacred on Trayodashi (13th tithi).",
        "Shiva worship and Abhishek. Lighting oil/ghee lamps. Evening prayers.",
        "Conflict, arguments, heavy meals. This is a Sattvic (pure) time meant for devotion.",
    ),
    "Nishita Kaal" to TimingExplain(
        "The midnight period — the 8th muhurta of the night, centered at astronomical midnight. Sacred for Kali, Shiva, and the birth time of Lord Krishna.",
        "Advanced Tantric practices, Kali/Shiva worship, deep meditation. Janmashtami celebration centers on this moment.",
        "Eating, worldly activities. This is the most Tamasic time.",
    ),
)

private data class ChogExplain(val what: String, val good: String, val avoid: String)
private val CHOG_EXPLAIN = mapOf(
    "Amrit"  to ChogExplain("Amrit means 'nectar of immortality'. The most auspicious Choghadiya period — ruled by the Moon.", "All activities: new work, travel, business, marriage, medical treatment.", "Nothing — this is universally favorable."),
    "Shubh"  to ChogExplain("Shubh means 'auspicious'. Ruled by Jupiter (Guru). Second-best Choghadiya period.", "Religious ceremonies, weddings, new learning, creative work, socializing.", "Generally none."),
    "Labh"   to ChogExplain("Labh means 'profit or gain'. Ruled by Mercury (Budha). Strong for material success.", "Business deals, trade, financial investments, buying property, opening shops.", "Spiritual rituals are better in other periods."),
    "Char"   to ChogExplain("Char means 'moving or dynamic'. Ruled by Venus (Shukra). Good for anything involving movement.", "Travel, changing homes/offices, starting journeys, outdoor activities.", "Signing long-term static commitments."),
    "Rog"    to ChogExplain("Rog means 'disease or illness'. Ruled by Mars (Mangal). Generally inauspicious.", "Medical treatment, surgery. Administrative/government work.", "New business, travel, weddings, buying property."),
    "Kaal"   to ChogExplain("Kaal means 'death or time'. Ruled by Saturn (Shani). Most inauspicious Choghadiya.", "Iron/metal work, collecting dues.", "All auspicious activities: new starts, travel, celebrations."),
    "Udveg"  to ChogExplain("Udveg means 'anxiety or disturbance'. Ruled by the Sun. Inauspicious for most activities.", "Government work, legal matters, dealing with authority figures.", "New ventures, travel, celebrations, financial deals."),
)

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun JsonObject.str(key: String): String {
    val el = this[key] ?: return "—"
    if (el !is JsonPrimitive) return "—"
    return el.contentOrNull?.takeIf { it.isNotBlank() && it != "null" } ?: "—"
}

private fun JsonObject.nested(outer: String, inner: String): String {
    val obj = this[outer]?.let { try { it.jsonObject } catch (_: Exception) { null } } ?: return "—"
    val el  = obj[inner] ?: return "—"
    if (el !is JsonPrimitive) return "—"
    return (el as JsonPrimitive).contentOrNull?.takeIf { it.isNotBlank() && it != "null" } ?: "—"
}

private fun JsonObject.boolVal(key: String): Boolean {
    return this[key]?.jsonPrimitive?.booleanOrNull ?: false
}

private fun JsonObject.nestedBool(outer: String, inner: String): Boolean {
    val obj = this[outer]?.let { try { it.jsonObject } catch (_: Exception) { null } } ?: return false
    return obj[inner]?.jsonPrimitive?.booleanOrNull ?: false
}

private fun JsonObject.timeRange(key: String): String {
    val obj = this[key]?.let { try { it.jsonObject } catch (_: Exception) { null } } ?: return "—"
    val s = obj["start"]?.let { if (it is JsonPrimitive) it.contentOrNull else null } ?: "?"
    val e = obj["end"]?.let   { if (it is JsonPrimitive) it.contentOrNull else null } ?: "?"
    return "$s – $e"
}

private val auspiciousNames = setOf("Amrit", "Shubh", "Labh", "Char")

private data class ChogItem(val name: String, val hindi: String, val auspicious: Boolean, val start: String, val end: String)

private fun JsonObject.parseChogList(tab: String): List<ChogItem>? = try {
    val chogObj = this["choghadiya"]?.jsonObject ?: return null
    chogObj[tab]?.jsonArray?.map { el ->
        val obj = el.jsonObject
        val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "—"
        ChogItem(
            name      = name,
            hindi     = obj["hindi"]?.jsonPrimitive?.contentOrNull ?: "",
            auspicious = obj["auspicious"]?.jsonPrimitive?.booleanOrNull ?: (name in auspiciousNames),
            start     = obj["start"]?.jsonPrimitive?.contentOrNull ?: "—",
            end       = obj["end"]?.jsonPrimitive?.contentOrNull ?: "—",
        )
    }
} catch (_: Exception) { null }

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun PanchangScreen(
    navController: NavController,
    vm: PanchangViewModel = hiltViewModel(),
    cityVm: CitySearchViewModel = hiltViewModel(),
) {
    val panchang  by vm.panchang.collectAsState()
    val festivals by vm.festivals.collectAsState()
    val grahan    by vm.grahan.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error     by vm.error.collectAsState()
    val cityName  by vm.cityName.collectAsState()

    val suggestions by cityVm.suggestions.collectAsState()

    // Live clock
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) { delay(1_000); now = LocalTime.now() }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    SwipeBackLayout(navController) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("📅 Panchang", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.load() }, enabled = !isLoading) {
                            Icon(
                                if (isLoading) Icons.Default.Refresh else Icons.Default.Refresh,
                                contentDescription = "Refresh", tint = BrahmGold,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BrahmBackground),
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).background(BrahmBackground)) {
                // Tab row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = BrahmBackground,
                    contentColor = BrahmGold,
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("📅 Panchang", modifier = Modifier.padding(vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (selectedTab == 0) BrahmGold else BrahmMutedForeground,
                                fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                            ))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("⭐ Muhurta", modifier = Modifier.padding(vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (selectedTab == 1) BrahmGold else BrahmMutedForeground,
                                fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                            ))
                    }
                }

                when (selectedTab) {
                    0 -> PanchangTabContent(
                        panchang    = panchang,
                        festivals   = festivals,
                        grahan      = grahan,
                        nowTime     = now,
                        isLoading   = isLoading,
                        error       = error,
                        cityName    = cityName ?: "New Delhi",
                        cityQuery   = cityVm.cityQuery.value,
                        suggestions = suggestions,
                        onCityQuery = { cityVm.cityQuery.value = it },
                        onCitySelect = { city ->
                            cityVm.cityQuery.value = ""
                            vm.loadForCity(city.lat, city.lon, city.tz, city.name)
                        },
                        onRetry = { vm.load() },
                    )
                    1 -> MuhurtaTabContent(navController)
                }
            }
        }
    }
}

// ─── Muhurta tab ──────────────────────────────────────────────────────────────

@Composable
private fun MuhurtaTabContent(navController: NavController) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("⭐", fontSize = 40.sp)
            Text("Muhurta", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
            Text("Find the best auspicious time for important activities",
                style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground),
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { navController.navigate(Route.MUHURTA) },
                colors = ButtonDefaults.buttonColors(containerColor = BrahmGold),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Open Muhurta →", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Panchang tab content ─────────────────────────────────────────────────────

@Composable
private fun PanchangTabContent(
    panchang:     JsonObject?,
    festivals:    JsonObject?,
    grahan:       JsonObject?,
    nowTime:      LocalTime,
    isLoading:    Boolean,
    error:        String?,
    cityName:     String,
    cityQuery:    String,
    suggestions:  List<City>,
    onCityQuery:  (String) -> Unit,
    onCitySelect: (City) -> Unit,
    onRetry:      () -> Unit,
) {
    val p = panchang

    var expandedKey by remember { mutableStateOf<String?>(null) }
    var chogTab     by remember { mutableStateOf("day") }

    val today = LocalDate.now()
    val dateStr = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))
    val timeStr = nowTime.format(DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH))

    // Derived values
    val panchaka      = p?.boolVal("panchaka") ?: false
    val tithiPaksha   = p?.nested("tithi", "paksha") ?: ""
    val tithiName     = p?.nested("tithi", "name") ?: ""
    val tithiType     = p?.nested("tithi", "tithi_type") ?: ""
    val pakshaBanner  = if (tithiPaksha == "Shukla") "🌕 Shukla Paksha" else if (tithiPaksha == "Krishna") "🌑 Krishna Paksha" else ""

    val yogaName       = p?.nested("yoga", "name") ?: ""
    val yogaAuspicious = p?.nestedBool("yoga", "is_auspicious") ?: false
    val abhijit        = p?.timeRange("abhijit_muhurta") ?: "—"
    val rahuKaal       = p?.timeRange("rahukaal") ?: "—"
    val yamagandam     = p?.timeRange("yamagandam") ?: "—"
    val brahmaMuhurta  = p?.timeRange("brahma_muhurta") ?: "—"

    // Nishita Kaal
    val nishitaStr = p?.let { pj ->
        try {
            val n = pj["nishita_kaal"]?.jsonObject ?: return@let null
            val s = n["start"]?.jsonPrimitive?.contentOrNull ?: return@let null
            val e = n["end"]?.jsonPrimitive?.contentOrNull ?: return@let null
            val mid = n["midpoint"]?.jsonPrimitive?.contentOrNull
            val label = if (mid != null) "Nishita Kaal (mid: $mid)" else "Nishita Kaal"
            Pair(label, "$s – $e")
        } catch (_: Exception) { null }
    }

    // Timings list
    data class TimingItem(val icon: String, val label: String, val value: String, val type: String)
    val timings = if (p != null) buildList {
        add(TimingItem("🌄", "Brahma Muhurta",  brahmaMuhurta,            "benefic"))
        add(TimingItem("🌅", "Sunrise",          p.str("sunrise"),          "benefic"))
        add(TimingItem("✨", "Abhijit Muhurta",  abhijit,                   "special"))
        add(TimingItem("⚠️", "Rahu Kaal",        rahuKaal,                  "malefic"))
        add(TimingItem("💀", "Yamagandam",       yamagandam,                "malefic"))
        add(TimingItem("🌀", "Gulika Kaal",      p.timeRange("gulika_kaal"),"malefic"))
        add(TimingItem("🌇", "Sunset",           p.str("sunset"),           "neutral"))
        add(TimingItem("🌆", "Pradosh Kaal",     p.timeRange("pradosh_kaal"),"special"))
        if (nishitaStr != null) add(TimingItem("🌑", nishitaStr.first, nishitaStr.second, "special"))
    } else emptyList()

    if (isLoading && p == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BrahmLoadingSpinner()
        }
        return
    }
    if (error != null && p == null) {
        BrahmErrorView(message = error, onRetry = onRetry)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(dateStr, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        Text("$timeStr · $cityName",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BrahmGold.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace))
                    }
                    if (panchaka) {
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFFEE2E2))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text("⚠ Panchaka",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF991B1B), fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
                // ── City selector ────────────────────────────────────────────
                Box {
                    OutlinedTextField(
                        value = cityQuery,
                        onValueChange = onCityQuery,
                        placeholder = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.LocationOn, null, tint = BrahmMutedForeground, modifier = Modifier.size(14.dp))
                                Text(cityName, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = BrahmMutedForeground, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BrahmGold, strokeWidth = 2.dp)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrahmGold,
                            unfocusedBorderColor = BrahmBorder,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                        ),
                        textStyle = MaterialTheme.typography.bodySmall,
                    )
                    // Suggestions dropdown
                    if (suggestions.isNotEmpty()) {
                        Popup(onDismissRequest = { onCityQuery("") }) {
                            Card(
                                modifier = Modifier.width(320.dp).padding(top = 56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(8.dp),
                            ) {
                                Column {
                                    suggestions.forEachIndexed { i, city ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .clickable { onCitySelect(city) }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Icon(Icons.Default.LocationOn, null, tint = BrahmGold, modifier = Modifier.size(14.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(city.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                                if (city.country.isNotBlank()) Text(city.country, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
                                            }
                                        }
                                        if (i < suggestions.lastIndex) HorizontalDivider(color = BrahmBorder)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Paksha banner ────────────────────────────────────────────────────
        if (p != null && pakshaBanner.isNotBlank()) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("$pakshaBanner · $tithiName",
                        style = MaterialTheme.typography.bodyMedium.copy(color = BrahmGold.copy(alpha = 0.8f)))
                    if (tithiType == "vridhi" || tithiType == "ksheya") {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(6.dp))
                                .background(if (tithiType == "vridhi") Color(0xFFFEF3C7) else Color(0xFFFEE2E2))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(if (tithiType == "vridhi") "Vridhi" else "Ksheya",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (tithiType == "vridhi") Color(0xFF92400E) else Color(0xFF991B1B),
                                    fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
            }
        }

        // ── Hint ─────────────────────────────────────────────────────────────
        if (p != null) {
            item {
                Text("Tap any card or row to understand what it means and what to do",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BrahmMutedForeground.copy(alpha = 0.6f), fontSize = 11.sp),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }

        // ── Panch Anga ───────────────────────────────────────────────────────
        if (p != null) {
            item {
                Text("PANCH ANGA — FIVE ELEMENTS OF THE DAY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BrahmMutedForeground, letterSpacing = 0.8.sp))
            }
            item {
                val angaItems = listOf(
                    AngaItem("Tithi",      "तिथि",  "🌙",
                        "$tithiPaksha $tithiName",
                        "Ends: ${p.nested("tithi","end_time")}${if (tithiType != "normal" && tithiType != "—" && tithiType.isNotBlank()) " · $tithiType" else ""}",
                        "neutral"),
                    AngaItem("Nakshatra",  "नक्षत्र","⭐",
                        p.nested("nakshatra","name"),
                        "${p.nested("nakshatra","hindi")} · Pada ${p.nested("nakshatra","pada")} · Lord: ${p.nested("nakshatra","lord")}${p.nested("nakshatra","end_time").let { if (it != "—") " · Ends: $it" else "" }}",
                        "benefic"),
                    AngaItem("Yoga",       "योग",   "🔮",
                        yogaName,
                        "${p.nested("yoga","hindi")}${p.nested("yoga","end_time").let { if (it != "—") " · Ends: $it" else "" }}",
                        if (yogaAuspicious) "benefic" else "malefic"),
                    AngaItem("Karana",     "करण",   "📐",
                        p.nested("karana","name"),
                        "${p.nested("karana","hindi")}${if (p.nestedBool("karana","is_bhadra")) " · ⚠ Bhadra — avoid new starts" else ""}",
                        if (p.nestedBool("karana","is_bhadra")) "malefic" else "neutral"),
                    AngaItem("Vara (Day)", "वार",   "📅",
                        p.nested("vara","name"),
                        "${p.nested("vara","hindi")} · Lord: ${p.nested("vara","lord")}",
                        "neutral"),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    angaItems.forEach { item ->
                        AngaCard(
                            item = item,
                            expanded = expandedKey == item.label,
                            onToggle = { expandedKey = if (expandedKey == item.label) null else item.label },
                        )
                    }
                }
            }
        }

        // ── Daily Timings ────────────────────────────────────────────────────
        if (p != null && timings.isNotEmpty()) {
            item {
                Text("DAILY TIMINGS · ${cityName.uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, letterSpacing = 0.8.sp))
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    timings.forEach { t ->
                        TimingRow(
                            icon = t.icon, label = t.label, value = t.value, type = t.type,
                            expanded = expandedKey == "t_${t.label}",
                            onToggle = { expandedKey = if (expandedKey == "t_${t.label}") null else "t_${t.label}" },
                        )
                    }
                }
            }
        }

        // ── Choghadiya ───────────────────────────────────────────────────────
        if (p != null) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("CHOGHADIYA — 8 TIME PERIODS",
                        style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, letterSpacing = 0.8.sp),
                        modifier = Modifier.weight(1f))
                    // Day/Night toggle
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF3F4F6)),
                    ) {
                        listOf("day" to "☀ Day", "night" to "🌙 Night").forEach { (tab, label) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (chogTab == tab) BrahmGold else Color.Transparent)
                                    .clickable { chogTab = tab }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (chogTab == tab) Color.White else BrahmMutedForeground,
                                    fontWeight = if (chogTab == tab) FontWeight.SemiBold else FontWeight.Normal,
                                ))
                            }
                        }
                    }
                }
            }
            item {
                Text("Tap a period to see what activities are good or bad in that window",
                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground.copy(alpha = 0.6f), fontSize = 11.sp))
            }
            item {
                val chogs = p.parseChogList(chogTab)
                if (chogs != null) {
                    val chunked = chogs.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        chunked.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { cg ->
                                    val key = "chog_${chogTab}_${cg.name}_${cg.start}"
                                    ChogCard(
                                        item = cg,
                                        expanded = expandedKey == key,
                                        onToggle = { expandedKey = if (expandedKey == key) null else key },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // ── Today's Summary ──────────────────────────────────────────────────
        if (p != null && yogaName.isNotBlank() && yogaName != "—") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrahmBorder),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("✨ Today's Summary", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        // Yoga quality
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(if (yogaAuspicious) Color(0xFFD1FAE5) else Color(0xFFFEF3C7))
                                .padding(10.dp),
                        ) {
                            Text(
                                "$yogaName Yoga" + if (yogaAuspicious) " — Auspicious energy today. Good for important work and new beginnings."
                                    else " — Inauspicious yoga. Prefer spiritual practices over major decisions today.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (yogaAuspicious) Color(0xFF065F46) else Color(0xFF92400E)),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SummaryBox(Color(0xFF065F46), Color(0xFFD1FAE5),
                                "Best window: ", "Abhijit $abhijit — use for your most important task today.")
                            SummaryBox(Color(0xFF991B1B), Color(0xFFFEE2E2),
                                "Avoid starting new work: ", "Rahu Kaal $rahuKaal, Yamagandam $yamagandam.")
                            if (brahmaMuhurta != "—") SummaryBox(BrahmGold, Color(0xFFFEF9EC),
                                "Brahma Muhurta: ", "$brahmaMuhurta — ideal for meditation and spiritual practice.")
                            if (panchaka) SummaryBox(Color(0xFF9F1239), Color(0xFFFFF1F2),
                                "⚠ Panchaka active — ", "Avoid rooftop construction, cremation rites, and storing firewood today.")
                        }
                    }
                }
            }
        }

        // ── Festivals ────────────────────────────────────────────────────────
        item {
            Text("FESTIVALS THIS MONTH",
                style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, letterSpacing = 0.8.sp))
        }
        item {
            val festList = festivals?.get("festivals")?.let { el ->
                try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
            }
            if (festList.isNullOrEmpty()) {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Text("No festivals data available.", modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    festList.forEach { fest -> FestivalCard(fest) }
                }
            }
        }

        // ── Eclipse Calendar ─────────────────────────────────────────────────
        item {
            Text("ECLIPSE CALENDAR",
                style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, letterSpacing = 0.8.sp))
        }
        item {
            val grahanList = grahan?.get("eclipses")?.let { el ->
                try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
            }
            if (grahanList.isNullOrEmpty()) {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Text("No upcoming eclipses.", modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    grahanList.forEach { eclipse -> GrahanCard(eclipse) }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ─── Anga Card ────────────────────────────────────────────────────────────────

private data class AngaItem(val label: String, val hindi: String, val icon: String, val value: String, val sub: String, val quality: String)

@Composable
private fun AngaCard(item: AngaItem, expanded: Boolean, onToggle: () -> Unit) {
    val (qBg, qFg) = when (item.quality) {
        "benefic" -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        "malefic" -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
        else      -> Color(0xFFF3F4F6) to Color(0xFF6B7280)
    }
    val exp = ANGA_EXPLAIN[item.label]

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(item.icon, fontSize = 22.sp, modifier = Modifier.padding(top = 2.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, letterSpacing = 0.5.sp))
                        Text("(${item.hindi})",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                    }
                    Text(item.value,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    if (item.sub.isNotBlank() && item.sub != "—") {
                        Text(item.sub,
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(qBg).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(item.quality, style = MaterialTheme.typography.labelSmall.copy(color = qFg, fontWeight = FontWeight.SemiBold, fontSize = 10.sp))
                    }
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null, tint = BrahmMutedForeground, modifier = Modifier.size(16.dp))
                }
            }
            if (exp != null) {
                AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        HorizontalDivider(color = BrahmBorder)
                        Spacer(Modifier.height(2.dp))
                        ExplainBox(Color(0xFFF8F9FA), BrahmBorder, "What is it? ", exp.what, BrahmForeground.copy(alpha = 0.8f))
                        ExplainBox(Color(0xFFFEF9EC), Color(0xFFFDE68A), "Why does it matter? ", exp.why, Color(0xFF92400E))
                        ExplainBox(Color(0xFFF0FDF4), Color(0xFF86EFAC), "What to do? ", exp.doWhat, Color(0xFF166534))
                    }
                }
            }
        }
    }
}

// ─── Timing Row ───────────────────────────────────────────────────────────────

@Composable
private fun TimingRow(icon: String, label: String, value: String, type: String, expanded: Boolean, onToggle: () -> Unit) {
    val (bg, border, clr) = when (type) {
        "benefic" -> Triple(Color(0xFFF0FDF4), Color(0xFF86EFAC), Color(0xFF15803D))
        "malefic" -> Triple(Color(0xFFFFF5F5), Color(0xFFFCA5A5), Color(0xFFDC2626))
        "special" -> Triple(Color(0xFFFEF9EC), Color(0xFFFDE68A), BrahmGold)
        else      -> Triple(Color(0xFFF8F9FA), BrahmBorder, BrahmMutedForeground)
    }
    val expKey = TIMING_EXPLAIN.keys.find { label.startsWith(it) }
    val exp = if (expKey != null) TIMING_EXPLAIN[expKey] else null

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(enabled = exp != null) { onToggle() },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(icon, fontSize = 16.sp)
            Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodySmall.copy(color = clr, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace))
            if (exp != null) Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null, tint = BrahmMutedForeground, modifier = Modifier.size(14.dp))
        }
        if (exp != null) {
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HorizontalDivider(color = border)
                    Spacer(Modifier.height(4.dp))
                    Text(exp.what,
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
                    Text("✓ Do: ${exp.doWhat}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF15803D), fontSize = 11.sp))
                    if (exp.avoid != null) {
                        Text("✕ Avoid: ${exp.avoid}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDC2626), fontSize = 11.sp))
                    }
                }
            }
        }
    }
}

// ─── Chog Card ────────────────────────────────────────────────────────────────

@Composable
private fun ChogCard(item: ChogItem, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val (bg, border, fg) = when (item.name) {
        "Amrit"  -> Triple(Color(0xFFD1FAE5), Color(0xFF6EE7B7), Color(0xFF065F46))
        "Shubh"  -> Triple(Color(0xFFCCFBF1), Color(0xFF5EEAD4), Color(0xFF134E4A))
        "Labh"   -> Triple(Color(0xFFE0F2FE), Color(0xFF7DD3FC), Color(0xFF0C4A6E))
        "Char"   -> Triple(Color(0xFFDBEAFE), Color(0xFF93C5FD), Color(0xFF1E3A5F))
        "Rog"    -> Triple(Color(0xFFFEE2E2), Color(0xFFFCA5A5), Color(0xFF991B1B))
        "Kaal"   -> Triple(Color(0xFFFFF1F2), Color(0xFFFDA4AF), Color(0xFF9F1239))
        "Udveg"  -> Triple(Color(0xFFFFF7ED), Color(0xFFFDBA74), Color(0xFF9A3412))
        else     -> Triple(Color(0xFFF3F4F6), BrahmBorder, BrahmForeground)
    }
    val exp = CHOG_EXPLAIN[item.name]

    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(bg)
            .clickable(enabled = exp != null) { onToggle() },
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(item.name, style = MaterialTheme.typography.titleSmall.copy(color = fg, fontWeight = FontWeight.SemiBold))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (item.auspicious) "✓ Good" else "✕ Avoid",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (item.auspicious) Color(0xFF15803D) else Color(0xFFDC2626), fontSize = 10.sp))
                    if (exp != null) Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null, tint = fg.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                }
            }
            if (item.hindi.isNotBlank()) Text(item.hindi, style = MaterialTheme.typography.labelSmall.copy(color = fg.copy(alpha = 0.6f), fontSize = 10.sp))
            Text("${item.start} – ${item.end}",
                style = MaterialTheme.typography.labelSmall.copy(color = fg.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace, fontSize = 10.sp))
        }
        if (exp != null) {
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HorizontalDivider(color = border)
                    Spacer(Modifier.height(2.dp))
                    Text(exp.what, style = MaterialTheme.typography.bodySmall.copy(color = fg.copy(alpha = 0.8f), fontSize = 10.sp))
                    Text("✓ ${exp.good}", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF15803D), fontSize = 10.sp))
                    Text("✕ ${exp.avoid}", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDC2626), fontSize = 10.sp))
                }
            }
        }
    }
}

// ─── Festival Card ────────────────────────────────────────────────────────────

private fun fmt12(t: String?): String {
    if (t.isNullOrBlank() || t == "N/A") return t ?: "—"
    val parts = t.split(":").mapNotNull { it.toIntOrNull() }
    if (parts.size < 2) return t
    val h = parts[0]; val m = parts[1]
    val ampm = if (h >= 12) "PM" else "AM"
    return "${(if (h % 12 == 0) 12 else h % 12)}:${m.toString().padStart(2,'0')} $ampm"
}

private fun festDateRange(date: String, dateEnd: String?): String {
    if (dateEnd.isNullOrBlank() || dateEnd == date) return date
    return "$date – $dateEnd"
}

private fun tithiWindow(fest: JsonObject): String? {
    val ts = fest["tithi_start"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "N/A" } ?: return null
    val te = fest["tithi_end"]?.jsonPrimitive?.contentOrNull ?: return null
    if (ts == "00:00" && te == "23:59") return "All day"
    val tithi = fest["tithi_name"]?.jsonPrimitive?.contentOrNull
    if (tithi == "Sankranti" || ts == te) return "Entry at ${fmt12(ts)}"
    return "${fmt12(ts)} – ${fmt12(te)}"
}

@Composable
private fun FestivalCard(fest: JsonObject) {
    val name       = fest["name"]?.jsonPrimitive?.contentOrNull ?: "Festival"
    val hindi      = fest["hindi"]?.jsonPrimitive?.contentOrNull ?: ""
    val deity      = fest["deity"]?.jsonPrimitive?.contentOrNull ?: ""
    val icon       = fest["icon"]?.jsonPrimitive?.contentOrNull ?: "🎉"
    val date       = fest["date"]?.jsonPrimitive?.contentOrNull ?: "—"
    val dateEnd    = fest["date_end"]?.jsonPrimitive?.contentOrNull
    val paksha     = fest["paksha"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "N/A" }
    val tithiName  = fest["tithi_name"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "N/A" && it != "Full day" }
    val tithiType  = fest["tithi_type"]?.jsonPrimitive?.contentOrNull
    val fastNote   = fest["fast_note"]?.jsonPrimitive?.contentOrNull?.takeIf { it != "N/A" }
    val significance = fest["significance"]?.jsonPrimitive?.contentOrNull
    val tradition  = fest["tradition"]?.jsonPrimitive?.contentOrNull ?: "Hindu"
    val window     = tithiWindow(fest)
    val doshNotes  = try { fest["dosh_notes"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList() } catch (_: Exception) { emptyList<String>() }
    val grahanNotes = doshNotes.filter { it.contains("Grahan", ignoreCase = true) || it.contains("eclipse", ignoreCase = true) }
    val otherNotes  = doshNotes.filter { it !in grahanNotes }

    var expanded by remember { mutableStateOf(false) }

    val tradColor = when (tradition) {
        "Hindu"    -> Color(0xFFFF6F00)
        "Jain"     -> Color(0xFF1565C0)
        "Buddhist" -> Color(0xFF43A047)
        "Sikh"     -> Color(0xFF8E24AA)
        else       -> BrahmGold
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = when {
            grahanNotes.isNotEmpty() -> BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
            otherNotes.isNotEmpty()  -> BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.2f))
            else -> BorderStroke(1.dp, BrahmBorder)
        },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header row
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(icon, fontSize = 28.sp)
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    if (hindi.isNotBlank() || deity.isNotBlank()) {
                        Text("${hindi.takeIf { it.isNotBlank() } ?: ""}${if (hindi.isNotBlank() && deity.isNotBlank()) " · " else ""}${deity.takeIf { it.isNotBlank() } ?: ""}",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (tithiType == "vridhi") TithiTypeBadge("vridhi")
                    else if (tithiType == "ksheya") TithiTypeBadge("ksheya")
                    if (fastNote != null) {
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFF97316).copy(alpha = 0.1f)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                            Text("🕐 Vrat", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFEA580C), fontSize = 9.sp))
                        }
                    }
                    if (grahanNotes.isNotEmpty()) {
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFEF4444).copy(alpha = 0.1f)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                            Text("⚠ Grahan", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFDC2626), fontSize = 9.sp))
                        }
                    }
                }
            }

            // Date + tithi window
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF8F9FA)).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.CalendarToday, null, tint = BrahmGold.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                    Text(festDateRange(date, dateEnd), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                    if (!paksha.isNullOrBlank()) Text("· $paksha", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
                }
                if (window != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.AccessTime, null, tint = BrahmMutedForeground, modifier = Modifier.size(12.dp))
                        Text(buildString {
                            if (!tithiName.isNullOrBlank()) { append(tithiName); append(" · ") }
                            append(window)
                        }, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
                    }
                }
            }

            // Fast note
            if (!fastNote.isNullOrBlank()) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF97316).copy(alpha = 0.05f))
                    .border(1.dp, Color(0xFFF97316).copy(alpha = 0.15f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                    Text("🕐 Vrat/Fast — $fastNote", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFEA580C).copy(alpha = 0.9f), fontSize = 11.sp))
                }
            }

            // Grahan conflict — always visible
            if (grahanNotes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🌑 Eclipse Conflict", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold))
                    grahanNotes.forEach { note ->
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFEF4444).copy(alpha = 0.05f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                            Text(note, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFEF4444).copy(alpha = 0.85f), fontSize = 11.sp))
                        }
                    }
                }
            }

            // Significance
            if (!significance.isNullOrBlank()) {
                Text(significance, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
            }

            // Other Vedic notes — collapsible
            if (otherNotes.isNotEmpty()) {
                Row(
                    modifier = Modifier.clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = BrahmGold.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    Text(if (expanded) "Hide Vedic Notes" else "Vedic Notes", style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold.copy(alpha = 0.8f), fontSize = 10.sp))
                }
                AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        otherNotes.forEach { note ->
                            val isBhadra = note.contains("Bhadra", ignoreCase = true)
                            val isOk = note.startsWith("✅")
                            val (nbg, nborder, nfg) = when {
                                isBhadra && !isOk -> Triple(Color(0xFFF59E0B).copy(alpha = 0.05f), Color(0xFFF59E0B).copy(alpha = 0.2f), Color(0xFF92400E))
                                isOk -> Triple(Color(0xFF22C55E).copy(alpha = 0.05f), Color(0xFF22C55E).copy(alpha = 0.2f), Color(0xFF166534))
                                else -> Triple(Color(0xFFF8F9FA), BrahmBorder, BrahmMutedForeground)
                            }
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(nbg).border(1.dp, nborder, RoundedCornerShape(8.dp)).padding(8.dp)) {
                                Text(note, style = MaterialTheme.typography.bodySmall.copy(color = nfg, fontSize = 11.sp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TithiTypeBadge(type: String) {
    val (bg, fg) = when (type) {
        "vridhi" -> Color(0xFF3B82F6).copy(alpha = 0.1f) to Color(0xFF1D4ED8)
        else     -> Color(0xFFF97316).copy(alpha = 0.1f) to Color(0xFFEA580C)
    }
    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(bg).padding(horizontal = 5.dp, vertical = 2.dp)) {
        Text(if (type == "vridhi") "Vridhi" else "Ksheya", style = MaterialTheme.typography.labelSmall.copy(color = fg, fontSize = 9.sp))
    }
}

// ─── Grahan Card (Eclipse) ────────────────────────────────────────────────────

private val ECLIPSE_GUIDANCE = mapOf(
    "Total Solar"   to listOf("Chant Surya mantra 108 times", "Fast during eclipse", "Donate wheat and copper", "Perform Surya Tarpan after Moksha"),
    "Annular Solar" to listOf("Recite Aditya Hridaya Stotra", "Avoid new ventures", "Bathe before and after", "Donate to charity"),
    "Partial Solar" to listOf("Observe silence", "Chant Gayatri Mantra", "Avoid cooking", "Sprinkle Ganga jal"),
    "Total Lunar"   to listOf("Chant Vishnu Sahasranama", "Meditate near water", "Avoid eating during Sutak", "Take bath after eclipse"),
    "Partial Lunar" to listOf("Reflect and introspect", "Light a ghee lamp", "Recite mantras"),
    "Penumbral"     to listOf("Heightened meditation time", "Avoid major decisions", "Chant mantras"),
)

@Composable
private fun GrahanCard(eclipse: JsonObject) {
    val type      = eclipse["type"]?.jsonPrimitive?.contentOrNull ?: "Eclipse"
    val date      = eclipse["date"]?.jsonPrimitive?.contentOrNull ?: "—"
    val nakshatra = eclipse["nakshatra"]?.jsonPrimitive?.contentOrNull
    val rashi     = eclipse["rashi"]?.jsonPrimitive?.contentOrNull
    val sparsha   = eclipse["sparsha"]?.jsonPrimitive?.contentOrNull ?: eclipse["time"]?.jsonPrimitive?.contentOrNull ?: "—"
    val madhya    = eclipse["madhya"]?.jsonPrimitive?.contentOrNull
    val moksha    = eclipse["moksha"]?.jsonPrimitive?.contentOrNull
    val sutakStart = eclipse["sutak_start"]?.jsonPrimitive?.contentOrNull
    val sutakHours = eclipse["sutak_hours"]?.jsonPrimitive?.intOrNull ?: eclipse["sutak_hours"]?.jsonPrimitive?.doubleOrNull?.toInt()
    val visible   = eclipse["visible_india"]?.jsonPrimitive?.contentOrNull
    val festConflicts = try { eclipse["festival_conflict"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList() } catch (_: Exception) { emptyList<String>() }
    val hasSutak  = sutakStart != null && (sutakHours ?: 0) > 0
    val isSolar   = type.contains("Solar", ignoreCase = true)
    val color     = if (isSolar) Color(0xFFF59E0B) else Color(0xFF3B82F6)
    val guidance  = ECLIPSE_GUIDANCE.entries.find { type.contains(it.key, ignoreCase = true) }?.value
        ?: if (isSolar) ECLIPSE_GUIDANCE["Partial Solar"]!! else ECLIPSE_GUIDANCE["Partial Lunar"]!!

    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, color.copy(alpha = 0.4f)),
    ) {
        Column {
            // Top accent strip
            Box(Modifier.fillMaxWidth().height(4.dp).background(Brush.horizontalGradient(
                if (isSolar) listOf(Color(0xFFF59E0B), Color(0xFFEA580C)) else listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
            )))
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (isSolar) "☀️" else "🌕", fontSize = 28.sp)
                    Column(Modifier.weight(1f)) {
                        Text("$type Eclipse", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text("${if (isSolar) "Surya Grahan" else "Chandra Grahan"}${if (!nakshatra.isNullOrBlank()) " · $nakshatra" else ""}${if (!rashi.isNullOrBlank()) " ($rashi)" else ""}",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.1f)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                            Text(if (isSolar) "Solar" else "Lunar", style = MaterialTheme.typography.labelSmall.copy(color = color, fontWeight = FontWeight.SemiBold))
                        }
                        if (festConflicts.isNotEmpty()) {
                            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFEF4444).copy(alpha = 0.1f)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                                Text("⚠ Conflict", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFDC2626), fontSize = 9.sp))
                            }
                        }
                    }
                }

                // Date
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.CalendarToday, null, tint = color.copy(alpha = 0.6f), modifier = Modifier.size(13.dp))
                    Text(date, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                    if (!visible.isNullOrBlank()) {
                        val visColor = if (visible == "true") Color(0xFF15803D) else BrahmMutedForeground
                        Text(if (visible == "true") "· Visible in India" else "· Not visible in India",
                            style = MaterialTheme.typography.bodySmall.copy(color = visColor, fontSize = 11.sp))
                    }
                }

                // Sparsha / Madhya / Moksha
                if (madhya != null && moksha != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("Sparsha", sparsha, color),
                            Triple("Madhya",  madhya, BrahmGold),
                            Triple("Moksha",  moksha, Color(0xFF22C55E)),
                        ).forEach { (label, time, c) ->
                            Column(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(c.copy(alpha = 0.07f)).padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(time, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = c, fontFamily = FontFamily.Monospace))
                                Text(label, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
                            }
                        }
                    }
                } else {
                    PRow("First Contact (Sparsha)", sparsha)
                }

                // Sutak
                if (hasSutak) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.05f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Default.AccessTime, null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                        Text(
                            buildString {
                                append("⏳ Sutak: "); append(sutakStart)
                                if (sutakHours != null) append(" (${sutakHours}h before Sparsha)")
                            },
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDC2626), fontSize = 11.sp),
                        )
                    }
                } else {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF8F9FA)).padding(8.dp)) {
                        Text("ℹ️ No Sutak — Penumbral eclipse (traditional practice varies)", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
                    }
                }

                // Festival conflicts
                if (festConflicts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🎪 Nearby Festival Impact", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold))
                        festConflicts.forEach { msg ->
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF59E0B).copy(alpha = 0.05f))
                                .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                                Text(msg, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF92400E), fontSize = 11.sp))
                            }
                        }
                    }
                }

                // Spiritual guidance — collapsible
                Row(
                    modifier = Modifier.clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    Text(if (expanded) "Hide Spiritual Guidance" else "Spiritual Guidance & Remedies",
                        style = MaterialTheme.typography.labelSmall.copy(color = color.copy(alpha = 0.8f), fontSize = 10.sp))
                }
                AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        HorizontalDivider(color = color.copy(alpha = 0.2f))
                        Spacer(Modifier.height(2.dp))
                        guidance.forEach { item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("•", color = color, style = MaterialTheme.typography.bodySmall)
                                Text(item, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, fontSize = 11.sp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Small helpers ────────────────────────────────────────────────────────────

@Composable
private fun ExplainBox(bg: Color, border: Color, boldLabel: String, text: String, textColor: Color) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(bg).padding(8.dp)) {
        Text(
            text = boldLabel + text,
            style = MaterialTheme.typography.bodySmall.copy(color = textColor, fontSize = 11.sp),
        )
    }
}

@Composable
private fun SummaryBox(labelColor: Color, bg: Color, boldLabel: String, text: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(bg).padding(8.dp)) {
        Text(
            text = boldLabel + text,
            style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp),
        )
    }
}

@Composable
private fun PRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
    }
}
