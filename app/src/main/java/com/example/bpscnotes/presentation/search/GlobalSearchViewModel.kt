package com.example.bpscnotes.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.SearchApiService
import com.example.bpscnotes.data.remote.api.SearchResultData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val query: String = "",
    val results: SearchResultData? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    private val api: SearchApiService,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q, error = null) }
        searchJob?.cancel()
        if (q.isBlank()) {
            _state.update { it.copy(results = null, isLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // debounce
            _state.update { it.copy(isLoading = true) }
            try {
                val res = api.search(q = q)
                _state.update { it.copy(isLoading = false, results = res.data) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
