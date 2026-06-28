package com.example.bpscnotes.presentation.currentaffairs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel          // ← use hiltViewModel, not viewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.core.ads.BannerAdView
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.example.bpscnotes.presentation.navigation.Routes.Screen

// Inline study time tracker — starts timing when screen opens, logs on leave
@androidx.compose.runtime.Composable
private fun TrackStudyTime(
    activityType: String,
    onLog: (activityType: String, durationSecs: Int) -> Unit
) {
    val startTime = androidx.compose.runtime.remember { System.currentTimeMillis() }
    androidx.compose.runtime.DisposableEffect(activityType) {
        onDispose {
            val elapsed = ((System.currentTimeMillis() - startTime) / 1000)
                .toInt().coerceIn(0, 3600)
            if (elapsed >= 10) onLog(activityType, elapsed)
        }
    }
}

// ── NOTE: mockCAArticles and local `categories` removed.
// ── Articles come from CurrentAffairsViewModel.
// ── Categories from CAArticle.kt → CA_CATEGORIES constant.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentAffairsScreen(
    navController: NavHostController,
    adManager: com.example.bpscnotes.core.ads.AdManager? = null,
    viewModel: CurrentAffairsViewModel = hiltViewModel()   // ← was missing before
) {
    val state        by viewModel.uiState.collectAsState()
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    LaunchedEffect(Unit) { com.example.bpscnotes.core.analytics.Event.screenView("current_affairs") }
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val mcqMarkingConfig by viewModel.mcqMarkingConfig.collectAsState()

    // ── Silent background time tracker ────────────────────────
    TrackStudyTime(
        activityType = "reading"
    ) { type, secs -> viewModel.logStudyTime(type, secs) }

    var selectedTab      by remember { mutableIntStateOf(0) }
    var searchQuery      by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(str.filterAll) }
    val focusManager      = LocalFocusManager.current
    val tabs              = listOf(str.filterAll, str.filterPrelims, str.filterMains, str.filterSaved)

    val context = LocalContext.current

    val allArticles = state.allArticles
    val articles    = state.articles

    // Categories derived from full unfiltered list — never changes when user picks a filter
    val dynamicCategories: List<String> = remember(allArticles) {
        listOf(str.filterAll) + allArticles.map { it.category }.distinct().filter { it.isNotBlank() }.sorted()
    }

    val filtered = allArticles.filter { article ->
        val matchesTab = when (selectedTab) {
            1 -> article.isPrelims
            2 -> article.isMains
            3 -> bookmarkedIds.contains(article.id)
            else -> true
        }
        val matchesCat = selectedCategory == str.filterAll || article.category == selectedCategory
        val matchesSearch = searchQuery.isNullOrEmpty() ||
                article.headline.stripHtmlTags().contains(searchQuery, ignoreCase = true) ||
                article.summary.stripHtmlTags().contains(searchQuery, ignoreCase = true) ||
                article.tags.any { it.contains(searchQuery, ignoreCase = true) }
        matchesTab && matchesCat && matchesSearch
    }

    val grouped = filtered.groupBy { it.rawDate }.entries.sortedByDescending { it.key }

    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = state.isLoading && allArticles.isNotEmpty(),
        onRefresh = { viewModel.refresh() }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(cs.background)) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Header ─────────────────────────────────────────
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)), start = Offset(0f, 0f), end = Offset(400f, 400f)))
                        .statusBarsPadding()
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(Color.White.copy(0.05f), 150.dp.toPx(), Offset(size.width + 20.dp.toPx(), -40.dp.toPx()))
                        drawCircle(Color.White.copy(0.04f), 80.dp.toPx(), Offset(-20.dp.toPx(), size.height * 0.6f))
                    }
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { navController.popBackStackSafe() }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(str.caTitle, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text(str.caSubtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Search bar
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(0.15f)).border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Search, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery, onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                decorationBox = { inner ->
                                    if (searchQuery.isNullOrEmpty()) Text(str.caSearchHint, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.5f))
                                    inner()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                Icon(Icons.Rounded.Close, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(16.dp).clickable { searchQuery = ""; focusManager.clearFocus() })
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Tabs
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            tabs.forEachIndexed { index, tab ->
                                val sel = selectedTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (sel) Color.White else Color.White.copy(0.12f))
                                        .clickable { selectedTab = index }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(tab, style = MaterialTheme.typography.bodyMedium, color = if (sel) BpscColors.Primary else Color.White.copy(0.85f), fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, fontSize = if (index == 3) 11.sp else 14.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Compact stats row inside header
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(
                                Triple("📰", "${filtered.size}", "articles"),
                                Triple("⭐", "${filtered.count { it.isImportant }}", str.caImportant.lowercase()),
                                Triple("❓", "${filtered.sumOf { it.mcqCount }}", "MCQs"),
                                Triple("🔖", "${bookmarkedIds.size}", "saved"),
                            ).forEach { (icon, value, label) ->
                                Text(
                                    "$icon $value $label",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(0.75f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Category chips — dynamic from loaded articles
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dynamicCategories) { cat ->
                        val sel = cat == selectedCategory
                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (sel) BpscColors.Primary else Color.White).border(1.dp, if (sel) BpscColors.Primary else cs.outline, RoundedCornerShape(20.dp)).clickable { selectedCategory = cat }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                            Text(cat, style = MaterialTheme.typography.bodyMedium, color = if (sel) Color.White else BpscColors.TextSecondary, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                // ── Content area ──────────────────────────────────
                when {
                    // Loading
                    state.isLoading && articles.isNullOrEmpty() -> {
                        com.example.bpscnotes.core.ui.ListScreenSkeleton(headerHeight = 150.dp, itemCount = 5, itemHeight = 100.dp)
                    }

                    // Error
                    state.error != null && articles.isNullOrEmpty() -> {
                        com.example.bpscnotes.core.ui.AppErrorState(
                            message = state.error!!,
                            onRetry = { viewModel.refresh() }
                        )
                    }

                    // Empty filter result
                    filtered.isNullOrEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (selectedTab == 3) "🔖" else "🔍", fontSize = 48.sp)
                                Text(if (selectedTab == 3) str.caNoSaved else str.caNoArticles, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, fontWeight = FontWeight.Bold)
                                Text(if (selectedTab == 3) str.caBookmarkHint else str.caTryFilter, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                            }
                        }
                    }

                    // Article list
                    else -> {
                        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            var globalArticleIndex = 0
                            grouped.forEach { (date, dateArticles) ->
                                stickyHeader(key = date) { DateGroupHeader(date = dateArticles.firstOrNull()?.date ?: date, count = dateArticles.size) }
                                itemsIndexed(dateArticles, key = { _, a -> a.id }) { _, article ->
                                    CAArticleCard(
                                        article     = article,
                                        isBookmarked = bookmarkedIds.contains(article.id),
                                        markingConfig = mcqMarkingConfig,
                                        onBookmark  = { viewModel.toggleBookmark(article.id) },
                                        onShare     = {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(android.content.Intent.EXTRA_SUBJECT, article.headline.stripHtmlTags())
                                                putExtra(android.content.Intent.EXTRA_TEXT, "${article.headline.stripHtmlTags()}\n\n${article.summary.stripHtmlTags()}\n\nRead more on BPSCNotes app")
                                            }
                                            context.startActivity(android.content.Intent.createChooser(intent, str.caShare))
                                        },
                                        onReadMore  = { navController.navigate(Screen.CaArticleDetail.createRoute(article.id)) },
                                        onStartMcq = {
                                            navController.navigate("ca_mcq_quiz/${article.id}")
                                        }

                                    )
                                    Spacer(Modifier.height(10.dp))

                                    // Banner every 7 articles (requirement: every 7 items)
                                    // Counted across the whole flattened list so a long
                                    // single-date group still gets ads at a steady cadence.
                                    globalArticleIndex++
                                    if (adManager != null && globalArticleIndex % 7 == 0) {
                                        BannerAdView(adUnitId = adManager.getBannerAdUnitId())
                                        Spacer(Modifier.height(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } // end PullToRefreshBox
}

// ─────────────────────────────────────────────────────────────
// STICKY DATE HEADER
// ─────────────────────────────────────────────────────────────
@Composable
private fun formatArticleDate(isoDate: String): String {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val parsed = sdf.parse(isoDate)
        if (parsed != null) {
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(parsed)
        } else isoDate.take(10) // fallback: just the date part
    } catch (e: Exception) {
        isoDate.take(10) // e.g. "2026-05-22"
    }
}

@Composable
private fun DateGroupHeader(date: String, count: Int) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(modifier = Modifier.fillMaxWidth().background(cs.background).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(BpscColors.PrimaryLight).padding(horizontal = 10.dp, vertical = 5.dp)) {
            Text(date, style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
        }
        Box(modifier = Modifier.height(1.dp).weight(1f).background(cs.outline))
        Text("$count articles", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
    }
}

// ─────────────────────────────────────────────────────────────
// ARTICLE CARD — unchanged
// ─────────────────────────────────────────────────────────────
@Composable
private fun CAArticleCard(article: CAArticle, isBookmarked: Boolean, markingConfig: com.example.bpscnotes.data.remote.api.CaMcqMarkingConfigDto = com.example.bpscnotes.data.remote.api.CaMcqMarkingConfigDto(), onBookmark: () -> Unit, onShare: () -> Unit, onReadMore: () -> Unit,   onStartMcq: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val categoryColors = mapOf("Economy" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)), "Polity" to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)), "International" to Pair(Color(0xFF3498DB), Color(0xFFE8F4FD)), "Science" to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)), "Education" to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)), "Sports" to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)), "Bihar GK" to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)))
    val (catFg, catBg) = categoryColors[article.category] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onReadMore() },  // FIX: whole card clickable
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(article.category, style = MaterialTheme.typography.labelSmall, color = catFg, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(catBg).padding(horizontal = 8.dp, vertical = 3.dp))
                    if (article.isImportant) Row(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFF3CD)).padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Text("⭐", fontSize = 9.sp); Text(str.caImportant, style = MaterialTheme.typography.labelSmall, color = Color(0xFF856404), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                    if (article.isPrelims) Text("P", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1ABC9C), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFE8FDF8)).padding(horizontal = 5.dp, vertical = 2.dp))
                    if (article.isMains) Text("M", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9B59B6), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFF3E8FD)).padding(horizontal = 5.dp, vertical = 2.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Icon(Icons.Rounded.Schedule, null, tint = BpscColors.TextHint, modifier = Modifier.size(11.dp)); Text("${article.readMinutes} min", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp) }
            }
            Spacer(Modifier.height(10.dp))
            RichHtmlText(article.headline, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.ExtraBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            RichHtmlText(article.summary, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(str.caReadMore, style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp).clickable { onReadMore() })
            Spacer(Modifier.height(12.dp)); HorizontalDivider(color = cs.outline); Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {

                val hasMcqs = article.mcqCount > 0

                Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (hasMcqs)
                                    BpscColors.PrimaryLight
                                else
                                    Color(0xFFF1F5F9)
                            )
                            .clickable(enabled = hasMcqs) {
                                onStartMcq()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            if (hasMcqs) "❓" else "🚫",
                            fontSize = 11.sp
                        )

                        Text(
                            if (hasMcqs)
                                "${article.mcqCount} MCQs from this topic"
                            else
                                "No MCQs available",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasMcqs)
                                BpscColors.Primary
                            else
                                BpscColors.TextHint,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        )
                    }
                    if (hasMcqs && markingConfig.negativeMarkingEnabled) {
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEE8E8))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("⚠️", fontSize = 10.sp)
                            Text(
                                "-${com.example.bpscnotes.presentation.quiz.formatMarks(markingConfig.marksPerWrong)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC0392B),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        }
                    }
                    if (article.mcqAttempted && article.mcqLastScore != null) {
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(BpscColors.Success.copy(0.1f))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("⭐", fontSize = 10.sp)
                            Text(
                                "Last: ${com.example.bpscnotes.presentation.quiz.formatMarks(article.mcqLastScore)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.Success,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(cs.background).clickable(onClick = onShare), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Share, null, tint = BpscColors.TextSecondary, modifier = Modifier.size(16.dp)) }
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(if (isBookmarked) Color(0xFFFFF8E1) else BpscColors.Surface).clickable(onClick = onBookmark), contentAlignment = Alignment.Center) { Icon(if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, null, tint = if (isBookmarked) BpscColors.CoinGold else BpscColors.TextSecondary, modifier = Modifier.size(16.dp)) }
                }
            }
        }
    }
}

