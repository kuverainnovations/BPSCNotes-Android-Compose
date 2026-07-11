package com.example.bpscnotes.presentation.answerwriting

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import com.example.bpscnotes.core.network.toUserMessage
import com.example.bpscnotes.data.remote.api.AnswerQuestionDetailDto
import com.example.bpscnotes.data.remote.api.AnswerQuestionDto
import com.example.bpscnotes.data.remote.api.AnswerSubmissionDto
import com.example.bpscnotes.data.remote.api.AnswerWritingApiService
import com.example.bpscnotes.data.remote.api.SubmitAnswerRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────
// ANSWER WRITING — daily Mains descriptive-answer practice.
// List of questions → write one answer per question → model answer
// unlocks on submit → admin grades it later (score + feedback).
// ─────────────────────────────────────────────────────────────

data class AnswerWritingUiState(
    // ── List ──────────────────────────────────────────────────
    val questions: List<AnswerQuestionDto>          = emptyList(),
    val mySubmissions: List<AnswerSubmissionDto>    = emptyList(),
    val isLoading: Boolean                          = true,
    val listError: String?                          = null,

    // ── Detail ────────────────────────────────────────────────
    val question: AnswerQuestionDetailDto?          = null,
    val submission: AnswerSubmissionDto?            = null,
    val isLoadingDetail: Boolean                    = false,
    val detailError: String?                        = null,

    // ── Writing / submit ──────────────────────────────────────
    // The draft lives in the VM so rotation/recomposition never eats
    // a half-written 250-word answer.
    val draftText: String                           = "",
    val isSubmitting: Boolean                       = false,
    val submitError: String?                        = null,
    /** Set right after a successful submit — drives the coin snackbar */
    val justEarnedCoins: Int?                       = null,
) {
    val todayQuestion: AnswerQuestionDto?
        get() = questions.firstOrNull { it.isToday && !it.isSubmitted }
            ?: questions.firstOrNull { !it.isSubmitted }
}

@HiltViewModel
class AnswerWritingViewModel @Inject constructor(
    private val api: AnswerWritingApiService,
    private val bus: RefreshEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnswerWritingUiState())
    val uiState: StateFlow<AnswerWritingUiState> = _uiState.asStateFlow()

    private var detailJob: Job? = null
    private var loadedQuestionId: String? = null

    init { load() }

    // ── List (questions + my history in one shot) ─────────────
    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, listError = null) }
            try {
                val questions = api.getQuestions().data?.questions ?: emptyList()
                val mine = try { api.getMySubmissions().data?.submissions ?: emptyList() }
                           catch (_: Exception) { emptyList() }
                _uiState.update {
                    it.copy(questions = questions, mySubmissions = mine, isLoading = false)
                }
            } catch (e: Exception) {
                Log.e("AnswerWritingVM", "load: ${e.message}", e)
                _uiState.update {
                    it.copy(isLoading = false, listError = e.toUserMessage("Failed to load questions"))
                }
            }
        }
    }

    // ── Detail ─────────────────────────────────────────────────
    fun loadDetail(questionId: String) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            // Keep the draft when re-opening the same question; clear it
            // when moving to a different one.
            val keepDraft = loadedQuestionId == questionId
            loadedQuestionId = questionId
            _uiState.update {
                it.copy(
                    isLoadingDetail = true, detailError = null,
                    question = null, submission = null,
                    draftText = if (keepDraft) it.draftText else "",
                    submitError = null, justEarnedCoins = null,
                )
            }
            try {
                val data = api.getQuestionDetail(questionId).data ?: throw Exception("Question not found")
                _uiState.update {
                    it.copy(question = data.question, submission = data.submission, isLoadingDetail = false)
                }
            } catch (e: Exception) {
                Log.e("AnswerWritingVM", "loadDetail: ${e.message}", e)
                _uiState.update {
                    it.copy(isLoadingDetail = false, detailError = e.toUserMessage("Failed to load question"))
                }
            }
        }
    }

    fun updateDraft(text: String) {
        _uiState.update { it.copy(draftText = text) }
    }

    val draftWordCount: Int
        get() = _uiState.value.draftText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size

    // ── Submit ─────────────────────────────────────────────────
    fun submit(questionId: String, timeTakenSecs: Int) {
        val text = _uiState.value.draftText.trim()
        if (text.isBlank() || _uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }
            try {
                val resp = api.submitAnswer(questionId, SubmitAnswerRequest(text, timeTakenSecs))
                val data = resp.data ?: throw Exception("Submit failed")
                _uiState.update { s ->
                    s.copy(
                        isSubmitting    = false,
                        submission      = data.submission,
                        // model answer unlocks the moment the submit lands
                        question        = s.question?.copy(modelAnswer = data.modelAnswer ?: s.question.modelAnswer),
                        justEarnedCoins = data.coinsEarned.takeIf { it > 0 },
                        draftText       = "",
                        // reflect the new status in the list without a reload
                        questions       = s.questions.map {
                            if (it.id == questionId) it.copy(isSubmitted = true, myStatus = "submitted") else it
                        },
                    )
                }
                if ((data.coinsEarned) > 0) bus.emit(RefreshEvent.CoinsChanged)
            } catch (e: Exception) {
                Log.e("AnswerWritingVM", "submit: ${e.message}", e)
                _uiState.update {
                    it.copy(isSubmitting = false, submitError = e.toUserMessage("Could not submit — please try again"))
                }
            }
        }
    }

    fun clearSubmitError()  { _uiState.update { it.copy(submitError = null) } }
    fun clearCoinsToast()   { _uiState.update { it.copy(justEarnedCoins = null) } }
}
