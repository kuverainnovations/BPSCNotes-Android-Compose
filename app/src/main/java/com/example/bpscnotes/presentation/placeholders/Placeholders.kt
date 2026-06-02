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
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.*
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
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
    private val materialsApi: StudyMaterialsApiService,
    private val tokenStore:   com.example.bpscnotes.data.local.TokenStore,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _state = MutableStateFlow(DownloadsUiState())
    val state: StateFlow<DownloadsUiState> = _state.asStateFlow()

    // FIX: Scan the root Downloads directory — that's where DownloadManager saves files
    // by default (setDestinationInExternalPublicDir(DIRECTORY_DOWNLOADS, fileName)).
    // The BPSCNotes subfolder doesn't exist / is never populated by our download code.
    init { load() }

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(
                isLoading    = !refresh && it.downloads.isEmpty(),
                isRefreshing = refresh
            )}
            try {
                val downloadHistory = try {
                    materialsApi.getMyDownloads().data?.downloads ?: emptyList()
                } catch (e: Exception) { emptyList() }

                val items = mutableListOf<DownloadedFileItem>()

                downloadHistory.forEach { dto ->
                    // Look up app-private local path saved by StudyMaterialsViewModel
                    // File is at context.filesDir/materials/<id>.<ext>
                    val localPath = tokenStore.getLocalPath(dto.id)
                    val localFile = if (localPath != null) java.io.File(localPath) else null
                    val fileExists = localFile?.exists() == true

                    items.add(DownloadedFileItem(
                        id           = dto.id,
                        title        = dto.title,
                        subject      = dto.subject,
                        materialType = dto.materialType,
                        fileSizeMb   = if (fileExists) localFile!!.length() / 1048576f
                        else dto.fileSizeBytes / 1048576f,
                        downloadedAt = dto.downloadedAt,
                        localPath    = localPath ?: "",
                        fileExists   = fileExists
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
                // Remove from TokenStore so the download button resets
                tokenStore.removeDownloadedId(item.id)
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
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val context      = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearToast()
        }
    }


    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }, containerColor = cs.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(cs.background)) {

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
                                .clickable { nav.popBackStackSafe() },
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(str.downloadsTitle, style = MaterialTheme.typography.titleLarge,
                                    color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text(str.downloadsSubtitle, style = MaterialTheme.typography.bodySmall,
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
                                    .clip(RoundedCornerShape(16.dp)).background(cs.outline))
                            }
                        }
                    }
                    state.error != null && state.downloads.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("⚠️", fontSize = 40.sp)
                                Text(state.error!!, color = cs.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp))
                                Button(onClick = viewModel::refresh,
                                    colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                                    Text(str.retry)
                                }
                            }
                        }
                    }
                    state.downloads.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("📂", fontSize = 52.sp)
                                Text(str.downloadsNone, style = MaterialTheme.typography.titleLarge,
                                    color = cs.onSurface, fontWeight = FontWeight.Bold)
                                Text(str.downloadsNoneHint,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = cs.onSurfaceVariant, textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp))
                                Button(
                                    onClick = { nav.navigate(Screen.StudyMaterials.route) },
                                    colors  = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                                    shape   = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Download, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(str.downloadsBrowse)
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
                                        if (item.fileExists && item.localPath.isNotBlank()) {
                                            // Open in in-app viewer using file:// URI — private storage
                                            val localUrl = "file://${item.localPath}"
                                            when (item.materialType.lowercase()) {
                                                "video" -> nav.navigate(
                                                    Screen.VideoPlayer.createRoute(localUrl, item.title)
                                                )
                                                else -> nav.navigate(
                                                    Screen.PdfViewer.createRoute(
                                                        fileUrl     = localUrl,
                                                        title       = item.title,
                                                        freePages   = Int.MAX_VALUE,
                                                        isPurchased = true
                                                    )
                                                )
                                            }
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val typeEmoji = when (item.materialType) { "pyq" -> "📝"; "book" -> "📚"; "video" -> "🎬"; else -> "📄" }
    val typeColor = when (item.materialType) { "pyq" -> Color(0xFF9B59B6); "book" -> Color(0xFF1565C0); else -> Color(0xFFE74C3C) }
    val typeBg    = when (item.materialType) { "pyq" -> Color(0xFFF3E8FD); "book" -> Color(0xFFE8F0FD); else -> Color(0xFFFEE8E8) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
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
                Text(item.title, style = MaterialTheme.typography.titleSmall, color = cs.onSurface,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.subject, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💾 ${"%.1f".format(item.fileSizeMb)} MB",
                        style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                    if (!item.fileExists)
                        Text(str.placeholderFileMissing, style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE74C3C), fontSize = 10.sp)
                    else
                        Text(str.placeholderOnDevice, style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.Success, fontSize = 10.sp)
                }
            }

            // Actions
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape)
                    .background(if (item.fileExists) BpscColors.PrimaryLight else cs.outline)
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
data class SubscriptionPlanItem(
    val id: String, val name: String, val price: Int,
    val originalPrice: Int = 0, val duration: String,
    val billingCycle: String = "", val bonusCoins: Int = 0, val savings: Int = 0
)

