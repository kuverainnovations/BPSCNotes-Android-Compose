package com.example.bpscnotes.presentation.auth.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.ExamDto
import com.example.bpscnotes.data.remote.api.ExamTargetRequest
import com.example.bpscnotes.data.remote.api.UpdateProfileRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WizardStep { EXAM, TARGET_YEAR, DAILY_GOAL }

data class OnboardingWizardUiState(
    val exams: List<ExamDto> = emptyList(),
    val isLoadingExams: Boolean = true,
    val step: WizardStep = WizardStep.EXAM,
    val selectedExam: ExamDto? = null,
    val targetYear: Int? = null,
    val dailyGoalMins: Int = 60,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isDone: Boolean = false
)

@HiltViewModel
class OnboardingWizardViewModel @Inject constructor(
    private val authApi: AuthApiService,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingWizardUiState())
    val state: StateFlow<OnboardingWizardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val resp = authApi.getExams()
                _state.update { it.copy(exams = resp.data?.exams ?: emptyList(), isLoadingExams = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingExams = false) }
            }
        }
    }

    fun selectExam(exam: ExamDto) = _state.update { it.copy(selectedExam = exam) }

    fun nextFromExam() {
        if (_state.value.selectedExam == null) return
        _state.update { it.copy(step = WizardStep.TARGET_YEAR) }
    }

    fun setTargetYear(year: Int?) = _state.update { it.copy(targetYear = year) }

    fun nextFromTargetYear() = _state.update { it.copy(step = WizardStep.DAILY_GOAL) }

    fun setDailyGoal(mins: Int) = _state.update { it.copy(dailyGoalMins = mins) }

    fun back() = _state.update {
        it.copy(step = when (it.step) {
            WizardStep.TARGET_YEAR -> WizardStep.EXAM
            WizardStep.DAILY_GOAL  -> WizardStep.TARGET_YEAR
            else                   -> it.step
        })
    }

    fun finish() {
        val s = _state.value
        val exam = s.selectedExam ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                authApi.saveExamTarget(ExamTargetRequest(
                    primaryExam = exam.name,
                    targetYear  = s.targetYear
                ))
                authApi.updateProfile(UpdateProfileRequest(
                    onboardingCompleted = true,
                    dailyGoalMins       = s.dailyGoalMins
                ))
                tokenStore.saveOnboardingCompleted(true)
                tokenStore.saveUserPrimaryExam(exam.name)
                _state.update { it.copy(isSaving = false, isDone = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message ?: "Failed to save. Please try again.") }
            }
        }
    }
}
