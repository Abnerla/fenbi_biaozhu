package com.example.annotation.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 悬浮窗横竖屏切换的几何回归测试。
 *
 * 这里用 1280×2856 / 2856×1280 作为竖屏/横屏基准（取自实机 `dumpsys` / uiautomator dump）。
 * 关键断言只有一句：**工具栏左上角必须落在容器内**。
 */
class OverlayGeometryTest {

    private val portraitWidth = 1280f
    private val portraitHeight = 2856f
    private val landscapeWidth = 2856f
    private val landscapeHeight = 1280f

    private val verticalToolbarWidth = 48f
    private val verticalToolbarHeight = 280f
    private val horizontalToolbarWidth = 280f
    private val horizontalToolbarHeight = 48f

    /**
     * 复刻 Compose 侧 `align(...) + offset { }` 的最终落点：
     * 先按对齐得到基准位置，再叠加 offset。
     */
    private fun toolbarTopLeft(
        dock: ToolbarPosition,
        offsetX: Float,
        offsetY: Float,
        containerWidthPx: Float,
        containerHeightPx: Float,
        toolbarWidth: Float,
        toolbarHeight: Float
    ): Pair<Float, Float> {
        val (dx, dy) = OverlayGeometry.boxOffsetFor(
            dock, offsetX, offsetY, containerWidthPx, containerHeightPx
        )
        val (baseX, baseY) = when (dock) {
            ToolbarPosition.LEFT -> 0f to (containerHeightPx - toolbarHeight) / 2f
            ToolbarPosition.RIGHT -> (containerWidthPx - toolbarWidth) to (containerHeightPx - toolbarHeight) / 2f
            ToolbarPosition.TOP -> ((containerWidthPx - toolbarWidth) / 2f) to 0f
            ToolbarPosition.BOTTOM -> ((containerWidthPx - toolbarWidth) / 2f) to (containerHeightPx - toolbarHeight)
        }
        return (baseX + dx) to (baseY + dy)
    }

    private fun assertInsideContainer(
        topLeft: Pair<Float, Float>,
        containerWidthPx: Float,
        containerHeightPx: Float,
        toolbarWidth: Float,
        toolbarHeight: Float,
        message: String
    ) {
        val (left, top) = topLeft
        assertTrue(
            "$message：left=$left 越出 [0, ${containerWidthPx - toolbarWidth}]",
            left >= -0.01f && left <= containerWidthPx - toolbarWidth + 0.01f
        )
        assertTrue(
            "$message：top=$top 越出 [0, ${containerHeightPx - toolbarHeight}]",
            top >= -0.01f && top <= containerHeightPx - toolbarHeight + 0.01f
        )
    }

    // ────────────────────────── 回归基准：旧实现为什么必然"消失" ──────────────────────────

    @Test
    fun `旧实现把竖屏像素直接套进横屏_竖排工具栏落到屏幕下方`() {
        // 竖屏右侧竖排时保存的锚点
        val staleX = portraitWidth - OverlayGeometry.EDGE_INSET_PX
        val staleY = portraitHeight / 2f

        val dock = OverlayGeometry.dockOf(
            ToolbarOrientation.VERTICAL, staleX, staleY, landscapeWidth, landscapeHeight
        )
        // 附带副作用：原来贴右侧的竖排，在横屏下会被判成"左侧"（换边）
        assertEquals(ToolbarPosition.LEFT, dock)
        val topLeft = toolbarTopLeft(
            dock, staleX, staleY, landscapeWidth, landscapeHeight,
            verticalToolbarWidth, verticalToolbarHeight
        )

        // top ≈ 1288 > 1280：完全掉出屏幕，这正是用户看到的"消失"
        assertEquals(1288f, topLeft.second, 0.5f)
        assertTrue("旧实现必须越界，否则这个 bug 不成立", topLeft.second > landscapeHeight)
    }

