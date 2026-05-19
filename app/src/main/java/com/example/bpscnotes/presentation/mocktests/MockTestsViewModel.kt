package com.example.bpscnotes.presentation.mocktests

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.QuizAnswerRequest
import com.example.bpscnotes.data.remote.api.QuizPreviewDto
import com.example.bpscnotes.data.remote.api.QuizQuestionDto
import com.example.bpscnotes.data.remote.api.QuizResultData
import com.example.bpscnotes.data.remote.api.QuizSubmitRequest
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
    val error: String?                      = null,
    // ── Session state ──────────────────────────────────────────
    val activeQuestions: List<QuizQuestionDto> = emptyList(),
    val isLoadingQuestions: Boolean            = false,
    val questionsError: String?                = null,
    // ── Submit result ──────────────────────────────────────────
    val submitResult: QuizResultData?          = null,
    val isSubmitting: Boolean                  = false,
    val submitError: String?                   = null,
) {
    val fullTests     get() = allTests.filter { it.type == "mock" }
    val topicTests    get() = allTests.filter { it.type == "topic" }
    val previousYears get() = allTests.filter { it.type == "previous_year" }
    val featured      get() = allTests.filter { it.type == "mock" || it.type == "previous_year" }.take(2)
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
                val testsJob = async { quizzesApi.getQuizzes(limit = 100).data?.quizzes ?: emptyList() }
                val statsJob = async {
                    try { statsApi.getStats().data } catch (e: Exception) { null }
                }
                val tests = testsJob.await()
                val stats = statsJob.await()

                _uiState.update {
                    it.copy(
                        allTests             = tests,
                        userAccuracy         = stats?.accuracy ?: 0.0,
                        userRank             = stats?.rank,
                        userQuizzesAttempted = stats?.quizzesAttempted ?: 0,
                        isLoading            = false
                    )
                }
            } catch (e: Exception) {
                Log.e("MockTestsVM", e.message ?: "", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load tests") }
            }
        }
    }

    /**
     * Fetch questions for a quiz before starting the active test.
     * Tries POST /quizzes/:id/start first (creates attempt), falls back to GET /quizzes/:id.
     */
    fun loadQuestionsForTest(quizId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingQuestions = true, questionsError = null, activeQuestions = emptyList()) }
            try {
                val detail = try {
                    quizzesApi.startQuiz(quizId)       // POST — creates session
                } catch (e: Exception) {
                    quizzesApi.getQuizDetail(quizId)   // GET fallback
                }
                val questions = detail.data?.questions ?: emptyList()
                if (questions.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoadingQuestions = false,
                            questionsError = "No questions available for this test yet. Try again later."
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingQuestions = false, activeQuestions = questions) }
                }
            } catch (e: Exception) {
                Log.e("MockTestsVM", "loadQuestions: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoadingQuestions = false,
                        questionsError = "Failed to load questions: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Submit user answers via POST /quizzes/:id/submit.
     * The result contains score, correct count, and per-question breakdown.
     */
    fun submitTest(quizId: String, answers: Map<String, Int>, timeTakenSecs: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }
            try {
                val answerList = answers.map { (qId, answerIdx) ->
                    val letter = listOf("a", "b", "c", "d").getOrElse(answerIdx) { "a" }
                    QuizAnswerRequest(questionId = qId, answer = letter)
                }
                val result = quizzesApi.submitQuiz(
                    quizId,
                    QuizSubmitRequest(answers = answerList, timeTakenSecs = timeTakenSecs)
                ).data
                _uiState.update { it.copy(isSubmitting = false, submitResult = result) }
            } catch (e: Exception) {
                Log.e("MockTestsVM", "submitTest: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        submitError  = "Could not submit results: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearQuestions() {
        _uiState.update { it.copy(activeQuestions = emptyList(), questionsError = null, submitResult = null) }
    }

    fun retry() = load()
}
