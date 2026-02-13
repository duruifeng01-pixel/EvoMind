package com.evomind.app.ocr.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * 图片处理工具类
 * 提供图片加载、压缩、裁剪等功能
 */
object ImageUtils {

    private const val TAG = "ImageUtils"

    // 最大图片尺寸（像素）
    private const val MAX_IMAGE_SIZE = 2048

    // 目标文件大小（字节）
    private const val TARGET_IMAGE_SIZE = 1024 * 1024 // 1MB

    // 压缩质量
    private const val COMPRESS_QUALITY = 85

    // 微信聊天截图的裁剪比例
    private const val WECHAT_TOP_CROP_RATIO = 0.10f
    private const val WECHAT_BOTTOM_CROP_RATIO = 0.85f

    // 小红书内容裁剪比例
    private const val XIAOHONGSHU_TOP_CROP_RATIO = 0.15f
    private const val XIAOHONGSHU_BOTTOM_CROP_RATIO = 0.80f

    // 知乎回答裁剪比例
    private const val ZHIHU_TOP_CROP_RATIO = 0.12f
    private const val ZHIHU_BOTTOM_CROP_RATIO = 0.90f

    // 微博内容裁剪比例
    private const val WEIBO_BOTTOM_CROP_RATIO = 0.70f

    /**
     * 加载并处理图片
     * 完整流程：加载 → 旋转 → 压缩
     */
    fun loadAndProcessImage(context: Context, imagePath: String): Bitmap? {
        return try {
            // 1. 加载原始图片
            val originalBitmap = loadBitmap(context, imagePath)
                ?: return null

            // 2. 根据EXIF信息旋转图片
            val rotatedBitmap = rotateBitmapIfNeeded(context, originalBitmap, imagePath)

            // 3. 压缩图片到合适大小
            val compressedBitmap = compressBitmap(rotatedBitmap)

            Log.d(TAG, "图片处理完成: ${compressedBitmap.width}x${compressedBitmap.height}, " +
                    "大小: ${getBitmapSizeInBytes(compressedBitmap) / 1024}KB")

            compressedBitmap
        } catch (e: Exception) {
            Log.e(TAG, "图片处理失败: ${e.message}", e)
            null
        }
    }

