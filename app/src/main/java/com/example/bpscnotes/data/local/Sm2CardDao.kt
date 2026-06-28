package com.example.bpscnotes.data.local

import androidx.room.*

@Dao
interface Sm2CardDao {
    @Query("SELECT * FROM sm2_cards WHERE cardId = :id LIMIT 1")
    suspend fun get(id: String): Sm2CardEntity?

    @Query("SELECT * FROM sm2_cards WHERE nextReviewDate <= :today ORDER BY nextReviewDate ASC")
    suspend fun getDueCards(today: String): List<Sm2CardEntity>

    @Query("SELECT * FROM sm2_cards")
    suspend fun getAll(): List<Sm2CardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(card: Sm2CardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cards: List<Sm2CardEntity>)
}
