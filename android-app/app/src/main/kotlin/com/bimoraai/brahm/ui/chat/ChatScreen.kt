package com.bimoraai.brahm.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bimoraai.brahm.core.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel = hiltViewModel()) {
    val messages          by vm.messages.collectAsState()
    val isStreaming       by vm.isStreaming.collectAsState()
    val sessions          by vm.sessions.collectAsState()
    val archivedSessions  by vm.archivedSessions.collectAsState()
    val sessionsLoading   by vm.sessionsLoading.collectAsState()

    var input           by remember { mutableStateOf("") }
    val listState       = rememberLazyListState()
    val keyboard        = LocalSoftwareKeyboardController.current
    var showHistory     by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val visibleMessages = messages.filter { !(it.role == "assistant" && it.content.isEmpty()) }
    val showTyping      = isStreaming && messages.lastOrNull()?.content?.isEmpty() == true
    val canSend         = input.isNotBlank() && !isStreaming

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // AI Avatar
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF3CD)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrahmGold, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Brahm AI", style = MaterialTheme.typography.titleMedium.copy(color = BrahmForeground, fontWeight = FontWeight.SemiBold))
                                Spacer(Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF0FDF4)) {
                                    Text("ONLINE", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, fontSize = 9.sp))
                                }
                            }
                            Text("Vedic Astrology Assistant", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                        }
                        // History button
                        IconButton(onClick = {
                            vm.loadAllSessions()
                            showHistory = true
                        }) {
                            Icon(Icons.Default.History, contentDescription = "Chat History", tint = BrahmMutedForeground)
                        }
                        // New Chat button
                        IconButton(onClick = { vm.startNewChat() }) {
                            Icon(Icons.Default.EditNote, contentDescription = "New Chat", tint = BrahmGold)
                        }
                    }
                    Surface(color = Color(0xFFFFFBEB)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", fontSize = 11.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("AI can make mistakes • Please verify important information",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF92400E), fontSize = 10.sp))
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp, modifier = Modifier.imePadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message Brahm AI...", color = BrahmMutedForeground, style = MaterialTheme.typography.bodyMedium) },
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrahmGold,
                            unfocusedBorderColor = BrahmBorder,
                            focusedTextColor = BrahmForeground,
                            unfocusedTextColor = BrahmForeground,
                            cursorColor = BrahmGold,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF9F9FA),
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (canSend) BrahmGold else BrahmBorder)
                            .clickable(enabled = canSend) {
                                vm.sendMessage(input.trim())
                                input = ""
                                keyboard?.hide()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        containerColor = BrahmBackground,
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (messages.isEmpty()) {
                item { EmptyState(onSuggestionClick = { vm.sendMessage(it) }) }
            }
            items(visibleMessages) { msg ->
                ChatBubble(msg, onFollowUpClick = { vm.sendFollowUp(it) })
            }
            if (showTyping) {
                item { TypingIndicator() }
            }
        }
    }

    // ── Chat History Bottom Sheet ─────────────────────────────────────────────
    if (showHistory) {
        ModalBottomSheet(
            onDismissRequest = { showHistory = false },
            containerColor = BrahmBackground,
            dragHandle = {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(BrahmBorder),
                    )
                }
            },
        ) {
            ChatHistorySheet(
                sessions         = sessions,
                archivedSessions = archivedSessions,
                isLoading        = sessionsLoading,
                onSessionClick   = { session -> vm.loadSession(session); showHistory = false },
                onDelete         = { vm.deleteSession(it) },
                onPin            = { vm.pinSession(it) },
                onArchive        = { vm.archiveSession(it) },
                onUnarchive      = { vm.unarchiveSession(it) },
                onRename         = { session, name -> vm.renameSession(session, name) },
            )
        }
    }
}

