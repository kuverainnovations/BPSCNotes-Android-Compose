package com.example.bpscnotes.presentation.rooms

import com.example.bpscnotes.core.network.toUserMessage

import com.example.bpscnotes.data.remote.api.TierRoomsApiService

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.*
import com.example.bpscnotes.data.remote.TierRoomsSocketManager
import com.example.bpscnotes.data.remote.PromotionEvent
import com.example.bpscnotes.data.remote.DemotionEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// ════════════════════════════════════════════════════════════
// FILE: presentation/rooms/TierRoomsViewModel.kt
//
// Manages: all tier rooms list, user's current tier + progress,
//          leaderboard, tier members.
// Used by: RoomsHubScreen, TierRoomScreen
// ════════════════════════════════════════════════════════════

data class AtRiskUiState(
    val isAtRisk:  Boolean = false,
    val progress:  Float   = 0f,
    val threshold: Float   = 50f,
    val tierKey:   String  = "",
    val tierName:  String  = "",
    val tierEmoji: String  = "",
)

data class TierRoomsUiState(
    // All 4 tiers (Starter/Serious/Consistent/Achiever)
    val allTiers: List<RoomTierDto>                 = emptyList(),
    val isLoadingTiers: Boolean                     = true,
    val tiersError: String?                         = null,

    // User's current tier + progress
    val myTierData: MyTierResponseData?             = null,
    val isLoadingMyTier: Boolean                    = true,
    val atRisk: AtRiskUiState                       = AtRiskUiState(),
    val showDemotionBanner: Boolean                 = true,
    val myTierError: String?                        = null,

    // Leaderboard for currently viewed tier
    val leaderboard: List<LeaderboardEntryDto>      = emptyList(),
    val leaderboardPeriod: String                   = "weekly",
    val leaderboardPeriodKey: String                = "",
    val isLoadingLeaderboard: Boolean               = false,
    val leaderboardError: String?                   = null,

    // Members of currently viewed tier
    val members: List<TierMemberDto>                = emptyList(),
    val isLoadingMembers: Boolean                   = false,
    val membersError: String?                       = null,

    // Room Insights card (spec section 4)
    val roomStats: RoomStatsResponseData?            = null,
    val isLoadingRoomStats: Boolean                  = false,

    // Room Champions (redesign section 5)
    val champions: RoomChampionsResponseData?         = null,
    val isLoadingChampions: Boolean                   = false,

    // Personal ranking (redesign section 4)
    val myRank: MyRankResponseData?                   = null,
    val isLoadingMyRank: Boolean                      = false,

    // Room Activity Feed (spec section 9) — newest first
    val activityFeed: List<ActivityFeedEntryDto>     = emptyList(),
    val isLoadingActivityFeed: Boolean                = false,

    // Which tier the user is currently browsing (may differ from their own tier)
    // Default "starter" so leaderboard loads immediately without waiting for getMyTier
    val selectedTierKey: String                     = "starter",
    // Real-time WebSocket state
    val isSocketConnected: Boolean                  = false,
    // true once the socket has connected at least once this app session —
    // distinguishes "Reconnecting" (was connected, dropped) from the
    // initial "Connecting" state (spec section 11: connection states).
    val hasConnectedBefore: Boolean                  = false,
    val pendingPromotion: PromotionEvent?            = null,
    val pendingDemotion: DemotionEvent?              = null,
    // Current user's ID — used to filter self from members list
    val myUserId: String                            = "",
)

