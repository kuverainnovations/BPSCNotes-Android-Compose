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
// TierRoomsSocketManager — WebSocket for rooms + real-time chat
// ════════════════════════════════════════════════════════════

data class PresenceUpdateEvent(val tierKey: String, val activeNow: Int)
data class PromotionEvent(val tierKey: String, val tierName: String, val tierEmoji: String, val message: String)
data class DemotionEvent(val tierKey: String, val tierName: String, val tierEmoji: String, val message: String)
data class LeaderboardTickEntry(val userId: String, val userName: String, val rankPosition: Int, val studyMinutes: Int, val coinsEarned: Int)
data class LeaderboardTickEvent(val tierKey: String, val top3: List<LeaderboardTickEntry>, val updatedAt: String)
data class HeartbeatAckEvent(val isAfk: Boolean, val activeMinsThisBeat: Int, val coinsEarnedThisBeat: Int, val xpEarnedThisBeat: Int, val totalCoinsThisSession: Int, val totalXpThisSession: Int, val totalActiveMinutes: Int)

// BUG FIX: member join/leave events for real-time member list updates
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

    // ── Flows ──────────────────────────────────────────────────
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

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _presenceSnapshot = MutableStateFlow<Map<String, Int>>(emptyMap())
    val presenceSnapshot: StateFlow<Map<String, Int>> = _presenceSnapshot.asStateFlow()

    // Chat messages
    private val _roomMessages = MutableSharedFlow<RoomMessageEvent>(extraBufferCapacity = 200)
    val roomMessages: SharedFlow<RoomMessageEvent> = _roomMessages.asSharedFlow()

    // BUG FIX: Real-time member join/leave flows
    // Without these, new members only appear after manual refresh.
    private val _memberJoins  = MutableSharedFlow<MemberJoinEvent>(extraBufferCapacity = 64)
    val memberJoins:  SharedFlow<MemberJoinEvent>  = _memberJoins.asSharedFlow()

    private val _memberLeaves = MutableSharedFlow<MemberLeaveEvent>(extraBufferCapacity = 64)
    val memberLeaves: SharedFlow<MemberLeaveEvent> = _memberLeaves.asSharedFlow()

    // Track current tier room so we can re-join after reconnect
    private var currentTierKey: String? = null

    // ── Connect ───────────────────────────────────────────────
    fun connect() {
        if (socket?.connected() == true) return
        val token = tokenStore.getToken() ?: return
        try {
            val opts = IO.Options.builder()
                .setAuth(mapOf("token" to token))
                .setTransports(arrayOf("websocket"))
                .setReconnection(true)
                .setReconnectionAttempts(Int.MAX_VALUE)   // BUG FIX: was 5 → keep trying forever
                .setReconnectionDelay(2000)
                .setReconnectionDelayMax(10_000)          // cap at 10s backoff
                .build()
            socket = IO.socket("$BASE_URL$NAMESPACE", opts)
            registerListeners()
            socket!!.connect()
            Log.d(TAG, "Connecting to $BASE_URL$NAMESPACE…")
        } catch (e: Exception) {
            Log.e(TAG, "Socket create failed: ${e.message}", e)
        }
    }

    private fun registerListeners() {
        val s = socket ?: return

        s.on(Socket.EVENT_CONNECT) {
            _isConnected.value = true
            Log.d(TAG, "Connected ✅")
            // Re-join tier room on every connect/reconnect
            currentTierKey?.let { key ->
                s.emit("tier:join_room", JSONObject().put("tierKey", key))
                Log.d(TAG, "Rejoined tier room: $key")
            }
        }

        s.on(Socket.EVENT_DISCONNECT) { args ->
            _isConnected.value = false
            Log.d(TAG, "Disconnected: ${args.firstOrNull()}")
        }

        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e(TAG, "WS connect error: ${args.firstOrNull()}")
        }

        // ── Presence ──────────────────────────────────────────
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
            json.keys().forEach { key ->
                val k = key?.toString() ?: return@forEach
                snapshot[k] = json.optInt(k)
            }
            _presenceSnapshot.value = snapshot
        }

        // BUG FIX: Listen for individual member join/leave events.
        // Backend emits "room:member_joined" when a user's session starts,
        // and "room:member_left" when it ends.
        // Without these, the members list only updates when you go back and return.
        s.on("room:member_joined") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _memberJoins.tryEmit(
                MemberJoinEvent(
                    tierKey  = json.optString("tierKey"),
                    userId   = json.optString("userId"),
                    userName = json.optString("userName")
                )
            )
            Log.d(TAG, "Member joined: ${json.optString("userName")}")
        }

        s.on("room:member_left") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _memberLeaves.tryEmit(
                MemberLeaveEvent(
                    tierKey = json.optString("tierKey"),
                    userId  = json.optString("userId")
                )
            )
            Log.d(TAG, "Member left: ${json.optString("userId")}")
        }

        // ── Tier events ───────────────────────────────────────
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

        // ── Leaderboard ───────────────────────────────────────
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

        // ── Heartbeat ─────────────────────────────────────────
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

        // ── Chat ──────────────────────────────────────────────
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
            Log.d(TAG, "Chat [${json.optString("tierKey")}] ${json.optString("senderName")}: ${json.optString("message").take(30)}")
        }
    }

    // ── Emit helpers ──────────────────────────────────────────

    fun joinTierRoom(tierKey: String) {
        currentTierKey = tierKey
        if (socket?.connected() != true) {
            connect()
            return
        }
        socket?.emit("tier:join_room", JSONObject().put("tierKey", tierKey))
        Log.d(TAG, "Emitting tier:join_room → $tierKey")
    }

    fun leaveTierRoom() {
        socket?.emit("tier:leave_room")
        // Keep currentTierKey so reconnect can rejoin
    }

    fun sendHeartbeat(sessionId: String) {
        if (socket?.connected() != true) {
            Log.w(TAG, "Heartbeat skipped — not connected")
            return
        }
        socket?.emit("session:heartbeat", JSONObject().put("sessionId", sessionId))
    }

    /**
     * Send a chat message.
     * BUG FIX: include tierKey so the server knows which room to broadcast to.
     * Without tierKey the server was broadcasting to all rooms or dropping the message.
     */
    fun sendChatMessage(message: String) {
        if (socket?.connected() != true) {
            Log.w(TAG, "sendChatMessage skipped — not connected")
            return
        }
        val payload = JSONObject()
            .put("message", message)
            .put("tierKey", currentTierKey ?: "")   // BUG FIX: was missing tierKey
        socket?.emit("room:send_message", payload)
        Log.d(TAG, "Sent chat message to room: $currentTierKey")
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
        currentTierKey = null
        _isConnected.value = false
    }

    fun reconnect() {
        disconnect()
        connect()
    }
}