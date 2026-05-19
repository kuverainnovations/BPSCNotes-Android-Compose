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
// TierRoomsSocketManager — Singleton WebSocket manager
//
// BUGS FIXED IN THIS VERSION:
//
// 1. "Failed to send" when socket is still connecting.
//    Root cause: sendChatMessage() returned immediately if socket.connected() != true.
//    When the user opens the chat sheet, the socket may still be in "connecting" state.
//    Fix: Messages are now queued in pendingMessages and flushed on EVENT_CONNECT.
//
// 2. "Reconnecting..." banner showing on initial open (before first connect).
//    Root cause: isConnected starts as false. Any collector sees false immediately.
//    The banner showed "Reconnecting" even on first connection.
//    Fix: Added hasConnectedBefore flag. UI should only show "Reconnecting" if this is true.
//
// 3. Server-side WsException errors were silently swallowed.
//    Root cause: Android socket.io ignores the 'exception' event from NestJS.
//    Fix: Added listener for 'exception' event → emits to socketErrors flow so
//    UI can show the actual error (e.g. "Too fast", "Not in a room").
// ════════════════════════════════════════════════════════════

data class PresenceUpdateEvent(val tierKey: String, val activeNow: Int)
data class PromotionEvent(val tierKey: String, val tierName: String, val tierEmoji: String, val message: String)
data class DemotionEvent(val tierKey: String, val tierName: String, val tierEmoji: String, val message: String)
data class LeaderboardTickEntry(val userId: String, val userName: String, val rankPosition: Int, val studyMinutes: Int, val coinsEarned: Int)
data class LeaderboardTickEvent(val tierKey: String, val top3: List<LeaderboardTickEntry>, val updatedAt: String)
data class HeartbeatAckEvent(
    val isAfk: Boolean,
    val activeMinsThisBeat: Int,
    val coinsEarnedThisBeat: Int,
    val xpEarnedThisBeat: Int,
    val totalCoinsThisSession: Int,
    val totalXpThisSession: Int,
    val totalActiveMinutes: Int
)
data class MemberJoinEvent(val tierKey: String, val userId: String, val userName: String)
data class MemberLeaveEvent(val tierKey: String, val userId: String)

data class RoomMessageEvent(
    val id:         String,
    val senderId:   String,
    val senderName: String,
    val message:    String,
    val tierKey:    String,
    val createdAt:  String
)

