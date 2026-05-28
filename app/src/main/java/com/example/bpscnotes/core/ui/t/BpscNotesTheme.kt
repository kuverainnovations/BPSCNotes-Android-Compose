package com.example.bpscnotes.core.ui.t

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// ════════════════════════════════════════════════════════════
// Light colour scheme — the default BPSC design
// ════════════════════════════════════════════════════════════
private val LightColors = lightColorScheme(
    primary          = BpscColors.Primary,
    secondary        = BpscColors.Accent,
    background       = BpscColors.Surface,
    surface          = BpscColors.CardBg,
    surfaceVariant   = BpscColors.Surface,
    onPrimary        = Color.White,
    onSecondary      = Color.White,
    onBackground     = BpscColors.TextPrimary,
    onSurface        = BpscColors.TextPrimary,
    onSurfaceVariant = BpscColors.TextSecondary,
    error            = Color(0xFFE53935),
    onError          = Color.White,
    outline          = BpscColors.Divider,
)

// ════════════════════════════════════════════════════════════
// Dark colour scheme — used when user toggles dark mode in Settings
// ════════════════════════════════════════════════════════════
private val DarkColors = darkColorScheme(
    primary          = Color(0xFF90CAF9),    // lighter blue on dark bg
    secondary        = Color(0xFF80CBC4),
    background       = Color(0xFF0D1117),    // GitHub-dark style bg
    surface          = Color(0xFF161B22),
    surfaceVariant   = Color(0xFF1C2128),
    onPrimary        = Color(0xFF0D47A1),
    onSecondary      = Color(0xFF00251A),
    onBackground     = Color(0xFFE6EDF3),
    onSurface        = Color(0xFFE6EDF3),
    onSurfaceVariant = Color(0xFF8B949E),
    error            = Color(0xFFFF6B6B),
    onError          = Color(0xFF1C0002),
    outline          = Color(0xFF30363D),
)

// ════════════════════════════════════════════════════════════
// Global dark mode state — a CompositionLocal so any composable
// in the tree can read the current dark mode value without
// passing it down manually through every function.
// ════════════════════════════════════════════════════════════
val LocalDarkMode = compositionLocalOf { false }

@Composable
fun BPSCNotesTheme(
    darkMode:  Boolean = false,          // ← read from SettingsViewModel in MainActivity
    language:  com.example.bpscnotes.core.language.AppLanguage = com.example.bpscnotes.core.language.AppLanguage.ENGLISH,
    content:   @Composable () -> Unit
) {
    val strings = when (language) {
        com.example.bpscnotes.core.language.AppLanguage.HINDI -> com.example.bpscnotes.core.language.HindiStrings
        else -> com.example.bpscnotes.core.language.EnglishStrings
    }
    val colors = if (/*darkMode*/false) DarkColors else LightColors

    // Keep status bar icons correct for both modes
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            // Light status bar icons on dark header (blue/dark); always false for our design
            androidx.core.view.WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = darkMode  // dark mode → light icons on status bar
        }
    }

    // Provide dark mode AND language strings to the whole composition tree
    CompositionLocalProvider(
        LocalDarkMode provides darkMode,
        com.example.bpscnotes.core.language.LocalStrings provides strings
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography  = BpscTypography,
            content     = content
        )
    }
}

// Backward-compat alias — any old code calling BpscNotesTheme still compiles
@Composable
fun BpscNotesTheme(content: @Composable () -> Unit) = BPSCNotesTheme(content = content)
