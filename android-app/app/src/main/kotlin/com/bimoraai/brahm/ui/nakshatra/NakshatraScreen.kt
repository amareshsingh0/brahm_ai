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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.theme.*

data class NakshatraInfo(
    val number: Int,
    val name: String,
    val deity: String,
    val lord: String,         // ruling planet name
    val lordSymbol: String,   // ☉ ☽ ♂ ☿ ♃ ♀ ♄ ☊ ☋
    val padas: Int,
    val symbolText: String,   // text description of symbol
    val symbolEmoji: String,  // emoji for the symbol
    val animal: String,
    val nature: String,       // Deva / Manushya / Rakshasa
    val gana: String,
    val varna: String,
    val dateRange: String,    // approx annual solar transit
    val traits: List<String>,
    val gradStart: Color,
    val gradEnd: Color,
)

private val nakshatraList = listOf(
    NakshatraInfo(1,  "Ashwini",          "Ashwini Kumars",  "Ketu",    "☋", 4, "Horse Head",    "🐴", "Horse",        "Deva",     "Deva",      "Vaishya",   "Apr 14 – Apr 26", listOf("Swift", "Healing", "Initiation"),         Color(0xFFE53935), Color(0xFF8B0000)),
    NakshatraInfo(2,  "Bharani",           "Yama",            "Venus",   "♀", 4, "Yoni",          "🔺", "Elephant",     "Manushya", "Manushya",  "Mleccha",   "Apr 27 – May 9",  listOf("Restraint", "Truth", "Transformation"),     Color(0xFFE91E63), Color(0xFF880E4F)),
    NakshatraInfo(3,  "Krittika",          "Agni",            "Sun",     "☉", 4, "Razor / Flame", "🔥", "Sheep",        "Rakshasa", "Rakshasa",  "Brahmin",   "May 10 – May 22", listOf("Purification", "Cutting", "Nurturing"),     Color(0xFFFF6F00), Color(0xFFBF360C)),
    NakshatraInfo(4,  "Rohini",            "Brahma",          "Moon",    "☽", 4, "Chariot",       "🌹", "Serpent",      "Manushya", "Manushya",  "Shudra",    "May 23 – Jun 4",  listOf("Fertility", "Beauty", "Growth"),            Color(0xFF43A047), Color(0xFF1B5E20)),
    NakshatraInfo(5,  "Mrigashira",        "Soma",            "Mars",    "♂", 4, "Deer Head",     "🦌", "Serpent",      "Deva",     "Deva",      "Farmer",    "Jun 5 – Jun 17",  listOf("Searching", "Gentle", "Curious"),           Color(0xFF00ACC1), Color(0xFF006064)),
    NakshatraInfo(6,  "Ardra",             "Rudra",           "Rahu",    "☊", 4, "Teardrop",      "💧", "Dog",          "Manushya", "Manushya",  "Butcher",   "Jun 18 – Jun 30", listOf("Effort", "Storm", "Transformation"),        Color(0xFF6A1B9A), Color(0xFF4A148C)),
    NakshatraInfo(7,  "Punarvasu",         "Aditi",           "Jupiter", "♃", 4, "Bow & Arrow",   "🏹", "Cat",          "Deva",     "Deva",      "Vaishya",   "Jul 1 – Jul 13",  listOf("Return", "Renewal", "Abundance"),           Color(0xFF1565C0), Color(0xFF0D47A1)),
    NakshatraInfo(8,  "Pushya",            "Brihaspati",      "Saturn",  "♄", 4, "Flower",        "🌸", "Sheep",        "Deva",     "Deva",      "Kshatriya", "Jul 14 – Jul 26", listOf("Nourishment", "Protection", "Growth"),      Color(0xFFFFB300), Color(0xFFE65100)),
    NakshatraInfo(9,  "Ashlesha",          "Nagas",           "Mercury", "☿", 4, "Serpent",       "🐍", "Cat",          "Rakshasa", "Rakshasa",  "Mleccha",   "Jul 27 – Aug 8",  listOf("Clinging", "Mystical", "Intense"),          Color(0xFF558B2F), Color(0xFF1B5E20)),
    NakshatraInfo(10, "Magha",             "Pitras",          "Ketu",    "☋", 4, "Throne",        "👑", "Rat",          "Rakshasa", "Rakshasa",  "Shudra",    "Aug 9 – Aug 21",  listOf("Power", "Ancestors", "Royalty"),            Color(0xFFBF360C), Color(0xFF7F0000)),
    NakshatraInfo(11, "Purva Phalguni",    "Bhaga",           "Venus",   "♀", 4, "Hammock",       "🛋", "Rat",          "Manushya", "Manushya",  "Brahmin",   "Aug 22 – Sep 3",  listOf("Pleasure", "Creativity", "Rest"),           Color(0xFFE91E63), Color(0xFF880E4F)),
    NakshatraInfo(12, "Uttara Phalguni",   "Aryaman",         "Sun",     "☉", 4, "Bed",           "🛏", "Bull / Cow",   "Manushya", "Manushya",  "Kshatriya", "Sep 4 – Sep 16",  listOf("Service", "Friendship", "Prosperity"),      Color(0xFFFF8F00), Color(0xFFBF360C)),
    NakshatraInfo(13, "Hasta",             "Savitar",         "Moon",    "☽", 4, "Hand",          "🤚", "Buffalo",      "Deva",     "Deva",      "Vaishya",   "Sep 17 – Sep 29", listOf("Skill", "Dexterity", "Healing"),            Color(0xFF00897B), Color(0xFF004D40)),
    NakshatraInfo(14, "Chitra",            "Tvashtar",        "Mars",    "♂", 4, "Bright Jewel",  "💎", "Tiger",        "Rakshasa", "Rakshasa",  "Farmer",    "Sep 30 – Oct 13", listOf("Creation", "Beauty", "Illusion"),           Color(0xFF7B1FA2), Color(0xFF4A148C)),
    NakshatraInfo(15, "Swati",             "Vayu",            "Rahu",    "☊", 4, "Coral / Sword", "⚔", "Buffalo",      "Deva",     "Deva",      "Butcher",   "Oct 14 – Oct 27", listOf("Independence", "Movement", "Dispersal"),    Color(0xFF1E88E5), Color(0xFF0D47A1)),
    NakshatraInfo(16, "Vishakha",          "Indra-Agni",      "Jupiter", "♃", 4, "Archway",       "🏛", "Tiger",        "Rakshasa", "Rakshasa",  "Mleccha",   "Oct 28 – Nov 10", listOf("Goal-oriented", "Determination", "Success"), Color(0xFFFF6F00), Color(0xFFBF360C)),
    NakshatraInfo(17, "Anuradha",          "Mitra",           "Saturn",  "♄", 4, "Lotus",         "🪷", "Hare / Deer",  "Deva",     "Deva",      "Shudra",    "Nov 11 – Nov 24", listOf("Friendship", "Devotion", "Cooperation"),    Color(0xFF2E7D32), Color(0xFF1B5E20)),
    NakshatraInfo(18, "Jyeshtha",          "Indra",           "Mercury", "☿", 4, "Earring",       "💫", "Hare / Deer",  "Rakshasa", "Rakshasa",  "Farmer",    "Nov 25 – Dec 8",  listOf("Elder", "Protection", "Power"),             Color(0xFFAD1457), Color(0xFF880E4F)),
    NakshatraInfo(19, "Mula",              "Nirriti",         "Ketu",    "☋", 4, "Roots",         "🌿", "Dog",          "Rakshasa", "Rakshasa",  "Butcher",   "Dec 9 – Dec 22",  listOf("Foundation", "Dissolution", "Truth"),       Color(0xFF4527A0), Color(0xFF311B92)),
    NakshatraInfo(20, "Purva Ashadha",     "Apas",            "Venus",   "♀", 4, "Fan / Tusk",    "🪭", "Monkey",       "Manushya", "Manushya",  "Brahmin",   "Dec 23 – Jan 5",  listOf("Invincibility", "Purification", "Energy"),  Color(0xFF0277BD), Color(0xFF01579B)),
    NakshatraInfo(21, "Uttara Ashadha",    "Vishvadevas",     "Sun",     "☉", 4, "Elephant Tusk", "🐘", "Mongoose",     "Manushya", "Manushya",  "Kshatriya", "Jan 6 – Jan 19",  listOf("Victory", "Leadership", "Righteousness"),  Color(0xFFE65100), Color(0xFFBF360C)),
    NakshatraInfo(22, "Shravana",          "Vishnu",          "Moon",    "☽", 4, "Ear",           "👂", "Monkey",       "Deva",     "Deva",      "Mleccha",   "Jan 20 – Feb 2",  listOf("Learning", "Listening", "Connection"),      Color(0xFF1565C0), Color(0xFF0D47A1)),
    NakshatraInfo(23, "Dhanishtha",        "Ashta Vasus",     "Mars",    "♂", 4, "Drum",          "🥁", "Lion",         "Rakshasa", "Rakshasa",  "Farmer",    "Feb 3 – Feb 16",  listOf("Wealth", "Rhythm", "Mars energy"),          Color(0xFFBF360C), Color(0xFF7F0000)),
    NakshatraInfo(24, "Shatabhisha",       "Varuna",          "Rahu",    "☊", 4, "1000 Stars",    "✦",  "Horse",        "Rakshasa", "Rakshasa",  "Butcher",   "Feb 17 – Mar 2",  listOf("Healing", "Mystical", "Solitary"),          Color(0xFF283593), Color(0xFF1A237E)),
    NakshatraInfo(25, "Purva Bhadra",      "Aja Ekapada",     "Jupiter", "♃", 4, "Sword / Bed",   "⚡", "Lion",         "Manushya", "Manushya",  "Brahmin",   "Mar 3 – Mar 16",  listOf("Transformation", "Intensity", "Spirituality"), Color(0xFF6A1B9A), Color(0xFF4A148C)),
    NakshatraInfo(26, "Uttara Bhadra",     "Ahir Budhnya",    "Saturn",  "♄", 4, "Twins",         "♊\uFE0E", "Cow",   "Manushya", "Manushya",  "Kshatriya", "Mar 17 – Mar 30", listOf("Depth", "Wisdom", "Detachment"),            Color(0xFF4527A0), Color(0xFF311B92)),
    NakshatraInfo(27, "Revati",            "Pushan",          "Mercury", "☿", 4, "Fish",          "🐟", "Elephant",     "Deva",     "Deva",      "Shudra",    "Mar 31 – Apr 13", listOf("Nourishment", "Completion", "Journey"),     Color(0xFF00695C), Color(0xFF004D40)),
)

