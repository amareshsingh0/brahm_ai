package com.bimoraai.brahm.ui.rectification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val uncertaintyOptions = listOf("±30 Minutes", "±1 Hour", "±2 Hours", "±3 Hours")
private val eventTypes = listOf("Marriage", "Child Birth", "Career Change", "Accident", "Education", "Property", "Travel Abroad", "Death of Close One")

@Composable
fun RectificationContent(data: JsonObject) {
    val bestTime    = data["best_time"]?.jsonPrimitive?.contentOrNull ?: data["rectified_time"]?.jsonPrimitive?.contentOrNull ?: "—"
    val lagna       = data["lagna"]?.jsonPrimitive?.contentOrNull ?: "—"
    val confidence  = data["confidence"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
    val candidatesList = data["candidates"]?.let { el ->
        try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
    }
    val explanation = data["explanation"]?.jsonPrimitive?.contentOrNull ?: ""

    val confidenceColor = when {
        confidence >= 70 -> Color(0xFF43A047)
        confidence >= 50 -> Color(0xFFFF8F00)
        else             -> Color(0xFFE53935)
    }
    val verdictText = when {
        confidence >= 70 -> "Excellent"
        confidence >= 50 -> "Good"
        else             -> "Average"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Best Match Hero ──
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7)),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrahmGold.copy(alpha = 0.3f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🎯", fontSize = 32.sp)
                    Text("Rectified Birth Time", style = MaterialTheme.typography.labelMedium.copy(color = BrahmMutedForeground))
                    Text(bestTime, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
                    Text("Lagna: $lagna", style = MaterialTheme.typography.bodyMedium.copy(color = BrahmForeground))
                    LinearProgressIndicator(
                        progress = { (confidence / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = confidenceColor,
                        trackColor = BrahmBorder,
                    )
                    Text("$confidence% Confidence · $verdictText", style = MaterialTheme.typography.bodySmall.copy(color = confidenceColor, fontWeight = FontWeight.SemiBold))
                    if (explanation.isNotBlank()) {
                        HorizontalDivider(color = BrahmGold.copy(alpha = 0.2f))
                        Text(explanation, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.8f), fontSize = 12.sp))
                    }
                }
            }
        }

        // ── Candidate Times ──
        if (!candidatesList.isNullOrEmpty()) {
            item { Text("📋 Candidate Birth Times", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
            items(candidatesList) { candidate ->
                val time  = candidate["time"]?.jsonPrimitive?.contentOrNull ?: "—"
                val score = candidate["score"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                val lgt   = candidate["lagna"]?.jsonPrimitive?.contentOrNull ?: "—"
                val dasha = candidate["dasha"]?.jsonPrimitive?.contentOrNull ?: "—"
                val isBest= candidate["best"]?.jsonPrimitive?.contentOrNull == "true"
                val sColor= when {
                    score >= 70 -> Color(0xFF43A047)
                    score >= 50 -> Color(0xFFFF8F00)
                    else        -> Color(0xFFE53935)
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isBest) Color(0xFFFFF8E7) else BrahmCard),
                    border = if (isBest) androidx.compose.foundation.BorderStroke(1.dp, BrahmGold.copy(alpha = 0.4f)) else null,
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(time, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Lagna: $lgt · Dasha: $dasha", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        }
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(sColor.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("$score%", style = MaterialTheme.typography.titleSmall.copy(color = sColor, fontWeight = FontWeight.Bold))
                        }
                        if (isBest) Box(Modifier.clip(RoundedCornerShape(6.dp)).background(BrahmGold.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("Best", style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontSize = 10.sp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RectificationInputForm(
    name: String, dob: String, approxTob: String, pob: String,
    uncertainty: String,
    event1Type: String, event1Date: String,
    event2Type: String, event2Date: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onDobChange: (String) -> Unit,
    onApproxTobChange: (String) -> Unit,
    onPobChange: (String) -> Unit,
    onCitySelected: (City) -> Unit,
    onUncertaintyChange: (String) -> Unit,
    onEvent1TypeChange: (String) -> Unit,
    onEvent1DateChange: (String) -> Unit,
    onEvent2TypeChange: (String) -> Unit,
    onEvent2DateChange: (String) -> Unit,
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
                    Text("Birth Details", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    BirthInputFields(
                        name = name, onNameChange = onNameChange,
                        dob = dob, onDobChange = onDobChange,
                        tob = approxTob, onTobChange = onApproxTobChange,
                        pob = pob, onPobChange = onPobChange,
                        onCitySelected = onCitySelected,
                    )
                    Text("Time Uncertainty", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uncertaintyOptions.forEach { opt ->
                            FilterChip(selected = uncertainty == opt, onClick = { onUncertaintyChange(opt) }, label = { Text(opt, fontSize = 11.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrahmGold, selectedLabelColor = Color.White))
                        }
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Life Events (helps accuracy)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text("Event 1", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = event1Type, onValueChange = onEvent1TypeChange, label = { Text("Type") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                        OutlinedTextField(value = event1Date, onValueChange = onEvent1DateChange, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.weight(1.5f), shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                    }
                    Text("Event 2 (optional)", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = event2Type, onValueChange = onEvent2TypeChange, label = { Text("Type") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                        OutlinedTextField(value = event2Date, onValueChange = onEvent2DateChange, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.weight(1.5f), shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                    }
                    if (error != null) Text(error, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)))
                    BrahmButton(text = "Rectify Birth Time", onClick = onCalculate)
                }
            }
        }
    }
}
