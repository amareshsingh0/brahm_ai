package com.bimoraai.brahm.ui.kundali.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimoraai.brahm.core.components.BrahmCard
import com.bimoraai.brahm.core.components.SectionHeader
import com.bimoraai.brahm.core.theme.*
import com.bimoraai.brahm.ui.kundali.chart.KundaliChartView
import java.time.LocalDate

// ─── File-level data classes (MUST be at file level to avoid ClassCastException) ─

// ─── Constants ────────────────────────────────────────────────────────────────

private val GRAHA_COLORS = mapOf(
    "Surya" to Color(0xFFF59E0B), "Sun" to Color(0xFFF59E0B),
    "Chandra" to Color(0xFF93C5FD), "Moon" to Color(0xFF93C5FD),
    "Mangal" to Color(0xFFEF4444), "Mars" to Color(0xFFEF4444),
    "Budha" to Color(0xFF22C55E), "Budh" to Color(0xFF22C55E), "Mercury" to Color(0xFF22C55E),
    "Guru" to Color(0xFFEAB308), "Jupiter" to Color(0xFFEAB308),
    "Shukra" to Color(0xFFA855F7), "Venus" to Color(0xFFA855F7),
    "Shani" to Color(0xFF94A3B8), "Saturn" to Color(0xFF94A3B8),
    "Rahu" to Color(0xFF6366F1), "Ketu" to Color(0xFFF97316),
)

private val GRAHA_SYMBOLS = mapOf(
    "Surya" to "☉", "Chandra" to "☽", "Mangal" to "♂", "Budha" to "☿", "Budh" to "☿",
    "Guru" to "♃", "Shukra" to "♀", "Shani" to "♄", "Rahu" to "☊", "Ketu" to "☋",
    "Sun" to "☉", "Moon" to "☽", "Mars" to "♂", "Mercury" to "☿",
    "Jupiter" to "♃", "Venus" to "♀", "Saturn" to "♄",
    "Lagna" to "Asc",
)

private val GRAHA_EN = mapOf(
    "Surya" to "Sun", "Chandra" to "Moon", "Mangal" to "Mars", "Budha" to "Mercury",
    "Budh" to "Mercury", "Guru" to "Jupiter", "Shukra" to "Venus", "Shani" to "Saturn",
    "Rahu" to "Rahu", "Ketu" to "Ketu",
)

val DASHA_COLORS = mapOf(
    "Ketu" to Color(0xFFf97316), "Shukra" to Color(0xFFa855f7),
    "Surya" to Color(0xFFf59e0b), "Chandra" to Color(0xFF94a3b8),
    "Mangal" to Color(0xFFef4444), "Rahu" to Color(0xFF6366f1),
    "Guru" to Color(0xFFeab308), "Shani" to Color(0xFF64748b),
    "Budha" to Color(0xFF22c55e), "Budh" to Color(0xFF22c55e), "Mercury" to Color(0xFF22c55e),
    "Jupiter" to Color(0xFFeab308), "Saturn" to Color(0xFF64748b),
    "Sun" to Color(0xFFf59e0b), "Moon" to Color(0xFF94a3b8),
    "Mars" to Color(0xFFef4444), "Venus" to Color(0xFFa855f7),
)

private val GRAHA_ORDER = listOf("Surya","Chandra","Mangal","Budha","Guru","Shukra","Shani","Rahu","Ketu")

private val RASHI_NAMES = listOf(
    "Mesha","Vrishabha","Mithuna","Karka","Simha","Kanya",
    "Tula","Vrischika","Dhanu","Makara","Kumbha","Meena",
)
private val RASHI_SHORT_NAMES = listOf(
    "Ari","Tau","Gem","Can","Leo","Vir","Lib","Sco","Sag","Cap","Aqu","Pis",
)

private val BHAVA_NAMES = listOf(
    "Tanu", "Dhana", "Sahaja", "Sukha", "Putra", "Ripu",
    "Kalatra", "Mrityu", "Dharma", "Karma", "Labha", "Vyaya",
)
private val BHAVA_MEANINGS = listOf(
    "Self, Body", "Wealth, Family", "Siblings, Courage", "Home, Mother",
    "Children, Education", "Enemies, Health", "Spouse, Partnership", "Death, Transformation",
    "Fortune, Religion", "Career, Status", "Gains, Friends", "Expenses, Liberation",
)

private val SPECIAL_ASPECTS_MAP = mapOf(
    "Mangal" to listOf(3, 7), "Mars" to listOf(3, 7),
    "Guru" to listOf(4, 8), "Jupiter" to listOf(4, 8),
    "Shani" to listOf(2, 9), "Saturn" to listOf(2, 9),
)

private val YOGA_CATEGORY_COLORS = mapOf(
    "Power" to Color(0xFFF59E0B),
    "Wealth" to Color(0xFF22C55E),
    "Intellect" to Color(0xFF60A5FA),
    "Spiritual" to Color(0xFFA855F7),
    "Marriage" to Color(0xFFF472B6),
    "Adversity" to Color(0xFFEF4444),
)

private val MALEFIC_UPAGRAHAS = setOf(
    "Dhuma", "Vyatipata", "Mandi", "Gulika", "Kala", "Mrityu", "YamaGhantaka",
)

// ─── Helper functions ─────────────────────────────────────────────────────────

private fun degToDMS(deg: Double): String {
    val rashiIdx = (deg / 30).toInt().coerceIn(0, 11)
    val inRashi = deg % 30
    val d = inRashi.toInt()
    val mf = (inRashi - d) * 60
    val m = mf.toInt()
    val s = ((mf - m) * 60).toInt()
    return "${RASHI_SHORT_NAMES[rashiIdx]} ${d}°${m}'${s}\""
}

private fun planetAbbr(name: String) = when (name.lowercase()) {
    "surya", "sun"     -> "Su"
    "chandra", "moon"  -> "Mo"
    "mangal", "mars"   -> "Ma"
    "budha", "budh", "mercury" -> "Bu"
    "guru", "jupiter"  -> "Gu"
    "shukra", "venus"  -> "Sh"
    "shani", "saturn"  -> "Sa"
    "rahu"             -> "Ra"
    "ketu"             -> "Ke"
    else               -> name.take(2).replaceFirstChar { it.uppercase() }
}

private fun buildHouseMapFromData(data: Map<String, Any?>): Map<Int, List<String>> {
    @Suppress("UNCHECKED_CAST")
    val grahasRaw = data["grahas"] as? Map<String, Any?> ?: return emptyMap()
    val result = mutableMapOf<Int, MutableList<String>>()
    grahasRaw.forEach { (planet, info) ->
        val house = (info as? Map<String, Any?>)?.get("house")?.toString()?.toIntOrNull() ?: return@forEach
        result.getOrPut(house) { mutableListOf() }.add(planetAbbr(planet))
    }
    return result
}

private fun computeAspects(grahaHouses: Map<String, Int>): Map<Int, List<String>> {
    val result = mutableMapOf<Int, MutableList<String>>()
    for (h in 1..12) result[h] = mutableListOf()
    grahaHouses.forEach { (planet, fromHouse) ->
        val offsets = mutableListOf(6)
        SPECIAL_ASPECTS_MAP[planet]?.let { offsets.addAll(it) }
        offsets.forEach { off ->
            val aspH = (fromHouse - 1 + off) % 12 + 1
            result.getOrPut(aspH) { mutableListOf() }.add(planet)
        }
    }
    return result
}

private fun today(): String = try { LocalDate.now().toString() } catch (_: Exception) { "" }

private fun isCurrentPeriod(start: String, end: String): Boolean {
    val now = today()
    return now.isNotEmpty() && start.isNotEmpty() && end.isNotEmpty() && now >= start && now <= end
}

// ─── Status Badge helper ──────────────────────────────────────────────────────

@Composable
private fun StatusBadge(status: String) {
    val (bg, fg) = when {
        status.contains("Uchcha", true) || status.contains("Exalt", true) ->
            Color(0xFFD1FAE5) to Color(0xFF065F46)
        status.contains("Neecha", true) || status.contains("Debil", true) ->
            Color(0xFFFEE2E2) to Color(0xFF991B1B)
        status.contains("Svak", true) || status.contains("Own", true) ->
            Color(0xFFFEF3C7) to Color(0xFF92400E)
        else -> Color(0xFFF3F4F6) to Color(0xFF6B7280)
    }
    val label = when {
        status.contains("Uchcha", true) || status.contains("Exalt", true) -> "Exalted"
        status.contains("Neecha", true) || status.contains("Debil", true) -> "Debil"
        status.contains("Svak", true) || status.contains("Own", true) -> "Own"
        else -> "Normal"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = fg, fontWeight = FontWeight.SemiBold, fontSize = 9.sp))
    }
}

// ─── Info Cell (label + value) ────────────────────────────────────────────────

