package com.bimoraai.brahm.ui.sky

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.BrahmErrorView
import com.bimoraai.brahm.core.components.BrahmLoadingSpinner
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.theme.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ── Planet meta ────────────────────────────────────────────────────────────────
private val GRAHA_SYMBOL = mapOf(
    "Surya" to "☉", "Chandra" to "☽", "Mangal" to "♂", "Budh" to "☿",
    "Guru" to "♃", "Shukra" to "♀", "Shani" to "♄", "Rahu" to "☊", "Ketu" to "☋",
)
private val GRAHA_COLOR_MAP = mapOf(
    "Surya"   to Color(0xFFD97706), "Chandra" to Color(0xFF4F46E5), "Mangal" to Color(0xFFDC2626),
    "Budh"    to Color(0xFF16A34A), "Guru"    to Color(0xFFB45309),  "Shukra" to Color(0xFF9333EA),
    "Shani"   to Color(0xFF334155), "Rahu"    to Color(0xFF0369A1),  "Ketu"   to Color(0xFFC2410C),
)
private val GRAHA_NAME_HI = mapOf(
    "Surya" to "सूर्य", "Chandra" to "चंद्र", "Mangal" to "मंगल", "Budh" to "बुध",
    "Guru" to "गुरु", "Shukra" to "शुक्र", "Shani" to "शनि", "Rahu" to "राहु", "Ketu" to "केतु",
)
private val PLANET_DOMAIN = mapOf(
    "Surya" to "Vitality & Soul", "Chandra" to "Mind & Emotions",
    "Mangal" to "Energy & Action", "Budh" to "Intellect & Speech",
    "Guru" to "Wisdom & Fortune", "Shukra" to "Love & Beauty",
    "Shani" to "Discipline & Karma", "Rahu" to "Ambition & Desire", "Ketu" to "Spirituality",
)
private val RASHI_QUALITY = mapOf(
    "Mesha" to "bold & driven", "Vrishabha" to "stable & grounded",
    "Mithuna" to "quick & communicative", "Karka" to "nurturing & sensitive",
    "Simha" to "powerful & radiant", "Kanya" to "precise & analytical",
    "Tula" to "balanced & harmonious", "Vrischika" to "intense & transformative",
    "Dhanu" to "expansive & optimistic", "Makara" to "disciplined & practical",
    "Kumbha" to "innovative & free", "Meena" to "intuitive & spiritual",
)
private val RASHI_NAMES = listOf(
    "Mesha", "Vrishabha", "Mithuna", "Karka",
    "Simha", "Kanya", "Tula", "Vrischika",
    "Dhanu", "Makara", "Kumbha", "Meena",
)
private val RASHI_SHORT = listOf("Mes","Vri","Mit","Kar","Sim","Kan","Tul","Vrc","Dha","Mak","Kum","Mee")

