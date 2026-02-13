package com.evomind.app.ocr

import com.evomind.app.database.dao.SourceDao
import com.evomind.app.database.entity.SourceEntity
import com.evomind.app.ocr.model.OcrResult
import com.evomind.app.ocr.model.PlatformType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

/**
 * OCR数据仓库
 * 管理OCR识别和素材存储的业务逻辑
 */
class OcrRepository(
    private val context: android.content.Context,
    private val sourceDao: SourceDao
) {
    private val ocrService = BaiduOcrService(context)

    /**
     * 识别图片并保存到数据库
     *
     * @param imagePath 图片路径
     * @param platformType 平台类型
     * @param title 自定义标题（可选）
     * @param useHighAccuracy 是否使用高精度识别
     * @return 保存的素材实体
     */
    fun processImage(
        imagePath: String,
        platformType: PlatformType,
        title: String? = null,
        useHighAccuracy: Boolean = false
    ): Flow<Result<SourceEntity>> = flow {
        // 1. 执行OCR识别
        val ocrResult = ocrService.recognizeText(
            imagePath = imagePath,
            platformType = platformType,
            useHighAccuracy = useHighAccuracy
        )

        // 2. 处理结果
        ocrResult.onSuccess { result ->
            // 创建素材实体
            val source = SourceEntity.fromOcrResult(result, title)

            // 保存到数据库
            val id = sourceDao.insert(source)
            val savedSource = source.copy(id = id)

            emit(Result.success(savedSource))

        }.onFailure { exception ->
            emit(Result.failure(exception))
        }

    }.catch { e ->
        emit(Result.failure(Exception("OCR处理失败: ${e.message}", e)))
    }.onStart {
        emit(Result.failure(Exception("开始OCR识别...")))
    }

    /**
     * 批量处理多张图片
     */
    fun processMultipleImages(
        imagePaths: List<String>,
        platformType: PlatformType
    ): Flow<Result<List<SourceEntity>>> = flow {
        val results = mutableListOf<SourceEntity>()
        var successCount = 0
        var failureCount = 0

        for (imagePath in imagePaths) {
            try {
                val result = ocrService.recognizeText(imagePath, platformType)
                result.onSuccess { ocrResult ->
                    val source = SourceEntity.fromOcrResult(ocrResult)
                    val id = sourceDao.insert(source)
                    results.add(source.copy(id = id))
                    successCount++
                }.onFailure { exception ->
                    Log.e("OcrRepository", "图片处理失败: $imagePath, ${exception.message}")
                    failureCount++
                }
            } catch (e: Exception) {
                Log.e("OcrRepository", "处理异常: $imagePath, ${e.message}")
                failureCount++
            }
        }

        if (results.isNotEmpty()) {
            emit(Result.success(results))
        } else {
            emit(Result.failure(Exception("所有图片处理失败")))
        }

    }.catch { e ->
        emit(Result.failure(Exception("批量OCR处理失败: ${e.message}", e)))
    }

    /**
     * 获取所有素材
     */
    fun getAllSources(): Flow<List<SourceEntity>> = sourceDao.getAll()

    /**
     * 根据ID获取素材
     */
    fun getSourceById(id: Long): Flow<SourceEntity?> = sourceDao.getById(id)

    /**
     * 根据平台获取素材
     */
    fun getSourcesByPlatform(platformType: PlatformType): Flow<List<SourceEntity>> {
        return sourceDao.getByPlatform(platformType.name)
    }

    /**
     * 搜索素材
     */
    fun searchSources(query: String): Flow<List<SourceEntity>> = sourceDao.searchByTitle(query)

    /**
     * 根据标签查询素材
     */
    fun getSourcesByTag(tag: String): Flow<List<SourceEntity>> = sourceDao.getByTag(tag)

    /**
     * 删除素材
     */
    suspend fun deleteSource(source: SourceEntity): Result<Unit> {
        return try {
            sourceDao.delete(source)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("删除素材失败: ${e.message}", e))
        }
    }

    /**
     * 更新素材
     */
    suspend fun updateSource(source: SourceEntity): Result<Unit> {
        return try {
            sourceDao.update(source.copy(updatedAt = System.currentTimeMillis()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("更新素材失败: ${e.message}", e))
        }
    }

    /**
     * 为素材添加标签
     */
    suspend fun addTagToSource(sourceId: Long, tag: String): Result<Unit> {
        return try {
            sourceDao.getById(sourceId).collect { source ->
                source?.let {
                    val updatedSource = it.addTag(tag)
                    sourceDao.update(updatedSource)
                    Result.success(Unit)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("添加标签失败: ${e.message}", e))
        }
    }

    /**
     * 获取素材统计信息
     */
    suspend fun getStatistics(): Result<SourceStatistics> {
        return try {
            val totalCount = sourceDao.getCount()
            val platformStats = sourceDao.getCountByPlatform()

            val stats = SourceStatistics(
                totalCount = totalCount,
                platformCount = platformStats.associateBy(
                    { it.platform },
                    { it.count }
                )
            )

            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(Exception("获取统计信息失败: ${e.message}", e))
        }
    }

    /**
     * 清理低置信度素材（通常是识别失败的）
     */
    suspend fun cleanupLowConfidenceSources(minConfidence: Float = 0.5f): Result<Int> {
        return try {
            val count = sourceDao.deleteLowConfidence(minConfidence)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(Exception("清理素材失败: ${e.message}", e))
        }
    }

    /**
     * 获取OCR服务状态（用于调试）
     */
    fun getOcrServiceStatus(): String {
        return ocrService.getServiceStatus()
    }

    /**
     * 释放OCR服务资源</p>
     */
    fun release() {
        ocrService.release()
    }

    /**
     * 统计信息数据类
     */
    data class SourceStatistics(
        val totalCount: Int,
        val platformCount: Map<String, Int>
    ) {
        /**
         * 获取某个平台的素材数量
         */
        fun getPlatformCount(platformType: PlatformType): Int {
            return platformCount[platformType.name] ?: 0
        }
    }

    /**
     * 处理状态
     */
    sealed interface ProcessingState {
        object Idle : ProcessingState
        data class Processing(val progress: Float = 0f) : ProcessingState
        data class Success(val source: SourceEntity) : ProcessingState
        data class BatchSuccess(val sources: List<SourceEntity>) : ProcessingState
        data class Error(val message: String) : ProcessingState
    }
}