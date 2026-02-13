package com.evomind.app.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.evomind.app.database.entity.ReviewSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 复习会话数据访问对象
 */
@Dao
interface ReviewSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ReviewSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<ReviewSessionEntity>): List<Long>

    @Update
    suspend fun update(session: ReviewSessionEntity)

    @Query("DELETE FROM review_sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: Long)

    @Query("DELETE FROM review_sessions WHERE card_id = :cardId")
    suspend fun deleteByCardId(cardId: Long)

    @Query("DELETE FROM review_sessions")
    suspend fun deleteAll()

    @Query("SELECT * FROM review_sessions WHERE id = :sessionId")
    fun getById(sessionId: Long): Flow<ReviewSessionEntity?>

    @Query("SELECT * FROM review_sessions WHERE card_id = :cardId ORDER BY reviewed_at DESC")
    fun getByCardId(cardId: Long): Flow<List<ReviewSessionEntity>>

    @Query("SELECT * FROM review_sessions WHERE card_id = :cardId ORDER BY reviewed_at DESC LIMIT :limit")
    fun getByCardIdLimit(cardId: Long, limit: Int): Flow<List<ReviewSessionEntity>>

    @Query("SELECT * FROM review_sessions ORDER BY reviewed_at DESC")
    fun getAll(): Flow<List<ReviewSessionEntity>>

    @Query("""
        SELECT rs.* FROM review_sessions rs
        WHERE rs.reviewed_at >= :startTime
        ORDER BY rs.reviewed_at DESC
    """)
    fun getSessionsSince(startTime: Long): Flow<List<ReviewSessionEntity>>

    @Query("""
        SELECT rs.* FROM review_sessions rs
        WHERE rs.reviewed_at >= :startTime AND rs.reviewed_at <= :endTime
        ORDER BY rs.reviewed_at DESC
    """)
    fun getSessionsBetween(startTime: Long, endTime: Long): Flow<List<ReviewSessionEntity>>

    @Query("SELECT COUNT(*) FROM review_sessions WHERE card_id = :cardId")
    suspend fun getReviewCountByCard(cardId: Long): Int

    @Query("SELECT COUNT(*) FROM review_sessions WHERE reviewed_at >= :startTime")
    suspend fun getReviewCountSince(startTime: Long): Int

    @Query("""
        SELECT AVG(quality) FROM review_sessions
        WHERE card_id = :cardId
    """)
    suspend fun getAverageQualityByCard(cardId: Long): Float?

    @Query("""
        SELECT AVG(ease_factor) FROM review_sessions
        WHERE card_id = :cardId
    """)
    suspend fun getAverageEaseFactorByCard(cardId: Long): Float?

    @Query("SELECT session_type, COUNT(*) as count FROM review_sessions GROUP BY session_type")
    suspend fun getReviewTypeDistribution(): List<ReviewTypeCount>

    @Query("SELECT COUNT(DISTINCT card_id) FROM review_sessions WHERE reviewed_at >= :startTime")
    suspend fun getDistinctCardsReviewedSince(startTime: Long): Int

    data class ReviewTypeCount(
        val sessionType: ReviewSessionEntity.ReviewSessionType,
        val count: Int
    )
}