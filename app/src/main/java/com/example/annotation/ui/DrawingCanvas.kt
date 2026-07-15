package com.example.annotation.ui

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.annotation.drawing.DrawingEngine
import com.example.annotation.model.*
import com.example.annotation.utils.PreferencesManager
import com.example.annotation.utils.StylusPressure
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

private class CanvasInputState {
    var drawing = false
    var maxPointerCount = 0
    var downTime = 0L
    var startCentroid = Offset.Zero
    var lastCentroid = Offset.Zero
    var rawOffset = Offset.Zero

    fun reset() {
        drawing = false
        maxPointerCount = 0
        downTime = 0L
        startCentroid = Offset.Zero
        lastCentroid = Offset.Zero
        rawOffset = Offset.Zero
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingCanvas(
    drawingEngine: DrawingEngine,
    modifier: Modifier = Modifier,
    preferencesManager: PreferencesManager? = null,
    onTwoFingerTap: () -> Unit = {},
    onThreeFingerTap: () -> Unit = {},
    onTwoFingerSwipe: (Offset, Offset, Long) -> Unit = { _, _, _ -> },
    onDrawingStart: () -> Unit = {}
) {
    val currentTool by drawingEngine.currentTool.collectAsState()
    val penConfig by drawingEngine.penConfig.collectAsState()
    val highlighterConfig by drawingEngine.highlighterConfig.collectAsState()
    val eraserConfig by drawingEngine.eraserConfig.collectAsState()
    val eraserPosition by drawingEngine.currentEraserPosition.collectAsState()
    val currentEraserSize by drawingEngine.currentEraserSize.collectAsState()
    var currentDrawingPath by remember { mutableStateOf<List<PathPoint>>(emptyList()) }
    val inputState = remember { CanvasInputState() }
    val density = LocalDensity.current
    val tapSlop = with(density) { 24.dp.toPx() }
    val swipeThreshold = with(density) { 48.dp.toPx() }
    fun centroid(event: MotionEvent): Offset {
        var x = 0f
        var y = 0f
        for (index in 0 until event.pointerCount) {
            x += event.getX(index)
            y += event.getY(index)
        }
        return Offset(x / event.pointerCount, y / event.pointerCount)
    }

    fun pressure(event: MotionEvent, historyPosition: Int? = null): Float {
        val reportedPressure = if (historyPosition == null) {
            event.getPressure(0)
        } else {
            event.getHistoricalPressure(0, historyPosition)
        }
        return StylusPressure.resolve(
            stylusEnabled = preferencesManager?.getStylusEnabled() == true,
            toolType = event.getToolType(0),
            reportedPressure = reportedPressure
        )
    }

    fun handleMotionEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                inputState.reset()
                inputState.maxPointerCount = 1
                inputState.downTime = event.eventTime
                inputState.startCentroid = Offset(event.x, event.y)
                inputState.lastCentroid = inputState.startCentroid
                inputState.rawOffset = Offset(event.rawX - event.x, event.rawY - event.y)
                inputState.drawing = true
                drawingEngine.startDrawing(Offset(event.x, event.y), pressure(event))
                currentDrawingPath = drawingEngine.getCurrentDrawingPath()
                onDrawingStart()
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                inputState.maxPointerCount = maxOf(inputState.maxPointerCount, event.pointerCount)
                if (inputState.drawing) {
                    drawingEngine.cancelDrawing()
                    currentDrawingPath = emptyList()
                    inputState.drawing = false
                }
                val center = centroid(event)
                if (inputState.maxPointerCount == event.pointerCount) inputState.startCentroid = center
                inputState.lastCentroid = center
            }

            MotionEvent.ACTION_MOVE -> {
                inputState.maxPointerCount = maxOf(inputState.maxPointerCount, event.pointerCount)
                if (inputState.maxPointerCount >= 2) {
                    inputState.lastCentroid = centroid(event)
                } else if (inputState.drawing) {
                    for (historyPosition in 0 until event.historySize) {
                        drawingEngine.continueDrawing(
                            Offset(
                                event.getHistoricalX(0, historyPosition),
                                event.getHistoricalY(0, historyPosition)
                            ),
                            pressure(event, historyPosition)
                        )
                    }
                    drawingEngine.continueDrawing(
                        Offset(event.x, event.y),
                        pressure(event)
                    )
                    currentDrawingPath = drawingEngine.getCurrentDrawingPath()
                }
            }

            MotionEvent.ACTION_UP -> {
                val duration = (event.eventTime - inputState.downTime).coerceAtLeast(1L)
                if (inputState.maxPointerCount == 1 && inputState.drawing) {
                    drawingEngine.endDrawing()
                    currentDrawingPath = emptyList()
                } else {
                    val distance = hypot(
                        inputState.lastCentroid.x - inputState.startCentroid.x,
                        inputState.lastCentroid.y - inputState.startCentroid.y
                    )
                    val isTap = duration <= 320L && distance <= tapSlop
                    when {
                        inputState.maxPointerCount == 2 && isTap &&
                            preferencesManager?.getTwoFingerTapUndoEnabled() != false -> onTwoFingerTap()

                        inputState.maxPointerCount == 3 && isTap &&
                            preferencesManager?.getThreeFingerTapRedoEnabled() != false -> onThreeFingerTap()

                        inputState.maxPointerCount == 2 && distance >= swipeThreshold &&
                            preferencesManager?.getTwoFingerPageMoveEnabled() == true -> {
                            onTwoFingerSwipe(
                                inputState.startCentroid + inputState.rawOffset,
                                inputState.lastCentroid + inputState.rawOffset,
                                duration
                            )
                        }
                    }
                }
                inputState.reset()
            }

            MotionEvent.ACTION_CANCEL -> {
                drawingEngine.cancelDrawing()
                currentDrawingPath = emptyList()
                inputState.reset()
            }
        }
        return true
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInteropFilter { event -> handleMotionEvent(event) }
    ) {
        drawingEngine.paths.forEach { path ->
            when (path.tool) {
                DrawingTool.PEN -> path.penConfig?.let { config ->
                    drawAnnotationPath(path, config.color, config.strokeWidth, 1f)
                }
                DrawingTool.HIGHLIGHTER -> path.highlighterConfig?.let { config ->
                    drawAnnotationPath(path, config.color, config.strokeWidth, config.alpha)
                }
                DrawingTool.ERASER -> Unit
            }
        }

        if (currentDrawingPath.isNotEmpty() && currentTool != DrawingTool.ERASER) {
            val temporaryPath = when (currentTool) {
                DrawingTool.PEN -> DrawingPath(DrawingTool.PEN, currentDrawingPath, penConfig = penConfig)
                DrawingTool.HIGHLIGHTER -> DrawingPath(
                    DrawingTool.HIGHLIGHTER,
                    currentDrawingPath,
                    highlighterConfig = highlighterConfig
                )
                DrawingTool.ERASER -> null
            }
            temporaryPath?.let { path ->
                when (currentTool) {
                    DrawingTool.PEN -> drawAnnotationPath(path, penConfig.color, penConfig.strokeWidth, 1f)
                    DrawingTool.HIGHLIGHTER -> drawAnnotationPath(
                        path,
                        highlighterConfig.color,
                        highlighterConfig.strokeWidth,
                        highlighterConfig.alpha
                    )
                    DrawingTool.ERASER -> Unit
                }
            }
        }

        eraserPosition?.let { position ->
            val radius = (currentEraserSize ?: eraserConfig.size) / 2
            drawCircle(Color.Gray.copy(alpha = 0.5f), radius, position, style = Stroke(width = 2f))
            drawLine(Color.Gray.copy(alpha = 0.5f), Offset(position.x - 10f, position.y), Offset(position.x + 10f, position.y), 1f)
            drawLine(Color.Gray.copy(alpha = 0.5f), Offset(position.x, position.y - 10f), Offset(position.x, position.y + 10f), 1f)
        }
    }
}

