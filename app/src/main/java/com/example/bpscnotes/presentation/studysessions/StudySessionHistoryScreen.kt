package com.example.bpscnotes.presentation.studysessions

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.StudySessionHistoryDto
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import java.text.SimpleDateFormat
import java.util.*

// ────────────────────────────────────────────────────────────────
// LOW-12: Study Session History
// ────────────────────────────────────────────────────────────────

@Composable
fun StudySessionHistoryScreen(
    navController: NavHostController,
    vm: StudySessionHistoryViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F7FA))) {

        // Header
        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(
                    listOf(Color(0xFF1A237E), Color(0xFF283593), Color(0xFF3949AB)),
                    Offset(0f, 0f), Offset(Float.MAX_VALUE, 0f),
                )
            )
        ) {
            Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.12f))
                            .clickable { navController.popBackStackSafe() },
                        Alignment.Center,
                    ) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.12f))
                            .clickable { vm.load() },
                        Alignment.Center,
                    ) { Icon(Icons.Rounded.Refresh, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                }
                Spacer(Modifier.height(8.dp))
                Text("Study Sessions", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("Your recent room study history", fontSize = 13.sp, color = Color.White.copy(0.75f))
                Spacer(Modifier.height(12.dp))

                // Summary chips
                if (!state.isLoading && state.error == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryChip("📅", "${state.sessions.size}", "sessions")
                        SummaryChip("⏱️", formatDuration(state.totalSecs), "total")
                        SummaryChip("⚡", "${state.totalXp} XP", "earned")
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // Body
        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading -> AppLoader()
                state.error != null -> ErrorState(state.error!!, onRetry = { vm.load() })
                state.sessions.isEmpty() -> EmptyState()
                else -> SessionList(state.sessions)
            }
        }
    }
}

@Composable
private fun SummaryChip(emoji: String, value: String, label: String) {
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.15f)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(emoji, fontSize = 12.sp)
        Text("$value ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color.White.copy(0.75f))
    }
}

@Composable
private fun SessionList(sessions: List<StudySessionHistoryDto>) {
    // Group by date
    val grouped = remember(sessions) {
        sessions.groupBy { s ->
            s.startedAt?.let { parseDate(it) } ?: "Unknown"
        }.entries.toList()
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp, ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        grouped.forEach { (date, daySessions) ->
            item(key = "header_$date") {
                Text(
                    date,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8888AA),
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
            }
            items(daySessions, key = { it.id }) { session ->
                SessionCard(session)
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SessionCard(session: StudySessionHistoryDto) {
    Card(
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Icon
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(BpscColors.brand.copy(0.1f)),
                Alignment.Center,
            ) { Text("📖", fontSize = 20.sp) }

            // Details
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    session.roomName ?: "Study Session",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E),
                )
                if (!session.tierName.isNullOrBlank()) {
                    Text(session.tierName, fontSize = 11.sp, color = Color(0xFF888899))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val startTime = session.startedAt?.let { formatTime(it) } ?: "—"
                    val endTime   = session.endedAt?.let { formatTime(it) } ?: "ongoing"
                    Text("$startTime – $endTime", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                }
            }

            // Right stats
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    formatDuration(session.durationSecs),
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BpscColors.brand,
                )
                if (session.xpEarned > 0) {
                    Text(
                        "+${session.xpEarned} XP",
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE67E22),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("📚", fontSize = 52.sp)
            Text("No study sessions yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF555566))
            Text("Join a study room to start tracking your sessions", fontSize = 13.sp, color = Color(0xFF888899), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("😕", fontSize = 48.sp)
            Text(message, textAlign = TextAlign.Center, color = Color(0xFF888899))
            Button(onClick = onRetry, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BpscColors.brand)) {
                Text("Retry")
            }
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────

fun formatDuration(secs: Int): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return when {
        h > 0  -> "${h}h ${m}m"
        m > 0  -> "${m}m ${s}s"
        else   -> "${s}s"
    }
}

private val ISO_PARSER = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

private fun parseDate(iso: String): String {
    return try {
        val date = ISO_PARSER.parse(iso) ?: return iso
        val today     = Calendar.getInstance().apply { time = Date() }
        val yesterday = Calendar.getInstance().apply { time = Date(); add(Calendar.DAY_OF_YEAR, -1) }
        val cal       = Calendar.getInstance().apply { time = date }
        return when {
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
            cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
            else -> SimpleDateFormat("d MMM yyyy", Locale.US).format(date)
        }
    } catch (_: Exception) { iso }
}

private fun formatTime(iso: String): String {
    return try {
        val date = ISO_PARSER.parse(iso) ?: return iso
        val local = SimpleDateFormat("h:mm a", Locale.US).apply { timeZone = TimeZone.getDefault() }
        local.format(date)
    } catch (_: Exception) { iso }
}