// ── Chat History Sheet Content ────────────────────────────────────────────────
@Composable
private fun ChatHistorySheet(
    sessions: List<ChatSession>,
    archivedSessions: List<ChatSession>,
    isLoading: Boolean,
    onSessionClick: (ChatSession) -> Unit,
    onDelete: (ChatSession) -> Unit,
    onPin: (ChatSession) -> Unit,
    onArchive: (ChatSession) -> Unit,
    onUnarchive: (ChatSession) -> Unit,
    onRename: (ChatSession, String) -> Unit,
) {
    var showArchived by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
        // Header — title only, no New Chat button here
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Chat History", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BrahmBorder)

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrahmGold, modifier = Modifier.size(32.dp))
            }
        } else if (sessions.isEmpty() && archivedSessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💬", fontSize = 40.sp)
                    Text("No previous chats", style = MaterialTheme.typography.titleMedium.copy(color = BrahmMutedForeground))
                    Text("Start a conversation using ✏️ above", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            }
        } else {
            val grouped    = sessions.groupBy { formatSessionDate(it.lastAt) }
            val dateOrder  = listOf("Today", "Yesterday") + grouped.keys.filter { it !in listOf("Today", "Yesterday") }

            LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 32.dp)) {
                // ── Pinned section ──────────────────────────────────────────────
                val pinned = sessions.filter { it.isPinned }
                if (pinned.isNotEmpty()) {
                    stickyHeader {
                        SectionHeader("📌 Pinned")
                    }
                    items(pinned) { s ->
                        SessionRow(
                            session   = s,
                            onClick   = { onSessionClick(s) },
                            onDelete  = { onDelete(s) },
                            onPin     = { onPin(s) },
                            onArchive = { onArchive(s) },
                            onRename  = { name -> onRename(s, name) },
                        )
                    }
                }

                // ── Regular date-grouped sections ───────────────────────────────
                val unpinned = sessions.filter { !it.isPinned }
                val unpinnedGrouped = unpinned.groupBy { formatSessionDate(it.lastAt) }
                val unpinnedOrder   = listOf("Today", "Yesterday") + unpinnedGrouped.keys.filter { it !in listOf("Today","Yesterday") }

                unpinnedOrder.forEach { dateLabel ->
                    val group = unpinnedGrouped[dateLabel] ?: return@forEach
                    stickyHeader { SectionHeader(dateLabel) }
                    items(group) { s ->
                        SessionRow(
                            session   = s,
                            onClick   = { onSessionClick(s) },
                            onDelete  = { onDelete(s) },
                            onPin     = { onPin(s) },
                            onArchive = { onArchive(s) },
                            onRename  = { name -> onRename(s, name) },
                        )
                    }
                }

                // ── Archived toggle ─────────────────────────────────────────────
                if (archivedSessions.isNotEmpty()) {
                    item {
                        Surface(
                            onClick = { showArchived = !showArchived },
                            color = Color.Transparent,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    if (showArchived) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null, tint = BrahmMutedForeground, modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    "Archived (${archivedSessions.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                            }
                        }
                    }
                    if (showArchived) {
                        items(archivedSessions) { s ->
                            SessionRow(
                                session     = s,
                                isArchived  = true,
                                onClick     = { onSessionClick(s) },
                                onDelete    = { onDelete(s) },
                                onPin       = {},
                                onArchive   = { onUnarchive(s) },
                                onRename    = { name -> onRename(s, name) },
                                archiveLabel = "Unarchive",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Surface(color = BrahmBackground, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                color = BrahmMutedForeground, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp,
            ),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SessionRow(
    session: ChatSession,
    isArchived: Boolean   = false,
    archiveLabel: String  = "Archive",
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onRename: (String) -> Unit,
) {
    var showMenu   by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var renameText by remember(session.sessionId) { mutableStateOf(session.customName ?: session.preview) }

    Box {
        Surface(
            color = Color.Transparent,
            modifier = Modifier.combinedClickable(
                onClick      = onClick,
                onLongClick  = { showMenu = true },
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape)
                        .background(if (session.isPinned) BrahmGold.copy(alpha = 0.15f) else Color(0xFFFFF3CD)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (session.isPinned) Icons.Default.PushPin else Icons.Default.AutoAwesome,
                        contentDescription = null, tint = BrahmGold, modifier = Modifier.size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = session.preview,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = "${session.messageCount} messages",
                        style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground),
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrahmMutedForeground, modifier = Modifier.size(18.dp))
            }
        }

        // Long-press context menu
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            if (!isArchived) {
                DropdownMenuItem(
                    text = { Text(if (session.isPinned) "Unpin" else "Pin", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    onClick = { showMenu = false; onPin() },
                )
            }
            DropdownMenuItem(
                text = { Text("Rename", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                onClick = { showMenu = false; showRename = true },
            )
            DropdownMenuItem(
                text = { Text(archiveLabel, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp)) },
                onClick = { showMenu = false; onArchive() },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Delete", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) },
                onClick = { showMenu = false; onDelete() },
            )
        }
    }

    HorizontalDivider(modifier = Modifier.padding(start = 72.dp), color = BrahmBorder)

    // Rename dialog
    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename Chat", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    placeholder = { Text("Chat name…") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrahmGold),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) onRename(renameText.trim())
                        showRename = false
                    }
                ) { Text("Save", color = BrahmGold, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancel") }
            },
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyState(onSuggestionClick: (String) -> Unit) {
    val suggestions = listOf(
        "What does my moon sign say?",
        "Tell me about Sade Sati",
        "Explain Jupiter in 7th house",
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFFFFF3CD)),
            contentAlignment = Alignment.Center,
        ) { Text("🔮", fontSize = 32.sp) }
        Spacer(Modifier.height(16.dp))
        Text("Brahm AI", style = MaterialTheme.typography.titleLarge.copy(color = BrahmForeground, fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(6.dp))
        Text("Your Vedic astrology guide.\nAsk about planets, kundali, doshas & more.",
            style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground),
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        suggestions.forEach { suggestion ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFFF8E7),
                border = BorderStroke(1.dp, Color(0xFFE8D5A3)),
                modifier = Modifier.padding(vertical = 4.dp).clickable { onSuggestionClick(suggestion) },
            ) {
                Text(suggestion, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF92400E)))
            }
        }
    }
}

// ── Typing dots ───────────────────────────────────────────────────────────────
@Composable
private fun TypingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AiAvatar()
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = Color.White, shadowElevation = 1.dp,
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically) {
                repeat(3) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(BrahmMutedForeground))
                }
            }
        }
    }
}

@Composable
private fun AiAvatar() {
    Box(
        modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFFFF3CD)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrahmGold, modifier = Modifier.size(16.dp))
    }
}

// ── Chat bubble ───────────────────────────────────────────────────────────────
@Composable
private fun ChatBubble(msg: ChatMessage, onFollowUpClick: (String) -> Unit) {
    if (msg.role == "user") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = BrahmGold,
                modifier = Modifier.widthIn(max = 280.dp),
            ) {
                Text(text = msg.content, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Top) {
                AiAvatar()
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.widthIn(max = 280.dp),
                ) {
                    Text(text = msg.content, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = BrahmForeground,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp))
                }
            }
            if (msg.isComplete && msg.followUps.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.padding(start = 40.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    msg.followUps.forEach { question ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFF8E7),
                            border = BorderStroke(1.dp, Color(0xFFE8D5A3)),
                            modifier = Modifier.fillMaxWidth().clickable { onFollowUpClick(question) },
                        ) {
                            Text(text = question,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF92400E), fontWeight = FontWeight.Medium))
                        }
                    }
                }
            }
        }
    }
}
