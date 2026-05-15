package com.example.bpscnotes.presentation.rooms

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.TierRoomsSocketManager
import com.example.bpscnotes.data.remote.api.ChatApiService
import com.example.bpscnotes.data.remote.api.ChatMessageDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ════════════════════════════════════════════════════════════
// FILE: presentation/rooms/RoomChatViewModel.kt
//
// Manages real-time room chat.
// Flow:
//   1. On open → GET /rooms/tiers/{tierKey}/messages (history)
//   2. Collect socket.roomMessages → append new messages live
//   3. On send → socket.sendChatMessage(text)
//               + optimistic local add (isMe=true, pending=true)
//               + when server echoes back → mark pending=false
// ════════════════════════════════════════════════════════════

data class ChatUiMessage(
    val id:         String,
    val senderId:   String,
    val senderName: String,
    val text:       String,
    val timeLabel:  String,
    val isMe:       Boolean,
    val isPending:  Boolean = false  // true = sent locally, not yet server-confirmed
)

data class ChatUiState(
    val messages:      List<ChatUiMessage> = emptyList(),
    val isLoadingHistory: Boolean          = true,
    val isSending:     Boolean             = false,
    val error:         String?             = null,
    val isConnected:   Boolean             = false
)

@HiltViewModel
class RoomChatViewModel @Inject constructor(
    private val chatApi:    ChatApiService,
    private val socket:     TierRoomsSocketManager,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // My userId from the JWT (stored in TokenStore after login)
    private val myUserId: String get() {
        val id = tokenStore.getUserId()
        if (id.isNullOrEmpty()) Log.w("RoomChatVM", "getUserId() is null — chat isMe detection disabled")
        return id ?: ""
    }

    companion object { private const val TAG = "RoomChatVM" }

    // ── Initialise for a specific tier room ──────────────────
    fun init(tierKey: String) {
        loadHistory(tierKey)
        observeLiveMessages()
        observeConnectionState()
    }

    // ── 1. Load history from REST ─────────────────────────────
    private fun loadHistory(tierKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }
            try {
                val res  = chatApi.getChatHistory(tierKey, limit = 50)
                val msgs = (res.data?.messages ?: emptyList()).map { dto ->
                    dto.toChatUiMessage(isMe = dto.senderId == myUserId)
                }
                _uiState.update { it.copy(messages = msgs, isLoadingHistory = false) }
            } catch (e: Exception) {
                Log.e(TAG, "loadHistory failed: ${e.message}", e)
                _uiState.update { it.copy(isLoadingHistory = false) }
                // Non-fatal — user can still chat, just won't see history
            }
        }
    }

    // ── 2. Observe live WebSocket messages ────────────────────
    private fun observeLiveMessages() {
        viewModelScope.launch {
            socket.roomMessages.collect { event ->
                val isMe = event.senderId == myUserId && myUserId.isNotEmpty()

                val currentMessages = _uiState.value.messages

                // Dedup: if server ID already in list, skip entirely (duplicate delivery)
                if (currentMessages.any { it.id == event.id }) return@collect

                // For my own echoed messages: find pending stub by tempId pattern + text.
                // We can't match by real id (stub has tempId). Match by: isMe + isPending + text.
                // Remove only the OLDEST pending stub with matching text (handles duplicates safely).
                var removedPending = false
                val withoutPending = if (isMe) {
                    val pendingIdx = currentMessages.indexOfFirst { msg ->
                        msg.isPending && msg.isMe && msg.text == event.message
                    }
                    if (pendingIdx >= 0) {
                        removedPending = true
                        currentMessages.toMutableList().also { it.removeAt(pendingIdx) }
                    } else currentMessages
                } else currentMessages

                val newMsg = ChatUiMessage(
                    id         = event.id,
                    senderId   = event.senderId,
                    senderName = if (isMe) "You" else event.senderName,
                    text       = event.message,
                    timeLabel  = formatTime(event.createdAt),
                    isMe       = isMe,
                    isPending  = false
                )

                _uiState.update { it.copy(messages = withoutPending + newMsg) }
            }
        }
    }

    // ── 3. Observe connection state ───────────────────────────
    private fun observeConnectionState() {
        viewModelScope.launch {
            socket.isConnected.collect { connected ->
                _uiState.update { it.copy(isConnected = connected) }
            }
        }
    }

    // ── 4. Send a message ─────────────────────────────────────
    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > 500) return

        // Optimistic: show message immediately with a local temp ID
        val tempId = "pending_${System.currentTimeMillis()}"
        val optimistic = ChatUiMessage(
            id         = tempId,
            senderId   = myUserId,
            senderName = "You",
            text       = trimmed,
            timeLabel  = "Sending…",
            isMe       = true,
            isPending  = true
        )
        _uiState.update { it.copy(messages = it.messages + optimistic) }

        // Emit via WebSocket — server will broadcast back to all including sender
        socket.sendChatMessage(trimmed)

        // Timeout: if no echo in 5s → mark as failed (show grey text)
        viewModelScope.launch {
            kotlinx.coroutines.delay(5000)
            val stillPending = _uiState.value.messages.any { it.id == tempId && it.isPending }
            if (stillPending) {
                _uiState.update { state ->
                    state.copy(messages = state.messages.map { msg ->
                        if (msg.id == tempId) msg.copy(timeLabel = "Failed to send", isPending = false)
                        else msg
                    })
                }
            }
        }
    }

    // ── Helper ────────────────────────────────────────────────
    private fun formatTime(isoString: String): String {
        return try {
            val parser  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date    = parser.parse(isoString) ?: return "Now"
            val display = SimpleDateFormat("h:mm a", Locale.getDefault())
            display.format(date)
        } catch (e: Exception) { "Now" }
    }
}

// ── Extension to convert DTO ──────────────────────────────────
private fun ChatMessageDto.toChatUiMessage(isMe: Boolean) =
    ChatUiMessage(
        id         = this.id,
        senderId   = this.senderId,
        senderName = if (isMe) "You" else this.senderName,
        text       = this.message,
        timeLabel  = try {
            val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val d = parser.parse(this.createdAt) ?: "Now"
            java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(d)
        } catch (e: Exception) { "Now" },
        isMe       = isMe,
        isPending  = false
    )
