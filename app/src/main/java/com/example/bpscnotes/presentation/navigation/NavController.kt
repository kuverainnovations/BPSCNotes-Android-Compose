package com.example.bpscnotes.presentation.navigation

import androidx.navigation.NavController

// Global debounce timestamp for all in-app back navigation.
// Prevents rapid double-tap on back arrows from popping two entries.
private var lastPopMs = 0L

fun NavController.popBackStackSafe(): Boolean {
    val now = System.currentTimeMillis()
    if (lastPopMs != 0L && now - lastPopMs < 800L) {
        return false // debounced
    }
    lastPopMs = now
    return popBackStack()
}