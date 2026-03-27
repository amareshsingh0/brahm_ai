package com.bimoraai.brahm.ui.kundali.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimoraai.brahm.core.components.BrahmCard
import com.bimoraai.brahm.core.components.SectionHeader
import com.bimoraai.brahm.core.theme.*
import com.bimoraai.brahm.ui.kundali.chart.KundaliChartView
import java.time.LocalDate

// ─── Chart Tab ───────────────────────────────────────────────────────────────

// API returns grahas as {planetName: {house: N, rashi: "...", ...}}
// Build Map<Int, List<String>> by grouping planet abbreviations by house number
private fun buildHouseMap(data: Map<String, Any?>): Map<Int, List<String>> {
    @Suppress("UNCHECKED_CAST")
    val grahasRaw = data["grahas"] as? Map<String, Any?> ?: return emptyMap()
    val result = mutableMapOf<Int, MutableList<String>>()
    grahasRaw.forEach { (planet, info) ->
        val house = (info as? Map<String, Any?>)?.get("house")?.toString()?.toIntOrNull() ?: return@forEach
        result.getOrPut(house) { mutableListOf() }.add(planetAbbr(planet))
    }
    return result
}

private fun planetAbbr(name: String) = when (name.lowercase()) {
    "surya", "sun"     -> "Su"
    "chandra", "moon"  -> "Mo"
    "mangal", "mars"   -> "Ma"
    "budha", "mercury" -> "Bu"
    "guru", "jupiter"  -> "Gu"
    "shukra", "venus"  -> "Sh"
    "shani", "saturn"  -> "Sa"
    "rahu"             -> "Ra"
    "ketu"             -> "Ke"
    else               -> name.take(2).replaceFirstChar { it.uppercase() }
}

