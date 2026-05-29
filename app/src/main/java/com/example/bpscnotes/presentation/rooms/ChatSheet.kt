package com.example.bpscnotes.presentation.rooms

import com.example.bpscnotes.core.language.LocalStrings
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.TierMemberDto

// ════════════════════════════════════════════════════════════
// ChatSheet — Real-time room chat bottom sheet
//
// CHANGES:
// 1. Connection status now uses ChatConnectionStatus enum (CONNECTING/LIVE/RECONNECTING)
//    instead of a boolean isConnected.
//    - CONNECTING  = initial state, no grace period needed, shows "Connecting..."
//    - LIVE        = connected, shows green dot + str.chatLive
//    - RECONNECTING = was connected, dropped, shows orange dot + "Reconnecting..."
//    This eliminates the false "Reconnecting" on first open.
//
// 2. Server error toast shown when gateway rejects a message (e.g. "Too fast").
//    Previously these errors were silently swallowed.
// ════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSheet(
    tierKey:   String,
    member:    TierMemberDto?,
    myName:    String,
    onDismiss: () -> Unit,
    viewModel: RoomChatViewModel = hiltViewModel()
) {
    val str = LocalStrings.current
    val state      by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputText  by remember { mutableStateOf("") }
    val listState  = rememberLazyListState()

    LaunchedEffect(tierKey) {
        viewModel.init(tierKey)
    }

    // Auto-scroll only when near bottom
    LaunchedEffect(state.messages.size) {
        if (state.messages.isEmpty()) return@LaunchedEffect
        val lastVisible  = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        val total        = listState.layoutInfo.totalItemsCount
        val isNearBottom = total == 0 || lastVisible >= total - 3
        if (isNearBottom) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    fun send() {
        if (inputText.isNotBlank()) {
            viewModel.sendMessage(inputText)
            inputText = ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color(0xFF0D1628),
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle       = null
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(520.dp)) {

            // ── HEADER ────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF0A1020))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(BpscColors.Primary.copy(0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (member != null) {
                                Text(
                                    member.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    style      = MaterialTheme.typography.titleSmall,
                                    color      = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            } else {
                                Icon(Icons.Rounded.Forum, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Column {
                            Text(
                                member?.name ?: str.chatRoomChat,
                                style      = MaterialTheme.typography.titleSmall,
                                color      = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            // FIX: Use ChatConnectionStatus instead of Boolean
                            // No more false "Reconnecting" on first open
                            val (dotColor, statusText) = when (state.connectionStatus) {
                                ChatConnectionStatus.LIVE         -> BpscColors.Success to str.chatLive
                                ChatConnectionStatus.CONNECTING   -> Color.Gray          to str.chatConnecting
                                ChatConnectionStatus.RECONNECTING -> Color(0xFFFFA726)   to str.chatReconnecting
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(6.dp).clip(CircleShape).background(dotColor)
                                )
                                Text(
                                    statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = dotColor
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Rounded.Close, null,
                            tint     = Color.White.copy(0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // FIX: Server error banner (e.g. "Too fast. Slow down.")
            AnimatedVisibility(
                visible = state.serverError != null,
                enter   = slideInVertically() + fadeIn(),
                exit    = slideOutVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFF7B1FA2).copy(0.8f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        "⚠️ ${state.serverError}",
                        style  = MaterialTheme.typography.labelSmall,
                        color  = Color.White
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(0.06f))

            // ── MESSAGES ──────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoadingHistory -> {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color       = BpscColors.Primary,
                                    modifier    = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    str.chatLoading,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(0.4f)
                                )
                            }
                        }
                    }

                    state.messages.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("💬", fontSize = 36.sp)
                                Text(
                                    str.chatNoMessages,
                                    style      = MaterialTheme.typography.titleSmall,
                                    color      = Color.White.copy(0.5f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    str.chatStartConversation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(0.3f)
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier            = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                            state               = listState,
                            contentPadding      = PaddingValues(vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item(key = "date_separator") {
                                Box(
                                    modifier         = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        str.chatToday,
                                        style    = MaterialTheme.typography.labelSmall,
                                        color    = Color.White.copy(0.3f),
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(0.06f))
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            items(state.messages, key = { it.id }) { msg ->
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = true,
                                    enter   = fadeIn() + slideInVertically { it / 2 }
                                ) {
                                    ChatBubble(message = msg)
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(0.06f))

            // ── INPUT BAR ────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A1020))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = inputText,
                    onValueChange = { if (it.length <= 500) inputText = it },
                    modifier      = Modifier.weight(1f),
                    placeholder   = {
                        Text(
                            if (member != null) "Message ${member.name.split(" ").first()}…"
                            else str.chatMessageHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(0.3f)
                        )
                    },
                    shape    = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = BpscColors.Primary,
                        unfocusedBorderColor = Color.White.copy(0.12f),
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = BpscColors.Primary
                    )
                )

                if (inputText.length > 400) {
                    Text(
                        "${500 - inputText.length}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (inputText.length > 480) Color(0xFFEF5350) else Color.White.copy(0.4f)
                    )
                }

                // Send button — enabled even when not connected (message will be queued)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank()) BpscColors.Primary
                            else Color.White.copy(0.08f)
                        )
                        .clickable(enabled = inputText.isNotBlank()) { send() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Send, null,
                        tint     = if (inputText.isNotBlank()) Color.White else Color.White.copy(0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// CHAT BUBBLE — left for others, right for me
// ════════════════════════════════════════════════════════════
@Composable
fun ChatBubble(message: ChatUiMessage) {
    val str = LocalStrings.current
    val isMe      = message.isMe
    val isPending = message.isPending

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        if (!isMe) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape)
                    .background(BpscColors.Primary.copy(0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    message.senderName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(6.dp))
        }

        Column(
            modifier            = Modifier.widthIn(max = 250.dp),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            if (!isMe) {
                Text(
                    message.senderName,
                    style      = MaterialTheme.typography.labelSmall,
                    color      = BpscColors.Primary.copy(0.8f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 10.sp,
                    modifier   = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart    = if (isMe) 16.dp else 4.dp,
                            topEnd      = if (isMe) 4.dp else 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd   = 16.dp
                        )
                    )
                    .background(
                        when {
                            isMe && isPending -> BpscColors.Primary.copy(0.5f)
                            isMe              -> BpscColors.Primary
                            else              -> Color.White.copy(0.1f)
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPending) Color.White.copy(0.6f) else Color.White
                )
            }

            Row(
                modifier              = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    message.timeLabel,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = if (message.timeLabel == str.chatFailedSend) Color(0xFFEF5350)
                    else Color.White.copy(0.35f),
                    fontSize = 9.sp
                )
                if (isMe && !isPending && message.timeLabel != str.chatFailedSend) {
                    Icon(
                        Icons.Rounded.DoneAll, null,
                        tint     = BpscColors.Success.copy(0.6f),
                        modifier = Modifier.size(10.dp)
                    )
                }
                if (isMe && isPending) {
                    Icon(
                        Icons.Rounded.Schedule, null,
                        tint     = Color.White.copy(0.35f),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        if (isMe) Spacer(Modifier.width(6.dp))
    }
}