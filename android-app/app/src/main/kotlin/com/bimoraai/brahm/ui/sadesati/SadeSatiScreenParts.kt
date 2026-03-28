package com.bimoraai.brahm.ui.sadesati

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimoraai.brahm.core.components.ScrollToTopFab
import com.bimoraai.brahm.core.theme.*

// ── Constants ─────────────────────────────────────────────────────────────────

private val RASHIS = listOf(
    "Mesha", "Vrishabha", "Mithuna", "Karka",
    "Simha", "Kanya", "Tula", "Vrischika",
    "Dhanu", "Makara", "Kumbha", "Meena",
)

private val RASHI_IDX = mapOf(
    "Mesha" to 0, "Vrishabha" to 1, "Mithuna" to 2, "Karka" to 3,
    "Simha" to 4, "Kanya" to 5, "Tula" to 6, "Vrischika" to 7,
    "Dhanu" to 8, "Makara" to 9, "Kumbha" to 10, "Meena" to 11,
)

private val RASHI_SYMBOLS = mapOf(
    "Mesha" to "♈\uFE0E", "Vrishabha" to "♉\uFE0E", "Mithuna" to "♊\uFE0E", "Karka" to "♋\uFE0E",
    "Simha" to "♌\uFE0E", "Kanya" to "♍\uFE0E", "Tula" to "♎\uFE0E", "Vrischika" to "♏\uFE0E",
    "Dhanu" to "♐\uFE0E", "Makara" to "♑\uFE0E", "Kumbha" to "♒\uFE0E", "Meena" to "♓\uFE0E",
)

private val KANTAKA_HOUSES = setOf(1, 4, 7, 10)

private val REMEDIES = listOf(
    "Worship Lord Shani every Saturday with sesame oil lamp and black sesame.",
    "Recite Shani Stotra or Shani Chalisa every Saturday morning.",
    "Donate black items (black sesame, mustard oil, iron, black cloth) on Saturdays.",
    "Serve and feed the poor, elderly, and disabled persons.",
    "Wear a blue sapphire (Neelam) only after consulting a qualified Jyotishi.",
    "Chant \"Om Sham Shanicharaya Namah\" 108 times daily.",
    "Visit Shani temples and offer mustard oil to the Shani idol.",
    "Avoid non-vegetarian food and alcohol on Saturdays.",
    "Be patient, disciplined, and karma-focused — Shani rewards sincere effort.",
    "Perform Hanuman puja — Lord Hanuman is said to mitigate Shani's harshness.",
)

// ── Computation ───────────────────────────────────────────────────────────────

private enum class SadeSatiPhase { Rising, Peak, Setting }
private enum class ShaniStatus   { SADE_SATI, ASHTAMA, KANTAKA, CLEAR }

private data class ShaniAnalysis(
    val status:         ShaniStatus,
    val sadeSatiPhase:  SadeSatiPhase?,
    val shaniFromMoon:  Int,
    val shaniFromLagna: Int,   // 0 if lagna unknown
    val moonRashi:      String,
    val shaniRashi:     String,
)

private fun computeShaniAnalysis(moonRashi: String, shaniRashi: String, lagnaRashi: String): ShaniAnalysis {
    val moonIdx  = RASHI_IDX[moonRashi]  ?: 0
    val shaniIdx = RASHI_IDX[shaniRashi] ?: 0
    val shaniFromMoon = ((shaniIdx - moonIdx + 12) % 12) + 1

    var status: ShaniStatus = ShaniStatus.CLEAR
    var phase:  SadeSatiPhase? = null

    when (shaniFromMoon) {
        12 -> { status = ShaniStatus.SADE_SATI; phase = SadeSatiPhase.Rising  }
        1  -> { status = ShaniStatus.SADE_SATI; phase = SadeSatiPhase.Peak    }
        2  -> { status = ShaniStatus.SADE_SATI; phase = SadeSatiPhase.Setting }
        8  -> { status = ShaniStatus.ASHTAMA }
    }

    var shaniFromLagna = 0
    if (lagnaRashi.isNotBlank()) {
        val lagnaIdx = RASHI_IDX[lagnaRashi] ?: 0
        shaniFromLagna = ((shaniIdx - lagnaIdx + 12) % 12) + 1
        if (status == ShaniStatus.CLEAR && shaniFromLagna in KANTAKA_HOUSES) {
            status = ShaniStatus.KANTAKA
        }
    }

    return ShaniAnalysis(status, phase, shaniFromMoon, shaniFromLagna, moonRashi, shaniRashi)
}

