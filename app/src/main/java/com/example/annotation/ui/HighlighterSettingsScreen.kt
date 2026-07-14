package com.example.annotation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.annotation.model.ColorPresets
import com.example.annotation.utils.PreferencesManager
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

/**
 * 荧光笔设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlighterSettingsScreen(
    preferencesManager: PreferencesManager,
    onNavigateBack: () -> Unit
) {
    var highlighterAlpha by remember {
        mutableStateOf(preferencesManager.getHighlighterAlpha())
    }

    // 画笔自定义颜色
    var customPenColors by remember {
        mutableStateOf(preferencesManager.getCustomPenColors().toMutableList())
    }

    // 荧光笔自定义颜色
    var customHighlighterColors by remember {
        mutableStateOf(preferencesManager.getCustomHighlighterColors().toMutableList())
    }

    // 画笔颜色顺序 - 组合预设颜色和自定义颜色
    var penColorOrder by remember {
        val savedOrder = preferencesManager.getPenColorOrder()
        val presetColors = ColorPresets.PEN_COLORS.map { it.value.toLong() }
        val customColorValues = customPenColors.map { it.value.toLong() }

        mutableStateOf(
            if (savedOrder.isEmpty()) {
                // 如果没有保存的顺序，使用默认顺序
                (presetColors + customColorValues).toMutableList()
            } else {
                // 如果有保存的顺序，确保包含所有自定义颜色
                // 先使用保存的顺序，然后添加不在保存顺序中的自定义颜色
                val missingCustomColors = customColorValues.filter { it !in savedOrder }
                (savedOrder + missingCustomColors).toMutableList()
            }
        )
    }

    // 荧光笔颜色顺序
    var highlighterColorOrder by remember {
        val savedOrder = preferencesManager.getHighlighterColorOrder()
        val presetColors = ColorPresets.HIGHLIGHTER_COLORS.map { it.value.toLong() }
        val customColorValues = customHighlighterColors.map { it.value.toLong() }

        mutableStateOf(
            if (savedOrder.isEmpty()) {
                // 如果没有保存的顺序，使用默认顺序
                (presetColors + customColorValues).toMutableList()
            } else {
                // 如果有保存的顺序，确保包含所有自定义颜色
                // 先使用保存的顺序，然后添加不在保存顺序中的自定义颜色
                val missingCustomColors = customColorValues.filter { it !in savedOrder }
                (savedOrder + missingCustomColors).toMutableList()
            }
        )
    }

    // 显示颜色选择器对话框
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerType by remember { mutableStateOf("pen") } // "pen" 或 "highlighter"
    var editingColorIndex by remember { mutableStateOf(-1) } // -1表示添加新颜色

    // 错误提示对话框
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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
                            text = "笔设置",
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
            // 荧光笔透明度设置
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
                    HighlighterAlphaSetting(
                        alpha = highlighterAlpha,
                        onAlphaChange = { newAlpha ->
                            highlighterAlpha = newAlpha
                            preferencesManager.setHighlighterAlpha(newAlpha)
                        }
                    )
                }
            }

            // 画笔颜色顺序设置
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "画笔颜色顺序",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        // 恢复默认顺序 - 只恢复预设颜色的顺序，保留自定义颜色在末尾
                        val defaultPresetColors = ColorPresets.PEN_COLORS.map { it.value.toLong() }
                        val customColorValues = customPenColors.map { it.value.toLong() }
                        val newOrder = (defaultPresetColors + customColorValues).toMutableList()
                        penColorOrder = newOrder
                        preferencesManager.setPenColorOrder(newOrder)
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
                            text = "颜色顺序",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "长按拖动调整",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ColorOrderSection(
                        colors = penColorOrder.map { Color(it.toULong()) },
                        customColors = customPenColors,
                        onOrderChange = { newOrder ->
                            penColorOrder = newOrder.map { it.value.toLong() }.toMutableList()
                            preferencesManager.setPenColorOrder(penColorOrder)
                        },
                        onAddColor = {
                            if (customPenColors.size < 3) {
                                colorPickerType = "pen"
                                editingColorIndex = -1
                                showColorPicker = true
                            }
                        },
                        onDeleteColor = { color ->
                            val index = customPenColors.indexOfFirst { it.value == color.value }
                            if (index >= 0) {
                                // 创建新的列表以触发重组
                                customPenColors = customPenColors.toMutableList().apply { removeAt(index) }
                                preferencesManager.setCustomPenColors(customPenColors)
                                // 同时更新颜色顺序
                                penColorOrder = penColorOrder.toMutableList().apply { remove(color.value.toLong()) }
                                preferencesManager.setPenColorOrder(penColorOrder)
                            }
                        },
                        isCustomColor = { color ->
                            customPenColors.any { it.value == color.value }
                        }
                    )
                }
            }

            // 荧光笔颜色顺序设置
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "荧光笔颜色顺序",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        // 恢复默认顺序 - 只恢复预设颜色的顺序，保留自定义颜色在末尾
                        val defaultPresetColors = ColorPresets.HIGHLIGHTER_COLORS.map { it.value.toLong() }
                        val customColorValues = customHighlighterColors.map { it.value.toLong() }
                        val newOrder = (defaultPresetColors + customColorValues).toMutableList()
                        highlighterColorOrder = newOrder
                        preferencesManager.setHighlighterColorOrder(newOrder)
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
                            text = "颜色顺序",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "长按拖动调整",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ColorOrderSection(
                        colors = highlighterColorOrder.map { Color(it.toULong()) },
                        customColors = customHighlighterColors,
                        onOrderChange = { newOrder ->
                            highlighterColorOrder = newOrder.map { it.value.toLong() }.toMutableList()
                            preferencesManager.setHighlighterColorOrder(highlighterColorOrder)
                        },
                        onAddColor = {
                            if (customHighlighterColors.size < 3) {
                                colorPickerType = "highlighter"
                                editingColorIndex = -1
                                showColorPicker = true
                            }
                        },
                        onDeleteColor = { color ->
                            val index = customHighlighterColors.indexOfFirst { it.value == color.value }
                            if (index >= 0) {
                                // 创建新的列表以触发重组
                                customHighlighterColors = customHighlighterColors.toMutableList().apply { removeAt(index) }
                                preferencesManager.setCustomHighlighterColors(customHighlighterColors)
                                // 同时更新颜色顺序
                                highlighterColorOrder = highlighterColorOrder.toMutableList().apply { remove(color.value.toLong()) }
                                preferencesManager.setHighlighterColorOrder(highlighterColorOrder)
                            }
                        },
                        isCustomColor = { color ->
                            customHighlighterColors.any { it.value == color.value }
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
                    Text(
                        text = "设置将实时生效，自定义颜色最多3个",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 颜色选择器对话框
        if (showColorPicker) {
            ColorPickerDialog(
                onDismiss = { showColorPicker = false },
                onColorSelected = { color ->
                    if (colorPickerType == "pen") {
                        // 检查是否与预设颜色重复
                        val isDuplicatePreset = ColorPresets.PEN_COLORS.any { it.value == color.value }
                        // 检查是否与其他自定义颜色重复（编辑时排除自己）
                        val isDuplicateCustom = customPenColors.any {
                            it.value == color.value && customPenColors.indexOf(it) != editingColorIndex
                        }

                        if (isDuplicatePreset) {
                            errorMessage = "该颜色已存在于预设颜色中，无法添加"
                            showErrorDialog = true
                            showColorPicker = false
                        } else if (isDuplicateCustom) {
                            errorMessage = "该颜色已存在于自定义颜色中，无法重复添加"
                            showErrorDialog = true
                            showColorPicker = false
                        } else {
                            if (editingColorIndex >= 0) {
                                customPenColors[editingColorIndex] = color
                            } else {
                                customPenColors.add(color)
                                penColorOrder.add(color.value.toLong())
                            }
                            preferencesManager.setCustomPenColors(customPenColors)
                            preferencesManager.setPenColorOrder(penColorOrder)
                            showColorPicker = false
                        }
                    } else {
                        // 检查是否与预设颜色重复
                        val isDuplicatePreset = ColorPresets.HIGHLIGHTER_COLORS.any { it.value == color.value }
                        // 检查是否与其他自定义颜色重复（编辑时排除自己）
                        val isDuplicateCustom = customHighlighterColors.any {
                            it.value == color.value && customHighlighterColors.indexOf(it) != editingColorIndex
                        }

                        if (isDuplicatePreset) {
                            errorMessage = "该颜色已存在于预设颜色中，无法添加"
                            showErrorDialog = true
                            showColorPicker = false
                        } else if (isDuplicateCustom) {
                            errorMessage = "该颜色已存在于自定义颜色中，无法重复添加"
                            showErrorDialog = true
                            showColorPicker = false
                        } else {
                            if (editingColorIndex >= 0) {
                                customHighlighterColors[editingColorIndex] = color
                            } else {
                                customHighlighterColors.add(color)
                                highlighterColorOrder.add(color.value.toLong())
                            }
                            preferencesManager.setCustomHighlighterColors(customHighlighterColors)
                            preferencesManager.setHighlighterColorOrder(highlighterColorOrder)
                            showColorPicker = false
                        }
                    }
                }
            )
        }

        // 错误提示对话框
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = {
                    Text(
                        text = "无法添加颜色",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { showErrorDialog = false }
                    ) {
                        Text("确定")
                    }
                }
            )
        }
    }
}

/**
 * 荧光笔透明度设置组件
 */
