package com.example.bpscnotes.core.ui.t

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun BpscNotesTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = BpscColors.Primary,
        secondary = BpscColors.Accent,
        background = BpscColors.Surface,
        surface = BpscColors.CardBg,
        onPrimary = Color.White,
        onBackground = BpscColors.TextPrimary,
        onSurface = BpscColors.TextPrimary,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = BpscTypography,
        content = content
    )
}