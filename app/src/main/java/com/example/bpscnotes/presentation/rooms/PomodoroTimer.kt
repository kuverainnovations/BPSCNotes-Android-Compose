package com.example.bpscnotes.presentation.rooms

import com.example.bpscnotes.core.language.LocalStrings
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.app.NotificationCompat
import com.example.bpscnotes.core.ui.t.BpscColors
import kotlinx.coroutines.delay

// ════════════════════════════════════════════════════════════
// FILE: presentation/rooms/PomodoroTimer.kt
//
// Fully local Pomodoro timer — no backend sync needed.
// Integrates with StudyFocusScreen as the "Pomodoro" mode tab.
// Features:
//   - 25 min work / 5 min break cycles (admin-configurable via params)
//   - Animated circular ring
//   - System notification when cycle completes
//   - Session auto-pauses during break (no heartbeat during break)
// ════════════════════════════════════════════════════════════

// ── Timer state ───────────────────────────────────────────────
enum class PomodoroPhase { WORK, BREAK, LONG_BREAK }

data class PomodoroState(
    val phase:            PomodoroPhase = PomodoroPhase.WORK,
    val totalSeconds:     Int           = 25 * 60,
    val remainingSeconds: Int           = 25 * 60,
    val isRunning:        Boolean       = false,
    val cyclesCompleted:  Int           = 0,    // work cycles done
    // Config (editable by user in screen)
    val workMinutes:      Int           = 25,
    val breakMinutes:     Int           = 5,
    val longBreakMinutes: Int           = 15,
    val longBreakAfter:   Int           = 4,    // long break after every N cycles
)

// ════════════════════════════════════════════════════════════
// Composable: PomodoroTimer
// Embedded inside StudyFocusScreen when mode == "pomodoro"
// ════════════════════════════════════════════════════════════
@Composable
fun PomodoroTimer(
    // Called with false during break (pause heartbeat), true during work
    onActiveStateChange: (isWorking: Boolean) -> Unit = {},
) {
    var state by remember { mutableStateOf(PomodoroState()) }
    val context = LocalContext.current
    val str = LocalStrings.current

    // Tick loop — counts down every second when running
    LaunchedEffect(state.isRunning, state.phase, state.cyclesCompleted) {
        if (!state.isRunning) return@LaunchedEffect
        while (state.isRunning && state.remainingSeconds > 0) {
            delay(1000L)
            state = state.copy(remainingSeconds = state.remainingSeconds - 1)
        }
        // Cycle complete
        if (state.remainingSeconds <= 0) {
            handleCycleComplete(state, context) { newState ->
                state = newState
                onActiveStateChange(newState.phase == PomodoroPhase.WORK)
            }
        }
    }

    val progress      = state.remainingSeconds.toFloat() / state.totalSeconds.toFloat()
    val animProgress  by animateFloatAsState(progress, tween(1000), label = "pomo_ring")
    val phaseColor    = when (state.phase) {
        PomodoroPhase.WORK       -> BpscColors.Primary
        PomodoroPhase.BREAK      -> BpscColors.Success
        PomodoroPhase.LONG_BREAK -> BpscColors.CoinGold
    }
    val phaseLabel = when (state.phase) {
        PomodoroPhase.WORK       -> str.pomodoroFocus
        PomodoroPhase.BREAK      -> str.pomodoroBreak
        PomodoroPhase.LONG_BREAK -> str.pomodoroLongBreak
    }

    val mins = state.remainingSeconds / 60
    val secs = state.remainingSeconds % 60

    Column(
        modifier              = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(20.dp)
    ) {
        // Phase label + cycle count
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(phaseLabel, style = MaterialTheme.typography.titleMedium, color = phaseColor, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(phaseColor.copy(0.1f)).padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("🍅", style = MaterialTheme.typography.labelSmall)
                Text("${state.cyclesCompleted} done", style = MaterialTheme.typography.labelSmall, color = phaseColor, fontWeight = FontWeight.Bold)
            }
        }

        // Ring timer
        Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 8.dp.toPx()
                val inset  = stroke / 2
                val sz     = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                drawArc(phaseColor.copy(0.15f), -90f, 360f, false, style = Stroke(stroke), topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = sz)
                drawArc(phaseColor, -90f, animProgress * 360f, false, style = Stroke(stroke, cap = StrokeCap.Round), topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = sz)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%02d:%02d".format(mins, secs), style = MaterialTheme.typography.headlineLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
                Text(if (state.phase == PomodoroPhase.WORK) "focus" else "rest", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
            }
        }

        // Controls
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Reset button
            OutlinedIconButton(
                onClick  = {
                    state = PomodoroState(workMinutes = state.workMinutes, breakMinutes = state.breakMinutes, longBreakMinutes = state.longBreakMinutes, longBreakAfter = state.longBreakAfter).let {
                        it.copy(totalSeconds = it.workMinutes * 60, remainingSeconds = it.workMinutes * 60)
                    }
                    onActiveStateChange(false)
                },
                modifier = Modifier.size(44.dp),
                border   = BorderStroke(1.dp, BpscColors.Divider)
            ) {
                Icon(Icons.Rounded.RestartAlt, null, tint = BpscColors.TextSecondary, modifier = Modifier.size(18.dp))
            }

            // Start / Pause
            Button(
                onClick  = { state = state.copy(isRunning = !state.isRunning) },
                modifier = Modifier.height(44.dp).width(120.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = phaseColor)
            ) {
                Icon(if (state.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (state.isRunning) str.pomodoroPause else str.pomodoroStart, style = MaterialTheme.typography.titleMedium)
            }

            // Skip phase
            OutlinedIconButton(
                onClick  = {
                    val next = advancePhase(state)
                    state = next
                    onActiveStateChange(next.phase == PomodoroPhase.WORK)
                },
                modifier = Modifier.size(44.dp),
                border   = BorderStroke(1.dp, BpscColors.Divider)
            ) {
                Icon(Icons.Rounded.SkipNext, null, tint = BpscColors.TextSecondary, modifier = Modifier.size(18.dp))
            }
        }

        // Config chips
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            PomodoroChip("Work: ${state.workMinutes}m",  phaseColor, state.phase == PomodoroPhase.WORK)
            Spacer(Modifier.width(8.dp))
            PomodoroChip("Break: ${state.breakMinutes}m", BpscColors.Success, state.phase != PomodoroPhase.WORK)
            Spacer(Modifier.width(8.dp))
            PomodoroChip("Long: ${state.longBreakMinutes}m", BpscColors.CoinGold, false)
        }

        Text(
            "Pomodoro technique: ${state.workMinutes}min focus → ${state.breakMinutes}min break. Every ${state.longBreakAfter} cycles, take a ${state.longBreakMinutes}min long break.",
            style = MaterialTheme.typography.bodySmall,
            color = BpscColors.TextHint,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PomodoroChip(label: String, color: Color, active: Boolean) {
    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (active) color.copy(0.12f) else Color(0xFFF5F5F5)).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) color else BpscColors.TextHint, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp)
    }
}

