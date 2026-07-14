package com.example.annotation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.annotation.R
import com.example.annotation.model.ToolbarItem
import com.example.annotation.model.ToolbarPresets
import com.example.annotation.utils.PreferencesManager

/**
 * 工具栏设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarSettingsScreen(
    preferencesManager: PreferencesManager,
    onNavigateBack: () -> Unit
) {
    var autoCollapseToolbar by remember {
        mutableStateOf(preferencesManager.getAutoCollapseToolbar())
    }

    // 读取工具顺序
    var toolOrder by remember {
        val savedOrder = preferencesManager.getToolbarOrder()
        mutableStateOf(savedOrder.toMutableList())
    }

    // 读取工具可见性
    var toolVisibilities by remember {
        val visibilityMap = mutableMapOf<String, Boolean>()
        ToolbarPresets.ALL_TOOLS.forEach { tool ->
            visibilityMap[tool.id] = preferencesManager.getToolVisibility(tool.id)
        }
        mutableStateOf(visibilityMap)
    }

    // 处理返回手势
    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(44.dp),
                title = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "工具栏设置",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets(0)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding(), start = 16.dp, end = 16.dp, bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 工具栏顺序设置
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "工具顺序",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        // 恢复默认顺序 - 只恢复位置顺序，不影响可见性设置
                        toolOrder = ToolbarPresets.DEFAULT_ORDER.toMutableList()
                        preferencesManager.setToolbarOrder(ToolbarPresets.DEFAULT_ORDER)
                    }
                ) {
                    Text(
                        text = "恢复默认",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "拖动调整顺序",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "长按拖动",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    ToolbarOrderSection(
                        toolOrder = toolOrder,
                        toolVisibilities = toolVisibilities,
                        onOrderChange = { newOrder ->
                            toolOrder = newOrder.toMutableList()
                            preferencesManager.setToolbarOrder(newOrder)
                        },
                        onVisibilityChange = { toolId, visible ->
                            toolVisibilities = toolVisibilities.toMutableMap().apply {
                                put(toolId, visible)
                            }
                            preferencesManager.setToolVisibility(toolId, visible)
                        }
                    )
                }
            }

            // 其他设置
            Text(
                text = "其他设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            // 设置卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 自动折叠二级功能栏开关
                    AutoCollapseToolbarSetting(
                        enabled = autoCollapseToolbar,
                        onEnabledChange = { enabled ->
                            autoCollapseToolbar = enabled
                            preferencesManager.setAutoCollapseToolbar(enabled)
                        }
                    )
                }
            }

            // 提示信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "💡",
                        fontSize = 20.sp
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "长按拖动工具调整顺序",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "点击眼睛图标隐藏/显示工具",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "设置将实时生效",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 工具栏顺序调整组件
 */
