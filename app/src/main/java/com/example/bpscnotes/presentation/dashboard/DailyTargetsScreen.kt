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
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.BpscDropdown
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.DailyTargetDto
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.example.bpscnotes.presentation.navigation.Routes.Screen
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
// UI-only model: enriches DailyTargetDto with display metadata
// Mapped from DailyTargetDto — no MockData involved.
// ─────────────────────────────────────────────────────────────

data class TargetItem(
    val target: DailyTargetDto,
    // val difficulty: Difficulty,   // derived from target.difficulty string
    val timeSlot: TimeSlot,       // derived from target.timeSlot string
)

enum class TimeSlot(val label: String, val icon: String, val range: String) {
    Morning  ("Morning",   "🌅", "6AM – 12PM"),
    Afternoon("Afternoon", "☀️", "12PM – 6PM"),
    Night    ("Night",     "🌙", "6PM – 10PM");
    @Composable fun localLabel(): String {
        val str = LocalStrings.current
        return when(this) { Morning -> str.targetMorning; Afternoon -> str.targetAfternoon; Night -> str.targetNight }
    }
}

/** Map a [DailyTargetDto] to the UI [TargetItem] — no static data */
private fun DailyTargetDto.toTargetItem() = TargetItem(
    target     = this,
// difficulty mapping removed
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
    val cs = MaterialTheme.colorScheme
    val state by viewModel.uiState.collectAsState()
    val str = LocalStrings.current
    val context = LocalContext.current

    var alertMessage by remember { mutableStateOf<String?>(null) }
    var alertIsError by remember { mutableStateOf(false) }

    LaunchedEffect(state.targetSuccess) {
        state.targetSuccess?.let { msg ->
            // Show dialog only for completion rewards; toast for create/delete actions
            if (msg.contains("coins", ignoreCase = true) || msg.contains("🎉")) {
                alertMessage = msg; alertIsError = false
            } else {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            viewModel.clearTargetSuccess()
        }
    }
    LaunchedEffect(state.targetError) {
        state.targetError?.let {
            alertMessage = it; alertIsError = true
            viewModel.clearTargetError()
        }
    }
    if (alertMessage != null) {
        AlertDialog(
            onDismissRequest = { alertMessage = null },
            containerColor = Color.White,
            icon = {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape)
                        .background(if (alertIsError) Color(0xFFFFF3CD) else Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (alertIsError) "⏱️" else "🎉", fontSize = 24.sp)
                }
            },
            title = {
                Text(
                    if (alertIsError) "Need More Time" else "Target Completed!",
                    fontWeight = FontWeight.Bold,
                    color = if (alertIsError) Color(0xFF0A2472) else Color(0xFF1B5E20)
                )
            },
            text = {
                Text(
                    alertMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BpscColors.TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { alertMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Text("Got it", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Map API DTOs → UI TargetItems once
    val allTargets = remember(state.dailyTargets) {
        state.dailyTargets.map { it.toTargetItem() }
    }

    // Local completion state (mirrors API; in production you'd call PATCH /users/daily-targets/{id})


    val completed = allTargets.count { it.target.isCompleted }
    val total     = allTargets.size
    // Live per-target reward (Coins page -> Daily Targets). Each completed
    // target awards this many coins server-side; the hero badge below
    // multiplies it by the remaining target count.
    val coinsPerTarget = viewModel.coinsConfig.coins("target_complete", 1)
    val progress by animateFloatAsState(
        targetValue   = if (total > 0) completed.toFloat() / total else 0f,
        animationSpec = tween(800),
        label         = "prog"
    )

    //var selectedTab    by remember { mutableIntStateOf(0) }
    var showAddSheet   by remember { mutableStateOf(false) }
    var showHistory    by remember { mutableStateOf(false) }

    //val tabs    = listOf("List", "Cards", "Timeline")

    Box(modifier = Modifier.fillMaxSize().background(cs.background)) {
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
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { navController.popBackStackSafe() },
                                Alignment.Center
                            ) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(str.targetTitle, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    "Today — ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())}",
                                    style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f)
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { showHistory = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.History, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.15f)).clickable { showAddSheet = true }.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text(str.targetAdd, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                }
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
                                    Text(str.targetLoading, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                                }
                            }
                        }
                        // Error state
                        state.error != null && allTargets.isEmpty() -> {
                            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.1f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("⚠️", fontSize = 14.sp)
                                Text(str.targetFailed, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.9f), modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.refresh() }) { Text(str.retry, color = Color.White) }
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
                                    Text("+${(total - completed) * coinsPerTarget}", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.Bold)
                                    Text(str.targetCoins, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Tabs
                    /*  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                          tabs.forEachIndexed { index, tab ->
                              val sel = selectedTab == index
                              Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (sel) Color.White else Color.White.copy(0.12f)).clickable { selectedTab = index }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                  Text(tab, style = MaterialTheme.typography.bodyMedium, color = if (sel) BpscColors.Primary else Color.White.copy(0.8f), fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                              }
                          }
                      }*/
                }
            }

            // ── Streak warning ──────────────────────────────────
            AnimatedVisibility(visible = completed == 0 && total > 0) {
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF3CD)).padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("⚠️", fontSize = 16.sp)
                    Column {
                        Text(str.targetStreak, style = MaterialTheme.typography.titleMedium, color = Color(0xFF856404), fontWeight = FontWeight.Bold)
                        Text(str.targetStreakProtect, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF856404))
                    }
                }
            }

            // ── Empty state when loaded but no targets ──────────
            if (!state.isLoading && allTargets.isEmpty() && state.error == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(BpscColors.PrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎯", fontSize = 44.sp)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                str.targetNoTargets,
                                style = MaterialTheme.typography.titleLarge,
                                color = cs.onSurface,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                "Set your daily study goals and earn coins when you complete them!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                        Button(
                            onClick = { showAddSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(str.targetCreate, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // ── Tab content — weight(1f) gives LazyColumn a bounded height ──
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    /*when (selectedTab) {
                        0 -> ListTabContent(
                            items            = allTargets,
                            filters          = filters,
                            selectedFilter   = selectedFilter,
                            onFilterChange   = { selectedFilter = it },
                            onToggleComplete = { id ->  viewModel.toggleTargetComplete(id)*//*if (completedIds.contains(id)) completedIds.remove(id) else completedIds.add(id)*//* },
                        onStartQuiz      = { subjectId -> navController.navigate(Screen.DailyQuiz.createRoute(subjectId)) },
                        onViewNotes      = { navController.navigate(Screen.ELibrary.route) }
                    )
                    1 -> CardsTabContent(
                        items            = allTargets,
                        onToggleComplete = { id ->  viewModel.toggleTargetComplete(id)*//*if (completedIds.contains(id)) completedIds.remove(id) else completedIds.add(id)*//* },
                        onStartQuiz      = { subjectId -> navController.navigate(Screen.DailyQuiz.createRoute(subjectId)) },
                        onViewNotes      = { navController.navigate(Screen.ELibrary.route) },
                        onAddMore        = { showAddSheet = true }
                    )
                    2 -> TimelineTabContent(
                        items            = allTargets,
                        onToggleComplete = { id ->     viewModel.toggleTargetComplete(id)
                            *//*if (completedIds.contains(id)) completedIds.remove(id) else completedIds.add(id)*//* },
                        onStartQuiz      = { subjectId -> navController.navigate(Screen.DailyQuiz.createRoute(subjectId)) },
                        onViewNotes      = { navController.navigate(Screen.ELibrary.route) }
                    )
                }*/
                    ListTabContent(
                        items            = allTargets,
                        onToggleComplete = { id -> viewModel.toggleTargetComplete(id) },
                        onDelete         = { id -> viewModel.deleteTarget(id) },
                        onUpdate         = { id, title, subject -> viewModel.updateTarget(id, title, subject) }
                    )
                } // end weight Box
            }
        }

        if (showAddSheet) {
            if (state.dailyTargets.size >= 10) {
                Toast.makeText(LocalContext.current, str.targetMax, Toast.LENGTH_SHORT).show()
                return
            }
            CreateTargetSheet(viewModel = viewModel, onDismiss = { showAddSheet = false })
        }

        if (showHistory) {
            DailyTargetHistorySheet(
                history   = state.targetHistory,
                onDismiss = { showHistory = false }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HISTORY PAGE — 30-day completion overview (full screen)
// ─────────────────────────────────────────────────────────────
@Composable
private fun DailyTargetHistorySheet(
    history:   List<com.example.bpscnotes.data.remote.api.DailyTargetHistoryDto>,
    onDismiss: () -> Unit
) {
    val cs   = MaterialTheme.colorScheme
    val dfmt = remember { java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            //.statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0D47A1), Color(0xFF1565C0))))
                    .padding(start = 20.dp, end = 20.dp, bottom = 16.dp,top=46.dp),
                Arrangement.SpaceBetween, Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { onDismiss() },
                        Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Target History", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Last 30 days", style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.7f))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp)
            ) {

            if (history.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📊", fontSize = 40.sp)
                        Text("No history yet", style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.Bold)
                        Text("Complete some targets to see your progress here", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                }
            } else {
                // Summary
                val totalCompleted = history.sumOf { it.completed }
                val totalTargets   = history.sumOf { it.total }
                val avgPct         = if (history.isNotEmpty()) history.sumOf { it.completionPct } / history.size else 0
                val activeDays     = history.count { it.completed > 0 }

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BpscColors.PrimaryLight.copy(0.3f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    Arrangement.SpaceEvenly
                ) {
                    HistoryStat("✅", "$totalCompleted", "Completed")
                    HistoryStat("📅", "$activeDays", "Active Days")
                    HistoryStat("📈", "$avgPct%", "Avg Rate")
                }

                Spacer(Modifier.height(12.dp))

                // List — flows naturally inside the outer scrollable Column
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    history.forEach { day ->
                        val date = remember(day.date) {
                            try { dfmt.format(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(day.date)!!) }
                            catch (_: Exception) { day.date }
                        }
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(cs.surface)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            Arrangement.SpaceBetween, Alignment.CenterVertically
                        ) {
                            Column {
                                Text(date, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                                Text("${day.completed}/${day.total} targets",
                                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Mini progress bar
                                Box(Modifier.width(60.dp).height(6.dp).clip(RoundedCornerShape(3.dp))
                                    .background(cs.onSurface.copy(0.08f))) {
                                    Box(Modifier.fillMaxWidth(day.completionPct / 100f).fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (day.completionPct == 100) BpscColors.Success else BpscColors.Primary))
                                }
                                Text("${day.completionPct}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (day.completionPct == 100) BpscColors.Success else BpscColors.Primary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        } // content column
    } // outer column
  } // box
} // function

@Composable
private fun HistoryStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(emoji, fontSize = 16.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = BpscColors.Primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary)
    }
}
// ─────────────────────────────────────────────────────────────

@Composable
private fun ListTabContent(
    items: List<TargetItem>,
    onToggleComplete: (String) -> Unit,
    onDelete: (String) -> Unit,
    onUpdate: (id: String, title: String, subject: String) -> Unit = { _, _, _ -> }
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val carried = items.filter { it.target.isCarriedForward }
    val today   = items.filter { !it.target.isCarriedForward }
    var editingTarget by remember { mutableStateOf<TargetItem?>(null) }

    // Show the true age of the oldest pending target — a target
    // pending for 3 days must not keep claiming "From yesterday".
    val carriedSubtitle = remember(carried) {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val todayMs = fmt.parse(fmt.format(java.util.Date()))?.time ?: 0L
        val oldestDays = carried
            .mapNotNull { it.target.createdAt?.take(10) }
            .mapNotNull { runCatching { fmt.parse(it) }.getOrNull()?.time }
            .maxOfOrNull { ((todayMs - it) / 86_400_000L).toInt() } ?: 1
        if (oldestDays <= 1) "From yesterday" else "Pending for $oldestDays days"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (carried.isNotEmpty()) {
            item { SectionLabel("📅", "Carried Forward", carriedSubtitle) }
            items(carried, key = { it.target.id }) { item ->
                TargetListCard(item = item, isCompleted = item.target.isCompleted, onToggleComplete = { onToggleComplete(item.target.id) }, onDelete = { onDelete(item.target.id) }, onEdit = { editingTarget = item })
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
        item { SectionLabel("🎯", "Today's Targets", "${today.size} topics assigned") }
        items(today, key = { it.target.id }) { item ->
            TargetListCard(item = item, isCompleted = item.target.isCompleted, onToggleComplete = { onToggleComplete(item.target.id) }, onDelete = { onDelete(item.target.id) }, onEdit = { editingTarget = item })
        }
    }

    // Edit bottom sheet
    editingTarget?.let { editing ->
        EditTargetSheet(target = editing.target, onDismiss = { editingTarget = null }, onSave = { title, subject ->
            onUpdate(editing.target.id, title, subject)
        })
    }
}

@Composable
private fun SectionLabel(icon: String, title: String, subtitle: String) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(icon, fontSize = 16.sp)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun TargetListCard(item: TargetItem, isCompleted: Boolean, onToggleComplete: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (isCompleted) Color(0xFFF0FBF5) else Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Completed = locked green check (cannot uncheck)
            // Incomplete = tappable circle
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape)
                    .background(if (isCompleted) BpscColors.Success else Color.Transparent)
                    .border(2.dp, if (isCompleted) BpscColors.Success else BpscColors.TextHint, CircleShape)
                    .then(if (!isCompleted) Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onToggleComplete() } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.target.title, style = MaterialTheme.typography.titleMedium, color = if (isCompleted) BpscColors.TextSecondary else BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, textDecoration = if (isCompleted) TextDecoration.LineThrough else null)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SubjectTag(item.target.subject)
                    if (item.target.isCarriedForward) {
                        Text(str.targetCarried, style = MaterialTheme.typography.labelSmall, color = Color(0xFFE67E22), modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFF0EA)).padding(horizontal = 5.dp, vertical = 2.dp), fontSize = 9.sp)
                    }
                }
            }
            // Edit button — only for incomplete targets
            if (!isCompleted) {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F4FD))
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Edit, null, tint = Color(0xFF1565C0), modifier = Modifier.size(15.dp))
                }
            }
            // Delete button
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFEE8E8))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Delete, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, bg: Color, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(bg).clickable(onClick = onClick).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
    }
}

