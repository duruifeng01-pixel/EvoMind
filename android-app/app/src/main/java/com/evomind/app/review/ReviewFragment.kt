package com.evomind.app.review

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.evomind.app.R
import com.evomind.app.database.entity.CardEntity
import com.evomind.app.database.entity.ReviewSessionEntity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

/**
 * 复习界面Fragment
 * 展示待复习卡片和复习功能
 */
class ReviewFragment : Fragment() {

    private val viewModel: ReviewViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: MaterialTextView
    private lateinit var statsView: MaterialTextView
    private lateinit var reviewButton: MaterialButton

    private val adapter = ReviewCardsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_review, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化视图
        recyclerView = view.findViewById(R.id.recyclerView)
        emptyView = view.findViewById(R.id.emptyView)
        statsView = view.findViewById(R.id.statsView)
        reviewButton = view.findViewById(R.id.reviewButton)

        // 设置RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // 观察待复习卡片
        viewModel.dueCards.observe(viewLifecycleOwner) { cards ->
            if (cards.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                reviewButton.isEnabled = false
            } else {
                emptyView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                reviewButton.isEnabled = true
                adapter.submitList(cards)
            }
        }

        // 观察统计信息
        viewModel.reviewStats.observe(viewLifecycleOwner) { stats ->
            updateStatsDisplay(stats)
        }

        // 复习按钮点击事件
        reviewButton.setOnClickListener {
            val cards = adapter.getCurrentList()
            if (cards.isNotEmpty()) {
                startQuickReview(cards.first())
            }
        }

        // 加载数据
        viewModel.loadReviewStats()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadReviewStats()
    }

    private fun updateStatsDisplay(stats: ReviewSessionService.ReviewStats) {
        val statsText = StringBuilder()
        statsText.append("今日复习: ${stats.todayReviews}次（${stats.todayDistinctCards}张卡片）\n")
        statsText.append("本周复习: ${stats.weekReviews}次\n")
        statsText.append("待复习: ${stats.dueCardsCount}张卡片\n")
        stats.averageQuality?.let {
            statsText.append("平均质量: ${String.format("%.1f", it)}/5.0")
        }

        statsView.text = statsText.toString()
    }

    private fun startQuickReview(card: CardEntity) {
        // 显示复习对话框
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(card.title)
            setMessage(card.oneLineGuide)
            setPositiveButton("非常简单 (5)") { _, _ ->
                completeReview(card, 5)
            }
            setNeutralButton("认识 (4)") { _, _ ->
                completeReview(card, 4)
            }
            setNegativeButton("模糊 (3)") { _, _ ->
                completeReview(card, 3)
            }
        }.show()
    }

    private fun completeReview(card: CardEntity, quality: Int) {
        lifecycleScope.launch {
            // 开始复习会话
            viewModel.startReview(card, "QUICK")

            // 立即完成（简单模式）
            viewModel.completeReview(quality)

            // 显示结果
            val description = viewModel.getQualityDescription(quality)
            showReviewCompleteDialog(description, quality)
        }
    }

    private fun showReviewCompleteDialog(qualityDescription: String, quality: Int) {
        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle("复习完成")
            setMessage(qualityDescription)
            setPositiveButton("确定", null)
        }.show()
    }
}

/**
 * 复习卡片适配器
 */
class ReviewCardsAdapter : RecyclerView.Adapter<ReviewCardsAdapter.ViewHolder>() {

    private var cards: List<CardEntity> = emptyList()

    fun submitList(cards: List<CardEntity>) {
        this.cards = cards
        notifyDataSetChanged()
    }

    fun getCurrentList() = cards

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(cards[position])
    }

    override fun getItemCount() = cards.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text1 = itemView.findViewById<MaterialTextView>(android.R.id.text1)
        private val text2 = itemView.findViewById<MaterialTextView>(android.R.id.text2)

        fun bind(card: CardEntity) {
            text1.text = card.title
            text2.text = card.oneLineGuide
        }
    }
}