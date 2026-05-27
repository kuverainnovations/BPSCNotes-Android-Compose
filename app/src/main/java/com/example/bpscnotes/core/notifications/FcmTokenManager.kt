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
    private val prefs = context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)

    /** Call on login/app open — gets current FCM token and syncs to backend if changed */
    suspend fun syncTokenIfNeeded() {
        val authToken = tokenStore.getToken() ?: return
        if (authToken.isBlank()) return
        try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            val cached   = prefs.getString("last_synced_token", "")
            if (fcmToken != cached) {
                saveAndSync(fcmToken)
            }
        } catch (e: Exception) {
            Log.w("FcmTokenManager", "FCM token sync failed: ${e.message}")
        }
    }

    /** Called by BpscFirebaseMessagingService when token refreshes */
    suspend fun saveAndSync(fcmToken: String) {
        try {
            authApi.updateFcmToken(mapOf("fcmToken" to fcmToken))
            prefs.edit().putString("last_synced_token", fcmToken).apply()
            Log.d("FcmTokenManager", "FCM token synced ✅")
        } catch (e: Exception) {
            Log.w("FcmTokenManager", "FCM token upload failed: ${e.message}")
        }
    }

    /** Call on logout to clear the token from backend */
    suspend fun clearToken() {
        try {
            authApi.updateFcmToken(mapOf("fcmToken" to ""))
            prefs.edit().remove("last_synced_token").apply()
        } catch (e: Exception) {
            Log.w("FcmTokenManager", "FCM clear failed: ${e.message}")
        }
    }
}
