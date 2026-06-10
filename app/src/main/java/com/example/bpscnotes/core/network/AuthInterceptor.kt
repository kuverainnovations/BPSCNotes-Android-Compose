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

        val request = chain.request().newBuilder()
            .apply {
                if (!token.isNullOrEmpty()) {
                    addHeader("Authorization", "Bearer $token")
                }
            }
            .build()

        val response = chain.proceed(request)

        if (response.code == 401) {
            tokenStore.clearToken()
            // Notify all active screens that the session has expired
            authEventBus.notifyExpired()
        }

        return response
    }
}
