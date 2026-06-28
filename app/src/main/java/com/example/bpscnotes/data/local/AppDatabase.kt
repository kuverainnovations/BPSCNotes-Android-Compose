package com.example.bpscnotes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities  = [CachedArticleEntity::class, Sm2CardEntity::class],
    version   = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedArticleDao(): CachedArticleDao
    abstract fun sm2CardDao(): Sm2CardDao
}