@Singleton
class TierRoomsSocketManager @Inject constructor(
    private val tokenStore: TokenStore
) {
    companion object {
        private const val TAG       = "TierRoomsSocket"
        private const val BASE_URL  = "https://api.bpscnotes.in"
        private const val NAMESPACE = "/tier-rooms"
    }

    private var socket: Socket? = null

    // ── Public flows ───────────────────────────────────────────
    private val _presenceUpdates = MutableSharedFlow<PresenceUpdateEvent>(extraBufferCapacity = 64)
    val presenceUpdates: SharedFlow<PresenceUpdateEvent> = _presenceUpdates.asSharedFlow()

    private val _promotionEvents = MutableSharedFlow<PromotionEvent>(extraBufferCapacity = 8)
    val promotionEvents: SharedFlow<PromotionEvent> = _promotionEvents.asSharedFlow()

    private val _demotionEvents = MutableSharedFlow<DemotionEvent>(extraBufferCapacity = 8)
    val demotionEvents: SharedFlow<DemotionEvent> = _demotionEvents.asSharedFlow()

    private val _leaderboardTicks = MutableSharedFlow<LeaderboardTickEvent>(extraBufferCapacity = 16)
    val leaderboardTicks: SharedFlow<LeaderboardTickEvent> = _leaderboardTicks.asSharedFlow()

    private val _heartbeatAcks = MutableSharedFlow<HeartbeatAckEvent>(extraBufferCapacity = 32)
    val heartbeatAcks: SharedFlow<HeartbeatAckEvent> = _heartbeatAcks.asSharedFlow()

    private val _afkWarnings = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val afkWarnings: SharedFlow<String> = _afkWarnings.asSharedFlow()

    private val _roomMessages = MutableSharedFlow<RoomMessageEvent>(extraBufferCapacity = 200)
    val roomMessages: SharedFlow<RoomMessageEvent> = _roomMessages.asSharedFlow()

    private val _memberJoins = MutableSharedFlow<MemberJoinEvent>(extraBufferCapacity = 64)
    val memberJoins: SharedFlow<MemberJoinEvent> = _memberJoins.asSharedFlow()

    private val _memberLeaves = MutableSharedFlow<MemberLeaveEvent>(extraBufferCapacity = 64)
    val memberLeaves: SharedFlow<MemberLeaveEvent> = _memberLeaves.asSharedFlow()

    // FIX 2: Socket errors from server (WsException) forwarded to UI
    private val _socketErrors = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val socketErrors: SharedFlow<String> = _socketErrors.asSharedFlow()

    // isConnected: true = socket is connected and authenticated on server
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // FIX 2: Track whether we have EVER connected successfully.
    // UI uses this to distinguish "initial connecting" from "reconnecting after drop".
    private val _hasConnectedBefore = MutableStateFlow(false)
    val hasConnectedBefore: StateFlow<Boolean> = _hasConnectedBefore.asStateFlow()

    private val _presenceSnapshot = MutableStateFlow<Map<String, Int>>(emptyMap())
    val presenceSnapshot: StateFlow<Map<String, Int>> = _presenceSnapshot.asStateFlow()

    // Track current tier room so we can re-join after reconnect
    private var currentTierKey: String? = null

    // FIX 1: Message queue — messages sent while socket is connecting
    // are queued here and flushed on EVENT_CONNECT
    private val pendingMessages = mutableListOf<String>()

    // ── Connect ────────────────────────────────────────────────
    fun connect() {
        if (socket?.connected() == true) return
        val token = tokenStore.getToken() ?: run {
            Log.w(TAG, "connect() skipped — no auth token")
            return
        }
        try {
            val opts = IO.Options.builder()
                .setAuth(mapOf("token" to token))
                .setTransports(arrayOf("websocket"))
                .setReconnection(true)
                .setReconnectionAttempts(Int.MAX_VALUE)
                .setReconnectionDelay(2000)
                .setReconnectionDelayMax(10_000)
                .build()
            socket = IO.socket("$BASE_URL$NAMESPACE", opts)
            registerListeners()
            socket!!.connect()
            Log.d(TAG, "Connecting to $BASE_URL$NAMESPACE …")
        } catch (e: Exception) {
            Log.e(TAG, "Socket create failed: ${e.message}", e)
        }
    }

    private fun registerListeners() {
        val s = socket ?: return

        s.on(Socket.EVENT_CONNECT) {
            _isConnected.value = true
            _hasConnectedBefore.value = true
            Log.d(TAG, "✅ Connected")

            // Re-join tier room on every connect/reconnect
            currentTierKey?.let { key ->
                s.emit("tier:join_room", JSONObject().put("tierKey", key))
                Log.d(TAG, "Rejoined tier room: $key")
            }

            // FIX 1: Flush queued messages that were sent while connecting
            synchronized(pendingMessages) {
                if (pendingMessages.isNotEmpty()) {
                    Log.d(TAG, "Flushing ${pendingMessages.size} queued message(s)")
                    pendingMessages.forEach { msg ->
                        val payload = JSONObject()
                            .put("message", msg)
                            .put("tierKey", currentTierKey ?: "")
                        s.emit("room:send_message", payload)
                    }
                    pendingMessages.clear()
                }
            }
        }

        s.on(Socket.EVENT_DISCONNECT) { args ->
            _isConnected.value = false
            Log.d(TAG, "Disconnected: ${args.firstOrNull()}")
        }

        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e(TAG, "WS connect error: ${args.firstOrNull()}")
        }

        // FIX 3: Listen for server-side WsException errors
        // NestJS @WebSocketGateway emits 'exception' when a @SubscribeMessage handler throws
        s.on("exception") { args ->
            val json    = args.firstOrNull() as? JSONObject
            val message = json?.optString("message") ?: args.firstOrNull()?.toString() ?: "Unknown error"
            Log.e(TAG, "Server exception: $message")
            _socketErrors.tryEmit(message)
        }

        // ── Presence ───────────────────────────────────────────
        s.on("tier:presence_update") { args ->
            val json  = args.firstOrNull() as? JSONObject ?: return@on
            val event = PresenceUpdateEvent(json.optString("tierKey"), json.optInt("activeNow"))
            _presenceUpdates.tryEmit(event)
            _presenceSnapshot.value = _presenceSnapshot.value.toMutableMap().also {
                it[event.tierKey] = event.activeNow
            }
        }

        s.on("presence:snapshot") { args ->
            val json     = args.firstOrNull() as? JSONObject ?: return@on
            val snapshot = mutableMapOf<String, Int>()
            json.keys().forEach { key ->
                val k = key?.toString() ?: return@forEach
                snapshot[k] = json.optInt(k)
            }
            _presenceSnapshot.value = snapshot
        }

        // ── Member join/leave ──────────────────────────────────
        s.on("room:member_joined") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _memberJoins.tryEmit(
                MemberJoinEvent(
                    tierKey  = json.optString("tierKey"),
                    userId   = json.optString("userId"),
                    userName = json.optString("userName")
                )
            )
        }

        s.on("room:member_left") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _memberLeaves.tryEmit(
                MemberLeaveEvent(
                    tierKey = json.optString("tierKey"),
                    userId  = json.optString("userId")
                )
            )
        }

        // ── Tier events ────────────────────────────────────────
        s.on("tier:promotion") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _promotionEvents.tryEmit(
                PromotionEvent(
                    json.optString("tierKey"), json.optString("tierName"),
                    json.optString("tierEmoji"), json.optString("message")
                )
            )
        }

        s.on("tier:demotion") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _demotionEvents.tryEmit(
                DemotionEvent(
                    json.optString("tierKey"), json.optString("tierName"),
                    json.optString("tierEmoji"), json.optString("message")
                )
            )
        }

        // ── Leaderboard ────────────────────────────────────────
        s.on("room:leaderboard_tick") { args ->
            val json    = args.firstOrNull() as? JSONObject ?: return@on
            val top3Arr = json.optJSONArray("top3") ?: return@on
            val top3    = (0 until top3Arr.length()).map { i ->
                val e = top3Arr.getJSONObject(i)
                LeaderboardTickEntry(
                    e.optString("user_id"), e.optString("user_name"),
                    e.optInt("rank_position"), e.optInt("study_minutes"), e.optInt("coins_earned")
                )
            }
            _leaderboardTicks.tryEmit(LeaderboardTickEvent(json.optString("tierKey"), top3, json.optString("updatedAt")))
        }

        // ── Heartbeat ──────────────────────────────────────────
        s.on("session:heartbeat_ack") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _heartbeatAcks.tryEmit(
                HeartbeatAckEvent(
                    json.optBoolean("isAfk"),
                    json.optInt("activeMinsThisBeat"),
                    json.optInt("coinsEarnedThisBeat"),
                    json.optInt("xpEarnedThisBeat"),
                    json.optInt("totalCoinsThisSession"),
                    json.optInt("totalXpThisSession"),
                    json.optInt("totalActiveMinutes")
                )
            )
        }

        s.on("session:afk_warning") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _afkWarnings.tryEmit(json.optString("sessionId"))
        }

        // ── Chat messages ──────────────────────────────────────
        s.on("room:new_message") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _roomMessages.tryEmit(
                RoomMessageEvent(
                    id         = json.optString("id"),
                    senderId   = json.optString("senderId"),
                    senderName = json.optString("senderName"),
                    message    = json.optString("message"),
                    tierKey    = json.optString("tierKey"),
                    createdAt  = json.optString("createdAt")
                )
            )
            Log.d(TAG, "📨 [${json.optString("tierKey")}] ${json.optString("senderName")}: ${json.optString("message").take(40)}")
        }
    }

    // ── Public emitters ────────────────────────────────────────

    fun joinTierRoom(tierKey: String) {
        currentTierKey = tierKey
        if (socket?.connected() != true) {
            connect()
            // connect() will emit tier:join_room in EVENT_CONNECT handler
            return
        }
        socket?.emit("tier:join_room", JSONObject().put("tierKey", tierKey))
        Log.d(TAG, "Emitting tier:join_room → $tierKey")
    }

    fun leaveTierRoom() {
        socket?.emit("tier:leave_room")
        // Keep currentTierKey — reconnect will rejoin
    }

    fun sendHeartbeat(sessionId: String) {
        if (socket?.connected() != true) {
            Log.w(TAG, "Heartbeat skipped — not connected")
            return
        }
        socket?.emit("session:heartbeat", JSONObject().put("sessionId", sessionId))
    }

    /**
     * FIX 1: Send a chat message.
     *
     * OLD behaviour: returned immediately if socket.connected() != true → "Failed to send"
     *
     * NEW behaviour:
     * - If connected → emit immediately (fast path, same as before)
     * - If connecting → add to pendingMessages queue, which is flushed in EVENT_CONNECT
     * - If socket is null → call connect() first, then queue
     *
     * Result: messages sent while the socket is still handshaking are no longer lost.
     */
    fun sendChatMessage(message: String) {
        val payload = JSONObject()
            .put("message", message)
            .put("tierKey", currentTierKey ?: "")

        if (socket?.connected() == true) {
            socket?.emit("room:send_message", payload)
            Log.d(TAG, "📤 Sent immediately to room: $currentTierKey")
        } else {
            // Queue the message — it will be sent once EVENT_CONNECT fires
            synchronized(pendingMessages) {
                pendingMessages.add(message)
            }
            Log.d(TAG, "📥 Queued message (socket connecting) for room: $currentTierKey")

            // Make sure the socket is actually trying to connect
            if (socket == null || socket?.connected() == false) {
                connect()
            }
        }
    }

    fun disconnect() {
        synchronized(pendingMessages) { pendingMessages.clear() }
        socket?.disconnect()
        socket = null
        currentTierKey = null
        _isConnected.value = false
        // NOTE: do NOT reset _hasConnectedBefore — it's lifetime state
    }

    fun reconnect() {
        disconnect()
        connect()
    }
}