package com.example.bpscnotes.presentation.course

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.Chapter
import com.example.bpscnotes.data.remote.api.CompleteLessonRequest
import com.example.bpscnotes.data.remote.api.CourseDetailResponse
import com.example.bpscnotes.data.remote.api.CourseDto
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import com.example.bpscnotes.data.remote.api.CoursesApiService
import com.example.bpscnotes.data.remote.api.SubmitReviewRequest
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CourseDetailUiState(
    val course: CourseDto? = null,
    val chapters: List<Chapter> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val enrollSuccess: Boolean = false,
    val isEnrolling: Boolean = false,
    // Payment required
    val purchaseRequired: Boolean = false,
    val purchasePrice: Int = 0,
    val purchaseOrderId: String? = null,
    val purchaseKeyId: String? = null,
    // Rating
    val showRatingSheet: Boolean = false,
    val isSubmittingRating: Boolean = false,
    val isRatingSubmitted: Boolean = false,
    val ratingError: String? = null
)

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val api: CoursesApiService
,
    private val bus: RefreshEventBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseDetailUiState())
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    private var activeCourseId: String = ""

    init {
        viewModelScope.launch {
            bus.events.collect { event ->
                when (event) {
                    is RefreshEvent.CourseProgressChanged ->
                        if (event.courseId == activeCourseId) load(activeCourseId)
                    is RefreshEvent.LessonCompleted ->
                        if (activeCourseId.isNotEmpty()) load(activeCourseId)
                    else -> {}
                }
            }
        }
    }

    companion object {
        /**
         * Survives ViewModel recreation (back-stack) within the same process.
         * Prevents the "Rate this Course" banner from reappearing after
         * the user navigates away and returns.
         */
        private val reviewedCourseIds = mutableSetOf<String>()
    }

    // ── Load ──────────────────────────────────────────────────

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
                        course            = detail.course,
                        chapters          = sortedChapters,
                        isLoading         = false,
                        // Restore submitted state if user already reviewed this course
                        isRatingSubmitted = reviewedCourseIds.contains(courseId)
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
            _uiState.update { it.copy(isEnrolling = true, purchaseRequired = false) }
            try {
                api.enrollCourse(courseId)
                _uiState.update { it.copy(isEnrolling = false, enrollSuccess = true) }
                load(courseId)
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 402) {
                    // Parse payment required response
                    try {
                        val body = e.response()?.errorBody()?.string() ?: ""
                        val json = org.json.JSONObject(body)
                        val data = json.optJSONObject("data") ?: json
                        val price   = data.optInt("price", 0)
                        val orderId = data.optString("razorpayOrderId").takeIf { it.isNotBlank() }
                        val keyId   = data.optString("razorpayKeyId").takeIf { it.isNotBlank() }
                        _uiState.update { it.copy(
                            isEnrolling      = false,
                            purchaseRequired = true,
                            purchasePrice    = price,
                            purchaseOrderId  = orderId,
                            purchaseKeyId    = keyId
                        )}
                    } catch (_: Exception) {
                        _uiState.update { it.copy(isEnrolling = false, error = "Purchase required") }
                    }
                } else {
                    Log.e("CourseDetailVM", "enroll: ${e.message}", e)
                    _uiState.update { it.copy(isEnrolling = false, error = e.message) }
                }
            } catch (e: Exception) {
                Log.e("CourseDetailVM", "enroll: ${e.message}", e)
                _uiState.update { it.copy(isEnrolling = false, error = e.message) }
            }
        }
    }

    // ── Complete lesson ───────────────────────────────────────

    fun completeLesson(courseId: String, lessonId: String, watchSecs: Int = 0) {
        viewModelScope.launch {
            try {
                api.completeCourseLesson(courseId, lessonId, CompleteLessonRequest(watchSecs))
                load(courseId)
            } catch (e: Exception) {
                Log.w("CourseDetailVM", "completeLesson failed: ${e.message}")
            }
        }
    }

    // ── Rating sheet ──────────────────────────────────────────

    fun showRatingSheet() {
        _uiState.update { it.copy(showRatingSheet = true, ratingError = null) }
    }

    fun dismissRatingSheet() {
        _uiState.update { it.copy(showRatingSheet = false, ratingError = null) }
    }

    fun submitRating(courseId: String, rating: Int, comment: String) {
        if (rating < 1 || rating > 5) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingRating = true, ratingError = null) }
            try {
                api.submitCourseReview(courseId, SubmitReviewRequest(rating, comment.trim()))

                // Mark as reviewed in the companion set so it persists on back-navigation
                reviewedCourseIds.add(courseId)

                _uiState.update {
                    it.copy(
                        isSubmittingRating = false,
                        showRatingSheet    = false,
                        isRatingSubmitted  = true
                    )
                }
                // Reload so the new review appears in the list
                load(courseId)
            } catch (e: Exception) {
                Log.e("CourseDetailVM", "submitRating: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isSubmittingRating = false,
                        ratingError        = "Could not submit review. Please try again."
                    )
                }
            }
        }
    }

    // ── Clear messages ─────────────────────────────────────────

    fun clearMessages() {
        _uiState.update { it.copy(enrollSuccess = false, error = null) }
    }
}