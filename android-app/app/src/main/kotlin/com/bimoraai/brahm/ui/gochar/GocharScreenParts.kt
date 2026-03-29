package com.bimoraai.brahm.ui.gochar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimoraai.brahm.core.components.BirthInputFields
import com.bimoraai.brahm.core.components.BrahmButton
import com.bimoraai.brahm.core.components.ScrollToTopFab
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ── Rashi symbols (text presentation, not emoji) ─────────────────────────────
private val RASHI_SYMBOLS = mapOf(
    "Mesha" to "♈\uFE0E", "Vrishabha" to "♉\uFE0E", "Mithuna" to "♊\uFE0E", "Karka" to "♋\uFE0E",
    "Simha" to "♌\uFE0E", "Kanya" to "♍\uFE0E", "Tula" to "♎\uFE0E", "Vrischika" to "♏\uFE0E",
    "Dhanu" to "♐\uFE0E", "Makara" to "♑\uFE0E", "Kumbha" to "♒\uFE0E", "Meena" to "♓\uFE0E",
)

// ── Planet metadata (matches website GRAHA_* maps) ───────────────────────────
private data class GrahaMeta(val symbol: String, val en: String, val color: Color, val desc: String)
private val ORDER = listOf("Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani","Rahu","Ketu")
private val GRAHA_META = mapOf(
    "Surya"  to GrahaMeta("☉", "Sun",     Color(0xFFD97706), "Vitality & Soul"),
    "Chandra" to GrahaMeta("☽", "Moon",    Color(0xFF4F46E5), "Mind & Emotions"),
    "Mangal" to GrahaMeta("♂", "Mars",    Color(0xFFDC2626), "Energy & Action"),
    "Budh"   to GrahaMeta("☿", "Mercury", Color(0xFF16A34A), "Intellect & Speech"),
    "Guru"   to GrahaMeta("♃", "Jupiter", Color(0xFFB45309), "Wisdom & Fortune"),
    "Shukra" to GrahaMeta("♀", "Venus",   Color(0xFF9333EA), "Love & Beauty"),
    "Shani"  to GrahaMeta("♄", "Saturn",  Color(0xFF334155), "Karma & Discipline"),
    "Rahu"   to GrahaMeta("☊", "Rahu",    Color(0xFF0369A1), "Ambition & Desire"),
    "Ketu"   to GrahaMeta("☋", "Ketu",    Color(0xFFC2410C), "Spirituality"),
)

