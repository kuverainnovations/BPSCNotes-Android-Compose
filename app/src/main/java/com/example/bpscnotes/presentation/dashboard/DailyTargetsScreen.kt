package com.example.bpscnotes.presentation.dashboard

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.DailyTargetDto
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
// UI-only model: enriches DailyTargetDto with display metadata
// Mapped from DailyTargetDto — no MockData involved.
// ─────────────────────────────────────────────────────────────

data class TargetItem(
    val target: DailyTargetDto,
    val difficulty: Difficulty,   // derived from target.difficulty string
    val timeSlot: TimeSlot,       // derived from target.timeSlot string
)

enum class Difficulty(val label: String, val color: Color) {
    Easy  ("Easy",   Color(0xFF2ECC71)),
    Medium("Medium", Color(0xFFF39C12)),
    Hard  ("Hard",   Color(0xFFE74C3C))
}

enum class TimeSlot(val label: String, val icon: String, val range: String) {
    Morning  ("Morning",   "🌅", "6AM – 12PM"),
    Afternoon("Afternoon", "☀️", "12PM – 6PM"),
    Night    ("Night",     "🌙", "6PM – 10PM")
}

/** Map a [DailyTargetDto] to the UI [TargetItem] — no static data */
private fun DailyTargetDto.toTargetItem() = TargetItem(
    target     = this,
    difficulty = when (difficulty.lowercase()) {
        "easy" -> Difficulty.Easy
        "hard" -> Difficulty.Hard
        else   -> Difficulty.Medium
    },
    timeSlot = when (timeSlot.lowercase()) {
        "afternoon" -> TimeSlot.Afternoon
        "night"     -> TimeSlot.Night
        else        -> TimeSlot.Morning
    }
)

