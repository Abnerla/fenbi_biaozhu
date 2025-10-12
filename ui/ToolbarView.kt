package com.example.annotation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.annotation.R
import com.example.annotation.drawing.DrawingEngine
import com.example.annotation.model.ColorPresets
import com.example.annotation.model.DrawingTool
import com.example.annotation.model.ToolbarPresets

/**
 * 工具栏视图 - 一级菜单和二级菜单系统
 */
@Composable
fun ToolbarView(
    drawingEngine: DrawingEngine,
    onExit: () -> Unit,
    onScreenshot: () -> Unit = {},
    toolbarPosition: com.example.annotation.ui.ToolbarPosition = com.example.annotation.ui.ToolbarPosition.RIGHT,
    modifier: Modifier = Modifier,
    onToggleOrientation: () -> Unit = {},
    secondaryMenuState: MutableState<SecondaryMenuType?>? = null,
    preferencesManager: com.example.annotation.utils.PreferencesManager? = null
) {
    val currentTool by drawingEngine.currentTool.collectAsState()
    val penConfig by drawingEngine.penConfig.collectAsState()
    val highlighterConfig by drawingEngine.highlighterConfig.collectAsState()
    val eraserConfig by drawingEngine.eraserConfig.collectAsState()

    // 如果外部提供了状态，使用外部状态；否则使用内部状态
    val internalMenuState = remember { mutableStateOf<SecondaryMenuType?>(null) }
    val showSecondaryMenu = secondaryMenuState ?: internalMenuState
    var showSecondaryMenuValue by showSecondaryMenu

    // 根据工具栏位置决定布局方向
    val isVerticalToolbar = toolbarPosition == com.example.annotation.ui.ToolbarPosition.LEFT ||
                            toolbarPosition == com.example.annotation.ui.ToolbarPosition.RIGHT
    // 根据位置使用不同的布局策略
    when (toolbarPosition) {
        com.example.annotation.ui.ToolbarPosition.LEFT,
        com.example.annotation.ui.ToolbarPosition.RIGHT -> {
            // 左右布局：使用 Row 来横向排列一级和二级菜单
            Row(
                modifier = modifier,
                horizontalArrangement = if (toolbarPosition == com.example.annotation.ui.ToolbarPosition.LEFT) {
                    Arrangement.Start
                } else {
                    Arrangement.End
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (toolbarPosition == com.example.annotation.ui.ToolbarPosition.RIGHT) {
                    // 右侧：先显示二级菜单，再显示一级菜单
                    showSecondaryMenuValue?.let { menuType ->
                        SecondaryMenu(
                            menuType = menuType,
                            drawingEngine = drawingEngine,
                            penConfig = penConfig,
                            highlighterConfig = highlighterConfig,
                            eraserConfig = eraserConfig,
                            onDismiss = { showSecondaryMenuValue = null },
                            toolbarPosition = toolbarPosition,
                            modifier = Modifier.padding(end = 6.dp),
                            preferencesManager = preferencesManager
                        )
                    }
                }

                // 一级菜单
                Column(
                    modifier = Modifier
                        .background(
                            color = Color(0xF0FFFFFF),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ToolbarButtons(
                        currentTool = currentTool,
                        penConfig = penConfig,
                        highlighterConfig = highlighterConfig,
                        eraserConfig = eraserConfig,
                        drawingEngine = drawingEngine,
                        showSecondaryMenu = showSecondaryMenuValue,
                        onSecondaryMenuChange = { showSecondaryMenuValue = it },
                        onExit = onExit,
                        onScreenshot = onScreenshot,
                        toolbarPosition = toolbarPosition,
                        onToggleOrientation = onToggleOrientation,
                        preferencesManager = preferencesManager
                    )
                }

                if (toolbarPosition == com.example.annotation.ui.ToolbarPosition.LEFT) {
                    // 左侧：先显示一级菜单，再显示二级菜单
                    showSecondaryMenuValue?.let { menuType ->
                        SecondaryMenu(
                            menuType = menuType,
                            drawingEngine = drawingEngine,
                            penConfig = penConfig,
                            highlighterConfig = highlighterConfig,
                            eraserConfig = eraserConfig,
                            onDismiss = { showSecondaryMenuValue = null },
                            toolbarPosition = toolbarPosition,
                            modifier = Modifier.padding(start = 6.dp),
                            preferencesManager = preferencesManager
                        )
                    }
                }
            }
        }

        com.example.annotation.ui.ToolbarPosition.TOP,
        com.example.annotation.ui.ToolbarPosition.BOTTOM -> {
            // 上下布局：使用 Column 来纵向排列一级和二级菜单
            Column(
                modifier = modifier,
                verticalArrangement = if (toolbarPosition == com.example.annotation.ui.ToolbarPosition.TOP) {
                    Arrangement.Top
                } else {
                    Arrangement.Bottom
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (toolbarPosition == com.example.annotation.ui.ToolbarPosition.BOTTOM) {
                    // 下方：先显示二级菜单，再显示一级菜单
                    showSecondaryMenuValue?.let { menuType ->
                        SecondaryMenu(
                            menuType = menuType,
                            drawingEngine = drawingEngine,
                            penConfig = penConfig,
                            highlighterConfig = highlighterConfig,
                            eraserConfig = eraserConfig,
                            onDismiss = { showSecondaryMenuValue = null },
                            toolbarPosition = toolbarPosition,
                            modifier = Modifier.padding(bottom = 6.dp),
                            preferencesManager = preferencesManager
                        )
                    }
                }

                // 一级菜单
                Row(
                    modifier = Modifier
                        .background(
                            color = Color(0xF0FFFFFF),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ToolbarButtons(
                        currentTool = currentTool,
                        penConfig = penConfig,
                        highlighterConfig = highlighterConfig,
                        eraserConfig = eraserConfig,
                        drawingEngine = drawingEngine,
                        showSecondaryMenu = showSecondaryMenuValue,
                        onSecondaryMenuChange = { showSecondaryMenuValue = it },
                        onExit = onExit,
                        onScreenshot = onScreenshot,
                        toolbarPosition = toolbarPosition,
                        onToggleOrientation = onToggleOrientation,
                        isHorizontal = true,
                        preferencesManager = preferencesManager
                    )
                }

                if (toolbarPosition == com.example.annotation.ui.ToolbarPosition.TOP) {
                    // 上方：先显示一级菜单，再显示二级菜单
                    showSecondaryMenuValue?.let { menuType ->
                        SecondaryMenu(
                            menuType = menuType,
                            drawingEngine = drawingEngine,
                            penConfig = penConfig,
                            highlighterConfig = highlighterConfig,
                            eraserConfig = eraserConfig,
                            onDismiss = { showSecondaryMenuValue = null },
                            toolbarPosition = toolbarPosition,
                            modifier = Modifier.padding(top = 6.dp),
                            preferencesManager = preferencesManager
                        )
                    }
                }
            }
        }
    }
}

/**
 * 工具栏按钮组
 */
@Composable
private fun ToolbarButtons(
    currentTool: DrawingTool,
    penConfig: com.example.annotation.model.PenConfig,
    highlighterConfig: com.example.annotation.model.HighlighterConfig,
    eraserConfig: com.example.annotation.model.EraserConfig,
    drawingEngine: DrawingEngine,
    showSecondaryMenu: SecondaryMenuType?,
    onSecondaryMenuChange: (SecondaryMenuType?) -> Unit,
    onExit: () -> Unit,
    onScreenshot: () -> Unit = {},
    toolbarPosition: com.example.annotation.ui.ToolbarPosition = com.example.annotation.ui.ToolbarPosition.RIGHT,
    onToggleOrientation: () -> Unit = {},
    isHorizontal: Boolean = false,
    preferencesManager: com.example.annotation.utils.PreferencesManager? = null
) {
    @Composable
    fun DividerItem() {
        if (isHorizontal) {
            VerticalDivider(modifier = Modifier.height(28.dp))
        } else {
            HorizontalDivider(modifier = Modifier.width(28.dp))
        }
    }

    // 读取工具顺序和可见性
    val toolOrder = preferencesManager?.getToolbarOrder() ?: ToolbarPresets.ALL_TOOLS.map { it.id }

    // 根据顺序和可见性渲染工具
    toolOrder.forEach { toolId ->
        val isVisible = preferencesManager?.getToolVisibility(toolId) ?: true
        if (!isVisible) return@forEach  // 跳过不可见的工具

        val tool = ToolbarPresets.getToolById(toolId) ?: return@forEach

        when (toolId) {
            "pen" -> {
                ToolButtonWithPreview(
                    painter = painterResource(id = R.drawable.paintbrush),
                    isSelected = currentTool == DrawingTool.PEN,
                    onClick = {
                        drawingEngine.setTool(DrawingTool.PEN)
                        onSecondaryMenuChange(if (showSecondaryMenu == SecondaryMenuType.PEN) null else SecondaryMenuType.PEN)
                    },
                    description = tool.name,
                    previewColor = penConfig.color,
                    previewSize = penConfig.strokeWidth
                )
            }
            "highlighter" -> {
                ToolButtonWithPreview(
                    painter = painterResource(id = R.drawable.highlighter),
                    isSelected = currentTool == DrawingTool.HIGHLIGHTER,
                    onClick = {
                        drawingEngine.setTool(DrawingTool.HIGHLIGHTER)
                        onSecondaryMenuChange(if (showSecondaryMenu == SecondaryMenuType.HIGHLIGHTER) null else SecondaryMenuType.HIGHLIGHTER)
                    },
                    description = tool.name,
                    previewColor = highlighterConfig.color.copy(alpha = highlighterConfig.alpha),
                    previewSize = highlighterConfig.strokeWidth
                )
            }
            "eraser" -> {
                ToolButtonWithPreview(
                    painter = painterResource(id = R.drawable.eraser),
                    isSelected = currentTool == DrawingTool.ERASER,
                    onClick = {
                        drawingEngine.setTool(DrawingTool.ERASER)
                        onSecondaryMenuChange(if (showSecondaryMenu == SecondaryMenuType.ERASER) null else SecondaryMenuType.ERASER)
                    },
                    description = tool.name,
                    previewColor = Color.Gray,
                    previewSize = eraserConfig.size,
                    showColorPreview = false
                )
            }
            "divider1", "divider2" -> {
                DividerItem()
            }
            "undo" -> {
                ToolButton(
                    icon = Icons.Default.Refresh,
                    isSelected = false,
                    onClick = { drawingEngine.undo() },
                    description = tool.name
                )
            }
            "clear" -> {
                ToolButton(
                    icon = Icons.Default.Delete,
                    isSelected = false,
                    onClick = { drawingEngine.clearAll() },
                    description = tool.name,
                    tint = Color.Red
                )
            }
            "screenshot" -> {
                ToolButtonWithImage(
                    painter = painterResource(id = R.drawable.screenshot),
                    isSelected = false,
                    onClick = onScreenshot,
                    description = tool.name,
                    tint = Color(0xFF4CAF50)
                )
            }
            "layout" -> {
                ToolButton(
                    icon = when (toolbarPosition) {
                        com.example.annotation.ui.ToolbarPosition.LEFT,
                        com.example.annotation.ui.ToolbarPosition.RIGHT -> Icons.Default.Menu
                        com.example.annotation.ui.ToolbarPosition.TOP,
                        com.example.annotation.ui.ToolbarPosition.BOTTOM -> Icons.Default.MoreVert
                    },
                    isSelected = false,
                    onClick = onToggleOrientation,
                    description = tool.name,
                    tint = Color(0xFF2196F3)
                )
            }
            "exit" -> {
                ToolButton(
                    icon = Icons.Default.Close,
                    isSelected = false,
                    onClick = onExit,
                    description = tool.name
                )
            }
        }
    }
}

/**
 * 二级菜单类型
 */
enum class SecondaryMenuType {
    PEN,
    HIGHLIGHTER,
    ERASER
}

/**
 * 二级菜单
 */
@Composable
private fun SecondaryMenu(
    menuType: SecondaryMenuType,
    drawingEngine: DrawingEngine,
    penConfig: com.example.annotation.model.PenConfig,
    highlighterConfig: com.example.annotation.model.HighlighterConfig,
    eraserConfig: com.example.annotation.model.EraserConfig,
    onDismiss: () -> Unit,
    toolbarPosition: com.example.annotation.ui.ToolbarPosition,
    modifier: Modifier = Modifier,
    preferencesManager: com.example.annotation.utils.PreferencesManager? = null
) {
    // 根据工具栏位置决定二级菜单的布局方向
    val isVerticalMenu = toolbarPosition == com.example.annotation.ui.ToolbarPosition.LEFT ||
                         toolbarPosition == com.example.annotation.ui.ToolbarPosition.RIGHT

    if (isVerticalMenu) {
        Column(
            modifier = modifier
                .background(
                    color = Color(0xF0FFFFFF),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SecondaryMenuContent(
                menuType = menuType,
                drawingEngine = drawingEngine,
                penConfig = penConfig,
                highlighterConfig = highlighterConfig,
                eraserConfig = eraserConfig,
                isVertical = true,
                preferencesManager = preferencesManager
            )
        }
    } else {
        Row(
            modifier = modifier
                .background(
                    color = Color(0xF0FFFFFF),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SecondaryMenuContent(
                menuType = menuType,
                drawingEngine = drawingEngine,
                penConfig = penConfig,
                highlighterConfig = highlighterConfig,
                eraserConfig = eraserConfig,
                isVertical = false,
                preferencesManager = preferencesManager
            )
        }
    }
}

/**
 * 二级菜单内容
 */
@Composable
private fun SecondaryMenuContent(
    menuType: SecondaryMenuType,
    drawingEngine: DrawingEngine,
    penConfig: com.example.annotation.model.PenConfig,
    highlighterConfig: com.example.annotation.model.HighlighterConfig,
    eraserConfig: com.example.annotation.model.EraserConfig,
    isVertical: Boolean,
    preferencesManager: com.example.annotation.utils.PreferencesManager? = null
) {
    @Composable
    fun DividerItem() {
        if (isVertical) {
            HorizontalDivider(modifier = Modifier.width(28.dp))
        } else {
            VerticalDivider(modifier = Modifier.height(28.dp))
        }
    }

    // 获取自定义颜色和颜色顺序 - 直接读取，不使用remember缓存
    // 这样每次SecondaryMenuContent重组时都会读取最新的值
    val customPenColors = preferencesManager?.getCustomPenColors() ?: emptyList()
    val penColorOrder = preferencesManager?.getPenColorOrder() ?: emptyList()
    val customHighlighterColors = preferencesManager?.getCustomHighlighterColors() ?: emptyList()
    val highlighterColorOrder = preferencesManager?.getHighlighterColorOrder() ?: emptyList()

    when (menuType) {
        SecondaryMenuType.PEN -> {
            Text("画笔", style = MaterialTheme.typography.labelSmall)

            // 颜色选择 - 使用动态颜色列表
            val penColors = ColorPresets.getPenColors(customPenColors, penColorOrder)
            ColorSelector(
                colors = penColors,
                currentColor = penConfig.color,
                onColorSelected = { color ->
                    drawingEngine.updatePenConfig(color = color)
                },
                isVertical = isVertical
            )

            DividerItem()

            // 粗细调整
            SizeAdjuster(
                currentSize = penConfig.strokeWidth,
                minSize = 1f,
                maxSize = 20f,
                onSizeChanged = { size ->
                    drawingEngine.updatePenConfig(strokeWidth = size)
                },
                previewColor = penConfig.color,
                isVertical = isVertical
            )
        }

        SecondaryMenuType.HIGHLIGHTER -> {
            Text("荧光笔", style = MaterialTheme.typography.labelSmall)

            // 颜色选择 - 使用动态颜色列表
            val highlighterColors = ColorPresets.getHighlighterColors(customHighlighterColors, highlighterColorOrder)
            ColorSelector(
                colors = highlighterColors,
                currentColor = highlighterConfig.color,
                onColorSelected = { color ->
                    drawingEngine.updateHighlighterConfig(color = color)
                },
                isVertical = isVertical
            )

            DividerItem()

            // 粗细调整
            SizeAdjuster(
                currentSize = highlighterConfig.strokeWidth,
                minSize = 10f,
                maxSize = 50f,
                onSizeChanged = { size ->
                    drawingEngine.updateHighlighterConfig(strokeWidth = size)
                },
                previewColor = highlighterConfig.color.copy(alpha = highlighterConfig.alpha),
                isVertical = isVertical
            )
        }

        SecondaryMenuType.ERASER -> {
            Text("橡皮擦", style = MaterialTheme.typography.labelSmall)

            // 大小调整
            SizeAdjuster(
                currentSize = eraserConfig.size,
                minSize = 10f,
                maxSize = 100f,
                onSizeChanged = { size ->
                    drawingEngine.updateEraserSize(size)
                },
                previewColor = Color.Gray,
                isVertical = isVertical
            )
        }
    }
}

/**
 * 颜色选择器
 */
@Composable
private fun ColorSelector(
    colors: List<Color>,
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    isVertical: Boolean = true
) {
    if (isVertical) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            colors.forEach { color ->
                ColorItem(color, currentColor, onColorSelected)
            }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEach { color ->
                ColorItem(color, currentColor, onColorSelected)
            }
        }
    }
}

@Composable
private fun ColorItem(
    color: Color,
    currentColor: Color,
    onColorSelected: (Color) -> Unit
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (color == currentColor) 2.dp else 0.8.dp,
                color = if (color == currentColor) Color.Blue else Color.Gray,
                shape = CircleShape
            )
            .clickable { onColorSelected(color) }
    )
}

/**
 * 大小调整器（带实时预览）
 */
@Composable
private fun SizeAdjuster(
    currentSize: Float,
    minSize: Float,
    maxSize: Float,
    onSizeChanged: (Float) -> Unit,
    previewColor: Color,
    isVertical: Boolean = true
) {
    if (isVertical) {
        // 竖版布局：垂直排列
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // 实时预览 - 使用像素值而不是dp值
            Box(
                modifier = Modifier
                    .size((currentSize / 2).dp.coerceIn(4.dp, 20.dp))
                    .clip(CircleShape)
                    .background(previewColor)
                    .border(0.8.dp, Color.Gray, CircleShape)
            )

            // 大小文字
            Text(
                text = "${currentSize.toInt()}",
                style = MaterialTheme.typography.labelSmall
            )

            // 增大按钮
            IconButton(
                onClick = {
                    val newSize = (currentSize + 2).coerceAtMost(maxSize)
                    onSizeChanged(newSize)
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "增大",
                    modifier = Modifier.size(14.dp)
                )
            }

            // 减小按钮
            IconButton(
                onClick = {
                    val newSize = (currentSize - 2).coerceAtLeast(minSize)
                    onSizeChanged(newSize)
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "减小",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    } else {
        // 横版布局：水平排列
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // 减小按钮
            IconButton(
                onClick = {
                    val newSize = (currentSize - 2).coerceAtLeast(minSize)
                    onSizeChanged(newSize)
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "减小",
                    modifier = Modifier.size(14.dp)
                )
            }

            // 实时预览 - 使用像素值而不是dp值
            Box(
                modifier = Modifier
                    .size((currentSize / 2).dp.coerceIn(4.dp, 20.dp))
                    .clip(CircleShape)
                    .background(previewColor)
                    .border(0.8.dp, Color.Gray, CircleShape)
            )

            // 大小文字
            Text(
                text = "${currentSize.toInt()}",
                style = MaterialTheme.typography.labelSmall
            )

            // 增大按钮
            IconButton(
                onClick = {
                    val newSize = (currentSize + 2).coerceAtMost(maxSize)
                    onSizeChanged(newSize)
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "增大",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * 带预览的工具按钮
 */
@Composable
private fun ToolButtonWithPreview(
    painter: Painter,
    isSelected: Boolean,
    onClick: () -> Unit,
    description: String,
    previewColor: Color,
    previewSize: Float,
    showColorPreview: Boolean = true,
    tint: Color = Color.Black
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color(0xFFE3F2FD) else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) Color(0xFF2196F3) else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painter,
                contentDescription = description,
                tint = if (isSelected) Color(0xFF2196F3) else tint,
                modifier = Modifier.size(14.dp)
            )

            if (showColorPreview) {
                Spacer(modifier = Modifier.height(1.dp))
                // 颜色预览小圆点
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(previewColor)
                        .border(0.5.dp, Color.Gray, CircleShape)
                )
            }
        }
    }
}

/**
 * 普通工具按钮
 */
@Composable
private fun ToolButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    description: String,
    tint: Color = Color.Black
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color(0xFFE3F2FD) else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) Color(0xFF2196F3) else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (isSelected) Color(0xFF2196F3) else tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * 带图片的工具按钮（使用Painter）
 */
@Composable
private fun ToolButtonWithImage(
    painter: Painter,
    isSelected: Boolean,
    onClick: () -> Unit,
    description: String,
    tint: Color = Color.Black
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color(0xFFE3F2FD) else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) Color(0xFF2196F3) else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = description,
            tint = if (isSelected) Color(0xFF2196F3) else tint,
            modifier = Modifier.size(16.dp)
        )
    }
}
