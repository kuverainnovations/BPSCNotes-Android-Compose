package com.example.bpscnotes.di

import android.content.Context
import com.example.bpscnotes.core.network.AuthInterceptor
import com.example.bpscnotes.data.remote.api.*
import com.bpscnotes.app.BuildConfig
import com.example.bpscnotes.presentation.nofification.NotificationsApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {


    // ── Retry GET requests only — never POST/auth ─────────────
    private val retryInterceptor = Interceptor { chain ->
        val request = chain.request()
        if (request.method != "GET") return@Interceptor chain.proceed(request)
        var attempt = 0
        var lastException: IOException? = null
        while (attempt < 3) {
            try {
                val response = chain.proceed(request)
                if (response.isSuccessful || response.code in 400..499) return@Interceptor response
                response.close()
            } catch (e: IOException) {
                lastException = e
            }
            attempt++
            if (attempt < 3) Thread.sleep(500L * (1 shl attempt))
        }
        throw (lastException ?: IOException("Request failed after 3 attempts"))
    }

    // ── Offline interceptor: serve cached GET responses when no internet ──
    // Only applies to GET — never to POST/login/OTP
    // Only serves previously SUCCESSFUL (200) cached responses
    private fun offlineInterceptor(context: Context) = Interceptor { chain ->
        val request = chain.request()

        // Never interfere with POST requests
        if (request.method != "GET") return@Interceptor chain.proceed(request)

        // If online — proceed normally
        if (isOnline(context)) return@Interceptor chain.proceed(request)

        // Offline — try to serve from cache (up to 7 days stale)
        val offlineRequest = request.newBuilder()
            .header("Cache-Control", "public, only-if-cached, max-stale=${7 * 24 * 60 * 60}")
            .build()
        try {
            val response = chain.proceed(offlineRequest)
            // Only return if it's a valid cached response
            if (response.code == 504) {
                // 504 = no cached response available — return as-is (will show error)
                response.close()
                return@Interceptor chain.proceed(request)
            }
            return@Interceptor response
        } catch (e: Exception) {
            return@Interceptor chain.proceed(request)
        }
    }

    // ── Online interceptor: cache successful GET responses for 5 min ──
    // ONLY caches 200 responses — never caches errors.
    // After any successful mutation (POST/PUT/PATCH/DELETE), evicts related
    // GET cache entries so the next load always hits the server fresh.
    private fun onlineInterceptor(cache: Cache) = Interceptor { chain ->
        val request  = chain.request()
        val response = chain.proceed(request)

        // After successful mutation — evict related cached GETs
        if (request.method != "GET" && response.isSuccessful) {
            val url = request.url.toString()
            // Evict GET caches for the same base path and user-level aggregates
            val toEvict = mutableListOf<String>()
            // Always evict the base path of the mutated resource
            val basePath = request.url.encodedPath
                .substringBefore("/:").split("/")
                .take(5).joinToString("/")
            toEvict.add(basePath)
            // Always evict user stats and profile since most mutations affect them
            toEvict.addAll(listOf("/api/v1/users", "/api/v1/auth/me", "/api/v1/coins"))
            // Evict by pattern
            try {
                val iter = cache.urls()
                while (iter.hasNext()) {
                    val cached = iter.next()
                    if (toEvict.any { cached.contains(it) }) iter.remove()
                }
            } catch (_: Exception) {}
        }

        // Cache successful GET responses for 5 minutes
        if (request.method == "GET" && response.code == 200) {
            val maxAge = 5 * 60 // 5 minutes
            return@Interceptor response.newBuilder()
                .header("Cache-Control", "public, max-age=$maxAge")
                .removeHeader("Pragma")
                .build()
        }

        // For errors — explicitly tell OkHttp NOT to cache
        if (!response.isSuccessful) {
            return@Interceptor response.newBuilder()
                .header("Cache-Control", "no-store")
                .removeHeader("Pragma")
                .build()
        }

        response
    }

    // Exposed so CacheInvalidator can evict entries by URL pattern
    @Provides
    @Singleton
    fun provideHttpCache(@ApplicationContext context: Context): Cache =
        Cache(File(context.cacheDir, "http_cache"), 10L * 1024 * 1024)

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? android.net.ConnectivityManager ?: return true
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps    = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }

    @Provides
    @Singleton
    fun provideOkHttp(
        authInterceptor: AuthInterceptor,
        @ApplicationContext context: Context,
        cache: Cache
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(authInterceptor)
            .addInterceptor(retryInterceptor)
            // offlineInterceptor goes as application interceptor (before network)
            .addInterceptor(offlineInterceptor(context))
            // onlineInterceptor goes as network interceptor (sees real response)
            .addNetworkInterceptor(onlineInterceptor(cache))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                        else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("upload")
    fun provideUploadOkHttp(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                        else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.MINUTES)
            .readTimeout(2, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton @Named("upload")
    fun provideUploadRetrofit(@Named("upload") client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton fun provideAuthApi(r: Retrofit): AuthApiService = r.create(AuthApiService::class.java)
    @Provides @Singleton fun provideCurrentAffairsApi(r: Retrofit): CurrentAffairsApiService = r.create(CurrentAffairsApiService::class.java)
    @Provides @Singleton fun provideCoursesApi(r: Retrofit): CoursesApiService = r.create(CoursesApiService::class.java)
    @Provides @Singleton fun provideQuizzesApi(r: Retrofit): QuizzesApiService = r.create(QuizzesApiService::class.java)
    @Provides @Singleton fun provideBannerApi(r: Retrofit): BannersApiService = r.create(BannersApiService::class.java)
    @Provides @Singleton fun provideUserStatsApi(r: Retrofit): UserStatsApiService = r.create(UserStatsApiService::class.java)
    @Provides @Singleton fun provideDailyTargetsApi(r: Retrofit): DailyTargetsApiService = r.create(DailyTargetsApiService::class.java)
    @Provides @Singleton fun provideLiveClassesApi(r: Retrofit): LiveClassesApiService = r.create(LiveClassesApiService::class.java)
    @Provides @Singleton fun provideCertificatesApi(r: Retrofit): CertificatesApiService = r.create(CertificatesApiService::class.java)
    @Provides @Singleton fun provideJobsApi(r: Retrofit): JobsApiService = r.create(JobsApiService::class.java)
    @Provides @Singleton fun provideStudyRoomsApi(r: Retrofit): StudyRoomsApiService = r.create(StudyRoomsApiService::class.java)
    @Provides @Singleton fun provideCoinsApi(r: Retrofit): CoinsApiService = r.create(CoinsApiService::class.java)
    @Provides @Singleton fun provideFlashcardsApi(r: Retrofit): CoinsApiService.FlashcardsApiService = r.create(CoinsApiService.FlashcardsApiService::class.java)
    @Provides @Singleton fun provideTierRoomsApi(r: Retrofit): TierRoomsApiService = r.create(TierRoomsApiService::class.java)
    @Provides @Singleton fun provideAchievementsApi(r: Retrofit): AchievementsApiService = r.create(AchievementsApiService::class.java)
    @Provides @Singleton fun provideChallengesApi(r: Retrofit): ChallengesApiService = r.create(ChallengesApiService::class.java)
    @Provides @Singleton fun provideChatApi(r: Retrofit): ChatApiService = r.create(ChatApiService::class.java)
    @Provides @Singleton fun provideStudyMaterialsApi(@Named("upload") r: Retrofit): StudyMaterialsApiService = r.create(StudyMaterialsApiService::class.java)
    @Provides @Singleton fun provideMaterialChatApi(r: Retrofit): MaterialChatApiService = r.create(MaterialChatApiService::class.java)
    @Provides @Singleton fun provideMarketplaceApi(r: Retrofit): MarketplaceApiService = r.create(MarketplaceApiService::class.java)
    @Provides @Singleton fun provideNotificationsApi(r: Retrofit): NotificationsApiService = r.create(NotificationsApiService::class.java)
    @Provides @Singleton fun provideGlobalLeaderboardApi(r: Retrofit): GlobalLeaderboardApiService = r.create(GlobalLeaderboardApiService::class.java)
    @Provides @Singleton fun provideStudySessionHistoryApi(r: Retrofit): StudySessionHistoryApiService = r.create(StudySessionHistoryApiService::class.java)
    @Provides @Singleton fun provideSearchApi(r: Retrofit): SearchApiService = r.create(SearchApiService::class.java)
    @Provides @Singleton fun provideBookmarksApi(r: Retrofit): BookmarksApiService = r.create(BookmarksApiService::class.java)
    @Provides @Singleton fun provideCoinStoreApi(r: Retrofit): CoinStoreApiService = r.create(CoinStoreApiService::class.java)
}