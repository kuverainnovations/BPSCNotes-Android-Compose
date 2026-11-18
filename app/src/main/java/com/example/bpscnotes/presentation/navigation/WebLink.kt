package com.example.bpscnotes.presentation.navigation

import android.net.Uri

/**
 * Translates a shared https://bpscnotes.in/... link into the same
 * (screen, ids) vocabulary a push notification uses.
 *
 * Deliberately a translator rather than a second router: it hands its result
 * straight to [notificationRoute], so web links and notifications can never
 * drift apart the way the two notification routers once did (see the header
 * of NotificationRoute.kt).
 *
 * Why this exists: course sharing used to emit
 * `https://bpscnotes.in/courses/<id>`, which 404'd — the site has no such
 * page and the app declared no link handling, so the URL could not work in
 * either direction.
 *
 * ── Why links point at api.bpscnotes.in ──────────────────────────────────
 * bpscnotes.in is hosted on WordPress.com, which will not serve custom code
 * or files on the plan in use: neither the share page nor the
 * /.well-known/assetlinks.json that App Links verification requires can be
 * put there. api.bpscnotes.in is ours — same server, existing certificate —
 * so both live there and new shares use [SHARE_HOST].
 *
 * [LEGACY_HOST] is still parsed so links shared before this change open the
 * right screen for anyone who has the app. Those links cannot be *verified*
 * (no assetlinks.json on that domain), which is exactly why the manifest
 * keeps them in a separate filter without autoVerify — on Android 12+ one
 * unverifiable host in an autoVerify filter fails verification for every
 * host in it.
 */
object WebLink {

    /** Host for links this app generates. Must match the manifest. */
    const val SHARE_HOST = "api.bpscnotes.in"

    /** Host of links shared before the move; parsed, never generated. */
    const val LEGACY_HOST = "bpscnotes.in"

    /** Path segments that are content links rather than marketing pages. */
    private val SECTIONS = mapOf(
        "courses"         to ("courses"        to "courseId"),
        "course"          to ("courses"        to "courseId"),
        "quizzes"         to ("quiz_list"      to "quizId"),
        "quiz"            to ("quiz_list"      to "quizId"),
        "answer-writing"  to ("answer_writing" to "questionId"),
        "jobs"            to ("jobs"           to "jobId"),
        "study-materials" to ("study_materials" to "materialId"),
    )

    /**
     * @return the in-app route for [uri], or null when the link is not one of
     *         ours — a marketing or policy page, an unknown section, or a
     *         malformed id. Null means "let the browser have it".
     */
    fun routeFor(uri: Uri?): String? {
        if (uri == null) return null

        val host = uri.host.orEmpty().removePrefix("www.").lowercase()
        if (host != SHARE_HOST && host != LEGACY_HOST) return null

        val segments = uri.pathSegments.orEmpty().filter { it.isNotBlank() }
        if (segments.isEmpty()) return null

        // api.bpscnotes.in/c/<id> is the short form this app now shares.
        if (host == SHARE_HOST && segments[0].equals("c", ignoreCase = true)) {
            val courseId = segments.getOrNull(1)?.takeIf { looksLikeId(it) } ?: return null
            return notificationRoute("courses", mapOf("courseId" to courseId))
        }

        val (screen, idKey) = SECTIONS[segments[0].lowercase()] ?: return null
        val id = segments.getOrNull(1)?.takeIf { looksLikeId(it) }

        // A section with no id is still useful — /jobs opens the job list.
        return notificationRoute(screen, if (id != null) mapOf(idKey to id) else emptyMap())
    }

    /**
     * Ids in every route here are UUIDs. Checking the shape stops a marketing
     * slug like /courses/bihar-special being passed to a screen that will
     * immediately 404 against the API; those fall through to the browser.
     */
    private fun looksLikeId(value: String): Boolean =
        value.length == 36 && value.count { it == '-' } == 4 &&
            value.all { it.isLetterOrDigit() || it == '-' }

    /**
     * The canonical shareable URL for a course.
     *
     * Short on purpose — people read this in a WhatsApp message.
     */
    fun courseUrl(courseId: String): String = "https://$SHARE_HOST/c/$courseId"
}
