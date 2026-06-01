package com.example.bpscnotes.presentation.currentaffairs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.CurrentAffairsApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
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
                _bookmarkedIds.value = serverBookmarked

                val articles = serverList.map { dto ->
                    dto.toUiModel(isBookmarked = serverBookmarked.contains(dto.id))
                }

                _uiState.update { it.copy(allArticles = articles, articles = articles, isLoading = false) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error     = e.message ?: "Failed to load current affairs"
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

    fun loadMcqs(affairId: String) {
        viewModelScope.launch {
            _mcqLoading.value = true
            _mcqs.value = emptyList()
            try {
                val data = api.getMcqs(affairId).data
                _mcqs.value = data?.mcqs ?: emptyList()
            } catch (e: Exception) {
                _mcqs.value = emptyList()
            }
            _mcqLoading.value = false
        }
    }
}