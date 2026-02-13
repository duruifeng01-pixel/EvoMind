package com.evomind.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 支付记录实体
 * 存储用户的所有支付记录
 */
@Entity(
    tableName = "payment_records",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id", "created_at"]),
        Index(value = ["transaction_id"], unique = true),
        Index(value = ["created_at"])
    ]
)
data class PaymentRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "transaction_id")
    val transactionId: String, // 支付平台交易号

    @ColumnInfo(name = "payment_type")
    val paymentType: PaymentType,

    @ColumnInfo(name = "amount")
    val amount: Double, // 支付金额（元）

    @ColumnInfo(name = "currency")
    val currency: String = "CNY",

    @ColumnInfo(name = "status")
    val status: PaymentStatus,

    @ColumnInfo(name = "description")
    val description: String, // 支付描述

    @ColumnInfo(name = "metadata", typeAffinity = ColumnInfo.TEXT)
    val metadata: String? = null, // 扩展数据（JSON）

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null
)

/**
 * 支付类型
 */
enum class PaymentType {
    WECHAT_PAY,    // 微信支付
    ALIPAY,        // 支付宝
    SUBSCRIPTION   // 订阅支付
}

/**
 * 支付状态
 */
enum class PaymentStatus {
    PENDING,       // 待支付
    SUCCESS,       // 支付成功
    FAILED,        // 支付失败
    REFUNDED,      // 已退款
    CANCELLED      // 已取消
}

/**
 * 支付记录扩展数据
 */
data class PaymentMetadata(
    val subscriptionId: Long? = null, // 关联的订阅ID
    val subscriptionPlanId: String? = null, // 订阅计划ID
    val aiServiceUsage: Int? = null, // AI服务使用量（tokens）
    val ipAddress: String? = null, // 支付IP地址
    val deviceInfo: String? = null // 设备信息
)
