package com.example.bpscnotes.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

// SimpleDateFormat is not thread-safe, and these run on Dispatchers.IO — a
// shared instance can interleave between two card ratings and emit a corrupt
// date (or throw), which would poison nextReviewDate. One instance per thread.
private val SDF = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat("yyyy-MM-dd", Locale.US)
}

private fun todayString(): String = SDF.get()!!.format(Date())

private fun daysFromNow(days: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, days)
    return SDF.get()!!.format(cal.time)
}

@Entity(tableName = "sm2_cards")
data class Sm2CardEntity(
    @PrimaryKey val cardId: String,
    val repetitions: Int = 0,
    val easeFactor: Double = 2.5,
    val interval: Int = 1,
    val nextReviewDate: String = todayString(),
    val lastRating: Int = -1,
    val updatedAt: Long = System.currentTimeMillis(),
)

fun Sm2CardEntity.applyRating(quality: Int): Sm2CardEntity {
    val q = when (quality) { 0 -> 1; 1 -> 3; 2 -> 4; 3 -> 5; else -> 3 }
    val newEF       = if (q >= 3) maxOf(1.3, easeFactor + 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)) else easeFactor
    val newReps     = if (q >= 3) repetitions + 1 else 0
    val newInterval = when {
        q < 3       -> 1
        newReps == 1 -> 1
        newReps == 2 -> 6
        else         -> (interval * newEF).toInt().coerceAtLeast(1)
    }
    return copy(
        repetitions    = newReps,
        easeFactor     = newEF,
        interval       = newInterval,
        nextReviewDate = daysFromNow(newInterval),
        lastRating     = quality,
        updatedAt      = System.currentTimeMillis(),
    )
}

fun Sm2CardEntity.isDueToday(): Boolean {
    val today = todayString()
    return nextReviewDate <= today
}
