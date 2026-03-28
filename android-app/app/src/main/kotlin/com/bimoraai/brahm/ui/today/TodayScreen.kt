package com.bimoraai.brahm.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.*
import com.bimoraai.brahm.core.data.CitySearchViewModel
import com.bimoraai.brahm.core.theme.*
import com.bimoraai.brahm.ui.main.Route
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── Knowledge maps (ported from website) ────────────────────────────────────

private data class AngaExplain(val what: String, val why: String, val doWhat: String)
private val ANGA_EXPLAIN = mapOf(
    "Tithi" to AngaExplain(
        "Tithi is the lunar day — calculated from the angle between the Sun and Moon. Every 12° of difference = 1 tithi. There are 30 tithis in a lunar month.",
        "Each tithi has a ruling deity and energy. It determines which activities are naturally supported — fasting, worship, travel, business, or rest.",
        "Pratipada: good for starting new work. Panchami/Dashami: auspicious for learning. Ekadashi: fast day, Vishnu worship. Purnima: powerful for charity. Amavasya: ancestor rites.",
    ),
    "Nakshatra" to AngaExplain(
        "The 27 lunar mansions (star clusters) through which the Moon passes. The Moon stays in each nakshatra for about 1 day.",
        "The Moon's nakshatra affects your emotional state, intuition, and the type of energy available for activities. Each has a ruling deity and planet.",
        "Ashwini, Pushya, Hasta, Rohini, Mrigashirsha: highly auspicious for new starts. Moola, Jyeshtha, Ashlesha, Magha: avoid major beginnings.",
    ),
    "Yoga" to AngaExplain(
        "Yoga is calculated by adding the longitudes of Sun and Moon, then dividing by 13°20'. There are 27 yogas, each with a specific quality.",
        "Yoga reflects the combined energy of the day — whether the universe supports success, obstacles, progress, or caution.",
        "Auspicious yogas (Siddhi, Shubha, Amrita, Brahma, Indra): begin important work. Inauspicious yogas (Vishkambha, Atiganda, Shula, Ganda): prefer spiritual practices.",
    ),
    "Karana" to AngaExplain(
        "Karana is half of a tithi — it changes approximately every 6 hours. There are 11 karanas, with Vishti (Bhadra) being the most inauspicious.",
        "Karana determines the quality of short time windows during the day. Important for choosing the right hour for specific tasks.",
        "Bhadra active: avoid starting journeys, new work, signing contracts. Bava, Balava, Kaulava: especially auspicious.",
    ),
    "Vara (Day)" to AngaExplain(
        "Vara is the weekday per the Hindu calendar, each ruled by a planet. Ravivara=Sun, Somavara=Moon, Mangalavara=Mars, Budhavara=Mercury, Guruvara=Jupiter, Shukravara=Venus, Shanivara=Saturn.",
        "The ruling planet of the day influences which activities are naturally supported and which deities are best worshipped.",
        "Sunday: Surya puja. Monday: Shiva puja. Tuesday: Hanuman puja. Wednesday: Ganesha. Thursday: Guru puja, start learning. Friday: Lakshmi puja. Saturday: Shani puja.",
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
        "Offer Surya Arghya (water to the Sun). Begin your day's important activities after sunrise — this is when the day's energy officially starts.",
    ),
    "Abhijit Muhurta" to TimingExplain(
        "The most powerful muhurta of the day — a 48-minute window centered exactly at solar noon. 'Abhijit' means 'victorious'.",
        "Start the most important task of your day here. Business meetings, signing agreements, beginning long journeys, important decisions.",
        "Not effective on Wednesdays (Mercury's day weakens Abhijit). Avoid for north-facing activities.",
    ),
    "Rahu Kaal" to TimingExplain(
        "An inauspicious 90-minute period each day, ruled by Rahu (the shadow planet / north lunar node). Each weekday has a fixed slot assigned to Rahu.",
        "Use this time for spiritual practice, reading, meditation, or routine work that doesn't need an auspicious start.",
        "Starting new work, travel, medical procedures, business deals, signing contracts.",
    ),
    "Yamagandam" to TimingExplain(
        "Inauspicious period governed by Yama — the deity of death and dharma. Like Rahu Kaal, it's a fixed segment of each weekday.",
        "Ancestor worship (Pitra Tarpan) is actually enhanced during this period. Routine tasks, spiritual practices.",
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
        "Shiva worship and Abhishek. Lighting oil/ghee lamps. Evening prayers. The Pradosh Vrat is observed on Shukla/Krishna Trayodashi.",
        "Conflict, arguments, heavy meals. This is a Sattvic (pure) time meant for devotion.",
    ),
    "Nishita Kaal" to TimingExplain(
        "The midnight period — the 8th muhurta of the night, centered at astronomical midnight. Sacred for Kali, Shiva, and birth time of Lord Krishna (Janmashtami).",
        "Advanced Tantric practices, Kali/Shiva worship, deep meditation.",
        "Eating, worldly activities. Only advanced practitioners use this for sadhana.",
    ),
)

