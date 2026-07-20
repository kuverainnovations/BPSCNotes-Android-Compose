package com.example.bpscnotes.presentation.notebook

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.NoteBlockDto
import com.example.bpscnotes.data.remote.api.NoteDto
import java.text.SimpleDateFormat
import java.util.Locale

// Subject chips offered in the editor — aligned with the app's standard
// subject taxonomy (live-classes, quizzes).
private val NoteSubjects = listOf(
    "Polity", "History", "Geography", "Economy", "Science",
    "Environment", "Bihar GK", "Current Affairs",
)

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

    BackHandler(enabled = state.editorOpen) { viewModel.closeEditor() }

    Scaffold(
        containerColor = BpscColors.Surface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("My Notebook", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.editorOpen) viewModel.closeEditor() else nav.popBackStack()
                    }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                actions = {
                    if (!state.editorOpen) {
                        var sortMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, "Sort")
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            listOf(
                                NoteSort.Recent to "Recently updated",
                                NoteSort.Oldest to "Oldest first",
                                NoteSort.TitleAZ to "Title A–Z",
                            ).forEach { (mode, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            label,
                                            fontWeight = if (state.sort == mode) FontWeight.Bold else FontWeight.Normal,
                                            color = if (state.sort == mode) BpscColors.Primary else Color.Unspecified,
                                        )
                                    },
                                    onClick = { viewModel.setSort(mode); sortMenu = false },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BpscColors.Surface),
            )
        },
        floatingActionButton = {
            if (!state.editorOpen) {
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
                state.editorOpen -> BlockEditor(
                    state = state,
                    viewModel = viewModel,
                    onShare = { note ->
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, viewModel.shareText(note))
                        }
                        nav.context.startActivity(android.content.Intent.createChooser(intent, "Share note"))
                    },
                )

                state.isLoading -> AppLoader()

                else -> NotesList(
                    notes = state.visibleNotes,
                    search = state.search,
                    subjects = state.subjects,
                    subjectFilter = state.subjectFilter,
                    onFilter = viewModel::setSubjectFilter,
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
    subjects: List<String>,
    subjectFilter: String?,
    onFilter: (String?) -> Unit,
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

        if (subjects.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = subjectFilter == null, onClick = { onFilter(null) }, label = { Text("All") })
                }
                items(subjects) { s ->
                    FilterChip(
                        selected = subjectFilter == s,
                        onClick = { onFilter(if (subjectFilter == s) null else s) },
                        label = { Text(s) },
                    )
                }
            }
        }
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
                        "Tap + to jot formulas, checklists,\nimages, answers to revise — anything.",
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
                modifier = Modifier.size(18.dp).clip(CircleShape).clickable(onClick = onTogglePin),
            )
        }

        // Block-aware preview (falls back to plain content for legacy notes)
        val blocks = note.blocks
        if (!blocks.isNullOrEmpty()) {
            Spacer(Modifier.height(6.dp))
            blocks.take(6).forEach { b -> NoteCardBlockLine(b) }
            if (blocks.size > 6) {
                Text("…", style = MaterialTheme.typography.bodySmall, color = BpscColors.TextHint)
            }
        } else if (note.content.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                note.content,
                style = MaterialTheme.typography.bodySmall,
                color = BpscColors.TextSecondary,
                maxLines = 6, overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatNoteDate(note.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = BpscColors.TextHint,
                modifier = Modifier.weight(1f),
            )
            note.subject?.takeIf { it.isNotBlank() }?.let { subject ->
                Text(
                    subject,
                    style = MaterialTheme.typography.labelSmall,
                    color = BpscColors.Primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun NoteCardBlockLine(b: NoteBlockDto) {
    when (b.type) {
        "image" -> if (!b.url.isNullOrBlank()) {
            // Card preview: cap the height so one tall image can't dominate
            // the card, but Fit (not Crop) so the whole image is still visible.
            AsyncImage(
                model = b.url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(vertical = 2.dp),
            )
        }
        "heading" -> Text(
            b.text, style = MaterialTheme.typography.labelLarge,
            color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        "check" -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (b.done) "☑ " else "☐ ", style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary)
            Text(
                b.text,
                style = MaterialTheme.typography.bodySmall,
                color = BpscColors.TextSecondary,
                textDecoration = if (b.done) TextDecoration.LineThrough else null,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        "bullet", "numbered" -> Text(
            "• ${b.text}",
            style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        else -> if (b.text.isNotBlank()) Text(
            b.text, style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Block editor
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockEditor(
    state: NotebookUiState,
    viewModel: NotebookViewModel,
    onShare: (NoteDto) -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var focusedId by remember(state.editingNoteId) { mutableStateOf<String?>(null) }
    val subjectChips = remember(state.editingNoteId) {
        (NoteSubjects + listOfNotNull(state.editorSubject?.takeIf { it.isNotBlank() && it !in NoteSubjects })).distinct()
    }
    // Where a new block/image is inserted: after the focused block, else end.
    fun anchorId(): String? = focusedId ?: state.editorBlocks.lastOrNull()?.id

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.addImage(anchorId(), uri)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 8.dp),
        ) {
            // Title
            item {
                OutlinedTextField(
                    value = state.editorTitle,
                    onValueChange = viewModel::setEditorTitle,
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
            }

            // Color chips
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    NoteColors.forEach { (name, bg) ->
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(bg)
                                .clickable { viewModel.setEditorColor(if (state.editorColor == name) null else name) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (state.editorColor == name) Text("✓", color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Subject chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subjectChips) { s ->
                        FilterChip(
                            selected = state.editorSubject == s,
                            onClick = { viewModel.setEditorSubject(if (state.editorSubject == s) null else s) },
                            label = { Text(s, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = BpscColors.Divider)
                Spacer(Modifier.height(4.dp))
            }

            // Blocks
            itemsIndexed(state.editorBlocks, key = { _, b -> b.id }) { index, block ->
                BlockRow(
                    block = block,
                    numberLabel = if (block.type == BlockType.Numbered)
                        "${state.editorBlocks.take(index + 1).count { it.type == BlockType.Numbered }}." else null,
                    onFocused = { focusedId = block.id },
                    onText = { viewModel.setBlockText(block.id, it) },
                    onToggleCheck = { viewModel.toggleCheck(block.id) },
                    onTypeChange = { viewModel.setBlockType(block.id, it) },
                    onMoveUp = { viewModel.moveBlock(block.id, up = true) },
                    onMoveDown = { viewModel.moveBlock(block.id, up = false) },
                    onDelete = { viewModel.deleteBlock(block.id) },
                )
            }
        }

        // Add-block toolbar
        Surface(color = Color.White, shadowElevation = 4.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { AddChip("¶ Text")   { viewModel.addBlock(anchorId(), BlockType.Text) } }
                    item { AddChip("H Heading") { viewModel.addBlock(anchorId(), BlockType.Heading) } }
                    item { AddChip("• Bullet")  { viewModel.addBlock(anchorId(), BlockType.Bullet) } }
                    item { AddChip("1. List")   { viewModel.addBlock(anchorId(), BlockType.Numbered) } }
                    item { AddChip("☑ Checklist") { viewModel.addBlock(anchorId(), BlockType.Check) } }
                    item { AddChip("🖼 Image")  { imagePicker.launch("image/*") } }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.editingNoteId != null) {
                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                        ) { Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp)) }
                        OutlinedButton(
                            onClick = {
                                onShare(NoteDto(
                                    title = state.editorTitle,
                                    blocks = state.editorBlocks.map {
                                        NoteBlockDto(it.type.name.lowercase(), it.text, it.done, it.url)
                                    },
                                ))
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BpscColors.Primary),
                        ) { Icon(Icons.Rounded.Share, null, Modifier.size(18.dp)) }
                    }
                    Button(
                        onClick = { viewModel.saveNote() },
                        enabled = !state.isSaving,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        if (state.isSaving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this note?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.deleteEditingNote() }) {
                    Text("Delete", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AddChip(label: String, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockRow(
    block: EditableBlock,
    numberLabel: String?,
    onFocused: () -> Unit,
    onText: (String) -> Unit,
    onToggleCheck: () -> Unit,
    onTypeChange: (BlockType) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    // Image block: thumbnail + remove; no text field.
    if (block.type == BlockType.Image) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (block.uploading || block.url.isNullOrBlank()) {
                Box(
                    Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp))
                        .background(BpscColors.Surface),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(Modifier.size(22.dp), color = BpscColors.Primary, strokeWidth = 2.dp) }
            } else {
                Box(Modifier.weight(1f)) {
                    // FillWidth + no fixed height: the whole image shows,
                    // scaled to the note width at its natural aspect ratio
                    // (Crop was chopping tall diagrams / handwritten photos).
                    // Same pattern as the peer-review answer photos.
                    AsyncImage(
                        model = block.url,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BpscColors.Divider, RoundedCornerShape(12.dp)),
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                            .size(28.dp).clip(CircleShape).background(Color.Black.copy(0.45f)),
                    ) { Icon(Icons.Rounded.Close, "Remove image", tint = Color.White, modifier = Modifier.size(16.dp)) }
                }
            }
        }
        return
    }

    val heading = block.type == BlockType.Heading
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        // Leading marker / control
        when (block.type) {
            BlockType.Bullet   -> Text("•  ", color = BpscColors.TextSecondary, fontWeight = FontWeight.Bold)
            BlockType.Numbered -> Text("${numberLabel ?: "•"}  ", color = BpscColors.TextSecondary, fontWeight = FontWeight.Bold)
            BlockType.Check    -> Checkbox(checked = block.done, onCheckedChange = { onToggleCheck() }, modifier = Modifier.size(36.dp))
            else -> {}
        }

        TextField(
            value = block.text,
            onValueChange = onText,
            placeholder = {
                Text(
                    when (block.type) {
                        BlockType.Heading -> "Heading"
                        BlockType.Bullet, BlockType.Numbered -> "List item"
                        BlockType.Check -> "To-do"
                        else -> "Write…"
                    },
                    color = BpscColors.TextHint,
                )
            },
            textStyle = if (heading)
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodyMedium.copy(
                textDecoration = if (block.type == BlockType.Check && block.done) TextDecoration.LineThrough else null,
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.weight(1f)
                .onFocusChanged { if (it.isFocused) onFocused() },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )

        // Per-block menu
        var menu by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.MoreVert, "Block options", tint = BpscColors.TextHint, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                Text("Turn into", style = MaterialTheme.typography.labelSmall,
                    color = BpscColors.TextHint, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                listOf(
                    BlockType.Text to "Text", BlockType.Heading to "Heading",
                    BlockType.Bullet to "Bullet", BlockType.Numbered to "Numbered", BlockType.Check to "Checklist",
                ).forEach { (t, label) ->
                    DropdownMenuItem(
                        text = { Text(label, fontWeight = if (block.type == t) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onTypeChange(t); menu = false },
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(text = { Text("Move up") }, onClick = { onMoveUp(); menu = false })
                DropdownMenuItem(text = { Text("Move down") }, onClick = { onMoveDown(); menu = false })
                DropdownMenuItem(
                    text = { Text("Delete", color = Color(0xFFD32F2F)) },
                    onClick = { onDelete(); menu = false },
                )
            }
        }
    }
}
