package com.evomind.app.ocr

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.evomind.app.R
import com.evomind.app.database.entity.SourceEntity
import com.evomind.app.ocr.model.PlatformType
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class OcrTestActivity : ComponentActivity() {
    private lateinit var viewModel: OcrViewModel
    private var currentPhotoPath: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "需要权限才能使用OCR功能", Toast.LENGTH_SHORT).show()
        }
    }

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val imagePath = getRealPathFromURI(it)
            imagePath?.let { path ->
                viewModel.setImagePath(path)
            }
        }
    }

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoPath?.let {
                viewModel.setImagePath(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = OcrViewModel(application)

        checkAndRequestPermissions()

        setContent {
            OcrTestScreen(
                viewModel = viewModel,
                onSelectImage = ::selectImage,
                onTakePhoto = ::takePhoto,
                onNavigateBack = { finish() }
            )
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun selectImage() {
        selectImageLauncher.launch("image/*")
    }

    private fun takePhoto() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: Exception) {
            Toast.makeText(this, "创建文件失败", Toast.LENGTH_SHORT).show()
            null
        }

        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                it
            )
            currentPhotoPath = it.absolutePath
            takePhotoLauncher.launch(photoURI)
        }
    }

    @Throws(Exception::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = getExternalFilesDir(null)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        return try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                it.moveToFirst()
                it.getString(columnIndex)
            }
        } catch (e: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrTestScreen(
    viewModel: OcrViewModel,
    onSelectImage: () -> Unit,
    onTakePhoto: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val imagePath by viewModel.currentImagePath.collectAsState()
    val platform by viewModel.selectedPlatform.collectAsState()
    val history by viewModel.recognitionHistory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OCR测试工具") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 平台选择
            Text(
                text = "选择素材来源",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlatformType.values().forEach { platformType ->
                    FilterChip(
                        selected = platform == platformType,
                        onClick = { viewModel.setPlatform(platformType) },
                        label = { Text(platformType.getDisplayName()) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 图片选择区域
            Text(
                text = "选择图片",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSelectImage,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📁 相册")
                }

                OutlinedButton(
                    onClick = onTakePhoto,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📷 拍照")
                }
            }

            // 显示选中的图片
            imagePath?.let { path ->
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "选中图片：",
                    style = MaterialTheme.typography.titleMedium
                )

                AsyncImage(
                    model = File(path),
                    contentDescription = "选中的图片",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 开始识别按钮
                Button(
                    onClick = { viewModel.startOcrRecognition() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState !is OcrUiState.Processing
                ) {
                    Text("开始OCR识别")
                }
            }

            // 状态和结果展示
            when (val state = uiState) {
                is OcrUiState.Processing -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "识别中...",
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 8.dp)
                    )
                }

                is OcrUiState.Success -> {
                    OcrResultView(source = state.source)
                }

                is OcrUiState.Error -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "错误：${state.message}",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                else -> {}
            }

            // 历史记录
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "识别历史（共 ${history.size} 条）",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (history.isEmpty()) {
                Text(
                    text = "暂无识别记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                history.take(10).forEach { source ->
                    SourceHistoryItem(
                        source = source,
                        onDelete = { viewModel.deleteSource(source) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun OcrResultView(source: SourceEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "识别成功！",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "标题：${source.title}",
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "平台：${PlatformType.valueOf(source.platform).getDisplayName()}"
            )

            Text(
                text = "置信度：${"%.2f".format(source.confidence * 100)}%"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "识别文本：",
                fontWeight = FontWeight.Medium
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            ) {
                Text(
                    text = source.cleanedText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SourceHistoryItem(
    source: SourceEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = source.title,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "${PlatformType.valueOf(source.platform).getDisplayName()} | " +
                           "${"%.0f".format(source.confidence * 100)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Text(
                    text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                        .format(Date(source.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            IconButton(onClick = onDelete) {
                Text("🗑️", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
