package com.bimoraai.brahm.ui.varshphal

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

@Composable
fun VarshpalContent(data: JsonObject) {
    val targetYear     = data["target_year"]?.jsonPrimitive?.contentOrNull ?: "—"
    val solarReturnDate= data["solar_return_date"]?.jsonPrimitive?.contentOrNull ?: "—"
    val solarReturnTime= data["solar_return_time"]?.jsonPrimitive?.contentOrNull ?: "—"
    val munthaPlanet   = data["muntha_planet"]?.jsonPrimitive?.contentOrNull ?: "—"
    val munthaRashi    = data["muntha_rashi"]?.jsonPrimitive?.contentOrNull ?: "—"
    val yearLord       = data["year_lord"]?.jsonPrimitive?.contentOrNull ?: "—"
    val summary        = data["summary"]?.jsonPrimitive?.contentOrNull ?: ""
    val themes         = data["themes"]?.let { el ->
        try { el.jsonArray.map { it.jsonPrimitive.contentOrNull ?: "" } } catch (_: Exception) { null }
    }
    val planets        = data["planets"]?.let { el ->
        try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
    }
    val lifeAreas      = data["life_areas"]?.let { el ->
        try { el.jsonObject } catch (_: Exception) { null }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Solar Return Info ──
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7)),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("☀ Solar Return $targetYear", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Date", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                            Text(solarReturnDate, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        VerticalDivider(modifier = Modifier.height(36.dp), color = BrahmBorder)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Time", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                            Text(solarReturnTime, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        VerticalDivider(modifier = Modifier.height(36.dp), color = BrahmBorder)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Year Lord", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                            Text(yearLord, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    if (summary.isNotBlank()) {
                        HorizontalDivider(color = BrahmGold.copy(alpha = 0.2f))
                        Text(summary, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.85f)))
                    }
                }
            }
        }

        // ── Muntha ──
        item {
            Text("🌟 Year Themes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(BrahmGold.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Text("🎯", fontSize = 20.sp)
                        }
                        Column {
                            Text("Muntha: $munthaPlanet in $munthaRashi", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            Text("Key area of focus this year", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        }
                    }
                    if (!themes.isNullOrEmpty()) {
                        HorizontalDivider(color = BrahmBorder)
                        themes.filter { it.isNotBlank() }.forEachIndexed { idx, theme ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${idx + 1}.", style = MaterialTheme.typography.bodySmall.copy(color = BrahmGold, fontWeight = FontWeight.Bold))
                                Text(theme, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // ── Planet positions ──
        if (!planets.isNullOrEmpty()) {
            item { Text("🪐 Varshphal Planets", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                    Column(Modifier.padding(4.dp)) {
                        Row(Modifier.fillMaxWidth().background(BrahmGold.copy(alpha = 0.08f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("Planet",    style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.2f))
                            Text("Rashi",     style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.2f))
                            Text("House",     style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.Bold), modifier = Modifier.weight(0.8f))
                            Text("Degree",    style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider(color = BrahmBorder)
                        planets.forEachIndexed { idx, planet ->
                            if (idx > 0) HorizontalDivider(color = BrahmBorder.copy(alpha = 0.5f))
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(planet["planet"]?.jsonPrimitive?.contentOrNull ?: planet["name"]?.jsonPrimitive?.contentOrNull ?: "—", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1.2f))
                                Text(planet["rashi"]?.jsonPrimitive?.contentOrNull ?: "—", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.2f))
                                Text(planet["house"]?.jsonPrimitive?.contentOrNull ?: "—", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.8f))
                                Text("${planet["degree"]?.jsonPrimitive?.contentOrNull ?: "—"}°", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground), modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VarshpalInputForm(
    name: String, dob: String, tob: String, pob: String, targetYear: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onDobChange: (String) -> Unit,
    onTobChange: (String) -> Unit,
    onPobChange: (String) -> Unit,
    onCitySelected: (City) -> Unit,
    onTargetYearChange: (String) -> Unit,
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
                        tob = tob, onTobChange = onTobChange,
                        pob = pob, onPobChange = onPobChange,
                        onCitySelected = onCitySelected,
                    )
                    OutlinedTextField(value = targetYear, onValueChange = onTargetYearChange, label = { Text("Target Year (e.g. 2025)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                    if (error != null) Text(error, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)))
                    BrahmButton(text = "Calculate Varshphal", onClick = onCalculate)
                }
            }
        }
    }
}
