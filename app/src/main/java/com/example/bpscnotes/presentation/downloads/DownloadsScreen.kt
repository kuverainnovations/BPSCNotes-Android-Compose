package com.example.bpscnotes.presentation.downloads

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors

enum class DownloadStatus { COMPLETED, DOWNLOADING, PAUSED }
enum class DownloadType(val label: String, val emoji: String, val color: Color, val bg: Color) {
    PDF  ("PDF Notes",   "📄", Color(0xFFE74C3C), Color(0xFFFEE8E8)),
    VIDEO("Video",       "🎬", Color(0xFFE67E22), Color(0xFFFFF0EA)),
    PYQ  ("Prev. Papers","📝", Color(0xFF9B59B6), Color(0xFFF3E8FD)),
    BOOK ("Books",       "📚", Color(0xFF1565C0), Color(0xFFE8F0FD)),
}

data class DownloadItem(
    val id: String,
    val title: String,
    val subject: String,
    val type: DownloadType,
    val sizeMb: Float,
    val downloadedDate: String,
    val status: DownloadStatus,
    val progress: Float = 1f,  // 0-1 for downloading
    val duration: String? = null, // for videos
)

val mockDownloads = listOf(
    DownloadItem("d1", "BPSC Polity Complete Notes",       "Polity",       DownloadType.PDF,   12.4f, "Today",          DownloadStatus.COMPLETED),
    DownloadItem("d2", "BPSC 69th Previous Year Paper",   "All Subjects", DownloadType.PYQ,   2.1f,  "Today",          DownloadStatus.COMPLETED),
    DownloadItem("d3", "Polity Master Class — Lecture 5", "Polity",       DownloadType.VIDEO, 245f,  "Yesterday",      DownloadStatus.DOWNLOADING, 0.65f, "1h 24m"),
    DownloadItem("d4", "Economy Video Notes — RBI",       "Economy",      DownloadType.VIDEO, 180f,  "Yesterday",      DownloadStatus.PAUSED,      0.3f,  "58m"),
    DownloadItem("d5", "Bihar GK Handbook 2026",          "Bihar GK",     DownloadType.BOOK,  22.5f, "2 days ago",     DownloadStatus.COMPLETED),
    DownloadItem("d6", "Modern India — Complete Notes",   "History",      DownloadType.PDF,   9.8f,  "3 days ago",     DownloadStatus.COMPLETED),
    DownloadItem("d7", "BPSC 68th Previous Year Paper",  "All Subjects", DownloadType.PYQ,   2.0f,  "1 week ago",     DownloadStatus.COMPLETED),
    DownloadItem("d8", "Geography Master Notes",          "Geography",    DownloadType.PDF,   11.2f, "1 week ago",     DownloadStatus.COMPLETED),
    DownloadItem("d9", "Bihar GK Video Lecture 1",        "Bihar GK",     DownloadType.VIDEO, 320f,  "2 weeks ago",    DownloadStatus.COMPLETED, 1f, "2h 10m"),
)

