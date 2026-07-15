package com.example.annotation.drawing

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.annotation.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 绘图引擎 - 管理所有绘图操作
 */
class DrawingEngine {

    // 当前绘图工具
    private val _currentTool = MutableStateFlow(DrawingTool.PEN)
    val currentTool: StateFlow<DrawingTool> = _currentTool.asStateFlow()

    // 画笔配置
    private val _penConfig = MutableStateFlow(PenConfig())
    val penConfig: StateFlow<PenConfig> = _penConfig.asStateFlow()

    // 荧光笔配置
    private val _highlighterConfig = MutableStateFlow(HighlighterConfig())
    val highlighterConfig: StateFlow<HighlighterConfig> = _highlighterConfig.asStateFlow()

    // 橡皮擦配置
    private val _eraserConfig = MutableStateFlow(EraserConfig())
    val eraserConfig: StateFlow<EraserConfig> = _eraserConfig.asStateFlow()

    // 自动折叠二级功能栏设置
    private val _autoCollapseToolbar = MutableStateFlow(false)
    val autoCollapseToolbar: StateFlow<Boolean> = _autoCollapseToolbar.asStateFlow()

    // 所有已完成的路径
    val paths = mutableStateListOf<DrawingPath>()

    // 当前正在绘制的路径
    private var currentPath: MutableList<PathPoint> = mutableListOf()

    // 当前橡皮擦位置（用于显示橡皮擦圆圈）
    private val _currentEraserPosition = MutableStateFlow<Offset?>(null)
    val currentEraserPosition: StateFlow<Offset?> = _currentEraserPosition.asStateFlow()

    private val _currentEraserSize = MutableStateFlow<Float?>(null)
    val currentEraserSize: StateFlow<Float?> = _currentEraserSize.asStateFlow()

    // 在一次橡皮擦划动中已经擦除的路径（避免重复擦除）
    private val erasedPathsInCurrentStroke = mutableSetOf<DrawingPath>()

    // 历史记录栈（用于撤销）
    // 新设计：historyStack 保存每次操作后的完整状态
    // 索引0是初始空白状态，索引1是第一次操作后的状态，依此类推
    private val historyStack = mutableListOf<List<DrawingPath>>()
    private val redoStack = mutableListOf<List<DrawingPath>>()
    private val maxHistorySize = 50
    private var eraserChangedCurrentStroke = false

    init {
        // 初始化：保存空白状态作为第一个历史记录
        historyStack.add(emptyList())
    }

    /**
     * 切换绘图工具
     */
    fun setTool(tool: DrawingTool) {
        _currentTool.value = tool
    }

    /**
     * 更新画笔配置（画笔无透明度）
     */
    fun updatePenConfig(
        color: Color? = null,
        strokeWidth: Float? = null
    ) {
        _penConfig.value = _penConfig.value.copy(
            color = color ?: _penConfig.value.color,
            strokeWidth = strokeWidth ?: _penConfig.value.strokeWidth
        )
    }

    /**
     * 更新荧光笔配置
     */
    fun updateHighlighterConfig(
        color: Color? = null,
        strokeWidth: Float? = null,
        alpha: Float? = null
    ) {
        _highlighterConfig.value = _highlighterConfig.value.copy(
            color = color ?: _highlighterConfig.value.color,
            strokeWidth = strokeWidth ?: _highlighterConfig.value.strokeWidth,
            alpha = alpha ?: _highlighterConfig.value.alpha
        )
    }

    /**
     * 更新橡皮擦大小
     */
    fun updateEraserSize(size: Float) {
        _eraserConfig.value = _eraserConfig.value.copy(size = size)
    }

    /**
     * 设置是否自动折叠二级功能栏
     */
    fun setAutoCollapseToolbar(autoCollapse: Boolean) {
        _autoCollapseToolbar.value = autoCollapse
    }

    /**
     * 开始绘制
     */
    fun startDrawing(offset: Offset, pressure: Float = 1f) {
        val normalizedPressure = normalizePressure(pressure)
        if (_currentTool.value == DrawingTool.ERASER) {
            // 橡皮擦模式：清空已擦除路径集合，开始新的擦除划动
            erasedPathsInCurrentStroke.clear()
            // 在开始擦除前，先保存当前状态（只保存一次）
            eraserChangedCurrentStroke = false
            _currentEraserSize.value = pressureAdjustedSize(_eraserConfig.value.size, normalizedPressure)
            _currentEraserPosition.value = offset
            erasePathsAt(offset, normalizedPressure)
        } else {
            // 画笔和荧光笔模式：正常绘制
            currentPath.clear()
            currentPath.add(PathPoint(offset, normalizedPressure))
        }
    }

