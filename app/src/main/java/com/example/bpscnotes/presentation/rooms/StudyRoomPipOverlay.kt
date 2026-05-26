package com.example.bpscnotes.presentation.rooms

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.bpscnotes.core.ui.t.BpscColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// ════════════════════════════════════════════════════════════
// STUDY ROOM PIP OVERLAY
//
// Timer computed from startedAt ISO timestamp — no dependency
// on ViewModel timer state. Always accurate, never resets.
// ════════════════════════════════════════════════════════════

@Composable
fun StudyRoomPipOverlay(
    isVisible:     Boolean,
    tierName:      String,
    tierEmoji:     String,
    tierColorHex:  String?,
    startedAt:     String?,   // ISO-8601 e.g. "2026-05-25T10:30:00.000Z"
    coinsEarned:   Int,
    onReturn:      () -> Unit,
    onEndSession:  () -> Unit
) {
    val tierColor = remember(tierColorHex) {
        try { Color(android.graphics.Color.parseColor(tierColorHex ?: "#1565C0")) }
        catch (_: Exception) { Color(0xFF1565C0) }
    }

    // ── Compute elapsed seconds directly from startedAt timestamp ──
    // This is the ground truth — independent of any ViewModel timer state.
    // It keeps running even if the ViewModel is re-created or timer resets.
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(startedAt) {
        if (startedAt.isNullOrBlank()) return@LaunchedEffect
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val sdf2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val startMs = try { sdf.parse(startedAt)?.time ?: sdf2.parse(startedAt)?.time ?: System.currentTimeMillis() }
        catch (_: Exception) { System.currentTimeMillis() }

        while (true) {
            elapsedSeconds = ((System.currentTimeMillis() - startMs) / 1000L).toInt().coerceAtLeast(0)
            delay(1_000L)
        }
    }

    val h = elapsedSeconds / 3600
    val m = (elapsedSeconds % 3600) / 60
    val s = elapsedSeconds % 60
    val timeStr = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)

    // Pulsing live indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pip_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    var offsetY by remember { mutableFloatStateOf(0f) }

    AnimatedVisibility(
        visible = isVisible,
        enter   = slideInVertically { it } + fadeIn(),
        exit    = slideOutVertically { it } + fadeOut()
    ) {
        Box(
            modifier         = Modifier.fillMaxSize().padding(bottom = 88.dp, start = 12.dp, end = 12.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, offsetY.toInt().coerceIn(-200, 0)) }
                    .pointerInput(Unit) {
                        detectDragGestures { _, drag -> offsetY = (offsetY + drag.y).coerceIn(-200f, 0f) }
                    }
            ) {
                Surface(
                    modifier       = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(20.dp)),
                    shape          = RoundedCornerShape(20.dp),
                    color          = Color(0xFF0D1B4B),
                    tonalElevation = 8.dp
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Box(
                            Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(0.25f)).align(Alignment.CenterHorizontally)
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape)
                                    .background(tierColor.copy(0.2f))
                                    .border(1.5.dp, tierColor.copy(pulseAlpha), CircleShape),
                                Alignment.Center
                            ) { Text(tierEmoji.ifBlank { "📚" }, fontSize = 20.sp) }

                            Column(Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4CAF50).copy(pulseAlpha)))
                                    Text("Session Active", style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                }
                                Text(tierName.ifBlank { "Study Room" },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("⏱ $timeStr",
                                        style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.85f),
                                        fontWeight = FontWeight.Bold)
                                    Text("🪙 $coinsEarned",
                                        style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold)
                                }
                            }

                            Button(
                                onClick        = onReturn,
                                shape          = RoundedCornerShape(12.dp),
                                colors         = ButtonDefaults.buttonColors(containerColor = tierColor),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Return", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }

                            IconButton(onClick = onEndSession, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.Stop, null, tint = Color(0xFFEF5350), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}