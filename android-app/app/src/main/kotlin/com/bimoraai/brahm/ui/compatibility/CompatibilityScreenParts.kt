package com.bimoraai.brahm.ui.compatibility

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimoraai.brahm.core.components.BirthInputFields
import com.bimoraai.brahm.core.components.BrahmButton
import com.bimoraai.brahm.core.components.ScrollToTopFab
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.Canvas

// ── Helpers ────────────────────────────────────────────────────────────────────

private fun JsonObject.str(key: String) = this[key]?.jsonPrimitive?.contentOrNull ?: ""
private fun JsonObject.flt(key: String) = this[key]?.jsonPrimitive?.contentOrNull?.toFloatOrNull() ?: 0f
private fun JsonObject.bool(key: String) = this[key]?.jsonPrimitive?.booleanOrNull ?: false
private fun JsonObject.obj(key: String) = try { this[key]?.jsonObject } catch (_: Exception) { null }
private fun JsonObject.arr(key: String) = try { this[key]?.jsonArray } catch (_: Exception) { null }

private fun scoreColor(score: Float, max: Float): Color {
    val p = if (max > 0) score / max else 0f
    return when {
        p >= 0.75f -> Color(0xFF43A047)
        p >= 0.5f  -> Color(0xFFF59E0B)
        p > 0f     -> Color(0xFFF97316)
        else       -> Color(0xFFEF4444)
    }
}

private fun scoreLabel(score: Float, max: Float): String {
    val p = if (max > 0) score / max else 0f
    return when {
        p == 1f    -> "Perfect"
        p >= 0.75f -> "Good"
        p >= 0.5f  -> "Average"
        p > 0f     -> "Weak"
        else       -> "None"
    }
}

private fun verdictColor(pct: Float) = when {
    pct >= 72 -> Color(0xFF10B981)
    pct >= 55 -> Color(0xFFF59E0B)
    pct >= 36 -> Color(0xFFF97316)
    else      -> Color(0xFFEF4444)
}

// ── Status Badge ───────────────────────────────────────────────────────────────

@Composable
private fun StatusBadge(label: String, type: String) {
    val (bg, fg) = when (type) {
        "green" -> Color(0xFF10B981).copy(alpha = 0.15f) to Color(0xFF10B981)
        "red"   -> Color(0xFFEF4444).copy(alpha = 0.15f) to Color(0xFFEF4444)
        else    -> Color(0xFFF59E0B).copy(alpha = 0.15f) to Color(0xFFF59E0B)
    }
    Box(
        Modifier.clip(RoundedCornerShape(50.dp)).background(bg).padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = 10.sp, color = fg, fontWeight = FontWeight.Medium)
    }
}

// ── Score Circle (SVG-equivalent using Canvas) ─────────────────────────────────

@Composable
private fun ScoreCircle(pct: Float, totalScore: Float, maxScore: Float) {
    val color = verdictColor(pct)
    val animPct by animateFloatAsState(
        targetValue = pct / 100f, animationSpec = tween(1400, easing = FastOutSlowInEasing), label = "ring"
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val radius = (size.minDimension - stroke) / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f
            // Track
            drawCircle(color = Color(0xFF334155), style = Stroke(width = stroke), radius = radius)
            // Arc
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animPct,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${pct.toInt()}%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BrahmGold)
            Text("${totalScore.toInt()}/36", fontSize = 11.sp, color = BrahmMutedForeground)
        }
    }
}

// ── Guna Bar ───────────────────────────────────────────────────────────────────