@Composable
private fun ToolbarOrderSection(
    toolOrder: List<String>,
    toolVisibilities: Map<String, Boolean>,
    onOrderChange: (List<String>) -> Unit,
    onVisibilityChange: (String, Boolean) -> Unit
) {
    // 拖拽状态
    var draggedToolId by remember { mutableStateOf<String?>(null) }
    var dragStartIndex by remember { mutableStateOf(0) }
    var dragCurrentOffset by remember { mutableStateOf(0f) }

    // 视觉显示顺序（不改变，保持稳定）
    var visualOrder by remember(toolOrder) { mutableStateOf(toolOrder) }

    // 逻辑顺序（用于计算哪些图标应该移动）
    var logicalOrder by remember(toolOrder) { mutableStateOf(toolOrder) }

    // 元素宽度和间距（使用 density 转换 dp 到 px）
    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemWidthDp = 72.dp  // 元素本身宽度
    val itemSpacingDp = 12.dp // 元素间距
    val itemWidth = with(density) { (itemWidthDp + itemSpacingDp).toPx() } // 总宽度（元素+间距）

    // 当不在拖拽状态时，同步外部顺序
    LaunchedEffect(toolOrder) {
        if (draggedToolId == null) {
            visualOrder = toolOrder
            logicalOrder = toolOrder
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    state = scrollState,
                    enabled = draggedToolId == null
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 使用视觉顺序渲染，保持DOM稳定
            visualOrder.forEachIndexed { visualIndex, toolId ->
                key(toolId) {
                    val tool = ToolbarPresets.getToolById(toolId)
                    tool?.let {
                        // 计算这个图标在逻辑顺序中的位置
                        val logicalIndex = logicalOrder.indexOf(toolId)

                        // 计算偏移量
                        val offset = when {
                            // 被拖拽的图标：使用手指偏移
                            draggedToolId == toolId -> {
                                dragCurrentOffset
                            }
                            // 其他图标：直接根据逻辑位置与视觉位置的差异计算偏移
                            else -> {
                                val positionDiff = logicalIndex - visualIndex
                                positionDiff * itemWidth
                            }
                        }

                        ToolbarOrderItem(
                            tool = it,
                            toolId = toolId,
                            isVisible = toolVisibilities[toolId] ?: true,
                            isDragged = draggedToolId == toolId,
                            dragOffset = Offset(offset, 0f),
                            onPositionChanged = { _ -> },
                            onDragStart = {
                                draggedToolId = toolId
                                dragStartIndex = logicalOrder.indexOf(toolId)
                                dragCurrentOffset = 0f
                            },
                            onDragEnd = {
                                // 同步视觉顺序到逻辑顺序
                                visualOrder = logicalOrder
                                onOrderChange(logicalOrder)

                                // 清除状态
                                draggedToolId = null
                                dragCurrentOffset = 0f
                                dragStartIndex = 0
                            },
                            onDrag = { change, offset ->
                                change.consume()
                                dragCurrentOffset += offset.x

                                // 计算目标索引 - 使用更严格的阈值
                                // 需要拖拽超过元素宽度的 75% 才触发位置交换
                                val threshold = itemWidth * 0.75f
                                val positionShift = if (dragCurrentOffset > 0) {
                                    // 向右拖动
                                    ((dragCurrentOffset + threshold / 2) / itemWidth).toInt()
                                } else {
                                    // 向左拖动
                                    ((dragCurrentOffset - threshold / 2) / itemWidth).toInt()
                                }

                                val targetIndex = (dragStartIndex + positionShift)
                                    .coerceIn(0, logicalOrder.size - 1)

                                val currentIndex = logicalOrder.indexOf(toolId)

                                // 如果目标位置改变，更新逻辑顺序
                                if (targetIndex != currentIndex) {
                                    val newOrder = logicalOrder.toMutableList()
                                    newOrder.removeAt(currentIndex)
                                    newOrder.add(targetIndex, toolId)
                                    logicalOrder = newOrder
                                }
                            },
                            onVisibilityToggle = {
                                onVisibilityChange(toolId, !(toolVisibilities[toolId] ?: true))
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单个工具项（支持拖拽）
 */
@Composable
private fun ToolbarOrderItem(
    tool: ToolbarItem,
    toolId: String,
    isVisible: Boolean,
    isDragged: Boolean,
    dragOffset: Offset,
    onPositionChanged: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Offset) -> Unit,
    onVisibilityToggle: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .width(72.dp)
            .onGloballyPositioned { coordinates ->
                onPositionChanged(coordinates.positionInRoot().x)
            }
            .graphicsLayer {
                // 所有元素都应用偏移，实现实时让位效果
                translationX = dragOffset.x
                translationY = dragOffset.y
            }
            .zIndex(if (isDragged) 1f else 0f)
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            // 工具图标圆圈
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .pointerInput(toolId) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                onDragStart()
                            },
                            onDragEnd = {
                                onDragEnd()
                            },
                            onDrag = { change, dragAmount ->
                                onDrag(change, dragAmount)
                            }
                        )
                    }
                    .clip(CircleShape)
                    .background(
                        color = if (isVisible) Color(0xFFF0F0F0) else Color(0xFFE0E0E0).copy(alpha = 0.5f)
                    )
                    .border(
                        width = if (isDragged) 3.dp else 1.dp,
                        color = when {
                            isDragged -> MaterialTheme.colorScheme.primary
                            else -> Color.Gray.copy(alpha = 0.3f)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 显示工具图标
                if (!tool.isDivider) {
                    when (val iconData = tool.icon) {
                        is ImageVector -> {
                            Icon(
                                imageVector = iconData,
                                contentDescription = tool.name,
                                tint = if (isVisible) Color.Black else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        is String -> {
                            // 使用drawable资源
                            val drawableId = when (iconData) {
                                "paintbrush" -> R.drawable.paintbrush
                                "highlighter" -> R.drawable.highlighter
                                "eraser" -> R.drawable.eraser
                                "screenshot" -> R.drawable.screenshot
                                else -> null
                            }
                            drawableId?.let {
                                Icon(
                                    painter = painterResource(id = it),
                                    contentDescription = tool.name,
                                    tint = if (isVisible) Color.Black else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    // 分隔线用竖线表示
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(24.dp)
                            .background(if (isVisible) Color.Gray else Color.Gray.copy(alpha = 0.3f))
                    )
                }
            }

            // 可见性切换按钮 - 放在右上角
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isVisible) MaterialTheme.colorScheme.primary
                        else Color.Gray.copy(alpha = 0.6f)
                    )
                    .clickable(onClick = onVisibilityToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = if (isVisible) "隐藏" else "显示",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // 工具名称
        Text(
            text = if (tool.isDivider) "分隔线" else tool.name,
            style = MaterialTheme.typography.bodySmall,
            color = if (isVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

/**
 * 自动折叠二级功能栏设置组件
 */
@Composable
private fun AutoCollapseToolbarSetting(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "二级功能栏",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "开始绘制时自动收起工具配置面板",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange
        )
    }
}
