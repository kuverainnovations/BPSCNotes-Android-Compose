package com.example.bpscnotes.presentation.mocktests

import com.example.bpscnotes.core.network.toUserMessage

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
import com.example.bpscnotes.data.remote.api.CoinsApiService
import com.example.bpscnotes.data.remote.api.UnlockContentRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.bpscnotes.data.remote.api.QuizLeaderboardItemResponse


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
    // ── Leaderboard ────────────────────────────────────────────
    val leaderboard: List<QuizLeaderboardEntry> = emptyList(),
    val isLoadingLeaderboard: Boolean           = false,
    val leaderboardError: String?               = null,
    // ── Session integrity ──────────────────────────────────────
    val activeSessionId: String?                = null,
    val backgroundSecs: Int                     = 0,

    // ── Coin unlock (premium tests) ─────────────────────────
    val coinBalance: Int?                       = null,
    val isUnlocking: Boolean                    = false,
    val unlockError: String?                    = null,
    /** Set right after a successful unlock so the screen can auto-proceed */
    val justUnlockedId: String?                 = null,
) {
    val fullTests     get() = allTests.filter { it.type == "mock" }
    val topicTests    get() = allTests.filter { it.type == "topic" }
    val previousYears get() = allTests.filter { it.type == "previous_year" }
    val featured      get() = allTests.filter { it.type == "mock" || it.type == "previous_year" }.take(2)
}

data class QuizLeaderboardEntry(
    val rank: Int,
    val userId: String,
    val userName: String,
    val avatarUrl: String?,
    val score: Float,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val timeTakenSecs: Int,
    val isCurrentUser: Boolean = false
)

// DTO matching GET /quizzes/:id/leaderboard response
data class QuizLeaderboardItemDto(
    @com.google.gson.annotations.SerializedName("rank_position")   val rankPosition: Int = 0,
    @com.google.gson.annotations.SerializedName("user_id")         val userId: String = "",
    @com.google.gson.annotations.SerializedName("user_name")       val userName: String = "",
    @com.google.gson.annotations.SerializedName("score")           val score: Float = 0f,
    @com.google.gson.annotations.SerializedName("correct_answers") val correctAnswers: Int = 0,
    @com.google.gson.annotations.SerializedName("total_questions") val totalQuestions: Int = 0,
    @com.google.gson.annotations.SerializedName("time_taken_secs") val timeTakenSecs: Int = 0,
    @com.google.gson.annotations.SerializedName("is_current_user") val isCurrentUser: Boolean = false
)

data class QuizLeaderboardData(
    val leaderboard: List<QuizLeaderboardItemDto> = emptyList()
)