@Composable
private fun InfoCell(label: String, value: String, valueColor: Color = BrahmForeground) {
    Column(Modifier.background(BrahmMuted.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(color = valueColor, fontWeight = FontWeight.Medium),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─── Varga Chart Selector ─────────────────────────────────────────────────────

private data class VargaMeta(val div: Int, val code: String, val name: String, val desc: String)

private val VARGA_OPTIONS = listOf(
    VargaMeta(0,   "BC",   "Bhav Chalit",    "Cuspal House Chart"),
    VargaMeta(1,   "D-1",  "Rashi",          "Body, Personality"),
    VargaMeta(2,   "D-2",  "Hora",           "Wealth"),
    VargaMeta(3,   "D-3",  "Drekkana",       "Siblings"),
    VargaMeta(4,   "D-4",  "Chaturthamsha",  "Property"),
    VargaMeta(5,   "D-5",  "Panchamsha",     "Power"),
    VargaMeta(6,   "D-6",  "Shashtiamsha",   "Health"),
    VargaMeta(7,   "D-7",  "Saptamsha",      "Children"),
    VargaMeta(8,   "D-8",  "Ashtamsha",      "Obstacles"),
    VargaMeta(9,   "D-9",  "Navamsha",       "Marriage, Soul"),
    VargaMeta(10,  "D-10", "Dashamsha",      "Career"),
    VargaMeta(11,  "D-11", "Ekadashamsha",   "Benefits"),
    VargaMeta(12,  "D-12", "Dwadashamsha",   "Parents"),
    VargaMeta(16,  "D-16", "Shodashamsha",   "Vehicles"),
    VargaMeta(20,  "D-20", "Vimsamsha",      "Spirituality"),
    VargaMeta(24,  "D-24", "Chaturvimsha",   "Education"),
    VargaMeta(27,  "D-27", "Nakshatramsha",  "Strength"),
    VargaMeta(30,  "D-30", "Trimsamsha",     "Misfortune"),
    VargaMeta(40,  "D-40", "Khavedamsha",    "Maternal"),
    VargaMeta(45,  "D-45", "Akshavedamsha",  "Paternal"),
    VargaMeta(60,  "D-60", "Shashtiamsha",   "General"),
)

// ─── VargaChartBlock composable ───────────────────────────────────────────────

@Composable
private fun VargaChartBlock(
    label: String,
    meta: VargaMeta,
    houseMap: Map<Int, List<String>>,
    lagnaRashi: String,
    chartStyle: String,
    onTapHeader: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Header row: [A] D-1 — Rashi  ↓  Lagna
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BrahmMuted)
                .border(1.dp, BrahmBorder, RoundedCornerShape(10.dp))
                .clickable { onTapHeader() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Label badge (A or B)
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BrahmGold.copy(alpha = 0.15f))
                    .border(1.dp, BrahmGold.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontWeight = FontWeight.Bold, fontSize = 10.sp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${meta.code} — ${meta.name}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = BrahmForeground),
                )
                Text(meta.desc, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
            }
            // Lagna rashi label
            if (lagnaRashi.isNotBlank() && lagnaRashi != "—") {
                Text(lagnaRashi, style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontWeight = FontWeight.SemiBold))
            }
            Icon(Icons.Default.KeyboardArrowDown, null, tint = BrahmMutedForeground, modifier = Modifier.size(16.dp))
        }
        // Chart canvas
        KundaliChartView(
            grahas     = houseMap,
            lagnaRashi = lagnaRashi,
            chartStyle = chartStyle,
            modifier   = Modifier.fillMaxWidth(),
        )
    }
}

// ─── Charts Tab ──────────────────────────────────────────────────────────────

