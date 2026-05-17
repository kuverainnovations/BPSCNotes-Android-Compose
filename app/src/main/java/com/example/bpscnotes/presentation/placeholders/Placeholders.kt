package com.example.bpscnotes.presentation.placeholders

import android.content.Intent
import android.net.Uri
import android.os.Environment
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.*
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ════════════════════════════════════════════════════════════
// FILE: presentation/placeholders/Placeholders.kt
// DownloadsScreen + SubscriptionScreen — fully dynamic
// ════════════════════════════════════════════════════════════

// ── DownloadsViewModel ────────────────────────────────────────
data class DownloadsUiState(
    val downloads:      List<DownloadedFileItem> = emptyList(),
    val isLoading:      Boolean                  = true,
    val isRefreshing:   Boolean                  = false,
    val totalSizeMb:    Float                    = 0f,
    val error:          String?                  = null,
    val toastMessage:   String?                  = null,
    val deletingId:     String?                  = null
)

data class DownloadedFileItem(
    val id:           String,
    val title:        String,
    val subject:      String,
    val materialType: String,
    val fileSizeMb:   Float,
    val downloadedAt: String,
    val localPath:    String,    // absolute path on device
    val fileExists:   Boolean    // file still on disk?
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val materialsApi: StudyMaterialsApiService
) : ViewModel() {

    private val _state = MutableStateFlow(DownloadsUiState())
    val state: StateFlow<DownloadsUiState> = _state.asStateFlow()

    private val downloadsDir: File =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "BPSCNotes")

    init { load() }

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(
                isLoading   = !refresh && it.downloads.isEmpty(),
                isRefreshing = refresh
            )}
            try {
                // 1. Get server-side download history (what user has downloaded)
                val serverHistory = try {
                    materialsApi.myUploads().data?.uploads ?: emptyList()
                } catch (e: Exception) { emptyList() }

                // 2. Cross-reference with actual files on device
                val localFiles = if (downloadsDir.exists())
                    downloadsDir.walkTopDown().filter { it.isFile && it.extension == "pdf" }.toList()
                else emptyList()

                val items = mutableListOf<DownloadedFileItem>()

                // Add items from server history that have local files
                serverHistory.forEach { dto ->
                    val safeName = dto.title.replace("[^a-zA-Z0-9]".toRegex(), "_") + ".pdf"
                    val localFile = localFiles.firstOrNull { it.name.contains(dto.id.take(8)) || it.name == safeName }
                    if (localFile != null || dto.downloadUrl != null) {
                        items.add(DownloadedFileItem(
                            id           = dto.id,
                            title        = dto.title,
                            subject      = dto.subject,
                            materialType = dto.materialType,
                            fileSizeMb   = localFile?.length()?.div(1048576f) ?: dto.fileSizeMb,
                            downloadedAt = dto.uploadedDate ?: "",
                            localPath    = localFile?.absolutePath ?: "",
                            fileExists   = localFile?.exists() == true
                        ))
                    }
                }

                // Also include local files not in server history (e.g. renamed)
                localFiles.filter { f -> items.none { it.localPath == f.absolutePath } }.forEach { f ->
                    items.add(DownloadedFileItem(
                        id           = f.nameWithoutExtension,
                        title        = f.nameWithoutExtension.replace("_", " "),
                        subject      = "Unknown",
                        materialType = "pdf",
                        fileSizeMb   = f.length() / 1048576f,
                        downloadedAt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(f.lastModified())),
                        localPath    = f.absolutePath,
                        fileExists   = true
                    ))
                }

                val totalMb = items.sumOf { it.fileSizeMb.toDouble() }.toFloat()

                _state.update { it.copy(
                    downloads    = items.sortedByDescending { i -> i.downloadedAt },
                    totalSizeMb  = totalMb,
                    isLoading    = false,
                    isRefreshing = false,
                    error        = null
                )}
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    fun deleteFile(item: DownloadedFileItem) {
        viewModelScope.launch {
            _state.update { it.copy(deletingId = item.id) }
            try {
                val file = File(item.localPath)
                if (file.exists()) file.delete()
                _state.update { s -> s.copy(
                    deletingId   = null,
                    downloads    = s.downloads.filterNot { it.id == item.id },
                    totalSizeMb  = s.totalSizeMb - item.fileSizeMb,
                    toastMessage = "Deleted '${item.title}'"
                )}
            } catch (e: Exception) {
                _state.update { it.copy(deletingId = null, toastMessage = "Failed to delete file") }
            }
        }
    }

    fun clearToast() = _state.update { it.copy(toastMessage = null) }
    fun refresh()    = load(refresh = true)
}

