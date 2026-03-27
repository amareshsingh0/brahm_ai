package com.bimoraai.brahm.ui.gemstone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.bimoraai.brahm.core.components.BirthInputFields
import com.bimoraai.brahm.core.components.BrahmButton
import com.bimoraai.brahm.core.components.ScrollToTopFab
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private data class GemMeta(val emoji: String, val color: Color)
private val gemMeta = mapOf(
    "Ruby"        to GemMeta("🔴", Color(0xFFE53935)),
    "Pearl"       to GemMeta("⚪", Color(0xFF90CAF9)),
    "Red Coral"   to GemMeta("🟠", Color(0xFFFF7043)),
    "Emerald"     to GemMeta("💚", Color(0xFF43A047)),
    "Yellow Sapphire" to GemMeta("💛", Color(0xFFFFB300)),
    "Diamond"     to GemMeta("💎", Color(0xFF7986CB)),
    "Blue Sapphire" to GemMeta("🔵", Color(0xFF1565C0)),
    "Hessonite"   to GemMeta("🟤", Color(0xFF8D6E63)),
    "Cat's Eye"   to GemMeta("👁", Color(0xFF78909C)),
)

@Composable
fun GemstoneContent(data: JsonObject) {
    val primaryGem    = data["primary_gem"]?.let { try { it.jsonObject } catch (_: Exception) { null } }
    val supportingGems= data["supporting_gems"]?.let { el -> try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null } }
    val avoidGems     = data["avoid_gems"]?.let { el -> try { el.jsonArray.map { it.jsonPrimitive.contentOrNull ?: "" } } catch (_: Exception) { null } }
    val lagna = data["lagna"]?.let { el ->
        try { el.jsonPrimitive.contentOrNull }
        catch (_: Exception) { try { el.jsonObject["name"]?.jsonPrimitive?.contentOrNull } catch (_: Exception) { null } }
    } ?: "—"

    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Primary Gemstone ──
        item { Text("💎 Primary Gemstone", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }

        if (primaryGem != null) {
            item { PrimaryGemCard(primaryGem, lagna) }
        } else {
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                    Text("Primary gemstone data not available.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground))
                }
            }
        }

        // ── Supporting Gemstones ──
        if (!supportingGems.isNullOrEmpty()) {
            item { Text("✨ Supporting Gemstones", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
            items(supportingGems) { gem -> SupportingGemCard(gem) }
        }

        // ── Avoid List ──
        if (!avoidGems.isNullOrEmpty()) {
            item { Text("⚠️ Gemstones to Avoid", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrahmCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.2f)),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("For $lagna Lagna — avoid these gemstones as they activate malefic planets:", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        avoidGems.filter { it.isNotBlank() }.forEach { gem ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE53935)))
                                Text(gem, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFFE53935)))
                            }
                        }
                    }
                }
            }
        }
    }
    ScrollToTopFab(listState, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp))
    } // Box
}

@Composable
private fun PrimaryGemCard(gem: JsonObject, lagna: String) {
    val name      = gem["gem"]?.jsonPrimitive?.contentOrNull ?: gem["name"]?.jsonPrimitive?.contentOrNull ?: "—"
    val planet    = gem["planet"]?.jsonPrimitive?.contentOrNull ?: "—"
    val finger    = gem["finger"]?.jsonPrimitive?.contentOrNull ?: "—"
    val metal     = gem["metal"]?.jsonPrimitive?.contentOrNull ?: "—"
    val mantra    = gem["mantra"]?.jsonPrimitive?.contentOrNull ?: "—"
    val benefits  = gem["benefits"]?.let { el -> try { el.jsonArray.map { it.jsonPrimitive.contentOrNull ?: "" } } catch (_: Exception) { null } }
    val wearing   = gem["wearing_instructions"]?.jsonPrimitive?.contentOrNull ?: ""
    val meta      = gemMeta[name] ?: GemMeta("💎", BrahmGold)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, meta.color.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(60.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(meta.color.copy(alpha = 0.2f), meta.color.copy(alpha = 0.05f)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(meta.emoji, fontSize = 28.sp)
                }
                Column {
                    Text(name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("For $lagna Lagna · $planet", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            }
            HorizontalDivider(color = BrahmBorder)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Finger", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                    Text(finger, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                }
                VerticalDivider(modifier = Modifier.height(32.dp), color = BrahmBorder)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Metal", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                    Text(metal, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                }
            }
            if (mantra.isNotBlank() && mantra != "—") {
                HorizontalDivider(color = BrahmBorder)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mantra", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                    Text(mantra, style = MaterialTheme.typography.bodySmall.copy(color = BrahmGold, fontWeight = FontWeight.SemiBold))
                }
            }
            if (!benefits.isNullOrEmpty()) {
                HorizontalDivider(color = BrahmBorder)
                Text("Benefits", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                benefits.filter { it.isNotBlank() }.forEach { benefit ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("✓", fontSize = 12.sp, color = Color(0xFF43A047))
                        Text(benefit, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (wearing.isNotBlank()) {
                HorizontalDivider(color = BrahmBorder)
                Text("Wearing Instructions", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                Text(wearing, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.8f), fontSize = 12.sp))
            }
        }
    }
}

@Composable
private fun SupportingGemCard(gem: JsonObject) {
    val name   = gem["gem"]?.jsonPrimitive?.contentOrNull ?: gem["name"]?.jsonPrimitive?.contentOrNull ?: "—"
    val planet = gem["planet"]?.jsonPrimitive?.contentOrNull ?: "—"
    val lord   = gem["lord"]?.jsonPrimitive?.contentOrNull ?: ""
    val meta   = gemMeta[name] ?: GemMeta("💎", BrahmGold)

    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(meta.color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Text(meta.emoji, fontSize = 22.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text("$planet${if (lord.isNotBlank()) " · $lord Lord" else ""}", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
            }
        }
    }
}

@Composable
fun GemstoneInputForm(
    name: String, dob: String, tob: String, pob: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onDobChange: (String) -> Unit,
    onTobChange: (String) -> Unit,
    onPobChange: (String) -> Unit,
    onCitySelected: (City) -> Unit,
    onCalculate: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter Birth Details", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    BirthInputFields(
                        name = name, onNameChange = onNameChange,
                        dob = dob, onDobChange = onDobChange,
                        tob = tob, onTobChange = onTobChange,
                        pob = pob, onPobChange = onPobChange,
                        onCitySelected = onCitySelected,
                    )
                    if (error != null) Text(error, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)))
                    BrahmButton(text = "Get Gemstone Recommendations", onClick = onCalculate)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7))) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Text("💎", fontSize = 24.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Vedic Gemology", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = BrahmGold))
                        Text("Vedic astrology recommends specific gemstones based on your Lagna (Ascendant) lord and benefic planets. The primary stone strengthens your Lagna lord while supporting stones activate 4th, 9th, and 10th house lords.", style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground.copy(alpha = 0.8f)))
                    }
                }
            }
        }
    }
}
