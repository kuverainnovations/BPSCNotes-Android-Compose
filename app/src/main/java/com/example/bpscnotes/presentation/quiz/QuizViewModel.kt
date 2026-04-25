package com.example.bpscnotes.presentation.quiz

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.api.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI state for the quiz lobby (list of sessions) ────────────────────────────

data class QuizLobbyUiState(
    val freeQuizzes: List<QuizPreviewDto> = emptyList(),
    val paidQuizzes: List<QuizPreviewDto> = emptyList(),
    val userCoins: Int = 0,
    val userAccuracy: Double = 0.0,
    val userStreak: Int = 0,
    val userRank: Int? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

// ── UI state for an active quiz session ───────────────────────────────────────

data class QuizSessionUiState(
    val quiz: QuizPreviewDto? = null,
    val questions: List<QuizSessionQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswers: Map<String, String> = emptyMap(), // questionId → "a"|"b"|"c"|"d"
    val isSubmitting: Boolean = false,
    val result: QuizResultDto? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * Runtime question model used in the quiz session.
 * Maps from [QuizQuestionDto] with all the extra UI fields added.
 */
data class QuizSessionQuestion(
    val id: String,
    val question: String,
    val options: List<String>,            // [optA, optB, optC, optD]
    val correctOption: String,            // "a"|"b"|"c"|"d"
    val explanation: String,
    val subject: String,
    val difficulty: String,
    val hint: String = ""                 // backend doesn't provide this yet
)

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val quizzesApi: QuizzesApiService,
    private val statsApi: UserStatsApiService,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _lobbyState = MutableStateFlow(QuizLobbyUiState())
    val lobbyState: StateFlow<QuizLobbyUiState> = _lobbyState.asStateFlow()

    private val _sessionState = MutableStateFlow(QuizSessionUiState())
    val sessionState: StateFlow<QuizSessionUiState> = _sessionState.asStateFlow()

    init { loadLobby() }

    fun loadLobby() {
        viewModelScope.launch {
            _lobbyState.update { it.copy(isLoading = true, error = null) }
            try {
                val quizzes = quizzesApi.getQuizzes(limit = 20).data?.quizzes ?: emptyList()
                val stats   = try { statsApi.getStats().data } catch (e: Exception) { null }
                _lobbyState.update {
                    it.copy(
                        freeQuizzes  = quizzes.filter { q -> q.type == "daily" || q.type == "topic" },
                        paidQuizzes  = quizzes.filter { q -> q.type == "mock" },
                        userAccuracy = stats?.accuracy ?: 0.0,
                        userStreak   = stats?.currentStreak ?: 0,
                        isLoading    = false
                    )
                }
            } catch (e: Exception) {
                Log.e("QuizVM", e.message ?: "", e)
                _lobbyState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load quizzes") }
            }
        }
    }

    /** Load quiz detail + questions for a session */
    fun loadQuizDetail(quizId: String) {
        viewModelScope.launch {
            _sessionState.update { it.copy(isLoading = true, error = null, result = null, selectedAnswers = emptyMap(), currentIndex = 0) }
            try {
                val response = quizzesApi.getQuizDetail(quizId)
                val detail   = response.data ?: throw Exception("Empty quiz response")
                val questions = detail.questions.mapIndexed { idx, q ->
                    QuizSessionQuestion(
                        id           = q.id,
                        question     = q.questionText,
                        options      = listOf(q.optionA, q.optionB, q.optionC, q.optionD),
                        correctOption = q.correctOption,
                        explanation  = q.explanation ?: "",
                        subject      = q.subject ?: detail.quiz.subject,
                        difficulty   = q.difficulty
                    )
                }
                _sessionState.update {
                    it.copy(quiz = detail.quiz, questions = questions, isLoading = false)
                }
            } catch (e: Exception) {
                Log.e("QuizVM", e.message ?: "", e)
                _sessionState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load quiz") }
            }
        }
    }

    fun selectAnswer(questionId: String, answer: String) {
        _sessionState.update {
            it.copy(selectedAnswers = it.selectedAnswers + (questionId to answer))
        }
    }

    fun nextQuestion() {
        val size = _sessionState.value.questions.size
        _sessionState.update { state ->
            if (state.currentIndex < size - 1) state.copy(currentIndex = state.currentIndex + 1)
            else state
        }
    }

    fun submitQuiz(quizId: String, timeTakenSecs: Int) {
        val state = _sessionState.value
        viewModelScope.launch {
            _sessionState.update { it.copy(isSubmitting = true) }
            try {
                val answers = state.questions.map { q ->
                    QuizAnswerRequest(
                        questionId = q.id,
                        answer     = state.selectedAnswers[q.id] ?: "a"
                    )
                }
                val response = quizzesApi.submitQuiz(quizId, QuizSubmitRequest(answers, timeTakenSecs))
                val result   = response.data?.result ?: throw Exception("Empty result")
                _sessionState.update { it.copy(result = result, isSubmitting = false) }
            } catch (e: Exception) {
                Log.e("QuizVM", e.message ?: "", e)
                _sessionState.update { it.copy(isSubmitting = false, error = e.message ?: "Submit failed") }
            }
        }
    }

    fun clearSession() {
        _sessionState.update { QuizSessionUiState() }
    }
}