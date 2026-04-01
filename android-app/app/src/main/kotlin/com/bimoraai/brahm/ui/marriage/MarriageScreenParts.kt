package com.bimoraai.brahm.ui.marriage

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimoraai.brahm.core.components.BirthInputFields
import com.bimoraai.brahm.core.components.BrahmButton
import com.bimoraai.brahm.core.components.RichAiCard
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ── Input form ────────────────────────────────────────────────────────────────

@Composable
fun MarriageInputForm(
    name: String,
    dob: String,
    tob: String,
    pob: String,
    gender: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onDobChange: (String) -> Unit,
    onTobChange: (String) -> Unit,
    onPobChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onCitySelected: (City) -> Unit,
    onCalculate: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = BrahmCard),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Vivah Analysis",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 17.sp,
                        color      = BrahmForeground,
                    )
                    Text(
                        "Classical Jyotish — Vimshottari Dasha + Gochar + Navamsa",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrahmMutedForeground,
                    )
                }
            }
        }

        item {
            BirthInputFields(
                name           = name,
                onNameChange   = onNameChange,
                dob            = dob,
                onDobChange    = onDobChange,
                tob            = tob,
                onTobChange    = onTobChange,
                pob            = pob,
                onPobChange    = onPobChange,
                onCitySelected = onCitySelected,
            )
        }

        // Gender selector
        item {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = BrahmCard),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gender", fontWeight = FontWeight.SemiBold, color = BrahmForeground, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            FilterChip(
                                selected = gender == g,
                                onClick  = { onGenderChange(g) },
                                label    = { Text(g, fontSize = 13.sp) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrahmGold,
                                    selectedLabelColor     = Color.White,
                                ),
                            )
                        }
                    }
                }
            }
        }

        if (error != null) {
            item {
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        }

        item {
            BrahmButton(text = "Analyze Marriage", onClick = onCalculate, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ── Results content ───────────────────────────────────────────────────────────

@Composable
fun MarriageContent(result: JsonObject) {
    LazyColumn(
        modifier        = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding  = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Probability ring card
        val timing = try { result["marriage_timing"]?.jsonObject } catch (_: Exception) { null }
        val currentPeriod = try { timing?.get("current_period")?.jsonObject } catch (_: Exception) { null }

        item {
            MarriageProbabilityCard(timing, currentPeriod)
        }

        // 2. Favorable timing windows
        val windows = try { timing?.get("favorable_windows")?.jsonArray?.mapNotNull { try { it.jsonObject } catch (_: Exception) { null } } } catch (_: Exception) { emptyList() }
        if (!windows.isNullOrEmpty()) {
            item { SectionLabel("Favorable Timing Windows") }
            item { TimingWindowsRow(windows) }
        }

        // 3. Spouse profile
        val spouse = try { result["spouse_profile"]?.jsonObject } catch (_: Exception) { null }
        if (spouse != null) {
            item { SectionLabel("Spouse Profile") }
            item { SpouseProfileCard(spouse) }
        }

        // 4. Marriage yogas
        val yogas = try { result["marriage_yogas"]?.jsonArray?.mapNotNull { try { it.jsonObject } catch (_: Exception) { null } } } catch (_: Exception) { emptyList() }
        if (!yogas.isNullOrEmpty()) {
            item { SectionLabel("Marriage Yogas") }
            item { MarriageYogasList(yogas) }
        }

        // 5. Delay factors (only if any present)
        val delays = try { result["delay_factors"]?.jsonArray?.mapNotNull { try { it.jsonObject } catch (_: Exception) { null } } } catch (_: Exception) { emptyList() }
        if (!delays.isNullOrEmpty()) {
            item { SectionLabel("Delay Factors") }
            item { DelayFactorsList(delays) }
        }

        // 6. AI narrative
        val ai = result["ai_analysis"]?.jsonPrimitive?.contentOrNull
        if (!ai.isNullOrBlank()) {
            item { SectionLabel("AI Analysis") }
            item { RichAiCard(ai) }
        }
    }
}

// ── Section 1: Probability ring ───────────────────────────────────────────────

@Composable
private fun MarriageProbabilityCard(timing: JsonObject?, currentPeriod: JsonObject?) {
    val probability = currentPeriod?.get("probability")?.jsonPrimitive?.contentOrNull ?: "—"
    val score       = currentPeriod?.get("score")?.jsonPrimitive?.intOrNull ?: 0
    val mahadasha   = currentPeriod?.get("mahadasha")?.jsonPrimitive?.contentOrNull ?: "—"
    val antardasha  = currentPeriod?.get("antardasha")?.jsonPrimitive?.contentOrNull ?: "—"
    val startDate   = currentPeriod?.get("start")?.jsonPrimitive?.contentOrNull ?: ""
    val endDate     = currentPeriod?.get("end")?.jsonPrimitive?.contentOrNull ?: ""
    val ageRange    = timing?.get("estimated_age_range")?.jsonPrimitive?.contentOrNull ?: ""
    val nextWindow  = timing?.get("next_strong_window")?.jsonPrimitive?.contentOrNull ?: ""

    val (ringColor, probLabel) = when (probability.lowercase()) {
        "high"     -> Pair(Color(0xFF16A34A), "High Probability")
        "moderate" -> Pair(Color(0xFFB45309), "Moderate Probability")
        "low"      -> Pair(Color(0xFFDC2626), "Low Probability")
        else       -> Pair(BrahmGold, probability)
    }

    // Normalize score (0–10) to 0–1 for ring
    val maxScore = 10f
    val fraction = (score.toFloat() / maxScore).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = fraction, animationSpec = tween(900), label = "ring")

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = BrahmCard),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Animated ring
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress          = { 1f },
                    modifier          = Modifier.size(120.dp),
                    strokeWidth       = 10.dp,
                    color             = BrahmCard.copy(alpha = 0.3f),
                    strokeCap         = StrokeCap.Round,
                    trackColor        = Color.Transparent,
                )
                CircularProgressIndicator(
                    progress          = { animatedProgress },
                    modifier          = Modifier.size(120.dp),
                    strokeWidth       = 10.dp,
                    color             = ringColor,
                    strokeCap         = StrokeCap.Round,
                    trackColor        = ringColor.copy(alpha = 0.15f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = "${score}/10",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp,
                        color      = ringColor,
                    )
                    Text("Score", fontSize = 11.sp, color = BrahmMutedForeground)
                }
            }

            Text(probLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ringColor)

            // Current dasha period
            HorizontalDivider(color = BrahmBorder)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Current Period", fontSize = 11.sp, color = BrahmMutedForeground)
                    Text("$mahadasha / $antardasha MD", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrahmForeground)
                    if (startDate.isNotBlank()) {
                        Text("$startDate → $endDate", fontSize = 11.sp, color = BrahmMutedForeground)
                    }
                }
                if (ageRange.isNotBlank()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Est. Age Range", fontSize = 11.sp, color = BrahmMutedForeground)
                        Text(ageRange, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrahmForeground)
                    }
                }
            }

            if (nextWindow.isNotBlank()) {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(8.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment   = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                        Text("Next strong window: $nextWindow", fontSize = 12.sp, color = Color(0xFF15803D))
                    }
                }
            }
        }
    }
}

