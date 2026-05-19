package com.example.bpscnotes.presentation.studymaterials

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.*

// ════════════════════════════════════════════════════════════
// FILE: presentation/studymaterials/StudyMaterialsScreen.kt
// Dynamic implementation — all data from API
// UI structure preserved from existing mockup.
// ════════════════════════════════════════════════════════════

@Composable
fun StudyMaterialsScreen(
    navController: NavHostController,
    viewModel:     StudyMaterialsViewModel = hiltViewModel()
) {
    val state       by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val context      = LocalContext.current

    // Show toast messages
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearToast()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }, containerColor = BpscColors.Surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)) { scaffoldPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(scaffoldPadding).background(BpscColors.Surface)) {

            // ── HEADER ──────────────────────────────────────────
            StudyMaterialsHeader(
                stats = state.stats,
                onBack = { navController.popBackStack() },
                onUpload = { viewModel.showUpload() }
            )

            // ── SEARCH + FILTERS ─────────────────────────────────
            SearchAndStats(
                query = state.searchQuery,
                onQueryChange = viewModel::setSearch,
                stats = state.stats,
                bookmarkedCount = state.bookmarkedIds.size,
                showBookmarksOnly = state.showBookmarksOnly,
                onToggleBookmarks = viewModel::toggleBookmarksOnly,
                onUpload = viewModel::showUpload
            )

            // Unified filter bar — type + sort in ONE row (subjects on demand)
            CompactFilterBar(
                selectedType    = state.selectedType,
                selectedSubject = state.selectedSubject,
                subjects        = state.subjects,
                sortBy          = state.sortBy,
                onTypeSelect    = viewModel::selectType,
                onSubjectSelect = viewModel::selectSubject,
                onSortSelect    = viewModel::setSortBy
            )

            // ── PULL-TO-REFRESH CONTENT ─────────────────────────
            val pullRefreshState = rememberPullToRefreshState()

            PullToRefreshBox(
                state = pullRefreshState,
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh() }
            ) {

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        state.isLoadingList && state.materials.isEmpty() -> {
                            LoadingGrid()
                        }

                        state.listError != null && state.materials.isEmpty() -> {
                            ErrorState(message = state.listError!!, onRetry = viewModel::refresh)
                        }

                        state.materials.isEmpty() -> {
                            EmptyState(showBookmarksOnly = state.showBookmarksOnly)
                        }

                        else -> {
                            MaterialsList(
                                state = state,
                                onView = viewModel::openDetail,
                                onBookmark = viewModel::toggleBookmark,
                                onDownload = viewModel::downloadMaterial,
                                onLoadMore = viewModel::loadMore
                            )
                        }
                    }

                }
            }
        }
    }

    // ── DETAIL SHEET ─────────────────────────────────────────────
    if (state.isLoadingDetail) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = BpscColors.Primary)
        }
    }
    state.selectedMaterial?.let { detail ->
        MaterialDetailSheet(
            material       = detail,
            isBookmarked   = state.bookmarkedIds.contains(detail.id),
            isDownloaded   = state.downloadedIds.contains(detail.id),
            isDownloading  = state.downloadingId == detail.id,
            onBookmark     = { viewModel.toggleBookmark(detail.id) },
            onDownload     = {
                val dto = StudyMaterialDto(
                    id = detail.id, title = detail.title, description = detail.description,
                    subject = detail.subject, materialType = detail.materialType,
                    author = detail.author, tags = detail.tags,
                    fileSizeBytes = detail.fileSizeBytes, pageCount = detail.pageCount,
                    isPremium = detail.isPremium, isFeatured = detail.isFeatured,
                    isTrending = detail.isTrending, isNew = detail.isNew,
                    downloadCount = detail.downloadCount, rating = detail.rating,
                    uploadedDate = detail.uploadedDate, uploaderName = detail.uploaderName
                )
                viewModel.downloadMaterial(dto)
            },
            onOpenPdf      = { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            },
            onDismiss      = viewModel::closeDetail
        )
    }

    // ── UPLOAD SHEET ─────────────────────────────────────────────
    if (state.showUploadSheet) {
        UploadSheet(
            isUploading    = state.isUploading,
            uploadProgress = state.uploadProgress,
            uploadError    = state.uploadError,
            onSubmit       = viewModel::uploadMaterial,
            onDismiss      = viewModel::hideUpload
        )
    }
}

