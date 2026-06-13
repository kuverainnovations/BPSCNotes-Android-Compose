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
// TierRoomsSocketManager
//
// BUG ROOT CAUSE — "only Amar can send, others stuck Reconnecting":
//
// Old connect() guard:
//   if (socket?.connected() == true) return   ← exits only if CONNECTED
//   socket = IO.socket(...)                    ← creates NEW socket if anything else
//
// socket.io has built-in reconnect (setReconnection=true). When a user's
// network drops, socket.io reconnects the EXISTING socket automatically.
// During this auto-reconnect: connected() == false, socket != null.
//
// Any call to connect() while socket is auto-reconnecting created a SECOND
// socket. Now two sockets race for the same userId on the server:
//   new socket connects → server kills old socket
//   old socket retries   → server kills new socket
//   ∞ loop → perpetual "Reconnecting..."
//
// Users on stable WiFi (Amar) never had a drop → connect() only called once
// → no race → works fine.
// Others on mobile data had brief drops → race condition → stuck forever.
//
// THE FIX (one line change):
//   if (socket != null) return   ← if socket exists, let socket.io reconnect
// Only create a new socket if socket is null (app start or explicit disconnect).
// ════════════════════════════════════════════════════════════

data class PresenceUpdateEvent(val tierKey: String, val activeNow: Int)
data class PromotionEvent(val tierKey: String, val tierName: String, val tierEmoji: String, val message: String)
data class DemotionEvent(val tierKey: String, val tierName: String, val tierEmoji: String, val message: String)
data class LeaderboardTickEntry(val userId: String, val userName: String, val rankPosition: Int, val studyMinutes: Int, val coinsEarned: Int)
data class LeaderboardTickEvent(val tierKey: String, val top3: List<LeaderboardTickEntry>, val updatedAt: String)
data class HeartbeatAckEvent(val isAfk: Boolean, val activeMinsThisBeat: Int, val coinsEarnedThisBeat: Int, val xpEarnedThisBeat: Int, val totalCoinsThisSession: Int, val totalXpThisSession: Int, val totalActiveMinutes: Int)
data class MemberJoinEvent(val tierKey: String, val userId: String, val userName: String)
data class MemberLeaveEvent(val tierKey: String, val userId: String)
data class RoomMessageEvent(
    val id: String, val senderId: String, val senderName: String,
    val message: String, val tierKey: String, val createdAt: String
)

@Singleton
class TierRoomsSocketManager @Inject constructor(private val tokenStore: TokenStore) {
    companion object {
        private const val TAG       = "TierRoomsSocket"
        //        private const val BASE_URL  = "https://api.bpscnotes.in"
        private const val BASE_URL  = "https://api-stg.bpscnotes.in"
        private const val NAMESPACE = "/tier-rooms"
    }

    private var socket: Socket? = null

    private val _presenceUpdates  = MutableSharedFlow<PresenceUpdateEvent>(extraBufferCapacity = 64)
    val presenceUpdates: SharedFlow<PresenceUpdateEvent> = _presenceUpdates.asSharedFlow()
    private val _promotionEvents  = MutableSharedFlow<PromotionEvent>(extraBufferCapacity = 8)
    val promotionEvents: SharedFlow<PromotionEvent> = _promotionEvents.asSharedFlow()
    private val _demotionEvents   = MutableSharedFlow<DemotionEvent>(extraBufferCapacity = 8)
    val demotionEvents: SharedFlow<DemotionEvent> = _demotionEvents.asSharedFlow()
    private val _leaderboardTicks = MutableSharedFlow<LeaderboardTickEvent>(extraBufferCapacity = 16)
    val leaderboardTicks: SharedFlow<LeaderboardTickEvent> = _leaderboardTicks.asSharedFlow()
    private val _heartbeatAcks    = MutableSharedFlow<HeartbeatAckEvent>(extraBufferCapacity = 32)
    val heartbeatAcks: SharedFlow<HeartbeatAckEvent> = _heartbeatAcks.asSharedFlow()
    private val _afkWarnings      = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val afkWarnings: SharedFlow<String> = _afkWarnings.asSharedFlow()
    private val _roomMessages     = MutableSharedFlow<RoomMessageEvent>(extraBufferCapacity = 200)
    val roomMessages: SharedFlow<RoomMessageEvent> = _roomMessages.asSharedFlow()
    private val _memberJoins      = MutableSharedFlow<MemberJoinEvent>(extraBufferCapacity = 64)
    val memberJoins: SharedFlow<MemberJoinEvent>  = _memberJoins.asSharedFlow()
    private val _memberLeaves     = MutableSharedFlow<MemberLeaveEvent>(extraBufferCapacity = 64)
    val memberLeaves: SharedFlow<MemberLeaveEvent> = _memberLeaves.asSharedFlow()
    private val _socketErrors     = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val socketErrors: SharedFlow<String> = _socketErrors.asSharedFlow()

