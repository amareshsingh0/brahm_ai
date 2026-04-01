package com.bimoraai.brahm.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import kotlinx.coroutines.launch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bimoraai.brahm.core.components.brahmFieldColors
import com.bimoraai.brahm.core.theme.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.time.LocalTime

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
    val snackbarState   = remember { SnackbarHostState() }
    val scope           = rememberCoroutineScope()

    // ── Voice input ───────────────────────────────────────────────────────────
    val context      = LocalContext.current
    var isListening  by remember { mutableStateOf(false) }
    var voiceRms     by remember { mutableStateOf(0f) }
    val recognizer   = remember(context) { SpeechRecognizer.createSpeechRecognizer(context) }

    DisposableEffect(Unit) {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                voiceRms = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { isListening = false; voiceRms = 0f }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrEmpty()) input = text
                isListening = false; voiceRms = 0f
            }
            override fun onPartialResults(partial: Bundle?) {
                val text = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrEmpty()) input = text
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose { recognizer.destroy() }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, java.util.Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
        }
        input = ""
        recognizer.startListening(intent)
        isListening = true
        voiceRms = 0f
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startListening() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val visibleMessages = messages.filter { !(it.role == "assistant" && it.content.isEmpty()) }
    val showTyping      = isStreaming && messages.lastOrNull()?.content?.isEmpty() == true
    val canSend         = input.isNotBlank() && !isStreaming

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = {
            SnackbarHost(snackbarState, modifier = Modifier.padding(bottom = 8.dp)) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF1A1A1A),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(10.dp),
                )
            }
        },
        topBar = {
            Surface(color = Color.White, shadowElevation = 2.dp) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // ── Bot avatar ────────────────────────────────────────
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2)))
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("🔮", fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        // ── Name + status ─────────────────────────────────────
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Brahm AI",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BrahmForeground,
                                ),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF43A047)),
                                )
                                Text(
                                    if (isStreaming) "Typing…" else "Online • Vedic AI Guide",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isStreaming) BrahmGold else Color(0xFF43A047),
                                        fontSize = 10.sp,
                                    ),
                                )
                            }
                        }
                        // ── Action buttons ────────────────────────────────────
                        IconButton(onClick = {
                            vm.loadAllSessions()
                            showHistory = true
                        }) {
                            Icon(Icons.Default.History, contentDescription = "Chat History", tint = BrahmMutedForeground)
                        }
                        IconButton(onClick = { vm.startNewChat() }) {
                            Icon(Icons.Default.EditNote, contentDescription = "New Chat", tint = BrahmGold)
                        }
                    }
                    // ── Disclaimer banner ─────────────────────────────────────
                    Surface(color = Color(0xFFFFFBEB)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("⚠️", fontSize = 11.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "AI can make mistakes • Please verify important information",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF92400E), fontSize = 10.sp,
                                ),
                            )
                        }
                    }
                }
            }
        },
        containerColor = BrahmBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .imePadding(),
        ) {
            // ── Messages area ──────────────────────────────────────────────────
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
                val keyboardOpen = imeBottom > 100
                if (messages.isEmpty() && !showTyping && !keyboardOpen) {
                    EmptyState(onSuggestionClick = { vm.sendMessage(it) })
                }
                if (messages.isNotEmpty() || showTyping) {
                    val lastAssistantIdx = visibleMessages.indexOfLast { it.role == "assistant" && it.isComplete }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        itemsIndexed(visibleMessages) { idx, msg ->
                            ChatBubble(
                                msg = msg,
                                onFollowUpClick = { vm.sendFollowUp(it) },
                                isLastAssistant = idx == lastAssistantIdx && !isStreaming,
                                onCopy = { scope.launch { snackbarState.showSnackbar("Copied") } },
                                onRegenerate = { vm.regenerate() },
                            )
                        }
                        if (showTyping) {
                            item { TypingIndicator() }
                        }
                    }
                }
            }

            // ── Input bar ──────────────────────────────────────────────────────
            Surface(color = Color.White, shadowElevation = 8.dp) {
                if (isListening) {
                    VoiceInputBar(
                        rmsValue = voiceRms,
                        onStop   = { recognizer.stopListening(); isListening = false; voiceRms = 0f },
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Message Brahm AI...", color = BrahmMutedForeground, style = MaterialTheme.typography.bodyMedium) },
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = brahmFieldColors(),
                        )
                        Spacer(Modifier.width(8.dp))
                        if (input.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (canSend) BrahmGold else BrahmBorder)
                                    .clickable(enabled = canSend) {
                                        vm.sendMessage(input.trim())
                                        input = ""
                                        keyboard?.hide()
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(BrahmGold.copy(alpha = 0.1f))
                                    .clickable {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context, android.Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) startListening()
                                        else permLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = BrahmGold, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
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
    tapOpensMenu: Boolean = false,
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
            modifier = if (tapOpensMenu) {
                Modifier.combinedClickable(
                    onClick     = { showMenu = true },
                    onLongClick = { showMenu = true },
                )
            } else {
                Modifier.combinedClickable(
                    onClick     = onClick,
                    onLongClick = { showMenu = true },
                )
            },
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
                    colors = brahmFieldColors(),
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

// ── Voice recording bar ───────────────────────────────────────────────────────
@Composable
private fun VoiceInputBar(rmsValue: Float, onStop: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice")

    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.07f, targetValue = 0.18f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "a1",
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 1.0f, targetValue = 1.22f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "s2",
    )
    val ring3Scale by infiniteTransition.animateFloat(
        initialValue = 1.05f, targetValue = 1.32f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "s3",
    )

    val ampBoost = 1f + rmsValue * 0.4f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Cancel button
        IconButton(onClick = onStop, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Cancel",
                tint = BrahmMutedForeground,
                modifier = Modifier.size(20.dp),
            )
        }

        // Animated pulsing mic
        Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            // Outermost slow ring
            Box(
                modifier = Modifier
                    .size((80f * ring3Scale * ampBoost).dp)
                    .clip(CircleShape)
                    .background(BrahmGold.copy(alpha = ring1Alpha * 0.5f))
            )
            // Middle ring
            Box(
                modifier = Modifier
                    .size((64f * ring2Scale * ampBoost).dp)
                    .clip(CircleShape)
                    .background(BrahmGold.copy(alpha = ring1Alpha))
            )
            // Inner glow
            Box(
                modifier = Modifier
                    .size((50f * ampBoost).dp)
                    .clip(CircleShape)
                    .background(BrahmGold.copy(alpha = 0.22f))
            )
            // Core mic button — tap to stop
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrahmGold)
                    .clickable(onClick = onStop),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Stop listening",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // Labels
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(72.dp),
        ) {
            Text(
                "Listening",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = BrahmGold,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                "Tap to stop",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = BrahmMutedForeground,
                    fontSize = 10.sp,
                ),
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyState(onSuggestionClick: (String) -> Unit) {
    val suggestions = listOf(
        "What does my moon sign say?",
        "Tell me about Sade Sati",
        "Explain Jupiter in 7th house",
        "What is my lucky gemstone?",
    )
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 40.dp),   // shift slightly above true center
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Bot avatar
            /*
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFFFFF8E7), Color(0xFFFFECC0)))
                    ),
                contentAlignment = Alignment.Center,
            ) { Text("🔮", fontSize = 36.sp) }

            Spacer(Modifier.height(16.dp))

            Text(
                "Brahm AI",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = BrahmForeground, fontWeight = FontWeight.Bold,
                ),
            )
            */
            Spacer(Modifier.height(6.dp))
            Text(
                "Your Vedic astrology guide.\nAsk about planets, kundali, doshas & more.",
                style = MaterialTheme.typography.bodyMedium.copy(color = BrahmMutedForeground),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            // Suggestion chips — 2 per row
            val chunked = suggestions.chunked(2)
            chunked.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFFF8E7),
                            border = BorderStroke(1.dp, Color(0xFFE8D5A3)),
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp)
                                .clickable { onSuggestionClick(suggestion) },
                        ) {
                            Text(
                                suggestion,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF92400E),
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                        }
                    }
                }
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
private fun ChatBubble(
    msg: ChatMessage,
    onFollowUpClick: (String) -> Unit,
    isLastAssistant: Boolean = false,
    onCopy: () -> Unit = {},
    onRegenerate: () -> Unit = {},
) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current

    if (msg.role == "user") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                color = BrahmGold,
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(msg.content))
                            onCopy()
                        },
                    ),
            ) {
                Text(
                    text = msg.content,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {

            when {
                msg.content.isBlank() -> {
                    // Waiting for first token
                    Surface(
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, BrahmBorder),
                        modifier = Modifier.widthIn(max = 280.dp),
                    ) {
                        Text("…", modifier = Modifier.padding(14.dp, 10.dp), color = BrahmMutedForeground)
                    }
                }
                msg.isComplete -> RichAiCard(msg.content)
                // Streaming — close unclosed markers so markdown renders cleanly mid-stream
                else -> RichAiCard(closeUnclosedMarkers(msg.content))
            }

            // Follow-up chips
            if (msg.isComplete && msg.followUps.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.padding(start = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    msg.followUps.forEach { question ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFF8E7),
                            border = BorderStroke(1.dp, Color(0xFFE8D5A3)),
                            modifier = Modifier.fillMaxWidth().clickable { onFollowUpClick(question) },
                        ) {
                            Text(
                                text = question,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF92400E),
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                        }
                    }
                }
            }

            // Copy + Regenerate action row — only on completed messages
            if (msg.isComplete) {
                Row(
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Copy
                    IconButton(
                        onClick = {
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(msg.content))
                            onCopy()
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = BrahmMutedForeground,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    // Regenerate — only on the last assistant message
                    if (isLastAssistant) {
                        IconButton(
                            onClick = onRegenerate,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Regenerate",
                                tint = BrahmMutedForeground,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun isRichResponse(text: String): Boolean =
    text.length > 200 || text.contains('\n') || text.contains("**") ||
    text.contains("\n- ") || text.contains("\n• ") || text.contains("\n# ")

// ── Rich AI card ──────────────────────────────────────────────────────────────
@Composable
internal fun RichAiCard(text: String) {
    val sections = remember(text) { parseMarkdown(text) }

    Card(
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, BrahmBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(
            Brush.horizontalGradient(listOf(BrahmGold, Color(0xFFD4540A), Color.Transparent))
        ))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 2.dp),
        ) {
            Box(Modifier.size(6.dp).background(
                Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFD4540A))),
                shape = RoundedCornerShape(2.dp),
            ))
            Spacer(Modifier.width(6.dp))
            Text("BRAHM AI", style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFFB45309), fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp, fontSize = 8.sp,
            ))
        }
        Column(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
        ) {
            sections.forEachIndexed { idx, section ->
                MdBlock(section)
                val next = sections.getOrNull(idx + 1)
                val spacing: Dp = when {
                    section is MdSection.Heading  -> 10.dp
                    section is MdSection.Divider  -> 4.dp
                    next    is MdSection.Heading  -> 14.dp
                    next    is MdSection.Divider  -> 10.dp
                    next    is MdSection.Callout  -> 10.dp
                    next    is MdSection.Quote    -> 10.dp
                    else -> 8.dp
                }
                if (idx < sections.lastIndex) Spacer(Modifier.height(spacing))
            }
        }
    }
}

