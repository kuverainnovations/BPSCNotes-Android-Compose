package com.example.bpscnotes.presentation.quiz

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.core.analytics.Event
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
    val options: List<String>,              // text options (may be empty for image-type)
    val subject: String,
    val difficulty: String,
    val correctOptionLetter: String? = null,
    val explanation: String? = null,
    // Image support
    val questionType: String = "text",      // "text" | "image"
    val questionImageUrl: String? = null,   // shown above question text
    val optionType: String = "text",        // "text" | "image" | "mixed"
    val optionImages: List<String?> = emptyList(), // [a,b,c,d] image URLs (null = no image)
) {
    val correctIndex: Int get() = when (correctOptionLetter?.lowercase()) {
        "a" -> 0; "b" -> 1; "c" -> 2; "d" -> 3; else -> -1
    }
    val isImageQuestion: Boolean get() = questionType == "image" && !questionImageUrl.isNullOrBlank()
    val isImageOptions:  Boolean get() = optionType == "image"
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
    val topicQuizzes: List<QuizPreviewDto>    = emptyList(),
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
                        dailyQuizzes    = all.filter { q -> q.type == "daily" },
                        topicQuizzes    = all.filter { q -> q.type == "topic" },
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
                        id               = q.id,
                        question         = q.questionText,
                        options          = listOf(q.optionA, q.optionB, q.optionC, q.optionD),
                        subject          = q.subject ?: quiz.subject,
                        difficulty       = q.difficulty,
                        explanation      = q.explanation,
                        questionType     = q.questionType,
                        questionImageUrl = q.questionImageUrl,
                        optionType       = q.optionType,
                        optionImages     = q.optionImages
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
                // Parse the actual message from the API response body
                // e.message just says "HTTP 400" — the real message is in the JSON body
                val msg =  when {
                    e.message?.contains("400") == true ->
                        "This quiz has no questions yet. Please try another quiz or contact admin."
                    e.message?.contains("404") == true -> "Quiz not found."
                    e.message?.contains("403") == true -> "You don't have access to this quiz."
                    else -> e.message ?: "Failed to start quiz. Please try again."
                }
                _uiState.update { it.copy(isStartingQuiz = false, startError = msg) }
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true, startError = null) }
            try {
                // Strategy 1: exact subject + topic type
                var quizzes = quizzesApi.getQuizzes(subject = subject, type = "topic", limit = 5)
                    .data?.quizzes ?: emptyList()

                // Strategy 2: exact subject, any type
                if (quizzes.isEmpty()) {
                    quizzes = quizzesApi.getQuizzes(subject = subject, limit = 5)
                        .data?.quizzes ?: emptyList()
                }

                // Strategy 3: broader subject match — filter out quizzes with 0 questions
                if (quizzes.isEmpty()) {
                    val all = quizzesApi.getQuizzes(limit = 50).data?.quizzes ?: emptyList()
                    quizzes = all.filter { q ->
                        q.totalQuestions > 0 && (
                                q.subject.contains(subject, ignoreCase = true) ||
                                        subject.contains(q.subject, ignoreCase = true)
                                )
                    }
                }

                // Strategy 4: absolute fallback — any topic quiz with questions
                if (quizzes.isEmpty()) {
                    quizzes = quizzesApi.getQuizzes(type = "topic", limit = 20)
                        .data?.quizzes?.filter { it.totalQuestions > 0 } ?: emptyList()
                }

                // Always skip quizzes with 0 questions — they will 400 on start
                val pick = quizzes.filter { it.totalQuestions > 0 }.firstOrNull()
                if (pick != null) {
                    // FIX: Reset isLoadingDetail before handing off to startQuiz
                    // startQuiz uses isStartingQuiz for its own spinner — it never resets
                    // isLoadingDetail, so the spinner from startTopicQuiz would stick forever
                    _uiState.update { it.copy(isLoadingDetail = false) }
                    startQuiz(pick.id)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingDetail = false,
                            startError = "No MCQ quiz available right now. The admin needs to add quizzes for this topic."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("QuizVM", "startTopicQuiz: \${e.message}", e)
                val errorMsg = "Failed to load quiz. Please check your connection."
                _uiState.update {
                    it.copy(
                        isLoadingDetail = false,
                        startError = errorMsg
                    )
                }
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