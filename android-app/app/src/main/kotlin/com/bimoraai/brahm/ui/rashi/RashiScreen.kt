package com.bimoraai.brahm.ui.rashi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.ScrollToTopFab
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.theme.*

data class RashiInfo(
    val number: Int,
    val name: String,
    val sanskrit: String,
    val symbol: String,
    val lord: String,
    val element: String,
    val quality: String,
    val nature: String,
    val bodyPart: String,
    val luckyColor: String,
    val traits: List<String>,
    val gradStart: Color,
    val gradEnd: Color,
)

private val rashiList = listOf(
    RashiInfo(1,  "Aries",       "Mesha",      "♈\uFE0E", "Mars",    "Fire",  "Movable", "Male",   "Head & Face",        "Red",       listOf("Courageous", "Dynamic", "Impulsive", "Leadership", "Energetic"), Color(0xFFE53935), Color(0xFF8B0000)),
    RashiInfo(2,  "Taurus",      "Vrishabha",  "♉\uFE0E", "Venus",   "Earth", "Fixed",   "Female", "Neck & Throat",      "Green",     listOf("Patient", "Reliable", "Stubborn", "Practical", "Sensual"),      Color(0xFF43A047), Color(0xFF1B5E20)),
    RashiInfo(3,  "Gemini",      "Mithuna",    "♊\uFE0E", "Mercury", "Air",   "Dual",    "Male",   "Arms & Shoulders",   "Yellow",    listOf("Curious", "Adaptable", "Witty", "Restless", "Communicative"),   Color(0xFFFFB300), Color(0xFFE65100)),
    RashiInfo(4,  "Cancer",      "Karka",      "♋\uFE0E", "Moon",    "Water", "Movable", "Female", "Chest & Stomach",    "White",     listOf("Intuitive", "Nurturing", "Moody", "Protective", "Empathetic"),   Color(0xFF1E88E5), Color(0xFF0D47A1)),
    RashiInfo(5,  "Leo",         "Simha",      "♌\uFE0E", "Sun",     "Fire",  "Fixed",   "Male",   "Heart & Back",       "Gold",      listOf("Proud", "Generous", "Dramatic", "Creative", "Loyal"),            Color(0xFFFF8F00), Color(0xFFBF360C)),
    RashiInfo(6,  "Virgo",       "Kanya",      "♍\uFE0E", "Mercury", "Earth", "Dual",    "Female", "Intestines & Waist", "Navy Blue", listOf("Analytical", "Precise", "Helpful", "Modest", "Perfectionist"),   Color(0xFF6D4C41), Color(0xFF3E2723)),
    RashiInfo(7,  "Libra",       "Tula",       "♎\uFE0E", "Venus",   "Air",   "Movable", "Male",   "Kidneys & Back",     "Pink",      listOf("Diplomatic", "Fair", "Indecisive", "Social", "Idealistic"),      Color(0xFFE91E63), Color(0xFF880E4F)),
    RashiInfo(8,  "Scorpio",     "Vrishchika", "♏\uFE0E", "Mars",    "Water", "Fixed",   "Female", "Genitals",           "Dark Red",  listOf("Intense", "Secretive", "Passionate", "Determined", "Magnetic"),  Color(0xFF6A1B9A), Color(0xFF4A148C)),
    RashiInfo(9,  "Sagittarius", "Dhanu",      "♐\uFE0E", "Jupiter", "Fire",  "Dual",    "Male",   "Hips & Thighs",      "Purple",    listOf("Optimistic", "Free-spirited", "Philosophical", "Blunt", "Adventurous"), Color(0xFF8E24AA), Color(0xFF4A148C)),
    RashiInfo(10, "Capricorn",   "Makara",     "♑\uFE0E", "Saturn",  "Earth", "Movable", "Female", "Knees & Bones",      "Dark Brown",listOf("Disciplined", "Ambitious", "Cautious", "Patient", "Responsible"), Color(0xFF546E7A), Color(0xFF263238)),
    RashiInfo(11, "Aquarius",    "Kumbha",     "♒\uFE0E", "Saturn",  "Air",   "Fixed",   "Male",   "Ankles & Calves",    "Blue",      listOf("Independent", "Humanitarian", "Eccentric", "Intellectual", "Innovative"), Color(0xFF1565C0), Color(0xFF0D47A1)),
    RashiInfo(12, "Pisces",      "Meena",      "♓\uFE0E", "Jupiter", "Water", "Dual",    "Female", "Feet",               "Sea Green", listOf("Compassionate", "Dreamy", "Intuitive", "Artistic", "Escapist"),  Color(0xFF00897B), Color(0xFF004D40)),
)

@Composable
fun RashiScreen(navController: NavController) {
    var selectedRashi by remember { mutableStateOf<RashiInfo?>(null) }

    SwipeBackLayout(navController) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Rashi Explorer", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BrahmBackground),
                )
            },
        ) { padding ->
            val listState = rememberLazyListState()
            Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(BrahmBackground)
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(rashiList) { rashi ->
                    RashiCard(
                        rashi = rashi,
                        expanded = selectedRashi?.number == rashi.number,
                        onClick = {
                            selectedRashi = if (selectedRashi?.number == rashi.number) null else rashi
                        },
                    )
                }
            }
            ScrollToTopFab(listState, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 88.dp))
            } // Box
        }
    } // SwipeBackLayout
}

@Composable
private fun RashiCard(rashi: RashiInfo, expanded: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 4.dp else 1.dp),
        border = if (expanded) androidx.compose.foundation.BorderStroke(1.5.dp, rashi.gradStart) else null,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // Header row: icon + name + chips on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(rashi.gradStart, rashi.gradEnd))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(rashi.symbol, fontSize = 26.sp)
                    }
                    Column {
                        Text(
                            rashi.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            rashi.sanskrit,
                            style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                        )
                    }
                }
                // Element + quality chips on right side
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    InfoChip(rashi.element, rashi.gradStart)
                    InfoChip(rashi.quality, Color(0xFF6B7280))
                }
            }

            Text(
                "Lord: ${rashi.lord}",
                style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
            )

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(color = BrahmBorder)
                    DetailRow("Body Part", rashi.bodyPart)
                    DetailRow("Nature", "${rashi.nature} · ${rashi.quality}")
                    DetailRow("Lucky Color", rashi.luckyColor)
                    Text(
                        "Traits",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BrahmMutedForeground,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    // FlowRow — wraps chips naturally, no overflow
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        rashi.traits.forEach { trait ->
                            InfoChip(trait, rashi.gradStart)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = color,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
    }
}
