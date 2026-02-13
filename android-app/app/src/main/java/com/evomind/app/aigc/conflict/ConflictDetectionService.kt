package com.evomind.app.aigc.conflict

import android.content.Context
import android.util.Log
import com.evomind.app.database.AppDatabase
import com.evomind.app.database.entity.SourceEntity
import com.evomind.app.ocr.model.PlatformType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 认知冲突检测服务
 * 检测新文章内容与用户已有语料库的冲突
 *
 * 用户明确指出：认知冲突是"显示该文章的某个观点和用户语料库的内容有冲突"
 */
class ConflictDetectionService(
    private val context: Context
) {
    private companion object {
        const val TAG = "ConflictDetectionService"

        // 相似度阈值（用于冲突判断）
        const val CONFLICT_SIMILARITY_THRESHOLD = 0.75f  // 相似度超过75%可能构成冲突
        const val HIGH_CONFIDENCE_CONFLICT_THRESHOLD = 0.85f  // 高置信度冲突阈值

        // 句子长度限制
        const val MIN_SENTENCE_LENGTH = 10  // 最短句子长度（字符）
        const val MAX_SENTENCE_LENGTH = 200  // 最长句子长度（字符）
    }

    private val database = AppDatabase.getInstance(context)

    /**
     * 检测新内容与用户语料库的冲突
     *
     * @param newContent 新内容（从OCR或爬虫得到）
     * @param platform 内容来源平台
     * @return 冲突检测结果
     */
    suspend fun detectConflicts(
        newContent: String,
        platform: PlatformType? = null
    ): ConflictDetectionResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始检测认知冲突，新内容长度: ${newContent.length}")

            // Step 1: 获取用户的语料库（之前的所有内容，不包括当前内容）
            val corpus = getUserCorpus()

            if (corpus.isEmpty()) {
                Log.d(TAG, "用户语料库为空，跳过冲突检测")
                return@withContext ConflictDetectionResult.EmptyCorpus
            }

            Log.d(TAG, "用户语料库大小: ${corpus.size} 条记录")

            // Step 2: 从新内容中提取关键陈述句（观点和事实）
            val newStatements = extractKeyStatements(newContent)

            if (newStatements.isEmpty()) {
                Log.d(TAG, "未从新内容中提取到有效陈述句")
                return@withContext ConflictDetectionResult.NoExtractableStatements
            }

            Log.d(TAG, "提取到新内容中的 ${newStatements.size} 个关键陈述")

            // Step 3: 对每个陈述，在语料库中寻找潜在的冲突
            val conflicts = mutableListOf<CognitiveConflict>()

            for (statement in newStatements) {
                val statementConflicts = findConflictsForStatement(statement, corpus)
                conflicts.addAll(statementConflicts)
            }

            Log.d(TAG, "检测到 ${conflicts.size} 个潜在冲突")

            // Step 4: 过滤和排序冲突
            val filteredConflicts = filterAndSortConflicts(conflicts)

            return@withContext if (filteredConflicts.isNotEmpty()) {
                ConflictDetectionResult.ConflictsFound(filteredConflicts)
            } else {
                ConflictDetectionResult.NoConflicts
            }

        } catch (e: Exception) {
            Log.e(TAG, "冲突检测失败: ${e.message}", e)
            return@withContext ConflictDetectionResult.Error(e.message ?: "未知错误")
        }
    }

    /**
     * 获取用户的语料库（所有历史内容）
     */
    private suspend fun getUserCorpus(): List<SourceEntity> {
        return try {
            // 从数据库获取所有素材内容
            val sources = database.sourceDao().getAll().first()

            Log.d(TAG, "从数据库加载 ${sources.size} 条素材")

            // 过滤掉空内容和置信度低的内容
            sources.filter { source ->
                source.cleanedText.isNotBlank() &&
                source.confidence > 0.6f &&  // 置信度>60%
                source.cleanedText.length > 20  // 内容长度>20字符
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载语料库失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 从文本中提取关键陈述句（观点和事实）
     * 使用简单但有效的规则提取重要句子
     */
    private fun extractKeyStatements(content: String): List<String> {
        val statements = mutableListOf<String>()

        // Step 1: 按句子分割（使用常见的标点符号）
        val sentences = content.split(
            "。", ".", "！", "!", "？", "?", "\n", "\r\n"
        ).map { it.trim() }.filter { it.isNotEmpty() }

        // Step 2: 过滤和评分每个句子
        for (sentence in sentences) {
            if (shouldIncludeAsStatement(sentence)) {
                statements.add(sentence)

                // 如果句子太长，尝试分割（简单的分号分割）
                if (sentence.length > MAX_SENTENCE_LENGTH && sentence.contains("；")) {
                    val subStatements = sentence.split("；").map { it.trim() }
                    statements.addAll(subStatements.filter { shouldIncludeAsStatement(it) })
                }
            }
        }

        // Step 3: 按长度排序，优先保留较长的句子（通常包含更多信息）
        return statements
            .distinct()  // 去重
            .sortedByDescending { it.length }  // 按长度降序排列
            .take(20)  // 最多保留20个陈述（避免过多导致性能问题）
    }

    /**
     * 判断一个句子是否应作为关键陈述被包含
     */
    private fun shouldIncludeAsStatement(sentence: String): Boolean {
        // 长度检查
        if (sentence.length < MIN_SENTENCE_LENGTH || sentence.length > MAX_SENTENCE_LENGTH) {
            return false
        }

        // 过滤掉纯数字、纯符号等无意义内容
        if (sentence.matches(Regex("^\\d+$")) || sentence.matches(Regex("^[\\W_]+$"))) {
            return false
        }

        // 包含中文或英文的句子才有意义
        val hasChinese = sentence.any { it.toInt() in 0x4E00..0x9FFF }
        val hasEnglish = sentence.any { it.isLetter() }

        if (!hasChinese && !hasEnglish) {
            return false
        }

        // 过滤掉纯引用或无实际内容的句子
        if (sentence.matches(Regex("^["\"'].*["\"']$") && sentence.length < 30)) {
            return false
        }

        return true
    }

    /**
     * 为单个陈述寻找冲突
     */
    private fun findConflictsForStatement(
        statement: String,
        corpus: List<SourceEntity>
    ): List<CognitiveConflict> {
        val conflicts = mutableListOf<CognitiveConflict>()

        // 对语料库中的每个条目，检查是否与当前陈述冲突
        for (corpusItem in corpus) {
            // 进行简单的文本重叠检查（更复杂的语义分析需要NLP模型）
            val similarity = calculateTextSimilarity(statement, corpusItem.cleanedText)

            // 如果相似度超过阈值，认为可能存在冲突
            if (similarity > CONFLICT_SIMILARITY_THRESHOLD) {
                // 进一步检查是否真的冲突（不仅仅是相似）
                if (isPotentialConflict(statement, corpusItem.cleanedText, similarity)) {
                    val conflictType = determineConflictType(statement, corpusItem.cleanedText)

                    conflicts.add(
                        CognitiveConflict(
                            newStatement = statement,
                            corpusContent = corpusItem.cleanedText,
                            corpusSourceId = corpusItem.id,
                            corpusTitle = corpusItem.title,
                            similarityScore = similarity,
                            conflictType = conflictType,
                            suggestion = generateConflictSuggestion(conflictType)
                        )
                    )
                }
            }
        }

        // 如果有多个冲突涉及同一个语料库条目，只保留相似度最高的
        val groupedBySource = conflicts.groupBy { it.corpusSourceId }
        return groupedBySource.map { (_, conflictsForSource) ->
            conflictsForSource.maxByOrNull { it.similarityScore }!!
        }
    }

    /**
     * 计算两段文本的相似度
     * 使用简单的Jaccard相似度（基于字符n-gram）
     */
    private fun calculateTextSimilarity(text1: String, text2: String): Float {
        try {
            // 简单的字符n-gram实现
            val n = 2 // 使用2-gram

            val grams1 = extractNGrams(text1, n)
            val grams2 = extractNGrams(text2, n)

            if (grams1.isEmpty() || grams2.isEmpty()) {
                return 0f
            }

            val intersection = grams1.intersect(grams2.toSet())
            val union = grams1.union(grams2.toSet())

            return if (union.isNotEmpty()) {
                intersection.size.toFloat() / union.size.toFloat()
            } else {
                0f
            }
        } catch (e: Exception) {
            Log.e(TAG, "计算相似度失败: ${e.message}")
            return 0f
        }
    }

    /**
     * 提取n-gram
     */
    private fun extractNGrams(text: String, n: Int): List<String> {
        val ngrams = mutableListOf<String>()
        val cleanText = text.replace(Regex("\\s+"), " ").trim()

        if (cleanText.length < n) {
            return listOf(cleanText)
        }

        for (i in 0..cleanText.length - n) {
            ngrams.add(cleanText.substring(i, i + n))
        }

        return ngrams
    }

    /**
     * 判断两个文本是否潜在冲突
     * 相似但包含相反的词语/观点
     */
    private fun isPotentialConflict(
        text1: String,
        text2: String,
        similarity: Float
    ): Boolean {
        // 如果相似度极高（>0.9），可能是重复内容，不一定是冲突
        if (similarity > 0.9f) {
            return false
        }

        // 如果相似度只是中等（0.75-0.9），但包含相反词，可能是冲突
        if (similarity in 0.75f..0.9f) {
            // 检查是否包含常见相反词
            val oppositeWords = listOf(
                "增加" to "减少", "上升" to "下降", "积极" to "消极",
                "支持" to "反对", "有利" to "不利", "提高" to "降低",
                "扩大" to "缩小", "加强" to "减弱", "促进" to "阻碍",
                "正确" to "错误", "真" to "假", "是" to "否"
            )

            for ((word1, word2) in oppositeWords) {
                val hasOpposite = (text1.contains(word1) && text2.contains(word2)) ||
                                (text1.contains(word2) && text2.contains(word1))

                if (hasOpposite) {
                    return true
                }
            }
        }

        // 对于高相似度但不完全相同的，可能是不同观点的表达
        return similarity > CONFLICT_SIMILARITY_THRESHOLD
    }

    /**
     * 确定冲突类型
     */
    private fun determineConflictType(
        statement: String,
        corpusContent: String
    ): ConflictType {
        // 简单的规则基分类

        // 检查是否是事实性冲突（包含数字、时间等具体信息）
        val hasNumber1 = statement.contains(Regex("\\d+"))
        val hasNumber2 = corpusContent.contains(Regex("\\d+"))

        if (hasNumber1 && hasNumber2) {
            val numbers1 = Regex("\\d+").findAll(statement).map { it.value }.toSet()
            val numbers2 = Regex("\\d+").findAll(corpusContent).map { it.value }.toSet()

            // 如果有不同的数字，可能是事实冲突
            if (numbers1.intersect(numbers2).size != numbers1.size.coerceAtMost(numbers2.size)) {
                return ConflictType.FACTUAL
            }
        }

        // 检查是否是时间冲突
        val timeKeywords = listOf("年", "月", "日", "当时", "现在", "过去", "未来")
        val hasTime1 = timeKeywords.any { statement.contains(it) }
        val hasTime2 = timeKeywords.any { corpusContent.contains(it) }

        if (hasTime1 && hasTime2) {
            return ConflictType.TEMPORAL
        }

        // 默认认为是观点冲突
        return ConflictType.OPINION
    }

    /**
     * 根据冲突类型生成建议
     */
    private fun generateConflictSuggestion(type: ConflictType): String {
        return when (type) {
            ConflictType.FACTUAL -> "存在事实性冲突，建议核实数据来源的准确性和时效性"
            ConflictType.TEMPORAL -> "时间信息不一致，注意信息可能随时间变化"
            ConflictType.OPINION -> "观点不同，思考两种观点的适用场景和背景"
            ConflictType.LOGICAL -> "逻辑推导冲突，检查推理过程的合理性"
        }
    }

    /**
     * 过滤和排序冲突（按相似度降序）
     */
    private fun filterAndSortConflicts(
        conflicts: List<CognitiveConflict>
    ): List<CognitiveConflict> {
        return conflicts
            .filter { it.similarityScore >= CONFLICT_SIMILARITY_THRESHOLD }
            .sortedByDescending { it.similarityScore }
    }
}

/**
 * 认知冲突类型
 */
enum class ConflictType {
    FACTUAL,    // 事实冲突（数据、统计、事实陈述不一致）
    OPINION,    // 观点冲突（不同立场和观点）
    TEMPORAL,   // 时间冲突（同一件事在不同时间的描述不一致）
    LOGICAL     // 逻辑冲突（推理过程自相矛盾）
}

/**
 * 冲突检测结果
 */
sealed class ConflictDetectionResult {
    object EmptyCorpus : ConflictDetectionResult()  // 语料库为空
    object NoExtractableStatements : ConflictDetectionResult()  // 无法提取陈述
    object NoConflicts : ConflictDetectionResult()  // 未发现冲突

    data class ConflictsFound(
        val conflicts: List<CognitiveConflict>
    ) : ConflictDetectionResult()

    data class Error(
        val message: String
    ) : ConflictDetectionResult()
}

/**
 * 认知冲突详情
 */
data class CognitiveConflict(
    /**
     * 新内容中的陈述
     */
    val newStatement: String,

    /**
     * 语料库中的相关内容
     */
    val corpusContent: String,

    /**
     * 语料库条目的ID
     */
    val corpusSourceId: Long,

    /**
     * 语料库条目的标题
     */
    val corpusTitle: String,

    /**
     * 相似度分数（0-1）
     */
    val similarityScore: Float,

    /**
     * 冲突类型
     */
    val conflictType: ConflictType,

    /**
     * 处理建议
     */
    val suggestion: String
) {
    /**
     * 是否为高置信度冲突（相似度>0.85）
     */
    val isHighConfidence: Boolean
        get() = similarityScore >= 0.85f

    /**
     * 简洁的冲突描述
     */
    fun shortDescription(): String {
        return "当前陈述与\"$corpusTitle\"存在${getConflictTypeName()}（相似度: ${String.format("%.1f%%", similarityScore * 100)}）"
    }

    private fun getConflictTypeName(): String {
        return when (conflictType) {
            ConflictType.FACTUAL -> "事实冲突"
            ConflictType.OPINION -> "观点冲突"
            ConflictType.TEMPORAL -> "时间冲突"
            ConflictType.LOGICAL -> "逻辑冲突"
        }
    }
}

/**
 * UI展示用的冲突信息
 */
data class ConflictDisplayInfo(
    val conflictCount: Int,
    val warningLevel: WarningLevel,
    val primaryConflict: CognitiveConflict?,
    val allConflicts: List<CognitiveConflict>
) {
    enum class WarningLevel {
        NONE,       // 无冲突
        LOW,        // 低度冲突（1-2个）
        MEDIUM,     // 中度冲突（3-4个）
        HIGH        // 高度冲突（5个及以上）
    }

    companion object {
        fun fromConflicts(conflicts: List<CognitiveConflict>): ConflictDisplayInfo {
            return if (conflicts.isEmpty()) {
                ConflictDisplayInfo(
                    conflictCount = 0,
                    warningLevel = WarningLevel.NONE,
                    primaryConflict = null,
                    allConflicts = emptyList()
                )
            } else {
                ConflictDisplayInfo(
                    conflictCount = conflicts.size,
                    warningLevel = when {
                        conflicts.size >= 5 -> WarningLevel.HIGH
                        conflicts.size >= 3 -> WarningLevel.MEDIUM
                        else -> WarningLevel.LOW
                    },
                    primaryConflict = conflicts.maxByOrNull { it.similarityScore },
                    allConflicts = conflicts
                )
            }
        }
    }
}
