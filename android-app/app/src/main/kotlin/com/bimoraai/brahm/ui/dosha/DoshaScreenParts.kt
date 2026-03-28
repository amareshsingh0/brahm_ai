package com.bimoraai.brahm.ui.dosha

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimoraai.brahm.core.components.BirthInputFields
import com.bimoraai.brahm.core.components.BrahmButton
import com.bimoraai.brahm.core.components.ScrollToTopFab
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ── Constants ─────────────────────────────────────────────────────────────────

private val RASHI_IDX = mapOf(
    "Mesha" to 0, "Vrishabha" to 1, "Mithuna" to 2, "Karka" to 3,
    "Simha" to 4, "Kanya" to 5, "Tula" to 6, "Vrischika" to 7,
    "Dhanu" to 8, "Makara" to 9, "Kumbha" to 10, "Meena" to 11,
)

private val RASHI_LIST = listOf(
    "Mesha", "Vrishabha", "Mithuna", "Karka", "Simha", "Kanya",
    "Tula", "Vrischika", "Dhanu", "Makara", "Kumbha", "Meena",
)

private val RASHI_SYMBOLS = mapOf(
    "Mesha" to "♈", "Vrishabha" to "♉", "Mithuna" to "♊", "Karka" to "♋",
    "Simha" to "♌", "Kanya" to "♍", "Tula" to "♎", "Vrischika" to "♏",
    "Dhanu" to "♐", "Makara" to "♑", "Kumbha" to "♒", "Meena" to "♓",
)

private val RASHI_LORDS = mapOf(
    "Mesha" to "Mangal", "Vrishabha" to "Shukra", "Mithuna" to "Budh", "Karka" to "Chandra",
    "Simha" to "Surya", "Kanya" to "Budh", "Tula" to "Shukra", "Vrischika" to "Mangal",
    "Dhanu" to "Guru", "Makara" to "Shani", "Kumbha" to "Shani", "Meena" to "Guru",
)

private val DEBILITATION = mapOf(
    "Surya" to "Tula", "Chandra" to "Vrischika", "Mangal" to "Karka", "Budh" to "Meena",
    "Guru" to "Makara", "Shukra" to "Kanya", "Shani" to "Mesha",
)

private val KAAL_SARP_TYPES = mapOf(
    0  to Pair("Ananta",      "Challenges in self-expression, obstacles from influential people."),
    1  to Pair("Kulika",      "Financial hardships, health concerns, ancestral debts."),
    2  to Pair("Vasuki",      "Problems with siblings, short journeys, communication troubles."),
    3  to Pair("Shankhapal",  "Domestic discord, difficulties with mother, emotional turbulence."),
    4  to Pair("Padma",       "Losses in speculation, problems with children, obstacles in creative work."),
    5  to Pair("Mahapadma",   "Health issues, conflicts with enemies, service-related difficulties."),
    6  to Pair("Taksha",      "Partnership troubles, legal disputes, obstacles in marriage."),
    7  to Pair("Karkotak",    "Hidden enemies, sudden reversals, occult troubles."),
    8  to Pair("Shankhachur", "Troubles with higher education, father, religious life."),
    9  to Pair("Ghatak",      "Career obstacles, public reputation issues, challenges with authority."),
    10 to Pair("Vishdhar",    "Gains from unorthodox means, social isolation, unpredictable gains/losses."),
    11 to Pair("Sheshnaag",   "Foreign connections create challenges, expenditure, spiritual restlessness."),
)

private val REMEDIES_MANGAL = listOf(
    "Chant Hanuman Chalisa every Tuesday",
    "Recite Mangal mantra: ॐ क्रां क्रीं क्रौं सः भौमाय नमः (108 times on Tuesdays)",
    "Donate red lentils (masoor dal) on Tuesdays",
    "Wear Red Coral (Moonga) only if Mars is a benefic lord for your lagna — consult a Jyotishi",
    "Avoid arguments and major decisions on Tuesdays",
    "Fast on Tuesdays and offer sindoor to Lord Hanuman",
)

