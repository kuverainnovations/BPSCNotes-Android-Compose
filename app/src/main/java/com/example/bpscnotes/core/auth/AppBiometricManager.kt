package com.example.bpscnotes.core.auth

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

/**
 * Thin wrapper around the Android framework BiometricPrompt (API 28+).
 * No extra dependency needed — uses android.hardware.biometrics from the SDK.
 *
 * SECURITY: We do NOT use CryptoObject / encrypted keys.
 *   Biometric success → POST /auth/login-mpin on server.
 */
object AppBiometricManager {

    /** Returns true if biometric hardware is present, enrolled, and API >= 29. */
    fun isAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val bm = context.getSystemService(BiometricManager::class.java) ?: return false
        return bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Show the biometric prompt.
     * Only works on API >= 28. Guard with [isAvailable] before calling.
     */
    @Suppress("DEPRECATION")
    fun authenticate(
        context:    Context,
        title:      String,
        subtitle:   String,
        onSuccess:  () -> Unit,
        onError:    (String) -> Unit,
        onFallback: () -> Unit   // user tapped "Use MPIN" (negative button)
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onFallback()
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val cancel   = CancellationSignal()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                // Error code 13 = BIOMETRIC_ERROR_NEGATIVE_BUTTON (user tapped cancel/fallback)
                // Error code 10 = BIOMETRIC_ERROR_USER_CANCELED
                if (errorCode == 13 || errorCode == 10) {
                    onFallback()
                } else {
                    onError(errString?.toString() ?: "Biometric error ($errorCode)")
                }
            }
            override fun onAuthenticationFailed() {
                // Single failure — prompt stays open, no action needed
            }
        }

        val prompt = BiometricPrompt.Builder(context)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton("Use MPIN", executor) { _, _ -> onFallback() }
            .build()

        prompt.authenticate(cancel, executor, callback)
    }
}