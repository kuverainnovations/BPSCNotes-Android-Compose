package com.example.bpscnotes

import android.app.Application
import android.util.Log
import com.example.bpscnotes.core.analytics.Analytics
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BpscApplication : Application() {

    @javax.inject.Inject lateinit var adManager: com.example.bpscnotes.core.ads.AdManager

    override fun onCreate() {
        super.onCreate()

        // ── Firebase — MUST be first, before any Firebase.getInstance() calls ──
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.e("TAG", "onCreate: init", )

                // Add this temporarily in MainActivity.onCreate()
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    Log.d("FCM_TOKEN", "Device token: $token")
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("BpscApp", "Firebase init failed (missing google-services.json?): ${e.message}")
        }

        // ── PostHog product analytics ─────────────────────────
        val config = PostHogAndroidConfig(
            apiKey = POSTHOG_API_KEY,
            host   = POSTHOG_HOST,
        ).apply {
            captureScreenViews = false  // we capture manually for better context
            captureDeepLinks   = true
            debug              = BuildConfig.DEBUG
        }
        PostHogAndroid.setup(this, config)

        // ── Unified analytics wrapper ─────────────────────────
        Analytics.init(this)

        // ── AdMob ─────────────────────────────────────────────
        adManager.initialize()
    }

    companion object {
        // Replace with your PostHog project API key from posthog.com
        const val POSTHOG_API_KEY = "phc_REPLACE_WITH_YOUR_POSTHOG_KEY"
        const val POSTHOG_HOST    = "https://app.posthog.com"   // or your self-hosted URL
    }
}