package com.bimoraai.brahm.ui.rectification

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimoraai.brahm.core.components.BirthInputFields
import com.bimoraai.brahm.core.components.BrahmButton
import com.bimoraai.brahm.core.components.ScrollToTopFab
import com.bimoraai.brahm.core.network.City
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonObject
import java.util.Calendar

// ── Event types (value → display label) ────────────────────────────────────────
private val EVENT_TYPE_OPTIONS = listOf(
    "marriage"       to "Marriage / Relationship",
    "child"          to "Child Birth",
    "career_start"   to "Career Start",
    "career_change"  to "Career Change",
    "property"       to "Property Purchase",
    "health"         to "Health Issue",
    "accident"       to "Accident",
    "foreign"        to "Foreign Travel",
    "loss"           to "Death / Loss",
    "success"        to "Major Success",
    "education"      to "Education",
    "spiritual"      to "Spiritual Event",
    "separation"     to "Separation / Divorce",
    "financial_loss" to "Financial Loss",
    "financial_gain" to "Financial Gain",
)

private val UNCERTAINTY_OPTIONS = listOf(
    30  to "±30 min",
    60  to "±1 hour",
    90  to "±1.5 hr",
    120 to "±2 hours",
    180 to "±3 hours",
)

private fun scoreColor(score: Int) = when {
    score >= 70 -> Color(0xFF43A047)
    score >= 50 -> Color(0xFFF59E0B)
    else        -> Color(0xFF9CA3AF)
}

private fun scoreBg(score: Int) = when {
    score >= 70 -> Color(0xFFD1FAE5)
    score >= 50 -> Color(0xFFFEF3C7)
    else        -> Color(0xFFF3F4F6)
}

private fun scoreBorder(score: Int) = when {
    score >= 70 -> Color(0xFF6EE7B7)
    score >= 50 -> Color(0xFFFCD34D)
    else        -> Color(0xFFE5E7EB)
}

// ── Date picker helper ─────────────────────────────────────────────────────────
@Composable
private fun EventDatePicker(
    date: String,          // stored as "YYYY-MM-DD" for API
    onDateSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val displayDate = if (date.isNotBlank()) {
        // Convert YYYY-MM-DD → dd-mm-yyyy for display
        val parts = date.split("-")
        if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else date
    } else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, BrahmBorder, RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable {
                val cal = Calendar.getInstance()
                if (date.isNotBlank()) {
                    val parts = date.split("-")
                    if (parts.size == 3) {
                        cal.set(parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR),
                            (parts[1].toIntOrNull() ?: 1) - 1,
                            parts[2].toIntOrNull() ?: 1)
                    }
                }
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onDateSelected("%04d-%02d-%02d".format(year, month + 1, day))
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH),
                ).show()
            }
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Text(
            text = if (displayDate.isBlank()) "dd-mm-yyyy" else displayDate,
            fontSize = 13.sp,
            color = if (displayDate.isBlank()) BrahmMutedForeground.copy(alpha = 0.5f) else BrahmForeground,
        )
    }
}

// ── Event type dropdown ────────────────────────────────────────────────────────
@Composable
private fun EventTypeDropdown(
    selected: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = EVENT_TYPE_OPTIONS.firstOrNull { it.first == selected }?.second ?: selected

    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, BrahmBorder, RoundedCornerShape(10.dp))
                .background(Color.White)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 13.sp, color = BrahmForeground, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = BrahmMutedForeground,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            EVENT_TYPE_OPTIONS.forEach { (value, display) ->
                DropdownMenuItem(
                    text = { Text(display, fontSize = 13.sp) },
                    onClick = { onSelected(value); expanded = false },
                    colors = MenuDefaults.itemColors(
                        textColor = if (selected == value) BrahmGold else BrahmForeground,
                    ),
                )
            }
        }
    }
}

