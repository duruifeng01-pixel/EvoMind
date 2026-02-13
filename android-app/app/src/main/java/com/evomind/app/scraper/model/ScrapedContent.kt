package com.evomind.app.scraper.model

/**
 * 爬取的内容数据模型
 */
data class ScrapedContent(
    /**
     * 文章标题
     */
    val title: String,

    /**
     * 作者
     */
    val author: String,

    /**
     * 文章内容（纯文本，已清理HTML）
     */
    val content: String,

    /**
     * 文章描述/摘要
     */
    val description: String = "",

    /**
     * 发布时间（时间戳）
     */
    val publishTime: Long = System.currentTimeMillis(),

    /**
     * 图片URL列表
     */
    val images: List<String> = emptyList(),

    /**
     * 原标题（如果和当前标题不同）
     */
    val originalTitle: String? = null,

    /**
     * 元数据（平台特定信息）
     */
    val metadata: Map<String, String> = emptyMap(),

    /**
     * 爬取时间
     */
    val scrapedAt: Long = System.currentTimeMillis(),

    /**
     * 字数统计
     */
    val wordCount: Int = content.length
) {
    /**
     * 转换为摘要（前N个字符）
     */
    fun toSummary(maxLength: Int = 200): String {
        return if (content.length <= maxLength) {
            content
        } else {
            content.substring(0, maxLength) + "..."
        }
    }

    /**
     * 获取平台类型
     */
    fun getPlatform(): String {
        return metadata["platform"] ?: "unknown"
    }

    /**
     * 转换为SourceEntity（用于存储到数据库）
     */
    fun toSourceEntity(url: String): com.evomind.app.database.entity.SourceEntity {
        return com.evomind.app.database.entity.SourceEntity(
            title = title,
            originalText = content,
            cleanedText = content,
            platform = getPlatform(),
            imagePath = null,
            confidence = 1.0f,  // 爬取的内容置信度为1
            tags = generateTags()
        )
    }

    /**
     * 生成标签
     */
    private fun generateTags(): String? {
        val tags = mutableListOf<String>()
        tags.add("SCRAPED")
        tags.add("PLATFORM_${getPlatform().uppercase()}")

        if (wordCount > 5000) tags.add("LONG_READ")
        if (images.isNotEmpty()) tags.add("WITH_IMAGES")

        return tags.joinToString(",")
    }
}

/**
 * 爬取结果包装类
 */
sealed class ScrapingResult<out T> {
    data class Success<T>(val data: T) : ScrapingResult<T>()
    data class Failure(val error: String, val status: ScrapingStatus = ScrapingStatus.FAILED) :
        ScrapingResult<Nothing>()

    enum class ScrapingStatus {
        SUCCESS,
        FAILED,
        TIMEOUT,
        NETWORK_ERROR,
        PARSE_ERROR,
        INVALID_URL,
        PLATFORM_NOT_SUPPORTED
    }
}

/**
 * 批量爬取结果
 */
data class BatchScrapingResult(
    val results: List<ScrapingResult<ScrapedContent>>,
    val successCount: Int,
    val failureCount: Int,
    val failures: List<Pair<String, String>> // URL to error message
) {
    /**
     * 获取成功的结果
     */
    fun getSuccessfulResults(): List<ScrapedContent> {
        return results.filterIsInstance<ScrapingResult.Success<ScrapedContent>>()
            .map { it.data }
    }

    /**
     * 是否所有请求都成功
     */
    fun allSucceeded(): Boolean = failureCount == 0

    /**
     * 成功率
     */
    fun successRate(): Double {
        return if (results.isEmpty()) 0.0
        else successCount.toDouble() / results.size.toDouble()
    }
}
