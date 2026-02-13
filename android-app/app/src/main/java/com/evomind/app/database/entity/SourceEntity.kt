package com.evomind.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 素材实体类
 * 存储从OCR识别得到的原始素材
 */
@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "original_text", typeAffinity = ColumnInfo.TEXT)
    val originalText: String,

    @ColumnInfo(name = "cleaned_text", typeAffinity = ColumnInfo.TEXT)
    val cleanedText: String,

    @ColumnInfo(name = "platform")
    val platform: String,  // PlatformType的字符串表示

    @ColumnInfo(name = "image_path")
    val imagePath: String? = null,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "tags")
    val tags: String? = null,  // 标签，用逗号分隔

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = createdAt
) {
    /**
     * 获取标签列表
     */
    fun getTagList(): List<String> {
        return tags?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    /**
     * 设置标签
     */
    fun withTags(tagList: List<String>): SourceEntity {
        return copy(tags = tagList.joinToString(","))
    }

    /**
     * 添加标签
     */
    fun addTag(tag: String): SourceEntity {
        val currentTags = getTagList().toMutableList()
        if (!currentTags.contains(tag)) {
            currentTags.add(tag)
        }
        return withTags(currentTags)
    }

    /**
     * 移除标签
     */
    fun removeTag(tag: String): SourceEntity {
        val currentTags = getTagList().toMutableList()
        currentTags.remove(tag)
        return withTags(currentTags)
    }

    companion object {
        /**
         * 创建空素材（用于测试）
         */
        fun empty(): SourceEntity {
            return SourceEntity(
                title = "空素材",
                originalText = "",
                cleanedText = "",
                platform = "GENERAL",
                confidence = 0f
            )
        }

        /**
         * 从OCR结果创建素材
         */
        fun fromOcrResult(
            ocrResult: com.evomind.app.ocr.model.OcrResult,
            title: String? = null
        ): SourceEntity {
            return SourceEntity(
                title = title ?: generateTitle(ocrResult.cleanedText),
                originalText = ocrResult.originalText,
                cleanedText = ocrResult.cleanedText,
                platform = ocrResult.platform.name,
                imagePath = ocrResult.imagePath,
                confidence = ocrResult.confidence
            )
        }

        /**
         * 从文本生成标题
         */
        private fun generateTitle(text: String): String {
            if (text.isBlank()) return "未命名素材"

            // 取前20个字符作为标题
            val firstLine = text.lineSequence()
                .firstOrNull { it.isNotBlank() }
                ?: return "未命名素材"

            return when {
                firstLine.length <= 20 -> firstLine
                else -> firstLine.substring(0, 20) + "..."
            }
        }
    }
}
