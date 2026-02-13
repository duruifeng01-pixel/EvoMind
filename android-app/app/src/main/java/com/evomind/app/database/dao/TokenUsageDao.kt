package com.evomind.app.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.evomind.app.database.entity.AIServiceType
import com.evomind.app.database.entity.TokenUsageRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TokenUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usage: TokenUsageRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(usages: List<TokenUsageRecordEntity>): List<Long>

    @Update
    suspend fun update(usage: TokenUsageRecordEntity)

    @Delete
    suspend fun delete(usage: TokenUsageRecordEntity)

    @Query("SELECT * FROM token_usages WHERE id = :id")
    fun getById(id: Long): Flow<TokenUsageRecordEntity?>

    @Query("SELECT * FROM token_usages WHERE user_id = :userId ORDER BY created_at DESC")
    fun getByUserId(userId: Long): Flow<List<TokenUsageRecordEntity>>

    @Query("SELECT * FROM token_usages WHERE user_id = :userId ORDER BY created_at DESC")
    fun getByUserIdLive(userId: Long): LiveData<List<TokenUsageRecordEntity>>

    @Query("SELECT * FROM token_usages WHERE user_id = :userId AND service_type = :serviceType ORDER BY created_at DESC")
    fun getByUserIdAndServiceType(userId: Long, serviceType: AIServiceType): Flow<List<TokenUsageRecordEntity>>

    @Query("SELECT * FROM token_usages WHERE user_id = :userId AND created_at >= :startTime ORDER BY created_at DESC")
    fun getUsagesSince(userId: Long, startTime: Long): Flow<List<TokenUsageRecordEntity>>

    @Query("SELECT SUM(tokens_used) FROM token_usages WHERE user_id = :userId")
    fun getTotalUsedTokens(userId: Long): Flow<Long?>

    @Query("SELECT SUM(tokens_used) FROM token_usages WHERE user_id = :userId AND service_type = :serviceType")
    suspend fun getTotalUsedTokensByService(userId: Long, serviceType: AIServiceType): Long?

    @Query("SELECT SUM(tokens_used) FROM token_usages WHERE user_id = :userId AND created_at >= :startTime")
    suspend fun getTotalUsedTokensSince(userId: Long, startTime: Long): Long?

    @Query("SELECT AVG(tokens_used) FROM token_usages WHERE user_id = :userId AND created_at >= :startTime")
    suspend fun getAverageDailyUsage(userId: Long, startTime: Long): Double?

    @Query("DELETE FROM token_usages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM token_usages WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: Long)

    @Query("DELETE FROM token_usages")
    suspend fun deleteAll()

    /**
     * Token使用统计（按服务类型）
     */
    data class TokenUsageByService(
        val serviceType: AIServiceType,
        val totalTokens: Int,
        val usageCount: Int
    )

    @Query("""
        SELECT
            service_type,
            SUM(tokens_used) as totalTokens,
            COUNT(*) as usageCount
        FROM token_usages
        WHERE user_id = :userId AND created_at >= :startTime
        GROUP BY service_type
        ORDER BY totalTokens DESC
    """)
    suspend fun getUsageStats(userId: Long, startTime: Long): List<TokenUsageByService>

    /**
     * 每日Token使用统计
     */
    data class DailyTokenUsage(
        val date: String,
        val tokensUsed: Int
    )

    @Query("""
        SELECT
            date(datetime(created_at / 1000, 'unixepoch')) as date,
            SUM(tokens_used) as tokensUsed
        FROM token_usages
        WHERE user_id = :userId
        GROUP BY date
        ORDER BY date DESC
    """)
    suspend fun getDailyTokenUsage(userId: Long): List<DailyTokenUsage>

    @Query("""
        SELECT created_at, tokens_used, service_type, description
        FROM token_usages
        WHERE user_id = :userId
          AND (service_type LIKE '%' || :query || '%'
               OR description LIKE '%' || :query || '%')
        ORDER BY created_at DESC
    """)
    fun search(userId: Long, query: String): Flow<List<TokenUsageRecordEntity>>
}