@Composable
private fun GunaBar(guna: JsonObject, delay: Int) {
    val name  = guna.str("name")
    val score = guna.flt("score")
    val max   = guna.flt("max").let { if (it == 0f) guna.flt("max_score") else it }
    val desc  = guna.str("desc").ifBlank { guna.str("description") }
    val interp = guna.str("interpretation")
    val color = scoreColor(score, max)
    val pct   = if (max > 0) score / max else 0f
    var open by remember { mutableStateOf(false) }
    val animPct by animateFloatAsState(targetValue = pct, animationSpec = tween(500 + delay * 40, easing = FastOutSlowInEasing), label = "bar")

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
        modifier = Modifier.fillMaxWidth().then(
            if (interp.isNotBlank()) Modifier.clickable { open = !open } else Modifier
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(scoreLabel(score, max), fontSize = 11.sp, color = color)
                            Text("${score.toInt()}/${max.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
                        }
                    }
                    if (desc.isNotBlank()) {
                        Text(desc, fontSize = 11.sp, color = BrahmMutedForeground, modifier = Modifier.padding(top = 2.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(BrahmBorder)
                    ) {
                        Box(
                            Modifier.fillMaxWidth(animPct).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(color)
                        )
                    }
                }
                if (interp.isNotBlank()) {
                    Icon(
                        if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null, tint = BrahmMutedForeground, modifier = Modifier.size(18.dp)
                    )
                }
            }
            AnimatedVisibility(visible = open && interp.isNotBlank()) {
                Text(
                    interp, fontSize = 11.sp, color = BrahmMutedForeground,
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

// ── Dosha Card ─────────────────────────────────────────────────────────────────

@Composable
private fun DoshaCard(dosha: JsonObject) {
    val name     = dosha.str("name")
    val present  = dosha.bool("present")
    val severity = dosha.str("severity")
    val note     = dosha.str("note")
    val cancel   = dosha.str("cancellation")
    val isHigh   = present && severity == "High"
    var open by remember { mutableStateOf(false) }

    val bg     = if (isHigh) Color(0xFFEF4444).copy(0.08f) else if (present) Color(0xFFF59E0B).copy(0.08f) else Color(0xFF10B981).copy(0.08f)
    val border = if (isHigh) Color(0xFFEF4444).copy(0.25f) else if (present) Color(0xFFF59E0B).copy(0.25f) else Color(0xFF10B981).copy(0.25f)
    val color  = if (isHigh) Color(0xFFEF4444) else if (present) Color(0xFFF59E0B) else Color(0xFF10B981)
    val icon: ImageVector = if (isHigh) Icons.Default.Close else if (present) Icons.Default.Warning else Icons.Default.Check

    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable { open = !open }
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.clip(RoundedCornerShape(50.dp)).background(color.copy(0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(if (present) severity else "None", fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
                    }
                    Icon(
                        if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null, tint = BrahmMutedForeground, modifier = Modifier.size(16.dp)
                    )
                }
            }
            AnimatedVisibility(visible = open) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (note.isNotBlank()) Text(note, fontSize = 11.sp, color = BrahmMutedForeground, lineHeight = 16.sp)
                    if (cancel.isNotBlank()) Text("✓ $cancel", fontSize = 11.sp, color = Color(0xFF10B981), lineHeight = 16.sp)
                }
            }
        }
    }
}

// ── Life Area Bar ──────────────────────────────────────────────────────────────

@Composable
private fun LifeAreaBar(area: JsonObject, delay: Int) {
    val name  = area.str("area")
    val icon  = area.str("icon")
    val score = area.flt("score")
    val label = area.str("label")
    val color = when (label) {
        "Excellent" -> Color(0xFF10B981)
        "Good"      -> Color(0xFFF59E0B)
        "Average"   -> Color(0xFFF97316)
        else        -> Color(0xFFEF4444)
    }
    val animScore by animateFloatAsState(targetValue = score / 100f, animationSpec = tween(500 + delay * 40, easing = FastOutSlowInEasing), label = "life")

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(icon, fontSize = 16.sp, modifier = Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(BrahmBorder)) {
                Box(Modifier.fillMaxWidth(animScore).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(color))
            }
        }
        Text("${score.toInt()}%", fontSize = 11.sp, color = BrahmMutedForeground, modifier = Modifier.width(32.dp))
    }
}

// ── Nakshatra Tab ─────────────────────────────────────────────────────────────

