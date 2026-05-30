package com.example.bpscnotes.presentation.rooms

import com.example.bpscnotes.core.language.LocalStrings
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.bpscnotes.core.ui.t.BpscColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StudyRoomPipOverlay(
    isVisible:     Boolean,
    tierName:      String,
    tierEmoji:     String,
    tierColorHex:  String?,
    startedAt:     String?,
    coinsEarned:   Int,
    onReturn:      () -> Unit,
    onEndSession:  () -> Unit
) {
    val str = LocalStrings.current
    val density = LocalDensity.current
    val tierColor = remember(tierColorHex) {
        try { Color(android.graphics.Color.parseColor(tierColorHex ?: "#1565C0")) }
        catch (_: Exception) { Color(0xFF1565C0) }
    }

    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(startedAt) {
        if (startedAt.isNullOrBlank()) return@LaunchedEffect
        val sdf  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val sdf2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
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

    val infiniteTransition = rememberInfiniteTransition(label = "pip_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    // Corner-snapping drag state
    // 0=top-left 1=top-right 2=bottom-left 3=bottom-right (default)
    var corner by remember { mutableIntStateOf(3) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var parentSize by remember { mutableStateOf(IntSize.Zero) }

    AnimatedVisibility(
        visible = isVisible,
        enter   = scaleIn(initialScale = 0.6f) + fadeIn(),
        exit    = scaleOut(targetScale = 0.6f) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { parentSize = it.size }
        ) {
            // Compute base position from current corner
            val pipW = with(density) { 168.dp.toPx() }
            val pipH = with(density) { 82.dp.toPx() }
            val margin = with(density) { 12.dp.toPx() }
            val bottomPad = with(density) { 96.dp.toPx() }  // above nav bar

            val baseX = when (corner) {
                0, 2 -> margin
                else -> parentSize.width - pipW - margin
            }
            val baseY = when (corner) {
                0, 1 -> with(density) { 100.dp.toPx() }   // top (below status bar)
                else -> parentSize.height - pipH - bottomPad
            }

            val finalX = (baseX + dragOffset.x).coerceIn(0f, (parentSize.width - pipW).coerceAtLeast(0f))
            val finalY = (baseY + dragOffset.y).coerceIn(0f, (parentSize.height - pipH).coerceAtLeast(0f))

            Box(
                modifier = Modifier
                    .offset { IntOffset(finalX.toInt(), finalY.toInt()) }
                    .width(168.dp)
                    .pointerInput(corner) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd   = {
                                isDragging = false
                                // Snap to nearest corner
                                val cx = finalX + pipW / 2
                                val cy = finalY + pipH / 2
                                corner = when {
                                    cx < parentSize.width / 2 && cy < parentSize.height / 2 -> 0
                                    cx >= parentSize.width / 2 && cy < parentSize.height / 2 -> 1
                                    cx < parentSize.width / 2 -> 2
                                    else -> 3
                                }
                                dragOffset = Offset.Zero
                            },
                            onDragCancel = { isDragging = false; dragOffset = Offset.Zero },
                            onDrag       = { _, drag -> dragOffset += drag }
                        )
                    }
            ) {
                Surface(
                    modifier       = Modifier.fillMaxWidth().shadow(if (isDragging) 20.dp else 10.dp, RoundedCornerShape(20.dp)),
                    shape          = RoundedCornerShape(20.dp),
                    color          = Color(0xFF0D1B4B),
                    tonalElevation = 8.dp
                ) {
                    Column(Modifier.padding(10.dp)) {
                        // Drag handle
                        Box(Modifier.width(32.dp).height(3.dp).clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(0.25f)).align(Alignment.CenterHorizontally))
                        Spacer(Modifier.height(8.dp))

                        // Tier + timer row
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier.size(34.dp).clip(CircleShape)
                                    .background(tierColor.copy(0.2f))
                                    .border(1.5.dp, tierColor.copy(pulseAlpha), CircleShape),
                                Alignment.Center
                            ) { Text(tierEmoji.ifBlank { "📚" }, fontSize = 16.sp) }

                            Column(Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF4CAF50).copy(pulseAlpha)))
                                    Text("LIVE", style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF4CAF50), fontWeight = FontWeight.ExtraBold, fontSize = 9.sp)
                                }
                                Text(tierName.ifBlank { str.roomsTitle },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            // End session
                            Box(Modifier.size(26.dp).clip(CircleShape).background(Color(0xFFEF5350).copy(0.2f))
                                .clickable(onClick = onEndSession), Alignment.Center) {
                                Icon(Icons.Rounded.Stop, null, tint = Color(0xFFEF5350), modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Timer + coins + return button
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("⏱ $timeStr", style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(0.85f), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("🪙 $coinsEarned", style = MaterialTheme.typography.labelSmall,
                                color = BpscColors.CoinGold, fontWeight = FontWeight.Bold)
                            Surface(
                                onClick = onReturn,
                                shape   = RoundedCornerShape(10.dp),
                                color   = tierColor
                            ) {
                                Text(str.pipReturn, style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold, color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}