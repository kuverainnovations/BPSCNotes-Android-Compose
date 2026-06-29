package com.example.bpscnotes.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.SearchResultArticle
import com.example.bpscnotes.data.remote.api.SearchResultCourse
import com.example.bpscnotes.data.remote.api.SearchResultQuiz
import com.example.bpscnotes.presentation.navigation.Routes.Screen

@Composable
fun GlobalSearchScreen(
    navController: NavHostController,
    viewModel: GlobalSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val cs = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(Modifier.fillMaxSize().background(BpscColors.Surface)) {
        // Search bar header
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape)
                        .background(Color.White.copy(0.15f))
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp)) }

                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.15f))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Search, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
                        BasicTextField(
                            value         = state.query,
                            onValueChange = viewModel::onQueryChange,
                            modifier      = Modifier.weight(1f).focusRequester(focusRequester),
                            singleLine    = true,
                            textStyle     = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            cursorBrush   = SolidColor(Color.White),
                            decorationBox = { inner ->
                                if (state.query.isEmpty()) {
                                    Text("Search quizzes, articles, courses…", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.5f))
                                }
                                inner()
                            }
                        )
                        if (state.query.isNotEmpty()) {
                            Icon(
                                Icons.Rounded.Close, null,
                                tint     = Color.White.copy(0.7f),
                                modifier = Modifier.size(16.dp).clickable { viewModel.onQueryChange("") }
                            )
                        }
                    }
                }
            }
        }

        val results = state.results
        when {
            state.query.isBlank() -> SearchEmptyHint()
            state.isLoading       -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BpscColors.Primary) }
            state.error != null   -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Search failed. Try again.", color = BpscColors.TextSecondary)
            }
            results == null || (results.quizzes.isEmpty() && results.articles.isEmpty() && results.courses.isEmpty()) -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔍", fontSize = 36.sp)
                        Text("No results for \"${state.query}\"", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                    }
                }
            }
            else -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                    // Quizzes section
                    if (results.quizzes.isNotEmpty()) {
                        item {
                            SearchSectionHeader("Quizzes", results.quizzes.size)
                        }
                        items(results.quizzes) { quiz ->
                            QuizResultItem(quiz) {
                                navController.navigate(Screen.QuizDetail.createRoute(quiz.id))
                            }
                        }
                    }
                    // Current Affairs section
                    if (results.articles.isNotEmpty()) {
                        item { SearchSectionHeader("Current Affairs", results.articles.size) }
                        items(results.articles) { article ->
                            ArticleResultItem(article) {
                                navController.navigate(Screen.CaArticleDetail.createRoute(article.id))
                            }
                        }
                    }
                    // Courses section
                    if (results.courses.isNotEmpty()) {
                        item { SearchSectionHeader("Courses", results.courses.size) }
                        items(results.courses) { course ->
                            CourseResultItem(course) {
                                navController.navigate(Screen.CourseDetail.createRoute(course.id))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEmptyHint() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔍", fontSize = 40.sp)
            Text("Search BPSC content", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
            Text("Find quizzes, current affairs, and courses", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
        }
    }
}

@Composable
private fun SearchSectionHeader(title: String, count: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(BpscColors.Surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
        Text("$count results", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.15f))
}

@Composable
private fun QuizResultItem(quiz: SearchResultQuiz, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(BpscColors.PrimaryLight),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Quiz, null, tint = BpscColors.Primary, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(quiz.title, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${quiz.totalQuestions} Qs · ${quiz.durationMins}m · ${quiz.subject}", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = BpscColors.TextHint, modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outline.copy(0.1f))
}

@Composable
private fun ArticleResultItem(article: SearchResultArticle, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFF3E0)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Article, null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(article.title.stripHtml(), style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val cleanSummary = article.summary?.stripHtml()
            if (!cleanSummary.isNullOrBlank()) {
                Text(cleanSummary, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = BpscColors.TextHint, modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outline.copy(0.1f))
}

@Composable
private fun CourseResultItem(course: SearchResultCourse, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.School, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(course.title, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(course.subject, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = BpscColors.TextHint, modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outline.copy(0.1f))
}

private fun String.stripHtml(): String =
    replace(Regex("<[^>]+>"), "").replace("&nbsp;", " ").replace("&amp;", "&")
        .replace("&lt;", "<").replace("&gt;", ">").trim()
