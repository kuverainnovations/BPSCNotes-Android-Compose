package com.example.bpscnotes.presentation.course

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.CourseDetailResponse
import com.example.bpscnotes.data.remote.api.CoursesApiService
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.sortedBy

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val api: CoursesApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseDetailUiState())
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    fun load(courseId: String) {
        if (_uiState.value.course?.id == courseId) return   // already loaded
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.getCourseDetail(courseId)

                val dataObj = response.data ?: run {
                    _uiState.update {
                        it.copy(isLoading = false, error = "No data")
                    }
                    return@launch
                }

                val gson = Gson()
                val detail = gson.fromJson(dataObj, CourseDetailResponse::class.java)

                if (detail?.course == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Course data is missing") }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        course = detail.course,
                        chapters = detail.course.chapters
                            ?.sortedBy { it.sortOrder }
                            ?.map { chapter ->
                                chapter.copy(
                                    lessons = chapter.lessons
                                        ?.sortedBy { it.sortOrder }
                                        ?: emptyList()
                                )
                            } ?: emptyList(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("CourseDetail", e.message ?: "", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load course") }
            }
        }
    }

    fun enroll(courseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEnrolling = true) }
            try {
                api.enrollCourse(courseId)
                _uiState.update { it.copy(isEnrolling = false, enrollSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isEnrolling = false, error = e.message) }
            }
        }
    }

    fun clearMessages() { _uiState.update { it.copy(enrollSuccess = false, error = null) } }

   /* fun completeLesson(courseId: String, lessonId: String, watchSecs: Int = 0) {
        viewModelScope.launch {
            try {
                api.completeCourseLesson(courseId, lessonId, CompleteLessonRequest(watchTimeSecs = watchSecs))
                // Reload course so is_completed reflects on chapter list
                load(courseId)
            } catch (e: Exception) {
                android.util.Log.w("CourseDetailVM", "completeLesson: ${e.message}")
            }
        }
    }

    fun showRatingSheet()    { _uiState.update { it.copy(showRatingSheet = true) } }
    fun dismissRatingSheet() { _uiState.update { it.copy(showRatingSheet = false) } }

    fun submitRating(courseId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingRating = true) }
            try {
                api.submitCourseReview(courseId, mapOf("rating" to rating, "comment" to comment))
                _uiState.update { it.copy(isSubmittingRating = false, showRatingSheet = false, isRatingSubmitted = true) }
                load(courseId) // refresh to show new review
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmittingRating = false, error = "Failed to submit rating: ${e.message}") }
            }
        }
    }*/
}