package com.example.bpscnotes.presentation.notebook

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.core.network.toUserMessage
import com.example.bpscnotes.data.remote.api.CreateNoteRequest
import com.example.bpscnotes.data.remote.api.NoteBlockDto
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.util.UUID
import javax.inject.Inject

enum class NoteSort { Recent, Oldest, TitleAZ }

// Block types the editor supports (mirror BLOCK_TYPES in the backend).
enum class BlockType { Heading, Text, Bullet, Numbered, Check, Image }

// An in-editor block with a stable local id (for Compose keys + targeting
// operations). Maps to/from the API NoteBlockDto on load/save.
data class EditableBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: BlockType = BlockType.Text,
    val text: String = "",
    val done: Boolean = false,
    val url: String? = null,
    val uploading: Boolean = false,   // image being uploaded
)

private fun BlockType.wire(): String = when (this) {
    BlockType.Heading -> "heading"; BlockType.Text -> "text"; BlockType.Bullet -> "bullet"
    BlockType.Numbered -> "numbered"; BlockType.Check -> "check"; BlockType.Image -> "image"
}
private fun blockTypeFromWire(s: String?): BlockType = when (s) {
    "heading" -> BlockType.Heading; "bullet" -> BlockType.Bullet; "numbered" -> BlockType.Numbered
    "check" -> BlockType.Check; "image" -> BlockType.Image; else -> BlockType.Text
}

data class NotebookUiState(
    val notes: List<NoteDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val search: String = "",
    val subjectFilter: String? = null,
    val sort: NoteSort = NoteSort.Recent,
    // ── Editor (null editingNoteId + null editor = closed) ──
    val editorOpen: Boolean = false,
    val editingNoteId: String? = null,   // null = new note
    val editorTitle: String = "",
    val editorColor: String? = null,
    val editorSubject: String? = null,
    val editorBlocks: List<EditableBlock> = emptyList(),
    val isSaving: Boolean = false,
) {
    val subjects: List<String>
        get() = notes.mapNotNull { it.subject?.takeIf { s -> s.isNotBlank() } }.distinct().sorted()

    val visibleNotes: List<NoteDto>
        get() {
            val filtered = if (subjectFilter == null) notes else notes.filter { it.subject == subjectFilter }
            val cmp = when (sort) {
                NoteSort.Recent  -> compareByDescending<NoteDto> { it.updatedAt }
                NoteSort.Oldest  -> compareBy { it.updatedAt }
                NoteSort.TitleAZ -> compareBy { it.title.lowercase().ifBlank { "￿" } }
            }
            return filtered.sortedWith(compareByDescending<NoteDto> { it.isPinned }.then(cmp))
        }
}

