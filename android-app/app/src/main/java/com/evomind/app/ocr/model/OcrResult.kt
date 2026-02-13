package com.evomind.app.ocr.model

/**
 * OCR识别结果数据模型
 */
data class OcrResult(
    /**
     * 原始识别文本（未清洗）
     */
    val originalText: String,

    /**
     * 清洗后的文本
     */
    val cleanedText: String,

    /**
     * 素材来源平台
     */
    val platform: PlatformType,

    /**
     * 平均置信度（0-1）
     */
    val confidence: Float,

    /**
     * 图片文件路径（如有）
     */
    val imagePath: String? = null,

    /**
     * 识别的单词/短语列表
     */
    val wordList: List<WordResult> = emptyList(),

    /**
     * 识别时间戳
     */
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 单词识别结果
     */
    data class WordResult(
        val text: String,
        val confidence: Float,
        val location: Location? = null
    )

    /**
     * 位置信息
     */
    data class Location(
        val left: Int,
        val top: Int,
        val width: Int,
        val height: Int
    )
}
