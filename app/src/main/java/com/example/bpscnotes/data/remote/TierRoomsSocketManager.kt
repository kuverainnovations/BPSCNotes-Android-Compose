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
// TierRoomsSocketManager — all WebSocket events including chat
// ════════════════════════════════════════════════════════════

// ── Existing event data classes ───────────────────────────────
data class PresenceUpdateEvent(val tierKey: String, val activeNow: Int)
data class PromotionEvent(val tierKey: String, val tierName: String, val tierEmoji: String, val message: String)
data class DemotionEvent(val tierKey: String, val tierName: String, val tierEmoji: String, val message: String)
data class LeaderboardTickEntry(val userId: String, val userName: String, val rankPosition: Int, val studyMinutes: Int, val coinsEarned: Int)
data class LeaderboardTickEvent(val tierKey: String, val top3: List<LeaderboardTickEntry>, val updatedAt: String)
data class HeartbeatAckEvent(val isAfk: Boolean, val activeMinsThisBeat: Int, val coinsEarnedThisBeat: Int, val xpEarnedThisBeat: Int, val totalCoinsThisSession: Int, val totalXpThisSession: Int, val totalActiveMinutes: Int)

// ── NEW: Chat message event ────────────────────────────────────
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
        private const val TAG      = "TierRoomsSocket"
        private const val BASE_URL = "https://api.bpscnotes.in"
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

    // NEW: Chat message flow — all incoming room messages
    private val _roomMessages = MutableSharedFlow<RoomMessageEvent>(extraBufferCapacity = 200)
    val roomMessages: SharedFlow<RoomMessageEvent> = _roomMessages.asSharedFlow()

    // ── Connect ───────────────────────────────────────────────
    fun connect() {
        if (socket?.connected() == true) return
        val token = tokenStore.getToken() ?: return
        try {
            val opts = IO.Options.builder()
                .setAuth(mapOf("token" to token))
                .setTransports(arrayOf("websocket"))
                .setReconnection(true)
                .setReconnectionAttempts(5)
                .setReconnectionDelay(2000)
                .build()
            socket = IO.socket("$BASE_URL$NAMESPACE", opts)
            registerListeners()
            socket!!.connect()
            Log.d(TAG, "Connecting…")
        } catch (e: Exception) {
            Log.e(TAG, "Socket create failed: ${e.message}", e)
        }
    }

    private fun registerListeners() {
        val s = socket ?: return

        s.on(Socket.EVENT_CONNECT)       { _isConnected.value = true;  Log.d(TAG, "Connected ✅") }
        s.on(Socket.EVENT_DISCONNECT)    { _isConnected.value = false; Log.d(TAG, "Disconnected") }
        s.on(Socket.EVENT_CONNECT_ERROR) { args -> Log.e(TAG, "WS error: ${args.firstOrNull()}") }

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

        s.on("tier:promotion") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _promotionEvents.tryEmit(PromotionEvent(
                json.optString("tierKey"), json.optString("tierName"),
                json.optString("tierEmoji"), json.optString("message")
            ))
        }

        s.on("tier:demotion") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _demotionEvents.tryEmit(DemotionEvent(
                json.optString("tierKey"), json.optString("tierName"),
                json.optString("tierEmoji"), json.optString("message")
            ))
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
                json.optBoolean("isAfk"),
                json.optInt("activeMinsThisBeat"), json.optInt("coinsEarnedThisBeat"),
                json.optInt("xpEarnedThisBeat"),  json.optInt("totalCoinsThisSession"),
                json.optInt("totalXpThisSession"), json.optInt("totalActiveMinutes")
            ))
        }

        s.on("session:afk_warning") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _afkWarnings.tryEmit(json.optString("sessionId"))
        }

        // ── NEW: Incoming room chat message ────────────────────
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
            Log.d(TAG, "Chat [${json.optString("tierKey")}] ${json.optString("senderName")}: ${json.optString("message").take(30)}")
        }
    }

    // ── Emit helpers ──────────────────────────────────────────

    fun joinTierRoom(tierKey: String) {
        if (socket?.connected() != true) { connect(); return }
        socket?.emit("tier:join_room", JSONObject().put("tierKey", tierKey))
    }

    fun leaveTierRoom() {
        socket?.emit("tier:leave_room")
    }

    fun sendHeartbeat(sessionId: String) {
        if (socket?.connected() != true) return
        socket?.emit("session:heartbeat", JSONObject().put("sessionId", sessionId))
    }

    // NEW: Send a chat message to the current tier room
    fun sendChatMessage(message: String) {
        if (socket?.connected() != true) {
            Log.w(TAG, "Cannot send message — socket not connected")
            return
        }
        socket?.emit("room:send_message", JSONObject().put("message", message))
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
        _isConnected.value = false
    }

    fun reconnect() { disconnect(); connect() }
}
