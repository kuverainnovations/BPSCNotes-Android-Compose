package com.example.bpscnotes.core.ui.t

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary         = BpscColors.Primary,
    secondary       = BpscColors.Accent,
    background      = BpscColors.Surface,
    surface         = BpscColors.CardBg,
    onPrimary       = Color.White,
    onSecondary     = Color.White,
    onBackground    = BpscColors.TextPrimary,
    onSurface       = BpscColors.TextPrimary,
)

private val DarkColorScheme = darkColorScheme(
    primary         = BpscColors.Primary,
    secondary       = BpscColors.Accent,
    background      = Color(0xFF121212),
    surface         = Color(0xFF1E1E1E),
    onPrimary       = Color.White,
    onSecondary     = Color.White,
    onBackground    = Color.White,
    onSurface       = Color.White,
)

@Composable
fun BPSCNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = BpscTypography,
        content     = content
    )
}