@Composable
private fun NakshatraTab(data: JsonObject, nameA: String, nameB: String) {
    val naksA = data.str("nakshatra_a")
    val naksB = data.str("nakshatra_b")
    val ganaA = data.str("gana_a")
    val ganaB = data.str("gana_b")
    val nadiA = data.str("nadi_a").split(" ").firstOrNull() ?: ""
    val nadiB = data.str("nadi_b").split(" ").firstOrNull() ?: ""
    val varnaA = data.str("varna_a")
    val varnaB = data.str("varna_b")
    val yoniA  = data.str("yoni_a")
    val yoniB  = data.str("yoni_b")
    val nadiSame = nadiA.isNotBlank() && nadiA == nadiB
    val rajju  = data.obj("rajju_dosha")
    val vedha  = data.obj("vedha_dosha")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Nakshatra pair cards
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(nameA to naksA, nameB to naksB).forEach { (nm, naks) ->
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BrahmCard),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(nm, fontSize = 11.sp, color = BrahmGold, fontWeight = FontWeight.SemiBold)
                        Text(naks, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Box(Modifier.clip(RoundedCornerShape(50.dp)).background(BrahmGold.copy(0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            val gana = if (nm == nameA) ganaA else ganaB
                            Text("Gana: $gana", fontSize = 10.sp, color = BrahmGold)
                        }
                    }
                }
            }
        }

        // Gana
        DoshaSection(
            title = "Gana (Nature Match)",
            valueA = ganaA, valueB = ganaB,
            isGood = ganaA == ganaB || (ganaA in listOf("Deva", "Manushya") && ganaB in listOf("Deva", "Manushya")),
            note = "Deva + Manushya = 5 pts · Same Gana = 6 pts · Rakshasa with others = 0 pts",
        )

        // Nadi Dosha
        val nadiColor = if (nadiSame) "red" else "green"
        SectionCard(title = "Nadi Dosha", borderColor = if (nadiSame) Color(0xFFEF4444).copy(0.25f) else Color(0xFF10B981).copy(0.25f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (nadiSame) Icons.Default.Close else Icons.Default.Check,
                    contentDescription = null,
                    tint = if (nadiSame) Color(0xFFEF4444) else Color(0xFF10B981),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("$nadiA × $nadiB", fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                StatusBadge(if (nadiSame) "Nadi Dosha" else "No Dosha", nadiColor)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (nadiSame) "Both share same Nadi — serious dosha affecting health & progeny. Remedy: Nadi dosha nivarana puja."
                else "Different Nadis — auspicious for health, compatibility and progeny.",
                fontSize = 11.sp, color = BrahmMutedForeground, lineHeight = 15.sp,
            )
        }

        // Rajju Dosha
        rajju?.let { r ->
            val present = r.bool("present")
            SectionCard(title = "Rajju Dosha", borderColor = if (present) Color(0xFFEF4444).copy(0.25f) else Color(0xFF10B981).copy(0.25f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (present) Icons.Default.Close else Icons.Default.Check, null,
                        tint = if (present) Color(0xFFEF4444) else Color(0xFF10B981), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Rajju Dosha", fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                    StatusBadge(if (present) r.str("severity").ifBlank { "Present" } else "Absent", if (present) "red" else "green")
                }
                val note = r.str("note")
                if (note.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(note, fontSize = 11.sp, color = BrahmMutedForeground, lineHeight = 15.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Vedha Dosha
        vedha?.let { v ->
            val present = v.bool("present")
            SectionCard(title = "Vedha Dosha", borderColor = if (present) Color(0xFFF59E0B).copy(0.25f) else Color(0xFF10B981).copy(0.25f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (present) Icons.Default.Warning else Icons.Default.Check, null,
                        tint = if (present) Color(0xFFF59E0B) else Color(0xFF10B981), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Vedha Dosha", fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                    StatusBadge(if (present) "Present" else "Absent", if (present) "amber" else "green")
                }
                val note = v.str("note")
                if (note.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(note, fontSize = 11.sp, color = BrahmMutedForeground, lineHeight = 15.sp)
                }
            }
        }

        // Yoni
        if (yoniA.isNotBlank() && yoniB.isNotBlank()) {
            val yoniGood = yoniA.split("(").first().trim() == yoniB.split("(").first().trim()
            DoshaSection("Yoni (Instinct Compatibility)", yoniA, yoniB, yoniGood, "Same/friendly yoni = deep natural harmony. Enemy yoni = friction.")
        }

        // Varna
        val varnaRank = mapOf("Brahmin" to 4, "Kshatriya" to 3, "Vaishya" to 2, "Shudra" to 1)
        val varnaGood = (varnaRank[varnaA] ?: 0) >= (varnaRank[varnaB] ?: 0)
        DoshaSection("Varna (Spiritual Temperament)", varnaA, varnaB, varnaGood, "Groom's varna should be equal or higher. Spiritual temperament by Moon nakshatra.")

        Text(
            "* Nakshatra matching is one factor — consult a qualified Jyotishi for complete guidance.",
            fontSize = 10.sp, color = BrahmMutedForeground.copy(0.6f), lineHeight = 14.sp,
        )
    }
}

@Composable
private fun SectionCard(title: String, borderColor: Color = BrahmBorder, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title.uppercase(), fontSize = 10.sp, color = BrahmMutedForeground, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun DoshaSection(title: String, valueA: String, valueB: String, isGood: Boolean, note: String) {
    val color  = if (isGood) Color(0xFF10B981) else Color(0xFFEF4444)
    val border = color.copy(0.25f)
    SectionCard(title, border) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isGood) Icons.Default.Check else Icons.Default.Close, null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("$valueA × $valueB", fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            StatusBadge(if (isGood) "Compatible" else "Incompatible", if (isGood) "green" else "red")
        }
        Spacer(Modifier.height(6.dp))
        Text(note, fontSize = 11.sp, color = BrahmMutedForeground, lineHeight = 15.sp)
    }
}

// ── Main CompatibilityContent ──────────────────────────────────────────────────

@Composable
fun CompatibilityContent(data: JsonObject, onEdit: () -> Unit = {}) {
    val totalScore  = data.flt("total_score").let { if (it == 0f) data.flt("score") else it }
    val pct         = data.flt("percentage").let { if (it == 0f) (totalScore / 36f * 100f) else it }
    val verdict     = data.str("verdict")
    val verdictDetail = data.str("verdict_detail")
    val nameA       = data.str("name_a").ifBlank { data.obj("person_a")?.str("name") ?: "Person A" }
    val nameB       = data.str("name_b").ifBlank { data.obj("person_b")?.str("name") ?: "Person B" }
    val vColor      = verdictColor(pct)

    val gunas       = data.arr("gunas")?.mapNotNull { try { it.jsonObject } catch (_: Exception) { null } } ?: emptyList()
    val lifeAreas   = data.arr("life_areas")?.mapNotNull { try { it.jsonObject } catch (_: Exception) { null } } ?: emptyList()
    val doshas      = data.arr("dosha_summary")?.mapNotNull { try { it.jsonObject } catch (_: Exception) { null } } ?: emptyList()
    val strengths   = data.arr("strengths")?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
    val challenges  = data.arr("challenges")?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
    val mangalA     = data.obj("mangal_dosha")?.bool("person_a") ?: false
    val mangalB     = data.obj("mangal_dosha")?.bool("person_b") ?: false

    val activeDoshas   = doshas.filter { it.bool("present") }
    val inactiveDoshas = doshas.filter { !it.bool("present") }

    var activeTab by remember { mutableStateOf(0) } // 0=Milan, 1=Nakshatra
    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Header: names + Edit ──
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "$nameA × $nameB",
                            fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text("Kundali Milan Report", fontSize = 12.sp, color = BrahmMutedForeground)
                    }
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrahmBorder),
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = BrahmMutedForeground)
                        Spacer(Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp, color = BrahmMutedForeground)
                    }
                }
            }

            // ── Tabs ──
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(BrahmCard),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    listOf("Ashtakoot Milan", "Nakshatra Match").forEachIndexed { idx, label ->
                        val selected = activeTab == idx
                        Box(
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) BrahmGold.copy(0.12f) else Color.Transparent)
                                .border(if (selected) 1.dp else 0.dp, if (selected) BrahmGold.copy(0.4f) else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { activeTab = idx }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) BrahmGold else BrahmMutedForeground,
                            )
                        }
                    }
                }
            }

            if (activeTab == 0) {
                // ── Score Circle + Verdict ──
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BrahmCard),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            ScoreCircle(pct, totalScore, 36f)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(verdict, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = vColor)
                                if (verdictDetail.isNotBlank()) {
                                    Text(verdictDetail, fontSize = 11.sp, color = BrahmMutedForeground, lineHeight = 15.sp)
                                }
                                // Mangal badges
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(nameA to mangalA, nameB to mangalB).forEach { (nm, has) ->
                                        Box(
                                            Modifier.clip(RoundedCornerShape(50.dp))
                                                .background(if (has) Color(0xFFEF4444).copy(0.15f) else Color(0xFF10B981).copy(0.15f))
                                                .border(1.dp, if (has) Color(0xFFEF4444).copy(0.25f) else Color(0xFF10B981).copy(0.25f), RoundedCornerShape(50.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                "$nm: Mangal ${if (has) "Present" else "Absent"}",
                                                fontSize = 10.sp,
                                                color = if (has) Color(0xFFEF4444) else Color(0xFF10B981),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Guna Bars ──
                item {
                    Text("Ashtakoot — 8 Kuta Analysis", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrahmMutedForeground)
                }
                if (gunas.isNotEmpty()) {
                    items(count = gunas.size) { i -> GunaBar(gunas[i], i) }
                }

                // ── Life Areas ──
                if (lifeAreas.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = BrahmCard),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Life Area Compatibility", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrahmMutedForeground)
                                lifeAreas.forEachIndexed { i, area -> LifeAreaBar(area, i) }
                            }
                        }
                    }
                }

                // ── Astro Profiles ──
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BrahmCard),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Astrological Profiles", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrahmMutedForeground)
                            // Header
                            Row(Modifier.fillMaxWidth()) {
                                Spacer(Modifier.weight(1f))
                                Text(nameA, fontSize = 11.sp, color = BrahmGold, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(nameB, fontSize = 11.sp, color = BrahmGold, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            }
                            HorizontalDivider(color = BrahmBorder)
                            listOf(
                                "Nakshatra" to (data.str("nakshatra_a") to data.str("nakshatra_b")),
                                "Rashi"     to (data.str("rashi_a") to data.str("rashi_b")),
                                "Gana"      to (data.str("gana_a") to data.str("gana_b")),
                                "Nadi"      to (data.str("nadi_a").split(" ").first() to data.str("nadi_b").split(" ").first()),
                                "Varna"     to (data.str("varna_a") to data.str("varna_b")),
                            ).forEach { (label, pair) ->
                                Row(Modifier.fillMaxWidth()) {
                                    Text(label, fontSize = 11.sp, color = BrahmMutedForeground, modifier = Modifier.weight(1f))
                                    Text(pair.first, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                    Text(pair.second, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                }
                                HorizontalDivider(color = BrahmBorder.copy(0.4f))
                            }
                        }
                    }
                }

                // ── Doshas ──
                if (doshas.isNotEmpty()) {
                    item { Text("Dosha Analysis", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrahmMutedForeground) }
                    items(count = activeDoshas.size)   { i -> DoshaCard(activeDoshas[i]) }
                    items(count = inactiveDoshas.size) { i -> DoshaCard(inactiveDoshas[i]) }
                }

                // ── Strengths ──
                if (strengths.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = BrahmCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(0.2f)),
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Check, null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                    Text("Strengths", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                                strengths.forEach { s ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("✓", fontSize = 11.sp, color = Color(0xFF10B981))
                                        Text(s, fontSize = 11.sp, color = BrahmMutedForeground, lineHeight = 15.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Challenges ──
                if (challenges.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = BrahmCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(0.2f)),
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Warning, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                    Text("Challenges", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                }
                                challenges.forEach { c ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("▲", fontSize = 11.sp, color = Color(0xFFF59E0B))
                                        Text(c, fontSize = 11.sp, color = BrahmMutedForeground, lineHeight = 15.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Lahiri Ayanamsha · Moon Nakshatra · Rajju & Vedha included\nAshtakoot Milan is one factor — consult a qualified Jyotishi for complete guidance.",
                        fontSize = 10.sp, color = BrahmMutedForeground.copy(0.5f), lineHeight = 14.sp,
                    )
                }

            } else {
                // ── Nakshatra Tab ──
                item {
                    NakshatraTab(data, nameA, nameB)
                }
            }
        }

        ScrollToTopFab(listState, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp))
    }
}

