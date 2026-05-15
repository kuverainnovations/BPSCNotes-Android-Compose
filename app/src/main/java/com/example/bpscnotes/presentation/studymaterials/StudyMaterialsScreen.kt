package com.example.bpscnotes.presentation.studymaterials

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors

@Composable
fun StudyMaterialsScreen(
    navController: NavHostController
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BpscColors.Surface)
    ) {

        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF051D56),
                            Color(0xFF0A2472),
                            Color(0xFF1565C0)
                        ),
                        Offset(0f, 0f),
                        Offset(500f, 500f)
                    )
                )
        ) {

            Column(
                modifier = Modifier
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 46.dp,
                        bottom = 30.dp
                    )
            ) {

                Text(
                    "Study Materials",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    "Notes, PDFs, PYQs & Books",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.7f)
                )
            }
        }

        // Existing reusable content
        StudyMaterialsContent()
    }
}

enum class LibraryContentType(
    val label: String,
    val emoji: String,
    val color: Color,
    val bg: Color
) {
    PDF("PDF Notes", "📄", Color(0xFFE74C3C), Color(0xFFFEE8E8)),
    PYQ("Prev. Papers", "📝", Color(0xFF9B59B6), Color(0xFFF3E8FD)),
    Book("Books", "📚", Color(0xFF1565C0), Color(0xFFE8F0FD)),
    Video("Video Notes", "🎬", Color(0xFFE67E22), Color(0xFFFFF0EA)),
    Bookmarked("My Bookmarks", "🔖", Color(0xFFF39C12), Color(0xFFFFF8E1)),
}

data class LibraryItem(
    val id: String, val title: String, val subject: String, val type: LibraryContentType,
    val author: String, val pages: Int, val fileSizeMb: Float, val downloads: Int,
    val rating: Float, val isPremium: Boolean, val isNew: Boolean = false,
    val isTrending: Boolean = false, val isPinned: Boolean = false,
    val isDownloaded: Boolean = false, val uploadedDate: String,
    val description: String, val tags: List<String> = emptyList(),
)

val mockLibraryItems = listOf(
    LibraryItem(
        "li1", "BPSC Polity Complete Notes", "Polity", LibraryContentType.PDF,
        "BPSCNotes Team", 185, 12.4f, 45200, 4.8f, false, isTrending = true, isPinned = true,
        uploadedDate = "10 Mar 2026",
        description = "Complete Polity notes covering Constitution, Fundamental Rights, DPSP, Parliament and Judiciary.",
        tags = listOf("Constitution", "Fundamental Rights", "Parliament")
    ),
    LibraryItem(
        "li2", "BPSC 69th Previous Year Paper", "All Subjects", LibraryContentType.PYQ,
        "BPSCNotes Team", 24, 2.1f, 38900, 4.9f, false, isTrending = true, isPinned = true,
        uploadedDate = "05 Mar 2026",
        description = "Complete BPSC 69th CCE Prelims paper with answer key.",
        tags = listOf("Prelims", "2024", "Answer Key")
    ),
    LibraryItem(
        "li3", "Modern India — Complete Notes", "History", LibraryContentType.PDF,
        "Prof. Anita Singh", 142, 9.8f, 28400, 4.7f, false, isNew = true,
        uploadedDate = "12 Mar 2026",
        description = "Comprehensive notes on Modern Indian History from 1757 to Independence.",
        tags = listOf("British Rule", "Freedom Movement")
    ),
    LibraryItem(
        "li4", "Bihar GK Handbook 2026", "Bihar GK", LibraryContentType.Book,
        "Rahul Kumar", 320, 22.5f, 51200, 4.9f, true, isTrending = true,
        uploadedDate = "01 Mar 2026",
        description = "Complete Bihar GK reference book covering geography, history, economy and culture.",
        tags = listOf("Bihar", "Comprehensive", "2026 Updated")
    ),
    LibraryItem(
        "li5", "Economy for BPSC — Video Notes", "Economy", LibraryContentType.Video,
        "CA Vikram Joshi", 68, 5.2f, 19800, 4.6f, true, isNew = true,
        uploadedDate = "13 Mar 2026",
        description = "Structured notes from Economy video lectures covering RBI, Banking and Budget.",
        tags = listOf("RBI", "Budget 2026", "GDP")
    ),
    LibraryItem(
        "li6", "BPSC 68th Previous Year Paper", "All Subjects", LibraryContentType.PYQ,
        "BPSCNotes Team", 24, 2.0f, 34100, 4.7f, false,
        uploadedDate = "20 Feb 2026",
        description = "BPSC 68th CCE Prelims paper with answer key.",
        tags = listOf("Prelims", "2022")
    ),
    LibraryItem(
        "li7", "Geography of India — Master Notes", "Geography", LibraryContentType.PDF,
        "Dr. S. Mishra", 156, 11.2f, 22300, 4.5f, false,
        uploadedDate = "25 Feb 2026",
        description = "Complete Indian Geography notes including physical, economic and human geography.",
        tags = listOf("Rivers", "Mountains", "Climate")
    ),
    LibraryItem(
        "li8", "Indian Economy — Ramesh Singh", "Economy", LibraryContentType.Book,
        "Ramesh Singh", 580, 48.0f, 67800, 4.8f, true, isTrending = true,
        uploadedDate = "15 Jan 2026",
        description = "Most trusted book for Indian Economy preparation.",
        tags = listOf("Standard Book", "Comprehensive")
    ),
    LibraryItem(
        "li9", "Current Affairs January 2026", "Current Affairs", LibraryContentType.PDF,
        "BPSCNotes Team", 45, 3.8f, 18200, 4.5f, false, isNew = true,
        uploadedDate = "01 Feb 2026",
        description = "Monthly current affairs for January 2026 curated for BPSC.",
        tags = listOf("Monthly", "January 2026")
    ),
)

