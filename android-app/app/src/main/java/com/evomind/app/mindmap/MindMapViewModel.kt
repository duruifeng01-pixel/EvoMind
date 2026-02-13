package com.evomind.app.mindmap

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evomind.app.aigc.model.MindMapNode
import com.evomind.app.database.AppDatabase
import com.evomind.app.database.entity.MindMapFavoriteEntity
import com.evomind.app.database.entity.NodeCommentEntity
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MindMapViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val mindMapFavoriteDao = database.mindMapFavoriteDao()
    private val nodeCommentDao = database.nodeCommentDao()

    private val _mindMapNode = MutableStateFlow<MindMapNode?>(null)
    val mindMapNode: StateFlow<MindMapNode?> = _mindMapNode.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MindMapNode>>(emptyList())
    val searchResults: StateFlow<List<MindMapNode>> = _searchResults.asStateFlow()

    private val _currentSearchIndex = MutableStateFlow(0)
    val currentSearchIndex: StateFlow<Int> = _currentSearchIndex.asStateFlow()

    private val _favoriteNodes = MutableStateFlow<Map<String, MindMapFavoriteEntity>>(emptyMap())
    val favoriteNodes: StateFlow<Map<String, MindMapFavoriteEntity>> = _favoriteNodes.asStateFlow()

    private val _nodeComments = MutableStateFlow<Map<String, List<NodeCommentEntity>>>(emptyMap())
    val nodeComments: StateFlow<Map<String, List<NodeCommentEntity>>> = _nodeComments.asStateFlow()

    private var currentCardId: Long = -1

    fun loadMindMap(cardId: Long, mindMapJson: String) {
        currentCardId = cardId

        viewModelScope.launch {
            val node = MindMapNode.fromJson(mindMapJson)
            _mindMapNode.value = node

            loadFavoriteNodes(cardId)
            loadAllComments(cardId)
        }
    }

    private fun loadFavoriteNodes(cardId: Long) {
        viewModelScope.launch {
            mindMapFavoriteDao.getByCardId(cardId)
                .collect { favorites ->
                    val favoriteMap = favorites.associateBy { it.nodeId }
                    _favoriteNodes.value = favoriteMap
                }
        }
    }

    private fun loadAllComments(cardId: Long) {
        viewModelScope.launch {
            nodeCommentDao.getByCardId(cardId)
                .collect { comments ->
                    val commentMap = comments.groupBy { it.nodeId }
                    _nodeComments.value = commentMap
                }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            val results = mutableListOf<MindMapNode>()
            val lowerQuery = query.lowercase()

            _mindMapNode.value?.let { root ->
                searchInNode(root, lowerQuery, results)
            }

            _searchResults.value = results
            _currentSearchIndex.value = if (results.isNotEmpty()) 0 else -1
        }
    }

    private fun searchInNode(node: MindMapNode, query: String, results: MutableList<MindMapNode>) {
        if (node.text.lowercase().contains(query)) {
            results.add(node)
        }
        node.children.forEach { child ->
            searchInNode(child, query, results)
        }
    }

    fun nextSearchResult() {
        val total = _searchResults.value.size
        if (total <= 1) return
        _currentSearchIndex.value = (_currentSearchIndex.value + 1) % total
    }

    fun previousSearchResult() {
        val total = _searchResults.value.size
        if (total <= 1) return
        _currentSearchIndex.value = if (_currentSearchIndex.value <= 0) total - 1 else _currentSearchIndex.value - 1
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
        _currentSearchIndex.value = 0
    }

    fun addFavoriteNode(nodeId: String, nodeText: String, parentPath: String, level: Int, notes: String? = null) {
        viewModelScope.launch {
            val favorite = MindMapFavoriteEntity(
                cardId = currentCardId,
                nodeId = nodeId,
                nodeText = nodeText,
                parentPath = parentPath,
                level = level,
                notes = notes
            )

            mindMapFavoriteDao.insert(favorite)
            loadFavoriteNodes(currentCardId)
        }
    }

    fun removeFavoriteNode(nodeId: String) {
        viewModelScope.launch {
            mindMapFavoriteDao.deleteByNodeIdAndCardId(nodeId, currentCardId)
            loadFavoriteNodes(currentCardId)
        }
    }

    fun isNodeFavorited(nodeId: String): Boolean {
        return _favoriteNodes.value.containsKey(nodeId)
    }

    fun addComment(nodeId: String, commentText: String, parentId: Long? = null) {
        viewModelScope.launch {
            val comment = NodeCommentEntity(
                cardId = currentCardId,
                nodeId = nodeId,
                commentText = commentText,
                parentCommentId = parentId
            )

            nodeCommentDao.insert(comment)
            loadAllComments(currentCardId)
        }
    }

    fun deleteComment(commentId: Long) {
        viewModelScope.launch {
            nodeCommentDao.deleteById(commentId)
            loadAllComments(currentCardId)
        }
    }

    fun getCommentsForNode(nodeId: String): List<NodeCommentEntity> {
        return _nodeComments.value[nodeId] ?: emptyList()
    }

    fun getCommentCount(nodeId: String): Int {
        return getCommentsForNode(nodeId).size
    }

    fun getCurrentFocusedNode(): MindMapNode? {
        val index = _currentSearchIndex.value
        val results = _searchResults.value
        return if (index >= 0 && index < results.size) results[index] else null
    }
}
