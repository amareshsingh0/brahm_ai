package com.bimoraai.brahm.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bimoraai.brahm.core.theme.*

data class ChatMessage(val role: String, val content: String)  // role: "user" | "assistant"

@Composable
fun ChatScreen(vm: ChatViewModel = hiltViewModel()) {
    val messages by vm.messages.collectAsState()
    val isStreaming by vm.isStreaming.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrahmBackground)
    ) {
        // Header
        Surface(color = BrahmCard, shadowElevation = 1.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🤖", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Brahm AI", style = MaterialTheme.typography.titleLarge.copy(color = BrahmGold))
                    Text("Vedic Astrology Assistant", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("🔮", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(12.dp))
                        Text("Ask anything about astrology", style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground))
                    }
                }
            }
            items(messages) { msg -> ChatBubble(msg) }

            if (isStreaming) {
                item {
                    Row(Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BrahmGold,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Thinking...", color = BrahmMutedForeground, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Input bar
        Surface(color = BrahmCard, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about your kundali...", color = BrahmMutedForeground) },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrahmGold,
                        unfocusedBorderColor = BrahmBorder,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        if (input.isNotBlank() && !isStreaming) {
                            vm.sendMessage(input.trim())
                            input = ""
                        }
                    },
                    containerColor = BrahmGold,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            color = if (isUser) BrahmGold else BrahmCard,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(
                text = msg.content,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) Color.White else BrahmForeground,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
