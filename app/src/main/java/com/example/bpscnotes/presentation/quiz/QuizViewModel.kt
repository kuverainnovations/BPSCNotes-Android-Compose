package com.example.bpscnotes.presentation.quiz

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.*
import com.example.bpscnotes.data.remote.dto.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────
// DOMAIN MODELS
// ─────────────────────────────────────────────────────────────

data class QuizSessionQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val subject: String,
    val difficulty: String,
    val correctOptionLetter: String? = null,   // null during play, set after submit
    val explanation: String? = null
) {
    val correctIndex: Int get() = when (correctOptionLetter?.lowercase()) {
        "a" -> 0; "b" -> 1; "c" -> 2; "d" -> 3; else -> -1
    }
}

data class QuizSession(
    val id: String,
    val title: String,
    val subtitle: String,
    val durationMins: Int,
    val passingScore: Int,
    val coinsReward: Int,
    val questions: List<QuizSessionQuestion>
)

data class QuizAnswerDetail(
    val question: QuizSessionQuestion,
    val selectedLetter: String,
    val correctLetter: String,
    val isCorrect: Boolean,
    val isSkipped: Boolean,
    val explanation: String
) {
    val selectedIndex: Int get() = when (selectedLetter.lowercase()) { "a"->0;"b"->1;"c"->2;"d"->3; else->-1 }
    val correctIndex:  Int get() = when (correctLetter.lowercase())   { "a"->0;"b"->1;"c"->2;"d"->3; else->-1 }
}

data class QuizResult(
    val score: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val skippedCount: Int,
    val totalQuestions: Int,
    val accuracy: Double,
    val coinsEarned: Int,
    val isPassed: Boolean,
    val timeTakenSecs: Int,
    val answerDetails: List<QuizAnswerDetail>
)

// ─────────────────────────────────────────────────────────────
// UI STATE
// ─────────────────────────────────────────────────────────────

data class QuizUiState(
    // ── List / Lobby ──────────────────────────────────────────
    val dailyQuizzes: List<QuizPreviewDto>    = emptyList(),
    val mockTestQuizzes: List<QuizPreviewDto> = emptyList(),
    val userProfile: UserDto?                 = null,
    val isLoadingList: Boolean                = true,
    val listError: String?                    = null,

    // ── Detail / Intro (pre-start) ────────────────────────────
    // The preview DTO loaded for the detail screen (no questions yet)
    val quizDetail: QuizPreviewDto?           = null,
    val isLoadingDetail: Boolean              = false,
    val detailError: String?                  = null,

    // ── Active session ────────────────────────────────────────
    val activeSession: QuizSession?           = null,
    val isStartingQuiz: Boolean               = false,
    val startError: String?                   = null,

    // ── Play state (persisted in VM — survives recomposition) ─
    val selectedAnswers: Map<String, String>  = emptyMap(),
    val isSubmitting: Boolean                 = false,
    val submitError: String?                  = null,

    // ── Result ────────────────────────────────────────────────
    val result: QuizResult?                   = null
)

