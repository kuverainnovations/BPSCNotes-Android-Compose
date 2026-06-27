package com.example.bpscnotes.presentation.rooms

import com.example.bpscnotes.core.network.toUserMessage

import com.example.bpscnotes.data.remote.api.TierRoomsApiService

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.core.analytics.Event
import com.example.bpscnotes.data.remote.api.*
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// ════════════════════════════════════════════════════════════
// FILE: presentation/rooms/StudySessionViewModel.kt
//
// Manages the full lifecycle of a study session:
//   1. Start session  → POST /rooms/sessions/start
//   2. Heartbeat loop → POST /rooms/sessions/heartbeat every 5 min
//   3. AFK detection  → backend detects gap > 7 min, UI shows warning
//   4. End session    → POST /rooms/sessions/end
//   5. Resume check   → GET /rooms/sessions/active on app resume
//
// Coin counter updates live after each heartbeat.
// AFK state shown with a dismissible snackbar.
// Session summary shown in EndSessionSheet after endSession().
// ════════════════════════════════════════════════════════════

// ── UI state ──────────────────────────────────────────────────

enum class SessionStatus {
    IDLE,           // No session
    STARTING,       // POST /start in progress
    ACTIVE,         // Session running, heartbeat ticking
    AFK,            // Last heartbeat returned isAfk=true
    ENDING,         // POST /end in progress
    ENDED,          // Session just ended — summary available
    ERROR           // Start or heartbeat failed
}

data class StudySessionUiState(
    val status: SessionStatus               = SessionStatus.IDLE,

    // Active session data
    val sessionId: String?                  = null,
    val startedAt: String?                  = null,
    val mode: String                        = "study",
    // The tier-room KEY this session is in (e.g. "starter") — distinct
    // from the user's home/reward tier. Used to join the correct chat
    // room (must match the socket's joined tier:* room).
    val roomTierKey: String?                = null,
    val tierName: String?                   = null,
    val tierEmoji: String?                  = null,
    val tierColorHex: String?               = null,

    // Live counters (update after each heartbeat)
    val activeMinutes: Int                  = 0,
    val elapsedSeconds: Int                 = 0,   // live second-by-second counter (ViewModel-owned)
    val coinsThisSession: Int               = 0,
    val xpThisSession: Int                  = 0,
    val afkCount: Int                       = 0,

    // Last heartbeat result (shown in snackbar/chip)
    val coinsLastBeat: Int                  = 0,
    val xpLastBeat: Int                     = 0,
    val wasAfkLastBeat: Boolean             = false,

    // Session summary (populated after endSession)
    val summary: EndSessionResponseData?    = null,

    // Error
    val error: String?                      = null,

    // Heartbeat interval received from backend (default 300s)
    val heartbeatIntervalSeconds: Int       = 300
)

// ════════════════════════════════════════════════════════════
// VIEW MODEL
// ════════════════════════════════════════════════════════════

