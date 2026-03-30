package com.bimoraai.brahm.ui.kp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimoraai.brahm.core.components.BirthInputFields
import com.bimoraai.brahm.core.components.BrahmButton
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ── Constants ─────────────────────────────────────────────────────────────────

private val GRAHA_ORDER = listOf("Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani","Rahu","Ketu")

private val GRAHA_EN = mapOf(
    "Surya" to "Sun", "Chandra" to "Moon", "Mangal" to "Mars", "Budh" to "Mercury",
    "Guru" to "Jupiter", "Shukra" to "Venus", "Shani" to "Saturn", "Rahu" to "Rahu", "Ketu" to "Ketu",
)

private val GRAHA_COLOR = mapOf(
    "Surya"  to Color(0xFFF59E0B),
    "Chandra" to Color(0xFF94A3B8),
    "Mangal" to Color(0xFFEF4444),
    "Budh"   to Color(0xFF22C55E),
    "Guru"   to Color(0xFFEAB308),
    "Shukra" to Color(0xFFA855F7),
    "Shani"  to Color(0xFF64748B),
    "Rahu"   to Color(0xFF6366F1),
    "Ketu"   to Color(0xFFF97316),
)

// ── Helper extensions ─────────────────────────────────────────────────────────

private fun JsonObject.str(key: String) = this[key]?.jsonPrimitive?.contentOrNull ?: ""
private fun JsonObject.obj(key: String) = try { this[key]?.jsonObject } catch (_: Exception) { null }

private val RASHI_SYMBOLS = mapOf(
    "Mesha" to "♈\uFE0E", "Vrishabha" to "♉\uFE0E", "Mithuna" to "♊\uFE0E", "Karka" to "♋\uFE0E",
    "Simha" to "♌\uFE0E", "Kanya" to "♍\uFE0E", "Tula" to "♎\uFE0E", "Vrischika" to "♏\uFE0E",
    "Dhanu" to "♐\uFE0E", "Makara" to "♑\uFE0E", "Kumbha" to "♒\uFE0E", "Meena" to "♓\uFE0E",
)

// ── Sub Lord Badge ─────────────────────────────────────────────────────────────

@Composable
private fun SubLordBadge(lord: String, small: Boolean = false) {
    val color = GRAHA_COLOR[lord] ?: Color(0xFF888888)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .padding(horizontal = if (small) 4.dp else 6.dp, vertical = 2.dp),
    ) {
        Text(
            lord,
            style = MaterialTheme.typography.labelSmall.copy(
                color = color,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (small) 9.sp else 10.sp,
            ),
        )
    }
}

// ── Main content ──────────────────────────────────────────────────────────────

