package com.evomind.app.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.evomind.app.database.entity.DiscussionMessageEntity
import com.evomind.app.database.entity.MessageSenderType
import kotlinx.coroutines.flow.Flow

/**
 * 讨论消息数据访问对象
 */
@Dao
interface DiscussionMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: DiscussionMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<DiscussionMessageEntity>): List<Long>

    @Update
    suspend fun update(message: DiscussionMessageEntity)

    @Delete
    suspend fun delete(message: DiscussionMessageEntity)

    @Query("DELETE FROM discussion_messages WHERE id = :messageId")
    suspend fun deleteById(messageId: Long)

    @Query("DELETE FROM discussion_messages WHERE discussion_id = :discussionId")
    suspend fun deleteByDiscussionId(discussionId: Long)

    @Query("DELETE FROM discussion_messages WHERE discussion_id IN (SELECT id FROM ai_discussions WHERE card_id = :cardId)")
    suspend fun deleteByCardId(cardId: Long)

    @Query("DELETE FROM discussion_messages")
    suspend fun deleteAll()

    @Query("SELECT * FROM discussion_messages WHERE id = :messageId")
    fun getById(messageId: Long): Flow<DiscussionMessageEntity?>

    @Query("SELECT * FROM discussion_messages WHERE discussion_id = :discussionId ORDER BY message_index")
    fun getByDiscussionId(discussionId: Long): Flow<List<DiscussionMessageEntity>>

    @Query("SELECT * FROM discussion_messages WHERE discussion_id = :discussionId ORDER BY message_index")
    fun getByDiscussionIdLive(discussionId: Long): LiveData<List<DiscussionMessageEntity>>

    @Query("SELECT * FROM discussion_messages WHERE discussion_id = :discussionId ORDER BY message_index DESC LIMIT 1")
    suspend fun getLastMessage(discussionId: Long): DiscussionMessageEntity?

    @Query("SELECT * FROM discussion_messages WHERE discussion_id = :discussionId AND sender_type = :senderType ORDER BY message_index DESC LIMIT 1")
    suspend fun getLastMessageBySender(discussionId: Long, senderType: MessageSenderType): DiscussionMessageEntity?

    @Query("SELECT COUNT(*) FROM discussion_messages WHERE discussion_id = :discussionId")
    suspend fun getCountByDiscussion(discussionId: Long): Int

    @Query("SELECT COUNT(*) FROM discussion_messages WHERE sender_type = :senderType AND discussion_id = :discussionId")
    suspend fun getCountBySender(discussionId: Long, senderType: MessageSenderType): Int

    @Query("SELECT COUNT(*) FROM discussion_messages WHERE discussion_id IN (SELECT id FROM ai_discussions WHERE card_id = :cardId)")
    suspend fun getCountByCard(cardId: Long): Int

    @Query("""
        SELECT COUNT(DISTINCT discussion_id)
        FROM discussion_messages
        WHERE sender_type = 'USER' AND discussion_id IN (SELECT id FROM ai_discussions WHERE card_id = :cardId)
    """)
    suspend fun getUserParticipatedDiscussions(cardId: Long): Int

    @Query("""
        SELECT AVG(token_usage) as avgTokenUsage,
               MIN(token_usage) as minTokenUsage,
               MAX(token_usage) as maxTokenUsage
        FROM discussion_messages
        WHERE sender_type = 'AI' AND token_usage IS NOT NULL
    """)
    suspend fun getTokenUsageStats(): TokenUsageStats?

    /**
     * Token使用量统计
     */
    data class TokenUsageStats(
        val avgTokenUsage: Double,
        val minTokenUsage: Int,
        val maxTokenUsage: Int
    )

    /**
     * 获取讨论消息的统计信息
     */
    data class MessageStats(
        val totalMessages: Int,
        val userMessages: Int,
        val aiMessages: Int,
        val avgMessageLength: Double
    )

    @Query("""
        SELECT
            COUNT(*) as totalMessages,
            SUM(CASE WHEN sender_type = 'USER' THEN 1 ELSE 0 END) as userMessages,
            SUM(CASE WHEN sender_type = 'AI' THEN 1 ELSE 0 END) as aiMessages,
            AVG(LENGTH(message_content)) as avgMessageLength
        FROM discussion_messages
        WHERE discussion_id = :discussionId
    """)
    suspend fun getMessageStats(discussionId: Long): MessageStats?

    /**
     * 重置讨论会话（清除所有消息，保留会话）
     */
    @Transaction
    suspend fun resetDiscussion(discussionId: Long) {
        deleteByDiscussionId(discussionId)

        // 添加一条系统消息标记重置
        val resetMessage = DiscussionMessageEntity(
            id = 0,
            discussionId = discussionId,
            messageIndex = 0,
            senderType = MessageSenderType.SYSTEM,
            messageContent = "讨论会话已重置"
        )
        insert(resetMessage)
    }
}