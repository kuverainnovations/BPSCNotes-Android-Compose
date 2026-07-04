package com.example.bpscnotes.core.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class RefreshEvent {
    object LessonCompleted   : RefreshEvent()
    object CourseEnrolled    : RefreshEvent()
    object ProfileUpdated    : RefreshEvent()
    object CoinsChanged      : RefreshEvent()
    object QuizCompleted     : RefreshEvent()
    object TargetUpdated     : RefreshEvent()
    object NotificationReceived : RefreshEvent()   // fires when push arrives → update badge
    /** Emitted after a study session ends so Dashboard/Profile refresh "time spent today". */
    object StudySessionEnded : RefreshEvent()
    /** Emitted after a subscription is activated so all screens re-check isPurchased/isSubscribed. */
    object SubscriptionChanged  : RefreshEvent()
    data class CourseProgressChanged(val courseId: String) : RefreshEvent()
    data class AvatarUpdated(val avatarUrl: String) : RefreshEvent()
    // Detail screen and list screen hold separate CurrentAffairsViewModel
    // instances (each Compose Navigation destination gets its own
    // hiltViewModel()) — this keeps their local bookmark state in sync
    // without forcing a full reload, same pattern as CoinsChanged.
    data class CaBookmarkChanged(val affairId: String, val isBookmarked: Boolean) : RefreshEvent()
    // Course Details screen and the course list screen hold separate
    // CourseDetailViewModel/MyLearningViewModel instances — this keeps the
    // list's savedCourseIds in sync with a save/unsave done from the
    // details screen without forcing a full reload, same pattern as
    // CaBookmarkChanged above.
    data class CourseSaveChanged(val courseId: String, val isSaved: Boolean) : RefreshEvent()
}

@Singleton
class RefreshEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()
    fun emit(event: RefreshEvent) = _events.tryEmit(event)
}