@Composable
fun ChartTab(data: Map<String, Any?>) {
    val today = today()

    // Build D-1 house map
    val d1HouseMap = buildHouseMapFromData(data)

    // Get navamsha house map for D-9
    @Suppress("UNCHECKED_CAST")
    val navamshaRaw = data["navamsha"] as? Map<String, Any?>

    // Build navamsha house map
    val navHouseMap = if (navamshaRaw != null) {
        val res = mutableMapOf<Int, MutableList<String>>()
        navamshaRaw.forEach { (planet, info) ->
            val house = (info as? Map<String, Any?>)?.get("house")?.toString()?.toIntOrNull() ?: return@forEach
            res.getOrPut(house) { mutableListOf() }.add(planetAbbr(planet))
        }
        res.toMap()
    } else emptyMap()

    val lagnaMap = data["lagna"] as? Map<String, Any?>
    val lagnaRashi = lagnaMap?.get("rashi")?.toString() ?: "—"

    // A/B varga selections (0=BC, 1=D-1, 9=D-9)
    var vargaA by remember { mutableIntStateOf(0) }   // default BC (Bhav Chalit)
    var vargaB by remember { mutableIntStateOf(1) }   // default D-1
    var showPickerFor by remember { mutableStateOf<String?>(null) } // "A" or "B"
    var chartStyle by remember { mutableStateOf("North") }
    var chartSubTab by remember { mutableStateOf("graha") }

    @Suppress("UNCHECKED_CAST")
    val grahasRaw = data["grahas"] as? Map<String, Any?> ?: emptyMap()
    @Suppress("UNCHECKED_CAST")
    val vargaCharts = data["varga_charts"] as? Map<String, Any?> ?: emptyMap()

    // Build house map for any varga index
    fun houseMapForVarga(idx: Int): Map<Int, List<String>> {
        val meta = VARGA_OPTIONS.getOrElse(idx) { VARGA_OPTIONS[0] }
        // BC = Bhav Chalit — use chalit house numbers
        if (meta.code == "BC") {
            @Suppress("UNCHECKED_CAST")
            val bcPlanets = (data["bhav_chalit"] as? Map<String, Any?>)?.get("planets") as? Map<String, Any?>
            if (bcPlanets != null) {
                val res = mutableMapOf<Int, MutableList<String>>()
                bcPlanets.forEach { (planet, info) ->
                    val h = (info as? Map<*, *>)?.get("chalit_house")?.toString()?.toIntOrNull()
                        ?: (info as? Map<*, *>)?.get("house")?.toString()?.toIntOrNull() ?: return@forEach
                    res.getOrPut(h) { mutableListOf() }.add(planetAbbr(planet))
                }
                return res.toMap()
            }
            return d1HouseMap // fallback to D-1 if no BC data
        }
        return when (meta.div) {
            1 -> d1HouseMap
            9 -> navHouseMap
            else -> {
                @Suppress("UNCHECKED_CAST")
                val vc = vargaCharts[meta.code] as? Map<String, Any?>
                    ?: vargaCharts["D${meta.div}"] as? Map<String, Any?>
                    ?: return emptyMap()
                val grahasInVc = vc["grahas"] as? Map<String, Any?> ?: vc
                val res = mutableMapOf<Int, MutableList<String>>()
                grahasInVc.forEach { (planet, info) ->
                    val h = (info as? Map<String, Any?>)?.get("house")?.toString()?.toIntOrNull() ?: return@forEach
                    res.getOrPut(h) { mutableListOf() }.add(planetAbbr(planet))
                }
                res.toMap()
            }
        }
    }

    // Lagna rashi for varga B (navamsha lagna for D-9, same for rest)
    val lagnaBRashi: String = run {
        val meta = VARGA_OPTIONS.getOrElse(vargaB) { VARGA_OPTIONS[0] }
        when {
            meta.code == "BC" -> lagnaRashi
            meta.div == 9 -> data["navamsha_lagna"]?.let { (it as? Map<*, *>)?.get("rashi")?.toString() } ?: lagnaRashi
            else -> lagnaRashi
        }
    }

    val grahaHousesForAspect = mutableMapOf<String, Int>()
    grahasRaw.forEach { (planet, info) ->
        val h = (info as? Map<String, Any?>)?.get("house")?.toString()?.toIntOrNull() ?: return@forEach
        grahaHousesForAspect[planet] = h
    }
    val aspectMap = computeAspects(grahaHousesForAspect)

    @Suppress("UNCHECKED_CAST")
    val bhavasRaw = data["bhavas"] as? List<Map<String, Any?>> ?: data["houses"] as? List<Map<String, Any?>> ?: emptyList()

    // Varga picker dialog
    if (showPickerFor != null) {
        val currentSel = if (showPickerFor == "A") vargaA else vargaB
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPickerFor = null },
            containerColor = BrahmCard,
            title = { Text("Select Chart ${showPickerFor}", fontWeight = FontWeight.SemiBold, color = BrahmForeground) },
            text = {
                Column(
                    modifier = androidx.compose.foundation.rememberScrollState().let {
                        Modifier.verticalScroll(it)
                    },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    VARGA_OPTIONS.forEachIndexed { idx, meta ->
                        val sel = idx == currentSel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) BrahmGold.copy(0.1f) else Color.Transparent)
                                .border(1.dp, if (sel) BrahmGold.copy(0.4f) else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (showPickerFor == "A") vargaA = idx else vargaB = idx
                                    showPickerFor = null
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                meta.code,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (sel) BrahmGold else BrahmForeground,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                modifier = Modifier.width(36.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(meta.name, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                                Text(meta.desc, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
                            }
                            if (sel) Text("✓", color = BrahmGold, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPickerFor = null }) {
                    Text("Cancel", color = BrahmGold)
                }
            },
        )
    }

    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Chart style selector: North / South / East / West
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("North","South","East","West").forEach { style ->
                val sel = chartStyle == style
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (sel) BrahmGold else Color.Transparent)
                        .border(1.dp, if (sel) BrahmGold else BrahmBorder, RoundedCornerShape(20.dp))
                        .clickable { chartStyle = style }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        style,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (sel) Color.White else BrahmMutedForeground,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    )
                }
            }
        }

        // ── Chart A ────────────────────────────────────────────────────────────
        val metaA = VARGA_OPTIONS.getOrElse(vargaA) { VARGA_OPTIONS[0] }
        VargaChartBlock(
            label      = "A",
            meta       = metaA,
            houseMap   = houseMapForVarga(vargaA),
            lagnaRashi = lagnaRashi,
            chartStyle = chartStyle,
            onTapHeader = { showPickerFor = "A" },
        )

        // ── Chart B ────────────────────────────────────────────────────────────
        val metaB = VARGA_OPTIONS.getOrElse(vargaB) { VARGA_OPTIONS[0] }
        VargaChartBlock(
            label      = "B",
            meta       = metaB,
            houseMap   = houseMapForVarga(vargaB),
            lagnaRashi = lagnaBRashi,
            chartStyle = chartStyle,
            onTapHeader = { showPickerFor = "B" },
        )

        // Lagna strip
        BrahmCard(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Lagna", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
                    Text(lagnaRashi, style = MaterialTheme.typography.titleSmall.copy(color = BrahmGold, fontWeight = FontWeight.SemiBold))
                    lagnaMap?.get("nakshatra")?.toString()?.let { nak ->
                        Text(
                            "$nak P${lagnaMap["pada"]?.toString() ?: ""}",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                (data["grahas"] as? Map<*, *>)?.get("Chandra")?.let { moon ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Moon", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
                        Text(
                            (moon as? Map<*, *>)?.get("rashi")?.toString() ?: "—",
                            style = MaterialTheme.typography.titleSmall.copy(color = Color(0xFF93C5FD), fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
                (data["grahas"] as? Map<*, *>)?.get("Surya")?.let { sun ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Sun", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
                        Text(
                            (sun as? Map<*, *>)?.get("rashi")?.toString() ?: "—",
                            style = MaterialTheme.typography.titleSmall.copy(color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }
        }

        // Sub-tab switcher
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("graha" to "Graha Details", "bhava" to "Bhava Details").forEach { (id, label) ->
                val sel = chartSubTab == id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) BrahmGold.copy(0.12f) else Color.Transparent)
                        .border(1.dp, if (sel) BrahmGold.copy(0.4f) else BrahmBorder, RoundedCornerShape(8.dp))
                        .clickable { chartSubTab = id }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (sel) BrahmGold else BrahmMutedForeground,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    )
                }
            }
        }

        if (chartSubTab == "graha") {
            // Graha detail rows — show only for D-1
            val grahaRows: List<Map<String, Any?>> = if (VARGA_OPTIONS.getOrElse(vargaA) { VARGA_OPTIONS[0] }.div == 1) {
                GRAHA_ORDER.mapNotNull { gn ->
                    val g = grahasRaw[gn] as? Map<String, Any?> ?: return@mapNotNull null
                    buildMap {
                        put("name", gn)
                        put("rashi", g["rashi"])
                        put("house", g["house"])
                        put("nakshatra", g["nakshatra"])
                        put("nakshatra_lord", g["nakshatra_lord"])
                        put("pada", g["pada"])
                        put("degree", g["degree"])
                        put("longitude", g["longitude"])
                        put("retro", g["retro"])
                        put("combust", g["combust"])
                        put("status", g["status"])
                    }
                }
            } else if (VARGA_OPTIONS.getOrElse(vargaA) { VARGA_OPTIONS[0] }.div == 9) {
                // Navamsha rows
                GRAHA_ORDER.mapNotNull { gn ->
                    val g = navamshaRaw?.get(gn) as? Map<String, Any?> ?: return@mapNotNull null
                    val d1g = grahasRaw[gn] as? Map<String, Any?>
                    buildMap {
                        put("name", gn)
                        put("rashi", g["rashi"])
                        put("house", g["house"])
                        put("nakshatra", d1g?.get("nakshatra"))
                        put("nakshatra_lord", d1g?.get("nakshatra_lord"))
                        put("pada", d1g?.get("pada"))
                        put("degree", d1g?.get("degree"))
                        put("longitude", d1g?.get("longitude"))
                        put("retro", g["retro"])
                        put("combust", false)
                        put("status", g["status"])
                    }
                }
            } else emptyList()

            if (grahaRows.isNotEmpty()) {
                // Lagna row
                BrahmCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Lagna",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmGold, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.width(56.dp),
                        )
                        Text(
                            lagnaRashi,
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            lagnaMap?.get("nakshatra")?.toString()?.take(10) ?: "—",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 10.sp),
                            modifier = Modifier.width(80.dp),
                        )
                        Text(
                            "H1",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, fontWeight = FontWeight.Medium),
                            modifier = Modifier.width(28.dp),
                            textAlign = TextAlign.End,
                        )
                    }
                }

                grahaRows.forEach { row ->
                    ChartGrahaRow(row = row, today = today)
                }

                Text(
                    "Tap row to expand · R=Retrograde C=Combust",
                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp),
                )
            }
        } else {
            // Bhava sub-tab
            bhavasRaw.forEach { house ->
                val h = house["house"]?.toString()?.toIntOrNull() ?: 0
                val rashi = house["rashi"]?.toString() ?: ""
                val lord = house["lord"]?.toString() ?: ""
                val planetsIn = GRAHA_ORDER.filter { gn ->
                    grahasRaw[gn]?.let { (it as? Map<*, *>)?.get("house")?.toString()?.toIntOrNull() == h } == true
                }
                val aspectors = aspectMap[h] ?: emptyList()
                val isKendra = h in listOf(1, 4, 7, 10)
                BrahmCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(8.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(
                                if (isKendra) BrahmGold.copy(0.15f) else BrahmMuted,
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$h",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isKendra) BrahmGold else BrahmMutedForeground,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (rashi.isNotBlank()) Text(rashi, style = MaterialTheme.typography.bodySmall.copy(color = BrahmGold, fontWeight = FontWeight.Medium))
                                if (lord.isNotBlank()) Text("Lord: $lord", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                            }
                            if (planetsIn.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 2.dp)) {
                                    planetsIn.forEach { gn ->
                                        Text(
                                            gn.take(3),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = GRAHA_COLORS[gn] ?: BrahmGold,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                        )
                                    }
                                }
                            }
                            if (aspectors.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 2.dp)) {
                                    Text(
                                        "Asp: ",
                                        style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp),
                                    )
                                    aspectors.forEach { gn ->
                                        Text(
                                            "${GRAHA_SYMBOLS[gn] ?: ""}${gn.take(3)}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = GRAHA_COLORS[gn] ?: BrahmMutedForeground,
                                                fontSize = 9.sp,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartGrahaRow(row: Map<String, Any?>, today: String) {
    val gn = row["name"]?.toString() ?: return
    var expanded by remember { mutableStateOf(false) }
    val dotColor = GRAHA_COLORS[gn] ?: BrahmGold
    val retro = row["retro"]?.toString() == "true"
    val combust = row["combust"]?.toString() == "true"
    val status = row["status"]?.toString() ?: ""
    val deg = row["degree"]?.toString()?.toDoubleOrNull() ?: 0.0
    val lon = row["longitude"]?.toString()?.toDoubleOrNull() ?: deg

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (expanded) BrahmGold.copy(0.04f) else Color.Transparent)
            .border(1.dp, if (expanded) BrahmGold.copy(0.2f) else BrahmBorder, RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded },
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 7.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                GRAHA_SYMBOLS[gn] ?: "",
                style = MaterialTheme.typography.bodySmall.copy(color = dotColor, fontWeight = FontWeight.Bold),
                modifier = Modifier.width(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                gn,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = BrahmForeground),
                modifier = Modifier.width(52.dp),
            )
            if (retro) Text(
                "℞",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE8445A), fontWeight = FontWeight.Bold),
                modifier = Modifier.width(14.dp),
            )
            else Spacer(Modifier.width(14.dp))
            if (combust) Text(
                "C",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFEA580C), fontWeight = FontWeight.Bold),
                modifier = Modifier.width(14.dp),
            )
            else Spacer(Modifier.width(14.dp))
            Text(
                if (deg > 0) degToDMS(lon) else row["rashi"]?.toString() ?: "—",
                style = MaterialTheme.typography.labelSmall.copy(color = BrahmForeground, fontSize = 10.sp),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "H${row["house"]?.toString() ?: "—"}",
                style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, fontWeight = FontWeight.Medium),
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.End,
            )
        }

        if (expanded) {
            HorizontalDivider(color = BrahmBorder)
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoCell("Rashi", row["rashi"]?.toString() ?: "—", BrahmGold)
                    InfoCell("House", "H${row["house"]?.toString() ?: "—"}", BrahmForeground)
                    InfoCell("Pada", "P${row["pada"]?.toString() ?: "—"}", BrahmMutedForeground)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoCell("Nakshatra", row["nakshatra"]?.toString() ?: "—", Color(0xFF7C6FCD))
                    InfoCell("Nak Lord", row["nakshatra_lord"]?.toString() ?: "—", BrahmMutedForeground)
                    if (status.isNotBlank()) Box { StatusBadge(status) }
                }
            }
        }
    }
}

