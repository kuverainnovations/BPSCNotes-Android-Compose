package com.example.bpscnotes.presentation.rooms

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.*
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

data class TierRoomsUiState(
    // All 4 tiers (Silver/Gold/Premium/Diamond)
    val allTiers: List<RoomTierDto>                 = emptyList(),
    val isLoadingTiers: Boolean                     = true,
    val tiersError: String?                         = null,

    // User's current tier + progress
    val myTierData: MyTierResponseData?             = null,
    val isLoadingMyTier: Boolean                    = true,
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
    val selectedTierKey: String?                    = null,
)

@HiltViewModel
class TierRoomsViewModel @Inject constructor(
    private val api: TierRoomsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TierRoomsUiState())
    val uiState: StateFlow<TierRoomsUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "TierRoomsVM"
    }

    init {
        loadAll()
    }

    // ── Load everything in parallel ───────────────────────────
    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTiers = true, isLoadingMyTier = true) }

            val tiersJob   = async { loadTiers() }
            val myTierJob  = async { loadMyTier() }

            tiersJob.await()
            myTierJob.await()
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
    fun loadMyTier() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMyTier = true, myTierError = null) }
            try {
                val response = api.getMyTier()
                val data     = response.data ?: throw Exception("Empty tier response")
                _uiState.update { s ->
                    s.copy(
                        myTierData      = data,
                        isLoadingMyTier = false,
                        // Default selected tier = user's own tier
                        selectedTierKey = s.selectedTierKey ?: data.currentTier.tierKey,
                    )
                }
                // Load leaderboard for the user's tier
                loadLeaderboard(data.currentTier.tierKey)
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
        loadLeaderboard(tierKey)
        loadMembers(tierKey)
    }

    fun clearErrors() {
        _uiState.update { it.copy(tiersError = null, myTierError = null, leaderboardError = null, membersError = null) }
    }
}