data class SubscriptionUiState(
    val premiumMaterials: List<StudyMaterialDto>    = emptyList(),
    val premiumCourses:   List<CourseDto>           = emptyList(),
    val plans:            List<SubscriptionPlanItem> = emptyList(),
    val activePlan:       String?                   = null,   // current user's plan
    val isLoading:        Boolean                   = true,
    val isRefreshing:     Boolean                   = false,
    val isPremiumUser:    Boolean                   = false,
    val error:            String?                   = null
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
                // FIX: Use backend isPremium=true filter — shows ONLY premium content
                // Previously loaded all materials and filtered client-side → showed non-premium too
                val matsRes = materialsApi.list(limit = 20, sort = "downloads", isPremium = true)
                val premiumMats = matsRes.data?.materials ?: emptyList()

                // Load paid courses
                val coursesRes = try { coursesApi.getCourses().data?.courses ?: emptyList() } catch (e: Exception) { emptyList() }
                val paidCourses = coursesRes.filter { it.isPaid }

                // Load subscription plans from backend
                val plansRes = try {
                    coursesApi.getSubscriptionPlans().data?.plans ?: emptyList()
                } catch (_: Exception) { emptyList() }

                val planItems = plansRes.map { p ->
                    SubscriptionPlanItem(
                        id            = p.id ?: "monthly",
                        name          = p.name ?: "Monthly",
                        price         = p.price ?: 199,
                        originalPrice = p.originalPrice ?: 299,
                        duration      = p.duration ?: "1 Month",
                        billingCycle  = p.billingCycle ?: "",
                        bonusCoins    = p.bonusCoins ?: 0,
                        savings       = p.savings ?: 0
                    )
                }.ifEmpty {
                    // Hardcoded fallback matching backend PLANS constant
                    listOf(
                        SubscriptionPlanItem("monthly", "Monthly", 199, 299, "1 Month", "Billed monthly", 20, 100),
                        SubscriptionPlanItem("quarterly", "Quarterly", 499, 699, "3 Months", "Billed every 3 months", 50, 200),
                        SubscriptionPlanItem("annual", "Annual", 1499, 2388, "12 Months", "Billed annually", 100, 889)
                    )
                }

                _state.update { it.copy(
                    premiumMaterials = premiumMats,
                    premiumCourses   = paidCourses,
                    plans            = planItems,
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val state     by viewModel.state.collectAsState()

    Scaffold(containerColor = cs.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(cs.background)) {

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
                                .clickable { nav.popBackStackSafe() },
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(str.premium + " Content", style = MaterialTheme.typography.titleLarge,
                                    color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text(str.placeholdersExclusiveNotes,
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

                    // FIX: Dynamic plan card from API
                    val featuredPlan = state.plans.firstOrNull() ?: SubscriptionPlanItem(
                        "monthly", "BPSCNotes Pro", 199, 299, "1 Month", str.placeholdersBilledMonthly, 20, 100)
                    // FIX: Premium card is now clickable — shows enroll/subscribe dialog
                    var showEnrollDialog by remember { mutableStateOf(false) }
                    if (showEnrollDialog) {
                        AlertDialog(
                            onDismissRequest = { showEnrollDialog = false },
                            shape = RoundedCornerShape(20.dp),
                            title = { Text(str.placeholderGetPro, fontWeight = FontWeight.ExtraBold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(str.placeholdersUnlockAll)
                                    val p = state.plans.firstOrNull()
                                    if (p != null) {
                                        Text(str.placeholderPremiumPdfs, style = MaterialTheme.typography.bodyMedium)
                                        Text(str.placeholderOfflineDownloads, style = MaterialTheme.typography.bodyMedium)
                                        Text(str.placeholdersPrioritySupport, style = MaterialTheme.typography.bodyMedium)
                                        if (p.bonusCoins > 0) Text("✅ +${p.bonusCoins} bonus coins on signup", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(Modifier.height(4.dp))
                                        Text("₹${p.price}/${p.duration} — ${p.billingCycle}", fontWeight = FontWeight.Bold, color = BpscColors.Primary)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    showEnrollDialog = false
                                    nav.navigate(Screen.Payment.route)
                                }, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                                    Text(str.paymentSubscribe + " →")
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { showEnrollDialog = false }) { Text(str.pdfMaybeLater) }
                            }
                        )
                    }

                    Card(
                        modifier = Modifier.clickable { showEnrollDialog = true },  // ← CLICKABLE NOW
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cs.surface.copy(0.15f)),
                        border = BorderStroke(1.dp, Color.White.copy(0.3f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("BPSCNotes ${featuredPlan.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text(str.placeholdersAllPremium,
                                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SubPill("📄 Premium PDFs")
                                    SubPill("📚 All Books")
                                    SubPill("🎬 Videos")
                                }
                                if (featuredPlan.bonusCoins > 0) {
                                    SubPill("🪙 +${featuredPlan.bonusCoins} Bonus Coins")
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (featuredPlan.originalPrice > featuredPlan.price) {
                                    Text("₹${featuredPlan.originalPrice}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(0.5f),
                                        textDecoration = TextDecoration.LineThrough)
                                }
                                Text("₹${featuredPlan.price}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text("/${featuredPlan.duration}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(0.7f))
                                if (featuredPlan.savings > 0) {
                                    Text("Save ₹${featuredPlan.savings}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                                }
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
                                        Text(str.placeholdersPremiumNotes,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                                        TextButton(onClick = { nav.navigate(Screen.StudyMaterials.route) }) {
                                            Text(str.placeholdersSeeAll2, color = BpscColors.Primary)
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
                                    Text(str.placeholdersPremiumCourses,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
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
                                            Text(str.placeholdersNoPremiumYet,
                                                style = MaterialTheme.typography.titleLarge, color = cs.onSurface)
                                            Text(str.placeholdersCheckBack,
                                                style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
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
    val cs = MaterialTheme.colorScheme
    Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 9.sp,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp))
}

@Composable
private fun PremiumMaterialCard(material: StudyMaterialDto, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
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
                    color = cs.onSurface, fontWeight = FontWeight.SemiBold,
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF3E5F5)), contentAlignment = Alignment.Center) {
                Text("🎓", fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(course.title, style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurface, fontWeight = FontWeight.SemiBold,
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(6) {
            Box(modifier = Modifier.fillMaxWidth().height(70.dp)
                .clip(RoundedCornerShape(14.dp)).background(cs.outline))
        }
    }
}

// Kept for backward compat
@Composable
fun PlaceholderScreen(title: String) {
    val str = LocalStrings.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$title\n(Coming Soon)", style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun NotesReaderScreen(nav: NavHostController, noteId: String) {
    val str = LocalStrings.current
    PlaceholderScreen(str.placeholdersNotesReader)
}