@HiltViewModel
class TierRoomsViewModel @Inject constructor(
    private val api:        TierRoomsApiService,
    private val socket:     TierRoomsSocketManager,
    private val tokenStore: com.example.bpscnotes.data.local.TokenStore,
    private val cacheInvalidator: com.example.bpscnotes.core.network.CacheInvalidator
) : ViewModel() {

    private val _uiState = MutableStateFlow(TierRoomsUiState())
    val uiState: StateFlow<TierRoomsUiState> = _uiState.asStateFlow()

    val currentUserId = tokenStore.getUserId() ?: ""

    val myUserId = currentUserId


    companion object {
        private const val TAG = "TierRoomsVM"
    }

    init {
        // Load user ID from TokenStore so screens can filter self from member lists
        val userId = tokenStore.getUserId() ?: ""
        _uiState.update { it.copy(myUserId = userId) }
        loadAll()
        connectSocket()
    }

    private fun connectSocket() {
        socket.connect()
        viewModelScope.launch {
            // Observe connection state
            socket.isConnected.collect { connected ->
                _uiState.update { it.copy(isSocketConnected = connected) }
                if (connected) {
                    // Join user's tier room. myTierData may not be loaded yet on first connect,
                    // so fall back to selectedTierKey (default "starter").
                    // loadMyTier() calls joinTierRoom again once real tier is known.
                    // BUG FIX: If a study session is currently active in a
                    // different (non-home) room — e.g. user joined "starter"
                    // while their home tier is "consistent" — don't clobber
                    // that join on reconnect. The socket already rejoins
                    // currentTierKey itself via EVENT_CONNECT; only set a
                    // tier here if nothing is currently joined.
                    if (socket.getCurrentTierKey() == null) {
                        val tierKey = _uiState.value.myTierData?.currentTier?.tierKey
                            ?: _uiState.value.selectedTierKey
                        socket.joinTierRoom(tierKey)
                    }
                }
            }
        }
        // Connected vs Reconnecting vs Connecting (spec section 11) — the
        // UI distinguishes "never connected yet" from "was connected, just
        // dropped" using this alongside isSocketConnected.
        viewModelScope.launch {
            socket.hasConnectedBefore.collect { value ->
                _uiState.update { it.copy(hasConnectedBefore = value) }
            }
        }
        // Observe individual presence update events — fires IMMEDIATELY on each join/leave.
        // This is faster than presenceSnapshot (which is the full map, batched).
        viewModelScope.launch {
            socket.presenceUpdates.collect { event ->
                // Update the active count for this tier in the list
                _uiState.update { state ->
                    state.copy(allTiers = state.allTiers.map { tier ->
                        if (tier.tierKey == event.tierKey) tier.copy(activeSessions = event.activeNow)
                        else tier
                    })
                }
                // If it's the room the user is actually sitting in (which
                // may differ from their home tier — e.g. studying in a
                // lower unlocked tier), refresh the member list immediately.
                // Using selectedTierKey (home tier) here meant a user
                // studying in a non-home room never saw new joiners.
                val viewedTierKey = socket.getCurrentTierKey() ?: _uiState.value.selectedTierKey
                if (event.tierKey == viewedTierKey) {
                    loadMembers(event.tierKey)
                }
            }
        }
        // Also collect the full snapshot for initial state on connect
        viewModelScope.launch {
            socket.presenceSnapshot.collect { snapshot ->
                _uiState.update { state ->
                    state.copy(allTiers = state.allTiers.map { tier ->
                        val liveCount = snapshot[tier.tierKey]
                        if (liveCount != null) tier.copy(activeSessions = liveCount) else tier
                    })
                }
            }
        }
        // Observe promotion events
        viewModelScope.launch {
            socket.promotionEvents.collect { event ->
                _uiState.update { it.copy(pendingPromotion = event) }
            }
        }
        // Observe demotion events
        viewModelScope.launch {
            socket.demotionEvents.collect { event ->
                _uiState.update { it.copy(pendingDemotion = event) }
            }
        }
        // Observe leaderboard ticks — refresh the rich member list (now the
        // leaderboard screen's primary data source), champions, and the
        // legacy snapshot leaderboard together.
        viewModelScope.launch {
            socket.leaderboardTicks.collect { tick ->
                val selected = _uiState.value.selectedTierKey
                if (tick.tierKey == selected) {
                    loadLeaderboard(selected)
                    loadMembers(selected)
                    loadChampions(selected)
                    loadMyRank(selected)
                }
            }
        }

        // BUG FIX: Real-time member join — add member to list immediately without API call.
        // Previously members only appeared after going back + returning (no auto-refresh).
        viewModelScope.launch {
            socket.memberJoins.collect { event ->
                val myTierKey = _uiState.value.myTierData?.currentTier?.tierKey ?: return@collect
                if (event.tierKey != myTierKey) return@collect
                val myUserId = _uiState.value.myUserId
                // Don't add self to the member list — user sees themselves in "You" card
                if (event.userId == myUserId) return@collect
                val existing = _uiState.value.members.any { it.id == event.userId }
                if (!existing) {
                    // Optimistic add: create a minimal TierMemberDto so the card appears instantly.
                    // Full details (XP, streak, rank, time windows) will load on next loadMembers() call.
                    val newMember = TierMemberDto(
                        id                = event.userId,
                        name              = event.userName,
                        isStudyingNow     = true,
                        status            = "studying",
                        xpLevel           = 1,
                        streak            = 0,
                        totalStudyMinutes = 0
                    )
                    _uiState.update { it.copy(members = it.members + newMember) }
                }
                // Also increment active count in tier cards
                _uiState.update { state ->
                    state.copy(allTiers = state.allTiers.map { tier ->
                        if (tier.tierKey == event.tierKey)
                            tier.copy(activeSessions = (tier.activeSessions + 1).coerceAtLeast(0))
                        else tier
                    })
                }
            }
        }

        // Real-time member leave — transition them to "online" rather than
        // removing them from the roster. The old behavior (filtering them
        // out entirely) predates the redesign's status tabs: it made sense
        // when "members" only ever meant "currently studying", but now
        // that the same list backs the leaderboard's All/Online/Offline
        // tabs too, removing the row would make them vanish from "All
        // Members" instead of correctly showing as Online until the next
        // loadMembers() poll settles their real last-active-based status.
        viewModelScope.launch {
            socket.memberLeaves.collect { event ->
                val myTierKey = _uiState.value.myTierData?.currentTier?.tierKey ?: return@collect
                if (event.tierKey != myTierKey) return@collect
                _uiState.update { state ->
                    state.copy(
                        members = state.members.map { m ->
                            if (m.id == event.userId) m.copy(isStudyingNow = false, status = "online")
                            else m
                        },
                        allTiers = state.allTiers.map { tier ->
                            if (tier.tierKey == event.tierKey)
                                tier.copy(activeSessions = (tier.activeSessions - 1).coerceAtLeast(0))
                            else tier
                        }
                    )
                }
            }
        }

        // Safety-net refresh every 5s during active session for real-time member list.
        // Primary updates come from room:member_joined/left socket events (immediate).
        // This catches any missed events (network jitter, race conditions).
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5_000L)
                // The room the user is physically sitting in (socket-joined),
                // which may differ from their home tier if they joined a
                // lower unlocked tier's room.
                val tierKey = socket.getCurrentTierKey()
                    ?: _uiState.value.myTierData?.currentTier?.tierKey
                    ?: continue
                // Only refresh if user is actually in session (saves API calls on lobby)
                if (_uiState.value.isSocketConnected) {
                    loadMembers(tierKey)
                }
            }
        }
    }

    // ── Load everything in parallel ───────────────────────────
    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTiers = true, isLoadingMyTier = true) }

            // Load tiers + my tier in parallel
            // Also pre-load leaderboard for default "starter" tier so it's ready immediately
            val tiersJob       = async { loadTiers() }
            val myTierJob      = async { loadMyTier() }
            val leaderboardJob = async { loadLeaderboard("starter") }

            tiersJob.await()
            myTierJob.await()
            leaderboardJob.await()
        }
    }

    // ── All tier rooms ─────────────────────────────────────────
    private suspend fun loadTiers() {
        try {
            val response = api.getAllTiers()
            val tiers    = response.data?.tiers ?: emptyList()
            _uiState.update { it.copy(allTiers = tiers, isLoadingTiers = false, tiersError = null) }
        } catch (e: Exception) {
            Log.e(TAG, "loadTiers: ${e.message}", e)
            _uiState.update { it.copy(isLoadingTiers = false, tiersError = e.toUserMessage("Failed to load tier rooms")) }
        }
    }

    // ── My tier + progress ────────────────────────────────────
    fun loadAtRiskStatus() {
        viewModelScope.launch {
            try {
                val res  = api.getAtRiskStatus()
                val data = res.data ?: return@launch
                _uiState.update { s -> s.copy(
                    atRisk = AtRiskUiState(
                        isAtRisk  = data.isAtRisk,
                        progress  = data.progress,
                        threshold = data.threshold,
                        tierKey   = data.tierKey,
                        tierName  = data.tierName,
                        tierEmoji = data.tierEmoji,
                    )
                )}
            } catch (e: Exception) {
                android.util.Log.w("TierRoomsVM", "loadAtRisk: ${e.message}")
            }
        }
    }

    fun dismissDemotionBanner() {
        _uiState.update { it.copy(showDemotionBanner = false) }
    }

    /**
     * Lightweight refresh of just the "my tier" member/active counts,
     * bypassing the 5-minute HTTP cache. Used by RoomsHubScreen's
     * periodic poll so the hero card's "X members · Y online now" stays
     * current without re-fetching the leaderboard/members list or
     * rejoining the socket room on every tick.
     */
    fun refreshMyTierCounts() {
        viewModelScope.launch {
            try {
                cacheInvalidator.evict(listOf("/api/v1/rooms"))
                val response = api.getMyTier()
                val data = response.data ?: return@launch
                _uiState.update { it.copy(myTierData = data) }
            } catch (e: Exception) {
                Log.w(TAG, "refreshMyTierCounts: ${e.message}")
            }
        }
    }

    fun loadMyTier() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMyTier = true, myTierError = null) }
            try {
                val response = api.getMyTier()
                val data     = response.data ?: throw Exception("Empty tier response")

                // Safety: fallback if tierKey is null from API
                val tierKey = data.currentTier.tierKey ?: "starter"

                _uiState.update { s ->
                    s.copy(
                        myTierData      = data,
                        isLoadingMyTier = false,
                        // Update selectedTierKey to actual user tier
                        selectedTierKey = tierKey,
                    )
                }
                // Load leaderboard for user's actual tier
                loadLeaderboard(tierKey)
                // Load members for the tab
                loadMembers(tierKey)
                // Room Insights (spec section 4)
                loadRoomStats(tierKey)
                // Champions + personal rank (redesign sections 4, 5)
                loadChampions(tierKey)
                loadMyRank(tierKey)
                // Join the correct socket room now that we know the user's actual tier —
                // but don't clobber an active session's join to a different room
                // (e.g. user started a session in a lower unlocked tier).
                val joined = socket.getCurrentTierKey()
                if (joined == null || joined == tierKey) {
                    socket.joinTierRoom(tierKey)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadMyTier: ${e.message}", e)
                _uiState.update { it.copy(isLoadingMyTier = false, myTierError = e.toUserMessage("Failed to load your tier")) }
            }
        }
    }

    // ── Leaderboard ───────────────────────────────────────────
    fun loadLeaderboard(tierKey: String, period: String = "weekly") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLeaderboard = true, leaderboardError = null) }
            try {
                val response = api.getTierLeaderboard(tierKey, period)
                val data     = response.data ?: throw Exception("Empty leaderboard response")
                _uiState.update { s ->
                    s.copy(
                        leaderboard          = data.leaderboard,
                        leaderboardPeriod    = data.period,
                        leaderboardPeriodKey = data.periodKey,
                        isLoadingLeaderboard = false,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadLeaderboard: ${e.message}", e)
                _uiState.update { it.copy(isLoadingLeaderboard = false, leaderboardError = e.message) }
            }
        }
    }

    // ── Tier members ──────────────────────────────────────────
    fun loadMembers(tierKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMembers = true, membersError = null) }
            try {
                // This is called from both periodic polls and socket-triggered
                // refreshes (room:member_joined etc.) — both want live data,
                // but GET /rooms/tiers/{tierKey}/members is cached for 5
                // minutes by the network layer. Evict first so we don't keep
                // serving the same snapshot for up to 5 minutes.
                cacheInvalidator.evict(listOf("/api/v1/rooms"))
                val response = api.getTierMembers(tierKey)
                _uiState.update { s ->
                    s.copy(
                        members          = response.data?.members ?: emptyList(),
                        isLoadingMembers = false,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadMembers: ${e.message}", e)
                _uiState.update { it.copy(isLoadingMembers = false, membersError = e.message) }
            }
        }
    }

    // ── Room Statistics (spec section 4) ───────────────────────
    fun loadRoomStats(tierKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRoomStats = true) }
            try {
                val response = api.getRoomStats(tierKey)
                _uiState.update { it.copy(roomStats = response.data, isLoadingRoomStats = false) }
            } catch (e: Exception) {
                Log.w(TAG, "loadRoomStats: ${e.message}")
                _uiState.update { it.copy(isLoadingRoomStats = false) }
            }
        }
    }

    // ── Room Champions (redesign section 5) ────────────────────
    fun loadChampions(tierKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChampions = true) }
            try {
                val response = api.getRoomChampions(tierKey)
                _uiState.update { it.copy(champions = response.data, isLoadingChampions = false) }
            } catch (e: Exception) {
                Log.w(TAG, "loadChampions: ${e.message}")
                _uiState.update { it.copy(isLoadingChampions = false) }
            }
        }
    }

    // ── Personal ranking (redesign section 4) ──────────────────
    fun loadMyRank(tierKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMyRank = true) }
            try {
                val response = api.getMyRank(tierKey)
                _uiState.update { it.copy(myRank = response.data, isLoadingMyRank = false) }
            } catch (e: Exception) {
                Log.w(TAG, "loadMyRank: ${e.message}")
                _uiState.update { it.copy(isLoadingMyRank = false) }
            }
        }
    }

    // ── Room Activity Feed (spec section 9) ────────────────────
    // Initial/refresh load via REST; new entries afterward arrive live
    // through the activityFeedEvents socket collector above.
    fun loadActivityFeed(tierKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingActivityFeed = true) }
            try {
                val response = api.getActivityFeed(tierKey)
                _uiState.update {
                    it.copy(activityFeed = response.data?.feed ?: emptyList(), isLoadingActivityFeed = false)
                }
            } catch (e: Exception) {
                Log.w(TAG, "loadActivityFeed: ${e.message}")
                _uiState.update { it.copy(isLoadingActivityFeed = false) }
            }
        }
    }

    // ── Select a tier to browse (does not change user's tier) ─
    fun selectTier(tierKey: String) {
        _uiState.update { it.copy(selectedTierKey = tierKey) }
        loadLeaderboard(tierKey)   // show leaderboard for any tier (browse mode)
        loadMembers(tierKey)       // show members of any tier
        loadRoomStats(tierKey)
        loadChampions(tierKey)
        loadMyRank(tierKey)
        socket.joinTierRoom(tierKey)
    }

    fun clearPendingPromotion() {
        _uiState.update { it.copy(pendingPromotion = null) }
    }

    fun clearPendingDemotion() {
        _uiState.update { it.copy(pendingDemotion = null) }
    }

    fun clearErrors() {
        _uiState.update { it.copy(tiersError = null, myTierError = null, leaderboardError = null, membersError = null) }
    }

    // ── User-initiated promotion (when requirements are met) ──
    // Calls POST /rooms/tiers/claim-promotion.
    // Backend re-verifies before promoting — safe to call from UI.
    fun claimPromotion(
        onSuccess: (emoji: String, name: String) -> Unit,
        onFail:    (reason: String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val res  = api.claimPromotion()
                val data = res.data
                val promoted = data?.promotedTo
                if (promoted != null) {
                    // Refresh tier data — user is now in new tier
                    loadAll()
                    onSuccess(promoted.emoji ?: "🥇", promoted.name ?: "")
                } else {
                    onFail(res.message ?: "Promotion failed. Please try again.")
                }
            } catch (e: retrofit2.HttpException) {
                val body = e.response()?.errorBody()?.string() ?: ""
                val msg = try { org.json.JSONObject(body).optString("message", "Not yet eligible") } catch (_: Exception) { "Not yet eligible" }
                // The claim was rejected as "not yet eligible" despite the UI
                // showing "Claim Now" — myTierData/progressItems are stale
                // (e.g. a cached snapshot from before/after a recent
                // promotion). Refresh so isClaimReady recomputes from live
                // data and the user doesn't keep tapping a claim that will
                // never succeed.
                loadMyTier()
                onFail(msg)
            } catch (e: Exception) {
                Log.e(TAG, "claimPromotion: ${e.message}", e)
                onFail(e.toUserMessage("Promotion failed. Please try again."))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        socket.leaveTierRoom()
    }
}