private data class ChogExplain(val what: String, val good: String, val avoid: String)
private val CHOG_EXPLAIN = mapOf(
    "Amrit" to ChogExplain("Amrit means 'nectar of immortality'. The most auspicious Choghadiya period — ruled by the Moon.", "All activities: new work, travel, business, marriage, medical treatment. Start anything important here.", "Nothing — this is universally favorable."),
    "Shubh"  to ChogExplain("Shubh means 'auspicious'. Ruled by Jupiter (Guru). Second-best Choghadiya period.", "Religious ceremonies, weddings, new learning, creative work, socializing, buying valuables.", "Generally none. Slightly less suited for purely material transactions."),
    "Labh"   to ChogExplain("Labh means 'profit or gain'. Ruled by Mercury (Budha). Strong for material success.", "Business deals, trade, financial investments, buying property, opening shops, sales.", "Spiritual rituals are better in other periods."),
    "Char"   to ChogExplain("Char means 'moving or dynamic'. Ruled by Venus (Shukra). Good for anything involving movement.", "Travel, changing homes/offices, starting journeys, outdoor activities, dynamic work.", "Signing long-term static commitments."),
    "Rog"    to ChogExplain("Rog means 'disease or illness'. Ruled by Mars (Mangal). Generally inauspicious.", "Medical treatment, surgery (Mars governs surgeons). Administrative/government work.", "New business, travel, weddings, buying property, signing contracts."),
    "Kaal"   to ChogExplain("Kaal means 'death or time'. Ruled by Saturn (Shani). Most inauspicious Choghadiya.", "Iron/metal work, collecting dues, cemetery-related work per some traditions.", "All auspicious activities: new starts, travel, celebrations, business deals."),
    "Udveg"  to ChogExplain("Udveg means 'anxiety or disturbance'. Ruled by the Sun. Inauspicious for most activities.", "Government work, legal matters, dealing with authority figures (Sun rules power).", "New ventures, travel, celebrations, financial deals."),
)

// ─── Quick-access chips ───────────────────────────────────────────────────────

