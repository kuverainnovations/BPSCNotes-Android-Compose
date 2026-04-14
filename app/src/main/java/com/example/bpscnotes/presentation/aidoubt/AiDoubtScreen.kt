package com.example.bpscnotes.presentation.aidoubt

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.domain.repository.ChatMessage
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import kotlinx.coroutines.launch

enum class AiTab { Home, Doubt }

@Composable
fun AiDoubtScreen(
    navController: NavHostController,
    subject: String = "",
    viewModel: AiDoubtViewModel = hiltViewModel()
) {
    val uiState   by viewModel.uiState.collectAsState()
    val listState  = rememberLazyListState()
    val scope      = rememberCoroutineScope()
    var activeTab  by remember { mutableStateOf(AiTab.Home) }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty())
            listState.animateScrollToItem(uiState.messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {

        // Gradient header — matches your dashboard style exactly
        AiHubHeader(
            navController = navController,
            uiState       = uiState,
            activeTab     = activeTab,
            onTabChange   = { activeTab = it }
        )

        AnimatedContent(
            targetState  = activeTab,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label        = "tabContent"
        ) { tab ->
            when (tab) {
                AiTab.Home  -> AiHomeTab(navController = navController, subject = subject)
                AiTab.Doubt -> AiDoubtTab(
                    uiState   = uiState,
                    listState = listState,
                    subject   = subject,
                    viewModel = viewModel,
                    onSend    = {
                        viewModel.sendMessage()
                        scope.launch { listState.animateScrollToItem(uiState.messages.size) }
                    }
                )
            }
        }
    }
}

// ── Header — same blue gradient as your dashboard ────────────

@Composable
private fun AiHubHeader(
    navController: NavHostController,
    uiState: AiDoubtUiState,
    activeTab: AiTab,
    onTabChange: (AiTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF051D56),
                        Color(0xFF0A2472),
                        Color(0xFF0D47A1),
                        Color(0xFF1565C0),
                        Color(0xFF1976D2),
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(600f, 500f)
                )
            )
            .statusBarsPadding()
    ) {
        // Same dot grid + blobs as dashboard
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(0.05f), 180.dp.toPx(), Offset(size.width + 40.dp.toPx(), -60.dp.toPx()))
            drawCircle(Color.White.copy(0.04f), 100.dp.toPx(), Offset(size.width - 10.dp.toPx(), size.height * 0.5f))
            drawCircle(Color.White.copy(0.06f), 60.dp.toPx(),  Offset(-20.dp.toPx(), size.height * 0.8f))
            val sp = 28.dp.toPx()
            var x = sp
            while (x < size.width) {
                var y = sp
                while (y < size.height) {
                    drawCircle(Color.White.copy(0.04f), 1.dp.toPx(), Offset(x, y))
                    y += sp
                }
                x += sp
            }
        }

        // Shiny accent line — same as dashboard
        Box(
            modifier = Modifier.fillMaxWidth().height(2.dp)
                .background(
                    Brush.horizontalGradient(listOf(
                        Color.Transparent, Color.White.copy(0.3f),
                        Color.White.copy(0.7f), Color.White.copy(0.3f), Color.Transparent
                    ))
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 0.dp)
        ) {
            // Top bar
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Back — same hamburger style as dashboard
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.12f))
                        .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(12.dp))
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }

                // Title pill — same as "🔥 BPSCNotes" logo style
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(0.1f))
                        .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text("🤖", fontSize = 14.sp)
                    Text(
                        "AI Tutor",
                        style         = MaterialTheme.typography.titleMedium,
                        color         = Color.White,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Pro/Upgrade pill — same as coins pill style
                if (!uiState.isPro) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFFFFB300).copy(0.15f))
                            .border(0.5.dp, Color(0xFFFFD54F).copy(0.5f), RoundedCornerShape(22.dp))
                            .clickable { navController.navigate(Screen.Subscription.route) }
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("✦", fontSize = 11.sp, color = BpscColors.CoinGold)
                        Text("Pro", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF2ECC71).copy(0.2f))
                            .border(0.5.dp, Color(0xFF2ECC71).copy(0.4f), RoundedCornerShape(22.dp))
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("✦", fontSize = 11.sp, color = Color(0xFF2ECC71))
                        Text("Pro", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2ECC71), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // Hero row — same layout as greeting + ring in dashboard
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(BpscColors.Success))
                        Text("Powered by Claude AI", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                    }
                    Text(
                        "AI Tutor 🤖",
                        style      = MaterialTheme.typography.headlineMedium,
                        color      = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 32.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(0.12f))
                            .border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(22.dp))
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Text(
                            if (uiState.isPro) "Unlimited · Pro"
                            else "${uiState.dailyLimit - uiState.dailyQuestionsUsed} doubts left today",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = Color.White.copy(0.9f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // AI tools ring — same style as progress ring
                Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 7.dp.toPx()
                        val inset  = stroke / 2
                        val arcSz  = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                        drawArc(
                            color = Color.White.copy(0.14f), startAngle = -90f, sweepAngle = 360f, useCenter = false,
                            topLeft = Offset(inset, inset), size = arcSz,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(listOf(Color(0xFF64B5F6), Color(0xFFE3F2FD), Color.White)),
                            startAngle = -90f, sweepAngle = 360f, useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                            topLeft = Offset(inset, inset), size = arcSz
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("4", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Text("AI tools", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Stats strip — same glass card style as dashboard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Brush.verticalGradient(listOf(Color.White.copy(0.16f), Color.White.copy(0.08f))))
                    .border(
                        0.5.dp,
                        Brush.verticalGradient(listOf(Color.White.copy(0.35f), Color.Transparent)),
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                AiStatTab(icon = "🤖", label = "AI Features",  selected = activeTab == AiTab.Home,  onClick = { onTabChange(AiTab.Home) })
                Box(modifier = Modifier.width(0.5.dp).height(36.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(0.28f), Color.Transparent))))
                AiStatTab(icon = "💬", label = "Doubt Solver", selected = activeTab == AiTab.Doubt, onClick = { onTabChange(AiTab.Doubt) })
            }
        }
    }
}

