package com.example.bpscnotes.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.bpscnotes.presentation.currentaffairs.CAArticle

@Entity(tableName = "cached_articles")
data class CachedArticleEntity(
    @PrimaryKey val id: String,
    val headline: String,
    val summary: String,
    val fullContent: String,
    val keyPoints: String,
    val examRelevance: String,
    val importantFacts: String,
    val category: String,
    val rawDate: String,
    val date: String,
    val readMinutes: Int,
    val mcqCount: Int,
    val mcqAttempted: Boolean,
    val mcqLastScore: Double?,
    val isImportant: Boolean,
    val isPrelims: Boolean,
    val isMains: Boolean,
    val tagsJson: String,   // JSON array string
    val source: String?,
    val isBookmarked: Boolean,
    val cachedAt: Long = System.currentTimeMillis(),
)

fun CachedArticleEntity.toUiModel(): CAArticle = CAArticle(
    id             = id,
    headline       = headline,
    summary        = summary,
    fullContent    = fullContent,
    keyPoints      = keyPoints,
    examRelevance  = examRelevance,
    importantFacts = importantFacts,
    category       = category,
    rawDate        = rawDate,
    date           = date,
    readMinutes    = readMinutes,
    mcqCount       = mcqCount,
    mcqAttempted   = mcqAttempted,
    mcqLastScore   = mcqLastScore,
    isImportant    = isImportant,
    isPrelims      = isPrelims,
    isMains        = isMains,
    tags           = tagsJson.removeSurrounding("[", "]").split(",").map { it.trim().removeSurrounding("\"") }.filter { it.isNotBlank() },
    source         = source,
    isBookmarked   = isBookmarked,
)

fun CAArticle.toEntity(): CachedArticleEntity = CachedArticleEntity(
    id             = id,
    headline       = headline,
    summary        = summary,
    fullContent    = fullContent,
    keyPoints      = keyPoints,
    examRelevance  = examRelevance,
    importantFacts = importantFacts,
    category       = category,
    rawDate        = rawDate,
    date           = date,
    readMinutes    = readMinutes,
    mcqCount       = mcqCount,
    mcqAttempted   = mcqAttempted,
    mcqLastScore   = mcqLastScore,
    isImportant    = isImportant,
    isPrelims      = isPrelims,
    isMains        = isMains,
    tagsJson       = "[${tags.joinToString(",") { "\"$it\"" }}]",
    source         = source,
    isBookmarked   = isBookmarked,
)