// ── Section 2: Timing windows ─────────────────────────────────────────────────

@Composable
private fun TimingWindowsRow(windows: List<JsonObject>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding        = PaddingValues(horizontal = 2.dp),
    ) {
        items(windows) { w ->
            val period = w["period"]?.jsonPrimitive?.contentOrNull ?: "—"
            val start  = w["start"]?.jsonPrimitive?.contentOrNull ?: ""
            val end    = w["end"]?.jsonPrimitive?.contentOrNull ?: ""
            val prob   = w["probability"]?.jsonPrimitive?.contentOrNull ?: "—"
            val sc     = w["score"]?.jsonPrimitive?.intOrNull ?: 0

            val (bgColor, textColor) = when (prob.lowercase()) {
                "high"     -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
                "moderate" -> Pair(Color(0xFFFEF9C3), Color(0xFF92400E))
                else       -> Pair(Color(0xFFFEE2E2), Color(0xFFB91C1C))
            }

            Card(
                modifier  = Modifier.width(180.dp),
                shape     = RoundedCornerShape(14.dp),
                colors    = CardDefaults.cardColors(containerColor = BrahmCard),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            text       = "$sc/10",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp,
                            color      = textColor,
                        )
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(bgColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(prob, fontSize = 10.sp, color = textColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(period, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrahmForeground, maxLines = 2)
                    if (start.isNotBlank()) {
                        Text("$start → $end", fontSize = 10.sp, color = BrahmMutedForeground)
                    }
                }
            }
        }
    }
}

// ── Section 3: Spouse profile ─────────────────────────────────────────────────

