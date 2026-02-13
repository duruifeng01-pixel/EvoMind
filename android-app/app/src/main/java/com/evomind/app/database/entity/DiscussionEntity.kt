package com.evomind.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * AI讨论会话实体
 * 存储用户与AI的对话会话
 */
@Entity(
    tableName = "ai_discussions",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["card_id", "created_at"]),
        Index(value = ["created_at"])
    ]
)
data class DiscussionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "card_id")
    val cardId: Long, // 关联的卡片ID

    @ColumnInfo(name = "session_title")
    val sessionTitle: String, // 会话标题

    @ColumnInfo(name = "context_summary")
    val contextSummary: String? = null, // 上下文摘要

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true, // 是否为活跃的会话

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_message_at")
    val lastMessageAt: Long = System.currentTimeMillis()
)
