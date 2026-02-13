package com.evomind.app.ocr

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evomind.app.database.AppDatabase
import com.evomind.app.database.entity.SourceEntity
import com.evomind.app.ocr.model.PlatformType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OcrViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = OcrRepository(application, database.sourceDao())

    private val _uiState = MutableStateFlow<OcrUiState>(OcrUiState.Idle)
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    private val _currentImagePath = MutableStateFlow<String?>(null)
    val currentImagePath: StateFlow<String?> = _currentImagePath.asStateFlow()

    private val _selectedPlatform = MutableStateFlow(PlatformType.GENERAL)
    val selectedPlatform: StateFlow<PlatformType> = _selectedPlatform.asStateFlow()

    private val _recognitionHistory = MutableStateFlow<List<SourceEntity>>(emptyList())
    val recognitionHistory: StateFlow<List<SourceEntity>> = _recognitionHistory.asStateFlow()

    val allSources: StateFlow<List<SourceEntity>> = repository.getAllSources()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var currentJob: Job? = null

    init {
        loadRecognitionHistory()
    }

    fun setImagePath(path: String) {
        _currentImagePath.value = path
        _uiState.value = OcrUiState.ImageSelected(path)
    }

    fun setPlatform(platform: PlatformType) {
        _selectedPlatform.value = platform
    }

    fun startOcrRecognition(
        title: String? = null,
        useHighAccuracy: Boolean = false
    ) {
        val imagePath = currentImagePath.value
        if (imagePath.isNullOrEmpty()) {
            _uiState.value = OcrUiState.Error("请先选择图片")
            return
        }

        currentJob?.cancel()

        currentJob = viewModelScope.launch {
            repository.processImage(
                imagePath = imagePath,
                platformType = selectedPlatform.value,
                title = title,
                useHighAccuracy = useHighAccuracy
            ).collect { result ->
                when {
                    result.isSuccess -> {
                        val source = result.getOrNull()!!
                        _uiState.value = OcrUiState.Success(source)
                        loadRecognitionHistory()
                    }
                    else -> {
                        val exception = result.exceptionOrNull()
                        val message = exception?.message ?: "处理失败"

                        if (!message.contains("开始")) {
                            _uiState.value = OcrUiState.Error(message)
                        }
                    }
                }
            }
        }
    }

    fun deleteSource(source: SourceEntity) {
        viewModelScope.launch {
            val result = repository.deleteSource(source)
            when {
                result.isSuccess -> {
                    _uiState.value = OcrUiState.Deleted
                    loadRecognitionHistory()
                }
                else -> {
                    _uiState.value = OcrUiState.Error("删除失败: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun searchSources(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                loadRecognitionHistory()
            } else {
                repository.searchSources(query)
                    .collect { sources ->
                        _recognitionHistory.value = sources
                    }
            }
        }
    }

    private fun loadRecognitionHistory() {
        viewModelScope.launch {
            repository.getAllSources()
                .collect { sources ->
                    _recognitionHistory.value = sources
                }
        }
    }

    fun resetState() {
        _uiState.value = OcrUiState.Idle
        currentJob?.cancel()
        currentJob = null
    }

    fun cleanup() {
        repository.release()
        currentJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}

/**
 * OCR UI状态
 */
sealed interface OcrUiState {
    data object Idle : OcrUiState
    data class ImageSelected(val imagePath: String) : OcrUiState
    data object Processing : OcrUiState
    data class BatchProcessing(val progress: Float) : OcrUiState
    data class Success(val source: SourceEntity) : OcrUiState
    data class BatchComplete(
        val successCount: Int,
        val failureCount: Int
    ) : OcrUiState
    data object Deleted : OcrUiState
    data object Updated : OcrUiState
    data class TagAdded(val sourceId: Long, val tag: String) : OcrUiState
    data class Error(val message: String) : OcrUiState
}