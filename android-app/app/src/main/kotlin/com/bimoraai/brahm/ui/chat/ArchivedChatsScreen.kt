package com.bimoraai.brahm.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bimoraai.brahm.core.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArchivedChatsScreen(
    navController: NavController,
    vm: ChatViewModel = hiltViewModel(),
) {
    val archived by vm.archivedSessions.collectAsState()
    val loading  by vm.sessionsLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Archived Chats", fontWeight = FontWeight.Bold)
                        Text("${archived.size} conversation${if (archived.size != 1) "s" else ""}",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrahmCard,
                    titleContentColor = BrahmForeground,
                ),
            )
        },
        containerColor = BrahmBackground,
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrahmGold)
            }
        } else if (archived.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📦", fontSize = 40.sp)
                    Text("No archived chats", style = MaterialTheme.typography.titleMedium.copy(color = BrahmMutedForeground))
                    Text("Tap any chat to manage it", style = MaterialTheme.typography.bodySmall.copy(color = BrahmMutedForeground))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(archived, key = { it.sessionId }) { session ->
                    SessionRow(
                        session      = session,
                        isArchived   = true,
                        archiveLabel = "Unarchive",
                        tapOpensMenu = true,
                        onClick      = {},
                        onDelete     = { vm.deleteSession(session) },
                        onPin        = {},
                        onArchive    = { vm.unarchiveSession(session) },
                        onRename     = { name -> vm.renameSession(session, name) },
                    )
                }
            }
        }
    }
}
