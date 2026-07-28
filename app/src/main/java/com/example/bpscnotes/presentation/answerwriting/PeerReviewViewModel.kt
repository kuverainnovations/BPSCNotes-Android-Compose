package com.example.bpscnotes.presentation.answerwriting

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import com.example.bpscnotes.core.network.toUserMessage
import com.example.bpscnotes.data.remote.api.AnswerWritingApiService
import com.example.bpscnotes.data.remote.api.ReviewAssignmentDto
import com.example.bpscnotes.data.remote.api.VoteReviewRequest
import com.example.bpscnotes.data.remote.api.ReviewQuestionDto
import com.example.bpscnotes.data.remote.api.SubmitPeerReviewRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────
// PEER REVIEW — question first, then the answers under it.
//
//   Questions (pending counts)  →  Answers for one question  →  Review form
//
// Reviewing an answer to a question unlocks the reviews waiting on YOUR
// answer to that same question (client reciprocity rule), so the question
// list flags exactly where a single review would unlock something.
// ─────────────────────────────────────────────────────────────

enum class PeerReviewStep { QUESTIONS, ANSWERS, FORM }

data class PeerReviewUiState(
    val step: PeerReviewStep               = PeerReviewStep.QUESTIONS,

    // ── Step 1: questions I can review ────────────────────────
    val questions: List<ReviewQuestionDto> = emptyList(),
    val totalPending: Int                  = 0,

    // ── Step 2: answers under the chosen question ─────────────
    val question: ReviewQuestionDto?       = null,
    val pool: List<ReviewAssignmentDto>    = emptyList(),
    /** Reviewing in this question unlocks the feedback on my own answer */
    val unlocksMyReviews: Boolean          = false,

    // ── Step 3: the answer open for review ────────────────────
    val assignment: ReviewAssignmentDto?   = null,

    val isLoading: Boolean                 = true,
    /** Nothing anywhere is available to review — show the done state */
    val noneAvailable: Boolean             = false,
    val loadError: String?                 = null,

    // ── Review form ───────────────────────────────────────────
    val verdict: String?                   = null,   // yes | partly | no
    val rating: Int                        = 0,      // 1..5
    /** "Top three weaknesses" — up to 3 areas */
    val improvementAreas: Set<String>      = emptySet(),
    val suggestion: String                 = "",

    val isSubmitting: Boolean              = false,
    val submitError: String?               = null,
    /** Set after each successful review — drives the credit snackbar */
    val justSubmittedMessage: String?      = null,
    val reviewsDoneThisSession: Int        = 0,
    /** Reviewer's reviews are being voted unhelpful — reviewing earns no coins */
    val lowReputation: Boolean             = false,
    /** Set when a review just unlocked my own feedback — drives the dialog */
    val unlockedQuestionId: String?        = null,
    val unlockedReviewCount: Int           = 0,
)

