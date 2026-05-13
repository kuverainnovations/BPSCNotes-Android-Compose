package com.example.bpscnotes.presentation.rooms

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.EndSessionResponseData
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// ════════════════════════════════════════════════════════════
// FILE: presentation/rooms/StudyFocusScreen.kt
//
// The active study session screen.
// Shows:
//   - Elapsed time ring with animated progress
//   - Live coin + XP counters (updated every heartbeat)
//   - AFK warning banner
//   - Session summary sheet when ended
// ════════════════════════════════════════════════════════════

@Composable
fun StudyFocusScreen(
    navController: NavHostController,
    viewModel: StudySessionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Show summary when session ends
    LaunchedEffect(state.status) {
        if (state.status == SessionStatus.ENDED) {
            // Stay on screen to show summary sheet
        }
        if (state.status == SessionStatus.IDLE && state.summary == null) {
            // Was IDLE from the start — shouldn't be here
            navController.popBackStack()
        }
    }

    when (state.status) {
        SessionStatus.ENDED -> {
            SessionSummarySheet(
                summary = state.summary,
                onDismiss = {
                    viewModel.clearSession()
                    navController.popBackStack()
                }
            )
        }
        else -> {
            ActiveSessionContent(
                state   = state,
                onEnd   = { viewModel.endSession() },
                onDismissAfk = { viewModel.dismissAfkWarning() },
                onBack  = {
                    // Back = end session confirmation
                    viewModel.endSession()
                }
            )
        }
    }
}

// ── Active Session UI ──────────────────────────────────────────

@Composable
private fun ActiveSessionContent(
    state: StudySessionUiState,
    onEnd: () -> Unit,
    onDismissAfk: () -> Unit,
    onBack: () -> Unit
) {
    // Elapsed time counter (local — increments every second)
    var elapsedSeconds by remember { mutableIntStateOf(state.activeMinutes * 60) }
    LaunchedEffect(state.status) {
        while (state.status == SessionStatus.ACTIVE || state.status == SessionStatus.AFK) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timeStr = if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)

    // Ring animation — full circle at 60 min
    val ringProgress by animateFloatAsState(
        targetValue = (elapsedSeconds % 3600) / 3600f,
        animationSpec = tween(1000),
        label = "ring"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF051D56), Color(0xFF0A2472), Color(0xFF1565C0)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Bar ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(Color.White.copy(0.15f)).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Study Session",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.tierName != null) {
                        Text(
                            "${state.tierEmoji ?: ""} ${state.tierName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(0.7f)
                        )
                    }
                }
                // Mode badge — tapping switches between study + pomodoro
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp).clickable {
                            // toggle between study and pomodoro (cosmetic — heartbeat continues)
                        }
                ) {
                    Text(
                        if (state.mode == "pomodoro") "🍅 Pomodoro" else "📚 Focus",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ── AFK Warning ──────────────────────────────────
            if (state.status == SessionStatus.AFK) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF6B35))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Warning,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    "AFK Detected",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Study time was not counted for the idle period.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(0.85f)
                                )
                            }
                        }
                        TextButton(onClick = onDismissAfk) {
                            Text("I'm Back", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Pomodoro mode: show PomodoroTimer instead of ring ─
            if (state.mode == "pomodoro") {
                Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    PomodoroTimer(
                        onActiveStateChange = { isWorking ->
                            // During break, AFK logic is fine — heartbeats still fire
                            // but user is on break so no new coins expected
                        }
                    )
                }
            } else {

                // ── Timer Ring (study mode) ─────────────────────────
                Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        val stroke = 10.dp.toPx()
                        val inset = stroke / 2
                        val sz = androidx.compose.ui.geometry.Size(
                            size.width - stroke,
                            size.height - stroke
                        )
                        // Track
                        drawArc(
                            Color.White.copy(0.15f),
                            -90f,
                            360f,
                            false,
                            style = Stroke(stroke),
                            topLeft = Offset(inset, inset),
                            size = sz
                        )
                        // Progress
                        drawArc(
                            brush = Brush.sweepGradient(listOf(Color(0xFF64B5F6), Color.White)),
                            startAngle = -90f,
                            sweepAngle = ringProgress * 360f,
                            useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                            topLeft = Offset(inset, inset),
                            size = sz
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            timeStr,
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 36.sp
                        )
                        Text(
                            "active",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(0.65f)
                        )
                        if (state.wasAfkLastBeat) {
                            Text(
                                "⚠️ AFK period",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFF6B35)
                            )
                        } else {
                            Text(
                                "🟢 Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.Success
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ── Live Coin + XP Counters ───────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    SessionStatChip(
                        label = "Coins",
                        value = "+${state.coinsThisSession}",
                        icon = "🪙"
                    )
                    SessionStatChip(label = "XP", value = "+${state.xpThisSession}", icon = "⚡")
                    SessionStatChip(label = "AFK", value = "${state.afkCount}×", icon = "💤")
                }

                // Last beat result
                if (state.coinsLastBeat > 0 || state.xpLastBeat > 0) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(0.12f))
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.coinsLastBeat > 0) Text(
                            "🪙+${state.coinsLastBeat}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BpscColors.CoinGold,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.xpLastBeat > 0) Text(
                            "⚡+${state.xpLastBeat} XP",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64B5F6),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // ── End Button ───────────────────────────────────
                Button(
                    onClick = onEnd,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = state.status != SessionStatus.ENDING,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(0.15f),
                        contentColor = Color.White
                    )
                ) {
                    if (state.status == SessionStatus.ENDING) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Rounded.StopCircle,
                            null,
                            modifier = Modifier.size(20.dp)
                        ); Spacer(Modifier.width(8.dp))
                        Text("End Session", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

    }
}

    @Composable
    fun SessionStatChip(label: String, value: String, icon: String) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 20.sp)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(0.65f),
                fontSize = 10.sp
            )
        }
    }


