package com.example.bpscnotes.core.ui.components

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

/**
 * Tracks elapsed wall-clock time during an active quiz/test and detects background periods.
 *
 * @param onTick       Called every second with total elapsed seconds.
 * @param onBackground Called when the app goes to background (onStop) with the number of
 *                     background seconds accumulated in that trip. The caller should add
 *                     these to a running backgroundSecs counter via ViewModel.addBackgroundSecs().
 */
@Composable
fun AppTimer(
    onTick: (elapsedSecs: Int) -> Unit = {},
    onBackground: (backgroundSecs: Int) -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var backgroundStartMs by remember { mutableLongStateOf(0L) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    backgroundStartMs = System.currentTimeMillis()
                }
                Lifecycle.Event.ON_START -> {
                    if (backgroundStartMs > 0L) {
                        val bgSecs = ((System.currentTimeMillis() - backgroundStartMs) / 1000).toInt()
                        if (bgSecs > 0) onBackground(bgSecs)
                        backgroundStartMs = 0L
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var elapsed by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            elapsed++
            onTick(elapsed)
        }
    }
}
