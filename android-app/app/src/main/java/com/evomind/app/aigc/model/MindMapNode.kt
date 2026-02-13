package com.evomind.app.aigc.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 思维导图节点（树形结构）
 * 支持下钻阅读和交互操作
 */
data class MindMapNode(
    /**
     * 节点ID
     */
    val id: String,

    /**
     * 节点文本
     */
    val text: String,

    /**
     * 节点层级（0=根节点）
     */
    val level: Int = 0,

    /**
     * 子节点
     */
    val children: List<MindMapNode> = emptyList(),

    /**
     * 节点附加数据（如详细内容、链接等）
     */
    val data: Map<String, Any> = emptyMap(),

    /**
     * 对应的原文段落引用
     * 格式: startIndex-endIndex，表示该节点对应的原文段落位置
     * 例如: "0-200" 表示对应原文的前200个字符
     */
    val sourceParagraphRef: String? = null,

    /**
     * 节点样式
     */
    val style: NodeStyle = NodeStyle()
) {
    /**
     * 是否为叶节点
     */
    val isLeaf: Boolean
        get() = children.isEmpty()

    /**
     * 获取所有后代节点数量
     */
    val totalDescendants: Int
        get() = children.size + children.sumOf { it.totalDescendants }

    /**
     * 根据ID查找节点
     */
    fun findNodeById(id: String): MindMapNode? {
        if (this.id == id) return this
        return children.firstNotNullOfOrNull { it.findNodeById(id) }
    }

    /**
     * 根据路径查找节点
     */
    fun findNodeByPath(path: List<Int>): MindMapNode? {
        var current: MindMapNode? = this
        for (index in path) {
            current = current?.children?.getOrNull(index) ?: return null
        }
        return current
    }

    /**
     * 获取节点路径（从根到当前节点）
     */
    fun getPathToNode(targetId: String, currentPath: List<Int> = emptyList()): List<Int>? {
        if (this.id == targetId) return currentPath

        children.forEachIndexed { index, child ->
            val path = child.getPathToNode(targetId, currentPath + index)
            if (path != null) return path
        }
        return null
    }

    /**
     * 转换为扁平列表（用于UI展示）
     */
    fun flatten(): List<FlatNode> {
        val result = mutableListOf<FlatNode>()
        flattenInternal(result, emptyList())
        return result
    }

    private fun flattenInternal(result: MutableList<FlatNode>, parentPath: List<Int>) {
        result.add(FlatNode(this, parentPath))
        children.forEachIndexed { index, child ->
            child.flattenInternal(result, parentPath + index)
        }
    }

    companion object {
        /**
         * 从Markdown解析思维导图
         * 格式：
         * # 根节点
         * ## 子节点1
         * ### 孙节点1.1
         * ## 子节点2
         */
        fun fromMarkdown(markdown: String): MindMapNode? {
            if (markdown.isBlank()) return null

            val lines = markdown.trim().lines()
            if (lines.isEmpty()) return null

            // 创建根节点
            val rootText = lines.first().trimStart('#', ' ')
            val root = MindMapNode(
                id = "0",
                text = rootText,
                level = 0
            )

            // 构建节点栈
            val stack = mutableListOf(root)

            // 从第2行开始解析
            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isBlank()) continue

                // 计算层级 (#号的数量)
                val level = line.takeWhile { it == '#' }.count()
                if (level == 0) continue

                val text = line.trimStart('#', ' ')
                val node = MindMapNode(
                    id = "$i",
                    text = text,
                    level = level
                )

                // 找到父节点（level-1的最近节点）
                val parent = stack.lastOrNull { it.level == level - 1 }
                if (parent != null) {
                    // 更新父节点的子节点
                    val updatedChildren = parent.children + node
                    val parentIndex = stack.indexOf(parent)
                    val newParent = parent.copy(children = updatedChildren)
                    stack[parentIndex] = newParent

                    // 更新栈
                    stack.removeAll { it.level >= level }
                    stack.add(newParent)
                    stack.add(node)
                }
            }

            return stack.firstOrNull { it.level == 0 }
        }

        /**
         * 从JSON字符串反序列化
         */
        fun fromJson(json: String): MindMapNode? {
            return try {
                val gson = Gson()
                gson.fromJson(json, MindMapNode::class.java)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * 序列化为JSON字符串
         */
        fun toJson(node: MindMapNode): String {
            val gson = Gson()
            return gson.toJson(node)
        }

        /**
         * 创建示例思维导图
         */
        fun createSample(): MindMapNode {
            return MindMapNode(
                id = "root",
                text = "认知卡片学习法",
                level = 0,
                children = listOf(
                    MindMapNode(
                        id = "1",
                        text = "核心理念",
                        level = 1,
                        children = listOf(
                            MindMapNode(
                                id = "1.1",
                                text = "主动回忆",
                                level = 2,
                                data = mapOf("content" to "通过主动回忆强化记忆")
                            ),
                            MindMapNode(
                                id = "1.2",
                                text = "间隔重复",
                                level = 2,
                                data = mapOf("content" to "按照遗忘曲线安排复习")
                            )
                        )
                    ),
                    MindMapNode(
                        id = "2",
                        text = "制作步骤",
                        level = 1,
                        children = listOf(
                            MindMapNode(
                                id = "2.1",
                                text = "提炼要点",
                                level = 2
                            ),
                            MindMapNode(
                                id = "2.2",
                                text = "设计问题",
                                level = 2
                            )
                        )
                    )
                )
            )
        }
    }

    /**
     * 节点样式
     */
    data class NodeStyle(
        val color: String = "#4CAF50",
        val backgroundColor: String = "#E8F5E8",
        val fontSize: Float = 14f,
        val isBold: Boolean = level < 2,
        val shape: NodeShape = NodeShape.ROUNDED_RECT
    )

    enum class NodeShape {
        CIRCLE,
        RECT,
        ROUNDED_RECT
    }

    /**
     * 扁平化节点（用于UI展示）
     */
    data class FlatNode(
        val node: MindMapNode,
        val parentPath: List<Int>
    ) {
        val depth: Int = parentPath.size
    }
}

/**
 * 思维导图下钻状态
 */
enum class DrillDownState {
    COLLAPSED,      // 折叠
    EXPANDED,       // 展开
    LOADING,        // 加载中
    DETAIL_VIEW     // 详情视图
}

/**
 * 下钻阅读的事件
 */
sealed class DrillDownEvent {
    data class NodeClicked(val nodeId: String) : DrillDownEvent()
    data class NodeExpanded(val nodeId: String) : DrillDownEvent()
    data class NodeCollapsed(val nodeId: String) : DrillDownEvent()
    data class LoadDetail(val nodeId: String) : DrillDownEvent()
    object BackClicked : DrillDownEvent()
}

/**
 * 节点详情数据
 */
data class NodeDetail(
    val nodeId: String,
    val title: String,
    val content: String,
    val relatedNodes: List<String> = emptyList(),
    val externalLinks: List<String> = emptyList()
)