@Composable
fun ChartTab(data: Map<String, Any?>) {
    val grahas = buildHouseMap(data)
    val lagna  = data["lagna"]?.toString() ?: "—"

    @Suppress("UNCHECKED_CAST")
    val planets = data["planets"] as? List<Map<String, Any?>> ?: emptyList()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Chart
        item {
            KundaliChartView(grahas = grahas, modifier = Modifier.fillMaxWidth())
        }

        // Lagna info card
        item {
            BrahmCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text("Lagna (Ascendant)", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                        Text(lagna, style = MaterialTheme.typography.titleMedium.copy(color = BrahmGold, fontWeight = FontWeight.SemiBold))
                    }
                    data["moon_rashi"]?.let { moon ->
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Moon Rashi", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                            Text(moon.toString(), style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF94a3b8), fontWeight = FontWeight.SemiBold))
                        }
                    }
                    data["sun_rashi"]?.let { sun ->
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Sun Rashi", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                            Text(sun.toString(), style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFFf59e0b), fontWeight = FontWeight.SemiBold))
                        }
                    }
                }
            }
        }

        // Quick planet summary
        if (planets.isNotEmpty()) {
            item { SectionHeader("Planet Summary") }
            items(planets) { planet ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val pname = planet["name"]?.toString() ?: "—"
                    val dotColor = DASHA_COLORS[pname] ?: BrahmGold
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(dotColor))
                    Spacer(Modifier.width(8.dp))
                    Text(pname, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), modifier = Modifier.width(80.dp))
                    Text(planet["rashi"]?.toString() ?: "—", style = MaterialTheme.typography.bodySmall.copy(color = BrahmGold), modifier = Modifier.width(80.dp))
                    Text("H${planet["house"]?.toString() ?: "—"}", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    Spacer(Modifier.weight(1f))
                    if (planet["retrograde"]?.toString() == "true") {
                        Text("℞", color = Color(0xFFE8445A), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// ─── Grahas Tab ──────────────────────────────────────────────────────────────
@Composable
fun GrahasTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val planets = data["planets"] as? List<Map<String, Any?>> ?: emptyList()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("Planetary Positions") }
        items(planets) { planet ->
            BrahmCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Top row: planet name + dignity badge
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            planet["name"]?.toString() ?: "—",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f),
                        )
                        val dignity = planet["dignity"]?.toString() ?: planet["status"]?.toString() ?: ""
                        if (dignity.isNotBlank()) {
                            DignityBadge(dignity)
                        }
                        if (planet["retrograde"]?.toString() == "true") {
                            Spacer(Modifier.width(4.dp))
                            Text("℞", color = Color(0xFFE8445A), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    // Details row
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        PlanetDetail("Rashi",    planet["rashi"]?.toString() ?: "—", BrahmGold)
                        PlanetDetail("House",    "H${planet["house"]?.toString() ?: "—"}", BrahmForeground)
                        PlanetDetail("Nakshatra", planet["nakshatra"]?.toString() ?: "—", Color(0xFF6C63FF))
                        val deg = planet["degree"]?.toString() ?: planet["lon"]?.toString() ?: ""
                        if (deg.isNotBlank()) {
                            val degNum = deg.toDoubleOrNull()
                            val degStr = if (degNum != null) "%.1f°".format(degNum % 30) else deg
                            PlanetDetail("Degree", degStr, BrahmMutedForeground)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanetDetail(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(color = valueColor, fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

@Composable
private fun DignityBadge(dignity: String) {
    val (bg, fg) = when {
        dignity.contains("Uchcha", true) || dignity.contains("Exalt", true) ->
            Color(0xFFD1FAE5) to Color(0xFF065F46)
        dignity.contains("Neecha", true) || dignity.contains("Debil", true) ->
            Color(0xFFFEE2E2) to Color(0xFF991B1B)
        dignity.contains("Svak", true) || dignity.contains("Own", true) ->
            Color(0xFFFEF3C7) to Color(0xFF92400E)
        else -> Color(0xFFF3F4F6) to Color(0xFF6B7280)
    }
    val label = when {
        dignity.contains("Uchcha", true) || dignity.contains("Exalt", true) -> "Exalted"
        dignity.contains("Neecha", true) || dignity.contains("Debil", true) -> "Debilitated"
        dignity.contains("Svak", true) || dignity.contains("Own", true) -> "Own Sign"
        else -> "Normal"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = fg, fontWeight = FontWeight.SemiBold, fontSize = 10.sp))
    }
}

// ─── Bhavas Tab ──────────────────────────────────────────────────────────────

private val BHAVA_NAMES = listOf(
    "Tanu", "Dhana", "Sahaja", "Sukha", "Putra", "Ripu",
    "Kalatra", "Mrityu", "Dharma", "Karma", "Labha", "Vyaya",
)
private val BHAVA_MEANINGS = listOf(
    "Self, Body", "Wealth, Family", "Siblings, Courage", "Home, Mother",
    "Children, Education", "Enemies, Health", "Spouse, Partnership", "Death, Transformation",
    "Fortune, Religion", "Career, Status", "Gains, Friends", "Expenses, Liberation",
)

// Planets that have special aspects (extra houses beyond 7th)
private val SPECIAL_ASPECTS = mapOf(
    "Mangal" to listOf(4, 7, 8),   // 4th, 7th, 8th from itself
    "Guru"   to listOf(5, 7, 9),   // 5th, 7th, 9th
    "Shani"  to listOf(3, 7, 10),  // 3rd, 7th, 10th
    "Mars"   to listOf(4, 7, 8),
    "Jupiter" to listOf(5, 7, 9),
    "Saturn" to listOf(3, 7, 10),
)

@Composable
fun BhavasTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val grahasRaw = data["grahas"] as? Map<String, Any?> ?: emptyMap()

    // Build house → residents map
    val houseResidents = mutableMapOf<Int, MutableList<String>>()
    val houseLords = mutableMapOf<Int, String>()   // from bhavas if available
    val houseRashis = mutableMapOf<Int, String>()

    @Suppress("UNCHECKED_CAST")
    val bhavasRaw = data["bhavas"] as? List<Map<String, Any?>>
    if (bhavasRaw != null) {
        bhavasRaw.forEach { b ->
            val h = b["house"]?.toString()?.toIntOrNull() ?: return@forEach
            houseRashis[h] = b["rashi"]?.toString() ?: ""
            houseLords[h]  = b["lord"]?.toString() ?: ""
        }
    }

    grahasRaw.forEach { (planet, info) ->
        val house = (info as? Map<String, Any?>)?.get("house")?.toString()?.toIntOrNull() ?: return@forEach
        houseResidents.getOrPut(house) { mutableListOf() }.add(planet)
    }

    // Compute aspects: for each planet, which houses does it aspect?
    val houseAspects = mutableMapOf<Int, MutableList<String>>()
    grahasRaw.forEach { (planet, info) ->
        val fromHouse = (info as? Map<String, Any?>)?.get("house")?.toString()?.toIntOrNull() ?: return@forEach
        val aspects = SPECIAL_ASPECTS[planet] ?: listOf(7)
        aspects.forEach { offset ->
            val targetHouse = ((fromHouse - 1 + offset - 1) % 12) + 1
            houseAspects.getOrPut(targetHouse) { mutableListOf() }.add(planet)
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("Bhava Analysis") }
        items(12) { idx ->
            val h = idx + 1
            val residents = houseResidents[h] ?: emptyList()
            val lord = houseLords[h] ?: ""
            val rashi = houseRashis[h] ?: ""
            val aspects = houseAspects[h] ?: emptyList()
            BrahmCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(BrahmGold.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$h", style = MaterialTheme.typography.labelMedium.copy(color = BrahmGold, fontWeight = FontWeight.Bold))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(BHAVA_NAMES.getOrElse(idx) { "H$h" }, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                            Text(BHAVA_MEANINGS.getOrElse(idx) { "" }, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
                        }
                        if (rashi.isNotBlank()) {
                            Text(rashi, style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontSize = 10.sp))
                        }
                    }
                    if (lord.isNotBlank() || residents.isNotEmpty() || aspects.isNotEmpty()) {
                        HorizontalDivider(color = BrahmBorder)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (lord.isNotBlank()) BhavaDetail("Lord", lord)
                            if (residents.isNotEmpty()) BhavaDetail("Planets", residents.joinToString(", "))
                            if (aspects.isNotEmpty()) BhavaDetail("Aspects", aspects.joinToString(", "))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BhavaDetail(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, fontWeight = FontWeight.Medium))
    }
}

// ─── Dashas Tab ──────────────────────────────────────────────────────────────

private val DASHA_COLORS = mapOf(
    "Ketu" to Color(0xFFf97316), "Shukra" to Color(0xFFa855f7),
    "Surya" to Color(0xFFf59e0b), "Chandra" to Color(0xFF94a3b8),
    "Mangal" to Color(0xFFef4444), "Rahu" to Color(0xFF6366f1),
    "Guru" to Color(0xFFeab308), "Shani" to Color(0xFF64748b),
    "Budha" to Color(0xFF22c55e), "Mercury" to Color(0xFF22c55e),
    "Jupiter" to Color(0xFFeab308), "Saturn" to Color(0xFF64748b),
    "Sun" to Color(0xFFf59e0b), "Moon" to Color(0xFF94a3b8),
    "Mars" to Color(0xFFef4444), "Venus" to Color(0xFFa855f7),
)

@Composable
fun DashasTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val dashas = data["dasha"] as? List<Map<String, Any?>> ?: emptyList()
    val today = try { LocalDate.now().toString() } catch (_: Exception) { "" }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("Vimshottari Dasha") }
        items(dashas) { dasha ->
            val planet = dasha["planet"]?.toString() ?: "—"
            val start  = dasha["start"]?.toString()  ?: ""
            val end    = dasha["end"]?.toString()    ?: ""
            val isActive = today.isNotEmpty() && start.isNotEmpty() && end.isNotEmpty()
                && today >= start && today <= end
            val dotColor = DASHA_COLORS[planet] ?: BrahmGold

            @Suppress("UNCHECKED_CAST")
            val antardashas = dasha["antardashas"] as? List<Map<String, Any?>> ?: emptyList()

            BrahmCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        // Colored planet dot
                        Box(
                            Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(dotColor)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            planet,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = dotColor,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        if (isActive) {
                            Box(
                                Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFD1FAE5)).padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Active", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF065F46), fontWeight = FontWeight.Bold, fontSize = 10.sp))
                            }
                        }
                    }
                    Text(
                        "$start  →  $end",
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                    )
                    // Antardasha mini-list (if present)
                    if (antardashas.isNotEmpty()) {
                        HorizontalDivider(color = BrahmBorder, modifier = Modifier.padding(vertical = 4.dp))
                        Text("Antardashas", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                        Spacer(Modifier.height(2.dp))
                        antardashas.forEach { ad ->
                            val adPlanet = ad["planet"]?.toString() ?: "—"
                            val adStart  = ad["start"]?.toString()  ?: ""
                            val adEnd    = ad["end"]?.toString()    ?: ""
                            val adActive = today.isNotEmpty() && adStart.isNotEmpty() && adEnd.isNotEmpty()
                                && today >= adStart && today <= adEnd
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "• $adPlanet",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (adActive) dotColor else BrahmForeground,
                                        fontWeight = if (adActive) FontWeight.SemiBold else FontWeight.Normal,
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "$adStart – $adEnd",
                                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Yogas Tab ───────────────────────────────────────────────────────────────
@Composable
fun YogasTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val yogas = data["yogas"] as? List<Map<String, Any?>> ?: emptyList()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("Yogas") }
        if (yogas.isEmpty()) {
            item { Text("No yogas found", color = BrahmMutedForeground, style = MaterialTheme.typography.bodyMedium) }
        }
        items(yogas) { yoga ->
            BrahmCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(yoga["name"]?.toString() ?: "—", style = MaterialTheme.typography.titleMedium.copy(color = BrahmGold, fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.height(4.dp))
                    Text(yoga["description"]?.toString() ?: "", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            }
        }
    }
}

// ─── Alerts Tab ──────────────────────────────────────────────────────────────
@Composable
fun AlertsTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val alerts = data["alerts"] as? List<Map<String, Any?>> ?: emptyList()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("Alerts & Doshas") }
        if (alerts.isEmpty()) {
            item { Text("No alerts", color = BrahmMutedForeground, style = MaterialTheme.typography.bodyMedium) }
        }
        items(alerts) { alert ->
            BrahmCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(alert["title"]?.toString() ?: "—", style = MaterialTheme.typography.titleMedium)
                    Text(alert["detail"]?.toString() ?: "", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            }
        }
    }
}

// ─── Shadbala Tab ────────────────────────────────────────────────────────────
@Composable
fun ShadbalaTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val shadbala = data["shadbala"] as? List<Map<String, Any?>> ?: emptyList()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionHeader("Shadbala (Planetary Strength)") }
        items(shadbala) { s ->
            BrahmCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(s["planet"]?.toString() ?: "—", style = MaterialTheme.typography.titleMedium)
                    Text("${s["total"]?.toString() ?: "—"} rupa", style = MaterialTheme.typography.bodyMedium.copy(color = BrahmGold))
                }
            }
        }
    }
}

// ─── Navamsha Tab ────────────────────────────────────────────────────────────
@Composable
fun NavamshaTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val navamsha = data["navamsha"] as? Map<String, Any?> ?: emptyMap()
    @Suppress("UNCHECKED_CAST")
    val navamshaData = navamsha as Map<String, Any?>
    val grahas = buildHouseMap(navamshaData)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        SectionHeader("Navamsha (D9) Chart")
        Spacer(Modifier.height(12.dp))
        KundaliChartView(grahas = grahas, modifier = Modifier.fillMaxWidth())
    }
}
