package com.example.bpscnotes.core.ui.t

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Fixed/semantic colors (same in light & dark) ──────────────
object BpscPalette {
    val Blue500     = Color(0xFF1A6FE8)
    val Blue700     = Color(0xFF0D47A1)
    val Blue100     = Color(0xFFE8F0FD)
    val Orange500   = Color(0xFFFF6B35)
    val Orange100   = Color(0xFFFFF0EA)
    val Green500    = Color(0xFF2ECC71)
    val Amber500    = Color(0xFFF39C12)
    val Gold        = Color(0xFFFFB800)
    val Red500      = Color(0xFFE53935)
    // Dark palette
    val Dark900     = Color(0xFF0D1117)
    val Dark800     = Color(0xFF161B22)
    val Dark700     = Color(0xFF1C2128)
    val Dark600     = Color(0xFF21262D)
    val Dark400     = Color(0xFF30363D)
    val DarkText    = Color(0xFFE6EDF3)
    val DarkHint    = Color(0xFF8B949E)
    val DarkBlue    = Color(0xFF58A6FF)
    val DarkOrange  = Color(0xFFFF8C61)
}

// ── Theme-aware color set ─────────────────────────────────────
data class BpscColorSet(
    val Primary:       Color,
    val PrimaryLight:  Color,
    val Accent:        Color,
    val AccentLight:   Color,
    val Success:       Color,
    val Warning:       Color,
    val Surface:       Color,       // page background
    val CardBg:        Color,       // card / sheet surface
    val CardBg2:       Color,       // slightly elevated card
    val TextPrimary:   Color,
    val TextSecondary: Color,
    val TextHint:      Color,
    val Divider:       Color,
    val CoinGold:      Color,
    val isDark:        Boolean,
)

val LightColorSet = BpscColorSet(
    Primary       = BpscPalette.Blue500,
    PrimaryLight  = BpscPalette.Blue100,
    Accent        = BpscPalette.Orange500,
    AccentLight   = BpscPalette.Orange100,
    Success       = BpscPalette.Green500,
    Warning       = BpscPalette.Amber500,
    Surface       = Color(0xFFF2F4F8),
    CardBg        = Color.White,
    CardBg2       = Color(0xFFF6F8FC),
    TextPrimary   = Color(0xFF1A1D2E),
    TextSecondary = Color(0xFF6B7280),
    TextHint      = Color(0xFFB0B7C3),
    Divider       = Color(0xFFEEF0F5),
    CoinGold      = BpscPalette.Gold,
    isDark        = false,
)

val DarkColorSet = BpscColorSet(
    Primary       = BpscPalette.DarkBlue,
    PrimaryLight  = BpscPalette.Dark700,
    Accent        = BpscPalette.DarkOrange,
    AccentLight   = Color(0xFF2D1F17),
    Success       = Color(0xFF3EE08A),
    Warning       = Color(0xFFFFBC30),
    Surface       = BpscPalette.Dark900,
    CardBg        = BpscPalette.Dark800,
    CardBg2       = BpscPalette.Dark700,
    TextPrimary   = BpscPalette.DarkText,
    TextSecondary = BpscPalette.DarkHint,
    TextHint      = Color(0xFF6E7681),
    Divider       = BpscPalette.Dark400,
    CoinGold      = BpscPalette.Gold,
    isDark        = true,
)

// CompositionLocal — read with BpscTheme.colors in any composable
val LocalBpscColors = staticCompositionLocalOf { LightColorSet }

// Convenience accessor
object BpscColors {
    // These are kept for backward compat — they point to light values
    // Screens should use MaterialTheme.colorScheme or LocalBpscColors.current instead
    val Primary         = BpscPalette.Blue500
    val PrimaryLight    = BpscPalette.Blue100
    val Accent          = BpscPalette.Orange500
    val AccentLight     = BpscPalette.Orange100
    val Success         = BpscPalette.Green500
    val Warning         = BpscPalette.Amber500
    val Surface         = Color(0xFFF2F4F8)
    val CardBg          = Color.White
    val TextPrimary     = Color(0xFF1A1D2E)
    val TextSecondary   = Color(0xFF6B7280)
    val TextHint        = Color(0xFFB0B7C3)
    val Divider         = Color(0xFFEEF0F5)
    val CoinGold        = BpscPalette.Gold
}
