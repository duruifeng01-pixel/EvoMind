package com.evomind.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = []
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "source_id")
    val sourceId: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "one_line_guide")
    val oneLineGuide: String,

    @ColumnInfo(name = "summary", typeAffinity = ColumnInfo.TEXT)
    val summary: String,

    @ColumnInfo(name = "key_points")
    val keyPointsJson: String, // JSON array字符串

    @ColumnInfo(name = "mind_map", typeAffinity = ColumnInfo.TEXT)
    val mindMap: String,

    @ColumnInfo(name = "review_advice")
    val reviewAdvice: String? = null,

    @ColumnInfo(name = "ai_tags")
    val aiTagsJson: String, // JSON array字符串

    @ColumnInfo(name = "difficulty_level")
    val difficultyLevel: Int,

    @ColumnInfo(name = "estimated_time")
    val estimatedTime: Int,

    @ColumnInfo(name = "review_count")
    val reviewCount: Int = 0,

    @ColumnInfo(name = "last_reviewed_at")
    val lastReviewedAt: Long? = null,

    @ColumnInfo(name = "next_review_at")
    val nextReviewAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = createdAt
) {
    companion object {
        fun fromAiCard(aiCard: com.evomind.app.aigc.model.AiCard, sourceId: Long): CardEntity {
            return CardEntity(
                id = aiCard.id,
                sourceId = sourceId,
                title = aiCard.title,
                oneLineGuide = aiCard.oneLineGuide,
                summary = aiCard.summary,
                keyPointsJson = com.google.gson.Gson().toJson(aiCard.keyPoints),
                mindMap = aiCard.mindMap,
                reviewAdvice = aiCard.reviewAdvice,
                aiTagsJson = com.google.gson.Gson().toJson(aiCard.aiTags),
                difficultyLevel = aiCard.difficultyLevel,
                estimatedTime = aiCard.estimatedTime
            )
        }
    }
}
