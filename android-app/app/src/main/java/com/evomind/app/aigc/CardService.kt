package com.evomind.app.aigc

import android.content.Context
import android.util.Log
import com.evomind.app.aigc.model.AiCard
import com.evomind.app.database.dao.SourceDao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

/**
 * 认知卡片服务
 * 管理从OCR素材到AI生成卡片的完整流程
 */
class CardService(
    private val context: Context,
    private val sourceDao: SourceDao
) {
    private companion object {
        const val TAG = "CardService"
    }

    private val deepseekService = DeepseekApiService(context)

    /**
     * 从素材生成认知卡片
     */
    fun generateCognitiveCard(sourceId: Long): Flow<Result<AiCard>> = flow {
        try {
            val source = sourceDao.getById(sourceId).first()
                ?: throw Exception("找不到素材ID: $sourceId")

            val updated = source.addTag("AI_CARD_GENERATED")
            sourceDao.update(updated)

            val card = deepseekService.generateCard(
                sourceTitle = source.title,
                sourceContent = source.cleanedText,
                sourceId = sourceId
            ).getOrThrow()

            emit(Result.success(card))

        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * 批量生成卡片
     */
    suspend fun batchGenerateCards(sourceIds: List<Long>): Result<BatchResult> {
        val successCards = mutableListOf<AiCard>()
        val failures = mutableListOf<Pair<Long, String>>()

        for (id in sourceIds) {
            try {
                val source = sourceDao.getById(id).first() ?: continue
                val card = deepseekService.generateCard(
                    sourceTitle = source.title,
                    sourceContent = source.cleanedText,
                    sourceId = id
                ).getOrThrow()
                successCards.add(card)
            } catch (e: Exception) {
                failures.add(id to e.message.toString())
            }
        }

        return Result.success(
            BatchResult(
                successCount = successCards.size,
                failureCount = failures.size,
                generatedCards = successCards,
                failures = failures
            )
        )
    }

    data class BatchResult(
        val successCount: Int,
        val failureCount: Int,
        val generatedCards: List<AiCard>,
        val failures: List<Pair<Long, String>>
    )
}
