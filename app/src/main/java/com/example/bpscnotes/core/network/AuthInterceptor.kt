package com.example.bpscnotes.core.network

import com.example.bpscnotes.data.local.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // getToken() is now plain synchronous — no runBlocking needed
        val token = tokenStore.getToken()

        val request = chain.request().newBuilder()
            .apply {
                if (!token.isNullOrEmpty()) {
                    addHeader("Authorization", "Bearer $token")
                }
            }
            .build()

        val response = chain.proceed(request)

        // If server rejects token, clear session so app can redirect to login
        if (response.code == 401) {
            tokenStore.clearToken()
        }

        return response
    }
}
