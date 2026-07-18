package com.example.bpscnotes.presentation.notebook

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.NoteDto
import java.text.SimpleDateFormat
import java.util.Locale

// Palette names must match NOTE_COLORS in backend notebook.module.ts
private val NoteColors = listOf(
    "yellow" to Color(0xFFFFF8E1),
    "blue"   to Color(0xFFE3F2FD),
    "green"  to Color(0xFFE8F5E9),
    "pink"   to Color(0xFFFCE4EC),
    "purple" to Color(0xFFEDE7F6),
    "orange" to Color(0xFFFFF3E0),
)

private fun noteBg(color: String?): Color =
    NoteColors.firstOrNull { it.first == color }?.second ?: Color.White

private fun formatNoteDate(iso: String): String = try {
    val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(iso.substringBefore('.').removeSuffix("Z"))
    SimpleDateFormat("d MMM yyyy", Locale.US).format(parsed!!)
} catch (_: Exception) { "" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookScreen(nav: NavController, viewModel: NotebookViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    // Editor is an in-screen overlay; hardware back closes it first.
    BackHandler(enabled = state.editingNote != null) { viewModel.closeEditor() }

    Scaffold(
        containerColor = BpscColors.Surface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("My Notebook", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.editingNote != null) viewModel.closeEditor() else nav.popBackStack()
                    }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BpscColors.Surface),
            )
        },
        floatingActionButton = {
            if (state.editingNote == null) {
                FloatingActionButton(
                    onClick = { viewModel.openNewNote() },
                    containerColor = BpscColors.Primary,
                    contentColor = Color.White,
                ) { Icon(Icons.Rounded.Add, "New note") }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.editingNote != null -> NoteEditor(
                    note = state.editingNote!!,
                    isSaving = state.isSaving,
                    onSave = { t, c, col -> viewModel.saveNote(t, c, col) },
                    onDelete = { viewModel.deleteNote(it) },
                )

                state.isLoading -> AppLoader()

                else -> NotesList(
                    notes = state.notes,
                    search = state.search,
                    onSearch = viewModel::onSearchChange,
                    onOpen = viewModel::openNote,
                    onTogglePin = viewModel::togglePin,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// List
// ─────────────────────────────────────────────────────────────
@Composable
private fun NotesList(
    notes: List<NoteDto>,
    search: String,
    onSearch: (String) -> Unit,
    onOpen: (NoteDto) -> Unit,
    onTogglePin: (NoteDto) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = search,
            onValueChange = onSearch,
            placeholder = { Text("Search notes…", color = BpscColors.TextHint) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = BpscColors.TextHint) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = BpscColors.Primary,
                unfocusedBorderColor = BpscColors.Divider,
            ),
        )
        Spacer(Modifier.height(12.dp))

        if (notes.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("📝", fontSize = 44.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    if (search.isBlank()) "No notes yet" else "No notes match your search",
                    style = MaterialTheme.typography.titleMedium,
                    color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold,
                )
                if (search.isBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap + to jot down formulas, dates,\nanswers to revise — anything.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BpscColors.TextSecondary,
                    )
                }
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp,
                contentPadding = PaddingValues(bottom = 90.dp),
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(note, onClick = { onOpen(note) }, onTogglePin = { onTogglePin(note) })
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: NoteDto, onClick: () -> Unit, onTogglePin: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(noteBg(note.color))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                note.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleSmall,
                color = BpscColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Rounded.PushPin, if (note.isPinned) "Unpin" else "Pin",
                tint = if (note.isPinned) BpscColors.Accent else BpscColors.TextHint.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onTogglePin),
            )
        }
        if (note.content.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                note.content,
                style = MaterialTheme.typography.bodySmall,
                color = BpscColors.TextSecondary,
                maxLines = 6, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            formatNoteDate(note.updatedAt),
            style = MaterialTheme.typography.labelSmall,
            color = BpscColors.TextHint,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Editor (in-screen overlay)
// ─────────────────────────────────────────────────────────────
@Composable
private fun NoteEditor(
    note: NoteDto,
    isSaving: Boolean,
    onSave: (title: String, content: String, color: String?) -> Unit,
    onDelete: (NoteDto) -> Unit,
) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var content by remember(note.id) { mutableStateOf(note.content) }
    var color by remember(note.id) { mutableStateOf(note.color) }
    var confirmDelete by remember { mutableStateOf(false) }

    // imePadding: the app is edge-to-edge, so adjustResize alone doesn't
    // shrink the window — without this the keyboard covers the Save row
    // and taps land on the IME, which reads as "save not working".
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(200) },
            placeholder = { Text("Title", color = BpscColors.TextHint) },
            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                focusedBorderColor = BpscColors.Primary, unfocusedBorderColor = BpscColors.Divider,
            ),
        )
        Spacer(Modifier.height(10.dp))

        // Color chips
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            NoteColors.forEach { (name, bg) ->
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .then(
                            if (color == name) Modifier.background(Color.Transparent).padding(0.dp) else Modifier
                        )
                        .clickable { color = if (color == name) null else name },
                    contentAlignment = Alignment.Center,
                ) {
                    if (color == name) Text("✓", color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            placeholder = { Text("Write your note…", color = BpscColors.TextHint) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                focusedBorderColor = BpscColors.Primary, unfocusedBorderColor = BpscColors.Divider,
            ),
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (note.id.isNotBlank()) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                ) { Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Delete") }
            }
            Button(
                onClick = { onSave(title, content, color) },
                enabled = !isSaving && (title.isNotBlank() || content.isNotBlank()),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Save", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this note?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(note) }) {
                    Text("Delete", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
