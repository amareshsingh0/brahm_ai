package com.bimoraai.brahm.ui.varshphal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray

private data class GrahaMeta(val symbol: String, val color: Color)

private val GRAHA_META = mapOf(
    "Surya"  to GrahaMeta("☉", Color(0xFFD97706)),
    "Chandra" to GrahaMeta("☽", Color(0xFF4F46E5)),
    "Mangal" to GrahaMeta("♂", Color(0xFFDC2626)),
    "Budh"   to GrahaMeta("☿", Color(0xFF16A34A)),
    "Guru"   to GrahaMeta("♃", Color(0xFFB45309)),
    "Shukra" to GrahaMeta("♀", Color(0xFF9333EA)),
    "Shani"  to GrahaMeta("♄", Color(0xFF334155)),
    "Rahu"   to GrahaMeta("☊", Color(0xFF0369A1)),
    "Ketu"   to GrahaMeta("☋", Color(0xFFC2410C)),
)
private val ORDER = listOf("Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani","Rahu","Ketu")

private fun JsonObject.str(key: String) = this[key]?.jsonPrimitive?.contentOrNull ?: ""
private fun JsonObject.obj(key: String) = try { this[key]?.jsonObject } catch (_: Exception) { null }

private val RASHI_SYMBOLS = mapOf(
    "Mesha" to "♈\uFE0E", "Vrishabha" to "♉\uFE0E", "Mithuna" to "♊\uFE0E", "Karka" to "♋\uFE0E",
    "Simha" to "♌\uFE0E", "Kanya" to "♍\uFE0E", "Tula" to "♎\uFE0E", "Vrischika" to "♏\uFE0E",
    "Dhanu" to "♐\uFE0E", "Makara" to "♑\uFE0E", "Kumbha" to "♒\uFE0E", "Meena" to "♓\uFE0E",
)

