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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.annotation.drawing.DrawingEngine
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
    /**
     * 工具栏实际落点 + 容器尺寸（同一套窗口坐标）。服务侧用它判断工具栏是否真的可见 ——
     * 标注层会吞掉全部触摸，工具栏一旦没出来用户就退不出去，必须有外部兜底。
     */
    onOverlayLayout: (OverlayGeometry.OverlayLayout) -> Unit = {},
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
    // 这里刻意不用 LocalConfiguration 作为权威来源：
    // 1) 非 Activity 承载的窗口（悬浮窗）里，Configuration 的分发不保证及时，而旋转一定会让
    //    窗口重新布局，onSizeChanged 更可靠；
    // 2) Configuration.screenWidthDp 不含系统栏，本窗口是 MATCH_PARENT + LAYOUT_IN_SCREEN，
    //    两者根本不是同一套坐标系。
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val screenWidthPx = if (containerSize.width > 0) {
        containerSize.width.toFloat()
    } else {
        with(density) { configuration.screenWidthDp.dp.toPx() }
    }
    val screenHeightPx = if (containerSize.height > 0) {
        containerSize.height.toFloat()
    } else {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }

    // 安全区：窗口铺满整块屏幕（含状态栏/导航栏），贴边必须以系统栏内侧为界，
    // 否则"贴底/贴右"的工具栏会被导航栏压住。这里在组合期直接读 WindowInsets，
    // 导航栏显隐、手势导航切换都会自动触发重新贴边。
    val safeDrawing = WindowInsets.safeDrawing
    val overlayInsets = OverlayGeometry.OverlayInsets(
        left = safeDrawing.getLeft(density, layoutDirection),
        top = safeDrawing.getTop(density),
        right = safeDrawing.getRight(density, layoutDirection),
        bottom = safeDrawing.getBottom(density)
    )

    // 工具栏停靠边（左/右/上/下）才是需要跨屏幕方向保留的语义状态；
    // 绝对像素只是它在当前尺寸下的投影，尺寸一变就必须重算，否则会整体落到屏幕外。
    var toolbarPosition by remember {
        mutableStateOf(
            OverlayGeometry.dockOf(
                initialOrientation,
                initialOffsetX,
                initialOffsetY,
                screenWidthPx,
                screenHeightPx
            )
        )
    }

    // 容器尺寸或安全区变化（旋转 / 分屏 / 折叠屏展开 / 导航栏显隐）后，
    // 按停靠边重新投影成新的绝对像素。用"尺寸 + 安全区"作为触发键：
    // 首帧为 Zero 时不动作，等真实尺寸到达后落位一次并记录，之后只有真的变了才重算，
    // 避免每次重进标注都重复写一次位置。
    var anchoredKey by remember { mutableStateOf<Pair<IntSize, OverlayGeometry.OverlayInsets>?>(null) }
    LaunchedEffect(containerSize, overlayInsets) {
        if (containerSize.width <= 0 || containerSize.height <= 0) return@LaunchedEffect
        val key = containerSize to overlayInsets
        if (key == anchoredKey) return@LaunchedEffect
        anchoredKey = key

        // 宽高必须由 containerSize 现算，不能用外面那对 screenWidthPx / screenHeightPx。
        // 它们是**组合期捕获的普通值**，而 onSizeChanged 会在本 effect 首次执行之前就写入新尺寸：
        // 实测窗口高 2956、捕获到的 Configuration 高 3198.75，下停靠的工具栏被多推 227px，
        // 整块落到窗口下方 —— 表现就是"工具栏消失"。
        val widthPx = containerSize.width.toFloat()
        val heightPx = containerSize.height.toFloat()

        val anchor = OverlayGeometry.anchorFor(toolbarPosition, widthPx, heightPx, overlayInsets)
            ?: return@LaunchedEffect
        android.util.Log.i(
            "OverlayContent",
            "re-anchor container=${containerSize.width}x${containerSize.height} " +
                "insets=$overlayInsets dock=$toolbarPosition " +
                "anchor=${anchor.first},${anchor.second} visible=$toolbarVisible"
        )
        offsetX = anchor.first
        offsetY = anchor.second
        animatedOffsetX.snapTo(anchor.first)
        animatedOffsetY.snapTo(anchor.second)
        onToolbarPositionChanged(anchor.first, anchor.second)
    }

    // 把当前停靠边投影到当前容器尺寸并动画过去。
    // 只负责"按已有语义状态落位"，不重新推导停靠边 —— 这样切换布局方向和拖拽吸附才能各走各的语义。
    val animateToDock: () -> Unit = {
        val anchor = OverlayGeometry.anchorFor(
            toolbarPosition,
            screenWidthPx,
            screenHeightPx,
            overlayInsets
        )
        if (anchor != null) {
            val targetX = anchor.first
            val targetY = anchor.second

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
    }

    // 自动吸附到边缘：先由手指松开时的位置定出停靠边，再落位
    val snapToEdge: () -> Unit = {
        toolbarPosition = OverlayGeometry.dockOf(
            orientation,
            offsetX,
            offsetY,
            screenWidthPx,
            screenHeightPx
        )
        animateToDock()
    }

    // 切换布局方向
    val toggleOrientation: () -> Unit = {
        val newOrientation = when (orientation) {
            ToolbarOrientation.VERTICAL -> ToolbarOrientation.HORIZONTAL
            ToolbarOrientation.HORIZONTAL -> ToolbarOrientation.VERTICAL
        }
        orientation = newOrientation
        // 停靠边要一起迁移到新方向的轴上（左→上、右→下），否则会因为轴不匹配算到屏幕外
        toolbarPosition = OverlayGeometry.dockAfterOrientationChange(toolbarPosition, newOrientation)
        onOrientationChanged(newOrientation)

        // 这里必须直接落位，不能走 snapToEdge —— 否则会立刻用旧坐标把停靠边重新推导回去
        animateToDock()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
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
                        // 用标量换算而不是 Pair：这个 lambda 在吸附动画期间每帧都会执行
                        IntOffset(
                            OverlayGeometry.offsetXPx(
                                toolbarPosition,
                                animatedOffsetX.value,
                                screenWidthPx
                            ).roundToInt(),
                            OverlayGeometry.offsetYPx(
                                toolbarPosition,
                                animatedOffsetY.value,
                                screenHeightPx
                            ).roundToInt()
                        )
                    }
                    .onGloballyPositioned { coordinates ->
                        val rect = coordinates.boundsInWindow()
                        onOverlayLayout(
                            OverlayGeometry.OverlayLayout(
                                toolbarLeft = rect.left.toInt(),
                                toolbarTop = rect.top.toInt(),
                                toolbarRight = rect.right.toInt(),
                                toolbarBottom = rect.bottom.toInt(),
                                containerWidth = containerSize.width,
                                containerHeight = containerSize.height
                            )
                        )
                    }
                    // 必须把容器尺寸作为 key：pointerInput 的闭包只在创建时捕获一次屏幕尺寸，
                    // 用 Unit 作 key 会让旋转后的拖拽仍然按旧尺寸夹取，可以一路把工具栏拖出屏幕。
                    .pointerInput(screenWidthPx, screenHeightPx) {
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