    @Test
    fun `旧实现把竖屏像素直接套进横屏_横排工具栏滑出下边缘`() {
        val staleX = portraitWidth - OverlayGeometry.EDGE_INSET_PX
        val staleY = portraitHeight / 2f

        val dock = OverlayGeometry.dockOf(
            ToolbarOrientation.HORIZONTAL, staleX, staleY, landscapeWidth, landscapeHeight
        )
        assertEquals(ToolbarPosition.BOTTOM, dock)

        val topLeft = toolbarTopLeft(
            dock, staleX, staleY, landscapeWidth, landscapeHeight,
            horizontalToolbarWidth, horizontalToolbarHeight
        )
        assertEquals(1380f, topLeft.second, 0.5f)
        assertTrue("旧实现必须越界", topLeft.second > landscapeHeight)
    }

    // ────────────────────────── 修复后：竖 → 横 ──────────────────────────

    @Test
    fun `竖屏右停靠旋转到横屏后仍贴右侧且垂直居中`() {
        val dock = OverlayGeometry.dockOf(
            ToolbarOrientation.VERTICAL,
            portraitWidth - OverlayGeometry.EDGE_INSET_PX,
            portraitHeight / 2f,
            portraitWidth,
            portraitHeight
        )
        assertEquals(ToolbarPosition.RIGHT, dock)

        val anchor = OverlayGeometry.anchorFor(dock, landscapeWidth, landscapeHeight)!!
        val topLeft = toolbarTopLeft(
            dock, anchor.first, anchor.second, landscapeWidth, landscapeHeight,
            verticalToolbarWidth, verticalToolbarHeight
        )

        assertInsideContainer(
            topLeft, landscapeWidth, landscapeHeight,
            verticalToolbarWidth, verticalToolbarHeight, "竖→横"
        )
        // 仍然贴右（间距 16px），纵向仍居中
        assertEquals(landscapeWidth - OverlayGeometry.EDGE_INSET_PX - verticalToolbarWidth, topLeft.first, 0.5f)
        assertEquals((landscapeHeight - verticalToolbarHeight) / 2f, topLeft.second, 0.5f)
    }

    @Test
    fun `竖屏左停靠旋转到横屏后仍贴左侧`() {
        val dock = OverlayGeometry.dockOf(
            ToolbarOrientation.VERTICAL, OverlayGeometry.EDGE_INSET_PX, 400f,
            portraitWidth, portraitHeight
        )
        assertEquals(ToolbarPosition.LEFT, dock)

        val anchor = OverlayGeometry.anchorFor(dock, landscapeWidth, landscapeHeight)!!
        val topLeft = toolbarTopLeft(
            dock, anchor.first, anchor.second, landscapeWidth, landscapeHeight,
            verticalToolbarWidth, verticalToolbarHeight
        )
        assertInsideContainer(
            topLeft, landscapeWidth, landscapeHeight,
            verticalToolbarWidth, verticalToolbarHeight, "竖→横（左）"
        )
        assertEquals(OverlayGeometry.EDGE_INSET_PX, topLeft.first, 0.5f)
    }

    // ────────────────────────── 修复后：横 → 竖（含往返） ──────────────────────────

    @Test
    fun `横屏下停靠旋转回竖屏后仍贴对应边`() {
        val landscapeDock = OverlayGeometry.dockOf(
            ToolbarOrientation.HORIZONTAL,
            landscapeWidth / 2f,
            landscapeHeight - OverlayGeometry.EDGE_INSET_PX,
            landscapeWidth,
            landscapeHeight
        )
        assertEquals(ToolbarPosition.BOTTOM, landscapeDock)

        val anchor = OverlayGeometry.anchorFor(landscapeDock, portraitWidth, portraitHeight)!!
        val topLeft = toolbarTopLeft(
            landscapeDock, anchor.first, anchor.second, portraitWidth, portraitHeight,
            horizontalToolbarWidth, horizontalToolbarHeight
        )
        assertInsideContainer(
            topLeft, portraitWidth, portraitHeight,
            horizontalToolbarWidth, horizontalToolbarHeight, "横→竖"
        )
        assertEquals(portraitHeight - OverlayGeometry.EDGE_INSET_PX - horizontalToolbarHeight, topLeft.second, 0.5f)
    }

