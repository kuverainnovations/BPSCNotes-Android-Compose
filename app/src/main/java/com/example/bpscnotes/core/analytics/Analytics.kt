package com.example.bpscnotes.core.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.posthog.PostHog

/**
 * Central analytics hub — all events flow through here.
 *
 * Sends to BOTH:
 *  • Firebase Analytics  — for Google/crashlytics dashboards
 *  • PostHog             — for product analytics, funnels, session recording
 *
 * Usage: inject [BpscAnalytics] anywhere, call track(Event.*)
 */
object Analytics {

    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        initialized = true
    }

    // ── Identify user after login ─────────────────────────────
    fun identify(userId: String, props: Map<String, Any?> = emptyMap()) {
        PostHog.identify(userId,
            userProperties = props.filterValues { it != null }.mapValues { it.value!! }
        )
        firebaseAnalytics?.setUserId(userId)
        props.forEach { (k, v) -> v?.let { firebaseAnalytics?.setUserProperty(k.take(24), it.toString().take(36)) } }
    }

    fun reset() {
        PostHog.reset()
        firebaseAnalytics?.setUserId(null)
    }

    // ── Core track function ───────────────────────────────────
    fun track(event: String, props: Map<String, Any?> = emptyMap()) {
        // PostHog
        PostHog.capture(event,
            properties = props.filterValues { it != null }.mapValues { it.value!! }
        )
        // Firebase
        val bundle = Bundle().apply {
            props.forEach { (k, v) ->
                val key = k.replace(" ", "_").take(40)
                when (v) {
                    is String  -> putString(key, v.take(100))
                    is Int     -> putInt(key, v)
                    is Long    -> putLong(key, v)
                    is Double  -> putDouble(key, v)
                    is Float   -> putFloat(key, v)
                    is Boolean -> putString(key, v.toString())
                    else       -> putString(key, v?.toString()?.take(100) ?: "")
                }
            }
        }
        firebaseAnalytics?.logEvent(event.replace(" ", "_").take(40), bundle)
    }

    fun screen(screenName: String, props: Map<String, Any?> = emptyMap()) {
        PostHog.screen(screenName, props.filterValues { it != null }.mapValues { it.value!! })
        track(FirebaseAnalytics.Event.SCREEN_VIEW, mapOf(
            FirebaseAnalytics.Param.SCREEN_NAME to screenName,
        ) + props)
    }
}

// ─────────────────────────────────────────────────────────────
// TYPED EVENT CATALOGUE
// Every event in the app is defined here as a helper function.
// Never use raw strings elsewhere — always call these.
// ─────────────────────────────────────────────────────────────
object Event {

    // ── Auth ─────────────────────────────────────────────────
    fun appOpen()                     = Analytics.track("app_open")
    fun login(method: String = "otp") = Analytics.track(FirebaseAnalytics.Event.LOGIN, mapOf("method" to method))
    fun register()                    = Analytics.track(FirebaseAnalytics.Event.SIGN_UP)
    fun logout()                      = Analytics.track("logout")

    // ── Screens ───────────────────────────────────────────────
    fun screenView(name: String, extra: Map<String, Any?> = emptyMap()) =
        Analytics.screen(name, extra)

    // ── Quiz ─────────────────────────────────────────────────
    fun quizStarted(quizId: String, quizTitle: String, quizType: String) =
        Analytics.track("quiz_started", mapOf("quiz_id" to quizId, "quiz_title" to quizTitle, "quiz_type" to quizType))

    fun quizCompleted(quizId: String, quizTitle: String, quizType: String,
                      score: Int, correct: Int, total: Int, timeSecs: Int, coinsEarned: Int) =
        Analytics.track("quiz_completed", mapOf(
            "quiz_id"      to quizId,
            "quiz_title"   to quizTitle,
            "quiz_type"    to quizType,
            "score"        to score,
            "correct"      to correct,
            "total"        to total,
            "time_secs"    to timeSecs,
            "coins_earned" to coinsEarned,
            "passed"       to (score >= 60),
        ))

    fun quizFailed(quizId: String, score: Int) =
        Analytics.track("quiz_failed", mapOf("quiz_id" to quizId, "score" to score))

