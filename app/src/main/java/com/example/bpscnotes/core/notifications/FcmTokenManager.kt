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
     * Called when Firebase refreshes token
     */
    suspend fun saveAndSync(fcmToken: String) {

        try {

            Log.d("FCM_SYNC", "NEW TOKEN = $fcmToken")

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
