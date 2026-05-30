package com.example.bpscnotes.core.ui.t

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// ════════════════════════════════════════════════════════════
// Light colour scheme
// ════════════════════════════════════════════════════════════
private val LightColors = lightColorScheme(
    primary          = BpscColors.Primary,
    secondary        = BpscColors.Accent,
    background       = Color(0xFFF2F4F8),
    surface          = Color.White,
    surfaceVariant   = Color(0xFFEEF2F7),
    onPrimary        = Color.White,
    onSecondary      = Color.White,
    onBackground     = Color(0xFF1A1A2E),
    onSurface        = Color(0xFF1A1A2E),
    onSurfaceVariant = Color(0xFF6B7280),
    error            = Color(0xFFE53935),
    onError          = Color.White,
    outline          = Color(0xFFE5E7EB),
)

// ════════════════════════════════════════════════════════════
// Dark colour scheme
// ════════════════════════════════════════════════════════════
private val DarkColors = darkColorScheme(
    primary          = Color(0xFF90CAF9),
    secondary        = Color(0xFF80CBC4),
    background       = Color(0xFF0D1117),
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

// Provide dark mode state to whole composition tree
val LocalDarkMode = compositionLocalOf { false }

@Composable
fun BPSCNotesTheme(
    darkMode: Boolean = false,
    language: com.example.bpscnotes.core.language.AppLanguage =
        com.example.bpscnotes.core.language.AppLanguage.ENGLISH,
    content: @Composable () -> Unit
) {
    val strings = when (language) {
        com.example.bpscnotes.core.language.AppLanguage.HINDI ->
            com.example.bpscnotes.core.language.HindiStrings
        else -> com.example.bpscnotes.core.language.EnglishStrings
    }
    val colors = if (darkMode) DarkColors else LightColors

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            androidx.core.view.WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkMode
        }
    }

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

@Composable
fun BpscNotesTheme(content: @Composable () -> Unit) = BPSCNotesTheme(content = content)