@HiltViewModel
class PeerReviewViewModel @Inject constructor(
    private val api: AnswerWritingApiService,
    private val bus: RefreshEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeerReviewUiState())
    val uiState: StateFlow<PeerReviewUiState> = _uiState.asStateFlow()

    /** Set once when the screen is opened with a question already in mind. */
    private var pendingDeepLinkQuestionId: String? = null

    init { loadQuestions() }

    /**
     * Open straight into one question's answers — used by the "review one
     * answer to unlock" button on the answer detail screen, so the student
     * lands on exactly the answers that will unlock their own feedback.
     */
    fun openQuestion(questionId: String) {
        pendingDeepLinkQuestionId = questionId
        val known = _uiState.value.questions.firstOrNull { it.id == questionId }
        if (known != null) selectQuestion(known) else loadQuestions()
    }

    // ── Step 1 ────────────────────────────────────────────────
    fun loadQuestions() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true, loadError = null, noneAvailable = false,
                    step = PeerReviewStep.QUESTIONS,
                    question = null, assignment = null, pool = emptyList(),
                    verdict = null, rating = 0, improvementAreas = emptySet(), suggestion = "",
                    submitError = null,
                )
            }
            try {
                val data = api.getReviewQuestions().data
                // Keep questions that still have answers to open — whether to
                // review or to re-read after reviewing.
                val list = data?.questions.orEmpty().filter { it.answerCount > 0 }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        questions = list,
                        totalPending = data?.totalPending ?: 0,
                        noneAvailable = list.isEmpty(),
                    )
                }
                // Honour a deep link once the list is in hand
                pendingDeepLinkQuestionId?.let { qid ->
                    list.firstOrNull { it.id == qid }?.let { selectQuestion(it) }
                    pendingDeepLinkQuestionId = null
                }
            } catch (e: Exception) {
                Log.e("PeerReviewVM", "loadQuestions: ${e.message}", e)
                _uiState.update {
                    it.copy(isLoading = false, loadError = e.toUserMessage("Could not load questions to review"))
                }
            }
        }
    }

    // ── Step 2 ────────────────────────────────────────────────
    fun selectQuestion(q: ReviewQuestionDto) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    step = PeerReviewStep.ANSWERS, question = q,
                    isLoading = true, loadError = null, pool = emptyList(), assignment = null,
                )
            }
            try {
                val data = api.getReviewList(q.id).data
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pool = data?.submissions.orEmpty(),
                        unlocksMyReviews = data?.unlocksMyReviews ?: false,
                    )
                }
            } catch (e: Exception) {
                Log.e("PeerReviewVM", "selectQuestion: ${e.message}", e)
                _uiState.update {
                    it.copy(isLoading = false, loadError = e.toUserMessage("Could not load answers to review"))
                }
            }
        }
    }

    // ── Step 3 ────────────────────────────────────────────────
    fun selectAssignment(a: ReviewAssignmentDto) {
        _uiState.update {
            it.copy(
                step = PeerReviewStep.FORM, assignment = a,
                // fresh form for the chosen answer
                verdict = null, rating = 0, improvementAreas = emptySet(), suggestion = "",
                submitError = null,
            )
        }
    }

    /**
     * "Helpful? 👍👎" on the answer currently open for review. Optimistic —
     * the vote is trivially reversible (tapping again flips it). Updates both
     * the open assignment and its row in the pool.
     */
    fun voteAnswerHelpful(helpful: Boolean) {
        val target = _uiState.value.assignment ?: return
        val before = _uiState.value
        fun ReviewAssignmentDto.applyVote(): ReviewAssignmentDto {
            val h = helpfulCount - (if (myHelpfulVote == true) 1 else 0) + (if (helpful) 1 else 0)
            val n = notHelpfulCount - (if (myHelpfulVote == false) 1 else 0) + (if (!helpful) 1 else 0)
            return copy(
                helpfulCount = h.coerceAtLeast(0),
                notHelpfulCount = n.coerceAtLeast(0),
                myHelpfulVote = helpful,
            )
        }
        _uiState.update { s ->
            s.copy(
                assignment = s.assignment?.applyVote(),
                pool = s.pool.map { if (it.id == target.id) it.applyVote() else it },
            )
        }
        viewModelScope.launch {
            try {
                api.voteAnswerHelpful(target.id, VoteReviewRequest(helpful))
            } catch (e: Exception) {
                Log.e("PeerReviewVM", "voteAnswerHelpful: ${e.message}", e)
                _uiState.update {
                    it.copy(assignment = before.assignment, pool = before.pool,
                        submitError = e.toUserMessage("Could not save your rating"))
                }
            }
        }
    }

    /** Back one level: form → answers → questions. Returns false at the top. */
    fun back(): Boolean = when (_uiState.value.step) {
        PeerReviewStep.FORM -> {
            _uiState.update {
                it.copy(step = PeerReviewStep.ANSWERS, assignment = null, submitError = null)
            }
            true
        }
        PeerReviewStep.ANSWERS -> { loadQuestions(); true }
        PeerReviewStep.QUESTIONS -> false
    }

    companion object { const val MAX_AREAS = 3 }

    fun setVerdict(v: String) { _uiState.update { it.copy(verdict = v) } }
    fun setRating(r: Int)     { _uiState.update { it.copy(rating = r) } }
    /** Toggle an area on/off; at most MAX_AREAS may be selected. */
    fun toggleImprovementArea(a: String) {
        _uiState.update {
            val cur = it.improvementAreas
            it.copy(improvementAreas = when {
                a in cur              -> cur - a
                cur.size >= MAX_AREAS -> cur          // full — ignore
                else                  -> cur + a
            })
        }
    }
    fun setSuggestion(s: String) { _uiState.update { it.copy(suggestion = s.take(200)) } }

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
                        improvementAreas = s.improvementAreas.toList().ifEmpty { null },
                        improvementArea = s.improvementAreas.firstOrNull(),
                        suggestion = s.suggestion.trim().ifBlank { null },
                    )
                )
                val data = resp.data
                val msg = resp.message.ifBlank { "Review submitted! +1 credit" }
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        justSubmittedMessage = msg,
                        reviewsDoneThisSession = it.reviewsDoneThisSession + 1,
                        // Reciprocity paid off — offer to open my own reviews
                        unlockedQuestionId = if (data?.unlockedMyReviews == true)
                            data.questionId.ifBlank { s.question?.id ?: "" } else null,
                        unlockedReviewCount = data?.myReviewCount ?: 0,
                        lowReputation = data?.lowReputation ?: false,
                    )
                }
                if ((data?.coinsEarned ?: 0) > 0) bus.emit(RefreshEvent.CoinsChanged)
                // Back to the answers for this question, refreshed (the one
                // just reviewed drops out of the pool).
                s.question?.let { q -> selectQuestion(q) } ?: loadQuestions()
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

    fun clearUnlockPrompt() {
        _uiState.update { it.copy(unlockedQuestionId = null, unlockedReviewCount = 0) }
    }
}
