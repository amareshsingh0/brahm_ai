package com.bimoraai.brahm.ui.prashna

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private data class QuestionType(val value: String, val label: String)

private val QUESTION_TYPES = listOf(
    QuestionType("general",      "General"),
    QuestionType("wealth",       "Wealth"),
    QuestionType("career",       "Career"),
    QuestionType("relationship", "Relationship"),
    QuestionType("health",       "Health"),
    QuestionType("property",     "Property"),
    QuestionType("travel",       "Travel"),
    QuestionType("education",    "Education"),
    QuestionType("children",     "Children"),
    QuestionType("spiritual",    "Spiritual"),
)

private val GRAHA_EN = mapOf(
    "Surya" to "Sun", "Chandra" to "Moon", "Mangal" to "Mars",
    "Budh" to "Mercury", "Guru" to "Jupiter", "Shukra" to "Venus",
    "Shani" to "Saturn", "Rahu" to "Rahu", "Ketu" to "Ketu",
)

private val GRAHA_SYMBOL = mapOf(
    "Surya" to "☉", "Chandra" to "☽", "Mangal" to "♂",
    "Budh" to "☿", "Guru" to "♃", "Shukra" to "♀",
    "Shani" to "♄", "Rahu" to "☊", "Ketu" to "☋",
)