// ─────────────────────────────────────────────────────────────
// SCREEN  — reads from DashboardViewModel (already injected)
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DailyTargetsScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel = hiltViewModel()   // same VM as Dashboard — targets already loaded
) {
    val state by viewModel.uiState.collectAsState()

    // Map API DTOs → UI TargetItems once
    val allTargets = remember(state.dailyTargets) {
        state.dailyTargets.map { it.toTargetItem() }
    }

    // Local completion state (mirrors API; in production you'd call PATCH /users/daily-targets/{id})


    val completed = allTargets.count { it.target.isCompleted }
    val total     = allTargets.size
    val progress by animateFloatAsState(
        targetValue   = if (total > 0) completed.toFloat() / total else 0f,
        animationSpec = tween(800),
        label         = "prog"
    )

    var selectedTab    by remember { mutableIntStateOf(0) }
    var selectedFilter by remember { mutableStateOf("All") }
    var showAddSheet   by remember { mutableStateOf(false) }

    val tabs    = listOf("List", "Cards", "Timeline")
    val filters = listOf("All") + allTargets.map { it.target.subject }.distinct()

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0D47A1), Color(0xFF1565C0))))
                    .statusBarsPadding()
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconButton(
                                onClick  = { navController.popBackStack() },
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f))
                            ) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Daily Targets", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    "Today — ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())}",
                                    style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.15f)).clickable { showAddSheet = true }.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Add", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    when {
                        // Loading state
                        state.isLoading && allTargets.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text("Loading targets…", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                                }
                            }
                        }
                        // Error state
                        state.error != null && allTargets.isEmpty() -> {
                            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.1f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("⚠️", fontSize = 14.sp)
                                Text("Failed to load targets", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.9f), modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.refresh() }) { Text("Retry", color = Color.White) }
                            }
                        }
                        else -> {
                            // Progress bar
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("$completed/$total Completed", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(0.2f))) {
                                        Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF64B5F6), Color.White)), RoundedCornerShape(4.dp)))
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🪙", fontSize = 20.sp)
                                    Text("+${total - completed}", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.Bold)
                                    Text("coins", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Tabs
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tabs.forEachIndexed { index, tab ->
                            val sel = selectedTab == index
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (sel) Color.White else Color.White.copy(0.12f)).clickable { selectedTab = index }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(tab, style = MaterialTheme.typography.bodyMedium, color = if (sel) BpscColors.Primary else Color.White.copy(0.8f), fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }

            // ── Streak warning ──────────────────────────────────
            AnimatedVisibility(visible = completed == 0 && total > 0) {
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF3CD)).padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("⚠️", fontSize = 16.sp)
                    Column {
                        Text("Streak at risk!", style = MaterialTheme.typography.titleMedium, color = Color(0xFF856404), fontWeight = FontWeight.Bold)
                        Text("Complete at least 1 topic to protect your streak", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF856404))
                    }
                }
            }

            // ── Empty state when loaded but no targets ──────────
            if (!state.isLoading && allTargets.isEmpty() && state.error == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🎯", fontSize = 56.sp)
                        Text("No targets for today", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Tap + Add to create your daily plan", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                        Button(onClick = { showAddSheet = true }, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Create Target")
                        }
                    }
                }
            } else {
                // ── Tab content ─────────────────────────────────
                when (selectedTab) {
                    0 -> ListTabContent(
                        items            = allTargets,
                        filters          = filters,
                        selectedFilter   = selectedFilter,
                        onFilterChange   = { selectedFilter = it },
                        onToggleComplete = { id ->  viewModel.toggleTargetComplete(id)/*if (completedIds.contains(id)) completedIds.remove(id) else completedIds.add(id)*/ },
                        onStartQuiz      = { subjectId -> navController.navigate(Screen.DailyQuiz.createRoute(subjectId)) },
                        onViewNotes      = { navController.navigate(Screen.ELibrary.route) }
                    )
                    1 -> CardsTabContent(
                        items            = allTargets,
                        onToggleComplete = { id ->  viewModel.toggleTargetComplete(id)/*if (completedIds.contains(id)) completedIds.remove(id) else completedIds.add(id)*/ },
                        onStartQuiz      = { subjectId -> navController.navigate(Screen.DailyQuiz.createRoute(subjectId)) },
                        onViewNotes      = { navController.navigate(Screen.ELibrary.route) }
                    )
                    2 -> TimelineTabContent(
                        items            = allTargets,
                        onToggleComplete = { id ->     viewModel.toggleTargetComplete(id)
                            /*if (completedIds.contains(id)) completedIds.remove(id) else completedIds.add(id)*/ },
                        onStartQuiz      = { subjectId -> navController.navigate(Screen.DailyQuiz.createRoute(subjectId)) },
                        onViewNotes      = { navController.navigate(Screen.ELibrary.route) }
                    )
                }
            }
        }

        if (showAddSheet) {
            if (state.dailyTargets.size >= 10) {
                Toast.makeText(LocalContext.current, "Max 10 targets allowed", Toast.LENGTH_SHORT).show()
                return
            }
            CreateTargetSheet(viewModel = viewModel, onDismiss = { showAddSheet = false })
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TAB 1 — LIST VIEW
// ─────────────────────────────────────────────────────────────

@Composable
private fun ListTabContent(
    items: List<TargetItem>,
    filters: List<String>,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    onToggleComplete: (String) -> Unit,
    onStartQuiz: (String) -> Unit,
    onViewNotes: () -> Unit
) {
    val filtered = if (selectedFilter == "All") items else items.filter { it.target.subject == selectedFilter }
    val carried  = filtered.filter { it.target.isCarriedForward }
    val today    = filtered.filter { !it.target.isCarriedForward }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { filter ->
                val sel = filter == selectedFilter
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (sel) BpscColors.Primary else Color.White).border(1.dp, if (sel) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(20.dp)).clickable { onFilterChange(filter) }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                    Text(filter, style = MaterialTheme.typography.bodyMedium, color = if (sel) Color.White else BpscColors.TextSecondary, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (carried.isNotEmpty()) {
                item { SectionLabel("📅", "Carried Forward", "From yesterday") }
                items(carried, key = { it.target.id }) { item ->
                    TargetListCard(item = item, isCompleted = item.target.isCompleted, onToggleComplete = { onToggleComplete(item.target.id) }, onStartQuiz = { onStartQuiz(item.target.id) }, onViewNotes = onViewNotes)
                }
                item { Spacer(Modifier.height(4.dp)) }
            }
            item { SectionLabel("🎯", "Today's Targets", "${today.size} topics assigned") }
            items(today, key = { it.target.id }) { item ->
                TargetListCard(item = item, isCompleted = item.target.isCompleted, onToggleComplete = { onToggleComplete(item.target.id) }, onStartQuiz = { onStartQuiz(item.target.id) }, onViewNotes = onViewNotes)
            }
        }
    }
}

@Composable
private fun SectionLabel(icon: String, title: String, subtitle: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(icon, fontSize = 16.sp)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
        }
    }
}

