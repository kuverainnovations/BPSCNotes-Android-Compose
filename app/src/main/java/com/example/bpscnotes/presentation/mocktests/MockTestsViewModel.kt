package com.example.bpscnotes.presentation.mocktests

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.QuizPreviewDto
import com.example.bpscnotes.data.remote.api.QuizzesApiService
import com.example.bpscnotes.data.remote.api.UserStatsApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MockTestsUiState(
    val allTests: List<QuizPreviewDto>      = emptyList(),
    val userAccuracy: Double                = 0.0,
    val userRank: Int?                      = null,
    val userQuizzesAttempted: Int           = 0,
    val isLoading: Boolean                  = true,
    val error: String?                      = null
) {
    val fullTests     get() = allTests.filter { it.type == "mock" }
    val topicTests    get() = allTests.filter { it.type == "topic" }
    val previousYears get() = allTests.filter { it.type == "previous_year" }
}

@HiltViewModel
class MockTestsViewModel @Inject constructor(
    private val quizzesApi: QuizzesApiService,
    private val statsApi: UserStatsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MockTestsUiState())
    val uiState: StateFlow<MockTestsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val testsJob = async { quizzesApi.getQuizzes(limit = 50).data?.quizzes ?: emptyList() }
                val statsJob = async { try { statsApi.getStats().data } catch (e: Exception) { null } }

                val tests = testsJob.await()
                val stats = statsJob.await()

                _uiState.update {
                    it.copy(
                        allTests              = tests,
                        userAccuracy          = stats?.accuracy ?: 0.0,
                        userQuizzesAttempted  = stats?.quizzesAttempted ?: 0,
                        isLoading             = false
                    )
                }
            } catch (e: Exception) {
                Log.e("MockTestsVM", e.message ?: "", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load tests") }
            }
        }
    }

    fun retry() = load()
}
