package com.evomind.app.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.evomind.app.database.entity.MindMapFavoriteEntity
import kotlinx.coroutines.flow.Flow

/**
 * 思维导图节点收藏数据访问对象
 */
@Dao
interface MindMapFavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: MindMapFavoriteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favorites: List<MindMapFavoriteEntity>): List<Long>

    @Query("SELECT * FROM mindmap_favorites WHERE id = :id")
    fun getById(id: Long): Flow<MindMapFavoriteEntity?>

    @Query("SELECT * FROM mindmap_favorites ORDER BY favorited_at DESC")
    fun getAll(): Flow<List<MindMapFavoriteEntity>>

    @Query("SELECT * FROM mindmap_favorites WHERE card_id = :cardId ORDER BY favorited_at DESC")
    fun getByCardId(cardId: Long): Flow<List<MindMapFavoriteEntity>>

    @Query("SELECT * FROM mindmap_favorites WHERE card_id = :cardId ORDER BY favorited_at DESC")
    fun getByCardIdLive(cardId: Long): LiveData<List<MindMapFavoriteEntity>>

    @Query("SELECT * FROM mindmap_favorites WHERE node_id = :nodeId AND card_id = :cardId")
    suspend fun getByNodeIdAndCardId(nodeId: String, cardId: Long): MindMapFavoriteEntity?

    @Query("SELECT COUNT(*) FROM mindmap_favorites WHERE card_id = :cardId")
    suspend fun getCountByCard(cardId: Long): Int

    @Query("SELECT COUNT(*) FROM mindmap_favorites")
    suspend fun getTotalCount(): Int

    @Delete
    suspend fun delete(favorite: MindMapFavoriteEntity)

    @Query("DELETE FROM mindmap_favorites WHERE node_id = :nodeId AND card_id = :cardId")
    suspend fun deleteByNodeIdAndCardId(nodeId: String, cardId: Long)

    @Query("DELETE FROM mindmap_favorites WHERE card_id = :cardId")
    suspend fun deleteByCardId(cardId: Long)

    @Query("DELETE FROM mindmap_favorites")
    suspend fun deleteAll()

    @Update
    suspend fun update(favorite: MindMapFavoriteEntity)

    @Query("""
        UPDATE mindmap_favorites
        SET notes = :notes
        WHERE node_id = :nodeId AND card_id = :cardId
    """)
    suspend fun updateNotes(nodeId: String, cardId: Long, notes: String)

    @Query("SELECT * FROM mindmap_favorites WHERE node_text LIKE '%' || :query || '%' ORDER BY favorited_at DESC")
    fun search(query: String): Flow<List<MindMapFavoriteEntity>>

    data class FavoriteTagCount(
        val tag: String,
        val count: Int
    )

    @Query("""
        SELECT
            SUBSTR(notes, INSTR(notes, 'tags') + 8, INSTR(SUBSTR(notes, INSTR(notes, 'tags')), ']') - 9) as tag,
            COUNT(*) as count
        FROM mindmap_favorites
        WHERE notes LIKE '%tags%'
        GROUP BY tag
        ORDER BY count DESC
    """)
    suspend fun getTagDistribution(): List<FavoriteTagCount>

    data class MonthlyFavoriteCount(
        val year: Int,
        val month: Int,
        val count: Int
    )

    @Query("""
        SELECT
            strftime('%Y', datetime(favorited_at / 1000, 'unixepoch')) as year,
            strftime('%m', datetime(favorited_at / 1000, 'unixepoch')) as month,
            COUNT(*) as count
        FROM mindmap_favorites
        GROUP BY year, month
        ORDER BY year DESC, month DESC
    """)
    suspend fun getMonthlyStatistics(): List<MonthlyFavoriteCount>
}