@Composable
private fun TargetListCard(item: TargetItem, isCompleted: Boolean, onToggleComplete: () -> Unit, onStartQuiz: () -> Unit, onViewNotes: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (isCompleted) Color(0xFFF0FBF5) else Color.White), elevation = CardDefaults.cardElevation(if (expanded) 4.dp else 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(if (isCompleted) BpscColors.Success else Color.Transparent).border(2.dp, if (isCompleted) BpscColors.Success else BpscColors.TextHint, CircleShape)
                .clickable(
                        indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onToggleComplete()
                }, contentAlignment = Alignment.Center) {
                    if (isCompleted) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.target.title, style = MaterialTheme.typography.titleMedium, color = if (isCompleted) BpscColors.TextSecondary else BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = if (expanded) Int.MAX_VALUE else 1, overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis, textDecoration = if (isCompleted) TextDecoration.LineThrough else null)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        SubjectTag(item.target.subject)
                        DifficultyBadge(item.difficulty)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Rounded.Schedule, null, tint = BpscColors.TextHint, modifier = Modifier.size(11.dp))
                            Text("${item.target.estimatedMinutes}m", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                        }
                        if (item.target.isCarriedForward) {
                            Text("Carried", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE67E22), modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFF0EA)).padding(horizontal = 5.dp, vertical = 2.dp), fontSize = 9.sp)
                        }
                    }
                }
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = BpscColors.TextHint, modifier = Modifier.size(20.dp).clickable { expanded = !expanded })
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = BpscColors.Divider)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(Icons.Rounded.Quiz,     "Start Quiz", BpscColors.PrimaryLight,  BpscColors.Primary,     Modifier.weight(1f), onStartQuiz)
                        ActionButton(Icons.Rounded.MenuBook, "View Notes", Color(0xFFF3E8FD),        Color(0xFF9B59B6),      Modifier.weight(1f), onViewNotes)
                        ActionButton(Icons.Rounded.Bookmark, "Bookmark",   Color(0xFFFFF8E1),        BpscColors.CoinGold,    Modifier.weight(1f)) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, bg: Color, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(bg).clickable(onClick = onClick).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
    }
}

@Composable
private fun SubjectTag(subject: String) {
    val colors = mapOf("Polity" to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)), "History" to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)), "Geography" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)), "Economy" to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)), "Science" to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)), "Bihar GK" to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)))
    val (fg, bg) = colors[subject] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)
    Text(subject, style = MaterialTheme.typography.labelSmall, color = fg, fontSize = 9.sp, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 6.dp, vertical = 2.dp))
}

@Composable
private fun DifficultyBadge(difficulty: Difficulty) {
    Text(difficulty.label, style = MaterialTheme.typography.labelSmall, color = difficulty.color, fontSize = 9.sp, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(difficulty.color.copy(0.1f)).padding(horizontal = 6.dp, vertical = 2.dp))
}

// ─────────────────────────────────────────────────────────────
// TAB 2 — CARD SWIPE VIEW
// ─────────────────────────────────────────────────────────────

