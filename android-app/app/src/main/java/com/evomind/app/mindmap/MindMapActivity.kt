package com.evomind.app.mindmap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import com.evomind.app.R
import com.evomind.app.aigc.model.MindMapNode
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * 思维导图查看器Activity
 * 支持搜索、节点定位、收藏等交互功能
 */
class MindMapActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_MIND_MAP_JSON = "extra_mind_map_json"
        private const val EXTRA_CARD_TITLE = "extra_card_title"

        fun createIntent(context: Context, mindMapJson: String, cardTitle: String): Intent {
            return Intent(context, MindMapActivity::class.java).apply {
                putExtra(EXTRA_MIND_MAP_JSON, mindMapJson)
                putExtra(EXTRA_CARD_TITLE, cardTitle)
            }
        }
    }

    private val viewModel: MindMapViewModel by viewModels()

    private lateinit var mindMapView: MindMapCanvasView
    private lateinit var searchView: SearchView
    private lateinit var searchPrevButton: ImageButton
    private lateinit var searchNextButton: ImageButton
    private lateinit var searchResultText: TextView
    private lateinit var btnResetView: MaterialButton
    private lateinit var btnToggleFullscreen: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mind_map)

        // 获取传递的数据
        val mindMapJson = intent.getStringExtra(EXTRA_MIND_MAP_JSON)
        val cardTitle = intent.getStringExtra(EXTRA_CARD_TITLE) ?: "思维导图"

        if (mindMapJson == null) {
            Toast.makeText(this, "思维导图数据无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 初始化视图
        initViews()

        // 加载思维导图
        viewModel.loadMindMap(mindMapJson)

        // 绑定ViewModel数据
        bindViewModel()

        // 设置标题
        supportActionBar?.title = cardTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initViews() {
        mindMapView = findViewById(R.id.mindMapView)
        searchView = findViewById(R.id.searchView)
        searchPrevButton = findViewById(R.id.btnSearchPrev)
        searchNextButton = findViewById(R.id.btnSearchNext)
        searchResultText = findViewById(R.id.searchResultText)
        btnResetView = findViewById(R.id.btnResetView)
        btnToggleFullscreen = findViewById(R.id.btnToggleFullscreen)

        setupSearchView()
        setupMindMapView()
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    performSearch(it)
                    hideKeyboard()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    clearSearch()
                }
                return true
            }
        })

        // 设置搜索框键盘事件
        searchView.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(v.text.toString())
                hideKeyboard()
                true
            } else {
                false
            }
        }

        searchPrevButton.setOnClickListener {
            showPrevSearchResult()
        }

        searchNextButton.setOnClickListener {
            showNextSearchResult()
        }
    }

    private fun setupMindMapView() {
        // 节点点击事件
        mindMapView.onNodeClickListener = object : MindMapCanvasView.OnNodeClickListener {
            override fun onNodeClick(node: MindMapNode) {
                showNodeDetailDialog(node)
            }
        }

        // 节点长按事件（收藏功能）
        mindMapView.onNodeLongClickListener = object : MindMapCanvasView.OnNodeLongClickListener {
            override fun onNodeLongClick(node: MindMapNode): Boolean {
                toggleNodeFavorite(node)
                return true
            }
        }

        // 重置视图按钮
        btnResetView.setOnClickListener {
            resetMindMapView()
        }

        // 全屏切换
        btnToggleFullscreen.setOnClickListener {
            toggleFullscreen()
        }
    }

    private fun bindViewModel() {
        lifecycleScope.launch {
            viewModel.mindMapNode.collect { node ->
                node?.let {
                    mindMapView.setMindMap(it)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.searchResults.collect { results ->
                updateSearchResults(results)
            }
        }

        lifecycleScope.launch {
            viewModel.currentSearchIndex.collect { index ->
                updateFocusNodeDisplay(index)
            }
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            clearSearch()
            return
        }

        lifecycleScope.launch {
            viewModel.search(query)
        }
    }

    private fun clearSearch() {
        searchView.setQuery("", false)
        searchResultText.text = ""
        searchResultText.visibility = View.GONE
        searchPrevButton.visibility = View.GONE
        searchNextButton.visibility = View.GONE
    }

    private fun updateSearchResults(results: List<MindMapNode>) {
        if (results.isEmpty()) {
            searchResultText.text = "未找到结果"
            searchResultText.visibility = View.VISIBLE
            searchPrevButton.visibility = View.GONE
            searchNextButton.visibility = View.GONE
            Toast.makeText(this, "未找到匹配结果", Toast.LENGTH_SHORT).show()
        } else {
            searchResultText.text = "${viewModel.currentSearchIndex.value + 1}/${results.size}"
            searchResultText.visibility = View.VISIBLE
            searchPrevButton.visibility = View.VISIBLE
            searchNextButton.visibility = View.VISIBLE
        }
    }

    private fun updateFocusNodeDisplay(currentIndex: Int) {
        val totalResults = viewModel.searchResults.value.size
        if (totalResults > 0) {
            searchResultText.text = "${currentIndex + 1}/$totalResults"
        }
    }

    private fun showPrevSearchResult() {
        lifecycleScope.launch {
            viewModel.previousSearchResult()
        }
    }

    private fun showNextSearchResult() {
        lifecycleScope.launch {
            viewModel.nextSearchResult()
        }
    }

    private fun showNodeDetailDialog(node: MindMapNode) {
        val message = buildString {
            appendLine("节点: ${node.text}")
            appendLine("层级: ${node.level}")
            if (node.children.isNotEmpty()) {
                appendLine("子节点数: ${node.children.size}")
            }
            node.sourceParagraphRef?.let {
                appendLine("原文引用: $it")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("节点详情")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .setNegativeButton("定位") { _, _ ->
                mindMapView.focusNode(node.id)
            }
            .show()
    }

    private fun toggleNodeFavorite(node: MindMapNode) {
        lifecycleScope.launch {
            if (viewModel.isNodeFavorited(node.id)) {
                viewModel.removeFavoriteNode(node.id)
                Toast.makeText(this@MindMapActivity, "已取消收藏", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.addFavoriteNode(node.id, node.text)
                Toast.makeText(this@MindMapActivity, "已收藏", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetMindMapView() {
        mindMapView.resetView()
        clearSearch()
    }

    private fun toggleFullscreen() {
        // 实现全屏模式切换
        val isFullscreen = supportActionBar?.isShowing ?: false
        if (isFullscreen) {
            supportActionBar?.hide()
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
            btnToggleFullscreen.text = "退出全屏"
        } else {
            supportActionBar?.show()
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            btnToggleFullscreen.text = "全屏"
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchView.windowToken, 0)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

// ViewModel
data class MindMapViewState(
    val mindMapNode: MindMapNode? = null,
    val searchResults: List<MindMapNode> = emptyList(),
    val currentSearchIndex: Int = 0,
    val favoriteNodes: Set<String> = emptySet(),
    val isLoading: Boolean = false
)

/**
 * 搜索结果高亮数据
 */
data class SearchHighlight(
    val nodeId: String,
    val isCurrent: Boolean
)