@Composable
private fun AiStatTab(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier            = Modifier.clickable(onClick = onClick).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(icon, fontSize = 18.sp)
        Text(
            label,
            style      = MaterialTheme.typography.labelSmall,
            color      = if (selected) Color.White else Color.White.copy(0.5f),
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
            fontSize   = 11.sp
        )
        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (selected) Color.White else Color.Transparent))
    }
}

// ═══════════════════════════════════════════════════════════
// TAB 1 — AI Features
// ═══════════════════════════════════════════════════════════

@Composable
private fun AiHomeTab(navController: NavHostController, subject: String) {
    LazyColumn(
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AiFeatureCard(
                icon       = Icons.Rounded.Chat,
                iconBg     = Brush.linearGradient(listOf(Color(0xFF7C4DFF), Color(0xFF5C4DFF))),
                title      = "AI Doubt Solver",
                subtitle   = "Ask any BPSC question in Hindi or English — get instant answers",
                tag        = "Most used",
                tagColor   = Color(0xFF7C4DFF),
                highlights = listOf("Instant answers", "Hindi & English", "Exam-focused"),
                onClick    = {}   // parent switches tab on tap of Doubt Solver tab above
            )
        }
        item {
            AiFeatureCard(
                icon       = Icons.Rounded.Quiz,
                iconBg     = Brush.linearGradient(listOf(Color(0xFF1565C0), Color(0xFF1976D2))),
                title      = "AI MCQ Generator",
                subtitle   = "5 fresh BPSC-style practice questions for any topic, never repeats",
                tag        = "New",
                tagColor   = BpscColors.Success,
                highlights = listOf("5 AI questions", "Never repeats", "With explanation"),
                onClick    = { navController.navigate(Screen.AiMcq.createRoute(subject = subject.ifEmpty { "General" })) }
            )
        }
        item {
            AiFeatureCard(
                icon       = Icons.Rounded.Newspaper,
                iconBg     = Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1976D2))),
                title      = "Current Affairs Summary",
                subtitle   = "Today's news analysed for BPSC relevance with likely exam questions",
                tag        = "Daily",
                tagColor   = BpscColors.Accent,
                highlights = listOf("BPSC-focused", "Likely questions", "Key insights"),
                onClick    = { navController.navigate(Screen.AiCaSummary.route) }
            )
        }
        item {
            AiFeatureCard(
                icon       = Icons.Rounded.CalendarMonth,
                iconBg     = Brush.linearGradient(listOf(Color(0xFF00695C), Color(0xFF00897B))),
                title      = "AI Study Plan",
                subtitle   = "Personalised 7-day schedule based on your weak areas",
                tag        = "Smart",
                tagColor   = Color(0xFF00695C),
                highlights = listOf("Weak area focus", "7-day plan", "Checkable tasks"),
                onClick    = { navController.navigate(Screen.AiStudyPlan.route) }
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFF8E1)).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.Top
            ) {
                Icon(Icons.Rounded.Lightbulb, null, tint = BpscColors.Warning, modifier = Modifier.size(18.dp))
                Text("Tap 💬 Doubt Solver tab above to chat with your AI tutor.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D4037), lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun AiFeatureCard(
    icon: ImageVector,
    iconBg: Brush,
    title: String,
    subtitle: String,
    tag: String,
    tagColor: Color,
    highlights: List<String>,
    onClick: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = BpscColors.CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(15.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BpscColors.TextPrimary)
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(tagColor.copy(0.1f)).padding(horizontal = 7.dp, vertical = 2.dp)) {
                        Text(tag, style = MaterialTheme.typography.labelSmall, color = tagColor, fontWeight = FontWeight.Bold)
                    }
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary, lineHeight = 18.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    highlights.forEach { h ->
                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(BpscColors.Surface).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(h, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
                        }
                    }
                }
            }
            Icon(Icons.Rounded.ArrowForwardIos, null, tint = BpscColors.TextHint, modifier = Modifier.size(14.dp).padding(top = 4.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════
// TAB 2 — Doubt Chat
// ═══════════════════════════════════════════════════════════

@Composable
private fun AiDoubtTab(
    uiState: AiDoubtUiState,
    listState: LazyListState,
    subject: String,
    viewModel: AiDoubtViewModel,
    onSend: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.messages.size <= 1) {
            SuggestedQuestionsRow(subject = subject, onSuggestionClick = { viewModel.onInputChanged(it) })
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.messages, key = { "${it.role}_${it.content.hashCode()}_${it.isLoading}" }) { message ->
                ChatBubble(message = message)
            }
        }

        // Full error display
        AnimatedVisibility(visible = uiState.errorMessage != null) {
            uiState.errorMessage?.let { error ->
                Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFEDED)).padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.ErrorOutline, null, tint = Color(0xFFC0392B), modifier = Modifier.size(18.dp))
                        Text("Error", style = MaterialTheme.typography.labelMedium, color = Color(0xFFC0392B), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearError() }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Rounded.Close, null, tint = Color(0xFFC0392B), modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(error, style = MaterialTheme.typography.bodySmall, color = Color(0xFFC0392B), lineHeight = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    val hint = when {
                        error.contains("401") || error.contains("api_key", true) || error.contains("auth", true) ->
                            "Fix: Add your API key in ClaudeModule.kt → CLAUDE_API_KEY"
                        error.contains("429") || error.contains("rate_limit", true) ->
                            "Fix: Too many requests. Wait 1 minute and try again."
                        error.contains("UnknownHost") || error.contains("connect", true) ->
                            "Fix: Check your internet connection."
                        error.contains("500") || error.contains("overloaded", true) ->
                            "Fix: Anthropic servers busy. Try again in a moment."
                        else -> "Fix: Check your API key at console.anthropic.com"
                    }
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFFFF0F0)).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.Lightbulb, null, tint = Color(0xFFE67E22), modifier = Modifier.size(14.dp))
                        Text(hint, style = MaterialTheme.typography.labelSmall, color = Color(0xFF7D3C00), lineHeight = 16.sp)
                    }
                }
            }
        }

        // Limit banner
        AnimatedVisibility(visible = !uiState.isPro && uiState.dailyQuestionsUsed >= uiState.dailyLimit) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF8E1)).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Lock, null, tint = BpscColors.Warning, modifier = Modifier.size(18.dp))
                Text("Daily limit reached.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D4037), modifier = Modifier.weight(1f))
                TextButton(onClick = { }) {
                    Text("Upgrade", color = BpscColors.Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        ChatInputBar(
            text = uiState.inputText,
            isLoading = uiState.isLoading,
            isLimitReached = !uiState.isPro && uiState.dailyQuestionsUsed >= uiState.dailyLimit,
            onTextChange = viewModel::onInputChanged,
            onSend = onSend
        )
    }
}

@Composable
private fun SuggestedQuestionsRow(subject: String, onSuggestionClick: (String) -> Unit) {
    val suggestions = when (subject.lowercase()) {
        "polity"    -> listOf("Explain Fundamental Rights simply", "What is DPSP?", "Difference between Rajya Sabha and Lok Sabha")
        "history"   -> listOf("Who were the Mauryan rulers?", "What was the Quit India Movement?")
        "geography" -> listOf("Major rivers of Bihar", "Explain monsoon system")
        else        -> listOf("Important Bihar GK for BPSC", "Explain Panchayati Raj system", "What is the BPSC exam pattern?")
    }
    Column(modifier = Modifier.background(BpscColors.CardBg).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text("Try asking:", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, modifier = Modifier.padding(bottom = 8.dp))
        suggestions.forEach { s ->
            SuggestionChip(
                onClick  = { onSuggestionClick(s) },
                label    = { Text(s, style = MaterialTheme.typography.bodySmall, color = BpscColors.Primary) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                border   = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = BpscColors.Primary.copy(0.25f)),
                colors   = SuggestionChipDefaults.suggestionChipColors(containerColor = BpscColors.PrimaryLight)
            )
        }
    }
    HorizontalDivider(color = BpscColors.Divider)
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1976D2)))),
                contentAlignment = Alignment.Center
            ) { Text("AI", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            shape = RoundedCornerShape(topStart = if (isUser) 16.dp else 4.dp, topEnd = if (isUser) 4.dp else 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = if (isUser) BpscColors.Primary else BpscColors.CardBg,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (message.isLoading) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(3) { index ->
                        val t = rememberInfiniteTransition(label = "d$index")
                        val a by t.animateFloat(0.2f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse, StartOffset(index * 160)), label = "a$index")
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BpscColors.Primary.copy(alpha = a)))
                    }
                }
            } else {
                Text(message.content, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyMedium, color = if (isUser) Color.White else BpscColors.TextPrimary, lineHeight = 22.sp)
            }
        }
        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(BpscColors.Accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun ChatInputBar(text: String, isLoading: Boolean, isLimitReached: Boolean, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(color = BpscColors.CardBg, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).navigationBarsPadding().imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text, onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (isLimitReached) "Daily limit reached. Upgrade to Pro!" else "Ask your BPSC doubt...", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint) },
                enabled = !isLoading && !isLimitReached, maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BpscColors.Primary, unfocusedBorderColor = BpscColors.Divider, focusedTextColor = BpscColors.TextPrimary, unfocusedTextColor = BpscColors.TextPrimary)
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onSend,
                enabled = text.isNotBlank() && !isLoading && !isLimitReached,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = BpscColors.Primary, disabledContainerColor = BpscColors.Divider)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(Icons.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}