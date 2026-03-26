package com.bimoraai.brahm.ui.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.bimoraai.brahm.core.theme.*
import kotlinx.coroutines.launch

data class VedicStory(
    val title: String,
    val subtitle: String,
    val symbol: String,
    val gradStart: Color,
    val gradEnd: Color,
    val pages: List<StoryPage>,
)

data class StoryPage(
    val heading: String,
    val body: String,
)

private val stories = listOf(
    VedicStory(
        "The Ashwini Kumars",
        "Divine Physicians of the Gods",
        "⚕",
        Color(0xFF1565C0), Color(0xFF0D47A1),
        listOf(
            StoryPage("Birth of the Divine Twins",
                "Long ago, the Sun god Surya had a wife named Sanjna who could not bear his brilliant light. She created a shadow of herself (Chhaya) and retreated to a forest in the form of a mare. Surya, searching for her, took the form of a horse. From this divine union were born the Ashwini Kumars — Nasatya and Dasra — the celestial twin physicians."),
            StoryPage("Masters of Healing",
                "The Ashwini Kumars became the physicians of the gods (Devavaidyas). They possessed unmatched knowledge of Ayurveda and could cure any disease, restore youth, and even bring the dead back to life. They rejuvenated the sage Chyavana, giving him back his youth so he could wed the young Sukanya."),
            StoryPage("Astronomical Significance",
                "In astrology, the Ashwini nakshatra (the first of 27 lunar mansions) is ruled by Ketu and presided over by the Ashwini Kumars. People born under this nakshatra are said to be swift, energetic, and natural healers. The symbol of Ashwini is a horse's head — representing speed and initiation."),
            StoryPage("Lesson of the Story",
                "The Ashwini Kumars teach us that true healing comes from compassion and divine knowledge. They did not distinguish between gods and mortals — they healed all who came to them. Their story reminds us that health is the greatest wealth, and the spirit of service is the highest dharma."),
        )
    ),
    VedicStory(
        "Saturn & Shani Dev",
        "The Inevitable Karma",
        "♄",
        Color(0xFF37474F), Color(0xFF263238),
        listOf(
            StoryPage("Birth of Shani",
                "Shani Dev (Saturn) was born to the Sun god Surya and his shadow wife Chhaya. Unlike his siblings, Shani was born with a dark complexion and had a different nature — introspective, slow-moving, and deeply connected to karma. His gaze was said to be so powerful that even his own father Surya was affected when Shani looked at him."),
            StoryPage("The God of Karma",
                "Shani is the lord of karma, discipline, and justice. He rules the signs Capricorn and Aquarius, and his transit through the zodiac takes 29.5 years. When Saturn transits your Moon sign and the signs before and after it, it is called Sade Sati — a 7.5-year period of trials, lessons, and ultimately, spiritual growth."),
            StoryPage("Shani's Justice",
                "Unlike other planets, Shani does not give suffering out of malice — he gives what you deserve based on your past karmas. He teaches patience, perseverance, and humility. Those who work hard, remain honest, and serve others are blessed by Shani. Those who are lazy, deceptive, or disrespectful of dharma face his corrective justice."),
            StoryPage("Lesson of Saturn",
                "Saturn's lesson is this: there are no shortcuts in life. Every action has a consequence. Pain is often the universe's way of redirecting you toward your true path. When Shani tests you, it is not punishment — it is purification. Those who emerge from Saturn's trials are wiser, stronger, and more compassionate souls."),
        )
    ),
    VedicStory(
        "Rahu & Ketu",
        "The Eclipse Demons",
        "☊",
        Color(0xFF4527A0), Color(0xFF311B92),
        listOf(
            StoryPage("The Churning of the Ocean",
                "During the great churning of the cosmic ocean (Samudra Manthan), the divine nectar of immortality (Amrita) emerged. The gods and demons had agreed to share it, but the demon Svarbhanu disguised himself as a god and sat between the Sun and Moon to drink the Amrita."),
            StoryPage("Vishnu's Discus",
                "The Sun and Moon recognized the demon and alerted Vishnu. Vishnu immediately hurled his Sudarshana Chakra (divine discus) and severed Svarbhanu's head from his body. But the demon had already swallowed the Amrita — his head and body both became immortal. The head became Rahu, the body became Ketu."),
            StoryPage("The Eternal Enmity",
                "Rahu and Ketu harbor eternal enmity toward the Sun and Moon for revealing their identity. This is why they periodically swallow the Sun and Moon — causing solar and lunar eclipses. The moment is temporary because the severed body cannot truly hold them — the luminaries escape."),
            StoryPage("Rahu-Ketu in Astrology",
                "In Vedic astrology, Rahu and Ketu are the lunar nodes — mathematical points where the Moon's orbit intersects the ecliptic. Rahu represents desires, obsession, material world, and karmic future. Ketu represents moksha, spirituality, past karma, and liberation. Together they reveal our soul's journey across lifetimes."),
            StoryPage("Lesson of the Nodes",
                "Rahu teaches us about our unquenchable desires and attachments. Ketu teaches detachment and spiritual wisdom. The axis of Rahu-Ketu in your birth chart shows your past-life gifts (Ketu) and your soul's current mission (Rahu). Balance between desire and detachment is the key to spiritual evolution."),
        )
    ),
)

@Composable
fun StoriesScreen(navController: NavController) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState { stories.size }
    val scope = rememberCoroutineScope()
    var storyPageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(pagerState.currentPage) { storyPageIndex = 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vedic Stories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(BrahmBackground).padding(padding),
        ) {
            // Story selector tabs
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.White,
                contentColor = BrahmGold,
                edgePadding = 12.dp,
            ) {
                stories.forEachIndexed { index, story ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(story.title, style = MaterialTheme.typography.bodySmall, maxLines = 1) },
                    )
                }
            }

            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { storyIndex ->
                val story = stories[storyIndex]
                val currentPage = story.pages[storyPageIndex.coerceIn(0, story.pages.lastIndex)]

                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Hero
                    Box(
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(story.gradStart, story.gradEnd))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(story.symbol, fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(story.title, style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                            Text(story.subtitle, style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f)))
                        }
                    }

                    // Story page card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BrahmCard),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                currentPage.heading,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = BrahmGold),
                            )
                            Text(
                                currentPage.body,
                                style = MaterialTheme.typography.bodyMedium.copy(color = BrahmForeground, lineHeight = 24.sp),
                            )
                        }
                    }

                    // Navigation + progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = { if (storyPageIndex > 0) storyPageIndex-- },
                            enabled = storyPageIndex > 0,
                        ) { Text("← Prev") }

                        // Progress dots
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            story.pages.indices.forEach { i ->
                                Box(
                                    modifier = Modifier.size(if (i == storyPageIndex) 10.dp else 7.dp)
                                        .clip(CircleShape)
                                        .background(if (i == storyPageIndex) BrahmGold else BrahmBorder),
                                )
                            }
                        }

                        TextButton(
                            onClick = { if (storyPageIndex < story.pages.lastIndex) storyPageIndex++ },
                            enabled = storyPageIndex < story.pages.lastIndex,
                        ) { Text("Next →", color = if (storyPageIndex < story.pages.lastIndex) BrahmGold else BrahmMutedForeground) }
                    }
                }
            }
        }
    }
}
