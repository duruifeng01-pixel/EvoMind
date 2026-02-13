package com.evomind.app.database.entity

import androidx.room.*

/**
 * 节点评论实体
 * 存储用户对思维导图节点的评论和笔记
 */
@Entity(
    tableName = "node_comments",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["card_id", "node_id"]),
        Index(value = ["node_id"]),
        Index(value = ["created_at"])
    ]
)
data class NodeCommentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "card_id")
    val cardId: Long, // 关联的卡片ID

    @ColumnInfo(name = "node_id")
    val nodeId: String, // 节点ID

    @ColumnInfo(name = "node_text")
    val nodeText: String? = null, // 节点文本快照

    @ColumnInfo(name = "comment_text")
    val commentText: String, // 评论内容

    @ColumnInfo(name = "parent_comment_id")
    val parentCommentId: Long? = null, // 父评论ID（支持嵌套评论）

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