@Composable
internal fun MdBlock(section: MdSection) {
    when (section) {
        is MdSection.Heading -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BrahmGold)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = styledText(section.text),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = BrahmForeground,
                    letterSpacing = (-0.2).sp,
                    lineHeight = 24.sp,
                ),
            )
        }
        is MdSection.Para -> Text(
            text = styledText(section.text),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF1A1A1A),
                fontSize = 15.sp,
                lineHeight = 27.sp,
            ),
        )
        is MdSection.Quote -> Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                .border(1.dp, Color(0xFFE8D5A3), RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
        ) {
            Box(Modifier.width(3.dp).background(BrahmGold))
            Surface(
                color = Color(0xFFFFF8E7),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = styledText(section.text),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF92400E),
                        fontSize = 14.sp,
                        lineHeight = 25.sp,
                    ),
                )
            }
        }
        is MdSection.BulletItem -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .padding(top = 11.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(BrahmGold)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = styledText(section.text),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF1A1A1A),
                    fontSize = 15.sp,
                    lineHeight = 27.sp,
                ),
            )
        }
        is MdSection.NumberedItem -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                "${section.number}.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = BrahmGold,
                    fontSize = 12.sp,
                ),
                modifier = Modifier.padding(top = 2.dp).widthIn(min = 22.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = styledText(section.text),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF1A1A1A),
                    fontSize = 15.sp,
                    lineHeight = 27.sp,
                ),
            )
        }
        is MdSection.Callout -> Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFFF8E7),
            border = BorderStroke(1.dp, Color(0xFFE8D5A3)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                Text("💡", fontSize = 16.sp, modifier = Modifier.padding(top = 1.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    styledText(section.text),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF92400E),
                        fontSize = 14.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
        is MdSection.Divider -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
        ) {
            HorizontalDivider(Modifier.weight(1f), color = BrahmBorder)
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFE8D5A3)))
            Spacer(Modifier.width(3.dp))
            Box(Modifier.size(5.dp).clip(CircleShape).background(BrahmGold.copy(alpha = 0.5f)))
            Spacer(Modifier.width(3.dp))
            Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFE8D5A3)))
            Spacer(Modifier.width(6.dp))
            HorizontalDivider(Modifier.weight(1f), color = BrahmBorder)
        }
    }
}

