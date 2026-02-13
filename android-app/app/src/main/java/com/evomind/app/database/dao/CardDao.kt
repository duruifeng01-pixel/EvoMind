package com.evomind.app.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.evomind.app.database.entity.CardEntity
import kotlinx.coroutines.flow.Flow

/**
 * 认知卡片数据访问对象
 */
@Dao
interface CardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: CardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<CardEntity>): List<Long>

    @Update
    suspend fun update(card: CardEntity)

    @Delete
    suspend fun delete(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :cardId")
    suspend fun deleteById(cardId: Long)

    @Query("DELETE FROM cards WHERE source_id = :sourceId")
    suspend fun deleteBySourceId(sourceId: Long)

    @Query("DELETE FROM cards")
    suspend fun deleteAll()

    @Query("SELECT * FROM cards WHERE id = :cardId")
    fun getById(cardId: Long): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE source_id = :sourceId")
    fun getBySourceId(sourceId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY created_at DESC")
    fun getAll(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY created_at DESC")
    fun getAllLiveData(): LiveData<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE difficulty_level BETWEEN :minLevel AND :maxLevel ORDER BY created_at DESC")
    fun getByDifficultyRange(minLevel: Int, maxLevel: Int): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE next_review_at <= :currentTime ORDER BY next_review_at ASC")
    fun getCardsDueForReview(currentTime: Long = System.currentTimeMillis()): Flow<List<CardEntity>>

    @Query("UPDATE cards SET review_count = review_count + 1, last_reviewed_at = :reviewedAt, next_review_at = :nextReviewAt WHERE id = :cardId")
    suspend fun updateReviewSchedule(cardId: Long, reviewedAt: Long, nextReviewAt: Long)

    @Query("SELECT AVG(difficulty_level) FROM cards")
    suspend fun getAverageDifficulty(): Float

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM cards WHERE source_id = :sourceId")
    suspend fun getCountBySource(sourceId: Long): Int

    @Query("SELECT difficulty_level, COUNT(*) as count FROM cards GROUP BY difficulty_level")
    suspend fun getDifficultyDistribution(): List<DifficultyCount>

    @Query("SELECT * FROM cards WHERE title LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun search(query: String): Flow<List<CardEntity>>

    @Query("UPDATE cards SET ai_tags_json = :tags WHERE id = :cardId")
    suspend fun updateTags(cardId: Long, tags: String)

    @Query("UPDATE cards SET mind_map = :mindMap WHERE id = :cardId")
    suspend fun updateMindMap(cardId: Long, mindMap: String)

    data class DifficultyCount(
        val difficultyLevel: Int,
        val count: Int
    )
}