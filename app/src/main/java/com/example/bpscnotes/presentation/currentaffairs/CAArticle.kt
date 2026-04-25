package com.example.bpscnotes.presentation.currentaffairs

import com.example.bpscnotes.data.remote.api.CurrentAffairDto

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
    val category: String,
    val date: String,
    val readMinutes: Int,
    val mcqCount: Int,
    val isImportant: Boolean,
    val isPrelims: Boolean,
    val isMains: Boolean,
    val tags: List<String>,
    val source: String?="",
    val isBookmarked: Boolean = false
)

/** Maps backend [CurrentAffairDto] to the UI [CAArticle] model. */
fun CurrentAffairDto.toUiModel(isBookmarked: Boolean = this.isBookmarked): CAArticle {
    // Estimate read time: ~200 words per minute
    val wordCount    = (fullContent ?: summary).split("\\s+".toRegex()).size
    val readMins     = maxOf(1, wordCount / 200)

    // Determine prelims/mains from examTags
    val examTagsLower = examTags.map { it.lowercase() }
    val isPrelims     = isImportant || examTagsLower.any { "prelim" in it || "pt" in it }
    val isMains       = examTagsLower.any { "main" in it || "gs" in it }

    // MCQ count: derive from importance + tags (backend doesn't provide this yet)
    val mcqCount = when {
        isImportant && tags.size >= 4 -> 8
        isImportant                   -> 5
        tags.size >= 3                -> 3
        else                          -> 2
    }

    return CAArticle(
        id          = id,
        headline    = title,
        summary     = summary,
        fullContent = fullContent ?: summary,
        category    = category,
        date        = date,
        readMinutes = readMins,
        mcqCount    = mcqCount,
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