private val REMEDIES_KAAL_SARP = listOf(
    "Perform Nag Panchami puja with milk offering to Shiva Lingam",
    "Rahu-Ketu Shanti puja — best on Saturday (Rahu) or Tuesday (Ketu)",
    "Visit Trimbakeshwar (Nashik) or Ujjain Mahakal for Kaal Sarp Dosha nivaran puja",
    "Donate black sesame seeds and black blanket on Saturdays",
    "Recite Maha Mrityunjaya mantra 108 times daily",
    "Keep a silver snake idol at your place of worship",
)

private val REMEDIES_PITRA = listOf(
    "Perform Pitru Tarpan — offer water mixed with sesame seeds to ancestors every morning",
    "Conduct Shraddha ceremony annually during Pitru Paksha (16-day lunar period)",
    "Donate food and clothes to Brahmins on Amavasya (new moon day)",
    "Offer water to a Peepal tree every Saturday",
    "Perform Pitra Puja at Gaya, Varanasi, or at a sacred river",
    "Recite Pitru Stotra or Gayatri Mantra 108 times daily",
)

private val REMEDIES_GRAHAN = listOf(
    "Recite Mahamrityunjaya mantra daily: ॐ त्र्यम्बकं यजामहे... (108 times)",
    "Perform Surya/Chandra Grahan remedies on actual eclipse days",
    "Avoid starting new ventures during eclipse periods",
    "Donate black sesame (for Rahu) or multicoloured cloth (for Ketu) on Saturdays/Tuesdays",
    "Recite Aditya Hridayam for Sun–Rahu/Ketu conjunction",
)

// ── Data models ────────────────────────────────────────────────────────────────

private data class PlanetData(val house: Int, val rashi: String)

private data class MangalResult(
    val present: Boolean,
    val house: Int,
    val severity: String, // "Strong" | "Moderate" | "Absent"
    val cancelled: Boolean,
    val cancellationReasons: List<String>,
    val mangalRashi: String,
)

private data class KaalSarpResult(
    val present: Boolean,
    val typeName: String,
    val typeEffect: String,
    val direction: String,
    val rahuRashi: String,
    val ketuRashi: String,
)

private data class PitraResult(
    val present: Boolean,
    val causes: List<String>,
)

private data class GrahanResult(
    val present: Boolean,
    val planets: List<String>,
    val description: String,
)

// ── Computation helpers ────────────────────────────────────────────────────────

private fun parsePlanet(grahas: JsonObject?, name: String): PlanetData? {
    val obj = try { grahas?.get(name)?.jsonObject } catch (_: Exception) { null } ?: return null
    val house = obj["house"]?.jsonPrimitive?.intOrNull ?: return null
    val rashi = obj["rashi"]?.jsonPrimitive?.contentOrNull ?: return null
    return PlanetData(house, rashi)
}

private fun computeMangalDosha(grahas: JsonObject?, lagnaRashi: String): MangalResult {
    val mangal = parsePlanet(grahas, "Mangal")
        ?: return MangalResult(false, 0, "Absent", false, emptyList(), "")
    val house = mangal.house
    val doshaHouses = setOf(1, 2, 4, 7, 8, 12)
    if (house !in doshaHouses) return MangalResult(false, house, "Absent", false, emptyList(), mangal.rashi)
    val severity = if (house == 7 || house == 8) "Strong" else "Moderate"
    val reasons = mutableListOf<String>()
    if (mangal.rashi == "Mesha" || mangal.rashi == "Vrischika")
        reasons += "Mars is in its own sign — inherent strength reduces malefic effect"
    if (mangal.rashi == "Makara")
        reasons += "Mars is exalted in Makara — exaltation cancels Dosha"
    if (lagnaRashi in listOf("Mesha", "Vrischika", "Karka", "Kumbha"))
        reasons += "$lagnaRashi lagna naturally neutralises Mangal Dosha"
    val shukra = parsePlanet(grahas, "Shukra")
    if (shukra != null && (shukra.house == 1 || shukra.house == 2))
        reasons += "Venus in H${shukra.house} counters Mars energy"
    val shani = parsePlanet(grahas, "Shani")
    if (shani != null && shani.house == 1)
        reasons += "Saturn in H1 provides discipline that offsets Mangal Dosha"
    return MangalResult(true, house, severity, reasons.isNotEmpty(), reasons, mangal.rashi)
}

