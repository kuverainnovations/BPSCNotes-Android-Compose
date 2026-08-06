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
    fun isAvailable(context: Context): Boolean = status(context) == Status.Ready

    /**
     * Why biometric login can or cannot be switched on.
     *
     * [isAvailable] collapses every reason into false, which is fine for
     * deciding whether to *offer* biometric login but useless for explaining a
     * refusal. Settings needs the difference: a phone with no sensor can never
     * do this, whereas a phone with a sensor and no fingerprint saved just
     * needs the user to go and add one — the case the client hit, where the
     * toggle flipped on and silently meant nothing.
     */
    enum class Status { Ready, NotEnrolled, NoHardware, Unsupported }

    @Suppress("DEPRECATION")
    fun status(context: Context): Status {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return Status.Unsupported
        val bm = context.getSystemService(BiometricManager::class.java) ?: return Status.NoHardware
        return when (bm.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS               -> Status.Ready
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED   -> Status.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE  -> Status.NoHardware
            else                                             -> Status.Unsupported
        }
    }

    /**
     * Opens the system screen where a fingerprint or face can be added.
     *
     * The app cannot enrol a biometric itself — only Settings can — so the most
     * it can do is take the user there. Falls back to the top-level Settings
     * app on OEM builds that do not expose the enrol intent.
     */
    fun openEnrollment(context: Context) {
        val intents = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(android.content.Intent(android.provider.Settings.ACTION_BIOMETRIC_ENROLL))
            }
            add(android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
            add(android.content.Intent(android.provider.Settings.ACTION_SETTINGS))
        }
        for (intent in intents) {
            try {
                context.startActivity(intent.apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) })
                return
            } catch (_: Exception) { /* try the next one */ }
        }
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