@HiltViewModel
class NotebookViewModel @Inject constructor(
    private val api: NotebookApiService,
    private val app: Application,
) : ViewModel() {

    private val _state = MutableStateFlow(NotebookUiState())
    val state: StateFlow<NotebookUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init { load() }

    // showLoader=false keeps the notes list (and the search field) on screen
    // while refreshing, so a search-triggered reload never disposes the
    // TextField — otherwise the keyboard closes on every keystroke.
    fun load(showLoader: Boolean = true) {
        viewModelScope.launch {
            if (showLoader) _state.update { it.copy(isLoading = true, error = null) }
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
        searchJob = viewModelScope.launch { delay(400); load(showLoader = false) }
    }

    fun setSubjectFilter(subject: String?) = _state.update { it.copy(subjectFilter = subject) }
    fun setSort(sort: NoteSort)            = _state.update { it.copy(sort = sort) }

    // ── Editor open/close ──────────────────────────────────────

    fun openNewNote() = _state.update {
        it.copy(
            editorOpen = true, editingNoteId = null,
            editorTitle = "", editorColor = null, editorSubject = null,
            editorBlocks = listOf(EditableBlock(type = BlockType.Text)),
        )
    }

    fun openNote(note: NoteDto) = _state.update {
        // Prefer structured blocks; fall back to a single text block holding
        // the plain content for legacy / mock-solution notes.
        val blocks = note.blocks?.takeIf { b -> b.isNotEmpty() }?.map { b ->
            EditableBlock(type = blockTypeFromWire(b.type), text = b.text, done = b.done, url = b.url)
        } ?: listOf(EditableBlock(type = BlockType.Text, text = note.content))
        it.copy(
            editorOpen = true, editingNoteId = note.id,
            editorTitle = note.title, editorColor = note.color, editorSubject = note.subject,
            editorBlocks = blocks,
        )
    }

    fun closeEditor() = _state.update {
        it.copy(editorOpen = false, editingNoteId = null, editorBlocks = emptyList())
    }

    fun setEditorTitle(v: String)   = _state.update { it.copy(editorTitle = v.take(200)) }
    fun setEditorColor(v: String?)  = _state.update { it.copy(editorColor = v) }
    fun setEditorSubject(v: String?) = _state.update { it.copy(editorSubject = v) }

    // ── Block operations ───────────────────────────────────────

    private fun updateBlocks(transform: (List<EditableBlock>) -> List<EditableBlock>) =
        _state.update { it.copy(editorBlocks = transform(it.editorBlocks)) }

    fun setBlockText(id: String, text: String) =
        updateBlocks { list -> list.map { if (it.id == id) it.copy(text = text) else it } }

    fun toggleCheck(id: String) =
        updateBlocks { list -> list.map { if (it.id == id) it.copy(done = !it.done) else it } }

    fun setBlockType(id: String, type: BlockType) =
        updateBlocks { list -> list.map { if (it.id == id) it.copy(type = type) else it } }

    /** Add a new empty block right after [afterId] (or at the end). */
    fun addBlock(afterId: String?, type: BlockType = BlockType.Text) {
        val fresh = EditableBlock(type = type)
        updateBlocks { list ->
            val idx = list.indexOfFirst { it.id == afterId }
            if (idx < 0) list + fresh else list.toMutableList().apply { add(idx + 1, fresh) }
        }
    }

    fun deleteBlock(id: String) = updateBlocks { list ->
        val filtered = list.filterNot { it.id == id }
        filtered.ifEmpty { listOf(EditableBlock(type = BlockType.Text)) }  // never fully empty
    }

    fun moveBlock(id: String, up: Boolean) = updateBlocks { list ->
        val idx = list.indexOfFirst { it.id == id }
        val target = if (up) idx - 1 else idx + 1
        if (idx < 0 || target < 0 || target >= list.size) list
        else list.toMutableList().apply { add(target, removeAt(idx)) }
    }

    /** Pick → upload → insert an image block after [afterId]. */
    fun addImage(afterId: String?, uri: android.net.Uri) {
        val placeholder = EditableBlock(type = BlockType.Image, uploading = true)
        updateBlocks { list ->
            val idx = list.indexOfFirst { it.id == afterId }
            if (idx < 0) list + placeholder else list.toMutableList().apply { add(idx + 1, placeholder) }
        }
        viewModelScope.launch {
            try {
                val bytes = if (uri.scheme == "file") java.io.File(uri.path!!).readBytes()
                    else app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw Exception("Cannot read image")
                val mime = if (uri.scheme == "file") "image/jpeg"
                    else app.contentResolver.getType(uri) ?: "image/jpeg"
                val ext  = if (mime.contains("png")) "png" else "jpg"
                val body = RequestBody.create(mime.toMediaTypeOrNull(), bytes)
                val part = MultipartBody.Part.createFormData("image", "note.$ext", body)
                val url  = api.uploadImage(part).data?.url
                if (url.isNullOrBlank()) throw Exception("No URL returned")
                updateBlocks { list -> list.map { if (it.id == placeholder.id) it.copy(url = url, uploading = false) else it } }
            } catch (e: Exception) {
                // Drop the placeholder and surface the error
                updateBlocks { list -> list.filterNot { it.id == placeholder.id } }
                _state.update { it.copy(error = e.toUserMessage("Image upload failed")) }
            }
        }
    }

    // ── Save / delete note ─────────────────────────────────────

    fun saveNote() {
        val s = _state.value
        val blocks = s.editorBlocks
        // Trim trailing empty text blocks so a stray blank line doesn't count.
        val meaningful = blocks.filterNot { it.type == BlockType.Image && it.url.isNullOrBlank() }
        val hasContent = s.editorTitle.isNotBlank() ||
            meaningful.any { (it.type == BlockType.Image && !it.url.isNullOrBlank()) || it.text.isNotBlank() }
        if (!hasContent) { closeEditor(); return }
        if (meaningful.any { it.uploading }) {
            _state.update { it.copy(error = "Please wait for the image to finish uploading.") }
            return
        }

        val wireBlocks = meaningful.map {
            NoteBlockDto(type = it.type.wire(), text = it.text, done = it.done, url = it.url)
        }
        val content = flattenToText(meaningful)

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                if (s.editingNoteId == null) {
                    api.createNote(CreateNoteRequest(s.editorTitle.trim(), content, s.editorColor, s.editorSubject, wireBlocks))
                } else {
                    api.updateNote(s.editingNoteId, UpdateNoteRequest(
                        title = s.editorTitle.trim(), content = content,
                        color = s.editorColor, subject = s.editorSubject ?: "", blocks = wireBlocks,
                    ))
                }
                _state.update { it.copy(isSaving = false, editorOpen = false, editingNoteId = null, editorBlocks = emptyList()) }
                load()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.toUserMessage("Could not save note")) }
            }
        }
    }

    fun deleteEditingNote() {
        val id = _state.value.editingNoteId ?: run { closeEditor(); return }
        viewModelScope.launch {
            try {
                api.deleteNote(id)
                _state.update { s -> s.copy(editorOpen = false, editingNoteId = null, editorBlocks = emptyList(), notes = s.notes.filterNot { it.id == id }) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toUserMessage("Could not delete note")) }
            }
        }
    }

    fun togglePin(note: NoteDto) {
        viewModelScope.launch {
            _state.update { s ->
                s.copy(notes = s.notes.map { if (it.id == note.id) it.copy(isPinned = !it.isPinned) else it }
                    .sortedWith(compareByDescending<NoteDto> { it.isPinned }.thenByDescending { it.updatedAt }))
            }
            try { api.updateNote(note.id, UpdateNoteRequest(isPinned = !note.isPinned)) }
            catch (_: Exception) { load() }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    /** Plain-text version of a note for the system share sheet. */
    fun shareText(note: NoteDto): String {
        val body = note.blocks?.takeIf { it.isNotEmpty() }
            ?.let { flattenDtoToText(it) } ?: note.content
        return buildString {
            if (note.title.isNotBlank()) appendLine(note.title).appendLine()
            append(body)
            appendLine().appendLine()
            append("— my BPSCNotes notebook")
        }
    }

    private fun flattenToText(blocks: List<EditableBlock>): String = blocks.joinToString("\n") { b ->
        when (b.type) {
            BlockType.Bullet   -> "• ${b.text}"
            BlockType.Numbered -> "- ${b.text}"
            BlockType.Check    -> "${if (b.done) "[x]" else "[ ]"} ${b.text}"
            BlockType.Image    -> "[image]"
            else               -> b.text
        }
    }.trim()

    private fun flattenDtoToText(blocks: List<NoteBlockDto>): String = blocks.joinToString("\n") { b ->
        when (b.type) {
            "bullet"   -> "• ${b.text}"
            "numbered" -> "- ${b.text}"
            "check"    -> "${if (b.done) "[x]" else "[ ]"} ${b.text}"
            "image"    -> "[image]"
            else       -> b.text
        }
    }.trim()
}