// ── Input Form ─────────────────────────────────────────────────────────────────

@Composable
fun CompatibilityInputForm(
    name1: String, dob1: String, tob1: String, pob1: String,
    name2: String, dob2: String, tob2: String, pob2: String,
    error: String?,
    varnaSystem: String,
    onVarnaSystemChange: (String) -> Unit,
    onName1Change: (String) -> Unit, onDob1Change: (String) -> Unit,
    onTob1Change: (String) -> Unit,  onPob1Change: (String) -> Unit,
    onCityASelected: (City) -> Unit,
    onName2Change: (String) -> Unit, onDob2Change: (String) -> Unit,
    onTob2Change: (String) -> Unit,  onPob2Change: (String) -> Unit,
    onCityBSelected: (City) -> Unit,
    onCalculate: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Favorite, null, tint = BrahmGold, modifier = Modifier.size(16.dp))
                        Text("Person A — Groom / Partner 1", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    BirthInputFields(
                        name = name1, onNameChange = onName1Change,
                        dob = dob1, onDobChange = onDob1Change,
                        tob = tob1, onTobChange = onTob1Change,
                        pob = pob1, onPobChange = onPob1Change,
                        onCitySelected = onCityASelected,
                        cityVmKey = "personA",
                    )
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Favorite, null, tint = Color(0xFFEC4899), modifier = Modifier.size(16.dp))
                        Text("Person B — Bride / Partner 2", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    BirthInputFields(
                        name = name2, onNameChange = onName2Change,
                        dob = dob2, onDobChange = onDob2Change,
                        tob = tob2, onTobChange = onTob2Change,
                        pob = pob2, onPobChange = onPob2Change,
                        onCitySelected = onCityBSelected,
                        cityVmKey = "personB",
                    )
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Varna System",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrahmMutedForeground,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "both"      to "Both",
                            "nakshatra" to "Nakshatra",
                            "rashi"     to "Rashi",
                        ).forEach { (sys, label) ->
                            val selected = varnaSystem == sys
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) BrahmGold else BrahmMuted)
                                    .clickable { onVarnaSystemChange(sys) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) Color.White else BrahmMutedForeground,
                                )
                            }
                        }
                    }
                    Text(
                        when (varnaSystem) {
                            "nakshatra" -> "Parashari — Varna by birth Nakshatra"
                            "rashi"     -> "Modern Drik Ganita — Varna by Moon sign element"
                            else        -> "Shows Nakshatra score; Rashi alternative shown if different"
                        },
                        fontSize = 11.sp,
                        color = BrahmMutedForeground,
                    )
                }
            }
        }

        item {
            if (error != null) {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEF4444).copy(0.1f)).padding(10.dp)
                ) {
                    Text(error, fontSize = 12.sp, color = Color(0xFFEF4444))
                }
                Spacer(Modifier.height(4.dp))
            }
            BrahmButton(
                text = "Check Kundali Compatibility",
                onClick = onCalculate,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Lahiri Ayanamsha · Moon Nakshatra · Rajju & Vedha included · 8 Kuta Analysis",
                fontSize = 10.sp, color = BrahmMutedForeground.copy(0.6f),
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 14.sp,
            )
        }
    }
}
