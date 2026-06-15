package com.example.bpscnotes.presentation.auth.examsetup

import com.example.bpscnotes.core.network.toUserMessage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.CoinsApiService
import com.example.bpscnotes.data.remote.api.ExamDto
import com.example.bpscnotes.data.remote.api.ExamTargetRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.toMutableList

// ── Steps in the exam setup flow ──────────────────────────────
enum class ExamSetupStep {
    SELECT_PRIMARY,     // Step 1: Choose your primary exam
    SELECT_SECONDARY,   // Step 2: Add more exams (optional)
    REVIEW_PLAN,        // Step 2: Confirm exam selection before finishing setup
    ALL_SET             // Final: Ready to go! (triggers navigation)
}

// Maximum number of secondary exams a user can add alongside their
// primary exam. Surfaced in the UI (counter + limit message) so the
// limit isn't a silent no-op when tapping a 4th exam.
const val MAX_SECONDARY_EXAMS = 3

data class ExamSetupUiState(
    val exams: List<ExamDto>         = emptyList(),
    val isLoadingExams: Boolean      = true,
    val examsError: String?          = null,

    val currentStep: ExamSetupStep   = ExamSetupStep.SELECT_PRIMARY,
    val selectedPrimary: ExamDto?    = null,
    val selectedSecondary: List<ExamDto> = emptyList(),
    // One-shot message shown via Snackbar when the user tries to add
    // more than MAX_SECONDARY_EXAMS secondary exams. Cleared by the
    // UI after showing (see clearSecondaryLimitMessage()).
    val secondaryLimitMessage: String? = null,

    val isSaving: Boolean            = false,
    val saveError: String?           = null,
    val isDone: Boolean              = false    // navigate to Main when true
)

@HiltViewModel
class ExamSetupViewModel @Inject constructor(
    private val authApi: AuthApiService,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamSetupUiState())
    val uiState: StateFlow<ExamSetupUiState> = _uiState.asStateFlow()

    init { loadExams() }

    // ── Load exams from API ──────────────────────────────────
    fun loadExams() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingExams = true, examsError = null) }
            try {
                val response = authApi.getExams()
                _uiState.update {
                    it.copy(
                        exams          = response.data?.exams ?: emptyList(),
                        isLoadingExams = false
                    )
                }
            } catch (e: Exception) {
                Log.e("ExamSetupVM", "loadExams: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoadingExams = false,
                        examsError     = e.toUserMessage("Failed to load exams")
                    )
                }
            }
        }
    }

    // ── Step navigation ──────────────────────────────────────

    fun selectPrimaryExam(exam: ExamDto) {
        _uiState.update { it.copy(selectedPrimary = exam) }
    }

    fun proceedFromPrimary() {
        if (_uiState.value.selectedPrimary == null) return
        // FIX: Skip SELECT_SECONDARY (merged into same screen) → go straight to target year
        _uiState.update { it.copy(currentStep = ExamSetupStep.REVIEW_PLAN) }
    }

    fun deselectPrimaryExam() {
        // Allow user to deselect their primary and re-choose. If they
        // already have secondary exams selected, promote the first one
        // to primary instead of leaving "no primary" — this keeps the
        // selection summary and the Next button in a sensible state,
        // and matches what most users expect ("my #1 choice just
        // changed, not 'I have no exam at all now'"). The user can
        // still tap the newly-promoted primary to deselect it too.
        _uiState.update { state ->
            val promoted = state.selectedSecondary.firstOrNull()
            if (promoted != null) {
                state.copy(
                    selectedPrimary   = promoted,
                    selectedSecondary = state.selectedSecondary.drop(1)
                )
            } else {
                state.copy(selectedPrimary = null)
            }
        }
    }

    fun toggleSecondaryExam(exam: ExamDto) {
        _uiState.update { state ->
            val current = state.selectedSecondary.toMutableList()
            // Cannot select primary exam as secondary
            if (exam.name == state.selectedPrimary?.name) return@update state
            if (current.any { it.name == exam.name }) {
                current.removeAll { it.name == exam.name }
            } else if (current.size < MAX_SECONDARY_EXAMS) {
                current.add(exam)
            } else {
                // Limit reached — let the UI show a Snackbar instead of
                // silently ignoring the tap.
                return@update state.copy(
                    secondaryLimitMessage =
                        "You can add up to $MAX_SECONDARY_EXAMS additional exams. Remove one to add a different exam."
                )
            }
            state.copy(selectedSecondary = current)
        }
    }

    fun clearSecondaryLimitMessage() {
        _uiState.update { it.copy(secondaryLimitMessage = null) }
    }

    fun proceedFromSecondary() {
        // Not used anymore — primary and secondary on same screen
        _uiState.update { it.copy(currentStep = ExamSetupStep.REVIEW_PLAN) }
    }

    fun goBackToExamSelection() {
        _uiState.update { it.copy(currentStep = ExamSetupStep.SELECT_PRIMARY) }
    }

    // ── Final save ───────────────────────────────────────────

    fun saveAndFinish() {
        val state    = _uiState.value
        val primary  = state.selectedPrimary ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                authApi.saveExamTarget(
                    ExamTargetRequest(
                        primaryExam   = primary.name,
                        secondaryExam = state.selectedSecondary.firstOrNull()?.name
                    )
                )
                // Save locally so SplashScreen knows setup is done
                tokenStore.setExamSetupDone()
                tokenStore.saveUserPrimaryExam(primary.name)
                // prep level no longer saved

                _uiState.update { it.copy(isSaving = false, isDone = true) }
            } catch (e: Exception) {
                Log.e("ExamSetupVM", "saveAndFinish: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isSaving   = false,
                        saveError  = e.toUserMessage("Failed to save. Please try again.")
                    )
                }
            }
        }
    }

    // ── Skip (user can skip) ──────────────────────────────────
    fun skip() {
        tokenStore.setExamSetupDone()
        _uiState.update { it.copy(isDone = true) }
    }
}