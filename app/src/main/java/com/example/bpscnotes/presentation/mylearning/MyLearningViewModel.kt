package com.example.bpscnotes.presentation.mylearning

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.CourseDto
import com.example.bpscnotes.data.remote.api.CoursesApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyLearningUiState(
    val storeCourses: List<CourseDto>    = emptyList(),
    val enrolledCourses: List<CourseDto> = emptyList(),
    val userCoins: Int                   = 0,
    val subjects: List<String>           = emptyList(),   // dynamic from API
    val isLoading: Boolean               = true,
    val isEnrolling: Boolean             = false,
    val enrollSuccess: String?           = null,          // enrolled course title
    val error: String?                   = null
)

@HiltViewModel
class MyLearningViewModel @Inject constructor(
    private val coursesApi: CoursesApiService,
    private val authApi: AuthApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyLearningUiState())
    val uiState: StateFlow<MyLearningUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Parallel: all courses + enrolled courses + user profile for coins
                val allCoursesJob = async { coursesApi.getCourses(limit = 50).data?.courses ?: emptyList() }
                val userJob       = async {
                    try { authApi.getMe().data?.user?.coins ?: 0 } catch (e: Exception) { 0 }
                }

                val allCourses = allCoursesJob.await()

                // FIX 3: Enrolled courses = courses where enrollment.status == "active"
                // Previously filtering from same getCourses call — but enrollment data
                // is only included when the user is authenticated. This is correct.
                val enrolledCourses = allCourses.filter { it.enrollment?.status=="active" }

                _uiState.update {
                    it.copy(
                        storeCourses    = allCourses,
                        enrolledCourses = enrolledCourses,
                        userCoins       = userJob.await(),
                        isLoading       = false
                    )
                }
            } catch (e: Exception) {
                Log.e("MyLearningVM", e.message ?: "", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load courses") }
            }
        }
    }

    fun retry() = load()

    // ── FIX 2: Enroll a free course via API ─────────────────────
    // Was missing — Enroll Free button just called onDismiss() before.
    fun enroll(courseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEnrolling = true) }
            try {
                coursesApi.enrollCourse(courseId)
                // Re-load so enrolled tab shows the new course
                load()
                _uiState.update { it.copy(isEnrolling = false, enrollSuccess = "Enrolled successfully!") }
            } catch (e: Exception) {
                Log.e("MyLearningVM", "enroll failed: ${e.message}", e)
                _uiState.update { it.copy(isEnrolling = false, error = e.message ?: "Enrollment failed") }
            }
        }
    }

    fun clearMessages() { _uiState.update { it.copy(enrollSuccess = null, error = null) } }
}
