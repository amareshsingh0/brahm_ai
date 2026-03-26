package com.bimoraai.brahm.ui.compatibility

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.bimoraai.brahm.core.components.BrahmButton
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private data class KootRow(val label: String, val key: String, val max: Int, val description: String)
private val kootRows = listOf(
    KootRow("Varna",        "varna",       1,  "Spiritual compatibility"),
    KootRow("Vashya",       "vashya",      2,  "Mutual attraction & control"),
    KootRow("Tara",         "tara",        3,  "Health & longevity"),
    KootRow("Yoni",         "yoni",        4,  "Physical compatibility"),
    KootRow("Graha Maitri", "graha_maitri",5,  "Mental friendship"),
    KootRow("Gana",         "gana",        6,  "Temperament"),
    KootRow("Bhakoot",      "bhakoot",     7,  "Family harmony"),
    KootRow("Nadi",         "nadi",        8,  "Health & progeny"),
)

private data class LifeAreaRow(val label: String, val emoji: String, val key: String)
private val lifeAreas = listOf(
    LifeAreaRow("Career",       "💼", "career"),
    LifeAreaRow("Relationship", "💫", "relationship"),
    LifeAreaRow("Family",       "🏡", "family"),
    LifeAreaRow("Wealth",       "💰", "wealth"),
    LifeAreaRow("Health",       "🌿", "health"),
    LifeAreaRow("Mental",       "🧘", "mental"),
)

