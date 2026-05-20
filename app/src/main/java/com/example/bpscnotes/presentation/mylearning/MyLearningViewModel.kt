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
    val storeCourses:    List<CourseDto> = emptyList(),  // all courses NOT enrolled (for Store tab)
    val enrolledCourses: List<CourseDto> = emptyList(),  // courses user has enrolled in
    val savedCourses:    List<CourseDto> = emptyList(),  // courses user has saved/wishlisted
    val savedCourseIds:  Set<String>     = emptySet(),   // for quick O(1) lookup in UI
    val userCoins:       Int             = 0,
    val subjects:        List<String>    = emptyList(),
    val isLoading:       Boolean         = true,
    val isEnrolling:     Boolean         = false,
    val enrollSuccess:   String?         = null,
    val saveToast:       String?         = null,
    val error:           String?         = null
)

@HiltViewModel
class MyLearningViewModel @Inject constructor(
    private val coursesApi: CoursesApiService,
    private val authApi:    AuthApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyLearningUiState())
    val uiState: StateFlow<MyLearningUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val allCoursesJob  = async { coursesApi.getCourses(limit = 100).data?.courses ?: emptyList() }
                val savedCoursesJob= async {
                    try { coursesApi.getSavedCourses().data?.courses ?: emptyList() }
                    catch (e: Exception) { emptyList() }
                }
                val userJob        = async {
                    try { authApi.getMe().data?.user?.coins ?: 0 } catch (_: Exception) { 0 }
                }

                val allCourses     = allCoursesJob.await()
                val savedCourses   = savedCoursesJob.await()
                val savedIds       = savedCourses.map { it.id }.toSet()

                // Enrolled = active enrollment
                val enrolledCourses = allCourses.filter { it.enrollment?.status == "active" }
                val enrolledIds     = enrolledCourses.map { it.id }.toSet()

                // FIX 1: Store tab = courses NOT enrolled yet (no Enroll button on already-enrolled courses)
                val storeCourses    = allCourses.filter { it.id !in enrolledIds }

                _uiState.update {
                    it.copy(
                        storeCourses    = storeCourses,
                        enrolledCourses = enrolledCourses,
                        savedCourses    = savedCourses,
                        savedCourseIds  = savedIds,
                        userCoins       = userJob.await(),
                        isLoading       = false
                    )
                }
            } catch (e: Exception) {
                Log.e("MyLearningVM", e.message ?: "", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load") }
            }
        }
    }

    fun retry() = load()

    // ── Enroll ─────────────────────────────────────────────────
    fun enroll(courseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEnrolling = true) }
            try {
                coursesApi.enrollCourse(courseId)
                load() // refresh so enrolled tab shows the new course & store removes it
                _uiState.update { it.copy(isEnrolling = false, enrollSuccess = "Enrolled! Go to My Courses tab") }
            } catch (e: Exception) {
                Log.e("MyLearningVM", "enroll: ${e.message}", e)
                _uiState.update { it.copy(isEnrolling = false, error = e.message ?: "Enrollment failed") }
            }
        }
    }

    // ── Save / Unsave (Wishlist) ────────────────────────────────
    fun toggleSave(courseId: String) {
        val isSaved = _uiState.value.savedCourseIds.contains(courseId)

        // Optimistic update
        _uiState.update { state ->
            val newIds = if (isSaved) state.savedCourseIds - courseId
            else         state.savedCourseIds + courseId
            state.copy(savedCourseIds = newIds)
        }

        viewModelScope.launch {
            try {
                if (isSaved) {
                    coursesApi.unsaveCourse(courseId)
                    _uiState.update { it.copy(saveToast = "Removed from saved") }
                } else {
                    coursesApi.saveCourse(courseId)
                    _uiState.update { it.copy(saveToast = "Course saved! View in My Courses → Saved tab") }
                }
                // Re-fetch saved list silently so savedCourses list is accurate
                val updated = try { coursesApi.getSavedCourses().data?.courses ?: emptyList() } catch (_: Exception) { emptyList() }
                _uiState.update { it.copy(savedCourses = updated, savedCourseIds = updated.map { c -> c.id }.toSet()) }
            } catch (e: Exception) {
                // Revert optimistic update on failure
                _uiState.update { state ->
                    val revertIds = if (isSaved) state.savedCourseIds + courseId
                    else          state.savedCourseIds - courseId
                    state.copy(savedCourseIds = revertIds, error = e.message ?: "Save failed")
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(enrollSuccess = null, saveToast = null, error = null) }
    }
}