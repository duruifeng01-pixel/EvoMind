package com.evomind.app.review

import android.app.Application
import androidx.lifecycle.*
import com.evomind.app.database.AppDatabase
import com.evomind.app.database.entity.CardEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 复习ViewModel
 * 为UI层提供复习相关的数据和操作
 */
class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val reviewService = ReviewSessionService(application)

    // 待复习的卡片
    val dueCards = reviewService.getDueCards().asLiveData()

    // 复习统计
    private val _reviewStats = MutableLiveData<ReviewSessionService.ReviewStats>()
    val reviewStats: LiveData<ReviewSessionService.ReviewStats> = _reviewStats

    // 当前复习会话
    private val _currentSession = MutableLiveData<CurrentSession?>()
    val currentSession: LiveData<CurrentSession?> = _currentSession

    // 加载状态
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * 加载复习统计
     */
    fun loadReviewStats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val stats = reviewService.getReviewStats()
                _reviewStats.value = stats
            } catch (e: Exception) {
                // 错误处理
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 开始复习
     */
    fun startReview(card: CardEntity, sessionType: String = "QUICK") {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val type = when (sessionType.uppercase()) {
                    "QUICK" -> ReviewSessionEntity.ReviewSessionType.QUICK
                    "DEEP" -> ReviewSessionEntity.ReviewSessionType.DEEP
                    "TEST" -> ReviewSessionEntity.ReviewSessionType.TEST
                    "ASSOCIATIVE" -> ReviewSessionEntity.ReviewSessionType.ASSOCIATIVE
                    else -> ReviewSessionEntity.ReviewSessionType.QUICK
                }

                val sessionId = reviewService.startReviewSession(card.id, type)
                _currentSession.value = CurrentSession(
                    sessionId = sessionId,
                    card = card,
                    sessionType = type
                )
            } catch (e: Exception) {
                // 错误处理
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 完成复习
     */
    fun completeReview(quality: Int, notes: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val session = _currentSession.value
                if (session != null) {
                    reviewService.completeReviewSession(session.sessionId, quality, notes)
                    _currentSession.value = null
                    loadReviewStats() // 刷新统计
                }
            } catch (e: Exception) {
                // 错误处理
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 获取质量评分描述
     */
    fun getQualityDescription(quality: Int): String {
        return reviewService.algorithm.getQualityDescription(quality)
    }

    /**
     * 检查卡片是否需要复习
     */
    fun isDueForReview(card: CardEntity): Boolean {
        return reviewService.algorithm.isDueForReview(card)
    }

    /**
     * 计算复习紧迫度
     */
    fun calculateUrgency(card: CardEntity): Float {
        return reviewService.algorithm.calculateUrgency(card)
    }

    /**
     * 当前复习会话
     */
    data class CurrentSession(
        val sessionId: Long,
        val card: CardEntity,
        val sessionType: ReviewSessionEntity.ReviewSessionType
    )
}