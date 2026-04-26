package com.example.bpscnotes.presentation.profile

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.UserStatsData
import com.example.bpscnotes.data.remote.api.UserStatsApiService
import com.example.bpscnotes.data.remote.dto.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI models (keep matching what ProfileScreen sub-composables expect) ────────




data class WeekDayUi(val label: String, val status: DayStatus)

data class ProfileUiState(
    val user: UserDto?                        = null,
    val stats: UserStatsData?                 = null,
    val subjects: List<SubjectProgress>       = emptyList(),
    val badges: List<BadgeItem>             = emptyList(),
    val weekDays: List<WeekDayUi>             = emptyList(),
    val isLoading: Boolean                    = true,
    val error: String?                        = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authApi: AuthApiService,
    private val statsApi: UserStatsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userJob  = async { authApi.getMe().data?.user }
                val statsJob = async {
                    try { statsApi.getStats().data } catch (e: Exception) { null }
                }
                val user  = userJob.await()
                val stats = statsJob.await()

                _uiState.update {
                    it.copy(
                        user      = user,
                        stats     = stats,
                        subjects  = buildSubjects(stats),
                        badges    = buildBadges(user, stats),
                        weekDays  = buildWeekDays(stats),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", e.message ?: "", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load profile") }
            }
        }
    }

    private fun buildSubjects(stats: UserStatsData?): List<SubjectProgress> {
        val defaults = listOf(
            SubjectProgress("Polity",    "⚖️",  0f, Color(0xFF1565C0L), Color(0xFFE3F2FDL)),
            SubjectProgress("History",   "🏛️", 0f, Color(0xFFFF8F00L),Color( 0xFFFFF3E0L)),
            SubjectProgress("Geography", "🗺️", 0f, Color(0xFF2E7D32L), Color(0xFFE8F5E9L)),
            SubjectProgress("Economy",   "📊",  0f, Color(0xFF7B1FA2L),Color( 0xFFF3E5F5L)),
            SubjectProgress("Bihar GK",  "🏔️", 0f, Color(0xFF00838FL),Color( 0xFFE0F7FAL)),
        )
        if (stats == null) return defaults

        // stats.subjectAccuracy would be a list if the backend returns it
        // Map backend accuracy by subject name → progress 0–1
        return defaults.map { sub ->
            sub // extend when backend returns per-subject stats
        }
    }

    private fun buildBadges(user: UserDto?, stats: UserStatsData?): List<BadgeItem> {
        val streak = stats?.currentStreak ?: user?.streak ?: 0

        val accuracy = stats?.accuracy
            ?: user?.accuracy?.toDoubleOrNull()
            ?: 0.0

        val rank = user?.rank
        val quizzes = user?.quizzesAttempted ?: 0

        return listOf(
            BadgeItem("🔥", "7-Day Streak",  streak >= 7,                Color(0xFFFFF8E1L)),
            BadgeItem("⚡", "Speed Reader",  accuracy >= 85.0,           Color(0xFFE3F2FDL)),
            BadgeItem("🎯", "Sharpshooter", accuracy >= 90.0,           Color(0xFFE8F5E9L)),
            BadgeItem("👑", "Top Ranker",   rank != null && rank <= 10, Color(0xFFF3E5F5L)),
            BadgeItem("📚", "100 Topics",   quizzes >= 100,             Color(0xFFFFF3E0L)),
        )
    }

    private fun buildWeekDays(stats: UserStatsData?): List<WeekDayUi> {
        val labels     = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val activity   = stats?.weeklyActivity ?: emptyList()
        val todayIndex = java.util.Calendar.getInstance()
            .get(java.util.Calendar.DAY_OF_WEEK)
            .let { if (it == java.util.Calendar.SUNDAY) 6 else it - 2 }   // Mon=0 … Sun=6

        return labels.mapIndexed { i, label ->
            val hasActivity = activity.getOrNull(i)?.activity ?: 0
            val status = when {
                i == todayIndex -> DayStatus.TODAY
                hasActivity > 0 -> DayStatus.DONE
                else            -> DayStatus.MISSED
            }
            WeekDayUi(label, status)
        }
    }
}