    private val _isConnected        = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Used by ChatSheet to show "Connecting..." vs "Reconnecting..."
    private val _hasConnectedBefore = MutableStateFlow(false)
    val hasConnectedBefore: StateFlow<Boolean> = _hasConnectedBefore.asStateFlow()

    private val _presenceSnapshot = MutableStateFlow<Map<String, Int>>(emptyMap())
    val presenceSnapshot: StateFlow<Map<String, Int>> = _presenceSnapshot.asStateFlow()

    private var currentTierKey: String? = null
    // Read-only access so other ViewModels can avoid clobbering an
    // active room join (e.g. a session started in a non-home tier room).
    fun getCurrentTierKey(): String? = currentTierKey
    private val pendingMessages = mutableListOf<String>()

    // ── Connect ────────────────────────────────────────────────
    fun connect() {
        // ─────────────────────────────────────────────────────
        // THE FIX: guard on socket != null, NOT socket.connected()
        //
        // socket != null means the socket object exists and socket.io
        // is managing its lifecycle (reconnecting automatically).
        // We must NOT interfere by creating a second socket.
        //
        // socket == null means we need to create the socket for the first time,
        // or we explicitly called disconnect() which sets socket = null.
        // ─────────────────────────────────────────────────────
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
            _isConnected.value      = true
            _hasConnectedBefore.value = true
            Log.d(TAG, "✅ Connected")

            currentTierKey?.let { key ->
                s.emit("tier:join_room", JSONObject().put("tierKey", key))
                Log.d(TAG, "↩️  Rejoined: $key")
            }

            synchronized(pendingMessages) {
                if (pendingMessages.isNotEmpty()) {
                    Log.d(TAG, "📤 Flushing ${pendingMessages.size} queued messages")
                    pendingMessages.forEach { msg ->
                        s.emit("room:send_message", JSONObject()
                            .put("message", msg)
                            .put("tierKey", currentTierKey ?: ""))
                    }
                    pendingMessages.clear()
                }
            }
        }

        s.on(Socket.EVENT_DISCONNECT) { args ->
            _isConnected.value = false
            Log.d(TAG, "🔴 Disconnected: ${args.firstOrNull()}")
            // Do NOT null socket here — socket.io will reconnect automatically
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

        s.on("tier:presence_update") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            val event = PresenceUpdateEvent(json.optString("tierKey"), json.optInt("activeNow"))
            _presenceUpdates.tryEmit(event)
            _presenceSnapshot.value = _presenceSnapshot.value.toMutableMap().also {
                it[event.tierKey] = event.activeNow
            }
        }

        s.on("presence:snapshot") { args ->
            val json     = args.firstOrNull() as? JSONObject ?: return@on
            val snapshot = mutableMapOf<String, Int>()
            json.keys().forEach { k -> snapshot[k.toString()] = json.optInt(k.toString()) }
            _presenceSnapshot.value = snapshot
        }

