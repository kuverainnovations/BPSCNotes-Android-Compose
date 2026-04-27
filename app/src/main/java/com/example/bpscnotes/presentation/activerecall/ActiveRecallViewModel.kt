package com.example.bpscnotes.presentation.activerecall

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.CoinsApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActiveRecallUiState(
    val allCards: List<CoinsApiService.FlashcardDto>       = emptyList(),
    val isLoading: Boolean                 = true,
    val error: String?                     = null,
    // Session-level mastery (survives subject switches within the same ViewModel scope)
    val masteredIds: Set<String>           = emptySet(),
    val weakIds: Set<String>               = emptySet()
)

@HiltViewModel
class ActiveRecallViewModel @Inject constructor(
    private val api: CoinsApiService.FlashcardsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveRecallUiState())
    val uiState: StateFlow<ActiveRecallUiState> = _uiState.asStateFlow()

    init { loadAll() }

    /** Load all flashcards up-front so subject filtering is instant (no per-subject API calls). */
    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.allCards.isEmpty(), error = null) }
            try {
                val response = api.getFlashcards(limit = 200)
                val cards    = response.data?.flashcards ?: emptyList()
                _uiState.update { it.copy(allCards = cards, isLoading = false) }
            } catch (e: Exception) {
                Log.e("ActiveRecallVM", e.message ?: "", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load flashcards") }
            }
        }
    }

    /** Returns flashcards for the selected subject (or all if "All"). */
    fun cardsForSubject(subject: String): List<CoinsApiService.FlashcardDto> {
        val all = _uiState.value.allCards
        return if (subject == "All") all else all.filter { it.subject == subject }
    }

    /** Distinct list of subjects from loaded cards, always starting with "All". */
    fun subjects(): List<String> =
        listOf("All") + _uiState.value.allCards.map { it.subject }.distinct().sorted()

    // ── Session-level mastery (in-memory — survives recomposition) ─────────

    fun markMastered(id: String) {
        _uiState.update {
            it.copy(
                masteredIds = it.masteredIds + id,
                weakIds     = it.weakIds - id
            )
        }
    }

    fun markWeak(id: String) {
        _uiState.update {
            it.copy(
                weakIds     = it.weakIds + id,
                masteredIds = it.masteredIds - id
            )
        }
    }

    fun retry() = loadAll()
}