    @Test
    fun `四次旋转往返停靠边保持稳定且始终在容器内`() {
        var dock = ToolbarPosition.RIGHT
        val sizes = listOf(
            portraitWidth to portraitHeight,
            landscapeWidth to landscapeHeight,
            portraitWidth to portraitHeight,
            landscapeWidth to landscapeHeight
        )

        sizes.forEachIndexed { index, (w, h) ->
            val anchor = OverlayGeometry.anchorFor(dock, w, h)!!
            val isVertical = dock == ToolbarPosition.LEFT || dock == ToolbarPosition.RIGHT
            val toolbarWidth = if (isVertical) verticalToolbarWidth else horizontalToolbarWidth
            val toolbarHeight = if (isVertical) verticalToolbarHeight else horizontalToolbarHeight

            assertInsideContainer(
                toolbarTopLeft(dock, anchor.first, anchor.second, w, h, toolbarWidth, toolbarHeight),
                w, h, toolbarWidth, toolbarHeight, "第 ${index + 1} 次旋转"
            )
            // 停靠边不因为旋转漂移
            val orientation = if (isVertical) ToolbarOrientation.VERTICAL else ToolbarOrientation.HORIZONTAL
            assertEquals(dock, OverlayGeometry.dockOf(orientation, anchor.first, anchor.second, w, h))
        }
    }

    // ────────────────────────── 停靠边迁移 / 非法尺寸 ──────────────────────────

    @Test
    fun `停靠边在切换布局方向时迁移到新轴上`() {
        // 与原实现保持一致：切换时旧锚点的自由轴坐标恰好等于半屏，
        // 判定落到"后一项"，因此竖排（无论左右）切横排落在底部，横排（无论上下）切竖排落在右侧。
        assertEquals(
            ToolbarPosition.BOTTOM,
            OverlayGeometry.dockAfterOrientationChange(ToolbarPosition.LEFT, ToolbarOrientation.HORIZONTAL)
        )
        assertEquals(
            ToolbarPosition.BOTTOM,
            OverlayGeometry.dockAfterOrientationChange(ToolbarPosition.RIGHT, ToolbarOrientation.HORIZONTAL)
        )
        assertEquals(
            ToolbarPosition.RIGHT,
            OverlayGeometry.dockAfterOrientationChange(ToolbarPosition.TOP, ToolbarOrientation.VERTICAL)
        )
        assertEquals(
            ToolbarPosition.RIGHT,
            OverlayGeometry.dockAfterOrientationChange(ToolbarPosition.BOTTOM, ToolbarOrientation.VERTICAL)
        )
    }

    @Test
    fun `切换布局方向后停靠边迁移并落在容器内`() {
        // 竖排右侧 → 切横排：应落到底部横排，而不是拿着旧的 x 坐标直接算
        val newDock = OverlayGeometry.dockAfterOrientationChange(
            ToolbarPosition.RIGHT, ToolbarOrientation.HORIZONTAL
        )
        assertEquals(ToolbarPosition.BOTTOM, newDock)

        val anchor = OverlayGeometry.anchorFor(newDock, portraitWidth, portraitHeight)!!
        val topLeft = toolbarTopLeft(
            newDock, anchor.first, anchor.second, portraitWidth, portraitHeight,
            horizontalToolbarWidth, horizontalToolbarHeight
        )
        assertInsideContainer(
            topLeft, portraitWidth, portraitHeight,
            horizontalToolbarWidth, horizontalToolbarHeight, "竖排切横排"
        )
        assertEquals(
            portraitHeight - OverlayGeometry.EDGE_INSET_PX - horizontalToolbarHeight,
            topLeft.second,
            0.5f
        )
        assertEquals((portraitWidth - horizontalToolbarWidth) / 2f, topLeft.first, 0.5f)
    }

    @Test
    fun `容器尺寸为零时不返回锚点`() {
        assertNull(OverlayGeometry.anchorFor(ToolbarPosition.RIGHT, 0f, 1280f))
        assertNull(OverlayGeometry.anchorFor(ToolbarPosition.RIGHT, 1280f, 0f))
        assertNull(OverlayGeometry.anchorFor(ToolbarPosition.RIGHT, -1f, 1280f))
    }

