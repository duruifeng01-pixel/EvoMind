package com.evomind.app.scraper

import android.content.Context
import android.util.Log
import com.evomind.app.scraper.model.ScrapedContent
import com.evomind.app.scraper.model.ScrapingResult
import com.evomind.app.scraper.model.ScrapingResult.*
import com.evomind.app.scraper.model.ScrapingResult.ScrapingStatus.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * 网页爬虫服务
 * 支持微信公众号、知乎、小红书等平台的文章爬取
 */
class WebScraperService(private val context: Context) {

    companion object {
        private const val TAG = "WebScraperService"

        // 请求配置
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 60L
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 2000L

        // User-Agent池（轮换使用）
        private val USER_AGENTS = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1"
        )

        // 各平台的特征选择器
        private val PLATFORM_SELECTORS = mapOf(
            "zhihu" to mapOf(
                "title" to "h1.Post-Title",
                "author" to "div.AuthorInfo-head span.UserLink-link",
                "content" to "div.Post-RichTextContainer",
                "description" to "meta[name=description]"
            ),
            "wechat" to mapOf(
                "title" to "h1#activity-name",
                "author" to "strong.profile_nickname",
                "content" to "div.rich_media_content",
                "publishTime" to "script[nonce]"
            ),
            "xiaohongshu" to mapOf(
                "title" to "h1#detail-title",
                "author" to "div.author-container span.name",
                "content" to "div#detail-desc",
                "likes" to "div.like-info"
            ),
            "jianshu" to mapOf(
                "title" to "h1._1RuRku",
                "author" to "span._22gUMi",
                "content" to "article._2rhmJa",
                "wordCount" to "span.word-count"
            )
        )
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val gson = com.google.gson.Gson()

    /**
     * 爬取文章主入口
     *
     * @param url 文章URL
     * @param platform 平台类型（zhihu, wechat, xiaohongshu, jianshu等）
     * @param options 额外选项（如是否爬取图片、超时时间等）
     */
    suspend fun scrapeArticle(
        url: String,
        platform: String? = null,
        options: ScrapingOptions = ScrapingOptions()
    ): Result<ScrapedContent> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始爬取文章: $url, 平台: ${platform ?: "自动识别"}")

            // 验证URL
            if (!isValidUrl(url)) {
                return@withContext Result.failure(
                    IOException("无效URL: $url")
                )
            }

            // 检测平台
            val detectedPlatform = platform ?: detectPlatform(url)
            Log.d(TAG, "检测到平台: $detectedPlatform")