// ════════════════════════════════════════════════════════════
// DOWNLOADS SCREEN
// ════════════════════════════════════════════════════════════
@Composable
fun DownloadsScreen(
    nav: NavHostController,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val state       by viewModel.state.collectAsState()
    val context      = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearToast()
        }
    }


    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }, containerColor = BpscColors.Surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(BpscColors.Surface)) {

            // ── Header ─────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(
                    listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)),
                    Offset(0f, 0f), Offset(500f, 300f)))) {
                Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 46.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(Color.White.copy(0.15f))
                                .clickable { nav.popBackStack() },
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("My Downloads", style = MaterialTheme.typography.titleLarge,
                                    color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text("Offline study files", style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(0.7f))
                            }
                        }
                    }
                    // Stats strip
                    if (!state.isLoading) {
                        Row(modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(0.1f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                            DlStat("📁", "${state.downloads.size}", "Files")
                            DlStat("💾", "${"%.1f".format(state.totalSizeMb)} MB", "Storage")
                            DlStat("📄", "${state.downloads.count { it.materialType == "pdf" }}", "PDFs")
                            DlStat("📝", "${state.downloads.count { it.materialType == "pyq" }}", "PYQs")
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(5) {
                                Box(modifier = Modifier.fillMaxWidth().height(80.dp)
                                    .clip(RoundedCornerShape(16.dp)).background(BpscColors.Divider))
                            }
                        }
                    }
                    state.error != null && state.downloads.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("⚠️", fontSize = 40.sp)
                                Text(state.error!!, color = BpscColors.TextSecondary,
                                    style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp))
                                Button(onClick = viewModel::refresh,
                                    colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    state.downloads.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("📂", fontSize = 52.sp)
                                Text("No downloads yet", style = MaterialTheme.typography.titleLarge,
                                    color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                                Text("Download study materials to access them offline",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = BpscColors.TextSecondary, textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp))
                                Button(
                                    onClick = { nav.navigate(Screen.StudyMaterials.route) },
                                    colors  = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                                    shape   = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Download, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Browse Study Materials")
                                }
                            }
                        }
                    }
                    else -> {
                        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.downloads, key = { it.id }) { item ->
                                DownloadedFileCard(
                                    item       = item,
                                    isDeleting = state.deletingId == item.id,
                                    onOpen     = {
                                        if (item.fileExists) {
                                            val file = File(item.localPath)
                                            val uri  = androidx.core.content.FileProvider.getUriForFile(
                                                context, "${context.packageName}.fileprovider", file)
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "application/pdf")
                                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            }
                                            context.startActivity(intent)
                                        }
                                    },
                                    onDelete   = { viewModel.deleteFile(item) }
                                )
                            }
                        }
                    }
                }
                //PullRefreshIndicator(state.isRefreshing, pullState, modifier = Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun DlStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(icon, fontSize = 14.sp)
        Text(value, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp)
    }
}

@Composable
private fun DownloadedFileCard(
    item: DownloadedFileItem, isDeleting: Boolean,
    onOpen: () -> Unit, onDelete: () -> Unit
) {
    val typeEmoji = when (item.materialType) { "pyq" -> "📝"; "book" -> "📚"; "video" -> "🎬"; else -> "📄" }
    val typeColor = when (item.materialType) { "pyq" -> Color(0xFF9B59B6); "book" -> Color(0xFF1565C0); else -> Color(0xFFE74C3C) }
    val typeBg    = when (item.materialType) { "pyq" -> Color(0xFFF3E8FD); "book" -> Color(0xFFE8F0FD); else -> Color(0xFFFEE8E8) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Icon
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(typeBg),
                contentAlignment = Alignment.Center) {
                if (isDeleting) CircularProgressIndicator(color = typeColor, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text(typeEmoji, fontSize = 22.sp)
            }

            // Info
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, color = BpscColors.TextPrimary,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.subject, style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💾 ${"%.1f".format(item.fileSizeMb)} MB",
                        style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                    if (!item.fileExists)
                        Text("⚠️ File missing", style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE74C3C), fontSize = 10.sp)
                    else
                        Text("✓ On device", style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.Success, fontSize = 10.sp)
                }
            }

            // Actions
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape)
                    .background(if (item.fileExists) BpscColors.PrimaryLight else BpscColors.Divider)
                    .clickable(enabled = item.fileExists, onClick = onOpen),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.OpenInNew, null,
                        tint = if (item.fileExists) BpscColors.Primary else BpscColors.TextHint,
                        modifier = Modifier.size(15.dp))
                }
                Box(modifier = Modifier.size(32.dp).clip(CircleShape)
                    .background(Color(0xFFFEE8E8))
                    .clickable(enabled = !isDeleting, onClick = onDelete),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Delete, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// SUBSCRIPTION / PAID CONTENT SCREEN
// ════════════════════════════════════════════════════════════
data class SubscriptionUiState(
    val premiumMaterials: List<StudyMaterialDto> = emptyList(),
    val premiumCourses:   List<CourseDto>         = emptyList(),
    val isLoading:        Boolean                 = true,
    val isRefreshing:     Boolean                 = false,
    val isPremiumUser:    Boolean                 = false,
    val error:            String?                 = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val materialsApi: StudyMaterialsApiService,
    private val coursesApi:   CoursesApiService
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionUiState())
    val state: StateFlow<SubscriptionUiState> = _state.asStateFlow()

    init { load() }

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = !refresh, isRefreshing = refresh) }
            try {
                // Load premium materials
                val matsRes = materialsApi.list(limit = 20, sort = "downloads")
                val premiumMats = matsRes.data?.materials?.filter { it.isPremium } ?: emptyList()

                // Load paid courses
                val coursesRes = try { coursesApi.getCourses().data?.courses ?: emptyList() } catch (e: Exception) { emptyList() }
                val paidCourses = coursesRes.filter { it.is_paid }

                _state.update { it.copy(
                    premiumMaterials = premiumMats,
                    premiumCourses   = paidCourses,
                    isLoading        = false,
                    isRefreshing     = false,
                    error            = null
                )}
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    fun refresh() = load(refresh = true)
}

