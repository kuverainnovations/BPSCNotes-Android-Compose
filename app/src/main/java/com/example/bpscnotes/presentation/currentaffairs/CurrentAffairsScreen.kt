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
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.shared.BookmarkViewModel

// ── Extended mock data ────────────────────────────────────────
data class CAArticle(
    val id: String,
    val headline: String,
    val summary: String,
    val fullContent: String,
    val category: String,
    val date: String,
    val readMinutes: Int,
    val mcqCount: Int,
    val isImportant: Boolean,
    val isPrelims: Boolean,
    val isMains: Boolean,
    val tags: List<String>,
)

val mockCAArticles = listOf(
    CAArticle(
        "ca1", "RBI Raises Repo Rate to 6.75%",
        "The Reserve Bank of India's Monetary Policy Committee raised the benchmark repo rate by 25 basis points in its latest review meeting.",
        "The Reserve Bank of India's Monetary Policy Committee (MPC) raised the benchmark repo rate by 25 basis points to 6.75% in its October policy review. This is the fifth consecutive rate hike aimed at bringing inflation within the 4% target band. The decision was unanimous among all six MPC members. Governor Shaktikanta Das highlighted that while inflation is moderating, it remains above the comfort zone. The hike will make loans more expensive but aims to anchor inflation expectations.",
        "Economy", "13 Mar 2026", 3, 5, true, true, true,
        listOf("RBI", "Monetary Policy", "Repo Rate", "Inflation")
    ),
    CAArticle(
        "ca2", "India Wins Bid to Host 2036 Olympics",
        "The International Olympic Committee formally awarded the 2036 Summer Olympics hosting rights to India, marking a historic moment.",
        "India has been formally awarded the hosting rights for the 2036 Summer Olympics by the International Olympic Committee (IOC) during its session in Mumbai. Prime Minister expressed this as a milestone for Indian sports. The games will likely be held in Ahmedabad, with events spread across multiple cities including Delhi and Mumbai. India will invest approximately ₹85,000 crore in infrastructure development for the event.",
        "Sports", "12 Mar 2026", 4, 3, true, true, false,
        listOf("Olympics", "IOC", "Sports", "India")
    ),
    CAArticle(
        "ca3", "New Education Policy 2.0 Draft Released",
        "The Ministry of Education released the draft of NEP 2.0, proposing major changes to the higher education framework.",
        "The Ministry of Education has released a comprehensive draft of NEP 2.0, proposing sweeping reforms to India's higher education system. Key proposals include a credit bank system allowing students to accumulate credits across institutions, mandatory internships, and greater flexibility in course selection. The draft also proposes a 5-year integrated program replacing the traditional 3+2 structure at top universities.",
        "Education", "12 Mar 2026", 5, 4, false, true, true,
        listOf("NEP", "Education", "Higher Education", "Policy")
    ),
    CAArticle(
        "ca4", "India-UAE Sign 14 MoUs on Digital Trade",
        "India and UAE signed 14 Memoranda of Understanding covering digital infrastructure, fintech, and cybersecurity cooperation.",
        "During the state visit of UAE President, India and the UAE signed 14 MoUs focused on digital trade and technology cooperation. The agreements cover areas including UPI-based payment integration in UAE, cybersecurity threat sharing, cloud infrastructure, and AI research collaboration. The bilateral trade target has been revised upward to \$100 billion by 2030.",
        "International", "11 Mar 2026", 3, 6, true, true, true,
        listOf("India-UAE", "MoU", "Digital Trade", "Foreign Policy")
    ),
    CAArticle(
        "ca5", "Bihar Launches Smart Village Mission",
        "The Bihar government launched the Smart Village Mission targeting 5,000 villages with digital and infrastructure upgrades.",
        "Chief Minister launched the Bihar Smart Village Mission with an outlay of ₹12,000 crore targeting digital connectivity, solar power, and clean water for 5,000 villages. The mission integrates PMGSY, Jal Jeevan Mission and BharatNet under a single dashboard. Special focus is given to flood-prone districts of North Bihar.",
        "Bihar GK", "11 Mar 2026", 4, 7, true, false, true,
        listOf("Bihar", "Smart Village", "Digital India", "Infrastructure")
    ),
    CAArticle(
        "ca6", "Supreme Court Verdict on Electoral Bonds",
        "The Supreme Court issued its final order on the electoral bonds scheme, directing full disclosure of donor details.",
        "A 5-judge Constitution Bench of the Supreme Court delivered a landmark judgment directing the State Bank of India to provide complete details of all electoral bond purchases and redemptions to the Election Commission of India. The court held that anonymous political funding violates voters' right to information under Article 19(1)(a).",
        "Polity", "10 Mar 2026", 5, 8, true, true, true,
        listOf("Supreme Court", "Electoral Bonds", "Polity", "Article 19")
    ),
    CAArticle(
        "ca7", "ISRO Successfully Tests Gaganyaan Crew Module",
        "ISRO conducted a successful pad abort test of the Gaganyaan crew module at Sriharikota, ahead of the 2026 crewed mission.",
        "The Indian Space Research Organisation successfully conducted a Crew Escape System test for the Gaganyaan human spaceflight mission. The test validated the abort system that will protect astronauts during launch emergencies. ISRO Chairman confirmed the crewed mission is on track for late 2026 with four astronaut-candidates completing training in Russia.",
        "Science", "10 Mar 2026", 3, 4, false, true, false,
        listOf("ISRO", "Gaganyaan", "Space", "Science & Tech")
    ),
    CAArticle(
        "ca8", "India Becomes 3rd Largest Economy",
        "IMF data confirms India has surpassed Japan to become the world's third largest economy by nominal GDP in 2026.",
        "According to the latest IMF World Economic Outlook data, India has surpassed Japan to become the world's third largest economy with a nominal GDP of \$4.27 trillion. India's growth rate of 7.2% remains the highest among major economies. The milestone is seen as a vindication of India's economic reform agenda over the past decade.",
        "Economy", "09 Mar 2026", 4, 9, true, true, true,
        listOf("GDP", "IMF", "Economy", "India Growth")
    ),
)

