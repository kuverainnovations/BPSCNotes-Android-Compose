package com.example.bpscnotes.presentation.course

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.browser.customtabs.CustomTabsIntent
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.popBackStackSafe

// ─────────────────────────────────────────────────────────────
// LESSON VIEWER SCREEN
//
// Handles all lesson types:
//   pdf   → WebView rendering PDF via Google Docs viewer
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

    Scaffold(
        topBar       = {
            LessonTopBar(
                title     = state.lesson?.title ?: "Lesson",
                isLoading = state.isLoading,
                onBack    = { nav.popBackStackSafe() }
            )
        },
        bottomBar    = {
            if (state.lesson != null) {
                LessonBottomBar(
                    isCompleted   = state.lesson?.is_completed == true,
                    isMarking     = state.isMarking,
                    onMarkComplete = { viewModel.markComplete() }
                )
            }
        },
        containerColor = Color(0xFF0F1117)
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> LoadingState()
                state.error != null -> ErrorState(state.error!!) { viewModel.load(courseId, lessonId) }
                state.lesson != null -> {
                    val lesson = state.lesson!!
                    when (lesson.type) {
                        "video"       -> VideoPlayer(lesson.video_url ?: lesson.notes_url)
                        "pdf", "notes" -> PdfViewer(lesson.notes_url)
                        "live"        -> LiveClassView(lesson)
                        "quiz"        -> QuizRedirectView(lesson, onQuizTap = { nav.popBackStackSafe() })
                        else          -> PdfViewer(lesson.notes_url ?: lesson.video_url)
                    }

                    // Completed banner overlay
                    if (state.lesson?.is_completed == true) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF2E7D32).copy(0.9f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment =  Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(str.coursesCompleted, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
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
private fun LessonTopBar(title: String, isLoading: Boolean, onBack: () -> Unit) {
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF1A1D27)
        )
    )
}

// ─────────────────────────────────────────────────────────────
// BOTTOM BAR — Mark Complete
// ─────────────────────────────────────────────────────────────

@Composable
private fun LessonBottomBar(
    isCompleted: Boolean,
    isMarking: Boolean,
    onMarkComplete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1D27))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (isCompleted) {
            Row(
                modifier             = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment    = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(str.lessonCompleted, style = MaterialTheme.typography.titleMedium, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick  = onMarkComplete,
                enabled  = !isMarking,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                if (isMarking) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(str.lessonSaving, style = MaterialTheme.typography.titleMedium)
                } else {
                    Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(str.lessonMarkComplete, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PDF VIEWER — WebView with Google Docs embed
// ─────────────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PdfViewer(notesUrl: String?) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    if (notesUrl.isNullOrBlank()) {
        NoContentState(str.lessonNoPdf)
        return
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    var loading  by remember { mutableStateOf(true) }
    var failed   by remember { mutableStateOf(false) }
    var retryKey by remember { mutableIntStateOf(0) }

    // FIX: Black screen fix — use a robust URL strategy:
    // 1. If it's a direct PDF URL → use Google Docs embedded viewer
    // 2. If it's already an HTML/web URL → load directly
    // 3. On failure → show retry + open-in-browser option
    val viewUrl = remember(notesUrl) {
        when {
            notesUrl.endsWith(".pdf", ignoreCase = true) ->
                "https://docs.google.com/viewer?embedded=true&url=" +
                        java.net.URLEncoder.encode(notesUrl, "UTF-8")
            notesUrl.contains("/pdf", ignoreCase = true) ->
                "https://docs.google.com/viewer?embedded=true&url=" +
                        java.net.URLEncoder.encode(notesUrl, "UTF-8")
            else -> notesUrl  // HTML notes or direct link
        }
    }

    if (failed) {
        // FIX: Show proper error with retry + open-in-browser
        Column(
            Modifier.fillMaxSize().background(cs.background).padding(32.dp),
            Arrangement.Center, Alignment.CenterHorizontally
        ) {
            Text("📄", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(str.lessonCantLoadPdf, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = cs.onSurface)
            Text(str.lessonViewerTimeout,
                style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Button(onClick = { failed = false; loading = true; retryKey++ },
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                Text(str.retry)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(notesUrl))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)
            ) { Text(str.lessonOpenBrowser) }
        }
        return
    }

    Box(Modifier.fillMaxSize().background(cs.surface)) {
        key(retryKey) {
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
                            builtInZoomControls  = true
                            displayZoomControls  = false
                            useWideViewPort      = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            cacheMode = WebSettings.LOAD_NO_CACHE  // FIX: no cache prevents stale blank pages
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                // FIX: check if WebView actually has content (not just blank loaded)
                                view?.evaluateJavascript("document.body.innerHTML.length") { result ->
                                    val bodyLen = result?.trim()?.toIntOrNull() ?: 0
                                    loading = false
                                    if (bodyLen < 10) failed = true  // empty page = failed load
                                }
                            }
                            override fun onReceivedError(view: WebView?, errorCode: Int, desc: String?, url: String?) {
                                loading = false; failed = true
                            }
                        }
                        webChromeClient = WebChromeClient()
                        loadUrl(viewUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        if (loading) {
            Box(Modifier.fillMaxSize().background(cs.surface), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = BpscColors.Primary)
                    Text(str.lessonLoadingPdf, style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant)
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
    val str = LocalStrings.current
    Box(Modifier.fillMaxSize().background(Color(0xFF0F1117)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = BpscColors.Primary)
            Text(str.lessonLoading, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.6f))
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    val str = LocalStrings.current
    Box(Modifier.fillMaxSize().background(Color(0xFF0F1117)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("⚠️", fontSize = 40.sp)
            Text(message, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f), textAlign = TextAlign.Center)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                Text(str.retry)
            }
        }
    }
}

@Composable
private fun NoContentState(message: String) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0F1117)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("📄", fontSize = 48.sp)
            Text(message, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f), textAlign = TextAlign.Center)
        }
    }
}