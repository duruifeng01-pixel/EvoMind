package com.evomind.app.aigc.model

/**
 * AI讨论消息数据模型
 */
data class AiMessage(
    /**
     * 消息ID
     */
    val id: Long,

    /**
     * 关联的素材ID
     */
    val sourceId: Long,

    /**
     * 角色: user/user_proxy 或 assistant
     */
    val role: String,

    /**
     * 消息内容
     */
    val content: String,

    /**
     * 发送时间戳
     */
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * AI讨论会话数据模型
 */
data class DiscussionSession(
    /**
     * 会话ID
     */
    val id: Long = 0,

    /**
     * 关联的素材ID
     */
    val sourceId: Long,

    /**
     * 会话标题（从素材生成）
     */
    val title: String,

    /**
     * 消息列表
     */
    val messages: List<AiMessage>,

    /**
     * 创建时间
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * 最后更新时间
     */
    val lastUpdatedAt: Long = createdAt,

    /**
     * 讨论总结（AI生成）
     */
    val summary: String? = null
)

/**
 * 讨论上下文（传递给Deepseek API）
 */
data class DiscussionContext(
    val sourceTitle: String,
    val sourceContent: String,
    val previousMessages: List<AiMessage>
)
