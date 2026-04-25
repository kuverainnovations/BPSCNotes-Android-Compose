package com.example.bpscnotes.presentation.currentaffairs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.CurrentAffairsApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CurrentAffairsUiState(
    val articles: List<CAArticle> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CurrentAffairsViewModel @Inject constructor(
    private val api: CurrentAffairsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurrentAffairsUiState())
    val uiState: StateFlow<CurrentAffairsUiState> = _uiState.asStateFlow()

    // Local bookmark state — toggled optimistically and synced with API
    private val _bookmarkedIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedIds: StateFlow<Set<String>> = _bookmarkedIds.asStateFlow()

    init {
        loadArticles()
    }

    fun loadArticles(category: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.articles.isEmpty(), error = null) }
            try {
                val response  = api.getAffairs(limit = 60, category = category?.takeIf { it != "All" })
                val serverList = response.data?.affairs ?: emptyList()

                // Seed bookmarks from server response
                val serverBookmarked = serverList.filter { it.isBookmarked }.map { it.id }.toSet()
                _bookmarkedIds.value = serverBookmarked

                val articles = serverList.map { dto ->
                    dto.toUiModel(isBookmarked = serverBookmarked.contains(dto.id))
                }

                _uiState.update { it.copy(articles = articles, isLoading = false) }

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
}
