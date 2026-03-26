package com.bimoraai.brahm.ui.dosha

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

private data class DoshaSection(
    val name: String,
    val emoji: String,
    val presentKey: String,
    val severityKey: String,
    val descKey: String,
    val remediesKey: String,
    val color: Color,
)

private val doshaSections = listOf(
    DoshaSection("Mangal Dosha",   "♂", "mangal_dosha",  "mangal_severity",  "mangal_description",  "mangal_remedies",  Color(0xFFE53935)),
    DoshaSection("Kaal Sarp Dosha","☊", "kaal_sarp",     "kaal_sarp_type",   "kaal_sarp_description","kaal_sarp_remedies",Color(0xFF4527A0)),
    DoshaSection("Pitra Dosha",    "🌙","pitra_dosha",   "pitra_severity",   "pitra_description",   "pitra_remedies",   Color(0xFF1565C0)),
    DoshaSection("Grahan Yoga",    "☀", "grahan_yoga",   "grahan_type",      "grahan_description",  "grahan_remedies",  Color(0xFF37474F)),
)

@Composable
fun DoshaContent(data: JsonObject) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("🔮 Dosha Analysis", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        doshaSections.forEach { section ->
            val isPresent = data[section.presentKey]?.jsonPrimitive?.contentOrNull.let {
                it == "true" || it == "1" || it == "yes"
            }
            val severity  = data[section.severityKey]?.jsonPrimitive?.contentOrNull ?: ""
            val desc      = data[section.descKey]?.jsonPrimitive?.contentOrNull ?: ""
            val remedies  = data[section.remediesKey]?.let { el ->
                try { el.jsonArray.map { it.jsonPrimitive.contentOrNull ?: "" } } catch (_: Exception) { null }
            }

            item {
                DoshaCard(section, isPresent, severity, desc, remedies)
            }
        }
    }
}

@Composable
private fun DoshaCard(
    section: DoshaSection,
    isPresent: Boolean,
    severity: String,
    desc: String,
    remedies: List<String>?,
) {
    var expanded by remember { mutableStateOf(isPresent) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
        border = if (isPresent) androidx.compose.foundation.BorderStroke(1.dp, section.color.copy(alpha = 0.3f)) else null,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                        .background(section.color.copy(alpha = if (isPresent) 0.15f else 0.06f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(section.emoji, fontSize = 20.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(section.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    if (severity.isNotBlank()) {
                        Text(severity, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    }
                }
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(if (isPresent) section.color.copy(alpha = 0.12f) else Color(0xFF43A047).copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isPresent) "Present" else "Absent",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isPresent) section.color else Color(0xFF43A047),
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            if (isPresent) {
                if (desc.isNotBlank()) {
                    Text(desc, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.85f), fontSize = 12.sp))
                }

                if (!remedies.isNullOrEmpty()) {
                    HorizontalDivider(color = BrahmBorder)
                    Text("Remedies", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        remedies.filter { it.isNotBlank() }.forEachIndexed { idx, remedy ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${idx + 1}.", style = MaterialTheme.typography.bodySmall.copy(color = section.color, fontWeight = FontWeight.Bold))
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
fun DoshaInputForm(
    name: String, dob: String, tob: String, pob: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onDobChange: (String) -> Unit,
    onTobChange: (String) -> Unit,
    onPobChange: (String) -> Unit,
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
                    OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                    OutlinedTextField(value = dob, onValueChange = onDobChange, label = { Text("Date of Birth (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                    OutlinedTextField(value = tob, onValueChange = onTobChange, label = { Text("Time of Birth (HH:MM)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                    OutlinedTextField(value = pob, onValueChange = onPobChange, label = { Text("Place of Birth") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold))
                    if (error != null) Text(error, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)))
                    BrahmButton(text = "Check Doshas", onClick = onCalculate)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Doshas Checked", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
                    listOf("♂ Mangal Dosha — Mars in 2/4/7/8/12 houses", "☊ Kaal Sarp Dosha — All planets between Rahu-Ketu", "🌙 Pitra Dosha — 9th house lord affliction", "☀ Grahan Yoga — Paapakartari on Lagna/Moon").forEach { text ->
                        Text(text, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.8f)))
                    }
                }
            }
        }
    }
}