@Composable
fun DownloadsScreen(navController: NavHostController) {
    var selectedType    by remember { mutableStateOf<DownloadType?>(null) }
    var searchQuery     by remember { mutableStateOf("") }
    val deleted         = remember { mutableStateListOf<String>() }
    val paused          = remember { mutableStateListOf<String>() }

    val filtered = mockDownloads.filter { item ->
        !deleted.contains(item.id) &&
                (selectedType == null || item.type == selectedType) &&
                (searchQuery.isEmpty() || item.title.contains(searchQuery, ignoreCase = true) || item.subject.contains(searchQuery, ignoreCase = true))
    }

    val totalSizeMb = filtered.filter { it.status == DownloadStatus.COMPLETED }.sumOf { it.sizeMb.toDouble() }.toFloat()
    val maxStorageMb = 2048f // 2GB assumed
    val storageProgress = (totalSizeMb / maxStorageMb).coerceIn(0f, 1f)
    val animStorage by animateFloatAsState(storageProgress, tween(1000), label = "stor")

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)), Offset(0f, 0f), Offset(400f, 400f)))
                    .statusBarsPadding()
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(0.05f), 160.dp.toPx(), Offset(size.width + 20.dp.toPx(), -50.dp.toPx()))
                }
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { navController.popBackStack() }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Downloads", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text("${filtered.size} files · ${String.format("%.1f", totalSizeMb)} MB used", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                            }
                        }
                    }

                    // Storage bar
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Storage Used", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                            Text("${String.format("%.1f", totalSizeMb)} MB / ${String.format("%.0f", maxStorageMb / 1024)} GB", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(0.2f))) {
                            Box(modifier = Modifier.fillMaxWidth(animStorage).fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(Color(0xFF64B5F6), Color.White)), RoundedCornerShape(4.dp)))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(DownloadType.PDF, DownloadType.VIDEO, DownloadType.PYQ, DownloadType.BOOK).forEach { type ->
                                val count = filtered.count { it.type == type && it.status == DownloadStatus.COMPLETED }
                                if (count > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(type.emoji, fontSize = 10.sp)
                                    Text("$count ${type.label}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f), fontSize = 9.sp)
                                }
                            }
                        }
                    }

                    // Search
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(0.15f))
                        .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Search, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
                        androidx.compose.foundation.text.BasicTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White), singleLine = true,
                            decorationBox = { inner -> if (searchQuery.isEmpty()) Text("Search downloads...", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.5f)); inner() })
                        if (searchQuery.isNotEmpty()) Icon(Icons.Rounded.Close, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(16.dp).clickable { searchQuery = "" })
                    }
                }
            }

            // Type filter
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (selectedType == null) BpscColors.Primary else Color.White)
                        .border(1.dp, if (selectedType == null) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(20.dp))
                        .clickable { selectedType = null }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                        Text("All", style = MaterialTheme.typography.bodyMedium, color = if (selectedType == null) Color.White else BpscColors.TextSecondary, fontWeight = if (selectedType == null) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                items(DownloadType.values()) { type ->
                    val sel = selectedType == type
                    Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (sel) type.color else Color.White)
                        .border(1.dp, if (sel) type.color else BpscColors.Divider, RoundedCornerShape(20.dp))
                        .clickable { selectedType = if (sel) null else type }.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(type.emoji, fontSize = 12.sp)
                        Text(type.label, style = MaterialTheme.typography.bodyMedium, color = if (sel) Color.White else BpscColors.TextSecondary, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📭", fontSize = 48.sp)
                        Text("No downloads yet", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Download notes, papers and videos for offline access", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Active downloads section
                    val active = filtered.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PAUSED }
                    if (active.isNotEmpty()) {
                        item { SectionLabel("⬇️ Active Downloads", "${active.size} files") }
                        items(active) { item ->
                            DownloadCard(
                                item      = item,
                                isPaused  = paused.contains(item.id) || item.status == DownloadStatus.PAUSED,
                                onPause   = { if (paused.contains(item.id)) paused.remove(item.id) else paused.add(item.id) },
                                onDelete  = { deleted.add(item.id) }
                            )
                        }
                        item { Spacer(Modifier.height(6.dp)) }
                    }

                    // Completed
                    val done = filtered.filter { it.status == DownloadStatus.COMPLETED }
                    if (done.isNotEmpty()) {
                        item { SectionLabel("✅ Downloaded", "${done.size} files") }
                        items(done) { item ->
                            DownloadCard(item = item, isPaused = false, onPause = {}, onDelete = { deleted.add(item.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
    }
}

@Composable
private fun DownloadCard(item: DownloadItem, isPaused: Boolean, onPause: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val animProgress by animateFloatAsState(item.progress, tween(800), label = "prog${item.id}")

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(item.type.bg), contentAlignment = Alignment.Center) {
                    Text(item.type.emoji, fontSize = 22.sp)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.type.label, style = MaterialTheme.typography.labelSmall, color = item.type.color, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(item.type.bg).padding(horizontal = 6.dp, vertical = 2.dp))
                        Text("·", color = BpscColors.TextHint)
                        if (item.duration != null) { Icon(Icons.Rounded.PlayCircle, null, tint = BpscColors.TextHint, modifier = Modifier.size(11.dp)); Text(item.duration, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp); Text("·", color = BpscColors.TextHint) }
                        Icon(Icons.Rounded.Storage, null, tint = BpscColors.TextHint, modifier = Modifier.size(11.dp))
                        Text("${item.sizeMb} MB", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                    }
                    Text(item.downloadedDate, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                }
                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED) {
                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(BpscColors.PrimaryLight).clickable(onClick = onPause), contentAlignment = Alignment.Center) {
                            Icon(if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null, tint = BpscColors.Primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFFEE8E8)).clickable { showDeleteDialog = true }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Delete, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Progress bar for downloading
            if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isPaused) "⏸ Paused" else "⬇️ Downloading...", style = MaterialTheme.typography.bodyMedium,
                            color = if (isPaused) BpscColors.TextHint else BpscColors.Primary, fontWeight = FontWeight.SemiBold)
                        Text("${(item.progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(BpscColors.Surface)) {
                        Box(modifier = Modifier.fillMaxWidth(animProgress).fillMaxHeight()
                            .background(if (isPaused) BpscColors.TextHint else BpscColors.Primary, RoundedCornerShape(3.dp)))
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor   = Color.White,
            shape            = RoundedCornerShape(20.dp),
            title  = { Text("Delete Download?", fontWeight = FontWeight.Bold) },
            text   = { Text("\"${item.title}\" will be removed from your device. You can download it again anytime.") },
            confirmButton = {
                Button(onClick = { onDelete(); showDeleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)), shape = RoundedCornerShape(10.dp)) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }, shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
            }
        )
    }
}