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
import com.example.bpscnotes.core.ui.t.BpscColors

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
//   3. "Mark as Complete" button → POST .../complete
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

    Scaffold(
        topBar       = {
            LessonTopBar(
                title     = state.lesson?.title ?: "Lesson",
                isLoading = state.isLoading,
                onBack    = { nav.popBackStack() }
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
                        "quiz"        -> QuizRedirectView(lesson, onQuizTap = { nav.popBackStack() })
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
                                Text("Completed", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
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
                Text("Lesson Completed", style = MaterialTheme.typography.titleMedium, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
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
                    Text("Saving…", style = MaterialTheme.typography.titleMedium)
                } else {
                    Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mark as Complete", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
    if (notesUrl.isNullOrBlank()) {
        NoContentState("No PDF attached to this lesson")
        return
    }
    var loading by remember { mutableStateOf(true) }

    // Build viewer URL — Google Docs can render PDFs inline
    val viewUrl = remember(notesUrl) {
        if (notesUrl.endsWith(".pdf", ignoreCase = true) ||
            notesUrl.contains("pdf", ignoreCase = true)) {
            "https://docs.google.com/gview?embedded=true&url=${
                java.net.URLEncoder.encode(notesUrl, "UTF-8")
            }"
        } else {
            notesUrl  // direct URL (HTML notes, etc.)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled       = true
                        domStorageEnabled       = true
                        builtInZoomControls     = true
                        displayZoomControls     = false
                        useWideViewPort         = true
                        loadWithOverviewMode    = true
                        setSupportZoom(true)
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                        }
                    }
                    webChromeClient = WebChromeClient()
                    loadUrl(viewUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (loading) {
            Box(Modifier.fillMaxSize().background(Color.White), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = BpscColors.Primary)
                    Text("Loading PDF…", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
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
    if (videoUrl.isNullOrBlank()) {
        NoContentState("No video attached to this lesson")
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
    Box(Modifier.fillMaxSize().background(Color(0xFF0F1117)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape)
                    .background(Color(0xFFE53935).copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🔴", fontSize = 36.sp)
            }
            Text("Live Class", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text(lesson.title, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f), textAlign = TextAlign.Center)
            if (!lesson.video_url.isNullOrBlank()) {
                Button(
                    onClick = { /* open join URL */ },
                    shape   = RoundedCornerShape(14.dp),
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Join Live Class", fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Live class link will be available when the session starts.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.5f), textAlign = TextAlign.Center)
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
    Box(Modifier.fillMaxSize().background(Color(0xFF0F1117)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
            Box(Modifier.size(80.dp).clip(CircleShape).background(BpscColors.Primary.copy(0.15f)), Alignment.Center) {
                Text("❓", fontSize = 36.sp)
            }
            Text("Chapter Quiz", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text(lesson.title, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f), textAlign = TextAlign.Center)
            Text("${lesson.duration_mins} min · Test your knowledge", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.5f))
            Button(
                onClick = onQuizTap,
                shape   = RoundedCornerShape(14.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                Icon(Icons.Rounded.Quiz, null)
                Spacer(Modifier.width(6.dp))
                Text("Start Quiz", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize().background(Color(0xFF0F1117)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = BpscColors.Primary)
            Text("Loading lesson…", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.6f))
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0F1117)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("⚠️", fontSize = 40.sp)
            Text(message, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.7f), textAlign = TextAlign.Center)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                Text("Retry")
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
