package com.example.bpscnotes.presentation.studymaterials

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.MaterialChatMessageDto
import kotlinx.coroutines.launch

// ════════════════════════════════════════════════════════════
// MaterialChatScreen — Phase 5
//
// Buyer <-> uploader chat for a single purchased material.
// Header shows the other party's name + material title.
// "🚩 Report" button opens an escalation sheet (category + reason).
// ════════════════════════════════════════════════════════════

private val ESCALATION_CATEGORIES = listOf(
    "refund"            to "💰 Refund request",
    "dispute"           to "⚖️ Content dispute",
    "content"           to "📄 Content issue (wrong/incomplete)",
    "seller_misconduct" to "🚫 Seller misconduct",
    "other"             to "❓ Something else",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialChatScreen(
    navController: NavHostController,
    chatId: String,
) {
    val viewModel: MaterialChatViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(chatId) { viewModel.loadThread(chatId) }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(state.messages.size - 1) }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHost.showSnackbar(it); viewModel.clearError() }
    }
    LaunchedEffect(state.escalationSuccess) {
        state.escalationSuccess?.let { snackbarHost.showSnackbar(it); viewModel.clearEscalationSuccess() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.chat?.otherPartyName ?: "Chat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        state.chat?.materialTitle?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.chat?.status != "escalated") {
                        IconButton(onClick = { viewModel.openEscalateSheet() }) {
                            Icon(Icons.Rounded.Flag, contentDescription = "Report", tint = Color(0xFFE74C3C))
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF3E0))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("🚩 Escalated", style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                text = state.inputText,
                onTextChange = viewModel::setInputText,
                onSend = viewModel::sendMessage,
                enabled = state.chat?.status != "closed",
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = BpscColors.Primary)
                }
            } else if (state.messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("💬", fontSize = 40.sp)
                        Text("Say hello! Ask questions about this material.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.messages, key = { it.id }) { msg ->
                        MaterialChatBubble(message = msg, isMe = msg.senderId == state.currentUserId)
                    }
                }
            }
        }

        if (state.showEscalateSheet) {
            EscalateSheet(
                category = state.escalationCategory,
                reason = state.escalationReason,
                isSubmitting = state.isEscalating,
                onCategoryChange = viewModel::setEscalationCategory,
                onReasonChange = viewModel::setEscalationReason,
                onSubmit = viewModel::submitEscalation,
                onDismiss = viewModel::closeEscalateSheet,
            )
        }
    }
}

@Composable
private fun MaterialChatBubble(message: MaterialChatMessageDto, isMe: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isMe) 16.dp else 4.dp,
                        topEnd = if (isMe) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp,
                    )
                )
                .background(if (isMe) BpscColors.Primary else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    message.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                )
                message.createdAt?.let {
                    Text(
                        it.takeLast(8).take(5),  // best-effort HH:mm from ISO timestamp
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMe) Color.White.copy(0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp).navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 1000) onTextChange(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (enabled) "Type a message…" else "This chat is closed") },
                enabled = enabled,
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSend() })
            )
            FilledIconButton(
                onClick = onSend,
                enabled = enabled && text.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = BpscColors.Primary)
            ) {
                Icon(Icons.Rounded.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EscalateSheet(
    category: String,
    reason: String,
    isSubmitting: Boolean,
    onCategoryChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("🚩 Report to Support", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(
                "If you're facing a refund issue, content dispute, or seller misconduct, our support team will review this conversation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            ESCALATION_CATEGORIES.forEach { (key, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (category == key) BpscColors.PrimaryLight else Color.Transparent)
                        .selectable(selected = category == key, onClick = { onCategoryChange(key) })
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioButton(selected = category == key, onClick = { onCategoryChange(key) })
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Text("What's the issue?", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = reason,
                onValueChange = { if (it.length <= 1000) onReasonChange(it) },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("Describe what happened…") },
                shape = RoundedCornerShape(12.dp),
            )

            Button(
                onClick = onSubmit,
                enabled = !isSubmitting && reason.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Submit Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}