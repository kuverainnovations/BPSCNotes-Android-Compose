package com.example.bpscnotes.presentation.course

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.CompleteLessonRequest
import com.example.bpscnotes.data.remote.api.CoursesApiService
import com.example.bpscnotes.data.remote.api.Lesson
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class LessonViewerUiState(
    val lesson:    Lesson? = null,
    val isLoading: Boolean = true,
    val isMarking: Boolean = false,
    val error:     String? = null,
    val watchSecs: Int     = 0,
)

@HiltViewModel
class LessonViewerViewModel @Inject constructor(
    private val api: CoursesApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonViewerUiState())
    val uiState: StateFlow<LessonViewerUiState> = _uiState.asStateFlow()

    private var courseId   = ""
    private var lessonId   = ""
    private var timerJob: Job? = null

    companion object { private const val TAG = "LessonViewerVM" }

    // ── Load lesson detail ────────────────────────────────────
    fun load(courseId: String, lessonId: String) {
        this.courseId = courseId
        this.lessonId = lessonId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val res    = api.getLessonDetail(courseId, lessonId)
                val lesson = res.data?.lesson
                    ?: throw Exception("Lesson not found")
                _uiState.update { it.copy(lesson = lesson, isLoading = false) }
                if (!lesson.is_locked) startTimer()
            } catch (e: Exception) {
                Log.e(TAG, "load: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load lesson") }
            }
        }
    }

    // ── Watch time timer ──────────────────────────────────────
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _uiState.update { it.copy(watchSecs = it.watchSecs + 1) }
            }
        }
    }

    // ── Mark complete ─────────────────────────────────────────
    fun markComplete() {
        if (_uiState.value.isMarking) return
        val lesson = _uiState.value.lesson ?: return

        timerJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isMarking = true) }
            try {
                val watched = _uiState.value.watchSecs
                api.completeCourseLesson(
                    courseId, lessonId,
                    CompleteLessonRequest(watchTimeSecs = watched)
                )
                _uiState.update {
                    it.copy(
                        isMarking = false,
                        lesson    = lesson.copy(is_completed = true)
                    )
                }
                Log.d(TAG, "Lesson $lessonId marked complete (${watched}s watched)")
            } catch (e: Exception) {
                Log.e(TAG, "markComplete: ${e.message}", e)
                _uiState.update { it.copy(isMarking = false, error = "Failed to mark complete") }
                startTimer() // resume timer so user can retry
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
