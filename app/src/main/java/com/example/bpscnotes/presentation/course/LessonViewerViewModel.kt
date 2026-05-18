package com.example.bpscnotes.presentation.course

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.CoursesApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────
// LESSON VIEWER VIEW MODEL
//
// State machine:
//   IDLE → load() → LOADING → lesson fetched → READY
//   READY → markComplete() → MARKING → backend called → COMPLETED
//
// Watch time:
//   A coroutine ticks every second while the screen is active.
//   On markComplete(), the accumulated watch_time_secs is sent
//   to the backend so the studied_minutes field updates correctly.
// ─────────────────────────────────────────────────────────────

data class LessonViewerUiState(
    val lesson:     LessonDto? = null,
    val isLoading:  Boolean    = true,
    val isMarking:  Boolean    = false,
    val error:      String?    = null,
    val watchSecs:  Int        = 0,
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

    // ── 1. Load lesson detail ──────────────────────────────────
    fun load(courseId: String, lessonId: String) {
        this.courseId = courseId
        this.lessonId = lessonId
        /*viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val res    = api.getLessonDetail(courseId, lessonId)
                val lesson = res.data ?: throw Exception("Lesson not found")
                _uiState.update { it.copy(lesson = lesson, isLoading = false) }

                // Start watch timer (paused if lesson is locked)
                if (!lesson) startTimer()

            } catch (e: Exception) {
                Log.e(TAG, "load: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load lesson") }
            }
        }*/
    }

    // ── 2. Watch time timer ────────────────────────────────────
    // Ticks every second while the user is on the screen.
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _uiState.update { it.copy(watchSecs = it.watchSecs + 1) }
            }
        }
    }

    // ── 3. Mark lesson complete ────────────────────────────────
    // Called when user taps "Mark as Complete" button.
    // Sends accumulated watch time to backend.
    fun markComplete() {
        if (_uiState.value.isMarking) return
        val currentLesson = _uiState.value.lesson ?: return

        timerJob?.cancel() // stop timer — freeze watch time

       /* viewModelScope.launch {
            _uiState.update { it.copy(isMarking = true) }
            try {
                val watchSecs = _uiState.value.watchSecs
                api.completeCourseLesson(
                    courseId,
                    lessonId,
                    CompleteLessonRequest(watchTimeSecs = watchSecs)
                )
                // Update local lesson state to show completed
                _uiState.update { state ->
                    state.copy(
                        isMarking = false,
                        lesson    = currentLesson.copy(isCompleted = true)
                    )
                }
                Log.d(TAG, "Lesson $lessonId marked complete (${watchSecs}s watched)")
            } catch (e: Exception) {
                Log.e(TAG, "markComplete: ${e.message}", e)
                _uiState.update { it.copy(isMarking = false, error = "Failed to mark complete: ${e.message}") }
                startTimer() // resume timer so user can retry
            }
        }*/
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
