package com.example.bpscnotes.presentation.studymaterials

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.ChatThreadDto
import com.example.bpscnotes.presentation.navigation.Routes.Screen

// ════════════════════════════════════════════════════════════
// ChatInboxScreen — Phase 5
//
// Lists all material chat threads for the current user, whether
// they're the buyer asking the uploader a question, or the
// uploader being messaged by a buyer about their material.
// Tapping a thread opens MaterialChatScreen.
// ════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInboxScreen(navController: NavHostController) {
    val viewModel: ChatInboxViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHost.showSnackbar(it); viewModel.clearError() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("💬 Chats", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> AppLoader()
                state.threads.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("💬", fontSize = 48.sp)
                        Text("No conversations yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Chats with buyers and sellers about study materials will show up here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp))
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.threads, key = { it.id }) { thread ->
                        ChatThreadRow(thread = thread, onClick = {
                            navController.navigate(Screen.MaterialChat.createRoute(thread.id))
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatThreadRow(thread: ChatThreadDto, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val hasUnread = thread.unreadCount > 0

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(if (hasUnread) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(BpscColors.PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    thread.otherPartyName?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = BpscColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        thread.otherPartyName ?: "Unknown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Bold,
                        color = cs.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    // Role badge: are we the buyer or the seller in this thread?
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(if (thread.role == "uploader") Color(0xFFF0FDF4) else Color(0xFFEFF6FF))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            if (thread.role == "uploader") "Buyer" else "Seller",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (thread.role == "uploader") BpscColors.Success else Color(0xFF2563EB),
                            fontSize = 9.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
                thread.materialTitle?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.Primary.copy(0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                thread.lastMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium,
                        color = if (hasUnread) cs.onSurface else cs.onSurfaceVariant,
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (thread.status == "escalated") {
                    Text("🚩 Escalated to support", style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                }
            }

            if (hasUnread) {
                Box(
                    modifier = Modifier.size(22.dp).clip(CircleShape).background(BpscColors.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (thread.unreadCount > 9) "9+" else "${thread.unreadCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp
                    )
                }
            }
        }
    }
}