@Composable
fun GocharContent(gocharData: JsonObject?, analyzeData: JsonObject?, isLoading: Boolean = false) {
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state           = listState,
            modifier        = Modifier.fillMaxSize().background(BrahmBackground),
            contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Section 1 header ─────────────────────────────────────
            item {
                SectionLabel("✦  Current Planetary Positions")
            }

            // ── Planet grid — 3 per row like website ─────────────────
            val positionsObj = gocharData?.get("positions")?.let {
                try { it.jsonObject } catch (_: Exception) { null }
            }

            item {
                if (positionsObj == null) {
                    InfoCard("Planet data not available.")
                } else {
                    // Split into rows of 3
                    val chunks = ORDER.chunked(3)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        chunks.forEach { row ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEach { planet ->
                                    val pos  = positionsObj[planet]?.let { try { it.jsonObject } catch (_: Exception) { null } }
                                    val meta = GRAHA_META[planet]
                                    Box(Modifier.weight(1f)) {
                                        PlanetCell(
                                            meta   = meta ?: GrahaMeta("★", planet, BrahmGold, ""),
                                            rashi  = pos?.get("rashi")?.jsonPrimitive?.contentOrNull ?: "—",
                                            degree = pos?.get("degree")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull(),
                                        )
                                    }
                                }
                                // Fill empty slots in last row
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }

            // ── Section 2: Personal Transit Analysis ─────────────────
            if (analyzeData == null && isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = CardDefaults.cardColors(containerColor = BrahmCard),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Row(
                            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment   = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color       = BrahmGold,
                            )
                            Text(
                                "Loading personal transit analysis…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrahmMutedForeground,
                            )
                        }
                    }
                }
            }

            if (analyzeData != null) {
                item { SectionLabel("📊  Personal Transit Analysis") }

                // Saturn event badges + banner
                val sadeSati      = analyzeData["sade_sati"]?.jsonPrimitive?.booleanOrNull ?: false
                val sadeSatiPhase = analyzeData["sade_sati_phase"]?.jsonPrimitive?.contentOrNull ?: ""
                val ashtamaShani  = analyzeData["ashtama_shani"]?.jsonPrimitive?.booleanOrNull ?: false
                val kantakaShani  = analyzeData["kantaka_shani"]?.jsonPrimitive?.booleanOrNull ?: false
                val shaniLagna    = analyzeData["shani_house_lagna"]?.jsonPrimitive?.intOrNull
                val shaniMoon     = analyzeData["shani_house_moon"]?.jsonPrimitive?.intOrNull
                val summary       = analyzeData["summary"]?.jsonPrimitive?.contentOrNull ?: ""

                // Saturn special banner
                if (sadeSati || ashtamaShani || kantakaShani) {
                    item {
                        SaturnBanner(
                            sadeSati      = sadeSati,
                            sadeSatiPhase = sadeSatiPhase,
                            ashtamaShani  = ashtamaShani,
                            kantakaShani  = kantakaShani,
                            shaniLagna    = shaniLagna,
                            shaniMoon     = shaniMoon,
                        )
                    }
                }

                // Current positions table
                val currentPositions = analyzeData["current_positions"]?.let {
                    try { it.jsonObject } catch (_: Exception) { null }
                }

                if (currentPositions != null && currentPositions.isNotEmpty()) {
                    item {
                        TransitPositionsTable(
                            currentPositions = currentPositions,
                            avScores = analyzeData["av_scores"]?.let { try { it.jsonObject } catch (_: Exception) { null } },
                        )
                    }
                }

                // Summary
                if (summary.isNotBlank()) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrahmCard)
                                .padding(14.dp),
                        ) {
                            Text(
                                "Transit Summary",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color      = BrahmMutedForeground,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 10.sp,
                                ),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                summary,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color      = BrahmForeground,
                                    lineHeight = 18.sp,
                                ),
                            )
                        }
                    }
                }

                // Opportunities
                val opportunities = analyzeData["opportunities"]?.let { el ->
                    try { el.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }.filter { it.isNotBlank() } }
                    catch (_: Exception) { null }
                }
                if (!opportunities.isNullOrEmpty()) {
                    item { OpportunitiesCard(opportunities) }
                }

                // Cautions
                val cautions = analyzeData["cautions"]?.let { el ->
                    try { el.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }.filter { it.isNotBlank() } }
                    catch (_: Exception) { null }
                }
                if (!cautions.isNullOrEmpty()) {
                    item { CautionsCard(cautions) }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        ScrollToTopFab(listState, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 88.dp))
    }
}

// ── Planet Cell (website: flex flex-col items-center gap-1) ─────────────────