@Composable
fun CompatibilityContent(data: JsonObject) {
    val totalScore = data["total_score"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull()
        ?: data["score"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull() ?: 0f
    val maxScore = 36f
    val scorePercent = (totalScore / maxScore).coerceIn(0f, 1f)
    val verdict = when {
        totalScore >= 28 -> Triple("Excellent Match", Color(0xFF43A047), "❤️")
        totalScore >= 18 -> Triple("Good Match",      Color(0xFF7CB342), "💚")
        totalScore >= 12 -> Triple("Average Match",   Color(0xFFFF8F00), "💛")
        else             -> Triple("Below Average",   Color(0xFFE53935), "💔")
    }

    val kootData = data["koot"]?.let { el ->
        try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
    }
    val lifeData = data["life_areas"]?.let { el ->
        try { el.jsonObject } catch (_: Exception) { null }
    }
    val doshas      = data["doshas"]?.let { el -> try { el.jsonObject } catch (_: Exception) { null } }
    val mangalDosha = data["mangal_dosha"]?.jsonPrimitive?.contentOrNull
    val kaalSarp    = data["kaal_sarp"]?.jsonPrimitive?.contentOrNull

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Total Score Hero ──
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrahmCard),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(verdict.third, fontSize = 36.sp)
                    Text(
                        "${totalScore.toInt()} / ${maxScore.toInt()} Points",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    LinearProgressIndicator(
                        progress = { scorePercent },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = verdict.second,
                        trackColor = BrahmBorder,
                    )
                    Box(
                        Modifier.clip(RoundedCornerShape(20.dp)).background(verdict.second.copy(alpha = 0.12f))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            verdict.first,
                            style = MaterialTheme.typography.titleSmall.copy(color = verdict.second, fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }

        // ── Ashta-Koot Table ──
        item {
            Text("⭐ Ashta-Koot Analysis", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrahmCard),
            ) {
                Column(Modifier.padding(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(BrahmGold.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text("Koot",   style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground), modifier = Modifier.weight(1.5f))
                        Text("Score", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground), modifier = Modifier.weight(0.7f))
                        Text("Max",   style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground), modifier = Modifier.weight(0.6f))
                        Text("Status",style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground), modifier = Modifier.weight(1.2f))
                    }
                    HorizontalDivider(color = BrahmBorder)

                    kootRows.forEachIndexed { idx, koot ->
                        val kootObj = kootData?.find {
                            it["name"]?.jsonPrimitive?.contentOrNull?.contains(koot.label, ignoreCase = true) == true
                        }
                        val score = kootObj?.get("score")?.jsonPrimitive?.contentOrNull?.toFloatOrNull()
                            ?: data[koot.key]?.jsonPrimitive?.contentOrNull?.toFloatOrNull() ?: 0f
                        val isGood = score >= koot.max * 0.5f
                        val scoreColor = when {
                            score >= koot.max * 0.75f -> Color(0xFF43A047)
                            score >= koot.max * 0.5f  -> Color(0xFFFF8F00)
                            else                       -> Color(0xFFE53935)
                        }

                        if (idx > 0) HorizontalDivider(color = BrahmBorder.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1.5f)) {
                                Text(koot.label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                Text(koot.description, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontSize = 10.sp))
                            }
                            Text(
                                score.toInt().toString(),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = scoreColor),
                                modifier = Modifier.weight(0.7f),
                            )
                            Text(
                                koot.max.toString(),
                                style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                                modifier = Modifier.weight(0.6f),
                            )
                            Box(Modifier.weight(1.2f)) {
                                Box(
                                    Modifier.clip(RoundedCornerShape(4.dp)).background(scoreColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        if (isGood) "Good" else "Weak",
                                        style = MaterialTheme.typography.labelSmall.copy(color = scoreColor, fontSize = 10.sp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Life Area Analysis ──
        item {
            Text("🌟 Life Area Compatibility", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrahmCard),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    lifeAreas.forEach { area ->
                        val areaObj = lifeData?.get(area.key)?.let {
                            try { it.jsonObject } catch (_: Exception) { null }
                        }
                        val score = areaObj?.get("score")?.jsonPrimitive?.contentOrNull?.toFloatOrNull()
                            ?: data["${area.key}_score"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull() ?: 0f
                        val areaColor = when {
                            score >= 75 -> Color(0xFF43A047)
                            score >= 50 -> Color(0xFFFF8F00)
                            else        -> Color(0xFFE53935)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${area.emoji} ${area.label}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                Spacer(Modifier.weight(1f))
                                Text("${score.toInt()}%", style = MaterialTheme.typography.labelSmall.copy(color = areaColor, fontWeight = FontWeight.Bold))
                            }
                            LinearProgressIndicator(
                                progress = { (score / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = areaColor,
                                trackColor = BrahmBorder,
                            )
                        }
                    }
                }
            }
        }

        // ── Dosha Analysis ──
        val doshaItems = listOfNotNull(
            mangalDosha?.let { "Mangal Dosha" to it },
            kaalSarp?.let { "Kaal Sarp" to it },
            doshas?.get("vedha")?.jsonPrimitive?.contentOrNull?.let { "Vedha Dosha" to it },
        )
        if (doshaItems.isNotEmpty()) {
            item {
                Text("⚠️ Dosha Analysis", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrahmCard),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        doshaItems.forEach { (doshaName, doshaInfo) ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    Modifier.size(8.dp).clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE53935))
                                        .align(Alignment.CenterVertically)
                                )
                                Column {
                                    Text(doshaName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                    Text(doshaInfo, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
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
fun CompatibilityInputForm(
    name1: String, dob1: String, tob1: String, pob1: String,
    name2: String, dob2: String, tob2: String, pob2: String,
    error: String?,
    onName1Change: (String) -> Unit, onDob1Change: (String) -> Unit,
    onTob1Change: (String) -> Unit,  onPob1Change: (String) -> Unit,
    onName2Change: (String) -> Unit, onDob2Change: (String) -> Unit,
    onTob2Change: (String) -> Unit,  onPob2Change: (String) -> Unit,
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
                    Text("👨 Person A — Groom / Partner 1", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(value = name1, onValueChange = onName1Change, label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                    OutlinedTextField(value = dob1, onValueChange = onDob1Change, label = { Text("Date of Birth (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                    OutlinedTextField(value = tob1, onValueChange = onTob1Change, label = { Text("Time of Birth (HH:MM)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                    OutlinedTextField(value = pob1, onValueChange = onPob1Change, label = { Text("Place of Birth") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("👩 Person B — Bride / Partner 2", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(value = name2, onValueChange = onName2Change, label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                    OutlinedTextField(value = dob2, onValueChange = onDob2Change, label = { Text("Date of Birth (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                    OutlinedTextField(value = tob2, onValueChange = onTob2Change, label = { Text("Time of Birth (HH:MM)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                    OutlinedTextField(value = pob2, onValueChange = onPob2Change, label = { Text("Place of Birth") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                }
            }
        }

        item {
            if (error != null) {
                Text(error, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)))
                Spacer(Modifier.height(4.dp))
            }
            BrahmButton(text = "Check Compatibility", onClick = onCalculate, modifier = Modifier.fillMaxWidth())
        }
    }
}
