package com.evomind.app.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.evomind.app.database.entity.NodeCommentEntity
import kotlinx.coroutines.flow.Flow

/**
 * 节点评论数据访问对象
 */
@Dao
interface NodeCommentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: NodeCommentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<NodeCommentEntity>): List<Long>

    @Query("SELECT * FROM node_comments WHERE id = :id")
    fun getById(id: Long): Flow<NodeCommentEntity?>

    @Query("SELECT * FROM node_comments ORDER BY created_at DESC")
    fun getAll(): Flow<List<NodeCommentEntity>>

    @Query("SELECT * FROM node_comments WHERE card_id = :cardId ORDER BY created_at DESC")
    fun getByCardId(cardId: Long): Flow<List<NodeCommentEntity>>

    @Query("SELECT * FROM node_comments WHERE card_id = :cardId ORDER BY created_at DESC")
    fun getByCardIdLive(cardId: Long): LiveData<List<NodeCommentEntity>>

    @Query("SELECT * FROM node_comments WHERE node_id = :nodeId ORDER BY created_at DESC")
    fun getByNodeId(nodeId: String): Flow<List<NodeCommentEntity>>

    @Query("SELECT * FROM node_comments WHERE node_id = :nodeId ORDER BY created_at DESC")
    fun getByNodeIdLive(nodeId: String): LiveData<List<NodeCommentEntity>>

    @Query("SELECT * FROM node_comments WHERE parent_comment_id = :parentCommentId ORDER BY created_at DESC")
    fun getReplies(parentCommentId: Long): Flow<List<NodeCommentEntity>>

    @Query("SELECT COUNT(*) FROM node_comments WHERE card_id = :cardId")
    suspend fun getCountByCard(cardId: Long): Int

    @Query("SELECT COUNT(*) FROM node_comments WHERE node_id = :nodeId")
    suspend fun getCountByNode(nodeId: String): Int

    @Query("SELECT COUNT(DISTINCT node_id) FROM node_comments WHERE card_id = :cardId")
    suspend fun getDistinctNodeCount(cardId: Long): Int

    @Delete
    suspend fun delete(comment: NodeCommentEntity)

    @Query("DELETE FROM node_comments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM node_comments WHERE card_id = :cardId")
    suspend fun deleteByCardId(cardId: Long)

    @Query("DELETE FROM node_comments WHERE node_id = :nodeId")
    suspend fun deleteByNodeId(nodeId: String)

    @Query("DELETE FROM node_comments")
    suspend fun deleteAll()

    @Update
    suspend fun update(comment: NodeCommentEntity)

    @Query("UPDATE node_comments SET comment_text = :commentText, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateComment(id: Long, commentText: String, updatedAt: Long)

    @Query("SELECT * FROM node_comments WHERE comment_text LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun search(query: String): Flow<List<NodeCommentEntity>>

    /**
     * 节点评论统计
     */
    data class NodeCommentCount(
        val nodeId: String,
        val count: Int
    )

    @Query("SELECT node_id, COUNT(*) as count FROM node_comments WHERE card_id = :cardId GROUP BY node_id")
    suspend fun getCommentCountByNode(cardId: Long): List<NodeCommentCount>

    /**
     * 每日评论统计
     */
    data class DailyCommentCount(
        val timestamp: Long, // 当天的开始时间戳
        val count: Int
    )

    @Query("""
        SELECT
            date(datetime(created_at / 1000, 'unixepoch')) as day,
            COUNT(*) as count
        FROM node_comments
        WHERE card_id = :cardId
        GROUP BY day
        ORDER BY day DESC
    """)
    suspend fun getDailyCommentStatistics(cardId: Long): List<DailyCommentCount>
}
