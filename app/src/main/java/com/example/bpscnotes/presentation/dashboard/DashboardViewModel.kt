package com.example.bpscnotes.presentation.dashboard

import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.BannersApiService
import com.example.bpscnotes.data.remote.api.CoursesApiService
import com.example.bpscnotes.data.remote.api.DailyTargetsApiService
import com.example.bpscnotes.data.remote.api.LiveClassesApiService
import com.example.bpscnotes.data.remote.api.QuizzesApiService
import com.example.bpscnotes.data.remote.api.UserStatsApiService

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import com.example.bpscnotes.core.config.AppConfigRepository
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
    val liveClasses: List<LiveClassDto>       = emptyList(),
    val achievements: List<AchievementItem>   = emptyList(),
    // Schedule interaction state
    val registeredClassIds: Set<String>       = emptySet(),        // classes user has registered for
    val scheduleToast:      String?           = null,
    val isLoading: Boolean                    = true,
    val isCreatingTarget: Boolean             = false,    // separate flag for create action
    val error: String?                        = null,
    val targetSuccess: String?                = null,      // success message after create/complete
)

data class AchievementItem(
    val emoji: String,
    val label: String,
    val earned: Boolean,
    val progress: Int,
    val max: Int,
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
    ,
    private val bus: RefreshEventBus,
    private val appConfig: AppConfigRepository
) : ViewModel() {


    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
        viewModelScope.launch {
            bus.events.collect { event ->
                when (event) {
                    is RefreshEvent.QuizCompleted,
                    is RefreshEvent.LessonCompleted,
                    is RefreshEvent.CoinsChanged,
                    is RefreshEvent.TargetUpdated -> loadDashboard()
                    else -> {}
                }
            }
        }
    }


    // ── Full dashboard load ───────────────────────────────────
    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userJob    = async { safeGet("user")    { authApi.getMe().data?.user } }
                val userPrimaryExam = tokenStore.getUserPrimaryExam()
                val courseLimit = appConfig.config.value.dailyQuizLimit.coerceAtLeast(5)
                val coursesJob = async { safeGet("courses") {
                    val params = if (!userPrimaryExam.isNullOrBlank())
                        coursesApi.getCourses(limit = courseLimit, exam = userPrimaryExam).data?.courses
                    else coursesApi.getCourses(limit = courseLimit).data?.courses
                    params
                } }
                val quizzesJob = async { safeGet("quizzes") { quizzesApi.getQuizzes(type = "daily", limit = 3).data?.quizzes } }
                val bannersJob = async { safeGet("banners") { bannersApi.getBanners().data?.banners } }
                val statsJob   = async { safeGet("stats")   { statsApi.getStats().data } }
                val targetsJob = async { safeGet("targets") { targetsApi.getDailyTargets().data } }
                val liveClassesJob = async { safeGet("live-classes") {
                    liveClassesApi.getLiveClasses(limit = 3).data?.liveClasses }

                }


                val user       = userJob.await()
                val courses    = coursesJob.await() ?: emptyList()
                val quizzes    = quizzesJob.await() ?: emptyList()
                val banners    = bannersJob.await() ?: emptyList()
                val stats      = statsJob.await()
                val targetsData = targetsJob.await()
                val liveClasses = liveClassesJob.await() ?: emptyList()

                // FIX: Seed registeredClassIds from the is_registered flag the GET API already returns.
                // No extra API call needed — backend joins live_class_registrations per user in getLiveClasses().
                // This persists across app restarts because it reads fresh from server every load().
                val serverRegisteredIds = liveClasses
                    .filter { it.isRegistered }
                    .map { it.id }
                    .toSet()


                user?.let { u ->
                    u.mobile?.let { tokenStore.saveUserMobile(it) }
                    tokenStore.saveUserName(u.name)
                    // (notification badge count handled by MainShell reading prefs written by FCM service)
                }

                // Build last-7-days array — always 7 items so the chart always renders.
                // Backend now returns 7 rows (with zeros) via generate_series.
                // As a safety net, we also generate client-side if the API returns fewer.
                val cal       = java.util.Calendar.getInstance()
                val dfmt      = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val dayAbbr   = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")
                val apiMap    = stats?.weeklyActivity?.associate { it.date.take(10) to it.activity } ?: emptyMap()
                val weekly    = (6 downTo 0).map { daysAgo ->
                    cal.timeInMillis = System.currentTimeMillis() - daysAgo * 86_400_000L
                    val dateStr  = dfmt.format(cal.time)
                    val dayLabel = dayAbbr[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
                    DayProgress(dayLabel, apiMap[dateStr] ?: 0)
                }


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
                        liveClasses=liveClasses,
                        // Merge server state with any optimistic local additions
                        registeredClassIds = serverRegisteredIds + it.registeredClassIds,
                        achievements=achievements,
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
    private fun buildAchievements(
        user: UserDto?,
        stats: UserStatsData?
    ): List<AchievementItem> {

        val streak   = stats?.currentStreak ?: user?.streak ?: 0
        // rank: Int? (null = not yet ranked)
        val rank     = stats?.rank ?: user?.rank ?: Int.MAX_VALUE
        val quizzes  = stats?.quizzesAttempted ?: user?.quizzesAttempted ?: 0
        // totalStudyMinutes: stats is Int? (nullable) so fallback to user works correctly
        val studyMin = stats?.totalStudyMinutes ?: user?.totalStudyMinutes ?: 0

        // accuracy: stats returns Double? (nullable), user returns String?
        val accuracyValue = stats?.accuracy
            ?: user?.accuracy?.toDoubleOrNull()
            ?: 0.0

        return listOf(

            AchievementItem(
                emoji = "🔥",
                label = "7 Day\nStreak",
                earned = streak >= 7,
                progress = streak,
                max = 7,
                colorHex = 0xFFFF6D00L
            ),

            AchievementItem(
                emoji = "🏆",
                label = "Top 10\nRank",
                earned = rank <= 10,
                progress = if (rank <= 10) 10 else 0,
                max = 10,
                colorHex = 0xFFFFB300L
            ),

            AchievementItem(
                emoji = "📚",
                label = "100\nTopics",
                earned = quizzes >= 100,
                progress = quizzes,
                max = 100,
                colorHex = 0xFF1565C0L
            ),

            AchievementItem(
                emoji = "⚡",
                label = "Speed\nStar",
                earned = accuracyValue >= 90,
                progress = accuracyValue.toInt(),
                max = 90,
                colorHex = 0xFF9B59B6L
            ),

            AchievementItem(
                emoji = "🎯",
                label = "Perfect\nScore",
                earned = accuracyValue >= 100,
                progress = accuracyValue.toInt(),
                max = 100,
                colorHex = 0xFF2ECC71L
            ),

            AchievementItem(
                emoji = "⏰",
                label = "10h Study",
                earned = studyMin >= 600,
                progress = studyMin,
                max = 600,
                colorHex = 0xFF00838FL
            )
        )
    }
    fun createTargets(titles: List<String>, estimatedMinutes: Int = 30, subject: String = "General") {
        if (titles.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingTarget = true, error = null) }
            try {
                val response = targetsApi.createTargets(
                    CreateTargetRequest(titles = titles, estimatedMinutes = estimatedMinutes, subject = subject)
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
                bus.emit(RefreshEvent.TargetUpdated)
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
                val response = targetsApi.deleteTarget(targetId)
                // If completed target deleted, backend debits coins — tell user
                if (response.data?.coinsDebited == true) {
                    _uiState.update { it.copy(targetSuccess = "🗑️ Target deleted — coins reversed") }
                }
                // Reload so coins balance in header reflects the debit
                loadDashboard()
            } catch (e: Exception) {
                Log.e("DASHBOARD", "deleteTarget failed: ${e.message}", e)
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

    // ── Register for a live class ────────────────────────────
    /**
     * Three behaviours depending on class status:
     *  - "live"      → no registration needed; caller opens meetingLink directly
     *  - "scheduled" → call POST /users/live-classes/:id/register
     *  - "ended"     → show toast; no action
     */
    fun registerLiveClass(classId: String) {
        viewModelScope.launch {
            // Optimistic: add to registered set immediately
            _uiState.update { it.copy(registeredClassIds = it.registeredClassIds + classId) }
            try {
                liveClassesApi.register(classId)
                _uiState.update {
                    it.copy(scheduleToast = "✅ Registered! You'll get a notification when it starts.")
                }
            } catch (e: Exception) {
                // Revert if it was already registered (409 Conflict) → still mark as registered
                val msg = e.message ?: ""
                if (msg.contains("409") || msg.contains("already", ignoreCase = true)) {
                    _uiState.update { it.copy(scheduleToast = "Already registered for this class") }
                } else {
                    _uiState.update {
                        it.copy(
                            registeredClassIds = it.registeredClassIds - classId,
                            scheduleToast = "Registration failed: $msg"
                        )
                    }
                }
                Log.e("DASHBOARD", "registerLiveClass: $msg", e)
            }
        }
    }

    fun clearScheduleToast() { _uiState.update { it.copy(scheduleToast = null) } }

    /** Called directly from composable for instant toast without an API call */
    fun setScheduleToast(msg: String) { _uiState.update { it.copy(scheduleToast = msg) } }

    fun refresh() = loadDashboard()

    /**
     * Lightweight refresh — only re-fetches daily targets.
     * Called on Lifecycle.State.RESUMED so Dashboard stays in sync
     * after returning from DailyTargetsScreen without a full reload.
     */
    fun refreshTargets() {
        viewModelScope.launch {
            try {
                val freshData = safeGet("targets-resume") { targetsApi.getDailyTargets().data }
                if (freshData != null) {
                    _uiState.update {
                        it.copy(
                            dailyTargets  = freshData.targets,
                            targetSummary = freshData.summary
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("DASHBOARD", "refreshTargets failed: \${e.message}", e)
            }
        }
    }

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