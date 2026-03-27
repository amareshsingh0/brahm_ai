package com.bimoraai.brahm.ui.prashna

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.bimoraai.brahm.core.components.ScrollToTopFab
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

private val questionTypes = listOf("General", "Career", "Marriage", "Health", "Finance", "Travel", "Legal", "Education", "Property", "Spirituality")

@Composable
fun PrashnaContent(data: JsonObject) {
    val verdict      = data["verdict"]?.jsonPrimitive?.contentOrNull ?: data["answer"]?.jsonPrimitive?.contentOrNull ?: "—"
    val horaLord     = data["hora_lord"]?.jsonPrimitive?.contentOrNull ?: "—"
    val horaDesc     = data["hora_description"]?.jsonPrimitive?.contentOrNull ?: ""
    val lagna        = data["lagna"]?.jsonPrimitive?.contentOrNull ?: "—"
    val moonPos      = data["moon_position"]?.jsonPrimitive?.contentOrNull ?: "—"
    val interpretation = data["interpretation"]?.jsonPrimitive?.contentOrNull ?: ""
    val factors      = data["factors"]?.let { el ->
        try { el.jsonArray.map { it.jsonPrimitive.contentOrNull ?: "" } } catch (_: Exception) { null }
    }
    val question     = data["question"]?.jsonPrimitive?.contentOrNull ?: ""

    val verdictColor = when {
        verdict.contains("yes", ignoreCase = true) || verdict.contains("favorable", ignoreCase = true) -> Color(0xFF43A047)
        verdict.contains("no", ignoreCase = true)  || verdict.contains("unfavorable", ignoreCase = true) -> Color(0xFFE53935)
        else -> Color(0xFFFF8F00)
    }
    val verdictEmoji = when {
        verdict.contains("yes", ignoreCase = true) -> "✅"
        verdict.contains("no", ignoreCase = true)  -> "❌"
        else -> "⚡"
    }

    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── Verdict Hero ──
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrahmCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, verdictColor.copy(alpha = 0.3f)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(verdictEmoji, fontSize = 40.sp)
                    Text(
                        verdict.uppercase(),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = verdictColor),
                    )
                    if (question.isNotBlank()) {
                        Text(
                            "\"$question\"",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                    if (interpretation.isNotBlank()) {
                        HorizontalDivider(color = BrahmBorder)
                        Text(
                            interpretation,
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.85f), fontSize = 12.sp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }

        // ── Hora Lord ──
        item {
            Text("⏰ Hora Lord Analysis", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(BrahmGold.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("☀", fontSize = 24.sp)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Hora Lord: $horaLord", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        if (horaDesc.isNotBlank()) {
                            Text(horaDesc, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        }
                    }
                }
            }
        }

        // ── Prashna Factors ──
        item {
            Text("📊 Prashna Factors", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🌅", fontSize = 14.sp)
                        Column {
                            Text("Prashna Lagna", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                            Text(lagna, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        }
                    }
                    HorizontalDivider(color = BrahmBorder)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🌙", fontSize = 14.sp)
                        Column {
                            Text("Moon Position", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                            Text(moonPos, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        }
                    }
                    if (!factors.isNullOrEmpty()) {
                        HorizontalDivider(color = BrahmBorder)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Key Factors", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                            factors.filter { it.isNotBlank() }.forEachIndexed { idx, factor ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${idx + 1}.", style = MaterialTheme.typography.bodySmall.copy(color = BrahmGold, fontWeight = FontWeight.Bold))
                                    Text(factor, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    ScrollToTopFab(listState, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp))
    } // Box
}

@Composable
fun PrashnaInputForm(
    question: String, questionType: String, pob: String,
    error: String?,
    onQuestionChange: (String) -> Unit,
    onQuestionTypeChange: (String) -> Unit,
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
                    Text("Ask Your Question", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    OutlinedTextField(
                        value = question, onValueChange = onQuestionChange,
                        label = { Text("Your Question") },
                        placeholder = { Text("Will I get the job? Should I travel now?") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold),
                    )
                    Text("Category", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(questionTypes) { type ->
                            FilterChip(
                                selected = questionType == type,
                                onClick = { onQuestionTypeChange(type) },
                                label = { Text(type, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrahmGold, selectedLabelColor = Color.White),
                            )
                        }
                    }
                    BirthInputFields(
                        dob = "", onDobChange = {},
                        tob = "", onTobChange = {},
                        pob = pob, onPobChange = onPobChange,
                        onCitySelected = onCitySelected,
                        showName = false,
                        cityVmKey = "prashna",
                    )
                    if (error != null) Text(error, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)))
                    BrahmButton(text = "Get Prashna Answer", onClick = onCalculate)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7))) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Text("🔮", fontSize = 24.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("What is Prashna?", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
                        Text("Prashna (Horary Astrology) answers specific questions using the exact moment the question is asked. No birth time needed — the cosmic positions at the moment of the question reveal the answer.", style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.8f)))
                    }
                }
            }
        }
    }
}
