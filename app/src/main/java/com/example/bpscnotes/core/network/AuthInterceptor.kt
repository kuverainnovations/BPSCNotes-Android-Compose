package com.example.bpscnotes.core.network

import android.util.Log
import com.example.bpscnotes.data.local.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        // Get token
        val token = runBlocking {
            tokenStore.getToken()
        }

        Log.d("AUTH_INTERCEPTOR", "TOKEN = $token")

        // Add auth header
        val request = chain.request()
            .newBuilder()
            .apply {
                if (!token.isNullOrBlank()) {
                    addHeader("Authorization", "Bearer $token")
                }
            }
            .build()

        Log.d(
            "AUTH_INTERCEPTOR",
            "URL = ${request.url} | AUTH = ${request.header("Authorization")}"
        )

        val response = chain.proceed(request)

        Log.d(
            "AUTH_INTERCEPTOR",
            "RESPONSE CODE = ${response.code}"
        )

        // IMPORTANT:
        // Don't auto-clear token for now.
        // FCM sync request may fail temporarily.
        // if (response.code == 401) {
        //     tokenStore.clearToken()
        // }

        return response
    }
}