@HiltViewModel
class MockTestsViewModel @Inject constructor(
    private val quizzesApi: QuizzesApiService,
    private val statsApi: UserStatsApiService,
    private val coinsApi: CoinsApiService,
    private val cacheInvalidator: com.example.bpscnotes.core.network.CacheInvalidator,
    private val bus: com.example.bpscnotes.core.events.RefreshEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MockTestsUiState())
    val uiState: StateFlow<MockTestsUiState> = _uiState.asStateFlow()

    init {
        load()
        // Daily/Topic quiz screen (separate ViewModel instance) broadcasts this
        // on submit — pick it up so a mock test's own attempt/score status
        // (shown via the same /quizzes list) doesn't go stale here either.
        viewModelScope.launch {
            bus.events.collect { event ->
                if (event is com.example.bpscnotes.core.events.RefreshEvent.QuizCompleted) load()
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                var testsError: String? = null
                val testsJob = async {
                    try { quizzesApi.getQuizzes(limit = 100).data?.quizzes ?: emptyList() }
                    catch (e: Exception) {
                        Log.e("MockTestsVM", e.toUserMessage(""), e)
                        testsError = e.toUserMessage("Failed to load tests")
                        emptyList()
                    }
                }
                val statsJob = async {
                    try { statsApi.getStats().data } catch (e: Exception) { null }
                }
                val tests = testsJob.await()
                val stats = statsJob.await()

                _uiState.update {
                    it.copy(
                        allTests             = tests.filter { it.type == "mock" || it.type == "previous_year" },
                        userAccuracy         = stats?.accuracy ?: 0.0,
                        userRank             = stats?.rank,
                        userQuizzesAttempted = stats?.quizzesAttempted ?: 0,
                        isLoading            = false,
                        error                = testsError
                    )
                }
            } catch (e: Exception) {
                Log.e("MockTestsVM", e.toUserMessage(""), e)
                _uiState.update { it.copy(isLoading = false, error = e.toUserMessage("Failed to load tests")) }
            }
        }
    }

    // ── Coin unlock (premium tests) ────────────────────────────

    /** Called when the unlock dialog opens — best-effort balance fetch. */
    fun fetchCoinBalance() {
        viewModelScope.launch {
            try {
                val bal = coinsApi.getBalance().data?.balance
                _uiState.update { it.copy(coinBalance = bal) }
            } catch (_: Exception) { /* dialog copes with null */ }
        }
    }

    /** Spend earned coins to permanently unlock a premium test (idempotent server-side). */
    fun unlockTest(quizId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUnlocking = true, unlockError = null) }
            try {
                val resp = coinsApi.unlockContent(UnlockContentRequest(contentId = quizId))
                val data = resp.data ?: throw Exception("Unlock failed")
                _uiState.update { s ->
                    s.copy(
                        isUnlocking    = false,
                        coinBalance    = data.balance,
                        justUnlockedId = quizId,
                        allTests       = s.allTests.map { if (it.id == quizId) it.copy(isUnlocked = true) else it },
                    )
                }
                bus.emit(com.example.bpscnotes.core.events.RefreshEvent.CoinsChanged)
            } catch (e: Exception) {
                Log.e("MockTestsVM", "unlockTest: ${e.message}", e)
                _uiState.update {
                    it.copy(isUnlocking = false, unlockError = e.toUserMessage("Could not unlock — please try again"))
                }
            }
        }
    }

    fun clearUnlockState() {
        _uiState.update { it.copy(unlockError = null, justUnlockedId = null) }
    }

    /** Fetch questions for a quiz. POST /quizzes/:id/start (creates session, no GET fallback). */
    fun loadQuestionsForTest(quizId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingQuestions = true, questionsError = null, activeQuestions = emptyList()) }
            try {
                val detail = quizzesApi.startQuiz(quizId)
                val questions = detail.data?.questions ?: emptyList()
                val sessionId = detail.data?.sessionId
                if (questions.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoadingQuestions = false,
                            questionsError = "No questions available for this test yet. Try again later."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingQuestions = false,
                            activeQuestions = questions,
                            activeSessionId = sessionId,
                            backgroundSecs = 0,
                        )
                    }
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

    fun addBackgroundSecs(secs: Int) {
        _uiState.update { it.copy(backgroundSecs = it.backgroundSecs + secs) }
    }

    /**
     * Submit user answers via POST /quizzes/:id/submit.
     * The result contains score, correct count, and per-question breakdown.
     */
    fun submitTest(quizId: String, answers: Map<String, Int>, timeTakenSecs: Int) {
        val sessionId      = _uiState.value.activeSessionId
        val backgroundSecs = _uiState.value.backgroundSecs
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }
            try {
                // Only send answered questions — skipped ones must NOT default to "a"
                val answerList = answers.mapNotNull { (qId, answerIdx) ->
                    val letter = listOf("a", "b", "c", "d").getOrNull(answerIdx) ?: return@mapNotNull null
                    QuizAnswerRequest(questionId = qId, answer = letter)
                }
                val result = quizzesApi.submitQuiz(
                    quizId,
                    QuizSubmitRequest(
                        answers        = answerList,
                        timeTakenSecs  = timeTakenSecs,
                        sessionId      = sessionId,
                        backgroundSecs = backgroundSecs,
                    )
                ).data
                _uiState.update { state ->
                    state.copy(
                        isSubmitting = false,
                        submitResult = result,
                        // Same fix as QuizViewModel's Daily/Topic quiz submit: without this,
                        // the mock test list kept showing "not attempted" / the old score
                        // until something else happened to reload it — this ViewModel never
                        // evicted the /quizzes cache or updated its own list after a submit.
                        // maxOf guards against a retry flashing a lower score than a
                        // previous best before the reload below corrects it.
                        allTests = result?.let { r ->
                            state.allTests.map { q ->
                                if (q.id == quizId) q.copy(isAttempted = true, myLastScore = maxOf(q.myLastScore ?: 0, r.score)) else q
                            }
                        } ?: state.allTests
                    )
                }
                cacheInvalidator.evict(com.example.bpscnotes.core.network.CacheInvalidator.QUIZ_ENDPOINTS)
                bus.emit(com.example.bpscnotes.core.events.RefreshEvent.QuizCompleted)
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

    fun loadLeaderboard(quizId: String, currentUserId: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLeaderboard = true, leaderboardError = null) }
            try {
                // QuizLeaderboardResponse has correct @SerializedName fields —
                // Gson deserializes score/correct_answers/total_questions/time_taken_secs directly
                val lbData = quizzesApi.getLeaderboard(quizId).data

                val entries = (lbData?.leaderboard ?: emptyList())
                    .filter { it.totalQuestions > 0 && it.score > 0 }  // skip incomplete
                    .groupBy { it.userId }                              // deduplicate per user
                    .values
                    .map { attempts -> attempts.maxByOrNull { it.score.toDouble() * 10000 - it.timeTakenSecs }!! }
                    .sortedWith(compareByDescending<QuizLeaderboardItemResponse> { it.score.toDouble() }
                        .thenBy { it.timeTakenSecs })
                    .mapIndexed { index, dto ->
                        QuizLeaderboardEntry(
                            rank           = index + 1,
                            userId         = dto.userId,
                            userName       = dto.userName,
                            avatarUrl      = null,
                            score          = dto.score,
                            correctAnswers = dto.correctAnswers,
                            totalQuestions = dto.totalQuestions,
                            timeTakenSecs  = dto.timeTakenSecs,
                            isCurrentUser  = dto.isCurrentUser || dto.userId == currentUserId
                        )
                    }
                _uiState.update { it.copy(isLoadingLeaderboard = false, leaderboard = entries) }
            } catch (e: Exception) {
                // Endpoint may not exist yet — build leaderboard from submit result
                // Show the current user's result as rank #1 as a temporary fallback
                val submitResult = _uiState.value.submitResult
                val fallback = if (submitResult != null) {
                    listOf(QuizLeaderboardEntry(
                        rank           = submitResult.rank.takeIf { it > 0 } ?: 1,
                        userId         = currentUserId,
                        userName       = "You",
                        avatarUrl      = null,
                        score          = submitResult.score.toFloat(),
                        correctAnswers = submitResult.correct,
                        totalQuestions = submitResult.total,
                        timeTakenSecs  = submitResult.timeTakenSecs,
                        isCurrentUser  = true
                    ))
                } else emptyList()
                _uiState.update { it.copy(isLoadingLeaderboard = false, leaderboard = fallback,
                    leaderboardError = if (fallback.isEmpty()) "Leaderboard not available yet" else null) }
            }
        }
    }

    fun retry() = load()
}