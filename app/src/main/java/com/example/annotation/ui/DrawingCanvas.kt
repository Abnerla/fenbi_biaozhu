package com.example.annotation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import com.example.annotation.drawing.DrawingEngine
import com.example.annotation.model.DrawingTool

/**
 * 绘图画布组件
 */
@Composable
fun DrawingCanvas(
    drawingEngine: DrawingEngine,
    modifier: Modifier = Modifier,
    onDoubleFingerTap: () -> Unit = {},
    onDrawingStart: () -> Unit = {}
) {
    val currentTool by drawingEngine.currentTool.collectAsState()
    val penConfig by drawingEngine.penConfig.collectAsState()
    val highlighterConfig by drawingEngine.highlighterConfig.collectAsState()
    val eraserConfig by drawingEngine.eraserConfig.collectAsState()
    val eraserPosition by drawingEngine.currentEraserPosition.collectAsState()

    var currentDrawingPath by remember { mutableStateOf<List<com.example.annotation.model.PathPoint>>(emptyList()) }

    // 双指双击检测状态
    var twoFingerTapCount by remember { mutableStateOf(0) }
    var lastTwoFingerTapTime by remember { mutableStateOf(0L) }
    var lastPointerCount by remember { mutableStateOf(0) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val pressure = 1f // 默认压感，将在后续优化中处理真实压感
                        drawingEngine.startDrawing(offset, pressure)
                        currentDrawingPath = drawingEngine.getCurrentDrawingPath()
                        // 触发绘制开始回调
                        onDrawingStart()
                    },
                    onDrag = { change: PointerInputChange, _ ->
                        val pressure = change.pressure
                        drawingEngine.continueDrawing(change.position, pressure)
                        currentDrawingPath = drawingEngine.getCurrentDrawingPath()
                        change.consume()
                    },
                    onDragEnd = {
                        drawingEngine.endDrawing()
                        currentDrawingPath = emptyList()
                    }
                )
            }
            .pointerInput(Unit) {
                // 检测双指双击
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val currentPointerCount = event.changes.size

                        // 检测从0或1个手指变为2个手指（双指按下）
                        if (lastPointerCount < 2 && currentPointerCount == 2) {
                            val currentTime = System.currentTimeMillis()

                            // 如果距离上次双指按下不到500ms，说明是第二次点击
                            if (currentTime - lastTwoFingerTapTime < 500) {
                                twoFingerTapCount++

                                // 如果是第二次双指点击，触发撤销
                                if (twoFingerTapCount >= 2) {
                                    onDoubleFingerTap()
                                    twoFingerTapCount = 0
                                    lastTwoFingerTapTime = 0L
                                }
                            } else {
                                // 重新开始计数
                                twoFingerTapCount = 1
                                lastTwoFingerTapTime = currentTime
                            }
                        }

                        // 如果超过500ms没有第二次点击，重置计数
                        if (twoFingerTapCount > 0 && System.currentTimeMillis() - lastTwoFingerTapTime > 500) {
                            twoFingerTapCount = 0
                            lastTwoFingerTapTime = 0L
                        }

                        lastPointerCount = currentPointerCount
                    }
                }
            }
    ) {
        // 绘制所有已完成的路径
        drawingEngine.paths.forEach { path ->
            when (path.tool) {
                DrawingTool.PEN -> {
                    path.penConfig?.let { config ->
                        drawPath(
                            path = path,
                            color = config.color,
                            baseWidth = config.strokeWidth,
                            alpha = 1f, // 画笔完全不透明
                            drawingEngine = drawingEngine,
                            isAntiAlias = true
                        )
                    }
                }
                DrawingTool.HIGHLIGHTER -> {
                    path.highlighterConfig?.let { config ->
                        drawPath(
                            path = path,
                            color = config.color,
                            baseWidth = config.strokeWidth,
                            alpha = config.alpha,
                            drawingEngine = drawingEngine,
                            isAntiAlias = true,
                            blendMode = BlendMode.SrcOver // 使用正常混合模式以在透明背景上显示
                        )
                    }
                }
                DrawingTool.ERASER -> {
                    // 橡皮擦不再保存为路径，所以这个分支不会被执行
                }
            }
        }

        // 绘制当前正在绘制的路径（仅画笔和荧光笔）
        if (currentDrawingPath.isNotEmpty() && currentTool != DrawingTool.ERASER) {
            val tempPath = when (currentTool) {
                DrawingTool.PEN -> com.example.annotation.model.DrawingPath(
                    tool = DrawingTool.PEN,
                    points = currentDrawingPath,
                    penConfig = penConfig
                )
                DrawingTool.HIGHLIGHTER -> com.example.annotation.model.DrawingPath(
                    tool = DrawingTool.HIGHLIGHTER,
                    points = currentDrawingPath,
                    highlighterConfig = highlighterConfig
                )
                DrawingTool.ERASER -> null
            }

            tempPath?.let { path ->
                when (currentTool) {
                    DrawingTool.PEN -> {
                        drawPath(
                            path = path,
                            color = penConfig.color,
                            baseWidth = penConfig.strokeWidth,
                            alpha = 1f, // 画笔完全不透明
                            drawingEngine = drawingEngine,
                            isAntiAlias = true
                        )
                    }
                    DrawingTool.HIGHLIGHTER -> {
                        drawPath(
                            path = path,
                            color = highlighterConfig.color,
                            baseWidth = highlighterConfig.strokeWidth,
                            alpha = highlighterConfig.alpha,
                            drawingEngine = drawingEngine,
                            isAntiAlias = true,
                            blendMode = BlendMode.SrcOver // 使用正常混合模式以在透明背景上显示
                        )
                    }
                    DrawingTool.ERASER -> {
                        // 不绘制橡皮擦路径
                    }
                }
            }
        }

        // 绘制橡皮擦圆圈指示器
        eraserPosition?.let { position ->
            val radius = eraserConfig.size / 2
            drawCircle(
                color = Color.Gray.copy(alpha = 0.5f),
                radius = radius,
                center = position,
                style = Stroke(width = 2f)
            )
            // 绘制十字准星
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(position.x - 10f, position.y),
                end = Offset(position.x + 10f, position.y),
                strokeWidth = 1f
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(position.x, position.y - 10f),
                end = Offset(position.x, position.y + 10f),
                strokeWidth = 1f
            )
        }
    }
}

