package com.bimoraai.brahm.ui.sadesati

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
import androidx.compose.ui.graphics.Brush
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

@Composable
fun SadeSatiContent(data: JsonObject) {
    val phase       = data["phase"]?.jsonPrimitive?.contentOrNull ?: data["current_phase"]?.jsonPrimitive?.contentOrNull ?: "—"
    val startDate   = data["start_date"]?.jsonPrimitive?.contentOrNull ?: "—"
    val endDate     = data["end_date"]?.jsonPrimitive?.contentOrNull ?: "—"
    val isActive    = data["is_active"]?.jsonPrimitive?.contentOrNull == "true"
        || data["active"]?.jsonPrimitive?.contentOrNull == "true"
    val saturnRashi = data["saturn_rashi"]?.jsonPrimitive?.contentOrNull ?: "—"
    val moonRashi   = data["moon_rashi"]?.jsonPrimitive?.contentOrNull ?: "—"
    val description = data["description"]?.jsonPrimitive?.contentOrNull ?: ""

    val phaseColor = when {
        phase.contains("Peak", ignoreCase = true) -> Color(0xFFE53935)
        phase.contains("Rising", ignoreCase = true) -> Color(0xFFFF8F00)
        phase.contains("Setting", ignoreCase = true) -> Color(0xFF43A047)
        else -> BrahmGold
    }

    val ashtamaShani  = data["ashtama_shani"]?.jsonPrimitive?.contentOrNull
    val kantakaShani  = data["kantaka_shani"]?.jsonPrimitive?.contentOrNull
    val remediesList  = data["remedies"]?.let { el ->
        try { el.jsonArray.map { it.jsonPrimitive.contentOrNull ?: "" } } catch (_: Exception) { null }
    }
    val periods = data["periods"]?.let { el ->
        try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Phase Indicator Hero ──
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrahmCard),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(
                        Brush.verticalGradient(listOf(phaseColor.copy(alpha = 0.08f), Color.Transparent))
                    ),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("♄", fontSize = 40.sp)
                        Text(
                            if (isActive) "Sade Sati — Active" else "Sade Sati — Not Active",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Box(
                            Modifier.clip(RoundedCornerShape(20.dp)).background(phaseColor.copy(alpha = 0.12f))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                if (phase == "—") "Status Unknown" else "$phase Phase",
                                style = MaterialTheme.typography.titleSmall.copy(color = phaseColor, fontWeight = FontWeight.Bold),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Saturn", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                                Text(saturnRashi, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                            VerticalDivider(modifier = Modifier.height(36.dp), color = BrahmBorder)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Moon Rashi", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                                Text(moonRashi, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                            VerticalDivider(modifier = Modifier.height(36.dp), color = BrahmBorder)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Period", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                                Text(
                                    if (startDate != "—") "$startDate →" else "—",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                )
                            }
                        }
                        if (description.isNotBlank()) {
                            HorizontalDivider(color = BrahmBorder)
                            Text(description, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.8f), fontSize = 12.sp))
                        }
                    }
                }
            }
        }

        // ── Ashtama & Kantaka Shani ──
        if (ashtamaShani != null || kantakaShani != null) {
            item {
                Text("⚠️ Special Saturn Effects", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (ashtamaShani != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE53935).copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                    Text("8", fontSize = 16.sp, color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Ashtama Shani", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text("Saturn in 8th from Moon — $ashtamaShani", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                                }
                            }
                        }
                        if (kantakaShani != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFFF8F00).copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                    Text("♄", fontSize = 16.sp, color = Color(0xFFFF8F00))
                                }
                                Column {
                                    Text("Kantaka Shani", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text(kantakaShani, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Sade Sati Timeline ──
        if (!periods.isNullOrEmpty()) {
            item {
                Text("📅 Sade Sati Timeline", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            items(periods) { period ->
                val pStart  = period["start"]?.jsonPrimitive?.contentOrNull ?: "—"
                val pEnd    = period["end"]?.jsonPrimitive?.contentOrNull ?: "—"
                val pPhase  = period["phase"]?.jsonPrimitive?.contentOrNull ?: "Past"
                val current = period["current"]?.jsonPrimitive?.contentOrNull == "true"
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (current) Color(0xFFFFF8E7) else BrahmCard),
                    border = if (current) androidx.compose.foundation.BorderStroke(1.dp, BrahmGold.copy(alpha = 0.4f)) else null,
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (current) "🔴" else "⚫", fontSize = 16.sp)
                        Column(Modifier.weight(1f)) {
                            Text(pPhase, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            Text("$pStart — $pEnd", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        }
                        if (current) {
                            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(BrahmGold.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("Current", style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontSize = 10.sp))
                            }
                        }
                    }
                }
            }
        }

        // ── Remedies ──
        if (!remediesList.isNullOrEmpty()) {
            item {
                Text("🙏 Remedies", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        remediesList.filter { it.isNotBlank() }.forEachIndexed { idx, remedy ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${idx + 1}.", style = MaterialTheme.typography.bodySmall.copy(color = BrahmGold, fontWeight = FontWeight.Bold))
                                Text(remedy, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SadeSatiInputForm(
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
                    if (error != null) Text(error, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)))
                    BrahmButton(text = "Check Sade Sati", onClick = onCalculate)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7))) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Text("♄", fontSize = 24.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("What is Sade Sati?", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
                        Text(
                            "Sade Sati is a 7½-year period when Saturn transits through the sign before, of, and after your Moon sign (Janma Rashi). It passes through 3 rashis — Rising (before Moon), Peak (on Moon), and Setting (after Moon). Each phase lasts ~2.5 years.",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.8f)),
                        )
                    }
                }
            }
        }
    }
}
