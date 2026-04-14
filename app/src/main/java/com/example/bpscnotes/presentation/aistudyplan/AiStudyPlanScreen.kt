package com.example.bpscnotes.presentation.aistudyplan

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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudyPlanScreen(
    navController: NavHostController,
    // Pass real data from your ProfileScreen / DashboardScreen
    polityProgress: Int = 82,
    historyProgress: Int = 65,
    geographyProgress: Int = 48,
    economyProgress: Int = 55,
    scienceProgress: Int = 60,
    viewModel: AiStudyPlanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val subjectProgress = remember {
        listOf(
            SubjectProgress("Polity",      polityProgress),
            SubjectProgress("History",     historyProgress),
            SubjectProgress("Geography",   geographyProgress),
            SubjectProgress("Economy",     economyProgress),
            SubjectProgress("Science",     scienceProgress),
            SubjectProgress("Bihar GK",    50),
            SubjectProgress("Current Affairs", 70)
        )
    }

    LaunchedEffect(Unit) {
        if (state.days.isEmpty() && !state.isLoading) {
            viewModel.generatePlan(subjectProgress)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Study Plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = BpscColors.TextPrimary)
                        Text("Personalised 7-day schedule", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = BpscColors.TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.generatePlan(subjectProgress) }) {
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
                state.isLoading -> PlanLoadingState(subjectProgress)
                state.error != null -> PlanErrorState(onRetry = { viewModel.generatePlan(subjectProgress) })
                state.days.isNotEmpty() -> PlanContent(state = state, viewModel = viewModel)
            }
        }
    }
}

// ── Loading ──────────────────────────────────────────────────

@Composable
private fun PlanLoadingState(subjects: List<SubjectProgress>) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF1A6FE8), Color(0xFF0D47A1)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Building Your Study Plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BpscColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("AI is analysing your weak areas\nand creating a personalised schedule...", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))

        // Show subject progress bars while loading
        subjects.forEach { sp ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(sp.subject, style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary, modifier = Modifier.width(90.dp))
                LinearProgressIndicator(
                    progress = { sp.progressPercent / 100f },
                    modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = when {
                        sp.progressPercent >= 75 -> Color(0xFF2ECC71)
                        sp.progressPercent >= 50 -> Color(0xFFF39C12)
                        else                     -> Color(0xFFE74C3C)
                    },
                    trackColor = BpscColors.Divider
                )
                Text("${sp.progressPercent}%", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, modifier = Modifier.width(32.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
            color = BpscColors.Primary,
            trackColor = BpscColors.Divider
        )
    }
}

@Composable
private fun PlanErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text("Could not generate plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = BpscColors.TextPrimary)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}

// ── Plan content ─────────────────────────────────────────────

@Composable
private fun PlanContent(
    state: StudyPlanUiState,
    viewModel: AiStudyPlanViewModel
) {
    val totalTasks    = state.days.sumOf { it.tasks.size }
    val doneTasks     = state.completedTasks.size

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary card
        item {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1565C0))))
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("7-Day Plan", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("AI personalised for your weak areas", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$doneTasks/$totalTasks", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("tasks done", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.8f))
                }
            }
        }

        // Day cards
        itemsIndexed(state.days) { dayIndex, day ->
            DayCard(
                day = day,
                dayIndex = dayIndex,
                viewModel = viewModel,
                state = state
            )
        }
    }
}

@Composable
private fun DayCard(
    day: StudyDay,
    dayIndex: Int,
    viewModel: AiStudyPlanViewModel,
    state: StudyPlanUiState
) {
    var expanded by remember { mutableStateOf(dayIndex == 0) }
    val doneTasks = day.tasks.indices.count { viewModel.isTaskCompleted(dayIndex, it) }
    val allDone   = doneTasks == day.tasks.size

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (allDone) Color(0xFFE8F5E9) else BpscColors.CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Day header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (allDone) Color(0xFF2ECC71) else BpscColors.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (allDone) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("${dayIndex + 1}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(day.day, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BpscColors.TextPrimary)
                    Text(day.focus, style = MaterialTheme.typography.bodySmall, color = BpscColors.TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${day.totalMinutes / 60}h ${day.totalMinutes % 60}m", style = MaterialTheme.typography.labelSmall, color = BpscColors.Primary, fontWeight = FontWeight.Bold)
                    Text("$doneTasks/${day.tasks.size} done", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                }
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    null, tint = BpscColors.TextHint, modifier = Modifier.size(20.dp)
                )
            }

            // Tasks
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = BpscColors.Divider)
                    Spacer(Modifier.height(2.dp))

                    day.tasks.forEachIndexed { taskIndex, task ->
                        val isCompleted = viewModel.isTaskCompleted(dayIndex, taskIndex)
                        TaskRow(
                            task = task,
                            isCompleted = isCompleted,
                            onToggle = { viewModel.toggleTask(dayIndex, taskIndex) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: StudyTask,
    isCompleted: Boolean,
    onToggle: () -> Unit
) {
    val priorityColor = when (task.priority) {
        "High"   -> Color(0xFFE74C3C)
        "Medium" -> Color(0xFFF39C12)
        else     -> Color(0xFF2ECC71)
    }

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (isCompleted) Color(0xFFE8F5E9) else BpscColors.Surface)
            .clickable(onClick = onToggle)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Checkbox
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape)
                .background(if (isCompleted) Color(0xFF2ECC71) else BpscColors.Divider)
                .border(if (isCompleted) 0.dp else 1.5.dp, BpscColors.TextHint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    task.topic,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) BpscColors.TextSecondary else BpscColors.TextPrimary,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                )
            }
            Text(task.subject, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
            if (!isCompleted) {
                Text("💡 ${task.tip}", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, lineHeight = 16.sp)
            }
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${task.durationMinutes}m",
                style = MaterialTheme.typography.labelSmall,
                color = BpscColors.TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(priorityColor.copy(0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(task.priority, style = MaterialTheme.typography.labelSmall, color = priorityColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}
