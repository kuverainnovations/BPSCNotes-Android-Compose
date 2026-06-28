package com.example.bpscnotes.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedArticleDao {
    @Query("SELECT * FROM cached_articles ORDER BY rawDate DESC")
    suspend fun getAll(): List<CachedArticleEntity>

    @Query("SELECT * FROM cached_articles ORDER BY rawDate DESC")
    fun observeAll(): Flow<List<CachedArticleEntity>>

    @Query("SELECT * FROM cached_articles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CachedArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<CachedArticleEntity>)

    @Query("UPDATE cached_articles SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun updateBookmark(id: String, bookmarked: Boolean)

    @Query("DELETE FROM cached_articles")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM cached_articles")
    suspend fun count(): Int
}