// ── Markdown data model ───────────────────────────────────────────────────────
internal sealed class MdSection {
    data class Heading(val text: String)                        : MdSection()
    data class Para(val text: String)                           : MdSection()
    data class Quote(val text: String)                          : MdSection()
    data class BulletItem(val text: String)                     : MdSection()
    data class NumberedItem(val number: Int, val text: String)  : MdSection()
    data class Callout(val label: String, val text: String)     : MdSection()
    data class Divider(val label: String = "")                  : MdSection()
}

// ── Markdown parser ───────────────────────────────────────────────────────────
internal fun parseMarkdown(raw: String): List<MdSection> {
    val result  = mutableListOf<MdSection>()
    var numIdx  = 0

    for (line in raw.trim().lines()) {
        val t = line.trim()
        when {
            t.isEmpty()             -> numIdx = 0
            t.startsWith("### ")   -> result += MdSection.Heading(t.removePrefix("### "))
            t.startsWith("## ")    -> result += MdSection.Heading(t.removePrefix("## "))
            t.startsWith("# ")     -> result += MdSection.Heading(t.removePrefix("# "))
            t.startsWith("> ")     -> result += MdSection.Quote(t.removePrefix("> "))
            t.startsWith("- ") || t.startsWith("• ") || t.startsWith("* ") -> {
                result += MdSection.BulletItem(t.drop(2).trim()); numIdx = 0
            }
            t.matches(Regex("\\d+\\.\\s.*")) -> {
                numIdx++
                result += MdSection.NumberedItem(numIdx, t.substringAfter(". "))
            }
            t == "---" || t == "***" -> result += MdSection.Divider()
            t.startsWith("💡") || t.startsWith("📌") || t.startsWith("✨") || t.startsWith("⚠️") -> {
                result += MdSection.Callout("Note", t.drop(if (t.startsWith("⚠️")) 3 else 2).trim())
            }
            else -> { result += MdSection.Para(t); numIdx = 0 }
        }
    }
    return result.filter { it !is MdSection.Para || (it as MdSection.Para).text.isNotBlank() }
}

