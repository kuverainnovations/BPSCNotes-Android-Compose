package com.example.bpscnotes.presentation.studymaterials

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.core.ads.BannerAdView
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.data.remote.api.*
import com.example.bpscnotes.presentation.payment.launchRazorpay

// ════════════════════════════════════════════════════════════
// FILE: presentation/studymaterials/StudyMaterialsScreen.kt
// Dynamic implementation — all data from API
// UI structure preserved from existing mockup.
// ════════════════════════════════════════════════════════════

// ─────────────────────────────────────────────────────────────
// TYPE-AWARE MATERIAL OPENER
// Routes each content type to the correct viewer:
//   PDF / PYQ / BOOK / NOTES  → PdfViewerScreen (in-app, page-locked)
//   VIDEO                     → Android's built-in video player (Intent)
//   IMAGE                     → Full-screen image viewer (Intent)
//   UNKNOWN                   → Browser fallback
// ─────────────────────────────────────────────────────────────
private fun openMaterial(
    context:      android.content.Context,
    navController: androidx.navigation.NavHostController,
    url:          String,
    title:        String,
    freePages:    Int,
    isPurchased:  Boolean,
    adManager:    com.example.bpscnotes.core.ads.AdManager? = null
) {
    val lower = url.lowercase()
    val activity = context as? android.app.Activity

    fun navigateAfterAd(destination: () -> Unit) {
        if (adManager != null && activity != null) {
            adManager.showInterstitialIfReady(activity) { destination() }
        } else {
            destination()
        }
    }

    when {
        lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") ||
                lower.endsWith(".avi") || lower.endsWith(".mov") || lower.contains("/video/") -> {
            navigateAfterAd {
                navController.navigate(Screen.VideoPlayer.createRoute(url, title))
            }
        }
        lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                lower.endsWith(".webp") || lower.endsWith(".gif") -> {
            navigateAfterAd {
                navController.navigate(Screen.ImageViewer.createRoute(url, title))
            }
        }
        else -> {
            navigateAfterAd {
                navController.navigate(
                    Screen.PdfViewer.createRoute(
                        fileUrl     = url,
                        title       = title,
                        freePages   = freePages,
                        isPurchased = isPurchased
                    )
                )
            }
        }
    }
}
@Composable
fun StudyMaterialsScreen(
    navController: NavHostController,
    adManager: com.example.bpscnotes.core.ads.AdManager? = null,
    initialTypeKey: String? = null,
    viewModel:     StudyMaterialsViewModel = hiltViewModel()
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val state       by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { com.example.bpscnotes.core.analytics.Event.screenView("study_materials") }

    // Coming from a specific item (e.g. a Premium Content card) - open
    // pre-filtered to that item's material type so the user lands near
    // what they tapped instead of an unfiltered "all materials" list.
    LaunchedEffect(initialTypeKey) {
        if (initialTypeKey != null && state.selectedType == null) {
            viewModel.selectType(com.example.bpscnotes.data.remote.api.MaterialType.fromKey(initialTypeKey))
        }
    }

    // ── Storage permission (Android 9 and below only) ──────────
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.retryPendingDownload()
        else         viewModel.onStoragePermissionDenied()
    }
    LaunchedEffect(state.needsStoragePermission) {
        if (state.needsStoragePermission && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    val snackbarHost = remember { SnackbarHostState() }
    val context      = LocalContext.current

    // Show toast messages
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearToast()
        }
    }

    // Purchase dialog
    var showPurchaseDialog by remember { mutableStateOf<StudyMaterialDto?>(null) }
    var coinsToApply by remember { mutableStateOf(0) }
    LaunchedEffect(state.purchaseSuccess) {
        state.purchaseSuccess?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearPurchaseMessages()
            showPurchaseDialog = null
        }
    }
    LaunchedEffect(state.purchaseError) {
        state.purchaseError?.let { snackbarHost.showSnackbar(it) }
    }

    // Phase 5: navigate to chat screen once a thread is ready
    LaunchedEffect(state.pendingChatId) {
        val chatId = state.pendingChatId ?: return@LaunchedEffect
        viewModel.consumePendingChat()
        navController.navigate(Screen.MaterialChat.createRoute(chatId))
    }

    // Launch Razorpay when a purchase requires payment for the remaining ₹ balance.
    // Consume the pending purchase immediately to avoid re-launching on recompose
    // (same pattern as CoursePaymentScreen).
    LaunchedEffect(state.pendingPurchase) {
        val pending = state.pendingPurchase ?: return@LaunchedEffect
        val orderId = pending.razorpayOrderId ?: return@LaunchedEffect
        val keyId   = pending.razorpayKeyId ?: return@LaunchedEffect
        if (orderId.isBlank() || keyId.isBlank()) return@LaunchedEffect

        viewModel.consumePendingPurchase()
        showPurchaseDialog = null

        launchRazorpay(
            context     = context,
            orderId     = orderId,
            keyId       = keyId,
            amount      = pending.amountDueInr,
            description = "Study material: ${pending.materialTitle ?: ""}",
            userName    = state.userName,
            userEmail   = state.userEmail,
            userPhone   = state.userPhone,
            onSuccess   = { paymentId, signature ->
                viewModel.confirmMaterialPurchase(paymentId, signature)
            },
            onFailure   = { code, msg ->
                viewModel.handleMaterialPaymentFailure(code, msg)
            },
            str
        )
    }

    showPurchaseDialog?.let { item ->
        PurchaseConfirmDialog(
            item      = item,
            isPurchasing = state.purchasingId == item.id || state.isConfirmingPurchase,
            coinsToApply = coinsToApply,
            onCoinsToApplyChange = { coinsToApply = it },
            userCoins = state.userCoins,
            maxCoinsPerPurchase = viewModel.coinsConfig.economy.maxCoinsPerPurchase,
            coinToInrRate = viewModel.coinsConfig.economy.coinToInrRate,
            onConfirm = { viewModel.purchaseMaterial(item.id, item.price ?: 0, item.title, coinsToApply) },
            onDismiss = { showPurchaseDialog = null; coinsToApply = 0; viewModel.clearPurchaseMessages() }
        )
    }

    // Marketplace rules — auto-shows on first visit, or via (i) info button
    if (state.showRulesSheet) {
        MarketplaceRulesSheet(onDismiss = viewModel::dismissRules)
    }

    // Negotiation sheet — accept/counter an admin's price offer
    state.negotiationSheet?.let { material ->
        NegotiationSheet(
            material = material,
            history = state.negotiationHistory,
            isLoading = state.isLoadingNegotiation,
            isResponding = state.isRespondingNegotiation,
            onAccept = viewModel::acceptNegotiationOffer,
            onCounter = viewModel::counterNegotiationOffer,
            onDismiss = viewModel::closeNegotiation
        )
    }

    // Seller wallet sheet — real-money marketplace earnings
    if (state.showWalletSheet) {
        WalletSheet(
            wallet = state.wallet,
            isLoading = state.isLoadingWallet,
            onDismiss = viewModel::closeWallet
        )
    }


    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }, containerColor = cs.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)) { scaffoldPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(scaffoldPadding).background(cs.background)) {

            // ── HEADER ──────────────────────────────────────────
            StudyMaterialsHeader(
                stats = state.stats,
                onBack = { navController.popBackStackSafe() },
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
                onUpload = viewModel::showUpload,
                onShowRules = viewModel::showRules
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

            // ── Tab row: Explore | My Uploads ────────────────────
            var selectedTab by remember { mutableIntStateOf(0) }

            // Reload My Uploads every time user switches to that tab
            LaunchedEffect(selectedTab) {
                if (selectedTab == 1) viewModel.loadMyUploads()
            }

            // Switch to My Uploads tab after successful upload
            LaunchedEffect(state.showUploadSheet) {
                if (!state.showUploadSheet && state.myUploads.isNotEmpty()) {
                    // Upload sheet just closed — switch to My Uploads so user sees their material
                }
            }
            LaunchedEffect(state.uploadSuccess) {
                if (state.uploadSuccess != null) {
                    selectedTab = 1  // switch to My Uploads tab after upload
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cs.surface)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                listOf(str.materialsExplore, str.materialsMyUploads).forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = index }
                            .padding(vertical = 12.dp)
                            .then(if (isSelected) Modifier.background(Color.Transparent) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                label,
                                style     = MaterialTheme.typography.titleSmall,
                                color     = if (isSelected) BpscColors.Primary else BpscColors.TextHint,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                            )
                            if (index == 1 && state.myUploads.isNotEmpty()) {
                                Text(
                                    "${state.myUploads.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BpscColors.Primary.copy(0.7f)
                                )
                            }
                            if (isSelected) {
                                Box(
                                    Modifier
                                        .padding(top = 4.dp)
                                        .fillMaxWidth(0.6f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(BpscColors.Primary)
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = cs.outline)

            // ── Tab content ───────────────────────────────────────
            if (selectedTab == 0) {
                // Explore tab
                PullToRefreshBox(
                    state = pullRefreshState,
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.refresh() }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            state.isLoadingList && state.materials.isEmpty() -> LoadingGrid()
                            state.listError != null && state.materials.isEmpty() ->
                                ErrorState(message = state.listError!!, onRetry = viewModel::refresh)
                            state.materials.isEmpty() ->
                                EmptyState(showBookmarksOnly = state.showBookmarksOnly)
                            else -> MaterialsList(
                                state     = state,
                                onView    = viewModel::openDetail,
                                onBookmark = viewModel::toggleBookmark,
                                onDownload = viewModel::downloadMaterial,
                                onPurchase = { mat -> showPurchaseDialog = mat },
                                onLoadMore = viewModel::loadMore
                            )
                        }
                    }
                }
            } else {
                // My Uploads tab — user owns everything here, no locks
                MyUploadsTab(
                    uploads   = state.myUploads,
                    isLoading = state.isLoadingList && state.myUploads.isEmpty(),
                    // Open PDF with full access — user owns their uploads, no page locks
                    onOpenPdf = { url, title, freePages, _ ->
                        openMaterial(context, navController, url, title, freePages, isPurchased = true, adManager = adManager)
                    },
                    onRefresh = { viewModel.refresh() },
                    onRespondNegotiation = { material -> viewModel.openNegotiation(material) },
                    onOpenWallet = { viewModel.openWallet() },
                    onOpenChats = { navController.navigate(Screen.ChatInbox.route) }
                )
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
        // FIX: Pass purchasedIds so sheet knows if user just bought this material
        // Full access if: purchased, is the uploader (owner), or is free
        val currentUserId = state.currentUserId  // from ViewModel / TokenStore
        val isOwner = detail.uploaderId != null && detail.uploaderId == currentUserId
        val isDetailPurchased = detail.isPurchased || state.purchasedIds.contains(detail.id) || isOwner
        MaterialDetailSheet(
            material       = detail,
            isBookmarked   = state.bookmarkedIds.contains(detail.id),
            isDownloaded   = state.downloadedIds.contains(detail.id),
            isDownloading  = state.downloadingId == detail.id,
            isPurchased    = isDetailPurchased,
            buyers         = state.buyers,
            currentUserId  = currentUserId,
            onChatWithUploader = { viewModel.openChatWithUploader(detail.id) },
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
            onOpenPdf      = { url, title, freePages, isPurchased ->
                openMaterial(
                    context, navController,
                    // Use local file path if downloaded — works offline, no network needed
                    viewModel.getLocalPath(detail.id)?.let { "file://$it" } ?: url,
                    title, freePages, isPurchased, adManager = adManager
                )
            },
            onDismiss      = viewModel::closeDetail
        )
    }

    // ── UPLOAD CANCEL CONFIRMATION DIALOG ────────────────────
    if (state.showUploadCancelDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCancelDialog,
            title = { Text("Upload in progress", fontWeight = FontWeight.Bold) },
            text  = { Text("Your file is still uploading in the background. Cancel the upload?") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmCancelUpload) {
                    Text("Cancel Upload", color = Color(0xFFE74C3C))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCancelDialog) {
                    Text("Continue Uploading", color = BpscColors.Primary)
                }
            }
        )
    }

    // ── UPLOAD SHEET ─────────────────────────────────────────────
    if (state.showUploadSheet) {
        UploadSheet(
            isUploading    = state.isUploading,
            uploadProgress = state.uploadProgress,
            uploadError    = state.uploadError,
            onSubmit       = viewModel::uploadMaterial,
            onDismiss      = viewModel::hideUpload,
            onCancel       = viewModel::confirmCancelUpload,
            onFormChange   = { t, d, s, a, tg, ty, ip, fp, p ->
                viewModel.updateUploadForm(t, d, s, a, tg, ty, ip, fp, p)
            },
            state          = state
        )
    }
}

