package com.bimoraai.brahm.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimoraai.brahm.core.theme.BrahmGold
import com.bimoraai.brahm.core.theme.BrahmMutedForeground

// ── Feature display metadata ──────────────────────────────────────────────────

private val FEATURE_EMOJI = mapOf(
    "ai_chat"        to "💬",
    "kundali"        to "🔮",
    "palm_reading"   to "🤚",
    "live_sky_today" to "🌌",
    "muhurta"        to "⏰",
    "horoscope_ai"   to "♈",
    "compatibility"  to "💑",
    "gochar"         to "🪐",
    "kp_system"      to "📐",
    "dosha"          to "⚠️",
    "gemstone"       to "💎",
    "nakshatra"      to "⭐",
    "prashna"        to "❓",
    "varshphal"      to "📅",
    "rectification"  to "🎯",
    "pdf_export"     to "📄",
    "chat_history"   to "📜",
)

private val FEATURE_TAGLINE = mapOf(
    "ai_chat"        to "Ask anything about your Vedic chart",
    "kundali"        to "Full birth chart + 7-tab deep analysis",
    "palm_reading"   to "AI-powered palmistry reading",
    "live_sky_today" to "Today's cosmic influence on your chart",
    "muhurta"        to "Find the most auspicious timing",
    "horoscope_ai"   to "AI-personalized daily horoscope",
    "compatibility"  to "Kundali matching & compatibility score",
    "gochar"         to "Current planetary transits over your chart",
    "kp_system"      to "Krishnamurti Paddhati predictions",
    "dosha"          to "Dosha detection and remedies",
    "gemstone"       to "Personalized gemstone recommendations",
    "nakshatra"      to "Your nakshatra's deep influence",
    "prashna"        to "Horary astrology — answers right now",
    "varshphal"      to "Annual solar return chart",
    "rectification"  to "Find your exact birth time",
    "pdf_export"     to "Download your kundali as PDF",
    "chat_history"   to "Access all your past AI conversations",
)

// ── UpgradeSheet ──────────────────────────────────────────────────────────────

/**
 * Bottom sheet shown when a user tries to access a locked feature.
 * Shows the feature name, emoji, tagline, and an upgrade CTA.
 *
 * Usage:
 *   var showUpgrade by remember { mutableStateOf(false) }
 *   if (!subVm.hasFeature("kundali")) {
 *       showUpgrade = true
 *   }
 *   if (showUpgrade) {
 *       UpgradeSheet(featureKey = "kundali", onDismiss = { showUpgrade = false })
 *   }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeSheet(
    featureKey:  String,
    featureName: String = featureKey.replace("_", " ").replaceFirstChar { it.uppercase() },
    isExpired:   Boolean = false,
    onDismiss:   () -> Unit,
    onUpgrade:   () -> Unit = {},
) {
    val emoji   = FEATURE_EMOJI[featureKey]   ?: "🔒"
    val tagline = FEATURE_TAGLINE[featureKey] ?: "Upgrade to unlock this feature"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFE5E7EB)),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Emoji icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF8E7)),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 32.sp)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (isExpired) "$featureName — Plan Expired" else "$featureName",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (isExpired)
                    "Your subscription has expired. Renew to continue."
                else
                    tagline,
                style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))

            // What's included card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFFBEB),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8D5A3)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = BrahmGold, modifier = Modifier.size(14.dp))
                        Text("What you unlock", style = MaterialTheme.typography.labelSmall.copy(
                            color = BrahmGold, fontWeight = FontWeight.SemiBold,
                        ))
                    }
                    listOf(
                        "AI Chat — ask anything about your chart",
                        "Kundali with full 7-tab analysis",
                        "Gochar transits & daily insights",
                        "Palmistry, Muhurta, Compatibility & more",
                    ).forEach { item ->
                        Text(
                            "• $item",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF92400E)),
                        )
                    }
                    Text(
                        "Plans starting at ₹299/month",
                        style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Upgrade CTA
            Button(
                onClick  = { onUpgrade(); onDismiss() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BrahmGold),
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isExpired) "Renew Plan" else "Upgrade Now",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Maybe later", color = BrahmMutedForeground, fontSize = 13.sp)
            }
        }
    }
}

// ── Daily limit sheet ──────────────────────────────────────────────────────────

/**
 * Shown when user hits their daily message limit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLimitSheet(
    used:      Int,
    limit:     Int,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("⚡", fontSize = 40.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Daily Limit Reached",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "You've used $used/$limit messages today.\nYour limit resets at midnight IST.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = BrahmMutedForeground, textAlign = TextAlign.Center,
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick  = { onUpgrade(); onDismiss() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BrahmGold),
            ) {
                Text("Upgrade for More", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("OK, come back tomorrow", color = BrahmMutedForeground, fontSize = 13.sp)
            }
        }
    }
}