/**
 * Close any unclosed inline markdown markers so the streaming render looks correct.
 * e.g. "**partial" → "**partial**"  |  "*italic" → "*italic*"
 * Operates only on the last line (where partial tokens always arrive).
 */
internal fun closeUnclosedMarkers(text: String): String {
    if (text.isBlank()) return text
    val lines = text.lines().toMutableList()
    val last  = lines.lastOrNull() ?: return text

    // Close unclosed bold **
    val boldParts = last.split("**")
    val closedLast = if (boldParts.size % 2 == 0) {
        // Even split count = odd number of ** = unclosed
        last + "**"
    } else {
        // Try closing unclosed single * (that are not **)
        val stripped = last.replace(Regex("""\*\*(.*?)\*\*"""), "")
        val singleStars = stripped.count { it == '*' }
        if (singleStars % 2 != 0) "$last*" else last
    }
    lines[lines.lastIndex] = closedLast
    return lines.joinToString("\n")
}

// ── Inline bold/italic/code → AnnotatedString ─────────────────────────────────
internal fun styledText(text: String) = buildAnnotatedString {
    val regex  = Regex("""\*\*(.*?)\*\*|\*(.*?)\*|`(.*?)`""")
    var cursor = 0
    regex.findAll(text).forEach { m ->
        if (m.range.first > cursor) append(text.substring(cursor, m.range.first))
        when {
            m.groupValues[1].isNotEmpty() -> withStyle(
                SpanStyle(fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
            ) { append(m.groupValues[1]) }
            m.groupValues[2].isNotEmpty() -> withStyle(
                SpanStyle(fontStyle = FontStyle.Italic)
            ) { append(m.groupValues[2]) }
            m.groupValues[3].isNotEmpty() -> withStyle(
                SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFFF0F0F0), fontSize = 13.sp)
            ) { append(m.groupValues[3]) }
        }
        cursor = m.range.last + 1
    }
    if (cursor < text.length) append(text.substring(cursor))
}
