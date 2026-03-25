package com.bimoraai.brahm.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.theme.*

// plan: "free" | "standard" | "premium"
private data class PlanInfo(
    val key: String,
    val label: String,
    val subtitle: String,
    val priceMonthly: Int,          // INR/month
    val priceYearly: Int,           // INR/year
    val gradStart: Color,
    val gradEnd: Color,
    val accentColor: Color,
    val icon: ImageVector,
    val features: List<String>,     // first item can be "Everything in X, plus:" header
)

private val PLANS = listOf(
    PlanInfo(
        key          = "free",
        label        = "Free",
        subtitle     = "Get started with daily guidance",
        priceMonthly = 0,
        priceYearly  = 0,
        gradStart    = Color(0xFF9E9E9E),
        gradEnd      = Color(0xFF616161),
        accentColor  = Color(0xFF9E9E9E),
        icon         = Icons.Default.Person,
        features     = listOf(
            "Daily Horoscope",
            "Today's Panchang",
            "Festival Calendar",
            "Palmistry AI Analysis",
            "Basic Kundali (view only)",
            "5 AI Chat messages / day",
        ),
    ),
    PlanInfo(
        key          = "standard",
        label        = "Standard",
        subtitle     = "Perfect for regular users",
        priceMonthly = 199,
        priceYearly  = 1999,
        gradStart    = Color(0xFF5C6BC0),
        gradEnd      = Color(0xFF3949AB),
        accentColor  = Color(0xFF5C6BC0),
        icon         = Icons.Default.Star,
        features     = listOf(
            "Everything in Free, plus:",
            "Unlimited AI Chat",
            "Full Kundali + All 7 Tabs",
            "Gochar Transits",
            "Compatibility Analysis",
            "Muhurta Finder",
            "Save unlimited charts",
        ),
    ),
    PlanInfo(
        key          = "premium",
        label        = "Premium",
        subtitle     = "Full access to everything",
        priceMonthly = 399,
        priceYearly  = 3999,
        gradStart    = Color(0xFFD4A017),
        gradEnd      = Color(0xFF8B5E00),
        accentColor  = BrahmGold,
        icon         = Icons.Default.WorkspacePremium,
        features     = listOf(
            "Everything in Standard, plus:",
            "Gemstone Recommendations",
            "Dosha + Sade Sati Reports",
            "Varshphal Annual Chart",
            "Prashna Kundali",
            "KP System",
            "Vedic Scripture Library",
        ),
    ),
)

private fun yearlyDiscount(monthly: Int, yearly: Int): Int {
    if (monthly == 0) return 0
    return ((monthly * 12 - yearly).toFloat() / (monthly * 12) * 100).toInt()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    navController: NavController,
    vm: ProfileViewModel = hiltViewModel(),
) {
    val user by vm.user.collectAsState()
    val currentPlan = user?.plan ?: "free"
    var isYearly by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Upgrade Plan", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
                        Text("Choose the plan right for you", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    }
                },
                navigationIcon = {
                    // X close button (top-left)
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    // No additional actions needed
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrahmCard),
            )
        },
        containerColor = BrahmBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Monthly / Yearly toggle ──────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp),
                color = BrahmCard,
                tonalElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                ) {
                    PeriodTab(
                        label    = "Monthly",
                        selected = !isYearly,
                        modifier = Modifier.weight(1f),
                        onClick  = { isYearly = false },
                    )
                    PeriodTab(
                        label    = "Yearly · Save 17%",
                        selected = isYearly,
                        modifier = Modifier.weight(1f),
                        onClick  = { isYearly = true },
                        badgeColor = Color(0xFF43A047),
                    )
                }
            }

            // ── Plan cards ──────────────────────────────────────────────────────
            PLANS.forEach { plan ->
                PlanCard(
                    plan        = plan,
                    isCurrent   = plan.key == currentPlan,
                    isYearly    = isYearly,
                )
            }

            // ── Trust row ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                listOf("🔒 Cashfree", "⚡ UPI / Cards", "🔁 Cancel Anytime").forEach { badge ->
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PeriodTab(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    badgeColor: Color = Color.Transparent,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50.dp),
        color = if (selected) BrahmGold else Color.Transparent,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 9.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else BrahmMutedForeground,
                ),
            )
        }
    }
}

@Composable
private fun PlanCard(plan: PlanInfo, isCurrent: Boolean, isYearly: Boolean) {
    val borderColor = if (isCurrent) plan.accentColor else BrahmBorder
    val borderWidth = if (isCurrent) 2.dp else 1.dp

    val displayPrice = if (isYearly) plan.priceYearly / 12 else plan.priceMonthly
    val discount     = yearlyDiscount(plan.priceMonthly, plan.priceYearly)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp)),
        shape    = RoundedCornerShape(16.dp),
        color    = BrahmCard,
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // ── Header row ────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Brush.linearGradient(listOf(plan.gradStart, plan.gradEnd))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(plan.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(plan.label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        if (plan.key == "standard") {
                            Surface(shape = RoundedCornerShape(20.dp), color = plan.accentColor.copy(alpha = 0.12f)) {
                                Text(
                                    "Popular",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(color = plan.accentColor, fontWeight = FontWeight.SemiBold),
                                )
                            }
                        }
                        if (isCurrent) {
                            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF43A047).copy(alpha = 0.12f)) {
                                Text(
                                    "Current",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF43A047), fontWeight = FontWeight.SemiBold),
                                )
                            }
                        }
                    }
                    Text(plan.subtitle, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            }

            // ── Price box (like Claude) ────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = plan.accentColor.copy(alpha = 0.07f),
                modifier = Modifier.fillMaxWidth().border(1.dp, plan.accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isCurrent || (!isYearly && plan.priceMonthly > 0) || (isYearly && plan.priceYearly > 0)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(plan.accentColor),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Column {
                        if (plan.priceMonthly == 0) {
                            Text("₹0", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Free forever", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        } else {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("₹$displayPrice", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                                Text(" / month", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground), modifier = Modifier.padding(bottom = 4.dp))
                            }
                            if (isYearly) {
                                Text(
                                    "₹${plan.priceYearly} / year · Save $discount%",
                                    style = MaterialTheme.typography.bodySmall.copy(color = plan.accentColor),
                                )
                            } else {
                                Text("Billed monthly", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                            }
                        }
                    }
                }
            }

            // ── CTA button ────────────────────────────────────────────────────
            if (!isCurrent && plan.key != "free") {
                Button(
                    onClick = { /* TODO: open payment flow */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = plan.gradStart,
                        contentColor   = Color.White,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Get ${plan.label} plan", fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = BrahmBorder.copy(alpha = 0.5f))

            // ── Features ──────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                plan.features.forEach { feature ->
                    val isHeader = feature.endsWith("plus:")
                    if (isHeader) {
                        Text(
                            feature,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF43A047),
                                modifier = Modifier.size(15.dp),
                            )
                            Text(feature, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        }
                    }
                }
            }
        }
    }
}
