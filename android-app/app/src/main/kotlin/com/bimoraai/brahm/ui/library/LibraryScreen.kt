package com.bimoraai.brahm.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.components.SwipeBackLayout
import com.bimoraai.brahm.core.theme.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val categories = listOf("All", "Vedas", "Upanishads", "Puranas", "Shastras", "Smritis")

@Composable
fun LibraryScreen(
    navController: NavController,
    vm: LibraryViewModel = hiltViewModel(),
) {
    val query     by vm.query.collectAsState()
    val results   by vm.results.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error     by vm.error.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }

    SwipeBackLayout(navController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vedic Library", fontWeight = FontWeight.Bold) },
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
            // Search bar
            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                placeholder = { Text("Search Vedic scriptures, mantras, verses…") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = BrahmMutedForeground)
                },
                trailingIcon = {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = BrahmGold)
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrahmGold,
                    unfocusedBorderColor = BrahmBorder,
                ),
            )

            // Category chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
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

            Spacer(Modifier.height(8.dp))

            when {
                query.trim().length < 2 -> {
                    // Empty state
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("📚", fontSize = 48.sp)
                            Text("Search the Sacred Texts", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "Enter a topic, mantra, deity name, or concept\nto search across 1.1M+ Vedic scripture chunks",
                                style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
                error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error!!, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFE53935)))
                    }
                }
                else -> {
                    val resultList = results?.get("results")?.let { el ->
                        try { el.jsonArray.map { it.jsonObject } } catch (_: Exception) { null }
                    }
                    val filtered = if (selectedCategory == "All") resultList else
                        resultList?.filter { item ->
                            item["source"]?.jsonPrimitive?.contentOrNull?.contains(selectedCategory, ignoreCase = true) == true
                        }

                    if (filtered.isNullOrEmpty() && !isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔍", fontSize = 36.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("No results found", style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground))
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            filtered?.let {
                                item {
                                    Text(
                                        "${it.size} results for \"$query\"",
                                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                                    )
                                }
                                items(it) { result ->
                                    SearchResultCard(result)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    } // SwipeBackLayout
}

@Composable
private fun SearchResultCard(result: JsonObject) {
    val source  = result["source"]?.jsonPrimitive?.contentOrNull ?: "Vedic Text"
    val chapter = result["chapter"]?.jsonPrimitive?.contentOrNull
    val verse   = result["verse"]?.jsonPrimitive?.contentOrNull
    val content = result["content"]?.jsonPrimitive?.contentOrNull ?: result["text"]?.jsonPrimitive?.contentOrNull ?: "—"
    val score   = result["score"]?.jsonPrimitive?.contentOrNull?.toFloatOrNull()

    val categoryColor = when {
        source.contains("Veda", ignoreCase = true) -> Color(0xFFFF6F00)
        source.contains("Upanishad", ignoreCase = true) -> Color(0xFF8E24AA)
        source.contains("Purana", ignoreCase = true) -> Color(0xFF1565C0)
        source.contains("Shastra", ignoreCase = true) -> Color(0xFF2E7D32)
        else -> BrahmGold
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrahmCard),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier.background(categoryColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(source, style = MaterialTheme.typography.labelSmall.copy(color = categoryColor, fontWeight = FontWeight.SemiBold))
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
                        style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold, fontSize = 10.sp),
                    )
                }
            }
            Text(
                content.take(300) + if (content.length > 300) "…" else "",
                style = MaterialTheme.typography.bodySmall.copy(color = BrahmForeground, lineHeight = 18.sp),
            )
        }
    }
}
