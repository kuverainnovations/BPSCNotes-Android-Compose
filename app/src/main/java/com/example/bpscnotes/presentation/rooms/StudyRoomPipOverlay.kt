package com.example.bpscnotes.presentation.rooms

import com.example.bpscnotes.core.language.LocalStrings
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.bpscnotes.core.ui.t.BpscColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StudyRoomPipOverlay(
    isVisible:    Boolean,
    tierName:     String,
    tierEmoji:    String,
    tierColorHex: String?,
    startedAt:    String?,
    coinsEarned:  Int,
    onReturn:     () -> Unit,
    onEndSession: () -> Unit
) {
    if (!isVisible) return

    val str    = LocalStrings.current
    val density = LocalDensity.current
    val config  = LocalConfiguration.current

    val tierColor = remember(tierColorHex) {
        try { Color(android.graphics.Color.parseColor(tierColorHex ?: "#1565C0")) }
        catch (_: Exception) { Color(0xFF1565C0) }
    }

    // ── Timer ─────────────────────────────────────────────────
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(startedAt) {
        if (startedAt.isNullOrBlank()) return@LaunchedEffect
        val fmts = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        val startMs = fmts.firstNotNullOfOrNull { fmt ->
            try { SimpleDateFormat(fmt, Locale.getDefault())
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(startedAt)?.time
            } catch (_: Exception) { null }
        } ?: System.currentTimeMillis()

        while (true) {
            elapsedSeconds = ((System.currentTimeMillis() - startMs) / 1000L).toInt().coerceAtLeast(0)
            delay(1_000L)
        }
    }
    val h = elapsedSeconds / 3600
    val m = (elapsedSeconds % 3600) / 60
    val s = elapsedSeconds % 60
    val timeStr = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)

    // ── Pulse animation ────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "pip_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    // ── Card dimensions & screen bounds ──────────────────────
    val cardW  = 150.dp
    val cardH  = 80.dp
    val margin = 12.dp

    val screenWpx  = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHpx  = with(density) { config.screenHeightDp.dp.toPx() }
    val cardWpx    = with(density) { cardW.toPx() }
    val cardHpx    = with(density) { cardH.toPx() }
    val marginPx   = with(density) { margin.toPx() }
    val navPadPx   = with(density) { 96.dp.toPx() }
    val topPadPx   = with(density) { 100.dp.toPx() }

    // Position stored as dp offset from top-left of screen
    var offsetX by remember { mutableFloatStateOf(screenWpx - cardWpx - marginPx) }
    var offsetY by remember { mutableFloatStateOf(screenHpx - cardHpx - navPadPx) }

    fun snapToCorner() {
        val cx = offsetX + cardWpx / 2f
        val cy = offsetY + cardHpx / 2f
        offsetX = if (cx >= screenWpx / 2f) screenWpx - cardWpx - marginPx else marginPx
        offsetY = if (cy >= screenHpx / 2f) screenHpx - cardHpx - navPadPx else topPadPx
    }

    // ─────────────────────────────────────────────────────────
    // KEY FIX: Card is placed with Modifier.offset() inside
    // a Box that is ONLY the card's size (wrapContentSize),
    // NOT fillMaxSize. This means touches outside the card
    // are never even seen by this composable — they go straight
    // to the page behind, so scrolling works normally.
    // ─────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()           // <── needed for absolute positioning
            .wrapContentSize(Alignment.TopStart, unbounded = true)
            .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
            .width(cardW)
    ) {
        Surface(
            modifier       = Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(20.dp)),
            shape          = RoundedCornerShape(20.dp),
            color          = Color(0xFF0D1B4B),
            tonalElevation = 8.dp
        ) {
            Column(Modifier.padding(8.dp)) {

                // ── DRAG HANDLE — the ONLY element with pointerInput ──
                // All other touches on the card pass through normally
                // (buttons work, and the page behind can scroll freely).
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd    = { snapToCorner() },
                                onDragCancel = { snapToCorner() },
                                onDrag       = { _, drag ->
                                    offsetX = (offsetX + drag.x).coerceIn(0f, screenWpx - cardWpx)
                                    offsetY = (offsetY + drag.y).coerceIn(0f, screenHpx - cardHpx)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .width(32.dp).height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(0.3f))
                    )
                }

                Spacer(Modifier.height(3.dp))

                // ── Tier + stop button row ────────────────────────────
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape)
                            .background(tierColor.copy(0.2f))
                            .border(1.5.dp, tierColor.copy(pulseAlpha), CircleShape),
                        Alignment.Center
                    ) { Text(tierEmoji.ifBlank { "📚" }, fontSize = 16.sp) }

                    Column(Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(5.dp).clip(CircleShape)
                                    .background(Color(0xFF4CAF50).copy(pulseAlpha))
                            )
                            Text(
                                str.roomsLive,
                                style      = MaterialTheme.typography.labelSmall,
                                color      = Color(0xFF4CAF50),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 9.sp
                            )
                        }
                        Text(
                            tierName.ifBlank { str.roomsTitle },
                            style      = MaterialTheme.typography.labelMedium,
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines   = 1
                        )
                    }

                    Box(
                        Modifier.size(24.dp).clip(CircleShape)
                            .background(Color(0xFFEF5350).copy(0.18f))
                            .clickable(onClick = onEndSession),
                        Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Stop, null,
                            tint = Color(0xFFEF5350), modifier = Modifier.size(12.dp))
                    }
                }

                Spacer(Modifier.height(5.dp))

                // ── Timer + coins + Return ───────────────────────────
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "⏱ $timeStr",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = Color.White.copy(0.85f),
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.weight(1f)
                    )
                    Text(
                        "🪙 $coinsEarned",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = BpscColors.CoinGold,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        onClick = onReturn,
                        shape   = RoundedCornerShape(10.dp),
                        color   = tierColor
                    ) {
                        Text(
                            str.pipReturn,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White,
                            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}