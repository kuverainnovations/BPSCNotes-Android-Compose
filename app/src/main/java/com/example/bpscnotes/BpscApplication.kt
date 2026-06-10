package com.example.bpscnotes

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.bpscnotes.core.analytics.Analytics
import com.google.firebase.FirebaseApp
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class BpscApplication : Application() {

    @javax.inject.Inject lateinit var adManager: com.example.bpscnotes.core.ads.AdManager

    override fun onCreate() {
        super.onCreate()

        // Wipe any bad cached responses from previous versions (HTML 503 pages)
        try { java.io.File(cacheDir, "http_cache").deleteRecursively() } catch (_: Exception) {}

        // ── Firebase ──────────────────────────────────────────
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            android.util.Log.w("BpscApp", "Firebase init failed: ${e.message}")
        }

        // ── Coil — optimized image loader for low-bandwidth Bihar users ──
        // 50 MB disk cache so images survive app restarts
        // 5% of RAM for memory cache — conservative for low-end devices
        Coil.setImageLoader {
            ImageLoader.Builder(this)
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.05)  // only 5% of RAM
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizeBytes(50L * 1024 * 1024)  // 50 MB
                        .build()
                }
                // Use cached images while revalidating in background
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                // Smooth crossfade instead of pop-in flash
                .crossfade(true)
                .crossfade(200)
                // Shorter image fetch timeout for slow networks
                .okHttpClient {
                    OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .build()
                }
                .build()
        }

        // ── PostHog ───────────────────────────────────────────
        // PostHog — replace with real key before release.
        // Once you add buildConfigField entries to app/build.gradle, switch these back to:
        //   apiKey = BuildConfig.POSTHOG_API_KEY
        //   host   = BuildConfig.POSTHOG_HOST
        val config = PostHogAndroidConfig(
            apiKey = "phc_REPLACE_WITH_YOUR_POSTHOG_KEY",
            host   = "https://app.posthog.com",
        ).apply {
            captureScreenViews = false
            captureDeepLinks   = true
        }
        PostHogAndroid.setup(this, config)

        // ── Analytics ─────────────────────────────────────────
        Analytics.init(this)

        // ── AdMob ─────────────────────────────────────────────
        adManager.initialize()
    }

    companion object {
        // PostHog keys are injected via BuildConfig fields in app/build.gradle:
        //   buildConfigField("String", "POSTHOG_API_KEY", '"phc_YOUR_KEY_HERE"')
        //   buildConfigField("String", "POSTHOG_HOST",    '"https://app.posthog.com"')
        // This keeps secrets out of source control.
    }
}