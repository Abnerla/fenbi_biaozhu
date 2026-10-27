package com.example.annotation.ui

/**
 * 悬浮窗几何计算。
 *
 * 只做纯数值运算，不依赖 Android / Compose，便于用 JVM 单元测试覆盖
 * "竖屏 → 横屏 → 竖屏" 这类只在真机上才容易复现的场景。
 *
 * 坐标约定：
 * - `containerWidthPx / containerHeightPx` 是承载工具栏的容器（= 标注层窗口）的**实际像素尺寸**；
 * - `offsetX / offsetY` 是工具栏的"虚拟锚点"，与 `Modifier.offset {}` 的最终偏移差一次
 *   [offsetXPx] / [offsetYPx] 换算；
 * - 贴左右时垂直居中、贴上下时水平居中，与改写前的实现完全一致。
 */
object OverlayGeometry {

    /** 工具栏贴边时与容器边缘保留的间距（沿用原实现的像素值，不改成 dp 以免改变视觉） */
    const val EDGE_INSET_PX = 16f

    /**
     * 安全区（系统栏 / 刘海 / 手势条）在容器内的占位，单位像素。
     *
     * 标注层窗口带 `FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_NO_LIMITS`，铺满整块屏幕（含状态栏和
     * 导航栏区域）。如果直接以窗口边缘作为"贴边基准"，工具栏就会被导航栏压住。
     * 改动前之所以没暴露这个问题，是因为当时用的是 Configuration 的可用区域而不是窗口尺寸。
     */
    data class OverlayInsets(
        val left: Int = 0,
        val top: Int = 0,
        val right: Int = 0,
        val bottom: Int = 0
    ) {
        companion object {
            val ZERO = OverlayInsets()
        }
    }

    /**
     * 由停靠边推出工具栏锚点（贴边以安全区为界）。
     *
     * 容器尺寸非法（<= 0，通常是首帧尚未完成布局）时返回 null，调用方应跳过本次重算，
     * 否则会把工具栏算到 (0, 0) 去。
     */
    fun anchorFor(
        dock: ToolbarPosition,
        containerWidthPx: Float,
        containerHeightPx: Float,
        insets: OverlayInsets = OverlayInsets.ZERO
    ): Pair<Float, Float>? {
        if (containerWidthPx <= 0f || containerHeightPx <= 0f) return null
        return when (dock) {
            ToolbarPosition.LEFT -> (insets.left + EDGE_INSET_PX) to containerHeightPx / 2f
            ToolbarPosition.RIGHT ->
                (containerWidthPx - insets.right - EDGE_INSET_PX) to containerHeightPx / 2f
            ToolbarPosition.TOP -> containerWidthPx / 2f to (insets.top + EDGE_INSET_PX)
            ToolbarPosition.BOTTOM ->
                containerWidthPx / 2f to (containerHeightPx - insets.bottom - EDGE_INSET_PX)
        }
    }

    /**
     * 锚点 → `Modifier.offset {}` 的横向像素。
     *
     * 拆成横纵两个标量函数而不是返回 Pair：`offset {}` 的 lambda 在吸附动画期间每帧都会执行，
     * 用 Pair 传值会额外产生装箱对象。
     */
    fun offsetXPx(
        dock: ToolbarPosition,
        offsetX: Float,
        containerWidthPx: Float
    ): Float = when (dock) {
        ToolbarPosition.LEFT -> offsetX
        ToolbarPosition.RIGHT -> -(containerWidthPx - offsetX)
        ToolbarPosition.TOP, ToolbarPosition.BOTTOM -> offsetX - containerWidthPx / 2f
    }

    /** 锚点 → `Modifier.offset {}` 的纵向像素。 */
    fun offsetYPx(
        dock: ToolbarPosition,
        offsetY: Float,
        containerHeightPx: Float
    ): Float = when (dock) {
        ToolbarPosition.LEFT, ToolbarPosition.RIGHT -> offsetY - containerHeightPx / 2f
        ToolbarPosition.TOP -> offsetY
        ToolbarPosition.BOTTOM -> -(containerHeightPx - offsetY)
    }

    /** 横纵一起取，仅供阅读与测试；热路径请直接调用 [offsetXPx] / [offsetYPx]。 */
    fun boxOffsetFor(
        dock: ToolbarPosition,
        offsetX: Float,
        offsetY: Float,
        containerWidthPx: Float,
        containerHeightPx: Float
    ): Pair<Float, Float> =
        offsetXPx(dock, offsetX, containerWidthPx) to offsetYPx(dock, offsetY, containerHeightPx)

    /**
     * 由当前绝对偏移反推停靠边。
     * 竖排只在左/右之间选，横排只在上/下之间选 —— 与 [ToolbarPosition] 的语义域一致。
     */
    fun dockOf(
        orientation: ToolbarOrientation,
        offsetX: Float,
        offsetY: Float,
        containerWidthPx: Float,
        containerHeightPx: Float
    ): ToolbarPosition = when (orientation) {
        ToolbarOrientation.VERTICAL ->
            if (offsetX < containerWidthPx / 2f) ToolbarPosition.LEFT else ToolbarPosition.RIGHT

        ToolbarOrientation.HORIZONTAL ->
            if (offsetY < containerHeightPx / 2f) ToolbarPosition.TOP else ToolbarPosition.BOTTOM
    }