// ─── Grahas Tab ──────────────────────────────────────────────────────────────

@Composable
fun GrahasTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val grahasRaw = data["grahas"] as? Map<String, Any?> ?: emptyMap()

    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader("Graha Positions — Full Planetary Data")

        // Table header
        BrahmCard(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp).fillMaxWidth()) {
                Text("Graha", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold, fontSize = 10.sp), modifier = Modifier.weight(1.6f))
                Text("R", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold, fontSize = 10.sp), modifier = Modifier.width(16.dp), textAlign = TextAlign.Center)
                Text("C", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold, fontSize = 10.sp), modifier = Modifier.width(16.dp), textAlign = TextAlign.Center)
                Text("Longitude", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold, fontSize = 10.sp), modifier = Modifier.weight(2f))
                Text("Nak/Pada", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold, fontSize = 10.sp), modifier = Modifier.weight(1.8f))
                Text("Status", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold, fontSize = 10.sp), modifier = Modifier.weight(1.4f))
            }
        }

        // Lagna row
        val lagnaMap = data["lagna"] as? Map<String, Any?>
        if (lagnaMap != null) {
            GrahaTableRow(
                symbol = "Asc",
                name = "Lagna",
                color = BrahmGold,
                retro = false,
                combust = false,
                longitude = lagnaMap["full_degree"]?.toString()?.toDoubleOrNull() ?: 0.0,
                rashi = lagnaMap["rashi"]?.toString() ?: "—",
                nakshatra = lagnaMap["nakshatra"]?.toString() ?: "—",
                nakshatraLord = null,
                pada = lagnaMap["pada"]?.toString()?.toIntOrNull() ?: 0,
                status = "",
            )
        }

        GRAHA_ORDER.forEach { gn ->
            val g = grahasRaw[gn] as? Map<String, Any?> ?: return@forEach
            val lon = g["longitude"]?.toString()?.toDoubleOrNull()
                ?: g["full_degree"]?.toString()?.toDoubleOrNull() ?: 0.0
            GrahaTableRow(
                symbol = GRAHA_SYMBOLS[gn] ?: "",
                name = gn,
                color = GRAHA_COLORS[gn] ?: BrahmGold,
                retro = g["retro"]?.toString() == "true",
                combust = g["combust"]?.toString() == "true",
                longitude = lon,
                rashi = g["rashi"]?.toString() ?: "—",
                nakshatra = g["nakshatra"]?.toString() ?: "—",
                nakshatraLord = g["nakshatra_lord"]?.toString(),
                pada = g["pada"]?.toString()?.toIntOrNull() ?: 0,
                status = g["status"]?.toString() ?: "",
                speed = g["speed"]?.toString()?.toDoubleOrNull(),
                latitude = g["lat_ecl"]?.toString()?.toDoubleOrNull(),
                house = g["house"]?.toString()?.toIntOrNull(),
            )
        }

        Text(
            "R = Retrograde · C = Combust · Longitude shown in Rashi DMS format",
            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun GrahaTableRow(
    symbol: String,
    name: String,
    color: Color,
    retro: Boolean,
    combust: Boolean,
    longitude: Double,
    rashi: String,
    nakshatra: String,
    nakshatraLord: String?,
    pada: Int,
    status: String,
    speed: Double? = null,
    latitude: Double? = null,
    house: Int? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (expanded) BrahmGold.copy(0.04f) else BrahmCard)
            .border(1.dp, if (expanded) BrahmGold.copy(0.2f) else BrahmBorder, RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded },
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 7.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1.6f), verticalAlignment = Alignment.CenterVertically) {
                Text(symbol, style = MaterialTheme.typography.bodySmall.copy(color = color, fontWeight = FontWeight.Bold), modifier = Modifier.width(18.dp))
                Text(name.take(7), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = BrahmForeground), maxLines = 1)
            }
            Text(
                if (retro) "℞" else "",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE8445A), fontWeight = FontWeight.Bold),
                modifier = Modifier.width(16.dp),
                textAlign = TextAlign.Center,
            )
            Text(
                if (combust) "C" else "",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFEA580C), fontWeight = FontWeight.Bold),
                modifier = Modifier.width(16.dp),
                textAlign = TextAlign.Center,
            )
            Text(
                if (longitude > 0) degToDMS(longitude) else rashi,
                style = MaterialTheme.typography.labelSmall.copy(color = BrahmForeground, fontSize = 10.sp),
                modifier = Modifier.weight(2f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Column(Modifier.weight(1.8f)) {
                Text(
                    "$nakshatra${if (pada > 0) " P$pada" else ""}",
                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!nakshatraLord.isNullOrBlank()) {
                    Text(
                        nakshatraLord,
                        style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp),
                        maxLines = 1,
                    )
                }
            }
            Box(Modifier.weight(1.4f)) {
                if (status.isNotBlank()) StatusBadge(status)
            }
        }

        if (expanded) {
            HorizontalDivider(color = BrahmBorder)
            Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (house != null) InfoCell("House", "H$house")
                    if (speed != null) InfoCell("Speed", "%.3f°/d".format(speed))
                    if (latitude != null) InfoCell("Lat Ecl", "%.4f°".format(latitude))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (longitude > 0) InfoCell("Longitude", "%.4f°".format(longitude))
                    InfoCell("DMS", degToDMS(longitude))
                }
            }
        }
    }
}

// ─── Dashas Tab ──────────────────────────────────────────────────────────────

private val DASHA_PREDICTIONS = mapOf(
    "Surya" to "Authority, recognition, government favor. Offer water to Sun, Aditya Hridayam.",
    "Chandra" to "Public popularity, real estate, travel. Monday fasts, Pearl, offer milk.",
    "Mangal" to "Energy, courage, property gains. Hanuman Chalisa Tuesdays, Red Coral.",
    "Budha" to "Intelligence, business, communication. Wednesday fasts, Emerald, feed parrots.",
    "Budh" to "Intelligence, business, communication. Wednesday fasts, Emerald, feed parrots.",
    "Guru" to "Fortune, expansion, wisdom, children. Thursday fast, Yellow Sapphire, Brihaspati puja.",
    "Shukra" to "Love, luxury, artistic success. Friday fast, Diamond or White Sapphire, Lakshmi puja.",
    "Shani" to "Discipline, delays, hard work rewarded. Saturday fast, Blue Sapphire if benefic, Shani puja.",
    "Rahu" to "Unconventional success, sudden changes. Gomed, Saturday Rahu puja, Naga puja.",
    "Ketu" to "Spiritual liberation, detachment. Cat's Eye, Ganesha worship, Ketu mantra.",
)

@Composable
fun DashasTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val dashas = data["dasha"] as? List<Map<String, Any?>>
        ?: data["dashas"] as? List<Map<String, Any?>> ?: emptyList()

    val today = today()

    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Vimshottari Dasha — Tap to expand Antardasha",
            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
        )

        if (dashas.isEmpty()) {
            Text("No dasha data found", color = BrahmMutedForeground, style = MaterialTheme.typography.bodyMedium)
        }

        dashas.forEach { dasha ->
            DashaCard(dasha = dasha, today = today)
        }
    }
}

@Composable
private fun DashaCard(dasha: Map<String, Any?>, today: String) {
    val planet = dasha["planet"]?.toString() ?: dasha["lord"]?.toString() ?: "—"
    val start = dasha["start"]?.toString() ?: ""
    val end = dasha["end"]?.toString() ?: ""
    val years = dasha["years"]?.toString() ?: ""
    val isActive = isCurrentPeriod(start, end)
    val dotColor = DASHA_COLORS[planet] ?: BrahmGold
    @Suppress("UNCHECKED_CAST")
    val antardashas = dasha["antardashas"] as? List<Map<String, Any?>> ?: emptyList()
    var expanded by remember { mutableStateOf(isActive) } // auto-expand active

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) Color(0xFFFFF8E7) else BrahmCard)
            .border(
                1.dp,
                if (isActive) Color(0xFFFDE68A) else BrahmBorder,
                RoundedCornerShape(10.dp),
            ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { if (antardashas.isNotEmpty()) expanded = !expanded }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(dotColor))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(planet, style = MaterialTheme.typography.titleSmall.copy(color = dotColor, fontWeight = FontWeight.SemiBold))
                    val en = GRAHA_EN[planet]
                    if (!en.isNullOrBlank()) {
                        Text("($en)", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                    }
                    if (isActive) {
                        Box(
                            Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFD1FAE5)).padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text("Active ★", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF065F46), fontWeight = FontWeight.Bold, fontSize = 9.sp))
                        }
                    }
                }
                Text(
                    "$start  →  $end${if (years.isNotBlank()) "  ($years yrs)" else ""}",
                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 10.sp),
                )
            }
            if (antardashas.isNotEmpty()) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = BrahmMutedForeground,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Active dasha prediction card
        if (isActive) {
            val pred = DASHA_PREDICTIONS[planet]
            if (!pred.isNullOrBlank()) {
                HorizontalDivider(color = Color(0xFFFDE68A))
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "$planet Dasha Theme",
                        style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                    )
                    Text(pred, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            }
        }

        // Antardasha expansion
        if (expanded && antardashas.isNotEmpty()) {
            HorizontalDivider(color = BrahmBorder)
            Column(
                Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "Antardasha",
                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp),
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                antardashas.forEach { ad ->
                    AntardashaRow(ad = ad, today = today, parentColor = dotColor)
                }
            }
        }
    }
}

