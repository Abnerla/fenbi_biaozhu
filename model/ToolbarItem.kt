package com.example.annotation.model

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

/**
 * 工具栏工具项
 */
data class ToolbarItem(
    val id: String,
    val name: String,
    val icon: Any? = null,  // ImageVector 或 Drawable资源ID
    val isDivider: Boolean = false
)

/**
 * 工具栏预设工具
 */
object ToolbarPresets {
    val ALL_TOOLS = listOf(
        ToolbarItem("pen", "画笔", "paintbrush"),
        ToolbarItem("highlighter", "荧光笔", "highlighter"),
        ToolbarItem("eraser", "橡皮擦", "eraser"),
        ToolbarItem("divider1", "", isDivider = true),
        ToolbarItem("undo", "撤销", Icons.Default.Refresh),
        ToolbarItem("clear", "清空", Icons.Default.Delete),
        ToolbarItem("screenshot", "截图", "screenshot"),
        ToolbarItem("divider2", "", isDivider = true),
        ToolbarItem("layout", "切换布局", Icons.Default.Menu),
        ToolbarItem("exit", "退出", Icons.Default.Close)
    )

    fun getToolById(id: String): ToolbarItem? {
        return ALL_TOOLS.find { it.id == id }
    }
}
