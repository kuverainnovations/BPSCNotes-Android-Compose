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
    val user: UserDto? = null,
    val courses: List<CourseDto> = emptyList(),
    val dailyQuizzes: List<QuizPreviewDto> = emptyList(),
    val banners: List<BannerDto> = emptyList(),
    val weeklyActivity: List<DayProgress> = emptyList(),
    val stats: UserStatsData? = null,   // ← added: full stats for header strip
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authApi: AuthApiService,
    private val coursesApi: CoursesApiService,
    private val quizzesApi: QuizzesApiService,
    private val bannersApi: BannersApiService,
    private val statsApi: UserStatsApiService,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // All parallel — fastest possible load
                val userJob = async { safeGet { authApi.getMe().data?.user } }
                val coursesJob =
                    async { safeGet { coursesApi.getCourses(limit = 6).data?.courses } }
                val quizzesJob = async {
                    safeGet {
                        quizzesApi.getQuizzes(
                            type = "daily",
                            limit = 3
                        ).data?.quizzes
                    }
                }
                val bannersJob = async { safeGet { bannersApi.getBanners().data?.banners } }
                val statsJob = async { safeGet { statsApi.getStats().data } }

                val user = userJob.await()
                val courses = coursesJob.await() ?: emptyList()
                val quizzes = quizzesJob.await() ?: emptyList()
                val banners = bannersJob.await() ?: emptyList()
                val stats = statsJob.await()

                Log.d(
                    "DASHBOARD",
                    "user=${user?.name}, courses=${courses.size}, quizzes=${quizzes.size}, banners=${banners.size}"
                )

                user?.let { u ->
                    u.mobile?.let { tokenStore.saveUserMobile(it) }
                    tokenStore.saveUserName(u.name)
                }

                val weekly = stats?.weeklyActivity?.mapIndexed { i, w ->
                    val label = when (i) {
                        0 -> "Mon"; 1 -> "Tue"; 2 -> "Wed"; 3 -> "Thu"; 4 -> "Fri"; 5 -> "Sat"; else -> "Sun"
                    }
                    DayProgress(label, w.activity)
                } ?: emptyList()

                _uiState.update {
                    it.copy(
                        user = user,
                        courses = courses,
                        dailyQuizzes = quizzes,
                        banners = banners,
                        weeklyActivity = weekly,
                        stats = stats,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load dashboard")
                }
            }
        }
    }

    fun refresh() = loadDashboard()
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun getGreeting(): String {
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            h < 12 -> "Good Morning"; h < 17 -> "Good Afternoon"; else -> "Good Evening"
        }
    }

    private suspend fun <T> safeGet(block: suspend () -> T?): T? = try {
        block()
    } catch (e: Exception) {
        Log.e("DASHBOARD_API", e.message ?: "Unknown error", e)
        null
    }
}