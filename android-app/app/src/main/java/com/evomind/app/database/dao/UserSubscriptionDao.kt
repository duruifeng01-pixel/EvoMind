package com.evomind.app.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.evomind.app.database.entity.SubscriptionStatus
import com.evomind.app.database.entity.UserSubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSubscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: UserSubscriptionEntity): Long

    @Update
    suspend fun update(subscription: UserSubscriptionEntity)

    @Delete
    suspend fun delete(subscription: UserSubscriptionEntity)

    @Query("SELECT * FROM user_subscriptions WHERE id = :id")
    fun getById(id: Long): Flow<UserSubscriptionEntity?>

    @Query("SELECT * FROM user_subscriptions WHERE user_id = :userId ORDER BY created_at DESC")
    fun getByUserId(userId: Long): Flow<List<UserSubscriptionEntity>>

    @Query("SELECT * FROM user_subscriptions WHERE user_id = :userId AND status = 'ACTIVE' ORDER BY end_date DESC LIMIT 1")
    fun getCurrentSubscription(userId: Long): Flow<UserSubscriptionEntity?>

    @Query("SELECT * FROM user_subscriptions WHERE user_id = :userId AND status = :status ORDER BY created_at DESC")
    fun getByUserIdAndStatus(userId: Long, status: SubscriptionStatus): Flow<List<UserSubscriptionEntity>>

    @Query("SELECT * FROM user_subscriptions WHERE plan_id = :planId ORDER BY created_at DESC")
    fun getByPlanId(planId: Long): Flow<List<UserSubscriptionEntity>>

    @Query("SELECT * FROM user_subscriptions WHERE end_date < :currentTime AND status = 'ACTIVE'")
    suspend fun getExpiredSubscriptions(currentTime: Long): List<UserSubscriptionEntity>

    @Query("UPDATE user_subscriptions SET status = 'EXPIRED' WHERE id = :id")
    suspend fun markAsExpired(id: Long)

    @Query("UPDATE user_subscriptions SET auto_renew = :autoRenew WHERE id = :id")
    suspend fun updateAutoRenew(id: Long, autoRenew: Boolean)

    @Query("SELECT SUM(token_balance) FROM user_subscriptions WHERE user_id = :userId AND status = 'ACTIVE'")
    suspend fun getTotalTokenBalance(userId: Long): Long?

    @Query("SELECT SUM(token_used) FROM user_subscriptions WHERE user_id = :userId")
    suspend fun getTotalTokenUsed(userId: Long): Long?

    @Query("DELETE FROM user_subscriptions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM user_subscriptions WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: Long)

    @Query("DELETE FROM user_subscriptions")
    suspend fun deleteAll()

    data class SubscriptionStats(
        val totalSubscriptions: Int,           // 总订阅数
        val activeSubscriptions: Int,          // 活跃订阅数
        val totalTokens: Long,                 // 总Token额度
        val usedTokens: Long                  // 已使用的Tokens
    )

    @Query("""
        SELECT
            COUNT(*) as totalSubscriptions,
            SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as activeSubscriptions,
            SUM(token_quota) as totalTokens,
            SUM(token_used) as usedTokens
        FROM user_subscriptions
        WHERE user_id = :userId
    """)
    suspend fun getSubscriptionStats(userId: Long): SubscriptionStats?
}