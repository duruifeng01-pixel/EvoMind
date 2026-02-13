package com.evomind.app.aigc.model

/**
 * AI生成的认知卡片数据模型
 */
data class AiCard(
    /**
     * 卡片ID
     */
    val id: Long = 0,

    /**
     * 关联的素材ID
     */
    val sourceId: Long,

    /**
     * 卡片标题（简化的主题）
     */
    val title: String,

    /**
     * 一句话导读（快速了解卡片主旨）
     * 例如："核心概念：认知卡片通过主动回忆提升记忆效率"
     */
    val oneLineGuide: String,

    /**
     * 原文本的摘要
     */
    val summary: String,

    /**
     * 关键要点列表
     */
    val keyPoints: List<String>,

    /**
     * 思维导图数据（JSON格式或Markdown）
     */
    val mindMap: String,

    /**
     * 复习建议（基于遗忘曲线算法）
     */
    val reviewAdvice: String? = null,

    /**
     * AI生成的标签列表
     */
    val aiTags: List<String>,

    /**
     * 难度等级（1-5）
     */
    val difficultyLevel: Int,

    /**
     * 估计学习时间（分钟）
     */
    val estimatedTime: Int,

    /**
     * 生成时间戳
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * 更新时间戳
     */
    val updatedAt: Long = createdAt
) {

    /**
     * 获取简化版卡片信息
     */
    fun toSimpleCard(): SimpleCard = SimpleCard(
        id = id,
        sourceId = sourceId,
        title = title,
        summary = summary,
        keyPointsCount = keyPoints.size,
        createdAt = createdAt
    )

    companion object {
        /**
         * 创建空卡片（用于测试）
         */
        fun empty(sourceId: Long = 0): AiCard {
            return AiCard(
                sourceId = sourceId,
                title = "空卡片",
                summary = "",
                keyPoints = emptyList(),
                mindMap = "",
                aiTags = emptyList(),
                difficultyLevel = 1,
                estimatedTime = 1
            )
        }
    }
}

/**
 * 简化版卡片信息（用于列表展示）
 */
data class SimpleCard(
    val id: Long,
    val sourceId: Long,
    val title: String,
    val summary: String,
    val keyPointsCount: Int,
    val createdAt: Long
)
