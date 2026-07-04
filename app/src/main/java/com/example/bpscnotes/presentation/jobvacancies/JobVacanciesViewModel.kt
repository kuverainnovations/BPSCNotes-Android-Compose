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
    val jobs: List<JobVacancyDto>    = emptyList(),
    val isLoading: Boolean           = true,
    val error: String?               = null,
    // Job alert category subscriptions — persisted locally and synced to server
    val alertCategories: Set<String> = emptySet()
)

@HiltViewModel
class JobVacanciesViewModel @Inject constructor(
    private val api:        JobsApiService,
    private val bus:        RefreshEventBus,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobVacanciesUiState())
    val uiState: StateFlow<JobVacanciesUiState> = _uiState.asStateFlow()

    companion object { private const val TAG = "JobsVM" }

    init {
        // Seed from local prefs immediately so UI is responsive
        _uiState.update { it.copy(alertCategories = tokenStore.getJobAlertCategories()) }
        load()
        // Then refresh from server — source of truth
        loadAlertPrefsFromServer()

        viewModelScope.launch {
            bus.events.collect { event ->
                when (event) {
                    is RefreshEvent.CoinsChanged -> load()
                    else -> {}
                }
            }
        }
    }

    // ── Load alert prefs from server ──────────────────────────
    // Local prefs record the user's intent; the server is the push-delivery
    // target. If the server comes back EMPTY while we hold local selections
    // (a failed earlier sync, a backend redeploy that lost the table, an old
    // backend build without the endpoint), re-push local instead of adopting
    // the empty list — adopting it is how enabled alerts kept "un-saving"
    // themselves after reopening the page (QA issue 13, regressed twice).
    private fun loadAlertPrefsFromServer() {
        viewModelScope.launch {
            try {
                val res = api.getAlertPrefs()
                val server = res.data?.subscribed?.toSet() ?: return@launch
                val local  = tokenStore.getJobAlertCategories()
                if (server.isEmpty() && local.isNotEmpty()) {
                    try {
                        api.syncAlertPrefs(mapOf("categories" to local.toList()))
                        Log.i(TAG, "loadAlertPrefs: server empty, re-pushed ${local.size} local prefs")
                    } catch (e: Exception) {
                        Log.w(TAG, "loadAlertPrefs re-push failed: ${e.message}")
                    }
                    // Either way keep showing the user's local selections.
                    return@launch
                }
                tokenStore.setJobAlertCategories(server)
                _uiState.update { it.copy(alertCategories = server) }
            } catch (e: Exception) {
                Log.w(TAG, "loadAlertPrefs: ${e.message}")
                // Keep local prefs — network may be unavailable
            }
        }
    }

    // ── Toggle alert category ─────────────────────────────────
    // Updates local prefs immediately, then syncs to server.
    // Server uses the subscribed list to target FCM pushes.
    fun toggleAlertCategory(label: String) {
        val current = _uiState.value.alertCategories
        val updated = if (current.contains(label)) current - label else current + label
        tokenStore.setJobAlertCategories(updated)
        _uiState.update { it.copy(alertCategories = updated) }
        // Sync to server (fire-and-forget — local prefs are the fast path)
        viewModelScope.launch {
            try {
                api.syncAlertPrefs(mapOf("categories" to updated.toList()))
            } catch (e: Exception) {
                Log.w(TAG, "syncAlertPrefs: ${e.message}")
                // Already persisted locally; will sync next time
            }
        }
    }

    // ── Load jobs ─────────────────────────────────────────────
    fun load(category: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.jobs.isEmpty(), error = null) }
            try {
                val response = api.getJobs(category = category, limit = 50)
                _uiState.update { it.copy(jobs = response.data?.jobs ?: emptyList(), isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, e.toUserMessage(""), e)
                _uiState.update { it.copy(isLoading = false, error = e.toUserMessage("Failed to load jobs")) }
            }
        }
    }

    // ── Toggle save ───────────────────────────────────────────
    // Optimistic flip, then the explicit idempotent endpoint for the
    // *intended* end state (POST=save, DELETE=unsave). The old blind
    // toggle endpoint flipped on server state, so drift made saves
    // land backwards (QA issue 14). Reverts on failure.
    fun toggleSave(jobId: String) {
        val wasSaved = _uiState.value.jobs.firstOrNull { it.id == jobId }?.isSaved == true
        val setSaved = { saved: Boolean ->
            _uiState.update { state ->
                state.copy(jobs = state.jobs.map { job ->
                    if (job.id == jobId) job.copy(isSaved = saved) else job
                })
            }
        }
        setSaved(!wasSaved)
        viewModelScope.launch {
            try {
                if (wasSaved) api.unsaveJob(jobId) else api.saveJob(jobId)
            } catch (e: Exception) {
                Log.e(TAG, "toggleSave failed", e)
                setSaved(wasSaved)   // revert optimistic update
            }
        }
    }

    fun retry() = load()
}