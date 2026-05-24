package com.example.bpscnotes.presentation.auth.examsetup

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
    SELECT_PREP_LEVEL,  // Step 3: Your preparation level
    ALL_SET             // Final: Ready to go! (triggers navigation)
}

data class ExamSetupUiState(
    val exams: List<ExamDto>         = emptyList(),
    val isLoadingExams: Boolean      = true,
    val examsError: String?          = null,

    val currentStep: ExamSetupStep   = ExamSetupStep.SELECT_PRIMARY,
    val selectedPrimary: ExamDto?    = null,
    val selectedSecondary: List<ExamDto> = emptyList(),
    // Prep level removed — not used

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
                        examsError     = e.message ?: "Failed to load exams"
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
        _uiState.update { it.copy(currentStep = ExamSetupStep.SELECT_PREP_LEVEL) }
    }

    fun deselectPrimaryExam() {
        // Allow user to deselect primary and re-choose
        _uiState.update { it.copy(selectedPrimary = null) }
    }

    fun toggleSecondaryExam(exam: ExamDto) {
        _uiState.update { state ->
            val current = state.selectedSecondary.toMutableList()
            // Cannot select primary exam as secondary
            if (exam.name == state.selectedPrimary?.name) return@update state
            if (current.any { it.name == exam.name }) {
                current.removeAll { it.name == exam.name }
            } else if (current.size < 3) {   // max 3 secondary exams
                current.add(exam)
            }
            state.copy(selectedSecondary = current)
        }
    }

    fun proceedFromSecondary() {
        // Not used anymore — primary and secondary on same screen
        _uiState.update { it.copy(currentStep = ExamSetupStep.SELECT_PREP_LEVEL) }
    }

    fun goBackToExamSelection() {
        _uiState.update { it.copy(currentStep = ExamSetupStep.SELECT_PRIMARY) }
    }

    // ── Final save ───────────────────────────────────────────

    fun saveAndFinish(targetYear: Int = 2026) {
        val state    = _uiState.value
        val primary  = state.selectedPrimary ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                authApi.saveExamTarget(
                    ExamTargetRequest(
                        primaryExam   = primary.name,
                        secondaryExam = state.selectedSecondary.firstOrNull()?.name,
                        targetYear    = targetYear
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
                        saveError  = e.message ?: "Failed to save. Please try again."
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