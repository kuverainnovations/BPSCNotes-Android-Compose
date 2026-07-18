package com.example.bpscnotes.presentation.notebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.core.network.toUserMessage
import com.example.bpscnotes.data.remote.api.CreateNoteRequest
import com.example.bpscnotes.data.remote.api.NoteDto
import com.example.bpscnotes.data.remote.api.NotebookApiService
import com.example.bpscnotes.data.remote.api.UpdateNoteRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotebookUiState(
    val notes: List<NoteDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val search: String = "",
    // Editor: null = closed; blank id = new note being composed
    val editingNote: NoteDto? = null,
    val isSaving: Boolean = false,
)

@HiltViewModel
class NotebookViewModel @Inject constructor(
    private val api: NotebookApiService,
) : ViewModel() {

    private val _state = MutableStateFlow(NotebookUiState())
    val state: StateFlow<NotebookUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val notes = api.getNotes(_state.value.search.ifBlank { null }).data?.notes ?: emptyList()
                _state.update { it.copy(isLoading = false, notes = notes) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.toUserMessage("Could not load notes")) }
            }
        }
    }

    fun onSearchChange(value: String) {
        _state.update { it.copy(search = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)                      // debounce while typing
            load()
        }
    }

    // ── Editor ────────────────────────────────────────────────

    fun openNewNote()          = _state.update { it.copy(editingNote = NoteDto()) }
    fun openNote(note: NoteDto) = _state.update { it.copy(editingNote = note) }
    fun closeEditor()          = _state.update { it.copy(editingNote = null) }

    fun saveNote(title: String, content: String, color: String?) {
        val editing = _state.value.editingNote ?: return
        if (title.isBlank() && content.isBlank()) { closeEditor(); return }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                if (editing.id.isBlank()) {
                    api.createNote(CreateNoteRequest(title.trim(), content, color))
                } else {
                    api.updateNote(editing.id, UpdateNoteRequest(title.trim(), content, color))
                }
                _state.update { it.copy(isSaving = false, editingNote = null) }
                load()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.toUserMessage("Could not save note")) }
            }
        }
    }

    fun togglePin(note: NoteDto) {
        viewModelScope.launch {
            // Optimistic flip so the list reorders instantly
            _state.update { s ->
                s.copy(notes = s.notes.map { if (it.id == note.id) it.copy(isPinned = !it.isPinned) else it }
                    .sortedWith(compareByDescending<NoteDto> { it.isPinned }.thenByDescending { it.updatedAt }))
            }
            try {
                api.updateNote(note.id, UpdateNoteRequest(isPinned = !note.isPinned))
            } catch (_: Exception) {
                load()                       // revert to server truth
            }
        }
    }

    fun deleteNote(note: NoteDto) {
        viewModelScope.launch {
            try {
                api.deleteNote(note.id)
                _state.update { s -> s.copy(editingNote = null, notes = s.notes.filterNot { it.id == note.id }) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toUserMessage("Could not delete note")) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
