package com.bimoraai.brahm.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.ScrollToTopFab
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// ── Static Data ───────────────────────────────────────────────────────────────

private data class VedicText(
    val name: String,
    val sanskrit: String,
    val statLabel: String,   // e.g. "1028 hymns"
    val stat2Label: String = "", // e.g. "10 books"
    val extra: String = "",  // e.g. "by Vyasa"
    val veda: String = "",
    val description: String,
    val topics: List<String>,
)

private data class VedicCategory(
    val id: String,
    val label: String,
    val emoji: String,
    val description: String,
    val texts: List<VedicText>,
)

private val LIBRARY_CATEGORIES = listOf(
    VedicCategory(
        id = "vedas", label = "Vedas", emoji = "🔥",
        description = "The four eternal Vedas — Rig, Yajur, Sama, and Atharva — are the oldest and most sacred scriptures of Hinduism, composed in Vedic Sanskrit.",
        texts = listOf(
            VedicText(
                name = "Rig Veda", sanskrit = "ऋग्वेद",
                statLabel = "1,028 hymns", stat2Label = "10 books",
                description = "Oldest scripture in human history. Contains 1028 hymns (suktas) dedicated to cosmic forces like Agni, Indra, Varuna and Surya. Foundation of Vedic civilization.",
                topics = listOf("Hymns", "Creation", "Cosmic Order", "Soma rituals", "Indra", "Varuna", "Agni"),
            ),
            VedicText(
                name = "Yajur Veda", sanskrit = "यजुर्वेद",
                statLabel = "1,975 mantras", stat2Label = "40 chapters",
                description = "The scripture of ritual formulas. Contains sacrificial mantras for yajna (fire rituals). Exists in Krishna (Taittiriya) and Shukla (Vajasaneyi) versions.",
                topics = listOf("Yajna", "Sacrificial rites", "Mantras", "Dharma", "Cosmic order"),
            ),
            VedicText(
                name = "Sama Veda", sanskrit = "सामवेद",
                statLabel = "1,549 chants", stat2Label = "2 books",
                description = "The Veda of melody and chants. Most verses are from Rig Veda set to musical notation for ritual singing. Foundation of Indian classical music.",
                topics = listOf("Sacred music", "Chanting", "Spiritual resonance", "Upasana", "Ritual melodies"),
            ),
            VedicText(
                name = "Atharva Veda", sanskrit = "अथर्ववेद",
                statLabel = "730 hymns", stat2Label = "20 books",
                description = "Practical and domestic wisdom. Contains hymns for healing, prosperity, protection, and daily life. More earthy and practical than the other three Vedas.",
                topics = listOf("Healing", "Protection", "Daily rituals", "Vedic medicine", "Prosperity"),
            ),
        ),
    ),
    VedicCategory(
        id = "upanishads", label = "Upanishads", emoji = "📜",
        description = "The Upanishads are the philosophical conclusions of the Vedas (Vedanta). They explore the ultimate nature of Brahman, Atman, and the path to liberation.",
        texts = listOf(
            VedicText(
                name = "Isha Upanishad", sanskrit = "ईशोपनिषद्",
                statLabel = "18 verses", veda = "Shukla Yajur Veda",
                description = "Explores the unity of the Self (Atman) with Brahman. Teaches non-attachment to worldly possessions while performing all actions with full dedication.",
                topics = listOf("Atman", "Brahman", "Non-attachment", "Divine presence", "Karma"),
            ),
            VedicText(
                name = "Kena Upanishad", sanskrit = "केनोपनिषद्",
                statLabel = "35 verses", veda = "Sama Veda",
                description = "Investigates the nature of ultimate reality through the question: \"By whom is the mind directed?\" — pointing beyond the mind to pure Brahman.",
                topics = listOf("Brahman", "Mind", "Cosmic intelligence", "Self-inquiry"),
            ),
            VedicText(
                name = "Katha Upanishad", sanskrit = "कठोपनिषद्",
                statLabel = "119 verses", veda = "Krishna Yajur Veda",
                description = "The story of Nachiketa who asks Yama (Death) about the secrets of life and death. Contains profound teachings on the immortal Self and the path of Yoga.",
                topics = listOf("Atman", "Death", "Immortality", "Yoga", "Self-realization"),
            ),
            VedicText(
                name = "Mandukya Upanishad", sanskrit = "माण्डूक्योपनिषद्",
                statLabel = "12 verses", veda = "Atharva Veda",
                description = "The shortest yet most profound Upanishad. Analyzes OM and the four states of consciousness: waking, dreaming, deep sleep, and turiya (pure awareness).",
                topics = listOf("OM", "States of consciousness", "Turiya", "Advaita", "Liberation"),
            ),
            VedicText(
                name = "Chandogya Upanishad", sanskrit = "छान्दोग्योपनिषद्",
                statLabel = "8 chapters", veda = "Sama Veda",
                description = "One of the largest Upanishads. Contains the famous mahavakya \"Tat Tvam Asi\" (That Thou Art) — the identity of individual self with universal Brahman.",
                topics = listOf("Om", "Brahman", "Tat Tvam Asi", "Meditation", "Self-knowledge"),
            ),
            VedicText(
                name = "Brihadaranyaka Upanishad", sanskrit = "बृहदारण्यकोपनिषद्",
                statLabel = "6 chapters", veda = "Shukla Yajur Veda",
                extra = "by Yajnavalkya",
                description = "The largest Upanishad. Attributed to the sage Yajnavalkya. Contains dialogues on the nature of Brahman, creation, and the immortal Self through deep inquiry.",
                topics = listOf("Brahman", "Yajnavalkya", "Self-inquiry", "Rebirth", "Liberation"),
            ),
        ),
    ),
    VedicCategory(
        id = "gita", label = "Bhagavad Gita", emoji = "🪶",
        description = "The Bhagavad Gita — 700 verses across 18 chapters — is a dialogue between Krishna and Arjuna on the battlefield of Kurukshetra, covering all paths to liberation.",
        texts = listOf(
            VedicText(
                name = "Ch 1–3: Crisis & Karma Yoga", sanskrit = "अर्जुनविषादयोग · सांख्ययोग · कर्मयोग",
                statLabel = "126 verses",
                description = "Arjuna's moral dilemma on the battlefield of Kurukshetra and Krishna's foundational response through Karma Yoga — selfless action without attachment to results.",
                topics = listOf("Dharma", "Karma Yoga", "Duty", "Detachment", "Action"),
            ),
            VedicText(
                name = "Ch 4–6: Knowledge & Meditation", sanskrit = "ज्ञानकर्मसन्यासयोग · कर्मसन्यासयोग · ध्यानयोग",
                statLabel = "131 verses",
                description = "Krishna reveals divine knowledge, renunciation of action in its fruits, and the path of meditation. He explains the yoga of wisdom transcending the yoga of action.",
                topics = listOf("Jnana Yoga", "Renunciation", "Meditation", "Brahman", "Liberation"),
            ),
            VedicText(
                name = "Ch 7–9: Divine Knowledge & Royal Secret", sanskrit = "ज्ञानविज्ञानयोग · अक्षरब्रह्मयोग · राजविद्याराजगुह्ययोग",
                statLabel = "101 verses",
                description = "Krishna reveals his divine nature, his role as the sustainer of creation, and the royal secret: pure devotion as the most direct path to him.",
                topics = listOf("Divine knowledge", "Devotion", "Maya", "Manifest Brahman", "Cosmic order"),
            ),
            VedicText(
                name = "Ch 10–12: Divine Glory & Devotion", sanskrit = "विभूतियोग · विश्वरूपदर्शनयोग · भक्तियोग",
                statLabel = "117 verses",
                description = "Krishna describes his divine manifestations (vibhutis), reveals his cosmic universal form to Arjuna, and culminates in the supreme path of pure Bhakti (devotion).",
                topics = listOf("Vibhutis", "Vishvarupa", "Bhakti Yoga", "Divine Love", "Universal form"),
            ),
            VedicText(
                name = "Ch 13–15: Field, Gunas & Supreme Self", sanskrit = "क्षेत्रक्षेत्रज्ञविभागयोग · गुणत्रयविभागयोग · पुरुषोत्तमयोग",
                statLabel = "100 verses",
                description = "Explaining the field of experience (body-mind), the three gunas (sattva, rajas, tamas), and the Supreme Purusha who transcends the perishable and imperishable.",
                topics = listOf("Kshetra", "Trigunas", "Purusha", "Prakriti", "Supreme Self"),
            ),
            VedicText(
                name = "Ch 16–18: Liberation & Surrender", sanskrit = "दैवासुरसम्पद्विभागयोग · श्रद्धात्रयविभागयोग · मोक्षसन्यासयोग",
                statLabel = "125 verses",
                description = "Divine vs. demonic qualities, the three types of faith and renunciation across the gunas, culminating in the supreme teaching: total surrender to Krishna.",
                topics = listOf("Daivi Sampad", "Sattva/Rajas/Tamas", "Moksha", "Surrender", "Liberation"),
            ),
        ),
    ),
    VedicCategory(
        id = "dharma", label = "Dharma Texts", emoji = "📖",
        description = "The great Itihasas, Sutras, and Shastras — epics, philosophical treatises, and law codes that together form the foundation of Dharmic civilisation.",
        texts = listOf(
            VedicText(
                name = "Mahabharata", sanskrit = "महाभारत",
                statLabel = "1,00,000 verses", extra = "by Vyasa",
                description = "World's longest epic poem. Contains the Bhagavad Gita, Shanti Parva, and countless stories of dharma, karma, and cosmic order across 18 books.",
                topics = listOf("Dharma", "War", "Ethics", "Karma", "Cosmic order"),
            ),
            VedicText(
                name = "Ramayana", sanskrit = "रामायण",
                statLabel = "24,000 verses", extra = "by Valmiki",
                description = "The story of Rama — ideal king, husband, and son — and his battle against Ravana. Eternal guide to righteous conduct, devotion, and dharmic living.",
                topics = listOf("Dharma", "Devotion", "Righteousness", "Ideal conduct", "Bhakti"),
            ),
            VedicText(
                name = "Yoga Sutras", sanskrit = "योगसूत्राणि",
                statLabel = "196 sutras", extra = "by Patanjali",
                description = "Foundational text of Yoga philosophy. Defines the eight limbs of yoga (Ashtanga) and maps the entire inner journey from restless mind to Samadhi.",
                topics = listOf("Ashtanga Yoga", "Samadhi", "Citta-vritti", "Meditation", "Liberation"),
            ),
            VedicText(
                name = "Brahma Sutras", sanskrit = "ब्रह्मसूत्राणि",
                statLabel = "555 sutras", extra = "by Badarayana",
                description = "Systematizes the teachings of the Upanishads. One of the three foundational texts of Vedanta (Prasthanatrayi) along with the Gita and Upanishads.",
                topics = listOf("Brahman", "Vedanta", "Atman", "Maya", "Liberation"),
            ),
            VedicText(
                name = "Arthashastra", sanskrit = "अर्थशास्त्र",
                statLabel = "150 chapters", extra = "by Kautilya (Chanakya)",
                description = "Ancient Indian treatise on statecraft, economic policy, military strategy, and governance. One of the most sophisticated political texts of the ancient world.",
                topics = listOf("Statecraft", "Economics", "Diplomacy", "Law", "Governance"),
            ),
            VedicText(
                name = "Manusmriti", sanskrit = "मनुस्मृति",
                statLabel = "2,685 verses", extra = "by Manu",
                description = "Ancient legal text outlining dharmic duties, social order, and law for all stages of life. Historically influential in shaping Hindu jurisprudence and custom.",
                topics = listOf("Dharma", "Social order", "Duties", "Law", "Ethics"),
            ),
        ),
    ),
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun LibraryScreen(
    navController: NavController,
    vm: LibraryViewModel = hiltViewModel(),
) {
    val query     by vm.query.collectAsState()
    val results   by vm.results.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error     by vm.error.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    SwipeBackLayout(navController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Vedic Library", fontWeight = FontWeight.Bold)
                        Text("1.1M+ scripture chunks · 4 traditions", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
    ) { padding ->
        val listState = rememberLazyListState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BrahmBackground)
                .padding(padding),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                // ── Search bar ──
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { vm.query.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        placeholder = { Text("Search mantras, verses, deities, concepts…", fontSize = 13.sp) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = BrahmMutedForeground)
                        },
                        trailingIcon = {
                            if (isLoading) CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = BrahmGold,
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrahmGold,
                            unfocusedBorderColor = BrahmBorder,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                        ),
                    )
                }

                // ── RAG search results (when query >= 2) ──
                if (query.trim().length >= 2) {
                    val resultList = results?.get("results")?.let { el ->
                        try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
                    }

                    item {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = BrahmMutedForeground, modifier = Modifier.size(12.dp))
                            Text(
                                if (isLoading) "Searching knowledge base…"
                                else if (!resultList.isNullOrEmpty()) "Showing ${resultList.size} results for \"$query\""
                                else if (error != null) error!!
                                else "No results found for \"$query\"",
                                style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                            )
                        }
                    }

                    if (!resultList.isNullOrEmpty()) {
                        items(resultList) { result ->
                            val source   = result["source"]?.jsonPrimitive?.contentOrNull ?: "Vedic Text"
                            val language = result["language"]?.jsonPrimitive?.contentOrNull ?: ""
                            val text     = result["text"]?.jsonPrimitive?.contentOrNull
                                           ?: result["content"]?.jsonPrimitive?.contentOrNull ?: "—"
                            val chapter  = result["chapter"]?.jsonPrimitive?.contentOrNull
                            val verse    = result["verse"]?.jsonPrimitive?.contentOrNull
                            val score    = result["score"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull()

                            val catColor = when {
                                source.contains("Veda", ignoreCase = true)      -> Color(0xFFE65100)
                                source.contains("Upanishad", ignoreCase = true) -> Color(0xFF7B1FA2)
                                source.contains("Gita", ignoreCase = true)      -> Color(0xFF1565C0)
                                source.contains("Mahabharata", ignoreCase = true) || source.contains("Ramayana", ignoreCase = true) -> Color(0xFF2E7D32)
                                else -> BrahmGold
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = BrahmCard),
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(catColor.copy(alpha = 0.1f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                        ) {
                                            Text(source, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = catColor)
                                        }
                                        if (language.isNotBlank()) {
                                            Text(language, style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
                                        }
                                        if (chapter != null || verse != null) {
                                            Text(
                                                listOfNotNull(chapter?.let { "Ch.$it" }, verse?.let { "V.$it" }).joinToString(" · "),
                                                style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
                                            )
                                        }
                                        Spacer(Modifier.weight(1f))
                                        if (score != null) {
                                            Text(
                                                "${(score * 100).toInt()}% match",
                                                fontSize = 10.sp,
                                                color = BrahmGold,
                                            )
                                        }
                                    }
                                    Text(
                                        text.take(320) + if (text.length > 320) "…" else "",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BrahmForeground,
                                            lineHeight = 19.sp,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }

                // ── Tab row ──
                item {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 16.dp,
                        containerColor = BrahmBackground,
                        contentColor = BrahmGold,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = BrahmGold,
                                )
                            }
                        },
                        divider = { HorizontalDivider(color = BrahmBorder) },
                    ) {
                        LIBRARY_CATEGORIES.forEachIndexed { index, cat ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(cat.emoji, fontSize = 13.sp)
                                        Text(cat.label, fontSize = 13.sp)
                                    }
                                },
                                selectedContentColor = BrahmGold,
                                unselectedContentColor = BrahmMutedForeground,
                            )
                        }
                    }
                }

                // ── Category description ──
                val currentCat = LIBRARY_CATEGORIES[selectedTab]
                item {
                    Text(
                        currentCat.description,
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }

                // ── Text cards ──
                val filtered = if (query.trim().length >= 2) {
                    val q = query.trim().lowercase()
                    currentCat.texts.filter { t ->
                        t.name.lowercase().contains(q) ||
                        t.description.lowercase().contains(q) ||
                        t.topics.any { it.lowercase().contains(q) }
                    }
                } else {
                    currentCat.texts
                }

                if (filtered.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("No texts match \"$query\"", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        }
                    }
                } else {
                    items(filtered) { text ->
                        VedicTextCard(text, currentCat.id)
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }

            ScrollToTopFab(
                listState,
                Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp),
            )
        }
    }
    } // SwipeBackLayout
}

