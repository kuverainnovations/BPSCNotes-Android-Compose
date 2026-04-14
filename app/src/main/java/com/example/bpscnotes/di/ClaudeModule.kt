package com.example.bpscnotes.di

import com.example.bpscnotes.data.remote.api.ClaudeApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ClaudeModule {

    // IMPORTANT: Replace with your actual Anthropic API key
    // For production, load this from BuildConfig or encrypted storage — never hardcode in git
    private const val CLAUDE_API_KEY = "sk-ant-YOUR_KEY_HERE"
    private const val CLAUDE_BASE_URL = "https://api.anthropic.com/v1/"

    @Provides
    @Singleton
    @Named("claude")
    fun provideClaudeOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-api-key", CLAUDE_API_KEY)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("content-type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @Named("claude")
    fun provideClaudeRetrofit(@Named("claude") client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(CLAUDE_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideClaudeApiService(@Named("claude") retrofit: Retrofit): ClaudeApiService =
        retrofit.create(ClaudeApiService::class.java)
}
