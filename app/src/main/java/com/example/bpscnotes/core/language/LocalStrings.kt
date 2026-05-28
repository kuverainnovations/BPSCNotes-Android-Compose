package com.example.bpscnotes.core.language

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal that provides the current [AppStrings] throughout the Compose tree.
 * Access with: val str = LocalStrings.current
 */
val LocalStrings = staticCompositionLocalOf<AppStrings> { EnglishStrings }
