package com.bimoraai.brahm.ui.muhurta

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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val activities = listOf("Wedding", "Travel", "Business", "Property", "Vehicle", "Education", "Medical", "Thread Ceremony", "Birth Ceremony")

private data class ChoghadiyaType(val color: Color, val nature: String)
private val choghadiyaTypes = mapOf(
    "Amrit" to ChoghadiyaType(Color(0xFF43A047), "Excellent"),
    "Shubh" to ChoghadiyaType(Color(0xFF7CB342), "Good"),
    "Labh"  to ChoghadiyaType(Color(0xFF1565C0), "Good"),
    "Char"  to ChoghadiyaType(Color(0xFF00ACC1), "Neutral"),
    "Rog"   to ChoghadiyaType(Color(0xFFE53935), "Avoid"),
    "Kaal"  to ChoghadiyaType(Color(0xFF37474F), "Avoid"),
    "Udveg" to ChoghadiyaType(Color(0xFFFF8F00), "Caution"),
)

@Composable
fun MuhurtaContent(data: JsonObject) {
    val muhurtaList = data["muhurtas"]?.let { el ->
        try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
    }
    val choghadiya = data["choghadiya"]?.let { el ->
        try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
    }
    val brahmaMuhurta  = data["brahma_muhurta"]?.jsonPrimitive?.contentOrNull
    val abhijitMuhurta = data["abhijit_muhurta"]?.jsonPrimitive?.contentOrNull

    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Special Muhurtas ──
        if (brahmaMuhurta != null || abhijitMuhurta != null) {
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("✨ Daily Sacred Timings", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
                        if (brahmaMuhurta != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("↑", fontSize = 16.sp)
                                Column {
                                    Text("Brahma Muhurta", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text(brahmaMuhurta, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                                }
                            }
                        }
                        if (abhijitMuhurta != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("☀", fontSize = 16.sp)
                                Column {
                                    Text("Abhijit Muhurta", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text(abhijitMuhurta, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Auspicious Muhurtas ──
        if (!muhurtaList.isNullOrEmpty()) {
            item {
                Text("🎯 Auspicious Muhurtas", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            items(muhurtaList) { muhurta ->
                val date      = muhurta["date"]?.jsonPrimitive?.contentOrNull ?: "—"
                val startTime = muhurta["start_time"]?.jsonPrimitive?.contentOrNull ?: "—"
                val endTime   = muhurta["end_time"]?.jsonPrimitive?.contentOrNull ?: "—"
                val score     = muhurta["score"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                val tithi     = muhurta["tithi"]?.jsonPrimitive?.contentOrNull ?: "—"
                val nakshatra = muhurta["nakshatra"]?.jsonPrimitive?.contentOrNull ?: "—"

                val scoreColor = when {
                    score >= 70 -> Color(0xFF43A047)
                    score >= 50 -> Color(0xFFFF8F00)
                    else        -> Color(0xFFE53935)
                }
                val verdictText = when {
                    score >= 70 -> "Excellent"
                    score >= 50 -> "Good"
                    else        -> "Average"
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BrahmCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, scoreColor.copy(alpha = 0.2f)),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(date, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Spacer(Modifier.weight(1f))
                            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(scoreColor.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("$score% · $verdictText", style = MaterialTheme.typography.labelSmall.copy(color = scoreColor, fontSize = 10.sp))
                            }
                        }
                        Text("$startTime — $endTime", style = MaterialTheme.typography.bodyMedium.copy(color = BrahmForeground))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Tithi: $tithi", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                            Text("Nakshatra: $nakshatra", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        }
                    }
                }
            }
        }

        // ── Choghadiya ──
        if (!choghadiya.isNullOrEmpty()) {
            item {
                Text("⏰ Today's Choghadiya", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            items(choghadiya) { slot ->
                val slotName  = slot["name"]?.jsonPrimitive?.contentOrNull ?: "—"
                val slotTime  = slot["time"]?.jsonPrimitive?.contentOrNull
                    ?: "${slot["start"]?.jsonPrimitive?.contentOrNull ?: "—"} – ${slot["end"]?.jsonPrimitive?.contentOrNull ?: "—"}"
                val slotType  = choghadiyaTypes[slotName] ?: ChoghadiyaType(BrahmGold, "Neutral")

                Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(slotType.color))
                        Text(slotName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                        Text(slotTime, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(slotType.color.copy(alpha = 0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(slotType.nature, style = MaterialTheme.typography.labelSmall.copy(color = slotType.color, fontSize = 10.sp))
                        }
                    }
                }
            }
        }

        if (muhurtaList.isNullOrEmpty() && choghadiya.isNullOrEmpty()) {
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                    Text("No muhurta data available for this period.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground))
                }
            }
        }
    }
    ScrollToTopFab(listState, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp))
    } // Box
}

@Composable
fun MuhurtaInputForm(
    activity: String, dob: String, tob: String, pob: String,
    fromDate: String, toDate: String,
    error: String?,
    onActivityChange: (String) -> Unit,
    onDobChange: (String) -> Unit,
    onTobChange: (String) -> Unit,
    onPobChange: (String) -> Unit,
    onCitySelected: (City) -> Unit,
    onFromDateChange: (String) -> Unit,
    onToDateChange: (String) -> Unit,
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
                    Text("Select Activity", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(activities) { act ->
                            FilterChip(
                                selected = activity == act,
                                onClick = { onActivityChange(act) },
                                label = { Text(act, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrahmGold, selectedLabelColor = Color.White),
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Date Range", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = fromDate, onValueChange = onFromDateChange, label = { Text("From (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                        OutlinedTextField(value = toDate, onValueChange = onToDateChange, label = { Text("To (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Birth Details (Optional)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    BirthInputFields(
                        dob = dob, onDobChange = onDobChange,
                        tob = tob, onTobChange = onTobChange,
                        pob = pob, onPobChange = onPobChange,
                        onCitySelected = onCitySelected,
                        showName = false,
                    )
                    if (error != null) Text(error, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)))
                    BrahmButton(text = "Find Muhurtas", onClick = onCalculate)
                }
            }
        }
    }
}