    // ── Courses ───────────────────────────────────────────────
    fun courseViewed(courseId: String, courseTitle: String, isPaid: Boolean) =
        Analytics.track("course_viewed", mapOf(
            "course_id"    to courseId,
            "course_title" to courseTitle,
            "is_paid"      to isPaid,
        ))

    fun courseEnrolled(courseId: String, courseTitle: String, isPaid: Boolean) {
        Analytics.track("course_enrolled", mapOf(
            "course_id"    to courseId,
            "course_title" to courseTitle,
            "is_paid"      to isPaid,
        ))
        Analytics.track(FirebaseAnalytics.Event.JOIN_GROUP, mapOf(
            FirebaseAnalytics.Param.GROUP_ID to courseId,
        ))
    }

    fun lessonCompleted(courseId: String, lessonId: String, lessonTitle: String, durationSecs: Int) =
        Analytics.track("lesson_completed", mapOf(
            "course_id"     to courseId,
            "lesson_id"     to lessonId,
            "lesson_title"  to lessonTitle,
            "duration_secs" to durationSecs,
        ))

    // ── Study Materials ───────────────────────────────────────
    fun materialViewed(materialId: String, title: String, subject: String, isPaid: Boolean) =
        Analytics.track("material_viewed", mapOf(
            "material_id" to materialId,
            "title"       to title,
            "subject"     to subject,
            "is_paid"     to isPaid,
        ))

    fun materialPurchased(materialId: String, title: String, price: Int) {
        Analytics.track("material_purchased", mapOf(
            "material_id" to materialId,
            "title"       to title,
            "price"       to price,
        ))
        Analytics.track(FirebaseAnalytics.Event.PURCHASE, mapOf(
            FirebaseAnalytics.Param.ITEM_ID    to materialId,
            FirebaseAnalytics.Param.ITEM_NAME  to title,
            FirebaseAnalytics.Param.VALUE      to price.toDouble(),
            FirebaseAnalytics.Param.CURRENCY   to "INR",
        ))
    }

    fun materialDownloaded(materialId: String, title: String) =
        Analytics.track("material_downloaded", mapOf("material_id" to materialId, "title" to title))

    // ── Study Room / Session ──────────────────────────────────
    fun studySessionStarted(tierName: String) =
        Analytics.track("study_session_started", mapOf("tier" to tierName))

    fun studySessionEnded(tierName: String, durationMins: Int, coinsEarned: Int) =
        Analytics.track("study_session_ended", mapOf(
            "tier"         to tierName,
            "duration_min" to durationMins,
            "coins_earned" to coinsEarned,
        ))

    fun tierPromoted(fromTier: String, toTier: String) =
        Analytics.track("tier_promoted", mapOf("from_tier" to fromTier, "to_tier" to toTier))

    // ── Current Affairs ───────────────────────────────────────
    fun articleRead(articleId: String, category: String, isImportant: Boolean) =
        Analytics.track("article_read", mapOf(
            "article_id"   to articleId,
            "category"     to category,
            "is_important" to isImportant,
        ))

    fun articleBookmarked(articleId: String, category: String) =
        Analytics.track("article_bookmarked", mapOf("article_id" to articleId, "category" to category))

    fun caMcqStarted(articleId: String, questionCount: Int) =
        Analytics.track("ca_mcq_started", mapOf("article_id" to articleId, "question_count" to questionCount))

    fun caMcqCompleted(articleId: String, score: Int, total: Int) =
        Analytics.track("ca_mcq_completed", mapOf(
            "article_id" to articleId,
            "score"      to score,
            "total"      to total,
            "pct"        to if (total > 0) (score * 100) / total else 0,
        ))

    // ── Payment ───────────────────────────────────────────────
    fun paymentInitiated(plan: String, amount: Int) =
        Analytics.track("payment_initiated", mapOf("plan" to plan, "amount" to amount))

    fun paymentSuccess(plan: String, amount: Int, method: String) {
        Analytics.track("payment_success", mapOf(
            "plan"   to plan,
            "amount" to amount,
            "method" to method,
        ))
        Analytics.track(FirebaseAnalytics.Event.PURCHASE, mapOf(
            FirebaseAnalytics.Param.VALUE        to amount.toDouble(),
            FirebaseAnalytics.Param.CURRENCY     to "INR",
            FirebaseAnalytics.Param.ITEM_NAME    to "BPSCNotes $plan Plan",
            FirebaseAnalytics.Param.PAYMENT_TYPE to method,
        ))
    }

