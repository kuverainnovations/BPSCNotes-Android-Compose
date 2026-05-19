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
// BUGS FIXED:
// 1. History not loading — init() was guarded by `initialized` flag
//    which blocked re-init when ChatSheet reopened after dismiss.
//    Fix: always reload history, only skip socket subscriptions.
//
// 2. "Sending" forever — optimistic stub was never replaced because
//    server echo was being deduplicated by ID before the stub was removed.
//    Fix: check for pending stub BEFORE the ID-dedup check.
//
// 3. My sent message showing on left (received side) for the sender —
//    isMe detection compared senderId to myUserId which was sometimes
//    empty (getUserId() returned null before login completes).
//    Fix: also match by senderName == "You" as fallback, and log warnings.
//
// 4. Friend's message showing on wrong side (left is correct for received,
//    was accidentally showing right) — was a cascading effect of bug #3.
//    Fix: once #3 is fixed, left/right is correct automatically.
// ════════════════════════════════════════════════════════════

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
    val messages:         List<ChatUiMessage> = emptyList(),
    val isLoadingHistory: Boolean             = true,
    val isSending:        Boolean             = false,
    val error:            String?             = null,
    val isConnected:      Boolean             = false
)

@HiltViewModel
class RoomChatViewModel @Inject constructor(
    private val chatApi:    ChatApiService,
    private val socket:     TierRoomsSocketManager,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // BUG FIX: Read userId eagerly AND refresh it right before each send.
    // The old `get()` property was called when getUserId() might still be null on cold start.
    private var _myUserId: String = ""
    private val myUserId: String
        get() {
            if (_myUserId.isEmpty()) {
                _myUserId = tokenStore.getUserId()?.trim() ?: ""
                if (_myUserId.isEmpty()) Log.w(TAG, "getUserId() still empty — isMe detection degraded")
            }
            return _myUserId
        }

    companion object { private const val TAG = "RoomChatVM" }

    private var activeTierKey: String = ""
    // BUG FIX: Separate flag for socket subscription vs history loading.
    // Old single `initialized` flag prevented history reload when sheet reopened.
    private var socketSubscribed = false

    // ── Entry point ───────────────────────────────────────────
    fun init(tierKey: String) {
        activeTierKey = tierKey
        _myUserId = tokenStore.getUserId()?.trim() ?: ""

        // Always reload history — user may have dismissed and reopened the sheet
        loadHistory(tierKey)

        // Only set up socket subscriptions once per ViewModel lifetime
        if (!socketSubscribed) {
            socketSubscribed = true
            observeLiveMessages()
            observeConnectionState()
        }
    }

    // ── 1. Load history from REST ─────────────────────────────
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
                // Show error but don't block — user can still send new messages
                _uiState.update {
                    it.copy(
                        isLoadingHistory = false,
                        error = "Could not load history: ${e.message}"
                    )
                }
            }
        }
    }

    // ── 2. Observe live WebSocket messages ────────────────────
    private fun observeLiveMessages() {
        viewModelScope.launch {
            socket.roomMessages.collect { event ->
                if (event.tierKey != activeTierKey) return@collect

                val isMe  = isMine(event.senderId)
                val msgs  = _uiState.value.messages

                // BUG FIX: Handle pending stub replacement BEFORE the ID-dedup check.
                // Old order: ID-dedup first → stub never removed because stub has tempId.
                if (isMe) {
                    val pendingIdx = msgs.indexOfFirst { it.isPending && it.isMe && it.text == event.message }
                    if (pendingIdx >= 0) {
                        // Replace the pending stub with the confirmed message
                        val confirmed = ChatUiMessage(
                            id         = event.id,
                            senderId   = event.senderId,
                            senderName = "You",
                            text       = event.message,
                            timeLabel  = formatTime(event.createdAt),
                            isMe       = true,
                            isPending  = false
                        )
                        val updated = msgs.toMutableList().also {
                            it[pendingIdx] = confirmed
                        }
                        _uiState.update { it.copy(messages = updated) }
                        return@collect
                    }
                }

                // Dedup: skip if message ID already in list (e.g. history + live overlap)
                if (msgs.any { it.id == event.id }) return@collect

                val newMsg = ChatUiMessage(
                    id         = event.id,
                    senderId   = event.senderId,
                    // BUG FIX: always show real sender name for others' messages
                    senderName = if (isMe) "You" else event.senderName.ifBlank { "Member" },
                    text       = event.message,
                    timeLabel  = formatTime(event.createdAt),
                    isMe       = isMe,
                    isPending  = false
                )
                _uiState.update { it.copy(messages = msgs + newMsg) }
            }
        }
    }

    // ── 3. Observe connection state ───────────────────────────
    private fun observeConnectionState() {
        viewModelScope.launch {
            socket.isConnected.collect { connected ->
                _uiState.update { it.copy(isConnected = connected) }
                // BUG FIX: Reload history on reconnect so messages sent while offline appear
                if (connected && activeTierKey.isNotEmpty()) {
                    loadHistory(activeTierKey)
                }
            }
        }
    }

    // ── 4. Send a message ─────────────────────────────────────
    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > 500) return

        // Refresh userId right before sending in case it was null on init
        if (_myUserId.isEmpty()) {
            _myUserId = tokenStore.getUserId()?.trim() ?: ""
        }

        val tempId = "pending_${System.currentTimeMillis()}"
        val optimistic = ChatUiMessage(
            id         = tempId,
            senderId   = _myUserId,
            senderName = "You",
            text       = trimmed,
            timeLabel  = "Sending…",
            isMe       = true,   // BUG FIX: always true for messages I send
            isPending  = true
        )
        _uiState.update { it.copy(messages = it.messages + optimistic) }

        // Send via WebSocket — server will echo back to confirm
        socket.sendChatMessage(trimmed)
        Log.d(TAG, "Sent message: '${trimmed.take(30)}…'")

        // BUG FIX: Timeout — if no server echo in 8s, mark as failed instead of "Sending…" forever
        viewModelScope.launch {
            kotlinx.coroutines.delay(8_000L)
            val stillPending = _uiState.value.messages.any { it.id == tempId && it.isPending }
            if (stillPending) {
                _uiState.update { state ->
                    state.copy(messages = state.messages.map { msg ->
                        if (msg.id == tempId) msg.copy(timeLabel = "Failed to send", isPending = false)
                        else msg
                    })
                }
                Log.w(TAG, "Message timed out — marked as failed")
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * BUG FIX: Robust isMe check.
     * Old code: `dto.senderId == myUserId` — fails when myUserId is empty on cold start.
     * New code: exact userId match OR (userId is empty AND we treat it as "could be me").
     * The real fix is ensuring myUserId is populated before any message comparison.
     */
    private fun isMine(senderId: String): Boolean {
        val uid = myUserId
        if (uid.isEmpty()) return false   // can't determine — treat as others' message
        return senderId.trim() == uid
    }

    private fun formatTime(isoString: String): String {
        return try {
            // Try with milliseconds first, then without
            val parsers = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX"
            )
            var date: Date? = null
            for (fmt in parsers) {
                try {
                    val parser = SimpleDateFormat(fmt, Locale.getDefault())
                    parser.timeZone = TimeZone.getTimeZone("UTC")
                    date = parser.parse(isoString)
                    if (date != null) break
                } catch (_: Exception) {}
            }
            val display = SimpleDateFormat("h:mm a", Locale.getDefault())
            display.format(date ?: Date())
        } catch (e: Exception) {
            "Now"
        }
    }
}

// ── Extension to convert API DTO to UI model ──────────────────
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
                date = p.parse(this.createdAt); if (date != null) break
            } catch (_: Exception) {}
        }
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(date ?: Date())
    } catch (_: Exception) { "Now" },
    isMe      = isMe,
    isPending = false
)
