package com.example.bpscnotes.presentation.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.BookmarkedQuestionDto
import com.example.bpscnotes.data.remote.api.BookmarksApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookmarksState(
    val questions: List<BookmarkedQuestionDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val bookmarkedIds: Set<String> = emptySet(),
    val page: Int = 1,
    val hasMore: Boolean = true,
)

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val api: BookmarksApiService,
) : ViewModel() {

    private val _state = MutableStateFlow(BookmarksState())
    val state: StateFlow<BookmarksState> = _state.asStateFlow()

    init { loadBookmarks() }

    fun loadBookmarks(refresh: Boolean = false) {
        if (_state.value.isLoading) return
        val page = if (refresh) 1 else _state.value.page
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val res = api.getBookmarks(page = page, limit = 20)
                val items = res.data?.questions ?: emptyList()
                val list = if (refresh) items else _state.value.questions + items
                _state.update {
                    it.copy(
                        isLoading    = false,
                        questions    = list,
                        bookmarkedIds = list.map { q -> q.id }.toSet(),
                        page         = if (items.size < 20) page else page + 1,
                        hasMore      = items.size >= 20,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleBookmark(questionId: String) {
        viewModelScope.launch {
            val wasBookmarked = questionId in _state.value.bookmarkedIds
            // Optimistic update
            _state.update { s ->
                val newIds = if (wasBookmarked) s.bookmarkedIds - questionId else s.bookmarkedIds + questionId
                val newList = if (wasBookmarked) s.questions.filter { it.id != questionId } else s.questions
                s.copy(bookmarkedIds = newIds, questions = newList)
            }
            try {
                api.toggleBookmark(questionId)
            } catch (_: Exception) {
                // Revert on error
                _state.update { s ->
                    val revertedIds = if (wasBookmarked) s.bookmarkedIds + questionId else s.bookmarkedIds - questionId
                    s.copy(bookmarkedIds = revertedIds)
                }
            }
        }
    }
}