private fun computeKaalSarp(grahas: JsonObject?): KaalSarpResult {
    val planets7 = listOf("Surya", "Chandra", "Mangal", "Budh", "Guru", "Shukra", "Shani")
    val rahuPl   = parsePlanet(grahas, "Rahu")
    val ketuPl   = parsePlanet(grahas, "Ketu")
    val rahuIdx  = RASHI_IDX[rahuPl?.rashi] ?: 0
    val ketuIdx  = (rahuIdx + 6) % 12
    val pData    = planets7.mapNotNull { parsePlanet(grahas, it) }
    val forwardArc = pData.all { (RASHI_IDX[it.rashi] ?: 0 - rahuIdx + 12) % 12 < 6 }
    val reverseArc = pData.all { (RASHI_IDX[it.rashi] ?: 0 - ketuIdx + 12) % 12 < 6 }
    val present = forwardArc || reverseArc
    if (!present) return KaalSarpResult(false, "", "", "", rahuPl?.rashi ?: "", ketuPl?.rashi ?: "")
    val direction = if (forwardArc) "Forward" else "Reverse"
    val ksEntry  = KAAL_SARP_TYPES[rahuIdx] ?: Pair("Unknown", "")
    return KaalSarpResult(true, ksEntry.first, ksEntry.second, direction, rahuPl?.rashi ?: "", ketuPl?.rashi ?: "")
}

private fun computePitraDosha(grahas: JsonObject?, lagnaRashi: String): PitraResult {
    val causes = mutableListOf<String>()
    val surya  = parsePlanet(grahas, "Surya")
    val rahu   = parsePlanet(grahas, "Rahu")
    val ketu   = parsePlanet(grahas, "Ketu")
    val shani  = parsePlanet(grahas, "Shani")
    if (surya != null) {
        val malefics = listOfNotNull(
            rahu?.let { it to "Rahu" },
            ketu?.let { it to "Ketu" },
            shani?.let { it to "Saturn" },
        )
        for ((m, mName) in malefics) {
            if (m.house == surya.house)
                causes += "Sun conjunct $mName in H${surya.house} — affliction of Pitru karaka"
        }
        if (surya.house == 9) {
            val maleficIn9 = malefics.filter { (m, _) -> m.house == 9 }
            if (maleficIn9.isNotEmpty())
                causes += "Sun in H9 (Pitru sthana) afflicted by malefic planet"
        }
    }
    val lagnaIdx = RASHI_IDX[lagnaRashi] ?: 0
    val ninthRashi = RASHI_LIST[(lagnaIdx + 8) % 12]
    val ninthLordName = RASHI_LORDS[ninthRashi]
    if (ninthLordName != null) {
        val ninthLord = parsePlanet(grahas, ninthLordName)
        if (ninthLord != null) {
            if (ninthLord.house in listOf(6, 8, 12))
                causes += "9th lord ($ninthLordName) in H${ninthLord.house} (dusthana) — weakens ancestral blessings"
            val debi = DEBILITATION[ninthLordName]
            if (debi != null && ninthLord.rashi == debi)
                causes += "9th lord ($ninthLordName) is debilitated in ${ninthLord.rashi}"
        }
    }
    return PitraResult(causes.isNotEmpty(), causes)
}

private fun computeGrahanYoga(grahas: JsonObject?): GrahanResult {
    val surya   = parsePlanet(grahas, "Surya")
    val chandra = parsePlanet(grahas, "Chandra")
    val rahu    = parsePlanet(grahas, "Rahu")
    val ketu    = parsePlanet(grahas, "Ketu")
    val affected = mutableListOf<String>()
    if (surya != null && rahu != null && surya.house == rahu.house) affected += "Sun + Rahu (Solar Eclipse Yoga)"
    if (surya != null && ketu != null && surya.house == ketu.house) affected += "Sun + Ketu (Solar Eclipse Yoga)"
    if (chandra != null && rahu != null && chandra.house == rahu.house) affected += "Moon + Rahu (Lunar Eclipse Yoga)"
    if (chandra != null && ketu != null && chandra.house == ketu.house) affected += "Moon + Ketu (Lunar Eclipse Yoga)"
    val desc = if (affected.isNotEmpty())
        "A luminary (Sun or Moon) is conjunct a lunar node (Rahu/Ketu), creating an eclipse-like shadow that can obscure the significations of the affected luminary."
    else ""
    return GrahanResult(affected.isNotEmpty(), affected, desc)
}