val categories = listOf(
    "All",
    "Economy",
    "Polity",
    "International",
    "Science",
    "Education",
    "Sports",
    "Bihar GK"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentAffairsScreen(
    navController: NavHostController,
    bookmarkViewModel: BookmarkViewModel = viewModel()
) {
    val bookmarkedIds by bookmarkViewModel.bookmarkedIds.collectAsState()  // ✅ only this
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedArticle by remember { mutableStateOf<CAArticle?>(null) }  // ← for bottom sheet
    val focusManager = LocalFocusManager.current
    val tabs = listOf("All", "Prelims", "Mains", "Saved 🔖")   // ← 4th tab

    val filtered = mockCAArticles.filter { article ->
        val matchesTab = when (selectedTab) {
            1 -> article.isPrelims
            2 -> article.isMains
            3 -> bookmarkedIds.contains(article.id)    // ← Saved tab
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

    Box(modifier = Modifier
        .fillMaxSize()
        .background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)),
                            start = Offset(0f, 0f), end = Offset(400f, 400f)
                        )
                    )
                    .statusBarsPadding()
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        Color.White.copy(0.05f),
                        150.dp.toPx(),
                        Offset(size.width + 20.dp.toPx(), -40.dp.toPx())
                    )
                    drawCircle(
                        Color.White.copy(0.04f),
                        80.dp.toPx(),
                        Offset(-20.dp.toPx(), size.height * 0.6f)
                    )
                }
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(0.15f))
                                    .clickable { navController.popBackStack() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.ArrowBack,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Current Affairs",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    "Stay updated, score higher",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(0.7f)
                                )
                            }
                        }
                        // ✅ Bookmark count — taps to Saved tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(0.15f))
                                .clickable { selectedTab = 3 }    // ← jump to Saved tab
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Bookmark,
                                    null,
                                    tint = BpscColors.CoinGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "${bookmarkedIds.size}",       // ✅ from VM
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Search bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(0.15f))
                            .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            null,
                            tint = Color.White.copy(0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) Text(
                                    "Search topics, keywords...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(0.5f)
                                )
                                inner()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Rounded.Close, null, tint = Color.White.copy(0.7f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { searchQuery = ""; focusManager.clearFocus() })
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                Text(
                                    tab,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (sel) BpscColors.Primary else Color.White.copy(0.85f),
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = if (index == 3) 11.sp else 14.sp   // ← smaller for "Saved 🔖"
                                )
                            }
                        }
                    }
                }
            }

            // Category chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val sel = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (sel) BpscColors.Primary else Color.White)
                            .border(
                                1.dp,
                                if (sel) BpscColors.Primary else BpscColors.Divider,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            cat, style = MaterialTheme.typography.bodyMedium,
                            color = if (sel) Color.White else BpscColors.TextSecondary,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CAStatChip("📰", "${filtered.size}", "Articles")
                Box(Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(BpscColors.Divider))
                CAStatChip("⭐", "${filtered.count { it.isImportant }}", "Important")
                Box(Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(BpscColors.Divider))
                CAStatChip("❓", "${filtered.sumOf { it.mcqCount }}", "MCQs")
                Box(Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(BpscColors.Divider))
                CAStatChip("🔖", "${bookmarkedIds.size}", "Saved")  // ✅ VM
            }

            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(if (selectedTab == 3) "🔖" else "🔍", fontSize = 48.sp)
                        Text(
                            if (selectedTab == 3) "No saved articles yet" else "No articles found",
                            style = MaterialTheme.typography.titleLarge,
                            color = BpscColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (selectedTab == 3) "Bookmark articles to see them here" else "Try a different search or filter",
                            style = MaterialTheme.typography.bodyLarge,
                            color = BpscColors.TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    grouped.forEach { (date, articles) ->
                        stickyHeader(key = date) {
                            DateGroupHeader(date = date, count = articles.size)
                        }
                        itemsIndexed(articles, key = { _, a -> a.id }) { _, article ->
                            CAArticleCard(
                                article = article,
                                isBookmarked = bookmarkedIds.contains(article.id),   // ✅ VM
                                onBookmark = { bookmarkViewModel.toggle(article.id) }, // ✅ VM
                                onShare = { },
                                onReadMore = {
                                    selectedArticle = article
                                }            // ✅ bottom sheet
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        // ✅ Bottom sheet — opens when article selected
        selectedArticle?.let { article ->
            ArticleBottomSheet(
                article = article,
                isBookmarked = bookmarkedIds.contains(article.id),
                onBookmark = { bookmarkViewModel.toggle(article.id) },
                onDismiss = { selectedArticle = null }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// STICKY DATE HEADER
// ─────────────────────────────────────────────────────────────
@Composable
private fun DateGroupHeader(date: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BpscColors.Surface)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(BpscColors.PrimaryLight)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                date,
                style = MaterialTheme.typography.labelSmall,
                color = BpscColors.Primary,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Box(
            modifier = Modifier
                .height(1.dp)
                .weight(1f)
                .background(BpscColors.Divider)
        )
        Text(
            "$count articles",
            style = MaterialTheme.typography.labelSmall,
            color = BpscColors.TextHint
        )
    }
}

// ─────────────────────────────────────────────────────────────
// ARTICLE CARD
// ─────────────────────────────────────────────────────────────
@Composable
private fun CAArticleCard(
    article: CAArticle,
    isBookmarked: Boolean,
    onBookmark: () -> Unit,
    onShare: () -> Unit,
    onReadMore: () -> Unit,       // ← new param
) {
    val categoryColors = mapOf(
        "Economy" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)),
        "Polity" to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)),
        "International" to Pair(Color(0xFF3498DB), Color(0xFFE8F4FD)),
        "Science" to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)),
        "Education" to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)),
        "Sports" to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)),
        "Bihar GK" to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)),
    )
    val (catFg, catBg) = categoryColors[article.category] ?: Pair(
        BpscColors.Primary,
        BpscColors.PrimaryLight
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        article.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = catFg,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(catBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                    if (article.isImportant) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFF3CD))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("⭐", fontSize = 9.sp)
                            Text(
                                "Important",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF856404),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (article.isPrelims) Text(
                        "P",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF1ABC9C),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE8FDF8))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                    if (article.isMains) Text(
                        "M",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9B59B6),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF3E8FD))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Rounded.Schedule,
                        null,
                        tint = BpscColors.TextHint,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        "${article.readMinutes} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.TextHint,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Headline
            Text(
                article.headline,
                style = MaterialTheme.typography.titleMedium,
                color = BpscColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(6.dp))

            // Summary — always 3 lines, no inline expand
            Text(
                article.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = BpscColors.TextSecondary,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // ✅ Read more → opens bottom sheet
            Text(
                "Read more ↓",
                style = MaterialTheme.typography.labelSmall,
                color = BpscColors.Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable { onReadMore() }
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = BpscColors.Divider)
            Spacer(Modifier.height(10.dp))

            // Bottom row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BpscColors.PrimaryLight)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("❓", fontSize = 11.sp)
                    Text(
                        "${article.mcqCount} MCQs from this topic",
                        style = MaterialTheme.typography.labelSmall,
                        color = BpscColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BpscColors.Surface)
                            .clickable(onClick = onShare),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Share,
                            null,
                            tint = BpscColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isBookmarked) Color(0xFFFFF8E1) else BpscColors.Surface)
                            .clickable(onClick = onBookmark), contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            null,
                            tint = if (isBookmarked) BpscColors.CoinGold else BpscColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleBottomSheet(
    article: CAArticle,
    isBookmarked: Boolean,
    onBookmark: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val categoryColors = mapOf(
        "Economy" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)),
        "Polity" to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)),
        "International" to Pair(Color(0xFF3498DB), Color(0xFFE8F4FD)),
        "Science" to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)),
        "Education" to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)),
        "Sports" to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)),
        "Bihar GK" to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)),
    )
    val (catFg, catBg) = categoryColors[article.category] ?: Pair(
        BpscColors.Primary,
        BpscColors.PrimaryLight
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // ── Blue header ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0A2472), Color(0xFF1565C0)),
                            start = Offset(0f, 0f), end = Offset(400f, 200f)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Badges row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            article.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = catFg, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(catBg)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                        if (article.isImportant) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFF3CD))
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("⭐", fontSize = 9.sp)
                                Text(
                                    "Important",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF856404),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Schedule,
                                null,
                                tint = Color.White.copy(0.7f),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                "${article.readMinutes} min read",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(0.7f),
                                fontSize = 10.sp
                            )
                        }
                    }
                    // Headline
                    Text(
                        article.headline,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 26.sp
                    )
                    // Date + P/M tags
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            article.date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(0.6f)
                        )
                        if (article.isPrelims) Text(
                            "Prelims",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1ABC9C),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE8FDF8))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        if (article.isMains) Text(
                            "Mains",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9B59B6),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF3E8FD))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // ── Scrollable content ───────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Full article text
                Text(
                    article.fullContent,
                    style = MaterialTheme.typography.bodyLarge,
                    color = BpscColors.TextPrimary,
                    lineHeight = 26.sp
                )

                // Tags
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    article.tags.forEach { tag ->
                        Text(
                            "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = BpscColors.Primary, fontSize = 10.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BpscColors.PrimaryLight)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // MCQ practice banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BpscColors.PrimaryLight)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Practice MCQs",
                            style = MaterialTheme.typography.titleMedium,
                            color = BpscColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${article.mcqCount} questions from this topic",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.TextSecondary
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                    ) {
                        Text("Start", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // ── Bottom actions ───────────────────────────────
            HorizontalDivider(color = BpscColors.Divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBookmark,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isBookmarked) BpscColors.CoinGold else BpscColors.Divider
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isBookmarked) BpscColors.CoinGold else BpscColors.TextSecondary
                    )
                ) {
                    Icon(
                        if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        null, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isBookmarked) "Saved ✓" else "Save",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Button(
                    onClick = {},
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────
@Composable
private fun CAStatChip(icon: String, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(icon, fontSize = 13.sp)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = BpscColors.TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = BpscColors.TextHint,
            fontSize = 9.sp
        )
    }
}