@Composable
private fun AntardashaRow(ad: Map<String, Any?>, today: String, parentColor: Color) {
    val planet = ad["planet"]?.toString() ?: ad["lord"]?.toString() ?: "—"
    val start = ad["start"]?.toString() ?: ""
    val end = ad["end"]?.toString() ?: ""
    val isActive = isCurrentPeriod(start, end)
    val dotColor = DASHA_COLORS[planet] ?: BrahmGold
    @Suppress("UNCHECKED_CAST")
    val pratyantardashas = ad["pratyantardashas"] as? List<Map<String, Any?>> ?: emptyList()
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(if (isActive) dotColor.copy(0.08f) else Color.Transparent)
                .clickable { if (pratyantardashas.isNotEmpty()) expanded = !expanded }
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(7.dp).clip(RoundedCornerShape(4.dp)).background(dotColor))
            Spacer(Modifier.width(6.dp))
            Text(
                planet,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isActive) dotColor else BrahmForeground,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 11.sp,
                ),
                modifier = Modifier.width(56.dp),
            )
            Text(
                "$start – $end",
                style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp),
                modifier = Modifier.weight(1f),
            )
            if (isActive) Text(
                "★",
                style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold),
            )
            if (pratyantardashas.isNotEmpty()) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = BrahmMutedForeground,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        if (expanded && pratyantardashas.isNotEmpty()) {
            Column(
                Modifier.padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                pratyantardashas.forEach { pd ->
                    val pName = pd["planet"]?.toString() ?: pd["lord"]?.toString() ?: "—"
                    val pStart = pd["start"]?.toString() ?: ""
                    val pEnd = pd["end"]?.toString() ?: ""
                    val pActive = isCurrentPeriod(pStart, pEnd)
                    val pColor = DASHA_COLORS[pName] ?: BrahmMutedForeground
                    Row(
                        Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(5.dp).clip(RoundedCornerShape(3.dp)).background(pColor.copy(0.7f)))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            pName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (pActive) pColor else BrahmMutedForeground,
                                fontWeight = if (pActive) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 10.sp,
                            ),
                            modifier = Modifier.width(50.dp),
                        )
                        Text(
                            "$pStart – $pEnd",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp),
                        )
                        if (pActive) Text(
                            " ★",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontSize = 9.sp),
                        )
                    }
                }
            }
        }
    }
}

// ─── Houses Tab ──────────────────────────────────────────────────────────────

private val RASHI_QUALITY = mapOf(
    "Mesha" to "Mas, Movable", "Vrishabha" to "Fem, Fixed", "Mithuna" to "Mas, Common",
    "Karka" to "Fem, Movable", "Simha" to "Mas, Fixed", "Kanya" to "Fem, Common",
    "Tula" to "Mas, Movable", "Vrischika" to "Fem, Fixed", "Dhanu" to "Mas, Common",
    "Makara" to "Fem, Movable", "Kumbha" to "Mas, Fixed", "Meena" to "Fem, Common",
)

@Composable
fun HousesTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val houses = data["houses"] as? List<Map<String, Any?>>
        ?: data["bhavas"] as? List<Map<String, Any?>> ?: emptyList()
    @Suppress("UNCHECKED_CAST")
    val grahasRaw = data["grahas"] as? Map<String, Any?> ?: emptyMap()

    val grahaHousesForAspect = mutableMapOf<String, Int>()
    grahasRaw.forEach { (planet, info) ->
        val h = (info as? Map<*, *>)?.get("house")?.toString()?.toIntOrNull() ?: return@forEach
        grahaHousesForAspect[planet] = h
    }
    val aspectMap = computeAspects(grahaHousesForAspect)

    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Bhava Chart — Whole Sign Houses",
            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
        )

        if (houses.isEmpty()) {
            // Fallback: build from grahas data
            (1..12).forEach { h ->
                val residents = GRAHA_ORDER.filter { gn ->
                    (grahasRaw[gn] as? Map<*, *>)?.get("house")?.toString()?.toIntOrNull() == h
                }
                val aspectors = aspectMap[h] ?: emptyList()
                HouseCard(
                    houseNum = h,
                    rashi = "",
                    lord = "",
                    signification = BHAVA_MEANINGS.getOrElse(h - 1) { "" },
                    gender = null, modality = null, tattva = null,
                    planetsIn = residents,
                    aspectors = aspectors,
                )
            }
        } else {
            houses.forEach { house ->
                val h = house["house"]?.toString()?.toIntOrNull() ?: 0
                val rashi = house["rashi"]?.toString() ?: ""
                val lord = house["lord"]?.toString() ?: ""
                val signification = house["signification"]?.toString() ?: BHAVA_MEANINGS.getOrElse(h - 1) { "" }
                val gender = house["gender"]?.toString()
                val modality = house["modality"]?.toString()
                val tattva = house["tattva"]?.toString()
                @Suppress("UNCHECKED_CAST")
                val planetsIn = (house["planets"] as? List<*>)?.map { it.toString() }
                    ?: GRAHA_ORDER.filter { gn ->
                        (grahasRaw[gn] as? Map<*, *>)?.get("house")?.toString()?.toIntOrNull() == h
                    }
                val aspectors = aspectMap[h] ?: emptyList()
                HouseCard(
                    houseNum = h,
                    rashi = rashi,
                    lord = lord,
                    signification = signification,
                    gender = gender,
                    modality = modality,
                    tattva = tattva,
                    planetsIn = planetsIn,
                    aspectors = aspectors,
                )
            }
        }
    }
}

