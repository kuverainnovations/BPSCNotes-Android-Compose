package com.example.bpscnotes.presentation.activerecall

import com.example.bpscnotes.core.network.toUserMessage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.local.Sm2CardDao
import com.example.bpscnotes.data.local.Sm2CardEntity
import com.example.bpscnotes.data.local.applyRating
import com.example.bpscnotes.data.local.isDueToday
import com.example.bpscnotes.data.remote.api.CoinsApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ActiveRecallUiState(
    val allCards:    List<CoinsApiService.FlashcardDto> = emptyList(),
    val isLoading:   Boolean           = true,
    val error:       String?           = null,
    val masteredIds: Set<String>       = emptySet(),
    val weakIds:     Set<String>       = emptySet(),
    // SM-2 scheduling data keyed by card ID
    val sm2Schedule: Map<String, Sm2CardEntity> = emptyMap(),
    // Notification subject subscriptions (e.g. "all", "Polity", "History")
    val notifTopics: Set<String>       = emptySet()
)

@HiltViewModel
class ActiveRecallViewModel @Inject constructor(
    private val api: CoinsApiService.FlashcardsApiService,
    private val bus: RefreshEventBus,
    private val sm2Dao: Sm2CardDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveRecallUiState())
    val uiState: StateFlow<ActiveRecallUiState> = _uiState.asStateFlow()

    init {
        loadAll()
        loadProgress()
        loadNotifPrefs()
        loadSm2Schedule()

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

    private fun loadSm2Schedule() {
        viewModelScope.launch {
            val schedule = withContext(Dispatchers.IO) {
                sm2Dao.getAll().associateBy { it.cardId }
            }
            _uiState.update { it.copy(sm2Schedule = schedule) }
        }
    }

    /** Returns cards due for review today according to SM-2. Falls back to all cards if no schedule data. */
    fun dueCardsForSubject(subject: String): List<CoinsApiService.FlashcardDto> {
        val cards = cardsForSubject(subject)
        val schedule = _uiState.value.sm2Schedule
        if (schedule.isEmpty()) return cards
        return cards.filter { card ->
            val entry = schedule[card.id] ?: return@filter true  // never reviewed = due
            entry.isDueToday()
        }
    }

    /** SM-2 rating. quality: 0=Again, 1=Hard, 2=Good, 3=Easy */
    fun sm2Rate(cardId: String, quality: Int) {
        viewModelScope.launch {
            val existing = withContext(Dispatchers.IO) { sm2Dao.get(cardId) }
                ?: Sm2CardEntity(cardId = cardId)
            val updated = existing.applyRating(quality)
            withContext(Dispatchers.IO) { sm2Dao.upsert(updated) }
            _uiState.update { state ->
                state.copy(sm2Schedule = state.sm2Schedule + (cardId to updated))
            }
            // Also sync the simple backend rating for continuity
            val backendRating = when (quality) { 0 -> "weak"; 3 -> "mastered"; else -> "mastered" }
            saveProgressToBackend(cardId, backendRating, updated.repetitions)
        }
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.allCards.isEmpty(), error = null) }
            try {
                val response = api.getFlashcards(limit = 200)
                val cards    = response.data?.flashcards ?: emptyList()
                _uiState.update { it.copy(allCards = cards, isLoading = false) }
            } catch (e: Exception) {
                Log.e("ActiveRecallVM", e.toUserMessage(""), e)
                _uiState.update { it.copy(isLoading = false, error = e.toUserMessage("Failed to load flashcards")) }
            }
        }
    }

    private fun loadProgress() {
        viewModelScope.launch {
            try {
                val res = api.getProgress()
                val data = res.data ?: return@launch
                _uiState.update { it.copy(
                    masteredIds = data.mastered.toSet(),
                    weakIds     = data.weak.toSet()
                )}
            } catch (e: Exception) {
                Log.w("ActiveRecallVM", "Progress load failed: ${e.message}")
            }
        }
    }

    // ── Notification prefs ─────────────────────────────────────
    private fun loadNotifPrefs() {
        viewModelScope.launch {
            try {
                val res = api.getNotifPrefs()
                val topics = res.data?.subscribed?.toSet() ?: return@launch
                _uiState.update { it.copy(notifTopics = topics) }
            } catch (e: Exception) {
                Log.w("ActiveRecallVM", "Notif prefs load failed: ${e.message}")
            }
        }
    }

    /**
     * Toggle a notification topic (subject name or "all").
     * Syncs to server immediately so admin can target users by subject.
     */
    fun toggleNotifTopic(topic: String) {
        val current = _uiState.value.notifTopics
        val updated = if (current.contains(topic)) current - topic else current + topic
        _uiState.update { it.copy(notifTopics = updated) }
        viewModelScope.launch {
            try {
                api.syncNotifPrefs(mapOf("topics" to updated.toList()))
            } catch (e: Exception) {
                Log.w("ActiveRecallVM", "syncNotifPrefs: ${e.message}")
            }
        }
    }

    fun cardsForSubject(subject: String): List<CoinsApiService.FlashcardDto> {
        val all = _uiState.value.allCards
        return if (subject == "All") all else all.filter { it.subject == subject }
    }

    fun subjects(): List<String> = listOf("All")

    fun markMastered(id: String, streak: Int = 0) {
        _uiState.update { it.copy(masteredIds = it.masteredIds + id, weakIds = it.weakIds - id) }
        saveProgressToBackend(id, "mastered", streak)
    }

    fun markWeak(id: String) {
        _uiState.update { it.copy(weakIds = it.weakIds + id, masteredIds = it.masteredIds - id) }
        saveProgressToBackend(id, "weak", 0)
    }

    fun markSkipped(id: String) { Log.d("ActiveRecallVM", "Skipped: $id") }

    private fun saveProgressToBackend(flashcardId: String, rating: String, streak: Int) {
        viewModelScope.launch {
            try {
                api.saveProgress(CoinsApiService.SaveProgressRequest(flashcardId, rating, streak))
            } catch (e: Exception) {
                Log.w("ActiveRecallVM", "Progress save failed: ${e.message}")
            }
        }
    }

    fun retry() { loadAll(); loadProgress() }
}