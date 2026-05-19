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
// RoomChatViewModel — real-time room chat
//
// ROOT CAUSES FIXED:
//
// 1. "Failed to send" when socket is connecting:
//    sendMessage() optimistically added the message to UI, then called
//    socket.sendChatMessage(). If socket was still connecting, sendChatMessage
//    returned immediately (old code returned early if !socket.connected()).
//    8s timeout → "Failed to send".
//    Fix: TierRoomsSocketManager now queues messages while connecting (see that file).
//    Here, we also listen to socketErrors to show server-side rejection reasons.
//
// 2. "Reconnecting" always showing on first open:
//    socket.isConnected is a StateFlow that starts as false.
//    observeConnectionState() collected it and set isConnected=false immediately.
//    ChatSheet saw isConnected=false → after 2s grace → "Reconnecting...".
//    Fix: Added connectionStatus (CONNECTING | LIVE | RECONNECTING) with proper
//    state transitions using hasConnectedBefore flag from TierRoomsSocketManager.
//
// 3. Wrong message sides (my messages on left, friend's on right):
//    isMine() compared senderId with myUserId from TokenStore.
//    If getUserId() returned null or whitespace-padded value, all messages
//    appeared as "received" (left side) including your own.
//    Fix: Normalize both sides with trim(). Fail-safe: if myUserId is truly
//    empty after init, reload from TokenStore before each comparison.
//
// 4. History replaying on every reconnect (minor UX issue):
//    observeConnectionState collected isConnected=true on EVERY reconnect and
//    called loadHistory() → message list jumped to newest.
//    Fix: Only reload history on reconnect if socket previously disconnected
//    (i.e., was connected before). Skip reload on initial connection since
//    init() already loaded history.
// ════════════════════════════════════════════════════════════

enum class ChatConnectionStatus {
    CONNECTING,    // Never connected yet — initial state
    LIVE,          // Connected and ready
    RECONNECTING   // Was connected, then dropped
}

data class ChatUiMessage(
    val id:         String,
    val senderId:   String,
    val senderName: String,
    val text:       String,
    val timeLabel:  String,
    val isMe:       Boolean,
    val isPending:  Boolean = false
)

data class ChatUiState(
    val messages:          List<ChatUiMessage>    = emptyList(),
    val isLoadingHistory:  Boolean                = true,
    val connectionStatus:  ChatConnectionStatus   = ChatConnectionStatus.CONNECTING,
    val serverError:       String?                = null,  // e.g. "Too fast. Slow down."
    val error:             String?                = null
) {
    // Convenience for ChatSheet
    val isConnected: Boolean get() = connectionStatus == ChatConnectionStatus.LIVE
}

