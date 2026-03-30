package com.bimoraai.brahm.ui.gemstone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimoraai.brahm.core.components.BirthInputFields
import com.bimoraai.brahm.core.components.BrahmButton
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ── Static data tables (mirrors website LAGNA_STONE + STONE_WEAR) ─────────────

private data class StoneEntry(
    val stone: String, val color: String, val planet: String, val purpose: String,
)

private data class WearEntry(
    val metal: String, val finger: String, val day: String, val mantra: String,
)

private val LAGNA_STONE: Map<String, StoneEntry> = mapOf(
    "Mesha"     to StoneEntry("Red Coral (Moonga)",              "Red",         "Mangal",  "Courage, Energy, Health"),
    "Vrishabha" to StoneEntry("Diamond or White Sapphire",       "White/Clear", "Shukra",  "Love, Wealth, Beauty"),
    "Mithuna"   to StoneEntry("Emerald (Panna)",                 "Green",       "Budh",    "Intellect, Communication, Business"),
    "Karka"     to StoneEntry("Pearl (Moti)",                    "White",       "Chandra", "Emotions, Home, Mother"),
    "Simha"     to StoneEntry("Ruby (Manik)",                    "Red",         "Surya",   "Authority, Career, Health"),
    "Kanya"     to StoneEntry("Emerald (Panna)",                 "Green",       "Budh",    "Analysis, Health, Service"),
    "Tula"      to StoneEntry("Diamond or White Sapphire",       "White/Clear", "Shukra",  "Relationships, Harmony, Wealth"),
    "Vrischika" to StoneEntry("Red Coral (Moonga)",              "Red",         "Mangal",  "Power, Research, Transformation"),
    "Dhanu"     to StoneEntry("Yellow Sapphire (Pukhraj)",       "Yellow",      "Guru",    "Fortune, Wisdom, Expansion"),
    "Makara"    to StoneEntry("Blue Sapphire (Neelam)",          "Blue",        "Shani",   "Discipline, Career, Karma"),
    "Kumbha"    to StoneEntry("Blue Sapphire (Neelam)",          "Blue",        "Shani",   "Humanitarian work, Innovation"),
    "Meena"     to StoneEntry("Yellow Sapphire (Pukhraj)",       "Yellow",      "Guru",    "Spirituality, Fortune, Compassion"),
)

private val STONE_WEAR: Map<String, WearEntry> = mapOf(
    "Mangal"  to WearEntry("Gold or Copper", "Ring finger",   "Tuesday",   "ॐ क्रां क्रीं क्रौं सः भौमाय नमः"),
    "Shukra"  to WearEntry("Gold or Silver", "Middle finger", "Friday",    "ॐ द्रां द्रीं द्रौं सः शुक्राय नमः"),
    "Budh"    to WearEntry("Gold",           "Little finger", "Wednesday", "ॐ ब्रां ब्रीं ब्रौं सः बुधाय नमः"),
    "Chandra" to WearEntry("Silver",         "Little finger", "Monday",    "ॐ श्रां श्रीं श्रौं सः चंद्रमसे नमः"),
    "Surya"   to WearEntry("Gold",           "Ring finger",   "Sunday",    "ॐ ह्रां ह्रीं ह्रौं सः सूर्याय नमः"),
    "Guru"    to WearEntry("Gold",           "Index finger",  "Thursday",  "ॐ ग्रां ग्रीं ग्रौं सः गुरवे नमः"),
    "Shani"   to WearEntry("Iron or Silver", "Middle finger", "Saturday",  "ॐ प्रां प्रीं प्रौं सः शनैश्चराय नमः"),
)

private val RASHI_ORDER = listOf(
    "Mesha","Vrishabha","Mithuna","Karka","Simha","Kanya",
    "Tula","Vrischika","Dhanu","Makara","Kumbha","Meena",
)

private val PLANET_LORD_RASHI: Map<String, String> = mapOf(
    "Surya" to "Simha", "Chandra" to "Karka", "Mangal" to "Mesha", "Budh" to "Mithuna",
    "Guru" to "Dhanu", "Shukra" to "Vrishabha", "Shani" to "Makara",
)

