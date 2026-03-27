package com.bimoraai.brahm.ui.nakshatra

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

data class NakshatraInfo(
    val number: Int,
    val name: String,
    val deity: String,
    val lord: String,       // ruling planet
    val padas: Int,
    val symbol: String,
    val animal: String,
    val nature: String,     // Deva / Manushya / Rakshasa
    val gana: String,
    val varna: String,
    val traits: List<String>,
    val gradStart: Color,
    val gradEnd: Color,
)

private val nakshatraList = listOf(
    NakshatraInfo(1,  "Ashwini",     "Ashwini Kumars", "Ketu",    4, "Horse Head",  "Horse",      "Deva",     "Deva",      "Vaishya",   listOf("Swift", "Healing", "Initiation"),    Color(0xFFE53935), Color(0xFF8B0000)),
    NakshatraInfo(2,  "Bharani",     "Yama",           "Venus",   4, "Yoni",        "Elephant",   "Manushya", "Manushya",  "Mleccha",   listOf("Restraint", "Truth", "Transformation"), Color(0xFFE91E63), Color(0xFF880E4F)),
    NakshatraInfo(3,  "Krittika",    "Agni",           "Sun",     4, "Razor/Flame", "Sheep",      "Rakshasa", "Rakshasa",  "Brahmin",   listOf("Purification", "Cutting", "Nurturing"), Color(0xFFFF6F00), Color(0xFFBF360C)),
    NakshatraInfo(4,  "Rohini",      "Brahma",         "Moon",    4, "Chariot",     "Serpent",    "Manushya", "Manushya",  "Shudra",    listOf("Fertility", "Beauty", "Growth"),        Color(0xFF43A047), Color(0xFF1B5E20)),
    NakshatraInfo(5,  "Mrigashira",  "Soma",           "Mars",    4, "Deer Head",   "Serpent",    "Deva",     "Deva",      "Farmer",    listOf("Searching", "Gentle", "Curious"),       Color(0xFF00ACC1), Color(0xFF006064)),
    NakshatraInfo(6,  "Ardra",       "Rudra",          "Rahu",    4, "Teardrop",    "Dog",        "Manushya", "Manushya",  "Butcher",   listOf("Effort", "Storm", "Transformation"),   Color(0xFF6A1B9A), Color(0xFF4A148C)),
    NakshatraInfo(7,  "Punarvasu",   "Aditi",          "Jupiter", 4, "Bow & Arrow", "Cat",        "Deva",     "Deva",      "Vaishya",   listOf("Return", "Renewal", "Abundance"),       Color(0xFF1565C0), Color(0xFF0D47A1)),
    NakshatraInfo(8,  "Pushya",      "Brihaspati",     "Saturn",  4, "Flower",      "Sheep",      "Deva",     "Deva",      "Kshatriya", listOf("Nourishment", "Protection", "Growth"),  Color(0xFFFFB300), Color(0xFFE65100)),
    NakshatraInfo(9,  "Ashlesha",    "Nagas",          "Mercury", 4, "Serpent",     "Cat",        "Rakshasa", "Rakshasa",  "Mleccha",   listOf("Clinging", "Mystical", "Intense"),      Color(0xFF558B2F), Color(0xFF1B5E20)),
    NakshatraInfo(10, "Magha",       "Pitras",         "Ketu",    4, "Throne",      "Rat",        "Rakshasa", "Rakshasa",  "Shudra",    listOf("Power", "Ancestors", "Royalty"),        Color(0xFFBF360C), Color(0xFF7f0000)),
    NakshatraInfo(11, "Purva Phalguni","Bhaga",        "Venus",   4, "Hammock",     "Rat",        "Manushya", "Manushya",  "Brahmin",   listOf("Pleasure", "Creativity", "Rest"),       Color(0xFFE91E63), Color(0xFF880E4F)),
    NakshatraInfo(12, "Uttara Phalguni","Aryaman",     "Sun",     4, "Bed",         "Bull (Cow)", "Manushya", "Manushya",  "Kshatriya", listOf("Service", "Friendship", "Prosperity"),  Color(0xFFFF8F00), Color(0xFFBF360C)),
    NakshatraInfo(13, "Hasta",       "Savitar",        "Moon",    4, "Hand",        "Buffalo",    "Deva",     "Deva",      "Vaishya",   listOf("Skill", "Dexterity", "Healing"),        Color(0xFF00897B), Color(0xFF004D40)),
    NakshatraInfo(14, "Chitra",      "Tvashtar",       "Mars",    4, "Bright Jewel","Tiger",      "Rakshasa", "Rakshasa",  "Farmer",    listOf("Creation", "Beauty", "Illusion"),       Color(0xFF7B1FA2), Color(0xFF4A148C)),
    NakshatraInfo(15, "Swati",       "Vayu",           "Rahu",    4, "Coral/Sword", "Buffalo",    "Deva",     "Deva",      "Butcher",   listOf("Independence", "Movement", "Dispersal"),Color(0xFF1E88E5), Color(0xFF0D47A1)),
    NakshatraInfo(16, "Vishakha",    "Indra-Agni",     "Jupiter", 4, "Archway",     "Tiger",      "Rakshasa", "Rakshasa",  "Mleccha",   listOf("Goal-oriented", "Determination", "Success"), Color(0xFFFF6F00), Color(0xFFBF360C)),
    NakshatraInfo(17, "Anuradha",    "Mitra",          "Saturn",  4, "Lotus",       "Hare/Deer",  "Deva",     "Deva",      "Shudra",    listOf("Friendship", "Devotion", "Cooperation"), Color(0xFF2E7D32), Color(0xFF1B5E20)),
    NakshatraInfo(18, "Jyeshtha",    "Indra",          "Mercury", 4, "Earring",     "Hare/Deer",  "Rakshasa", "Rakshasa",  "Farmer",    listOf("Elder", "Protection", "Power"),         Color(0xFFAD1457), Color(0xFF880E4F)),
    NakshatraInfo(19, "Mula",        "Nirriti",        "Ketu",    4, "Roots",       "Dog",        "Rakshasa", "Rakshasa",  "Butcher",   listOf("Foundation", "Dissolution", "Truth"),   Color(0xFF4527A0), Color(0xFF311B92)),
    NakshatraInfo(20, "Purva Ashadha","Apas",          "Venus",   4, "Fan/Tusk",    "Monkey",     "Manushya", "Manushya",  "Brahmin",   listOf("Invincibility", "Purification", "Energy"), Color(0xFF0277BD), Color(0xFF01579B)),
    NakshatraInfo(21, "Uttara Ashadha","Vishvadevas",  "Sun",     4, "Elephant Tusk","Mongoose",  "Manushya", "Manushya",  "Kshatriya", listOf("Victory", "Leadership", "Righteousness"), Color(0xFFE65100), Color(0xFFBF360C)),
    NakshatraInfo(22, "Shravana",    "Vishnu",         "Moon",    4, "Ear",         "Monkey",     "Deva",     "Deva",      "Mleccha",   listOf("Learning", "Listening", "Connection"),  Color(0xFF1565C0), Color(0xFF0D47A1)),
    NakshatraInfo(23, "Dhanishtha",  "Ashta Vasus",    "Mars",    4, "Drum",        "Lion",       "Rakshasa", "Rakshasa",  "Farmer",    listOf("Wealth", "Rhythm", "Mars energy"),     Color(0xFFBF360C), Color(0xFF7f0000)),
    NakshatraInfo(24, "Shatabhisha", "Varuna",         "Rahu",    4, "1000 Stars",  "Horse",      "Rakshasa", "Rakshasa",  "Butcher",   listOf("Healing", "Mystical", "Solitary"),     Color(0xFF283593), Color(0xFF1A237E)),
    NakshatraInfo(25, "Purva Bhadra","Aja Ekapada",    "Jupiter", 4, "Sword/Bed",   "Lion",       "Manushya", "Manushya",  "Brahmin",   listOf("Transformation", "Intensity", "Spirituality"), Color(0xFF6A1B9A), Color(0xFF4A148C)),
    NakshatraInfo(26, "Uttara Bhadra","Ahir Budhnya",  "Saturn",  4, "Twins",       "Cow",        "Manushya", "Manushya",  "Kshatriya", listOf("Depth", "Wisdom", "Detachment"),       Color(0xFF4527A0), Color(0xFF311B92)),
    NakshatraInfo(27, "Revati",      "Pushan",         "Mercury", 4, "Fish",        "Elephant",   "Deva",     "Deva",      "Shudra",    listOf("Nourishment", "Completion", "Journey"), Color(0xFF00695C), Color(0xFF004D40)),
)