    /**
     * 从文件或URI加载Bitmap
     */
    private fun loadBitmap(context: Context, imagePath: String): Bitmap? {
        return try {
            when {
                // 文件路径
                imagePath.startsWith("/") -> {
                    BitmapFactory.decodeFile(imagePath)
                }
                // content:// URI
                imagePath.startsWith("content://") -> {
                    val uri = Uri.parse(imagePath)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(inputStream)
                }
                // assets中的文件
                else -> {
                    val inputStream = context.assets.open(imagePath)
                    BitmapFactory.decodeStream(inputStream)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载图片失败: ${e.message}", e)
            null
        }
    }

    /**
     * 根据EXIF信息旋转图片
     */
    private fun rotateBitmapIfNeeded(context: Context, bitmap: Bitmap, imagePath: String): Bitmap {
        return try {
            val exif = when {
                imagePath.startsWith("/") -> {
                    ExifInterface(imagePath)
                }
                imagePath.startsWith("content://") -> {
                    val uri = Uri.parse(imagePath)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    ExifInterface(inputStream!!)
                }
                else -> {
                    return bitmap
                }
            }

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            if (rotationDegrees == 0) {
                bitmap
            } else {
                Log.d(TAG, "旋转图片: $rotationDegrees°")
                rotateBitmap(bitmap, rotationDegrees.toFloat())
            }
        } catch (e: Exception) {
            Log.w(TAG, "EXIF信息读取失败: ${e.message}")
            bitmap
        }
    }

    /**
     * 旋转Bitmap
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        return try {
            val matrix = Matrix().apply {
                postRotate(degrees)
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )

            // 回收原始bitmap
            if (rotatedBitmap != bitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }

            rotatedBitmap
        } catch (e: Exception) {
            Log.e(TAG, "旋转图片失败: ${e.message}", e)
            bitmap
        }
    }

    /**
     * 压缩Bitmap到合适大小
     * 策略：
     * 1. 如果图片太大，先按比例缩放
     * 2. 如果还是太大，降低质量
     */
    private fun compressBitmap(originalBitmap: Bitmap): Bitmap {
        var bitmap = originalBitmap

        // 1. 如果尺寸太大，先缩放
        val maxDimension = maxOf(bitmap.width, bitmap.height)
        if (maxDimension > MAX_IMAGE_SIZE) {
            val scaleRatio = MAX_IMAGE_SIZE.toFloat() / maxDimension
            val newWidth = (bitmap.width * scaleRatio).toInt()
            val newHeight = (bitmap.height * scaleRatio).toInt()

            Log.d(TAG, "缩放图片: ${bitmap.width}x${bitmap.height} -> ${newWidth}x${newHeight}")

            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

            // 如果创建了新的bitmap，回收旧的
            if (bitmap != originalBitmap && !originalBitmap.isRecycled) {
                originalBitmap.recycle()
            }
        }

        return bitmap
    }

    /**
     * 微信聊天截图裁剪
     * 去除顶部状态栏和底部输入框
     */
    fun cropWechatChatArea(originalBitmap: Bitmap): Bitmap {
        return try {
            val width = originalBitmap.width
            val height = originalBitmap.height

            val topCrop = (height * WECHAT_TOP_CROP_RATIO).toInt()
            val bottomCrop = (height * WECHAT_BOTTOM_CROP_RATIO).toInt()
            val newHeight = bottomCrop - topCrop

            Log.d(TAG, "裁剪微信聊天区域: 高度 ${height} -> ${newHeight}")

            Bitmap.createBitmap(originalBitmap, 0, topCrop, width, newHeight)
        } catch (e: Exception) {
            Log.e(TAG, "微信图片裁剪失败: ${e.message}", e)
            originalBitmap
        }
    }

    /**
     * 小红书内容区域裁剪
     * 去除顶部标题区和底部标签区
     */
    fun cropXiaohongshuContent(originalBitmap: Bitmap): Bitmap {
        return try {
            val width = originalBitmap.width
            val height = originalBitmap.height

            val topCrop = (height * XIAOHONGSHU_TOP_CROP_RATIO).toInt()
            val bottomCrop = (height * XIAOHONGSHU_BOTTOM_CROP_RATIO).toInt()
            val newHeight = bottomCrop - topCrop

            Log.d(TAG, "裁剪小红书内容区域: 高度 ${height} -> ${newHeight}")

            Bitmap.createBitmap(originalBitmap, 0, topCrop, width, newHeight)
        } catch (e: Exception) {
            Log.e(TAG, "小红书图片裁剪失败: ${e.message}", e)
            originalBitmap
        }
    }

    /**
     * 知乎回答区域裁剪
     * 去除用户信息和评论区
     */
    fun cropZhihuAnswer(originalBitmap: Bitmap): Bitmap {
        return try {
            val width = originalBitmap.width
            val height = originalBitmap.height

            val topCrop = (height * ZHIHU_TOP_CROP_RATIO).toInt()
            val bottomCrop = (height * ZHIHU_BOTTOM_CROP_RATIO).toInt()
            val newHeight = bottomCrop - topCrop

            Log.d(TAG, "裁剪知乎回答区域: 高度 ${height} -> ${newHeight}")

            Bitmap.createBitmap(originalBitmap, 0, topCrop, width, newHeight)
        } catch (e: Exception) {
            Log.e(TAG, "知乎图片裁剪失败: ${e.message}", e)
            originalBitmap
        }
    }

    /**
     * 微博内容区域裁剪
     * 微博内容通常在图片上半部分
     */
    fun cropWeiboContent(originalBitmap: Bitmap): Bitmap {
        return try {
            val width = originalBitmap.width
            val height = originalBitmap.height

            val bottomCrop = (height * WEIBO_BOTTOM_CROP_RATIO).toInt()

            Log.d(TAG, "裁剪微博内容区域: 高度 ${height} -> ${bottomCrop}")

            Bitmap.createBitmap(originalBitmap, 0, 0, width, bottomCrop)
        } catch (e: Exception) {
            Log.e(TAG, "微博图片裁剪失败: ${e.message}", e)
            originalBitmap
        }
    }

    /**
     * Bitmap转换为ByteArray
     */
    fun bitmapToByteArray(bitmap: Bitmap, quality: Int = 85): ByteArray {
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }
    }

    /**
     * 获取Bitmap大小（字节）
     */
    fun getBitmapSizeInBytes(bitmap: Bitmap): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            bitmap.allocationByteCount
        } else {
            bitmap.byteCount
        }
    }

    /**
     * 获取图片文件URI
     */
    fun getImageUri(context: Context, imagePath: String): Uri? {
        return try {
            when {
                imagePath.startsWith("content://") -> Uri.parse(imagePath)
                imagePath.startsWith("/") -> Uri.fromFile(File(imagePath))
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取图片URI失败: ${e.message}", e)
            null
        }
    }

    /**
     * 保存Bitmap到文件
     */
    fun saveBitmapToFile(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "ocr_image_${System.currentTimeMillis()}.jpg"
    ): File? {
        return try {
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, out)
            }
            Log.d(TAG, "图片已保存到: ${file.absolutePath}")
            file
        } catch (e: IOException) {
            Log.e(TAG, "保存图片失败: ${e.message}", e)
            null
        }
    }

    /**
     * 计算采样率
     * @param options BitmapFactory.Options
     * @param reqWidth 目标宽度
     * @param reqHeight 目标高度
     */
    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight &&
                   halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * 从URI加载Bitmap（适用于content://）
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "从URI加载Bitmap失败: ${e.message}", e)
            null
        }
    }
}