@HiltViewModel
class StudySessionViewModel @Inject constructor(
    private val tierRoomsApi: TierRoomsApiService,
    private val socket: com.example.bpscnotes.data.remote.TierRoomsSocketManager,
    private val bus: RefreshEventBus) : ViewModel() {

    private val _uiState = MutableStateFlow(StudySessionUiState())
    val uiState: StateFlow<StudySessionUiState> = _uiState.asStateFlow()

    private var heartbeatJob: Job? = null
    // Remembers the user's home-tier room key so socket presence can be
    // moved back there when a session started in a different (lower)
    // tier room ends. Set by startSession's caller via homeTierKey param.
    // Named differently from the startSession parameter to avoid shadowing.
    private var activeSessionHomeTierKey: String? = null

    companion object {
        private const val TAG = "StudySessionVM"
    }

    // ── 1. Check for existing active session on app start/resume ──
    //    Called from RoomsHubScreen's LaunchedEffect
    fun checkForExistingSession() {
        viewModelScope.launch {
            try {
                val response = tierRoomsApi.getActiveSession()
                val session  = response.data?.session
                if (session != null) {
                    Log.d(TAG, "Restored active session: ${session.id}")
                    // Calculate wall-clock elapsed from startedAt so the timer
                    // shows real time even if heartbeats were missed while offline.
                    val startMs = try {
                        val fmts = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'")
                        fmts.firstNotNullOfOrNull { fmt ->
                            val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.getDefault())
                            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            try { sdf.parse(session.startedAt)?.time } catch (_: Exception) { null }
                        } ?: System.currentTimeMillis()
                    } catch (_: Exception) { System.currentTimeMillis() }
                    val wallClockSecs = ((System.currentTimeMillis() - startMs) / 1000L)
                        .toInt().coerceAtLeast(session.activeMinutes * 60)
                    _uiState.update { s ->
                        s.copy(
                            status           = if (session.isAfkNow) SessionStatus.AFK else SessionStatus.ACTIVE,
                            sessionId        = session.id,
                            startedAt        = session.startedAt,
                            mode             = session.mode,
                            activeMinutes    = session.activeMinutes,
                            elapsedSeconds   = wallClockSecs,   // wall-clock, not last heartbeat
                            coinsThisSession = session.coinsEarned,
                            xpThisSession    = session.xpEarned,
                            afkCount         = session.afkCount,
                            roomTierKey      = session.tier?.tierKey,
                            tierName         = session.tier?.name,
                            tierEmoji        = session.tier?.iconEmoji,
                            tierColorHex     = session.tier?.colorHex,
                        )
                    }
                    // Resume heartbeat + elapsed timer for the restored session
                    startHeartbeat(session.id)
                    startElapsedTimer()
                }
            } catch (e: Exception) {
                Log.w(TAG, "checkForExistingSession failed: ${e.message}")
                // Non-fatal — user just has no active session
            }
        }
    }

    // ── 2. Start a new session ─────────────────────────────────
    fun startSession(
        roomId: String? = null,
        mode: String = "study",
        tierKey: String? = null,
        homeTierKey: String? = null,
        tierName: String? = null,
        tierEmoji: String? = null,
        tierColorHex: String? = null
    ) {
        if (_uiState.value.status == SessionStatus.ACTIVE) {
            Log.w(TAG, "Tried to start session while one is already active")
            return
        }

        // The user's socket presence defaults to their HOME tier room
        // (joined by TierRoomsViewModel on connect). If they start a
        // session in a different room (e.g. a lower unlocked tier), move
        // the socket presence to that room so the "X studying" live count
        // appears on the room they're actually sitting in, not their home tier.
        if (!tierKey.isNullOrBlank()) {
            activeSessionHomeTierKey = homeTierKey?.takeIf { it != tierKey }
            socket.joinTierRoom(tierKey)
        }

        viewModelScope.launch {
            // FIX: Set tier info immediately so room header shows correct tier name
            // instead of defaulting to "Silver Room" while the session is starting
            _uiState.update { it.copy(
                status       = SessionStatus.STARTING,
                error        = null,
                roomTierKey  = tierKey,
                tierName     = tierName,
                tierEmoji    = tierEmoji,
                tierColorHex = tierColorHex
            ) }
            try {
                val response = tierRoomsApi.startSession(
                    StartSessionRequest(roomId = roomId, mode = mode, tierKey = tierKey)
                )
                val data = response.data ?: throw Exception("Empty response from /start")

                Log.d(TAG, "Session started: ${data.sessionId}")
                _uiState.update { s ->
                    s.copy(
                        status                   = SessionStatus.ACTIVE,
                        sessionId                = data.sessionId,
                        startedAt                = data.startedAt,
                        mode                     = data.mode,
                        activeMinutes            = 0,
                        coinsThisSession         = 0,
                        xpThisSession            = 0,
                        afkCount                 = 0,
                        coinsLastBeat            = 0,
                        xpLastBeat               = 0,
                        wasAfkLastBeat           = false,
                        summary                  = null,
                        error                    = null,
                        heartbeatIntervalSeconds = data.heartbeatIntervalSeconds,
                    )
                }
                startHeartbeat(data.sessionId)
                startElapsedTimer()

            } catch (e: Exception) {
                Log.e(TAG, "startSession failed: ${e.message}", e)
                // Session never started — undo the presence-room switch we
                // did before this try block, so the user doesn't appear
                // "studying" in a room they failed to join.
                if (!tierKey.isNullOrBlank() && !activeSessionHomeTierKey.isNullOrBlank()) {
                    socket.joinTierRoom(activeSessionHomeTierKey!!)
                    activeSessionHomeTierKey = null
                }
                val msg = when {
                    e is retrofit2.HttpException -> {
                        val body = e.response()?.errorBody()?.string() ?: ""
                        try {
                            org.json.JSONObject(body).optString("message", "Failed to start session")
                        } catch (_: Exception) {
                            "Failed to start session"
                        }
                    }
                    e.message?.contains("Active session exists") == true ->
                        "You already have an active session. End it first."
                    else -> e.toUserMessage("Failed to start session")
                }
                _uiState.update { it.copy(status = SessionStatus.ERROR, error = msg) }
            }
        }
    }

    // ── 3. Heartbeat loop ──────────────────────────────────────
    // Fires every heartbeatIntervalSeconds (default 300s = 5 min).
    // Backend awards coins for verified active interval.
    // If gap > 7 min: isAfk=true, no coins.
    private fun startHeartbeat(sessionId: String) {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            val intervalMs = _uiState.value.heartbeatIntervalSeconds * 1000L
            Log.d(TAG, "Heartbeat started for session=$sessionId interval=${intervalMs}ms")

            // FIX: Send one early heartbeat at 4.5 min (just above anti-cheat 4-min minimum).
            // Without this, ending before the first 5-min heartbeat shows 0m active time.
            val earlyDelayMs = 270_000L // 4.5 minutes
            if (earlyDelayMs < intervalMs) {
                delay(earlyDelayMs)
                if (_uiState.value.sessionId == sessionId &&
                    _uiState.value.status != SessionStatus.ENDED) {
                    sendHeartbeat(sessionId)
                }
                // Then wait the remaining time before the regular loop kicks in
                val remaining = intervalMs - earlyDelayMs
                if (remaining > 0) delay(remaining)
            } else {
                delay(intervalMs)
            }

            while (isActive) {
                if (_uiState.value.sessionId != sessionId) {
                    Log.d(TAG, "Session changed — stopping heartbeat for $sessionId")
                    break
                }
                sendHeartbeat(sessionId)
                delay(intervalMs)
            }
        }
    }

    private suspend fun sendHeartbeat(sessionId: String) {
        // Retry once on network failure before marking error
        var attempt = 0
        while (attempt < 2) {
            try {
                val response = tierRoomsApi.heartbeat(HeartbeatRequest(sessionId))
                val data     = response.data ?: return

                Log.d(TAG,
                    "Heartbeat: afk=${data.isAfk} coins=+${data.coinsEarnedThisBeat} " +
                            "xp=+${data.xpEarnedThisBeat} activeMins=${data.totalActiveMinutes}"
                )

                _uiState.update { s ->
                    s.copy(
                        status           = if (data.isAfk) SessionStatus.AFK else SessionStatus.ACTIVE,
                        activeMinutes    = data.totalActiveMinutes,
                        // Sync elapsed seconds to server value on every heartbeat
                        // so drift doesn't accumulate over long sessions
                        elapsedSeconds   = data.totalActiveMinutes * 60,
                        coinsThisSession = data.totalCoinsThisSession,
                        xpThisSession    = data.totalXpThisSession,
                        coinsLastBeat    = data.coinsEarnedThisBeat,
                        xpLastBeat       = data.xpEarnedThisBeat,
                        wasAfkLastBeat   = data.isAfk,
                    )
                }
                return  // success
            } catch (e: Exception) {
                attempt++
                if (attempt < 2) {
                    delay(10_000L) // wait 10s before retry
                    Log.w(TAG, "Heartbeat attempt $attempt failed: ${e.message}")
                } else {
                    // Both attempts failed — log but don't crash the loop
                    // The backend will auto-close the session after 15 min of no heartbeats
                    Log.e(TAG, "Heartbeat failed twice: ${e.message}")
                }
            }
        }
    }

    // ── 4. Dismiss AFK warning (user tapped "I'm back") ───────
    fun dismissAfkWarning() {
        if (_uiState.value.status == SessionStatus.AFK) {
            _uiState.update { it.copy(status = SessionStatus.ACTIVE, wasAfkLastBeat = false) }
        }
    }

    // ── 5. End session ─────────────────────────────────────────
    // ── Elapsed timer — runs in ViewModel so it survives navigation (PIP mode) ──
    private var timerJob: kotlinx.coroutines.Job? = null

    private fun startElapsedTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var secs = _uiState.value.activeMinutes * 60
            _uiState.update { it.copy(elapsedSeconds = secs) }
            while (true) {
                delay(1_000L)
                val status = _uiState.value.status
                // Tick for all live states — ACTIVE, AFK, and even ENDING
                // (so the timer doesn't freeze the moment user presses End Session).
                // Only stop on ENDED or IDLE.
                if (status == SessionStatus.ACTIVE ||
                    status == SessionStatus.AFK    ||
                    status == SessionStatus.ENDING) {
                    secs++
                    _uiState.update { it.copy(elapsedSeconds = secs) }
                } else if (status == SessionStatus.ENDED || status == SessionStatus.IDLE) {
                    break
                }
            }
        }
    }

    fun endSession() {
        val sessionId = _uiState.value.sessionId ?: return
        if (_uiState.value.status == SessionStatus.ENDING) return

        heartbeatJob?.cancel()
        heartbeatJob = null

        // Move socket presence back to the user's home tier room, if the
        // session was started in a different (lower) tier room.
        activeSessionHomeTierKey?.let { socket.joinTierRoom(it) }
        activeSessionHomeTierKey = null

        viewModelScope.launch {
            _uiState.update { it.copy(status = SessionStatus.ENDING, error = null) }
            try {
                // Fire one final heartbeat before ending so any time since the
                // last scheduled heartbeat (e.g. screen-off gap) is credited by
                // the backend before the session is closed. The backend's
                // endSession() also does a finalGapSecs credit, but a heartbeat
                // here ensures the DB last_heartbeat timestamp is fresh, which
                // makes the backend's own calculation more accurate.
                try {
                    tierRoomsApi.heartbeat(HeartbeatRequest(sessionId))
                    Log.d(TAG, "Pre-end heartbeat sent")
                } catch (hbErr: Exception) {
                    Log.w(TAG, "Pre-end heartbeat failed (non-fatal): ${hbErr.message}")
                    // Don't abort endSession just because this failed
                }
                val response = tierRoomsApi.endSession(EndSessionRequest(sessionId))
                val summary  = response.data ?: throw Exception("Empty end session response")

                Log.d(TAG,
                    "Session ended: active=${summary.activeMinutes}min " +
                            "coins=${summary.totalCoins} xp=${summary.totalXp}"
                )

                _uiState.update { s ->
                    s.copy(
                        status    = SessionStatus.ENDED,
                        summary   = summary,
                        sessionId = null,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "endSession failed: ${e.message}", e)
                // Build a local summary from what we already know so the summary
                // screen shows real numbers instead of 0m when offline.
                // Backend auto-closes the session when connectivity returns.
                val s = _uiState.value
                val wallClockMins = s.elapsedSeconds / 60
                val bestActiveMin = maxOf(s.activeMinutes, wallClockMins)
                val localSummary  = com.example.bpscnotes.data.remote.api.EndSessionResponseData(
                    sessionId         = sessionId,
                    durationMinutes   = wallClockMins,
                    activeMinutes     = bestActiveMin,
                    todayStudyMinutes = bestActiveMin,
                    totalCoins        = s.coinsThisSession,
                    totalXp           = s.xpThisSession,
                    bonusCoins        = 0,
                    message           = if (e.message?.contains("Unable to resolve") == true ||
                        e.message?.contains("timeout") == true)
                        "Session saved — will sync when internet returns"
                    else "Session ended offline — will be auto-completed"
                )
                _uiState.update { st ->
                    st.copy(
                        status    = SessionStatus.ENDED,
                        summary   = localSummary,
                        sessionId = null,
                        error     = "Offline — showing approximate session data"
                    )
                }
            }
        }
    }

    // ── 6. Reset after viewing summary ────────────────────────
    fun clearSession() {
        _uiState.update { StudySessionUiState() }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ── Cleanup ───────────────────────────────────────────────
    override fun onCleared() {
        super.onCleared()
        heartbeatJob?.cancel()
        Log.d(TAG, "ViewModel cleared — heartbeat stopped")
        // NOTE: session is NOT automatically ended here.
        // The backend cron closes sessions with no heartbeat after 15 min.
        // If we called endSession() here we'd fire a network call from a dead scope.
    }
}