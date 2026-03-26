package com.bimoraai.brahm.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.LiveHelp
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
    ToolCard(Icons.Default.Explore,        "Gochar",         "Planetary transits",      Route.GOCHAR,         Color(0xFF6C63FF), Color(0xFF3B2FBF)),
    ToolCard(Icons.Default.Favorite,       "Compatibility",  "Kundali matching",        Route.COMPATIBILITY,  Color(0xFFE8445A), Color(0xFFB0203A)),
    ToolCard(Icons.Default.Schedule,       "Muhurta",        "Auspicious timings",      Route.MUHURTA,        Color(0xFF20A090), Color(0xFF0D6B60)),
    ToolCard(Icons.Default.Timelapse,      "Sade Sati",      "Saturn transit",          Route.SADE_SATI,      Color(0xFF5C6BC0), Color(0xFF303F9F)),
    ToolCard(Icons.Default.HealthAndSafety,"Dosha Check",    "Manglik, Kaal Sarpa",     Route.DOSHA,          Color(0xFFE53935), Color(0xFFB71C1C)),
    ToolCard(Icons.Default.Diamond,        "Gemstones",      "Rashi ratna",             Route.GEMSTONE,       Color(0xFF00ACC1), Color(0xFF006978)),
    ToolCard(Icons.Default.Science,        "KP System",      "Krishnamurti Paddhati",   Route.KP,             Color(0xFF43A047), Color(0xFF1B5E20)),
    ToolCard(Icons.AutoMirrored.Filled.LiveHelp, "Prashna", "Horary astrology",        Route.PRASHNA,        Color(0xFF8E24AA), Color(0xFF4A148C)),
    ToolCard(Icons.Default.CalendarMonth,  "Varshphal",      "Solar return chart",      Route.VARSHPHAL,      Color(0xFFE67E22), Color(0xFFB7460C)),
    ToolCard(Icons.Default.Analytics,      "Rectification",  "Birth time correction",   Route.RECTIFICATION,  Color(0xFF546E7A), Color(0xFF263238)),
    ToolCard(Icons.Default.FrontHand,      "Palmistry",      "AI palm reading",         Route.PALMISTRY,      Color(0xFFD4A017), Color(0xFF9A6E00)),
    ToolCard(Icons.Default.WbSunny,        "Horoscope",      "Daily rashi forecast",    Route.HOROSCOPE,      Color(0xFFFF7043), Color(0xFFBF360C)),
    ToolCard(Icons.Default.CalendarViewDay,"Panchang",       "Full daily panchang",     Route.PANCHANG,       Color(0xFF0288D1), Color(0xFF01579B)),
    ToolCard(Icons.Default.NightsStay,     "Live Sky",       "Current planet positions",Route.SKY,            Color(0xFF1A237E), Color(0xFF0D1442)),
    ToolCard(Icons.AutoMirrored.Filled.MenuBook, "Vedic Stories", "Mythology & wisdom", Route.STORIES,        Color(0xFFAD1457), Color(0xFF6A0036)),
    ToolCard(Icons.Default.Brightness5,    "Rashi Explorer", "12 zodiac signs",         Route.RASHI,          Color(0xFFEF6C00), Color(0xFFBF360C)),
    ToolCard(Icons.Default.Stars,          "Nakshatra",      "27 lunar mansions",       Route.NAKSHATRA,      Color(0xFF4527A0), Color(0xFF1A0072)),
    ToolCard(Icons.Default.AutoAwesome,    "Yogas",          "Astrological yogas",      Route.YOGAS,          Color(0xFF00838F), Color(0xFF004D56)),
    ToolCard(Icons.Default.Healing,        "Remedies",       "Planet remedies",         Route.REMEDIES,       Color(0xFF2E7D32), Color(0xFF1B5E20)),
    ToolCard(Icons.Default.MusicNote,      "Mantras",        "Sacred chants & prayers", Route.MANTRA,         Color(0xFFC62828), Color(0xFF7F0000)),
    ToolCard(Icons.AutoMirrored.Filled.LibraryBooks, "Vedic Library", "Search scriptures", Route.LIBRARY,      Color(0xFF558B2F), Color(0xFF33691E)),
    ToolCard(Icons.Default.AccountTree,    "Gotra Finder",   "Vedic lineage system",    Route.GOTRA,          Color(0xFF6D4C41), Color(0xFF3E2723)),
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
