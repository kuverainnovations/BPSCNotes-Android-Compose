package com.example.bpscnotes.di

import com.example.bpscnotes.core.network.AuthInterceptor
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.BannersApiService
import com.example.bpscnotes.data.remote.api.CoursesApiService
import com.example.bpscnotes.data.remote.api.CurrentAffairsApiService
import com.example.bpscnotes.data.remote.api.DailyTargetsApiService
import com.example.bpscnotes.data.remote.api.QuizzesApiService
import com.example.bpscnotes.data.remote.api.UserStatsApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttp(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("http://192.168.66.186:5000/api/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideCurrentAffairsApi(retrofit: Retrofit): CurrentAffairsApiService =
        retrofit.create(CurrentAffairsApiService::class.java)

    @Provides
    @Singleton
    fun provideCoursesApi(retrofit: Retrofit): CoursesApiService =
        retrofit.create(CoursesApiService::class.java)

    @Provides
    @Singleton
    fun provideQuizzesApi(retrofit: Retrofit): QuizzesApiService =
        retrofit.create(QuizzesApiService::class.java)
    @Provides
    @Singleton
    fun provideBannerApi(retrofit: Retrofit): BannersApiService =
        retrofit.create(BannersApiService::class.java)

    @Provides
    @Singleton
    fun provideUserStatsApi(retrofit: Retrofit): UserStatsApiService =
        retrofit.create(UserStatsApiService::class.java)


    @Provides @Singleton
    fun provideDailyTargetsApi(r: Retrofit): DailyTargetsApiService =
        r.create(DailyTargetsApiService::class.java)
}