private fun DrawScope.drawAnnotationPath(
    drawingPath: DrawingPath,
    color: Color,
    width: Float,
    alpha: Float
) {
    val points = drawingPath.points
    if (points.isEmpty()) return

    val drawColor = color.copy(alpha = alpha)
    if (points.size == 1) {
        drawCircle(
            color = drawColor,
            radius = pressureAdjustedSize(width, points.first().pressure) / 2,
            center = points.first().offset,
            blendMode = BlendMode.SrcOver
        )
        return
    }

    val minPressure = points.minOf(PathPoint::pressure)
    val maxPressure = points.maxOf(PathPoint::pressure)
    if (maxPressure - minPressure > 0.01f) {
        drawVariableWidthPath(points, drawColor, width)
        return
    }

    val path = Path().apply {
        val first = points.first().offset
        moveTo(first.x, first.y)
        for (index in 1 until points.size) {
            val previous = points[index - 1].offset
            val current = points[index].offset
            if (index < points.lastIndex) {
                quadraticTo(
                    previous.x,
                    previous.y,
                    (previous.x + current.x) / 2,
                    (previous.y + current.y) / 2
                )
            } else {
                lineTo(current.x, current.y)
            }
        }
    }
    drawPath(
        path = path,
        color = drawColor,
        style = Stroke(
            width = pressureAdjustedSize(width, points.first().pressure),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        ),
        blendMode = BlendMode.SrcOver
    )
}