// ── Main Screen ────────────────────────────────────────────────────────────────
@Composable
fun SkyScreen(navController: NavController, vm: SkyViewModel = hiltViewModel()) {
    val snapshot  by vm.snapshot.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error     by vm.error.collectAsState()
    val tick      by vm.tick.collectAsState()

    val timeStr = remember(tick) {
        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    }

    SwipeBackLayout(navController) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Live Sky", fontWeight = FontWeight.Bold)
                            Text("Real-time sidereal positions", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BrahmBackground),
                    actions = {
                        IconButton(onClick = { vm.load() }) {
                            Icon(Icons.Default.Sync, contentDescription = "Refresh", tint = BrahmGold)
                        }
                    },
                )
            },
        ) { padding ->
            when {
                isLoading && snapshot == null -> BrahmLoadingSpinner(modifier = Modifier.fillMaxSize().padding(padding))
                error != null && snapshot == null -> BrahmErrorView(message = error!!, onRetry = { vm.load() }, modifier = Modifier.padding(padding))
                else -> snapshot?.let { snap ->
                    SkyContent(
                        snapshot  = snap,
                        timeStr   = timeStr,
                        vm        = vm,
                        modifier  = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}

// ── Tabs content ───────────────────────────────────────────────────────────────
@Composable
private fun SkyContent(snapshot: SkySnapshot, timeStr: String, vm: SkyViewModel, modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Live Sky", "24h Movement", "Today for You")

    Column(modifier = modifier.fillMaxSize().background(BrahmBackground)) {
        // ── Header row ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    "Live Sky",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = BrahmForeground),
                )
                Text(
                    "Real-time sidereal positions · Updates every second",
                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    timeStr,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        color = BrahmGold,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Text("IST", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
            }
        }

        // ── Quick stats 3-col ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickStatCard(
                modifier = Modifier.weight(1f),
                emoji = "👁",
                label = "Visible",
                value = snapshot.visibleCount.toString(),
                sub = "planets",
                valueColor = Color(0xFF4ADE80),
            )
            QuickStatCard(
                modifier = Modifier.weight(1f),
                emoji = "℞",
                label = "Retro",
                value = snapshot.retroCount.toString(),
                sub = "grahas",
                valueColor = Color(0xFFFBBF24),
            )
            QuickStatCard(
                modifier = Modifier.weight(1f),
                emoji = "L",
                label = "Lagna",
                value = snapshot.lagna?.rashi?.take(5) ?: "—",
                sub = snapshot.lagna?.dms ?: "—",
                valueColor = BrahmGold,
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Tab Row ──
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = BrahmBackground,
            contentColor = BrahmGold,
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    text = { Text(title, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        // ── Tab content ──
        when (selectedTab) {
            0 -> LiveSkyTab(snapshot)
            1 -> Track24hTab(snapshot)
            2 -> TodayForYouTab(snapshot, vm)
        }
    }
}

// ── Tab 1: Live Sky ────────────────────────────────────────────────────────────
@Composable
private fun LiveSkyTab(snapshot: SkySnapshot) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Cosmic energy summary
        item { CosmicSummary(snapshot) }

        // Zodiac wheel card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sidereal Zodiac — Live", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, letterSpacing = 1.sp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4ADE80)))
                            Text("Live", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4ADE80)))
                        }
                    }
                    ZodiacWheel(snapshot)
                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        LegendItem("●", Color(0xFF4ADE80), "Visible")
                        LegendItem("℞", Color(0xFFFBBF24), "Retrograde")
                        LegendItem("●", Color(0xFFFB923C), "Combust")
                        LegendItem("L", BrahmGold, "Lagna")
                    }
                }
            }
        }

        // Graha positions header
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Graha Positions", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, letterSpacing = 1.sp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4ADE80)))
                    Text("Live", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4ADE80)))
                }
            }
        }

        // Planet cards 3-col grid
        item { PlanetCardsGrid(snapshot) }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Tab 2: 24h Movement ───────────────────────────────────────────────────────