@HiltViewModel
class RoomChatViewModel @Inject constructor(
    private val chatApi:    ChatApiService,
    private val socket:     TierRoomsSocketManager,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    companion object { private const val TAG = "RoomChatVM" }

    private var activeTierKey: String = ""
    private var socketSubscribed      = false

    // myUserId: normalized, trimmed UUID from TokenStore
    private var myUserId: String = ""
        get() {
            if (field.isEmpty()) {
                field = tokenStore.getUserId()?.trim() ?: ""
            }
            return field
        }

    // ── Entry point ────────────────────────────────────────────
    fun init(tierKey: String) {
        activeTierKey = tierKey
        // Eagerly load userId so isMine() works correctly for history messages
        myUserId = tokenStore.getUserId()?.trim() ?: ""
        if (myUserId.isEmpty()) {
            Log.w(TAG, "⚠️ getUserId() returned null/empty — message sides may be wrong")
        } else {
            Log.d(TAG, "My userId: $myUserId")
        }

        // Always reload history when sheet opens (user may have dismissed and reopened)
        loadHistory(tierKey)

        // Set up socket subscriptions only once per ViewModel lifetime
        if (!socketSubscribed) {
            socketSubscribed = true
            observeLiveMessages()
            observeConnectionState()
            observeServerErrors()
        }
    }

    // ── 1. History from REST ───────────────────────────────────
    private fun loadHistory(tierKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true, error = null) }
            try {
                val res  = chatApi.getChatHistory(tierKey, limit = 50)
                val msgs = (res.data?.messages ?: emptyList()).map { dto ->
                    dto.toChatUiMessage(isMe = isMine(dto.senderId))
                }
                _uiState.update { it.copy(messages = msgs, isLoadingHistory = false) }
                Log.d(TAG, "Loaded ${msgs.size} history messages for $tierKey")
            } catch (e: Exception) {
                Log.e(TAG, "loadHistory failed: ${e.message}", e)
                _uiState.update { it.copy(isLoadingHistory = false, error = "Couldn't load history") }
            }
        }
    }

    // ── 2. Live WebSocket messages ─────────────────────────────
    private fun observeLiveMessages() {
        viewModelScope.launch {
            socket.roomMessages.collect { event ->
                if (event.tierKey != activeTierKey) return@collect

                val isMe = isMine(event.senderId)
                val msgs = _uiState.value.messages

                // Handle pending stub replacement BEFORE ID-dedup.
                // Pending stubs have a tempId that won't match the real ID.
                if (isMe) {
                    val pendingIdx = msgs.indexOfFirst {
                        it.isPending && it.isMe && it.text == event.message
                    }
                    if (pendingIdx >= 0) {
                        val confirmed = ChatUiMessage(
                            id         = event.id,
                            senderId   = event.senderId,
                            senderName = "You",
                            text       = event.message,
                            timeLabel  = formatTime(event.createdAt),
                            isMe       = true,
                            isPending  = false
                        )
                        _uiState.update {
                            it.copy(messages = it.messages.toMutableList().also { list ->
                                list[pendingIdx] = confirmed
                            })
                        }
                        return@collect
                    }
                }

                // Dedup: skip if ID already in list (history + live overlap)
                if (msgs.any { it.id == event.id }) return@collect

                val newMsg = ChatUiMessage(
                    id         = event.id,
                    senderId   = event.senderId,
                    senderName = if (isMe) "You" else event.senderName.ifBlank { "Member" },
                    text       = event.message,
                    timeLabel  = formatTime(event.createdAt),
                    isMe       = isMe,
                    isPending  = false
                )
                _uiState.update { it.copy(messages = it.messages + newMsg) }
            }
        }
    }

    // ── 3. Connection state ────────────────────────────────────
    private fun observeConnectionState() {
        viewModelScope.launch {
            // Combine isConnected and hasConnectedBefore to determine status
            combine(
                socket.isConnected,
                socket.hasConnectedBefore
            ) { connected, hasConnectedBefore ->
                when {
                    connected          -> ChatConnectionStatus.LIVE
                    hasConnectedBefore -> ChatConnectionStatus.RECONNECTING
                    else               -> ChatConnectionStatus.CONNECTING
                }
            }.collect { status ->
                _uiState.update { it.copy(connectionStatus = status) }

                // Reload history only on RECONNECT (not on initial connect,
                // since init() already called loadHistory())
                if (status == ChatConnectionStatus.LIVE
                    && socket.hasConnectedBefore.value
                    && activeTierKey.isNotEmpty()
                ) {
                    // Small delay to let the server settle before fetching
                    kotlinx.coroutines.delay(500)
                    loadHistory(activeTierKey)
                }
            }
        }
    }

    // ── 4. Server errors (WsException forwarded from gateway) ─
    private fun observeServerErrors() {
        viewModelScope.launch {
            socket.socketErrors.collect { errorMsg ->
                Log.e(TAG, "Server error: $errorMsg")
                // Mark any pending messages as failed when server rejects
                _uiState.update { state ->
                    val hasPending = state.messages.any { it.isPending }
                    if (hasPending) {
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.isPending) msg.copy(timeLabel = "Failed to send", isPending = false)
                                else msg
                            },
                            serverError = errorMsg
                        )
                    } else {
                        state.copy(serverError = errorMsg)
                    }
                }
                // Auto-clear server error after 3s
                kotlinx.coroutines.delay(3_000)
                _uiState.update { it.copy(serverError = null) }
            }
        }
    }

    // ── 5. Send a message ──────────────────────────────────────
    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > 500) return

        // Refresh userId right before sending (safety net)
        if (myUserId.isEmpty()) {
            myUserId = tokenStore.getUserId()?.trim() ?: ""
        }

        val tempId     = "pending_${System.currentTimeMillis()}"
        val optimistic = ChatUiMessage(
            id         = tempId,
            senderId   = myUserId,
            senderName = "You",
            text       = trimmed,
            timeLabel  = "Sending…",
            isMe       = true,   // Always true — I am the sender
            isPending  = true
        )
        _uiState.update { it.copy(messages = it.messages + optimistic) }

        // TierRoomsSocketManager.sendChatMessage() now queues the message if
        // the socket is still connecting, so this will succeed even during initial connect
        socket.sendChatMessage(trimmed)
        Log.d(TAG, "Sent: '${trimmed.take(40)}'")

        // Timeout: if server doesn't echo within 10s, mark as failed
        viewModelScope.launch {
            kotlinx.coroutines.delay(10_000L)
            val stillPending = _uiState.value.messages.any { it.id == tempId && it.isPending }
            if (stillPending) {
                _uiState.update { state ->
                    state.copy(messages = state.messages.map { msg ->
                        if (msg.id == tempId) msg.copy(timeLabel = "Failed to send", isPending = false)
                        else msg
                    })
                }
                Log.w(TAG, "Message timed out after 10s")
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────

    /**
     * Robust isMe check.
     * Both senderId and myUserId are trimmed before comparison.
     * Returns false (not mine) if myUserId is empty — safe default.
     */
    private fun isMine(senderId: String): Boolean {
        val uid = myUserId
        if (uid.isEmpty()) return false
        return senderId.trim() == uid
    }

    private fun formatTime(isoString: String): String {
        if (isoString.isEmpty()) return "Now"
        return try {
            val parsers = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX"
            )
            var date: Date? = null
            for (fmt in parsers) {
                try {
                    val p = SimpleDateFormat(fmt, Locale.getDefault())
                    p.timeZone = TimeZone.getTimeZone("UTC")
                    date = p.parse(isoString)
                    if (date != null) break
                } catch (_: Exception) {}
            }
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(date ?: Date())
        } catch (_: Exception) { "Now" }
    }
}

// ── DTO → UI model conversion ─────────────────────────────────
private fun ChatMessageDto.toChatUiMessage(isMe: Boolean) = ChatUiMessage(
    id         = this.id,
    senderId   = this.senderId,
    senderName = if (isMe) "You" else this.senderName.ifBlank { "Member" },
    text       = this.message,
    timeLabel  = try {
        val parsers = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'")
        var date: Date? = null
        for (fmt in parsers) {
            try {
                val p = SimpleDateFormat(fmt, Locale.getDefault())
                p.timeZone = TimeZone.getTimeZone("UTC")
                date = p.parse(this.createdAt)
                if (date != null) break
            } catch (_: Exception) {}
        }
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(date ?: Date())
    } catch (_: Exception) { "Now" },
    isMe      = isMe,
    isPending = false
)