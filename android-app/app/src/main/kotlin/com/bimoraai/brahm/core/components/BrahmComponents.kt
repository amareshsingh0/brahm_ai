package com.bimoraai.brahm.core.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bimoraai.brahm.core.theme.*
import com.bimoraai.brahm.ui.chat.BotMsg
import com.bimoraai.brahm.ui.chat.PageBotViewModel
import kotlinx.coroutines.launch

// ─── BrahmCard — mirrors website Card component ───────────────────────────────
@Composable
fun BrahmCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val cardContent: @Composable ColumnScope.() -> Unit = content

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = BrahmCard,
                contentColor = BrahmForeground,
            ),
            border = BorderStroke(1.dp, BrahmBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            content = cardContent,
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = BrahmCard,
                contentColor = BrahmForeground,
            ),
            border = BorderStroke(1.dp, BrahmBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            content = cardContent,
        )
    }
}

// ─── BrahmButton — primary gold button (mirrors website saffron/gold CTA) ────
@Composable
fun BrahmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrahmGold,
            contentColor = Color.White,
            disabledContainerColor = BrahmBorder,
            disabledContentColor = BrahmMutedForeground,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ─── BrahmOutlinedButton — secondary bordered button ─────────────────────────
@Composable
fun BrahmOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = BrahmForeground,
        ),
        border = BorderStroke(1.dp, BrahmBorder),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

// ─── Loading spinner ──────────────────────────────────────────────────────────
@Composable
fun BrahmLoadingSpinner(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BrahmGold)
    }
}

// ─── Error view ───────────────────────────────────────────────────────────────
@Composable
fun BrahmErrorView(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = BrahmMutedForeground,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            BrahmButton(text = "Retry", onClick = onRetry, modifier = Modifier.width(120.dp))
        }
    }
}

// ─── Ask Brahm AI chip — floating CTA at bottom of tool result screens ────────
/**
 * Add this at the bottom of any tool result screen to let users ask Brahm AI
 * about their results. [onAsk] should navigate to the Chat tab.
 *
 * Usage:
 *   Box(Modifier.fillMaxSize()) {
 *       // ... screen content ...
 *       AskBrahmAiChip(
 *           modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
 *           onAsk = { tabNavController.navigate("tab_chat") },
 *       )
 *   }
 */
@Composable
fun AskBrahmAiChip(
    onAsk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onAsk,
        label = { Text("💬  Ask Brahm AI", style = MaterialTheme.typography.labelLarge) },
        leadingIcon = {
            Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = BrahmGold,
            labelColor = Color.White,
            leadingIconContentColor = Color.White,
        ),
        border = null,
        elevation = AssistChipDefaults.assistChipElevation(elevation = 4.dp),
    )
}

// ─── Shimmer loading box ───────────────────────────────────────────────────────
/**
 * A shimmering placeholder box for skeleton loading states.
 * Replaces CircularProgressIndicator on content-heavy screens.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue  = 0.55f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFFCCCCCC).copy(alpha = alpha)),
    )
}

// ─── Section header — gold accent line (mirrors website section headers) ──────
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(18.dp)
                .background(BrahmGold, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

// ─── PageBot — floating AI assistant available on every screen ────────────────

/**
 * Floating Action Button that opens a contextual AI chat bottom sheet.
 * Each page gets its own isolated ViewModel instance (keyed by pageContext).
 *
 * @param pageContext  Sent to backend — determines which AI context/session to use.
 * @param pageData     JSON string of the screen's current API result for AI analysis.
 */
@Composable
fun PageBotFab(
    pageContext: String = "general",
    pageData: String = "{}",
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }

    FloatingActionButton(
        onClick = { showSheet = true },
        modifier = modifier,
        containerColor = BrahmGold,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
    ) {
        Icon(Icons.Default.Android, contentDescription = "Ask Brahm AI")
    }

    if (showSheet) {
        PageBotSheet(
            pageContext = pageContext,
            pageData    = pageData,
            onDismiss   = { showSheet = false },
        )
    }
}

/**
 * Wraps a feature screen with the AI FAB at bottom-end.
 */
@Composable
fun WithAiFab(
    pageContext: String = "general",
    pageData: String = "{}",
    content: @Composable BoxScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        content()
        PageBotFab(
            pageContext = pageContext,
            pageData    = pageData,
            modifier    = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 16.dp),
        )
    }
}

