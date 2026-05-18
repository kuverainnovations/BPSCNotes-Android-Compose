package com.example.bpscnotes.presentation.course

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.Chapter
import com.example.bpscnotes.data.remote.api.CompleteLessonRequest
import com.example.bpscnotes.data.remote.api.CourseDetailResponse
import com.example.bpscnotes.data.remote.api.CoursesApiService
import com.example.bpscnotes.data.remote.api.CourseDto
import com.example.bpscnotes.data.remote.api.SubmitReviewRequest
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ────────────────────────────────────────────────────────────
// UI STATE
// ────────────────────────────────────────────────────────────



// ────────────────────────────────────────────────────────────
// VIEW MODEL
// ────────────────────────────────────────────────────────────

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val api: CoursesApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseDetailUiState())
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    // Cached courseId so we can reload after lesson completion
    private var activeCourseId: String = ""

    // ── Load course detail ────────────────────────────────────
    fun load(courseId: String) {
        activeCourseId = courseId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.getCourseDetail(courseId)
                val dataObj  = response.data ?: run {
                    _uiState.update { it.copy(isLoading = false, error = "No data returned") }
                    return@launch
                }
                val detail = Gson().fromJson(dataObj, CourseDetailResponse::class.java)
                if (detail?.course == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Course data missing") }
                    return@launch
                }
                val sortedChapters = (detail.course.chapters ?: emptyList())
                    .sortedBy { it.sortOrder }
                    .map { ch -> ch.copy(lessons = ch.lessons?.sortedBy { it.sort_order } ?: emptyList()) }

                _uiState.update {
                    it.copy(
                        course   = detail.course,
                        chapters = sortedChapters,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("CourseDetailVM", "load: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load course") }
            }
        }
    }

    // ── Enroll ────────────────────────────────────────────────
    fun enroll(courseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEnrolling = true) }
            try {
                api.enrollCourse(courseId)
                _uiState.update { it.copy(isEnrolling = false, enrollSuccess = true) }
                // Reload so enrollment object appears in course data
                load(courseId)
            } catch (e: Exception) {
                Log.e("CourseDetailVM", "enroll: ${e.message}", e)
                _uiState.update { it.copy(isEnrolling = false, error = e.message) }
            }
        }
    }

    // ── Mark lesson complete ──────────────────────────────────
    // Called by LessonViewerViewModel after user taps "Mark as Complete".
    // Reloads the course so is_completed checkmarks update on chapter list.
    fun completeLesson(courseId: String, lessonId: String, watchSecs: Int = 0) {
        viewModelScope.launch {
            try {
                api.completeCourseLesson(courseId, lessonId, CompleteLessonRequest(watchSecs))
                // Reload course so chapter list reflects new is_completed
                load(courseId)
            } catch (e: Exception) {
                Log.w("CourseDetailVM", "completeLesson failed: ${e.message}")
            }
        }
    }

    // ── Rating sheet ──────────────────────────────────────────
    /*fun showRatingSheet()    { _uiState.update { it.copy(showRatingSheet = true) } }
    fun dismissRatingSheet() { _uiState.update { it.copy(showRatingSheet = false) } }

    fun submitRating(courseId: String, rating: Int, comment: String) {
        if (rating < 1 || rating > 5) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingRating = true) }
            try {
                api.submitCourseReview(courseId, SubmitReviewRequest(rating, comment))
                _uiState.update {
                    it.copy(
                        isSubmittingRating = false,
                        showRatingSheet    = false,
                        isRatingSubmitted  = true
                    )
                }
                // Reload to show new review in the list
                load(courseId)
            } catch (e: Exception) {
                Log.e("CourseDetailVM", "submitRating: ${e.message}", e)
                _uiState.update {
                    it.copy(isSubmittingRating = false, error = "Rating failed: ${e.message}")
                }
            }
        }
    }*/

    // ── Clear messages ─────────────────────────────────────────
    fun clearMessages() {
        _uiState.update { it.copy(enrollSuccess = false, error = null) }
    }
}
