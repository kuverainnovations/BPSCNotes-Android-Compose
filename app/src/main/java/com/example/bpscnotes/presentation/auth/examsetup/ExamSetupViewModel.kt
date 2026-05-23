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

data class PrepLevel(
    val id: String,
    val label: String,
    val subtitle: String,
    val emoji: String
)

val PREP_LEVELS = listOf(
    PrepLevel("beginner",      "Beginner",      "Just started preparing",       "🌱"),
    PrepLevel("intermediate",  "Intermediate",  "Preparing for 6–12 months",    "📘"),
    PrepLevel("advanced",      "Advanced",      "Preparing for 1+ years",       "🎯"),
)

data class ExamSetupUiState(
    val exams: List<ExamDto>         = emptyList(),
    val isLoadingExams: Boolean      = true,
    val examsError: String?          = null,

    val currentStep: ExamSetupStep   = ExamSetupStep.SELECT_PRIMARY,
    val selectedPrimary: ExamDto?    = null,
    val selectedSecondary: List<ExamDto> = emptyList(),
    val selectedPrepLevel: PrepLevel?= null,

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
        _uiState.update { it.copy(currentStep = ExamSetupStep.SELECT_SECONDARY) }
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
        _uiState.update { it.copy(currentStep = ExamSetupStep.SELECT_PREP_LEVEL) }
    }

    fun selectPrepLevel(level: PrepLevel) {
        _uiState.update { it.copy(selectedPrepLevel = level) }
    }

    // ── Final save ───────────────────────────────────────────

    fun saveAndFinish() {
        val state    = _uiState.value
        val primary  = state.selectedPrimary ?: return
        val prepLevel = state.selectedPrepLevel ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                authApi.saveExamTarget(
                    ExamTargetRequest(
                        primaryExam = primary.name,
                        secondaryExam = state.selectedSecondary.firstOrNull()?.name,
                        prepLevel = prepLevel.id
                    )
                )
                // Save locally so SplashScreen knows setup is done
                tokenStore.setExamSetupDone()
                tokenStore.saveUserPrimaryExam(primary.name)
                tokenStore.saveUserPrepLevel(prepLevel.id)

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