package com.evomind.app.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.evomind.app.database.entity.SubscriptionPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: SubscriptionPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plans: List<SubscriptionPlanEntity>): List<Long>

    @Update
    suspend fun update(plan: SubscriptionPlanEntity)

    @Delete
    suspend fun delete(plan: SubscriptionPlanEntity)

    @Query("SELECT * FROM subscription_plans WHERE id = :id")
    fun getById(id: Long): Flow<SubscriptionPlanEntity?>

    @Query("SELECT * FROM subscription_plans WHERE plan_id = :planId")
    suspend fun getByPlanId(planId: String): SubscriptionPlanEntity?

    @Query("SELECT * FROM subscription_plans WHERE id = :id")
    fun getPlanById(id: Long): Flow<SubscriptionPlanEntity?>

    @Query("SELECT * FROM subscription_plans WHERE is_active = 1 ORDER BY sort_order")
    fun getAllActive(): Flow<List<SubscriptionPlanEntity>>

    @Query("SELECT * FROM subscription_plans WHERE is_active = 1 ORDER BY sort_order")
    fun getAllActiveLive(): LiveData<List<SubscriptionPlanEntity>>

    @Query("SELECT * FROM subscription_plans ORDER BY sort_order")
    fun getAll(): Flow<List<SubscriptionPlanEntity>>

    @Query("UPDATE subscription_plans SET is_active = :isActive WHERE id = :id")
    suspend fun updateActiveStatus(id: Long, isActive: Boolean)

    @Query("DELETE FROM subscription_plans WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM subscription_plans")
    suspend fun deleteAll()

    @Query("SELECT * FROM subscription_plans WHERE name LIKE '%' || :query || '%' ORDER BY sort_order")
    fun search(query: String): Flow<List<SubscriptionPlanEntity>>
}
