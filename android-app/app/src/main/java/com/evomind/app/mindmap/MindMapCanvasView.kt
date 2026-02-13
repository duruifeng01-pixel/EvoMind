package com.evomind.app.mindmap

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.graphics.withTranslation
import com.evomind.app.aigc.model.MindMapNode

/**
 * 交互式思维导图Canvas视图
 * 支持缩放、拖拽、点击、搜索定位
 */
class MindMapCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val MAX_SCALE = 5f
        private const val MIN_SCALE = 0.3f
        private const val DEFAULT_SCALE = 1f
        private const val NODE_PADDING = 16f
        private const val LEVEL_WIDTH = 200f
        private const val NODE_VERTICAL_SPACING = 10f
        private const val TEXT_OFFSET = 24f
    }

    private var rootNode: MindMapNode? = null
    private var nodePositions = mutableMapOf<String, RectF>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        color = Color.BLACK
    }
    private val connectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFCCCCCC.toInt()
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private var scale = DEFAULT_SCALE
    private var offsetX = 0f
    private var offsetY = 0f
    private val gestureDetector: GestureDetector
    private val scaleGestureDetector: ScaleGestureDetector

    private var searchResults = mutableListOf<String>()
    private var focusedNodeId: String? = null
    private var searchText = ""

    var onNodeClickListener: OnNodeClickListener? = null
    var onNodeLongClickListener: OnNodeLongClickListener? = null

    interface OnNodeClickListener {
        fun onNodeClick(node: MindMapNode)
    }

    interface OnNodeLongClickListener {
        fun onNodeLongClick(node: MindMapNode): Boolean
    }

    init {
        gestureDetector = GestureDetector(context, GestureListener())
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.withTranslation(offsetX, offsetY) {
            scale(scale, scale)
            rootNode?.let { root ->
                drawConnections(canvas, root)
                drawNode(canvas, root, 0f, height / (2 * scale))
            }
        }
    }

    private fun drawConnections(canvas: Canvas, node: MindMapNode) {
        nodePositions[node.id]?.let { nodeRect ->
            node.children.forEach { child ->
                nodePositions[child.id]?.let { childRect ->
                    val path = Path().apply {
                        moveTo(nodeRect.right, nodeRect.centerY())
                        cubicTo(
                            nodeRect.right + LEVEL_WIDTH / 2, nodeRect.centerY(),
                            childRect.left - LEVEL_WIDTH / 2, childRect.centerY(),
                            childRect.left, childRect.centerY()
                        )
                    }
                    canvas.drawPath(path, connectionPaint)
                    drawConnections(canvas, child)
                }
            }
        }
    }

    private fun drawNode(canvas: Canvas, node: MindMapNode, x: Float, y: Float): RectF {
        val textWidth = textPaint.measureText(node.text)
        val nodeWidth = textWidth + NODE_PADDING * 2
        val nodeHeight = textPaint.textSize + NODE_PADDING * 2
        val nodeY = y - nodeHeight / 2

        val nodeRect = RectF(x, nodeY, x + nodeWidth, nodeY + nodeHeight)
        nodePositions[node.id] = nodeRect

        val backgroundColor = when {
            searchResults.contains(node.id) && node.id == focusedNodeId -> Color.YELLOW
            searchResults.contains(node.id) -> Color.LTGRAY
            node.id == focusedNodeId -> Color.CYAN
            else -> 0xFFE3F2FD.toInt()
        }

        paint.color = backgroundColor
        canvas.drawRoundRect(nodeRect, 12f, 12f, paint)
        paint.color = 0xFF1976D2.toInt()
        canvas.drawRoundRect(nodeRect, 12f, 12f, paint)

        textPaint.color = Color.BLACK
        canvas.drawText(node.text, nodeRect.left + NODE_PADDING, nodeRect.top + NODE_PADDING + TEXT_OFFSET, textPaint)

        var childY = nodeY
        node.children.forEach { child ->
            drawNode(canvas, child, x + LEVEL_WIDTH, childY)
            childY += nodeHeight + NODE_VERTICAL_SPACING
        }

        return nodeRect
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = gestureDetector.onTouchEvent(event)
        handled = scaleGestureDetector.onTouchEvent(event) || handled

        if (!handled) {
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    offsetX += event.x - lastTouchX
                    offsetY += event.y - lastTouchY
                    invalidate()
                }
            }
            lastTouchX = event.x
            lastTouchY = event.y
        }
        return true
    }

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val x = (e.x - offsetX) / scale
            val y = (e.y - offsetY) / scale
            val clickedNode = findNodeAt(x, y)

            clickedNode?.let { node ->
                onNodeClickListener?.onNodeClick(node)
                focusedNodeId = node.id
                invalidate()
            }
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            val x = (e.x - offsetX) / scale
            val y = (e.y - offsetY) / scale
            val longPressedNode = findNodeAt(x, y)

            longPressedNode?.let { node ->
                onNodeLongClickListener?.onNodeLongClick(node)
            }
        }
    }

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var focusX = 0f
        private var focusY = 0f

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            focusX = detector.focusX
            focusY = detector.focusY
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val newScale = (scale * detector.scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
            if (newScale != scale) {
                offsetX += focusX - (focusX - offsetX) * newScale / scale
                offsetY += focusY - (focusY - offsetY) * newScale / scale
                scale = newScale
                invalidate()
            }
            return true
        }
    }

    private fun findNodeAt(x: Float, y: Float): MindMapNode? {
        nodePositions.forEach { (nodeId, rect) ->
            if (rect.contains(x, y)) {
                return rootNode?.findNodeById(nodeId)
            }
        }
        return null
    }

    fun setMindMap(node: MindMapNode) {
        rootNode = node
        nodePositions.clear()
        invalidate()
    }

    fun resetView() {
        scale = DEFAULT_SCALE
        offsetX = 0f
        offsetY = 0f
        clearSearch()
    }

    fun search(query: String): List<MindMapNode> {
        if (query.isBlank()) {
            clearSearch()
            return emptyList()
        }

        searchText = query
        searchResults.clear()
        val results = mutableListOf<MindMapNode>()
        val lowerQuery = query.lowercase()

        rootNode?.let { root ->
            searchInNode(root, lowerQuery, results)
        }

        searchResults.addAll(results.map { it.id })
        focusedNodeId = searchResults.firstOrNull()
        focusedNodeId?.let { focusNode(it) }
        invalidate()
        return results
    }

    private fun searchInNode(node: MindMapNode, query: String, results: MutableList<MindMapNode>) {
        if (node.text.lowercase().contains(query)) {
            results.add(node)
        }
        node.children.forEach { child ->
            searchInNode(child, query, results)
        }
    }

    fun clearSearch() {
        searchText = ""
        searchResults.clear()
        focusedNodeId = null
        invalidate()
    }

    fun focusNode(nodeId: String) {
        val nodeRect = nodePositions[nodeId] ?: return
        focusedNodeId = nodeId

        val targetX = width / 2f - nodeRect.centerX() * scale
        val targetY = height / 2f - nodeRect.centerY() * scale
        offsetX = targetX
        offsetY = targetY
        invalidate()
    }

    fun nextSearchResult(): Boolean {
        if (searchResults.isEmpty()) return false
        val currentIndex = searchResults.indexOf(focusedNodeId)
        val nextIndex = (currentIndex + 1) % searchResults.size
        focusedNodeId = searchResults[nextIndex]
        focusedNodeId?.let { focusNode(it) }
        return true
    }

    fun previousSearchResult(): Boolean {
        if (searchResults.isEmpty()) return false
        val currentIndex = searchResults.indexOf(focusedNodeId)
        val prevIndex = if (currentIndex <= 0) searchResults.size - 1 else currentIndex - 1
        focusedNodeId = searchResults[prevIndex]
        focusedNodeId?.let { focusNode(it) }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (offsetX == 0f && offsetY == 0f) {
            rootNode?.let {
                offsetX = width / 2f - LEVEL_WIDTH
                offsetY = height / 2f
                invalidate()
            }
        }
    }
}
