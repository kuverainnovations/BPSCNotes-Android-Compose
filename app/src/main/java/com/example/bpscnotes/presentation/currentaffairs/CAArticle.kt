package com.example.bpscnotes.presentation.currentaffairs

import com.example.bpscnotes.data.remote.api.CurrentAffairDto
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Formats any ISO date string to a human-readable form.
 *  Input examples: "2026-05-31T07:10:13.670Z", "2026-05-31", "2026-05-31T07:10:13+00:00"
 *  Output: "31 May 2026"
 */
fun formatCaDate(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val fmts = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd"
    )
    for (fmt in fmts) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.getDefault()).apply { isLenient = true }
            val parsed = sdf.parse(raw) ?: continue
            return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(parsed)
        } catch (_: Exception) {}
    }
    // Fallback: take just the date part if it looks like yyyy-MM-dd...
    return if (raw.length >= 10) raw.substring(0, 10) else raw
}

/**
 * Strips HTML tags and decodes common HTML entities to plain text.
 * Used at specific call sites that need plain text from a rich HTML field:
 *  - Search filtering (so searching "India" finds it regardless of <span> tags)
 *  - Share intents / notifications (HTML renders as literal tags in those contexts)
 *  - PDF filename generation
 * NOT applied in toUiModel() — the UI model keeps raw HTML so that
 * RichHtmlText composables can render colors, bold, highlights, etc.
 */
fun String.stripHtmlTags(): String =
    this.replace(Regex("<[^>]+>"), "")
        .replace("&amp;",  "&")
        .replace("&lt;",   "<")
        .replace("&gt;",   ">")
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&#39;",  "'")
        .replace(Regex("\\s+"), " ")
        .trim()

/**
 * UI model for a Current Affairs article.
 *
 * Mapped from [CurrentAffairDto] in [CurrentAffairsViewModel].
 * Used by [CurrentAffairsScreen] for display.
 */
data class CAArticle(
    val id: String,
    val headline: String,
    val summary: String,
    val fullContent: String,
    val keyPoints: String,          // TipTap HTML — key takeaways
    val examRelevance: String,      // TipTap HTML — which exam + why
    val importantFacts: String,     // TipTap HTML — numbers/names/dates
    val category: String,
    val rawDate: String,      // original ISO string — used for grouping/sorting
    val date: String,             // formatted for display e.g. "31 May 2026"
    val readMinutes: Int,
    val mcqCount: Int,
    val mcqAttempted: Boolean = false,
    val mcqLastScore: Double? = null,
    val isImportant: Boolean,
    val isPrelims: Boolean,
    val isMains: Boolean,
    val tags: List<String>,
    val source: String?="",
    val isBookmarked: Boolean = false
)

/** Maps backend [CurrentAffairDto] to the UI [CAArticle] model. */
fun CurrentAffairDto.toUiModel(isBookmarked: Boolean = this.isBookmarked): CAArticle {
    // Use admin-set read time; fall back to word count estimate only if not set
    val readMins = if (readTime > 0) readTime
    else maxOf(1, (fullContent ?: summary).split("\\s+".toRegex()).size / 200)

    // Determine prelims/mains from examTags
    val examTagsLower = examTags.map { it.lowercase() }
    val isBoth    = examTagsLower.any { it == "both" }
    val isPrelims = isBoth || isImportant || examTagsLower.any { "prelim" in it || "pt" in it }
    val isMains   = isBoth || examTagsLower.any { "main" in it || "gs" in it }

    // MCQ count: use real count from backend (ca_mcqs table)
    val mcqCount = mcqCount

    return CAArticle(
        id          = id,
        // Headline and Summary are kept as raw HTML from the admin TipTap editor
        // so RichHtmlText composables can render colors, bold, highlights etc.
        // Use .stripHtmlTags() at the specific call sites that need plain text
        // (search filter, share intents, PDF filename).
        headline    = title,
        summary     = summary,
        fullContent = fullContent ?: summary,
        keyPoints      = keyPoints      ?: "",
        examRelevance  = examRelevance  ?: "",
        importantFacts = importantFacts ?: "",
        category    = category,
        rawDate     = date,
        date        = formatCaDate(date),
        readMinutes = readMins,
        mcqCount    = mcqCount,  // real count from API
        mcqAttempted = mcqAttempted,
        mcqLastScore = mcqLastScore,
        isImportant = this.isImportant,
        isPrelims   = isPrelims,
        isMains     = isMains,
        tags        = tags.ifEmpty { listOf(category) },
        source      = source,
        isBookmarked = isBookmarked
    )
}

/** All category labels for the filter row */
val CA_CATEGORIES = listOf(
    "All", "Economy", "Polity", "International",
    "Science", "Education", "Sports", "Bihar GK", "Environment"
)