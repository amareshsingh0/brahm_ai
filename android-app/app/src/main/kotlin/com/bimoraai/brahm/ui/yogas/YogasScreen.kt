package com.bimoraai.brahm.ui.yogas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.BirthInputFields
import com.bimoraai.brahm.core.components.BrahmButton
import com.bimoraai.brahm.core.components.BrahmLoadingSpinner
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ── Effect colours (matches website effectStyles) ─────────────────────────────
private data class EffectStyle(val badgeBg: Color, val badgeFg: Color, val border: Color)

private fun categoryToEffect(category: String): EffectStyle = when (category.lowercase()) {
    "adversity", "karma" -> EffectStyle(
        badgeBg = Color(0xFFE53935).copy(alpha = 0.12f),
        badgeFg = Color(0xFFE53935),
        border  = Color(0xFFE53935).copy(alpha = 0.25f),
    )
    "marriage" -> EffectStyle(
        badgeBg = Color(0xFFFF8F00).copy(alpha = 0.12f),
        badgeFg = Color(0xFFFF8F00),
        border  = Color(0xFFFF8F00).copy(alpha = 0.25f),
    )
    else -> EffectStyle(  // power, wealth, intellect, spiritual, character
        badgeBg = Color(0xFF43A047).copy(alpha = 0.12f),
        badgeFg = Color(0xFF43A047),
        border  = Color(0xFF43A047).copy(alpha = 0.25f),
    )
}

private fun strengthColor(strength: String): Color = when {
    strength.contains("Very Strong", ignoreCase = true) -> Color(0xFF43A047)
    strength.contains("Strong",      ignoreCase = true) -> BrahmGold
    strength.contains("Moderate",    ignoreCase = true) -> Color(0xFF1976D2)
    else                                                 -> Color(0xFFFF8F00)
}

@Composable
fun YogasScreen(
    navController: NavController,
    vm: YogasViewModel = hiltViewModel(),
) {
    val yogas     by vm.yogas.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error     by vm.error.collectAsState()
    val hasData   by vm.hasData.collectAsState()
    val name      by vm.name.collectAsState()
    val dob       by vm.dob.collectAsState()
    val tob       by vm.tob.collectAsState()
    val pob       by vm.pob.collectAsState()

    SwipeBackLayout(navController) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Yogas", fontWeight = FontWeight.Bold, color = BrahmGold) },
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
                    state           = listState,
                    modifier        = Modifier.fillMaxSize().background(BrahmBackground).padding(padding),
                    contentPadding  = PaddingValues(horizontal = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {

                    // ── Page header ───────────────────────────────────────────
                    item {
                        Column(Modifier.padding(bottom = 20.dp)) {
                            Text(
                                "Planetary Yogas",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color      = BrahmForeground,
                                ),
                            )
                            Text(
                                "Classical combinations present in your birth chart",
                                style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                            )
                        }
                    }

                    // ── Input form ────────────────────────────────────────────
                    if (!hasData) {
                        item {
                            Card(
                                shape  = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = BrahmCard),
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Enter Birth Details", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    BirthInputFields(
                                        name          = name, onNameChange = { vm.name.value = it },
                                        dob           = dob,  onDobChange  = { vm.dob.value  = it },
                                        tob           = tob,  onTobChange  = { vm.tob.value  = it },
                                        pob           = pob,  onPobChange  = { vm.pob.value  = it },
                                        onCitySelected = { city ->
                                            vm.pob.value = city.name
                                            vm.lat.value = city.lat
                                            vm.lon.value = city.lon
                                            vm.tz.value  = city.tz.toString()
                                        },
                                    )
                                    if (error != null) {
                                        Text(error!!, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)))
                                    }
                                    BrahmButton(text = "Calculate Yogas", onClick = { vm.calculate() })
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    // ── Loading ───────────────────────────────────────────────
                    if (isLoading) {
                        item { BrahmLoadingSpinner(modifier = Modifier.fillMaxWidth().height(120.dp)) }
                    }

                    // ── Results ───────────────────────────────────────────────
                    if (hasData && yogas != null && !isLoading) {
                        val yogaList = yogas!!.get("yogas")?.let { el ->
                            try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
                        }

                        if (yogaList.isNullOrEmpty()) {
                            item {
                                Card(
                                    shape  = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = BrahmCard),
                                ) {
                                    Text(
                                        "No yoga data found.",
                                        modifier = Modifier.padding(16.dp),
                                        style    = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground),
                                    )
                                }
                            }
                        } else {
                            val present = yogaList.filter { it.isPresent() }
                            val absent  = yogaList.filter { !it.isPresent() }

                            // ── Present heading ───────────────────────────────
                            item {
                                Text(
                                    "PRESENT YOGAS  (${present.size})",
                                    style    = MaterialTheme.typography.labelSmall.copy(
                                        color       = BrahmGold,
                                        fontWeight  = FontWeight.SemiBold,
                                        letterSpacing = 1.sp,
                                        fontSize    = 11.sp,
                                    ),
                                    modifier = Modifier.padding(bottom = 10.dp),
                                )
                            }

                            items(present) { yoga ->
                                YogaCard(yoga = yoga, dimmed = false)
                                Spacer(Modifier.height(12.dp))
                            }

                            // ── Absent heading ────────────────────────────────
                            if (absent.isNotEmpty()) {
                                item {
                                    Text(
                                        "NOT TRIGGERED  (${absent.size})",
                                        style    = MaterialTheme.typography.labelSmall.copy(
                                            color       = BrahmMutedForeground,
                                            fontWeight  = FontWeight.SemiBold,
                                            letterSpacing = 1.sp,
                                            fontSize    = 11.sp,
                                        ),
                                        modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
                                    )
                                }
                                items(absent) { yoga ->
                                    YogaCard(yoga = yoga, dimmed = true)
                                    Spacer(Modifier.height(10.dp))
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }

            }
        }
    }
}

