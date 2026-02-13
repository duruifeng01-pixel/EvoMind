package com.evomind.app.aigc.discussion

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.evomind.app.R
import com.evomind.app.database.entity.DiscussionMessageEntity
import com.evomind.app.database.entity.MessageSenderType
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class DiscussionActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_CARD_ID = "extra_card_id"
        private const val EXTRA_CARD_TITLE = "extra_card_title"

        fun createIntent(context: Context, cardId: Long, cardTitle: String): Intent {
            return Intent(context, DiscussionActivity::class.java).apply {
                putExtra(EXTRA_CARD_ID, cardId)
                putExtra(EXTRA_CARD_TITLE, cardTitle)
            }
        }
    }

    private val viewModel: DiscussionViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: TextInputEditText
    private lateinit var sendButton: MaterialButton
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var discussionTitleText: TextView

    private lateinit var messagesAdapter: DiscussionMessagesAdapter

    private var cardId: Long = -1
    private var cardTitle: String = ""
    private var activeDiscussionId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discussion)

        cardId = intent.getLongExtra(EXTRA_CARD_ID, -1)
        cardTitle = intent.getStringExtra(EXTRA_CARD_TITLE) ?: ""

        if (cardId == -1L) {
            Toast.makeText(this, "无效的卡片ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        initRecyclerView()
        bindViewModel()

        supportActionBar?.title = "讨论: $cardTitle"
        discussionTitleText.text = "讨论: $cardTitle"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.initDiscussion(cardId, cardTitle)
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        discussionTitleText = findViewById(R.id.discussionTitleText)

        sendButton.setOnClickListener {
            sendMessage()
        }

        messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun initRecyclerView() {
        messagesAdapter = DiscussionMessagesAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = messagesAdapter
    }

    private fun bindViewModel() {
        lifecycleScope.launch {
            viewModel.currentDiscussion.collect { discussion ->
                activeDiscussionId = discussion?.id
            }
        }

        lifecycleScope.launch {
            viewModel.messages.collect { messages ->
                messagesAdapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    recyclerView.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
                sendButton.isEnabled = !isLoading
                messageInput.isEnabled = !isLoading
            }
        }
    }

    private fun sendMessage() {
        val messageText = messageInput.text?.toString()?.trim() ?: ""
        if (messageText.isEmpty()) {
            Toast.makeText(this, "请输入消息", Toast.LENGTH_SHORT).show()
            return
        }

        val discussionId = activeDiscussionId
        if (discussionId == null) {
            Toast.makeText(this, "请先创建讨论会话", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.sendMessage(discussionId, messageText)
        messageInput.text?.clear()
        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(messageInput.windowToken, 0)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_discussion, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_discussions_list -> {
                showDiscussionsList()
                true
            }
            R.id.action_generate_summary -> {
                generateDiscussionSummary()
                true
            }
            R.id.action_new_discussion -> {
                viewModel.createNewDiscussion(cardId, cardTitle)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDiscussionsList() {
        lifecycleScope.launch {
            val discussions = viewModel.getDiscussionsByCard(cardId)
            if (discussions.isEmpty()) {
                Toast.makeText(this@DiscussionActivity, "暂无讨论历史", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val titles = discussions.map { it.sessionTitle }
            androidx.appcompat.app.AlertDialog.Builder(this@DiscussionActivity)
                .setTitle("选择讨论会话")
                .setItems(titles.toTypedArray()) { _, which ->
                    val selectedDiscussion = discussions[which]
                    viewModel.switchDiscussion(selectedDiscussion.id)
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun generateDiscussionSummary() {
        val discussionId = activeDiscussionId
        if (discussionId == null) {
            Toast.makeText(this, "请先创建讨论", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            viewModel.generateSummary(discussionId) { summary ->
                androidx.appcompat.app.AlertDialog.Builder(this@DiscussionActivity)
                    .setTitle("讨论总结")
                    .setMessage(summary)
                    .setPositiveButton("确定", null)
                    .setNeutralButton("保存") { _, _ ->
                        Toast.makeText(this@DiscussionActivity, "讨论总结已保存", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
        }
    }
}