@Composable
fun NakshatraScreen(navController: NavController) {
    var selectedNakshatra by remember { mutableStateOf<NakshatraInfo?>(null) }

    SwipeBackLayout(navController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nakshatra Explorer", fontWeight = FontWeight.Bold) },
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
            modifier = Modifier.fillMaxSize().background(BrahmBackground).padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(nakshatraList) { n ->
                NakshatraCard(
                    nakshatra = n,
                    expanded = selectedNakshatra?.number == n.number,
                    onClick = { selectedNakshatra = if (selectedNakshatra?.number == n.number) null else n },
                )
            }
        }
        ScrollToTopFab(listState, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp))
        } // Box
    }
    } // SwipeBackLayout
}

@Composable
private fun NakshatraCard(nakshatra: NakshatraInfo, expanded: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
        border = if (expanded) androidx.compose.foundation.BorderStroke(1.5.dp, nakshatra.gradStart) else null,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(nakshatra.gradStart, nakshatra.gradEnd))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${nakshatra.number}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(nakshatra.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text("${nakshatra.deity} · ${nakshatra.lord}", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
                NatureChip(nakshatra.nature)
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(color = BrahmBorder)
                    DetailRow("Symbol",   nakshatra.symbol)
                    DetailRow("Animal",   nakshatra.animal)
                    DetailRow("Gana",     nakshatra.gana)
                    DetailRow("Varna",    nakshatra.varna)
                    DetailRow("Padas",    "${nakshatra.padas}")
                    Spacer(Modifier.height(4.dp))
                    Text("Traits", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        nakshatra.traits.forEach { trait ->
                            Box(
                                Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(nakshatra.gradStart.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(trait, style = MaterialTheme.typography.labelSmall.copy(color = nakshatra.gradStart))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NatureChip(nature: String) {
    val color = when (nature) {
        "Deva"     -> Color(0xFF43A047)
        "Manushya" -> Color(0xFF1E88E5)
        else       -> Color(0xFFE53935)
    }
    Box(
        Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(nature, style = MaterialTheme.typography.labelSmall.copy(color = color, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
    }
}
