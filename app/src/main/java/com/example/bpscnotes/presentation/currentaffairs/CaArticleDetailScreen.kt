package com.example.bpscnotes.presentation.currentaffairs

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.AppErrorState
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import kotlinx.coroutines.launch

// Inline study time tracker — same pattern as CurrentAffairsScreen.kt /
// CaMcqQuizScreen.kt (each screen keeps its own private copy).
@Composable
private fun TrackStudyTime(
    activityType: String,
    onLog: (activityType: String, durationSecs: Int) -> Unit
) {
    val startTime = remember { System.currentTimeMillis() }
    DisposableEffect(activityType) {
        onDispose {
            val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt().coerceIn(0, 3600)
            if (elapsed >= 10) onLog(activityType, elapsed)
        }
    }
}

private val CA_DETAIL_CATEGORY_COLORS = mapOf(
    "Economy" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)),
    "Polity" to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)),
    "International" to Pair(Color(0xFF3498DB), Color(0xFFE8F4FD)),
    "Science" to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)),
    "Education" to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)),
    "Sports" to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)),
    "Bihar GK" to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)),
)

private fun Color.toCssHex(): String = String.format("#%06X", 0xFFFFFF and this.toArgb())

private fun escapeHtml(text: String): String = text
    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    .replace("\"", "&quot;").replace("'", "&#39;")

/**
 * Wraps the sanitized rich-content HTML (already cleaned server-side via
 * sanitize-html — see backend CurrentAffairsService) in a full document with
 * CSS that mirrors RichContentView in the admin panel, but using the app's
 * live MaterialTheme colors so it matches light/dark mode instead of being
 * hardcoded light-only like the admin's preview.
 */
