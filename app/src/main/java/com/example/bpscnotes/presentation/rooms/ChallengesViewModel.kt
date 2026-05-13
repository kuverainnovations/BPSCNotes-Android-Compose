package com.example.bpscnotes.presentation.rooms

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ════════════════════════════════════════════════════════════
// FILE: presentation/rooms/ChallengesViewModel.kt
// ════════════════════════════════════════════════════════════

data class ChallengesUiState(
    val challenges: List<ChallengeDto>  = emptyList(),
    val weekLabel: String               = "",
    val periodKey: String               = "",
    val isLoading: Boolean              = true,
    val isClaiming: Boolean             = false,
    val error: String?                  = null,
    val claimSuccess: String?           = null
)

@HiltViewModel
class ChallengesViewModel @Inject constructor(
    private val api: ChallengesApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChallengesUiState())
    val uiState: StateFlow<ChallengesUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.challenges.isEmpty(), error = null) }
            try {
                val res  = api.getCurrent()
                val data = res.data
                _uiState.update {
                    it.copy(
                        challenges = data?.challenges ?: emptyList(),
                        weekLabel  = data?.weekLabel  ?: "",
                        periodKey  = data?.periodKey  ?: "",
                        isLoading  = false,
                    )
                }
            } catch (e: Exception) {
                Log.e("ChallengesVM", e.message ?: "", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load challenges") }
            }
        }
    }

    fun claimReward(challengeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isClaiming = true, claimSuccess = null, error = null) }
            try {
                val res  = api.claimReward(challengeId)
                val data = res.data
                // Optimistically mark as claimed in UI
                _uiState.update { state ->
                    state.copy(
                        isClaiming   = false,
                        claimSuccess = "🎉 +${data?.coinsRewarded ?: 0} coins claimed!",
                        challenges   = state.challenges.map { c ->
                            if (c.id == challengeId) c.copy(rewardClaimed = true) else c
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("ChallengesVM", "claimReward: ${e.message}", e)
                _uiState.update { it.copy(isClaiming = false, error = e.message ?: "Claim failed") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(claimSuccess = null, error = null) }
    }
}