            // 发送请求
            val response = sendRequest(url, detectedPlatform)

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("请求失败: ${response.code} ${response.message}")
                )
            }

            // 解析HTML
            val html = response.body?.string() ?: ""
            if (html.isEmpty()) {
                return@withContext Result.failure(
                    IOException("响应内容为空")
                )
            }

            // 提取内容
            val scrapedContent = extractContent(html, detectedPlatform, url)

            // 应用额外的处理
            val processedContent = processContent(scrapedContent, options)

            Log.i(TAG, "文章爬取成功: ${processedContent.title}")
            Log.d(TAG, "内容长度: ${processedContent.content.length}")
            Log.d(TAG, "图片数量: ${processedContent.images.size}")

            Result.success(processedContent)

        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "请求超时: ${e.message}")
            Result.failure(IOException("请求超时，请重试", e))
        } catch (e: IOException) {
            Log.e(TAG, "IO错误: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "未知错误: ${e.message}", e)
            Result.failure(Exception("爬取失败: ${e.message}", e))
        }
    }

    /**
     * 批量爬取文章
     */
    suspend fun batchScrape(
        urls: List<String>,
        options: ScrapingOptions = ScrapingOptions()
    ): BatchScrapingResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScrapingResult<ScrapedContent>>()
        val failures = mutableListOf<Pair<String, String>>()

        urls.forEachIndexed { index, url ->
            try {
                Log.d(TAG, "批量爬取 ${index + 1}/${urls.size}: $url")

                val result = scrapeArticle(url, options = options)
                result.onSuccess { content ->
                    results.add(ScrapingResult.Success(content))
                }.onFailure { exception ->
                    results.add(ScrapingResult.Failure(exception.message ?: "未知错误"))
                    failures.add(url to (exception.message ?: "未知错误"))
                }

                // 添加延迟，避免请求过快
                delay(options.delayBetweenRequests)

            } catch (e: Exception) {
                results.add(ScrapingResult.Failure(e.message ?: "未知错误"))
                failures.add(url to (e.message ?: "未知错误"))
            }
        }

        BatchScrapingResult(
            results = results,
            successCount = results.count { it is ScrapingResult.Success },
            failureCount = results.count { it is ScrapingResult.Failure },
            failures = failures
        )
    }

    /**
     * 发送HTTP请求
     */
    private suspend fun sendRequest(url: String, platform: String): Response {
        var lastException: Exception? = null

        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                if (attempt > 0) {
                    Log.d(TAG, "重试请求: ${attempt + 1}/$MAX_RETRY_COUNT")
                    delay(RETRY_DELAY_MS * attempt)
                }

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENTS.random())
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .apply {
                        // 平台特定的headers
                        when (platform) {
                            "zhihu" -> {
                                header("Referer", "https://www.zhihu.com/")
                            }
                            "wechat" -> {
                                header("Referer", "https://mp.weixin.qq.com/")
                            }
                        }
                    }
                    .build()

                return client.newCall(request).execute()

            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "请求失败 (尝试 ${attempt + 1}/$MAX_RETRY_COUNT): ${e.message}")
            }
        }

        throw lastException ?: IOException("所有重试都失败了")
    }

    /**
     * 从HTML提取内容
     */
    private fun extractContent(
        html: String,
        platform: String,
        url: String
    ): ScrapedContent {
        val doc = Jsoup.parse(html, url)
        val selectors = PLATFORM_SELECTORS[platform] ?: PLATFORM_SELECTORS["jianshu"]!!

        return when (platform) {
            "zhihu" -> scrapeZhihu(doc, selectors)
            "wechat" -> scrapeWechat(doc, selectors)
            "xiaohongshu" -> scrapeXiaohongshu(doc, selectors)
            else -> scrapeGeneric(doc, selectors)
        }
    }

    /**
     * 爬取知乎文章
     */
    private fun scrapeZhihu(doc: Document, selectors: Map<String, String>): ScrapedContent {
        val title = doc.select(selectors["title"]).text().trim()
        val author = doc.select(selectors["author"]).text().trim()
        val content = doc.select(selectors["content"]).html()
        val description = doc.select(selectors["description"]).attr("content")

        // 提取赞同数、评论数等
        val voteCount = doc.select("span.Voters button").text().trim()
        val commentCount = doc.select("meta[itemprop=commentCount]").attr("content")

        return ScrapedContent(
            title = title,
            author = author,
            content = cleanHtmlContent(content, platform = "zhihu"),
            description = description,
            publishTime = System.currentTimeMillis(),
            images = extractImages(doc.select(selectors["content"])),
            metadata = mapOf(
                "platform" to "zhihu",
                "voteCount" to voteCount,
                "commentCount" to commentCount
            )
        )
    }

    /**
     * 爬取微信文章
     */
    private fun scrapeWechat(doc: Document, selectors: Map<String, String>): ScrapedContent {
        val title = doc.select(selectors["title"]).text().trim()
        val author = doc.select(selectors["author"]).text().trim()
        val content = doc.select(selectors["content"]).html()

        // 从script中提取发布时间
        val scripts = doc.select(selectors["publishTime"] ?: "script")
        var publishTime = System.currentTimeMillis()

        for (script in scripts) {
            val scriptContent = script.html()
            if (scriptContent.contains("publish_time")) {
                // 简化的提取逻辑
                val timeMatch = Regex("\"publish_time\":\"([^\"]+)\"")
                    .find(scriptContent)
                if (timeMatch != null) {
                    publishTime = parseWechatTime(timeMatch.groupValues[1])
                }
            }
        }

        // 提取阅读数、点赞数（如果有）
        val readCount = doc.select("span.read-count").text().trim()
        val likeCount = doc.select("span.like-count").text().trim()

        return ScrapedContent(
            title = title,
            author = author,
            content = cleanHtmlContent(content, platform = "wechat"),
            description = doc.select("meta[name=description]").attr("content"),
            publishTime = publishTime,
            images = extractImages(doc.select(selectors["content"])),
            metadata = mapOf(
                "platform" to "wechat",
                "readCount" to readCount,
                "likeCount" to likeCount
            )
        )
    }

    /**
     * 爬取小红书笔记
     */
    private fun scrapeXiaohongshu(doc: Document, selectors: Map<String, String>): ScrapedContent {
        val title = doc.select(selectors["title"]).text().trim()
        val author = doc.select(selectors["author"]).text().trim()
        val content = doc.select(selectors["content"]).html()

        // 提取点赞、收藏、评论数
        val likeInfo = doc.select(selectors["likes"] ?: "div.like-info").text().trim()

        return ScrapedContent(
            title = title,
            author = author,
            content = cleanHtmlContent(content, platform = "xiaohongshu"),
            description = doc.select("meta[name=description]").attr("content"),
            publishTime = System.currentTimeMillis(),
            images = extractImages(doc.select(selectors["content"])),
            metadata = mapOf(
                "platform" to "xiaohongshu",
                "likeInfo" to likeInfo
            )
        )
    }

    /**
     * 通用爬虫
     */
    private fun scrapeGeneric(doc: Document, selectors: Map<String, String>): ScrapedContent {
        val title = doc.title().trim()
        val author = doc.select("meta[name=author]").attr("content")

        // 尝试找到主要内容区域
        val contentSelectors = listOf(
            "article",
            "div.content",
            "div.post-content",
            "div.entry-content",
            "main",
            "body"
        )

        var content = ""
        for (selector in contentSelectors) {
            val elements = doc.select(selector)
            if (elements.isNotEmpty() && elements.text().length > 100) {
                content = elements.html()
                break
            }
        }

        return ScrapedContent(
            title = title,
            author = author,
            content = cleanHtmlContent(content),
            description = doc.select("meta[name=description]").attr("content"),
            publishTime = System.currentTimeMillis(),
            images = extractImages(doc),
            metadata = mapOf("platform" to "generic")
        )
    }

    /**
     * 清理HTML内容
     */
    private fun cleanHtmlContent(html: String, platform: String = ""): String {
        val doc = Jsoup.parseBodyFragment(html)

        // 移除不需要的元素
        val removeSelectors = listOf(
            "script", "style", "nav", "header", "footer",
            "aside", ".advertisement", ".ad", "#advertisement"
        )

        removeSelectors.forEach { selector ->
            doc.select(selector).remove()
        }

        // 特定平台的清理
        when (platform) {
            "wechat" -> {
                // 移除微信特定的分享按钮等
                doc.select("share_toolbox, qr_code").remove()
            }
            "zhihu" -> {
                // 移除知乎的赞同按钮等
                doc.select(".Voter, .RichContent-actions").remove()
            }
        }

        // 提取文本
        val text = doc.text()

        // 图片保留为占位符
        val images = doc.select("img")
        var imageIndex = 0
        val processedText = text.replace(Regex("\\[图片\\]|image")) {
            if (images.size > imageIndex) {
                val src = images[imageIndex].attr("src")
                imageIndex++
                "\n[图片: $src]\n"
            } else {
                "\n[图片]\n"
            }
        }

        return processedText.trim()
    }

    /**
     * 提取图片
     */
    private fun extractImages(elements: org.jsoup.nodes.Elements): List<String> {
        return elements.select("img").map { img ->
            var src = img.attr("src")

            // 处理data-src属性（懒加载图片）
            if (src.isBlank()) {
                src = img.attr("data-src")
            }

            // 处理相对路径
            if (src.isNotBlank() && !src.startsWith("http")) {
                src = makeAbsoluteUrl(src, elements.baseUri())
            }

            src
        }.filter { it.isNotBlank() }
    }

    private fun extractImages(doc: Document): List<String> {
        return extractImages(doc.select("img"))
    }

    /**
     * 处理HTML内容（进一步处理）
     */
    private fun processContent(content: ScrapedContent, options: ScrapingOptions): ScrapedContent {
        var processed = content

        // 长度限制
        if (options.maxContentLength > 0 && processed.content.length > options.maxContentLength) {
            processed = processed.copy(
                content = processed.content.substring(0, options.maxContentLength) + "..."
            )
        }

        // 图片数量限制
        if (options.maxImages > 0 && processed.images.size > options.maxImages) {
            processed = processed.copy(
                images = processed.images.take(options.maxImages)
            )
        }

        return processed
    }

    /**
     * 检测平台类型
     */
    private fun detectPlatform(url: String): String {
        return when {
            url.contains("zhihu.com") -> "zhihu"
            url.contains("mp.weixin.qq.com") -> "wechat"
            url.contains("xiaohongshu.com") -> "xiaohongshu"
            url.contains("jianshu.com") -> "jianshu"
            url.contains("weibo.com") -> "weibo"
            else -> "generic"
        }
    }

    /**
     * 验证URL
     */
    private fun isValidUrl(url: String): Boolean {
        return try {
            url.toHttpUrl()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 将相对URL转换为绝对URL
     */
    private fun makeAbsoluteUrl(relativeUrl: String, baseUrl: String): String {
        return try {
            val base = okhttp3.HttpUrl.parse(baseUrl) ?: return relativeUrl
            base.newBuilder().addPathSegment(relativeUrl).build().toString()
        } catch (e: Exception) {
            relativeUrl
        }
    }

    /**
     * 解析微信发布时间
     */
    private fun parseWechatTime(timeStr: String): Long {
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.CHINA)
            format.parse(timeStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    /**
     * 获取平台名称
     */
    fun getPlatformName(platform: String): String {
        return when (platform) {
            "zhihu" -> "知乎"
            "wechat" -> "微信公众号"
            "xiaohongshu" -> "小红书"
            "jianshu" -> "简书"
            "weibo" -> "微博"
            else -> "未知平台"
        }
    }
}

/**
 * 爬虫配置选项
 */
data class ScrapingOptions(
    // 最大内容长度（字符数），0表示不限制
    val maxContentLength: Int = 20000,

    // 最大图片数量，0表示不限制
    val maxImages: Int = 10,

    // 请求之间的延迟（毫秒）
    val delayBetweenRequests: Long = 1000,

    // 是否爬取图片
    val fetchImages: Boolean = true,

    // 是否爬取元数据
    val fetchMetadata: Boolean = true,

    // 超时时间（秒）
    val timeoutSeconds: Long = 60
)

sealed class ScrapingResult<T> {
    data class Success<T>(val data: T) : ScrapingResult<T>()
    data class Failure<T>(val error: String) : ScrapingResult<T>()

    enum class ScrapingStatus {
        SUCCESS,
        FAILED,
        TIMEOUT,
        NETWORK_ERROR,
        PARSE_ERROR
    }
}

data class BatchScrapingResult(
    val results: List<ScrapingResult<ScrapedContent>>,
    val successCount: Int,
    val failureCount: Int,
    val failures: List<Pair<String, String>> // URL to error message
)