package com.example.bpscnotes.core.ui

import com.example.bpscnotes.core.language.LocalStrings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bpscnotes.core.ui.t.BpscColors

// ─────────────────────────────────────────────────────────────
// AppLoader — uniform circular loader with optional message
// ─────────────────────────────────────────────────────────────
@Composable
fun AppLoader(
    message: String = "",
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier.background(cs.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AnimatedBookLoader()
            if (message.isNotBlank()) {
                Text(
                    message,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = BpscColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// AppFullScreenLoader — overlay card style loader
// ─────────────────────────────────────────────────────────────
@Composable
fun AppFullScreenLoader(message: String = "Loading…") {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = cs.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp, 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedBookLoader(size = 44.dp)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// AnimatedBookLoader — flipping book pages animation
// ─────────────────────────────────────────────────────────────
@Composable
fun AnimatedBookLoader(size: Dp = 52.dp) {
    // Page flip: 0f = flat open, 1f = page fully turned
    val infiniteTransition = rememberInfiniteTransition(label = "book")
    val pageFlip by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Restart
        ),
        label = "pageFlip"
    )
    val spineGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spineGlow"
    )

    Canvas(modifier = Modifier.size(size)) {
        val w   = size.toPx()
        val h   = size.toPx()
        val mid = w / 2f
        val bookH  = h * 0.72f
        val bookTop = (h - bookH) / 2f

        // ── Back cover (right side) ──
        drawRect(
            color    = BpscColors.PrimaryLight,
            topLeft  = androidx.compose.ui.geometry.Offset(mid, bookTop),
            size     = androidx.compose.ui.geometry.Size(mid - 2.dp.toPx(), bookH)
        )
        // ── Left page (static) ──
        drawRect(
            color    = Color.White,
            topLeft  = androidx.compose.ui.geometry.Offset(2.dp.toPx(), bookTop + 2.dp.toPx()),
            size     = androidx.compose.ui.geometry.Size(mid - 4.dp.toPx(), bookH - 4.dp.toPx())
        )
        // ── Lines on left page ──
        val lineColor = BpscColors.Primary.copy(0.15f)
        val lineX     = 6.dp.toPx()
        val lineEnd   = mid - 6.dp.toPx()
        for (i in 1..4) {
            val ly = bookTop + bookH * (i / 5.5f)
            drawLine(lineColor, androidx.compose.ui.geometry.Offset(lineX, ly),
                androidx.compose.ui.geometry.Offset(lineEnd, ly), 1.5.dp.toPx())
        }
        // ── Turning page (skew based on pageFlip) ──
        val pageW   = (mid - 4.dp.toPx()) * (1f - pageFlip * 2f).coerceIn(-1f, 1f).let { Math.abs(it.toDouble()).toFloat() }
        val pageColor = androidx.compose.ui.graphics.lerp(Color.White, BpscColors.PrimaryLight, pageFlip * 0.3f)
        if (pageW > 1f) {
            val startX = if (pageFlip < 0.5f) mid - 2.dp.toPx() - pageW else mid + 2.dp.toPx()
            drawRect(
                color   = pageColor,
                topLeft = androidx.compose.ui.geometry.Offset(startX, bookTop + 2.dp.toPx()),
                size    = androidx.compose.ui.geometry.Size(pageW, bookH - 4.dp.toPx())
            )
        }
        // ── Spine ──
        drawRect(
            color    = BpscColors.Primary.copy(spineGlow),
            topLeft  = androidx.compose.ui.geometry.Offset(mid - 2.dp.toPx(), bookTop),
            size     = androidx.compose.ui.geometry.Size(4.dp.toPx(), bookH)
        )
        // ── Cover outline ──
        drawRoundRect(
            color       = BpscColors.Primary.copy(0.3f),
            topLeft     = androidx.compose.ui.geometry.Offset(2.dp.toPx(), bookTop),
            size        = androidx.compose.ui.geometry.Size(w - 4.dp.toPx(), bookH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            style        = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())
        )
    }
}

// ─────────────────────────────────────────────────────────────
// AppEmptyState — uniform empty state with emoji, title, body, action
// ─────────────────────────────────────────────────────────────
@Composable
fun AppEmptyState(
    emoji:       String  = "📭",
    title:       String,
    body:        String  = "",
    actionLabel: String  = "",
    onAction:    (() -> Unit)? = null,
    modifier:    Modifier = Modifier.fillMaxSize()
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Text(emoji, fontSize = 52.sp)
            Text(
                title,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = BpscColors.TextPrimary,
                textAlign  = TextAlign.Center
            )
            if (body.isNotBlank()) {
                Text(
                    body,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = BpscColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
            if (actionLabel.isNotBlank() && onAction != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    colors  = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                    shape   = RoundedCornerShape(12.dp)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// AppQuitDialog — uniform quit confirmation for Quiz / MockTest / MCQ
// ─────────────────────────────────────────────────────────────
@Composable
fun AppQuitDialog(
    title:     String = "Quit?",
    body:      String = "Your progress will be lost.",
    quitLabel: String = "Yes, Quit",
    keepLabel: String = "Keep Going",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(20.dp),
        containerColor   = Color.White,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("⚠️", fontSize = 26.sp)
                Text(title, fontWeight = FontWeight.ExtraBold, color = cs.onSurface)
            }
        },
        text = {
            Text(body, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                shape   = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Rounded.ExitToApp, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(quitLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text(keepLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────
// AppErrorDialog — uniform error dialog with optional retry + dismiss
// Use for transient errors (submit failed, questions failed to load)
// ─────────────────────────────────────────────────────────────
@Composable
fun AppErrorDialog(
    title:        String  = "",
    message:      String,
    retryLabel:   String  = "",
    dismissLabel: String  = "",
    onRetry:      (() -> Unit)? = null,
    onDismiss:    () -> Unit
) {
    val cs  = MaterialTheme.colorScheme
    val str = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(20.dp),
        containerColor   = Color.White,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("⚠️", fontSize = 26.sp)
                Text(
                    title.ifBlank { str.uiSomethingWrong },
                    fontWeight = FontWeight.ExtraBold,
                    color      = cs.onSurface
                )
            }
        },
        text = {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )
        },
        confirmButton = {
            if (onRetry != null) {
                Button(
                    onClick = onRetry,
                    colors  = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                    shape   = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(retryLabel.ifBlank { str.uiTryAgain }, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text(dismissLabel.ifBlank { str.back }, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────
// AppInfoScreen — full-screen info state (scheduled quiz, no access, etc.)
// Use for non-error states that block navigation (not really a crash)
// ─────────────────────────────────────────────────────────────
@Composable
fun AppInfoScreen(
    emoji:        String,
    title:        String,
    message:      String,
    actionLabel:  String  = "",
    onAction:     (() -> Unit)? = null,
    modifier:     Modifier = Modifier.fillMaxSize()
) {
    val cs  = MaterialTheme.colorScheme
    val str = LocalStrings.current
    Box(modifier = modifier.background(cs.background), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(emoji, fontSize = 52.sp)
            Text(
                title,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = BpscColors.TextPrimary,
                textAlign  = TextAlign.Center
            )
            Text(
                message,
                style     = MaterialTheme.typography.bodyMedium,
                color     = cs.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel.isNotBlank() && onAction != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    colors  = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                    shape   = RoundedCornerShape(12.dp)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
@Composable
fun AppErrorState(
    message:         String,
    onRetry:         (() -> Unit)? = null,
    modifier:        Modifier = Modifier.fillMaxSize(),
    secondaryAction: (@Composable () -> Unit)? = null
) {
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("⚠️", fontSize = 44.sp)
            Text(
                str.uiSomethingWrong,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = BpscColors.TextPrimary,
                textAlign  = TextAlign.Center
            )
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
            if (onRetry != null) {
                if (secondaryAction != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        secondaryAction()
                        Button(
                            onClick = onRetry,
                            colors  = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                            shape   = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(str.uiTryAgain, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Button(
                        onClick = onRetry,
                        colors  = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                        shape   = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(str.uiTryAgain, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// BpscDropdown — on-brand dropdown replacing ExposedDropdownMenuBox
//
// Usage (simple):
//   BpscDropdown(value=selectedSubject, label="Subject", options=list, onSelect={})
//
// Usage (with leading icon):
//   BpscDropdown(..., leadingIcon={ Icon(Icons.Rounded.LocationOn, null) })
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BpscDropdown(
    value:       String,
    label:       String,
    options:     List<String>,
    onSelect:    (String) -> Unit,
    modifier:    Modifier = Modifier,
    placeholder: String   = "",
    enabled:     Boolean  = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val displayValue = value.ifBlank { placeholder }
    val isEmpty = value.isBlank()

    ExposedDropdownMenuBox(
        expanded         = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier         = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (expanded) BpscColors.Primary.copy(0.06f)
                    else          Color(0xFFF7F8FA)
                )
                .border(
                    width = if (expanded) 1.5.dp else 1.dp,
                    color = if (expanded) BpscColors.Primary else Color(0xFFE2E5EC),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (leadingIcon != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                                if (expanded) BpscColors.Primary else BpscColors.TextSecondary
                    ) {
                        Box(Modifier.size(20.dp)) { leadingIcon() }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (expanded) BpscColors.Primary else BpscColors.TextSecondary
                    )
                    Text(
                        text  = displayValue,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isEmpty) FontWeight.Normal else FontWeight.SemiBold
                        ),
                        color = if (isEmpty) BpscColors.TextHint else BpscColors.TextPrimary
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp
                    else         Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint     = if (expanded) BpscColors.Primary else BpscColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        ExposedDropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            modifier         = Modifier
                .exposedDropdownSize()
                .background(Color.White)
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = option == value
                DropdownMenuItem(
                    text = {
                        Text(
                            text  = option,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) BpscColors.Primary else BpscColors.TextPrimary
                        )
                    },
                    onClick = { onSelect(option); expanded = false },
                    leadingIcon = if (isSelected) ({
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(BpscColors.Primary)
                        )
                    }) else null,
                    modifier = Modifier.background(
                        if (isSelected) BpscColors.Primary.copy(0.07f) else Color.Transparent
                    ),
                    colors = MenuDefaults.itemColors(textColor = BpscColors.TextPrimary)
                )
                if (index < options.lastIndex) {
                    HorizontalDivider(
                        color     = Color(0xFFF2F4F8),
                        thickness = 0.5.dp,
                        modifier  = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}