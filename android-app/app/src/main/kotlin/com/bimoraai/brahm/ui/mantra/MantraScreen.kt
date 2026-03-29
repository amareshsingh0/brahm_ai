package com.bimoraai.brahm.ui.mantra

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.ScrollToTopFab
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.theme.*

data class MantraInfo(
    val deity: String,
    val category: String,
    val sanskrit: String,       // Devanagari
    val transliteration: String,
    val meaning: String,
    val benefits: List<String>,
    val accentColor: Color,
)

private val mantras = listOf(
    MantraInfo("Gayatri Mantra", "Surya",
        "ॐ भूर्भुवः स्वः तत्सवितुर्वरेण्यं भर्गो देवस्य धीमहि धियो यो नः प्रचोदयात्",
        "Om Bhur Bhuva Swaha, Tat Savitur Varenyam, Bhargo Devasya Dheemahi, Dhiyo Yo Nah Prachodayat",
        "We meditate on the glory of the Creator who has created the Universe, who is worthy of worship, who is the embodiment of Knowledge and Light, who is the remover of all Sin and Ignorance. May He enlighten our intellect.",
        listOf("Enhances intellect & wisdom", "Removes sins & negativity", "Brings divine light", "Ideal for students & seekers"),
        Color(0xFFFF8F00)),
    MantraInfo("Mahamrityunjaya", "Shiva",
        "ॐ त्र्यम्बकं यजामहे सुगन्धिं पुष्टिवर्धनम् उर्वारुकमिव बन्धनान्मृत्योर्मुक्षीय माऽमृतात्",
        "Om Tryambakam Yajamahe Sugandhim Pushtivardhanam, Urvarukamiva Bandhanan Mrityor Mukshiya Maamritat",
        "We worship the three-eyed Lord Shiva who is fragrant and nourishes all beings. May He liberate us from death for the sake of immortality, just as a cucumber is severed from its bondage.",
        listOf("Protection from untimely death", "Healing & health restoration", "Liberation from fear", "Moksha & spiritual growth"),
        Color(0xFF6A1B9A)),
    MantraInfo("Hanuman Chalisa", "Hanuman",
        "जय हनुमान ज्ञान गुण सागर। जय कपीश तिहुँ लोक उजागर॥",
        "Jai Hanuman Gyan Gun Sagar, Jai Kapeesh Tihu Lok Ujagar",
        "Victory to Hanuman, the ocean of wisdom and virtue, who illumines all three worlds.",
        listOf("Removes fear & evil spirits", "Strength & courage", "Protection from enemies", "Grants boons & wishes"),
        Color(0xFFE53935)),
    MantraInfo("Lakshmi Mantra", "Lakshmi",
        "ॐ श्रीं ह्रीं क्लीं त्रिभुवन महालक्ष्म्यै अस्माकं दारिद्र्य नाशय प्रचुर धन देहि देहि क्लीं ह्रीं श्रीं ॐ",
        "Om Shreem Hreem Kleem Tribhuvan Mahalakshmyai Asmakam Daridra Nashaya Prachura Dhana Dehi Dehi Kleem Hreem Shreem Om",
        "O Mahalakshmi of three worlds, destroy our poverty and grant us abundant wealth.",
        listOf("Attracts wealth & prosperity", "Removes financial obstacles", "Brings business success", "Domestic happiness"),
        Color(0xFFFFB300)),
    MantraInfo("Saraswati Mantra", "Saraswati",
        "ॐ ऐं महासरस्वत्यै नमः",
        "Om Aim Mahasaraswatyai Namah",
        "Salutations to the great Goddess Saraswati, the bestower of wisdom.",
        listOf("Enhances learning & memory", "Blessings for students & artists", "Eloquence in speech", "Mastery of arts & sciences"),
        Color(0xFF1565C0)),
    MantraInfo("Ganesh Mantra", "Ganesha",
        "ॐ गं गणपतये नमः",
        "Om Gam Ganapataye Namah",
        "Salutations to Lord Ganesha, the remover of obstacles.",
        listOf("Removes obstacles from life", "Auspicious beginning of any work", "Success in endeavors", "Wisdom & intellect"),
        Color(0xFFFF6F00)),
    MantraInfo("Navgraha Mantra", "Navagrahas",
        "ॐ सूर्याय नमः। ॐ चन्द्राय नमः। ॐ मंगलाय नमः। ॐ बुधाय नमः। ॐ बृहस्पतये नमः। ॐ शुक्राय नमः। ॐ शनये नमः। ॐ राहवे नमः। ॐ केतवे नमः।",
        "Om Suryaya Namah. Om Chandraya Namah. Om Mangalaya Namah. Om Budhaya Namah. Om Brihaspataye Namah. Om Shukraya Namah. Om Shanaye Namah. Om Rahave Namah. Om Ketave Namah.",
        "Salutations to all nine planetary lords — Sun, Moon, Mars, Mercury, Jupiter, Venus, Saturn, Rahu, Ketu.",
        listOf("Pacifies all planetary doshas", "Reduces malefic effects", "Brings planetary harmony", "Health, wealth & peace"),
        Color(0xFF00897B)),
    MantraInfo("Shiva Panchakshara", "Shiva",
        "नमः शिवाय",
        "Namah Shivaya",
        "Salutations to the auspicious Shiva — the five elements: Na=Earth, Ma=Water, Shi=Fire, Va=Air, Ya=Space.",
        listOf("Liberation & moksha", "Purification of the soul", "Removes ego & ignorance", "Divine grace & blessings"),
        Color(0xFF546E7A)),
    MantraInfo("Vishnu Mantra", "Vishnu",
        "ॐ नमो भगवते वासुदेवाय",
        "Om Namo Bhagavate Vasudevaya",
        "Salutations to Lord Vasudeva, the all-pervading supreme being.",
        listOf("Protection & preservation", "Spiritual liberation", "Removes suffering", "Divine love & devotion"),
        Color(0xFF1E88E5)),
    MantraInfo("Durga Mantra", "Durga",
        "ॐ दुं दुर्गायै नमः",
        "Om Dum Durgayai Namah",
        "Salutations to Goddess Durga, the fierce and protective divine mother.",
        listOf("Protection from evil & danger", "Courage & inner strength", "Victory over enemies", "Dispels fear & negativity"),
        Color(0xFFE91E63)),
)