@Composable
private fun Track24hTab(snapshot: SkySnapshot) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Midnight countdown + day progress
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Day resets at midnight", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                val h = snapshot.secondsToMidnight / 3600
                val m = (snapshot.secondsToMidnight % 3600) / 60
                val s = snapshot.secondsToMidnight % 60
                Text(
                    "%02d:%02d:%02d".format(h, m, s),
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace, color = BrahmGold, fontWeight = FontWeight.Medium),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Day progress", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                Text(
                    "${(snapshot.dayProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge.copy(color = BrahmForeground, fontWeight = FontWeight.Medium),
                )
                LinearProgressIndicator(
                    progress = { snapshot.dayProgress },
                    modifier = Modifier.width(80.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = BrahmGold,
                    trackColor = Color(0xFFE5E7EB),
                )
            }
        }

        // Moon Track card
        val moon = snapshot.grahas["Chandra"]
        if (moon != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("☽", fontSize = 22.sp, color = GRAHA_COLOR_MAP["Chandra"] ?: BrahmGold)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Moon Track Today", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                "Moves ~13.2° across the sky today",
                                style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Now", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                            Text(
                                "${moon.rashi} ${moon.dms}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = BrahmForeground),
                            )
                        }
                    }
                    // 24h bar visualization
                    Box(
                        Modifier.fillMaxWidth().height(48.dp)
                            .clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F5F5)),
                    ) {
                        // Hour tick labels
                        Row(Modifier.fillMaxSize().padding(horizontal = 0.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            listOf("00", "06", "12", "18", "24").forEach { h ->
                                Text(h, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 8.sp), modifier = Modifier.padding(bottom = 4.dp, start = if (h == "00") 4.dp else 0.dp, end = if (h == "24") 4.dp else 0.dp))
                            }
                        }
                        // Moon gradient track
                        Box(
                            Modifier.fillMaxWidth().height(4.dp).align(Alignment.Center)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(GRAHA_COLOR_MAP["Chandra"]!!.copy(0.2f), GRAHA_COLOR_MAP["Chandra"]!!.copy(0.7f))
                                    )
                                ),
                        )
                        // Current time marker
                        Box(
                            Modifier.fillMaxHeight().width(2.dp)
                                .offset(x = (snapshot.dayProgress * 300).dp.coerceAtMost(300.dp))
                                .background(BrahmGold),
                        )
                    }
                }
            }
        }

        // All planets day range
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "ALL PLANETS — TODAY'S RANGE",
                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, letterSpacing = 1.sp),
                )
                GRAHA_ORDER.forEach { name ->
                    val p = snapshot.grahas[name] ?: return@forEach
                    val color = GRAHA_COLOR_MAP[name] ?: BrahmGold
                    val speed = PLANET_SPEED_PER_DAY[name] ?: 0f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(GRAHA_SYMBOL[name] ?: "", fontSize = 16.sp, color = color, modifier = Modifier.width(20.dp))
                        Column(Modifier.weight(1f)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name, style = MaterialTheme.typography.labelSmall.copy(color = BrahmForeground))
                                Row {
                                    Text("${"%.3f".format(kotlin.math.abs(speed))}°/day", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = BrahmMutedForeground))
                                    if (p.retro) Text(" ℞", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFFBBF24)))
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Box(
                                Modifier.fillMaxWidth().height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)).background(Color(0xFFE5E7EB)),
                            ) {
                                Box(
                                    Modifier.fillMaxWidth(snapshot.dayProgress).fillMaxHeight()
                                        .clip(RoundedCornerShape(2.dp)).background(color.copy(alpha = 0.7f)),
                                )
                            }
                        }
                        Text(
                            "${p.rashi.take(4)} ${p.dms}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = BrahmForeground),
                            modifier = Modifier.width(90.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Tab 3: Today for You ──────────────────────────────────────────────────────
@Composable
private fun TodayForYouTab(snapshot: SkySnapshot, vm: SkyViewModel) {
    val user by vm.userRepository.user.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (user == null || user?.rashi.isNullOrBlank()) {
            // No kundali
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("🌟", fontSize = 32.sp)
                    Text("Generate Your Kundali First", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        "Your personalized transit forecast requires your natal chart. Go to My Kundali to generate it.",
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            return@Column
        }

        val natalMoonRashi  = user!!.rashi         // already English e.g. "Mesha"
        val userName        = user!!.name
        val transitMoonRashi = snapshot.grahas["Chandra"]?.rashi ?: "Mesha"
        val transitSunRashi  = snapshot.grahas["Surya"]?.rashi   ?: "Mesha"

        fun getTransitHouse(transitRashi: String, natalRashi: String): Int {
            val t = RASHI_INDEX[transitRashi] ?: 0
            val n = RASHI_INDEX[natalRashi]   ?: 0
            return ((t - n + 12) % 12) + 1
        }
        fun ordinal(n: Int) = when (n) { 1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> "${n}th" }

        val moonHouse = getTransitHouse(transitMoonRashi, natalMoonRashi)
        val sunHouse  = getTransitHouse(transitSunRashi,  natalMoonRashi)
        val jupHouse  = getTransitHouse(snapshot.grahas["Guru"]?.rashi  ?: "Mesha", natalMoonRashi)
        val satHouse  = getTransitHouse(snapshot.grahas["Shani"]?.rashi ?: "Mesha", natalMoonRashi)
        val overallGood = listOf(3, 4, 5, 9, 10, 11).contains(moonHouse)

        val moonEffect = MOON_TRANSIT_EFFECTS[moonHouse]

        // Day quality banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (overallGood) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, if (overallGood) Color(0xFF86EFAC) else Color(0xFFFDE68A)
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Today's cosmic energy for", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                    Text(userName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        "Janma Rashi: $natalMoonRashi",
                        style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (overallGood) "🌟" else "⚡", fontSize = 28.sp)
                    Text(
                        if (overallGood) "Auspicious day" else "Moderate day",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (overallGood) Color(0xFF16A34A) else Color(0xFFD97706),
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }

        // Moon Transit
        if (moonEffect != null) {
            val qualityColor = when (moonEffect.quality) {
                "good"  -> Color(0xFF4ADE80); "bad" -> Color(0xFFF87171); else -> Color(0xFFFBBF24)
            }
            val qualityBg = when (moonEffect.quality) {
                "good"  -> Color(0xFFF0FDF4); "bad" -> Color(0xFFFEF2F2); else -> Color(0xFFFFFBEB)
            }
            val qualityBorder = when (moonEffect.quality) {
                "good"  -> Color(0xFF86EFAC); "bad" -> Color(0xFFFCA5A5); else -> Color(0xFFFDE68A)
            }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = qualityBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, qualityBorder),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("☽", fontSize = 22.sp, color = GRAHA_COLOR_MAP["Chandra"] ?: BrahmGold)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Moon Transit — House $moonHouse", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                            Box(
                                Modifier.clip(RoundedCornerShape(50.dp)).background(qualityColor.copy(0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    when (moonEffect.quality) { "good" -> "Favorable"; "bad" -> "Caution"; else -> "Mixed" },
                                    style = MaterialTheme.typography.labelSmall.copy(color = qualityColor, fontWeight = FontWeight.SemiBold),
                                )
                            }
                        }
                        Text(moonEffect.text, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                        Text(moonEffect.advice, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        Text(
                            "Moon in $transitMoonRashi transiting your ${ordinal(moonHouse)} from natal Moon ($natalMoonRashi)",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
                        )
                    }
                }
            }
        }

        // Sun Transit
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("☉", fontSize = 22.sp, color = GRAHA_COLOR_MAP["Surya"] ?: BrahmGold)
                Column {
                    Text("Sun Transit — House $sunHouse", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = BrahmForeground))
                    val note = when {
                        listOf(1,4,7,10).contains(sunHouse) -> " Kendra — strong influence on your actions."
                        listOf(5,9).contains(sunHouse)      -> " Trikona — blessings and fortune."
                        listOf(3,6,10,11).contains(sunHouse) -> " Upachaya — growth through effort."
                        else -> ""
                    }
                    Text(
                        "Sun is in $transitSunRashi, transiting your ${ordinal(sunHouse)} house.$note",
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                    )
                }
            }
        }

        // Jupiter & Saturn 2-col grid
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Guru" to jupHouse, "Shani" to satHouse).forEach { (name, house) ->
                val good = listOf(2,5,7,9,11).contains(house)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (good) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (good) Color(0xFF86EFAC) else Color(0xFFFDE68A)
                    ),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(GRAHA_SYMBOL[name] ?: "", fontSize = 16.sp, color = GRAHA_COLOR_MAP[name] ?: BrahmGold)
                            Text(if (name == "Guru") "Jupiter" else "Saturn", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = BrahmForeground))
                        }
                        Text("House $house from your Moon", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                        Text(
                            if (good) "Supportive transit" else "Challenging, builds strength",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (good) Color(0xFF16A34A) else Color(0xFFD97706),
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }
            }
        }

        // Favorable planets (upachaya 3,6,10,11 from natal moon)
        val luckyGrahas = GRAHA_ORDER.filter { name ->
            val rashi = snapshot.grahas[name]?.rashi ?: return@filter false
            listOf(3,6,10,11).contains(getTransitHouse(rashi, natalMoonRashi))
        }
        if (luckyGrahas.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("FAVORABLE PLANETS TODAY", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, letterSpacing = 1.sp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        luckyGrahas.forEach { name ->
                            val color = GRAHA_COLOR_MAP[name] ?: BrahmGold
                            Box(
                                Modifier.clip(RoundedCornerShape(50.dp))
                                    .background(color.copy(0.12f))
                                    .border(1.dp, color.copy(0.4f), RoundedCornerShape(50.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(GRAHA_SYMBOL[name] ?: "", fontSize = 13.sp, color = color)
                                    Text(name, style = MaterialTheme.typography.labelSmall.copy(color = color, fontWeight = FontWeight.SemiBold))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Zodiac Wheel (Canvas) ─────────────────────────────────────────────────────
@Composable
private fun ZodiacWheel(snapshot: SkySnapshot) {
    Canvas(
        modifier = Modifier.size(280.dp),
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val scale = min(w, h) / 400f   // website viewBox is 0 0 400 400
        val R     = 192f * scale
        val INNER = 88f  * scale
        val r45   = 45f  * scale

        val outerPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#F2F4F7")
            style = android.graphics.Paint.Style.FILL
        }
        val outerStroke = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#B0BBC8")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1.2f * scale
        }

        // Outer circle fill + stroke
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawCircle(cx, cy, R, outerPaint)
            canvas.nativeCanvas.drawCircle(cx, cy, R, outerStroke)
        }

        // 12 Rashi segments
        RASHI_NAMES.forEachIndexed { i, rashiName ->
            val startDeg = i * 30f - 90f
            val endDeg   = startDeg + 30f

            // Check planets in this rashi
            val planetsHere = GRAHA_ORDER.filter { snapshot.grahas[it]?.rashi == rashiName }
            val hasPlanet   = planetsHere.isNotEmpty()
            val segColor    = if (hasPlanet) GRAHA_COLOR_MAP[planetsHere[0]] else null

            // Segment arc fill
            drawIntoCanvas { canvas ->
                val segPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.FILL
                    color = if (hasPlanet && segColor != null) {
                        val c = segColor.copy(alpha = 0.15f)
                        android.graphics.Color.argb((c.alpha * 255).toInt(), (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
                    } else {
                        val hue = i * 30
                        val hsv = floatArrayOf(hue.toFloat(), 0.08f, 0.92f)
                        android.graphics.Color.HSVToColor(hsv)
                    }
                }
                val segStroke = android.graphics.Paint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    color = android.graphics.Color.parseColor("#C0C8D0")
                    strokeWidth = 0.8f * scale
                }
                val path = android.graphics.Path()
                val startRad = Math.toRadians(startDeg.toDouble())
                val endRad   = Math.toRadians(endDeg.toDouble())
                val ix1 = (cx + INNER * cos(startRad)).toFloat()
                val iy1 = (cy + INNER * sin(startRad)).toFloat()
                path.moveTo(ix1, iy1)
                val ox1 = (cx + R * cos(startRad)).toFloat()
                val oy1 = (cy + R * sin(startRad)).toFloat()
                path.lineTo(ox1, oy1)
                val rect = android.graphics.RectF(cx - R, cy - R, cx + R, cy + R)
                path.arcTo(rect, startDeg, 30f)
                val ox2 = (cx + R * cos(endRad)).toFloat()
                val oy2 = (cy + R * sin(endRad)).toFloat()
                val ix2 = (cx + INNER * cos(endRad)).toFloat()
                val iy2 = (cy + INNER * sin(endRad)).toFloat()
                path.lineTo(ix2, iy2)
                val innerRect = android.graphics.RectF(cx - INNER, cy - INNER, cx + INNER, cy + INNER)
                path.arcTo(innerRect, endDeg, -30f)
                path.close()
                canvas.nativeCanvas.drawPath(path, segPaint)
                canvas.nativeCanvas.drawPath(path, segStroke)
            }

            // Rashi label
            val midDeg = startDeg + 15f
            val labelR  = INNER + (R - INNER) * 0.72f
            val midRad  = Math.toRadians(midDeg.toDouble())
            val lx = (cx + labelR * cos(midRad)).toFloat()
            val ly = (cy + labelR * sin(midRad)).toFloat()
            drawIntoCanvas { canvas ->
                val textPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 10f * scale
                    color = if (hasPlanet && segColor != null) {
                        android.graphics.Color.argb(255, (segColor.red * 255 * 0.6f).toInt(), (segColor.green * 255 * 0.5f).toInt(), (segColor.blue * 255 * 0.3f).toInt())
                    } else android.graphics.Color.parseColor("#6B7280")
                    typeface = if (hasPlanet) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                }
                canvas.nativeCanvas.drawText(RASHI_SHORT[i], lx, ly + textPaint.textSize / 3, textPaint)
            }
        }

        // Inner circles
        drawIntoCanvas { canvas ->
            val innerFill = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#EEF0F4")
                style = android.graphics.Paint.Style.FILL
            }
            val innerStroke = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#B0BBC8")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 1f * scale
            }
            canvas.nativeCanvas.drawCircle(cx, cy, INNER, innerFill)
            canvas.nativeCanvas.drawCircle(cx, cy, INNER, innerStroke)
            val midFill = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#E8EBF0")
                style = android.graphics.Paint.Style.FILL
            }
            canvas.nativeCanvas.drawCircle(cx, cy, r45, midFill)
            canvas.nativeCanvas.drawCircle(cx, cy, r45, innerStroke)
        }

        // Lagna marker
        snapshot.lagna?.let { lagna ->
            val lagnaIdx = RASHI_INDEX[lagna.rashi] ?: 0
            val lagnaLon = lagnaIdx * 30f
            val lagnaRad = Math.toRadians((lagnaLon - 90).toDouble())
            val lx1 = (cx + INNER * cos(lagnaRad)).toFloat()
            val ly1 = (cy + INNER * sin(lagnaRad)).toFloat()
            val lx2 = (cx + (R - 8 * scale) * cos(lagnaRad)).toFloat()
            val ly2 = (cy + (R - 8 * scale) * sin(lagnaRad)).toFloat()
            drawIntoCanvas { canvas ->
                val linePaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.parseColor("#B07A00")
                    strokeWidth = 1.5f * scale
                    style = android.graphics.Paint.Style.STROKE
                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(3f * scale, 2f * scale), 0f)
                }
                canvas.nativeCanvas.drawLine(lx1, ly1, lx2, ly2, linePaint)
                val lPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 8f * scale
                    color = android.graphics.Color.parseColor("#B07A00")
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                canvas.nativeCanvas.drawText("L", lx2, ly2 + lPaint.textSize / 3, lPaint)
            }
        }

        // Planets
        val BASE_R = INNER + 30f * scale
        GRAHA_ORDER.forEachIndexed { idx, name ->
            val p = snapshot.grahas[name] ?: return@forEachIndexed
            val rashiIdx = RASHI_INDEX[p.rashi] ?: 0
            val lon = rashiIdx * 30f + 15f  // center of rashi segment

            // Count overlapping planets in same rashi for radial offset
            val prevInSameRashi = GRAHA_ORDER.subList(0, idx).count { n ->
                snapshot.grahas[n]?.rashi == p.rashi
            }
            val r = BASE_R + when {
                prevInSameRashi == 0 -> 0f
                prevInSameRashi % 2 == 1 -> -14f * scale
                else -> 14f * scale
            }

            val rad = Math.toRadians((lon - 90).toDouble())
            val px  = (cx + r * cos(rad)).toFloat()
            val py  = (cy + r * sin(rad)).toFloat()
            val color = GRAHA_COLOR_MAP[name] ?: BrahmGold

            drawIntoCanvas { canvas ->
                // Glow halo for visible planets
                if (p.visible) {
                    val haloPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.FILL
                        this.color = android.graphics.Color.argb(30, (color.red*255).toInt(), (color.green*255).toInt(), (color.blue*255).toInt())
                    }
                    canvas.nativeCanvas.drawCircle(px, py, 10f * scale, haloPaint)
                }
                val symbolPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 14f * scale
                    this.color = android.graphics.Color.argb(
                        if (p.combust) 150 else 255,
                        (color.red*255).toInt(), (color.green*255).toInt(), (color.blue*255).toInt()
                    )
                    typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
                }
                canvas.nativeCanvas.drawText(GRAHA_SYMBOL[name] ?: "", px, py + symbolPaint.textSize / 3, symbolPaint)
                // Retro marker
                if (p.retro) {
                    val retroPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 6f * scale
                        this.color = android.graphics.Color.argb(200, (color.red*255).toInt(), (color.green*255).toInt(), (color.blue*255).toInt())
                    }
                    canvas.nativeCanvas.drawText("℞", px + 8f * scale, py - 6f * scale, retroPaint)
                }
            }
        }

        // Center ॐ
        drawIntoCanvas { canvas ->
            val omPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 18f * scale
                color = android.graphics.Color.argb(90, 0xB0, 0x7A, 0x00)
                typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.NORMAL)
            }
            canvas.nativeCanvas.drawText("ॐ", cx, cy + omPaint.textSize / 3, omPaint)
        }
    }
}