@Composable
fun PrashnaContent(data: JsonObject, onReset: () -> Unit) {
    val verdict       = data["prashna_verdict"]?.jsonPrimitive?.contentOrNull
                         ?: data["verdict"]?.jsonPrimitive?.contentOrNull ?: "MIXED"
    val datetime      = data["prashna_datetime"]?.jsonPrimitive?.contentOrNull ?: ""
    val horaLord      = data["hora_lord"]?.jsonPrimitive?.contentOrNull ?: ""
    val prashnaQ      = data["prashna_question"]?.jsonPrimitive?.contentOrNull ?: ""
    val prashnaType   = data["prashna_type"]?.jsonPrimitive?.contentOrNull ?: ""

    val factors = data["prashna_factors"]?.let { el ->
        try { el.jsonArray.map { it.jsonPrimitive.contentOrNull ?: "" }.filter { it.isNotBlank() } }
        catch (_: Exception) { emptyList() }
    } ?: emptyList()

    // lagna
    val lagnaObj  = try { data["lagna"]?.jsonObject } catch (_: Exception) { null }
    val lagnaRashi = lagnaObj?.get("rashi")?.jsonPrimitive?.contentOrNull ?: "—"
    val lagnaDeg   = lagnaObj?.get("degree")?.jsonPrimitive?.contentOrNull ?: ""

    // Moon (Chandra)
    val grahas    = try { data["grahas"]?.jsonObject } catch (_: Exception) { null }
    val chandraObj = try { grahas?.get("Chandra")?.jsonObject } catch (_: Exception) { null }
    val moonRashi = chandraObj?.get("rashi")?.jsonPrimitive?.contentOrNull ?: "—"
    val moonHouse = chandraObj?.get("house")?.jsonPrimitive?.contentOrNull ?: "—"

    val verdictColor = when (verdict.uppercase()) {
        "YES"   -> Color(0xFF2E7D32)
        "NO"    -> Color(0xFFC62828)
        else    -> Color(0xFFE65100)
    }
    val verdictBg = when (verdict.uppercase()) {
        "YES"   -> Color(0xFFE8F5E9)
        "NO"    -> Color(0xFFFFEBEE)
        else    -> Color(0xFFFFF3E0)
    }
    val verdictBorder = when (verdict.uppercase()) {
        "YES"   -> Color(0xFF81C784)
        "NO"    -> Color(0xFFEF9A9A)
        else    -> Color(0xFFFFCC80)
    }

    val horaDisplay  = GRAHA_EN[horaLord] ?: horaLord
    val horaSymbol   = GRAHA_SYMBOL[horaLord] ?: ""
    val typeLabel    = QUESTION_TYPES.find { it.value == prashnaType }?.label ?: prashnaType

    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().background(BrahmBackground),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            // ── Verdict Banner ──
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(verdictBg)
                        .border(1.dp, verdictBorder, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "VERDICT",
                            fontSize = 10.sp,
                            color = verdictColor.copy(alpha = 0.7f),
                            letterSpacing = 3.sp,
                        )
                        Text(
                            verdict.uppercase(),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = verdictColor,
                            ),
                        )
                        if (datetime.isNotBlank() || horaLord.isNotBlank()) {
                            Text(
                                buildString {
                                    if (datetime.isNotBlank()) append(datetime)
                                    if (horaLord.isNotBlank()) {
                                        if (datetime.isNotBlank()) append(" · ")
                                        append("Hora Lord: $horaDisplay $horaSymbol")
                                    }
                                },
                                fontSize = 11.sp,
                                color = verdictColor.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (prashnaQ.isNotBlank()) {
                            Text(
                                "\"$prashnaQ\"",
                                fontSize = 11.sp,
                                color = verdictColor.copy(alpha = 0.65f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            // ── Prashna Factors ──
            if (factors.isNotEmpty()) {
                item {
                    Text(
                        "Chart Indicators",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BrahmCard),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            factors.forEach { factor ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        Modifier
                                            .width(3.dp)
                                            .height(IntrinsicSize.Max)
                                            .background(BrahmGold.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                            .align(Alignment.Top)
                                    )
                                    Text(
                                        factor,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BrahmForeground.copy(alpha = 0.85f),
                                        ),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Chart Summary Grid ──
            item {
                Text(
                    "Prashna Chart Summary",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
            }
            item {
                val cells = listOf(
                    "Prashna Lagna" to "$lagnaRashi${if (lagnaDeg.isNotBlank()) " $lagnaDeg°" else ""}",
                    "Moon Position" to "$moonRashi H$moonHouse",
                    "Hora Lord"     to "$horaDisplay $horaSymbol".trim(),
                    "Question Type" to typeLabel,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    cells.forEach { (label, value) ->
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BrahmCard),
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    label,
                                    fontSize = 9.sp,
                                    color = BrahmMutedForeground,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = 0.3.sp,
                                )
                                Text(
                                    value,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrahmGold,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }

            // ── Recalculate ──
            item {
                BrahmButton(text = "Recalculate", onClick = onReset)
            }
        }

        ScrollToTopFab(
            listState,
            Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp),
        )
    }
}

@Composable
fun PrashnaInputForm(
    question: String,
    questionType: String,
    pob: String,
    error: String?,
    onQuestionChange: (String) -> Unit,
    onQuestionTypeChange: (String) -> Unit,
    onPobChange: (String) -> Unit,
    onCitySelected: (City) -> Unit,
    onCalculate: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BrahmBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {

        // ── Main input card ──
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrahmCard),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    // Location
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Current Location",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
                        )
                        BirthInputFields(
                            dob = "", onDobChange = {},
                            tob = "", onTobChange = {},
                            pob = pob, onPobChange = onPobChange,
                            onCitySelected = onCitySelected,
                            showName = false,
                            cityVmKey = "prashna",
                        )
                    }

                    HorizontalDivider(color = BrahmBorder)

                    // Question type chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Question Domain",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(QUESTION_TYPES) { qt ->
                                val selected = questionType == qt.value
                                FilterChip(
                                    selected = selected,
                                    onClick = { onQuestionTypeChange(qt.value) },
                                    label = { Text(qt.label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrahmGold,
                                        selectedLabelColor = Color.White,
                                    ),
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = BrahmBorder)

                    // Question text
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Your Question (Optional)",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
                        )
                        OutlinedTextField(
                            value = question,
                            onValueChange = onQuestionChange,
                            placeholder = { Text("Will I get the job? Should I travel now?", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold),
                            singleLine = true,
                        )
                    }

                    if (error != null) {
                        Text(
                            error,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935)),
                        )
                    }

                    BrahmButton(text = "Ask Prashna", onClick = onCalculate)
                }
            }
        }

        // ── Info card ──
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7)),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text("🔮", fontSize = 24.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "What is Prashna?",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrahmGold,
                            ),
                        )
                        Text(
                            "Prashna (Horary Astrology) answers specific questions using the exact moment the question is asked. No birth time needed — the cosmic positions at the moment of the question reveal the answer.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BrahmForeground.copy(alpha = 0.8f),
                            ),
                        )
                    }
                }
            }
        }
    }
}