private data class QuickItem(val icon: ImageVector, val gs: Color, val ge: Color, val title: String, val route: String, val useTab: Boolean = false)
private val QUICK_ITEMS = listOf(
    QuickItem(Icons.AutoMirrored.Filled.Chat, Color(0xFF6C63FF), Color(0xFF3B2FBF), "Brahm AI",   "tab_chat",    useTab = true),
    QuickItem(Icons.Default.Stars,            Color(0xFFD4A017), Color(0xFF9A6E00), "My Kundali", "tab_kundali", useTab = true),
    QuickItem(Icons.Default.CalendarViewDay,  Color(0xFF0288D1), Color(0xFF01579B), "Panchang",   Route.PANCHANG),
    QuickItem(Icons.Default.NightsStay,       Color(0xFF1A237E), Color(0xFF0D1442), "Live Sky",   Route.SKY),
    QuickItem(Icons.Default.PanTool,          Color(0xFF8B5CF6), Color(0xFF5B21B6), "Palmistry",  Route.PALMISTRY),
    QuickItem(Icons.Default.Favorite,         Color(0xFFE8445A), Color(0xFFB0203A), "Compat.",    Route.COMPATIBILITY),
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun TodayScreen(
    navController: NavController,
    onNavigateTab: (String) -> Unit = {},
    vm: TodayViewModel = hiltViewModel(),
    cityVm: CitySearchViewModel = hiltViewModel(),
) {
    val panchang      by vm.panchang.collectAsState()
    val isLoading     by vm.isLoading.collectAsState()
    val error         by vm.error.collectAsState()
    val userName      by vm.userName.collectAsState()
    val smartAlert    by vm.smartAlert.collectAsState()
    val hasBirthData  by vm.hasBirthData.collectAsState()
    val todayEvents   by vm.todayEvents.collectAsState()
    val panchangCity  by vm.panchangCity.collectAsState()
    val citySuggestions by cityVm.suggestions.collectAsState()
    val cityQuery     by cityVm.cityQuery.collectAsState()

    // Live clock
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); now = System.currentTimeMillis() } }
    val timeStr = remember(now) {
        val cal = java.util.Calendar.getInstance()
        "%02d:%02d:%02d".format(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), cal.get(java.util.Calendar.SECOND))
    }
    val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))

    // Expand state for anga / timing / chog cards
    var expandedKey by remember { mutableStateOf<String?>(null) }
    val toggle: (String) -> Unit = { k -> expandedKey = if (expandedKey == k) null else k }

    // Choghadiya tab — auto-detect day/night from current time vs sunrise/sunset
    val autoChogDay = remember(panchang) {
        val sunriseStr = panchang?.get("sunrise")?.jsonPrimitive?.contentOrNull ?: ""
        val sunsetStr  = panchang?.get("sunset")?.jsonPrimitive?.contentOrNull ?: ""
        fun parseHhmm(s: String): Int? {
            val clean = s.trim().uppercase()
            return try {
                val ampm = when {
                    clean.endsWith("AM") -> 0
                    clean.endsWith("PM") -> 1
                    else -> -1
                }
                val time = clean.replace("AM", "").replace("PM", "").trim()
                val parts = time.split(":")
                var h = parts[0].toInt(); val m = parts[1].toInt()
                if (ampm == 0 && h == 12) h = 0
                if (ampm == 1 && h != 12) h += 12
                h * 60 + m
            } catch (_: Exception) { null }
        }
        val now = java.util.Calendar.getInstance().let { it.get(java.util.Calendar.HOUR_OF_DAY) * 60 + it.get(java.util.Calendar.MINUTE) }
        val sunriseMin = parseHhmm(sunriseStr)
        val sunsetMin  = parseHhmm(sunsetStr)
        if (sunriseMin != null && sunsetMin != null) {
            now in sunriseMin until sunsetMin
        } else {
            now in 360 until 1080  // fallback: 6 AM – 6 PM
        }
    }
    var chogDay by remember(autoChogDay) { mutableStateOf(autoChogDay) }

    // City picker dialog
    var showCityPicker by remember { mutableStateOf(false) }

    val p = panchang

    // Inline city search — shown when user taps location pin
    if (showCityPicker) {
        Dialog(onDismissRequest = { showCityPicker = false; cityVm.cityQuery.value = "" }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BrahmCard,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LocationOn, null, tint = BrahmGold, modifier = Modifier.size(18.dp))
                        Text("Panchang Location", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = BrahmForeground))
                    }
                    OutlinedTextField(
                        value = cityQuery,
                        onValueChange = { cityVm.cityQuery.value = it },
                        placeholder = { Text("Search city...", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold, unfocusedBorderColor = BrahmBorder),
                    )
                    citySuggestions.forEach { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    vm.setCity(city.name, city.lat, city.lon, city.tz)
                                    showCityPicker = false
                                    cityVm.cityQuery.value = ""
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = BrahmGold, modifier = Modifier.size(14.dp))
                            Column {
                                Text(city.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                if (city.country.isNotBlank()) Text(city.country, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
                            }
                        }
                    }
                }
            }
        }
    }

    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {

        // ── 1. Greeting header ───────────────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Namaste${if (!userName.isNullOrBlank()) ", $userName" else ""}! 🙏",
                    style = MaterialTheme.typography.headlineSmall.copy(color = BrahmGold, fontWeight = FontWeight.Bold),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(dateStr, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    Text("·", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    Text(timeStr, style = MaterialTheme.typography.bodySmall.copy(color = BrahmGold, fontWeight = FontWeight.SemiBold))
                }
            }
        }

        // ── 2. Complete Profile nudge ────────────────────────────────────────
        if (!hasBirthData) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = BorderStroke(1.dp, BrahmGold.copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(BrahmGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Default.PersonAdd, null, tint = BrahmGold, modifier = Modifier.size(20.dp)) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Complete your profile", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Add birth details to unlock personalized predictions", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        }
                        Button(
                            onClick = { navController.navigate(Route.PROFILE) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrahmGold),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        ) { Text("Setup", style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold)) }
                    }
                }
            }
        }

        // ── 3. Smart Alert ───────────────────────────────────────────────────
        smartAlert?.let { alert ->
            item { SmartAlertCard(alert = alert, onClick = { navController.navigate(alert.route) }) }
        }

        // ── 4. Get Started chips ─────────────────────────────────────────────
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TodaySectionHeader("🚀 Get Started")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(QUICK_ITEMS) { q ->
                        QuickChip(
                            item = q,
                            onClick = { if (q.useTab) onNavigateTab(q.route) else navController.navigate(q.route) },
                        )
                    }
                }
            }
        }

        // ── 5. Loading / Error ───────────────────────────────────────────────
        if (isLoading && p == null) {
            item { PanchangSkeleton() }
        }
        error?.let { err ->
            item { BrahmErrorView(message = err, onRetry = { vm.load() }) }
        }

        // ── 5. Panchang content (when loaded) ────────────────────────────────
        if (p != null) {

            // ── 5a. Paksha banner ────────────────────────────────────────────
            item {
                val tithiname  = p.nested("tithi", "name")
                val paksha     = p.nested("tithi", "paksha")
                val pakshaIcon = if (paksha == "Shukla") "○" else "●"
                val tithiType  = p.nested("tithi", "tithi_type")
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrahmGold.copy(alpha = 0.06f))
                        .border(1.dp, BrahmGold.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("$pakshaIcon $paksha Paksha · $tithiname", style = MaterialTheme.typography.bodyMedium.copy(color = BrahmGold, fontWeight = FontWeight.SemiBold))
                    if (tithiType == "vridhi" || tithiType == "ksheya") {
                        Spacer(Modifier.width(8.dp))
                        val badgeColor = if (tithiType == "vridhi") Color(0xFFF59E0B) else Color(0xFFEF4444)
                        Box(
                            Modifier.clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.12f))
                                .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(if (tithiType == "vridhi") "Vridhi" else "Ksheya", style = MaterialTheme.typography.labelSmall.copy(color = badgeColor, fontSize = 10.sp))
                        }
                    }
                }
            }

            // ── 5b. Panch Anga section ───────────────────────────────────────
            item {
                TodaySectionHeader("🌿 Panch Anga — Five Elements of the Day")
                Spacer(Modifier.height(2.dp))
                Text("Tap any card to learn what it means and what to do", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground.copy(alpha = 0.6f), fontSize = 10.sp))
            }

            // Build anga list
            val angaItems = buildList {
                val tithi     = p["tithi"]?.jsonObjectOrNull
                val nakshatra = p["nakshatra"]?.jsonObjectOrNull
                val yoga      = p["yoga"]?.jsonObjectOrNull
                val karana    = p["karana"]?.jsonObjectOrNull
                val vara      = p["vara"]?.jsonObjectOrNull
                if (tithi != null) add(AngaItem(
                    label = "Tithi", hindi = "तिथि", icon = "☽",
                    value = "${tithi.str("paksha")} ${tithi.str("name")}",
                    sub   = "Ends: ${tithi.str("end_time")}${if (tithi.str("tithi_type") != "normal" && tithi.str("tithi_type") != "—") " · ${tithi.str("tithi_type")}" else ""}",
                    quality = "neutral",
                ))
                if (nakshatra != null) add(AngaItem(
                    label = "Nakshatra", hindi = "नक्षत्र", icon = "⭐",
                    value = nakshatra.str("name"),
                    sub   = "${nakshatra.str("hindi")} · Pada ${nakshatra.str("pada")} · Lord: ${nakshatra.str("lord")}${if (nakshatra.str("end_time") != "—") " · Ends: ${nakshatra.str("end_time")}" else ""}",
                    quality = "benefic",
                ))
                if (yoga != null) add(AngaItem(
                    label = "Yoga", hindi = "योग", icon = "🔮",
                    value = yoga.str("name"),
                    sub   = "${yoga.str("hindi")}${if (yoga.str("end_time") != "—") " · Ends: ${yoga.str("end_time")}" else ""}",
                    quality = if (yoga["is_auspicious"]?.jsonPrimitive?.booleanOrNull == true) "benefic" else "malefic",
                ))
                if (karana != null) add(AngaItem(
                    label = "Karana", hindi = "करण", icon = "📐",
                    value = karana.str("name"),
                    sub   = "${karana.str("hindi")}${if (karana["is_bhadra"]?.jsonPrimitive?.booleanOrNull == true) " · ⚠ Bhadra — avoid new starts" else ""}",
                    quality = if (karana["is_bhadra"]?.jsonPrimitive?.booleanOrNull == true) "malefic" else "neutral",
                ))
                if (vara != null) add(AngaItem(
                    label = "Vara (Day)", hindi = "वार", icon = "📅",
                    value = vara.str("name"),
                    sub   = "${vara.str("hindi")} · Lord: ${vara.str("lord")}",
                    quality = "neutral",
                ))
            }
            items(angaItems) { item ->
                AngaCard(item = item, expanded = expandedKey == item.label, onToggle = { toggle(item.label) })
            }

            // ── 5c. Today's Festivals & Events ──────────────────────────────
            if (todayEvents.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(2.dp))
                    TodaySectionHeader("🎉 Today's Festivals & Events")
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        todayEvents.forEach { event ->
                            val name    = event.str("name")
                            val emoji   = event.str("emoji").takeIf { it != "—" } ?: "🪔"
                            val desc    = event.str("description").takeIf { it != "—" }
                            val notes   = event["dosh_notes"]?.jsonArrayOrNull
                                ?.mapNotNull { it.jsonPrimitive?.contentOrNull }
                                ?.filter { it.isNotBlank() } ?: emptyList()
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                                border = BorderStroke(1.dp, BrahmGold.copy(alpha = 0.3f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Text(emoji, fontSize = 28.sp)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = BrahmForeground))
                                        if (!desc.isNullOrBlank()) {
                                            Text(desc, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp), maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        }
                                        notes.forEach { note ->
                                            Text("⚠ $note", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDC2626), fontSize = 10.sp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 5d. Daily Timings section ────────────────────────────────────
            item {
                Spacer(Modifier.height(2.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TodaySectionHeader("🕐 Daily Timings")
                    // Location indicator — tap to change city
                    Row(
                        modifier = Modifier
                            .clickable { showCityPicker = true }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(Icons.Default.LocationOn, null, tint = BrahmGold, modifier = Modifier.size(14.dp))
                        Text(
                            panchangCity?.name?.substringBefore(",")?.take(14) ?: "Set Location",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontSize = 11.sp),
                        )
                    }
                }
            }

            // Build timings list
            val timings = buildList<TimingItem> {
                p["brahma_muhurta"]?.jsonObjectOrNull?.let { add(TimingItem("🌄", "Brahma Muhurta", "${it.str("start")} – ${it.str("end")}", "benefic")) }
                p.str("sunrise").takeIf { it != "—" }?.let { add(TimingItem("↑", "Sunrise", it, "benefic")) }
                p["abhijit_muhurta"]?.jsonObjectOrNull?.let { add(TimingItem("✨", "Abhijit Muhurta", "${it.str("start")} – ${it.str("end")}", "special")) }
                p["rahukaal"]?.jsonObjectOrNull?.let { add(TimingItem("⚠️", "Rahu Kaal", "${it.str("start")} – ${it.str("end")}", "malefic")) }
                p["yamagandam"]?.jsonObjectOrNull?.let { add(TimingItem("💀", "Yamagandam", "${it.str("start")} – ${it.str("end")}", "malefic")) }
                p["gulika_kaal"]?.jsonObjectOrNull?.let { add(TimingItem("🌀", "Gulika Kaal", "${it.str("start")} – ${it.str("end")}", "malefic")) }
                p.str("sunset").takeIf { it != "—" }?.let { add(TimingItem("🌇", "Sunset", it, "neutral")) }
                p["pradosh_kaal"]?.jsonObjectOrNull?.let { add(TimingItem("🌆", "Pradosh Kaal", "${it.str("start")} – ${it.str("end")}", "special")) }
                p["nishita_kaal"]?.jsonObjectOrNull?.let {
                    val mid = it.str("midpoint").let { m -> if (m != "—") " (mid: $m)" else "" }
                    add(TimingItem("●", "Nishita Kaal$mid", "${it.str("start")} – ${it.str("end")}", "special"))
                }
            }
            items(timings) { t ->
                TimingRow(item = t, expanded = expandedKey == "t_${t.label}", onToggle = { toggle("t_${t.label}") })
            }

            // ── 5e. Choghadiya section ───────────────────────────────────────
            val choghadiya = p["choghadiya"]?.jsonObjectOrNull
            if (choghadiya != null) {
                item {
                    Spacer(Modifier.height(2.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        TodaySectionHeader("⏱ Choghadiya")
                        // Day / Night toggle
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, BrahmBorder, RoundedCornerShape(8.dp)),
                        ) {
                            ChogTabBtn("☉ Day",   chogDay) { chogDay = true }
                            ChogTabBtn("☽ Night", !chogDay) { chogDay = false }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Tap a period to see what's good or bad in that window", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground.copy(alpha = 0.6f), fontSize = 10.sp))
                }

                // Choghadiya grid
                val chogList = (if (chogDay) choghadiya["day"] else choghadiya["night"])
                    ?.jsonArrayOrNull?.mapNotNull { it.jsonObjectOrNull } ?: emptyList()

                chogList.chunked(2).forEachIndexed { rowIdx, pair ->
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEachIndexed { colIdx, chog ->
                                val key = "chog_${if (chogDay) "day" else "night"}_${rowIdx * 2 + colIdx}"
                                ChogCard(
                                    chog = chog,
                                    expanded = expandedKey == key,
                                    onToggle = { toggle(key) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── 5f. Today's Summary ──────────────────────────────────────────
            item {
                Spacer(Modifier.height(2.dp))
                // Dynamic header based on today's events
                val summaryTitle = when {
                    todayEvents.any { it.str("name").contains("Grahan", ignoreCase = true) ||
                            it.str("name").contains("Eclipse", ignoreCase = true) } -> "● Eclipse Day"
                    todayEvents.isNotEmpty() -> "🎊 ${todayEvents.first().str("name")} — Today's Summary"
                    else -> "✨ Today's Summary"
                }
                TodaySectionHeader(summaryTitle)
            }
            item {
                val yoga         = p["yoga"]?.jsonObjectOrNull
                val isAuspicious = yoga?.get("is_auspicious")?.jsonPrimitive?.booleanOrNull == true
                val yogaName     = yoga?.str("name") ?: "—"
                val abhijit      = p["abhijit_muhurta"]?.jsonObjectOrNull
                val rahu         = p["rahukaal"]?.jsonObjectOrNull
                val yama         = p["yamagandam"]?.jsonObjectOrNull
                val brahma       = p["brahma_muhurta"]?.jsonObjectOrNull
                val panchaka     = p["panchaka"]?.jsonPrimitive?.booleanOrNull == true
                val hasEclipse   = todayEvents.any { it.str("name").contains("Grahan", ignoreCase = true) || it.str("name").contains("Eclipse", ignoreCase = true) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Yoga quality row (single full-width)
                    SummaryBox(
                        modifier = Modifier.fillMaxWidth(),
                        bg = if (isAuspicious) Color(0xFF22C55E).copy(alpha = 0.06f) else Color(0xFFF59E0B).copy(alpha = 0.06f),
                        border = if (isAuspicious) Color(0xFF22C55E).copy(alpha = 0.25f) else Color(0xFFF59E0B).copy(alpha = 0.25f),
                        label = "$yogaName Yoga",
                        labelColor = if (isAuspicious) Color(0xFF15803D) else Color(0xFF92400E),
                        text = if (isAuspicious) "Auspicious energy today. Good for important work and new beginnings."
                               else "Inauspicious yoga. Prefer spiritual practices over major decisions today.",
                    )
                    // Festival greeting
                    if (todayEvents.isNotEmpty() && !hasEclipse) {
                        val festName = todayEvents.first().str("name")
                        val festEmoji = todayEvents.first().str("emoji").takeIf { it != "—" } ?: "🪔"
                        SummaryBox(
                            modifier = Modifier.fillMaxWidth(),
                            bg = Color(0xFFFFF7ED).copy(alpha = 0.8f),
                            border = BrahmGold.copy(alpha = 0.3f),
                            label = "$festEmoji Happy $festName!",
                            labelColor = BrahmGold,
                            text = "May this auspicious occasion bring joy, peace, and blessings to you and your family. 🙏",
                        )
                    }
                    if (hasEclipse) {
                        SummaryBox(
                            modifier = Modifier.fillMaxWidth(),
                            bg = Color(0xFF1E1B4B).copy(alpha = 0.05f),
                            border = Color(0xFF6366F1).copy(alpha = 0.3f),
                            label = "● Grahan Day — Special observances apply",
                            labelColor = Color(0xFF4F46E5),
                            text = "Avoid auspicious ceremonies. Chant mantras, fast, and avoid eating during eclipse. Add Tulsi to stored water.",
                        )
                    }
                    // Best window (single row)
                    if (abhijit != null) {
                        SummaryBox(
                            modifier = Modifier.fillMaxWidth(),
                            bg = Color(0xFF22C55E).copy(alpha = 0.05f),
                            border = Color(0xFF22C55E).copy(alpha = 0.2f),
                            label = "✨ Best window — Abhijit Muhurta",
                            labelColor = Color(0xFF15803D),
                            text = "${abhijit.str("start")} – ${abhijit.str("end")} — use for your most important task today.",
                        )
                    }
                    // Rahu Kaal (single row)
                    if (rahu != null) {
                        SummaryBox(
                            modifier = Modifier.fillMaxWidth(),
                            bg = Color(0xFFEF4444).copy(alpha = 0.05f),
                            border = Color(0xFFEF4444).copy(alpha = 0.2f),
                            label = "⚠ Avoid starting new work",
                            labelColor = Color(0xFFDC2626),
                            text = buildString {
                                append("Rahu Kaal ${rahu.str("start")} – ${rahu.str("end")}")
                                if (yama != null) append(" · Yamagandam ${yama.str("start")} – ${yama.str("end")}")
                            },
                        )
                    }
                    // Brahma Muhurta (single row)
                    if (brahma != null) {
                        SummaryBox(
                            modifier = Modifier.fillMaxWidth(),
                            bg = BrahmGold.copy(alpha = 0.05f),
                            border = BrahmGold.copy(alpha = 0.2f),
                            label = "🌄 Brahma Muhurta",
                            labelColor = BrahmGold,
                            text = "${brahma.str("start")} – ${brahma.str("end")} — ideal for meditation and spiritual practice.",
                        )
                    }
                    // Panchaka warning
                    if (panchaka) {
                        SummaryBox(
                            modifier = Modifier.fillMaxWidth(),
                            bg = Color(0xFFEF4444).copy(alpha = 0.05f),
                            border = Color(0xFFEF4444).copy(alpha = 0.2f),
                            label = "⚠ Panchaka active",
                            labelColor = Color(0xFFDC2626),
                            text = "Avoid rooftop construction, cremation rites, and storing firewood today.",
                        )
                    }
                }
            }
        } // end if (p != null)

        item { Spacer(Modifier.height(12.dp)) }
    }
    ScrollToTopFab(listState, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp))
    } // Box
}

// ─── Data models for composables ─────────────────────────────────────────────

private data class AngaItem(
    val label: String, val hindi: String, val icon: String,
    val value: String, val sub: String,
    val quality: String, // "benefic" | "malefic" | "neutral"
)

private data class TimingItem(
    val icon: String, val label: String, val value: String,
    val type: String, // "benefic" | "malefic" | "special" | "neutral"
)

// ─── Composables ──────────────────────────────────────────────────────────────

@Composable
private fun TodaySectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = BrahmForeground))
}