// Per-page starter suggestions (mirrors website PAGE_SUGGESTIONS)
private val PAGE_SUGGESTIONS = mapOf(
    "gochar"        to listOf("Mere liye current transit kaisa hai?", "Shani ka asar kab tak rahega?", "Kaunsa graha vakri hai?"),
    "kp"            to listOf("Mera star lord kaun hai?", "Career kab banega KP ke hisaab se?", "Sub lord ka matlab batao"),
    "sadesati"      to listOf("Meri sade sati kab khatam hogi?", "Kaunsa phase chal raha hai?", "Sade sati ke upay batao"),
    "varshpal"      to listOf("Is saal ka sabse important graha?", "Varshphal lagna ka matlab?", "Muntha ka asar kya hoga?"),
    "kundali"       to listOf("Sabse strong graha kaun hai?", "Agle saal kaisa rahega?", "Career ke liye best period?"),
    "dosha"         to listOf("Mangal dosha ka kya asar hai?", "Dosha ke upay batao", "Kaal sarp dosh cancel hoga?"),
    "palmistry"     to listOf("Meri jeewan rekha kaisi hai?", "Career line kya kehti hai?", "Vivah rekha batao"),
    "horoscope"     to listOf("Aaj ka din kaisa rahega?", "Is mahine ka overview?", "Lucky time kab hai?"),
    "panchang"      to listOf("Aaj ka din kaisa hai?", "Rahukaal mein kya nahi karna chahiye?", "Aaj ki tithi ka mahatva?"),
    "compatibility" to listOf("Yeh score achha hai?", "Nadi dosha ke upay?", "Vivah ke liye sahi samay?"),
    "prashna"       to listOf("Kya meri wish poori hogi?", "Prashna kundali kya keh rahi hai?", "Lagna lord strong hai?"),
    "rectification" to listOf("Mera sahi janam samay kya hai?", "Rectification kaise kaam karta hai?", "Dasha se birth time confirm karo"),
)

