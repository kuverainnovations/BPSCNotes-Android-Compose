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
    val stats: UserStatsData?                 = null,          // ← rank / accuracy / study mins
    val dailyTargets: List<DailyTargetDto>    = emptyList(),   // ← from GET /users/daily-targets
    val isLoading: Boolean                    = true,
    val error: String?                        = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authApi: AuthApiService,
    private val coursesApi: CoursesApiService,
    private val quizzesApi: QuizzesApiService,
    private val bannersApi: BannersApiService,
    private val statsApi: UserStatsApiService,
    private val targetsApi: DailyTargetsApiService,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // All six calls in parallel — fastest possible load
                val userJob    = async { safeGet("user")    { authApi.getMe().data?.user } }
                val coursesJob = async { safeGet("courses") { coursesApi.getCourses(limit = 6).data?.courses } }
                val quizzesJob = async { safeGet("quizzes") { quizzesApi.getQuizzes(type = "daily", limit = 3).data?.quizzes } }
                val bannersJob = async { safeGet("banners") { bannersApi.getBanners().data?.banners } }
                val statsJob   = async { safeGet("stats")   { statsApi.getStats().data } }
                val targetsJob = async { safeGet("targets") { targetsApi.getDailyTargets().data?.targets } }

                val user    = userJob.await()
                val courses = coursesJob.await() ?: emptyList()
                val quizzes = quizzesJob.await() ?: emptyList()
                val banners = bannersJob.await() ?: emptyList()
                val stats   = statsJob.await()
                val targets = targetsJob.await() ?: emptyList()

                // Cache user info in TokenStore for splash / drawer
                user?.let { u ->
                    u.mobile?.let { tokenStore.saveUserMobile(it) }
                    tokenStore.saveUserName(u.name)
                }

                // Map WeeklyActivityDto → DayProgress for the chart
                val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val weekly = stats?.weeklyActivity?.mapIndexed { i, w ->
                    DayProgress(dayLabels.getOrElse(i) { "D${i+1}" }, w.activity)
                } ?: emptyList()

                _uiState.update {
                    it.copy(
                        user           = user,
                        courses        = courses,
                        dailyQuizzes   = quizzes,
                        banners        = banners,
                        weeklyActivity = weekly,
                        stats          = stats,     // ← stored so UI can read rank / accuracy / studyMins
                        dailyTargets   = targets,   // ← no more placeholderTargets
                        isLoading      = false
                    )
                }
            } catch (e: Exception) {
                Log.e("DASHBOARD", "loadDashboard failed: ${e.message}", e)
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load dashboard")
                }
            }
        }
    }

    fun refresh() = loadDashboard()

    fun clearError() { _uiState.update { it.copy(error = null) } }

    fun getGreeting(): String {
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            h < 12 -> "Good Morning"
            h < 17 -> "Good Afternoon"
            else   -> "Good Evening"
        }
    }

    /**
     * Wraps each API call so a failure in one section
     * does NOT crash the whole dashboard.
     * Logs the section name so errors are easy to trace.
     */
    private suspend fun <T> safeGet(section: String, block: suspend () -> T?): T? {
        return try {
            block()
        } catch (e: Exception) {
            Log.e("DASHBOARD_API", "[$section] ${e.message}", e)
            null
        }
    }
}