private val CAUTION_RULES = listOf(
    "Always consult a qualified Vedic astrologer before wearing any gemstone — an incompatible stone can activate malefic planets.",
    "Blue Sapphire (Shani) and Hessonite (Rahu) require a trial period of 3 days before permanent wearing.",
    "Minimum weight recommended: Ruby 3 ct · Pearl 5 ct · Emerald 3 ct · Yellow Sapphire 3 ct · Diamond 0.5 ct.",
    "Gemstones must be energised (abhimantrit) on the correct day with the prescribed mantra before first wear.",
    "Avoid wearing stones for Dusthana lords (6th, 8th, 12th house lords) — they tend to strengthen malefic significations.",
    "Natural, unheated gems are strongly preferred over synthetic or treated stones for astrological benefit.",
)

// ── Gem color → Android Color ──────────────────────────────────────────────────

private fun gemColor(color: String): Color = when {
    color.contains("Red",    ignoreCase = true) -> Color(0xFFE53935)
    color.contains("Green",  ignoreCase = true) -> Color(0xFF43A047)
    color.contains("Yellow", ignoreCase = true) -> Color(0xFFF59E0B)
    color.contains("Blue",   ignoreCase = true) -> Color(0xFF1565C0)
    color.contains("White",  ignoreCase = true) ||
    color.contains("Clear",  ignoreCase = true) -> Color(0xFF90A4AE)
    else                                         -> Color(0xFF9333EA)
}

private fun gemEmoji(color: String): String = when {
    color.contains("Red",    ignoreCase = true) -> "💎"
    color.contains("Green",  ignoreCase = true) -> "💚"
    color.contains("Yellow", ignoreCase = true) -> "💛"
    color.contains("Blue",   ignoreCase = true) -> "🔵"
    color.contains("White",  ignoreCase = true) ||
    color.contains("Clear",  ignoreCase = true) -> "⚪"
    else                                         -> "💎"
}

// ── Main content ──────────────────────────────────────────────────────────────