// ── Input Form ─────────────────────────────────────────────────────────────────
@Composable
fun RectificationInputForm(
    name: String, dob: String, approxTob: String, pob: String,
    lat: Double, lon: Double,
    uncertainty: Int,
    events: List<LifeEvent>,
    error: String?,
    onNameChange: (String) -> Unit,
    onDobChange: (String) -> Unit,
    onApproxTobChange: (String) -> Unit,
    onPobChange: (String) -> Unit,
    onCitySelected: (City) -> Unit,
    onUncertaintyChange: (Int) -> Unit,
    onAddEvent: () -> Unit,
    onRemoveEvent: (Int) -> Unit,
    onUpdateEventDate: (Int, String) -> Unit,
    onUpdateEventType: (Int, String) -> Unit,
    onCalculate: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── Birth Details ──
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Schedule, null, tint = BrahmGold, modifier = Modifier.size(16.dp))
                        Text("Birth Details", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrahmForeground)
                    }
                    BirthInputFields(
                        name = name, onNameChange = onNameChange,
                        dob = dob, onDobChange = onDobChange,
                        tob = approxTob, onTobChange = onApproxTobChange,
                        pob = pob, onPobChange = onPobChange,
                        onCitySelected = onCitySelected,
                    )
                    if (lat != 0.0 && lon != 0.0) {
                        Text(
                            "%.4f°N  %.4f°E".format(lat, lon),
                            fontSize = 10.sp,
                            color = BrahmMutedForeground.copy(alpha = 0.6f),
                        )
                    }

                    // Uncertainty chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Time Uncertainty", fontSize = 11.sp, color = BrahmMutedForeground)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            UNCERTAINTY_OPTIONS.forEach { (mins, label) ->
                                val selected = uncertainty == mins
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, if (selected) BrahmGold else BrahmBorder, RoundedCornerShape(8.dp))
                                        .background(if (selected) BrahmGold.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { onUncertaintyChange(mins) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        label,
                                        fontSize = 11.sp,
                                        color = if (selected) BrahmGold else BrahmMutedForeground,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Life Events ──
        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = BrahmCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = BrahmGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Known Life Events", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrahmForeground)
                            Text("(add at least 2 for best accuracy)", fontSize = 11.sp, color = BrahmMutedForeground)
                        }
                        TextButton(onClick = onAddEvent, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(15.dp), tint = BrahmGold)
                            Spacer(Modifier.width(3.dp))
                            Text("Add Event", fontSize = 12.sp, color = BrahmGold)
                        }
                    }

                    events.forEachIndexed { i, event ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrahmMuted)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Event ${i + 1}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrahmMutedForeground)
                                if (events.size > 1) {
                                    IconButton(
                                        onClick = { onRemoveEvent(i) },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Event Date
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Event Date", fontSize = 11.sp, color = BrahmMutedForeground)
                                EventDatePicker(
                                    date = event.date,
                                    onDateSelected = { onUpdateEventDate(i, it) },
                                )
                            }

                            // Event Type
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Event Type", fontSize = 11.sp, color = BrahmMutedForeground)
                                EventTypeDropdown(
                                    selected = event.type,
                                    onSelected = { onUpdateEventType(i, it) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Error + Submit ──
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
            BrahmButton(text = "Rectify Birth Time", onClick = onCalculate, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text(
                "Lahiri Ayanamsha · Score = how well active Dasha lord matches event's karaka",
                fontSize = 10.sp, color = BrahmMutedForeground.copy(0.6f),
                modifier = Modifier.fillMaxWidth(), lineHeight = 14.sp,
            )
        }
    }
}

// ── Results ────────────────────────────────────────────────────────────────────
@Composable
fun RectificationContent(data: JsonObject, onReset: () -> Unit) {
    val candidates = data["candidates"]?.let { el ->
        try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
    } ?: emptyList()
    val eventCount = data["event_count"]?.jsonPrimitive?.contentOrNull ?: ""

    var expandedIdx by remember { mutableStateOf<Int?>(0) }

    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().background(BrahmBackground),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = BrahmGold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Rectification Results", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BrahmForeground, modifier = Modifier.weight(1f))
                    if (eventCount.isNotBlank()) {
                        Text("${candidates.size} candidates · $eventCount events", fontSize = 10.sp, color = BrahmMutedForeground)
                    }
                }
            }

            // Info banner
            item {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(BrahmMuted).padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Score = how well the active Mahadasha/Antardasha lord matches the event type's natural karaka. Higher = stronger alignment. Best candidate shown first.",
                        fontSize = 11.sp, color = BrahmMutedForeground, lineHeight = 16.sp,
                    )
                }
            }

            // Candidate cards
            candidates.forEachIndexed { i, c ->
                val time        = c["time"]?.jsonPrimitive?.contentOrNull ?: "—"
                val delta       = c["delta_minutes"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                val lagnaRashi  = c["lagna_rashi"]?.jsonPrimitive?.contentOrNull ?: "—"
                val lagnaChanged= c["lagna_changed"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
                val score       = c["score"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                val moonNak     = c["moon_nakshatra"]?.jsonPrimitive?.contentOrNull ?: ""
                val sampleMaha  = c["sample_maha"]?.jsonPrimitive?.contentOrNull ?: ""
                val sampleAntar = c["sample_antar"]?.jsonPrimitive?.contentOrNull ?: ""
                val isExpanded  = expandedIdx == i

                item(key = "cand_$i") {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = scoreBg(score)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, scoreBorder(score)),
                    ) {
                        Column {
                            // Header row — tappable
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedIdx = if (isExpanded) null else i }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                // Rank badge
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (i == 0) Color(0xFFFEF3C7) else BrahmMuted),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "${i + 1}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (i == 0) Color(0xFFB45309) else BrahmMutedForeground,
                                    )
                                }

                                // Time + score bar
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(time, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrahmForeground)
                                        if (delta != 0) {
                                            Text(
                                                "(${if (delta > 0) "+" else ""}${delta} min)",
                                                fontSize = 10.sp, color = BrahmMutedForeground,
                                            )
                                        }
                                        if (i == 0) {
                                            Box(
                                                Modifier.clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFFEF3C7))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Top Match", fontSize = 9.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Medium)
                                            }
                                        }
                                        if (lagnaChanged) {
                                            Box(
                                                Modifier.clip(RoundedCornerShape(4.dp))
                                                    .background(BrahmGold.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Lagna: $lagnaRashi", fontSize = 9.sp, color = BrahmGold)
                                            }
                                        }
                                    }
                                    // Score bar
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LinearProgressIndicator(
                                            progress = { (score / 100f).coerceIn(0f, 1f) },
                                            modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)),
                                            color = scoreColor(score),
                                            trackColor = Color(0xFFE5E7EB),
                                        )
                                        Text("$score%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = scoreColor(score))
                                    }
                                }

                                Icon(
                                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = BrahmMutedForeground,
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            // Expanded detail
                            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                                Column {
                                    HorizontalDivider(color = scoreBorder(score))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        listOfNotNull(
                                            "Lagna Rashi" to lagnaRashi,
                                            if (moonNak.isNotBlank()) "Moon Nakshatra" to moonNak else null,
                                            if (sampleMaha.isNotBlank()) "Mahadasha" to sampleMaha else null,
                                            if (sampleAntar.isNotBlank()) "Antardasha" to sampleAntar else null,
                                        ).forEach { (label, value) ->
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.White.copy(alpha = 0.6f))
                                                    .padding(8.dp),
                                            ) {
                                                Text(label.uppercase(), fontSize = 8.sp, color = BrahmMutedForeground, letterSpacing = 0.5.sp)
                                                Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = BrahmForeground)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Disclaimer + recalculate
            item {
                Text(
                    "This is a computational aid — final rectification should be confirmed with a qualified Jyotishi. Accuracy improves with more life events and precise event dates.",
                    fontSize = 10.sp, color = BrahmMutedForeground.copy(0.6f), lineHeight = 14.sp,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrahmGold),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrahmGold),
                ) {
                    Text("Recalculate", fontSize = 13.sp)
                }
            }
        }
        ScrollToTopFab(listState, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp))
    }
}