@Composable
private fun AngaCard(item: AngaItem, expanded: Boolean, onToggle: () -> Unit) {
    val qualityColor = when (item.quality) {
        "benefic" -> Color(0xFF22C55E)
        "malefic" -> Color(0xFFEF4444)
        else      -> BrahmMutedForeground
    }
    val exp = ANGA_EXPLAIN[item.label]
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = if (expanded) BorderStroke(1.dp, qualityColor.copy(alpha = 0.35f)) else BorderStroke(1.dp, BrahmBorder),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(item.icon, fontSize = 22.sp, modifier = Modifier.padding(top = 2.dp))
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.label.uppercase(), style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, letterSpacing = 0.5.sp))
                        Text("(${item.hindi})", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                    }
                    Text(item.value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(top = 2.dp))
                    if (item.sub.isNotBlank()) Text(item.sub, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp), modifier = Modifier.padding(top = 1.dp))
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp))
                            .background(qualityColor.copy(alpha = 0.1f))
                            .border(1.dp, qualityColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) { Text(item.quality, style = MaterialTheme.typography.labelSmall.copy(color = qualityColor, fontSize = 9.sp)) }
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = BrahmMutedForeground, modifier = Modifier.size(16.dp),
                    )
                }
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                if (exp != null) {
                    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        HorizontalDivider(color = BrahmBorder)
                        ExplainBox(label = "What is it?", text = exp.what, bg = Color(0xFFF8F9FA), labelColor = BrahmForeground.copy(alpha = 0.7f))
                        ExplainBox(label = "Why it matters?", text = exp.why, bg = BrahmGold.copy(alpha = 0.04f), labelColor = BrahmGold.copy(alpha = 0.8f))
                        ExplainBox(label = "What to do?", text = exp.doWhat, bg = Color(0xFF22C55E).copy(alpha = 0.04f), labelColor = Color(0xFF15803D).copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimingRow(item: TimingItem, expanded: Boolean, onToggle: () -> Unit) {
    val bg = when (item.type) {
        "benefic" -> Color(0xFF22C55E).copy(alpha = 0.05f) to Color(0xFF22C55E).copy(alpha = 0.2f)
        "malefic" -> Color(0xFFEF4444).copy(alpha = 0.05f) to Color(0xFFEF4444).copy(alpha = 0.2f)
        "special" -> BrahmGold.copy(alpha = 0.05f)         to BrahmGold.copy(alpha = 0.2f)
        else      -> Color(0xFFF8F9FA)                     to BrahmBorder
    }
    val textColor = when (item.type) {
        "benefic" -> Color(0xFF15803D)
        "malefic" -> Color(0xFFDC2626)
        "special" -> BrahmGold
        else      -> BrahmMutedForeground
    }
    val expKey = item.label.split(" ").first()
    val exp = TIMING_EXPLAIN.entries.find { item.label.startsWith(it.key) }?.value

    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg.first)
            .border(1.dp, bg.second, RoundedCornerShape(12.dp))
            .clickable { onToggle() },
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(item.icon, fontSize = 16.sp)
                Text(item.label, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground), modifier = Modifier.weight(1f))
                Text(item.value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = textColor))
                if (exp != null) Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null, tint = BrahmMutedForeground, modifier = Modifier.size(14.dp),
                )
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                if (exp != null) {
                    Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        HorizontalDivider(color = bg.second)
                        Spacer(Modifier.height(4.dp))
                        Text(exp.what, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
                        Text("✓ ${exp.doWhat}", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF15803D), fontSize = 11.sp))
                        exp.avoid?.let { Text("✕ $it", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDC2626), fontSize = 11.sp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChogCard(chog: JsonObject, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val name      = chog.str("name")
    val hindi     = chog.str("hindi")
    val start     = chog.str("start")
    val end       = chog.str("end")
    val auspicious = chog["auspicious"]?.jsonPrimitive?.booleanOrNull == true
    val exp       = CHOG_EXPLAIN[name]

    val (bg, border, nameColor) = when (name) {
        "Amrit" -> Triple(Color(0xFF22C55E).copy(alpha = 0.08f), Color(0xFF22C55E).copy(alpha = 0.3f), Color(0xFF15803D))
        "Shubh" -> Triple(Color(0xFF14B8A6).copy(alpha = 0.08f), Color(0xFF14B8A6).copy(alpha = 0.3f), Color(0xFF0D9488))
        "Labh"  -> Triple(Color(0xFF0EA5E9).copy(alpha = 0.08f), Color(0xFF0EA5E9).copy(alpha = 0.3f), Color(0xFF0369A1))
        "Char"  -> Triple(Color(0xFF3B82F6).copy(alpha = 0.08f), Color(0xFF3B82F6).copy(alpha = 0.3f), Color(0xFF1D4ED8))
        "Rog"   -> Triple(Color(0xFFEF4444).copy(alpha = 0.07f), Color(0xFFEF4444).copy(alpha = 0.25f), Color(0xFFDC2626))
        "Kaal"  -> Triple(Color(0xFFE11D48).copy(alpha = 0.07f), Color(0xFFE11D48).copy(alpha = 0.25f), Color(0xFFBE123C))
        "Udveg" -> Triple(Color(0xFFF97316).copy(alpha = 0.07f), Color(0xFFF97316).copy(alpha = 0.25f), Color(0xFFEA580C))
        else    -> Triple(Color(0xFFF8F9FA), BrahmBorder, BrahmMutedForeground)
    }

    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(bg).border(1.dp, border, RoundedCornerShape(12.dp)).clickable { onToggle() },
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = nameColor))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(if (auspicious) "✓" else "✕", style = MaterialTheme.typography.labelSmall.copy(color = if (auspicious) Color(0xFF15803D) else Color(0xFFDC2626), fontSize = 9.sp))
                    if (exp != null) Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = BrahmMutedForeground.copy(alpha = 0.6f), modifier = Modifier.size(12.dp),
                    )
                }
            }
            Text(hindi, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
            Text("$start – $end", style = MaterialTheme.typography.labelSmall.copy(color = nameColor.copy(alpha = 0.8f), fontSize = 10.sp))
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                if (exp != null) {
                    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        HorizontalDivider(color = border)
                        Spacer(Modifier.height(2.dp))
                        Text(exp.what, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
                        Text("✓ ${exp.good}", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF15803D), fontSize = 10.sp))
                        Text("✕ ${exp.avoid}", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDC2626), fontSize = 10.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChogTabBtn(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) BrahmGold.copy(alpha = 0.12f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(
            color = if (selected) BrahmGold else BrahmMutedForeground,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 11.sp,
        ))
    }
}

