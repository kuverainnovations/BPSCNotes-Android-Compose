package com.example.bpscnotes.core.ui.components

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.example.bpscnotes.MainActivity
import com.example.bpscnotes.di.AppConfigEntryPoint
import dagger.hilt.android.EntryPointAccessors

/**
 * Prevents screenshots and screen recording while the composable is in composition.
 * Adds FLAG_SECURE on entry and removes it on exit, so the flag only applies to
 * screens that use this wrapper.
 *
 * Honours the admin `screen_capture_protection` switch. That toggle has existed on
 * the Settings page (and been returned by /app-config) since launch, but the client
 * never read the key — the flag was applied unconditionally, so turning it off did
 * nothing. Defaults to protected: an unloaded config or an offline launch keeps
 * screenshots blocked rather than opening a hole.
 */
@Composable
fun SecureScreen(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val appConfig = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AppConfigEntryPoint::class.java
        ).appConfigRepository()
    }
    val protectionEnabled by appConfig.config.collectAsState()

    DisposableEffect(protectionEnabled.screenCaptureProtection) {
        val activity = context as? MainActivity
        if (protectionEnabled.screenCaptureProtection) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    content()
}
