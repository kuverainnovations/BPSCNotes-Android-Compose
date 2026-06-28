package com.example.bpscnotes.di

import android.content.Context
import androidx.room.Room
import com.example.bpscnotes.data.local.AppDatabase
import com.example.bpscnotes.data.local.CachedArticleDao
import com.example.bpscnotes.data.local.Sm2CardDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "bpscnotes.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideCachedArticleDao(db: AppDatabase): CachedArticleDao = db.cachedArticleDao()

    @Provides
    @Singleton
    fun provideSm2CardDao(db: AppDatabase): Sm2CardDao = db.sm2CardDao()
}
