package com.bimoraai.brahm.ui.yogas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.BirthInputFields
import com.bimoraai.brahm.core.components.BrahmButton
import com.bimoraai.brahm.core.components.BrahmLoadingSpinner
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun YogasScreen(
    navController: NavController,
    vm: YogasViewModel = hiltViewModel(),
) {
    val yogas     by vm.yogas.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error     by vm.error.collectAsState()
    val hasData   by vm.hasData.collectAsState()
    val name      by vm.name.collectAsState()
    val dob       by vm.dob.collectAsState()
    val tob       by vm.tob.collectAsState()
    val pob       by vm.pob.collectAsState()

    SwipeBackLayout(navController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yogas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrahmBackground),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(BrahmBackground).padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Input form
            if (!hasData) {
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Enter Birth Details", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            BirthInputFields(
                                name = name, onNameChange = { vm.name.value = it },
                                dob = dob, onDobChange = { vm.dob.value = it },
                                tob = tob, onTobChange = { vm.tob.value = it },
                                pob = pob, onPobChange = { vm.pob.value = it },
                                onCitySelected = { city -> vm.pob.value = city.name; vm.lat.value = city.lat; vm.lon.value = city.lon; vm.tz.value = city.tz.toString() },
                            )
                            if (error != null) {
                                Text(error!!, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)))
                            }
                            BrahmButton(text = "Calculate Yogas", onClick = { vm.calculate() })
                        }
                    }
                }
            }

            if (isLoading) {
                item { BrahmLoadingSpinner(modifier = Modifier.fillMaxWidth().height(120.dp)) }
            }

            if (hasData && yogas != null && !isLoading) {
                val yogaList = yogas!!.get("yogas")?.let { el ->
                    try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
                }

                val present = yogaList?.filter {
                    it["present"]?.jsonPrimitive?.contentOrNull == "true" || it["present"]?.jsonPrimitive?.contentOrNull == "1"
                } ?: emptyList()
                val absent = yogaList?.filter {
                    it["present"]?.jsonPrimitive?.contentOrNull != "true" && it["present"]?.jsonPrimitive?.contentOrNull != "1"
                } ?: emptyList()

                if (yogaList.isNullOrEmpty()) {
                    item {
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                            Text("No yoga data returned.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    item {
                        Text(
                            "✨ ${present.size} Yogas Present · ${absent.size} Absent",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    items(present) { yoga -> YogaCard(yoga, isPresent = true) }
                    if (absent.isNotEmpty()) {
                        item {
                            Text("Absent Yogas", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = BrahmMutedForeground))
                        }
                        items(absent) { yoga -> YogaCard(yoga, isPresent = false) }
                    }
                }
            }
        }
    }
    } // SwipeBackLayout
}

@Composable
private fun YogaCard(yoga: JsonObject, isPresent: Boolean) {
    val name  = yoga["name"]?.jsonPrimitive?.contentOrNull ?: "—"
    val type  = yoga["type"]?.jsonPrimitive?.contentOrNull ?: "—"
    val effect= yoga["effect"]?.jsonPrimitive?.contentOrNull ?: "—"
    val desc  = yoga["description"]?.jsonPrimitive?.contentOrNull ?: ""

    val effectColor = when {
        effect.contains("Benefic", ignoreCase = true) -> Color(0xFF43A047)
        effect.contains("Malefic", ignoreCase = true) -> Color(0xFFE53935)
        else -> Color(0xFFFF8F00)
    }
    val cardColor = if (isPresent) BrahmCard else Color(0xFFF5F5F5)
    val alpha     = if (isPresent) 1f else 0.5f

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = if (isPresent) androidx.compose.foundation.BorderStroke(1.dp, effectColor.copy(alpha = 0.3f)) else null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (isPresent) "✨" else "○", fontSize = 16.sp)
                Text(name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrahmForeground.copy(alpha = alpha)))
                Spacer(Modifier.weight(1f))
                if (effect.isNotBlank() && effect != "—") {
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp)).background(effectColor.copy(alpha = 0.12f * alpha))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(effect, style = MaterialTheme.typography.labelSmall.copy(color = effectColor.copy(alpha = alpha), fontSize = 10.sp))
                    }
                }
            }
            if (type.isNotBlank() && type != "—") {
                Text(type, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground.copy(alpha = alpha)))
            }
            if (isPresent && desc.isNotBlank()) {
                Text(desc, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.75f), fontSize = 12.sp))
            }
        }
    }
}