// ════════════════════════════════════════════════════════════
// HEADER
// ════════════════════════════════════════════════════════════
@Composable
private fun StudyMaterialsHeader(stats: StatsData?, onBack: () -> Unit, onUpload: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(Brush.linearGradient(
                listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)),
                Offset(0f, 0f), Offset(500f, 500f)
            ))
        // .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 46.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f))
                        .clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Study Materials", style = MaterialTheme.typography.titleLarge,
                            color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Text("Notes, PDFs, PYQs & Books", style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(0.7f))
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// SEARCH + STATS BAR
// ════════════════════════════════════════════════════════════
@Composable
private fun SearchAndStats(
    query: String, onQueryChange: (String) -> Unit,
    stats: StatsData?, bookmarkedCount: Int,
    showBookmarksOnly: Boolean, onToggleBookmarks: () -> Unit, onUpload: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth().background(Color.White)
        .padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // Search bar
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(BpscColors.Surface).border(1.dp, BpscColors.Divider, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Search, null, tint = BpscColors.TextHint, modifier = Modifier.size(18.dp))
            BasicTextField(
                value = query, onValueChange = onQueryChange, modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = BpscColors.TextPrimary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search notes, papers, books...",
                        style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextHint)
                    inner()
                }
            )
            if (query.isNotEmpty()) Icon(Icons.Rounded.Close, null, tint = BpscColors.TextHint,
                modifier = Modifier.size(16.dp).clickable { onQueryChange("") })
        }

        // Stats + upload
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LibSmallStat("📄", "${stats?.pdfs ?: "—"}", "PDFs")
                LibSmallStat("📝", "${stats?.pyqs ?: "—"}", "PYQs")
                LibSmallStat("📚", "${stats?.books ?: "—"}", "Books")
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(if (showBookmarksOnly) BpscColors.CoinGold.copy(0.15f) else BpscColors.Surface)
                    .clickable(onClick = onToggleBookmarks).padding(horizontal = 6.dp, vertical = 4.dp)) {
                    LibSmallStat("🔖", "$bookmarkedCount", "Saved")
                }
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(BpscColors.PrimaryLight)
                .clickable(onClick = onUpload).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Upload, null, tint = BpscColors.Primary, modifier = Modifier.size(14.dp))
                    Text("Upload", style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LibSmallStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(icon, fontSize = 11.sp)
        Text(value, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextPrimary,
            fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 8.sp)
    }
}