@Composable
private fun ExplainBox(label: String, text: String, bg: Color, labelColor: Color) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(bg)
            .border(0.dp, Color.Transparent, RoundedCornerShape(8.dp)).padding(8.dp),
    ) {
        Text(
            buildString { append(label); append(" "); append(text) },
            style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp),
        )
    }
}

@Composable
private fun SummaryBox(modifier: Modifier, bg: Color, border: Color, label: String, labelColor: Color, text: String) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(bg).border(1.dp, border, RoundedCornerShape(10.dp)).padding(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("$label: ", style = MaterialTheme.typography.labelSmall.copy(color = labelColor, fontWeight = FontWeight.SemiBold))
            Text(text, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
        }
    }
}

@Composable
private fun SmartAlertCard(alert: SmartAlert, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(alert.icon, fontSize = 20.sp)
            Column(Modifier.weight(1f)) {
                Text(alert.message, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = Color(0xFF92400E)))
            }
            TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                Text(alert.actionLabel, color = BrahmGold, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@Composable
private fun QuickChip(item: QuickItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(14.dp),
        color = Color.White, shadowElevation = 2.dp, modifier = Modifier.width(76.dp),
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(item.gs, item.ge))),
                contentAlignment = Alignment.Center,
            ) { Icon(item.icon, contentDescription = item.title, tint = Color.White, modifier = Modifier.size(17.dp)) }
            Text(item.title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = BrahmForeground, fontSize = 10.sp), maxLines = 1)
        }
    }
}

// ─── Skeleton shimmer ─────────────────────────────────────────────────────────
@Composable
private fun PanchangSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(0.25f, 0.65f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "shimmerAlpha")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(Color.LightGray.copy(alpha = alpha)))
        repeat(3) {
            Box(Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(14.dp)).background(Color.LightGray.copy(alpha = alpha)))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(10.dp)).background(Color.LightGray.copy(alpha = alpha))) }
        }
    }
}

// ─── JsonElement helpers ──────────────────────────────────────────────────────

private val JsonElement.jsonObjectOrNull get(): JsonObject? = try { jsonObject } catch (_: Exception) { null }
private val JsonElement.jsonArrayOrNull  get(): JsonArray?  = try { jsonArray }  catch (_: Exception) { null }
