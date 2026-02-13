package com.evomind.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 订阅计划实体
 * 定义不同的订阅套餐
 */
@Entity(
    tableName = "subscription_plans",
    indices = [
        Index(value = ["plan_id"], unique = true),
        Index(value = ["is_active"])
    ]
)
data class SubscriptionPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "plan_id")
    val planId: String, // 计划唯一标识

    @ColumnInfo(name = "name")
    val name: String, // 计划名称

    @ColumnInfo(name = "description")
    val description: String, // 计划描述

    @ColumnInfo(name = "price")
    val price: Double, // 价格（元/月）

    @ColumnInfo(name = "duration_days")
    val durationDays: Int, // 订阅时长（天）

    @ColumnInfo(name = "token_quota")
    val tokenQuota: Long, // Token额度

    @ColumnInfo(name = "features", typeAffinity = ColumnInfo.TEXT)
    val features: String, // 功能列表（JSON数组）

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0, // 排序顺序

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 预定义的订阅计划
 */
enum class SubscriptionPlanId {
    FREE,          // 免费版
    BASIC,         // 基础版
    PRO,          // 专业版
    ENTERPRISE    // 企业版
}

// 示例：
// FREE: 0元/月，500 tokens
// BASIC: 29元/月，5000 tokens + 基础功能
// PRO: 99元/月，20000 tokens + 高级功能
// ENTERPRISE: 299元/月，无限tokens + 全部功能