// ════════════════════════════════════════════════════════════
// TYPE FILTER ROW
// ════════════════════════════════════════════════════════════
@Composable
// ════════════════════════════════════════════════════════════
// COMPACT FILTER BAR — replaces 3 separate filter rows
// Row 1: Type chips (All · PDF · PYQ · Books) — horizontally scrollable
// Row 2: Sort pills (Popular · Newest) + Subject dropdown on same line
// ════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
private fun CompactFilterBar(
    selectedType:    MaterialType?,
    selectedSubject: String,
    subjects:        List<String>,
    sortBy:          String,
    onTypeSelect:    (MaterialType?) -> Unit,
    onSubjectSelect: (String) -> Unit,
    onSortSelect:    (String) -> Unit,
) {
    var showSubjectSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(bottom = 6.dp)
    ) {
        // Row 1: Type filter chips
        LazyRow(
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                TypeChip(label = "All", emoji = "📋", selected = selectedType == null) {
                    onTypeSelect(null)
                }
            }
            items(MaterialType.values()) { type ->
                TypeChip(
                    label    = type.label,
                    emoji    = type.emoji,
                    selected = selectedType == type
                ) { onTypeSelect(if (selectedType == type) null else type) }
            }
        }

        HorizontalDivider(color = BpscColors.Divider, thickness = 0.5.dp)

        // Row 2: Sort tabs + Subject picker — all in one line
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sort pills
            listOf("downloads" to "🔥 Popular", "newest" to "🆕 Newest").forEach { (key, label) ->
                val sel = sortBy == key
                Text(
                    label,
                    style     = MaterialTheme.typography.labelSmall,
                    fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.Normal,
                    color     = if (sel) Color.White else BpscColors.TextSecondary,
                    modifier  = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (sel) BpscColors.Primary else BpscColors.Surface)
                        .clickable { onSortSelect(key) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            // Subject picker — shows selected subject, opens bottom sheet on tap
            Row(
                modifier          = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, BpscColors.Divider, RoundedCornerShape(20.dp))
                    .clickable { showSubjectSheet = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    selectedSubject.ifEmpty { "Subject" },
                    style  = MaterialTheme.typography.labelSmall,
                    color  = if (selectedSubject.isEmpty() || selectedSubject == "All") BpscColors.TextHint else BpscColors.Primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Icon(Icons.Rounded.KeyboardArrowDown, null,
                    tint = BpscColors.TextHint, modifier = Modifier.size(14.dp))
            }
        }
    }

    // Subject bottom sheet
    if (showSubjectSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSubjectSheet = false },
            sheetState       = sheetState,
            containerColor   = Color.White
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Filter by Subject",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color      = BpscColors.TextPrimary,
                    modifier   = Modifier.padding(bottom = 8.dp)
                )
                subjects.forEach { subj ->
                    val sel = selectedSubject == subj
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) BpscColors.PrimaryLight else Color.Transparent)
                            .clickable { onSubjectSelect(subj); showSubjectSheet = false }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            subj,
                            style      = MaterialTheme.typography.bodyLarge,
                            color      = if (sel) BpscColors.Primary else BpscColors.TextPrimary,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (sel) Icon(Icons.Rounded.Check, null,
                            tint = BpscColors.Primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeChip(label: String, emoji: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier              = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) BpscColors.Primary else BpscColors.Surface)
            .border(1.dp, if (selected) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(emoji, fontSize = 12.sp)
        Text(
            label,
            style      = MaterialTheme.typography.labelSmall,
            color      = if (selected) Color.White else BpscColors.TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ════════════════════════════════════════════════════════════
// ORIGINAL TYPE FILTER (kept for reference, no longer used)
// ════════════════════════════════════════════════════════════
//private fun TypeFilterRow(selected: MaterialType?, onSelect: (MaterialType?) -> Unit) {
//    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
//        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//        item {
//            FilterChip(label = "All", emoji = null, selected = selected == null,
//                color = BpscColors.Primary, bg = BpscColors.PrimaryLight,
//                onClick = { onSelect(null) })
//        }
//        items(MaterialType.values()) { type ->
//            val typeColor = typeColor(type)
//            val typeBg    = typeBg(type)
//            FilterChip(label = type.label, emoji = type.emoji, selected = selected == type,
//                color = typeColor, bg = typeBg,
//                onClick = { onSelect(if (selected == type) null else type) })
//        }
//    }
//}

@Composable
private fun SubjectFilterRow(subjects: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(subjects) { sub ->
            FilterChip(label = sub, emoji = null, selected = selected == sub,
                color = BpscColors.Primary, bg = BpscColors.PrimaryLight,
                onClick = { onSelect(sub) })
        }
    }
}

@Composable
private fun SortRow(current: String, onSelect: (String) -> Unit) {
    val options = listOf("downloads" to "🔥 Popular", "newest" to "🆕 Newest", "rating" to "⭐ Top Rated")
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { (key, label) ->
            val sel = current == key
            Text(label,
                style = MaterialTheme.typography.labelSmall,
                color = if (sel) Color.White else BpscColors.TextSecondary,
                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .background(if (sel) BpscColors.Primary else Color.White)
                    .border(1.dp, if (sel) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(20.dp))
                    .clickable { onSelect(key) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, emoji: String?, selected: Boolean,
                       color: Color, bg: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.clip(RoundedCornerShape(20.dp))
        .background(if (selected) color else Color.White)
        .border(1.dp, if (selected) color else BpscColors.Divider, RoundedCornerShape(20.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        if (emoji != null) Text(emoji, fontSize = 12.sp)
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = if (selected) Color.White else BpscColors.TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

// ════════════════════════════════════════════════════════════
// MATERIALS LIST
// ════════════════════════════════════════════════════════════
@Composable
private fun MaterialsList(
    state:      StudyMaterialsUiState,
    onView:     (String) -> Unit,
    onBookmark: (String) -> Unit,
    onDownload: (StudyMaterialDto) -> Unit,
    onLoadMore: () -> Unit
) {
    val pinned   = state.materials.filter { it.isFeatured }
    val trending = state.materials.filter { it.isTrending && !it.isFeatured }
    val newItems = state.materials.filter { it.isNew && !it.isTrending && !it.isFeatured }
    val rest     = state.materials.filter { !it.isFeatured && !it.isTrending && !it.isNew }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)) {

        fun sectionItems(label: String, count: Int, items: List<StudyMaterialDto>) {
            if (items.isEmpty()) return
            item { LibSectionHeader(label, "$count items") }
            items(items, key = { it.id }) { item ->
                LibraryItemCard(
                    item         = item,
                    isBookmarked = state.bookmarkedIds.contains(item.id),
                    isDownloaded = state.downloadedIds.contains(item.id),
                    isDownloading = state.downloadingId == item.id,
                    onBookmark   = { onBookmark(item.id) },
                    onDownload   = { onDownload(item) },
                    onView       = { onView(item.id) }
                )
                Spacer(Modifier.height(10.dp))
            }
            item { Spacer(Modifier.height(6.dp)) }
        }

        sectionItems("📌 Pinned by Admin", pinned.size, pinned)
        sectionItems("🔥 Trending This Week", trending.size, trending)
        sectionItems("🆕 Recently Added", newItems.size, newItems)
        sectionItems("📂 All Resources", rest.size, rest)

        // Load more trigger
        if (state.hasNextPage) {
            item(key = "load_more") {
                LaunchedEffect(Unit) { onLoadMore() }
                Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                    if (state.isLoadingMore) {
                        CircularProgressIndicator(color = BpscColors.Primary, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LibSectionHeader(title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
    }
}

// ════════════════════════════════════════════════════════════
// MATERIAL CARD — reuse exact existing design
// ════════════════════════════════════════════════════════════
@Composable
private fun LibraryItemCard(
    item:         StudyMaterialDto,
    isBookmarked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onBookmark:   () -> Unit,
    onDownload:   () -> Unit,
    onView:       () -> Unit
) {
    val color = typeColor(item.type)
    val bg    = typeBg(item.type)

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onView),
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(bg),
                    contentAlignment = Alignment.Center) {
                    Text(item.type.emoji, fontSize = 22.sp)
                    if (item.isPremium) Box(modifier = Modifier.align(Alignment.TopEnd).padding(3.dp)
                        .clip(RoundedCornerShape(4.dp)).background(BpscColors.CoinGold)
                        .padding(horizontal = 3.dp, vertical = 1.dp)) {
                        Text("PRO", style = MaterialTheme.typography.labelSmall,
                            color = Color.White, fontSize = 6.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        TypeBadge(item.type); if (item.isNew) NewBadge()
                        if (item.isTrending) Text("🔥", fontSize = 12.sp)
                        if (!item.isPremium) FreeBadge()
                    }
                    Text(item.title, style = MaterialTheme.typography.titleMedium,
                        color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                    Text(item.author ?: item.uploaderName ?: "BPSCNotes Team",
                        style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                }
                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (isBookmarked) Color(0xFFFFF8E1) else BpscColors.Surface)
                    .clickable(onClick = onBookmark), contentAlignment = Alignment.Center) {
                    Icon(if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, null,
                        tint = if (isBookmarked) BpscColors.CoinGold else BpscColors.TextHint,
                        modifier = Modifier.size(15.dp))
                }
            }

            // Stats row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (item.pageCount > 0) LibInfoChip(Icons.Rounded.Description, "${item.pageCount} pages")
                LibInfoChip(Icons.Rounded.Storage, "${"%.1f".format(item.fileSizeMb)} MB")
                LibInfoChip(Icons.Rounded.Download, formatCount(item.downloadCount))
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Rounded.Star, null, tint = BpscColors.CoinGold, modifier = Modifier.size(12.dp))
                    Text("${item.rating}", style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.TextSecondary, fontWeight = FontWeight.Bold)
                }
            }

            // Action buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onView, modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BpscColors.Primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BpscColors.Primary),
                    contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Rounded.Visibility, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Read", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onDownload, modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when { isDownloaded -> BpscColors.Success; item.isPremium -> BpscColors.CoinGold; else -> BpscColors.Primary }),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    enabled = !isDownloading) {
                    if (isDownloading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(if (isDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.Download, null,
                            modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(when { isDownloaded -> "Saved"; item.isPremium -> "Unlock"; else -> "Download" },
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// DETAIL SHEET
// ════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialDetailSheet(
    material:     MaterialDetailData,
    isBookmarked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onBookmark:   () -> Unit,
    onDownload:   () -> Unit,
    onOpenPdf:    (String) -> Unit,
    onDismiss:    () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val color = typeColor(material.type)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            // Coloured header
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(color.copy(0.7f), color)))
                .padding(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(material.type.emoji, fontSize = 22.sp)
                        TypeBadgeWhite(material.type.label)
                        if (!material.isPremium) FreeBadgeWhite() else ProBadge()
                    }
                    Text(material.title, style = MaterialTheme.typography.titleLarge,
                        color = Color.White, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
                    Text("By ${material.author ?: material.uploaderName ?: "BPSCNotes Team"} · ${material.uploadedDate ?: ""}",
                        style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (material.pageCount > 0) SheetStatWhite("📄", "${material.pageCount} pages")
                        SheetStatWhite("💾", "${"%.1f".format(material.fileSizeBytes / 1048576f)} MB")
                        SheetStatWhite("⬇️", formatCount(material.downloadCount))
                        SheetStatWhite("⭐", "${material.rating}")
                    }
                }
            }

            // Body
            Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                .padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                if (!material.description.isNullOrEmpty()) {
                    Text("About", style = MaterialTheme.typography.titleMedium,
                        color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                    Text(material.description, style = MaterialTheme.typography.bodyLarge,
                        color = BpscColors.TextSecondary, lineHeight = 24.sp)
                }

                if (material.tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        material.tags.forEach { tag ->
                            Text("#$tag", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary,
                                fontSize = 11.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(BpscColors.PrimaryLight).padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                }

                // Preview area / open PDF button
                val downloadUrl = material.downloadUrl
                Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))
                    .background(BpscColors.Surface).border(1.dp, BpscColors.Divider, RoundedCornerShape(16.dp))
                    .then(if (downloadUrl != null) Modifier.clickable { onOpenPdf(downloadUrl) } else Modifier),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(material.type.emoji, fontSize = 44.sp)
                        Text(if (downloadUrl != null) "Tap to open" else "Preview",
                            style = MaterialTheme.typography.titleMedium, color = BpscColors.TextSecondary)
                        Text("Open full document in PDF viewer",
                            style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint)
                    }
                }

                if (material.isPremium) {
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFF8E1)).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🔒", fontSize = 22.sp)
                        Column(Modifier.weight(1f)) {
                            Text("Premium Content", style = MaterialTheme.typography.titleMedium,
                                color = BpscColors.CoinGold, fontWeight = FontWeight.Bold)
                            Text("Unlock with BPSCNotes Pro", style = MaterialTheme.typography.bodyMedium,
                                color = BpscColors.TextSecondary)
                        }
                    }
                }
            }

            HorizontalDivider(color = BpscColors.Divider)
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onBookmark, modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isBookmarked) BpscColors.CoinGold else BpscColors.Divider),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isBookmarked) BpscColors.CoinGold else BpscColors.TextSecondary)) {
                    Icon(if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, null,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isBookmarked) "Saved" else "Save", style = MaterialTheme.typography.titleMedium)
                }
                Button(onClick = if (material.downloadUrl != null) {{ onOpenPdf(material.downloadUrl) }} else onDownload,
                    modifier = Modifier.weight(2f).height(48.dp), shape = RoundedCornerShape(12.dp),
                    enabled = !isDownloading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when { isDownloaded -> BpscColors.Success; material.isPremium -> BpscColors.CoinGold; else -> BpscColors.Primary })) {
                    if (isDownloading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(when { isDownloaded -> Icons.Rounded.CheckCircle; material.isPremium -> Icons.Rounded.Lock; else -> Icons.Rounded.Download }, null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(when { isDownloaded -> "Downloaded ✓"; material.isPremium -> "Unlock with Pro"; material.downloadUrl != null -> "Open PDF"; else -> "Download Free" },
                        style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// UPLOAD SHEET
// ════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadSheet(
    isUploading:    Boolean,
    uploadProgress: Float,
    uploadError:    String?,
    onSubmit:       (Uri, String, String, String, MaterialType, String, List<String>, Int) -> Unit,
    onDismiss:      () -> Unit
) {
    var title       by remember { mutableStateOf("") }
    var subject     by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var author      by remember { mutableStateOf("") }
    var tagsInput   by remember { mutableStateOf("") }
    var selType     by remember { mutableStateOf(MaterialType.PDF) }
    var fileUri     by remember { mutableStateOf<Uri?>(null) }
    var fileName    by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            fileUri  = it
            fileName = it.lastPathSegment ?: "file.pdf"
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)
            .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            Text("Upload Your Notes", style = MaterialTheme.typography.headlineSmall,
                color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
            Text("Share notes with 10,000+ BPSC aspirants",
                style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
            HorizontalDivider(color = BpscColors.Divider)

            // Error banner
            uploadError?.let { err ->
                Card(shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE8E8))) {
                    Text(err, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE74C3C),
                        modifier = Modifier.padding(12.dp))
                }
            }

            OutlinedTextField(value = title, onValueChange = { title = it },
                label = { Text("Notes Title *") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true)

            OutlinedTextField(value = subject, onValueChange = { subject = it },
                label = { Text("Subject *") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true)

            OutlinedTextField(value = author, onValueChange = { author = it },
                label = { Text("Author (your name)") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true)

            // Type selector
            Text("Content Type", style = MaterialTheme.typography.titleMedium,
                color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MaterialType.values()) { type ->
                    val sel = selType == type
                    val color = typeColor(type)
                    Row(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (sel) color else typeBg(type))
                        .clickable { selType = type }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(type.emoji, fontSize = 12.sp)
                        Text(type.label, style = MaterialTheme.typography.bodyMedium,
                            color = if (sel) Color.White else color,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            OutlinedTextField(value = description, onValueChange = { description = it },
                label = { Text("Brief Description") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), minLines = 3, maxLines = 4)

            OutlinedTextField(value = tagsInput, onValueChange = { tagsInput = it },
                label = { Text("Tags (comma separated)") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                placeholder = { Text("Constitution, Parliament, DPSP") })

            // File picker
            Box(modifier = Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(14.dp))
                .background(if (fileUri != null) BpscColors.Success.copy(0.08f) else BpscColors.Surface)
                .border(1.5.dp, if (fileUri != null) BpscColors.Success else BpscColors.Divider, RoundedCornerShape(14.dp))
                .clickable { filePicker.launch("*/*") }, contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.AttachFile, null,
                        tint = if (fileUri != null) BpscColors.Success else BpscColors.Primary,
                        modifier = Modifier.size(22.dp))
                    Text(if (fileUri != null) "✅ $fileName" else "Tap to attach file (PDF / DOC)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (fileUri != null) BpscColors.Success else BpscColors.TextSecondary)
                }
            }

            // Upload progress
            if (isUploading) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(progress = uploadProgress, modifier = Modifier.fillMaxWidth(),
                        color = BpscColors.Primary, trackColor = BpscColors.Divider)
                    Text("${(uploadProgress * 100).toInt()}% uploaded…",
                        style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary)
                }
            }

            Button(
                onClick = {
                    val uri = fileUri ?: return@Button
                    val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onSubmit(uri, title, description, subject, selType, author, tags, 0)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                enabled = title.isNotBlank() && subject.isNotBlank() && fileUri != null && !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Uploading…", style = MaterialTheme.typography.titleMedium)
                } else {
                    Icon(Icons.Rounded.Upload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Submit for Review", style = MaterialTheme.typography.titleMedium)
                }
            }

            Text("📋 All uploads are reviewed before publishing",
                style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ════════════════════════════════════════════════════════════
// EMPTY / ERROR / LOADING STATES
// ════════════════════════════════════════════════════════════
@Composable
private fun EmptyState(showBookmarksOnly: Boolean) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (showBookmarksOnly) "🔖" else "🔍", fontSize = 48.sp)
            Text(if (showBookmarksOnly) "No saved materials" else "No resources found",
                style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
            Text(if (showBookmarksOnly) "Bookmark materials to see them here" else "Try a different search or filter",
                style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("⚠️", fontSize = 40.sp)
            Text(message, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun LoadingGrid() {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(6) {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(16.dp))
                .background(BpscColors.Divider))
        }
    }
}

// ════════════════════════════════════════════════════════════
// SMALL BADGE COMPOSABLES
// ════════════════════════════════════════════════════════════
@Composable
private fun TypeBadge(type: MaterialType) {
    val color = typeColor(type); val bg = typeBg(type)
    Text(type.label, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 9.sp,
        fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(5.dp))
            .background(bg).padding(horizontal = 6.dp, vertical = 2.dp))
}
@Composable private fun TypeBadgeWhite(label: String) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.85f),
        fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(0.2f)).padding(horizontal = 8.dp, vertical = 3.dp))
}
@Composable private fun FreeBadge() {
    Text("FREE", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontSize = 9.sp,
        fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(5.dp))
            .background(Color(0xFFE8FDF4)).padding(horizontal = 6.dp, vertical = 2.dp))
}
@Composable private fun FreeBadgeWhite() {
    Text("FREE", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success,
        fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE8FDF4)).padding(horizontal = 8.dp, vertical = 3.dp))
}
@Composable private fun ProBadge() {
    Text("PRO", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold,
        fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFFFF8E1)).padding(horizontal = 8.dp, vertical = 3.dp))
}
@Composable private fun NewBadge() {
    Text("🆕 New", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontSize = 9.sp,
        fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(5.dp))
            .background(Color(0xFFE8FDF4)).padding(horizontal = 6.dp, vertical = 2.dp))
}
@Composable private fun SheetStatWhite(icon: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(icon, fontSize = 12.sp)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.85f), fontWeight = FontWeight.SemiBold)
    }
}
@Composable private fun LibInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = BpscColors.TextHint, modifier = Modifier.size(11.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, fontSize = 10.sp)
    }
}