private fun DrawScope.drawVariableWidthPath(
    points: List<PathPoint>,
    color: Color,
    baseWidth: Float
) {
    val samples = mutableListOf(
        StrokeSample(points.first().offset, pressureAdjustedSize(baseWidth, points.first().pressure))
    )
    var sampleStart = points.first().offset
    var sampleStartPressure = points.first().pressure

    for (index in 1 until points.size) {
        val control = points[index - 1]
        val current = points[index]
        val segmentEnd = if (index < points.lastIndex) {
            Offset(
                (control.offset.x + current.offset.x) / 2,
                (control.offset.y + current.offset.y) / 2
            )
        } else {
            current.offset
        }
        val segmentEndPressure = if (index < points.lastIndex) {
            (control.pressure + current.pressure) / 2
        } else {
            current.pressure
        }
        val estimatedLength = (sampleStart - control.offset).getDistance() +
            (control.offset - segmentEnd).getDistance()
        val sampleCount = ceil(estimatedLength / 2f).toInt().coerceIn(1, 32)

        for (sample in 1..sampleCount) {
            val t = sample.toFloat() / sampleCount
            val inverseT = 1f - t
            val sampledOffset = Offset(
                inverseT * inverseT * sampleStart.x +
                    2f * inverseT * t * control.offset.x +
                    t * t * segmentEnd.x,
                inverseT * inverseT * sampleStart.y +
                    2f * inverseT * t * control.offset.y +
                    t * t * segmentEnd.y
            )
            val sampledPressure = sampleStartPressure +
                (segmentEndPressure - sampleStartPressure) * t
            val sampledWidth = pressureAdjustedSize(baseWidth, sampledPressure)
            samples.add(StrokeSample(sampledOffset, sampledWidth))
        }

        sampleStart = segmentEnd
        sampleStartPressure = segmentEndPressure
    }

    drawPath(
        path = variableWidthOutline(samples),
        color = color,
        blendMode = BlendMode.SrcOver
    )
}

private data class StrokeSample(val offset: Offset, val width: Float)

private fun variableWidthOutline(samples: List<StrokeSample>): Path {
    val normals = samples.indices.map { index ->
        val before = samples[if (index == 0) index else index - 1].offset
        val after = samples[if (index == samples.lastIndex) index else index + 1].offset
        val dx = after.x - before.x
        val dy = after.y - before.y
        val length = sqrt(dx * dx + dy * dy)
        if (length > 0.001f) Offset(-dy / length, dx / length) else Offset(0f, 1f)
    }
    val left = samples.indices.map { index ->
        samples[index].offset + normals[index] * (samples[index].width / 2)
    }
    val right = samples.indices.map { index ->
        samples[index].offset - normals[index] * (samples[index].width / 2)
    }

    return Path().apply {
        moveTo(left.first().x, left.first().y)
        for (index in 1 until left.size) lineTo(left[index].x, left[index].y)

        appendRoundCap(
            center = samples.last().offset,
            radius = samples.last().width / 2,
            startAngle = atan2(normals.last().y, normals.last().x)
        )

        for (index in right.lastIndex - 1 downTo 0) lineTo(right[index].x, right[index].y)

        appendRoundCap(
            center = samples.first().offset,
            radius = samples.first().width / 2,
            startAngle = atan2(-normals.first().y, -normals.first().x)
        )
        close()
    }
}

private fun Path.appendRoundCap(
    center: Offset,
    radius: Float,
    startAngle: Float,
    steps: Int = 8
) {
    for (step in 1..steps) {
        val angle = startAngle - PI.toFloat() * step / steps
        lineTo(
            center.x + cos(angle) * radius,
            center.y + sin(angle) * radius
        )
    }
}
