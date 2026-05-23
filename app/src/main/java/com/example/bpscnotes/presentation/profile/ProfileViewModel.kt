package com.example.bpscnotes.presentation.profile

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.UpdateProfileRequest
import com.example.bpscnotes.data.remote.api.UserStatsData
import com.example.bpscnotes.data.remote.api.UserStatsApiService
import com.example.bpscnotes.data.remote.dto.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


data class WeekDayUi(val label: String, val status: DayStatus)

data class ProfileUiState(
    val user: UserDto?                  = null,
    val stats: UserStatsData?           = null,
    val subjects: List<SubjectProgress> = emptyList(),
    val badges: List<BadgeItem>         = emptyList(),
    val weekDays: List<WeekDayUi>       = emptyList(),
    // Study heatmap — 28 integers (minutes studied per day)
    val studyHeatmap: List<Int>         = emptyList(),
    // Recent coin transactions for profile wallet section
    val recentTransactions: List<com.example.bpscnotes.data.remote.api.CoinTransactionDto> = emptyList(),
    val isLoading: Boolean              = true,
    val isSaving: Boolean               = false,
    val error: String?                  = null,
    val successMessage: String?         = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authApi:    AuthApiService,
    private val statsApi:   UserStatsApiService,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userJob  = async { authApi.getMe().data?.user }
                val statsJob = async { try { statsApi.getStats().data } catch (e: Exception) { null } }
                val user  = userJob.await()
                val stats = statsJob.await()

                // Cache in TokenStore so chat/settings can use without re-fetching
                user?.id?.let   { tokenStore.saveUserId(it) }
                user?.name?.let { tokenStore.saveUserName(it) }

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

    // ── Update profile via PATCH /users/profile ───────────────
    fun updateProfile(name: String, email: String?, bio: String?,
                      district: String?, targetYear: Int?, prepLevel: String?) {
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Name cannot be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, successMessage = null) }
            try {
                val res = authApi.updateProfile(
                    UpdateProfileRequest(
                        name       = name.trim(),
                        email      = email?.trim()?.ifEmpty { null },
                        bio        = bio?.trim()?.ifEmpty { null },
                        district   = district?.trim()?.ifEmpty { null },
                        targetYear = targetYear,
                        prepLevel  = prepLevel?.ifEmpty { null }
                    )
                )
                val updatedUser = res.data?.user
                tokenStore.saveUserName(updatedUser?.name ?: name)
                _uiState.update {
                    it.copy(
                        user           = updatedUser ?: it.user?.copy(name = name, email = email),
                        isSaving       = false,
                        successMessage = "✅ Profile updated!"
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", "updateProfile: ${e.message}", e)
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "Failed to update") }
            }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }

    // ── Builders ───────────────────────────────────────────────
    private fun buildSubjects(stats: UserStatsData?) = listOf(
        SubjectProgress("Polity",    "⚖️",  0f, Color(0xFF1565C0), Color(0xFFE3F2FD)),
        SubjectProgress("History",   "🏛️", 0f, Color(0xFFFF8F00), Color(0xFFFFF3E0)),
        SubjectProgress("Geography", "🗺️", 0f, Color(0xFF2E7D32), Color(0xFFE8F5E9)),
        SubjectProgress("Economy",   "📊",  0f, Color(0xFF7B1FA2), Color(0xFFF3E5F5)),
        SubjectProgress("Bihar GK",  "🏔️", 0f, Color(0xFF00838F), Color(0xFFE0F7FA)),
    )

    private fun buildBadges(user: UserDto?, stats: UserStatsData?): List<BadgeItem> {
        val streak   = stats?.currentStreak ?: user?.streak ?: 0
        val accuracy = stats?.accuracy ?: user?.accuracy?.toDoubleOrNull() ?: 0.0
        val rank     = user?.rank
        val quizzes  = user?.quizzesAttempted ?: 0
        return listOf(
            BadgeItem("🔥", "7-Day Streak",  streak >= 7,               Color(0xFFFFF8E1)),
            BadgeItem("⚡", "Speed Reader",  accuracy >= 85.0,          Color(0xFFE3F2FD)),
            BadgeItem("🎯", "Sharpshooter", accuracy >= 90.0,          Color(0xFFE8F5E9)),
            BadgeItem("👑", "Top Ranker",   rank != null && rank <= 10, Color(0xFFF3E5F5)),
            BadgeItem("📚", "100 Topics",   quizzes >= 100,            Color(0xFFFFF3E0)),
        )
    }

    private fun buildWeekDays(stats: UserStatsData?): List<WeekDayUi> {
        val labels   = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val activity = stats?.weeklyActivity ?: emptyList()
        val today    = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
            .let { if (it == java.util.Calendar.SUNDAY) 6 else it - 2 }
        return labels.mapIndexed { i, label ->
            val has = activity.getOrNull(i)?.activity ?: 0
            WeekDayUi(label, when { i == today -> DayStatus.TODAY; has > 0 -> DayStatus.DONE; else -> DayStatus.MISSED })
        }
    }
}