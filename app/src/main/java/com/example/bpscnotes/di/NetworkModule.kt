package com.example.bpscnotes.di

import com.example.bpscnotes.core.network.AuthInterceptor
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.BannersApiService
import com.example.bpscnotes.data.remote.api.CoinsApiService
import com.example.bpscnotes.data.remote.api.CoursesApiService
import com.example.bpscnotes.data.remote.api.CurrentAffairsApiService
import com.example.bpscnotes.data.remote.api.DailyTargetsApiService
import com.example.bpscnotes.data.remote.api.JobsApiService
import com.example.bpscnotes.data.remote.api.LiveClassesApiService
import com.example.bpscnotes.data.remote.api.QuizzesApiService
import com.example.bpscnotes.data.remote.api.StudyRoomsApiService
import com.example.bpscnotes.data.remote.api.TierRoomsApiService
import com.example.bpscnotes.data.remote.api.ChatApiService
import com.example.bpscnotes.data.remote.api.AchievementsApiService
import com.example.bpscnotes.data.remote.api.ChallengesApiService
import com.example.bpscnotes.data.remote.api.UserStatsApiService
import com.example.bpscnotes.data.remote.api.StudyMaterialsApiService
import com.example.bpscnotes.data.remote.api.MarketplaceApiService
import com.example.bpscnotes.presentation.nofification.NotificationsApiService
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

import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.bpscnotes.in/api/v1/"
//    private const val BASE_URL = "https://api-stg.bpscnotes.in/api/v1/"

    // ── Standard client — 30s timeout for all normal API calls ──
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
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    // ── Upload client — 10 min timeout for large video/file uploads ──
    @Provides
    @Singleton
    @Named("upload")
    fun provideUploadOkHttp(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.MINUTES)
            .readTimeout(2, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    // ── Separate Retrofit instance for uploads ────────────────
    @Provides
    @Singleton
    @Named("upload")
    fun provideUploadRetrofit(@Named("upload") client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
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

    @Provides @Singleton fun provideLiveClassesApi(r: Retrofit): LiveClassesApiService = r.create(LiveClassesApiService::class.java)
    @Provides @Singleton fun provideJobsApi(r: Retrofit): JobsApiService = r.create(JobsApiService::class.java)
    @Provides @Singleton fun provideStudyRoomsApi(r: Retrofit): StudyRoomsApiService = r.create(StudyRoomsApiService::class.java)
    @Provides @Singleton fun provideCoinsApi(r: Retrofit): CoinsApiService = r.create(CoinsApiService::class.java)

    @Provides @Singleton
    fun provideFlashcardsApi(r: Retrofit): CoinsApiService.FlashcardsApiService =
        r.create(CoinsApiService.FlashcardsApiService::class.java)

    // ── Tier Rooms (Phase 1 — study sessions + tier progression) ──
    @Provides @Singleton
    fun provideTierRoomsApi(r: Retrofit): TierRoomsApiService =
        r.create(TierRoomsApiService::class.java)

    @Provides @Singleton
    fun provideAchievementsApi(r: Retrofit): AchievementsApiService =
        r.create(AchievementsApiService::class.java)

    @Provides @Singleton
    fun provideChallengesApi(r: Retrofit): ChallengesApiService =
        r.create(ChallengesApiService::class.java)

    @Provides @Singleton
    fun provideChatApi(r: Retrofit): ChatApiService =
        r.create(ChatApiService::class.java)

    @Provides @Singleton
    fun provideStudyMaterialsApi(@Named("upload") r: Retrofit): StudyMaterialsApiService =
        r.create(StudyMaterialsApiService::class.java)

    @Provides @Singleton
    fun provideMarketplaceApi(r: Retrofit): MarketplaceApiService =
        r.create(MarketplaceApiService::class.java)

    @Provides @Singleton
    fun provideNotificationsApi(r: Retrofit): NotificationsApiService =
        r.create(NotificationsApiService::class.java)

}