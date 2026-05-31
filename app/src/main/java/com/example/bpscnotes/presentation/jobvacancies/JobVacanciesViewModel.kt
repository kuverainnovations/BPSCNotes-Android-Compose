package com.example.bpscnotes.presentation.jobvacancies

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val error: String?              = null
)

@HiltViewModel
class JobVacanciesViewModel @Inject constructor(
    private val api: JobsApiService,
    private val bus: RefreshEventBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobVacanciesUiState())
    val uiState: StateFlow<JobVacanciesUiState> = _uiState.asStateFlow()

    init { load()
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

    fun load(category: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.jobs.isEmpty(), error = null) }
            try {
                val response = api.getJobs(category = category, limit = 50)
                _uiState.update { it.copy(jobs = response.data?.jobs ?: emptyList(), isLoading = false) }
            } catch (e: Exception) {
                Log.e("JobsVM", e.message ?: "", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load jobs") }
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