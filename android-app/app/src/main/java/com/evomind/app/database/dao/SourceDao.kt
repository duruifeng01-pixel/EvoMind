package com.evomind.app.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.Companion.REPLACE
import com.evomind.app.database.entity.SourceEntity
import kotlinx.coroutines.flow.Flow

/**
 * 素材数据访问对象
 */
@Dao
interface SourceDao {

    /**
     * 插入素材
     * @return 插入的素材ID
     */
    @Insert(onConflict = REPLACE)
    suspend fun insert(source: SourceEntity): Long

    /**
     * 批量插入素材
     */
    @Insert(onConflict = REPLACE)
    suspend fun insertAll(sources: List<SourceEntity>): List<Long>

    /**
     * 更新素材
     */
    @Update
    suspend fun update(source: SourceEntity)

    /**
     * 删除素材
     */
    @Delete
    suspend fun delete(source: SourceEntity)

    /**
     * 根据ID删除素材
     */
    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 删除所有素材
     */
    @Query("DELETE FROM sources")
    suspend fun deleteAll()

    /**
     * 根据ID查询素材
     */
    @Query("SELECT * FROM sources WHERE id = :id")
    fun getById(id: Long): Flow<SourceEntity?>

    /**
     * 查询所有素材（按创建时间倒序）
     */
    @Query("SELECT * FROM sources ORDER BY created_at DESC")
    fun getAll(): Flow<List<SourceEntity>>

    /**
     * 查询所有素材（LiveData）
     */
    @Query("SELECT * FROM sources ORDER BY created_at DESC")
    fun getAllLiveData(): LiveData<List<SourceEntity>>

    /**
     * 根据平台查询素材
     */
    @Query("SELECT * FROM sources WHERE platform = :platform ORDER BY created_at DESC")
    fun getByPlatform(platform: String): Flow<List<SourceEntity>>

    /**
     * 根据标题模糊查询素材
     */
    @Query("SELECT * FROM sources WHERE title LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun searchByTitle(query: String): Flow<List<SourceEntity>>

    /**
     * 根据标签查询素材
     */
    @Query("SELECT * FROM sources WHERE tags LIKE '%' || :tag || '%' ORDER BY created_at DESC")
    fun getByTag(tag: String): Flow<List<SourceEntity>>

    /**
     * 统计素材数量
     */
    @Query("SELECT COUNT(*) FROM sources")
    suspend fun getCount(): Int

    /**
     * 统计各平台的素材数量
     */
    @Query("SELECT platform, COUNT(*) as count FROM sources GROUP BY platform")
    suspend fun getCountByPlatform(): List<PlatformCount>

    /**
     * 按置信度排序查询素材
     */
    @Query("SELECT * FROM sources ORDER BY confidence DESC")
    fun getAllOrderByConfidence(): Flow<List<SourceEntity>>

    /**
     * 删除置信度低于阈值的素材
     */
    @Query("DELETE FROM sources WHERE confidence < :minConfidence")
    suspend fun deleteLowConfidence(minConfidence: Float)

    /**
     * 平台统计结果
     */
    data class PlatformCount(
        val platform: String,
        val count: Int
    )
}
