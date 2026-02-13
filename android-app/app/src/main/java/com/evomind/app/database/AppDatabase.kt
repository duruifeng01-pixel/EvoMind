package com.evomind.app.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.evomind.app.database.dao.*
import com.evomind.app.database.entity.*

/**
 * 应用数据库
 */
@Database(
    entities = [
        SourceEntity::class,
        CardEntity::class,
        ReviewSessionEntity::class,
        MindMapFavoriteEntity::class,
        NodeCommentEntity::class,
        DiscussionEntity::class,
        DiscussionMessageEntity::class,
        PaymentRecordEntity::class,
        SubscriptionPlanEntity::class,
        UserSubscriptionEntity::class,
        UserEntity::class,
        TokenUsageRecordEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun cardDao(): CardDao
    abstract fun reviewSessionDao(): ReviewSessionDao
    abstract fun mindMapFavoriteDao(): MindMapFavoriteDao
    abstract fun nodeCommentDao(): NodeCommentDao
    abstract fun discussionDao(): DiscussionDao
    abstract fun discussionMessageDao(): DiscussionMessageDao
    abstract fun paymentRecordDao(): PaymentRecordDao
    abstract fun subscriptionPlanDao(): SubscriptionPlanDao
    abstract fun userSubscriptionDao(): UserSubscriptionDao
    abstract fun userDao(): UserDao
    abstract fun tokenUsageDao(): TokenUsageDao

    companion object {
        const val DATABASE_NAME = "evomind_database"

        // 单例实例
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