@Composable
fun SubscriptionScreen(
    nav: NavHostController,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val state     by viewModel.state.collectAsState()

    Scaffold(containerColor = BpscColors.Surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(BpscColors.Surface)) {

            // ── Header ─────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(
                    listOf(Color(0xFF6A0DAD), Color(0xFF9B59B6), Color(0xFFBA68C8)),
                    Offset(0f, 0f), Offset(500f, 300f)))
             ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 46.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(Color.White.copy(0.15f))
                                .clickable { nav.popBackStack() },
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Premium Content", style = MaterialTheme.typography.titleLarge,
                                    color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text("Exclusive notes, papers & courses",
                                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
                            }
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(0.2f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text("⭐ PRO", style = MaterialTheme.typography.labelSmall,
                                color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    // PRO card
                    Card(shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.15f)),
                        border = BorderStroke(1.dp, Color.White.copy(0.3f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("BPSCNotes Pro", style = MaterialTheme.typography.titleMedium,
                                    color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text("All premium content · No ads · Priority support",
                                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SubPill("📄 Premium PDFs")
                                    SubPill("📚 All Books")
                                    SubPill("🎬 Videos")
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹299", style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text("/month", style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(0.7f))
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> SubLoadingGrid()
                    else -> {
                        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {

                            // Premium Materials
                            if (state.premiumMaterials.isNotEmpty()) {
                                item {
                                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Text("📄 Premium Notes & Papers",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                                        TextButton(onClick = { nav.navigate(Screen.StudyMaterials.route) }) {
                                            Text("See all", color = BpscColors.Primary)
                                        }
                                    }
                                }
                                items(state.premiumMaterials, key = { it.id }) { mat ->
                                    PremiumMaterialCard(material = mat,
                                        onClick = { nav.navigate(Screen.StudyMaterials.route) })
                                }
                            }

                            // Premium Courses
                            if (state.premiumCourses.isNotEmpty()) {
                                item {
                                    Spacer(Modifier.height(8.dp))
                                    Text("🎓 Premium Courses",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                                }
                                items(state.premiumCourses, key = { it.id }) { course ->
                                    PremiumCourseCard(course = course,
                                        onClick = { nav.navigate(Screen.CourseDetail.createRoute(course.id)) })
                                }
                            }

                            if (state.premiumMaterials.isEmpty() && state.premiumCourses.isEmpty()) {
                                item {
                                    Box(Modifier.fillParentMaxSize(), Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("⭐", fontSize = 52.sp)
                                            Text("No premium content yet",
                                                style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary)
                                            Text("Check back soon for exclusive content",
                                                style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubPill(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 9.sp,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp))
}

@Composable
private fun PremiumMaterialCard(material: StudyMaterialDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF8E1)), contentAlignment = Alignment.Center) {
                Text(material.type.emoji, fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐ PRO", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold,
                        fontWeight = FontWeight.ExtraBold, fontSize = 9.sp,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFF8E1)).padding(horizontal = 5.dp, vertical = 2.dp))
                    Text(material.subject, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                }
                Text(material.title, style = MaterialTheme.typography.bodyMedium,
                    color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("⬇️ ${material.downloadCount} downloads · ⭐ ${material.rating}",
                    style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
            }
            Icon(Icons.Rounded.Lock, null, tint = BpscColors.CoinGold, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PremiumCourseCard(course: CourseDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF3E5F5)), contentAlignment = Alignment.Center) {
                Text("🎓", fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(course.title, style = MaterialTheme.typography.bodyMedium,
                    color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(course.instructor ?: "BPSCNotes Team",
                    style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${course.price?.toInt() ?: 0}",
                    style = MaterialTheme.typography.titleSmall, color = Color(0xFF7B1FA2), fontWeight = FontWeight.ExtraBold)
                Icon(Icons.Rounded.ChevronRight, null, tint = BpscColors.TextHint, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SubLoadingGrid() {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(6) {
            Box(modifier = Modifier.fillMaxWidth().height(70.dp)
                .clip(RoundedCornerShape(14.dp)).background(BpscColors.Divider))
        }
    }
}

// Kept for backward compat
@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$title\n(Coming Soon)", style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun NotesReaderScreen(nav: NavHostController, noteId: String) = PlaceholderScreen("Notes Reader")