    // ────────────────────────── 悬浮球夹取 ──────────────────────────

    @Test
    fun `竖屏中部的悬浮球旋转到横屏后被夹回屏内`() {
        val (x, y) = OverlayGeometry.clampFloatingButton(
            x = 1180,
            y = 1428,
            buttonWidth = 120,
            buttonHeight = 120,
            screenWidth = 2856,
            screenHeight = 1280
        )
        // y 必须是可见的（原坐标 1428 > 1280 会整体落在屏幕外）
        assertEquals(1160, y)
        // 落在左边缘（1180 相对 2856 宽属于左半侧）
        assertEquals(0, x)
    }

    @Test
    fun `悬浮球在合法位置时只贴边不改变高度`() {
        val (x, y) = OverlayGeometry.clampFloatingButton(
            x = 900,
            y = 600,
            buttonWidth = 120,
            buttonHeight = 120,
            screenWidth = 1280,
            screenHeight = 2856
        )
        assertEquals(1160, x) // 1280 - 120，贴右
        assertEquals(600, y)
    }

    @Test
    fun `屏幕尺寸非法时原样返回`() {
        val (x, y) = OverlayGeometry.clampFloatingButton(100, 200, 120, 120, 0, 0)
        assertEquals(100, x)
        assertEquals(200, y)
    }

    @Test
    fun `悬浮球比屏幕还大时不会算出负坐标`() {
        val (x, y) = OverlayGeometry.clampFloatingButton(500, 500, 200, 200, 150, 150)
        assertEquals(0, x)
        assertEquals(0, y)
    }

    @Test
    fun `悬浮球夹取是幂等的`() {
        val once = OverlayGeometry.clampFloatingButton(1180, 1428, 120, 120, 2856, 1280)
        val twice = OverlayGeometry.clampFloatingButton(once.first, once.second, 120, 120, 2856, 1280)
        assertEquals(once, twice)
    }

    // ────────────────────────── 悬浮球停靠边必须跨方向保持 ──────────────────────────

    @Test
    fun `右边停靠的悬浮球旋转到横屏后仍贴右侧`() {
        // 竖屏 1440x3200，球贴右：x = 1440 - 150
        val side = OverlayGeometry.floatingSideOf(1290, 150, 1440)
        assertEquals(OverlayGeometry.FloatingSide.RIGHT, side)

        // 旧逻辑：按新屏宽就近判断，1290 落在 3200 的左半侧 → 被推到左边（用户报的现象）
        val byNearest = OverlayGeometry.clampFloatingButton(1290, 745, 150, 150, 3200, 1440)
        assertEquals(0, byNearest.first)

        // 带上停靠边：仍然贴右侧
        val fixed = OverlayGeometry.clampFloatingButton(
            1290, 745, 150, 150, 3200, 1440,
            preferredSide = OverlayGeometry.FloatingSide.RIGHT
        )
        assertEquals(3200 - 150, fixed.first)
        assertEquals(745, fixed.second)
    }

    @Test
    fun `左边停靠的悬浮球旋转到超宽屏后仍贴左侧`() {
        val fixed = OverlayGeometry.clampFloatingButton(
            0, 300, 150, 150, 3200, 1440,
            preferredSide = OverlayGeometry.FloatingSide.LEFT
        )
        assertEquals(0, fixed.first)
        assertEquals(300, fixed.second)
    }

    // ────────────────────────── 工具栏可见性自检（防"锁死"） ──────────────────────────

    @Test
    fun `工具栏完整落在容器内判为可见`() {
        assertTrue(OverlayGeometry.isVisibleWithin(16, 1300, 160, 1580, 1440, 2956))
    }

    @Test
    fun `工具栏整体落在容器下方判为不可见`() {
        // 实测出现过的那次：容器高 2956，工具栏被推到 2960 之后
        assertFalse(OverlayGeometry.isVisibleWithin(700, 2960, 880, 3140, 1440, 2956))
    }