/**
 * 绘制路径的扩展函数
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPath(
    path: com.example.annotation.model.DrawingPath,
    color: Color,
    baseWidth: Float,
    alpha: Float,
    drawingEngine: DrawingEngine,
    isAntiAlias: Boolean = true,
    blendMode: BlendMode = BlendMode.SrcOver
) {
    if (path.points.size < 2) return

    val drawPath = Path()
    val firstPoint = path.points.first()
    drawPath.moveTo(firstPoint.offset.x, firstPoint.offset.y)

    // 使用贝塞尔曲线平滑路径
    for (i in 1 until path.points.size) {
        val prevPoint = path.points[i - 1]
        val currentPoint = path.points[i]

        if (i < path.points.size - 1) {
            val nextPoint = path.points[i + 1]
            val controlPoint = Offset(
                (prevPoint.offset.x + currentPoint.offset.x) / 2,
                (prevPoint.offset.y + currentPoint.offset.y) / 2
            )
            drawPath.quadraticTo(
                prevPoint.offset.x,
                prevPoint.offset.y,
                controlPoint.x,
                controlPoint.y
            )
        } else {
            drawPath.lineTo(currentPoint.offset.x, currentPoint.offset.y)
        }
    }

    // 使用基础宽度，不再应用压感影响（用户期望看到设定的固定粗细）
    val adjustedWidth = baseWidth

    // 使用固定透明度，不再受压感影响
    val adjustedAlpha = alpha

    drawPath(
        path = drawPath,
        color = color.copy(alpha = adjustedAlpha),
        style = Stroke(
            width = adjustedWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        ),
        blendMode = blendMode
    )
}