// ── Session Summary Sheet ──────────────────────────────────────

    @Composable
    fun SessionSummarySheet(
        summary: EndSessionResponseData?,
        onDismiss: () -> Unit
    ) {
        val animProg by animateFloatAsState(
            targetValue = if (summary != null && summary.durationMinutes > 0)
                (summary.activeMinutes.toFloat() / summary.durationMinutes).coerceIn(0f, 1f)
            else 0f,
            animationSpec = tween(1200),
            label = "summary_prog"
        )

        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0A2472),
                        Color(0xFF1565C0),
                        BpscColors.Surface
                    )
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(48.dp))
                Text("✅", fontSize = 64.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Session Complete!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    summary?.message ?: "Great work!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(Modifier.height(24.dp))

                // Arc ring showing active vs total time
                Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        val stroke = 10.dp.toPx();
                        val inset = stroke / 2
                        val sz = androidx.compose.ui.geometry.Size(
                            size.width - stroke,
                            size.height - stroke
                        )
                        drawArc(
                            Color.White.copy(0.15f),
                            -90f,
                            360f,
                            false,
                            style = Stroke(stroke),
                            topLeft = Offset(inset, inset),
                            size = sz
                        )
                        drawArc(
                            Brush.sweepGradient(listOf(Color(0xFF64B5F6), Color.White)),
                            -90f,
                            animProg * 360f,
                            false,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                            topLeft = Offset(inset, inset),
                            size = sz
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${summary?.activeMinutes ?: 0}m",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "active",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.7f)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SumItem("⏱️", "${summary?.durationMinutes ?: 0}m", "Total Time")
                        SumItem("🎯", "${summary?.activeMinutes ?: 0}m", "Active")
                        SumItem("🪙", "+${summary?.totalCoins ?: 0}", "Coins")
                        SumItem("⚡", "+${summary?.totalXp ?: 0}", "XP")
                    }
                }

                if ((summary?.bonusCoins ?: 0) > 0) {
                    Spacer(Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = BpscColors.CoinGold.copy(0.15f)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎉", fontSize = 20.sp)
                            Text(
                                "Bonus +${summary?.bonusCoins} coins for 30+ active minutes!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BpscColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    Text("Back to Rooms", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }


    @Composable
    fun SumItem(icon: String, value: String, label: String) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 20.sp)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = BpscColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = BpscColors.TextHint,
                fontSize = 9.sp
            )
        }
    }