private val categories = listOf("All", "Surya", "Shiva", "Hanuman", "Lakshmi", "Saraswati", "Ganesha", "Navagrahas", "Vishnu", "Durga")

@Composable
fun MantraScreen(navController: NavController) {
    var selectedCategory by remember { mutableStateOf("All") }
    var expandedMantra by remember { mutableStateOf<String?>(null) }

    val filtered = remember(selectedCategory) {
        if (selectedCategory == "All") mantras else mantras.filter { it.category == selectedCategory }
    }

    SwipeBackLayout(navController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mantra Dictionary", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrahmBackground),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(BrahmBackground).padding(padding),
        ) {
            // Category filter
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrahmGold,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
            val listState = rememberLazyListState()
            Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = { it.deity }) { mantra ->
                    MantraCard(
                        mantra = mantra,
                        expanded = expandedMantra == mantra.deity,
                        onClick = { expandedMantra = if (expandedMantra == mantra.deity) null else mantra.deity },
                    )
                }
            }
            ScrollToTopFab(listState, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 88.dp))
            } // Box
        }
    }
    } // SwipeBackLayout
}

@Composable
private fun MantraCard(mantra: MantraInfo, expanded: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
        border = if (expanded) androidx.compose.foundation.BorderStroke(1.5.dp, mantra.accentColor) else null,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(mantra.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🕉", fontSize = 22.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(mantra.deity, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(mantra.category, style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            }
            // Sanskrit text preview
            Text(
                if (expanded) mantra.sanskrit else mantra.sanskrit.take(80) + "…",
                style = MaterialTheme.typography.bodyMedium.copy(color = BrahmForeground, lineHeight = 22.sp),
            )
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = BrahmBorder)
                    Text(
                        "Transliteration",
                        style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold),
                    )
                    Text(mantra.transliteration, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                    Text(
                        "Meaning",
                        style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold),
                    )
                    Text(mantra.meaning, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                    Text(
                        "Benefits",
                        style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold),
                    )
                    mantra.benefits.forEach { benefit ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(mantra.accentColor).offset(y = 5.dp))
                            Text(benefit, style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground))
                        }
                    }
                }
            }
        }
    }
}
