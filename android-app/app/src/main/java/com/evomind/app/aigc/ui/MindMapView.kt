package com.evomind.app.aigc.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.evomind.app.aigc.model.MindMapNode


/**
 * 思维导图预览和下钻入口
 */
@Composable
fun MindMapPreview(
    mindMapMarkdown: String,
    onDrillDown: (MindMapNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val mindMapNode = remember(mindMapMarkdown) {
        MindMapNode.fromMarkdown(mindMapMarkdown)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (mindMapNode != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = mindMapNode.text,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "${mindMapNode.children.size} 个子主题",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    text = mindMapMarkdown,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 下钻按钮
            FloatingActionButton(
                onClick = { mindMapNode?.let { onDrillDown(it) } },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(40.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("🔍", fontSize = 18.sp)
            }
        }
    }
}
