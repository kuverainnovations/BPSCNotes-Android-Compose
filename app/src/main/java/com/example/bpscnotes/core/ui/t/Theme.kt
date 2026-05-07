package com.example.bpscnotes.core.ui.t

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─────────────────────────────────────────────────────────────
// BPSCNotes uses a fixed light design system.
// The UI is built entirely with BpscColors (all light values).
// Forcing light mode prevents dark-mode system settings from
// making text invisible (e.g. dark text on dark background in
// OutlinedTextField, BasicTextField, Column backgrounds).
// ─────────────────────────────────────────────────────────────
private val AppColorScheme = lightColorScheme(
    primary          = BpscColors.Primary,
    secondary        = BpscColors.Accent,
    background       = BpscColors.Surface,
    surface          = BpscColors.CardBg,
    onPrimary        = Color.White,
    onSecondary      = Color.White,
    onBackground     = BpscColors.TextPrimary,
    onSurface        = BpscColors.TextPrimary,
    error            = Color(0xFFE53935),
    onError          = Color.White,
)

@Composable
fun BPSCNotesTheme(content: @Composable () -> Unit) {
    // Status bar: make icons dark (since our status bar area is blue,
    // we want light icons — handled per-screen via systemBarsPadding.
    // Here we just ensure the window decoration fits our design.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            // Draw behind status bar (edge-to-edge already set in MainActivity)
            // Set status bar icon color: false = light icons (for our blue header)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = BpscTypography,
        content     = content
    )
}

// Keep BpscNotesTheme as alias so any legacy references compile
//@Composable
//fun BpscNotesTheme(content: @Composable () -> Unit) = BPSCNotesTheme(content)