// ─────────────────────────────────────────────────────────────
// VIEW MODEL
// ─────────────────────────────────────────────────────────────

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val quizzesApi: QuizzesApiService,
    private val authApi: AuthApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var listJob:   Job? = null
    private var detailJob: Job? = null
    private var startJob:  Job? = null

    init { loadLobby() }

    // ── 1. LIST ───────────────────────────────────────────────

    fun loadLobby() {
        listJob?.cancel()
        listJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingList = true, listError = null) }
            try {
                val quizzesResponse = quizzesApi.getQuizzes(limit = 50)
                val profileResponse = try { authApi.getMe() } catch (e: Exception) { null }
                val all = quizzesResponse.data?.quizzes ?: emptyList()

                _uiState.update {
                    it.copy(
                        dailyQuizzes    = all.filter { q -> q.type == "daily" || q.type == "topic" },
                        mockTestQuizzes = all.filter { q -> q.type == "mock" },
                        userProfile     = profileResponse?.data?.user,
                        isLoadingList   = false
                    )
                }
            } catch (e: Exception) {
                Log.e("QuizVM", "loadLobby: ${e.message}", e)
                _uiState.update {
                    it.copy(isLoadingList = false, listError = e.message ?: "Failed to load quizzes")
                }
            }
        }
    }

    // ── 2. DETAIL (intro screen) ──────────────────────────────

    /**
     * Loads quiz metadata (no questions) for the detail/intro screen.
     * GET /quizzes/:id — returns quiz info only.
     */
    fun loadQuizDetail(quizId: String) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true, detailError = null, quizDetail = null) }
            try {
                val response = quizzesApi.getQuizDetail(quizId)
                val detail   = response.data ?: throw Exception("Quiz not found")
                _uiState.update {
                    it.copy(quizDetail = detail.quiz, isLoadingDetail = false)
                }
            } catch (e: Exception) {
                Log.e("QuizVM", "loadQuizDetail: ${e.message}", e)
                _uiState.update {
                    it.copy(isLoadingDetail = false, detailError = e.message ?: "Failed to load quiz")
                }
            }
        }
    }

    // ── 3. START (creates session with questions) ─────────────

    /**
     * Called from QuizDetail "Start Quiz" button.
     * Calls POST /quizzes/:id/start → returns questions for this attempt.
     * On success navigates to QuizPlay (caller handles navigation).
     */
    fun startQuiz(quizId: String) {
        startJob?.cancel()
        startJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isStartingQuiz  = true,
                    startError      = null,
                    activeSession   = null,
                    selectedAnswers = emptyMap(),
                    result          = null,
                    submitError     = null
                )
            }
            try {
                val response  = quizzesApi.startQuiz(quizId)
                val data      = response.data ?: throw Exception("Failed to start quiz")
                val quiz      = data.quiz
                val questions = data.questions.map { q ->
                    QuizSessionQuestion(
                        id         = q.id,
                        question   = q.questionText,
                        options    = listOf(q.optionA, q.optionB, q.optionC, q.optionD),
                        subject    = q.subject ?: quiz.subject,
                        difficulty = q.difficulty
                    )
                }
                if (questions.isEmpty()) throw Exception("This quiz has no questions yet. Ask admin to add questions.")

                val session = QuizSession(
                    id           = quiz.id,
                    title        = quiz.title,
                    subtitle     = "${questions.size} questions · ${quiz.durationMins} min · ${quiz.subject}",
                    durationMins = quiz.durationMins,
                    passingScore = quiz.passingScore,
                    coinsReward  = quiz.coinsReward,
                    questions    = questions
                )
                _uiState.update { it.copy(activeSession = session, isStartingQuiz = false) }
            } catch (e: Exception) {
                Log.e("QuizVM", "startQuiz: ${e.message}", e)
                _uiState.update {
                    it.copy(isStartingQuiz = false, startError = e.message ?: "Failed to start quiz")
                }
            }
        }
    }

    // ── 4. RECORD ANSWER ─────────────────────────────────────

    fun recordAnswer(questionId: String, letter: String) {
        _uiState.update { it.copy(selectedAnswers = it.selectedAnswers + (questionId to letter)) }
    }

    fun getAnswer(questionId: String): String? = _uiState.value.selectedAnswers[questionId]

    // ── 5. SUBMIT ─────────────────────────────────────────────

    fun submitQuiz(timeTakenSecs: Int) {
        val session = _uiState.value.activeSession ?: return
        val answers = _uiState.value.selectedAnswers

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }
            try {
                val requestAnswers = session.questions.map { q ->
                    QuizAnswerRequest(questionId = q.id, answer = answers[q.id] ?: "a")
                }
                val response = quizzesApi.submitQuiz(
                    session.id,
                    QuizSubmitRequest(answers = requestAnswers, timeTakenSecs = timeTakenSecs)
                )
                val data = response.data ?: throw Exception("Empty submit response")

                val resultMap = data.answers.associateBy { it.questionId }
                val skippedIds = session.questions.filter { answers[it.id] == null }.map { it.id }.toSet()

                val updatedQuestions = session.questions.map { q ->
                    val r = resultMap[q.id]
                    q.copy(correctOptionLetter = r?.correctAnswer, explanation = r?.explanation)
                }

                val answerDetails = updatedQuestions.map { q ->
                    val r = resultMap[q.id]
                    QuizAnswerDetail(
                        question       = q,
                        selectedLetter = answers[q.id] ?: "",
                        correctLetter  = r?.correctAnswer ?: "",
                        isCorrect      = r?.isCorrect ?: false,
                        isSkipped      = q.id in skippedIds,
                        explanation    = r?.explanation ?: ""
                    )
                }

                _uiState.update { state ->
                    state.copy(
                        activeSession   = session.copy(questions = updatedQuestions),
                        result          = QuizResult(
                            score          = data.score,
                            correctCount   = data.correct,
                            wrongCount     = data.wrong,
                            skippedCount   = skippedIds.size,
                            totalQuestions = data.total,
                            accuracy       = data.accuracy,
                            coinsEarned    = data.coinsEarned,
                            isPassed       = data.isPassed,
                            timeTakenSecs  = data.timeTakenSecs,
                            answerDetails  = answerDetails
                        ),
                        isSubmitting    = false,
                        dailyQuizzes    = state.dailyQuizzes.map { q ->
                            if (q.id == session.id) q.copy(isAttempted = true) else q
                        },
                        mockTestQuizzes = state.mockTestQuizzes.map { q ->
                            if (q.id == session.id) q.copy(isAttempted = true) else q
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("QuizVM", "submitQuiz: ${e.message}", e)
                _uiState.update { it.copy(isSubmitting = false, submitError = e.message ?: "Submit failed") }
            }
        }
    }

    // ── 6. TOPIC QUIZ ─────────────────────────────────────────

    fun startTopicQuiz(subject: String) {
        val match = _uiState.value.dailyQuizzes
            .firstOrNull { it.subject.equals(subject, ignoreCase = true) && it.type == "topic" }
            ?: _uiState.value.dailyQuizzes.firstOrNull()
        if (match != null) {
            startQuiz(match.id)
        } else {
            viewModelScope.launch {
                loadLobby()
                val q = _uiState.value.dailyQuizzes
                    .firstOrNull { it.subject.equals(subject, ignoreCase = true) }
                    ?: _uiState.value.dailyQuizzes.firstOrNull()
                q?.let { startQuiz(it.id) }
            }
        }
    }

    // ── 7. RESET ──────────────────────────────────────────────

    fun exitSession() {
        _uiState.update {
            it.copy(
                activeSession   = null,
                selectedAnswers = emptyMap(),
                result          = null,
                isStartingQuiz  = false,
                isSubmitting    = false,
                submitError     = null,
                startError      = null
            )
        }
    }

    fun clearErrors() {
        _uiState.update {
            it.copy(listError = null, detailError = null, startError = null, submitError = null)
        }
    }
}
