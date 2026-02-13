package com.evomind.app.review

import android.util.Log
import com.evomind.app.database.entity.CardEntity
import com.evomind.app.database.entity.ReviewSessionEntity
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/**
 * 间隔重复算法服务
 * 基于改进的SM2算法，专为认知卡片优化
 *
 * SM2算法原理：
 * 1. 初始间隔为1天
 * 2. 每次复习后，根据质量评分(0-5)调整间隔
 * 3. 好的复习结果会让间隔指数增长（1天→6天→16天...）
 * 4. 记忆容易度因子(ease factor)会根据表现动态调整
 */
class SpacedRepetitionAlgorithm {

    companion object {
        private const val TAG = "SpacedRepetitionAlgorithm"

        // 默认记忆容易度因子（1.3表示间隔大约增加30%）
        private const val DEFAULT_EASE_FACTOR = 2.5f

        // 记忆容易度因子的最小值（不能小于这个值）
        private const val MIN_EASE_FACTOR = 1.3f

        // 初次复习后的基础间隔（天）
        private const val INITIAL_INTERVAL = 1

        // 第二次复习后的间隔（天）
        private const val SECOND_INTERVAL = 6

        // 连续正确复习的阈值（达到这个次数后进入指数增长阶段）
        // 修改为2次，意味着第3次开始指数增长
        private const val CONSECUTIVE_CORRECT_THRESHOLD = 2
        // 最大间隔天数（约20年，避免间隔过大）
        private const val MAX_INTERVAL_DAYS = 7300
    }

    /**
     * 计算下次复习时间
     *
     * @param card 当前卡片
     * @param quality 复习质量（0-5）
     * @param reviewCount 复习次数（从Session获取更准确）
     * @param currentEaseFactor 当前记忆容易度因子
     * @return 下次复习的间隔天数和新的记忆容易度因子
     */
    data class ReviewResult(
        val nextIntervalDays: Int,
        val newEaseFactor: Float,
        val shouldReset: Boolean
    )

    fun calculateNextReview(
        card: CardEntity,
        quality: Int,
        reviewCount: Int,
        currentEaseFactor: Float = DEFAULT_EASE_FACTOR
    ): ReviewResult {
        Log.d(TAG, "计算下次复习: cardId=${card.id}, quality=$quality, reviewCount=$reviewCount, easeFactor=$currentEaseFactor")

        return when {
            // 质量低于3，需要重新学习
            quality < 3 -> {
                Log.d(TAG, "质量评分<3，重置为初始间隔")
                ReviewResult(
                    nextIntervalDays = INITIAL_INTERVAL,
                    newEaseFactor = max(currentEaseFactor - 0.2f, MIN_EASE_FACTOR),
                    shouldReset = true
                )
            }

            // 第一次复习（reviewCount == 0）
            reviewCount == 0 -> {
                Log.d(TAG, "第一次复习，使用初始间隔1天")
                ReviewResult(
                    nextIntervalDays = INITIAL_INTERVAL,
                    newEaseFactor = calculateNewEaseFactor(currentEaseFactor, quality),
                    shouldReset = false
                )
            }

            // 第二次复习（reviewCount == 1）
            reviewCount == 1 -> {
                Log.d(TAG, "第二次复习，使用间隔6天")
                ReviewResult(
                    nextIntervalDays = SECOND_INTERVAL,
                    newEaseFactor = calculateNewEaseFactor(currentEaseFactor, quality),
                    shouldReset = false
                )
            }

            // 多次复习，进入指数增长阶段
            else -> {
                val newEaseFactor = calculateNewEaseFactor(currentEaseFactor, quality)
                var interval = when (reviewCount) {
                    2 -> (SECOND_INTERVAL * newEaseFactor).toInt()
                    else -> {
                        // 使用最后一次的实际间隔计算
                        val lastInterval = calculateDaysSinceLastReview(card)
                        if (lastInterval > 0) {
                            (lastInterval * newEaseFactor).toInt()
                        } else {
                            // 如果无法获取上次间隔，使用保守估计
                            (SECOND_INTERVAL * Math.pow(newEaseFactor.toDouble(), (reviewCount - 1).toDouble())).toInt()
                        }
                    }
                }

                // 限制最大间隔
                interval = min(interval, MAX_INTERVAL_DAYS)

                Log.d(TAG, "第${reviewCount + 1}次复习，新间隔=${interval}天，新easeFactor=${newEaseFactor}")
                ReviewResult(
                    nextIntervalDays = interval,
                    newEaseFactor = newEaseFactor,
                    shouldReset = false
                )
            }
        }
    }

    /**
     * 计算新的记忆容易度因子
     *
     * SM2算法公式：
     * EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
     * 其中：
     * - EF 是当前记忆容易度因子
     * - q 是质量评分（0-5）
     * - EF' 是新的记忆容易度因子
     *
     * 公式解析：
     * - 质量越高（q接近5），EF增加越多
     * - 质量越低（q接近0），EF减少越多
     * - 最小值限制为1.3，确保不会无限降低
     */
    private fun calculateNewEaseFactor(currentEaseFactor: Float, quality: Int): Float {
        val easeFactorDelta = 0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f)
        val newEaseFactor = currentEaseFactor + easeFactorDelta

        // 确保不低于最小值
        return max(newEaseFactor, MIN_EASE_FACTOR)
    }

    /**
     * 计算自上次复习以来的天数
     */
    private fun calculateDaysSinceLastReview(card: CardEntity): Int {
        val lastReviewedAt = card.lastReviewedAt ?: return 0
        val now = System.currentTimeMillis()
        val diff = now - lastReviewedAt
        return TimeUnit.MILLISECONDS.toDays(diff).toInt()
    }

    /**
     * 将质量评分转换为描述性文字
     */
    fun getQualityDescription(quality: Int): String {
        return when (quality) {
            5 -> "完美回答 - 轻松回忆"
            4 -> "正确回答 - 有些犹豫"
            3 -> "正确回答 - 费了很大劲"
            2 -> "错误回答 - 看到答案后觉得熟悉"
            1 -> "错误回答 - 看到答案后记得"
            0 -> "完全忘记"
            else -> "未知评分"
        }
    }

    /**
     * 检查卡片是否需要复习
     */
    fun isDueForReview(card: CardEntity): Boolean {
        val now = System.currentTimeMillis()
        return now >= card.nextReviewAt
    }

    /**
     * 计算复习紧迫度
     * 返回值：0-1，越接近1表示越紧急
     */
    fun calculateUrgency(card: CardEntity): Float {
        val now = System.currentTimeMillis()
        val daysOverdue = TimeUnit.MILLISECONDS.toDays(now - card.nextReviewAt).toFloat()

        return when {
            daysOverdue <= 0 -> 0f // 未到复习时间
            daysOverdue <= 1 -> 0.3f // 逾期1天内
            daysOverdue <= 3 -> 0.6f // 逾期3天内
            daysOverdue <= 7 -> 0.8f // 逾期7天内
            else -> 1f // 逾期超过7天
        }
    }
}