    /**
     * 布局方向切换后把停靠边迁移到新方向对应的轴上：左 → 上，右 → 下。
     * 与原来"切到横排后落在底部、切回竖排后落在右侧"的行为保持一致。
     */
    fun dockAfterOrientationChange(
        dock: ToolbarPosition,
        newOrientation: ToolbarOrientation
    ): ToolbarPosition = when (newOrientation) {
        ToolbarOrientation.VERTICAL ->
            if (dock == ToolbarPosition.LEFT) ToolbarPosition.LEFT else ToolbarPosition.RIGHT

        ToolbarOrientation.HORIZONTAL ->
            if (dock == ToolbarPosition.TOP) ToolbarPosition.TOP else ToolbarPosition.BOTTOM
    }

    /**
     * 悬浮球停靠边。
     *
     * 悬浮球的 `x` 是绝对像素，旋转后屏幕宽度变化，"右半侧"的判断会跟着改变，
     * 所以必须把停靠边当成独立状态保存下来 —— 否则右边停靠的球转到横屏会被判成左侧，跑到左下角。
     */
    enum class FloatingSide { LEFT, RIGHT }

    /** 由当前位置判断停靠边（仅在用户拖拽结束时使用）。 */
    fun floatingSideOf(x: Int, buttonWidth: Int, screenWidth: Int): FloatingSide =
        if (x + buttonWidth / 2 <= screenWidth / 2) FloatingSide.LEFT else FloatingSide.RIGHT

    /** 由停靠边算出贴边的 x。 */
    fun floatingButtonX(side: FloatingSide, buttonWidth: Int, screenWidth: Int): Int =
        if (side == FloatingSide.LEFT) 0 else (screenWidth - buttonWidth).coerceAtLeast(0)

    /**
     * 一次布局快照：工具栏在窗口内的落点 + 窗口（容器）尺寸。
     *
     * 两者必须是同一套坐标系（都相对窗口），否则会把"窗口外"判成"屏内仍可见" ——
     * 实测窗口比屏幕矮 244px（状态栏 + 导航栏），用整屏尺寸做上界会漏判。
     */
    data class OverlayLayout(
        val toolbarLeft: Int,
        val toolbarTop: Int,
        val toolbarRight: Int,
        val toolbarBottom: Int,
        val containerWidth: Int,
        val containerHeight: Int
    ) {
        fun isVisible(): Boolean = isVisibleWithin(
            toolbarLeft, toolbarTop, toolbarRight, toolbarBottom,
            containerWidth, containerHeight
        )
    }

    /**
     * 判断工具栏是否真的落在可见区域内。
     *
     * 标注层窗口铺满整屏并且会吞掉所有触摸事件，一旦工具栏没渲染出来或落在窗口外，
     * 用户既看不到入口、也点不到「退出」，连返回手势都会被画布吃掉 —— 只能杀进程。
     * 所以服务侧必须能自己判断"工具栏到底出来了没有"，这个判定就是它的依据。
     *
     * 判定标准：与容器有交叠，且可见面积不少于工具栏自身的一半。
     */
    fun isVisibleWithin(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        containerWidthPx: Int,
        containerHeightPx: Int
    ): Boolean {
        if (right <= left || bottom <= top) return false
        if (containerWidthPx <= 0 || containerHeightPx <= 0) return false
        val overlapWidth = minOf(right, containerWidthPx) - maxOf(left, 0)
        val overlapHeight = minOf(bottom, containerHeightPx) - maxOf(top, 0)
        if (overlapWidth <= 0 || overlapHeight <= 0) return false
        return overlapWidth.toLong() * overlapHeight * 2 >=
            (right - left).toLong() * (bottom - top)
    }

    /**
     * 悬浮球：把绝对窗口坐标夹回屏幕可见范围，并贴到**指定的**停靠边。
     *
     * 悬浮球窗口用 `Gravity.TOP or Gravity.START` + 绝对 `x/y`，屏幕方向变化后系统只是按
     * 原坐标重新布局，不会替调用方换算 —— 必须主动调用本方法，否则窗口会整体留在屏幕外。
     *
     * `preferredSide` 省略时才退化为"按当前 x 就近判断"，只适合首次定位；
     * 旋转等场景必须显式传入已保存的停靠边，否则会跑到另一侧。
     *
     * 幂等：对同一组输入重复调用，结果不变。
     */
    fun clampFloatingButton(
        x: Int,
        y: Int,
        buttonWidth: Int,
        buttonHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
        preferredSide: FloatingSide = floatingSideOf(x, buttonWidth, screenWidth)
    ): Pair<Int, Int> {
        if (screenWidth <= 0 || screenHeight <= 0) return x to y
        val maxY = (screenHeight - buttonHeight).coerceAtLeast(0)
        return floatingButtonX(preferredSide, buttonWidth, screenWidth) to y.coerceIn(0, maxY)
    }
}