@Composable
fun GemstoneContent(kundaliData: JsonObject) {
    // Parse lagna and moon rashi from kundali JSON
    val lagnaRashi = kundaliData["lagna"]?.let { try { it.jsonObject["rashi"]?.jsonPrimitive?.contentOrNull } catch (_: Exception) { null } } ?: ""
    val moonRashi  = kundaliData["grahas"]?.let { try { it.jsonObject["Chandra"]?.jsonObject?.get("rashi")?.jsonPrimitive?.contentOrNull } catch (_: Exception) { null } } ?: ""
    val grahasObj  = kundaliData["grahas"]?.let { try { it.jsonObject } catch (_: Exception) { null } }

    val primaryEntry   = LAGNA_STONE[lagnaRashi]
    val secondaryEntry = LAGNA_STONE[moonRashi]

    // Benefic house lords: 4th (idx+3), 9th (idx+8), 10th (idx+9) from lagna
    val lagnaIdx = RASHI_ORDER.indexOf(lagnaRashi)
    val beneficSections = listOf(
        "4th House Lord" to 3,
        "9th House Lord" to 8,
        "10th House Lord" to 9,
    ).mapNotNull { (label, offset) ->
        if (lagnaIdx < 0) return@mapNotNull null
        val rashi = RASHI_ORDER[(lagnaIdx + offset) % 12]
        val entry = LAGNA_STONE[rashi] ?: return@mapNotNull null
        if (entry.planet == primaryEntry?.planet) return@mapNotNull null
        Triple(label, rashi, entry)
    }
    // Deduplicate by planet
    val seenPlanets = mutableSetOf<String>().apply {
        primaryEntry?.let { add(it.planet) }
        secondaryEntry?.let { add(it.planet) }
    }
    val uniqueBenefic = beneficSections.filter { (_, _, entry) ->
        if (seenPlanets.contains(entry.planet)) false else { seenPlanets.add(entry.planet); true }
    }

    // Debilitated + Exalted planets
    val debilitatedPlanets = grahasObj?.entries?.mapNotNull { (planet, v) ->
        val status = try { v.jsonObject["status"]?.jsonPrimitive?.contentOrNull } catch (_: Exception) { null }
        if (status?.contains("Neecha", ignoreCase = true) == true) {
            val lordRashi = PLANET_LORD_RASHI[planet] ?: return@mapNotNull null
            val entry = LAGNA_STONE[lordRashi] ?: return@mapNotNull null
            Pair(planet, entry)
        } else null
    } ?: emptyList()

    val exaltedPlanets = grahasObj?.entries?.mapNotNull { (planet, v) ->
        val status = try { v.jsonObject["status"]?.jsonPrimitive?.contentOrNull } catch (_: Exception) { null }
        if (status?.contains("Uchcha", ignoreCase = true) == true) {
            val lordRashi = PLANET_LORD_RASHI[planet] ?: return@mapNotNull null
            val entry = LAGNA_STONE[lordRashi] ?: return@mapNotNull null
            Pair(planet, entry)
        } else null
    } ?: emptyList()

    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state           = listState,
            modifier        = Modifier.fillMaxSize().background(BrahmBackground),
            contentPadding  = PaddingValues(horizontal = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Page header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "💎  Gemstone Recommendations",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = BrahmForeground),
                    )
                    if (lagnaRashi.isNotBlank() || moonRashi.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (lagnaRashi.isNotBlank()) Pill("$lagnaRashi Lagna", BrahmGold.copy(0.12f), BrahmGold)
                            if (moonRashi.isNotBlank())  Pill("$moonRashi Moon",   BrahmMuted.copy(0.8f), BrahmMutedForeground)
                        }
                    }
                }
            }

            // ── Primary stone (lagna lord) ────────────────────────────
            if (primaryEntry != null) {
                item {
                    SectionHeader(
                        icon  = "★",
                        label = "PRIMARY GEMSTONE — $lagnaRashi",
                        color = Color(0xFFF59E0B),
                    )
                }
                item {
                    StoneCard(
                        label    = "Lagna Stone · ${primaryEntry.planet}",
                        entry    = primaryEntry,
                        badge    = "Primary",
                        badgeBg  = BrahmGold.copy(0.15f),
                        badgeFg  = BrahmGold,
                    )
                }
            }

            // ── Secondary stone (moon) ────────────────────────────────
            if (secondaryEntry != null && secondaryEntry.planet != primaryEntry?.planet) {
                item {
                    SectionHeader(
                        icon  = "☽",
                        label = "SECONDARY GEMSTONE — $moonRashi",
                        color = Color(0xFF4F46E5),
                    )
                }
                item {
                    StoneCard(
                        label   = "Moon Stone · ${secondaryEntry.planet}",
                        entry   = secondaryEntry,
                        badge   = "Secondary",
                        badgeBg = Color(0xFF4F46E5).copy(0.12f),
                        badgeFg = Color(0xFF4F46E5),
                    )
                }
            }

            // ── Benefic / supportive stones ───────────────────────────
            if (uniqueBenefic.isNotEmpty()) {
                item {
                    SectionHeader(icon = "✦", label = "BENEFIC SUPPORTIVE GEMS", color = Color(0xFF43A047))
                }
                uniqueBenefic.forEach { (label, _, entry) ->
                    item {
                        StoneCard(
                            label   = label,
                            entry   = entry,
                        )
                    }
                }
            }

            // ── Exalted planets ───────────────────────────────────────
            if (exaltedPlanets.isNotEmpty()) {
                item {
                    SectionHeader(icon = "↑", label = "EXALTED PLANETS — FORTIFY", color = Color(0xFF43A047))
                }
                item {
                    Text(
                        "These planets are exalted in your chart. Wearing their gem amplifies their already-strong benefic energy.",
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp),
                    )
                }
                exaltedPlanets.forEach { (planet, entry) ->
                    item {
                        StoneCard(
                            label   = "Exalted $planet",
                            entry   = entry,
                            badge   = "Exalted",
                            badgeBg = Color(0xFF43A047).copy(0.12f),
                            badgeFg = Color(0xFF43A047),
                        )
                    }
                }
            }

            // ── Debilitated planets ───────────────────────────────────
            if (debilitatedPlanets.isNotEmpty()) {
                item {
                    SectionHeader(icon = "⚠", label = "DEBILITATED PLANETS — REMEDY", color = Color(0xFFF59E0B))
                }
                item {
                    Text(
                        "These planets are debilitated (Neecha). Wearing their gemstone can help strengthen and stabilise their weak energy.",
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp),
                    )
                }
                debilitatedPlanets.forEach { (planet, entry) ->
                    item {
                        StoneCard(
                            label    = "Remedy for $planet",
                            entry    = entry,
                            remedyFor = planet,
                        )
                    }
                }
            }

            // ── Cautions ──────────────────────────────────────────────
            item {
                SectionHeader(icon = "⚠", label = "IMPORTANT CAUTIONS", color = Color(0xFFE53935))
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrahmCard)
                        .border(1.dp, Color(0xFFE53935).copy(0.2f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CAUTION_RULES.forEach { rule ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("⚠", fontSize = 11.sp, color = Color(0xFFF59E0B), modifier = Modifier.padding(top = 1.dp))
                            Text(
                                rule,
                                style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, lineHeight = 17.sp, fontSize = 11.sp),
                            )
                        }
                    }
                }
            }

            // Disclaimer
            item {
                Text(
                    "Disclaimer: Gemstone recommendations are for spiritual/astrological guidance only. Always consult a qualified astrologer and medical professional. Results may vary.",
                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground.copy(0.55f), fontSize = 9.sp, lineHeight = 13.sp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }

    }
}

