package com.example.bpscnotes.presentation.notebook

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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

    // First-launch intro — explains what the Notebook is for (saving intros,
    // conclusions, key data to reuse in Mains answers). Shown once, tracked
    // in a tiny SharedPreferences flag (QA 20-07).
    val context = androidx.compose.ui.platform.LocalContext.current
    var showIntro by remember {
        mutableStateOf(
            !context.getSharedPreferences("notebook_prefs", android.content.Context.MODE_PRIVATE)
                .getBoolean("intro_shown", false)
        )
    }
    if (showIntro) {
        AlertDialog(
            onDismissRequest = {
                context.getSharedPreferences("notebook_prefs", android.content.Context.MODE_PRIVATE)
                    .edit().putBoolean("intro_shown", true).apply()
                showIntro = false
            },
            icon = { Text("📓", fontSize = 34.sp) },
            title = { Text("Your study notebook", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    "Save your introductions, conclusions, and key data (facts, figures, quotes) " +
                    "here — then reuse them in future Mains answers. Add headings, bullet lists, " +
                    "checklists and images to keep everything organised.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        context.getSharedPreferences("notebook_prefs", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean("intro_shown", true).apply()
                        showIntro = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                ) { Text("Got it", fontWeight = FontWeight.Bold) }
            },
        )
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
                    requestFocus = state.pendingFocusId == block.id,
                    focusCaret = if (state.pendingFocusId == block.id) state.pendingFocusCaret else null,
                    onFocused = { focusedId = block.id },
                    onFocusHandled = { viewModel.clearPendingFocus() },
                    onText = { viewModel.setBlockText(block.id, it) },
                    // Enter and Backspace are handled as real key presses, not
                    // by sniffing the text for '\n' — that double-fired and
                    // spawned extra bullets.
                    onEnter = { before, after -> viewModel.onEnterAt(block.id, before, after) },
                    onBackspaceAtStart = { viewModel.onBackspaceAtStart(block.id) },
                    onToggleCheck = { viewModel.toggleCheck(block.id) },
                    onDeleteImage = { viewModel.deleteBlock(block.id) },
                )
            }
        }

        // Add-block toolbar
        Surface(color = Color.White, shadowElevation = 4.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Tapping a type on an empty line converts that line; on a
                    // line with text it starts a new block below. So a single
                    // tap never leaves you with two empty bullets.
                    item { AddChip("¶ Text")   { viewModel.toolbarBlock(anchorId(), BlockType.Text) } }
                    item { AddChip("H Heading") { viewModel.toolbarBlock(anchorId(), BlockType.Heading) } }
                    item { AddChip("• Bullet")  { viewModel.toolbarBlock(anchorId(), BlockType.Bullet) } }
                    item { AddChip("1. List")   { viewModel.toolbarBlock(anchorId(), BlockType.Numbered) } }
                    item { AddChip("☑ Checklist") { viewModel.toolbarBlock(anchorId(), BlockType.Check) } }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BlockRow(
    block: EditableBlock,
    numberLabel: String?,
    requestFocus: Boolean,
    focusCaret: Int?,
    onFocused: () -> Unit,
    onFocusHandled: () -> Unit,
    onText: (String) -> Unit,
    onEnter: (String, String) -> Unit,  // text before / after the split point
    onBackspaceAtStart: () -> Unit,     // Backspace at offset 0
    onToggleCheck: () -> Unit,
    onDeleteImage: () -> Unit,
) {
    // Image block: thumbnail + remove; no text field, no row menu.
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
                    AsyncImage(
                        model = block.url,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BpscColors.Divider, RoundedCornerShape(12.dp)),
                    )
                    IconButton(
                        onClick = onDeleteImage,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                            .size(28.dp).clip(CircleShape).background(Color.Black.copy(0.45f)),
                    ) { Icon(Icons.Rounded.Close, "Remove image", tint = Color.White, modifier = Modifier.size(16.dp)) }
                }
            }
        }
        return
    }

    val heading = block.type == BlockType.Heading

    // Local editing value carries the caret position, which we need to split on
    // Enter and to detect Backspace at the very start of a block.
    var tfv by remember(block.id) {
        mutableStateOf(TextFieldValue(block.text, TextRange(block.text.length)))
    }
    // Reflect external text changes (Enter split trims this block to "before",
    // a merge grows the previous block) without clobbering the caret mid-typing.
    LaunchedEffect(block.text) {
        if (block.text != tfv.text) tfv = TextFieldValue(block.text, TextRange(block.text.length))
    }

    val focusRequester = remember { FocusRequester() }
    // Pull the row above the keyboard when it takes focus, so a new line on a
    // full page isn't hidden behind the keyboard.
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            val t = block.text
            val caret = (focusCaret ?: t.length).coerceIn(0, t.length)
            tfv = TextFieldValue(t, TextRange(caret))
            runCatching { focusRequester.requestFocus() }
            onFocusHandled()
        }
    }

    // No per-row menu any more: Enter makes the next block, Backspace at the
    // start removes the marker then merges up, and the toolbar changes type.
    Row(
        Modifier.fillMaxWidth().padding(vertical = 1.dp).bringIntoViewRequester(bringIntoView),
        verticalAlignment = Alignment.Top,
    ) {
        // Leading marker / control, top-aligned so it sits on the first line.
        when (block.type) {
            BlockType.Bullet   -> Text("•  ", color = BpscColors.TextSecondary, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp))
            BlockType.Numbered -> Text("${numberLabel ?: "•"}  ", color = BpscColors.TextSecondary, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp))
            BlockType.Check    -> Checkbox(checked = block.done, onCheckedChange = { onToggleCheck() },
                modifier = Modifier.size(30.dp))
            else -> {}
        }

        val textStyle = (if (heading)
            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        else MaterialTheme.typography.bodyMedium).copy(
            color = BpscColors.TextPrimary,
            textDecoration = if (block.type == BlockType.Check && block.done) TextDecoration.LineThrough else null,
        )
        BasicTextField(
            value = tfv,
            onValueChange = { v ->
                // Enter arrives as a committed newline on Android soft keyboards
                // (not as a key event), so detect it here: split at the newline
                // and DON'T push it into the block. This is the single Enter
                // path — one edit, one new block, so no more double bullets.
                val nl = v.text.indexOf('\n')
                if (nl >= 0) {
                    // Split around the newline; use the field's own text so an
                    // autocorrect-on-Enter can't desync us from the block.
                    onEnter(v.text.substring(0, nl), v.text.substring(nl + 1))
                } else {
                    tfv = v
                    if (v.text != block.text) onText(v.text)
                }
            },
            textStyle = textStyle,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            cursorBrush = SolidColor(BpscColors.Primary),
            modifier = Modifier.weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (it.isFocused) {
                        onFocused()
                        scope.launch { runCatching { bringIntoView.bringIntoView() } }
                    }
                }
                // Backspace at the very start doesn't change the text (nothing
                // before the caret), so onValueChange never sees it — catch it
                // as a key event to drop the marker / merge with the block above.
                .onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown &&
                        e.key == Key.Backspace &&
                        tfv.selection.collapsed && tfv.selection.start == 0
                    ) { onBackspaceAtStart(); true } else false
                },
            decorationBox = { inner ->
                Box(Modifier.padding(vertical = 4.dp)) {
                    if (tfv.text.isEmpty()) {
                        Text(
                            when (block.type) {
                                BlockType.Heading -> "Heading"
                                BlockType.Bullet, BlockType.Numbered -> "List item"
                                BlockType.Check -> "To-do"
                                else -> "Write something…"
                            },
                            style = textStyle.copy(color = BpscColors.TextHint),
                        )
                    }
                    inner()
                }
            },
        )
    }
}
