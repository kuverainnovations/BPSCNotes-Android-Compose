package com.example.bpscnotes.presentation.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.api.*
import com.example.bpscnotes.data.remote.dto.UserDto
import com.example.bpscnotes.domain.model.DayProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val user: UserDto?                        = null,
    val courses: List<CourseDto>              = emptyList(),
    val dailyQuizzes: List<QuizPreviewDto>    = emptyList(),
    val banners: List<BannerDto>              = emptyList(),
    val weeklyActivity: List<DayProgress>     = emptyList(),
    val stats: UserStatsData?                 = null,
    val dailyTargets: List<DailyTargetDto>    = emptyList(),
    val targetSummary: DailyTargetsSummary    = DailyTargetsSummary(),
    val liveClasses: List<LiveClassDto>       = emptyList(),       // ← NEW: real schedule
    val achievements: List<AchievementItem>   = emptyList(),       // ← NEW: derived from user
    val isLoading: Boolean                    = true,
    val isCreatingTarget: Boolean             = false,    // separate flag for create action
    val error: String?                        = null,
    val targetSuccess: String?                = null,      // success message after create/complete
)

data class AchievementItem(
    val emoji: String,
    val label: String,
    val earned: Boolean,
    val colorHex: Long          // stored as Long to avoid needing Compose Color in ViewModel
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authApi: AuthApiService,
    private val coursesApi: CoursesApiService,
    private val quizzesApi: QuizzesApiService,
    private val bannersApi: BannersApiService,
    private val statsApi: UserStatsApiService,
    private val targetsApi: DailyTargetsApiService,
    private val liveClassesApi: LiveClassesApiService,   // ← NEW
    private val tokenStore: TokenStore
) : ViewModel() {


    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { loadDashboard() }


    // ── Full dashboard load ───────────────────────────────────
    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userJob    = async { safeGet("user")    { authApi.getMe().data?.user } }
                val coursesJob = async { safeGet("courses") { coursesApi.getCourses(limit = 6).data?.courses } }
                val quizzesJob = async { safeGet("quizzes") { quizzesApi.getQuizzes(type = "daily", limit = 3).data?.quizzes } }
                val bannersJob = async { safeGet("banners") { bannersApi.getBanners().data?.banners } }
                val statsJob   = async { safeGet("stats")   { statsApi.getStats().data } }
                val targetsJob = async { safeGet("targets") { targetsApi.getDailyTargets().data } }
                val liveClassesJob = async { safeGet("live-classes") { liveClassesApi.getLiveClasses(limit = 3).data?.liveClasses } }

                val user       = userJob.await()
                val courses    = coursesJob.await() ?: emptyList()
                val quizzes    = quizzesJob.await() ?: emptyList()
                val banners    = bannersJob.await() ?: emptyList()
                val stats      = statsJob.await()
                val targetsData = targetsJob.await()
                val liveClasses = liveClassesJob.await() ?: emptyList()


                user?.let { u ->
                    u.mobile?.let { tokenStore.saveUserMobile(it) }
                    tokenStore.saveUserName(u.name)
                }

                val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val weekly = stats?.weeklyActivity?.mapIndexed { i, w ->
                    DayProgress(dayLabels.getOrElse(i) { "D${i+1}" }, w.activity)
                } ?: emptyList()
// Derive achievements from user profile — no extra API call needed
                val achievements = buildAchievements(user, stats)

                _uiState.update {
                    it.copy(
                        user           = user,
                        courses        = courses,
                        dailyQuizzes   = quizzes,
                        banners        = banners,
                        weeklyActivity = weekly,
                        stats          = stats,
                        dailyTargets   = targetsData?.targets ?: emptyList(),
                        targetSummary  = targetsData?.summary ?: DailyTargetsSummary(),
                        isLoading      = false
                    )
                }
            } catch (e: Exception) {
                Log.e("DASHBOARD", "loadDashboard failed: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load dashboard") }
            }
        }
    }

    // ── Create targets (called from CreateTargetSheet) ────────
    /**
     * Creates one or more targets from the user's input.
     * Uses optimistic update: adds items to local list instantly,
     * then refreshes from server to get real IDs.
     */

    /** Achievements derived purely from user data — zero extra network calls */
    private fun buildAchievements(user: UserDto?, stats: UserStatsData?): List<AchievementItem> {
        val streak   = stats?.currentStreak ?: user?.streak ?: 0
        val rank     = user?.rank
        val quizzes  = user?.quizzesAttempted ?: 0
        val accuracy = stats?.accuracy ?: user?.accuracy ?: 0.0
        val studyMin = stats?.totalStudyMinutes ?: user?.totalStudyMinutes ?: 0

        val accuracyValue = stats?.accuracy
            ?: user?.accuracy?.toDoubleOrNull()
            ?: 0.0

        return listOf(
            AchievementItem("🔥", "7 Day\nStreak",   streak >= 7,             0xFFFF6D00L),
            AchievementItem("🏆", "Top 10\nRank",    rank != null && rank <= 10, 0xFFFFB300L),
            AchievementItem("📚", "100\nTopics",     quizzes >= 100,          0xFF1565C0L),
            AchievementItem("⚡", "Speed\nStar",     accuracyValue >= 90.0, 0xFF9B59B6L),
            AchievementItem("🎯", "Perfect\nScore", accuracyValue >= 100.0, 0xFF2ECC71L),
            AchievementItem("⏰", "10h Study",        studyMin >= 600,         0xFF00838FL),
        )
    }
    fun createTargets(titles: List<String>) {
        if (titles.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingTarget = true, error = null) }
            try {
                val response = targetsApi.createTargets(
                    CreateTargetRequest(titles = titles)
                )

                if (!response.success) {
                    _uiState.update {
                        it.copy(
                            isCreatingTarget = false,
                            error = response.message.ifEmpty { "Failed to create targets" }
                        )
                    }
                    return@launch
                }

                // Refresh full target list so IDs, carry-forward etc. are correct
                val freshData = safeGet("targets-refresh") {
                    targetsApi.getDailyTargets().data
                }

                _uiState.update {
                    it.copy(
                        isCreatingTarget = false,
                        dailyTargets     = freshData?.targets ?: it.dailyTargets,
                        targetSummary    = freshData?.summary ?: it.targetSummary,
                        targetSuccess    = "${titles.size} target${if (titles.size > 1) "s" else ""} added! ✅"
                    )
                }
            } catch (e: Exception) {
                Log.e("DASHBOARD", "createTargets failed: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isCreatingTarget = false,
                        error = e.message ?: "Failed to create targets"
                    )
                }
            }
        }
    }

    // ── Toggle target complete ────────────────────────────────
    /**
     * Optimistic update: flip isCompleted in local state immediately
     * so the UI responds instantly. Then call API and refresh.
     */


    private val toggleInProgress = mutableSetOf<String>()


    fun toggleTargetComplete(targetId: String) {
        if (toggleInProgress.contains(targetId)) return

        toggleInProgress.add(targetId)

        viewModelScope.launch {

            // ✅ ONLY optimistic update (this is enough)
            _uiState.update { state ->
                val updated = state.dailyTargets.map {
                    if (it.id == targetId) it.copy(isCompleted = !it.isCompleted)
                    else it
                }

                val completed = updated.count { it.isCompleted }

                state.copy(
                    dailyTargets = updated,
                    targetSummary = state.targetSummary.copy(
                        completed = completed,
                        pending = updated.size - completed,
                        completionPct = if (updated.isNotEmpty()) (completed * 100) / updated.size else 0
                    )
                )
            }

            try {
                // ✅ ONLY call API (no override)
                val response = targetsApi.toggleComplete(targetId)

                // optional: show coins
                if (response.data?.coinsEarned ?: 0 > 0) {
                    _uiState.update {
                        it.copy(targetSuccess = "🎉 +${response.data?.coinsEarned} coins earned!")
                    }
                }

            } catch (e: Exception) {

                // ❌ revert if failed
                _uiState.update { state ->
                    val reverted = state.dailyTargets.map {
                        if (it.id == targetId) it.copy(isCompleted = !it.isCompleted)
                        else it
                    }

                    val completed = reverted.count { it.isCompleted }

                    state.copy(
                        dailyTargets = reverted,
                        targetSummary = state.targetSummary.copy(
                            completed = completed,
                            pending = reverted.size - completed
                        )
                    )
                }

            } finally {
                toggleInProgress.remove(targetId)
            }
        }
    }

    // ── Delete target ─────────────────────────────────────────
    fun deleteTarget(targetId: String) {
        // Optimistic remove
        _uiState.update { state ->
            state.copy(dailyTargets = state.dailyTargets.filter { it.id != targetId })
        }

        viewModelScope.launch {
            try {
                targetsApi.deleteTarget(targetId)
            } catch (e: Exception) {
                Log.e("DASHBOARD", "deleteTarget failed: ${e.message}", e)
                // Reload to restore correct state
                val freshData = safeGet("targets-restore") { targetsApi.getDailyTargets().data }
                _uiState.update { state ->
                    state.copy(
                        dailyTargets  = freshData?.targets ?: state.dailyTargets,
                        targetSummary = freshData?.summary ?: state.targetSummary,
                        error         = "Failed to delete target"
                    )
                }
            }
        }
    }

    fun refresh() = loadDashboard()

    fun clearError()         { _uiState.update { it.copy(error = null) } }
    fun clearTargetSuccess() { _uiState.update { it.copy(targetSuccess = null) } }

    fun getGreeting(): String {
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when { h < 12 -> "Good Morning"; h < 17 -> "Good Afternoon"; else -> "Good Evening" }
    }

    private suspend fun <T> safeGet(section: String, block: suspend () -> T?): T? {
        return try {
            block()
        } catch (e: Exception) {
            Log.e("DASHBOARD_API", "[$section] ${e.message}", e)
            null
        }
    }
}