        s.on("room:member_joined") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _memberJoins.tryEmit(MemberJoinEvent(
                json.optString("tierKey"), json.optString("userId"), json.optString("userName")))
            Log.d(TAG, "👤+ ${json.optString("userName")}")
        }

        s.on("room:member_left") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _memberLeaves.tryEmit(MemberLeaveEvent(json.optString("tierKey"), json.optString("userId")))
            Log.d(TAG, "👤- ${json.optString("userId")}")
        }

        s.on("tier:promotion") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _promotionEvents.tryEmit(PromotionEvent(
                json.optString("tierKey"), json.optString("tierName"),
                json.optString("tierEmoji"), json.optString("message")))
        }

        s.on("tier:demotion") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _demotionEvents.tryEmit(DemotionEvent(
                json.optString("tierKey"), json.optString("tierName"),
                json.optString("tierEmoji"), json.optString("message")))
        }

        s.on("room:leaderboard_tick") { args ->
            val json    = args.firstOrNull() as? JSONObject ?: return@on
            val top3Arr = json.optJSONArray("top3") ?: return@on
            val top3    = (0 until top3Arr.length()).map { i ->
                val e = top3Arr.getJSONObject(i)
                LeaderboardTickEntry(e.optString("user_id"), e.optString("user_name"),
                    e.optInt("rank_position"), e.optInt("study_minutes"), e.optInt("coins_earned"))
            }
            _leaderboardTicks.tryEmit(LeaderboardTickEvent(
                json.optString("tierKey"), top3, json.optString("updatedAt")))
        }

        s.on("session:heartbeat_ack") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _heartbeatAcks.tryEmit(HeartbeatAckEvent(
                json.optBoolean("isAfk"), json.optInt("activeMinsThisBeat"),
                json.optInt("coinsEarnedThisBeat"), json.optInt("xpEarnedThisBeat"),
                json.optInt("totalCoinsThisSession"), json.optInt("totalXpThisSession"),
                json.optInt("totalActiveMinutes")))
        }

        s.on("session:afk_warning") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _afkWarnings.tryEmit(json.optString("sessionId"))
        }

        s.on("room:new_message") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _roomMessages.tryEmit(RoomMessageEvent(
                id         = json.optString("id"),
                senderId   = json.optString("senderId"),
                senderName = json.optString("senderName"),
                message    = json.optString("message"),
                tierKey    = json.optString("tierKey"),
                createdAt  = json.optString("createdAt")
            ))
            Log.d(TAG, "💬 ${json.optString("senderName")}: ${json.optString("message").take(40)}")
        }
    }

    // ── Emitters ───────────────────────────────────────────────

    fun joinTierRoom(tierKey: String) {
        currentTierKey = tierKey
        if (socket?.connected() == true) {
            socket?.emit("tier:join_room", JSONObject().put("tierKey", tierKey))
            Log.d(TAG, "📍 Joined: $tierKey")
        } else {
            connect() // no-op if socket exists (it will rejoin via EVENT_CONNECT + currentTierKey)
            Log.d(TAG, "📍 Will join $tierKey on connect")
        }
    }

    fun leaveTierRoom() {
        socket?.emit("tier:leave_room")
    }

    fun sendHeartbeat(sessionId: String) {
        if (socket?.connected() != true) { Log.w(TAG, "Heartbeat skipped"); return }
        socket?.emit("session:heartbeat", JSONObject().put("sessionId", sessionId))
    }

    fun sendChatMessage(message: String) {
        if (socket?.connected() == true) {
            socket?.emit("room:send_message", JSONObject()
                .put("message", message).put("tierKey", currentTierKey ?: ""))
            Log.d(TAG, "📤 Sent: ${message.take(40)}")
        } else {
            synchronized(pendingMessages) { pendingMessages.add(message) }
            connect() // no-op if socket exists and reconnecting
            Log.d(TAG, "📥 Queued: ${message.take(40)}")
        }
    }

    fun disconnect() {
        synchronized(pendingMessages) { pendingMessages.clear() }
        socket?.off()       // remove all listeners
        socket?.disconnect()
        socket = null       // null allows connect() to create fresh socket
        currentTierKey = null
        _isConnected.value = false
    }
}