    /**
     * 继续绘制
     */
    fun continueDrawing(offset: Offset, pressure: Float = 1f) {
        val normalizedPressure = normalizePressure(pressure)
        if (_currentTool.value == DrawingTool.ERASER) {
            // 橡皮擦模式：持续擦除
            _currentEraserSize.value = pressureAdjustedSize(_eraserConfig.value.size, normalizedPressure)
            _currentEraserPosition.value = offset
            erasePathsAt(offset, normalizedPressure)
        } else {
            // 画笔和荧光笔模式：正常绘制
            if (currentPath.isEmpty()) {
                startDrawing(offset, normalizedPressure)
            } else {
                currentPath.add(PathPoint(offset, normalizedPressure))
            }
        }
    }

    /**
     * 结束绘制
     */
    fun endDrawing() {
        if (_currentTool.value == DrawingTool.ERASER) {
            // 橡皮擦模式：清除橡皮擦位置指示和已擦除路径集合
            _currentEraserPosition.value = null
            _currentEraserSize.value = null
            erasedPathsInCurrentStroke.clear()
            if (eraserChangedCurrentStroke) saveToHistory()
            eraserChangedCurrentStroke = false
            return
        }

        if (currentPath.isEmpty()) return

        val path = when (_currentTool.value) {
            DrawingTool.PEN -> DrawingPath(
                tool = DrawingTool.PEN,
                points = currentPath.toList(),
                penConfig = _penConfig.value
            )
            DrawingTool.HIGHLIGHTER -> DrawingPath(
                tool = DrawingTool.HIGHLIGHTER,
                points = currentPath.toList(),
                highlighterConfig = _highlighterConfig.value
            )
            DrawingTool.ERASER -> {
                // 这个分支不应该被执行到，因为橡皮擦在上面已经处理了
                null
            }
        }

        path?.let {
            paths.add(it)
            // 添加路径后，保存新状态到历史记录
            saveToHistory()
        }
        currentPath.clear()
    }

    /**
     * 在指定位置擦除路径
     * 检测橡皮擦是否与任何路径相交，如果相交则删除整条路径
     */
    private fun erasePathsAt(eraserPosition: Offset, pressure: Float) {
        val eraserRadius = pressureAdjustedSize(_eraserConfig.value.size, pressure) / 2
        val pathsToRemove = mutableListOf<DrawingPath>()

        // 遍历所有路径，检查是否与橡皮擦相交（但跳过本次划动中已擦除的路径）
        paths.forEach { path ->
            if (!erasedPathsInCurrentStroke.contains(path) &&
                isPathIntersectingEraser(path, eraserPosition, eraserRadius)) {
                pathsToRemove.add(path)
                erasedPathsInCurrentStroke.add(path) // 标记为已擦除
            }
        }

        // 如果有路径被擦除，直接删除（历史已在 startDrawing 中保存）
        if (pathsToRemove.isNotEmpty()) {
            paths.removeAll(pathsToRemove)
            eraserChangedCurrentStroke = true
        }
    }

    /**
     * 检测路径是否与橡皮擦相交
     * 改进版：检测橡皮擦圆形与路径线段的距离
     */
    private fun isPathIntersectingEraser(
        path: DrawingPath,
        eraserPosition: Offset,
        eraserRadius: Float
    ): Boolean {
        val points = path.points
        if (points.isEmpty()) return false

        // 检查第一个点
        val firstEffectiveRadius = eraserRadius + strokeWidthAt(path, points[0]) / 2
        if (distanceToPoint(eraserPosition, points[0].offset) <= firstEffectiveRadius) {
            return true
        }

        // 检查所有线段
        for (i in 1 until points.size) {
            val p1 = points[i - 1].offset
            val p2 = points[i].offset
            val segmentWidth = maxOf(
                strokeWidthAt(path, points[i - 1]),
                strokeWidthAt(path, points[i])
            )
            val effectiveRadius = eraserRadius + segmentWidth / 2

            if (distanceToLineSegment(eraserPosition, p1, p2) <= effectiveRadius) {
                return true
            }
        }

        return false
    }

