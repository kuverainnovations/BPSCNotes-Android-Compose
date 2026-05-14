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
    // All 4 tiers (Silver/Gold/Premium/Diamond)
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
    // Default "silver" so leaderboard loads immediately without waiting for getMyTier
    val selectedTierKey: String                     = "silver",
    // Real-time WebSocket state
    val isSocketConnected: Boolean                  = false,
    val pendingPromotion: PromotionEvent?            = null,
    val pendingDemotion: DemotionEvent?              = null,
)

@HiltViewModel
class TierRoomsViewModel @Inject constructor(
    private val api:    TierRoomsApiService,
    private val socket: TierRoomsSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TierRoomsUiState())
    val uiState: StateFlow<TierRoomsUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "TierRoomsVM"
    }

    init {
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
                    // Join the user's own tier room on connect
                    val tierKey = _uiState.value.myTierData?.currentTier?.tierKey
                    if (tierKey != null) socket.joinTierRoom(tierKey)
                }
            }
        }
        // Observe live presence updates
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
    }

    // ── Load everything in parallel ───────────────────────────
    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTiers = true, isLoadingMyTier = true) }

            // Load tiers + my tier in parallel
            // Also pre-load leaderboard for default "silver" tier so it's ready immediately
            val tiersJob       = async { loadTiers() }
            val myTierJob      = async { loadMyTier() }
            val leaderboardJob = async { loadLeaderboard("silver") }

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
                val tierKey = data.currentTier.tierKey ?: "silver"
                
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
                // Also load members for the tab
                loadMembers(tierKey)
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

    override fun onCleared() {
        super.onCleared()
        socket.leaveTierRoom()
    }
}