@Composable
fun VarshpalContent(data: JsonObject) {
    val varshphalYear       = data["varshphal_year"]?.jsonPrimitive?.intOrNull ?: 0
    val solarReturnDatetime = data.str("solar_return_datetime")
    val natalSunLon         = data["natal_sun_longitude"]?.jsonPrimitive?.doubleOrNull
    val lagnaRashi          = data.obj("lagna")?.str("rashi") ?: ""
    val yearThemes          = try { data["year_themes"]?.jsonArray?.map { it.jsonPrimitive.contentOrNull ?: "" } } catch (_: Exception) { null }
    val grahas              = try { data.obj("grahas") } catch (_: Exception) { null }
    val yogas               = try { data["yogas"]?.jsonArray?.map { it.jsonObject } } catch (_: Exception) { null }

    var showGrahas by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // ── Header card ──
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7)),
                modifier = Modifier.fillMaxWidth().border(1.dp, BrahmGold.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("☀", fontSize = 18.sp, color = BrahmGold)
                        Text(
                            "Varshphal $varshphalYear",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BrahmGold)
                        )
                    }
                    if (solarReturnDatetime.isNotBlank()) {
                        Text(
                            "Solar Return: $solarReturnDatetime",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (natalSunLon != null) {
                            Text(
                                "Natal Sun: ${"%.2f".format(natalSunLon)}°",
                                style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground)
                            )
                        }
                        if (lagnaRashi.isNotBlank()) {
                            Text("·", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    RASHI_SYMBOLS[lagnaRashi] ?: "",
                                    fontSize = 16.sp,
                                    color = BrahmGold,
                                    fontWeight = FontWeight.Light,
                                )
                                Text(
                                    "Varshphal Lagna: $lagnaRashi",
                                    style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Year Themes ──
        if (!yearThemes.isNullOrEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrahmCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BrahmGold.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Year Themes $varshphalYear",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        yearThemes.filter { it.isNotBlank() }.forEach { theme ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("✦", style = MaterialTheme.typography.bodySmall.copy(color = BrahmGold), modifier = Modifier.padding(top = 1.dp))
                                Text(theme, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                            }
                        }
                    }
                }
            }
        }

        // ── Planet Positions (collapsible) ──
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrahmCard),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    // Toggle header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (showGrahas) "Hide Grahas" else "Show Grahas",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground)
                        )
                        IconButton(onClick = { showGrahas = !showGrahas }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                if (showGrahas) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = BrahmMutedForeground,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    if (showGrahas && grahas != null) {
                        Spacer(Modifier.height(8.dp))
                        // 3-column grid
                        ORDER.chunked(3).forEach { rowPlanets ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowPlanets.forEach { planet ->
                                    val g = grahas.obj(planet)
                                    val meta = GRAHA_META[planet]
                                    Box(Modifier.weight(1f)) {
                                        if (g != null && meta != null) {
                                            Card(
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(containerColor = BrahmBackground),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                Column(
                                                    Modifier.fillMaxWidth().padding(8.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                                ) {
                                                    Text(meta.symbol, fontSize = 20.sp, color = meta.color)
                                                    Text(planet, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground), fontSize = 9.sp)
                                                    Text(
                                                        RASHI_SYMBOLS[g.str("rashi")] ?: "",
                                                        fontSize = 16.sp,
                                                        color = BrahmGold,
                                                        fontWeight = FontWeight.Light,
                                                    )
                                                    Text(g.str("rashi"), style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontWeight = FontWeight.SemiBold), fontSize = 9.sp)
                                                    val degree = g["degree"]?.jsonPrimitive?.doubleOrNull
                                                    val house  = g["house"]?.jsonPrimitive?.intOrNull
                                                    Text(
                                                        "${if (degree != null) "%.1f".format(degree) + "°" else ""} ${if (house != null) "H$house" else ""}".trim(),
                                                        style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
                                                        fontSize = 9.sp,
                                                    )
                                                    val retro = g["retro"]?.jsonPrimitive?.booleanOrNull ?: false
                                                    if (retro) Text("℞", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFF59E0B)), fontSize = 9.sp)
                                                }
                                            }
                                        } else {
                                            // empty slot for alignment
                                            Spacer(Modifier.fillMaxWidth())
                                        }
                                    }
                                }
                                // fill remaining slots in last row
                                repeat(3 - rowPlanets.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // ── Active Yogas ──
        val activeYogas = yogas?.filter { y ->
            val p = y["present"]?.jsonPrimitive
            p?.booleanOrNull != false && p?.contentOrNull != "false" && p?.contentOrNull != "0"
        }
        if (!activeYogas.isNullOrEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrahmCard),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Active Yogas this Year",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        activeYogas.forEach { y ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("✓", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF10B981), fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 1.dp))
                                Column {
                                    Text(y.str("name"), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                    val desc = y.str("desc").ifBlank { y.str("description") }
                                    if (desc.isNotBlank()) {
                                        Text(desc, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    } // LazyColumn
    } // Box
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
    onYearDecrement: () -> Unit,
    onYearIncrement: () -> Unit,
    onCalculate: () -> Unit,
) {
    val currentYear = java.time.LocalDate.now().year
    val displayYear = targetYear.toIntOrNull() ?: currentYear
    val birthYear   = dob.take(4).toIntOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 12.dp),
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

                    // Year selector: − / YEAR / +
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Target Year", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onYearDecrement,
                                modifier = Modifier.size(36.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrahmMutedForeground),
                            ) {
                                Text("−", fontSize = 18.sp)
                            }
                            Text(
                                displayYear.toString(),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.width(56.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            OutlinedButton(
                                onClick = onYearIncrement,
                                modifier = Modifier.size(36.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrahmMutedForeground),
                            ) {
                                Text("+", fontSize = 18.sp)
                            }
                            if (birthYear != null) {
                                Text(
                                    "Age ${displayYear - birthYear}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
                                )
                            }
                        }
                    }

                    if (error != null) Text(error, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)))
                    BrahmButton(text = "Calculate Varshphal $displayYear", onClick = onCalculate)
                }
            }
        }
    }
}
