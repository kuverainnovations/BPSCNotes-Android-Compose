package com.example.bpscnotes.presentation.course
import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Bitmap
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import com.example.bpscnotes.core.pdf.downloadPdf
import com.example.bpscnotes.core.pdf.renderPdfPages
import com.example.bpscnotes.core.network.toUserMessage
import kotlinx.coroutines.withContext

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.kuvera.bpscnotes.R
import androidx.browser.customtabs.CustomTabsIntent
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

// ─────────────────────────────────────────────────────────────
// LESSON VIEWER SCREEN
//
// Handles all lesson types:
//   pdf   → PdfRenderer (same as PdfViewerScreen — per-page bitmaps with centered watermark)
//   video → WebView (YouTube embed / direct mp4)
//   quiz  → navigates to quiz
//   live  → shows live class join button
//
// Completion flow:
//   1. Screen opens → GET /courses/:courseId/lessons/:lessonId
//      (loads notesUrl / videoUrl + current is_completed)
//   2. User reads PDF / watches video
//   3. str.lessonMarkComplete button → POST .../complete
//      Backend updates lesson_progress + user_enrollments
//   4. CourseDetailScreen auto-refreshes via viewModel.load()
// ─────────────────────────────────────────────────────────────

@Composable
fun LessonViewerScreen(
    nav: NavHostController,
    courseId: String,
    lessonId: String,
    viewModel: LessonViewerViewModel = hiltViewModel()
) {
    LaunchedEffect(lessonId) { viewModel.load(courseId, lessonId) }

    val state by viewModel.uiState.collectAsState()
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    LaunchedEffect(Unit) { com.example.bpscnotes.core.analytics.Event.screenView("lesson_viewer") }

    // Track whether user has scrolled to the last page (for PDF lessons).
    // For non-PDF lessons we show the button immediately.
    var reachedEnd by remember { mutableStateOf(false) }

    Scaffold(
        topBar       = {
            LessonTopBar(
                title         = state.lesson?.title ?: "Lesson",
                isLoading     = state.isLoading,
                isCompleted   = state.lesson?.is_completed == true,
                isMarking     = state.isMarking,
                showMarkBtn   = state.lesson != null && state.isEnrolled && reachedEnd,
                onBack        = { nav.popBackStackSafe() },
                onMarkComplete = { viewModel.markComplete() }
            )
        },
        containerColor = cs.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(state.error!!) { viewModel.load(courseId, lessonId) }
                state.lesson != null -> {
                    val lesson = state.lesson!!
                    when (lesson.type) {
                        "video"        -> {
                            reachedEnd = true  // video: show button once loaded
                            VideoPlayer(lesson.video_url ?: lesson.notes_url)
                        }
                        "pdf", "notes" -> PdfViewer(
                            notesUrl    = lesson.notes_url,
                            onReachEnd  = { reachedEnd = true }
                        )
                        "live"         -> {
                            reachedEnd = true
                            LiveClassView(lesson)
                        }
                        "quiz"         -> {
                            reachedEnd = true
                            QuizRedirectView(lesson, onQuizTap = { nav.popBackStackSafe() })
                        }
                        else           -> PdfViewer(
                            notesUrl   = lesson.notes_url ?: lesson.video_url,
                            onReachEnd = { reachedEnd = true }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonTopBar(
    title:          String,
    isLoading:      Boolean,
    isCompleted:    Boolean,
    isMarking:      Boolean,
    showMarkBtn:    Boolean,
    onBack:         () -> Unit,
    onMarkComplete: () -> Unit
) {
    val str = LocalStrings.current
    TopAppBar(
        title = {
            Text(
                title,
                style    = MaterialTheme.typography.titleMedium,
                color    = Color.White,
                maxLines = 1
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
            }
        },
        actions = {
            AnimatedVisibility(
                visible = showMarkBtn,
                enter   = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
                exit    = fadeOut()
            ) {
                if (isCompleted) {
                    // Completed badge
                    Row(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF2E7D32).copy(0.9f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text(str.coursesCompleted, style = MaterialTheme.typography.labelMedium,
                            color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Mark complete pill button
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(if (isMarking) 0.15f else 0.2f))
                            .clickable(enabled = !isMarking, onClick = onMarkComplete)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            if (isMarking) {
                                CircularProgressIndicator(color = Color.White,
                                    modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                            } else {
                                Icon(Icons.Rounded.CheckCircle, null, tint = Color.White,
                                    modifier = Modifier.size(14.dp))
                            }
                            Text(str.lessonMarkComplete,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BpscColors.Primary
        )
    )
}

// ─────────────────────────────────────────────────────────────
// PDF VIEWER — PdfRenderer (bitmap per page, same approach as PdfViewerScreen)
// ─────────────────────────────────────────────────────────────

@Composable
private fun PdfViewer(notesUrl: String?, onReachEnd: () -> Unit = {}) {
    val cs      = MaterialTheme.colorScheme
    val str     = LocalStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current

    if (notesUrl.isNullOrBlank()) { NoContentState(str.lessonNoPdf); return }

    var pdfPages   by remember { mutableStateOf<List<android.graphics.Bitmap>>(emptyList()) }
    var isLoading  by remember { mutableStateOf(true) }
    var error      by remember { mutableStateOf<String?>(null) }
    val listState  = rememberLazyListState()

    // Watermark logo decoded once
    val watermarkLogo = remember {
        android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.ic_bpsc_logo)
    }

    // Download + render in background (same as PdfViewerScreen)
    LaunchedEffect(notesUrl) {
        isLoading = true; error = null
        try {
            val file    = withContext(kotlinx.coroutines.Dispatchers.IO) { downloadPdf(notesUrl, context.cacheDir) }
            val bitmaps = withContext(kotlinx.coroutines.Dispatchers.Default) { renderPdfPages(file) }
            pdfPages = bitmaps
        } catch (e: Exception) {
            error = e.toUserMessage("Could not open PDF")
        } finally {
            isLoading = false
        }
    }

    // Fire onReachEnd when user scrolls to last page
    val lastIndex = pdfPages.lastIndex
    LaunchedEffect(listState.firstVisibleItemIndex, listState.layoutInfo.visibleItemsInfo) {
        if (pdfPages.isNotEmpty()) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (lastVisible >= lastIndex) onReachEnd()
        }
    }

    Box(Modifier.fillMaxSize().background(cs.background)) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = BpscColors.Primary)
                    Text(str.lessonLoadingPdf, style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant)
                }
            }
            error != null -> Column(
                Modifier.fillMaxSize().padding(32.dp),
                Arrangement.Center, Alignment.CenterHorizontally
            ) {
                Text("\uD83D\uDCC4", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text(str.lessonCantLoadPdf, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = cs.onSurface)
                Spacer(Modifier.height(8.dp))
                Text(error!!, style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Button(onClick = {
                    error = null; isLoading = true
                    // re-trigger by toggling — parent recomposes via state
                },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) { Text(str.retry) }
            }
            pdfPages.isEmpty() -> NoContentState(str.lessonNoPdf)
            else -> LazyColumn(
                state               = listState,
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(pdfPages.size, key = { it }) { pageIndex ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        // Page bitmap
                        Image(
                            bitmap             = pdfPages[pageIndex].asImageBitmap(),
                            contentDescription = null,
                            modifier           = Modifier.fillMaxWidth(),
                            contentScale       = ContentScale.FillWidth
                        )
                        // Watermark — centered on every page, moves with scroll
                        Image(
                            bitmap             = watermarkLogo.asImageBitmap(),
                            contentDescription = null,
                            modifier           = Modifier
                                .fillMaxWidth(0.35f)
                                .aspectRatio(1f)
                                .align(Alignment.Center)
                                .alpha(0.12f),
                            contentScale = ContentScale.Fit
                        )
                        // Page number badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(0.45f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "${pageIndex + 1} / ${pdfPages.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
// ─────────────────────────────────────────────────────────────
// VIDEO PLAYER — WebView (YouTube iframe / direct video)
// ─────────────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VideoPlayer(videoUrl: String?) {
    val str = LocalStrings.current
    if (videoUrl.isNullOrBlank()) {
        NoContentState(str.lessonNoVideo)
        return
    }

    var loading by remember { mutableStateOf(true) }

    // Build embeddable URL
    val embedUrl = remember(videoUrl) {
        when {
            videoUrl.contains("youtube.com/watch") -> {
                val id = videoUrl.substringAfter("v=").substringBefore("&")
                "https://www.youtube.com/embed/$id?autoplay=1&rel=0&playsinline=1"
            }
            videoUrl.contains("youtu.be/") -> {
                val id = videoUrl.substringAfter("youtu.be/").substringBefore("?")
                "https://www.youtube.com/embed/$id?autoplay=1&rel=0&playsinline=1"
            }
            else -> videoUrl  // direct mp4 / other
        }
    }

    val isYoutube = remember(embedUrl) { embedUrl.contains("youtube.com/embed") }
    val html = if (isYoutube) """
        <!DOCTYPE html><html>
        <head><meta name='viewport' content='width=device-width, initial-scale=1.0'>
        <style>body{margin:0;padding:0;background:#000}
        iframe{width:100%;height:100%;position:fixed;top:0;left:0;border:0}</style>
        </head>
        <body><iframe src='$embedUrl' allowfullscreen allow='autoplay'></iframe></body>
        </html>
    """.trimIndent() else null

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled    = true
                        domStorageEnabled    = true
                        mediaPlaybackRequiresUserGesture = false
                        useWideViewPort      = true
                        loadWithOverviewMode = true
                        allowContentAccess   = true
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) { loading = false }
                    }
                    webChromeClient = WebChromeClient()
                    if (html != null) loadData(html, "text/html", "UTF-8")
                    else loadUrl(embedUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (loading) {
            Box(Modifier.fillMaxSize().background(Color.Black), Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// LIVE CLASS VIEW
// ─────────────────────────────────────────────────────────────

@Composable
private fun LiveClassView(lesson: com.example.bpscnotes.data.remote.api.Lesson) {
    val str = LocalStrings.current
    Box(Modifier.fillMaxSize().background(Color(0xFF0F1117)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape)
                    .background(Color(0xFFE53935).copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🔴", fontSize = 36.sp)
            }
            Text(str.lessonLiveClass, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text(lesson.title, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f), textAlign = TextAlign.Center)
            val context = androidx.compose.ui.platform.LocalContext.current
            if (!lesson.video_url.isNullOrBlank()) {
                Button(
                    onClick = {
                        // Chrome Custom Tabs — in-app browser, user stays in BPSCNotes
                        try {
                            val uri = android.net.Uri.parse(lesson.video_url)
                            val intent = CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .setUrlBarHidingEnabled(false)
                                .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
                                .build()
                            intent.launchUrl(context, uri)
                        } catch (e: Exception) {
                            // Fallback if Chrome not installed
                            context.startActivity(
                                android.content.Intent(android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(lesson.video_url))
                            )
                        }
                        com.example.bpscnotes.core.analytics.Event.track("live_class_joined",
                            mapOf("lesson_id" to lesson.id, "title" to lesson.title))
                    },
                    shape   = RoundedCornerShape(14.dp),
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text(str.lessonJoinLive, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(str.lessonInAppBrowser,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(0.4f))
            } else {
                Text(str.lessonLiveNotReady, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.5f), textAlign = TextAlign.Center)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// QUIZ REDIRECT VIEW
// ─────────────────────────────────────────────────────────────

@Composable
private fun QuizRedirectView(
    lesson: com.example.bpscnotes.data.remote.api.Lesson,
    onQuizTap: () -> Unit
) {
    val str = LocalStrings.current
    Box(Modifier.fillMaxSize().background(Color(0xFF0F1117)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
            Box(Modifier.size(80.dp).clip(CircleShape).background(BpscColors.Primary.copy(0.15f)), Alignment.Center) {
                Text("❓", fontSize = 36.sp)
            }
            Text(str.lessonChapterQuiz, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text(lesson.title, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f), textAlign = TextAlign.Center)
            Text("${lesson.duration_mins} min · Test your knowledge", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.5f))
            Button(
                onClick = onQuizTap,
                shape   = RoundedCornerShape(14.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                Icon(Icons.Rounded.Quiz, null)
                Spacer(Modifier.width(6.dp))
                Text(str.quizStart, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Box(Modifier.fillMaxSize().background(cs.background), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = BpscColors.Primary)
            Text(str.lessonLoading, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Box(Modifier.fillMaxSize().background(cs.background), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("⚠️", fontSize = 40.sp)
            Text(message, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                Text(str.retry)
            }
        }
    }
}

@Composable
private fun NoContentState(message: String) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(cs.background), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("📄", fontSize = 48.sp)
            Text(message, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}