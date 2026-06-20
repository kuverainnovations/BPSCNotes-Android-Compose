package com.example.bpscnotes.presentation.currentaffairs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.CurrentAffairsApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import com.example.bpscnotes.core.network.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CurrentAffairsUiState(
    val allArticles: List<CAArticle> = emptyList(),   // full unfiltered list — for category chips
    val articles: List<CAArticle> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CurrentAffairsViewModel @Inject constructor(
    private val api: CurrentAffairsApiService,
    private val bus: RefreshEventBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurrentAffairsUiState())
    val uiState: StateFlow<CurrentAffairsUiState> = _uiState.asStateFlow()

    // Local bookmark state — toggled optimistically and synced with API
    private val _bookmarkedIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedIds: StateFlow<Set<String>> = _bookmarkedIds.asStateFlow()

    init {
        loadArticles()

        // ── Refresh on bus events ─────────────────────────────
        viewModelScope.launch {
            bus.events.collect { event ->
                when (event) {
                    is RefreshEvent.CoinsChanged -> refresh()
                    else -> {}
                }
            }
        }
    }

    fun loadArticles(category: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.articles.isEmpty(), error = null) }
            try {
                // Always fetch ALL articles — no server-side category filter
                // Categories are filtered locally so chips never disappear
                val response  = api.getAffairs(limit = 60, category = null)
                val serverList = response.data?.affairs ?: emptyList()

                val serverBookmarked = serverList.filter { it.isBookmarked }.map { it.id }.toSet()
                // Merge: keep any locally-toggled bookmarks even if cache returns stale data
                val mergedBookmarked = serverBookmarked + _bookmarkedIds.value
                _bookmarkedIds.value = mergedBookmarked

                val articles = serverList.map { dto ->
                    dto.toUiModel(isBookmarked = mergedBookmarked.contains(dto.id))
                }

                _uiState.update { it.copy(allArticles = articles, articles = articles, isLoading = false) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error     = e.toUserMessage("Failed to load current affairs")
                    )
                }
            }
        }
    }

    /**
     * Optimistic bookmark toggle:
     * 1. Update local state immediately (instant UI feedback)
     * 2. Call API in background
     * 3. Revert if API fails
     */
    fun toggleBookmark(id: String) {
        val wasBookmarked = _bookmarkedIds.value.contains(id)

        // Optimistic update
        _bookmarkedIds.update { current ->
            if (wasBookmarked) current - id else current + id
        }
        // Also update the article list so the card redraws correctly
        _uiState.update { state ->
            state.copy(
                articles = state.articles.map { article ->
                    if (article.id == id) article.copy(isBookmarked = !wasBookmarked) else article
                }
            )
        }

        // Background API call
        viewModelScope.launch {
            try {
                api.toggleBookmark(id)
            } catch (e: Exception) {
                // Revert on failure
                _bookmarkedIds.update { current ->
                    if (wasBookmarked) current + id else current - id
                }
                _uiState.update { state ->
                    state.copy(
                        articles = state.articles.map { article ->
                            if (article.id == id) article.copy(isBookmarked = wasBookmarked) else article
                        }
                    )
                }
            }
        }
    }

    fun refresh() = loadArticles()

    fun clearError() { _uiState.update { it.copy(error = null) } }

    // ── MCQs ─────────────────────────────────────────────────────
    private val _mcqs = MutableStateFlow<List<com.example.bpscnotes.data.remote.api.CaMcqDto>>(emptyList())
    val mcqs: StateFlow<List<com.example.bpscnotes.data.remote.api.CaMcqDto>> = _mcqs.asStateFlow()

    private val _mcqLoading = MutableStateFlow(false)
    val mcqLoading: StateFlow<Boolean> = _mcqLoading.asStateFlow()

    private val _mcqError = MutableStateFlow<String?>(null)
    val mcqError: StateFlow<String?> = _mcqError.asStateFlow()

    private val _mcqAnswers = MutableStateFlow<Map<String, CaMcqAnswerDto>>(emptyMap())
    val mcqAnswers: StateFlow<Map<String, CaMcqAnswerDto>> = _mcqAnswers.asStateFlow()

    fun loadMcqs(affairId: String) {
        viewModelScope.launch {
            _mcqLoading.value = true
            _mcqError.value   = null
            _mcqs.value       = emptyList()
            _mcqAnswers.value = emptyMap()
            try {
                val data = api.getMcqs(affairId).data
                _mcqs.value = data?.mcqs ?: emptyList()
            } catch (e: Exception) {
                _mcqError.value = e.toUserMessage("Failed to load questions")
                _mcqs.value = emptyList()
            }
            _mcqLoading.value = false
        }
    }

    /** After user taps — resolve correct answer from already-loaded mcqs */
    fun fetchMcqAnswer(mcqId: String) {
        val mcq = _mcqs.value.find { it.id == mcqId } ?: return
        val answer = CaMcqAnswerDto(correct = mcq.correct, explanation = mcq.explanation)
        _mcqAnswers.update { it + (mcqId to answer) }
    }

    fun clearMcqError() { _mcqError.value = null }

    /** Called by TrackStudyTime — logs time silently in background */
    fun logStudyTime(activityType: String, durationSecs: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                api.logActivity(
                    com.example.bpscnotes.data.remote.api.LogActivityRequest(
                        activityType = activityType,
                        durationSecs = durationSecs
                    )
                )
            } catch (_: Exception) { /* silent — tracking must never crash the app */ }
        }
    }
}