package com.evomind.app.ocr.model

/**
 * 支持的素材平台类型
 */
enum class PlatformType {
    /**
     * 微信聊天/朋友圈
     */
    WECHAT,

    /**
     * 小红书笔记
     */
    XIAOHONGSHU,

    /**
     * 知乎回答/文章
     */
    ZHIHU,

    /**
     * 微博帖子
     */
    WEIBO,

    /**
     * 通用/其他
     */
    GENERAL;

    /**
     * 获取平台显示名称
     */
    fun getDisplayName(): String = when (this) {
        WECHAT -> "微信"
        XIAOHONGSHU -> "小红书"
        ZHIHU -> "知乎"
        WEIBO -> "微博"
        GENERAL -> "通用"
    }

    companion object {
        /**
         * 从字符串解析平台类型
         */
        fun fromString(value: String): PlatformType = try {
            valueOf(value.uppercase())
        } catch (e: IllegalArgumentException) {
            GENERAL
        }
    }
}