// ── Stone Card (matches website StoneCard) ────────────────────────────────────

@Composable
private fun StoneCard(
    label: String,
    entry: StoneEntry,
    badge: String? = null,
    badgeBg: Color = BrahmMuted,
    badgeFg: Color = BrahmMutedForeground,
    remedyFor: String? = null,
) {
    val gemCol = gemColor(entry.color)
    val wear   = STONE_WEAR[entry.planet]
    val isCaution = entry.planet == "Shani"
    var expanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(gemCol.copy(alpha = 0.06f))
            .border(1.dp, gemCol.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── Header row: gem icon + name + badge ───────────────────────
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Gem icon box
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(gemCol.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(gemEmoji(entry.color), fontSize = 22.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    entry.stone,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrahmForeground),
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp),
                )
            }
            // Badges
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (badge != null) {
                    Pill(badge, badgeBg, badgeFg)
                }
                if (isCaution) {
                    Pill("⚠ Caution", Color(0xFFF59E0B).copy(0.15f), Color(0xFFF59E0B))
                }
            }
        }

        // ── Planet + Color chips ──────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill("Planet: ${entry.planet}", BrahmMuted.copy(0.5f), BrahmMutedForeground)
            Pill("Color: ${entry.color}",   BrahmMuted.copy(0.5f), BrahmMutedForeground)
        }

        // ── Purpose ───────────────────────────────────────────────────
        Text(
            entry.purpose,
            style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, lineHeight = 17.sp, fontSize = 12.sp),
        )

        // Remedy note
        if (remedyFor != null) {
            Text(
                "Strengthens debilitated $remedyFor",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFF59E0B).copy(0.85f), fontSize = 10.sp),
            )
        }

        // ── How to wear toggle ────────────────────────────────────────
        if (wear != null) {
            HorizontalDivider(color = gemCol.copy(alpha = 0.15f))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "ℹ How to wear",
                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontSize = 11.sp),
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = BrahmGold,
                    modifier = Modifier.size(14.dp),
                )
            }

            if (expanded) {
                // 2-column grid: Metal, Finger, Day + full-width Mantra
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrahmMuted.copy(alpha = 0.3f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WearCell("Metal",  wear.metal,  Modifier.weight(1f))
                        WearCell("Finger", wear.finger, Modifier.weight(1f))
                    }
                    WearCell("Day", wear.day, Modifier.fillMaxWidth())
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Mantra",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp),
                        )
                        Text(
                            wear.mantra,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color      = BrahmGold,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 13.sp,
                                lineHeight = 18.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WearCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 9.sp))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, fontWeight = FontWeight.Medium, fontSize = 11.sp))
    }
}

@Composable
private fun SectionHeader(icon: String, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(icon, fontSize = 14.sp, color = color)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                color         = color,
                fontWeight    = FontWeight.SemiBold,
                fontSize      = 11.sp,
                letterSpacing = 0.8.sp,
            ),
        )
    }
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(50.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall.copy(color = fg, fontSize = 10.sp))
    }
}

// ── Input Form ────────────────────────────────────────────────────────────────

@Composable
fun GemstoneInputForm(
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
        contentPadding  = PaddingValues(horizontal = 16.dp, top = 4.dp, bottom = 16.dp),
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
                        name = name, onNameChange = onNameChange,
                        dob  = dob,  onDobChange  = onDobChange,
                        tob  = tob,  onTobChange  = onTobChange,
                        pob  = pob,  onPobChange  = onPobChange,
                        onCitySelected = onCitySelected,
                    )
                    if (error != null) {
                        Text(error, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)))
                    }
                    BrahmButton(text = "Get Gemstone Recommendations", onClick = onCalculate)
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
                Text("💎  Vedic Gemology", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
                Text(
                    "Vedic astrology recommends specific gemstones based on your Lagna (Ascendant) lord and benefic planets. The primary stone strengthens your Lagna lord while supporting stones activate 4th, 9th, and 10th house lords.",
                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.8f), lineHeight = 18.sp),
                )
            }
        }
    }
}