// ════════════════════════════════════════════════════════════
// HEADER
// ════════════════════════════════════════════════════════════
@Composable
private fun StudyMaterialsHeader(stats: StatsData?, onBack: () -> Unit, onUpload: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(Brush.linearGradient(
                listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)),
                Offset(0f, 0f), Offset(500f, 500f)
            ))
        // .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 56.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f))
                        .clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(str.materialsTitle, style = MaterialTheme.typography.titleLarge,
                            color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Text(str.materialsSubtitle, style = MaterialTheme.typography.bodyMedium,
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
    showBookmarksOnly: Boolean, onToggleBookmarks: () -> Unit, onUpload: () -> Unit,
    onShowRules: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth().background(cs.surface)
        .padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // Search bar
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(cs.background).border(1.dp, cs.outline, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Search, null, tint = BpscColors.TextHint, modifier = Modifier.size(18.dp))
            BasicTextField(
                value = query, onValueChange = onQueryChange, modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text(str.materialsSearchHint,
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // (i) Rules info button — re-opens the marketplace rules sheet anytime
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape)
                        .background(BpscColors.PrimaryLight)
                        .clickable(onClick = onShowRules),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Info, contentDescription = LocalStrings.current.marketRulesInfoTooltip,
                        tint = BpscColors.Primary, modifier = Modifier.size(18.dp)
                    )
                }

                // Upload button — highlighted (filled, prominent) per marketplace rules
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(BpscColors.Primary, Color(0xFF1E88E5))))
                        .clickable(onClick = onUpload)
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.Upload, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text(str.materialsUpload, style = MaterialTheme.typography.labelLarge,
                            color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibSmallStat(icon: String, value: String, label: String) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(icon, fontSize = 11.sp)
        Text(value, style = MaterialTheme.typography.labelSmall, color = cs.onSurface,
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
    val cs = MaterialTheme.colorScheme

    val str = LocalStrings.current
    var showSubjectSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cs.surface)
            .padding(bottom = 6.dp)
    ) {
        // Row 1: Type filter chips
        LazyRow(
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                TypeChip(label = str.filterAll, emoji = "📋", selected = selectedType == null) {
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

        HorizontalDivider(color = cs.outline, thickness = 0.5.dp)

        // Row 2: Sort tabs + Subject picker — all in one line
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sort pills
            listOf("downloads" to str.materialsPopular, "newest" to str.materialsNewest).forEach { (key, label) ->
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
                    .border(1.dp, cs.outline, RoundedCornerShape(20.dp))
                    .clickable { showSubjectSheet = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    selectedSubject.ifEmpty { str.materialsFilterSubject },
                    style  = MaterialTheme.typography.labelSmall,
                    color  = if (selectedSubject.isEmpty() || selectedSubject == str.filterAll) BpscColors.TextHint else BpscColors.Primary,
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
                    str.materialsFilterSubject,
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(
        modifier              = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) BpscColors.Primary else BpscColors.Surface)
            .border(1.dp, if (selected) BpscColors.Primary else cs.outline, RoundedCornerShape(20.dp))
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
//            FilterChip(label = str.filterAll, emoji = null, selected = selected == null,
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val options = listOf("downloads" to str.materialsPopular, "newest" to str.materialsNewest, "rating" to str.materialsTopRated)
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
                    .border(1.dp, if (sel) BpscColors.Primary else cs.outline, RoundedCornerShape(20.dp))
                    .clickable { onSelect(key) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, emoji: String?, selected: Boolean,
                       color: Color, bg: Color, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(modifier = Modifier.clip(RoundedCornerShape(20.dp))
        .background(if (selected) color else Color.White)
        .border(1.dp, if (selected) color else cs.outline, RoundedCornerShape(20.dp))
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
    onPurchase: (StudyMaterialDto) -> Unit = {     },
    onLoadMore: () -> Unit
) {
    val str = LocalStrings.current
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
                    item          = item,
                    isBookmarked  = state.bookmarkedIds.contains(item.id),
                    isDownloaded  = state.downloadedIds.contains(item.id),
                    isDownloading = state.downloadingId == item.id,
                    purchasedIds  = state.purchasedIds,
                    onBookmark    = { onBookmark(item.id) },
                    onDownload    = { onDownload(item) },
                    onPurchase    = { onPurchase(item) },
                    onView        = { onView(item.id) }
                )
                Spacer(Modifier.height(10.dp))
            }
            item { Spacer(Modifier.height(6.dp)) }
        }

        sectionItems(str.materialsPinned, pinned.size, pinned)
        sectionItems(str.materialsTrending, trending.size, trending)
        sectionItems(str.materialsRecent, newItems.size, newItems)
        sectionItems(str.materialsAll, rest.size, rest)

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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
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
    purchasedIds: Set<String> = emptySet(),
    onBookmark:   () -> Unit,
    onDownload:   () -> Unit,
    onPurchase:   () -> Unit = {},
    onView:       () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    val str = LocalStrings.current
    val color = typeColor(item.type)
    val bg    = typeBg(item.type)

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onView),
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cs.surface),
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
                        color = cs.onSurface, fontWeight = FontWeight.ExtraBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                    Text(item.author ?: item.uploaderName ?: "BPSCNotes Team",
                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
                // FIX: Increased touch target from 28dp → 44dp (min recommended: 48dp)
                // Small touch targets inside a Card.clickable() cause the outer click to fire instead.
                Box(
                    modifier = Modifier
                        .size(44.dp)          // bigger touch target — was 28dp, too small
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isBookmarked) Color(0xFFFFF8E1) else BpscColors.Surface)
                        .clickable(onClick = onBookmark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = if (isBookmarked) str.materialsRemoveSaved else str.materialsSave,
                        tint     = if (isBookmarked) BpscColors.CoinGold else BpscColors.TextHint,
                        modifier = Modifier.size(20.dp)  // icon also slightly bigger
                    )
                }
            }

            // Stats row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (item.pageCount > 0) LibInfoChip(Icons.Rounded.Description, "${item.pageCount} pages")
                LibInfoChip(Icons.Rounded.Storage, if (item.fileSizeMb > 0f && item.fileSizeMb < 1f) "${(item.fileSizeMb * 1024).toInt()} KB" else "${"%.1f".format(item.fileSizeMb)} MB")
                LibInfoChip(Icons.Rounded.Download, formatCount(item.downloadCount))
                // Show price/lock badge for paid materials
                if ((item.price ?: 0) > 0) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF8E1))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("🪙", fontSize = 10.sp)
                        Text("${item.price}", style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF856404), fontWeight = FontWeight.Bold)
                    }
                }
                // Social proof: "12 students bought this" — only for paid materials with sales
                if ((item.price ?: 0) > 0 && item.buyerCount > 0) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0FDF4))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.People, null, tint = BpscColors.Success, modifier = Modifier.size(11.dp))
                        Text("${item.buyerCount} bought", style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.Success, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Rounded.Star, null, tint = BpscColors.CoinGold, modifier = Modifier.size(12.dp))
                    Text("${item.rating}", style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant, fontWeight = FontWeight.Bold)
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
                // FIX: Unlock (premium) calls onPurchase, Download (free) calls onDownload
                // FIX: Check BOTH the API field (is_purchased from backend)
                // AND the local purchasedIds set (optimistic update after buying this session)
                val isPurchased = item.isPurchased || purchasedIds.contains(item.id)
                val buttonAction: () -> Unit = when {
                    item.isPremium && !isPurchased -> { { onPurchase() } }  // Unlock = purchase
                    else                           -> { { onDownload() } }  // Free/purchased = download
                }
                Button(
                    onClick  = buttonAction,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isDownloaded              -> BpscColors.Success
                            item.isPremium && !isPurchased -> BpscColors.CoinGold  // Unlock
                            else                      -> BpscColors.Primary
                        }
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    enabled = !isDownloading
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        val icon = when {
                            isDownloaded                   -> Icons.Rounded.CheckCircle
                            item.isPremium && !isPurchased -> Icons.Rounded.Lock
                            else                           -> Icons.Rounded.Download
                        }
                        Icon(icon, null, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        when {
                            isDownloaded                   -> "Saved"
                            item.isPremium && !isPurchased -> "Unlock 🪙${item.price}"
                            else                           -> str.materialsDownload
                        },
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// MARKETPLACE RULES SHEET
// Shown automatically on first visit to Study Materials, and any
// time via the (i) info button next to Upload. Persisted via
// TokenStore.setBoolPref so the auto-show only happens once.
// ════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarketplaceRulesSheet(onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cs.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 24.dp).padding(bottom = 24.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header icon
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF1565C0), Color(0xFF42A5F5)))),
                contentAlignment = Alignment.Center
            ) {
                Text("🏪", fontSize = 30.sp)
            }

            Text(
                str.marketRulesTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = cs.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                str.marketRulesSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            HorizontalDivider(color = cs.outline, modifier = Modifier.padding(vertical = 4.dp))

            // Rule bullets
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                listOf(
                    str.marketRule1,
                    str.marketRule2,
                    str.marketRule3,
                    str.marketRule4,
                    str.marketRule5,
                ).forEach { rule ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(cs.background)
                            .padding(14.dp)
                    ) {
                        Text(
                            rule,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurface,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                Text(
                    str.marketRulesGotIt,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// NEGOTIATION SHEET
// Shown when the uploader taps the "respond" banner on a material
// that has an admin counter-offer pending. Shows the full offer
// history and lets the user Accept or send a Counter-offer.
// ════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NegotiationSheet(
    material: StudyMaterialDto,
    history: NegotiationHistoryData?,
    isLoading: Boolean,
    isResponding: Boolean,
    onAccept: () -> Unit,
    onCounter: (price: Int, message: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var counterPriceText by remember { mutableStateOf("") }
    var counterMessage by remember { mutableStateOf("") }
    var showCounterForm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cs.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "💬 Price Negotiation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = cs.onSurface
            )
            Text(
                material.title,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (isLoading || history == null) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BpscColors.Primary)
                }
            } else {
                // Current offer summary
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFEEF2FF))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Your price", style = MaterialTheme.typography.labelMedium, color = Color(0xFF6366F1))
                        Text("₹${history.originalPrice}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = cs.onSurface)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Admin's offer", style = MaterialTheme.typography.labelMedium, color = Color(0xFF6366F1))
                        Text("₹${history.currentOfferPrice}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3730A3))
                    }
                    Text(
                        "Negotiation round ${history.negotiationRound}/3" + if (history.isFinalRound) " — final round" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6366F1)
                    )
                }

                // Offer history
                if (history.history.isNotEmpty()) {
                    Text("History", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = cs.onSurface)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        history.history.forEach { offer ->
                            val isAdmin = offer.offeredBy == "admin"
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isAdmin) Color(0xFFF8FAFC) else Color(0xFFF0FDF4))
                                    .padding(10.dp)
                            ) {
                                Text(if (isAdmin) "🛡️" else "🙋", fontSize = 14.sp)
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${if (isAdmin) "Admin" else "You"} · Round ${offer.round} · ₹${offer.offerPrice}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = cs.onSurface
                                    )
                                    if (!offer.message.isNullOrBlank()) {
                                        Text(offer.message, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                if (history.canRespond) {
                    if (!showCounterForm) {
                        // Accept / Counter buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = onAccept,
                                enabled = !isResponding,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Success)
                            ) {
                                if (isResponding) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                else Text("Accept ₹${history.currentOfferPrice}", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    counterPriceText = (history.currentOfferPrice ?: history.originalPrice).toString()
                                    showCounterForm = true
                                },
                                enabled = !isResponding && !history.isFinalRound,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Counter")
                            }
                        }
                        if (history.isFinalRound) {
                            Text(
                                "This is the final round — you can only Accept or wait for our team's final decision.",
                                style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.TextHint
                            )
                        }
                    } else {
                        // Counter-offer form
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = counterPriceText,
                                onValueChange = { counterPriceText = it.filter { c -> c.isDigit() } },
                                label = { Text("Your counter price (₹)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = counterMessage,
                                onValueChange = { counterMessage = it },
                                label = { Text("Message (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2, maxLines = 3
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { showCounterForm = false },
                                    enabled = !isResponding,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Cancel") }
                                Button(
                                    onClick = {
                                        val price = counterPriceText.toIntOrNull() ?: return@Button
                                        onCounter(price, counterMessage.ifBlank { null })
                                    },
                                    enabled = !isResponding && counterPriceText.toIntOrNull() != null,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                                ) {
                                    if (isResponding) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    else Text("Send Counter-Offer", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "Waiting for our team to review your response.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// WALLET SHEET
// Shows the seller's ₹ wallet balance (real-money marketplace
// earnings, separate from coins) and transaction history with
// pending/disbursed/failed status per the marketplace spec.
// ════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletSheet(
    wallet: WalletData?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cs.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(20.dp)
                .heightIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "💰 Seller Wallet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = cs.onSurface
            )
            Text(
                "Real-money earnings from your marketplace sales — separate from your coins.",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )

            if (isLoading || wallet == null) {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BpscColors.Primary)
                }
            } else {
                // Balance card
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Available Balance", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(0.8f))
                    Text("₹${wallet.balance}", style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold, color = Color.White)
                    HorizontalDivider(color = Color.White.copy(0.2f))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Total earned", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.7f))
                        Text("₹${wallet.totalEarned}", style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Text("Transaction History", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = cs.onSurface)

                if (wallet.transactions.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions yet — sell a material to earn your first payout!",
                            style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                        items(wallet.transactions, key = { it.id }) { txn ->
                            val (statusEmoji, statusColor) = when (txn.status) {
                                "disbursed" -> "✅" to BpscColors.Success
                                "pending"   -> "⏳" to Color(0xFFF59E0B)
                                "failed"    -> "❌" to Color(0xFFE74C3C)
                                else        -> "•" to BpscColors.TextHint
                            }
                            val isCredit = txn.type == "sale_credit"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cs.background)
                                    .padding(12.dp)
                            ) {
                                Text(statusEmoji, fontSize = 16.sp)
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        txn.materialTitle ?: txn.description ?: (if (isCredit) "Sale credit" else txn.type),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = cs.onSurface,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${txn.status.replaceFirstChar { it.uppercase() }}" +
                                                (txn.createdAt?.take(10)?.let { " · $it" } ?: ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor
                                    )
                                }
                                Text(
                                    "${if (isCredit) "+" else "−"}₹${txn.amount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isCredit) BpscColors.Success else Color(0xFFE74C3C)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialDetailSheet(
    material:     MaterialDetailData,
    isBookmarked: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isPurchased:  Boolean = false,   // FIX: true if user has purchased this session
    buyers:       BuyersData? = null,   // Phase 4: anonymized buyer names for social proof
    currentUserId: String = "",         // Phase 5: to hide chat button on own uploads
    onChatWithUploader: () -> Unit = {}, // Phase 5: opens chat thread with uploader
    onBookmark:   () -> Unit,
    onDownload:   () -> Unit,
    onOpenPdf:     (url: String, title: String, freePages: Int, isPurchased: Boolean) -> Unit,
    onDismiss:    () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val color = typeColor(material.type)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = cs.surface, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
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
                    // FIX: uploaderName from backend, with sensible fallback
                    Text("By ${material.uploaderName?.ifBlank { null } ?: material.author?.ifBlank { null } ?: "BPSCNotes"} · ${material.uploadedDate?.take(10) ?: ""}",
                        style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (material.pageCount > 0) SheetStatWhite("📄", "${material.pageCount} pages")
                        SheetStatWhite("💾", run { val mb = material.fileSizeBytes / 1048576f; if (mb > 0f && mb < 1f) "${(mb * 1024).toInt()} KB" else "${"%.1f".format(mb)} MB" })
                        SheetStatWhite("⬇️", formatCount(material.downloadCount))
                        SheetStatWhite("⭐", "${material.rating}")
                    }
                }
            }

            // Body
            Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                .padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                if (!material.description.isNullOrEmpty()) {
                    Text(str.materialAbout, style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface, fontWeight = FontWeight.Bold)
                    Text(material.description, style = MaterialTheme.typography.bodyLarge,
                        color = cs.onSurfaceVariant, lineHeight = 24.sp)
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

                // Social proof: "Rahul K. and 11 others bought this" — only for paid
                // materials with at least one purchase.
                if (material.price > 0 && material.buyerCount > 0) {
                    val names = buyers?.buyers ?: emptyList()
                    val totalBuyers = buyers?.totalBuyers ?: material.buyerCount
                    val label = when {
                        names.isEmpty() -> "$totalBuyers ${if (totalBuyers == 1) "student" else "students"} bought this"
                        names.size == 1 && totalBuyers == 1 -> "${names[0]} bought this"
                        totalBuyers > names.size -> "${names[0]} and ${totalBuyers - 1} other${if (totalBuyers - 1 != 1) "s" else ""} bought this"
                        names.size == 1 -> "${names[0]} bought this"
                        else -> "${names[0]} and ${names.size - 1} other${if (names.size - 1 != 1) "s" else ""} bought this"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0FDF4))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.People, null, tint = BpscColors.Success, modifier = Modifier.size(16.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.Success, fontWeight = FontWeight.Bold)
                    }
                }

                // Preview area / open PDF button
                val downloadUrl = material.resolvedUrl
                Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))
                    .background(cs.background).border(1.dp, cs.outline, RoundedCornerShape(16.dp))
                    .then(if (!downloadUrl.isNullOrBlank()) Modifier.clickable {
                        // FIX: use runtime isPurchased (includes purchases made this session)
                        onOpenPdf(downloadUrl, material.title, material.freePages,
                            isPurchased || material.isFree)
                    } else Modifier),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(material.type.emoji, fontSize = 44.sp)
                        Text(if (!downloadUrl.isNullOrBlank()) str.materialsTapOpen else str.materialsNoPreview,
                            style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant)
                        Text(str.materialsOpenPdf,
                            style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint)
                    }
                }

                if (material.isPremium) {
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFF8E1)).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🔒", fontSize = 22.sp)
                        Column(Modifier.weight(1f)) {
                            Text(str.materialsPremiumContent, style = MaterialTheme.typography.titleMedium,
                                color = BpscColors.CoinGold, fontWeight = FontWeight.Bold)
                            Text(str.materialsUnlockPro2, style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant)
                        }
                    }
                }
            }

            HorizontalDivider(color = cs.outline)
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Save button removed — download option already serves this purpose
                Button(onClick = if (!material.resolvedUrl.isNullOrBlank()) { { onOpenPdf(material.resolvedUrl ?: "", material.title, material.freePages,
                    isPurchased || material.isFree) } } else onDownload,
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
                    Text(when {
                        isDownloaded -> str.materialsDownloadedDone
                        material.isPremium -> str.materialsUnlockPro
                        !material.resolvedUrl.isNullOrBlank() -> "Open PDF"
                        else -> str.materialsDownloadFree
                    }, style = MaterialTheme.typography.titleMedium)
                }
            }

            // Phase 5: Chat with uploader — only after purchase, and not for own uploads
            // FEATURE FLAG: hidden for now, planned for a later phase (Phase 2 round 2)
            val CHAT_FEATURE_ENABLED = false
            val canChatWithUploader = CHAT_FEATURE_ENABLED && isPurchased &&
                    !material.uploaderId.isNullOrBlank() &&
                    material.uploaderId != currentUserId
            if (canChatWithUploader) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 0.dp)
                    .padding(bottom = 16.dp)) {
                    OutlinedButton(
                        onClick = onChatWithUploader,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BpscColors.Primary)
                    ) {
                        Icon(Icons.Rounded.ChatBubbleOutline, null, modifier = Modifier.size(16.dp), tint = BpscColors.Primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Chat with uploader", style = MaterialTheme.typography.titleMedium,
                            color = BpscColors.Primary, fontWeight = FontWeight.SemiBold)
                    }
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
    isUploading: Boolean,
    uploadProgress: Float,
    uploadError: String?,
    onSubmit: (Uri, String, String, String, MaterialType, String, List<String>, Int, Boolean, Int, Int) -> Unit,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onFormChange: (title: String?, description: String?, subject: String?, author: String?,
                   tags: String?, type: MaterialType?, isPremium: Boolean?, freePages: String?, price: String?) -> Unit,
    state: StudyMaterialsUiState
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val context = LocalContext.current

    // Form state lives in ViewModel — survives sheet close/reopen
    val title       = state.uploadTitle
    val subject     = state.uploadSubject
    val description = state.uploadDescription
    val author      = state.uploadAuthor
    val tagsInput   = state.uploadTags
    val selType     = state.uploadType
    val isPremium   = state.uploadIsPremium
    val freePages   = state.uploadFreePages
    val price       = state.uploadPrice

    // File URI is still local — can't serialize a URI into ViewModel state safely
    var fileUri     by remember { mutableStateOf<Uri?>(null) }
    var fileName    by remember { mutableStateOf("") }
    var fileSizeWarning by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Check file size — warn if > 20MB
            val sizeBytes = try {
                context.contentResolver.query(it, null, null, null, null)?.use { c ->
                    val sizeIdx = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (c.moveToFirst() && sizeIdx >= 0) c.getLong(sizeIdx) else 0L
                } ?: 0L
            } catch (_: Exception) { 0L }

            if (sizeBytes > 20 * 1024 * 1024) {
                // Show warning but still allow — ViewModel will compress video
                fileSizeWarning = "⚠️ File is ${sizeBytes / (1024 * 1024)}MB — large files may take longer to upload on slow connections."
            } else {
                fileSizeWarning = null
            }

            fileUri = it
            var name = "file"
            try {
                context.contentResolver.query(it, null, null, null, null)
                    ?.use { cursor ->
                        val nameIndex =
                            cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex >= 0) {
                            name = cursor.getString(nameIndex)
                        }
                    }
            } catch (_: Exception) {
            }

            fileName = name
        }
    }

    // Request READ_MEDIA_VIDEO + READ_MEDIA_IMAGES on Android 13+
    // or READ_EXTERNAL_STORAGE on older — without this, openInputStream() returns null
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Launch picker regardless — partial grant still allows document picker to work
        filePicker.launch("*/*")
    }

    fun launchFilePicker() {
        val mime = "application/pdf" // VIDEO disabled — Phase 2
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val perms = arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
            val allGranted = perms.all {
                androidx.core.content.ContextCompat.checkSelfPermission(context, it) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            if (allGranted) filePicker.launch(mime) else mediaPermissionLauncher.launch(perms)
        } else if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
            val perm = android.Manifest.permission.READ_EXTERNAL_STORAGE
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, perm) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) filePicker.launch(mime)
            else mediaPermissionLauncher.launch(arrayOf(perm))
        } else {
            filePicker.launch(mime)
        }
    }

    // Block sheet from physically closing while upload is in progress
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            // When user tries to hide/swipe-close while uploading, block it and show dialog
            if (isUploading && newValue == androidx.compose.material3.SheetValue.Hidden) {
                onDismiss() // this calls hideUpload() → shows the cancel dialog
                false       // return false = block the physical close
            } else {
                true
            }
        }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = cs.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)
            .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            Text(str.materialsUploadTitle, style = MaterialTheme.typography.headlineSmall,
                color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
            Text(str.materialsShareHint,
                style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
            HorizontalDivider(color = cs.outline)

            // Error banner
            uploadError?.let { err ->
                Card(shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE8E8))) {
                    Text(err, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE74C3C),
                        modifier = Modifier.padding(12.dp))
                }
            }

            OutlinedTextField(value = title, onValueChange = { onFormChange(it, null, null, null, null, null, null, null, null) },
                label = { Text(str.materialsNotesTitle) }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true)

            // FIX: Subject dropdown populated from backend /subjects API
            var subjectExpanded by remember { mutableStateOf(false) }
            val backendSubjects = state.subjects.filter { it != str.filterAll }
                .ifEmpty { listOf("Polity","History","Geography","Economy","Bihar GK","Science","Environment","Current Affairs","BPSC Specific") }

            ExposedDropdownMenuBox(
                expanded = subjectExpanded, onExpandedChange = { subjectExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = subject, onValueChange = {},
                    readOnly = true,
                    label = { Text("${str.materialsFilterSubject} *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = subjectExpanded, onDismissRequest = { subjectExpanded = false }) {
                    backendSubjects.forEach { s ->
                        DropdownMenuItem(text = { Text(s) }, onClick = { onFormChange(null, null, s, null, null, null, null, null, null); subjectExpanded = false })
                    }
                }
            }

            OutlinedTextField(value = author, onValueChange = { onFormChange(null, null, null, it, null, null, null, null, null) },
                label = { Text(str.materialsAuthorName) }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true)

            // Type selector
            Text(str.materialsContentType, style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MaterialType.values()) { type ->
                    val sel = selType == type
                    val color = typeColor(type)
                    Row(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (sel) color else typeBg(type))
                        .clickable {
                            fileUri = null; fileName = ""  // reset local file state
                            onFormChange(null, null, null, null, null, type, null, "3", null)
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(type.emoji, fontSize = 12.sp)
                        Text(type.label, style = MaterialTheme.typography.bodyMedium,
                            color = if (sel) Color.White else color,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            // Type description
            Text(
                when (selType) {
                    MaterialType.PDF   -> "📄 Study notes, summaries, handwritten scans, any PDF material"
                    MaterialType.PYQ   -> "📝 Previous Year Question papers from past BPSC / UPSC exams"
                    MaterialType.BOOK  -> "📚 Reference books, standard textbooks, study guides (PDF)"
                },
                style = MaterialTheme.typography.bodySmall,
                color = BpscColors.TextSecondary,
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BpscColors.Surface)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            )

            OutlinedTextField(value = description, onValueChange = { onFormChange(null, it, null, null, null, null, null, null, null) },
                label = { Text("Message to Admin (optional)") },
                placeholder = { Text("Tell the reviewer what this material is about, source, year, etc.") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), minLines = 3, maxLines = 4)

            OutlinedTextField(value = tagsInput, onValueChange = { onFormChange(null, null, null, null, it, null, null, null, null) },
                label = { Text("Tags (optional)") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                placeholder = { Text("e.g. BPSC, Polity, Indian Constitution") },
                supportingText = { Text("Comma separated — helps others find your material") })

            // ── Marketplace / Premium Settings ─────────────────
            // Free pages concept only applies to PDF-type content (VIDEO disabled — Phase 2)
            val isPdfType = true
            Card(shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPremium) Color(0xFFFFF8E1) else BpscColors.Surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column {
                            Text(str.materialsPremiumContent, style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = cs.onSurface)
                            Text(
                                if (isPdfType) str.materialsChargeCoins
                                else str.materialsChargeCoins,
                                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                        Switch(checked = isPremium, onCheckedChange = { onFormChange(null, null, null, null, null, null, it, null, null) },
                            colors = SwitchDefaults.colors(checkedThumbColor = BpscColors.CoinGold,
                                checkedTrackColor = BpscColors.CoinGold.copy(0.3f)))
                    }
                    AnimatedVisibility(visible = isPremium) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = price,
                                    onValueChange = { onFormChange(null, null, null, null, null, null, null, null, it.filter { c -> c.isDigit() }) },
                                    label = { Text(str.materialPriceCoins) },
                                    modifier = if (isPdfType) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp), singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                                // Free pages — only relevant for PDF/PYQ/BOOK
                                if (isPdfType) {
                                    OutlinedTextField(
                                        value = freePages,
                                        onValueChange = { onFormChange(null, null, null, null, null, null, null, it.filter { c -> c.isDigit() }, null) },
                                        label = { Text("${str.coursesFree} pages") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp), singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                    )
                                }
                            }
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFF3CD)).padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("💡", fontSize = 12.sp)
                                Text(
                                    if (isPdfType)
                                        "Users see ${freePages.ifEmpty { "3" }} free pages. Full PDF unlocks after purchase."
                                    else
                                        "Users see ${freePages.ifEmpty { "3" }} free pages. Full PDF unlocks after purchase.",
                                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF856404))
                            }
                        }
                    }
                }
            }

            // File picker
            Box(modifier = Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(14.dp))
                .background(if (fileUri != null) BpscColors.Success.copy(0.08f) else BpscColors.Surface)
                .border(1.5.dp, if (fileUri != null) BpscColors.Success else cs.outline, RoundedCornerShape(14.dp))
                .clickable { launchFilePicker() }, contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.AttachFile, null,
                        tint = if (fileUri != null) BpscColors.Success else BpscColors.Primary,
                        modifier = Modifier.size(22.dp))
                    Text(
                        if (fileUri != null) "✅ $fileName"
                        else when (selType) {
                            else               -> "Tap to attach PDF file"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (fileUri != null) BpscColors.Success else BpscColors.TextSecondary
                    )
                }
            }

            // File size warning
            fileSizeWarning?.let { warning ->
                Text(warning,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF856404),
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF3CD))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }

            // Upload progress
            if (isUploading) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(progress = uploadProgress, modifier = Modifier.fillMaxWidth(),
                        color = BpscColors.Primary, trackColor = cs.outline)
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("${(uploadProgress * 100).toInt()}% uploaded…",
                            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        TextButton(onClick = onCancel) {
                            Text("Cancel", color = Color(0xFFE74C3C),
                                style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val uri = fileUri ?: return@Button
                    val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onSubmit(uri, title, description, subject, selType, author, tags, 0,
                        isPremium, freePages.toIntOrNull() ?: 3, price.toIntOrNull() ?: 0)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                enabled = title.isNotBlank() && subject.isNotBlank() && fileUri != null && !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(str.materialsUploading, style = MaterialTheme.typography.titleMedium)
                } else {
                    Icon(Icons.Rounded.Upload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(str.materialsSubmitReview, style = MaterialTheme.typography.titleMedium)
                }
            }

            Text(str.materialsReviewNote,
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
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (showBookmarksOnly) "🔖" else "🔍", fontSize = 48.sp)
            Text(if (showBookmarksOnly) str.materialsNoSaved else str.materialsNoResources,
                style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.Bold)
            Text(if (showBookmarksOnly) str.materialsBookmarkHint else str.caTryFilter,
                style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("⚠️", fontSize = 40.sp)
            Text(message, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                Text(str.retry)
            }
        }
    }
}

