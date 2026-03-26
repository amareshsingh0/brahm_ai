package com.bimoraai.brahm.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.*
import com.bimoraai.brahm.core.theme.*
import com.bimoraai.brahm.ui.main.Route

@Composable
fun TodayScreen(
    navController: NavController,          // outer — for full-screen routes (Compatibility, Palmistry…)
    tabNavController: NavController? = null, // inner — for tab routes (Chat, Kundali)
    vm: TodayViewModel = hiltViewModel(),
) {
    val panchang by vm.panchang.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // ── Hero Card ─────────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D59A)),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Namaste! 🙏",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = BrahmGold, fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        "The stars align today with unique cosmic energy. Explore your celestial blueprint below.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground),
                    )
                    Spacer(Modifier.height(4.dp))
                    BrahmButton(
                        text = "Generate Your Kundali →",
                        onClick = { (tabNavController ?: navController).navigate("tab_kundali") },
                    )
                }
            }
        }

        // ── 4 Stat Cards (2×2) ───────────────────────────────────────────────
        item {
            when {
                isLoading -> BrahmLoadingSpinner(modifier = Modifier.height(140.dp))
                error != null -> BrahmErrorView(message = error!!, onRetry = { vm.load() })
                else -> {
                    val p = panchang
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard(Icons.Default.Nightlight, Color(0xFF5C6BC0), Color(0xFF3949AB), "Moon Rashi", p?.str("chandra_rashi") ?: "—", p?.str("nakshatra") ?: "—", Modifier.weight(1f))
                            StatCard(Icons.Default.WbSunny,   Color(0xFFFF7043), Color(0xFFBF360C), "Sun Sign",   p?.str("surya_rashi") ?: "—",  p?.str("surya_nakshatra") ?: "—", Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard(Icons.Default.Stars,         Color(0xFF43A047), Color(0xFF1B5E20), "Nakshatra", p?.str("nakshatra") ?: "—", p?.str("nakshatra_lord") ?: "—", Modifier.weight(1f))
                            StatCard(Icons.Default.CalendarToday, Color(0xFFD4A017), Color(0xFF9A6E00), "Tithi",     p?.str("tithi") ?: "—",    p?.str("tithi_lord") ?: "—",    Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // ── Panchang + Guidance row ───────────────────────────────────────────
        if (panchang != null && !isLoading && error == null) {
            item {
                val p = panchang!!
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrahmCard(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionHeader("Panchang")
                            PanchangRow("Yoga",      p.str("yoga"))
                            PanchangRow("Karan",     p.str("karan"))
                            PanchangRow("Var",       p.str("var"))
                            PanchangRow("Rahu Kaal", p.str("rahu_kaal"))
                            PanchangRow("Sunrise",   p.str("sunrise"))
                            PanchangRow("Sunset",    p.str("sunset"))
                        }
                    }
                    BrahmCard(modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionHeader("Guidance")
                            GuidanceItem(Icons.Default.Work,          Color(0xFF5C6BC0), "Career",    "Focus on long-term goals today.")
                            GuidanceItem(Icons.Default.Favorite,      Color(0xFFE8445A), "Relations", "Nurture bonds with loved ones.")
                            GuidanceItem(Icons.Default.HealthAndSafety, Color(0xFF43A047), "Health",    "Practice mindful breathing.")
                            GuidanceItem(Icons.Default.AutoAwesome,   Color(0xFFD4A017), "Spiritual", "Chant your ishta mantra at sunrise.")
                        }
                    }
                }
            }
        }

        // ── Quick Links (2×2) ─────────────────────────────────────────────────
        item { SectionHeader("Quick Access") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickLinkCard(Icons.AutoMirrored.Filled.Chat, Color(0xFF6C63FF), Color(0xFF3B2FBF), "Brahm AI Chat", "Ask anything",      "tab_chat",    tabNavController ?: navController, Modifier.weight(1f))
                    QuickLinkCard(Icons.Default.Stars,            Color(0xFFD4A017), Color(0xFF9A6E00), "My Kundali",    "Vedic birth chart", "tab_kundali", tabNavController ?: navController, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickLinkCard(Icons.Default.Favorite,   Color(0xFFE8445A), Color(0xFFB0203A), "Compatibility", "Kundali matching", Route.COMPATIBILITY, navController, Modifier.weight(1f))
                    QuickLinkCard(Icons.Default.FrontHand,  Color(0xFF20A090), Color(0xFF0D6B60), "Palmistry",     "AI palm reading",  Route.PALMISTRY,     navController, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    gradStart: Color,
    gradEnd: Color,
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier,
) {
    BrahmCard(modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(gradStart, gradEnd))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Text(label, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(sub,   style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
        }
    }
}

@Composable
private fun PanchangRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

@Composable
private fun GuidanceItem(icon: ImageVector, iconColor: Color, title: String, desc: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(15.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
            Text(desc,  style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground, fontSize = 11.sp))
        }
    }
}

@Composable
private fun QuickLinkCard(
    icon: ImageVector,
    gradStart: Color,
    gradEnd: Color,
    title: String,
    desc: String,
    route: String,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    BrahmCard(modifier = modifier, onClick = { navController.navigate(route) }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(listOf(gradStart, gradEnd))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Text(desc,  style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
        }
    }
}