    fun paymentFailed(plan: String, errorCode: Int, reason: String) =
        Analytics.track("payment_failed", mapOf(
            "plan"       to plan,
            "error_code" to errorCode,
            "reason"     to reason.take(100),
        ))

    // ── Coins ─────────────────────────────────────────────────
    fun coinsEarned(action: String, amount: Int) =
        Analytics.track("coins_earned", mapOf("action" to action, "amount" to amount))

    fun coinsSpent(action: String, amount: Int) =
        Analytics.track("coins_spent", mapOf("action" to action, "amount" to amount))

    // ── Daily Target ──────────────────────────────────────────
    fun targetCreated(targetType: String) =
        Analytics.track("target_created", mapOf("type" to targetType))

    fun targetCompleted(targetType: String, coinsEarned: Int) =
        Analytics.track("target_completed", mapOf("type" to targetType, "coins_earned" to coinsEarned))

    // ── Search ────────────────────────────────────────────────
    fun searched(query: String, section: String) =
        Analytics.track("searched", mapOf("query" to query.take(50), "section" to section))

    // ── Notifications ─────────────────────────────────────────
    fun notificationReceived(type: String) =
        Analytics.track("notification_received", mapOf("type" to type))

    fun notificationTapped(type: String, notifId: String) =
        Analytics.track("notification_tapped", mapOf("type" to type, "notif_id" to notifId))

    // ── Onboarding ────────────────────────────────────────────
    fun onboardingStarted()                       = Analytics.track("onboarding_started")
    fun onboardingCompleted(exam: String)         = Analytics.track("onboarding_completed", mapOf("exam" to exam))
    fun onboardingStepCompleted(step: Int)        = Analytics.track("onboarding_step", mapOf("step" to step))


    fun streakMilestone(days: Int) =
        Analytics.track("streak_milestone", mapOf("days" to days))
    // ── Achievements & Tiers ──────────────────────────────────────
    fun achievementUnlocked(id: String, label: String) =
        Analytics.track("achievement_unlocked", mapOf("id" to id, "label" to label))
    fun tierChanged(from: String, to: String, direction: String) =
        Analytics.track("tier_changed", mapOf("from" to from, "to" to to, "direction" to direction))
    // ── Ads ──────────────────────────────────────────────────────
    fun adWatched(type: String, coinsEarned: Int = 0) =
        Analytics.track("ad_watched", mapOf("type" to type, "coins_earned" to coinsEarned))
    fun adFailed(type: String, reason: String) =
        Analytics.track("ad_failed", mapOf("type" to type, "reason" to reason))
    // ── Settings ─────────────────────────────────────────────────
    fun settingsChanged(key: String, value: String) =
        Analytics.track("settings_changed", mapOf("key" to key, "value" to value))
    fun languageChanged(from: String, to: String) =
        Analytics.track("language_changed", mapOf("from" to from, "to" to to))
    // ── Notifications ─────────────────────────────────────────────
    fun notificationTapped(type: String) =
        Analytics.track("notification_tapped", mapOf("type" to type))
    // ── Live Classes ──────────────────────────────────────────────
    fun liveClassJoined(classId: String, title: String) =
        Analytics.track("live_class_joined", mapOf("class_id" to classId, "title" to title))
    fun liveClassRegistered(classId: String, title: String) =
        Analytics.track("live_class_registered", mapOf("class_id" to classId, "title" to title))
    // ── Flashcards ────────────────────────────────────────────────
    fun flashcardSessionStarted(deckSize: Int) =
        Analytics.track("flashcard_session_started", mapOf("deck_size" to deckSize))
    fun flashcardSessionCompleted(reviewed: Int, correct: Int) =
        Analytics.track("flashcard_session_completed", mapOf("reviewed" to reviewed, "correct" to correct))
    // ── Daily Targets ─────────────────────────────────────────────
    fun dailyTargetCompleted(title: String) =
        Analytics.track("daily_target_completed", mapOf("title" to title))
    fun allTargetsCompleted(streak: Int) =
        Analytics.track("all_targets_completed", mapOf("streak" to streak))
    // ── Generic ───────────────────────────────────────────────────
    fun track(event: String, props: Map<String, Any?> = emptyMap()) =
        Analytics.track(event, props)

}