package com.example.bpscnotes.presentation.jobvacancies

import com.example.bpscnotes.core.network.toUserMessage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.api.JobVacancyDto
import com.example.bpscnotes.data.remote.api.JobsApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobVacanciesUiState(
    val jobs: List<JobVacancyDto>   = emptyList(),
    val isLoading: Boolean          = true,
    val error: String?              = null,
    // Job alert category subscriptions — persisted locally via TokenStore
    val alertCategories: Set<String> = emptySet()
)

@HiltViewModel
class JobVacanciesViewModel @Inject constructor(
    private val api: JobsApiService,
    private val bus: RefreshEventBus,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobVacanciesUiState())
    val uiState: StateFlow<JobVacanciesUiState> = _uiState.asStateFlow()

    init { load()
        // Load persisted alert preferences so the toggle reflects saved state
        // every time the sheet is opened — fixes "toggle not saving".
        _uiState.update { it.copy(alertCategories = tokenStore.getJobAlertCategories()) }
        // ── Refresh on bus events ─────────────────────────────
        viewModelScope.launch {
            bus.events.collect { event ->
                when (event) {
                    is RefreshEvent.CoinsChanged -> load()
                    else -> {}
                }
            }
        }
    }

    fun toggleAlertCategory(label: String) {
        val current = _uiState.value.alertCategories
        val updated = if (current.contains(label)) current - label else current + label
        tokenStore.setJobAlertCategories(updated)
        _uiState.update { it.copy(alertCategories = updated) }
    }

    fun load(category: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.jobs.isEmpty(), error = null) }
            try {
                val response = api.getJobs(category = category, limit = 50)
                _uiState.update { it.copy(jobs = response.data?.jobs ?: emptyList(), isLoading = false) }
            } catch (e: Exception) {
                Log.e("JobsVM", e.toUserMessage(""), e)
                _uiState.update { it.copy(isLoading = false, error = e.toUserMessage("Failed to load jobs")) }
            }
        }
    }

    fun toggleSave(jobId: String) {
        viewModelScope.launch {
            try {

                api.toggleSaveJob(jobId)

                _uiState.update { state ->
                    state.copy(
                        jobs = state.jobs.map { job ->
                            if (job.id == jobId) {
                                job.copy(
                                    isSaved = !(job.isSaved ?: false)
                                )
                            } else job
                        }
                    )
                }

            } catch (e: Exception) {
                Log.e("JobsVM", "Save job failed", e)
            }
        }
    }

    fun retry() = load()
}