package com.example.bpscnotes.core.network

import android.util.Log
import okhttp3.Cache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evicts specific URL patterns from the OkHttp disk cache.
 *
 * Use this BEFORE calling load() after a mutating action (enroll, login/logout)
 * so the next GET hits the server instead of returning stale cached data.
 *
 * Normal loads (no prior mutation) still benefit from the 5-min cache —
 * fast, offline-capable, no flicker.
 */
@Singleton
class CacheInvalidator @Inject constructor(
    private val cache: Cache
) {
    companion object {
        private const val TAG = "CacheInvalidator"

        // URL substrings to evict after enrollment or login
        val USER_ENDPOINTS = listOf(
            "/api/v1/courses",        // enrollment status embedded per user
            "/api/v1/auth/me",        // user profile, name, coins
        )
    }

    /**
     * Evict all cached responses whose URL contains any of [patterns].
     * OkHttp's cache iterator lets us walk and remove entries one by one.
     */
    fun evict(patterns: List<String> = USER_ENDPOINTS) {
        try {
            val iterator = cache.urls()
            while (iterator.hasNext()) {
                val url = iterator.next()
                if (patterns.any { url.contains(it) }) {
                    Log.d(TAG, "Evicting cache: $url")
                    iterator.remove()
                }
            }
        } catch (e: Exception) {
            // Non-fatal — worst case user sees stale data once
            Log.w(TAG, "Cache eviction failed: ${e.message}")
        }
    }
}