// ── Cosmic Energy Summary ─────────────────────────────────────────────────────
@Composable
private fun CosmicSummary(snapshot: SkySnapshot) {
    val moonRashi   = snapshot.grahas["Chandra"]?.rashi ?: "Mesha"
    val moonQuality = RASHI_QUALITY[moonRashi] ?: "dynamic"
    val retroList   = GRAHA_ORDER.filter { snapshot.grahas[it]?.retro == true }
    val combustList = GRAHA_ORDER.filter { snapshot.grahas[it]?.combust == true }
    val benefics    = listOf("Guru", "Shukra", "Budh")
    val beneficCount = benefics.count { snapshot.grahas[it]?.visible == true }

    val (mood, moodColor) = when {
        beneficCount >= 2 && retroList.isEmpty() -> "Auspicious" to Color(0xFF4ADE80)
        retroList.size >= 3                      -> "Reflective"  to Color(0xFFFBBF24)
        combustList.size >= 2                    -> "Intense"     to Color(0xFFFB923C)
        else                                     -> "Balanced"    to BrahmGold
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5C27A)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TODAY'S COSMIC ENERGY", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, letterSpacing = 1.sp))
                Text(mood, style = MaterialTheme.typography.labelSmall.copy(color = moodColor, fontWeight = FontWeight.SemiBold))
            }
            Text(
                buildString {
                    append("Moon in $moonRashi — bringing $moonQuality energy to the mind and emotions.")
                    if (retroList.isNotEmpty()) append(" ${retroList.joinToString(", ")} ${if (retroList.size == 1) "is" else "are"} retrograde — review, reflect, revisit.")
                    if (combustList.isNotEmpty()) append(" ${combustList.joinToString(", ")} near the Sun — heightened energy, handle with care.")
                },
                style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, lineHeight = 18.sp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("${snapshot.visibleCount} visible", Color(0xFF4ADE80))
                if (retroList.isNotEmpty()) StatPill("${retroList.size} retro", Color(0xFFFBBF24))
                if (combustList.isNotEmpty()) StatPill("${combustList.size} combust", Color(0xFFFB923C))
            }
        }
    }
}