@Composable
private fun PlanetCell(meta: GrahaMeta, rashi: String, degree: Double?) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BrahmCard)
            .border(1.dp, BrahmBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        // Symbol — large, colored
        Text(
            meta.symbol,
            fontSize   = 22.sp,
            color      = meta.color,
            fontWeight = FontWeight.Bold,
        )
        // English name
        Text(
            meta.en,
            style = MaterialTheme.typography.labelSmall.copy(
                color      = BrahmForeground,
                fontWeight = FontWeight.Medium,
                fontSize   = 10.sp,
            ),
        )
        // Rashi — symbol (large, text rendering) + name below
        Text(
            RASHI_SYMBOLS[rashi] ?: "",
            fontSize   = 18.sp,
            color      = BrahmGold,
            fontWeight = FontWeight.Light,
        )
        Text(
            rashi,
            style = MaterialTheme.typography.labelSmall.copy(
                color      = BrahmGold,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 9.sp,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // Degree
        if (degree != null) {
            // Use BigDecimal to match JS toFixed(1) — avoids +0.1° drift from float representation
            val d1 = java.math.BigDecimal(degree).setScale(1, java.math.RoundingMode.HALF_DOWN).toDouble()
            Text(
                "${"%.1f".format(d1)}°",
                style = MaterialTheme.typography.labelSmall.copy(
                    color    = BrahmMutedForeground,
                    fontSize = 9.sp,
                ),
            )
        }
        // Description (small, muted — hidden on website small screens, show here)
        Text(
            meta.desc,
            style = MaterialTheme.typography.labelSmall.copy(
                color      = BrahmMutedForeground.copy(alpha = 0.7f),
                fontSize   = 8.sp,
                lineHeight = 11.sp,
            ),
            maxLines  = 2,
            overflow  = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

// ── Saturn Banner ────────────────────────────────────────────────────────────

@Composable
private fun SaturnBanner(
    sadeSati: Boolean, sadeSatiPhase: String,
    ashtamaShani: Boolean, kantakaShani: Boolean,
    shaniLagna: Int?, shaniMoon: Int?,
) {
    val (bg, border, textColor) = when {
        sadeSati     -> Triple(Color(0xFFE53935).copy(0.05f), Color(0xFFE53935).copy(0.25f), Color(0xFFE53935))
        ashtamaShani -> Triple(Color(0xFFFF8F00).copy(0.05f), Color(0xFFFF8F00).copy(0.25f), Color(0xFFFF8F00))
        else         -> Triple(Color(0xFFF59E0B).copy(0.05f), Color(0xFFF59E0B).copy(0.25f), Color(0xFFF59E0B))
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val title = when {
            sadeSati     -> "☄ Sade Sati${if (sadeSatiPhase.isNotBlank()) " · $sadeSatiPhase" else ""}"
            ashtamaShani -> "⚠ Ashtama Shani"
            else         -> "⚡ Kantaka Shani${if (shaniLagna != null) " (H$shaniLagna)" else ""}"
        }
        Text(title, style = MaterialTheme.typography.labelMedium.copy(color = textColor, fontWeight = FontWeight.Bold))

        val detail = when {
            sadeSati     -> "Shani H${shaniMoon ?: "?"} from Moon. Duration ~2.5 yrs per phase, total 7.5 yrs. Focus on discipline, avoid shortcuts. Worship Shani Dev on Saturdays."
            ashtamaShani -> "Saturn in 8th from Moon (H${shaniMoon ?: "?"}). Sudden obstacles, health concerns, hidden enemies possible. Hanuman/Shani puja recommended."
            else         -> "Saturn in H${shaniLagna ?: "?"} from Lagna. Career/relationship disruptions possible. Remedy: iron ring, sesame oil donation Saturdays, Shani Chalisa."
        }
        Text(detail, style = MaterialTheme.typography.bodySmall.copy(color = textColor.copy(0.85f), lineHeight = 17.sp, fontSize = 11.sp))
    }
}

// ── Transit Positions Table ───────────────────────────────────────────────────

@Composable
private fun TransitPositionsTable(currentPositions: JsonObject, avScores: JsonObject?) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BrahmCard)
            .border(1.dp, BrahmBorder, RoundedCornerShape(12.dp)),
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .background(BrahmMuted.copy(alpha = 0.3f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                "Transits relative to natal chart",
                style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp),
            )
            if (avScores != null && avScores.isNotEmpty()) {
                Text(
                    "AV score",
                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground.copy(0.6f), fontSize = 9.sp),
                )
            }
        }
        HorizontalDivider(color = BrahmBorder)

        ORDER.forEach { planet ->
            val pos  = currentPositions[planet]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val meta = GRAHA_META[planet] ?: return@forEach
            val av   = avScores?.get(planet)?.let { try { it.jsonObject } catch (_: Exception) { null } }
            val avScore = av?.get("score")?.jsonPrimitive?.intOrNull

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Symbol
                Text(meta.symbol, fontSize = 15.sp, color = meta.color, modifier = Modifier.width(22.dp))
                Spacer(Modifier.width(8.dp))
                // English name
                Text(
                    meta.en,
                    style    = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground),
                    modifier = Modifier.width(60.dp),
                )
                // Position (symbol + rashi)
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val sym = RASHI_SYMBOLS[pos]
                    if (sym != null) {
                        Text(sym, fontSize = 14.sp, color = BrahmGold, fontWeight = FontWeight.Light)
                    }
                    Text(pos, style = MaterialTheme.typography.bodySmall.copy(color = BrahmGold, fontWeight = FontWeight.Medium))
                }
                // AV score badge
                if (avScore != null) {
                    val avColor = when {
                        avScore >= 5 -> Color(0xFF43A047)
                        avScore >= 4 -> Color(0xFFFF8F00)
                        else         -> Color(0xFFE53935)
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BrahmMuted.copy(0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "AV $avScore/8",
                            style = MaterialTheme.typography.labelSmall.copy(color = avColor, fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
            HorizontalDivider(color = BrahmBorder.copy(alpha = 0.4f))
        }
    }
}

// ── Opportunities Card ────────────────────────────────────────────────────────

@Composable
private fun OpportunitiesCard(items: List<String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BrahmCard)
            .border(1.dp, Color(0xFF43A047).copy(0.25f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("↑", fontSize = 16.sp, color = Color(0xFF43A047), fontWeight = FontWeight.Bold)
            Text(
                "Opportunities",
                style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF43A047), fontWeight = FontWeight.SemiBold),
            )
        }
        items.forEach { opp ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(with(androidx.compose.ui.platform.LocalDensity.current) { (12.sp.toPx() * 2.5f).dp })
                        .background(Color(0xFF43A047).copy(0.4f))
                )
                Text(
                    opp,
                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, lineHeight = 17.sp, fontSize = 12.sp),
                )
            }
        }
    }
}

