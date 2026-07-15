package com.example.annotation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.annotation.drawing.DrawingEngine
import com.example.annotation.model.DrawingTool
import com.example.annotation.model.StylusButtonAction
import com.example.annotation.service.GestureForwardingAccessibilityService
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 工具栏位置枚举
 */
enum class ToolbarPosition {
    LEFT,   // 屏幕左侧 - 二级菜单在右边
    RIGHT,  // 屏幕右侧 - 二级菜单在左边
    TOP,    // 屏幕上方 - 二级菜单在下边
    BOTTOM  // 屏幕下方 - 二级菜单在上边
}

/**
 * 工具栏布局方向
 */
enum class ToolbarOrientation {
    VERTICAL,   // 竖排：吸附到左右
    HORIZONTAL  // 横排：吸附到上下
}

/**
 * 悬浮层完整内容
 */
@Composable
fun OverlayContent(
    drawingEngine: DrawingEngine,
    onExit: () -> Unit,
    onScreenshot: () -> Unit = {},
    initialOffsetX: Float = 0f,
    initialOffsetY: Float = 0f,
    initialOrientation: ToolbarOrientation = ToolbarOrientation.VERTICAL,
    onToolbarPositionChanged: (Float, Float) -> Unit = { _, _ -> },
    onOrientationChanged: (ToolbarOrientation) -> Unit = {},
    toolbarVisible: Boolean = true,
    preferencesManager: com.example.annotation.utils.PreferencesManager? = null
) {
    // 二级菜单显示状态
    val secondaryMenuState = remember { mutableStateOf<SecondaryMenuType?>(null) }

    // 自动折叠设置
    val autoCollapseEnabled by drawingEngine.autoCollapseToolbar.collectAsState()

    // 工具栏布局方向
    var orientation by remember { mutableStateOf(initialOrientation) }

    // 工具栏偏移位置（像素）- 这里的offset代表工具栏左上角的位置
    var offsetX by remember { mutableStateOf(initialOffsetX) }
    var offsetY by remember { mutableStateOf(initialOffsetY) }

    // 用于动画的偏移
    val animatedOffsetX = remember { Animatable(initialOffsetX) }
    val animatedOffsetY = remember { Animatable(initialOffsetY) }
    val coroutineScope = rememberCoroutineScope()

    // 获取屏幕尺寸
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // 判断工具栏应该在哪个边缘（基于当前位置和布局方向）
    val toolbarPosition = remember(offsetX, offsetY, screenWidthPx, screenHeightPx, orientation) {
        when (orientation) {
            ToolbarOrientation.VERTICAL -> {
                if (offsetX < screenWidthPx / 2) {
                    ToolbarPosition.LEFT
                } else {
                    ToolbarPosition.RIGHT
                }
            }
            ToolbarOrientation.HORIZONTAL -> {
                if (offsetY < screenHeightPx / 2) {
                    ToolbarPosition.TOP
                } else {
                    ToolbarPosition.BOTTOM
                }
            }
        }
    }

    // 自动吸附到边缘
    val snapToEdge: () -> Unit = {
        val (targetX, targetY) = when (orientation) {
            ToolbarOrientation.VERTICAL -> {
                // 竖排：吸附到左或右，Y保持在屏幕中心
                val x = if (offsetX < screenWidthPx / 2) {
                    16f  // 左边
                } else {
                    screenWidthPx - 16f  // 右边
                }
                val y = screenHeightPx / 2  // 保持在屏幕垂直中心
                Pair(x, y)
            }
            ToolbarOrientation.HORIZONTAL -> {
                // 横排：吸附到上或下，X保持在屏幕中心
                val x = screenWidthPx / 2  // 保持在屏幕水平中心
                val y = if (offsetY < screenHeightPx / 2) {
                    16f  // 上边
                } else {
                    screenHeightPx - 16f  // 下边
                }
                Pair(x, y)
            }
        }

        // 执行动画
        coroutineScope.launch {
            launch {
                animatedOffsetX.animateTo(
                    targetValue = targetX,
                    animationSpec = tween(durationMillis = 300)
                )
                offsetX = targetX
                onToolbarPositionChanged(targetX, targetY)
            }
            launch {
                animatedOffsetY.animateTo(
                    targetValue = targetY,
                    animationSpec = tween(durationMillis = 300)
                )
                offsetY = targetY
            }
        }
    }

    // 切换布局方向
    val toggleOrientation: () -> Unit = {
        val newOrientation = when (orientation) {
            ToolbarOrientation.VERTICAL -> ToolbarOrientation.HORIZONTAL
            ToolbarOrientation.HORIZONTAL -> ToolbarOrientation.VERTICAL
        }
        orientation = newOrientation
        onOrientationChanged(newOrientation)

        // 切换后自动吸附到最近的边缘
        snapToEdge()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // 绘图画布 - 全屏
        DrawingCanvas(
            drawingEngine = drawingEngine,
            preferencesManager = preferencesManager,
            onTwoFingerTap = {
                // 双指单击撤销
                drawingEngine.undo()
            },
            onThreeFingerTap = drawingEngine::redo,
            onTwoFingerSwipe = { start, end, duration ->
                GestureForwardingAccessibilityService.forwardSwipe(
                    start.x,
                    start.y,
                    end.x,
                    end.y,
                    duration
                )
            },
            onStylusAction = { action ->
                when (action) {
                    StylusButtonAction.NONE -> Unit
                    StylusButtonAction.PEN -> drawingEngine.setTool(DrawingTool.PEN)
                    StylusButtonAction.HIGHLIGHTER -> drawingEngine.setTool(DrawingTool.HIGHLIGHTER)
                    StylusButtonAction.ERASER -> drawingEngine.setTool(DrawingTool.ERASER)
                    StylusButtonAction.UNDO -> drawingEngine.undo()
                    StylusButtonAction.REDO -> drawingEngine.redo()
                    StylusButtonAction.CLEAR -> drawingEngine.clearAll()
                    StylusButtonAction.SCREENSHOT -> onScreenshot()
                    StylusButtonAction.EXIT_ANNOTATION -> onExit()
                }
            },
            onDrawingStart = {
                // 如果启用了自动折叠，且二级菜单正在显示，则折叠它
                if (autoCollapseEnabled && secondaryMenuState.value != null) {
                    secondaryMenuState.value = null
                }
            }
        )

        // 工具栏 - 使用align和offset定位，可拖拽
        // 只有在toolbarVisible为true时才显示
        if (toolbarVisible) {
            Box(
                modifier = Modifier
                    .align(
                        when (toolbarPosition) {
                            ToolbarPosition.LEFT -> Alignment.CenterStart
                            ToolbarPosition.RIGHT -> Alignment.CenterEnd
                            ToolbarPosition.TOP -> Alignment.TopCenter
                            ToolbarPosition.BOTTOM -> Alignment.BottomCenter
                        }
                    )
                    .offset {
                        // 根据工具栏位置调整offset
                        val xOffset = when (toolbarPosition) {
                            ToolbarPosition.LEFT -> animatedOffsetX.value.roundToInt()
                            ToolbarPosition.RIGHT -> -(screenWidthPx - animatedOffsetX.value).roundToInt()
                            ToolbarPosition.TOP, ToolbarPosition.BOTTOM -> {
                                // 横排时，offsetX是相对于屏幕中心的偏移
                                (animatedOffsetX.value - screenWidthPx / 2).roundToInt()
                            }
                        }

                        val yOffset = when (toolbarPosition) {
                            ToolbarPosition.TOP -> animatedOffsetY.value.roundToInt()
                            ToolbarPosition.BOTTOM -> -(screenHeightPx - animatedOffsetY.value).roundToInt()
                            ToolbarPosition.LEFT, ToolbarPosition.RIGHT -> {
                                // 竖排时，offsetY是相对于屏幕中心的偏移
                                (animatedOffsetY.value - screenHeightPx / 2).roundToInt()
                            }
                        }

                        IntOffset(xOffset, yOffset)
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                // 开始拖拽时，取消动画，使用实际偏移
                                coroutineScope.launch {
                                    animatedOffsetX.snapTo(offsetX)
                                    animatedOffsetY.snapTo(offsetY)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidthPx)
                                offsetY = (offsetY + dragAmount.y).coerceIn(0f, screenHeightPx)

                                // 实时更新动画值
                                coroutineScope.launch {
                                    animatedOffsetX.snapTo(offsetX)
                                    animatedOffsetY.snapTo(offsetY)
                                }
                            },
                            onDragEnd = {
                                // 拖拽结束，自动吸附到边缘
                                snapToEdge()
                            }
                        )
                    }
            ) {
                ToolbarView(
                    drawingEngine = drawingEngine,
                    onExit = onExit,
                    onScreenshot = onScreenshot,
                    toolbarPosition = toolbarPosition,
                    onToggleOrientation = toggleOrientation,
                    secondaryMenuState = secondaryMenuState,
                    preferencesManager = preferencesManager
                )
            }
        }
    }
}
