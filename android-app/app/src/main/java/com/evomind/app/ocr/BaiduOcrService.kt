package com.evomind.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.baidu.aip.ocr.AipOcr
import com.evomind.app.ocr.model.OcrResult
import com.evomind.app.ocr.model.PlatformType
import com.evomind.app.ocr.model.OcrResult.WordResult
import com.evomind.app.ocr.model.OcrResult.Location
import com.evomind.app.ocr.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.SocketTimeoutException
import kotlin.math.round

/**
 * 百度OCR服务类
 * 封装百度OCR SDK的调用逻辑
 */
class BaiduOcrService(
    private val context: Context
) {

    companion object {
        private const val TAG = "BaiduOcrService"

        // 百度AI访问令牌配置
        // 方式1：使用Access Token（一次性配置，有效期30天）
        private const val ACCESS_TOKEN = "1cab6b4943645bf247c3ad99d7aa15e28f59a6ff"

        // 方式2：使用API Key和Secret Key（推荐，自动获取Access Token）
        // - 创建百度AI账号：https://ai.baidu.com/
        // - 创建应用获得APP_ID, API_KEY, SECRET_KEY
        // - 填入下方后注释掉ACCESS_TOKEN方式
        // private const val APP_ID = "YOUR_APP_ID"
        // private const val API_KEY = "YOUR_API_KEY"
        // private const val SECRET_KEY = "YOUR_SECRET_KEY"

        // OCR识别类型
        private const val TYPE_GENERAL_BASIC = "general_basic"           // 通用文字识别
        private const val TYPE_ACCURATE_BASIC = "accurate_basic"         // 高精度通用识别
        private const val TYPE_WEB_IMAGE = "webimage"                   // 网络图片识别

        // 重试配置
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    private var aipOcr: AipOcr? = null
    private var retryCount = 0

    init {
        initializeOcr()
    }

    /**
     * 初始化OCR客户端
     */
    private fun initializeOcr() {
        try {
            // 使用Access Token方式（您当前使用的方式）
            aipOcr = AipOcr().apply {
                setAccessToken(ACCESS_TOKEN)
                // 设置连接超时（20秒）
                setConnectionTimeoutInMillis(20000)
                // 设置socket超时（60秒）
                setSocketTimeoutInMillis(60000)
            }

            Log.i(TAG, "百度OCR服务初始化成功（使用Access Token）")
            Log.i(TAG, "Access Token: ${ACCESS_TOKEN.take(16)}...（有效期30天）")

            // 注意：Access Token有效期为30天
            // 到期后需要重新申请，或者使用API Key方式（推荐）
            Log.w(TAG, "⚠️ 当前使用Access Token方式，有效期30天，请提前续约")

        } catch (e: Exception) {
            Log.e(TAG, "百度OCR服务初始化失败: ${e.message}", e)
        }
    }

    /**
     * 识别图片中的文字（主入口）
     *
     * @param imagePath 图片文件路径
     * @param platformType 素材来源平台，用于优化识别策略
     * @param useHighAccuracy 是否使用高精度识别
     * @return OCR识别结果
     */
    suspend fun recognizeText(
        imagePath: String,
        platformType: PlatformType = PlatformType.GENERAL,
        useHighAccuracy: Boolean = false
    ): Result<OcrResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始识别图片: $imagePath, 平台: $platformType, 高精度: $useHighAccuracy")

            // 1. 检查OCR客户端是否初始化
            if (aipOcr == null) {
                return@withContext Result.failure(Exception("OCR服务未初始化"))
            }

            // 2. 加载并预处理图片
            val bitmap = ImageUtils.loadAndProcessImage(context, imagePath)
                ?: return@withContext Result.failure(Exception("图片加载失败"))

            Log.d(TAG, "图片加载成功: ${bitmap.width}x${bitmap.height}")

            // 3. 根据平台类型裁剪图片
            val croppedBitmap = cropImageForPlatform(bitmap, platformType)

            // 4. 调用百度OCR API
            val jsonResult = callBaiduOcr(croppedBitmap, useHighAccuracy)
                ?: return@withContext Result.failure(Exception("OCR识别失败，返回结果为空"))

            // 5. 解析结果
            val ocrResult = parseOcrResult(jsonResult, platformType, imagePath)

            Log.i(TAG, "OCR识别成功，识别到 ${ocrResult.wordList.size} 个字符块")
            Log.d(TAG, "清洗后的文本：${ocrResult.cleanedText.take(100)}...")

            Result.success(ocrResult)

        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "OCR请求超时: ${e.message}")
            handleRetry(imagePath, platformType, useHighAccuracy, e)

        } catch (e: Exception) {
            Log.e(TAG, "OCR识别失败: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 根据平台类型裁剪图片
     */
    private fun cropImageForPlatform(bitmap: Bitmap, platformType: PlatformType): Bitmap {
        return when (platformType) {
            PlatformType.WECHAT -> {
                Log.d(TAG, "裁剪微信聊天区域")
                ImageUtils.cropWechatChatArea(bitmap)
            }
            PlatformType.XIAOHONGSHU -> {
                Log.d(TAG, "裁剪小红书内容区域")
                ImageUtils.cropXiaohongshuContent(bitmap)
            }
            PlatformType.ZHIHU -> {
                Log.d(TAG, "裁剪知乎回答区域")
                ImageUtils.cropZhihuAnswer(bitmap)
            }
            PlatformType.WEIBO -> {
                Log.d(TAG, "裁剪微博内容区域")
                ImageUtils.cropWeiboContent(bitmap)
            }
            else -> {
                Log.d(TAG, "使用完整图片")
                bitmap
            }
        }
    }

    /**
     * 调用百度OCR API
     */
    private fun callBaiduOcr(bitmap: Bitmap, useHighAccuracy: Boolean): JSONObject? {
        return try {
            val imageBytes = bitmapToByteArray(bitmap)
            val options = mutableMapOf<String, String?>().apply {
                put("detect_direction", "true")   // 检测图片方向
                put("detect_language", "true")    // 检测语言
                put("probability", "true")        // 返回置信度
            }

            val result = if (useHighAccuracy) {
                Log.d(TAG, "使用高精度识别模式")
                aipOcr?.accurateBasic(imageBytes, options)
            } else {
                Log.d(TAG, "使用标准识别模式")
                aipOcr?.generalBasic(imageBytes, options)
            }

            result?.let {
                Log.d(TAG, "OCR API返回: ${it.toString().take(200)}...")
            }

            result

        } catch (e: Exception) {
            Log.e(TAG, "调用OCR API失败: ${e.message}", e)
            null
        } finally {
            // 清理bitmap
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    /**
     * 解析OCR结果
     */
    private fun parseOcrResult(
        jsonResult: JSONObject,
        platformType: PlatformType,
        imagePath: String
    ): OcrResult {
        try {
            // 解析words_result数组
            val wordsResult = jsonResult.optJSONArray("words_result")
            val wordList = mutableListOf<WordResult>()
            val stringBuilder = StringBuilder()

            if (wordsResult != null && wordsResult.length() > 0) {
                for (i in 0 until wordsResult.length()) {
                    val wordObj = wordsResult.getJSONObject(i)

                    // 提取文字
                    val word = wordObj.optString("words", "")
                    if (word.isNotEmpty()) {
                        stringBuilder.append(word).append("\n")

                        // 提取置信度和位置
                        val probability = wordObj.optJSONObject("probability")
                        val confidence = probability?.optDouble("average", 0.0)?.toFloat() ?: 0f

                        val locationObj = wordObj.optJSONObject("location")
                        val location = locationObj?.let {
                            Location(
                                left = it.optInt("left", 0),
                                top = it.optInt("top", 0),
                                width = it.optInt("width", 0),
                                height = it.optInt("height", 0)
                            )
                        }

                        wordList.add(
                            WordResult(
                                text = word,
                                confidence = confidence,
                                location = location
                            )
                        )
                    }
                }
            }

            val originalText = stringBuilder.toString().trim()

            // 根据平台类型清洗文本
            val cleanedText = cleanTextByPlatform(originalText, platformType)

            // 计算平均置信度
            val averageConfidence = calculateAverageConfidence(wordList)

            Log.d(TAG, "原始文本长度: ${originalText.length}, 清洗后: ${cleanedText.length}")
            Log.d(TAG, "平均置信度: ${"%.2f".format(averageConfidence)}")

            return OcrResult(
                originalText = originalText,
                cleanedText = cleanedText,
                platform = platformType,
                confidence = averageConfidence,
                imagePath = imagePath,
                wordList = wordList
            )

        } catch (e: Exception) {
            Log.e(TAG, "解析OCR结果失败: ${e.message}", e)

            // 返回一个包含原始文本的结果
            return OcrResult(
                originalText = "",
                cleanedText = "",
                platform = platformType,
                confidence = 0f,
                imagePath = imagePath,
                wordList = emptyList()
            )
        }
    }

    /**
     * 根据平台类型清洗文本
     */
    private fun cleanTextByPlatform(text: String, platform: PlatformType): String {
        if (text.isEmpty()) return ""

        return when (platform) {
            PlatformType.WECHAT -> cleanWechatText(text)
            PlatformType.XIAOHONGSHU -> cleanXiaohongshuText(text)
            PlatformType.ZHIHU -> cleanZhihuText(text)
            PlatformType.WEIBO -> cleanWeiboText(text)
            else -> text
        }
    }

    /**
     * 清洗微信文本
     * 去除：时间戳、昵称、免打扰标记等
     */
    private fun cleanWechatText(text: String): String {
        val lines = text.split("\n")
        val filteredLines = lines.filter { line ->
            // 过滤时间戳 (HH:MM)
            !line.matches(Regex("^\\d{1,2}:\\d{2}$")) &&
            // 过滤昵称 + 时间戳
            !line.matches(Regex("^.+?\\s+\\d{1,2}:\\d{2}$")) &&
            // 过滤免打扰、置顶等标记
            !line.contains("免打扰") &&
            !line.contains("置顶") &&
            !line.contains("消息免打扰") &&
            // 过滤撤回消息
            !line.contains("撤回了一条消息") &&
            // 过滤系统提示
            !line.matches(Regex("^\\[.+?\\]$"))
        }

        // 移除空行和重复行
        return filteredLines
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n")
            .trim()
    }

    /**
     * 清洗小红书文本
     * 去除：标签、位置信息、发布时间等
     */
    private fun cleanXiaohongshuText(text: String): String {
        val lines = text.split("\n")
        val filteredLines = lines.filter { line ->
            // 过滤标签 (#xxx)
            !line.matches(Regex("^#\\S+$")) &&
            // 过滤位置 (@xxx)
            !line.matches(Regex("^@\\S+$")) &&
            // 过滤发布时间
            !line.contains("发布于") &&
            // 过滤收藏、点赞数
            !line.matches(Regex("^\\d+\\s*(收藏|点赞|评论)$"))
        }

        // 保留正文内容
        return filteredLines
            .joinToString("\n")
            .trim()
    }

    /**
     * 清洗知乎文本
     * 去除：赞同数、评论数、用户信息等
     */
    private fun cleanZhihuText(text: String): String {
        val lines = text.split("\n")
        val filteredLines = lines.filter { line ->
            // 过滤赞同数
            !line.matches(Regex("^\\d+\\s*赞同$")) &&
            // 过滤评论数
            !line.matches(Regex("^\\d+\\s*评论$")) &&
            // 过滤喜欢数
            !line.matches(Regex("^\\d+\\s*喜欢$")) &&
            // 过滤用户信息
            !line.contains("实名用户") &&
            // 过滤"赞同了回答"
            !line.matches(Regex("^.+?赞同了回答$"))
        }

        return filteredLines
            .joinToString("\n")
            .trim()
    }

    /**
     * 清洗微博文本
     * 去除：@用户名、话题标签、位置等
     */
    private fun cleanWeiboText(text: String): String {
        // 1. 移除@用户名
        var cleaned = text.replace(Regex("@[\\u4e00-\\u9fa5a-zA-Z0-9_]+"), "")

        // 2. 移除话题标签 (#xxx#)
        cleaned = cleaned.replace(Regex("#[^#]+#"), "")

        // 3. 移除位置信息
        cleaned = cleaned.replace(Regex("来自 .+"), "")
        cleaned = cleaned.replace(Regex("L .+"), "")

        // 4. 移除微博特有的标识
        cleaned = cleaned.replace("转发微博", "")
        cleaned = cleaned.replace("展开全文", "")

        // 5. 移除多余的空行
        cleaned = cleaned.split("\n")
            .filter { it.isNotBlank() }
            .joinToString("\n")

        return cleaned.trim()
    }

    /**
     * 计算平均置信度
     */
    private fun calculateAverageConfidence(wordList: List<WordResult>): Float {
        if (wordList.isEmpty()) return 0f

        val totalConfidence = wordList.sumOf { it.confidence.toDouble() }
        return (totalConfidence / wordList.size).toFloat()
    }

    /**
     * Bitmap转为ByteArray
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        return ByteArrayOutputStream().use { stream ->
            // 使用JPEG格式，质量85%
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            stream.toByteArray()
        }
    }

    /**
     * 处理重试逻辑
     */
    private suspend fun handleRetry(
        imagePath: String,
        platformType: PlatformType,
        useHighAccuracy: Boolean,
        exception: Exception
    ): Result<OcrResult> {
        return if (retryCount < MAX_RETRY_COUNT) {
            retryCount++
            Log.w(TAG, "OCR请求失败，${RETRY_DELAY_MS}ms后进行第${retryCount}次重试...")
            kotlinx.coroutines.delay(RETRY_DELAY_MS * retryCount)

            // 递归重试
            recognizeText(imagePath, platformType, useHighAccuracy)
        } else {
            Log.e(TAG, "OCR请求失败，已达到最大重试次数($MAX_RETRY_COUNT)")
            retryCount = 0
            Result.failure(exception)
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        aipOcr = null
        retryCount = 0
        Log.i(TAG, "百度OCR服务资源已释放")
    }

    /**
     * 获取服务状态
     */
    fun getServiceStatus(): String {
        return when {
            aipOcr == null -> "未初始化"
            retryCount > 0 -> "重试中($retryCount/$MAX_RETRY_COUNT)"
            else -> "正常运行"
        }
    }
}
