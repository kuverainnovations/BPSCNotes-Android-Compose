package com.example.bpscnotes.presentation.jobvacancies

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.core.network.toUserMessage
import com.example.bpscnotes.data.remote.api.JobVacancyDto
import com.example.bpscnotes.data.remote.api.JobsApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobDetailUiState(
    val job: JobVacancyDto? = null,
    val isLoading: Boolean  = true,
    val error: String?      = null,
)

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val api: JobsApiService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val jobId: String = checkNotNull(savedStateHandle["jobId"])

    private val _uiState = MutableStateFlow(JobDetailUiState())
    val uiState: StateFlow<JobDetailUiState> = _uiState.asStateFlow()

    companion object { private const val TAG = "JobDetailVM" }

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val res = api.getJob(jobId)
                _uiState.update { it.copy(job = res.data?.job, isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "load: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.toUserMessage("Failed to load job details")) }
            }
        }
    }

    fun toggleSave() {
        val job = _uiState.value.job ?: return
        viewModelScope.launch {
            try {
                api.toggleSaveJob(job.id)
                _uiState.update { it.copy(job = it.job?.copy(isSaved = !job.isSaved)) }
            } catch (e: Exception) {
                Log.e(TAG, "toggleSave: ${e.message}", e)
            }
        }
    }
}
