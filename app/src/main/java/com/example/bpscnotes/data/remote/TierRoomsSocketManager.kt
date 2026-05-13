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
// FILE: data/remote/TierRoomsSocketManager.kt
//
// Manages the Socket.IO connection to /tier-rooms namespace.
// Uses socket.io-client-java (already a common Android dep).
//
// Add to build.gradle.kts:
//   implementation("io.socket:socket.io-client:2.1.0")
//
// Features:
//   - Auth via JWT token in handshake
//   - Reconnects automatically (socket.io default)
//   - Emits events: tier:join_room, tier:leave_room, session:heartbeat
//   - Receives events: tier:presence_update, tier:promotion,
//                      tier:demotion, room:leaderboard_tick,
//                      session:afk_warning, presence:snapshot
// ════════════════════════════════════════════════════════════

// ── Event data classes ────────────────────────────────────────

data class PresenceUpdateEvent(
    val tierKey:   String,
    val activeNow: Int
)

data class PromotionEvent(
    val tierKey:   String,
    val tierName:  String,
    val tierEmoji: String,
    val message:   String
)

data class DemotionEvent(
    val tierKey:   String,
    val tierName:  String,
    val tierEmoji: String,
    val message:   String
)

data class LeaderboardTickEntry(
    val userId:       String,
    val userName:     String,
    val rankPosition: Int,
    val studyMinutes: Int,
    val coinsEarned:  Int
)

data class LeaderboardTickEvent(
    val tierKey:   String,
    val top3:      List<LeaderboardTickEntry>,
    val updatedAt: String
)

data class HeartbeatAckEvent(
    val isAfk:               Boolean,
    val activeMinsThisBeat:  Int,
    val coinsEarnedThisBeat: Int,
    val xpEarnedThisBeat:    Int,
    val totalCoinsThisSession: Int,
    val totalXpThisSession:    Int,
    val totalActiveMinutes:    Int
)

// ════════════════════════════════════════════════════════════
// SOCKET MANAGER
// ════════════════════════════════════════════════════════════

@Singleton
class TierRoomsSocketManager @Inject constructor(
    private val tokenStore: TokenStore
) {
    companion object {
        private const val TAG = "TierRoomsSocket"
        private const val BASE_URL = "https://api.bpscnotes.in"
        private const val NAMESPACE = "/tier-rooms"
    }

    private var socket: Socket? = null

    // ── Exposed event flows ───────────────────────────────────
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

    // snapshot: tierKey -> activeNow
    private val _presenceSnapshot = MutableStateFlow<Map<String, Int>>(emptyMap())
    val presenceSnapshot: StateFlow<Map<String, Int>> = _presenceSnapshot.asStateFlow()

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
            Log.d(TAG, "Connecting to $BASE_URL$NAMESPACE")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create socket: ${e.message}", e)
        }
    }

    private fun registerListeners() {
        val s = socket ?: return

        s.on(Socket.EVENT_CONNECT) {
            _isConnected.value = true
            Log.d(TAG, "Connected ✅")
        }

        s.on(Socket.EVENT_DISCONNECT) {
            _isConnected.value = false
            Log.d(TAG, "Disconnected")
        }

        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val reason = args.firstOrNull()?.toString() ?: "unknown"
            Log.e(TAG, "Connection error: $reason")
        }

        // Live member count
        s.on("tier:presence_update") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            val event = PresenceUpdateEvent(
                tierKey   = json.optString("tierKey"),
                activeNow = json.optInt("activeNow")
            )
            _presenceUpdates.tryEmit(event)
            // Update snapshot
            _presenceSnapshot.value = _presenceSnapshot.value.toMutableMap().also {
                it[event.tierKey] = event.activeNow
            }
        }

        // Initial snapshot on connect
        s.on("presence:snapshot") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            val snapshot = mutableMapOf<String, Int>()
            json.keys().forEach { key ->
                val safeKey = key?.toString() ?: return@forEach
                snapshot[safeKey] = json.optInt(safeKey)
            }
            _presenceSnapshot.value = snapshot
        }

        // Tier promotion
        s.on("tier:promotion") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _promotionEvents.tryEmit(PromotionEvent(
                tierKey   = json.optString("tierKey"),
                tierName  = json.optString("tierName"),
                tierEmoji = json.optString("tierEmoji"),
                message   = json.optString("message")
            ))
        }

        // Tier demotion
        s.on("tier:demotion") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _demotionEvents.tryEmit(DemotionEvent(
                tierKey   = json.optString("tierKey"),
                tierName  = json.optString("tierName"),
                tierEmoji = json.optString("tierEmoji"),
                message   = json.optString("message")
            ))
        }

        // Leaderboard tick (every 30 min)
        s.on("room:leaderboard_tick") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            val tierKey = json.optString("tierKey")
            val top3Arr = json.optJSONArray("top3") ?: return@on
            val top3    = (0 until top3Arr.length()).map { i ->
                val e = top3Arr.getJSONObject(i)
                LeaderboardTickEntry(
                    userId       = e.optString("user_id"),
                    userName     = e.optString("user_name"),
                    rankPosition = e.optInt("rank_position"),
                    studyMinutes = e.optInt("study_minutes"),
                    coinsEarned  = e.optInt("coins_earned")
                )
            }
            _leaderboardTicks.tryEmit(LeaderboardTickEvent(
                tierKey   = tierKey,
                top3      = top3,
                updatedAt = json.optString("updatedAt")
            ))
        }

        // Heartbeat ack (when using WS heartbeat instead of REST)
        s.on("session:heartbeat_ack") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _heartbeatAcks.tryEmit(HeartbeatAckEvent(
                isAfk                = json.optBoolean("isAfk"),
                activeMinsThisBeat   = json.optInt("activeMinsThisBeat"),
                coinsEarnedThisBeat  = json.optInt("coinsEarnedThisBeat"),
                xpEarnedThisBeat     = json.optInt("xpEarnedThisBeat"),
                totalCoinsThisSession = json.optInt("totalCoinsThisSession"),
                totalXpThisSession    = json.optInt("totalXpThisSession"),
                totalActiveMinutes    = json.optInt("totalActiveMinutes")
            ))
        }

        // AFK warning from server
        s.on("session:afk_warning") { args ->
            val json = args.firstOrNull() as? JSONObject ?: return@on
            _afkWarnings.tryEmit(json.optString("sessionId"))
        }
    }

    // ── Emit helpers ──────────────────────────────────────────

    fun joinTierRoom(tierKey: String) {
        if (socket?.connected() != true) { connect(); return }
        val payload = JSONObject().put("tierKey", tierKey)
        socket?.emit("tier:join_room", payload)
        Log.d(TAG, "Joined tier room: $tierKey")
    }

    fun leaveTierRoom() {
        socket?.emit("tier:leave_room")
    }

    // Use WS heartbeat instead of REST (optional — REST is fallback)
    fun sendHeartbeat(sessionId: String) {
        if (socket?.connected() != true) return
        val payload = JSONObject().put("sessionId", sessionId)
        socket?.emit("session:heartbeat", payload)
    }

    // ── Lifecycle ─────────────────────────────────────────────

    fun disconnect() {
        socket?.disconnect()
        socket = null
        _isConnected.value = false
    }

    fun reconnect() {
        disconnect()
        connect()
    }
}
