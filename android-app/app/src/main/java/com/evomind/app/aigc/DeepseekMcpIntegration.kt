package com.evomind.app.aigc

import android.util.Log
import com.evomind.app.aigc.model.AiCard

/**
 * Deepseek MCP集成帮助类
 * 通过Claude的MCP服务器调用Deepseek API
 */
object DeepseekMcpIntegration {
    private const val TAG = "DeepseekMcpIntegration"

    /**
     * 生成认知卡片的系统提示词
     *
     * 使用示例：
     * 1. 确保已在 ~/.claude/settings.json 中配置Deepseek API Key
     * 2. 调用 /plan 或直接使用 deepseek MCP
     * 3. 传入此提示词和素材内容
     */
    fun getCardGenerationPrompt(sourceTitle: String, sourceContent: String): String {
        return """
            # 认知卡片生成任务

            请为以下学习素材生成一个认知卡片：

            ## 素材信息
            - **标题**: $sourceTitle
            - **内容**: $sourceContent

            ## 生成要求

            请生成包含以下字段的JSON格式认知卡片：

            ```json
            {
              "title": "简洁的标题（不超过20字）",
              "oneLineGuide": "一句话导读，概括核心概念（20-30字）",
              "summary": "100-200字的摘要",
              "keyPoints": ["要点1", "要点2", "要点3", "要点4", "要点5"],
              "mindMap": "Markdown格式的思维导图，支持节点下钻",
              "reviewAdvice": "复习建议（50-100字）",
              "tags": ["标签1", "标签2", "标签3"],
              "difficultyLevel": 3,
              "estimatedTime": 15
            }
            ```

            ## 字段说明

            1. **title**: 从素材中提取核心主题作为标题
            2. **oneLineGuide**: **一句话导读，用简洁的语言概括核心概念（20-30字）**
            3. **summary**: 概括素材的主要内容和学习价值
            4. **keyPoints**: 提取3-5个最重要的知识点或观点
            5. **mindMap**: 使用Markdown格式创建层级结构的思维导图，**支持节点下钻查看原文段落**
            6. **reviewAdvice**: 基于遗忘曲线提供复习建议
            7. **tags**: 生成3-5个相关标签，便于分类
            8. **difficultyLevel**: 评估难度（1-5分，1=非常简单，5=非常困难）
            9. **estimatedTime**: 估计掌握所需时间（分钟）

            ## 思维导图特殊要求

            **重要**: 生成的思维导图需要支持"下钻显示原文段落"功能：

            1. 每个主要节点应该对应原文的一个逻辑段落
            2. 如果可能，在生成要点和思维导图时，记录与原文的对应关系
            3. 思维导图应该清晰地反映原文的结构层次（引言、正文各段、结论）
            4. 当用户点击思维导图的某个节点时，应该能够定位到对应的原文段落

            示例：
            # 文章主题
            ## 第一段核心观点（对应原文第1段）
            ### 分论点1（对应原文第1段细节）
            ## 第二段核心观点（对应原文第2段）
            ### 分论点2（对应原文第2段细节）

            ## 生成步骤

            请按以下步骤生成：

            1. 分析素材的核心主题和关键信息
            2. 创建简洁明了的标题
            3. 生成**一句话导读**
            4. 撰写包含主要观点的摘要
            5. 提取最重要的3-5个要点
            6. 设计思维导图展示知识结构（确保与原文段落有对应关系）
            7. 根据内容难度给出复习建议
            8. 生成相关标签
            9. 评估难度等级和所需时间

            ## 输出格式

            请直接返回JSON格式的认知卡片，不要包含其他文本或解释。

        "} trimIndent()
    }

    /**
     * 生成讨论提示词
     */
    fun getDiscussionPrompt(
        sourceTitle: String,
        sourceContent: String,
        conversationHistory: List<String> = emptyList()
    ): String {
        val historyText = if (conversationHistory.isNotEmpty()) {
            conversationHistory.joinToString("\n")
        } else {
            "（无历史对话）"
        }

        return """
            # AI讨论任务

            你是一位知识渊博的导师，正在和学生讨论学习材料。

            ## 学习材料

            **标题**: $sourceTitle

            **内容**:
            $sourceContent

            ## 对话历史

            $historyText

            ## 讨论要求

            1. 基于材料内容回答问题
            2. 提供深入见解和扩展思考
            3. 引导用户深入理解主题
            4. 语言要亲切、易懂
            5. 可以适当提问促进思考

            请继续对话，提供有帮助的回复。
        """.trimIndent()
    }

    /**
     * 计算预估成本
     *
     * Deepseek API 定价：
     * - 输入: ￥1.00 / 1M tokens
     * - 输出: ￥2.00 / 1M tokens
     */
    fun estimateGenerationCost(
        sourceTitle: String,
        sourceContent: String,
        prompt: String = PROMPT_SUMMARY
    ): CostEstimate {
        val titleTokens = estimateTokens(sourceTitle)
        val contentTokens = estimateTokens(sourceContent)
        val promptTokens = estimateTokens(prompt)

        val inputTokens = titleTokens + contentTokens + promptTokens
        val outputTokens = 800 // 预估输出token数

        val inputCost = (inputTokens / 1_000_000.0) * 1.0
        val outputCost = (outputTokens / 1_000_000.0) * 2.0
        val totalCost = inputCost + outputCost

        return CostEstimate(
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            estimatedCost = totalCost
        )
    }

    /**
     * 估算文本的token数
     * 中文: ~1.8 tokens/字符
     * 英文: ~0.25 tokens/单词
     */
    private fun estimateTokens(text: String): Int {
        return if (text.any { it.toInt() > 127 }) {
            // 包含中文，按1.8 tokens/字符估算
            (text.length * 1.8).toInt()
        } else {
            // 纯英文，按0.25 tokens/字符估算
            (text.length * 0.25).toInt()
        }
    }

    /**
     * 从JSON响应解析AiCard
     */
    fun parseAiCardFromJson(jsonString: String, sourceId: Long): AiCard? {
        return try {
            val json = JSONObject(jsonString)

            AiCard(
                sourceId = sourceId,
                title = json.optString("title"),
                summary = json.optString("summary"),
                keyPoints = json.optJSONArray("keyPoints")?.let { array ->
                    List(array.length()) { i -> array.optString(i) }
                } ?: emptyList(),
                mindMap = json.optString("mindMap"),
                reviewAdvice = json.optString("reviewAdvice"),
                aiTags = json.optJSONArray("tags")?.let { array ->
                    List(array.length()) { i -> array.optString(i) }
                } ?: emptyList(),
                difficultyLevel = json.optInt("difficultyLevel", 3),
                estimatedTime = json.optInt("estimatedTime", 15)
            )
        } catch (e: Exception) {
            Log.e(TAG, "解析JSON失败: ${e.message}", e)
            null
        }
    }

    /**
     * 日志记录（在实现时调用）
     */
    fun logGenerationSuccess(card: AiCard, cost: CostEstimate? = null) {
        Log.i(TAG, "✅ 认知卡片生成成功: ${card.title}")
        Log.d(TAG, "   - 要点数: ${card.keyPoints.size}")
        Log.d(TAG, "   - 难度等级: ${card.difficultyLevel}/5")
        Log.d(TAG, "   - 预估时间: ${card.estimatedTime}分钟")
        cost?.let {
            Log.d(TAG, "   - 预估成本: ￥${String.format("%.4f", it.estimatedCost)}")
        }
    }

    fun logGenerationError(sourceId: Long, error: String) {
        Log.e(TAG, "❌ 认知卡片生成失败 (sourceId: $sourceId): $error")
    }

    /**
     * 成本估算结果
     */
    data class CostEstimate(
        val inputTokens: Int,
        val outputTokens: Int,
        val estimatedCost: Double  // 单位：元
    )

    /**
     * 使用示例（集成指南）
     *
     * 在您的ViewModel或Repository中：
     *
     * 1. 生成提示词：
     *    val prompt = DeepseekMcpIntegration.getCardGenerationPrompt(title, content)
     *
     * 2. 检查MCP配置：
     *    - 确保~/.claude/settings.json包含deepseek MCP配置
     *    - 确认API Key已设置
     *
     * 3. 使用/plan命令或手动调用MCP：
     *    在Claude Code中：
     *     > 使用deepseek MCP，传入上述提示词，生成认知卡片
     *
     * 4. 处理响应：
     *    val cardJson = // 从MCP响应获取JSON
     *    val aiCard = DeepseekMcpIntegration.parseAiCardFromJson(cardJson, sourceId)
     *
     * 5. 保存卡片：
     *    // 保存到Room数据库
     *    cardDao.insert(aiCard.toEntity())
     */
}
""".trimIndent()