@Composable
fun NakshatraScreen(navController: NavController) {
    var selectedNakshatra by remember { mutableStateOf<NakshatraInfo?>(null) }

    SwipeBackLayout(navController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nakshatra Explorer", fontWeight = FontWeight.Bold, color = BrahmGold)
                        Text("27 Lunar Mansions", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    }
                },
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
            contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 12.dp),
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
        } // Box
    }
    } // SwipeBackLayout
}

@Composable
private fun NakshatraCard(nakshatra: NakshatraInfo, expanded: Boolean, onClick: () -> Unit) {
    val accentAlpha = if (expanded) 0.14f else 0.09f
    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onClick() },
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = BrahmCard),
        border    = if (expanded)
            androidx.compose.foundation.BorderStroke(1.5.dp, nakshatra.gradStart.copy(alpha = 0.55f))
        else null,
    ) {
        Column {

            // ── Collapsed header ─────────────────────────────────────────────
            Row(
                modifier            = Modifier
                    .fillMaxWidth()
                    .background(
                        if (expanded)
                            Brush.horizontalGradient(
                                listOf(
                                    nakshatra.gradStart.copy(alpha = accentAlpha),
                                    Color.Transparent
                                )
                            )
                        else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = if (expanded) 10.dp else 14.dp),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {

                // Planet symbol orb (gradient circle)
                Box(
                    modifier        = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.verticalGradient(listOf(nakshatra.gradStart, nakshatra.gradEnd))),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            nakshatra.lordSymbol,
                            color      = Color.White,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 18.sp,
                        )
                        Text(
                            "${nakshatra.number}",
                            color      = Color.White.copy(alpha = 0.75f),
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 10.sp,
                        )
                    }
                }

                // Name + date range
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        nakshatra.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp,
                        ),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "☽",
                            fontSize = 10.sp,
                            color    = BrahmMutedForeground,
                        )
                        Text(
                            nakshatra.deity,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color    = BrahmMutedForeground,
                                fontSize = 10.sp,
                            ),
                        )
                    }
                    // Date range badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(nakshatra.gradStart.copy(alpha = 0.10f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            nakshatra.dateRange,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color      = nakshatra.gradStart,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 9.sp,
                            ),
                        )
                    }
                }

                // Symbol emoji + nature chip — stacked on right
                Column(
                    horizontalAlignment  = Alignment.End,
                    verticalArrangement  = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        nakshatra.symbolEmoji,
                        fontSize  = 26.sp,
                        textAlign = TextAlign.Center,
                    )
                    NatureChip(nakshatra.nature)
                }
            }

            // ── Expanded body ────────────────────────────────────────────────
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HorizontalDivider(color = nakshatra.gradStart.copy(alpha = 0.20f))

                    // Symbol description strip
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(nakshatra.gradStart.copy(alpha = 0.07f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(nakshatra.symbolEmoji, fontSize = 22.sp)
                        Column {
                            Text(
                                "Symbol",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color    = BrahmMutedForeground,
                                    fontSize = 9.sp,
                                ),
                            )
                            Text(
                                nakshatra.symbolText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        // Lord badge
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(nakshatra.gradStart.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                nakshatra.lordSymbol,
                                fontSize   = 13.sp,
                                color      = nakshatra.gradStart,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                nakshatra.lord,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color      = nakshatra.gradStart,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }

                    // Detail grid — 2 columns
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DetailCell("Animal",   nakshatra.animal,          nakshatra.gradStart)
                            DetailCell("Gana",     nakshatra.gana,            nakshatra.gradStart)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DetailCell("Varna",    nakshatra.varna,           nakshatra.gradStart)
                            DetailCell("Padas",    "${nakshatra.padas} padas", nakshatra.gradStart)
                        }
                    }

                    // Traits
                    Text(
                        "Key Traits",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color      = BrahmMutedForeground,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 10.sp,
                        ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        nakshatra.traits.forEach { trait ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(nakshatra.gradStart.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    trait,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color      = nakshatra.gradStart,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCell(label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.05f))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                color    = BrahmMutedForeground,
                fontSize = 10.sp,
            ),
        )
        Text(
            value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize   = 11.sp,
            ),
        )
    }
}

@Composable
private fun NatureChip(nature: String) {
    val color = when (nature) {
        "Deva"     -> Color(0xFF2E7D32)
        "Manushya" -> Color(0xFF1565C0)
        else       -> Color(0xFFC62828)   // Rakshasa
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            nature,
            style = MaterialTheme.typography.labelSmall.copy(
                color      = color,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 9.sp,
            ),
        )
    }
}