    @Test
    fun `工具栏只露出不到一半判为不可见`() {
        // 高 180：只露出 40px
        assertFalse(OverlayGeometry.isVisibleWithin(700, 2916, 880, 3096, 1440, 2956))
        // 露出 96px（> 一半）
        assertTrue(OverlayGeometry.isVisibleWithin(700, 2860, 880, 3040, 1440, 2956))
    }

    @Test
    fun `窗口比屏幕矮时不能拿整屏当上界`() {
        // 实测设备：屏幕 3200 高，标注层窗口只有 2956 高（状态栏 169 + 导航栏 75）
        // 工具栏被推到窗口下方（top=3000）时，用整屏上界会漏判成"可见"
        val belowWindow = OverlayGeometry.OverlayLayout(700, 3000, 880, 3180, 1440, 2956)
        assertFalse(belowWindow.isVisible())

        val normal = OverlayGeometry.OverlayLayout(16, 747, 182, 1968, 1440, 2956)
        assertTrue(normal.isVisible())
    }

    @Test
    fun `零尺寸或非法容器判为不可见`() {
        assertFalse(OverlayGeometry.isVisibleWithin(10, 10, 10, 10, 1440, 2956))
        assertFalse(OverlayGeometry.isVisibleWithin(10, 10, 100, 10, 1440, 2956))
        assertFalse(OverlayGeometry.isVisibleWithin(10, 10, 100, 200, 0, 0))
    }

    @Test
    fun `由停靠边回算 x`() {
        assertEquals(0, OverlayGeometry.floatingButtonX(OverlayGeometry.FloatingSide.LEFT, 150, 1440))
        assertEquals(1290, OverlayGeometry.floatingButtonX(OverlayGeometry.FloatingSide.RIGHT, 150, 1440))
        assertEquals(3050, OverlayGeometry.floatingButtonX(OverlayGeometry.FloatingSide.RIGHT, 150, 3200))
        assertEquals(0, OverlayGeometry.floatingButtonX(OverlayGeometry.FloatingSide.LEFT, 200, 150))
    }

    // ────────────────────────── 覆盖矩阵：四个停靠边 × 三种屏幕尺寸 ──────────────────────────

    @Test
    fun `四个停靠边在竖屏横屏折叠屏下都贴边且在容器内`() {
        val sizes = listOf(
            "竖屏" to (portraitWidth to portraitHeight),
            "横屏" to (landscapeWidth to landscapeHeight),
            "折叠屏展开" to (2208f to 1840f)
        )
        val docks = listOf(
            ToolbarPosition.LEFT to (verticalToolbarWidth to verticalToolbarHeight),
            ToolbarPosition.RIGHT to (verticalToolbarWidth to verticalToolbarHeight),
            ToolbarPosition.TOP to (horizontalToolbarWidth to horizontalToolbarHeight),
            ToolbarPosition.BOTTOM to (horizontalToolbarWidth to horizontalToolbarHeight)
        )

        docks.forEach { (dock, toolbar) ->
            sizes.forEach { (name, size) ->
                val (w, h) = size
                val anchor = OverlayGeometry.anchorFor(dock, w, h)!!
                val topLeft = toolbarTopLeft(
                    dock, anchor.first, anchor.second, w, h, toolbar.first, toolbar.second
                )
                assertInsideContainer(topLeft, w, h, toolbar.first, toolbar.second, "$name / $dock")

                val gap = when (dock) {
                    ToolbarPosition.LEFT -> topLeft.first
                    ToolbarPosition.RIGHT -> w - (topLeft.first + toolbar.first)
                    ToolbarPosition.TOP -> topLeft.second
                    ToolbarPosition.BOTTOM -> h - (topLeft.second + toolbar.second)
                }
                assertEquals("$name / $dock 的贴边间距", OverlayGeometry.EDGE_INSET_PX, gap, 0.5f)
            }
        }
    }