// ── Planet Cards 3-col grid ───────────────────────────────────────────────────
@Composable
private fun PlanetCardsGrid(snapshot: SkySnapshot) {
    val planets = GRAHA_ORDER.mapNotNull { name -> snapshot.grahas[name]?.let { name to it } }
    // 3 columns
    planets.chunked(3).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            row.forEach { (name, p) ->
                val color = GRAHA_COLOR_MAP[name] ?: BrahmGold
                Box(
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, color.copy(0.15f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                ) {
                    // Radial glow bg
                    Box(
                        Modifier.matchParentSize().clip(RoundedCornerShape(12.dp)).background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                listOf(color.copy(0.07f), Color.Transparent),
                                radius = 80f,
                            ),
                        ),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Text(GRAHA_SYMBOL[name] ?: "", fontSize = 22.sp, color = color.copy(if (p.combust) 0.5f else 1f))
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (p.retro) Badge(text = "℞ Retro", bg = Color(0xFFFEF3C7), fg = Color(0xFFD97706))
                                if (p.combust) Badge(text = "Combust", bg = Color(0xFFFFF7ED), fg = Color(0xFFEA580C))
                                if (p.visible && !p.combust) Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4ADE80)))
                            }
                        }
                        Column {
                            Text(name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = BrahmForeground))
                            Text(GRAHA_NAME_HI[name] ?: "", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                        }
                        Box(
                            Modifier.clip(RoundedCornerShape(50.dp))
                                .background(color.copy(0.12f))
                                .border(1.dp, color.copy(0.4f), RoundedCornerShape(50.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(p.rashi.take(7), style = MaterialTheme.typography.labelSmall.copy(color = color, fontWeight = FontWeight.Medium))
                        }
                        Column {
                            Text(PLANET_DOMAIN[name] ?: "", style = MaterialTheme.typography.labelSmall.copy(color = BrahmForeground, fontWeight = FontWeight.Medium, fontSize = 9.sp))
                            Text(RASHI_QUALITY[p.rashi] ?: "", style = MaterialTheme.typography.labelSmall.copy(color = color, fontSize = 9.sp))
                        }
                    }
                }
            }
            // Fill empty slots in last row
            repeat(3 - row.size) { Box(Modifier.weight(1f)) }
        }
    }
}

