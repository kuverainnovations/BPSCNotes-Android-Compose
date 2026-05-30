package com.example.bpscnotes.presentation.rooms

import com.example.bpscnotes.data.remote.api.AchievementsApiService

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ════════════════════════════════════════════════════════════
// FILE: presentation/rooms/AchievementsViewModel.kt
// ════════════════════════════════════════════════════════════

data class AchievementsUiState(
    val achievements: List<AchievementDto>  = emptyList(),
    val grouped: Map<String, List<AchievementDto>> = emptyMap(),
    val earnedCount: Int                    = 0,
    val totalCount: Int                     = 0,
    val recentlyEarned: List<AchievementDto> = emptyList(),
    val isLoading: Boolean                  = true,
    val error: String?                      = null,
    // Newly earned since last check (for promotion animation)
    val newlyEarned: List<AchievementDto>   = emptyList()
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val api: AchievementsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val allJob    = async { api.getAll() }
                val recentJob = async {
                    try { api.getRecent(5).data?.achievements ?: emptyList() }
                    catch (e: Exception) { emptyList() }
                }
                val allRes    = allJob.await()
                val data      = allRes.data

                // Detect newly earned since last load (for toasts/animations)
                val prevEarned  = _uiState.value.achievements.filter { it.isEarned }.map { it.key }.toSet()
                val nowEarned   = (data?.achievements ?: emptyList()).filter { it.isEarned }.map { it.key }.toSet()
                val newKeys     = nowEarned - prevEarned
                val newlyEarned = (data?.achievements ?: emptyList()).filter { it.key in newKeys }

                _uiState.update {
                    it.copy(
                        achievements  = data?.achievements ?: emptyList(),
                        grouped       = data?.grouped ?: emptyMap(),
                        earnedCount   = data?.earnedCount ?: 0,
                        totalCount    = data?.totalCount  ?: 0,
                        recentlyEarned = recentJob.await(),
                        newlyEarned   = newlyEarned,
                        isLoading     = false,
                    )
                }
            } catch (e: Exception) {
                Log.e("AchievementsVM", e.message ?: "", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load achievements") }
            }
        }
    }

    fun clearNewlyEarned() {
        _uiState.update { it.copy(newlyEarned = emptyList()) }
    }
}
