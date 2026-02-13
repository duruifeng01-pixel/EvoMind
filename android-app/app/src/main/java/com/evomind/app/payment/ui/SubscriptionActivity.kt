package com.evomind.app.payment.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.evomind.app.R
import com.evomind.app.database.entity.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

class SubscriptionActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_USER_ID = "extra_user_id"
        private const val EXTRA_USER_UUID = "extra_user_uuid"

        fun createIntent(context: Context, userId: Long, userUuid: String): Intent {
            return Intent(context, SubscriptionActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_USER_UUID, userUuid)
            }
        }
    }

    private val viewModel: SubscriptionViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var currentSubscriptionView: View
    private lateinit var tokenBalanceView: View
    private lateinit var loadingView: View
    private lateinit var adapter: SubscriptionPlansAdapter

    private var userId: Long = -1
    private var userUuid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        userId = intent.getLongExtra(EXTRA_USER_ID, -1)
        userUuid = intent.getStringExtra(EXTRA_USER_UUID) ?: ""

        if (userId == -1L) {
            Toast.makeText(this, "无效的用户ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        bindViewModel()

        supportActionBar?.title = "订阅管理"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.initialize(userId)
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.plansRecyclerView)
        currentSubscriptionView = findViewById(R.id.currentSubscriptionView)
        tokenBalanceView = findViewById(R.id.tokenBalanceView)
        loadingView = findViewById(R.id.loadingView)

        adapter = SubscriptionPlansAdapter { plan ->
            subscribeToPlan(plan)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun bindViewModel() {
        lifecycleScope.launch {
            viewModel.subscriptionPlans.collect { plans ->
                adapter.submitList(plans)
            }
        }

        lifecycleScope.launch {
            viewModel.currentSubscription.collect { subscription ->
                updateCurrentSubscription(subscription)
            }
        }

        lifecycleScope.launch {
            viewModel.tokenBalance.collect { balance ->
                updateTokenBalance(balance)
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { message ->
                message?.let {
                    Toast.makeText(this@SubscriptionActivity, it, Toast.LENGTH_SHORT).show()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.successMessage.collect { message ->
                message?.let {
                    Toast.makeText(this@SubscriptionActivity, it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateCurrentSubscription(subscription: UserSubscriptionEntity?) {
        val titleText = currentSubscriptionView.findViewById<MaterialTextView>(R.id.subscriptionTitle)
        val statusText = currentSubscriptionView.findViewById<MaterialTextView>(R.id.subscriptionStatus)

        if (subscription == null) {
            titleText.text = "当前订阅: 免费版"
            statusText.text = "状态: 活跃"
        } else {
            lifecycleScope.launch {
                val planName = viewModel.getPlanName(subscription.planId ?: 0)
                titleText.text = "当前订阅: $planName"
                statusText.text = "状态: ${subscription.status}"
            }
        }
    }

    private fun updateTokenBalance(balance: SubscriptionService.TokenBalance?) {
        balance?.let {
            val balanceText = tokenBalanceView.findViewById<MaterialTextView>(R.id.tokenBalanceText)
            val usedText = tokenBalanceView.findViewById<MaterialTextView>(R.id.tokenUsedText)
            val remainingText = tokenBalanceView.findViewById<MaterialTextView>(R.id.tokenRemainingText)

            balanceText.text = "Token余额: ${it.totalTokens}"
            usedText.text = "已使用: ${it.usedTokens}"
            remainingText.text = "剩余: ${it.remainingTokens}"
        }
    }

    private fun subscribeToPlan(plan: SubscriptionPlanEntity) {
        if (plan.price > 0) {
            showPaymentDialog(plan)
        } else {
            // 免费计划直接订阅
            lifecycleScope.launch {
                viewModel.subscribeToFreePlan(plan.planId)
            }
        }
    }

    private fun showPaymentDialog(plan: SubscriptionPlanEntity) {
        val message = buildString {
            appendLine("计划: ${plan.name}")
            appendLine("价格: ¥${plan.price}")
            appendLine("时长: ${plan.durationDays}天")
            appendLine("Token额度: ${plan.tokenQuota}")
            appendLine()
            appendLine("请选择支付方式:")
        }

        val options = arrayOf("微信支付", "支付宝", "取消")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("确认订阅")
            .setMessage(message)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> processPayment(plan, PaymentType.WECHAT_PAY)
                    1 -> processPayment(plan, PaymentType.ALIPAY)
                }
            }
            .show()
    }

    private fun processPayment(plan: SubscriptionPlanEntity, paymentType: PaymentType) {
        lifecycleScope.launch {
            viewModel.processPayment(plan, paymentType)
        }
    }
}

class SubscriptionPlansAdapter(
    private val onSubscribeClick: (SubscriptionPlanEntity) -> Unit
) : RecyclerView.Adapter<SubscriptionPlansAdapter.ViewHolder>() {

    private var plans: List<SubscriptionPlanEntity> = emptyList()

    fun submitList(plans: List<SubscriptionPlanEntity>) {
        this.plans = plans
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subscription_plan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(plans[position])
    }

    override fun getItemCount() = plans.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: MaterialTextView = itemView.findViewById(R.id.planNameText)
        private val priceText: MaterialTextView = itemView.findViewById(R.id.planPriceText)
        private val descriptionText: MaterialTextView = itemView.findViewById(R.id.planDescriptionText)
        private val tokenQuotaText: MaterialTextView = itemView.findViewById(R.id.tokenQuotaText)
        private val featuresText: MaterialTextView = itemView.findViewById(R.id.featuresText)
        private val subscribeButton: MaterialButton = itemView.findViewById(R.id.subscribeButton)

        fun bind(plan: SubscriptionPlanEntity) {
            nameText.text = plan.name
            priceText.text = if (plan.price == 0.0) "免费" else "¥${plan.price}"
            descriptionText.text = plan.description
            tokenQuotaText.text = "${plan.tokenQuota} tokens"

            // 解析功能列表
            try {
                val features = com.google.gson.Gson().fromJson(
                    plan.features,
                    Array<String>::class.java
                )
                featuresText.text = features.joinToString("\n") { "• $it" }
            } catch (e: Exception) {
                featuresText.text = ""
            }

            subscribeButton.text = if (plan.price == 0.0) "使用免费版" else "立即订阅"
            subscribeButton.setOnClickListener {
                onSubscribeClick(plan)
            }
        }
    }
}
