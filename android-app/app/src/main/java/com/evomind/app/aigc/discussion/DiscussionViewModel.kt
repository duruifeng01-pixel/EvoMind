package com.evomind.app.aigc.discussion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evomind.app.database.AppDatabase
import com.evomind.app.database.entity.DiscussionEntity
import com.evomind.app.database.entity.DiscussionMessageEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DiscussionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val discussionService = DiscussionService(application)
    private val discussionDao = database.discussionDao()
    private val messageDao = database.discussionMessageDao()

    private val _currentDiscussion = MutableStateFlow<DiscussionEntity?>(null)
    val currentDiscussion: StateFlow<DiscussionEntity?> = _currentDiscussion.asStateFlow()

    private val _messages = MutableStateFlow<List<DiscussionMessageEntity>>(emptyList())
    val messages: StateFlow<List<DiscussionMessageEntity>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentCardId: Long = -1

    fun initDiscussion(cardId: Long, cardTitle: String) {
        currentCardId = cardId
        viewModelScope.launch {
            val activeDiscussion = discussionService.getActiveDiscussion(cardId)
            if (activeDiscussion != null) {
                _currentDiscussion.value = activeDiscussion
                loadDiscussionMessages(activeDiscussion.id)
            } else {
                createNewDiscussion(cardId, cardTitle)
            }
        }
    }

    fun createNewDiscussion(cardId: Long, cardTitle: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val discussion = discussionService.createDiscussion(
                    cardId = cardId,
                    sessionTitle = "关于: $cardTitle",
                    contextSummary = "讨论卡片《$cardTitle》的内容"
                )

                _currentDiscussion.value = discussion
                _messages.value = emptyList()
            } catch (e: Exception) {
                _messages.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun switchDiscussion(discussionId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val discussion = discussionDao.getById(discussionId).first()
                if (discussion != null) {
                    discussionDao.update(discussion.copy(isActive = true))
                    discussionService.closeDiscussion(discussionId)
                }

                _currentDiscussion.value = discussion
                loadDiscussionMessages(discussionId)
            } catch (e: Exception) {
                // Error handling
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(discussionId: Long, messageContent: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                discussionService.sendMessage(
                    discussionId = discussionId,
                    messageContent = messageContent,
                    onLoading = {
                        _isLoading.value = true
                    },
                    onSuccess = { aiReply ->
                        viewModelScope.launch {
                            loadDiscussionMessages(discussionId)
                        }
                        _isLoading.value = false
                    },
                    onError = { errorMessage ->
                        _isLoading.value = false
                        // Error handling
                    }
                )
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    suspend fun getDiscussionsByCard(cardId: Long): List<DiscussionEntity> {
        return discussionDao.getByCardId(cardId).first()
    }

    fun generateSummary(discussionId: Long, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            discussionService.generateDiscussionSummary(discussionId) { summary ->
                onComplete(summary)
            }
        }
    }

    private suspend fun loadDiscussionMessages(discussionId: Long) {
        messageDao.getByDiscussionId(discussionId).collect { messages ->
            _messages.value = messages
        }
    }

    override fun onCleared() {
        super.onCleared()
        discussionService.cleanup()
    }
}