package com.bimoraai.brahm.ui.gochar

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

private data class GocharPlanetMeta(val symbol: String, val color: Color)
private val gocharPlanetMeta = mapOf(
    "Sun"     to GocharPlanetMeta("☀", Color(0xFFFF8F00)),
    "Moon"    to GocharPlanetMeta("🌙", Color(0xFF1565C0)),
    "Mars"    to GocharPlanetMeta("♂", Color(0xFFE53935)),
    "Mercury" to GocharPlanetMeta("☿", Color(0xFF2E7D32)),
    "Jupiter" to GocharPlanetMeta("♃", Color(0xFFFFB300)),
    "Venus"   to GocharPlanetMeta("♀", Color(0xFFE91E63)),
    "Saturn"  to GocharPlanetMeta("♄", Color(0xFF37474F)),
    "Rahu"    to GocharPlanetMeta("☊", Color(0xFF4527A0)),
    "Ketu"    to GocharPlanetMeta("☋", Color(0xFF546E7A)),
)

@Composable
fun GocharContent(gocharData: JsonObject?, analyzeData: JsonObject?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── Section 1: Current Sky ──
        item {
            Text(
                "🪐 Current Sky",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }

        val planetList = gocharData?.get("planets")?.let { el ->
            try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
        } ?: gocharData?.get("transits")?.let { el ->
            try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
        }

        if (planetList.isNullOrEmpty()) {
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                    Text(
                        "Planet data not available.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground),
                    )
                }
            }
        } else {
            items(planetList) { planet ->
                val name      = planet["name"]?.jsonPrimitive?.contentOrNull ?: "—"
                val rashi     = planet["rashi"]?.jsonPrimitive?.contentOrNull
                    ?: planet["current_rashi"]?.jsonPrimitive?.contentOrNull ?: "—"
                val degree    = planet["degree"]?.jsonPrimitive?.contentOrNull ?: "—"
                val nakshatra = planet["nakshatra"]?.jsonPrimitive?.contentOrNull ?: "—"
                val retro     = planet["retrograde"]?.jsonPrimitive?.contentOrNull == "true"
                    || planet["is_retrograde"]?.jsonPrimitive?.contentOrNull == "true"
                GocharPlanetCard(name, rashi, degree, nakshatra, retro)
            }
        }

        // ── Section 2: Personal Analysis ──
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                "📊 Personal Transit Analysis",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }

        val analysisList = analyzeData?.get("analysis")?.let { el ->
            try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
        } ?: analyzeData?.get("transits")?.let { el ->
            try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
        }

        if (analysisList.isNullOrEmpty()) {
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                    Text(
                        "Personal analysis not available.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground),
                    )
                }
            }
        } else {
            items(analysisList) { item ->
                TransitAnalysisCard(item)
            }
        }

        // ── Section 3: Opportunities & Cautions ──
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                "⚡ Opportunities & Cautions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }

        val opportunities = analyzeData?.get("opportunities")?.let { el ->
            try { el.jsonArray.map { it.jsonPrimitive.contentOrNull ?: "" } } catch (_: Exception) { null }
        }
        val cautions = analyzeData?.get("cautions")?.let { el ->
            try { el.jsonArray.map { it.jsonPrimitive.contentOrNull ?: "" } } catch (_: Exception) { null }
        }
        val specialEvents = listOfNotNull(
            analyzeData?.get("sade_sati")?.jsonPrimitive?.contentOrNull,
            analyzeData?.get("ashtama_shani")?.jsonPrimitive?.contentOrNull,
            analyzeData?.get("kantaka_shani")?.jsonPrimitive?.contentOrNull,
        ).filter { it.isNotBlank() }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrahmCard),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (specialEvents.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Saturn Events", style = MaterialTheme.typography.labelMedium.copy(color = BrahmMutedForeground))
                            specialEvents.forEach { event ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("♄", fontSize = 14.sp, color = Color(0xFF37474F))
                                    Text(event, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                                }
                            }
                        }
                        HorizontalDivider(color = BrahmBorder)
                    }

                    if (!opportunities.isNullOrEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Opportunities", style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF2E7D32)))
                            opportunities.filter { it.isNotBlank() }.forEach { opp ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("✓", fontSize = 13.sp, color = Color(0xFF43A047))
                                    Text(opp, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                                }
                            }
                        }
                        HorizontalDivider(color = BrahmBorder)
                    }

                    if (!cautions.isNullOrEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Cautions", style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFFE53935)))
                            cautions.filter { it.isNotBlank() }.forEach { caution ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("⚠", fontSize = 13.sp, color = Color(0xFFFF8F00))
                                    Text(caution, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                                }
                            }
                        }
                    }

                    if (specialEvents.isEmpty() && opportunities.isNullOrEmpty() && cautions.isNullOrEmpty()) {
                        Text(
                            "No special transit events detected at this time.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GocharPlanetCard(name: String, rashi: String, degree: String, nakshatra: String, retrograde: Boolean) {
    val meta = gocharPlanetMeta[name] ?: GocharPlanetMeta("★", BrahmGold)
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(meta.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(meta.symbol, fontSize = 22.sp)
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    if (retrograde) {
                        Box(
                            Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFE53935).copy(alpha = 0.12f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("℞", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE53935), fontSize = 10.sp))
                        }
                    }
                }
                Text("$rashi · $degree°", style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                Text(nakshatra, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
            }
        }
    }
}

@Composable
private fun TransitAnalysisCard(item: JsonObject) {
    val planet = item["planet"]?.jsonPrimitive?.contentOrNull ?: "—"
    val house  = item["natal_house"]?.jsonPrimitive?.contentOrNull
        ?: item["house"]?.jsonPrimitive?.contentOrNull ?: "—"
    val effect = item["effect"]?.jsonPrimitive?.contentOrNull ?: "—"
    val desc   = item["description"]?.jsonPrimitive?.contentOrNull ?: ""
    val meta   = gocharPlanetMeta[planet] ?: GocharPlanetMeta("★", BrahmGold)

    val effectColor = when {
        effect.contains("favorable", ignoreCase = true) || effect.contains("benefic", ignoreCase = true) -> Color(0xFF43A047)
        effect.contains("unfavorable", ignoreCase = true) || effect.contains("malefic", ignoreCase = true) -> Color(0xFFE53935)
        else -> Color(0xFFFF8F00)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, effectColor.copy(alpha = 0.2f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(meta.symbol, fontSize = 16.sp)
                Text(
                    "$planet in House $house",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp)).background(effectColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(effect, style = MaterialTheme.typography.labelSmall.copy(color = effectColor, fontSize = 10.sp))
                }
            }
            if (desc.isNotBlank()) {
                Text(desc, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.8f), fontSize = 12.sp))
            }
        }
    }
}

@Composable
fun GocharInputForm(
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
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrahmCard),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter Birth Details",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    BirthInputFields(
                        name = name, onNameChange = onNameChange,
                        dob = dob, onDobChange = onDobChange,
                        tob = tob, onTobChange = onTobChange,
                        pob = pob, onPobChange = onPobChange,
                        onCitySelected = onCitySelected,
                    )
                    if (error != null) {
                        Text(error, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)))
                    }
                    BrahmButton(text = "Analyze Transits", onClick = onCalculate)
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7)),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text("🪐", fontSize = 24.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "What is Gochar?",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrahmGold),
                        )
                        Text(
                            "Gochar (planetary transit) shows how current planetary positions affect your natal chart. It reveals timing of events in career, health, relationships, and spiritual growth based on transiting planets relative to your birth chart.",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.8f)),
                        )
                    }
                }
            }
        }
    }
}
