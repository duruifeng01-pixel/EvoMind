package com.evomind.app.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.evomind.app.database.entity.PaymentRecordEntity
import com.evomind.app.database.entity.PaymentStatus
import com.evomind.app.database.entity.PaymentType
import kotlinx.coroutines.flow.Flow

/**
 * 支付记录数据访问对象
 */
@Dao
interface PaymentRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PaymentRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<PaymentRecordEntity>): List<Long>

    @Update
    suspend fun update(record: PaymentRecordEntity)

    @Delete
    suspend fun delete(record: PaymentRecordEntity)

    @Query("SELECT * FROM payment_records WHERE id = :id")
    fun getById(id: Long): Flow<PaymentRecordEntity?>

    @Query("SELECT * FROM payment_records WHERE id = :id")
    fun getByIdLive(id: Long): LiveData<PaymentRecordEntity?>

    @Query("SELECT * FROM payment_records WHERE transaction_id = :transactionId")
    suspend fun getByTransactionId(transactionId: String): PaymentRecordEntity?

    @Query("SELECT * FROM payment_records WHERE user_id = :userId ORDER BY created_at DESC")
    fun getByUserId(userId: Long): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records WHERE user_id = :userId ORDER BY created_at DESC")
    fun getByUserIdLive(userId: Long): LiveData<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records WHERE user_id = :userId AND payment_type = :paymentType ORDER BY created_at DESC")
    fun getByUserIdAndType(userId: Long, paymentType: PaymentType): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records WHERE user_id = :userId AND status = :status ORDER BY created_at DESC")
    fun getByUserIdAndStatus(userId: Long, status: PaymentStatus): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records ORDER BY created_at DESC")
    fun getAll(): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records WHERE status = :status ORDER BY created_at DESC")
    fun getByStatus(status: PaymentStatus): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records WHERE payment_type = :paymentType ORDER BY created_at DESC")
    fun getByPaymentType(paymentType: PaymentType): Flow<List<PaymentRecordEntity>>

    @Query("SELECT COUNT(*) FROM payment_records WHERE user_id = :userId")
    suspend fun getCountByUser(userId: Long): Int

    @Query("SELECT SUM(amount) FROM payment_records WHERE user_id = :userId AND status = :status")
    suspend fun getTotalAmountByUserAndStatus(userId: Long, status: PaymentStatus): Double?

    @Query("SELECT SUM(amount) FROM payment_records WHERE user_id = :userId AND status = 'SUCCESS'")
    suspend fun getTotalSpent(userId: Long): Double?

    @Query("SELECT AVG(amount) FROM payment_records WHERE user_id = :userId AND status = 'SUCCESS'")
    suspend fun getAveragePaymentAmount(userId: Long): Double?

    @Query("SELECT COUNT(DISTINCT DATE(created_at / 1000, 'unixepoch')) FROM payment_records WHERE user_id = :userId")
    suspend fun getDaysWithPayments(userId: Long): Int

    @Query("DELETE FROM payment_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM payment_records WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: Long)

    @Query("DELETE FROM payment_records")
    suspend fun deleteAll()

    /**
     * 获取支付统计
     */
    data class PaymentStats(
        val totalPayments: Int,
        val totalAmount: Double,
        val successPayments: Int,
        val successAmount: Double,
        val averageAmount: Double
    )

    @Query("""
        SELECT
            COUNT(*) as totalPayments,
            SUM(CASE WHEN status = 'SUCCESS' THEN amount ELSE 0 END) as totalAmount,
            SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as successPayments,
            SUM(CASE WHEN status = 'SUCCESS' THEN amount ELSE 0 END) as successAmount,
            AVG(CASE WHEN status = 'SUCCESS' THEN amount ELSE NULL END) as averageAmount
        FROM payment_records
        WHERE user_id = :userId
    """)
    suspend fun getPaymentStats(userId: Long): PaymentStats?

    /**
     * 每日支付统计
     */
    data class DailyPaymentStats(
        val date: String,
        val paymentCount: Int,
        val totalAmount: Double
    )

    @Query("""
        SELECT
            date(datetime(created_at / 1000, 'unixepoch')) as date,
            COUNT(*) as paymentCount,
            SUM(CASE WHEN status = 'SUCCESS' THEN amount ELSE 0 END) as totalAmount
        FROM payment_records
        WHERE user_id = :userId
        GROUP BY date
        ORDER BY date DESC
    """)
    suspend fun getDailyPaymentStats(userId: Long): List<DailyPaymentStats>

    @Query("SELECT * FROM payment_records WHERE description LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun search(query: String): Flow<List<PaymentRecordEntity>>
}