// ── Helpers ───────────────────────────────────────────────────
private fun typeColor(type: MaterialType) = when (type) {
    MaterialType.PDF   -> Color(0xFFE74C3C)
    MaterialType.PYQ   -> Color(0xFF9B59B6)
    MaterialType.BOOK  -> Color(0xFF1565C0)
    MaterialType.VIDEO -> Color(0xFFE67E22)
}
private fun typeBg(type: MaterialType) = when (type) {
    MaterialType.PDF   -> Color(0xFFFEE8E8)
    MaterialType.PYQ   -> Color(0xFFF3E8FD)
    MaterialType.BOOK  -> Color(0xFFE8F0FD)
    MaterialType.VIDEO -> Color(0xFFFFF0EA)
}
private fun formatCount(count: Int): String {
    return if (count >= 1000) "${"%.1f".format(count / 1000f)}k" else "$count"
}

// needed for text field in search
@Composable
private fun BasicTextField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier,
    textStyle: androidx.compose.ui.text.TextStyle, singleLine: Boolean,
    keyboardOptions: KeyboardOptions, keyboardActions: KeyboardActions,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value, onValueChange = onValueChange, modifier = modifier,
        textStyle = textStyle, singleLine = singleLine,
        keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
        decorationBox = decorationBox
    )
}