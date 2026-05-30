package com.example.bpscnotes.core.ui.t

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// ════════════════════════════════════════════════════════════
// Light scheme (used by Material3 components)
// ════════════════════════════════════════════════════════════
private val LightColors = lightColorScheme(
    primary          = BpscPalette.Blue500,
    secondary        = BpscPalette.Orange500,
    background       = Color(0xFFF2F4F8),
    surface          = Color.White,
    surfaceVariant   = Color(0xFFEEF2F7),
    onPrimary        = Color.White,
    onSecondary      = Color.White,
    onBackground     = Color(0xFF1A1D2E),
    onSurface        = Color(0xFF1A1D2E),
    onSurfaceVariant = Color(0xFF6B7280),
    error            = Color(0xFFE53935),
    onError          = Color.White,
    outline          = Color(0xFFE5E7EB),
)

// ════════════════════════════════════════════════════════════
// Dark scheme (GitHub-inspired deep dark)
// ════════════════════════════════════════════════════════════
private val DarkColors = darkColorScheme(
    primary          = BpscPalette.DarkBlue,
    secondary        = BpscPalette.DarkOrange,
    background       = BpscPalette.Dark900,
    surface          = BpscPalette.Dark800,
    surfaceVariant   = BpscPalette.Dark700,
    onPrimary        = BpscPalette.Dark900,
    onSecondary      = BpscPalette.Dark900,
    onBackground     = BpscPalette.DarkText,
    onSurface        = BpscPalette.DarkText,
    onSurfaceVariant = BpscPalette.DarkHint,
    error            = Color(0xFFFF6B6B),
    onError          = BpscPalette.Dark900,
    outline          = BpscPalette.Dark400,
)

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
    val colors  = if (darkMode) DarkColors  else LightColors
    val bpscSet = if (darkMode) DarkColorSet else LightColorSet

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            androidx.core.view.WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkMode
            // Tint navigation bar to match theme
            window.navigationBarColor = if (darkMode)
                android.graphics.Color.parseColor("#161B22")
            else
                android.graphics.Color.TRANSPARENT
        }
    }

    CompositionLocalProvider(
        LocalDarkMode        provides darkMode,
        LocalBpscColors      provides bpscSet,
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
