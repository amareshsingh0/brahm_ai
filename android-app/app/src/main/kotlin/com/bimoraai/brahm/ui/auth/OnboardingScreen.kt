package com.bimoraai.brahm.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bimoraai.brahm.core.components.BrahmButton
import com.bimoraai.brahm.core.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(val emoji: String, val title: String, val subtitle: String)

private val pages = listOf(
    OnboardingPage("🔮", "Vedic Astrology\nAt Your Fingertips", "Accurate Kundali, Panchang, Dasha, Yogas — powered by Drik Ganit"),
    OnboardingPage("🤖", "AI Astrologer\n24/7", "Ask anything in Hindi or English. Get instant, personalized answers."),
    OnboardingPage("🌟", "Your Complete\nJyotish Companion", "Gochar, KP, Prashna, Varshphal, Palmistry and much more."),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutines()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrahmBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))

        // Logo
        Text("ब्रह्म AI", style = MaterialTheme.typography.headlineLarge.copy(color = BrahmGold))
        Text("Brahm AI", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(40.dp))

        // Pager
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val p = pages[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(p.emoji, style = MaterialTheme.typography.headlineLarge.copy(fontSize = androidx.compose.ui.unit.TextUnit(64f, androidx.compose.ui.unit.TextUnitType.Sp)))
                Spacer(Modifier.height(32.dp))
                Text(p.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(p.subtitle, style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground), textAlign = TextAlign.Center)
            }
        }

        // Dots
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(pages.size) { index ->
                Box(
                    Modifier
                        .clip(CircleShape)
                        .size(if (pagerState.currentPage == index) 20.dp else 8.dp, 8.dp)
                        .background(if (pagerState.currentPage == index) BrahmGold else BrahmBorder)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Next / Get Started button
        BrahmButton(
            text = if (pagerState.currentPage == pages.size - 1) "Get Started" else "Next",
            onClick = {
                if (pagerState.currentPage == pages.size - 1) {
                    onFinished()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        // Skip
        AnimatedVisibility(visible = pagerState.currentPage < pages.size - 1) {
            TextButton(onClick = onFinished) {
                Text("Skip", color = BrahmMutedForeground)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun rememberCoroutines() = rememberCoroutineScope()
