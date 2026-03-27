package com.bimoraai.brahm.core.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
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
 * Add to any screen's Scaffold floatingActionButton slot or via [WithAiFab].
 *
 * @param pageContext  Sent to backend as page_context so AI has screen-specific awareness.
 */
@Composable
fun PageBotFab(
    pageContext: String = "general",
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
        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Ask Brahm AI")
    }

    if (showSheet) {
        PageBotSheet(pageContext = pageContext, onDismiss = { showSheet = false })
    }
}

/**
 * Wraps a feature screen with the AI FAB at bottom-end.
 * Use in AppNavHost around screens that don't have their own Scaffold FAB slot.
 */
@Composable
fun WithAiFab(
    pageContext: String = "general",
    content: @Composable BoxScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        content()
        PageBotFab(
            pageContext = pageContext,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageBotSheet(
    pageContext: String = "general",
    onDismiss: () -> Unit,
    vm: PageBotViewModel = hiltViewModel(),
) {
    val msgs      by vm.msgs.collectAsState()
    val streaming by vm.streaming.collectAsState()
    var input     by remember { mutableStateOf("") }
    val listState  = rememberLazyListState()

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
                    Text("Vedic astrology assistant", style = MaterialTheme.typography.labelSmall.copy(color = BrahmMutedForeground))
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
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Ask anything about astrology…",
                        color = BrahmMutedForeground,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    state             = listState,
                    modifier          = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding    = PaddingValues(vertical = 4.dp),
                ) {
                    items(msgs) { msg -> BotMsgBubble(msg) }
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
                    placeholder   = { Text("Type your question…", color = BrahmMutedForeground) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(24.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = BrahmGold,
                        unfocusedBorderColor = BrahmBorder,
                    ),
                )
                val canSend = input.isNotBlank() && !streaming
                FilledIconButton(
                    onClick  = { if (canSend) { vm.send(input, pageContext); input = "" } },
                    enabled  = canSend,
                    colors   = IconButtonDefaults.filledIconButtonColors(containerColor = BrahmGold),
                ) {
                    if (streaming) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BotMsgBubble(msg: BotMsg) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart    = 16.dp, topEnd = 16.dp,
                bottomStart = if (msg.isUser) 16.dp else 4.dp,
                bottomEnd   = if (msg.isUser) 4.dp  else 16.dp,
            ),
            color    = if (msg.isUser) BrahmGold else Color(0xFFF5F0E8),
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(
                text     = msg.text.ifBlank { "…" },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style    = MaterialTheme.typography.bodySmall,
                color    = if (msg.isUser) Color.White else BrahmForeground,
            )
        }
    }
}

// ─── ScrollToTopFab — shows in bottom-right when user scrolls down 3+ items ──
@Composable
fun ScrollToTopFab(listState: LazyListState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val visible by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(initialScale = 0.7f),
        exit  = fadeOut() + scaleOut(targetScale = 0.7f),
    ) {
        FloatingActionButton(
            onClick = { scope.launch { listState.animateScrollToItem(0) } },
            shape = CircleShape,
            containerColor = BrahmGold,
            contentColor   = Color.White,
            modifier = Modifier.size(44.dp),
            elevation = FloatingActionButtonDefaults.elevation(4.dp),
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top", modifier = Modifier.size(22.dp))
        }
    }
}
