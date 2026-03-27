package com.bimoraai.brahm.ui.remedies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.theme.*
import kotlinx.coroutines.launch

data class PlanetRemedy(
    val planet: String,
    val symbol: String,
    val mantra: String,
    val gemstone: String,
    val fastingDay: String,
    val color: String,
    val donation: String,
    val sacredDay: String,
    val gradStart: Color,
    val gradEnd: Color,
)

private val planetRemedies = listOf(
    PlanetRemedy("Sun", "☀️", "Om Hram Hrim Hraum Sah Suryaya Namah (108×)", "Ruby", "Sunday", "Red / Orange", "Wheat, Jaggery, Copper", "Ravi Pushya Nakshatra", Color(0xFFFF6F00), Color(0xFFBF360C)),
    PlanetRemedy("Moon", "🌙", "Om Shram Shrim Shraum Sah Chandraya Namah (108×)", "Pearl / Moonstone", "Monday", "White / Silver", "Rice, Milk, White clothes", "Purnima (Full Moon)", Color(0xFF1565C0), Color(0xFF0D47A1)),
    PlanetRemedy("Mars", "♂", "Om Kram Krim Kraum Sah Bhaumaya Namah (108×)", "Red Coral", "Tuesday", "Red / Coral", "Red lentils, Jaggery, Copper", "Mangal Chaturdashi", Color(0xFFE53935), Color(0xFF8B0000)),
    PlanetRemedy("Mercury", "☿", "Om Bram Brim Braum Sah Budhaya Namah (108×)", "Emerald / Green Tourmaline", "Wednesday", "Green", "Green Moong, Green cloth", "Budha Ashtami", Color(0xFF2E7D32), Color(0xFF1B5E20)),
    PlanetRemedy("Jupiter", "♃", "Om Gram Grim Graum Sah Gurave Namah (108×)", "Yellow Sapphire / Topaz", "Thursday", "Yellow / Gold", "Besan, Turmeric, Gold", "Guru Purnima", Color(0xFFFFB300), Color(0xFFE65100)),
    PlanetRemedy("Venus", "♀", "Om Dram Drim Draum Sah Shukraya Namah (108×)", "Diamond / White Sapphire", "Friday", "White / Pink", "Rice, White sweets, Silver", "Shukra Saptami", Color(0xFFE91E63), Color(0xFF880E4F)),
    PlanetRemedy("Saturn", "♄", "Om Pram Prim Praum Sah Shanaischaraya Namah (108×)", "Blue Sapphire / Amethyst", "Saturday", "Dark Blue / Black", "Sesame, Iron, Black cloth", "Shani Jayanti / Amavasya", Color(0xFF37474F), Color(0xFF263238)),
    PlanetRemedy("Rahu", "☊", "Om Bhram Bhrim Bhraum Sah Rahave Namah (108×)", "Hessonite Garnet (Gomed)", "Saturday", "Dark Navy / Smoky", "Sesame, Coconut, Blanket", "Rahu Kaal Puja", Color(0xFF4527A0), Color(0xFF311B92)),
    PlanetRemedy("Ketu", "☋", "Om Shram Shrim Shraum Sah Ketave Namah (108×)", "Cat's Eye (Lehsunia)", "Tuesday", "Smoky / Grey", "Sesame, Copper, Blanket", "Ketu Puja on Ekadashi", Color(0xFF546E7A), Color(0xFF263238)),
)

@Composable
fun RemediesScreen(navController: NavController) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState { planetRemedies.size }
    val scope = rememberCoroutineScope()

    SwipeBackLayout(navController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planetary Remedies", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrahmBackground),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(BrahmBackground).padding(padding),
        ) {
            // Planet tab row
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = BrahmBackground,
                contentColor = BrahmGold,
                edgePadding = 12.dp,
            ) {
                planetRemedies.forEachIndexed { index, planet ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(planet.planet, style = MaterialTheme.typography.bodySmall) },
                    )
                }
            }
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                RemedyPage(planetRemedies[page])
            }
        }
    }
    } // SwipeBackLayout
}

@Composable
private fun RemedyPage(remedy: PlanetRemedy) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrahmCard),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(140.dp)
                    .background(Brush.linearGradient(listOf(remedy.gradStart, remedy.gradEnd))),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(remedy.symbol, style = MaterialTheme.typography.displayMedium)
                    Text(
                        remedy.planet,
                        style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }

        // Mantra card
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Beej Mantra")
                Text(
                    remedy.mantra,
                    style = MaterialTheme.typography.bodyMedium.copy(color = BrahmForeground),
                )
            }
        }

        // Remedy grid
        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Remedies")
                RemedyRow("Gemstone",     remedy.gemstone,   remedy.gradStart)
                RemedyRow("Fasting Day",  remedy.fastingDay, remedy.gradStart)
                RemedyRow("Color",        remedy.color,      remedy.gradStart)
                RemedyRow("Donation",     remedy.donation,   remedy.gradStart)
                RemedyRow("Sacred Day",   remedy.sacredDay,  remedy.gradStart)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(
            color = BrahmMutedForeground,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp),
        ),
    )
}

@Composable
private fun RemedyRow(label: String, value: String, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp))
                .background(accentColor),
        )
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground), modifier = Modifier.width(90.dp))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
    }
}
