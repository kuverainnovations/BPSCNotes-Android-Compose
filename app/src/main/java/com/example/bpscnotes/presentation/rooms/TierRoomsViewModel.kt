package com.example.bpscnotes.presentation.rooms

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

    // Which tier the user is currently browsing (may differ from their own tier)
    // Default "starter" so leaderboard loads immediately without waiting for getMyTier
    val selectedTierKey: String                     = "starter",
    // Real-time WebSocket state
    val isSocketConnected: Boolean                  = false,
    val pendingPromotion: PromotionEvent?            = null,
    val pendingDemotion: DemotionEvent?              = null,
    // Current user's ID — used to filter self from members list
    val myUserId: String                            = "",
)

@HiltViewModel
class TierRoomsViewModel @Inject constructor(
    private val api:        TierRoomsApiService,
    private val socket:     TierRoomsSocketManager,
    private val tokenStore: com.example.bpscnotes.data.local.TokenStore
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
                    val tierKey = _uiState.value.myTierData?.currentTier?.tierKey
                        ?: _uiState.value.selectedTierKey
                    socket.joinTierRoom(tierKey)
                }
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
                // If it's the currently viewed tier, refresh members immediately
                if (event.tierKey == _uiState.value.selectedTierKey) {
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
        // Observe leaderboard ticks — update in-memory leaderboard
        viewModelScope.launch {
            socket.leaderboardTicks.collect { tick ->
                val selected = _uiState.value.selectedTierKey
                if (tick.tierKey == selected) loadLeaderboard(selected)
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
                    // Full details (XP, streak) will load on next loadMembers() call.
                    val newMember = TierMemberDto(
                        id                = event.userId,
                        name              = event.userName,
                        isStudyingNow     = true,
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

        // BUG FIX: Real-time member leave — remove immediately
        viewModelScope.launch {
            socket.memberLeaves.collect { event ->
                val myTierKey = _uiState.value.myTierData?.currentTier?.tierKey ?: return@collect
                if (event.tierKey != myTierKey) return@collect
                _uiState.update { state ->
                    state.copy(
                        members = state.members.filter { it.id != event.userId },
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
                val tierKey = _uiState.value.myTierData?.currentTier?.tierKey ?: continue
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
            _uiState.update { it.copy(isLoadingTiers = false, tiersError = e.message ?: "Failed to load tier rooms") }
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
                // Join the correct socket room now that we know the user's actual tier
                socket.joinTierRoom(tierKey)
            } catch (e: Exception) {
                Log.e(TAG, "loadMyTier: ${e.message}", e)
                _uiState.update { it.copy(isLoadingMyTier = false, myTierError = e.message ?: "Failed to load your tier") }
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

    // ── Select a tier to browse (does not change user's tier) ─
    fun selectTier(tierKey: String) {
        _uiState.update { it.copy(selectedTierKey = tierKey) }
        loadLeaderboard(tierKey)   // show leaderboard for any tier (browse mode)
        loadMembers(tierKey)       // show members of any tier
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
                val data = res.data ?: return@launch
                if (data.success) {
                    // Refresh tier data — user is now in Serious/Consistent/Achiever
                    loadAll()
                    onSuccess(data.newTierEmoji ?: "🥇", data.newTierName ?: "Serious")
                } else {
                    onFail(data.missing.joinToString("\n") { "• $it" }.ifEmpty { data.message })
                }
            } catch (e: Exception) {
                Log.e(TAG, "claimPromotion: ${e.message}", e)
                onFail(e.message ?: "Promotion failed. Please try again.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        socket.leaveTierRoom()
    }
}