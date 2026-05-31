package com.example.bpscnotes.presentation.activerecall

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.CoinsApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActiveRecallUiState(
    val allCards:    List<CoinsApiService.FlashcardDto> = emptyList(),
    val isLoading:   Boolean           = true,
    val error:       String?           = null,
    // Persisted mastery — loaded from backend on init, saved on every rating
    val masteredIds: Set<String>       = emptySet(),
    val weakIds:     Set<String>       = emptySet(),
    // Session streak (in-memory)
    val sessionStreak: Int             = 0
)

@HiltViewModel
class ActiveRecallViewModel @Inject constructor(
    private val api: CoinsApiService.FlashcardsApiService,
    private val bus: RefreshEventBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveRecallUiState())
    val uiState: StateFlow<ActiveRecallUiState> = _uiState.asStateFlow()

    init {
        loadAll()
        loadProgress()   // FIX: load persisted progress from backend on startup

        // ── Refresh on bus events ─────────────────────────────
        viewModelScope.launch {
            bus.events.collect { event ->
                when (event) {
                    is RefreshEvent.CoinsChanged,
                    is RefreshEvent.LessonCompleted -> loadAll()
                    else -> {}
                }
            }
        }
    }

    /** Load all flashcards (cached in backend for 5 min). */
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

    /** FIX: Load mastered/weak IDs from backend so progress survives app restarts. */
    private fun loadProgress() {
        viewModelScope.launch {
            try {
                val res = api.getProgress()
                val data = res.data ?: return@launch
                _uiState.update { it.copy(
                    masteredIds = data.mastered.toSet(),
                    weakIds     = data.weak.toSet()
                )}
                Log.d("ActiveRecallVM", "Progress loaded: ${data.mastered.size} mastered, ${data.weak.size} weak")
            } catch (e: Exception) {
                // Non-fatal — progress just won't be restored if offline
                Log.w("ActiveRecallVM", "Progress load failed: ${e.message}")
            }
        }
    }

    fun cardsForSubject(subject: String): List<CoinsApiService.FlashcardDto> {
        val all = _uiState.value.allCards
        return if (subject == "All") all else all.filter { it.subject == subject }
    }

    fun subjects(): List<String> =
        listOf("All") + _uiState.value.allCards.map { it.subject }.distinct().sorted()

    // ── Mastery — optimistic update + backend persist ──────────────

    fun markMastered(id: String) {
        _uiState.update { it.copy(
            masteredIds   = it.masteredIds + id,
            weakIds       = it.weakIds - id,
            sessionStreak = it.sessionStreak + 1
        )}
        saveProgressToBackend(id, "mastered", _uiState.value.sessionStreak)
    }

    fun markWeak(id: String) {
        _uiState.update { it.copy(
            weakIds       = it.weakIds + id,
            masteredIds   = it.masteredIds - id,
            sessionStreak = 0   // streak resets on weak
        )}
        saveProgressToBackend(id, "weak", 0)
    }

    fun markSkipped(id: String) {
        // Skipped doesn't change mastery — no backend call needed
        Log.d("ActiveRecallVM", "Skipped: $id")
    }

    private fun saveProgressToBackend(flashcardId: String, rating: String, streak: Int) {
        viewModelScope.launch {
            try {
                api.saveProgress(
                    CoinsApiService.SaveProgressRequest(
                        flashcardId = flashcardId,
                        rating      = rating,
                        streak      = streak
                    )
                )
                Log.d("ActiveRecallVM", "Saved: $flashcardId → $rating (streak=$streak)")
            } catch (e: Exception) {
                Log.w("ActiveRecallVM", "Progress save failed (offline?): ${e.message}")
                // Don't update UI — optimistic update already applied above
            }
        }
    }

    fun retry() { loadAll(); loadProgress() }
}