val librarySubjects = listOf(
    "All",
    "Polity",
    "History",
    "Geography",
    "Economy",
    "Bihar GK",
    "Science",
    "Current Affairs",
    "All Subjects"
)

@Composable
private fun StudyMaterialsContent() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<LibraryContentType?>(null) }
    var selectedSubject by remember { mutableStateOf("All") }
    var selectedItem by remember { mutableStateOf<LibraryItem?>(null) }
    val bookmarked = remember { mutableStateListOf<String>() }
    val downloaded = remember { mutableStateListOf<String>() }
    var showUpload by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val filtered = mockLibraryItems.filter { item ->
        val matchesType = selectedType == null || item.type == selectedType
        val matchesSub = selectedSubject == "All" || item.subject == selectedSubject
        val matchesSearch = searchQuery.isEmpty() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.tags.any { it.contains(searchQuery, ignoreCase = true) }
        val matchesBM =
            selectedType != LibraryContentType.Bookmarked || bookmarked.contains(item.id)
        matchesType && matchesSub && matchesSearch && matchesBM
    }
    val pinned = filtered.filter { it.isPinned }
    val trending = filtered.filter { it.isTrending && !it.isPinned }
    val newItems = filtered.filter { it.isNew && !it.isTrending && !it.isPinned }
    val rest = filtered.filter { !it.isPinned && !it.isTrending && !it.isNew }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search + stats
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BpscColors.Surface)
                    .border(1.dp, BpscColors.Divider, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.Search,
                    null,
                    tint = BpscColors.TextHint,
                    modifier = Modifier.size(18.dp)
                )
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = BpscColors.TextPrimary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) Text(
                            "Search notes, papers, books...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = BpscColors.TextHint
                        )
                        inner()
                    }
                )
                if (searchQuery.isNotEmpty()) Icon(
                    Icons.Rounded.Close,
                    null,
                    tint = BpscColors.TextHint,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { searchQuery = "" })
            }

            // Stats + Upload row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LibSmallStat(
                        "📄",
                        "${mockLibraryItems.count { it.type == LibraryContentType.PDF }}",
                        "PDFs"
                    )
                    LibSmallStat(
                        "📝",
                        "${mockLibraryItems.count { it.type == LibraryContentType.PYQ }}",
                        "PYQs"
                    )
                    LibSmallStat(
                        "📚",
                        "${mockLibraryItems.count { it.type == LibraryContentType.Book }}",
                        "Books"
                    )
                    LibSmallStat("🔖", "${bookmarked.size}", "Saved")
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BpscColors.PrimaryLight)
                        .clickable { showUpload = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Upload,
                            null,
                            tint = BpscColors.Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Upload",
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Content type filter
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selectedType == null) BpscColors.Primary else Color.White)
                        .border(
                            1.dp,
                            if (selectedType == null) BpscColors.Primary else BpscColors.Divider,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedType = null }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        "All",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedType == null) Color.White else BpscColors.TextSecondary,
                        fontWeight = if (selectedType == null) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            items(LibraryContentType.values()) { type ->
                val sel = selectedType == type
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (sel) type.color else Color.White)
                        .border(
                            1.dp,
                            if (sel) type.color else BpscColors.Divider,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedType = if (sel) null else type }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(type.emoji, fontSize = 12.sp)
                    Text(
                        type.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (sel) Color.White else BpscColors.TextSecondary,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Subject filter
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(librarySubjects) { sub ->
                val sel = selectedSubject == sub
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (sel) BpscColors.Primary else Color.White)
                        .border(
                            1.dp,
                            if (sel) BpscColors.Primary else BpscColors.Divider,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedSubject = sub }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        sub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (sel) Color.White else BpscColors.TextSecondary,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔍", fontSize = 48.sp)
                    Text(
                        "No resources found",
                        style = MaterialTheme.typography.titleLarge,
                        color = BpscColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Try a different search or filter",
                        style = MaterialTheme.typography.bodyLarge,
                        color = BpscColors.TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (pinned.isNotEmpty()) {
                    item { LibSectionHeader("📌 Pinned by Admin", "${pinned.size} items") }
                    items(pinned) { item ->
                        LibraryItemCard(
                            item,
                            bookmarked.contains(item.id),
                            downloaded.contains(item.id),
                            {
                                if (bookmarked.contains(item.id)) bookmarked.remove(item.id) else bookmarked.add(
                                    item.id
                                )
                            },
                            { if (!downloaded.contains(item.id)) downloaded.add(item.id) }) {
                            selectedItem = item
                        }; Spacer(Modifier.height(10.dp))
                    }
                    item { Spacer(Modifier.height(6.dp)) }
                }
                if (trending.isNotEmpty()) {
                    item { LibSectionHeader("🔥 Trending This Week", "${trending.size} items") }
                    items(trending) { item ->
                        LibraryItemCard(
                            item,
                            bookmarked.contains(item.id),
                            downloaded.contains(item.id),
                            {
                                if (bookmarked.contains(item.id)) bookmarked.remove(item.id) else bookmarked.add(
                                    item.id
                                )
                            },
                            { if (!downloaded.contains(item.id)) downloaded.add(item.id) }) {
                            selectedItem = item
                        }; Spacer(Modifier.height(10.dp))
                    }
                    item { Spacer(Modifier.height(6.dp)) }
                }
                if (newItems.isNotEmpty()) {
                    item { LibSectionHeader("🆕 Recently Added", "${newItems.size} items") }
                    items(newItems) { item ->
                        LibraryItemCard(
                            item,
                            bookmarked.contains(item.id),
                            downloaded.contains(item.id),
                            {
                                if (bookmarked.contains(item.id)) bookmarked.remove(item.id) else bookmarked.add(
                                    item.id
                                )
                            },
                            { if (!downloaded.contains(item.id)) downloaded.add(item.id) }) {
                            selectedItem = item
                        }; Spacer(Modifier.height(10.dp))
                    }
                    item { Spacer(Modifier.height(6.dp)) }
                }
                if (rest.isNotEmpty()) {
                    item { LibSectionHeader("📂 All Resources", "${rest.size} items") }
                    items(rest) { item ->
                        LibraryItemCard(
                            item,
                            bookmarked.contains(item.id),
                            downloaded.contains(item.id),
                            {
                                if (bookmarked.contains(item.id)) bookmarked.remove(item.id) else bookmarked.add(
                                    item.id
                                )
                            },
                            { if (!downloaded.contains(item.id)) downloaded.add(item.id) }) {
                            selectedItem = item
                        }; Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }

    selectedItem?.let { item ->
        LibraryDetailSheet(
            item = item,
            isBookmarked = bookmarked.contains(item.id),
            isDownloaded = downloaded.contains(item.id),
            onBookmark = {
                if (bookmarked.contains(item.id)) bookmarked.remove(item.id) else bookmarked.add(
                    item.id
                )
            },
            onDownload = { if (!downloaded.contains(item.id)) downloaded.add(item.id) },
            onDismiss = { selectedItem = null })
    }

    if (showUpload) UploadNotesSheet(onDismiss = { showUpload = false })
}

@Composable
private fun LibSmallStat(icon: String, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(icon, fontSize = 11.sp)
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            color = BpscColors.TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = BpscColors.TextHint,
            fontSize = 8.sp
        )
    }
}