@Composable
private fun SubjectTag(subject: String) {
    val cs = MaterialTheme.colorScheme
    val colors = mapOf("Polity" to Pair(Color(0xFF9B59B6), Color(0xFFF3E8FD)), "History" to Pair(Color(0xFFE74C3C), Color(0xFFFEE8E8)), "Geography" to Pair(Color(0xFF1ABC9C), Color(0xFFE8FDF8)), "Economy" to Pair(Color(0xFFE67E22), Color(0xFFFFF0EA)), "Science" to Pair(Color(0xFF2ECC71), Color(0xFFE8FDF4)), "Bihar GK" to Pair(Color(0xFFF39C12), Color(0xFFFFF8E1)))
    val (fg, bg) = colors[subject] ?: Pair(BpscColors.Primary, BpscColors.PrimaryLight)
    Text(subject, style = MaterialTheme.typography.labelSmall, color = fg, fontSize = 9.sp, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 6.dp, vertical = 2.dp))
}


// ─────────────────────────────────────────────────────────────
// TAB 2 — CARD SWIPE VIEW
// ─────────────────────────────────────────────────────────────

@Composable
private fun CardsTabContent(items: List<TargetItem>, onToggleComplete: (String) -> Unit, onStartQuiz: (String) -> Unit, onViewNotes: () -> Unit, onAddMore: () -> Unit = {}) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    var currentIndex by remember { mutableIntStateOf(0) }
    val scope        = rememberCoroutineScope()
    val current      = items.getOrNull(currentIndex)
    val total        = items.size

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEachIndexed { i, item ->
                val done = item.target.isCompleted
                Box(modifier = Modifier.size(if (i == currentIndex) 10.dp else 8.dp).clip(CircleShape).background(when { i == currentIndex -> BpscColors.Primary; done -> BpscColors.Success; else -> cs.outline }))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("${currentIndex + 1} of $total", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
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
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SubjectTag(current.target.subject);
                        //DifficultyBadge(current.difficulty)
                        Spacer(Modifier.weight(1f))
                        if (current.target.isCarriedForward) Text(str.targetCarriedForward, style = MaterialTheme.typography.labelSmall, color = Color(0xFFE67E22), fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(current.target.title, style = MaterialTheme.typography.headlineSmall, color = cs.onSurface, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cs.background).padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        CardStat("⏱️", "${current.target.estimatedMinutes} min",   "Est. Time")
                        CardStat("📝", "${current.target.totalQuestions}Q",         "Questions")
                        CardStat("🕐", java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date()), "Current Time")
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { onStartQuiz(current.target.id) }, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                            Icon(Icons.Rounded.Quiz, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(str.quizTitle)
                        }
                        OutlinedButton(onClick = onViewNotes, modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, BpscColors.Primary), colors = ButtonDefaults.outlinedButtonColors(contentColor = BpscColors.Primary)) {
                            Icon(Icons.Rounded.MenuBook, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(str.materialsTitle)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Icon(Icons.Rounded.KeyboardArrowLeft, null, tint = BpscColors.TextHint, modifier = Modifier.size(16.dp)); Text("Skip", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint) }
                        Text(str.targetSwipeNavigate, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text("Done", style = MaterialTheme.typography.bodyMedium, color = BpscColors.Success); Icon(Icons.Rounded.KeyboardArrowRight, null, tint = BpscColors.Success, modifier = Modifier.size(16.dp)) }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = { onToggleComplete(current.target.id); if (currentIndex < items.size - 1) currentIndex++ }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isCompleted) BpscColors.Success else BpscColors.Primary)) {
                Icon(if (isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.Check, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (isCompleted) "${str.targetCompleted} ✓" else str.targetComplete)
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🎉", fontSize = 60.sp)
                Text("${str.targetCompleted}! 🎉", style = MaterialTheme.typography.headlineSmall, color = cs.onSurface, fontWeight = FontWeight.ExtraBold)
                Text(str.targetAllDone, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(BpscColors.PrimaryLight)
                        .clickable(onClick = onAddMore)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Add, null, tint = BpscColors.Primary, modifier = Modifier.size(18.dp))
                    Text("Need more time? Add a target", style = MaterialTheme.typography.titleSmall, color = BpscColors.Primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CardStat(icon: String, value: String, label: String) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(icon, fontSize = 14.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, fontSize = 9.sp)
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
    val cs = MaterialTheme.colorScheme
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        TimeSlot.values().forEach { slot ->
            val slotItems = items.filter { it.timeSlot == slot }
            if (slotItems.isEmpty()) return@forEach
            item {
                Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(BpscColors.PrimaryLight), contentAlignment = Alignment.Center) { Text(slot.icon, fontSize = 18.sp) }
                    Column { Text(slot.label, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.Bold); Text(slot.range, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant) }
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
                                        SubjectTag(item.target.subject);
//                                        DifficultyBadge(item.difficulty)
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

// ─────────────────────────────────────────────────────────────
// EDIT TARGET SHEET
// ─────────────────────────────────────────────────────────────
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun EditTargetSheet(
    target: com.example.bpscnotes.data.remote.api.DailyTargetDto,
    onDismiss: () -> Unit,
    onSave: (title: String, subject: String) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    val subjects = listOf(
        "Polity", "History", "Geography", "Economy", "Bihar GK",
        "Science & Tech", "General Studies", "Environment", "Maths", "English", "Current Affairs"
    )

    // Titles are stored as "Subject - Subtopic" (see CreateTargetSheet).
    // Split that out so this field only shows/edits the subtopic, and the
    // dropdown below pre-selects whichever subject is actually part of this
    // target — target.subject is preferred, but falls back to the prefix
    // already in the title (covers carried-forward targets where the
    // subject column may not match).
    val dashIndex   = target.title.indexOf(" - ")
    val titlePrefix = if (dashIndex > 0) target.title.substring(0, dashIndex) else ""
    val initialSubject = subjects.firstOrNull { it.equals(target.subject, ignoreCase = true) }
        ?: subjects.firstOrNull { it.equals(titlePrefix, ignoreCase = true) }
        ?: "General Studies"
    val initialSubtopic =
        if (dashIndex > 0 && titlePrefix.equals(initialSubject, ignoreCase = true))
            target.title.substring(dashIndex + 3)
        else target.title

    var title by remember { mutableStateOf(initialSubtopic) }
    var selectedSubject by remember { mutableStateOf(initialSubject) }
    var subjectExpanded by remember { mutableStateOf(false) }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Edit Target", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = cs.onSurface)

            // Subject dropdown — pick subject first, then enter sub-topic
            BpscDropdown(
                value    = selectedSubject,
                label    = "Subject",
                options  = subjects,
                onSelect = { selectedSubject = it }
            )

            // Title field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Sub-topic") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Cancel") }

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave("$selectedSubject - ${title.trim()}", selectedSubject)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.bpscnotes.core.ui.t.BpscColors.Primary)
                ) { Text("Save", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}