// ── Helper composables ────────────────────────────────────────────────────────
@Composable
private fun QuickStatCard(modifier: Modifier, emoji: String, label: String, value: String, sub: String, valueColor: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 12.sp, color = valueColor)
                Text(label, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
            }
            Text(value, style = MaterialTheme.typography.titleLarge.copy(color = valueColor, fontWeight = FontWeight.Bold))
            Text(sub, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground), maxLines = 1)
        }
    }
}

@Composable
private fun LegendItem(symbol: String, color: Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(symbol, fontSize = 11.sp, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
    }
}

@Composable
private fun Badge(text: String, bg: Color, fg: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(4.dp)).background(bg).padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall.copy(color = fg, fontSize = 8.sp))
    }
}

@Composable
private fun StatPill(text: String, color: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(50.dp))
            .background(color.copy(0.12f))
            .border(1.dp, color.copy(0.25f), RoundedCornerShape(50.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall.copy(color = color))
    }
}

// ── Moon Transit Effects ───────────────────────────────────────────────────────
private data class MoonEffect(val quality: String, val text: String, val advice: String)
private val MOON_TRANSIT_EFFECTS = mapOf(
    1  to MoonEffect("mixed", "Moon in 1st — introspection & self-focus.", "Good for meditation, self-care. Avoid initiating conflicts."),
    2  to MoonEffect("mixed", "Moon in 2nd — speech & finances.", "Favorable for financial discussions. Watch harsh words."),
    3  to MoonEffect("good",  "Moon in 3rd — courage, siblings, short travel.", "Excellent for communication, writing, short journeys."),
    4  to MoonEffect("good",  "Moon in 4th — home, mother, emotions.", "Spend time at home. Connect with family. Nurture inner self."),
    5  to MoonEffect("good",  "Moon in 5th — creativity, children, intellect.", "Good for studies, creative work, romance."),
    6  to MoonEffect("bad",   "Moon in 6th — obstacles, health matters.", "Be careful with health. Avoid lending money. Stay patient."),
    7  to MoonEffect("mixed", "Moon in 7th — relationships, partnerships.", "Good for partnerships, marriage matters, social interaction."),
    8  to MoonEffect("bad",   "Moon in 8th — sudden changes, hidden matters.", "Avoid risky ventures. Good for research, spiritual practices."),
    9  to MoonEffect("good",  "Moon in 9th — dharma, fortune, teachers.", "Blessings from elders. Good for spiritual activities, long travel."),
    10 to MoonEffect("good",  "Moon in 10th — career, public life, action.", "Excellent for career moves, public dealings, leadership."),
    11 to MoonEffect("good",  "Moon in 11th — gains, friends, fulfilment.", "Expect gains, social connections. Good day for networking."),
    12 to MoonEffect("bad",   "Moon in 12th — rest, expenses, foreign.", "Rest and retreat. Avoid spending. Spiritual practice is favored."),
)