@Composable
private fun CardsTabContent(items: List<TargetItem>, onToggleComplete: (String) -> Unit, onStartQuiz: (String) -> Unit, onViewNotes: () -> Unit) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val scope        = rememberCoroutineScope()
    val current      = items.getOrNull(currentIndex)
    val total        = items.size

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEachIndexed { i, item ->
                val done = item.target.isCompleted
                Box(modifier = Modifier.size(if (i == currentIndex) 10.dp else 8.dp).clip(CircleShape).background(when { i == currentIndex -> BpscColors.Primary; done -> BpscColors.Success; else -> BpscColors.Divider }))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("${currentIndex + 1} of $total", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
        Spacer(Modifier.height(16.dp))

        if (current != null) {
            val isCompleted = current.target.isCompleted
            val offsetX     = remember { Animatable(0f) }
            Card(
                modifier = Modifier.fillMaxWidth().wrapContentHeight().offset(x = offsetX.value.dp).shadow(12.dp, RoundedCornerShape(24.dp))
                    .pointerInput(currentIndex) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    when {
                                       offsetX.value > 100f  -> {
                                        offsetX.animateTo(400f, tween(200))
                                        if (currentIndex < items.size - 1) currentIndex++
                                        offsetX.snapTo(0f)
                                    }
                                        offsetX.value < -100f -> { offsetX.animateTo(-400f, tween(200)); if (currentIndex < items.size - 1) currentIndex++; offsetX.snapTo(0f) }
                                        else                  -> offsetX.animateTo(0f, spring())
                                    }
                                }
                            },
                            onHorizontalDrag = { _, d -> scope.launch { offsetX.snapTo(offsetX.value + d * 0.3f) } }
                        )
                    },
                shape  = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SubjectTag(current.target.subject); DifficultyBadge(current.difficulty)
                        Spacer(Modifier.weight(1f))
                        if (current.target.isCarriedForward) Text("📅 Carried Forward", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE67E22), fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(current.target.title, style = MaterialTheme.typography.headlineSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BpscColors.Surface).padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        CardStat("⏱️", "${current.target.estimatedMinutes} min",   "Est. Time")
                        CardStat("📝", "${current.target.totalQuestions}Q",         "Questions")
                        CardStat("🕐", current.timeSlot.range,                      current.timeSlot.label)
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { onStartQuiz(current.target.id) }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                            Icon(Icons.Rounded.Quiz, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Quiz")
                        }
                        OutlinedButton(onClick = onViewNotes, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, BpscColors.Primary), colors = ButtonDefaults.outlinedButtonColors(contentColor = BpscColors.Primary)) {
                            Icon(Icons.Rounded.MenuBook, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Notes")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Icon(Icons.Rounded.KeyboardArrowLeft, null, tint = BpscColors.TextHint, modifier = Modifier.size(16.dp)); Text("Skip", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint) }
                        Text("Swipe to navigate", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text("Done", style = MaterialTheme.typography.bodyMedium, color = BpscColors.Success); Icon(Icons.Rounded.KeyboardArrowRight, null, tint = BpscColors.Success, modifier = Modifier.size(16.dp)) }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = { onToggleComplete(current.target.id); if (currentIndex < items.size - 1) currentIndex++ }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isCompleted) BpscColors.Success else BpscColors.Primary)) {
                Icon(if (isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.Check, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (isCompleted) "Completed ✓" else "Mark as Done")
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🎉", fontSize = 60.sp)
                Text("All Topics Done!", style = MaterialTheme.typography.headlineSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                Text("You've completed today's targets.\nYour streak is safe!", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun CardStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(icon, fontSize = 14.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, fontSize = 9.sp)
    }
}

// ─────────────────────────────────────────────────────────────
// TAB 3 — TIMELINE VIEW
// ─────────────────────────────────────────────────────────────

@Composable
private fun TimelineTabContent(
    items: List<TargetItem>,
    onToggleComplete: (String) -> Unit,
    onStartQuiz: (String) -> Unit,
    onViewNotes: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        TimeSlot.values().forEach { slot ->
            val slotItems = items.filter { it.timeSlot == slot }
            if (slotItems.isEmpty()) return@forEach
            item {
                Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(BpscColors.PrimaryLight), contentAlignment = Alignment.Center) { Text(slot.icon, fontSize = 18.sp) }
                    Column { Text(slot.label, style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold); Text(slot.range, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary) }
                    Spacer(Modifier.weight(1f))
                    val doneCount = slotItems.count { it.target.isCompleted }
                    Text("$doneCount/${slotItems.size}", style = MaterialTheme.typography.bodyMedium, color = if (doneCount == slotItems.size) BpscColors.Success else BpscColors.TextSecondary, fontWeight = FontWeight.Bold)
                }
            }
            itemsIndexed(slotItems, key = { _, item -> item.target.id }) { index, item ->
                val isCompleted = item.target.isCompleted
                val isLast      = index == slotItems.size - 1
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 4.dp)) {
                        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(if (isCompleted) BpscColors.Success else BpscColors.Primary).border(2.dp, if (isCompleted) BpscColors.Success else BpscColors.PrimaryLight, CircleShape)) {
                            if (isCompleted) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(8.dp).align(Alignment.Center))
                        }
                        if (!isLast) Box(modifier = Modifier.width(2.dp).height(80.dp).background(Brush.verticalGradient(listOf(BpscColors.Primary.copy(0.3f), BpscColors.Primary.copy(0.1f)))))
                    }
                    Spacer(Modifier.width(12.dp))
                    Card(modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 8.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = if (isCompleted) Color(0xFFF0FBF5) else Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.target.title, style = MaterialTheme.typography.titleMedium, color = if (isCompleted) BpscColors.TextSecondary else BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textDecoration = if (isCompleted) TextDecoration.LineThrough else null)
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        SubjectTag(item.target.subject); DifficultyBadge(item.difficulty)
                                        Text("⏱ ${item.target.estimatedMinutes}m", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SmallIconButton(Icons.Rounded.Quiz,          BpscColors.PrimaryLight,    BpscColors.Primary)   { onStartQuiz(item.target.id) }
                                    SmallIconButton(Icons.Rounded.MenuBook,      Color(0xFFF3E8FD),          Color(0xFF9B59B6))     { onViewNotes() }
                                    SmallIconButton(
                                        if (isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                        if (isCompleted) Color(0xFFE8FDF4) else BpscColors.Surface,
                                        if (isCompleted) BpscColors.Success else BpscColors.TextHint
                                    ) { onToggleComplete(item.target.id) }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SmallIconButton(icon: ImageVector, bg: Color, tint: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
    }
}
