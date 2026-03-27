package com.bimoraai.brahm.ui.kp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private val kpTabs = listOf("Planets", "Cusps", "Significators")

@Composable
fun KPContent(data: JsonObject) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val planets      = data["planets"]?.let { el -> try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null } }
    val cusps        = data["cusps"]?.let { el -> try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null } }
    val significators= data["significators"]?.let { el -> try { el.jsonObject } catch (_: Exception) { null } }

    Column(Modifier.fillMaxSize().background(BrahmBackground)) {
        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = BrahmGold,
            edgePadding = 16.dp,
        ) {
            kpTabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label, fontSize = 13.sp) },
                )
            }
        }

        val listState = rememberLazyListState()
        Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (selectedTab) {
                0 -> {
                    // Planets Table
                    item {
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                            Column(Modifier.padding(4.dp)) {
                                KPTableHeader(listOf("Planet", "Star Lord", "Sub Lord", "Sub-Sub"))
                                HorizontalDivider(color = BrahmBorder)
                                if (planets.isNullOrEmpty()) {
                                    Text("No planet data", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground))
                                } else {
                                    planets.forEachIndexed { idx, planet ->
                                        if (idx > 0) HorizontalDivider(color = BrahmBorder.copy(alpha = 0.5f))
                                        KPTableRow(listOf(
                                            planet["planet"]?.jsonPrimitive?.contentOrNull ?: planet["name"]?.jsonPrimitive?.contentOrNull ?: "—",
                                            planet["star_lord"]?.jsonPrimitive?.contentOrNull ?: "—",
                                            planet["sub_lord"]?.jsonPrimitive?.contentOrNull ?: "—",
                                            planet["sub_sub_lord"]?.jsonPrimitive?.contentOrNull ?: "—",
                                        ), idx == 0)
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Cusps Table
                    item {
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                            Column(Modifier.padding(4.dp)) {
                                KPTableHeader(listOf("House", "Star Lord", "Sub Lord"))
                                HorizontalDivider(color = BrahmBorder)
                                if (cusps.isNullOrEmpty()) {
                                    Text("No cusp data", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground))
                                } else {
                                    cusps.forEachIndexed { idx, cusp ->
                                        if (idx > 0) HorizontalDivider(color = BrahmBorder.copy(alpha = 0.5f))
                                        KPTableRow(listOf(
                                            cusp["house"]?.jsonPrimitive?.contentOrNull ?: "${idx + 1}",
                                            cusp["star_lord"]?.jsonPrimitive?.contentOrNull ?: "—",
                                            cusp["sub_lord"]?.jsonPrimitive?.contentOrNull ?: "—",
                                        ), false)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Significators
                    if (significators == null) {
                        item {
                            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                                Text("No significator data", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground))
                            }
                        }
                    } else {
                        items(significators.entries.toList()) { (house, planets) ->
                            val planetNames = try {
                                planets.jsonArray.map { it.jsonPrimitive.contentOrNull ?: "" }.filter { it.isNotBlank() }.joinToString(", ")
                            } catch (_: Exception) { planets.jsonPrimitive.contentOrNull ?: "—" }

                            Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(36.dp).background(BrahmGold.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(house.removePrefix("house_").removePrefix("H"), style = MaterialTheme.typography.titleSmall.copy(color = BrahmGold, fontWeight = FontWeight.Bold))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text("House $house", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                        Text(planetNames.ifBlank { "None" }, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
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
}

@Composable
private fun KPTableHeader(columns: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth().background(BrahmGold.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        columns.forEach { col ->
            Text(col, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun KPTableRow(values: List<String>, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(if (highlight) BrahmGold.copy(alpha = 0.04f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        values.forEachIndexed { idx, value ->
            Text(
                value,
                style = if (idx == 0) MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                else MaterialTheme.typography.bodySmall.copy(color = BrahmForeground),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun KPInputForm(
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
                    BrahmButton(text = "Generate KP Chart", onClick = onCalculate)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7))) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Text("🔮", fontSize = 24.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("KP System", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
                        Text("Krishnamurti Paddhati uses Star Lords and Sub Lords to give precise event timing predictions. It shows planets' Star Lord, Sub Lord, and Sub-Sub Lord for deep event analysis.", style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.8f)))
                    }
                }
            }
        }
    }
}