@Composable
private fun SpouseProfileCard(spouse: JsonObject) {
    val sign        = spouse["7th_house_sign"]?.jsonPrimitive?.contentOrNull ?: "—"
    val lord        = spouse["7th_lord"]?.jsonPrimitive?.contentOrNull ?: "—"
    val lordIn      = spouse["7th_lord_placed_in"]?.jsonPrimitive?.contentOrNull ?: ""
    val planetIn7th = spouse["planet_in_7th"]?.jsonPrimitive?.contentOrNull
    val direction   = spouse["direction"]?.jsonPrimitive?.contentOrNull ?: ""
    val profHint    = spouse["profession_hint"]?.jsonPrimitive?.contentOrNull ?: ""
    val navamsa7th  = spouse["navamsa_7th"]?.jsonPrimitive?.contentOrNull ?: ""
    val traits      = try { spouse["traits"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } } catch (_: Exception) { emptyList() }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = BrahmCard),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("7th House Sign", fontSize = 11.sp, color = BrahmMutedForeground)
                    Text(sign, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BrahmGold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("7th Lord", fontSize = 11.sp, color = BrahmMutedForeground)
                    Text(lord, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BrahmForeground)
                    if (lordIn.isNotBlank()) {
                        Text(lordIn, fontSize = 11.sp, color = BrahmMutedForeground)
                    }
                }
            }

            if (!planetIn7th.isNullOrBlank() && planetIn7th != "null") {
                InfoRow(label = "Planet in 7th", value = planetIn7th)
            }
            if (direction.isNotBlank()) InfoRow(label = "Direction of Spouse", value = direction)
            if (profHint.isNotBlank()) InfoRow(label = "Profession Hint", value = profHint)
            if (navamsa7th.isNotBlank()) InfoRow(label = "Navamsa D9 Insight", value = navamsa7th)

            // Traits chips
            if (!traits.isNullOrEmpty()) {
                HorizontalDivider(color = BrahmBorder)
                Text("Spouse Traits", fontSize = 12.sp, color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(traits) { trait ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFFFF7ED))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(trait, fontSize = 12.sp, color = Color(0xFFB45309))
                        }
                    }
                }
            }
        }
    }
}

// ── Section 4: Yogas ──────────────────────────────────────────────────────────

@Composable
private fun MarriageYogasList(yogas: List<JsonObject>) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = BrahmCard),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(4.dp)) {
            yogas.forEachIndexed { idx, yoga ->
                val yogaName = yoga["name"]?.jsonPrimitive?.contentOrNull ?: "—"
                val present  = yoga["present"]?.jsonPrimitive?.booleanOrNull ?: false
                val desc     = yoga["description"]?.jsonPrimitive?.contentOrNull ?: ""

                if (idx > 0) HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = BrahmBorder)
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.Top,
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (present) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = if (present) Icons.Default.Check else Icons.Default.Remove,
                            contentDescription = null,
                            tint               = if (present) Color(0xFF16A34A) else BrahmMutedForeground,
                            modifier           = Modifier.size(16.dp),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(yogaName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrahmForeground)
                        if (desc.isNotBlank() && present) {
                            Text(desc, fontSize = 12.sp, color = BrahmMutedForeground)
                        }
                    }
                }
            }
        }
    }
}

// ── Section 5: Delay factors ──────────────────────────────────────────────────

@Composable
private fun DelayFactorsList(delays: List<JsonObject>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        delays.forEach { d ->
            val factor   = d["factor"]?.jsonPrimitive?.contentOrNull ?: "—"
            val effect   = d["effect"]?.jsonPrimitive?.contentOrNull ?: ""
            val severity = d["severity"]?.jsonPrimitive?.contentOrNull ?: "Moderate"

            val (bgColor, textColor) = when (severity.lowercase()) {
                "high", "strong"   -> Pair(Color(0xFFFEE2E2), Color(0xFFB91C1C))
                "moderate", "medium" -> Pair(Color(0xFFFEF9C3), Color(0xFF92400E))
                else               -> Pair(Color(0xFFF1F5F9), BrahmMutedForeground)
            }

            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = BrahmCard),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.Top,
                ) {
                    Icon(
                        imageVector        = Icons.Default.Warning,
                        contentDescription = null,
                        tint               = textColor,
                        modifier           = Modifier.size(18.dp).padding(top = 2.dp),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically,
                        ) {
                            Text(factor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrahmForeground, modifier = Modifier.weight(1f))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(bgColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(severity, fontSize = 10.sp, color = textColor, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (effect.isNotBlank()) {
                            Text(effect, fontSize = 12.sp, color = BrahmMutedForeground)
                        }
                    }
                }
            }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp,
        color      = BrahmForeground,
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = BrahmMutedForeground)
        Text(value, fontSize = 12.sp, color = BrahmForeground, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1f, fill = false))
    }
}

@Composable
private fun InfoCard(message: String) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = BrahmCard),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Text(
            message,
            modifier = Modifier.padding(16.dp),
            color    = BrahmMutedForeground,
            fontSize = 13.sp,
        )
    }
}