    @Test
    fun `标量换算与合并换算结果一致`() {
        val docks = listOf(
            ToolbarPosition.LEFT, ToolbarPosition.RIGHT,
            ToolbarPosition.TOP, ToolbarPosition.BOTTOM
        )
        docks.forEach { dock ->
            val pair = OverlayGeometry.boxOffsetFor(dock, 640f, 1200f, portraitWidth, portraitHeight)
            assertEquals(
                "横向偏移 $dock",
                pair.first,
                OverlayGeometry.offsetXPx(dock, 640f, portraitWidth),
                0.001f
            )
            assertEquals(
                "纵向偏移 $dock",
                pair.second,
                OverlayGeometry.offsetYPx(dock, 1200f, portraitHeight),
                0.001f
            )
        }
    }

    @Test
    fun `横排时位于上半屏判定为上停靠`() {
        assertEquals(
            ToolbarPosition.TOP,
            OverlayGeometry.dockOf(
                ToolbarOrientation.HORIZONTAL,
                portraitWidth / 2f,
                OverlayGeometry.EDGE_INSET_PX,
                portraitWidth,
                portraitHeight
            )
        )
    }

    // ────────────────────────── 安全区：贴边不能被系统栏压住 ──────────────────────────

    @Test
    fun `底部导航栏存在时下停靠要避开导航栏`() {
        val navBarPx = 132
        val insets = OverlayGeometry.OverlayInsets(bottom = navBarPx)
        val dock = ToolbarPosition.BOTTOM

        val anchor = OverlayGeometry.anchorFor(dock, portraitWidth, portraitHeight, insets)!!
        val topLeft = toolbarTopLeft(
            dock, anchor.first, anchor.second, portraitWidth, portraitHeight,
            horizontalToolbarWidth, horizontalToolbarHeight
        )
        val bottom = topLeft.second + horizontalToolbarHeight

        assertEquals(
            "工具栏底边应在导航栏上沿再留 16px",
            portraitHeight - navBarPx - OverlayGeometry.EDGE_INSET_PX,
            bottom,
            0.5f
        )
        assertTrue("不能被导航栏压住", bottom <= portraitHeight - navBarPx)
    }

    @Test
    fun `横屏侧边导航栏存在时右停靠要避开导航栏`() {
        val navBarPx = 132
        val insets = OverlayGeometry.OverlayInsets(right = navBarPx)
        val dock = ToolbarPosition.RIGHT

        val anchor = OverlayGeometry.anchorFor(dock, landscapeWidth, landscapeHeight, insets)!!
        val topLeft = toolbarTopLeft(
            dock, anchor.first, anchor.second, landscapeWidth, landscapeHeight,
            verticalToolbarWidth, verticalToolbarHeight
        )

        assertEquals(
            "工具栏右边应在导航栏左侧再留 16px",
            landscapeWidth - navBarPx - OverlayGeometry.EDGE_INSET_PX,
            topLeft.first + verticalToolbarWidth,
            0.5f
        )
    }

    @Test
    fun `顶部状态栏存在时上停靠要避开状态栏`() {
        val statusBarPx = 66
        val insets = OverlayGeometry.OverlayInsets(top = statusBarPx)

        val anchor = OverlayGeometry.anchorFor(
            ToolbarPosition.TOP, portraitWidth, portraitHeight, insets
        )!!
        val topLeft = toolbarTopLeft(
            ToolbarPosition.TOP, anchor.first, anchor.second, portraitWidth, portraitHeight,
            horizontalToolbarWidth, horizontalToolbarHeight
        )

        assertEquals(statusBarPx + OverlayGeometry.EDGE_INSET_PX, topLeft.second, 0.5f)
    }

    @Test
    fun `安全区为零时的落点与改动前一致`() {
        val anchor = OverlayGeometry.anchorFor(
            ToolbarPosition.RIGHT, portraitWidth, portraitHeight, OverlayGeometry.OverlayInsets.ZERO
        )!!
        assertEquals(portraitWidth - OverlayGeometry.EDGE_INSET_PX, anchor.first, 0.001f)
        assertEquals(portraitHeight / 2f, anchor.second, 0.001f)
    }
}