// ── Cautions Card ─────────────────────────────────────────────────────────────

@Composable
private fun CautionsCard(items: List<String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BrahmCard)
            .border(1.dp, Color(0xFFF59E0B).copy(0.25f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("⚠", fontSize = 14.sp, color = Color(0xFFF59E0B))
            Text(
                "Cautions",
                style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold),
            )
        }
        items.forEach { caution ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(with(androidx.compose.ui.platform.LocalDensity.current) { (12.sp.toPx() * 2.5f).dp })
                        .background(Color(0xFFF59E0B).copy(0.4f))
                )
                Text(
                    caution,
                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, lineHeight = 17.sp, fontSize = 12.sp),
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrahmForeground),
    )
}

@Composable
private fun InfoCard(text: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BrahmCard).padding(16.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground))
    }
}

// ── Input Form ────────────────────────────────────────────────────────────────

@Composable
fun GocharInputForm(
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
        modifier        = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding  = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrahmCard),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter Birth Details", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    BirthInputFields(
                        name          = name, onNameChange   = onNameChange,
                        dob           = dob,  onDobChange    = onDobChange,
                        tob           = tob,  onTobChange    = onTobChange,
                        pob           = pob,  onPobChange    = onPobChange,
                        onCitySelected = onCitySelected,
                    )
                    if (error != null) {
                        Text(error, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)))
                    }
                    BrahmButton(text = "Analyze Transits", onClick = onCalculate)
                }
            }
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFF8E7))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("✦  What is Gochar?", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
                Text(
                    "Gochar (planetary transit) shows how current planetary positions affect your natal chart. It reveals timing of events in career, health, relationships, and spiritual growth based on transiting planets relative to your birth chart.",
                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.8f), lineHeight = 18.sp),
                )
            }
        }
    }
}
