package com.example.bpscnotes.core.notifications


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.bpscnotes.MainActivity
import com.kuvera.bpscnotes.R
import com.example.bpscnotes.core.analytics.Event
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BpscFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var tokenManager: FcmTokenManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Save and sync FCM token to backend
        CoroutineScope(Dispatchers.IO).launch {
            tokenManager.saveAndSync(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data    = message.data
        val notif   = message.notification
        val type    = data["type"] ?: "announcement"
        val notifId = data["notifId"] ?: data["notification_id"] ?: ""
        val screen  = data["screen"] ?: ""

        val title = notif?.title ?: data["title"] ?: "BPSCNotes"
        val body  = notif?.body  ?: data["body"]  ?: ""

        // Track notification received
        Event.notificationReceived(type)

        // Create channel and show notification
        createNotificationChannels()
        showNotification(
            title    = title,
            body     = body,
            type     = type,
            notifId  = notifId,
            screen   = screen,
            deepLink = data["deepLink"],
        )
    }

    private fun showNotification(
        title: String, body: String, type: String,
        notifId: String, screen: String, deepLink: String?
    ) {
        val channelId = when (type) {
            "tier_promotion", "demotion_warning",
            "weekly_rank", "challenge_deadline" -> CHANNEL_STUDY_ROOMS
            "new_course", "course_update"        -> CHANNEL_COURSES
            "material_approved"                  -> CHANNEL_MATERIALS
            "quiz_result", "mock_result"         -> CHANNEL_QUIZZES
            "payment", "subscription"            -> CHANNEL_PAYMENTS
            "new_job"                            -> CHANNEL_JOBS
            "ca_update"                          -> CHANNEL_CURRENT_AFFAIRS
            else                                 -> CHANNEL_GENERAL
        }

        // Deep-link intent — pass screen and notifId to MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("screen",   screen)
            putExtra("notifId",  notifId)
            putExtra("type",     type)
            deepLink?.let { putExtra("deepLink", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, notifId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId.hashCode().coerceIn(1, Int.MAX_VALUE), notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            Triple(CHANNEL_GENERAL,         "General",           "App announcements and updates"),
            Triple(CHANNEL_STUDY_ROOMS,     "Study Rooms",       "Tier promotions, rank reveals, challenges"),
            Triple(CHANNEL_COURSES,         "Courses",           "New courses and course updates"),
            Triple(CHANNEL_MATERIALS,       "Student Hub",   "Approved uploads and new materials"),
            Triple(CHANNEL_QUIZZES,         "Quizzes",           "Quiz results and mock test scores"),
            Triple(CHANNEL_PAYMENTS,        "Payments",          "Payment confirmations and subscription alerts"),
            Triple(CHANNEL_JOBS,            "Job Alerts",        "New government job vacancies"),
            Triple(CHANNEL_CURRENT_AFFAIRS, "Current Affairs",   "Daily current affairs updates"),
        )

        channels.forEach { (id, name, desc) ->
            if (nm.getNotificationChannel(id) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                        description = desc
                        enableVibration(true)
                    }
                )
            }
        }
    }

    companion object {
        const val CHANNEL_GENERAL          = "general"
        const val CHANNEL_STUDY_ROOMS      = "study_rooms"
        const val CHANNEL_COURSES          = "courses"
        const val CHANNEL_MATERIALS        = "materials"
        const val CHANNEL_QUIZZES          = "quizzes"
        const val CHANNEL_PAYMENTS         = "payments"
        const val CHANNEL_JOBS             = "jobs"
        const val CHANNEL_CURRENT_AFFAIRS  = "current_affairs"
    }
}