@Composable
private fun LoadingGrid() {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(6) {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(16.dp))
                .background(cs.outline))
        }
    }
}

// ════════════════════════════════════════════════════════════
// SMALL BADGE COMPOSABLES
// ════════════════════════════════════════════════════════════
@Composable
private fun TypeBadge(type: MaterialType) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val color = typeColor(type); val bg = typeBg(type)
    Text(type.label, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 9.sp,
        fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(5.dp))
            .background(bg).padding(horizontal = 6.dp, vertical = 2.dp))
}
@Composable private fun TypeBadgeWhite(label: String) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.85f),
        fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(0.2f)).padding(horizontal = 8.dp, vertical = 3.dp))
}
@Composable private fun FreeBadge() {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Text("FREE", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontSize = 9.sp,
        fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(5.dp))
            .background(Color(0xFFE8FDF4)).padding(horizontal = 6.dp, vertical = 2.dp))
}
@Composable private fun FreeBadgeWhite() {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Text("FREE", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success,
        fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE8FDF4)).padding(horizontal = 8.dp, vertical = 3.dp))
}
@Composable private fun ProBadge() {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Text("PRO", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold,
        fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFFFF8E1)).padding(horizontal = 8.dp, vertical = 3.dp))
}
@Composable private fun NewBadge() {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Text(str.jobsNew, style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontSize = 9.sp,
        fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(5.dp))
            .background(Color(0xFFE8FDF4)).padding(horizontal = 6.dp, vertical = 2.dp))
}
@Composable private fun SheetStatWhite(icon: String, value: String) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(icon, fontSize = 12.sp)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.85f), fontWeight = FontWeight.SemiBold)
    }
}
@Composable private fun LibInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = BpscColors.TextHint, modifier = Modifier.size(11.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, fontSize = 10.sp)
    }
}