@Composable
private fun LibSectionHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = BpscColors.TextPrimary,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = BpscColors.TextSecondary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadNotesSheet(onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var selType by remember { mutableStateOf(LibraryContentType.PDF) }
    var description by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Upload Your Notes",
                style = MaterialTheme.typography.headlineSmall,
                color = BpscColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Share notes with 10,000+ BPSC aspirants",
                style = MaterialTheme.typography.bodyLarge,
                color = BpscColors.TextSecondary
            )
            HorizontalDivider(color = BpscColors.Divider)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Notes Title") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Text(
                "Content Type",
                style = MaterialTheme.typography.titleMedium,
                color = BpscColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    LibraryContentType.values()
                        .filter { it != LibraryContentType.Bookmarked }) { type ->
                    val sel = selType == type
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (sel) type.color else type.bg)
                            .clickable { selType = type }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(type.emoji, fontSize = 12.sp)
                        Text(
                            type.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (sel) Color.White else type.color,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Brief Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 4
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BpscColors.Surface)
                    .border(1.5.dp, BpscColors.Divider, RoundedCornerShape(14.dp))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.AttachFile,
                        null,
                        tint = BpscColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        "Tap to attach file (PDF / DOC)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = BpscColors.TextSecondary
                    )
                }
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = title.isNotBlank() && subject.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                Icon(
                    Icons.Rounded.Upload,
                    null,
                    modifier = Modifier.size(18.dp)
                ); Spacer(Modifier.width(8.dp)); Text(
                "Submit for Review",
                style = MaterialTheme.typography.titleMedium
            )
            }
            Text(
                "📋 All uploads are reviewed before publishing",
                style = MaterialTheme.typography.bodyMedium,
                color = BpscColors.TextHint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryDetailSheet(
    item: LibraryItem, isBookmarked: Boolean, isDownloaded: Boolean,
    onBookmark: () -> Unit, onDownload: () -> Unit, onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                item.type.color.copy(
                                    red = item.type.color.red * 0.6f,
                                    green = item.type.color.green * 0.6f,
                                    blue = item.type.color.blue * 0.6f
                                ), item.type.color
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.type.emoji, fontSize = 22.sp)
                        Text(
                            item.type.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.85f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                        if (!item.isPremium) Text(
                            "FREE",
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.Success,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE8FDF4))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                        else Text(
                            "PRO",
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.CoinGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFF8E1))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 26.sp
                    )
                    Text(
                        "By ${item.author} · ${item.uploadedDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.75f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SheetStatWhite("📄", "${item.pages} pages"); SheetStatWhite(
                        "💾",
                        "${item.fileSizeMb} MB"
                    )
                        SheetStatWhite(
                            "⬇️",
                            "${(item.downloads / 1000f).let { if (it >= 1f) "${it.toInt()}k" else "${item.downloads}" }}"
                        ); SheetStatWhite("⭐", "${item.rating}")
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    //    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "About",
                    style = MaterialTheme.typography.titleMedium,
                    color = BpscColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = BpscColors.TextSecondary,
                    lineHeight = 24.sp
                )
                if (item.tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        item.tags.forEach { tag ->
                            Text(
                                "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.Primary,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BpscColors.PrimaryLight)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BpscColors.Surface)
                        .border(1.dp, BpscColors.Divider, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(item.type.emoji, fontSize = 44.sp)
                        Text(
                            "Preview",
                            style = MaterialTheme.typography.titleMedium,
                            color = BpscColors.TextSecondary
                        )
                        Text(
                            "Tap Read to open full document",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.TextHint
                        )
                    }
                }
                if (item.isPremium) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFF8E1))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🔒", fontSize = 22.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Premium Content",
                                style = MaterialTheme.typography.titleMedium,
                                color = BpscColors.CoinGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Unlock with BPSCNotes Pro",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BpscColors.TextSecondary
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = BpscColors.Divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBookmark,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isBookmarked) BpscColors.CoinGold else BpscColors.Divider
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isBookmarked) BpscColors.CoinGold else BpscColors.TextSecondary)
                ) {
                    Icon(
                        if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp)); Text(
                    if (isBookmarked) "Saved" else "Save",
                    style = MaterialTheme.typography.titleMedium
                )
                }
                Button(
                    onClick = { onDownload() },
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDownloaded) BpscColors.Success else if (item.isPremium) BpscColors.CoinGold else BpscColors.Primary)
                ) {
                    Icon(
                        when {
                            isDownloaded -> Icons.Rounded.CheckCircle; item.isPremium -> Icons.Rounded.Lock; else -> Icons.Rounded.Download
                        }, null, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp)); Text(
                    when {
                        isDownloaded -> "Downloaded ✓"; item.isPremium -> "Unlock with Pro"; else -> "Download Free"
                    }, style = MaterialTheme.typography.titleMedium
                )
                }
            }
        }
    }
}

