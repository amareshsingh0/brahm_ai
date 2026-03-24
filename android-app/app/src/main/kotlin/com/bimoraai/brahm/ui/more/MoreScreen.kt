package com.bimoraai.brahm.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.BrahmCard
import com.bimoraai.brahm.core.theme.*
import com.bimoraai.brahm.ui.main.Route

data class ToolCard(val emoji: String, val title: String, val subtitle: String, val route: String)

private val tools = listOf(
    ToolCard("🌍", "Gochar", "Planetary transits", Route.GOCHAR),
    ToolCard("💑", "Compatibility", "Kundali matching", Route.COMPATIBILITY),
    ToolCard("⏰", "Muhurta", "Auspicious timings", Route.MUHURTA),
    ToolCard("🪐", "Sade Sati", "Saturn transit", Route.SADE_SATI),
    ToolCard("⚠️", "Dosha Check", "Manglik, Kaal Sarpa", Route.DOSHA),
    ToolCard("💎", "Gemstones", "Rashi ratna", Route.GEMSTONE),
    ToolCard("🔬", "KP System", "Krishnamurti Paddhati", Route.KP),
    ToolCard("❓", "Prashna", "Horary astrology", Route.PRASHNA),
    ToolCard("📅", "Varshphal", "Solar return chart", Route.VARSHPHAL),
    ToolCard("⏱️", "Rectification", "Birth time correction", Route.RECTIFICATION),
    ToolCard("🖐️", "Palmistry", "AI palm reading", Route.PALMISTRY),
    ToolCard("♈", "Horoscope", "Daily rashi forecast", Route.HOROSCOPE),
)

@Composable
fun MoreScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrahmBackground)
    ) {
        Surface(color = BrahmCard, shadowElevation = 1.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("All Tools", style = MaterialTheme.typography.headlineSmall.copy(color = BrahmGold))
                Text("Explore all Jyotish features", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(tools) { tool ->
                BrahmCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navController.navigate(tool.route) },
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(tool.emoji, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(tool.title, style = MaterialTheme.typography.titleMedium.copy(color = BrahmGold), textAlign = TextAlign.Center)
                        Text(tool.subtitle, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
