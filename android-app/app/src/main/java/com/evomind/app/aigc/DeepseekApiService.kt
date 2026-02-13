package com.evomind.app.aigc

import android.content.Context
import android.util.Log
import com.evomind.app.aigc.model.AiCard
import com.evomind.app.aigc.model.AiMessage
import com.evomind.app.aigc.model.DiscussionContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Deepseek AI API 服务类
 * 用于生成认知卡片和AI讨论
 */
class DeepseekApiService(private val context: Context) {

    companion object {
        private const val TAG = "DeepseekApiService"

        // Deepseek API 配置
        // 重要：请在 MCP 配置中设置您的 Deepseek API Key
        // 位置：~/.claude/settings.json -> pluginConfigs.mcp.deepseek.apiKey
        private const val BASE_URL = "https://api.deepseek.com"
        private const val API_VERSION = "v1"

        // API端点
        private const val ENDPOINT_CHAT = "/chat/completions"

        // 模型配置
        private const val MODEL_CHAT = "deepseek-chat"
        private const val MODEL_CODER = "deepseek-coder"

        // 重试配置
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 2000L

        // 超时配置
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 60L
        private const val WRITE_TIMEOUT = 30L

        // Token成本估算（用于计算费用）
        private const val COST_PER_1K_INPUT = 0.001  // 每1k输入token，单位：元
        private const val COST_PER_1K_OUTPUT = 0.002 // 每1k输出token，单位：元

        // 提示词模板
        private const val PROMPT_SUMMARY = """
            请对以下文本进行总结，生成一个包含一句话导读的认知卡片：

            要求：
            1. 提供一个简洁的标题（不超过20个字）
            2. 生成**一句话导读**（用一句话概括核心概念，20-30字，例如："核心概念：认知卡片通过主动回忆提升记忆效率"）
            3. 生成一段100-200字的摘要
            4. 提取3-5个关键要点
            5. 创建一个简单的思维导图（用markdown格式）
            6. 给出复习建议
            7. 生成相关标签（3-5个）
            8. 评估难度等级（1-5分）
            9. 估计学习时间（分钟）

            请用JSON格式返回：
            {
                "title": "标题",
                "oneLineGuide": "一句话导读，概括核心概念",
                "summary": "摘要",
                "keyPoints": ["要点1", "要点2", "要点3"],
                "mindMap": "思维导图markdown",
                "reviewAdvice": "复习建议",
                "tags": ["标签1", "标签2"],
                "difficultyLevel": 3,
                "estimatedTime": 15
            }

            文本内容：
        """.trimIndent()

        private const val PROMPT_DISCUSSION = """
            你是一位知识渊博的导师，正在和学生讨论以下学习材料。

            要求：
            1. 基于材料内容回答问题
            2. 提供深入见解和扩展思考
            3. 引导用户深入理解主题
            4. 语言要亲切、易懂
            5. 可以适当提问促进思考

            材料标题：{0}
            材料内容：{1}

            当前对话：
        """.trimIndent()

        private const val PROMPT_MINDMAP = """
            请为以下文本创建一个思维导图，用Markdown格式输出：

            要求：
            1. 使用层级结构展示主要概念
            2. 用简洁的词语或短语
            3. 突出关键关系和联系
            4. 格式清晰，易于理解

            文本内容：
        """.trimIndent()
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    private var retryCount = 0

    init {
        Log.i(TAG, "Deepseek AI服务初始化成功")
        Log.i(TAG, "API端点: $BASE_URL/$API_VERSION")
        Log.i(TAG, "建议在MCP配置中设置API Key，位置: ~/.claude/settings.json")
    }

    /**
     * 从OCR结果生成认知卡片
     *
     * @param sourceTitle 素材标题
     * @param sourceContent 素材内容（清洗后的文本）
     * @param sourceId 关联的素材ID
     * @return AI生成的认知卡片
     */
    suspend fun generateCard(
        sourceTitle: String,
        sourceContent: String,
        sourceId: Long
    ): Result<AiCard> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始生成认知卡片: $sourceTitle")
            Log.d(TAG, "内容长度: ${sourceContent.length}")

            // 构建请求消息
            val prompt = "$PROMPT_SUMMARY\n\n标题: $sourceTitle\n内容: $sourceContent"

            // 调用Deepseek API
            val response = callDeepseekApi(prompt, MODEL_CHAT)

            // 解析JSON响应
            val card = parseCardResponse(response, sourceId)

            Log.i(TAG, "认知卡片生成成功: ${card.title}")
            Log.d(TAG, "卡片包含 ${card.keyPoints.size} 个关键要点")

            Result.success(card)

        } catch (e: Exception) {
            Log.e(TAG, "生成认知卡片失败: ${e.message}", e)
            handleCardError(sourceId, e)
        }
    }

    /**
     * 生成文本摘要
     */
    suspend fun generateSummary(
        sourceTitle: String,
        sourceContent: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "生成文本摘要: $sourceTitle")

            val prompt = """
                请对以下文本进行总结，生成一段100-200字的摘要：

                标题：$sourceTitle
                内容：$sourceContent

                摘要：
            """.trimIndent()

            val response = callDeepseekApi(prompt, MODEL_CHAT, maxTokens = 300)
            val summary = extractContentFromResponse(response)

            Log.i(TAG, "摘要生成成功：${summary.take(50)}...")
            Result.success(summary)

        } catch (e: Exception) {
            Log.e(TAG, "生成摘要失败: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 生成思维导图
     */
    suspend fun generateMindMap(
        sourceContent: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "生成思维导图")

            val prompt = "$PROMPT_MINDMAP\n\n$sourceContent"
            val response = callDeepseekApi(prompt, MODEL_CHAT, maxTokens = 500)
            val mindMap = extractContentFromResponse(response)

            Log.i(TAG, "思维导图生成成功，共 ${mindMap.length} 字符")
            Result.success(mindMap)

        } catch (e: Exception) {
            Log.e(TAG, "生成思维导图失败: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun callDeepseekApi(prompt: String, model: String, maxTokens: Int): String {
        val messages = listOf(Message(role = "user", content = prompt))
        return callDeepseekApiChat(messages, model, maxTokens, 0.7)
    }

    private suspend fun callDeepseekApiChat(
        messages: List<Message>,
        model: String = MODEL_CHAT,
        maxTokens: Int = 2000,
        temperature: Double = 0.7
    ): String {
        retryCount = 0

        while (retryCount < MAX_RETRY_COUNT) {
            try {
                Log.d(TAG, "调用Deepseek API (${retryCount + 1}/$MAX_RETRY_COUNT)")

                val requestBody = JSONObject().apply {
                    put("model", model)
                    put("messages", JSONArray(messages.map { it.toJson() }))
                    put("max_tokens", maxTokens)
                    put("temperature", temperature)
                }

                val request = Request.Builder()
                    .url("https://api.deepseek.com/v1/chat/completions")
                    .post(RequestBody.create(MediaType.parse("application/json"), requestBody.toString()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body()?.string()

                if (!response.isSuccessful) {
                    throw IOException("API调用失败: ${response.code()}")
                }

                return responseBody ?: throw IOException("API返回空响应")

            } catch (e: IOException) {
                retryCount++
                if (retryCount >= MAX_RETRY_COUNT) {
                    throw e
                }
                kotlinx.coroutines.delay(RETRY_DELAY_MS * retryCount)
            }
        }
        throw IOException("达到最大重试次数")
    }

    private fun parseCardResponse(response: String, sourceId: Long): AiCard {
        val json = JSONObject(response)
        val choices = json.optJSONArray("choices") ?: throw Exception("Invalid response")
        val choice = choices.getJSONObject(0)
        val content = choice.optJSONObject("message")?.optString("content") ?: ""

        val cleanedContent = content.removeSurrounding("```json\n", "\n```").trim()
        val cardData = JSONObject(cleanedContent)

        return AiCard(
            sourceId = sourceId,
            title = cardData.optString("title", "未命名卡片"),
            oneLineGuide = cardData.optString("oneLineGuide", ""),
            summary = cardData.optString("summary", ""),
            keyPoints = cardData.optJSONArray("keyPoints")?.let { array ->
                List(array.length()) { i -> array.optString(i, "") }
            } ?: emptyList(),
            mindMap = cardData.optString("mindMap", ""),
            reviewAdvice = cardData.optString("reviewAdvice", ""),
            aiTags = cardData.optJSONArray("tags")?.let { array ->
                List(array.length()) { i -> array.optString(i, "") }
            } ?: emptyList(),
            difficultyLevel = cardData.optInt("difficultyLevel", 3),
            estimatedTime = cardData.optInt("estimatedTime", 15)
        )
    }

    private fun handleCardError(sourceId: Long, e: Exception): Result<AiCard> {
        val errorCard = AiCard(
            sourceId = sourceId,
            title = "生成失败",
            oneLineGuide = "生成过程中出现错误",
            summary = "AI服务调用失败: ${e.message}",
            keyPoints = listOf("错误: ${e.javaClass.simpleName}"),
            mindMap = "",
            aiTags = listOf("错误"),
            difficultyLevel = 1,
            estimatedTime = 5
        )
        return Result.success(errorCard)
    }

    private fun extractContentFromResponse(response: String): String {
        val json = JSONObject(response)
        val choices = json.optJSONArray("choices") ?: return response
        val choice = choices.getJSONObject(0)
        return choice.optJSONObject("message")?.optString("content") ?: response
    }

    private data class Message(val role: String, val content: String) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("role", role)
            put("content", content)
        }
    }
}
