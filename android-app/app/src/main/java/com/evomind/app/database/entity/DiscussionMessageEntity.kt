package com.evomind.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 讨论消息实体
 * 存储会话中的每条消息（用户或AI发送的）
 */
@Entity(
    tableName = "discussion_messages",
    foreignKeys = [
        ForeignKey(
            entity = DiscussionEntity::class,
            parentColumns = ["id"],
            childColumns = ["discussion_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["discussion_id", "created_at"]),
        Index(value = ["discussion_id", "message_index"])
    ]
)
data class DiscussionMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "discussion_id")
    val discussionId: Long, // 关联的讨论会话ID

    @ColumnInfo(name = "message_index")
    val messageIndex: Int, // 消息在会话中的序号

    @ColumnInfo(name = "sender_type")
    val senderType: MessageSenderType, // 发送者类型（用户或AI）

    @ColumnInfo(name = "message_content")
    val messageContent: String, // 消息内容

    @ColumnInfo(name = "related_nodes", typeAffinity = ColumnInfo.TEXT)
    val relatedNodes: String? = null, // 相关的思维导图节点ID（JSON数组）

    @ColumnInfo(name = "token_usage")
    val tokenUsage: Int? = null, // Token使用量

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 消息发送者类型
 */
enum class MessageSenderType {
    USER,    // 用户发送的消息
    AI,      // AI回复的消息
    SYSTEM   // 系统消息（如会话开始）
}

/**
 * 讨论消息扩展数据
 * 存储于DiscussionMessageEntity.related_nodes字段
 */
data class MessageRelatedNodes(
    val nodeIds: List<String>, // 相关的节点ID列表
    val referencedText: String? = null // 引用的原文内容
)
