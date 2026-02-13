package com.evomind.app.aigc.discussion

import android.content.Context
import android.util.Log
import com.evomind.app.database.AppDatabase
import com.evomind.app.database.entity.DiscussionEntity
import com.evomind.app.database.entity.DiscussionMessageEntity
import com.evomind.app.database.entity.MessageSenderType
import com.evomind.app.aigc.AIServiceWrapper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * AI讨论服务
 * 管理用户与AI的讨论会话，支持多轮对话和上下文保持
 */
class DiscussionService(
    private val context: Context
) {
    companion object {
        private const val TAG = "DiscussionService"
        private const val MAX_CONTEXT_MESSAGES = 10 // 最大上下文消息数
        private const val DEFAULT_TEMPERATURE = 0.7 // 默认创意温度
    }

    private val database = AppDatabase.getInstance(context)
    private val discussionDao = database.discussionDao()
    private val messageDao = database.discussionMessageDao()
    private val aiService = AIServiceWrapper.getInstance(context)

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    data class DiscussionSession(
        val discussion: DiscussionEntity,
        val messages: List<DiscussionMessageEntity>
    )

    data class AIReply(
        val content: String,
        val tokenUsage: Int,
        val relatedNodes: List<String> = emptyList()
    )

    suspend fun createDiscussion(
        cardId: Long,
        sessionTitle: String,
        contextSummary: String? = null
    ): DiscussionEntity = withContext(Dispatchers.IO) {
        Log.d(TAG, "创建新讨论会话: cardId=$cardId, title=$sessionTitle")

        val discussion = DiscussionEntity(
            cardId = cardId,
            sessionTitle = sessionTitle,
            contextSummary = contextSummary
        )

        val discussionId = discussionDao.insert(discussion)
        discussionDao.deactivateOtherDiscussions(discussionId, cardId)

        Log.d(TAG, "讨论会话创建成功: id=$discussionId")
        return@withContext discussion.copy(id = discussionId)
    }

    suspend fun getActiveDiscussion(cardId: Long): DiscussionEntity? = withContext(Dispatchers.IO) {
        discussionDao.getActiveDiscussionByCard(cardId)
    }

    fun getDiscussionsByCard(cardId: Long): Flow<List<DiscussionEntity>> {
        return discussionDao.getByCardId(cardId)
    }

    suspend fun getDiscussionDetails(discussionId: Long): DiscussionSession? = withContext(Dispatchers.IO) {
        val discussion = discussionDao.getById(discussionId).first()
        if (discussion == null) {
            Log.w(TAG, "找不到讨论会话: $discussionId")
            return@withContext null
        }

        val messages = messageDao.getByDiscussionId(discussionId).first()
        Log.d(TAG, "获取讨论详情: ${messages.size} 条消息")
        return@withContext DiscussionSession(discussion, messages)
    }

    suspend fun sendMessage(
        discussionId: Long,
        messageContent: String,
        relatedNodes: List<String> = emptyList(),
        temperature: Double = DEFAULT_TEMPERATURE,
        onLoading: () -> Unit = {},
        onSuccess: (AIReply) -> Unit = {},
        onError: (String) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "发送消息: discussionId=$discussionId")

        onLoading()

        val discussion = discussionDao.getById(discussionId).first()
        if (discussion == null) {
            onError("讨论会话不存在")
            return@withContext
        }

        val recentMessages = getRecentContext(discussionId)
        val userMessage = DiscussionMessageEntity(
            discussionId = discussionId,
            messageIndex = getNextMessageIndex(discussionId),
            senderType = MessageSenderType.USER,
            messageContent = messageContent,
            relatedNodes = if (relatedNodes.isEmpty()) null else relatedNodes.joinToString(",")
        )

        messageDao.insert(userMessage)
        val aiMessages = buildConversationHistory(recentMessages, messageContent)

        aiService.chat(
            messages = aiMessages,
            temperature = temperature,
            onSuccess = { aiResponse ->
                if (aiResponse.success) {
                    val aiMessage = DiscussionMessageEntity(
                        discussionId = discussionId,
                        messageIndex = getNextMessageIndex(discussionId),
                        senderType = MessageSenderType.AI,
                        messageContent = aiResponse.content,
                        tokenUsage = aiResponse.tokenUsage
                    )

                    messageDao.insert(aiMessage)
                    val now = System.currentTimeMillis()
                    discussionDao.updateLastMessageTime(discussionId, now)

                    onSuccess(
                        AIReply(
                            content = aiResponse.content,
                            tokenUsage = aiResponse.tokenUsage
                        )
                    )

                    Log.d(TAG, "AI回复成功: 使用 ${aiResponse.tokenUsage} tokens")
                } else {
                    onError(aiResponse.errorMessage ?: "未知错误")
                }
            },
            onError = { errorMessage ->
                onError(errorMessage)
                Log.e(TAG, "AI回复失败: $errorMessage")
            }
        )
    }

    private suspend fun getRecentContext(discussionId: Long): List<DiscussionMessageEntity> =
        withContext(Dispatchers.IO) {
            val allMessages = messageDao.getByDiscussionId(discussionId).first()
            val recentCount = minOf(allMessages.size, MAX_CONTEXT_MESSAGES)
            return@withContext allMessages.takeLast(recentCount)
        }

    private fun buildConversationHistory(
        recentMessages: List<DiscussionMessageEntity>,
        newMessage: String
    ): List<AIServiceWrapper.AIMessage> {
        val aiMessages = mutableListOf<AIServiceWrapper.AIMessage>()

        recentMessages.forEach { message ->
            val role = when (message.senderType) {
                MessageSenderType.USER -> AIServiceWrapper.Role.USER
                MessageSenderType.AI -> AIServiceWrapper.Role.ASSISTANT
                MessageSenderType.SYSTEM -> AIServiceWrapper.Role.SYSTEM
            }

            aiMessages.add(
                AIServiceWrapper.AIMessage(
                    content = message.messageContent,
                    role = role
                )
            )
        }

        aiMessages.add(
            AIServiceWrapper.AIMessage(
                content = newMessage,
                role = AIServiceWrapper.Role.USER
            )
        )

        return aiMessages
    }

    private suspend fun getNextMessageIndex(discussionId: Long): Int = withContext(Dispatchers.IO) {
        val count = messageDao.getCountByDiscussion(discussionId)
        return@withContext count
    }

    suspend fun generateDiscussionSummary(discussionId: Long, onComplete: (String) -> Unit) = withContext(Dispatchers.IO) {
        val discussion = discussionDao.getById(discussionId).first()
        if (discussion == null) {
            Log.e(TAG, "生成总结失败: 找不到讨论会话")
            onComplete("")
            return@withContext
        }

        val messages = messageDao.getByDiscussionId(discussionId).first()
        if (messages.isEmpty()) {
            onComplete("没有讨论内容需要总结")
            return@withContext
        }

        val summaryPrompt = buildSummaryPrompt(messages, discussion.sessionTitle)
        val summaryMessage = DiscussionMessageEntity(
            discussionId = discussionId,
            messageIndex = getNextMessageIndex(discussionId),
            senderType = MessageSenderType.AI,
            messageContent = "[生成讨论总结...]"
        )

        messageDao.insert(summaryMessage)
        aiService.chat(
            messages = listOf(
                AIServiceWrapper.AIMessage(
                    content = summaryPrompt,
                    role = AIServiceWrapper.Role.USER
                )
            ),
            temperature = 0.5,
            onSuccess = { aiResponse ->
                if (aiResponse.success) {
                    val summary = aiResponse.content

                    coroutineScope.launch(Dispatchers.IO) {
                        val finalSummaryMessage = summaryMessage.copy(
                            messageContent = "## 讨论总结\n\n$summary",
                            tokenUsage = aiResponse.tokenUsage
                        )
                        messageDao.update(finalSummaryMessage)

                        discussionDao.update(
                            discussion.copy(
                                contextSummary = summary,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }

                    onComplete(summary)
                    Log.d(TAG, "讨论总结生成成功")
                } else {
                    onComplete("生成总结失败")
                }
            },
            onError = { errorMessage ->
                onComplete("生成总结失败: $errorMessage")
                Log.e(TAG, "生成总结失败: $errorMessage")
            }
        )
    }

    private fun buildSummaryPrompt(messages: List<DiscussionMessageEntity>, sessionTitle: String): String {
        val userMessages = messages.filter { it.senderType == MessageSenderType.USER }
        val aiMessages = messages.filter { it.senderType == MessageSenderType.AI }

        val prompt = StringBuilder()
        prompt.appendLine("请为下面的讨论生成一个简洁的总结。讨论主题是: $sessionTitle")
        prompt.appendLine()
        prompt.appendLine("用户提问:")
        userMessages.takeLast(5).forEach { message ->
            prompt.appendLine("- ${message.messageContent}")
        }
        prompt.appendLine()
        prompt.appendLine("AI回复重点:")
        aiMessages.takeLast(5).forEach { message ->
            val content = if (message.messageContent.length > 100) {
                message.messageContent.substring(0, 100) + "..."
            } else {
                message.messageContent
            }
            prompt.appendLine("- $content")
        }
        prompt.appendLine()
        prompt.appendLine("请生成一个包含以下内容的总结:")
        prompt.appendLine("1. 讨论的主要话题")
        prompt.appendLine("2. 关键知识点")
        prompt.appendLine("3. 待深入研究的问题")

        return prompt.toString()
    }

    suspend fun closeDiscussion(discussionId: Long) = withContext(Dispatchers.IO) {
        val discussion = discussionDao.getById(discussionId).first()
        if (discussion != null) {
            discussionDao.update(discussion.copy(isActive = false))
            Log.d(TAG, "讨论会话已关闭: $discussionId")
        }
    }

    suspend fun getDiscussionStats(discussionId: Long): DiscussionStats = withContext(Dispatchers.IO) {
        val messageStats = messageDao.getMessageStats(discussionId)
        val totalMessages = messageStats?.totalMessages ?: 0
        val userMessages = messageStats?.userMessages ?: 0
        val aiMessages = messageStats?.aiMessages ?: 0
        val avgMessageLength = messageStats?.avgMessageLength ?: 0.0

        val durationMillis = if (totalMessages > 1) {
            val firstMessage = messageDao.getByDiscussionId(discussionId).first().firstOrNull()
            val lastMessage = messageDao.getLastMessage(discussionId)

            if (firstMessage != null && lastMessage != null) {
                lastMessage.createdAt - firstMessage.createdAt
            } else {
                0L
            }
        } else {
            0L
        }

        DiscussionStats(
            totalMessages = totalMessages,
            userMessages = userMessages,
            aiMessages = aiMessages,
            durationMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis),
            avgMessageLength = avgMessageLength
        )
    }

    data class DiscussionStats(
        val totalMessages: Int,
        val userMessages: Int,
        val aiMessages: Int,
        val durationMinutes: Long,
        val avgMessageLength: Double
    )

    fun cleanup() {
        coroutineScope.cancel()
    }
}
