package com.example.annotation.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * 绘图工具类型
 */
enum class DrawingTool {
    PEN,           // 普通画笔
    HIGHLIGHTER,   // 荧光笔
    ERASER         // 橡皮擦
}

/**
 * 画笔配置（无透明度，完全不透明）
 */
data class PenConfig(
    val color: Color = Color.Black,
    val strokeWidth: Float = 5f
)

/**
 * 荧光笔配置
 */
data class HighlighterConfig(
    val color: Color = Color.Yellow,
    val strokeWidth: Float = 20f,
    val alpha: Float = 0.4f
)

/**
 * 橡皮擦配置
 */
data class EraserConfig(
    val size: Float = 30f
)

/**
 * 绘图路径点
 */
data class PathPoint(
    val offset: Offset,
    val pressure: Float = 1f,      // 压感值 0-1
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 绘图路径
 */
data class DrawingPath(
    val tool: DrawingTool,
    val points: List<PathPoint>,
    val penConfig: PenConfig? = null,
    val highlighterConfig: HighlighterConfig? = null,
    val eraserConfig: EraserConfig? = null
) {
    fun isEmpty(): Boolean = points.isEmpty()
}

/**
 * 画笔颜色预设
 */
object ColorPresets {
    val PEN_COLORS = listOf(
        Color.Black,
        Color.White,
        Color.Red,
        Color(0xFFFF6B00), // 橙色
        Color(0xFFFFD700), // 金色
        Color.Green,
        Color.Blue,
        Color(0xFF9370DB)  // 紫色
    )

    val HIGHLIGHTER_COLORS = listOf(
        Color.Yellow,
        Color(0xFFFFB6C1), // 粉色
        Color(0xFF98FB98), // 浅绿
        Color(0xFF87CEEB), // 天蓝
        Color(0xFFDDA0DD), // 梅红
        Color(0xFFFFDAB9)  // 桃色
    )

    /**
     * 根据自定义颜色和顺序获取画笔颜色列表
     * @param customColors 自定义颜色列表
     * @param colorOrder 颜色顺序（以Long形式存储的颜色值）
     * @return 按顺序排列的颜色列表，仅包含设置了的自定义颜色
     */
    fun getPenColors(customColors: List<Color>, colorOrder: List<Long>): List<Color> {
        return if (colorOrder.isEmpty()) {
            // 如果没有自定义顺序，返回默认顺序 + 自定义颜色
            PEN_COLORS + customColors
        } else {
            // 根据保存的顺序返回颜色
            colorOrder.mapNotNull { colorValue ->
                Color(colorValue.toULong())
            }.filter { orderedColor ->
                // 过滤掉不在预设或自定义颜色中的颜色
                PEN_COLORS.any { it.value == orderedColor.value } ||
                customColors.any { it.value == orderedColor.value }
            }
        }
    }

    /**
     * 根据自定义颜色和顺序获取荧光笔颜色列表
     * @param customColors 自定义颜色列表
     * @param colorOrder 颜色顺序（以Long形式存储的颜色值）
     * @return 按顺序排列的颜色列表，仅包含设置了的自定义颜色
     */
    fun getHighlighterColors(customColors: List<Color>, colorOrder: List<Long>): List<Color> {
        return if (colorOrder.isEmpty()) {
            // 如果没有自定义顺序，返回默认顺序 + 自定义颜色
            HIGHLIGHTER_COLORS + customColors
        } else {
            // 根据保存的顺序返回颜色
            colorOrder.mapNotNull { colorValue ->
                Color(colorValue.toULong())
            }.filter { orderedColor ->
                // 过滤掉不在预设或自定义颜色中的颜色
                HIGHLIGHTER_COLORS.any { it.value == orderedColor.value } ||
                customColors.any { it.value == orderedColor.value }
            }
        }
    }
}
