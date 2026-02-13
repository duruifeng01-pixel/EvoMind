package com.evomind.app.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.evomind.app.database.entity.DiscussionEntity
import kotlinx.coroutines.flow.Flow

/**
 * AI讨论会话数据访问对象
 */
@Dao
interface DiscussionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(discussion: DiscussionEntity): Long

    @Update
    suspend fun update(discussion: DiscussionEntity)

    @Delete
    suspend fun delete(discussion: DiscussionEntity)

    @Query("DELETE FROM ai_discussions WHERE id = :discussionId")
    suspend fun deleteById(discussionId: Long)

    @Query("DELETE FROM ai_discussions WHERE card_id = :cardId")
    suspend fun deleteByCardId(cardId: Long)

    @Query("DELETE FROM ai_discussions")
    suspend fun deleteAll()

    @Query("SELECT * FROM ai_discussions WHERE id = :discussionId")
    fun getById(discussionId: Long): Flow<DiscussionEntity?>

    @Query("SELECT * FROM ai_discussions WHERE id = :discussionId")
    fun getByIdLive(discussionId: Long): LiveData<DiscussionEntity?>

    @Query("SELECT * FROM ai_discussions WHERE card_id = :cardId ORDER BY last_message_at DESC")
    fun getByCardId(cardId: Long): Flow<List<DiscussionEntity>>

    @Query("SELECT * FROM ai_discussions WHERE card_id = :cardId ORDER BY last_message_at DESC")
    fun getByCardIdLive(cardId: Long): LiveData<List<DiscussionEntity>>

    @Query("SELECT * FROM ai_discussions ORDER BY last_message_at DESC")
    fun getAll(): Flow<List<DiscussionEntity>>

    @Query("SELECT * FROM ai_discussions WHERE is_active = 1 ORDER BY last_message_at DESC")
    fun getActiveDiscussions(): Flow<List<DiscussionEntity>>

    @Query("SELECT * FROM ai_discussions WHERE card_id = :cardId AND is_active = 1 ORDER BY last_message_at DESC LIMIT 1")
    suspend fun getActiveDiscussionByCard(cardId: Long): DiscussionEntity?

    @Query("UPDATE ai_discussions SET is_active = 0 WHERE id != :discussionId AND card_id = :cardId")
    suspend fun deactivateOtherDiscussions(discussionId: Long, cardId: Long)

    @Query("UPDATE ai_discussions SET is_active = 0 WHERE id = :discussionId")
    suspend fun deactivateDiscussion(discussionId: Long)

    @Query("UPDATE ai_discussions SET last_message_at = :timestamp, updated_at = :timestamp WHERE id = :discussionId")
    suspend fun updateLastMessageTime(discussionId: Long, timestamp: Long)

    @Query("SELECT COUNT(*) FROM ai_discussions WHERE card_id = :cardId")
    suspend fun getCountByCard(cardId: Long): Int

    @Query("SELECT COUNT(*) FROM ai_discussions")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM ai_discussions WHERE is_active = 1")
    suspend fun getActiveCount(): Int

    @Query("SELECT * FROM ai_discussions WHERE session_title LIKE '%' || :query || '%' ORDER BY last_message_at DESC")
    fun search(query: String): Flow<List<DiscussionEntity>>

    /**
     * 每日讨论会话统计
     */
    data class DailyDiscussionCount(
        val timestamp: Long, // 当天的开始时间戳
        val count: Int
    )

    @Query("""
        SELECT
            date(datetime(created_at / 1000, 'unixepoch')) as day,
            COUNT(*) as count
        FROM ai_discussions
        GROUP BY day
        ORDER BY day DESC
    """)
    suspend fun getDailyStatistics(): List<DailyDiscussionCount>

    /**
     * 卡片讨论活跃度统计
     */
    data class CardDiscussionStats(
        val cardId: Long,
        val discussionCount: Int,
        val lastDiscussionAt: Long
    )

    @Query("""
        SELECT
            card_id as cardId,
            COUNT(*) as discussionCount,
            MAX(last_message_at) as lastDiscussionAt
        FROM ai_discussions
        GROUP BY card_id
        ORDER BY lastDiscussionAt DESC
    """)
    suspend fun getCardDiscussionStats(): List<CardDiscussionStats>
}
