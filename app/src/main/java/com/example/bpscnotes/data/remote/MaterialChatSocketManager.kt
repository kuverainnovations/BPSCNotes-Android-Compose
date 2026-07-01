package com.example.bpscnotes.data.remote

import android.util.Log
import com.example.bpscnotes.data.local.TokenStore
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

// ════════════════════════════════════════════════════════════
// MaterialChatSocketManager
//
// Real-time delivery for marketplace material chat (Phase 5).
// Mirrors TierRoomsSocketManager's connection-lifecycle pattern:
//  - guard on `socket != null` (not `connected()`) so socket.io's
//    built-in reconnection isn't fought with a second socket
//  - rejoin the current chat room on reconnect
//  - queue messages sent while disconnected, flush on reconnect
// ════════════════════════════════════════════════════════════

data class ChatNewMessageEvent(
    val id: String,
    val chatId: String,
    val senderId: String,
    val message: String,
    val createdAt: String,
)

@Singleton
class MaterialChatSocketManager @Inject constructor(private val tokenStore: TokenStore) {
    companion object {
        private const val TAG       = "MaterialChatSocket"
        private const val BASE_URL  = "https://api-stg.bpscnotes.in"
//        private const val BASE_URL  = "https://api.bpscnotes.in"
        private const val NAMESPACE = "/material-chat"
    }

    private var socket: Socket? = null

    private val _newMessages = MutableSharedFlow<ChatNewMessageEvent>(extraBufferCapacity = 64)
    val newMessages: SharedFlow<ChatNewMessageEvent> = _newMessages.asSharedFlow()

    private val _socketErrors = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val socketErrors: SharedFlow<String> = _socketErrors.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var currentChatId: String? = null
    private val pendingMessages = mutableListOf<String>()

    // ── Connect ────────────────────────────────────────────────
    fun connect() {
        // Same fix as TierRoomsSocketManager: guard on socket != null,
        // NOT socket.connected() — let socket.io handle reconnection.
        if (socket != null) {
            Log.d(TAG, "connect(): socket exists (connected=${socket?.connected()}), leaving socket.io to handle reconnect")
            return
        }

        val token = tokenStore.getToken() ?: run {
            Log.w(TAG, "connect(): skipped — no token")
            return
        }

        try {
            val opts = IO.Options.builder()
                .setAuth(mapOf("token" to token))
                .setTransports(arrayOf("websocket"))
                .setReconnection(true)
                .setReconnectionAttempts(Int.MAX_VALUE)
                .setReconnectionDelay(1500)
                .setReconnectionDelayMax(8_000)
                .build()
            socket = IO.socket("$BASE_URL$NAMESPACE", opts)
            registerListeners()
            socket!!.connect()
            Log.d(TAG, "🔌 Socket created → $BASE_URL$NAMESPACE")
        } catch (e: Exception) {
            Log.e(TAG, "Socket create failed: ${e.message}", e)
            socket = null
        }
    }

    private fun registerListeners() {
        val s = socket ?: return

        s.on(Socket.EVENT_CONNECT) {
            _isConnected.value = true
            Log.d(TAG, "✅ Connected")

            currentChatId?.let { id ->
                s.emit("chat:join", JSONObject().put("chatId", id))
                Log.d(TAG, "↩️  Rejoined chat: $id")
            }

            synchronized(pendingMessages) {
                if (pendingMessages.isNotEmpty() && currentChatId != null) {
                    Log.d(TAG, "📤 Flushing ${pendingMessages.size} queued messages")
                    pendingMessages.forEach { msg ->
                        s.emit("chat:message", JSONObject()
                            .put("chatId", currentChatId)
                            .put("message", msg))
                    }
                    pendingMessages.clear()
                }
            }
        }

        s.on(Socket.EVENT_DISCONNECT) { args ->
            _isConnected.value = false
            Log.d(TAG, "🔴 Disconnected: ${args.firstOrNull()}")
            // Do NOT null socket — socket.io will reconnect automatically
        }

        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e(TAG, "⚠️  Connect error: ${args.firstOrNull()}")
        }

        s.on("exception") { args ->
            val json    = args.firstOrNull() as? JSONObject
            val message = json?.optString("message") ?: args.firstOrNull()?.toString() ?: "Server error"
            Log.e(TAG, "🔥 Server exception: $message")
            _socketErrors.tryEmit(message)
        }

        s.on("chat:new_message") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _newMessages.tryEmit(ChatNewMessageEvent(
                id        = json.optString("id"),
                chatId    = json.optString("chatId"),
                senderId  = json.optString("senderId"),
                message   = json.optString("message"),
                createdAt = json.optString("createdAt"),
            ))
            Log.d(TAG, "💬 ${json.optString("senderId")}: ${json.optString("message").take(40)}")
        }
    }

    // ── Emitters ───────────────────────────────────────────────

    fun joinChat(chatId: String) {
        currentChatId = chatId
        if (socket?.connected() == true) {
            socket?.emit("chat:join", JSONObject().put("chatId", chatId))
            Log.d(TAG, "📍 Joined chat: $chatId")
        } else {
            connect() // no-op if socket exists (will rejoin via EVENT_CONNECT + currentChatId)
            Log.d(TAG, "📍 Will join $chatId on connect")
        }
    }

    fun leaveChat() {
        currentChatId?.let { id -> socket?.emit("chat:leave", JSONObject().put("chatId", id)) }
        currentChatId = null
    }

    fun sendMessage(message: String) {
        val chatId = currentChatId
        if (socket?.connected() == true && chatId != null) {
            socket?.emit("chat:message", JSONObject()
                .put("chatId", chatId).put("message", message))
            Log.d(TAG, "📤 Sent: ${message.take(40)}")
        } else {
            synchronized(pendingMessages) { pendingMessages.add(message) }
            connect() // no-op if socket exists and reconnecting
            Log.d(TAG, "📥 Queued: ${message.take(40)}")
        }
    }

    fun disconnect() {
        synchronized(pendingMessages) { pendingMessages.clear() }
        socket?.off()
        socket?.disconnect()
        socket = null
        currentChatId = null
        _isConnected.value = false
    }
}