    private fun strokeWidthAt(path: DrawingPath, point: PathPoint): Float {
        val baseWidth = when (path.tool) {
            DrawingTool.PEN -> path.penConfig?.strokeWidth ?: 5f
            DrawingTool.HIGHLIGHTER -> path.highlighterConfig?.strokeWidth ?: 20f
            DrawingTool.ERASER -> 0f
        }
        return pressureAdjustedSize(baseWidth, point.pressure)
    }

    /**
     * 计算点到点的距离
     */
    private fun distanceToPoint(p1: Offset, p2: Offset): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    /**
     * 计算点到线段的最短距离
     */
    private fun distanceToLineSegment(point: Offset, lineStart: Offset, lineEnd: Offset): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val lengthSquared = dx * dx + dy * dy

        // 如果线段长度为0（起点和终点重合），返回点到起点的距离
        if (lengthSquared < 0.0001f) {
            return distanceToPoint(point, lineStart)
        }

        // 计算点在线段上的投影参数 t
        val t = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / lengthSquared

        // 将 t 限制在 [0, 1] 范围内
        val clampedT = t.coerceIn(0f, 1f)

        // 计算线段上距离点最近的点
        val closestX = lineStart.x + clampedT * dx
        val closestY = lineStart.y + clampedT * dy

        // 返回点到最近点的距离
        val distX = point.x - closestX
        val distY = point.y - closestY
        return kotlin.math.sqrt(distX * distX + distY * distY)
    }

    /**
     * 撤销上一步操作
     * 新逻辑：historyStack 始终保存完整的状态快照
     * 每次撤销都恢复到上一个状态
     */
    fun undo() {
        // historyStack 至少包��初始空白状态
        // 如果只剩一个状态（空白状态），则无法继续撤销
        if (historyStack.size <= 1) {
            return
        }

        // 移除当前状态
        redoStack.add(historyStack.removeAt(historyStack.size - 1))

        // 恢复到上一个状态（historyStack 中的最后一个）
        val previousState = historyStack.last()
        paths.clear()
        paths.addAll(previousState)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val nextState = redoStack.removeAt(redoStack.size - 1)
        historyStack.add(nextState)
        paths.clear()
        paths.addAll(nextState)
    }

    /**
     * 清空所有绘制内容
     */
    fun clearAll() {
        // 清空前保存当前状态
        if (paths.isEmpty()) return
        paths.clear()
        currentPath.clear()
        // 清空后也保存新的空白状态
        saveToHistory()
    }

    /**
     * 保存当前状态到历史记录
     * 新逻辑：保存当前 paths 的完整快照
     */
    private fun saveToHistory() {
        val snapshot = paths.toList()
        if (historyStack.lastOrNull() == snapshot) return
        redoStack.clear()
        // 限制历史记录数量
        if (historyStack.size >= maxHistorySize) {
            historyStack.removeAt(0)
        }
        // 保存当前状态的深拷贝
        historyStack.add(snapshot)
    }

    fun cancelDrawing() {
        currentPath.clear()
        _currentEraserPosition.value = null
        _currentEraserSize.value = null
        erasedPathsInCurrentStroke.clear()
        eraserChangedCurrentStroke = false
    }

    /**
     * 获取当前正在绘制的路径
     */
    fun getCurrentDrawingPath(): List<PathPoint> = currentPath.toList()

    /**
     * 计算压感调整后的笔画宽度
     */
    fun calculatePressureAdjustedWidth(baseWidth: Float, pressure: Float): Float {
        return pressureAdjustedSize(baseWidth, pressure)
    }

    /**
     * 计算压感调整后的颜色透明度
     */
    fun calculatePressureAdjustedAlpha(baseAlpha: Float, pressure: Float): Float {
        // 轻触时透明度降低，重压时接近完全不透明
        val minScale = 0.5f
        val maxScale = 1.0f
        val scale = minScale + (maxScale - minScale) * normalizePressure(pressure)
        return baseAlpha * scale
    }

    private fun normalizePressure(pressure: Float): Float =
        pressure.takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 1f
}
