package com.example.annotation.ui

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.annotation.drawing.DrawingEngine
import com.example.annotation.model.*
import com.example.annotation.utils.PreferencesManager
import com.example.annotation.utils.StylusButtonGestureDetector
import kotlin.math.hypot

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
    onStylusAction: (StylusButtonAction) -> Unit = {},
    onDrawingStart: () -> Unit = {}
) {
    val currentTool by drawingEngine.currentTool.collectAsState()
    val penConfig by drawingEngine.penConfig.collectAsState()
    val highlighterConfig by drawingEngine.highlighterConfig.collectAsState()
    val eraserConfig by drawingEngine.eraserConfig.collectAsState()
    val eraserPosition by drawingEngine.currentEraserPosition.collectAsState()
    var currentDrawingPath by remember { mutableStateOf<List<PathPoint>>(emptyList()) }
    val inputState = remember { CanvasInputState() }
    val density = LocalDensity.current
    val tapSlop = with(density) { 24.dp.toPx() }
    val swipeThreshold = with(density) { 48.dp.toPx() }
    val latestStylusAction by rememberUpdatedState(onStylusAction)
    val stylusDetector = remember {
        StylusButtonGestureDetector { button, pressType ->
            val action = preferencesManager?.getStylusButtonMappings()?.actionFor(button, pressType)
                ?: StylusButtonAction.NONE
            latestStylusAction(action)
        }
    }

    DisposableEffect(stylusDetector) {
        onDispose { stylusDetector.dispose() }
    }

    fun processStylusButtons(event: MotionEvent) {
        if (preferencesManager?.getStylusEnabled() != true) return
        val configured = preferencesManager.getStylusProfile()
        val resolved = configured.resolvedForDevice()
        val primaryMask = if (configured == StylusProfile.CUSTOM) {
            preferencesManager.getStylusCustomPrimaryMask()
        } else {
            resolved.primaryButtonMask
        }
        val secondaryMask = if (configured == StylusProfile.CUSTOM) {
            preferencesManager.getStylusCustomSecondaryMask()
        } else {
            resolved.secondaryButtonMask
        }
        stylusDetector.process(event.buttonState, primaryMask, secondaryMask, event.eventTime)
    }

    fun centroid(event: MotionEvent): Offset {
        var x = 0f
        var y = 0f
        for (index in 0 until event.pointerCount) {
            x += event.getX(index)
            y += event.getY(index)
        }
        return Offset(x / event.pointerCount, y / event.pointerCount)
    }

    fun handleMotionEvent(event: MotionEvent): Boolean {
        processStylusButtons(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                inputState.reset()
                inputState.maxPointerCount = 1
                inputState.downTime = event.eventTime
                inputState.startCentroid = Offset(event.x, event.y)
                inputState.lastCentroid = inputState.startCentroid
                inputState.rawOffset = Offset(event.rawX - event.x, event.rawY - event.y)
                inputState.drawing = true
                drawingEngine.startDrawing(Offset(event.x, event.y), event.pressure.coerceIn(0f, 1f))
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
                    drawingEngine.continueDrawing(
                        Offset(event.x, event.y),
                        event.pressure.coerceIn(0f, 1f)
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
            val radius = eraserConfig.size / 2
            drawCircle(Color.Gray.copy(alpha = 0.5f), radius, position, style = Stroke(width = 2f))
            drawLine(Color.Gray.copy(alpha = 0.5f), Offset(position.x - 10f, position.y), Offset(position.x + 10f, position.y), 1f)
            drawLine(Color.Gray.copy(alpha = 0.5f), Offset(position.x, position.y - 10f), Offset(position.x, position.y + 10f), 1f)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAnnotationPath(
    drawingPath: DrawingPath,
    color: Color,
    width: Float,
    alpha: Float
) {
    if (drawingPath.points.size < 2) return
    val path = Path().apply {
        val first = drawingPath.points.first().offset
        moveTo(first.x, first.y)
        for (index in 1 until drawingPath.points.size) {
            val previous = drawingPath.points[index - 1].offset
            val current = drawingPath.points[index].offset
            if (index < drawingPath.points.lastIndex) {
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
        color = color.copy(alpha = alpha),
        style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
        blendMode = BlendMode.SrcOver
    )
}