private val PAGE_LABELS = mapOf(
    "gochar"        to "Gochar Analysis",
    "kp"            to "KP System",
    "sadesati"      to "Sade Sati",
    "varshpal"      to "Varshphal",
    "kundali"       to "Kundali",
    "dosha"         to "Dosha",
    "palmistry"     to "Palmistry",
    "horoscope"     to "Horoscope",
    "panchang"      to "Panchang",
    "compatibility" to "Compatibility",
    "prashna"       to "Prashna Kundali",
    "rectification" to "Rectification",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageBotSheet(
    pageContext: String = "general",
    pageData: String = "{}",
    onDismiss: () -> Unit,
    // key = pageContext gives each page its own isolated ViewModel & message history
    vm: PageBotViewModel = hiltViewModel(key = pageContext),
) {
    val msgs      by vm.msgs.collectAsState()
    val streaming by vm.streaming.collectAsState()
    var input     by remember { mutableStateOf("") }
    val listState  = rememberLazyListState()
    val suggestions = PAGE_SUGGESTIONS[pageContext] ?: listOf("Is report ke baare mein batao", "Kya yeh mere liye achha hai?", "Upay batao")
    val pageLabel   = PAGE_LABELS[pageContext] ?: "Brahm AI"

    LaunchedEffect(msgs.size) {
        if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.lastIndex)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
                .imePadding(),
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(BrahmGold),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ask Brahm AI", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Text(pageLabel, style = MaterialTheme.typography.labelSmall.copy(color = BrahmGold))
                }
                if (msgs.isNotEmpty()) {
                    TextButton(onClick = { vm.clear() }) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall, color = BrahmMutedForeground)
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = BrahmBorder)

            // Messages area
            if (msgs.isEmpty()) {
                // Suggestions chips
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Yeh puch sakte ho:",
                        style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground),
                    )
                    suggestions.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { vm.send(suggestion, pageContext, pageData); },
                            label   = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.fillMaxWidth(),
                            colors  = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = BrahmGold.copy(alpha = 0.08f),
                            ),
                            border  = SuggestionChipDefaults.suggestionChipBorder(
                                enabled        = true,
                                borderColor    = BrahmGold.copy(alpha = 0.3f),
                                borderWidth    = 1.dp,
                            ),
                        )
                    }
                }
            } else {
                val clipboardManager = LocalClipboardManager.current
                LazyColumn(
                    state             = listState,
                    modifier          = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding    = PaddingValues(vertical = 4.dp),
                ) {
                    itemsIndexed(msgs) { idx, msg ->
                        val isStreamingThis = streaming && !msg.isUser && idx == msgs.lastIndex
                        BotMsgBubble(
                            msg = msg,
                            isStreaming = isStreamingThis,
                            onLongPress = {
                                if (!msg.isUser) clipboardManager.setText(AnnotatedString(parseFollowups(msg.text).first))
                            },
                        )
                    }
                }

                // Copy + Regenerate row — shown below last assistant message after stream ends
                if (!streaming && msgs.lastOrNull()?.isUser == false) {
                    val lastBotText = parseFollowups(msgs.last().text).first
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextButton(
                            onClick = { clipboardManager.setText(AnnotatedString(lastBotText)) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(13.dp), tint = BrahmMutedForeground)
                            Spacer(Modifier.width(4.dp))
                            Text("Copy", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = BrahmMutedForeground))
                        }
                        TextButton(
                            onClick = { vm.regenerate() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(13.dp), tint = BrahmMutedForeground)
                            Spacer(Modifier.width(4.dp))
                            Text("Regenerate", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = BrahmMutedForeground))
                        }
                    }
                }

                // Followup suggestion chips — parsed from last assistant message after stream ends
                if (!streaming && msgs.isNotEmpty()) {
                    val lastAssistant = msgs.lastOrNull { !it.isUser }
                    val followups = lastAssistant?.let { parseFollowups(it.text).second } ?: emptyList()
                    if (followups.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            followups.forEach { q ->
                                SuggestionChip(
                                    onClick = { vm.send(q, pageContext, pageData) },
                                    label   = { Text(q, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors  = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = BrahmGold.copy(alpha = 0.08f),
                                    ),
                                    border  = SuggestionChipDefaults.suggestionChipBorder(
                                        enabled     = true,
                                        borderColor = BrahmGold.copy(alpha = 0.35f),
                                        borderWidth = 1.dp,
                                    ),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // Input row
            Row(
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value         = input,
                    onValueChange = { input = it },
                    modifier      = Modifier.weight(1f),
                    placeholder   = { Text("Kuchh bhi puchho…", color = BrahmMutedForeground) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(24.dp),
                    colors        = brahmFieldColors(),
                )
                val canSend = input.isNotBlank() && !streaming
                FilledIconButton(
                    onClick  = { if (canSend) { vm.send(input, pageContext, pageData); input = "" } },
                    enabled  = canSend,
                    colors   = IconButtonDefaults.filledIconButtonColors(containerColor = BrahmGold),
                ) {
                    if (streaming) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private val followupsRegex = Regex("""\[FOLLOWUPS:\s*(.*?)\]""", setOf(RegexOption.DOT_MATCHES_ALL))

/** Strips the [FOLLOWUPS: ...] tag from text and returns (cleanText, listOfSuggestions). */
private fun parseFollowups(text: String): Pair<String, List<String>> {
    val match = followupsRegex.find(text) ?: return text to emptyList()
    val clean = text.substring(0, match.range.first).trimEnd()
    val items = match.groupValues[1]
        .split("|")
        .map { it.trim().removeSurrounding("\"") }
        .filter { it.isNotBlank() }
    return clean to items
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BotMsgBubble(msg: BotMsg, isStreaming: Boolean = false, onLongPress: () -> Unit = {}) {
    if (msg.isUser) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = BrahmGold,
                modifier = Modifier.widthIn(max = 280.dp),
            ) {
                Text(
                    text     = msg.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style    = MaterialTheme.typography.bodySmall,
                    color    = Color.White,
                )
            }
        }
    } else {
        val (cleanText, _) = parseFollowups(msg.text)
        if (isStreaming || cleanText.isBlank()) {
            // Stream tokens as plain text
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = Color(0xFFF5F0E8),
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = {}, onLongClick = onLongPress),
            ) {
                Text(
                    text     = cleanText.ifBlank { "…" },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style    = MaterialTheme.typography.bodySmall,
                    color    = BrahmForeground,
                )
            }
        } else {
            // Complete — use rich card from ChatScreen (reuse same logic via Surface for PageBot)
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = Color(0xFFF5F0E8),
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = {}, onLongClick = onLongPress),
            ) {
                Text(
                    text     = cleanText.ifBlank { "…" },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style    = MaterialTheme.typography.bodySmall,
                    color    = BrahmForeground,
                )
            }
        }
    }
}

// ─── ScrollToTopFab — shows briefly when scrolling UP, auto-hides after 1.5s ──
@Composable
fun ScrollToTopFab(listState: LazyListState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }

    // Track scroll direction via snapshotFlow — detects each frame
    LaunchedEffect(listState) {
        var prevIndex = 0
        var prevOffset = 0
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val atTop    = index == 0
                val scrolledUp = index < prevIndex || (index == prevIndex && offset < prevOffset)
                prevIndex  = index
                prevOffset = offset
                if (atTop) visible = false
                else if (scrolledUp) visible = true
            }
    }

    // Auto-hide 0.5s after the last scroll frame (restarts on every scroll event)
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        kotlinx.coroutines.delay(500)
        visible = false
    }

    AnimatedVisibility(
        visible  = visible,
        modifier = modifier
            .navigationBarsPadding()
            .padding(end = 16.dp, bottom = 80.dp),
        enter = fadeIn() + scaleIn(initialScale = 0.7f),
        exit  = fadeOut() + scaleOut(targetScale = 0.7f),
    ) {
        FloatingActionButton(
            onClick        = { scope.launch { listState.animateScrollToItem(0) } },
            shape          = CircleShape,
            containerColor = BrahmGold,
            contentColor   = Color.White,
            modifier       = Modifier.size(44.dp),
            elevation      = FloatingActionButtonDefaults.elevation(4.dp),
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top", modifier = Modifier.size(22.dp))
        }
    }
}