@Composable
private fun SheetStatWhite(icon: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(icon, fontSize = 12.sp)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(0.85f),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LibraryItemCard(
    item: LibraryItem, isBookmarked: Boolean, isDownloaded: Boolean,
    onBookmark: () -> Unit, onDownload: () -> Unit, onView: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(item.type.bg), contentAlignment = Alignment.Center
                ) {
                    Text(item.type.emoji, fontSize = 22.sp)
                    if (item.isPremium) Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(BpscColors.CoinGold)
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            "PRO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 6.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            item.type.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = item.type.color,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(item.type.bg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        if (item.isNew) Text(
                            "🆕 New",
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.Success,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFFE8FDF4))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        if (item.isTrending) Text("🔥", fontSize = 12.sp)
                        if (!item.isPremium) Text(
                            "FREE",
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.Success,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFFE8FDF4))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = BpscColors.TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    Text(
                        item.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BpscColors.TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isBookmarked) Color(0xFFFFF8E1) else BpscColors.Surface)
                        .clickable(onClick = onBookmark), contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        null,
                        tint = if (isBookmarked) BpscColors.CoinGold else BpscColors.TextHint,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LibInfoChip(Icons.Rounded.Description, "${item.pages} pages")
                LibInfoChip(Icons.Rounded.Storage, "${item.fileSizeMb} MB")
                LibInfoChip(
                    Icons.Rounded.Download,
                    "${(item.downloads / 1000f).let { if (it >= 1f) "${it.toInt()}k" else "${item.downloads}" }}"
                )
                Spacer(Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Rounded.Star,
                        null,
                        tint = BpscColors.CoinGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        "${item.rating}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onView,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BpscColors.Primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BpscColors.Primary),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Rounded.Visibility, null, modifier = Modifier.size(14.dp)); Spacer(
                    Modifier.width(4.dp)
                ); Text(
                    "Read",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                }
                Button(
                    onClick = onDownload,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDownloaded) BpscColors.Success else if (item.isPremium) BpscColors.CoinGold else BpscColors.Primary),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        if (isDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                        null,
                        modifier = Modifier.size(14.dp)
                    ); Spacer(Modifier.width(4.dp))
                    Text(
                        if (isDownloaded) "Saved" else if (item.isPremium) "Unlock" else "Download",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LibInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = BpscColors.TextHint, modifier = Modifier.size(11.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = BpscColors.TextSecondary,
            fontSize = 10.sp
        )
    }
}