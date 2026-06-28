package com.example.bpscnotes.core.network

import com.example.bpscnotes.data.local.TokenStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/** Emits Unit whenever the server returns 401 — used to kick user to login screen. */
@Singleton
class AuthEventBus @Inject constructor() {
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired = _sessionExpired.asSharedFlow()
    fun notifyExpired() = _sessionExpired.tryEmit(Unit)
}

class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
    private val authEventBus: AuthEventBus
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.getToken()

        val deviceId = tokenStore.getDeviceId()
        val request = chain.request().newBuilder()
            .apply {
                if (!token.isNullOrEmpty()) {
                    addHeader("Authorization", "Bearer $token")
                }
                addHeader("X-Device-ID", deviceId ?: "")
            }
            .build()

        val response = chain.proceed(request)

        if (response.code == 401) {
            val path = request.url.encodedPath
            // Don't fire sessionExpired for auth endpoints — 401 there means
            // wrong credentials (wrong MPIN, wrong OTP), not an expired session
            val isAuthEndpoint = path.contains("login-mpin") ||
                    path.contains("verify-otp") ||
                    path.contains("send-otp") ||
                    path.contains("forgot-mpin") ||
                    path.contains("reset-mpin") ||
                    path.contains("register") ||
                    path.contains("refresh")
            // Only treat this as a SESSION EXPIRY if we actually sent a
            // token (i.e. the user was logged in) and the server rejected
            // it. A 401 on a request sent with NO token at all just means
            // "not logged in" — expected for a protected endpoint hit by
            // a background task before login, not a session that expired.
            val hadToken = !token.isNullOrEmpty()
            if (!isAuthEndpoint && hadToken) {
                tokenStore.clearToken()
                authEventBus.notifyExpired()
            }
        }

        return response
    }
}
// ── Notif count refresh bus ──────────────────────────────────
// Emitted by NotificationSettingsScreen when user reads/marks-all-read
// Consumed by DashboardViewModel to update badge immediately
object NotifCountBus {
    private val _refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refresh = _refresh.asSharedFlow()
    fun emit() { _refresh.tryEmit(Unit) }
}