// ── UI Composables ─────────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(present: Boolean, cancelled: Boolean = false) {
    val (label, bg, fg) = when {
        !present   -> Triple("Absent",            Color(0xFFE8F5E9), Color(0xFF2E7D32))
        cancelled  -> Triple("Present · Cancelled", Color(0xFFFFF8E1), Color(0xFFE65100))
        else       -> Triple("Present",            Color(0xFFFFEBEE), Color(0xFFC62828))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun SeverityBadge(severity: String) {
    if (severity == "Absent") return
    val (bg, fg) = if (severity == "Strong")
        Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
    else
        Pair(Color(0xFFFFF8E1), Color(0xFFE65100))
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(severity, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun RemediesPanel(remedies: List<String>, accentColor: Color) {
    var open by remember { mutableStateOf(false) }
    Column {
        TextButton(
            onClick = { open = !open },
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(
                if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = BrahmGold,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (open) "Hide Remedies" else "View Remedies",
                fontSize = 12.sp,
                color = BrahmGold,
                fontWeight = FontWeight.Medium,
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                remedies.forEach { remedy ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text("★", fontSize = 11.sp, color = BrahmGold, modifier = Modifier.padding(top = 1.dp))
                        Text(
                            remedy,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BrahmForeground.copy(alpha = 0.8f),
                            ),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

// ── Shared gradient card header ───────────────────────────────────────────────

@Composable
private fun DoshaCardHeader(
    symbol: String,
    symbolColor: Color,
    name: String,
    subtitle: String,
    present: Boolean,
    cancelled: Boolean = false,
    severity: String = "",
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(symbolColor.copy(alpha = 0.13f), symbolColor.copy(alpha = 0.03f)),
                ),
            )
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(symbolColor.copy(alpha = 0.25f), symbolColor.copy(alpha = 0.08f)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(symbol, fontSize = 26.sp, color = symbolColor)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                StatusBadge(present, cancelled)
                if (severity.isNotBlank() && severity != "Absent") SeverityBadge(severity)
            }
        }
    }
}

// ── Mangal Dosha Card ──────────────────────────────────────────────────────────

@Composable
private fun MangalDoshaCard(result: MangalResult) {
    val borderColor = when {
        result.present && !result.cancelled -> Color(0xFFEF9A9A)
        result.cancelled -> Color(0xFFFFCC80)
        else -> BrahmBorder
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Column {
            DoshaCardHeader(
                symbol = "♂",
                symbolColor = Color(0xFFE53935),
                name = "Mangal Dosha",
                subtitle = "Mars in 1/2/4/7/8/12 houses",
                present = result.present,
                cancelled = result.cancelled,
                severity = result.severity,
            )
            HorizontalDivider(color = BrahmBorder)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Body
            if (result.present) {
                val rashiSymbol = RASHI_SYMBOLS[result.mangalRashi] ?: ""
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Mars in H${result.house}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    if (result.mangalRashi.isNotBlank()) {
                        Text(
                            "$rashiSymbol ${result.mangalRashi}",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmGold),
                        )
                    }
                }
                Text(
                    if (result.severity == "Strong")
                        "Mars in H${result.house} creates strong Mangal Dosha — significant influence on marriage and partnerships."
                    else
                        "Mars in H${result.house} creates moderate Mangal Dosha — some impact on domestic and partnership matters.",
                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.8f)),
                )
                if (result.cancelled && result.cancellationReasons.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFF8E1))
                            .border(1.dp, Color(0xFFFFCC80), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Cancellation Factors",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE65100),
                            )
                            result.cancellationReasons.forEach { reason ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(start = 4.dp),
                                ) {
                                    Box(
                                        Modifier
                                            .width(2.dp)
                                            .fillMaxHeight()
                                            .background(Color(0xFFFFCC80), RoundedCornerShape(1.dp))
                                    )
                                    Text(
                                        reason,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE65100).copy(alpha = 0.85f)),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    "Mars is not in any Dosha house (H1/2/4/7/8/12). Mangal Dosha is absent in this chart.",
                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                )
            }

            HorizontalDivider(color = BrahmBorder)
            RemediesPanel(REMEDIES_MANGAL, Color(0xFFE53935))
            } // body Column
        }
    }
}

