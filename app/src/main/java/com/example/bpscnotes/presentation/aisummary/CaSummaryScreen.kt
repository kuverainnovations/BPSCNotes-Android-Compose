package com.example.bpscnotes.presentation.aisummary

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import com.example.bpscnotes.core.ui.t.BpscColors

// Pass in headlines from your existing CurrentAffairsScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaSummaryScreen(
    navController: NavHostController,
    headlines: List<String>,
    date: String = "Today",
    viewModel: CaSummaryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(headlines) {
        if (headlines.isNotEmpty() && state.points.isEmpty() && !state.isLoading) {
            viewModel.summarise(headlines, date)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI CA Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = BpscColors.TextPrimary)
                        Text("BPSC-focused · $date", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = BpscColors.TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.retry(headlines, date, state.category) }) {
                        Icon(Icons.Rounded.Refresh, null, tint = BpscColors.Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BpscColors.CardBg)
            )
        },
        containerColor = BpscColors.Surface
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CaLoadingState()
                state.error != null -> CaErrorState(onRetry = { viewModel.retry(headlines, date, state.category) })
                state.points.isNotEmpty() -> CaSummaryList(points = state.points)
                else -> CaEmptyState()
            }
        }
    }
}

@Composable
private fun CaLoadingState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF1A6FE8), Color(0xFF0D47A1)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Newspaper, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Analysing Current Affairs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BpscColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("AI is finding BPSC-relevant insights\nfrom today's news...", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = BpscColors.Primary,
            trackColor = BpscColors.Divider
        )
    }
}

@Composable
private fun CaErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text("Could not load summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BpscColors.TextPrimary)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}

@Composable
private fun CaEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No headlines to summarise", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextSecondary)
    }
}

@Composable
private fun CaSummaryList(points: List<CaSummaryPoint>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Header banner
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1976D2))))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Column {
                    Text("AI-Powered BPSC Summary", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${points.size} news items analysed for exam relevance", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.8f))
                }
            }
        }

        items(points) { point ->
            CaSummaryCard(point = point)
        }
    }
}

@Composable
private fun CaSummaryCard(point: CaSummaryPoint) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BpscColors.CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Headline row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape)
                        .background(BpscColors.Primary)
                        .padding(top = 6.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(point.headline, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BpscColors.TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(point.summary, style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary, lineHeight = 18.sp)
                }
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    null, tint = BpscColors.TextHint, modifier = Modifier.size(20.dp)
                )
            }

            // Expanded section
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    HorizontalDivider(color = BpscColors.Divider)

                    // BPSC relevance
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(BpscColors.PrimaryLight)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.School, null, tint = BpscColors.Primary, modifier = Modifier.size(16.dp))
                        Text(point.bpscRelevance, style = MaterialTheme.typography.bodySmall, color = BpscColors.Primary, lineHeight = 18.sp)
                    }

                    // Likely question
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFF8E1))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Quiz, null, tint = BpscColors.Warning, modifier = Modifier.size(16.dp))
                        Column {
                            Text("Likely BPSC Question:", style = MaterialTheme.typography.labelSmall, color = BpscColors.Warning, fontWeight = FontWeight.Bold)
                            Text(point.likelyQuestion, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D4037), lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}
