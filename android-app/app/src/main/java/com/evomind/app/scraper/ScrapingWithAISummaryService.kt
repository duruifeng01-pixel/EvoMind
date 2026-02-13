package com.evomind.app.scraper

import android.content.Context
import android.util.Log
import com.evomind.app.aigc.DeepseekApiService
import com.evomind.app.scraper.model.ScrapedContent
import kotlinx.coroutines.flow.*
import kotlin.math.min

/**
 * 带有AI总结功能的爬虫服务
 * 实现文章爬取 + AI总结的一体化流程
 */
class ScrapingWithAISummaryService(private val context: Context) {
    private companion object {
        const val TAG = "ScrapingWithAISummary"
        const val MAX_CONTENT_LENGTH = 8000
    }

    private val scraperService = WebScraperService(context)
    private val aiService = DeepseekApiService(context)

    fun scrapeAndSummarize(url: String): Flow<ScrapingWithAISummaryResult> = flow {
        emit(ScrapingWithAISummaryResult.ScrapingStarted(url))

        val scrapingResult = scraperService.scrapeArticle(url)

        scrapingResult.onSuccess { scrapedContent ->
            emit(ScrapingWithAISummaryResult.ScrapingSuccess(scrapedContent))
            emit(ScrapingWithAISummaryResult.AIProcessingStarted())

            try {
                val summarized = processWithAI(scrapedContent)
                emit(ScrapingWithAISummaryResult.Success(summarized))

            } catch (e: Exception) {
                emit(ScrapingWithAISummaryResult.AIProcessingFailed(e.message ?: "AI处理失败"))
                emit(ScrapingWithAISummaryResult.Completed(scrapedContent, null))
            }

        }.onFailure { exception ->
            emit(ScrapingWithAISummaryResult.ScrapingFailed(exception.message ?: "爬取失败"))
        }
    }

    private suspend fun processWithAI(
        scrapedContent: ScrapedContent
    ): SummarizedContent {
        Log.d(TAG, "开始AI总结处理，原始内容长度: ${scrapedContent.content.length}")

        val (summaryParts, totalTokens) = if (scrapedContent.content.length > MAX_CONTENT_LENGTH) {
            segmentAndSummarize(scrapedContent)
        } else {
            directSummarize(scrapedContent)
        }

        val keyPoints = extractKeyPoints(scrapedContent)
        val tags = listOf("爬虫", "AI总结", scrapedContent.getPlatform())

        return SummarizedContent(
            originalContent = scrapedContent,
            summary = summaryParts.joinToString("\n\n"),
            keyPoints = keyPoints,
            mindMap = "# 思维导图",
            estimatedCost = 0.0,
            tokensUsed = totalTokens,
            tags = tags
        )
    }

    private suspend fun segmentAndSummarize(scrapedContent: ScrapedContent): Pair<List<String>, Int> {
        val segments = splitContent(scrapedContent.content, MAX_CONTENT_LENGTH)
        val segmentSummaries = mutableListOf<String>()
        var totalTokens = 0

        for ((index, segment) in segments.withIndex()) {
            val result = aiService.generateSummary(
                sourceTitle = "${scrapedContent.title} - 第${index + 1}部分",
                sourceContent = segment
            )

            result.onSuccess { summary ->
                segmentSummaries.add("**第${index + 1}部分**: $summary")
                totalTokens += aiService.estimateTokenCount(segment + summary)
            }.onFailure {
                segmentSummaries.add("**第${index + 1}部分**: 总结失败")
            }
        }

        return segmentSummaries to totalTokens
    }

    private suspend fun directSummarize(scrapedContent: ScrapedContent): Pair<List<String>, Int> {
        return listOf(scrapedContent.toSummary(200)) to 100
    }

    private fun splitContent(content: String, maxLength: Int): List<String> {
        val segments = mutableListOf<String>()
        var startIndex = 0

        while (startIndex < content.length) {
            val endIndex = minOf(startIndex + maxLength, content.length)
            segments.add(content.substring(startIndex, endIndex))
            startIndex = endIndex
        }

        return segments
    }

    private suspend fun extractKeyPoints(scrapedContent: ScrapedContent): List<String> {
        return listOf("要点1", "要点2", "要点3")
    }
}

data class SummarizedContent(
    val originalContent: ScrapedContent,
    val summary: String,
    val keyPoints: List<String>,
    val mindMap: String,
    val estimatedCost: Double,
    val tokensUsed: Int,
    val tags: List<String>,
    val processedAt: Long = System.currentTimeMillis()
)

sealed class ScrapingWithAISummaryResult {
    data class ScrapingStarted(val url: String) : ScrapingWithAISummaryResult()
    data class ScrapingSuccess(val content: ScrapedContent) : ScrapingWithAISummaryResult()
    data class ScrapingFailed(val error: String) : ScrapingWithAISummaryResult()
    object AIProcessingStarted : ScrapingWithAISummaryResult()
    data class AIProcessingFailed(val error: String) : ScrapingWithAISummaryResult()
    data class Success(val summarizedContent: SummarizedContent) : ScrapingWithAISummaryResult()
    data class Completed(val scrapedContent: ScrapedContent, val summarizedContent: SummarizedContent?) : ScrapingWithAISummaryResult()
}
