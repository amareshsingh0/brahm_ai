package com.bimoraai.brahm.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.BrahmCard
import com.bimoraai.brahm.core.theme.*
import com.bimoraai.brahm.ui.main.Route

data class ToolCard(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val route: String,
    val gradientStart: Color,
    val gradientEnd: Color,
)

private val tools = listOf(
    ToolCard(Icons.Default.Explore,        "Gochar",        "Planetary transits",      Route.GOCHAR,         Color(0xFF6C63FF), Color(0xFF3B2FBF)),
    ToolCard(Icons.Default.Favorite,       "Compatibility", "Kundali matching",         Route.COMPATIBILITY,  Color(0xFFE8445A), Color(0xFFB0203A)),
    ToolCard(Icons.Default.Schedule,       "Muhurta",       "Auspicious timings",       Route.MUHURTA,        Color(0xFF20A090), Color(0xFF0D6B60)),
    ToolCard(Icons.Default.Timelapse,      "Sade Sati",     "Saturn transit",           Route.SADE_SATI,      Color(0xFF5C6BC0), Color(0xFF303F9F)),
    ToolCard(Icons.Default.HealthAndSafety,"Dosha Check",   "Manglik, Kaal Sarpa",      Route.DOSHA,          Color(0xFFE53935), Color(0xFFB71C1C)),
    ToolCard(Icons.Default.Diamond,        "Gemstones",     "Rashi ratna",              Route.GEMSTONE,       Color(0xFF00ACC1), Color(0xFF006978)),
    ToolCard(Icons.Default.Science,        "KP System",     "Krishnamurti Paddhati",    Route.KP,             Color(0xFF43A047), Color(0xFF1B5E20)),
    ToolCard(Icons.Default.LiveHelp,       "Prashna",       "Horary astrology",         Route.PRASHNA,        Color(0xFF8E24AA), Color(0xFF4A148C)),
    ToolCard(Icons.Default.CalendarMonth,  "Varshphal",     "Solar return chart",       Route.VARSHPHAL,      Color(0xFFE67E22), Color(0xFFB7460C)),
    ToolCard(Icons.Default.Analytics,      "Rectification", "Birth time correction",    Route.RECTIFICATION,  Color(0xFF546E7A), Color(0xFF263238)),
    ToolCard(Icons.Default.FrontHand,      "Palmistry",     "AI palm reading",          Route.PALMISTRY,      Color(0xFFD4A017), Color(0xFF9A6E00)),
    ToolCard(Icons.Default.WbSunny,        "Horoscope",     "Daily rashi forecast",     Route.HOROSCOPE,      Color(0xFFFF7043), Color(0xFFBF360C)),
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
                    ) {
                        // Gradient icon tile
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(tool.gradientStart, tool.gradientEnd),
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = tool.title,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            tool.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            tool.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                        )
                    }
                }
            }
        }
    }
}
