package com.example.bpscnotes.domain.model

data class DayProgress(val day: String, val score: Int)

data class DailyTarget(
    val id: String,
    val title: String,
    val subject: String,
    val isCompleted: Boolean,
    val totalQuestions: Int,
    val attemptedQuestions: Int,
)

data class CurrentAffairItem(
    val id: String,
    val date: String,
    val headline: String,
    val category: String,
    val isBookmarked: Boolean,
    val detail: String = "",
)

data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)

data class Flashcard(
    val id: String,
    val subject: String,
    val front: String,
    val back: String,
    var isRevealed: Boolean = false,
)

data class MockTest(
    val id: String,
    val title: String,
    val subject: String,
    val totalQuestions: Int,
    val durationMinutes: Int,
    val attemptedBy: Int,
    val avgScore: Int,
    val isPaid: Boolean,
)

data class Course(
    val id: String,
    val title: String,
    val subject: String,
    val instructor: String?,
    val totalLessons: Int,
    val completedLessons: Int,
    val thumbnail: String?,
    val isPaid: Boolean,
    val price: Int,
)

data class ReadingRoom(
    val id: String,
    val name: String,
    val activeUsers: Int,
    val totalCapacity: Int,
    val studyStreakHours: Int,
    val isJoined: Boolean,
)

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val studyHours: Float,
    val coins: Int,
    val avatarInitials: String,
    val isCurrentUser: Boolean = false,
)

data class JobVacancy(
    val id: String,
    val title: String,
    val organization: String,
    val posts: Int,
    val lastDate: String,
    val category: String,
    val isNotified: Boolean,
)

data class DownloadItem(
    val id: String,
    val title: String,
    val subject: String,
    val sizeMb: Float,
    val isDownloaded: Boolean,
)

data class SubscriptionPlan(
    val id: String,
    val name: String,
    val durationMonths: Int,
    val price: Int,
    val originalPrice: Int,
    val features: List<String>,
    val isPopular: Boolean = false,
)