private fun buildArticleHtml(
    article: CAArticle,
    surface: String, onSurface: String, onSurfaceVariant: String,
    outline: String, primary: String, primaryLight: String,
): String {
    val tagsHtml = if (article.tags.isNotEmpty()) {
        "<div class=\"tags\">" + article.tags.joinToString("") { "<span>#${escapeHtml(it)}</span>" } + "</div>"
    } else ""
    val sourceHtml = if (!article.source.isNullOrBlank()) {
        "<p class=\"source\">Source: ${escapeHtml(article.source)}</p>"
    } else ""

    return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <style>
          * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
          html, body { margin: 0; padding: 0; background: $surface; }
          body { padding: 2px 2px 40px; font-family: -apple-system, Roboto, sans-serif; font-size: 16px; line-height: 1.75; color: $onSurface; }
          p { margin: 0 0 14px; }
          h1 { font-size: 1.5em; font-weight: 800; margin: 22px 0 12px; }
          h2 { font-size: 1.3em; font-weight: 800; margin: 20px 0 10px; }
          h3 { font-size: 1.15em; font-weight: 700; margin: 18px 0 8px; }
          ul, ol { padding-left: 1.4em; margin: 0 0 14px; }
          li { margin-bottom: 6px; }
          blockquote { border-left: 3px solid $primary; padding: 6px 16px; margin: 14px 0; background: $primaryLight; border-radius: 0 10px 10px 0; color: $onSurfaceVariant; font-style: italic; }
          a { color: $primary; text-decoration: underline; }
          table { border-collapse: collapse; width: 100%; margin: 16px 0; display: block; overflow-x: auto; }
          th, td { border: 1px solid $outline; padding: 8px 12px; text-align: left; }
          th { background: $primaryLight; font-weight: 700; }
          img { max-width: 100%; height: auto; border-radius: 10px; }
          mark { border-radius: 3px; padding: 0 2px; }
          .tags { margin-top: 24px; padding-top: 14px; border-top: 1px solid $outline; }
          .tags span { display: inline-block; font-size: 0.8em; color: $primary; background: $primaryLight; border-radius: 6px; padding: 4px 8px; margin: 0 6px 6px 0; }
          .source { margin-top: 10px; font-size: 0.85em; color: $onSurfaceVariant; }
        </style>
        </head>
        <body>${article.fullContent}$sourceHtml$tagsHtml</body>
        </html>
    """.trimIndent()
}

@Composable
private fun ArticleContentWebView(html: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var measuring by remember { mutableStateOf(true) }
    var contentHeight by remember { mutableFloatStateOf(360f) } // dp — sensible default while measuring

    Box(modifier = modifier.fillMaxWidth().height(contentHeight.dp)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        // JS is enabled ONLY so we can measure document.body.scrollHeight
                        // below and size this WebView to its real content height — that's
                        // what lets it sit inline in one continuous scrolling page instead
                        // of scrolling internally underneath a frozen header. This doesn't
                        // reopen an injection surface: no <script> tags, event-handler
                        // attributes, or javascript: URIs survive sanitize-html's allowlist
                        // server-side, so there's nothing here for the JS engine to run
                        // beyond our own measurement call.
                        javaScriptEnabled = true
                        domStorageEnabled = false
                        allowContentAccess = false
                        allowFileAccess = false
                        loadWithOverviewMode = true
                        useWideViewPort = false
                    }
                    // This WebView no longer owns its own scroll — the outer Compose
                    // Column scrolls everything (header + this) together.
                    isVerticalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            // With width=device-width / initial-scale=1.0 and
                            // useWideViewPort=false, 1 CSS px == 1 dp here, so the
                            // measured value can be used directly as a dp height.
                            view?.evaluateJavascript("document.body.scrollHeight.toString()") { result ->
                                val px = result?.trim('"')?.toFloatOrNull()
                                if (px != null && px > 0f) contentHeight = px + 16f
                                measuring = false
                            }
                        }
                        // Article links open in the user's browser instead of
                        // navigating away inside this WebView.
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val uri = request?.url ?: return false
                            return try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                true
                            } catch (_: Exception) { false }
                        }
                    }
                    loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (measuring) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp, color = BpscColors.Primary)
            }
        }
    }
}

@Composable
fun CaArticleDetailScreen(
    navController: NavHostController,
    affairId: String,
    viewModel: CurrentAffairsViewModel = hiltViewModel()
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val context = LocalContext.current

    val detailState by viewModel.articleDetailState.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val mcqs by viewModel.mcqs.collectAsState()
    val mcqLoading by viewModel.mcqLoading.collectAsState()
    val mcqMarkingConfig by viewModel.mcqMarkingConfig.collectAsState()
    val isDownloadingPdf by viewModel.isDownloadingPdf.collectAsState()
    val pdfError by viewModel.pdfError.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(pdfError) {
        pdfError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearPdfError()
        }
    }

    LaunchedEffect(affairId) {
        com.example.bpscnotes.core.analytics.Event.screenView("ca_article_detail")
        viewModel.loadArticleDetail(affairId)
        viewModel.loadMcqs(affairId)
    }

    TrackStudyTime(activityType = "reading") { type, secs -> viewModel.logStudyTime(type, secs) }

    val article = detailState.article
    val isBookmarked = article?.let { bookmarkedIds.contains(it.id) } ?: false

    Box(modifier = Modifier.fillMaxSize().background(cs.background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Hero header — status bar, top bar, and the MCQ CTA all sit
            // on one continuous gradient panel, matching the header style
            // used in CurrentAffairsScreen and CaMcqQuizScreen. Top bar is
            // always shown (back works even while loading/erroring); the
            // CTA only appears once the article has loaded.
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)),
                            start = Offset(0f, 0f), end = Offset(400f, 400f)
                        )
                    )
                    .statusBarsPadding()
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                                .clickable { navController.popBackStackSafe() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            article?.category ?: str.caTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (article != null) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                                        .clickable(enabled = !isDownloadingPdf) {
                                            scope.launch {
                                                val uri = viewModel.downloadArticlePdf(article.id, article.headline)
                                                uri?.let {
                                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(it, "application/pdf")
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    try {
                                                        context.startActivity(Intent.createChooser(viewIntent, "Open PDF"))
                                                    } catch (_: Exception) {
                                                        // No PDF viewer installed — fall back to a share
                                                        // sheet so they can still save it via Drive/etc.
                                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = "application/pdf"
                                                            putExtra(Intent.EXTRA_STREAM, it)
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDownloadingPdf) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                    } else {
                                        Icon(Icons.Rounded.Download, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                                        .clickable {
                                            val shareText = "${article.headline}\n\n${article.summary}\n\nRead more on BPSCNotes app"
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, article.headline)
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(intent, str.caShare))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Share, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                                        .clickable { viewModel.toggleBookmark(article.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, null,
                                        tint = if (isBookmarked) BpscColors.CoinGold else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (article != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 2.dp, bottom = 16.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(str.caMcqPractice, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(
                                    when {
                                        mcqLoading -> str.caLoadingQuestions
                                        mcqs.isEmpty() -> str.caNoMcqs
                                        else -> "${mcqs.size} questions from this article"
                                    },
                                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f)
                                )
                                if (mcqs.isNotEmpty() && mcqMarkingConfig.negativeMarkingEnabled) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("⚠️", fontSize = 10.sp)
                                        Text(
                                            "+${com.example.bpscnotes.presentation.quiz.formatMarks(mcqMarkingConfig.marksPerCorrect)} correct · -${com.example.bpscnotes.presentation.quiz.formatMarks(mcqMarkingConfig.marksPerWrong)} wrong",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFFCDD2),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (article.mcqAttempted && article.mcqLastScore != null) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("⭐", fontSize = 10.sp)
                                        Text(
                                            "Last score: ${com.example.bpscnotes.presentation.quiz.formatMarks(article.mcqLastScore)} marks",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFC8FACC),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            if (mcqLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Button(
                                    onClick = { navController.navigate(Screen.CaAdGate.createRoute(article.id, "mcq")) },
                                    enabled = mcqs.isNotEmpty(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF0A2472),
                                        disabledContainerColor = Color.White.copy(alpha = 0.3f),
                                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                                    )
                                ) { Text("Start MCQ", style = MaterialTheme.typography.titleSmall) }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = cs.outline.copy(alpha = 0.3f))

            when {
                detailState.isLoading && article == null -> {
                    AppLoader(modifier = Modifier.weight(1f).fillMaxWidth())
                }
                detailState.error != null && article == null -> {
                    AppErrorState(
                        message = detailState.error!!,
                        onRetry = { viewModel.loadArticleDetail(affairId) },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
                article != null -> {
                    val (catFg, catBg) = CA_DETAIL_CATEGORY_COLORS[article.category] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)

                    // ── Everything below scrolls as ONE page — heading,
                    // meta, and article body move together. Only the
                    // gradient header above stays fixed. ─────────────────
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    article.category, style = MaterialTheme.typography.labelSmall, color = catFg, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(catBg).padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                                if (article.isImportant) {
                                    Row(
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFF3CD)).padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text("⭐", fontSize = 9.sp)
                                        Text(str.caImportant, style = MaterialTheme.typography.labelSmall, color = Color(0xFF856404), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (article.isPrelims) Text(str.filterPrelims, style = MaterialTheme.typography.labelSmall, color = Color(0xFF1ABC9C), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFE8FDF8)).padding(horizontal = 6.dp, vertical = 2.dp))
                                if (article.isMains) Text(str.filterMains, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9B59B6), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFF3E8FD)).padding(horizontal = 6.dp, vertical = 2.dp))
                            }

                            Text(article.headline, style = MaterialTheme.typography.headlineSmall, color = cs.onSurface, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(article.date, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                                Text("•", color = cs.onSurfaceVariant)
                                Icon(Icons.Rounded.Schedule, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(13.dp))
                                Text("${article.readMinutes} min read", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                            }
                        }

                        val html = remember(article.id) {
                            buildArticleHtml(
                                article = article,
                                surface = cs.surface.toCssHex(),
                                onSurface = cs.onSurface.toCssHex(),
                                onSurfaceVariant = cs.onSurfaceVariant.toCssHex(),
                                outline = cs.outline.toCssHex(),
                                primary = BpscColors.Primary.toCssHex(),
                                primaryLight = BpscColors.PrimaryLight.toCssHex(),
                            )
                        }
                        ArticleContentWebView(
                            html = html,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}