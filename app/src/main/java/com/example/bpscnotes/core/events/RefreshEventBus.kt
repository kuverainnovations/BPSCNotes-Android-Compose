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
    data class CourseProgressChanged(val courseId: String) : RefreshEvent()
    data class AvatarUpdated(val avatarUrl: String) : RefreshEvent()
}

@Singleton
class RefreshEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()
    fun emit(event: RefreshEvent) = _events.tryEmit(event)
}