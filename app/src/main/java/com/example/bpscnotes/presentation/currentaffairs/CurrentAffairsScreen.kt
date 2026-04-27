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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel          // ← use hiltViewModel, not viewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors

// ── NOTE: mockCAArticles and local `categories` removed.
// ── Articles come from CurrentAffairsViewModel.
// ── Categories from CAArticle.kt → CA_CATEGORIES constant.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentAffairsScreen(
    navController: NavHostController,
    viewModel: CurrentAffairsViewModel = hiltViewModel()   // ← was missing before
) {
    val state        by viewModel.uiState.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()

    var selectedTab      by remember { mutableIntStateOf(0) }
    var searchQuery      by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedArticle  by remember { mutableStateOf<CAArticle?>(null) }
    val focusManager      = LocalFocusManager.current
    val tabs              = listOf("All", "Prelims", "Mains", "Saved 🔖")

    // Reload when category changes
    LaunchedEffect(selectedCategory) {
        viewModel.loadArticles(category = selectedCategory)
    }

    val articles = state.articles

    val filtered = articles.filter { article ->
        val matchesTab = when (selectedTab) {
            1 -> article.isPrelims
            2 -> article.isMains
            3 -> bookmarkedIds.contains(article.id)
            else -> true
        }
        val matchesCat = selectedCategory == "All" || article.category == selectedCategory
        val matchesSearch = searchQuery.isEmpty() ||
                article.headline.contains(searchQuery, ignoreCase = true) ||
                article.summary.contains(searchQuery, ignoreCase = true) ||
                article.tags.any { it.contains(searchQuery, ignoreCase = true) }
        matchesTab && matchesCat && matchesSearch
    }

    val grouped = filtered.groupBy { it.date }.entries.sortedByDescending { it.key }

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { navController.popBackStack() }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Current Affairs", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text("Stay updated, score higher", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                            }
                        }
                        // Bookmark count chip — taps to Saved tab
                        Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.15f)).clickable { selectedTab = 3 }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.Bookmark, null, tint = BpscColors.CoinGold, modifier = Modifier.size(14.dp))
                                Text("${bookmarkedIds.size}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            }
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
                                if (searchQuery.isEmpty()) Text("Search topics, keywords...", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.5f))
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
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (sel) Color.White else Color.White.copy(0.12f)).clickable { selectedTab = index }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(tab, style = MaterialTheme.typography.bodyMedium, color = if (sel) BpscColors.Primary else Color.White.copy(0.85f), fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, fontSize = if (index == 3) 11.sp else 14.sp)
                            }
                        }
                    }
                }
            }

            // Category chips — uses CA_CATEGORIES from CAArticle.kt
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CA_CATEGORIES) { cat ->
                    val sel = cat == selectedCategory
                    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (sel) BpscColors.Primary else Color.White).border(1.dp, if (sel) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(20.dp)).clickable { selectedCategory = cat }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                        Text(cat, style = MaterialTheme.typography.bodyMedium, color = if (sel) Color.White else BpscColors.TextSecondary, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            // Stats row
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                CAStatChip("📰", "${filtered.size}", "Articles")
                Box(Modifier.width(1.dp).height(24.dp).background(BpscColors.Divider))
                CAStatChip("⭐", "${filtered.count { it.isImportant }}", "Important")
                Box(Modifier.width(1.dp).height(24.dp).background(BpscColors.Divider))
                CAStatChip("❓", "${filtered.sumOf { it.mcqCount }}", "MCQs")
                Box(Modifier.width(1.dp).height(24.dp).background(BpscColors.Divider))
                CAStatChip("🔖", "${bookmarkedIds.size}", "Saved")
            }

            Spacer(Modifier.height(8.dp))

            // ── Content area ──────────────────────────────────
            when {
                // Loading
                state.isLoading && articles.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BpscColors.Primary)
                    }
                }

                // Error
                state.error != null && articles.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(24.dp)) {
                            Text("⚠️", fontSize = 40.sp)
                            Text("Couldn't load articles", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                            Text(state.error!!, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary, textAlign = TextAlign.Center)
                            Button(onClick = { viewModel.refresh() }, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Retry")
                            }
                        }
                    }
                }

                // Empty filter result
                filtered.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (selectedTab == 3) "🔖" else "🔍", fontSize = 48.sp)
                            Text(if (selectedTab == 3) "No saved articles yet" else "No articles found", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                            Text(if (selectedTab == 3) "Bookmark articles to see them here" else "Try a different search or filter", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                        }
                    }
                }

                // Article list
                else -> {
                    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        grouped.forEach { (date, dateArticles) ->
                            stickyHeader(key = date) { DateGroupHeader(date = date, count = dateArticles.size) }
                            itemsIndexed(dateArticles, key = { _, a -> a.id }) { _, article ->
                                CAArticleCard(
                                    article     = article,
                                    isBookmarked = bookmarkedIds.contains(article.id),
                                    onBookmark  = { viewModel.toggleBookmark(article.id) },
                                    onShare     = { },
                                    onReadMore  = { selectedArticle = article }
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }

        // Bottom sheet
        selectedArticle?.let { article ->
            ArticleBottomSheet(
                article     = article,
                isBookmarked = bookmarkedIds.contains(article.id),
                onBookmark  = { viewModel.toggleBookmark(article.id) },
                onDismiss   = { selectedArticle = null }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STICKY DATE HEADER
// ─────────────────────────────────────────────────────────────
@Composable
private fun DateGroupHeader(date: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth().background(BpscColors.Surface).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(BpscColors.PrimaryLight).padding(horizontal = 10.dp, vertical = 5.dp)) {
            Text(date, style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
        }
        Box(modifier = Modifier.height(1.dp).weight(1f).background(BpscColors.Divider))
        Text("$count articles", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
    }
}

// ─────────────────────────────────────────────────────────────
// ARTICLE CARD — unchanged
// ─────────────────────────────────────────────────────────────
@Composable
private fun CAArticleCard(article: CAArticle, isBookmarked: Boolean, onBookmark: () -> Unit, onShare: () -> Unit, onReadMore: () -> Unit) {
    val categoryColors = mapOf("Economy" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)), "Polity" to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)), "International" to Pair(Color(0xFF3498DB), Color(0xFFE8F4FD)), "Science" to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)), "Education" to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)), "Sports" to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)), "Bihar GK" to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)))
    val (catFg, catBg) = categoryColors[article.category] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(article.category, style = MaterialTheme.typography.labelSmall, color = catFg, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(catBg).padding(horizontal = 8.dp, vertical = 3.dp))
                    if (article.isImportant) Row(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFF3CD)).padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Text("⭐", fontSize = 9.sp); Text("Important", style = MaterialTheme.typography.labelSmall, color = Color(0xFF856404), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                    if (article.isPrelims) Text("P", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1ABC9C), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFE8FDF8)).padding(horizontal = 5.dp, vertical = 2.dp))
                    if (article.isMains) Text("M", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9B59B6), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFF3E8FD)).padding(horizontal = 5.dp, vertical = 2.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Icon(Icons.Rounded.Schedule, null, tint = BpscColors.TextHint, modifier = Modifier.size(11.dp)); Text("${article.readMinutes} min", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp) }
            }
            Spacer(Modifier.height(10.dp))
            Text(article.headline, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold, lineHeight = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(article.summary, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text("Read more ↓", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp).clickable { onReadMore() })
            Spacer(Modifier.height(12.dp)); HorizontalDivider(color = BpscColors.Divider); Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(BpscColors.PrimaryLight).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text("❓", fontSize = 11.sp); Text("${article.mcqCount} MCQs from this topic", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.SemiBold, fontSize = 10.sp) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(BpscColors.Surface).clickable(onClick = onShare), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Share, null, tint = BpscColors.TextSecondary, modifier = Modifier.size(16.dp)) }
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(if (isBookmarked) Color(0xFFFFF8E1) else BpscColors.Surface).clickable(onClick = onBookmark), contentAlignment = Alignment.Center) { Icon(if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, null, tint = if (isBookmarked) BpscColors.CoinGold else BpscColors.TextSecondary, modifier = Modifier.size(16.dp)) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ARTICLE BOTTOM SHEET — unchanged
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleBottomSheet(article: CAArticle, isBookmarked: Boolean, onBookmark: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val categoryColors = mapOf("Economy" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)), "Polity" to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)), "International" to Pair(Color(0xFF3498DB), Color(0xFFE8F4FD)), "Science" to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)), "Education" to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)), "Sports" to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)), "Bihar GK" to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)))
    val (catFg, catBg) = categoryColors[article.category] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), contentWindowInsets = { WindowInsets(0, 0, 0, 0) }) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0)), start = Offset(0f, 0f), end = Offset(400f, 200f))).padding(horizontal = 20.dp, vertical = 20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(article.category, style = MaterialTheme.typography.labelSmall, color = catFg, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(catBg).padding(horizontal = 8.dp, vertical = 3.dp))
                        if (article.isImportant) Row(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFF3CD)).padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Text("⭐", fontSize = 9.sp); Text("Important", style = MaterialTheme.typography.labelSmall, color = Color(0xFF856404), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Icon(Icons.Rounded.Schedule, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(11.dp)); Text("${article.readMinutes} min read", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f), fontSize = 10.sp) }
                    }
                    Text(article.headline, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(article.date, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.6f))
                        if (article.isPrelims) Text("Prelims", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1ABC9C), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFE8FDF8)).padding(horizontal = 6.dp, vertical = 2.dp))
                        if (article.isMains) Text("Mains", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9B59B6), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFF3E8FD)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(article.fullContent, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, lineHeight = 26.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    article.tags.forEach { tag -> Text("#$tag", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontSize = 10.sp, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(BpscColors.PrimaryLight).padding(horizontal = 8.dp, vertical = 3.dp)) }
                }
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BpscColors.PrimaryLight).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("Practice MCQs", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.Bold); Text("${article.mcqCount} questions from this topic", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary) }
                    Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) { Text("Start", style = MaterialTheme.typography.titleMedium) }
                }
            }
            HorizontalDivider(color = BpscColors.Divider)
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onBookmark, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, if (isBookmarked) BpscColors.CoinGold else BpscColors.Divider), colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isBookmarked) BpscColors.CoinGold else BpscColors.TextSecondary)) {
                    Icon(if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(if (isBookmarked) "Saved ✓" else "Save", style = MaterialTheme.typography.titleMedium)
                }
                Button(onClick = {}, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                    Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Share", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STAT CHIP
// ─────────────────────────────────────────────────────────────
@Composable
private fun CAStatChip(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(icon, fontSize = 13.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 9.sp)
    }
}