// ── Kaal Sarp Dosha Card ───────────────────────────────────────────────────────

@Composable
private fun KaalSarpCard(result: KaalSarpResult) {
    val borderColor = if (result.present) Color(0xFFB39DDB) else BrahmBorder
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Column {
            DoshaCardHeader(
                symbol = "☊",
                symbolColor = Color(0xFF7C4DFF),
                name = "Kaal Sarp Dosha",
                subtitle = "All planets between Rahu–Ketu axis",
                present = result.present,
            )
            HorizontalDivider(color = BrahmBorder)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (result.present) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        result.typeName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Text("Kaal Sarp", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(BrahmMuted)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text("${result.direction} arc", fontSize = 10.sp, color = BrahmMutedForeground)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val rahuSym = RASHI_SYMBOLS[result.rahuRashi] ?: ""
                    val ketuSym = RASHI_SYMBOLS[result.ketuRashi] ?: ""
                    Text("Rahu: ", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    Text("$rahuSym ${result.rahuRashi}", style = MaterialTheme.typography.bodySmall.copy(color = BrahmGold))
                    Text("Ketu: ", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    Text("$ketuSym ${result.ketuRashi}", style = MaterialTheme.typography.bodySmall.copy(color = BrahmGold))
                }
                if (result.typeEffect.isNotBlank()) {
                    Text(
                        result.typeEffect,
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.8f)),
                    )
                }
            } else {
                Text(
                    "All 7 planets are not confined between the Rahu–Ketu axis. Kaal Sarp Dosha is absent.",
                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                )
            }

            HorizontalDivider(color = BrahmBorder)
            RemediesPanel(REMEDIES_KAAL_SARP, Color(0xFF7C4DFF))
            } // body Column
        }
    }
}

// ── Pitra Dosha Card ───────────────────────────────────────────────────────────

@Composable
private fun PitraDoshaCard(result: PitraResult) {
    val borderColor = if (result.present) Color(0xFFFFCC80) else BrahmBorder
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Column {
            DoshaCardHeader(
                symbol = "☉",
                symbolColor = Color(0xFFD97706),
                name = "Pitra Dosha",
                subtitle = "Sun affliction, 9th house lord analysis",
                present = result.present,
            )
            HorizontalDivider(color = BrahmBorder)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (result.present) {
                Text(
                    "Planetary combinations indicate ancestral karmic debt. These patterns suggest need for Pitru Tarpan and ancestral remedies.",
                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.8f)),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    result.causes.forEach { cause ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFF8E1))
                                .border(
                                    width = 2.dp,
                                    color = Color(0xFFFFCC80),
                                    shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp),
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text("⚠", fontSize = 12.sp, color = Color(0xFFE65100), modifier = Modifier.padding(top = 1.dp))
                            Text(
                                cause,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE65100).copy(alpha = 0.9f)),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            } else {
                Text(
                    "No affliction to the Sun or 9th house lord found. Pitra Dosha is absent in this chart.",
                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                )
            }

            HorizontalDivider(color = BrahmBorder)
            RemediesPanel(REMEDIES_PITRA, Color(0xFFD97706))
            } // body Column
        }
    }
}

// ── Grahan Yoga Card ───────────────────────────────────────────────────────────

@Composable
private fun GrahanYogaCard(result: GrahanResult) {
    val borderColor = if (result.present) Color(0xFFCE93D8) else BrahmBorder
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Column {
            DoshaCardHeader(
                symbol = "☽●",
                symbolColor = Color(0xFF9C27B0),
                name = "Grahan Yoga",
                subtitle = "Sun/Moon conjunct Rahu or Ketu",
                present = result.present,
            )
            HorizontalDivider(color = BrahmBorder)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (result.present) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    result.planets.forEach { planet ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("⚡", fontSize = 12.sp, color = Color(0xFF9C27B0))
                            Text(
                                planet,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF9C27B0),
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                        }
                    }
                }
                if (result.description.isNotBlank()) {
                    Text(
                        result.description,
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.8f)),
                    )
                }
            } else {
                Text(
                    "No Sun–Moon conjunction with Rahu or Ketu found. Grahan Yoga is absent.",
                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                )
            }

            HorizontalDivider(color = BrahmBorder)
            RemediesPanel(REMEDIES_GRAHAN, Color(0xFF9C27B0))
            } // body Column
        }
    }
}

