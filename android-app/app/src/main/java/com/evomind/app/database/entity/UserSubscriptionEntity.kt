package com.evomind.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户订阅记录实体
 * 存储用户的订阅状态和Token使用情况
 */
@Entity(
    tableName = "user_subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SubscriptionPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["plan_id"]),
        Index(value = ["status"])
    ]
)
data class UserSubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "plan_id")
    val planId: Long?, // 关联的订阅计划ID

    @ColumnInfo(name = "status")
    val status: SubscriptionStatus,

    @ColumnInfo(name = "token_balance")
    val tokenBalance: Long = 0, // 剩余Token额度

    @ColumnInfo(name = "token_used")
    val tokenUsed: Long = 0, // 已使用Token数量

    @ColumnInfo(name = "start_date")
    val startDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "end_date")
    val endDate: Long?, // 订阅结束时间

    @ColumnInfo(name = "auto_renew")
    val autoRenew: Boolean = false, // 是否自动续费

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "metadata", typeAffinity = ColumnInfo.TEXT)
    val metadata: String? = null // 扩展数据
)

/**
 * 订阅状态
 */
enum class SubscriptionStatus {
    ACTIVE,        // 活跃（正常订阅中）
    EXPIRED,       // 已过期
    CANCELLED,     // 已取消
    SUSPENDED,     // 已暂停
    PENDING        // 待支付
}

/**
 * 用户实体（简化版本，实际可能包含更多字段）
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "uuid")
    val uuid: String, // 用户唯一标识

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Token使用记录
 * 记录每次AI服务的Token消耗
 */
@Entity(
    tableName = "token_usages",
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
        Index(value = ["service_type"])
    ]
)
data class TokenUsageRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "tokens_used")
    val tokensUsed: Int, // 本次使用的Token数量

    @ColumnInfo(name = "service_type")
    val serviceType: AIServiceType,

    @ColumnInfo(name = "description")
    val description: String, // 服务描述

    @ColumnInfo(name = "metadata", typeAffinity = ColumnInfo.TEXT)
    val metadata: String? = null, // 扩展数据

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * AI服务类型
 */
enum class AIServiceType {
    CARD_GENERATION,    // 卡片生成
    DISCUSSION,         // AI讨论
    SUMMARY,            // 内容总结
    MINDMAP,            // 思维导图
    CONFLICT_DETECTION  // 冲突检测
}
