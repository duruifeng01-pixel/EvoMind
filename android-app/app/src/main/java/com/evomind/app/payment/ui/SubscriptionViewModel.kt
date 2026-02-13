package com.evomind.app.payment.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evomind.app.database.entity.*
import com.evomind.app.payment.SubscriptionService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val subscriptionService = SubscriptionService(application)
    private val _subscriptionPlans = MutableStateFlow<List<SubscriptionPlanEntity>>(emptyList())
    val subscriptionPlans: StateFlow<List<SubscriptionPlanEntity>> = _subscriptionPlans.asStateFlow()

    private val _currentSubscription = MutableStateFlow<UserSubscriptionEntity?>(null)
    val currentSubscription: StateFlow<UserSubscriptionEntity?> = _currentSubscription.asStateFlow()

    private val _tokenBalance = MutableStateFlow<SubscriptionService.TokenBalance?>(null)
    val tokenBalance: StateFlow<SubscriptionService.TokenBalance?> = _tokenBalance.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private lateinit var userUuid: String
    private var userId: Long = -1

    fun initialize(userId: Long) {
        this.userId = userId
        this.userUuid = UUID.randomUUID().toString()

        viewModelScope.launch {
            _isLoading.value = true
            try {
                subscriptionService.initializeSubscriptionPlans()
                loadData()
            } catch (e: Exception) {
                _errorMessage.value = "初始化失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadData() {
        subscriptionService.getAvailablePlans().also {
            _subscriptionPlans.value = it
        }

        subscriptionService.getUserSubscription(userId).also {
            _currentSubscription.value = it
        }

        subscriptionService.getTokenBalance(userId).also {
            _tokenBalance.value = it
        }
    }

    suspend fun getPlanName(planId: Long): String {
        return try {
            subscriptionService.getPlanName(planId)
        } catch (e: Exception) {
            "未知"
        }
    }

    suspend fun getDiscussionsByCard(cardId: Long): List<DiscussionEntity> {
        return emptyList()
    }

    fun subscribeToFreePlan(planId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = subscriptionService.subscribePlan(userId, planId)
                if (result != null) {
                    _currentSubscription.value = result
                    loadData()
                    _successMessage.value = "订阅成功！"
                } else {
                    _errorMessage.value = "订阅失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = "订阅失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun processPayment(plan: SubscriptionPlanEntity, paymentType: PaymentType) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val transactionId = generateTransactionId()

                if (plan.price > 0) {
                    val paymentSuccess = subscriptionService.purchaseTokens(
                        userId = userId,
                        yuanAmount = plan.price,
                        paymentType = paymentType,
                        transactionId = transactionId
                    )

                    if (!paymentSuccess) {
                        _errorMessage.value = "支付处理失败"
                        _isLoading.value = false
                        return@launch
                    }
                }

                val paymentRecord = PaymentRecordEntity(
                    userId = userId,
                    transactionId = transactionId,
                    paymentType = paymentType,
                    amount = plan.price,
                    status = PaymentStatus.SUCCESS,
                    description = "订阅 ${plan.name}"
                )

                val paymentId = paymentRecordDao().insert(paymentRecord)

                val subscription = subscriptionService.subscribePlan(
                    userId = userId,
                    planId = plan.planId,
                    paymentRecordId = paymentId
                )

                if (subscription != null) {
                    _currentSubscription.value = subscription
                    loadData()
                    _successMessage.value = "订阅成功！欢迎成为${plan.name}用户！"
                } else {
                    _errorMessage.value = "订阅失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = "处理失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun paymentRecordDao() = getApplication<android.app.Application>()
        .let { AppDatabase.getInstance(it) }
        .paymentRecordDao()

    private fun generateTransactionId(): String {
        return "TXN_${System.currentTimeMillis()}_${userUuid.takeLast(8)}"
    }
}