@Composable
private fun HighlighterAlphaSetting(
    alpha: Float,
    onAlphaChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "透明度",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${(alpha * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 滑块
        Slider(
            value = alpha,
            onValueChange = onAlphaChange,
            valueRange = 0.1f..1.0f,
            steps = 8,
            modifier = Modifier.fillMaxWidth()
        )

        // 预览效果
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "预览效果",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 预览条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            // 模拟荧光笔效果
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 黄色荧光笔预览
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .background(
                            color = Color.Yellow.copy(alpha = alpha),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 粉色荧光笔预览
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .background(
                            color = Color(0xFFFFB6C1).copy(alpha = alpha),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 绿色荧光笔预览
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .background(
                            color = Color(0xFF98FB98).copy(alpha = alpha),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
        }

        // 说明文字
        Text(
            text = "透明度越低，荧光笔颜色越浅；透明度越高，颜色越鲜艳",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 颜色顺序调整组件（支持拖拽排序）
 */
@Composable
private fun ColorOrderSection(
    colors: List<Color>,
    customColors: List<Color>,
    onOrderChange: (List<Color>) -> Unit,
    onAddColor: () -> Unit,
    onDeleteColor: (Color) -> Unit,
    isCustomColor: (Color) -> Boolean
) {
    // 拖拽状态
    var draggedColorValue by remember { mutableStateOf<Long?>(null) }
    var dragStartIndex by remember { mutableStateOf(0) }
    var dragCurrentOffset by remember { mutableStateOf(0f) }

    // 视觉显示顺序（不改变，保持稳定）
    var visualOrder by remember(colors) { mutableStateOf(colors) }

    // 逻辑顺序（用于计算哪些颜色应该移动）
    var logicalOrder by remember(colors) { mutableStateOf(colors) }

    // 元素宽度和间距（使用 density 转换 dp 到 px）
    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemWidthDp = 40.dp  // 元素本身宽度
    val itemSpacingDp = 8.dp // 元素间距
    val itemWidth = with(density) { (itemWidthDp + itemSpacingDp).toPx() } // 总宽度（元素+间距）

    // 当不在拖拽状态时，同步外部顺序
    LaunchedEffect(colors) {
        if (draggedColorValue == null) {
            visualOrder = colors
            logicalOrder = colors
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
                    enabled = draggedColorValue == null
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 使用视觉顺序渲染，保持DOM稳定
            visualOrder.forEachIndexed { visualIndex, color ->
                key(color.value) {
                    val colorValue = color.value.toLong()

                    // 计算这个颜色在逻辑顺序中的位置
                    val logicalIndex = logicalOrder.indexOfFirst { it.value == color.value }

                    // 计算偏移量
                    val offset = when {
                        // 被拖拽的颜色：使用手指偏移
                        draggedColorValue == colorValue -> {
                            dragCurrentOffset
                        }
                        // 其他颜色：直接根据逻辑位置与视觉位置的差异计算偏移
                        else -> {
                            val positionDiff = logicalIndex - visualIndex
                            positionDiff * itemWidth
                        }
                    }

                    ColorOrderItem(
                        color = color,
                        isCustom = isCustomColor(color),
                        isDragged = draggedColorValue == colorValue,
                        dragOffset = Offset(offset, 0f),
                        onPositionChanged = { _ -> },
                        onDragStart = {
                            draggedColorValue = colorValue
                            dragStartIndex = logicalOrder.indexOfFirst { it.value == color.value }
                            dragCurrentOffset = 0f
                        },
                        onDragEnd = {
                            // 同步视觉顺序到逻辑顺序
                            visualOrder = logicalOrder
                            onOrderChange(logicalOrder)

                            // 清除状态
                            draggedColorValue = null
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

                            val currentIndex = logicalOrder.indexOfFirst { it.value == color.value }

                            // 如果目标位置改变，更新逻辑顺序
                            if (targetIndex != currentIndex && currentIndex != -1) {
                                val newOrder = logicalOrder.toMutableList()
                                newOrder.removeAt(currentIndex)
                                newOrder.add(targetIndex, color)
                                logicalOrder = newOrder
                            }
                        },
                        onDelete = { onDeleteColor(color) }
                    )
                }
            }

            // 添加按钮
            if (customColors.size < 3) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable(onClick = onAddColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加自定义颜色",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * 单个颜色项（支持拖拽）- 圆形展示
 */
@Composable
private fun ColorOrderItem(
    color: Color,
    isCustom: Boolean,
    isDragged: Boolean,
    dragOffset: Offset,
    onPositionChanged: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Offset) -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .onGloballyPositioned { coordinates ->
                onPositionChanged(coordinates.positionInRoot().x)
            }
            .graphicsLayer {
                // 所有元素都应用偏移，实现实时让位效果
                translationX = dragOffset.x
                translationY = dragOffset.y
            }
            .zIndex(if (isDragged) 1f else 0f),
        contentAlignment = Alignment.Center
    ) {
        // 颜色圆
        Box(
            modifier = Modifier
                .size(32.dp)
                .pointerInput(color.value) {
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
                .background(color)
                .border(
                    width = if (isDragged) 3.dp else 1.dp,
                    color = when {
                        isDragged -> MaterialTheme.colorScheme.primary
                        else -> Color.Gray.copy(alpha = 0.3f)
                    },
                    shape = CircleShape
                )
        )

        // 自定义颜色的删除按钮
        if (isCustom && !isDragged) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "删除",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * 颜色输入模式
 */
enum class ColorInputMode {
    HEX,  // 十六进制
    RGB   // RGB
}

/**
 * 颜色选择器对话框
 */
@Composable
private fun ColorPickerDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val controller = rememberColorPickerController()
    var selectedColor by remember { mutableStateOf(Color.Red) }

    // RGB输入值
    var redValue by remember { mutableStateOf("255") }
    var greenValue by remember { mutableStateOf("0") }
    var blueValue by remember { mutableStateOf("0") }

    // 十六进制输入值
    var hexValue by remember { mutableStateOf("FF0000") }

    // 输入模式（默认十六进制）
    var inputMode by remember { mutableStateOf(ColorInputMode.HEX) }
    var showInputModeMenu by remember { mutableStateOf(false) }

    // 是否正在通过文本输入更新颜色（避免循环更新）
    var isUpdatingFromText by remember { mutableStateOf(false) }

    // 从颜色更新文本输入框
    fun updateTextFieldsFromColor(color: Color) {
        if (!isUpdatingFromText) {
            val red = (color.red * 255).toInt()
            val green = (color.green * 255).toInt()
            val blue = (color.blue * 255).toInt()

            redValue = red.toString()
            greenValue = green.toString()
            blueValue = blue.toString()
            hexValue = String.format("%02X%02X%02X", red, green, blue)
        }
    }

    // 从RGB输入更新颜色
    fun updateColorFromRGB() {
        isUpdatingFromText = true
        try {
            val r = redValue.toIntOrNull()?.coerceIn(0, 255) ?: 0
            val g = greenValue.toIntOrNull()?.coerceIn(0, 255) ?: 0
            val b = blueValue.toIntOrNull()?.coerceIn(0, 255) ?: 0

            selectedColor = Color(r, g, b)
            controller.selectByColor(selectedColor, fromUser = false)
            hexValue = String.format("%02X%02X%02X", r, g, b)
        } finally {
            isUpdatingFromText = false
        }
    }

    // 从十六进制输入更新颜色
    fun updateColorFromHex() {
        isUpdatingFromText = true
        try {
            val cleanHex = hexValue.removePrefix("#").take(6)
            if (cleanHex.length == 6) {
                val colorValue = cleanHex.toLongOrNull(16)
                if (colorValue != null) {
                    selectedColor = Color(0xFF000000 or colorValue)
                    controller.selectByColor(selectedColor, fromUser = false)

                    val r = ((colorValue shr 16) and 0xFF).toInt()
                    val g = ((colorValue shr 8) and 0xFF).toInt()
                    val b = (colorValue and 0xFF).toInt()

                    redValue = r.toString()
                    greenValue = g.toString()
                    blueValue = b.toString()
                }
            }
        } finally {
            isUpdatingFromText = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "选择颜色",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // 颜色选择器
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    controller = controller,
                    onColorChanged = { colorEnvelope ->
                        selectedColor = colorEnvelope.color
                        updateTextFieldsFromColor(colorEnvelope.color)
                    }
                )

                // 亮度滑块
                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    controller = controller
                )

                // 颜色输入区域 - 横向布局
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧：紧凑的模式切换按钮
                        Box {
                            OutlinedButton(
                                onClick = { showInputModeMenu = true },
                                modifier = Modifier.width(70.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = when (inputMode) {
                                        ColorInputMode.HEX -> "HEX"
                                        ColorInputMode.RGB -> "RGB"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp
                                )
                            }

                            // 下拉菜单
                            DropdownMenu(
                                expanded = showInputModeMenu,
                                onDismissRequest = { showInputModeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("十六进制", style = MaterialTheme.typography.bodySmall) },
                                    onClick = {
                                        inputMode = ColorInputMode.HEX
                                        showInputModeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("RGB", style = MaterialTheme.typography.bodySmall) },
                                    onClick = {
                                        inputMode = ColorInputMode.RGB
                                        showInputModeMenu = false
                                    }
                                )
                            }
                        }

                        // 右侧：输入框
                        when (inputMode) {
                            ColorInputMode.HEX -> {
                                // 十六进制输入框
                                OutlinedTextField(
                                    value = hexValue,
                                    onValueChange = {
                                        hexValue = it.filter { char ->
                                            char.isDigit() || char.uppercaseChar() in 'A'..'F'
                                        }.take(6).uppercase()
                                        updateColorFromHex()
                                    },
                                    label = { Text("HEX", style = MaterialTheme.typography.labelSmall) },
                                    prefix = { Text("#", style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                            }
                            ColorInputMode.RGB -> {
                                // RGB输入框组
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // R输入框
                                    OutlinedTextField(
                                        value = redValue,
                                        onValueChange = {
                                            redValue = it.filter { char -> char.isDigit() }.take(3)
                                            updateColorFromRGB()
                                        },
                                        label = { Text("R", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )

                                    // G输入框
                                    OutlinedTextField(
                                        value = greenValue,
                                        onValueChange = {
                                            greenValue = it.filter { char -> char.isDigit() }.take(3)
                                            updateColorFromRGB()
                                        },
                                        label = { Text("G", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )

                                    // B输入框
                                    OutlinedTextField(
                                        value = blueValue,
                                        onValueChange = {
                                            blueValue = it.filter { char -> char.isDigit() }.take(3)
                                            updateColorFromRGB()
                                        },
                                        label = { Text("B", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                // 预览颜色
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "预览",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(selectedColor)
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    )
                }

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onColorSelected(selectedColor) }) {
                        Text("确定")
                    }
                }
            }
        }
    }
}