private fun getNextSadeSatiRashi(moonRashi: String): String {
    val idx = RASHI_IDX[moonRashi] ?: 0
    return RASHIS[(idx - 1 + 12) % 12]
}

// ── Main composable ───────────────────────────────────────────────────────────

@Composable
fun SadeSatiContent(
    shaniRashi:         String,
    shaniDegree:        Double,
    lagnaRashi:         String,
    isLoading:          Boolean,
    saturnError:        String?,
    selectedMoonRashi:  String,
    onMoonRashiSelected:(String) -> Unit,
) {
    val analysis = if (selectedMoonRashi.isNotBlank() && shaniRashi.isNotBlank())
        computeShaniAnalysis(selectedMoonRashi, shaniRashi, lagnaRashi)
    else null

    var remediesOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // ── Saturn Position Card ──
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrahmCard),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("♄", fontSize = 28.sp, color = Color(0xFF64748B))
                        Column {
                            Text(
                                "SATURN TODAY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BrahmMutedForeground,
                                    letterSpacing = 1.sp,
                                ),
                            )
                            when {
                                isLoading -> Box(
                                    Modifier.width(120.dp).height(18.dp).clip(RoundedCornerShape(4.dp)).background(BrahmMutedForeground.copy(alpha = 0.15f))
                                )
                                saturnError != null -> Text(saturnError, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)))
                                shaniRashi.isNotBlank() -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(RASHI_SYMBOLS[shaniRashi] ?: "", fontSize = 24.sp, color = BrahmGold, fontWeight = androidx.compose.ui.text.font.FontWeight.Light)
                                    Text(shaniRashi, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Text("${"%.2f".format(shaniDegree)}°", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                                }
                                else -> Text("—", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                            }
                        }
                    }
                    Text(
                        "~2.5 yrs\nper rashi",
                        style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground.copy(alpha = 0.6f)),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }

        // ── Moon Rashi Selector ──
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrahmCard),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("☽", fontSize = 16.sp, color = Color(0xFF4F46E5))
                        Text("Your Moon Rashi", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        if (selectedMoonRashi.isNotBlank()) {
                            Box(
                                Modifier.clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("from kundali", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF10B981)))
                            }
                        }
                    }
                    // 4-column grid of rashis
                    RASHIS.chunked(4).forEach { rowRashis ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowRashis.forEach { rashi ->
                                val selected = selectedMoonRashi == rashi
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) BrahmGold.copy(alpha = 0.15f) else BrahmMutedForeground.copy(alpha = 0.06f))
                                        .border(1.dp, if (selected) BrahmGold.copy(alpha = 0.5f) else BrahmBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable { onMoonRashiSelected(rashi) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            RASHI_SYMBOLS[rashi] ?: "",
                                            fontSize = 22.sp,
                                            color = if (selected) BrahmGold else BrahmMutedForeground,
                                            fontWeight = FontWeight.Light,
                                        )
                                        Text(
                                            rashi,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (selected) BrahmGold else BrahmMutedForeground,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
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

        // ── Status Banner ──
        if (analysis != null) {
            item {
                val (bgColor, borderColor, textColor, iconTint, title, subtitle) = when (analysis.status) {
                    ShaniStatus.SADE_SATI -> BannerConfig(
                        bgColor   = Color(0xFFDC2626).copy(alpha = 0.1f),
                        border    = Color(0xFFDC2626).copy(alpha = 0.3f),
                        textColor = Color(0xFFDC2626),
                        icon      = Icons.Default.Warning,
                        title     = "Sade Sati Active — ${analysis.sadeSatiPhase} Phase",
                        subtitle  = when (analysis.sadeSatiPhase) {
                            SadeSatiPhase.Rising  -> "Saturn is in ${shaniRashi}, the 12th house from your Moon (${analysis.moonRashi}). Rising phase — approaching the peak."
                            SadeSatiPhase.Peak    -> "Saturn is in ${shaniRashi}, the same sign as your Moon (${analysis.moonRashi}). Peak phase — most intense period."
                            SadeSatiPhase.Setting -> "Saturn is in ${shaniRashi}, the 2nd house from your Moon (${analysis.moonRashi}). Setting phase — gradually easing."
                            null -> ""
                        },
                    )
                    ShaniStatus.ASHTAMA -> BannerConfig(
                        bgColor   = Color(0xFFEA580C).copy(alpha = 0.1f),
                        border    = Color(0xFFEA580C).copy(alpha = 0.3f),
                        textColor = Color(0xFFEA580C),
                        icon      = Icons.Default.Warning,
                        title     = "Ashtama Shani Active",
                        subtitle  = "Saturn in the 8th house from your Moon (${analysis.moonRashi}) — a period calling for caution and inner strength.",
                    )
                    ShaniStatus.KANTAKA -> BannerConfig(
                        bgColor   = Color(0xFFD97706).copy(alpha = 0.1f),
                        border    = Color(0xFFD97706).copy(alpha = 0.3f),
                        textColor = Color(0xFFD97706),
                        icon      = Icons.Default.Warning,
                        title     = "Kantaka Shani — H${analysis.shaniFromLagna} from Lagna",
                        subtitle  = "Saturn is in the ${analysis.shaniFromLagna}th house (${shaniRashi}) from your Lagna — a significant transit position.",
                    )
                    ShaniStatus.CLEAR -> BannerConfig(
                        bgColor   = Color(0xFF10B981).copy(alpha = 0.1f),
                        border    = Color(0xFF10B981).copy(alpha = 0.3f),
                        textColor = Color(0xFF10B981),
                        icon      = Icons.Default.CheckCircle,
                        title     = "Clear — No Sade Sati",
                        subtitle  = "Saturn is in the ${analysis.shaniFromMoon}${ordinal(analysis.shaniFromMoon)} house from your Moon (${analysis.moonRashi}). No major Saturn affliction active.",
                    )
                }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    modifier = Modifier.fillMaxWidth().border(1.dp, borderColor, RoundedCornerShape(14.dp)),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(iconTint, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = textColor))
                        }
                        Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground), modifier = Modifier.padding(start = 28.dp))
                    }
                }
            }

            // ── Sade Sati Phase Details ──
            if (analysis.sadeSatiPhase != null) {
                item {
                    val moonIdx = RASHI_IDX[analysis.moonRashi] ?: 0
                    val risingRashi  = RASHIS[(moonIdx - 1 + 12) % 12]
                    val settingRashi = RASHIS[(moonIdx + 1) % 12]

                    val phases = listOf(
                        PhaseRow(
                            label    = "Rising Phase",
                            rashi    = risingRashi,
                            active   = analysis.sadeSatiPhase == SadeSatiPhase.Rising,
                            color    = Color(0xFFD97706),
                            activeBg = Color(0xFFD97706).copy(alpha = 0.08f),
                            desc     = "Saturn transits the 12th from Moon. Subtle but growing pressure — disturbances in sleep, travel, mental unrest.",
                        ),
                        PhaseRow(
                            label    = "Peak Phase",
                            rashi    = analysis.moonRashi,
                            active   = analysis.sadeSatiPhase == SadeSatiPhase.Peak,
                            color    = Color(0xFFDC2626),
                            activeBg = Color(0xFFDC2626).copy(alpha = 0.08f),
                            desc     = "Saturn conjunct natal Moon. The most intense phase — health, career, relationships tested significantly.",
                        ),
                        PhaseRow(
                            label    = "Setting Phase",
                            rashi    = settingRashi,
                            active   = analysis.sadeSatiPhase == SadeSatiPhase.Setting,
                            color    = Color(0xFF10B981),
                            activeBg = Color(0xFF10B981).copy(alpha = 0.08f),
                            desc     = "Saturn in the 2nd from Moon. Gradual relief — finances and speech affected; slow recovery as Saturn moves away.",
                        ),
                    )

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BrahmCard),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFDC2626).copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Sade Sati Lifecycle", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                            phases.forEach { p ->
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (p.active) p.activeBg else Color.Transparent)
                                        .border(1.dp, if (p.active) p.color.copy(alpha = 0.3f) else BrahmBorder.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .alpha(if (p.active) 1f else 0.55f)
                                        .padding(12.dp),
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(p.label, style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (p.active) p.color else BrahmMutedForeground,
                                                ))
                                                if (p.active) {
                                                    Box(
                                                        Modifier.clip(RoundedCornerShape(20.dp))
                                                            .background(p.activeBg)
                                                            .border(1.dp, p.color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("NOW", style = MaterialTheme.typography.labelSmall.copy(color = p.color, fontWeight = FontWeight.Bold, fontSize = 9.sp))
                                                    }
                                                }
                                            }
                                            Text(
                                                "${RASHI_SYMBOLS[p.rashi] ?: ""} ${p.rashi}",
                                                style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold),
                                            )
                                        }
                                        if (p.active) {
                                            Text(p.desc, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Ashtama Shani detail ──
            if (analysis.status == ShaniStatus.ASHTAMA) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BrahmCard),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFEA580C).copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Ashtama Shani", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFFEA580C)))
                            Text(
                                "Saturn in the 8th house from the Moon is called Ashtama Shani. This transit can bring sudden changes, obstacles, health concerns, and hidden challenges.",
                                style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                            )
                            Text(
                                "Advice: Exercise caution in financial decisions, avoid risky ventures, and focus on spiritual practices. This period builds resilience.",
                                style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                            )
                        }
                    }
                }
            }

            // ── Kantaka Shani detail ──
            if (analysis.status == ShaniStatus.KANTAKA && lagnaRashi.isNotBlank()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BrahmCard),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFD97706).copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Kantaka Shani — H${analysis.shaniFromLagna} from Lagna ($lagnaRashi)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFFD97706)),
                            )
                            Text(
                                "Kantaka Shani occurs when Saturn transits the 1st, 4th, 7th, or 10th house from the Lagna (Ascendant). These are kendra houses — Saturn here creates friction in core life areas.",
                                style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                            )
                        }
                    }
                }
            }

            // ── Upcoming Sade Sati (when not active) ──
            if (analysis.status != ShaniStatus.SADE_SATI && selectedMoonRashi.isNotBlank()) {
                item {
                    val nextTrigger = getNextSadeSatiRashi(selectedMoonRashi)
                    val moonIdx = RASHI_IDX[selectedMoonRashi] ?: 0
                    val r3 = RASHIS[(moonIdx + 1) % 12]
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BrahmCard),
                        modifier = Modifier.fillMaxWidth().border(1.dp, BrahmBorder.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    ) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = BrahmMutedForeground.copy(alpha = 0.6f), modifier = Modifier.size(16.dp).padding(top = 1.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Next Sade Sati", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    "Your next Sade Sati begins when Saturn enters ${RASHI_SYMBOLS[nextTrigger] ?: ""}$nextTrigger (12th from your Moon — $selectedMoonRashi). Currently Saturn is in $shaniRashi.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                                )
                                Text(
                                    "Sade Sati spans 3 rashis: $nextTrigger → $selectedMoonRashi → $r3 (~7.5 years total).",
                                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground.copy(alpha = 0.7f)),
                                )
                            }
                        }
                    }
                }
            }

            // ── Shani Summary Table ──
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrahmCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BrahmBorder.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                ) {
                    Column {
                        Box(Modifier.fillMaxWidth().background(BrahmMutedForeground.copy(alpha = 0.06f)).padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text("SHANI STHANA SUMMARY", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, letterSpacing = 1.sp))
                        }
                        val rows = buildList {
                            add("Moon Rashi" to "${RASHI_SYMBOLS[selectedMoonRashi] ?: ""} $selectedMoonRashi")
                            if (shaniRashi.isNotBlank()) add("Saturn Current Rashi" to "${RASHI_SYMBOLS[shaniRashi] ?: ""} $shaniRashi")
                            add("Saturn from Moon" to "${analysis.shaniFromMoon}${ordinal(analysis.shaniFromMoon)} house")
                            if (lagnaRashi.isNotBlank() && analysis.shaniFromLagna > 0) add("Saturn from Lagna" to "${analysis.shaniFromLagna}${ordinal(analysis.shaniFromLagna)} house")
                            add("Status" to when {
                                analysis.sadeSatiPhase != null -> "Active — ${analysis.sadeSatiPhase} Phase"
                                analysis.status == ShaniStatus.ASHTAMA -> "Ashtama Shani"
                                analysis.status == ShaniStatus.KANTAKA -> "Kantaka Shani (H${analysis.shaniFromLagna})"
                                else -> "Clear"
                            })
                        }
                        rows.forEachIndexed { i, (label, value) ->
                            if (i > 0) HorizontalDivider(color = BrahmBorder.copy(alpha = 0.3f))
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(label, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                                Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }
        }

        // ── Remedies (collapsible) ──
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrahmCard),
                modifier = Modifier.fillMaxWidth().border(1.dp, BrahmBorder.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { remediesOpen = !remediesOpen },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("★", fontSize = 14.sp, color = BrahmGold)
                            Text("Shani Remedies", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                        Icon(
                            if (remediesOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null, tint = BrahmMutedForeground, modifier = Modifier.size(20.dp),
                        )
                    }
                    if (remediesOpen) {
                        Spacer(Modifier.height(12.dp))
                        REMEDIES.forEach { remedy ->
                            Row(Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("★", fontSize = 10.sp, color = BrahmGold, modifier = Modifier.padding(top = 3.dp))
                                Text(remedy, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                            }
                        }
                    }
                }
            }
        }

        // ── About Sade Sati ──
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrahmCard),
                modifier = Modifier.fillMaxWidth().border(1.dp, BrahmBorder.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("About Sade Sati", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        "Sade Sati (literally \"seven and a half\") is a 7.5-year period in Vedic astrology when Saturn (Shani) transits through three consecutive zodiac signs — the sign before your Moon sign, the sign of your Moon, and the sign after. Each sign takes approximately 2.5 years.",
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                    )
                    Text(
                        "This period is traditionally associated with challenges, delays, and tests in various areas of life — particularly career, health, relationships, and finances. However, it also brings discipline, wisdom, and karmic lessons.",
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                    )
                    Text(
                        "Not all Sade Sati periods are equally difficult. The effects depend on Saturn's placement in your natal chart, its strength, and the running dasha (period). Some people experience major transformations and growth during Sade Sati.",
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                    )
                }
            }
        }

    } // LazyColumn
    ScrollToTopFab(listState, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp))
    } // Box
}

// ── Helper data classes ───────────────────────────────────────────────────────

private data class BannerConfig(
    val bgColor:   Color,
    val border:    Color,
    val textColor: Color,
    val icon:      androidx.compose.ui.graphics.vector.ImageVector,
    val title:     String,
    val subtitle:  String,
)

private data class PhaseRow(
    val label:    String,
    val rashi:    String,
    val active:   Boolean,
    val color:    Color,
    val activeBg: Color,
    val desc:     String,
)

private fun ordinal(n: Int) = when (n) {
    1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th"
}
