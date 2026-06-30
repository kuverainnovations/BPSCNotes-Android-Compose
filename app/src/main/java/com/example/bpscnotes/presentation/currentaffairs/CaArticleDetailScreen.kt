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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
    val filteredTags = article.tags.filter { !it.equals("general", ignoreCase = true) }
    val tagsHtml = if (filteredTags.isNotEmpty()) {
        "<div class=\"tags\">" + filteredTags.joinToString("") { "<span>#${escapeHtml(it)}</span>" } + "</div>"
    } else ""
    val sourceHtml = if (!article.source.isNullOrBlank()) {
        "<p class=\"source\">📌 Source: ${escapeHtml(article.source)}</p>"
    } else ""

    // Summary as an italic lead paragraph before the full body.
    val summaryHtml = if (article.summary.stripHtmlTags().isNotBlank()) {
        "<p class=\"lead\">${article.summary}</p><hr class=\"lead-rule\">"
    } else ""

    // ── New content sections — each wrapped in a labelled section div ──
    // Only rendered if the admin populated the field (non-empty after strip).
    val keyPointsHtml = if (article.keyPoints.stripHtmlTags().isNotBlank()) {
        """<div class="section-block key-points">
          <div class="section-label">🔑 Key Points</div>
          ${article.keyPoints}
        </div>"""
    } else ""

    val examRelevanceHtml = if (article.examRelevance.stripHtmlTags().isNotBlank()) {
        """<div class="section-block exam-relevance">
          <div class="section-label">🎯 Exam Relevance</div>
          ${article.examRelevance}
        </div>"""
    } else ""

    val importantFactsHtml = if (article.importantFacts.stripHtmlTags().isNotBlank()) {
        """<div class="section-block important-facts">
          <div class="section-label">📊 Important Facts &amp; Figures</div>
          ${article.importantFacts}
        </div>"""
    } else ""

    return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=2.0, user-scalable=yes">
        <style>
          * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
          html, body { margin: 0; padding: 0; background: $surface; }
          body { padding: 4px 4px 48px; font-family: -apple-system, Roboto, 'Segoe UI', sans-serif;
                 font-size: 16px; line-height: 1.8; color: $onSurface; word-break: break-word; }
          p { margin: 0 0 16px; }
          h1 { font-size: 1.5em; font-weight: 800; margin: 28px 0 12px; color: $primary; line-height: 1.3; }
          h2 { font-size: 1.3em; font-weight: 800; margin: 24px 0 10px; color: $primary; line-height: 1.35; }
          h3 { font-size: 1.12em; font-weight: 700; margin: 20px 0 8px; color: $primary; line-height: 1.4; }
          ul, ol { padding-left: 1.5em; margin: 0 0 16px; }
          li { margin-bottom: 8px; }
          li::marker { color: $primary; font-weight: 700; }
          blockquote {
            border-left: 3.5px solid $primary;
            padding: 8px 16px;
            margin: 18px 0;
            background: $primaryLight;
            border-radius: 0 10px 10px 0;
            color: $onSurfaceVariant;
            font-style: italic;
          }
          a { color: $primary; text-decoration: underline; }
          table { border-collapse: collapse; width: 100%; margin: 20px 0; display: block; overflow-x: auto; -webkit-overflow-scrolling: touch; }
          th, td { border: 1px solid $outline; padding: 10px 14px; text-align: left; min-width: 80px; }
          th { background: $primaryLight; font-weight: 700; color: $primary; }
          img { max-width: 100%; height: auto; border-radius: 10px; display: block; margin: 16px auto; }
          mark { border-radius: 3px; padding: 1px 3px; }
          strong { font-weight: 700; }
          em { font-style: italic; }
          u { text-decoration: underline; }
          s { text-decoration: line-through; }
          hr.lead-rule { border: none; border-top: 1px solid $outline; margin: 18px 0 16px; }
          .lead {
            font-style: italic;
            color: $onSurfaceVariant;
            font-size: 1.05em;
            line-height: 1.75;
            margin: 0 0 0;
            padding: 12px 16px;
            background: $primaryLight;
            border-radius: 10px;
            border-left: 3px solid $primary;
          }
          .tags { margin-top: 28px; padding-top: 16px; border-top: 1px solid $outline; }
          .tags span { display: inline-block; font-size: 0.82em; color: $primary;
                       background: $primaryLight; border-radius: 6px;
                       padding: 4px 10px; margin: 0 6px 6px 0; font-weight: 600; }
          .source { margin-top: 14px; font-size: 0.85em; color: $onSurfaceVariant; font-style: italic; }

          /* ── New content section blocks ───────────────────────── */
          .section-block { margin: 20px 0; padding: 14px 16px 12px;
                           border-radius: 10px; border-left: 4px solid; }
          .section-label { font-size: 0.78em; font-weight: 800; letter-spacing: 0.04em;
                           text-transform: uppercase; margin-bottom: 10px; opacity: 0.85; }
          .key-points      { background: #F0F7FF; border-color: #1565C0; color: $onSurface; }
          .key-points .section-label      { color: #1565C0; }
          .exam-relevance  { background: #FFF8E6; border-color: #E65100; color: $onSurface; }
          .exam-relevance .section-label  { color: #E65100; }
          .important-facts { background: #F3F9F3; border-color: #2E7D32; color: $onSurface; }
          .important-facts .section-label { color: #2E7D32; }
          /* Lists inside section blocks */
          .section-block ul { list-style-type: disc !important; padding-left: 1.4em !important; margin: 6px 0; }
          .section-block ol { list-style-type: decimal !important; padding-left: 1.4em !important; margin: 6px 0; }
          .section-block li { margin: 4px 0; }
          .section-block p  { margin: 0 0 8px; }
        </style>
        </head>
        <body>$summaryHtml$keyPointsHtml${article.fullContent}$examRelevanceHtml$importantFactsHtml$sourceHtml$tagsHtml</body>
        </html>
    """.trimIndent()
}

@Composable
private fun ArticleContentWebView(html: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = false
                    domStorageEnabled = false
                    allowContentAccess = false
                    allowFileAccess = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
                isVerticalScrollBarEnabled = true
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?, request: WebResourceRequest?
                    ): Boolean {
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
        modifier = modifier
    )
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
                            article?.headline?.stripHtmlTags() ?: str.caTitle,
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
                                                val uri = viewModel.downloadArticlePdf(article.id, article.headline.stripHtmlTags())
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
                                            val headline = article.headline.stripHtmlTags()
                                            val summary = article.summary.stripHtmlTags().take(300).let {
                                                if (it.length == 300) "$it…" else it
                                            }
                                            val tags = article.tags
                                                .filter { !it.equals("general", ignoreCase = true) }
                                                .take(3)
                                                .joinToString(" ") { "#${it.replace(" ", "")}" }
                                            val shareText = buildString {
                                                appendLine("📰 $headline")
                                                appendLine()
                                                if (summary.isNotBlank()) {
                                                    appendLine(summary)
                                                    appendLine()
                                                }
                                                if (tags.isNotBlank()) appendLine(tags)
                                                appendLine()
                                                append("📚 BPSCNotes App — Stay ahead in BPSC preparation")
                                            }
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, headline)
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
                                    onClick = { navController.navigate("ca_mcq_quiz/${article.id}") },
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

                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (article.category.isNotBlank() && !article.category.equals("general", ignoreCase = true)) {
                                    Text(
                                        article.category, style = MaterialTheme.typography.labelSmall, color = catFg, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(catBg).padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
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

                            RichHtmlText(article.headline, style = MaterialTheme.typography.headlineSmall, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)

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
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)
                        )
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