// ── VedicTextCard ─────────────────────────────────────────────────────────────

@Composable
private fun VedicTextCard(text: VedicText, categoryId: String) {
    val accentColor = when (categoryId) {
        "vedas"       -> Color(0xFFE65100)
        "upanishads"  -> Color(0xFF7B1FA2)
        "gita"        -> Color(0xFF1565C0)
        else          -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrahmBorder),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text.sanskrit,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = accentColor,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    if (text.extra.isNotBlank()) {
                        Text(
                            text.extra,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BrahmMutedForeground,
                                fontSize = 11.sp,
                            ),
                        )
                    }
                    if (text.veda.isNotBlank()) {
                        Text(
                            text.veda,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BrahmMutedForeground,
                                fontSize = 11.sp,
                            ),
                        )
                    }
                }

                // Stats badges
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    StatBadge(text.statLabel, accentColor)
                    if (text.stat2Label.isNotBlank()) {
                        StatBadge(text.stat2Label, accentColor.copy(alpha = 0.6f))
                    }
                }
            }

            // ── Description ──
            Text(
                text.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BrahmForeground.copy(alpha = 0.8f),
                    lineHeight = 18.sp,
                ),
            )

            // ── Topic chips ──
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(text.topics) { topic ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(accentColor.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            topic,
                            fontSize = 11.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            textAlign = TextAlign.End,
        )
    }
}
