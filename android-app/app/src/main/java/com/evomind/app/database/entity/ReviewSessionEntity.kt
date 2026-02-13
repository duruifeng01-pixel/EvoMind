package com.evomind.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 复习会话实体
 * 记录每一次复习的详细信息
 */
@Entity(
    tableName = "review_sessions",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReviewSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "card_id")
    val cardId: Long,

    @ColumnInfo(name = "session_type")
    val sessionType: ReviewSessionType,

    @ColumnInfo(name = "ease_factor")
    val easeFactor: Float, // 记忆容易度因子（SM2算法）

    @ColumnInfo(name = "quality")
    val quality: Int, // 复习质量评分（0-5）

    @ColumnInfo(name = "interval_days")
    val intervalDays: Int, // 间隔天数

    @ColumnInfo(name = "reviewed_at")
    val reviewedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "review_duration")
    val reviewDuration: Long? = null, // 复习时长（毫秒）

    @ColumnInfo(name = "notes")
    val notes: String? = null // 用户笔记
) {
    /**
     * 复习会话类型
     */
    enum class ReviewSessionType {
        QUICK,      // 快速复习（浏览）
        DEEP,       // 深度复习（详细阅读）
        TEST,       // 测试复习（自测）
        ASSOCIATIVE // 联想复习（关联其他知识）
    }

    /**
     * 复习质量评分标准（SM2算法）
     * 0 - 完全忘记
     * 1 - 错误回答，但看到正确答案后记得
     * 2 - 错误回答，但看到正确答案后觉得熟悉
     * 3 - 正确回答，但费了很大劲
     * 4 - 正确回答，有些犹豫
     * 5 - 完美回答，轻松回忆
     */
    enum class ReviewQuality(val value: Int, val description: String) {
        PERFECT(5, "完美回答"),
        GOOD(4, "正确但有些犹豫"),
        HARD(3, "正确但费了很大劲"),
        DIFFICULT(2, "错误，但看到答案后觉得熟悉"),
        FORGOTTEN(1, "错误，但看到答案后记得"),
        COMPLETELY_FORGOTTEN(0, "完全忘记")
    }
}
