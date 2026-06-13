package com.example.bpscnotes.presentation.mylearning

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.Chapter
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

// ════════════════════════════════════════════════════════════
// CourseSyllabusViewModel
//
// Lightweight loader used by the course-discovery bottom sheet
// (CourseDetailSheet) to show the REAL chapter/lesson curriculum
// — with lock icons and free-preview badges — before the user
// enrolls or purchases. Read-only; no enrollment logic here.
// ════════════════════════════════════════════════════════════

data class CourseSyllabusUiState(
    val isLoading: Boolean = true,
    val chapters: List<Chapter> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class CourseSyllabusViewModel @Inject constructor(
    private val coursesApi: CoursesApiService,
) : ViewModel() {

    private val _state = MutableStateFlow(CourseSyllabusUiState())
    val state: StateFlow<CourseSyllabusUiState> = _state.asStateFlow()

    private var loadedCourseId: String? = null

    fun load(courseId: String) {
        if (loadedCourseId == courseId) return  // avoid re-fetching on recompose
        loadedCourseId = courseId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val res = coursesApi.getCourseDetail(courseId)
                val detail = res.data?.let { Gson().fromJson(it, CourseDetailResponse::class.java) }
                val sorted = (detail?.course?.chapters ?: emptyList())
                    .sortedBy { it.sortOrder }
                    .map { ch -> ch.copy(lessons = ch.lessons?.sortedBy { it.sort_order } ?: emptyList()) }
                _state.update { it.copy(isLoading = false, chapters = sorted) }
            } catch (e: Exception) {
                Log.e("CourseSyllabusVM", "load: ${e.message}")
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load curriculum") }
            }
        }
    }
}