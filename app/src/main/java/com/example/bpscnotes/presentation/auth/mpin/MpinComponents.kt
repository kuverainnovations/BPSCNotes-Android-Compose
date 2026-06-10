package com.example.bpscnotes.presentation.auth.mpin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.bpscnotes.core.ui.t.BpscColors

// ── 6 MPIN dots ──────────────────────────────────────────────
@Composable
fun MpinDots(digits: List<String>, hasError: Boolean = false) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        digits.forEach { d ->
            val filled    = d.isNotEmpty()
            val dotColor by animateColorAsState(
                targetValue = when {
                    hasError -> MaterialTheme.colorScheme.error
                    filled   -> BpscColors.Primary
                    else     -> MaterialTheme.colorScheme.outline
                },
                animationSpec = spring(), label = "dot"
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (filled) dotColor else Color.Transparent)
                    .border(
                        width = if (filled) 0.dp else 2.dp,
                        color = dotColor,
                        shape = CircleShape
                    )
            )
        }
    }
}

// ── Custom numpad (no system keyboard) ───────────────────────
@Composable
fun MpinNumpad(
    enabled: Boolean = true,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 40.dp, vertical = 8.dp)
    ) {
        val rows = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","⌫"))
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Box(modifier = Modifier.weight(1f)) {
                        when (key) {
                            ""  -> { /* empty cell — spacer */ }
                            "⌫" -> {
                                NumpadKey(label = key, enabled = enabled, onClick = onBackspace,
                                    containerColor = cs.errorContainer, contentColor = cs.error)
                            }
                            else -> {
                                NumpadKey(label = key, enabled = enabled,
                                    onClick = { onDigit(key) })
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
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) containerColor else containerColor.copy(0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium, color = if (enabled) contentColor else contentColor.copy(0.4f))
    }
}
