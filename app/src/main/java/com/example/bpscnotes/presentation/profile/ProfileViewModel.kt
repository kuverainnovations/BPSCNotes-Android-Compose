package com.example.bpscnotes.presentation.profile

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.CoinsApiService
import com.example.bpscnotes.data.remote.api.CoinTransactionDto
import com.example.bpscnotes.data.remote.api.UpdateProfileRequest
import com.example.bpscnotes.data.remote.api.UserStatsApiService
import com.example.bpscnotes.data.remote.api.UserStatsData
import com.example.bpscnotes.data.remote.dto.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

data class WeekDayUi(val label: String, val status: DayStatus)

data class ProfileUiState(
    val user: UserDto?                  = null,
    val stats: UserStatsData?           = null,
    val subjects: List<SubjectProgress> = emptyList(),
    val badges: List<BadgeItem>         = emptyList(),
    val weekDays: List<WeekDayUi>       = emptyList(),
    // Study heatmap — 28 integers (minutes studied per day, from API)
    val studyHeatmap: List<Int>         = emptyList(),
    // Coin transactions from API
    val recentTransactions: List<CoinTransactionDto> = emptyList(),
    val isLoading: Boolean              = true,
    val isSaving: Boolean               = false,
    val error: String?                  = null,
    val successMessage: String?         = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authApi:    AuthApiService,
    private val statsApi:   UserStatsApiService,
    private val coinsApi:   CoinsApiService,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Use supervisorScope so one failure doesn't crash the others
                var user: UserDto?  = null
                var stats: UserStatsData? = null
                var transactions: List<CoinTransactionDto> = emptyList()

                supervisorScope {
                    val userJob  = async { authApi.getMe().data?.user }
                    val statsJob = async {
                        try { statsApi.getStats().data } catch (e: Exception) { null }
                    }
                    val txJob = async {
                        try {
                            coinsApi.getTransactions(limit = 5).data?.transactions ?: emptyList()
                        } catch (e: Exception) { emptyList() }
                    }

                    user         = userJob.await()
                    stats        = statsJob.await()
                    transactions = txJob.await()
                }

                // Persist for other screens
                user?.id?.let   { tokenStore.saveUserId(it) }
                user?.name?.let { tokenStore.saveUserName(it) }

                // Build 28-day heatmap from weekly_activity (now 28 days from backend)
                val heatmap = buildHeatmap(stats)

                // Build subject progress from REAL subjectAccuracy API data
                val subjects = buildSubjects(stats)

                _uiState.update {
                    it.copy(
                        user               = user,
                        stats              = stats,
                        subjects           = subjects,
                        badges             = buildBadges(user, stats),
                        weekDays           = buildWeekDays(stats),
                        studyHeatmap       = heatmap,
                        recentTransactions = transactions,
                        isLoading          = false
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", e.message ?: "", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load profile") }
            }
        }
    }

    // ── Heatmap — 28 days of study minutes ────────────────────
    private fun buildHeatmap(stats: UserStatsData?): List<Int> {
        val activity = stats?.weeklyActivity ?: return List(28) { 0 }
        // Backend now returns 28 days. Map each day's activity count to minutes.
        // activity = quiz count or study minutes depending on backend response
        return if (activity.size >= 28) {
            activity.map {
                val raw = it.activity?.toString()?.toDoubleOrNull() ?: 0.0
                raw.toInt().coerceAtLeast(0)
            }
        } else {
            // Pad to 28 days if backend returns fewer
            val values = activity.map {
                (it.activity?.toString()?.toDoubleOrNull() ?: 0.0).toInt().coerceAtLeast(0)
            }
            List(28 - values.size) { 0 } + values
        }
    }

    // ── Subject progress — built from real API subjectAccuracy data ──
    private fun buildSubjects(stats: UserStatsData?): List<SubjectProgress> {
        // Subject metadata: display name → (emoji, primary color, bg color)
        val meta = mapOf(
            "Polity"      to Triple("⚖️",  Color(0xFF1565C0), Color(0xFFE3F2FD)),
            "History"     to Triple("🏛️", Color(0xFFFF8F00), Color(0xFFFFF3E0)),
            "Geography"   to Triple("🗺️", Color(0xFF2E7D32), Color(0xFFE8F5E9)),
            "Economy"     to Triple("📊",  Color(0xFF7B1FA2), Color(0xFFF3E5F5)),
            "Economics"   to Triple("📊",  Color(0xFF7B1FA2), Color(0xFFF3E5F5)),
            "Bihar GK"    to Triple("🏔️", Color(0xFF00838F), Color(0xFFE0F7FA)),
            "Science"     to Triple("🔬",  Color(0xFF1B5E20), Color(0xFFE8F5E9)),
            "Current Affairs" to Triple("📰", Color(0xFF37474F), Color(0xFFECEFF1)),
            "International"   to Triple("🌍", Color(0xFF0277BD), Color(0xFFE1F5FE)),
            "Sports"      to Triple("🏆",  Color(0xFFE65100), Color(0xFFFFF3E0)),
            "Environment" to Triple("🌱",  Color(0xFF33691E), Color(0xFFF1F8E9)),
        )

        val apiSubjects = stats?.subjectAccuracy ?: emptyList()

        // If we have real data, use it; otherwise show default placeholders
        return if (apiSubjects.isNotEmpty()) {
            apiSubjects.map { dto ->
                val (emoji, color, bgColor) = meta[dto.subject]
                    ?: Triple("📚", Color(0xFF546E7A), Color(0xFFECEFF1))
                val accuracy = dto.avgAccuracy?.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
                SubjectProgress(
                    name     = dto.subject,
                    emoji    = emoji,
                    progress = (accuracy / 100f).toFloat(),
                    color    = color,
                    bgColor  = bgColor
                )
            }
        } else {
            // Default placeholder list when no quiz data yet
            listOf(
                SubjectProgress("Polity",    "⚖️",  0f, Color(0xFF1565C0), Color(0xFFE3F2FD)),
                SubjectProgress("History",   "🏛️", 0f, Color(0xFFFF8F00), Color(0xFFFFF3E0)),
                SubjectProgress("Geography", "🗺️", 0f, Color(0xFF2E7D32), Color(0xFFE8F5E9)),
                SubjectProgress("Economy",   "📊",  0f, Color(0xFF7B1FA2), Color(0xFFF3E5F5)),
                SubjectProgress("Bihar GK",  "🏔️", 0f, Color(0xFF00838F), Color(0xFFE0F7FA)),
            )
        }
    }

    // ── Update profile ─────────────────────────────────────────
    fun updateProfile(
        name: String, email: String?, bio: String?,
        district: String?, targetYear: Int?, prepLevel: String?
    ) {
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

    // ── Badge system ───────────────────────────────────────────
    private fun buildBadges(user: UserDto?, stats: UserStatsData?): List<BadgeItem> {
        val streak   = stats?.currentStreak ?: user?.streak ?: 0
        val accuracy = stats?.accuracy ?: user?.accuracy?.toDoubleOrNull() ?: 0.0
        val rank     = user?.rank
        val quizzes  = stats?.quizzesAttempted ?: user?.quizzesAttempted ?: 0
        return listOf(
            BadgeItem("🔥", "7-Day Streak",   streak >= 7,              Color(0xFFFFF8E1)),
            BadgeItem("⚡", "Speed Reader",   accuracy >= 80.0,         Color(0xFFE3F2FD)),
            BadgeItem("🎯", "Sharpshooter",  accuracy >= 90.0,         Color(0xFFE8F5E9)),
            BadgeItem("👑", "Top Ranker",    rank != null && rank <= 10,Color(0xFFF3E5F5)),
            BadgeItem("📚", "Quiz Master",   quizzes >= 50,            Color(0xFFFFF3E0)),
        )
    }

    // ── Weekly streak days ──────────────────────────────────────
    private fun buildWeekDays(stats: UserStatsData?): List<WeekDayUi> {
        val labels   = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val activity = stats?.weeklyActivity ?: emptyList()
        val today    = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
            .let { if (it == java.util.Calendar.SUNDAY) 6 else it - 2 }
        // Take the last 7 days from the 28-day activity list
        val lastSeven = if (activity.size >= 7) activity.takeLast(7) else activity
        return labels.mapIndexed { i, label ->
            val has = lastSeven.getOrNull(i)?.activity?.toString()?.toDoubleOrNull() ?: 0.0
            WeekDayUi(
                label,
                when { i == today -> DayStatus.TODAY; has > 0.0 -> DayStatus.DONE; else -> DayStatus.MISSED }
            )
        }
    }
}