// ── DoshaContent ──────────────────────────────────────────────────────────────

@Composable
fun DoshaContent(data: JsonObject, onReset: () -> Unit = {}) {
    val grahasObj   = try { data["grahas"]?.jsonObject } catch (_: Exception) { null }
    val lagnaObj    = try { data["lagna"]?.jsonObject } catch (_: Exception) { null }
    val lagnaRashi  = lagnaObj?.get("rashi")?.jsonPrimitive?.contentOrNull ?: ""

    val mangalResult  = computeMangalDosha(grahasObj, lagnaRashi)
    val kaalSarpResult = computeKaalSarp(grahasObj)
    val pitraResult   = computePitraDosha(grahasObj, lagnaRashi)
    val grahanResult  = computeGrahanYoga(grahasObj)

    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().background(BrahmBackground),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Summary row ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrahmCard),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Dosha Summary",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            data class DoshaSummaryItem(val sym: String, val name: String, val color: Color, val present: Boolean, val cancelled: Boolean)
                            listOf(
                                DoshaSummaryItem("♂",   "Mangal",   Color(0xFFE53935), mangalResult.present && !mangalResult.cancelled, mangalResult.cancelled),
                                DoshaSummaryItem("☊",   "Kaal Sarp",Color(0xFF7C4DFF), kaalSarpResult.present, false),
                                DoshaSummaryItem("☉",   "Pitra",    Color(0xFFD97706), pitraResult.present, false),
                                DoshaSummaryItem("☽●",  "Grahan",   Color(0xFF9C27B0), grahanResult.present, false),
                            ).forEach { item ->
                                val statusColor = when {
                                    item.present    -> item.color
                                    item.cancelled  -> Color(0xFFE65100)
                                    else            -> Color(0xFF2E7D32)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    statusColor.copy(alpha = 0.12f),
                                                    statusColor.copy(alpha = 0.04f),
                                                ),
                                            ),
                                        )
                                        .border(1.dp, statusColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(item.sym, fontSize = 20.sp, color = statusColor)
                                        Text(item.name, fontSize = 9.sp, color = BrahmMutedForeground, textAlign = TextAlign.Center)
                                        Text(
                                            when {
                                                item.cancelled -> "Cancelled"
                                                item.present   -> "Present"
                                                else           -> "Absent"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 4 Dosha Cards ──
            item { MangalDoshaCard(mangalResult) }
            item { KaalSarpCard(kaalSarpResult) }
            item { PitraDoshaCard(pitraResult) }
            item { GrahanYogaCard(grahanResult) }

            // ── Recalculate ──
            item {
                BrahmButton(text = "Recalculate", onClick = onReset)
            }

            // ── Disclaimer ──
            item {
                Text(
                    "Dosha analysis is indicative. Consult a qualified Jyotishi for personalised guidance.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BrahmMutedForeground.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }
        }
        ScrollToTopFab(listState, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp))
    }
}

// ── DoshaInputForm ─────────────────────────────────────────────────────────────

@Composable
fun DoshaInputForm(
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter Birth Details", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    BirthInputFields(
                        name = name, onNameChange = onNameChange,
                        dob = dob, onDobChange = onDobChange,
                        tob = tob, onTobChange = onTobChange,
                        pob = pob, onPobChange = onPobChange,
                        onCitySelected = onCitySelected,
                    )
                    if (error != null) {
                        Text(error, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)))
                    }
                    BrahmButton(text = "Analyse Doshas", onClick = onCalculate)
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7)),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Doshas Checked",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrahmGold),
                    )
                    listOf(
                        "♂  Mangal Dosha — Mars in 1/2/4/7/8/12 houses",
                        "☊  Kaal Sarp Dosha — All planets between Rahu–Ketu axis",
                        "☉  Pitra Dosha — Sun affliction & 9th lord analysis",
                        "●  Grahan Yoga — Luminary conjunct lunar node",
                    ).forEach { text ->
                        Text(text, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.8f)))
                    }
                }
            }
        }
    }
}