@Composable
private fun HouseCard(
    houseNum: Int,
    rashi: String,
    lord: String,
    signification: String,
    gender: String?,
    modality: String?,
    tattva: String?,
    planetsIn: List<String>,
    aspectors: List<String>,
) {
    val isKendra = houseNum in listOf(1, 4, 7, 10)
    val idx = houseNum - 1
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BrahmCard)
            .border(1.dp, BrahmBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(
                if (isKendra) BrahmGold.copy(0.15f) else BrahmMuted,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$houseNum",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (isKendra) BrahmGold else BrahmMutedForeground,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (rashi.isNotBlank()) Text(rashi, style = MaterialTheme.typography.titleSmall.copy(color = BrahmForeground, fontWeight = FontWeight.SemiBold))
                if (lord.isNotBlank()) Text(
                    "Lord: $lord",
                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
                )
            }
            Text(
                BHAVA_NAMES.getOrElse(idx) { "H$houseNum" },
                style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontWeight = FontWeight.Medium, fontSize = 10.sp),
            )
            if (signification.isNotBlank()) Text(
                signification,
                style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp),
            )
            val quality = rashi.let { RASHI_QUALITY[it] } ?: run {
                listOfNotNull(gender, modality, tattva).joinToString(" · ").takeIf { it.isNotBlank() }
            }
            if (!quality.isNullOrBlank()) {
                Text(
                    quality,
                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp),
                )
            }
            if (planetsIn.isNotEmpty()) {
                Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    planetsIn.forEach { gn ->
                        Text(
                            "${GRAHA_SYMBOLS[gn] ?: ""}${gn.take(3)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GRAHA_COLORS[gn] ?: BrahmGold,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                            ),
                        )
                    }
                }
            }
            if (aspectors.isNotEmpty()) {
                Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Asp: ",
                        style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp),
                    )
                    aspectors.forEach { gn ->
                        Text(
                            "${GRAHA_SYMBOLS[gn] ?: ""}${gn.take(3)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GRAHA_COLORS[gn] ?: BrahmMutedForeground,
                                fontSize = 9.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// ─── Strength (Shadbala) Tab ─────────────────────────────────────────────────

@Composable
fun StrengthTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val shadbalaPlanets = (data["shadbala"] as? Map<String, Any?>)?.get("planets") as? Map<String, Any?>
    @Suppress("UNCHECKED_CAST")
    val bhavabala = data["bhavabala"] as? List<Map<String, Any?>>

    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Shadbala — Six-fold Planetary Strength")

        if (shadbalaPlanets == null) {
            Text(
                if (data.containsKey("grahas")) "Shadbala not calculated. Regenerate Kundali." else "Generate Kundali to see Shadbala.",
                color = BrahmMutedForeground,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            val planets7 = listOf("Surya", "Chandra", "Mangal", "Budha", "Guru", "Shukra", "Shani")

            // Summary grid
            val rowCount = (planets7.size + 1) / 2
            for (rowIdx in 0 until rowCount) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0..1) {
                        val gIdx = rowIdx * 2 + col
                        val gn = planets7.getOrNull(gIdx)
                        if (gn != null) {
                            val s = shadbalaPlanets[gn] as? Map<*, *>
                            if (s != null) {
                                val total = s["total_rupas"]?.toString() ?: s["total"]?.toString() ?: "—"
                                val required = s["required_rupas"]?.toString() ?: "—"
                                val ratio = s["ratio"]?.toString()?.toDoubleOrNull() ?: 0.0
                                val isStrong = s["is_strong"]?.toString() == "true" || ratio >= 1.0
                                val pct = (ratio * 100).toInt().coerceIn(0, 100)
                                val strengthColor = if (isStrong) Color(0xFF16A34A) else BrahmError
                                BrahmCard(modifier = Modifier.weight(1f)) {
                                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(GRAHA_SYMBOLS[gn] ?: "", style = MaterialTheme.typography.bodySmall.copy(color = GRAHA_COLORS[gn] ?: BrahmGold))
                                            Text(GRAHA_EN[gn] ?: gn, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
                                        }
                                        Text("${total}R", style = MaterialTheme.typography.titleSmall.copy(color = strengthColor, fontWeight = FontWeight.Bold))
                                        Text("Need ${required}R", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp))
                                        Box(
                                            Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(BrahmMuted),
                                        ) {
                                            Box(
                                                Modifier.fillMaxWidth(pct / 100f).fillMaxHeight().background(strengthColor),
                                            )
                                        }
                                        Text(
                                            if (isStrong) "✓ Strong" else "✗ Weak",
                                            style = MaterialTheme.typography.labelSmall.copy(color = strengthColor, fontWeight = FontWeight.SemiBold, fontSize = 9.sp),
                                        )
                                    }
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // Detailed component table
            SectionHeader("Bala Components (Virupas)")
            BrahmCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    // Header row
                    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                        Text(
                            "Component",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                            modifier = Modifier.width(80.dp),
                        )
                        planets7.forEach { gn ->
                            if (shadbalaPlanets[gn] != null) {
                                Text(
                                    GRAHA_SYMBOLS[gn] ?: gn.take(2),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = GRAHA_COLORS[gn] ?: BrahmGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = BrahmBorder)
                    val components = listOf(
                        "Sthana" to "sthana_bala",
                        "  ↳ Uccha" to null,
                        "  ↳ Oja-Yugma" to null,
                        "  ↳ Kendradi" to null,
                        "  ↳ Drekkana" to null,
                        "Dig" to "dig_bala",
                        "Kala" to "kaala_bala",
                        "Chesta" to "chesta_bala",
                        "Naisargika" to "naisargika_bala",
                        "Drik" to "drik_bala",
                    )
                    val subKeys = mapOf(
                        "  ↳ Uccha" to "uccha",
                        "  ↳ Oja-Yugma" to "oja_yugma",
                        "  ↳ Kendradi" to "kendradi",
                        "  ↳ Drekkana" to "drekkana",
                    )
                    components.forEach { (label, key) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BrahmMutedForeground,
                                    fontSize = if (key == null) 9.sp else 10.sp,
                                    fontWeight = if (key != null) FontWeight.Medium else FontWeight.Normal,
                                ),
                                modifier = Modifier.width(80.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            planets7.forEach { gn ->
                                val s = shadbalaPlanets[gn] as? Map<*, *> ?: return@forEach
                                val value = if (key != null) {
                                    (s[key] as? String) ?: s[key]?.toString() ?: "—"
                                } else {
                                    val sub = subKeys[label] ?: ""
                                    @Suppress("UNCHECKED_CAST")
                                    (s["sthana_detail"] as? Map<String, Any?>)?.get(sub)?.toString() ?: "—"
                                }
                                Text(
                                    value.take(5),
                                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmForeground, fontSize = 9.sp),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = BrahmBorder)
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text(
                            "Total (R)",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrahmForeground, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            modifier = Modifier.width(80.dp),
                        )
                        planets7.forEach { gn ->
                            val s = shadbalaPlanets[gn] as? Map<*, *> ?: return@forEach
                            val isStrong = s["is_strong"]?.toString() == "true"
                            Text(
                                s["total_rupas"]?.toString() ?: s["total"]?.toString() ?: "—",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isStrong) Color(0xFF16A34A) else BrahmError,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                ),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        // Bhavabala
        if (bhavabala != null && bhavabala.isNotEmpty()) {
            SectionHeader("Bhavabala — House Strength")
            val rowCount = (bhavabala.size + 2) / 3
            for (rowIdx in 0 until rowCount) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (col in 0..2) {
                        val bh = bhavabala.getOrNull(rowIdx * 3 + col)
                        if (bh != null) {
                            val strength = bh["strength"]?.toString()?.toDoubleOrNull() ?: 0.0
                            val rank = bh["rank"]?.toString() ?: ""
                            val lord = bh["lord"]?.toString() ?: ""
                            val houseNum = bh["house"]?.toString() ?: "—"
                            val pct = (strength / 12.0 * 100).toInt().coerceIn(0, 100)
                            BrahmCard(modifier = Modifier.weight(1f)) {
                                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("H$houseNum", style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontWeight = FontWeight.Bold))
                                        Text("#$rank", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp))
                                    }
                                    Text("${strength}R", style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, fontWeight = FontWeight.Medium))
                                    Text(lord, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp))
                                    Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(BrahmMuted)) {
                                        Box(Modifier.fillMaxWidth(pct / 100f).fillMaxHeight().background(BrahmGold.copy(0.6f)))
                                    }
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// ─── Ashtakavarga Tab ────────────────────────────────────────────────────────

@Composable
fun AshtakavargaTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val av = data["ashtakavarga"] as? Map<String, Any?>

    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Ashtakavarga — Bindu Points per Rashi")

        if (av == null) {
            Text(
                if (data.containsKey("grahas")) "Ashtakavarga not calculated. Regenerate Kundali." else "Generate Kundali to see Ashtakavarga.",
                color = BrahmMutedForeground,
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }

        @Suppress("UNCHECKED_CAST")
        val sav = av["sarva"] as? Map<String, Any?> ?: av["sav"] as? Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val savPoints = (sav?.get("points") as? List<*>)?.map { it.toString().toIntOrNull() ?: 0 } ?: emptyList()
        val savTotal = sav?.get("total")?.toString() ?: ""

        @Suppress("UNCHECKED_CAST")
        val reducedSav = av["reduced_sarva"] as? Map<String, Any?> ?: av["reduced_sav"] as? Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val reducedPoints = (reducedSav?.get("points") as? List<*>)?.map { it.toString().toIntOrNull() ?: 0 } ?: emptyList()
        val reducedTotal = reducedSav?.get("total")?.toString() ?: ""

        @Suppress("UNCHECKED_CAST")
        val bav = av["bhinnashtakavarga"] as? Map<String, Any?> ?: av["bav"] as? Map<String, Any?>

        // SAV grid
        if (savPoints.isNotEmpty()) {
            Text(
                "Sarvashtakavarga (SAV) — Total Bindus · ≥30 = Favourable",
                style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp),
            )
            // 2 rows of 6
            for (rowIdx in 0..1) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0..5) {
                        val i = rowIdx * 6 + col
                        val pts = savPoints.getOrNull(i) ?: 0
                        val rashi = RASHI_NAMES.getOrElse(i) { "" }
                        val isGood = pts >= 30
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isGood) Color(0xFFD1FAE5) else Color(0xFFFEE2E2))
                                .border(1.dp, if (isGood) Color(0xFF6EE7B7) else Color(0xFFFCA5A5), RoundedCornerShape(6.dp))
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    rashi.take(3),
                                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp),
                                )
                                Text(
                                    "$pts",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = if (isGood) Color(0xFF065F46) else Color(0xFF991B1B),
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
            if (savTotal.isNotBlank()) {
                Text(
                    "Total: $savTotal · ≥30 per rashi = favourable",
                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp),
                )
            }
        }

        // Reduced SAV
        if (reducedPoints.isNotEmpty()) {
            Text(
                "Reduced SAV (after Trikona Shodhana)",
                style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp),
            )
            for (rowIdx in 0..1) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0..5) {
                        val i = rowIdx * 6 + col
                        val pts = reducedPoints.getOrNull(i) ?: 0
                        val rashi = RASHI_NAMES.getOrElse(i) { "" }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(BrahmMuted)
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(rashi.take(3), style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp))
                                Text("$pts", style = MaterialTheme.typography.titleSmall.copy(color = BrahmForeground, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
            if (reducedTotal.isNotBlank()) {
                Text("Total: $reducedTotal", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
            }
        }

        // BAV table — per planet
        if (bav != null) {
            SectionHeader("Bhinnashtakavarga (BAV) — Per Planet")
            val planets7 = listOf("Surya", "Chandra", "Mangal", "Budha", "Guru", "Shukra", "Shani")

            BrahmCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    // Header
                    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                        Text(
                            "Planet",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                            modifier = Modifier.width(48.dp),
                        )
                        RASHI_NAMES.forEach { rashi ->
                            Text(
                                rashi.take(2),
                                style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 8.sp),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                            )
                        }
                        Text(
                            "Σ",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            modifier = Modifier.width(20.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                    HorizontalDivider(color = BrahmBorder)

                    planets7.forEach { gn ->
                        @Suppress("UNCHECKED_CAST")
                        val bavData = bav[gn] as? Map<String, Any?> ?: return@forEach
                        @Suppress("UNCHECKED_CAST")
                        val points = (bavData["points"] as? List<*>)?.map { it.toString().toIntOrNull() ?: 0 } ?: return@forEach
                        val total = bavData["total"]?.toString() ?: "—"
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                GRAHA_SYMBOLS[gn] ?: gn.take(2),
                                style = MaterialTheme.typography.bodySmall.copy(color = GRAHA_COLORS[gn] ?: BrahmGold, fontSize = 10.sp),
                                modifier = Modifier.width(48.dp),
                            )
                            points.forEach { pt ->
                                Text(
                                    "$pt",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = when {
                                            pt >= 4 -> Color(0xFF16A34A)
                                            pt <= 2 -> BrahmError.copy(0.7f)
                                            else -> BrahmForeground
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = if (pt >= 4) FontWeight.SemiBold else FontWeight.Normal,
                                    ),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                )
                            }
                            Text(
                                total,
                                style = MaterialTheme.typography.labelSmall.copy(color = BrahmForeground, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                modifier = Modifier.width(20.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    // SAV row
                    if (savPoints.isNotEmpty()) {
                        HorizontalDivider(color = BrahmBorder)
                        Row(Modifier.fillMaxWidth().padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "SAV",
                                style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                modifier = Modifier.width(48.dp),
                            )
                            savPoints.forEach { pt ->
                                Text(
                                    "$pt",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (pt >= 30) Color(0xFF16A34A) else BrahmError,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                    ),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                )
                            }
                            Text(
                                savTotal,
                                style = MaterialTheme.typography.labelSmall.copy(color = BrahmForeground, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                modifier = Modifier.width(20.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            Text(
                "≥4 bindus = green (good) · ≤2 = red (weak)",
                style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp),
            )
        }
    }
}

// ─── Upagraha Tab ────────────────────────────────────────────────────────────

@Composable
fun UpagrahaTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val upagraha = data["upagraha"] as? Map<String, Any?>

    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Upagraha — Sub-planetary Points")

        if (upagraha == null || upagraha.isEmpty()) {
            Text(
                if (data.containsKey("grahas")) "Upagraha not calculated. Regenerate Kundali." else "Generate Kundali to see Upagrahas.",
                color = BrahmMutedForeground,
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }

        upagraha.forEach { (name, entry) ->
            val e = entry as? Map<*, *> ?: return@forEach
            val isMalefic = MALEFIC_UPAGRAHAS.contains(name)
            val dotColor = if (isMalefic) BrahmError else Color(0xFF22C55E)
            BrahmCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(8.dp).offset(y = 4.dp).clip(RoundedCornerShape(4.dp)).background(dotColor),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                name,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = BrahmForeground,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                            Box(
                                Modifier.clip(RoundedCornerShape(4.dp)).background(dotColor.copy(0.12f)).padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    if (isMalefic) "Malefic" else "Benefic",
                                    style = MaterialTheme.typography.labelSmall.copy(color = dotColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column {
                                Text("Rashi", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp))
                                Text(
                                    e["rashi_name"]?.toString() ?: e["rashi"]?.toString() ?: "—",
                                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmGold, fontWeight = FontWeight.Medium),
                                )
                            }
                            Column {
                                Text("Longitude", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp))
                                val dms = e["dms"]?.toString() ?: run {
                                    val lon = e["longitude"]?.toString()?.toDoubleOrNull()
                                        ?: e["lon"]?.toString()?.toDoubleOrNull() ?: 0.0
                                    if (lon > 0) degToDMS(lon) else "—"
                                }
                                Text(dms, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, fontWeight = FontWeight.Medium))
                            }
                            val nak = e["nakshatra"]?.toString()
                            if (!nak.isNullOrBlank()) {
                                Column {
                                    Text("Nakshatra", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp))
                                    Text(nak, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                                    val lord = e["nakshatra_lord"]?.toString()
                                    if (!lord.isNullOrBlank()) {
                                        Text(lord, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp))
                                    }
                                }
                            }
                        }
                        val significance = e["significance"]?.toString()
                        if (!significance.isNullOrBlank()) {
                            val shortSig = significance.split(" — ").getOrElse(1) { significance }
                            Text(
                                shortSig,
                                style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        Text(
            "Sun-derived: Dhuma, Vyatipata, Parivesha, Indrachapa, Upaketu\nHora-based: Mandi, Gulika, Kala, Mrityu, ArdhaPrahara, YamaGhantaka",
            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp),
        )
    }
}

// ─── Yogas Tab ───────────────────────────────────────────────────────────────

@Composable
fun YogasTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val yogas = data["yogas"] as? List<Map<String, Any?>> ?: emptyList()

    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val presentCount = yogas.count { it["present"]?.toString() != "false" }
        Text(
            "$presentCount active of ${yogas.size} total yogas — tap any yoga for remedies",
            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
        )

        if (yogas.isEmpty()) {
            Text("No yogas found", color = BrahmMutedForeground, style = MaterialTheme.typography.bodyMedium)
        }

        yogas.forEach { yoga ->
            YogaCard(yoga = yoga)
        }
    }
}

@Composable
private fun YogaCard(yoga: Map<String, Any?>) {
    val name = yoga["name"]?.toString() ?: "—"
    val desc = yoga["description"]?.toString() ?: yoga["desc"]?.toString() ?: ""
    val strength = yoga["strength"]?.toString() ?: ""
    val category = yoga["category"]?.toString() ?: ""
    val isPresent = yoga["present"]?.toString() != "false"
    val mantra = yoga["mantra"]?.toString()
    val gemstone = yoga["gemstone"]?.toString()
    val deity = yoga["deity"]?.toString()
    val remedy = yoga["remedy"]?.toString()
    val hasRemedies = !mantra.isNullOrBlank() || !gemstone.isNullOrBlank() || !deity.isNullOrBlank() || !remedy.isNullOrBlank()

    var expanded by remember { mutableStateOf(false) }

    val catColor = YOGA_CATEGORY_COLORS[category] ?: BrahmMutedForeground
    val (cardBg, cardBorder) = when {
        !isPresent -> BrahmCard to BrahmBorder.copy(0.5f)
        strength == "Very Strong" -> Color(0xFFD1FAE5) to Color(0xFF6EE7B7)
        strength == "Strong" -> Color(0xFFFEF3C7) to Color(0xFFFDE68A)
        else -> BrahmCard to BrahmBorder
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
            .then(if (!isPresent) Modifier else Modifier),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { if (isPresent && hasRemedies) expanded = !expanded }
                .padding(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                if (isPresent) "✓" else "✗",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isPresent) Color(0xFF16A34A) else BrahmError.copy(0.5f),
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.padding(end = 6.dp, top = 2.dp),
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = if (isPresent) BrahmForeground else BrahmMutedForeground.copy(0.7f),
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    if (category.isNotBlank()) {
                        Text(category, style = MaterialTheme.typography.labelSmall.copy(color = catColor, fontSize = 9.sp))
                    }
                }
                if (isPresent && desc.isNotBlank()) {
                    Text(desc, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp), modifier = Modifier.padding(top = 2.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (isPresent && strength.isNotBlank()) {
                    val (sBg, sFg) = when (strength) {
                        "Very Strong" -> Color(0xFFD1FAE5) to Color(0xFF065F46)
                        "Strong" -> Color(0xFFFEF3C7) to Color(0xFF92400E)
                        else -> Color(0xFFF3F4F6) to Color(0xFF6B7280)
                    }
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(sBg).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(strength, style = MaterialTheme.typography.labelSmall.copy(color = sFg, fontSize = 9.sp, fontWeight = FontWeight.SemiBold))
                    }
                }
                if (isPresent && hasRemedies) {
                    Spacer(Modifier.height(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = BrahmMutedForeground,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        if (isPresent && expanded && hasRemedies) {
            HorizontalDivider(color = BrahmBorder)
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Remedies",
                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                )
                listOf(
                    "🕉" to "Mantra" to mantra,
                    "💎" to "Gemstone" to gemstone,
                    "🪔" to "Deity" to deity,
                    "🌿" to "Remedy" to remedy,
                ).forEach { (iconLabel, value) ->
                    val (icon, label) = iconLabel
                    if (!value.isNullOrBlank()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(icon, fontSize = 12.sp)
                            Column {
                                Text(label, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp))
                                Text(value, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Lagna Tab ───────────────────────────────────────────────────────────────

@Composable
fun LagnaTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val lagnaMap = data["lagna"] as? Map<String, Any?>
    @Suppress("UNCHECKED_CAST")
    val navamshaLagna = data["navamsha_lagna"] as? Map<String, Any?>
    @Suppress("UNCHECKED_CAST")
    val birthPanchang = data["birth_panchang"] as? Map<String, Any?>
    @Suppress("UNCHECKED_CAST")
    val personal = data["personal"] as? Map<String, Any?>

    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Lagna & Ascendant Details")

        if (lagnaMap == null) {
            Text("Generate Kundali to see Lagna details", color = BrahmMutedForeground, style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        // Lagna info grid
        val lagnaRashi = lagnaMap["rashi"]?.toString() ?: "—"
        val lagnaRashiEn = lagnaMap["rashi_en"]?.toString() ?: ""
        val lagnaNak = lagnaMap["nakshatra"]?.toString() ?: "—"
        val lagnaPada = lagnaMap["pada"]?.toString() ?: "—"
        val lagnaDeg = lagnaMap["degree"]?.toString()?.toDoubleOrNull()
        val lagnaFullDeg = lagnaMap["full_degree"]?.toString()?.toDoubleOrNull()
        val ayanaRaw = data["ayanamsha"]?.toString() ?: ""
        val ayanaLabel = data["ayanamsha_label"]?.toString() ?: "Lahiri"
        val rahuMode = data["rahu_mode"]?.toString()

        val lagnaItems = listOf(
            "Lagna Rashi" to "$lagnaRashi${if (lagnaRashiEn.isNotBlank()) " ($lagnaRashiEn)" else ""}",
            "Nakshatra" to lagnaNak,
            "Pada" to "P$lagnaPada",
            "Degree" to "${lagnaDeg?.let { "%.2f°".format(it) } ?: "—"}${lagnaFullDeg?.let { " (%.2f° abs)".format(it) } ?: ""}",
            "Navamsha Lagna" to (navamshaLagna?.get("rashi")?.toString() ?: "—"),
            "Ayanamsha" to "$ayanaRaw° ($ayanaLabel)",
            "Rahu/Ketu Mode" to if (rahuMode == "true") "True Node" else "Mean Node",
            "Place" to (data["place"]?.toString() ?: "—"),
        )

        val rowCount = (lagnaItems.size + 1) / 2
        for (rowIdx in 0 until rowCount) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (col in 0..1) {
                    val item = lagnaItems.getOrNull(rowIdx * 2 + col)
                    if (item != null) {
                        InfoCellWide(item.first, item.second, modifier = Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Birth Panchang
        if (birthPanchang != null) {
            SectionHeader("Birth Panchang")
            val panchangItems = listOf(
                "Tithi" to "${birthPanchang["tithi"]?.toString() ?: "—"} (${birthPanchang["paksha"]?.toString() ?: ""})",
                "Vara (Day)" to (birthPanchang["vara"]?.toString() ?: "—"),
                "Nakshatra" to (birthPanchang["moon_nakshatra"]?.toString() ?: birthPanchang["yoga"]?.toString() ?: "—"),
                "Yoga" to (birthPanchang["yoga"]?.toString() ?: "—"),
                "Karana" to (birthPanchang["karana"]?.toString() ?: "—"),
                "Sunrise" to (birthPanchang["sunrise"]?.toString() ?: "—"),
                "Sunset" to (birthPanchang["sunset"]?.toString() ?: "—"),
                "Moon Sign" to (birthPanchang["moonsign"]?.toString() ?: birthPanchang["moon_sign"]?.toString() ?: "—"),
                "Sun Sign" to (birthPanchang["sunsign"]?.toString() ?: birthPanchang["sun_sign"]?.toString() ?: "—"),
                "Surya Nak." to (birthPanchang["surya_nakshatra"]?.toString() ?: "—"),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                panchangItems.forEach { (k, v) ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(BrahmMuted.copy(0.4f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(k, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        Text(v, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, fontWeight = FontWeight.Medium))
                    }
                }
            }
        }

        // Personal characteristics
        if (!personal.isNullOrEmpty()) {
            SectionHeader("Personal Characteristics")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                personal.forEach { (k, v) ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(BrahmMuted.copy(0.4f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(k.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        Text(v.toString(), style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, fontWeight = FontWeight.Medium))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCellWide(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(BrahmMuted.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, fontWeight = FontWeight.Medium),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─── Chalit Tab ──────────────────────────────────────────────────────────────

@Composable
fun ChalitTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val bhavChalit = data["bhav_chalit"] as? Map<String, Any?>

    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Bhav Chalit — Placidus House Cusps (Sidereal)")

        if (bhavChalit == null) {
            Text(
                if (data.containsKey("grahas")) "Bhav Chalit not calculated. Regenerate Kundali." else "Generate Kundali to see Bhav Chalit.",
                color = BrahmMutedForeground,
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }

        @Suppress("UNCHECKED_CAST")
        val cuspsSid = (bhavChalit["cusps_sid"] as? List<*>)?.mapNotNull { it.toString().toDoubleOrNull() }
        @Suppress("UNCHECKED_CAST")
        val planetsChalit = bhavChalit["planets"] as? Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val grahasRaw = data["grahas"] as? Map<String, Any?>

        // 12 House cusps
        if (!cuspsSid.isNullOrEmpty()) {
            Text("12 House Cusps", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
            val rowCount = (cuspsSid.size + 1) / 2
            for (rowIdx in 0 until rowCount) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0..1) {
                        val i = rowIdx * 2 + col
                        val cusp = cuspsSid.getOrNull(i)
                        if (cusp != null) {
                            val rashiIdx = (cusp / 30).toInt().coerceIn(0, 11)
                            val deg = "%.2f".format(cusp % 30)
                            val rashi = RASHI_NAMES.getOrElse(rashiIdx) { "?" }
                            Row(
                                Modifier.weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrahmMuted)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${i + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.width(18.dp),
                                )
                                Text(
                                    rashi,
                                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground),
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "$deg°",
                                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp),
                                )
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Planet Rashi vs Chalit comparison
        if (planetsChalit != null && grahasRaw != null) {
            SectionHeader("Planet Placements (Rashi → Chalit)")
            val shiftedPlanets = GRAHA_ORDER.filter { gn ->
                val chHouse = planetsChalit[gn]?.toString()?.toIntOrNull()
                val rashiHouse = (grahasRaw[gn] as? Map<*, *>)?.get("house")?.toString()?.toIntOrNull()
                chHouse != null && rashiHouse != null && chHouse != rashiHouse
            }
            if (shiftedPlanets.isNotEmpty()) {
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF8E7))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                ) {
                    Text(
                        "Shifted planets: ${shiftedPlanets.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB45309)),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GRAHA_ORDER.forEach { gn ->
                    val chHouse = planetsChalit[gn]?.toString()?.toIntOrNull() ?: return@forEach
                    val rashiHouse = (grahasRaw[gn] as? Map<*, *>)?.get("house")?.toString()?.toIntOrNull()
                    val shifted = rashiHouse != null && chHouse != rashiHouse
                    val planetColor = GRAHA_COLORS[gn] ?: BrahmGold
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (shifted) Color(0xFFFFF8E7) else BrahmCard)
                            .border(
                                1.dp,
                                if (shifted) Color(0xFFFDE68A) else BrahmBorder,
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            GRAHA_SYMBOLS[gn] ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = planetColor, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            modifier = Modifier.width(20.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(gn, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, fontWeight = FontWeight.Medium))
                            val en = GRAHA_EN[gn]
                            if (!en.isNullOrBlank()) Text(en, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp))
                        }
                        if (rashiHouse != null) {
                            Text(
                                "Rashi: H$rashiHouse",
                                style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                            )
                        }
                        Text(
                            "Chalit: H$chHouse",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (shifted) Color(0xFFB45309) else BrahmForeground,
                                fontWeight = if (shifted) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                        )
                        if (shifted) {
                            Box(
                                Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFFDE68A).copy(0.5f)).padding(horizontal = 4.dp, vertical = 2.dp),
                            ) {
                                Text("shifted", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF92400E), fontSize = 8.sp, fontWeight = FontWeight.SemiBold))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Navamsha Tab (kept for backward compat) ─────────────────────────────────

@Composable
fun NavamshaTab(data: Map<String, Any?>) {
    @Suppress("UNCHECKED_CAST")
    val navamsha = data["navamsha"] as? Map<String, Any?> ?: emptyMap()
    val grahas = run {
        val result = mutableMapOf<Int, MutableList<String>>()
        navamsha.forEach { (planet, info) ->
            val house = (info as? Map<String, Any?>)?.get("house")?.toString()?.toIntOrNull() ?: return@forEach
            result.getOrPut(house) { mutableListOf() }.add(planetAbbr(planet))
        }
        result.toMap()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        SectionHeader("Navamsha (D9) Chart")
        Spacer(Modifier.height(12.dp))
        KundaliChartView(grahas = grahas, modifier = Modifier.fillMaxWidth())
    }
}