// ── Logic helpers ─────────────────────────────────────────────

private fun advancePhase(state: PomodoroState): PomodoroState {
    return when (state.phase) {
        PomodoroPhase.WORK -> {
            val newCycles = state.cyclesCompleted + 1
            val isLong    = newCycles % state.longBreakAfter == 0
            val phase     = if (isLong) PomodoroPhase.LONG_BREAK else PomodoroPhase.BREAK
            val secs      = if (isLong) state.longBreakMinutes * 60 else state.breakMinutes * 60
            state.copy(phase = phase, totalSeconds = secs, remainingSeconds = secs, cyclesCompleted = newCycles, isRunning = true)
        }
        else -> {
            val secs = state.workMinutes * 60
            state.copy(phase = PomodoroPhase.WORK, totalSeconds = secs, remainingSeconds = secs, isRunning = true)
        }
    }
}

private fun handleCycleComplete(
    state:    PomodoroState,
    context:  Context,
    onUpdate: (PomodoroState) -> Unit,
) {
    val (title, body) = when (state.phase) {
        PomodoroPhase.WORK       -> "🎉 Focus session done!" to "Great work! Time for a break."
        PomodoroPhase.BREAK      -> "⏰ Break over!" to "Back to studying. You've got this!"
        PomodoroPhase.LONG_BREAK -> "🚀 Long break done!" to "Ready for another focus sprint?"
    }
    sendLocalNotification(context, title, body)
    onUpdate(advancePhase(state))
}

private fun sendLocalNotification(context: Context, title: String, body: String) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(
            NotificationChannel("pomodoro", "Pomodoro Timer", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Study timer notifications" }
        )
    }
    val notif = NotificationCompat.Builder(context, "pomodoro")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(body)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    manager.notify(System.currentTimeMillis().toInt(), notif)
}
