package com.example.bpscnotes.core.notifications

import android.content.Context
import android.util.Log
import com.example.bpscnotes.data.local.TokenStore
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authApi: AuthApiService,
    private val tokenStore: TokenStore,
) {

    private val prefs =
        context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)

    /**
     * Call after login/app open
     */
    suspend fun syncTokenIfNeeded() {

        val authToken = tokenStore.getToken()

        Log.d("FCM_SYNC", "AUTH TOKEN = $authToken")

        if (authToken.isNullOrBlank()) {
            Log.e("FCM_SYNC", "NO AUTH TOKEN")
            return
        }

        try {

            val fcmToken = FirebaseMessaging.getInstance().token.await()

            Log.d("FCM_SYNC", "FCM TOKEN = $fcmToken")

            Log.d("FCM_SYNC", "FORCING TOKEN SYNC")

            val response = authApi.updateFcmToken(
                mapOf("fcmToken" to fcmToken)
            )

            Log.d("FCM_SYNC", "API RESPONSE = $response")

            prefs.edit()
                .putString("last_synced_token", fcmToken)
                .apply()

            Log.d("FCM_SYNC", "FCM TOKEN SYNCED ✅")

        } catch (e: Exception) {

            Log.e(
                "FCM_SYNC",
                "FCM TOKEN SYNC FAILED",
                e
            )
        }
    }

    /**
     * Called when Firebase (re)generates an FCM registration token.
     *
     * NOTE: this fires on EVERY app install, not just on refresh — there
     * is no prior token to "refresh" on a fresh install, so onNewToken()
     * is the FIRST callback too. If we're not logged in yet, there's no
     * userId to attach this token to on the backend, and /auth/fcm-token
     * requires auth — calling it anyway produced a 401, which
     * AuthInterceptor treated as "session expired" and redirected to
     * Login (clearing the nav stack, including Onboarding) on first
     * launch. Skip silently here; syncTokenIfNeeded() (called after
     * login/OTP/MPIN) fetches the current FCM token itself and will sync
     * this same token once there's a session.
     */
    suspend fun saveAndSync(fcmToken: String) {

        if (tokenStore.getToken().isNullOrBlank()) {
            Log.d("FCM_SYNC", "NEW TOKEN = $fcmToken (not logged in yet — will sync after login)")
            return
        }

        try {

            Log.d("FCM_SYNC", "Syncing FCM token (logged in) = $fcmToken")

            val response = authApi.updateFcmToken(
                mapOf("fcmToken" to fcmToken)
            )

            Log.d("FCM_SYNC", "UPLOAD RESPONSE = $response")

            prefs.edit()
                .putString("last_synced_token", fcmToken)
                .apply()

            Log.d("FCM_SYNC", "FCM TOKEN UPLOADED ✅")
            Log.d("FCM_DEBUG", "Uploading token = $fcmToken")

        } catch (e: Exception) {

            Log.e(
                "FCM_SYNC",
                "FCM TOKEN UPLOAD FAILED",
                e
            )
        }
    }

    suspend fun clearToken() {

        try {

            authApi.updateFcmToken(
                mapOf("fcmToken" to "")
            )

            prefs.edit()
                .remove("last_synced_token")
                .apply()

            Log.d("FCM_SYNC", "FCM TOKEN CLEARED")

            Log.d("FCM_DEBUG", "CLEARING TOKEN")
        } catch (e: Exception) {

            Log.e(
                "FCM_SYNC",
                "CLEAR TOKEN FAILED",
                e
            )
        }
    }
}