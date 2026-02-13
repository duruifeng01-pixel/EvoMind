package com.evomind.app.payment

import android.content.Context
import android.util.Log
import com.evomind.app.database.AppDatabase
import com.evomind.app.database.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SubscriptionService(
    private val context: Context
) {
    companion object {
        private const val TAG = "SubscriptionService"

        const val TOKENS_PER_YUAN = 150 // 1元 = 150 tokens (示例比例)
        const val FREE_TOKENS_DAILY = 50 // 免费用户每日额度
    }

    private val database = AppDatabase.getInstance(context)
    private val userSubscriptionDao = database.userSubscriptionDao()
    private val subscriptionPlanDao = database.subscriptionPlanDao()
    private val tokenUsageDao = database.tokenUsageDao()
    private val paymentRecordDao = database.paymentRecordDao()

    suspend fun initializeSubscriptionPlans() = withContext(Dispatchers.IO) {
        Log.d(TAG, "初始化订阅计划")

        val existingPlans = subscriptionPlanDao.getAllActive().first()
        if (existingPlans.isNotEmpty()) {
            Log.d(TAG, "订阅计划已存在，跳过初始化")
            return@withContext
        }

        val plans = listOf(
            SubscriptionPlanEntity(
                planId = SubscriptionPlanId.FREE.name,
                name = "免费版",
                description = "适合轻度使用者",
                price = 0.0,
                durationDays = 30,
                tokenQuota = 1000,
                features = listOf(
                    "基础AI总结",
                    "每日50 tokens",
                    "最多3个讨论会话",
                    "基础思维导图"
                ).let { com.google.gson.Gson().toJson(it) },
                sortOrder = 0
            ),
            SubscriptionPlanEntity(
                planId = SubscriptionPlanId.BASIC.name,
                name = "基础版",
                description = "适合日常使用",
                price = 29.0,
                durationDays = 30,
                tokenQuota = 5000,
                features = listOf(
                    "AI卡片生成（5000 tokens）",
                    "无限讨论会话",
                    "高级思维导图",
                    "认知冲突检测",
                    "优先客服支持"
                ).let { com.google.gson.Gson().toJson(it) },
                sortOrder = 1
            ),
            SubscriptionPlanEntity(
                planId = SubscriptionPlanId.PRO.name,
                name = "专业版",
                description = "适合重度使用者",
                price = 99.0,
                durationDays = 30,
                tokenQuota = 20000,
                features = listOf(
                    "AI卡片生成（20000 tokens）",
                    "无限讨论会话",
                    "高级思维导图",
                    "认知冲突检测",
                    "复习系统（间隔重复）",
                    "文章爬虫（100篇）",
                    "导出功能",
                    "优先客服支持"
                ).let { com.google.gson.Gson().toJson(it) },
                sortOrder = 2
            ),
            SubscriptionPlanEntity(
                planId = SubscriptionPlanId.ENTERPRISE.name,
                name = "企业版",
                description = "适合团队使用",
                price = 299.0,
                durationDays = 30,
                tokenQuota = 100000, // 实际上应该是无限，这里设置一个很大的数
                features = listOf(
                    "AI卡片生成（100000 tokens）",
                    "无限讨论会话",
                    "高级思维导图",
                    "认知冲突检测",
                    "复习系统（间隔重复）",
                    "文章爬虫（无限）",
                    "导出功能",
                    "团队共享",
                    "专属客服支持"
                ).let { com.google.gson.Gson().toJson(it) },
                sortOrder = 3
            )
        )

        subscriptionPlanDao.insertAll(plans)
        Log.d(TAG, "订阅计划初始化成功，共 ${plans.size} 个计划")
    }

    suspend fun getAvailablePlans(): List<SubscriptionPlanEntity> = withContext(Dispatchers.IO) {
        subscriptionPlanDao.getAllActive().first()
    }

    suspend fun getUserSubscription(userId: Long): UserSubscriptionEntity? = withContext(Dispatchers.IO) {
        userSubscriptionDao.getCurrentSubscription(userId).first()
    }

    suspend fun getTokenBalance(userId: Long): TokenBalance = withContext(Dispatchers.IO) {
        val subscription = getUserSubscription(userId)
        val usedTokens = tokenUsageDao.getTotalUsedTokens(userId).first() ?: 0

        TokenBalance(
            totalTokens = subscription?.tokenQuota ?: 1000,
            usedTokens = usedTokens.toInt(),
            remainingTokens = (subscription?.tokenQuota ?: 1000) - usedTokens.toInt(),
            subscriptionName = subscription?.let { getPlanName(it.planId) } ?: "免费版",
            subscriptionStatus = subscription?.status ?: SubscriptionStatus.ACTIVE
        )
    }

    private suspend fun getPlanName(planId: Long?): String {
        if (planId == null) return "免费版"
        val plan = subscriptionPlanDao.getPlanById(planId).first()
        return plan?.name ?: "未知"
    }

    data class TokenBalance(
        val totalTokens: Long,
        val usedTokens: Int,
        val remainingTokens: Long,
        val subscriptionName: String,
        val subscriptionStatus: SubscriptionStatus
    )

    suspend fun consumeTokens(
        userId: Long,
        tokens: Int,
        serviceType: AIServiceType,
        description: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (tokens <= 0) {
            Log.w(TAG, "Token消耗数量无效: $tokens")
            return@withContext false
        }

        val balance = getTokenBalance(userId)

        if (balance.remainingTokens < tokens) {
            Log.w(TAG, "Token余额不足，需要: $tokens, 剩余: ${balance.remainingTokens}")
            return@withContext false
        }

        val usageRecord = TokenUsageRecordEntity(
            userId = userId,
            tokensUsed = tokens,
            serviceType = serviceType,
            description = description
        )

        tokenUsageDao.insert(usageRecord)

        Log.d(TAG, "Token消耗成功: $tokens tokens，服务: $serviceType，描述: $description")
        return@withContext true
    }

    suspend fun purchaseTokens(
        userId: Long,
        yuanAmount: Double,
        paymentType: PaymentType,
        transactionId: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (yuanAmount <= 0) {
            Log.w(TAG, "购买金额无效: $yuanAmount")
            return@withContext false
        }

        val tokensToAdd = (yuanAmount * TOKENS_PER_YUAN).toLong()

        val paymentRecord = PaymentRecordEntity(
            userId = userId,
            transactionId = transactionId,
            paymentType = paymentType,
            amount = yuanAmount,
            status = PaymentStatus.SUCCESS,
            description = "充值 ${tokensToAdd} tokens"
        )

        paymentRecordDao.insert(paymentRecord)

        val currentSubscription = getUserSubscription(userId)
        if (currentSubscription != null) {
            val updatedSubscription = currentSubscription.copy(
                tokenBalance = currentSubscription.tokenBalance + tokensToAdd,
                updatedAt = System.currentTimeMillis()
            )
            userSubscriptionDao.update(updatedSubscription)
        } else {
            val newSubscription = UserSubscriptionEntity(
                userId = userId,
                planId = null,
                status = SubscriptionStatus.ACTIVE,
                tokenBalance = tokensToAdd,
                tokenUsed = 0,
                endDate = null
            )
            userSubscriptionDao.insert(newSubscription)
        }

        Log.d(TAG, "Token购买成功: $yuanAmount 元 = $tokensToAdd tokens")
        return@withContext true
    }

    suspend fun subscribePlan(
        userId: Long,
        planId: String,
        paymentRecordId: Long? = null
    ): UserSubscriptionEntity? = withContext(Dispatchers.IO) {
        val plan = subscriptionPlanDao.getPlanByPlanId(planId).first()
        if (plan == null) {
            Log.e(TAG, "订阅计划不存在: $planId")
            return@withContext null
        }

        val endDate = System.currentTimeMillis() + plan.durationDays * 24 * 60 * 60 * 1000L

        val subscription = UserSubscriptionEntity(
            userId = userId,
            planId = plan.id,
            status = SubscriptionStatus.ACTIVE,
            tokenBalance = plan.tokenQuota,
            tokenUsed = 0,
            endDate = endDate
        )

        val subscriptionId = userSubscriptionDao.insert(subscription)

        if (paymentRecordId != null) {
            val paymentRecord = paymentRecordDao.getById(paymentRecordId).first()
            if (paymentRecord != null) {
                val updatedPayment = paymentRecord.copy(
                    metadata = PaymentMetadata(
                        subscriptionId = subscriptionId,
                        subscriptionPlanId = planId
                    ).let { com.google.gson.Gson().toJson(it) }
                )
                paymentRecordDao.update(updatedPayment)
            }
        }

        Log.d(TAG, "用户订阅成功: userId=$userId, plan=$planId, tokens=${plan.tokenQuota}")
        return@withContext subscription.copy(id = subscriptionId)
    }

    suspend fun getUsageReport(
        userId: Long,
        days: Int = 30
    ): UsageReport = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L

        val usages = tokenUsageDao.getUsagesSince(userId, startTime).first()
        val stats = tokenUsageDao.getUsageStats(userId, startTime).first()

        val byServiceType = usages.groupBy { it.serviceType }
            .mapValues { (_, records) ->
                records.sumOf { it.tokensUsed }
            }

        UsageReport(
            periodDays = days,
            totalTokensUsed = stats?.totalTokens ?: 0,
            averageDailyUsage = stats?.avgDailyUsage ?: 0.0,
            byServiceType = byServiceType,
            records = usages
        )
    }

    data class UsageReport(
        val periodDays: Int,
        val totalTokensUsed: Int,
        val averageDailyUsage: Double,
        val byServiceType: Map<AIServiceType, Int>,
        val records: List<TokenUsageRecordEntity>
    )

    suspend fun getCostEstimate(tokens: Int): CostEstimate {
        val yuan = tokens / TOKENS_PER_YUAN.toDouble()
        return CostEstimate(
            tokens = tokens,
            estimatedCost = yuan,
            unitPrice = 1.0 / TOKENS_PER_YUAN
        )
    }

    data class CostEstimate(
        val tokens: Int,
        val estimatedCost: Double,
        val unitPrice: Double
    )
}
