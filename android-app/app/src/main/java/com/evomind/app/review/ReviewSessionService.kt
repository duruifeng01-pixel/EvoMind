package com.evomind.app.review

import android.content.Context
import android.util.Log
import com.evomind.app.database.AppDatabase
import com.evomind.app.database.entity.CardEntity
import com.evomind.app.database.entity.ReviewSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 复习会话管理服务
 * 处理复习逻辑、记录复习历史、更新卡片复习计划
 */
class ReviewSessionService(
    private val context: Context
) {
    private companion object {
        const val TAG = "ReviewSessionService"
    }

    private val database = AppDatabase.getInstance(context)
    private val algorithm = SpacedRepetitionAlgorithm()

    /**
     * 开始复习会话
     *
     * @param cardId 卡片ID
     * @param sessionType 复习类型
     * @return 复习会话ID
     */
    suspend fun startReviewSession(
        cardId: Long,
        sessionType: ReviewSessionEntity.ReviewSessionType
    ): Long = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始复习会话: cardId=$cardId, type=$sessionType")

            val card = database.cardDao().getById(cardId).first()
                ?: throw IllegalArgumentException("找不到卡片: $cardId")

            val reviewCount = database.reviewSessionDao().getReviewCountByCard(cardId)
            val averageEaseFactor = database.reviewSessionDao().getAverageEaseFactorByCard(cardId)
                ?: 2.5f

            val session = ReviewSessionEntity(
                cardId = cardId,
                sessionType = sessionType,
                easeFactor = averageEaseFactor,
                quality = 0, // 初始为0，将在结束时更新
                intervalDays = 0
            )

            val sessionId = database.reviewSessionDao().insert(session)
            Log.d(TAG, "创建复习会话成功: sessionId=$sessionId")

            return@withContext sessionId
        } catch (e: Exception) {
            Log.e(TAG, "创建复习会话失败: ${e.message}", e)
            throw e
        }
    }

    /**
     * 完成复习会话
     *
     * @param sessionId 会话ID
     * @param quality 复习质量（0-5）
     * @param notes 用户笔记（可选）
     * @return 更新后的卡片
     */
    suspend fun completeReviewSession(
        sessionId: Long,
        quality: Int,
        notes: String? = null
    ): CardEntity = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "完成复习会话: sessionId=$sessionId, quality=$quality")

            // Step 1: 获取复习会话和卡片
            val session = database.reviewSessionDao().getById(sessionId).first()
                ?: throw IllegalArgumentException("找不到复习会话: $sessionId")

            val card = database.cardDao().getById(session.cardId).first()
                ?: throw IllegalArgumentException("找不到卡片: ${session.cardId}")

            // Step 2: 获取历史复习次数（不包括当前会话）
            val reviewCount = database.reviewSessionDao().getReviewCountByCard(card.id)

            // Step 3: 使用算法计算下次复习时间
            val reviewResult = algorithm.calculateNextReview(
                card = card,
                quality = quality,
                reviewCount = reviewCount,
                currentEaseFactor = session.easeFactor
            )

            // Step 4: 更新复习会话记录
            val updatedSession = session.copy(
                quality = quality,
                intervalDays = reviewResult.nextIntervalDays,
                notes = notes,
                reviewDuration = calculateSessionDuration(session.reviewedAt)
            )
            database.reviewSessionDao().update(updatedSession)

            // Step 5: 更新卡片复习计划
            val now = System.currentTimeMillis()
            val nextReviewAt = now + TimeUnit.DAYS.toMillis(reviewResult.nextIntervalDays.toLong())

            val updatedCard = card.copy(
                reviewCount = card.reviewCount + 1,
                lastReviewedAt = now,
                nextReviewAt = nextReviewAt,
                updatedAt = now
            )

            database.cardDao().update(updatedCard)

            Log.d(TAG, "复习会话完成: card=${card.id}, " +
                    "quality=$quality, " +
                    "nextReviewIn=${reviewResult.nextIntervalDays}天, " +
                    "newEaseFactor=${reviewResult.newEaseFactor}")

            return@withContext updatedCard
        } catch (e: Exception) {
            Log.e(TAG, "完成复习会话失败: ${e.message}", e)
            throw e
        }
    }

    /**
     * 获取待复习的卡片
     */
    fun getDueCards() = database.cardDao().getCardsDueForReview()

    /**
     * 获取复习统计信息
     */
    suspend fun getReviewStats(): ReviewStats = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()

            // 今日统计
            val todayStart = getDayStartTime(now)
            val todayReviews = database.reviewSessionDao().getReviewCountSince(todayStart)
            val todayDistinctCards = database.reviewSessionDao().getDistinctCardsReviewedSince(todayStart)

            // 本周统计
            val weekStart = todayStart - TimeUnit.DAYS.toMillis(7)
            val weekReviews = database.reviewSessionDao().getReviewCountSince(weekStart)

            // 待复习卡片数
            val dueCardsCount = database.cardDao().getCardsDueForReview().first().size

            // 平均质量分
            val averageQuality = database.reviewSessionDao().getSessionsSince(weekStart).first()
                .map { it.quality }
                .average()
                .takeIf { it.isFinite() }

            ReviewStats(
                todayReviews = todayReviews,
                todayDistinctCards = todayDistinctCards,
                weekReviews = weekReviews,
                dueCardsCount = dueCardsCount,
                averageQuality = averageQuality?.toFloat()
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取复习统计失败: ${e.message}", e)
            ReviewStats(0, 0, 0, 0, null)
        }
    }

    /**
     * 获取单个卡片的复习历史
     */
    fun getCardReviewHistory(cardId: Long) =
        database.reviewSessionDao().getByCardId(cardId)

    /**
     * 计算复习统计信息
     */
    data class ReviewStats(
        val todayReviews: Int,              // 今日复习次数
        val todayDistinctCards: Int,        // 今日复习的不同卡片数
        val weekReviews: Int,               // 本周复习次数
        val dueCardsCount: Int,             // 待复习卡片数
        val averageQuality: Float?          // 平均质量分（本周）
    )

    /**
     * 计算会话时长
     */
    private fun calculateSessionDuration(startTime: Long): Long {
        val now = System.currentTimeMillis()
        return now - startTime
    }

    /**
     * 获取一天开始的时间戳
     */
    private fun getDayStartTime(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
