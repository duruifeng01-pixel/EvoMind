package com.evomind.app.database.entity

import androidx.room.*

/**
 * 思维导图节点收藏实体
 * 存储用户收藏的思维导图节点
 */
@Entity(
    tableName = "mindmap_favorites",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["card_id", "node_id"], unique = true),
        Index(value = ["card_id"])
    ]
)
data class MindMapFavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "card_id")
    val cardId: Long, // 关联的卡片ID

    @ColumnInfo(name = "node_id")
    val nodeId: String, // 节点ID

    @ColumnInfo(name = "node_text")
    val nodeText: String, // 节点文本

    @ColumnInfo(name = "parent_path")
    val parentPath: String, // 父节点路径（JSON格式）

    @ColumnInfo(name = "level")
    val level: Int, // 节点层级

    @ColumnInfo(name = "favorited_at")
    val favoritedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "notes")
    val notes: String? = null // 用户备注
)

/**
 * 节点收藏扩展数据
 * 存储于MindMapFavoriteEntity.notes字段
 */
data class NodeFavoriteNotes(
    val sourceParagraphRef: String? = null, // 原文引用
    val tags: List<String> = emptyList(), // 用户标签
    val color: String? = null // 高亮颜色
)