// ── Yoga Card — matches website cosmic-card style ─────────────────────────────

@Composable
private fun YogaCard(yoga: JsonObject, dimmed: Boolean) {
    val name     = yoga["name"]?.jsonPrimitive?.contentOrNull     ?: "—"
    val category = yoga["category"]?.jsonPrimitive?.contentOrNull ?: ""
    val strength = yoga["strength"]?.jsonPrimitive?.contentOrNull ?: ""
    val desc     = yoga["desc"]?.jsonPrimitive?.contentOrNull     ?: yoga["description"]?.jsonPrimitive?.contentOrNull ?: ""
    val mantra   = yoga["mantra"]?.jsonPrimitive?.contentOrNull   ?: ""
    val gemstone = yoga["gemstone"]?.jsonPrimitive?.contentOrNull ?: ""
    val deity    = yoga["deity"]?.jsonPrimitive?.contentOrNull    ?: ""
    val remedy   = yoga["remedy"]?.jsonPrimitive?.contentOrNull   ?: ""

    val style   = categoryToEffect(category)
    val alpha   = if (dimmed) 0.45f else 1f
    var expanded by remember { mutableStateOf(false) }

    val hasDetails = !dimmed && (mantra.isNotBlank() || gemstone.isNotBlank() || remedy.isNotBlank() || deity.isNotBlank())

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BrahmCard.copy(alpha = alpha))
            .border(1.dp, style.border.copy(alpha = style.border.alpha * alpha), RoundedCornerShape(12.dp))
            .then(if (hasDetails) Modifier.clickable { expanded = !expanded } else Modifier)
            .padding(16.dp),
    ) {
        // ── Top row: Name + Effect badge ──────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Top,
        ) {
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color      = BrahmForeground.copy(alpha = alpha),
                    ),
                )
                // Strength below name — like website's sanskritName row
                if (strength.isNotBlank()) {
                    Text(
                        strength,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color    = strengthColor(strength).copy(alpha = alpha),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
            // Category badge — like website's effect badge (rounded-full)
            if (category.isNotBlank()) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(style.badgeBg.copy(alpha = style.badgeBg.alpha * alpha))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color    = style.badgeFg.copy(alpha = alpha),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }

        // ── Description ───────────────────────────────────────────────
        if (desc.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall.copy(
                    color      = BrahmMutedForeground.copy(alpha = alpha),
                    lineHeight = 18.sp,
                    fontSize   = 12.sp,
                ),
            )
        }

        // ── Chips row: mantra + gemstone as pills (like website planet chips) ─
        if (!dimmed && (mantra.isNotBlank() || gemstone.isNotBlank())) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                if (gemstone.isNotBlank()) {
                    Chip(label = gemstone, bg = BrahmMuted.copy(alpha = 0.5f), fg = BrahmMutedForeground)
                }
                if (deity.isNotBlank()) {
                    Chip(label = deity, bg = BrahmGold.copy(alpha = 0.1f), fg = BrahmGold.copy(alpha = 0.8f))
                }
            }
        }

        // ── Expand arrow ──────────────────────────────────────────────
        if (hasDetails) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    if (expanded) "Less" else "Mantra & Remedy",
                    style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontSize = 10.sp),
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint     = BrahmGold,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // ── Expanded: Mantra + Remedy ─────────────────────────────────
        if (expanded) {
            HorizontalDivider(color = BrahmBorder, modifier = Modifier.padding(vertical = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (mantra.isNotBlank()) {
                    DetailRow(label = "Mantra", value = mantra, valueColor = Color(0xFF7C6FCD))
                }
                if (remedy.isNotBlank()) {
                    DetailRow(label = "Remedy", value = remedy, valueColor = BrahmForeground)
                }
                if (gemstone.isNotBlank()) {
                    DetailRow(label = "Gemstone", value = gemstone, valueColor = BrahmGold)
                }
                if (deity.isNotBlank()) {
                    DetailRow(label = "Deity", value = deity, valueColor = BrahmMutedForeground)
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, bg: Color, fg: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(color = fg, fontSize = 10.sp),
            maxLines = 1,
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                color    = BrahmMutedForeground,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            ),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(
                color      = valueColor,
                fontSize   = 12.sp,
                lineHeight = 17.sp,
            ),
        )
    }
}

private fun JsonObject.isPresent(): Boolean {
    val v = this["present"]?.jsonPrimitive?.contentOrNull ?: return false
    return v == "true" || v == "1" || v.toBooleanStrictOrNull() == true
}