// ── Helpers ───────────────────────────────────────────────────
private fun typeColor(type: MaterialType) = when (type) {
    MaterialType.PDF   -> Color(0xFFE74C3C)
    MaterialType.PYQ   -> Color(0xFF9B59B6)
    MaterialType.BOOK  -> Color(0xFF1565C0)
}
private fun typeBg(type: MaterialType) = when (type) {
    MaterialType.PDF   -> Color(0xFFFEE8E8)
    MaterialType.PYQ   -> Color(0xFFF3E8FD)
    MaterialType.BOOK  -> Color(0xFFE8F0FD)
}
private fun formatCount(count: Int): String {
    return if (count >= 1000) "${"%.1f".format(count / 1000f)}k" else "$count"
}

// needed for text field in search

// ════════════════════════════════════════════════════════════
// MY UPLOADS TAB — user's own uploaded materials (no lock)
// ════════════════════════════════════════════════════════════
@Composable
fun MyUploadsTab(
    uploads:   List<StudyMaterialDto>,
    isLoading: Boolean,
    onOpenPdf: (url: String, title: String, freePages: Int, isPurchased: Boolean) -> Unit,
    onRefresh: () -> Unit,
    onRespondNegotiation: (StudyMaterialDto) -> Unit = {},
    onOpenWallet: () -> Unit = {},
    onOpenChats: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    when {
        isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = BpscColors.Primary)
        }
        uploads.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("📤", fontSize = 56.sp)
                Text(str.materialsNoUploads, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(str.materialsUploadHint,
                    style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(12.dp)) { Text(str.retry) }
            }
        }
        else -> LazyColumn(
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${uploads.size} uploaded material${if (uploads.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Phase 5 "Chats" inbox — hidden for now, planned for a later phase
                        if (false) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                    .background(BpscColors.PrimaryLight)
                                    .clickable(onClick = onOpenChats)
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text("💬", fontSize = 13.sp)
                                Text("Chats", style = MaterialTheme.typography.labelMedium, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))
                                .clickable(onClick = onOpenWallet)
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text("💰", fontSize = 13.sp)
                            Text("Wallet", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            items(uploads, key = { it.id }) { item ->
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(14.dp),
                    colors    = CardDefaults.cardColors(containerColor = cs.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(BpscColors.PrimaryLight), Alignment.Center) {
                            Text(when (item.materialType) { "pdf" -> "📄"; "video" -> "🎬"; else -> "📋" }, fontSize = 22.sp)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(item.subject, style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Status badge
                                val statusColor = when (item.status?.lowercase()) { "approved" -> BpscColors.Success; "rejected" -> Color(0xFFE74C3C); "negotiating" -> Color(0xFF6366F1); else -> BpscColors.TextHint }
                                val statusLabel = when (item.status?.lowercase()) { "approved" -> str.materialsPublished; "rejected" -> str.materialsRejected; "negotiating" -> "💬 Negotiating"; else -> "⏳ Under Review" }
                                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(statusColor.copy(0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                                if (item.price > 0) {
                                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(BpscColors.CoinGold.copy(0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text("🪙 ${item.price}", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                            }
                            // Show rejection reason if rejected
                            if (item.status?.lowercase() == "rejected" && !item.rejectionReason.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFEE8E8))
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text("❌", fontSize = 10.sp)
                                    Text(
                                        "Reason: ${item.rejectionReason}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFB71C1C),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Negotiation banner — admin sent a counter-offer, tap to respond
                            if (item.hasNegotiationOffer) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEEF2FF))
                                        .clickable { onRespondNegotiation(item) }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Text("💬", fontSize = 12.sp)
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Offer: ₹${item.currentOfferPrice} (you asked ₹${item.price})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF3730A3),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            "Round ${item.negotiationRound}/3 · Tap to respond",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF6366F1),
                                            fontSize = 10.sp
                                        )
                                    }
                                    Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                                }
                            } else if (item.status == "negotiating" && item.negotiationStatus == "awaiting_admin") {
                                // User already responded (counter sent) — waiting on admin
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Text("⏳", fontSize = 12.sp)
                                    Text(
                                        "Your offer of ₹${item.currentOfferPrice} sent · awaiting review (round ${item.negotiationRound}/3)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        // Open button — owner always gets full access (no lock)
                        if (!item.resolvedUrl.isNullOrBlank()) {
                            IconButton(onClick = { onOpenPdf(item.resolvedUrl ?: "", item.title, item.freePages, true) }) {
                                Icon(Icons.Rounded.OpenInNew, null, tint = BpscColors.Primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// DOWNLOADS TAB — shows user's download history with PDF access
// ════════════════════════════════════════════════════════════
@Composable
fun DownloadsTab(
    downloads:    List<com.example.bpscnotes.data.remote.api.DownloadHistoryItem>,
    isLoading:    Boolean,
    purchasedIds: Set<String>,
    onOpenPdf:    (url: String, title: String, freePages: Int, isPurchased: Boolean) -> Unit,
    onRefresh:    () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    when {
        isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = BpscColors.Primary)
        }
        downloads.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("📂", fontSize = 56.sp)
                Text(str.materialsNoDownloads, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = cs.onSurface)
                Text(str.materialsDownloadHint,
                    style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(12.dp)) {
                    Text(str.retry)
                }
            }
        }
        else -> LazyColumn(
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("${downloads.size} downloaded file${if (downloads.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp))
            }
            items(downloads, key = { it.id }) { item ->
                val isPurchased = purchasedIds.contains(item.id) || item.isPurchased
                val hasFullAccess = !item.isPremium || isPurchased || item.price == 0

                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = cs.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
                                .background(BpscColors.PrimaryLight),
                            Alignment.Center
                        ) {
                            Text(
                                when (item.materialType.lowercase()) {
                                    "pdf"   -> "📄"; "pyq" -> "📝"; "book" -> "📚"
                                    "video" -> "🎬"; else  -> "📋"
                                }, fontSize = 24.sp
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = cs.onSurface,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(item.subject, style = MaterialTheme.typography.labelSmall,
                                    color = BpscColors.Primary)
                                if (item.pageCount > 0) {
                                    Text("• ${item.pageCount} pages", style = MaterialTheme.typography.labelSmall,
                                        color = BpscColors.TextHint)
                                }
                            }
                            // Lock indicator
                            if (item.isPremium && !hasFullAccess) {
                                Row(
                                    Modifier.clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFF8E1))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text("🔒", fontSize = 10.sp)
                                    Text("${item.freePages} of ${item.pageCount} pages free",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF856404))
                                }
                            }
                        }
                        // Action button
                        if (!item.fileUrl.isNullOrBlank()) {
                            Button(
                                onClick  = { onOpenPdf(item.fileUrl ?: "", item.title, item.freePages, item.isPurchased) },
                                shape    = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(36.dp),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = if (hasFullAccess) BpscColors.Primary else Color(0xFFF59E0B))
                            ) {
                                Text(if (hasFullAccess) str.materialOpen else str.materialPreview,
                                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// PURCHASE CONFIRM DIALOG — shown when user taps Buy
// Shows price, preview page count, and commission notice
// ════════════════════════════════════════════════════════════
@Composable
fun PurchaseConfirmDialog(
    item:         StudyMaterialDto,
    isPurchasing: Boolean,
    coinsToApply: Int = 0,
    onCoinsToApplyChange: (Int) -> Unit = {},
    userCoins:    Int = 0,
    maxCoinsPerPurchase: Int = 50,
    coinToInrRate: Double = 1.0,
    onConfirm:    () -> Unit,
    onDismiss:    () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current

    val price = item.price ?: 0
    // Mirrors the backend's Math.min(price, floor(coins * coin_to_inr_rate))
    // exactly (studyMaterialsService.initPurchase) — coinDiscount is in
    // rupees, coinsToApply is in coins.
    val coinDiscount = minOf(price, (coinsToApply * coinToInrRate).toInt())
    // Slider cap is in coins, so convert the price ceiling from rupees to
    // coins via the live rate (admin: Coins page -> Economy Settings).
    val priceInCoins = if (coinToInrRate > 0) (price / coinToInrRate).toInt() else 0
    val maxApplicable = remember(price, userCoins, maxCoinsPerPurchase, coinToInrRate) { minOf(maxCoinsPerPurchase, userCoins, priceInCoins) }
    val amountDue = (price - coinDiscount).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(20.dp),
        containerColor   = Color.White,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(str.materialUnlockAccess, fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge)
                Text(item.title, style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant, maxLines = 2)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // What they get
                Card(shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FBF5))) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("✅"); Text("Full PDF — all ${item.pageCount ?: 0} pages",
                            style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("✅"); Text(str.materialsDownloadDevice,
                            style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("✅"); Text(str.materialsLifetimeAccess,
                            style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
                        }
                    }
                }
                // Price + coin discount slider
                Row(
                    Modifier.fillMaxWidth(),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    Text(str.materialPrice, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text("₹$price",
                        style = MaterialTheme.typography.titleLarge,
                        color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                }

                if (maxApplicable > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Use coins for discount", style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🪙", fontSize = 14.sp)
                                Text("$coinsToApply / $maxApplicable", style = MaterialTheme.typography.labelLarge,
                                    color = BpscColors.CoinGold, fontWeight = FontWeight.Bold)
                            }
                        }
                        Slider(
                            value = coinsToApply.toFloat(),
                            onValueChange = { onCoinsToApplyChange(it.toInt()) },
                            valueRange = 0f..maxApplicable.toFloat(),
                            steps = if (maxApplicable > 1) maxApplicable - 1 else 0,
                            colors = SliderDefaults.colors(thumbColor = BpscColors.CoinGold, activeTrackColor = BpscColors.CoinGold)
                        )
                    }
                }

                HorizontalDivider(color = cs.outline)

                Row(
                    Modifier.fillMaxWidth(),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    Text("You pay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("₹$amountDue", style = MaterialTheme.typography.titleLarge,
                            color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                        if (coinsToApply > 0) {
                            Text("🪙 $coinsToApply coins applied (−₹$coinDiscount)",
                                style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = onConfirm,
                enabled  = !isPurchasing,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                if (isPurchasing) {
                    CircularProgressIndicator(color = Color.White,
                        modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(str.courseProcessing)
                } else if (amountDue == 0) {
                    Text(if (coinsToApply > 0) "🪙 Pay with $coinsToApply coins" else "Unlock for free",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                } else {
                    Text("Pay ₹$amountDue",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(str.cancel, color = cs.onSurfaceVariant)
            }
        }
    )
}



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