package com.example.bpscnotes.presentation.answerwriting

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import com.example.bpscnotes.core.network.toUserMessage
import com.example.bpscnotes.data.remote.api.AnswerWritingApiService
import com.example.bpscnotes.data.remote.api.ReviewAssignmentDto
import com.example.bpscnotes.data.remote.api.SubmitPeerReviewRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────
// PEER REVIEW — one anonymous answer at a time. Submit a structured
// review (+1 credit) and the next answer loads automatically.
// ─────────────────────────────────────────────────────────────

data class PeerReviewUiState(
    val assignment: ReviewAssignmentDto?  = null,
    val isLoading: Boolean                = true,
    /** Pool is empty (or reviewing is locked) — show the done state */
    val noneAvailable: Boolean            = false,
    val loadError: String?                = null,

    // ── Review form ───────────────────────────────────────────
    val verdict: String?                  = null,   // yes | partly | no
    val rating: Int                       = 0,      // 1..5
    val improvementArea: String?          = null,
    val suggestion: String                = "",

    val isSubmitting: Boolean             = false,
    val submitError: String?              = null,
    /** Set after each successful review — drives the credit snackbar */
    val justSubmittedMessage: String?     = null,
    val reviewsDoneThisSession: Int       = 0,
)

@HiltViewModel
class PeerReviewViewModel @Inject constructor(
    private val api: AnswerWritingApiService,
    private val bus: RefreshEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeerReviewUiState())
    val uiState: StateFlow<PeerReviewUiState> = _uiState.asStateFlow()

    init { loadNext() }

    fun loadNext() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true, loadError = null, noneAvailable = false,
                    assignment = null,
                    // reset the form for the new answer
                    verdict = null, rating = 0, improvementArea = null, suggestion = "",
                    submitError = null,
                )
            }
            try {
                val next = api.getNextToReview().data?.submission
                _uiState.update {
                    it.copy(isLoading = false, assignment = next, noneAvailable = next == null)
                }
            } catch (e: Exception) {
                Log.e("PeerReviewVM", "loadNext: ${e.message}", e)
                _uiState.update {
                    it.copy(isLoading = false, loadError = e.toUserMessage("Could not load an answer to review"))
                }
            }
        }
    }

    fun setVerdict(v: String)         { _uiState.update { it.copy(verdict = v) } }
    fun setRating(r: Int)             { _uiState.update { it.copy(rating = r) } }
    fun setImprovementArea(a: String) {
        _uiState.update { it.copy(improvementArea = if (it.improvementArea == a) null else a) }
    }
    fun setSuggestion(s: String)      { _uiState.update { it.copy(suggestion = s.take(200)) } }

    val canSubmit: Boolean
        get() = _uiState.value.let { it.verdict != null && it.rating in 1..5 && !it.isSubmitting }

    fun submit() {
        val s = _uiState.value
        val target = s.assignment ?: return
        if (s.verdict == null || s.rating !in 1..5 || s.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }
            try {
                val resp = api.submitPeerReview(
                    target.id,
                    SubmitPeerReviewRequest(
                        verdict = s.verdict,
                        rating = s.rating,
                        improvementArea = s.improvementArea,
                        suggestion = s.suggestion.trim().ifBlank { null },
                    )
                )
                val msg = resp.message.ifBlank { "Review submitted! +1 credit" }
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        justSubmittedMessage = msg,
                        reviewsDoneThisSession = it.reviewsDoneThisSession + 1,
                    )
                }
                if ((resp.data?.coinsEarned ?: 0) > 0) bus.emit(RefreshEvent.CoinsChanged)
                loadNext()
            } catch (e: Exception) {
                Log.e("PeerReviewVM", "submit: ${e.message}", e)
                _uiState.update {
                    it.copy(isSubmitting = false, submitError = e.toUserMessage("Could not submit review"))
                }
            }
        }
    }

    fun clearToasts() {
        _uiState.update { it.copy(justSubmittedMessage = null, submitError = null) }
    }
}
