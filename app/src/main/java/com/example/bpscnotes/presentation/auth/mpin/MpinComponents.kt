package com.example.bpscnotes.presentation.auth.mpin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.bpscnotes.core.ui.t.BpscColors

// ── 4 MPIN dots ───────────────────────────────────────────────
@Composable
fun MpinDots(digits: List<String>, hasError: Boolean = false) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        digits.forEach { d ->
            val filled = d.isNotEmpty()
            val dotColor by animateColorAsState(
                targetValue = when {
                    hasError -> Color(0xFFE74C3C)
                    filled   -> Color.White
                    else     -> Color.White.copy(0.3f)
                },
                animationSpec = spring(stiffness = Spring.StiffnessMedium), label = "dot"
            )
            val scale by animateFloatAsState(
                targetValue = if (filled) 1f else 0.85f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale"
            )
            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (filled) dotColor else Color.Transparent)
                    .border(
                        width = 2.5.dp,
                        color = dotColor,
                        shape = CircleShape
                    )
            )
        }
    }
}

// ── Custom numpad ─────────────────────────────────────────────
@Composable
fun MpinNumpad(
    enabled: Boolean = true,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    showBiometric: Boolean = false,
    onBiometric: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
    ) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(if (showBiometric) "BIO" else "", "0", "⌫")
        )
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { key ->
                    Box(modifier = Modifier.weight(1f)) {
                        when (key) {
                            "" -> { /* empty spacer */ }
                            "BIO" -> {
                                NumpadKey(
                                    label = "BIO",
                                    isBiometric = true,
                                    enabled = enabled && onBiometric != null,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onBiometric?.invoke()
                                    }
                                )
                            }
                            "⌫" -> {
                                NumpadKey(
                                    label = "⌫",
                                    isBackspace = true,
                                    enabled = enabled,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onBackspace()
                                    }
                                )
                            }
                            else -> {
                                NumpadKey(
                                    label = key,
                                    enabled = enabled,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onDigit(key)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumpadKey(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    isBackspace: Boolean = false,
    isBiometric: Boolean = false
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "keyScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    !enabled    -> Color.White.copy(0.05f)
                    isBackspace -> Color.White.copy(0.08f)
                    isBiometric -> Color.White.copy(0.08f)
                    else        -> Color.White.copy(0.12f)
                }
            )
            .clickable(enabled = enabled) {
                pressed = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (isBackspace) {
            Icon(
                Icons.Rounded.Backspace,
                contentDescription = "Delete",
                tint = if (enabled) Color.White else Color.White.copy(0.3f),
                modifier = Modifier.size(22.dp)
            )
        } else if (isBiometric) {
            Text(
                "👆",
                fontSize = 24.sp,
                color = if (enabled) Color.White else Color.White.copy(0.3f)
            )
        } else {
            Text(
                label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = if (enabled) Color.White else Color.White.copy(0.3f)
            )
        }
    }

    // Reset press state
    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(100)
            pressed = false
        }
    }
}