@Composable
fun KPContent(data: JsonObject) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // planets is a JsonObject keyed by planet name
    val planetsObj   = data.obj("planets")
    val cuspsArr     = try { data["cusps"]?.jsonArray?.map { it.jsonObject } } catch (_: Exception) { null }
    val sigsObj      = data.obj("significators")
    val lagnaObj     = data.obj("lagna")
    val ayanamsha    = data["ayanamsha"]?.jsonPrimitive?.doubleOrNull

    val listState = rememberLazyListState()

    Column(Modifier.fillMaxSize().background(BrahmBackground)) {

        // ── Tab toggle ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrahmBackground)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            val tabs = listOf("Planet Sub-Lords", "Cusp Sub-Lords", "Significators")
            tabs.forEachIndexed { idx, label ->
                val selected = selectedTab == idx
                val shape = when (idx) {
                    0              -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    tabs.lastIndex -> RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                    else           -> RoundedCornerShape(0.dp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shape)
                        .background(if (selected) BrahmGold else Color.Transparent)
                        .border(1.dp, if (selected) BrahmGold else BrahmBorder, shape)
                        .clickable { selectedTab = idx }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (selected) Color.White else BrahmMutedForeground,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 10.sp,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }

        Box(Modifier.weight(1f)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Lagna Summary ──
            if (lagnaObj != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BrahmGold.copy(alpha = 0.06f)),
                        modifier = Modifier.fillMaxWidth().border(1.dp, BrahmGold.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("KP LAGNA", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, letterSpacing = 1.sp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    RASHI_SYMBOLS[lagnaObj.str("rashi")] ?: "",
                                    fontSize = 26.sp,
                                    color = BrahmGold,
                                    fontWeight = FontWeight.Light,
                                )
                                Text(lagnaObj.str("rashi"), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    "${"%.2f".format(lagnaObj["longitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0)}°",
                                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                                )
                                if (ayanamsha != null) {
                                    Text(
                                        "Ayanamsha: ${"%.4f".format(ayanamsha)}°",
                                        style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
                                        modifier = Modifier.weight(1f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Star:", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                                SubLordBadge(lagnaObj.str("star_lord"))
                                Text("Sub:", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                                SubLordBadge(lagnaObj.str("sub_lord"))
                                Text("Sub-Sub:", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                                SubLordBadge(lagnaObj.str("sub_sub_lord"), small = true)
                            }
                        }
                    }
                }
            }

            when (selectedTab) {

                // ── Planets tab ──
                0 -> item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BrahmCard),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Horizontal scroll for wide table
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            Column(Modifier.width(640.dp)) {
                                // Header
                                Row(
                                    Modifier.fillMaxWidth()
                                        .background(BrahmGold.copy(alpha = 0.08f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    listOf("Planet" to 1.4f, "Rashi" to 1.1f, "Deg" to 0.7f, "Star Lord" to 1.1f, "Sub Lord" to 1.1f, "Sub-Sub" to 1.0f, "Sig" to 1.0f, "R" to 0.4f).forEach { (h, w) ->
                                        Text(h, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.Bold), modifier = Modifier.weight(w))
                                    }
                                }
                                HorizontalDivider(color = BrahmBorder)

                                if (planetsObj == null) {
                                    Text("No planet data", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                                } else {
                                    GRAHA_ORDER.forEachIndexed { idx, gname ->
                                        val p = planetsObj.obj(gname) ?: return@forEachIndexed
                                        val color = GRAHA_COLOR[gname] ?: BrahmForeground
                                        val sigs = try {
                                            sigsObj?.get(gname)?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull }
                                        } catch (_: Exception) { null }
                                        val retro = p["retro"]?.jsonPrimitive?.booleanOrNull ?: false

                                        if (idx > 0) HorizontalDivider(color = BrahmBorder.copy(alpha = 0.4f))
                                        Row(
                                            Modifier.fillMaxWidth()
                                                .background(if (idx % 2 == 0) BrahmMutedForeground.copy(alpha = 0.03f) else Color.Transparent)
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            // Planet
                                            Column(Modifier.weight(1.4f)) {
                                                Text(gname, style = MaterialTheme.typography.labelMedium.copy(color = color, fontWeight = FontWeight.SemiBold))
                                                Text(GRAHA_EN[gname] ?: "", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp))
                                            }
                                            // Rashi
                                            Row(Modifier.weight(1.1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                val sym = RASHI_SYMBOLS[p.str("rashi")]
                                                if (sym != null) Text(sym, fontSize = 13.sp, color = BrahmGold, fontWeight = FontWeight.Light)
                                                Text(p.str("rashi"), style = MaterialTheme.typography.bodySmall)
                                            }
                                            // Degree
                                            Text(
                                                "${"%.1f".format(p["degree"]?.jsonPrimitive?.doubleOrNull ?: 0.0)}°",
                                                style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                                                modifier = Modifier.weight(0.7f),
                                            )
                                            // Star Lord
                                            Box(Modifier.weight(1.1f)) { SubLordBadge(p.str("star_lord")) }
                                            // Sub Lord
                                            Box(Modifier.weight(1.1f)) { SubLordBadge(p.str("sub_lord")) }
                                            // Sub-Sub
                                            Box(Modifier.weight(1.0f)) { SubLordBadge(p.str("sub_sub_lord"), small = true) }
                                            // Significators
                                            Row(Modifier.weight(1.0f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                sigs?.take(4)?.forEach { h ->
                                                    Box(
                                                        Modifier.clip(RoundedCornerShape(3.dp))
                                                            .background(BrahmMutedForeground.copy(alpha = 0.1f))
                                                            .padding(horizontal = 3.dp, vertical = 1.dp),
                                                    ) {
                                                        Text("H$h", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                                                    }
                                                }
                                            }
                                            // Retro
                                            Box(Modifier.weight(0.4f), contentAlignment = Alignment.Center) {
                                                if (retro) Text("℞", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFF59E0B)))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Cusps tab ──
                1 -> item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BrahmCard),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            Column(Modifier.width(500.dp)) {
                                // Header
                                Row(
                                    Modifier.fillMaxWidth()
                                        .background(BrahmGold.copy(alpha = 0.08f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    listOf("House" to 0.7f, "Rashi" to 1.2f, "Degree" to 0.9f, "Star Lord" to 1.1f, "Sub Lord" to 1.1f, "Sub-Sub" to 1.0f).forEach { (h, w) ->
                                        Text(h, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.Bold), modifier = Modifier.weight(w))
                                    }
                                }
                                HorizontalDivider(color = BrahmBorder)

                                if (cuspsArr.isNullOrEmpty()) {
                                    Text("No cusp data", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                                } else {
                                    cuspsArr.forEachIndexed { idx, cusp ->
                                        if (idx > 0) HorizontalDivider(color = BrahmBorder.copy(alpha = 0.4f))
                                        val houseNum = cusp["house"]?.jsonPrimitive?.intOrNull ?: (idx + 1)
                                        Row(
                                            Modifier.fillMaxWidth()
                                                .background(if (idx % 2 == 0) BrahmMutedForeground.copy(alpha = 0.03f) else Color.Transparent)
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("H$houseNum", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = BrahmGold), modifier = Modifier.weight(0.7f))
                                            Row(Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                val sym = RASHI_SYMBOLS[cusp.str("rashi")]
                                                if (sym != null) Text(sym, fontSize = 13.sp, color = BrahmGold, fontWeight = FontWeight.Light)
                                                Text(cusp.str("rashi"), style = MaterialTheme.typography.bodySmall)
                                            }
                                            Text(
                                                "${"%.1f".format(cusp["degree"]?.jsonPrimitive?.doubleOrNull ?: 0.0)}°",
                                                style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                                                modifier = Modifier.weight(0.9f),
                                            )
                                            Box(Modifier.weight(1.1f)) { SubLordBadge(cusp.str("star_lord")) }
                                            Box(Modifier.weight(1.1f)) { SubLordBadge(cusp.str("sub_lord")) }
                                            Box(Modifier.weight(1.0f)) { SubLordBadge(cusp.str("sub_sub_lord"), small = true) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Significators tab ──
                2 -> {
                    if (sigsObj == null) {
                        item {
                            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                                Text("No significator data", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                            }
                        }
                    } else {
                        // Group by planet: show which houses each planet signifies
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = BrahmCard),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(4.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .background(BrahmGold.copy(alpha = 0.08f))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                    ) {
                                        Text("Planet", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.3f))
                                        Text("Signified Houses", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.Bold), modifier = Modifier.weight(2.7f))
                                    }
                                    HorizontalDivider(color = BrahmBorder)

                                    GRAHA_ORDER.forEachIndexed { idx, gname ->
                                        val houseNums = try {
                                            sigsObj[gname]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull }
                                        } catch (_: Exception) { null }
                                        if (idx > 0) HorizontalDivider(color = BrahmBorder.copy(alpha = 0.4f))
                                        val color = GRAHA_COLOR[gname] ?: BrahmForeground
                                        Row(
                                            Modifier.fillMaxWidth()
                                                .background(if (idx % 2 == 0) BrahmMutedForeground.copy(alpha = 0.03f) else Color.Transparent)
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(Modifier.weight(1.3f)) {
                                                Text(gname, style = MaterialTheme.typography.labelMedium.copy(color = color, fontWeight = FontWeight.SemiBold))
                                                Text(GRAHA_EN[gname] ?: "", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp))
                                            }
                                            Row(Modifier.weight(2.7f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                if (houseNums.isNullOrEmpty()) {
                                                    Text("—", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                                                } else {
                                                    houseNums.forEach { h ->
                                                        Box(
                                                            Modifier.clip(RoundedCornerShape(4.dp))
                                                                .background(color.copy(alpha = 0.1f))
                                                                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 5.dp, vertical = 2.dp),
                                                        ) {
                                                            Text("H$h", style = MaterialTheme.typography.labelSmall.copy(color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium))
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
                }
            }

            // ── Interpretation guide (always shown below) ──
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrahmMutedForeground.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BrahmBorder.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("How to read KP chart", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        listOf(
                            "Star Lord" to "The nakshatra lord — represents the broad theme/area of the planet's energy.",
                            "Sub Lord" to "The most important KP factor — determines whether the planet gives results for a house/event.",
                            "Significators" to "Houses the planet signifies by occupation and ownership — used for event prediction.",
                        ).forEach { (term, desc) ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(term, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.width(70.dp))
                                Text("— $desc", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                            }
                        }
                    }
                }
            }
        }
        } // Box
    }
}

// ── Input Form ────────────────────────────────────────────────────────────────

@Composable
fun KPInputForm(
    name: String, dob: String, tob: String, pob: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onDobChange: (String) -> Unit,
    onTobChange: (String) -> Unit,
    onPobChange: (String) -> Unit,
    onCitySelected: (City) -> Unit,
    onCalculate: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(horizontal = 8.dp, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Info card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrahmGold.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth().border(1.dp, BrahmGold.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
            ) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ℹ", fontSize = 14.sp, color = BrahmGold)
                    Text(
                        "KP System uses the Krishnamurti ayanamsha with Placidus house cusps. Sub Lords are the primary predictive tool — the Sub Lord of a cusp determines whether that house's significations will materialise.",
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                    )
                }
            }
        }

        // Birth details form
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Birth Details", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    BirthInputFields(
                        name = name, onNameChange = onNameChange,
                        dob = dob, onDobChange = onDobChange,
                        tob = tob, onTobChange = onTobChange,
                        pob = pob, onPobChange = onPobChange,
                        onCitySelected = onCitySelected,
                    )
                    if (error != null) Text(error, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)))
                    BrahmButton(text = "Generate KP Chart", onClick = onCalculate)
                }
            }
        }
    }
}
