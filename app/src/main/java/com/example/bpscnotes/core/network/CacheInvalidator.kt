package com.example.bpscnotes.core.network

import android.util.Log
import okhttp3.Cache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evicts specific URL patterns from the OkHttp disk cache.
 *
 * Use this BEFORE calling load() after a mutating action (enroll, login/logout,
 * quiz submit, target complete, profile update, etc.)
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

        // ── All user-specific endpoints that change after mutations ──
        val USER_ENDPOINTS = listOf(
            "/api/v1/auth/me",              // profile, name, coins
            "/api/v1/courses",              // enrollment status per user
            "/api/v1/users",                // stats, leaderboard, enrollments, targets
            "/api/v1/coins",                // balance, transactions, tasks
            "/api/v1/notifications",        // unread count, list
            "/api/v1/quizzes",              // attempt status per quiz
            "/api/v1/flashcards/progress",  // progress per deck
            "/api/v1/rooms",                // tier, session, members
            "/api/v1/achievements",         // unlocked achievements
            "/api/v1/challenges",           // claim status
        )

        // Lightweight set — only clears profile + coins (use after coin earn/spend)
        val COINS_ENDPOINTS = listOf(
            "/api/v1/coins",
            "/api/v1/auth/me",
        )

        // Only clears quiz-related (use after quiz submit)
        val QUIZ_ENDPOINTS = listOf(
            "/api/v1/quizzes",
            "/api/v1/users/stats",
            "/api/v1/coins",
            "/api/v1/achievements",
        )

        // Only clears daily targets (use after target create/complete/delete)
        val TARGETS_ENDPOINTS = listOf(
            "/api/v1/users/daily-targets",
            "/api/v1/users/stats",
        )
    }

    /**
     * Evict all cached responses whose URL contains any of [patterns].
     * Defaults to ALL user endpoints — safe to call after any mutation.
     */
    fun evict(patterns: List<String> = USER_ENDPOINTS) {
        try {
            val iterator = cache.urls()
            var count = 0
            while (iterator.hasNext()) {
                val url = iterator.next()
                if (patterns.any { url.contains(it) }) {
                    iterator.remove()
                    count++
                }
            }
            if (count > 0) Log.d(TAG, "Evicted $count cached entries")
        } catch (e: Exception) {
            // Non-fatal — worst case user sees stale data once
            Log.w(TAG, "Cache eviction failed: ${e.message}")
        }
    }

    /** Evict everything — use on logout or account switch */
    fun evictAll() {
        try {
            cache.evictAll()
            Log.d(TAG, "Full cache cleared")
        } catch (e: Exception) {
            Log.w(TAG, "Full cache eviction failed: ${e.message}")
        }
    }
}