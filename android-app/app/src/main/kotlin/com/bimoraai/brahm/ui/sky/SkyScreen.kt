package com.bimoraai.brahm.ui.sky

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.BrahmErrorView
import com.bimoraai.brahm.core.components.BrahmLoadingSpinner
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private data class PlanetMeta(val symbol: String, val gradStart: Color, val gradEnd: Color)
private val planetMeta = mapOf(
    "Surya"   to PlanetMeta("☉", Color(0xFFFF8F00), Color(0xFFBF360C)),
    "Chandra" to PlanetMeta("☽", Color(0xFF1565C0), Color(0xFF0D47A1)),
    "Mangal"  to PlanetMeta("♂", Color(0xFFE53935), Color(0xFF8B0000)),
    "Budh"    to PlanetMeta("☿", Color(0xFF2E7D32), Color(0xFF1B5E20)),
    "Guru"    to PlanetMeta("♃", Color(0xFFFFB300), Color(0xFFE65100)),
    "Shukra"  to PlanetMeta("♀", Color(0xFFE91E63), Color(0xFF880E4F)),
    "Shani"   to PlanetMeta("♄", Color(0xFF37474F), Color(0xFF263238)),
    "Rahu"    to PlanetMeta("☊", Color(0xFF4527A0), Color(0xFF311B92)),
    "Ketu"    to PlanetMeta("☋", Color(0xFF546E7A), Color(0xFF263238)),
)

@Composable
fun SkyScreen(
    navController: NavController,
    vm: SkyViewModel = hiltViewModel(),
) {
    val planets   by vm.planets.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error     by vm.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Live Sky", fontWeight = FontWeight.Bold)
                        Text("Current Planetary Positions", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                actions = {
                    IconButton(onClick = { vm.load() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Refresh", tint = BrahmGold)
                    }
                },
            )
        },
    ) { padding ->
        when {
            isLoading -> BrahmLoadingSpinner(modifier = Modifier.fillMaxSize().padding(padding))
            error != null -> BrahmErrorView(message = error!!, onRetry = { vm.load() }, modifier = Modifier.padding(padding))
            else -> SkyContent(planets, Modifier.padding(padding))
        }
    }
}

@Composable
private fun SkyContent(planets: JsonObject?, modifier: Modifier = Modifier) {
    // API returns {"grahas": {"Surya": {...}, "Chandra": {...}, ...}}
    val GRAHA_ORDER = listOf("Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani","Rahu","Ketu")
    val planetList = planets?.get("grahas")?.let { el ->
        try {
            el.jsonObject.entries
                .sortedBy { GRAHA_ORDER.indexOf(it.key).let { i -> if (i < 0) 99 else i } }
                .map { (name, value) ->
                    buildJsonObject {
                        put("name", name)
                        value.jsonObject.forEach { k, v -> put(k, v) }
                    }
                }
        } catch (_: Exception) { null }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🌌", fontSize = 40.sp)
                        Text("Current Sky", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                        Text(
                            "Real-time positions of 9 Grahas · Refreshes every 60 seconds",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.6f)),
                        )
                    }
                }
            }
        }

        if (planetList.isNullOrEmpty()) {
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                    Text(
                        "Planet data not available. Please try again.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground),
                    )
                }
            }
        } else {
            items(planetList) { planet ->
                val name      = planet["name"]?.jsonPrimitive?.contentOrNull ?: "—"
                val rashi     = planet["rashi"]?.jsonPrimitive?.contentOrNull ?: "—"
                val degree    = planet["degree"]?.jsonPrimitive?.contentOrNull ?: "—"
                val nakshatra = planet["nakshatra"]?.jsonPrimitive?.contentOrNull ?: "—"
                val retro     = planet["retro"]?.jsonPrimitive?.booleanOrNull == true
                PlanetCard(name, rashi, degree, nakshatra, retro)
            }
        }
    }
}

@Composable
private fun PlanetCard(name: String, rashi: String, degree: String, nakshatra: String, retrograde: Boolean) {
    val meta = planetMeta[name] ?: PlanetMeta("★", BrahmGold, Color(0xFF9A6E00))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(meta.gradStart, meta.gradEnd))),
                contentAlignment = Alignment.Center,
            ) {
                Text(meta.symbol, fontSize = 24.sp, color = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    if (retrograde) {
                        Box(
                            Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFE53935).copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("℞ Retrograde", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE53935), fontSize = 10.sp))
                        }
                    }
                }
                Text(
                    "$rashi · $degree°",
                    style = MaterialTheme.typography.bodyMedium.copy(color = BrahmForeground),
                )
                Text(nakshatra, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
            }
        }
    }
}
