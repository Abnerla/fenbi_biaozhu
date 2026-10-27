package com.example.annotation.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Create
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.annotation.MainActivity
import com.example.annotation.R
import com.example.annotation.drawing.DrawingEngine
import com.example.annotation.model.DrawingTool
import com.example.annotation.model.StylusButtonAction
import com.example.annotation.ui.OverlayContent
import com.example.annotation.ui.OverlayGeometry
import com.example.annotation.ui.theme.AnnotationTheme
import com.example.annotation.utils.AppThemeMode
import com.example.annotation.utils.PreferencesManager
import com.example.annotation.utils.ScreenshotHelper
import com.example.annotation.utils.ScreenCaptureManager
import com.example.annotation.utils.StylusInputRouter
import android.widget.Toast
import android.widget.FrameLayout
import com.example.annotation.ScreenCapturePermissionActivity

/**
 * 悬浮窗服务
 */
class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var floatingButton: ComposeView? = null
    private val drawingEngine = DrawingEngine()
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var screenCaptureManager: ScreenCaptureManager
    private lateinit var stylusInputRouter: StylusInputRouter
    private val themeModeState = mutableStateOf(AppThemeMode.SYSTEM)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingOverlayAfterStylusBridge = false

    // 工具栏可见性状态 - 用于截图时隐藏UI
    private val toolbarVisibleState = mutableStateOf(true)

    // 设置变更监听器
    private val preferencesListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            "highlighter_alpha" -> {
                // 荧光笔透明度变更
                val alpha = preferencesManager.getHighlighterAlpha()
                drawingEngine.updateHighlighterConfig(alpha = alpha)
            }
            "auto_collapse_toolbar" -> {
                // 自动折叠设置变更
                val autoCollapse = preferencesManager.getAutoCollapseToolbar()
                drawingEngine.setAutoCollapseToolbar(autoCollapse)
            }
            "theme_mode" -> {
                themeModeState.value = preferencesManager.getThemeMode()
            }
        }
    }

    // 保存悬浮按钮位置的参数
    private var floatingButtonParams: WindowManager.LayoutParams? = null

    /**
     * 悬浮球停靠边（语义状态）。
     *
     * 旋转时宽度变化，"x 在不在右半侧"这个判断会跟着变，所以必须单独保存；
     * 只由绝对 x 推断会让右边停靠的球转到横屏后跑到左下角。
     */
    private var floatingButtonSide = OverlayGeometry.FloatingSide.RIGHT

    /** 悬浮球纵向位置占可用高度的比例（0~1）；-1 表示尚未知，退化为直接夹取像素。 */
    private var floatingButtonRatioY = -1f

    /**
     * 工具栏是否「曾经」真的出现在可见区域。
     *
     * 标注层铺满整屏且吞掉所有触摸，工具栏一旦没渲染出来或落在窗口外，用户就退不出去。
     * 进入标注模式后服务侧会在 [ANNOTATION_UI_TIMEOUT_MS] 后复查这个标记，没亮过就自动退出。
     * 用「曾经」而不是「当前」，是为了避免用户正常拖拽时的中间态被误判。
     */
    private var annotationToolbarSeenVisible = false

    // 保存工具栏位置
    private var toolbarOffsetX: Float = 0f
    private var toolbarOffsetY: Float = 0f

    // 保存工具栏布局方向
    private var toolbarOrientation: com.example.annotation.ui.ToolbarOrientation =
        com.example.annotation.ui.ToolbarOrientation.VERTICAL

    // Lifecycle管理
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "overlay_service_channel"

        // SharedPreferences相关常量
        private const val PREFS_NAME = "overlay_button_prefs"

        /** 进入标注模式后多久复查工具栏是否真的可见（毫秒）。 */
        private const val ANNOTATION_UI_TIMEOUT_MS = 3000L
        private const val KEY_BUTTON_X = "button_x"
        private const val KEY_BUTTON_Y = "button_y"
        private const val KEY_BUTTON_SIDE = "button_side"
        private const val KEY_BUTTON_RATIO_Y = "button_ratio_y"
        private const val KEY_FIRST_LAUNCH = "first_launch"

        // 工具栏位置相关常量
        private const val KEY_TOOLBAR_X = "toolbar_x"
        private const val KEY_TOOLBAR_Y = "toolbar_y"
        private const val KEY_TOOLBAR_FIRST_LAUNCH = "toolbar_first_launch"
        private const val KEY_TOOLBAR_ORIENTATION = "toolbar_orientation"

        // 绘图工具配置相关常量
        private const val KEY_PEN_COLOR = "pen_color"
        private const val KEY_PEN_STROKE_WIDTH = "pen_stroke_width"
        private const val KEY_HIGHLIGHTER_COLOR = "highlighter_color"
        private const val KEY_HIGHLIGHTER_STROKE_WIDTH = "highlighter_stroke_width"
        private const val KEY_HIGHLIGHTER_ALPHA = "highlighter_alpha"
        private const val KEY_ERASER_SIZE = "eraser_size"

        const val ACTION_SET_MEDIA_PROJECTION = "ACTION_SET_MEDIA_PROJECTION"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
        const val EXTRA_CAPTURE_AFTER_PERMISSION = "captureAfterPermission"
        const val ACTION_SET_ANNOTATION_MODE = "com.example.annotation.action.SET_ANNOTATION_MODE"
        const val EXTRA_ANNOTATION_ENABLED = "annotationEnabled"

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var isAnnotationModeActive: Boolean = false
            private set

        private var activeInstance: OverlayService? = null

        fun setOverlayTouchable(touchable: Boolean) {
            activeInstance?.updateOverlayTouchable(touchable)
        }

        fun setFloatingButtonSuppressed(suppressed: Boolean) {
            activeInstance?.updateFloatingButtonSuppressed(suppressed)
        }

        fun processStylusKeyEvent(event: KeyEvent): Boolean =
            activeInstance?.stylusInputRouter?.processKeyEvent(event) ?: false

        fun shouldConsumeStylusKeyEvent(event: KeyEvent): Boolean =
            activeInstance?.stylusInputRouter?.shouldConsumeKeyEvent(event) ?: false

        fun onStylusKeyBridgeConnected() {
            activeInstance?.handleStylusKeyBridgeConnected()
        }

        fun onStylusKeyBridgeDisconnected() {
            activeInstance?.handleStylusKeyBridgeDisconnected()
        }

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.stopService(intent)
        }

        fun setAnnotationMode(context: Context, enabled: Boolean) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_SET_ANNOTATION_MODE
                putExtra(EXTRA_ANNOTATION_ENABLED, enabled)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        activeInstance = this

        // 初始化 PreferencesManager
        preferencesManager = PreferencesManager(this)
        stylusInputRouter = StylusInputRouter(preferencesManager, ::executeStylusAction)
        themeModeState.value = preferencesManager.getThemeMode()

        // 获取ScreenCaptureManager单例实例
        screenCaptureManager = ScreenCaptureManager.getInstance(this)

        // 注册设置变更监听器
        val prefs = getSharedPreferences("annotation_preferences", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(preferencesListener)

        // 初始化Lifecycle
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()

        // Android 14+ 使用 specialUse 类型维持悬浮标注服务。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        // 恢复绘图工具配置
        restoreDrawingConfigs()

    }

    /**
     * 在创建MediaProjection前将当前前台服务升级为mediaProjection类型。
     * Android 14+要求此调用严格发生在用户授权之后、getMediaProjection之前。
     */
    private fun promoteToMediaProjectionForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var serviceTypes = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                serviceTypes = serviceTypes or
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            }
            startForeground(NOTIFICATION_ID, createNotification(), serviceTypes)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }

    /**
     * 尝试恢复MediaProjection
     */
    private fun tryRestoreMediaProjection() {
        android.util.Log.d("OverlayService", "检查是否有缓存的MediaProjection权限")

        val cachedData = screenCaptureManager.getCachedPermissionData()
        if (cachedData != null) {
            val (resultCode, data) = cachedData
            android.util.Log.d("OverlayService", "发现缓存的权限数据，尝试初始化MediaProjection")

            promoteToMediaProjectionForegroundService()
            val success = screenCaptureManager.initializeMediaProjection(resultCode, data)
            if (success) {
                android.util.Log.d("OverlayService", "从缓存恢复MediaProjection成功")
            } else {
                android.util.Log.w("OverlayService", "从缓存恢复MediaProjection失败")
                screenCaptureManager.reset()
            }
        } else {
            android.util.Log.d("OverlayService", "没有缓存的权限数据")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("OverlayService", "onStartCommand called, action=${intent?.action}")

        // 服务可能被系统以START_STICKY恢复，也可能在权限页操作过程中收到启动请求。
        // 无悬浮窗权限时绝不能添加TYPE_APPLICATION_OVERLAY窗口。
        if (!Settings.canDrawOverlays(this)) {
            android.util.Log.w("OverlayService", "缺少悬浮窗权限，停止服务以避免窗口权限异常")
            removeOverlay()
            removeFloatingButton()
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_SET_ANNOTATION_MODE) {
            if (intent.getBooleanExtra(EXTRA_ANNOTATION_ENABLED, false)) {
                showOverlay()
            } else {
                pendingOverlayAfterStylusBridge = false
                removeOverlay()
                showFloatingButton()
            }
            return START_STICKY
        }

        showFloatingButton()

        // 处理屏幕捕获权限设置
        if (intent?.action == ACTION_SET_MEDIA_PROJECTION) {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
            val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_DATA)
            }

            android.util.Log.d("OverlayService", "收到MediaProjection权限数据: resultCode=$resultCode, data=$data")

            if (resultCode == android.app.Activity.RESULT_OK && data != null) {
                android.util.Log.d("OverlayService", "在前台服务中初始化MediaProjection")

                // 在前台服务中初始化MediaProjection
                promoteToMediaProjectionForegroundService()
                val success = screenCaptureManager.initializeMediaProjection(resultCode, data)

                if (success) {
                    android.util.Log.d("OverlayService", "MediaProjection初始化成功")
                    if (intent.getBooleanExtra(EXTRA_CAPTURE_AFTER_PERMISSION, false)) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                            { handleScreenshot() },
                            250
                        )
                    } else {
                        Toast.makeText(this, "截图权限已准备", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    android.util.Log.e("OverlayService", "MediaProjection初始化失败")
                    screenCaptureManager.reset()
                    Toast.makeText(this, "屏幕捕获权限初始化失败，请重试", Toast.LENGTH_LONG).show()
                }
            } else {
                android.util.Log.w("OverlayService", "权限数据无效")
            }
        } else if (intent == null || intent.action == null) {
            android.util.Log.d("OverlayService", "普通启动，不启用屏幕捕获")
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        isAnnotationModeActive = false
        activeInstance = null

        // 注销设置变更监听器
        val prefs = getSharedPreferences("annotation_preferences", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(preferencesListener)

        // 停止屏幕捕获（但不清除权限信息，以便下次使用）
        screenCaptureManager.stopMediaProjection()
        stylusInputRouter.dispose()

        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        // 保存悬浮按钮位置
        floatingButtonParams?.let { params ->
            saveButtonPosition(params.x, params.y)
        }

        // 保存工具栏位置
        saveToolbarPosition(toolbarOffsetX, toolbarOffsetY)

        // 保存工具栏布局方向
        saveToolbarOrientation(toolbarOrientation)

        // 保存绘图工具配置
        saveDrawingConfigs()

        removeOverlay()
        removeFloatingButton()
    }

    /**
     * 保存悬浮按钮位置到SharedPreferences。
     *
     * 同时保存**停靠边**和**纵向比例**：绝对像素只在当前屏幕方向下有意义，
     * 旋转后必须靠这两项还原，否则右边的球会被判成左侧、纵向位置也会漂移。
     */
    private fun saveButtonPosition(x: Int, y: Int) {
        val bounds = displayBounds()
        val size = floatingButtonSizePx()
        val usableHeight = (bounds.height() - size).coerceAtLeast(1)
        floatingButtonRatioY = (y.toFloat() / usableHeight).coerceIn(0f, 1f)
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(KEY_BUTTON_X, x)
            putInt(KEY_BUTTON_Y, y)
            putString(KEY_BUTTON_SIDE, floatingButtonSide.name)
            putFloat(KEY_BUTTON_RATIO_Y, floatingButtonRatioY)
            apply()
        }
    }

    /**
     * 保存工具栏位置到SharedPreferences
     */
    private fun saveToolbarPosition(x: Float, y: Float) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat(KEY_TOOLBAR_X, x)
            putFloat(KEY_TOOLBAR_Y, y)
            apply()
        }
    }

    /**
     * 保存工具栏布局方向到SharedPreferences
     */
    private fun saveToolbarOrientation(orientation: com.example.annotation.ui.ToolbarOrientation) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_TOOLBAR_ORIENTATION, orientation.name)
            apply()
        }
    }

    /**
     * 保存绘图工具配置到SharedPreferences
     */
    private fun saveDrawingConfigs() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val penConfig = drawingEngine.penConfig.value
        val highlighterConfig = drawingEngine.highlighterConfig.value
        val eraserConfig = drawingEngine.eraserConfig.value

        prefs.edit().apply {
            // 保存画笔配置
            putLong(KEY_PEN_COLOR, penConfig.color.value.toLong())
            putFloat(KEY_PEN_STROKE_WIDTH, penConfig.strokeWidth)

            // 保存荧光笔配置
            putLong(KEY_HIGHLIGHTER_COLOR, highlighterConfig.color.value.toLong())
            putFloat(KEY_HIGHLIGHTER_STROKE_WIDTH, highlighterConfig.strokeWidth)
            putFloat(KEY_HIGHLIGHTER_ALPHA, highlighterConfig.alpha)

            // 保存橡皮擦配置
            putFloat(KEY_ERASER_SIZE, eraserConfig.size)

            apply()
        }
    }

    /**
     * 从SharedPreferences恢复绘图工具配置
     */
    private fun restoreDrawingConfigs() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 恢复画笔配置
        if (prefs.contains(KEY_PEN_COLOR)) {
            val penColor = androidx.compose.ui.graphics.Color(prefs.getLong(KEY_PEN_COLOR, 0xFF000000).toULong())
            val penStrokeWidth = prefs.getFloat(KEY_PEN_STROKE_WIDTH, 5f)
            drawingEngine.updatePenConfig(color = penColor, strokeWidth = penStrokeWidth)
        }

        // 恢复荧光笔配置 - 优先使用用户在设置页面配置的透明度
        if (prefs.contains(KEY_HIGHLIGHTER_COLOR)) {
            val highlighterColor = androidx.compose.ui.graphics.Color(prefs.getLong(KEY_HIGHLIGHTER_COLOR, 0xFFFFFF00).toULong())
            val highlighterStrokeWidth = prefs.getFloat(KEY_HIGHLIGHTER_STROKE_WIDTH, 20f)
            // 从设置中读取透明度，如果设置中没有则使用之前保存的值
            val highlighterAlpha = preferencesManager.getHighlighterAlpha()
            drawingEngine.updateHighlighterConfig(
                color = highlighterColor,
                strokeWidth = highlighterStrokeWidth,
                alpha = highlighterAlpha
            )
        } else {
            // 如果没有保存过荧光笔配置，使用设置中的透明度作为默认值
            val highlighterAlpha = preferencesManager.getHighlighterAlpha()
            drawingEngine.updateHighlighterConfig(alpha = highlighterAlpha)
        }

        // 恢复橡皮擦配置
        if (prefs.contains(KEY_ERASER_SIZE)) {
            val eraserSize = prefs.getFloat(KEY_ERASER_SIZE, 30f)
            drawingEngine.updateEraserSize(eraserSize)
        }

        // 恢复自动折叠设置
        val autoCollapseToolbar = preferencesManager.getAutoCollapseToolbar()
        drawingEngine.setAutoCollapseToolbar(autoCollapseToolbar)
    }

    /**
     * 悬浮按钮边长估算值（像素）。
     *
     * 布局完成前拿不到 `view.width / height`，用 40dp 换算来参与定位与夹取；
     * 按钮本身是 40.dp，所以在正常密度下这个估算值就等于真实尺寸。
     */
    private fun floatingButtonSizePx(): Int =
        (40 * resources.displayMetrics.density).toInt().coerceAtLeast(1)

    /**
     * 当前方向下的真实屏幕区域（像素）。
     *
     * 悬浮窗的 `x / y` 是相对屏幕左上角的绝对坐标，必须用**含系统栏**的真实尺寸来夹取，
     * 否则横屏（导航栏在侧边）会算错可用宽度。
     */
    private fun displayBounds(): Rect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val bounds = getSystemService(WindowManager::class.java).currentWindowMetrics.bounds
                if (bounds.width() > 0 && bounds.height() > 0) return bounds
            } catch (e: Exception) {
                android.util.Log.w("OverlayService", "currentWindowMetrics 不可用，回退到 getRealMetrics", e)
            }
        }
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        return Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

    /**
     * 屏幕方向 / 尺寸变化回调。
     *
     * Service 不会因为配置变化被重建（Android 没有 `<service android:configChanges>` 这个属性），
     * 但窗口的绝对像素坐标也不会被系统换算 —— 必须在这里主动把窗口重新投影回可见区域。
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        android.util.Log.i(
            "OverlayService",
            "onConfigurationChanged: orientation=${newConfig.orientation} " +
                "screen=${newConfig.screenWidthDp}x${newConfig.screenHeightDp}dp"
        )
        applyDisplayGeometry()
    }

    /**
     * 把已有窗口重新投影到当前屏幕区域。
     *
     * 标注层是 MATCH_PARENT，WMS 会自动 resize，内部工具栏由 Compose 的 onSizeChanged 自愈；
     * 需要在这里处理的只有悬浮球这种使用绝对 `x / y` 的窗口。
     */
    private fun applyDisplayGeometry() {
        val bounds = displayBounds()
        val ball = floatingButton
        val params = floatingButtonParams
        if (ball == null || params == null) {
            android.util.Log.d(
                "OverlayService",
                "applyDisplayGeometry: bounds=${bounds.width()}x${bounds.height()}，当前无悬浮按钮"
            )
            return
        }

        // 布局未完成时宽高可能为 0，用 dp 估算兜底，避免这次旋转被整段跳过
        val estimated = floatingButtonSizePx()
        val buttonWidth = if (ball.width > 0) ball.width else estimated
        val buttonHeight = if (ball.height > 0) ball.height else estimated

        val beforeX = params.x
        val beforeY = params.y
        // 纵向按比例还原：直接沿用像素会把球夹到新屏幕的底边（用户看到的"掉到左下角"）
        val desiredY = if (floatingButtonRatioY >= 0f) {
            (floatingButtonRatioY * (bounds.height() - buttonHeight).coerceAtLeast(0)).toInt()
        } else {
            beforeY
        }
        // 停靠边用已保存的语义，不能由 beforeX 现推 —— 横屏变宽后"右半侧"的判断会翻转
        val (x, y) = OverlayGeometry.clampFloatingButton(
            x = beforeX,
            y = desiredY,
            buttonWidth = buttonWidth,
            buttonHeight = buttonHeight,
            screenWidth = bounds.width(),
            screenHeight = bounds.height(),
            preferredSide = floatingButtonSide
        )
        android.util.Log.i(
            "OverlayService",
            "applyDisplayGeometry: bounds=${bounds.width()}x${bounds.height()} " +
                "ball=${buttonWidth}x${buttonHeight} pos=($beforeX,$beforeY)->($x,$y)"
        )
        if (x != params.x || y != params.y) {
            params.x = x
            params.y = y
            try {
                windowManager.updateViewLayout(ball, params)
            } catch (e: IllegalArgumentException) {
                android.util.Log.w("OverlayService", "悬浮按钮已被系统移除，跳过位置修正", e)
            }
            saveButtonPosition(x, y)
        }
    }

    /**
     * 显示悬浮按钮
     */
    private fun showFloatingButton() {
        if (floatingButtonSuppressed) return
        if (floatingButton != null) return
        if (!Settings.canDrawOverlays(this)) {
            android.util.Log.w("OverlayService", "showFloatingButton: 悬浮窗权限不可用")
            return
        }

        // 用真实屏幕尺寸：窗口坐标就是以它为基准的
        val bounds = displayBounds()
        val screenWidth = bounds.width()
        val screenHeight = bounds.height()

        // 获取SharedPreferences
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true)

        // 计算初始位置
        val initialX: Int
        val initialY: Int

        // 按钮实际约 40dp，用 dp 换算而不是硬编码像素，否则高密度屏上会有一截出界
        val estimatedButtonSize = floatingButtonSizePx()

        // 停靠边优先取保存的语义状态；老版本没有这项时，按历史 x 推断一次
        val savedX = prefs.getInt(KEY_BUTTON_X, screenWidth - estimatedButtonSize)
        floatingButtonSide = prefs.getString(KEY_BUTTON_SIDE, null)
            ?.let { name -> runCatching { OverlayGeometry.FloatingSide.valueOf(name) }.getOrNull() }
            ?: OverlayGeometry.floatingSideOf(savedX, estimatedButtonSize, screenWidth)

        if (isFirstLaunch) {
            // 第一次启动：位于屏幕右边中间位置
            initialX = OverlayGeometry.floatingButtonX(
                floatingButtonSide, estimatedButtonSize, screenWidth
            )
            initialY = (screenHeight - estimatedButtonSize) / 2

            // 标记已经不是第一次启动
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
        } else {
            // 横向用停靠边还原、纵向用比例还原：绝对像素只在保存它的那个方向下才有效。
            // 历史版本存的是另一套方向下的像素，所以两条路都还要再夹一次。
            val usableHeight = (screenHeight - estimatedButtonSize).coerceAtLeast(1)
            val ratioY = prefs.getFloat(KEY_BUTTON_RATIO_Y, -1f)
            floatingButtonRatioY = ratioY
            val restoredY = if (ratioY >= 0f) {
                (ratioY * usableHeight).toInt()
            } else {
                prefs.getInt(KEY_BUTTON_Y, screenHeight / 2)
            }
            val clamped = OverlayGeometry.clampFloatingButton(
                x = savedX,
                y = restoredY,
                buttonWidth = estimatedButtonSize,
                buttonHeight = estimatedButtonSize,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                preferredSide = floatingButtonSide
            )
            initialX = clamped.first
            initialY = clamped.second
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // 不再使用 FLAG_LAYOUT_NO_LIMITS：悬浮球不需要越出屏幕，
            // 保留系统的越界夹取能力可以在任何遗漏场景下兜底。
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }

        // 保存params引用
        floatingButtonParams = params
        android.util.Log.i(
            "OverlayService",
            "showFloatingButton: bounds=${bounds.width()}x${bounds.height()} " +
                "pos=($initialX,$initialY) estimated=$estimatedButtonSize"
        )

        floatingButton = ComposeView(this).apply {
            // 设置Lifecycle和SavedStateRegistry
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)

            setContent {
                AnnotationTheme(themeMode = themeModeState.value) {
                    DraggableFloatingButton(
                        onClick = { showOverlay() },
                        onDrag = { deltaX, deltaY ->
                            params.x += deltaX.toInt()
                            params.y += deltaY.toInt()
                            windowManager.updateViewLayout(this@apply, params)
                        },
                        onDragEnd = {
                            // 自动吸附到屏幕边缘
                            snapToEdge(params, this@apply)
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(floatingButton, params)
        } catch (e: Exception) {
            android.util.Log.e("OverlayService", "添加悬浮按钮失败", e)
            floatingButton = null
            floatingButtonParams = null
            stopSelf()
        }
    }

    /**
     * 将悬浮按钮吸附到屏幕最近的边缘
     */
    private fun snapToEdge(params: WindowManager.LayoutParams, view: View) {
        // 布局尚未完成时宽高为 0，此时算出来的目标位置会把按钮推出屏幕
        if (view.width <= 0 || view.height <= 0) return

        val bounds = displayBounds()
        // 拖拽结束是用户表达"想停在哪一侧"的唯一时机，在这里更新停靠边
        floatingButtonSide = OverlayGeometry.floatingSideOf(
            params.x, view.width, bounds.width()
        )
        val (targetX, targetY) = OverlayGeometry.clampFloatingButton(
            x = params.x,
            y = params.y,
            buttonWidth = view.width,
            buttonHeight = view.height,
            screenWidth = bounds.width(),
            screenHeight = bounds.height(),
            preferredSide = floatingButtonSide
        )

        // 使用动画平滑移动到边缘
        val startX = params.x
        val startY = params.y

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                params.x = (startX + (targetX - startX) * progress).toInt()
                params.y = (startY + (targetY - startY) * progress).toInt()
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (e: Exception) {
                    // 如果view已经被移除，忽略错误
                }
            }
            // 动画结束后保存位置
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationCancel(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    // 保存最终位置到SharedPreferences
                    saveButtonPosition(params.x, params.y)
                }
            })
            start()
        }
    }

    /**
     * 显示标注界面
     */
    private fun showOverlay() {
        if (overlayView != null) return
        if (!Settings.canDrawOverlays(this)) {
            android.util.Log.w("OverlayService", "showOverlay: 悬浮窗权限已失效")
            removeFloatingButton()
            stopSelf()
            return
        }
        if (preferencesManager.requiresStylusKeyBridge() &&
            !GestureForwardingAccessibilityService.isReady
        ) {
            requestStylusKeyBridge()
            return
        }
        pendingOverlayAfterStylusBridge = false

        // 隐藏悬浮按钮
        removeFloatingButton()

        // 上一次若因为截图中断而把工具栏藏起来了，这里必须复位，否则进标注模式就是"白屏"
        annotationToolbarSeenVisible = false
        toolbarVisibleState.value = true

        // 获取屏幕尺寸（与标注层窗口同一套坐标：含系统栏的真实像素）
        val bounds = displayBounds()
        val screenWidth = bounds.width().toFloat()
        val screenHeight = bounds.height().toFloat()

        // 获取SharedPreferences
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isToolbarFirstLaunch = prefs.getBoolean(KEY_TOOLBAR_FIRST_LAUNCH, true)

        // 读取工具栏布局方向
        val orientationName = prefs.getString(KEY_TOOLBAR_ORIENTATION, com.example.annotation.ui.ToolbarOrientation.VERTICAL.name)
        toolbarOrientation = try {
            com.example.annotation.ui.ToolbarOrientation.valueOf(orientationName ?: com.example.annotation.ui.ToolbarOrientation.VERTICAL.name)
        } catch (e: IllegalArgumentException) {
            com.example.annotation.ui.ToolbarOrientation.VERTICAL
        }

        // 计算工具栏初始位置
        if (isToolbarFirstLaunch) {
            // 第一次启动：位于屏幕右边中间位置
            // offsetX为靠右的标记值，实际会对齐到右侧
            toolbarOffsetX = screenWidth - OverlayGeometry.EDGE_INSET_PX
            toolbarOffsetY = screenHeight / 2

            // 标记已经不是第一次启动
            prefs.edit().putBoolean(KEY_TOOLBAR_FIRST_LAUNCH, false).apply()
        } else {
            // 读取上次保存的位置
            toolbarOffsetX = prefs.getFloat(KEY_TOOLBAR_X, screenWidth - 16f)
            toolbarOffsetY = prefs.getFloat(KEY_TOOLBAR_Y, screenHeight / 2)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        overlayParams = params

        val inputRoot = object : FrameLayout(this) {
            override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
                if (stylusInputRouter.processMotionEvent(event)) return true
                return super.dispatchGenericMotionEvent(event)
            }

            override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                val stylusHandled = stylusInputRouter.processMotionEvent(event)
                return super.dispatchTouchEvent(event) || stylusHandled
            }
        }
        inputRoot.setViewTreeLifecycleOwner(this@OverlayService)
        inputRoot.setViewTreeSavedStateRegistryOwner(this@OverlayService)
        val composeView = ComposeView(this).apply {
            // 设置Lifecycle和SavedStateRegistry
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)

            setContent {
                AnnotationTheme(themeMode = themeModeState.value) {
                    // 读取工具栏可见性状态
                    val toolbarVisible by toolbarVisibleState

                    OverlayContent(
                        drawingEngine = drawingEngine,
                        onExit = {
                            removeOverlay()
                            showFloatingButton()
                        },
                        onScreenshot = {
                            handleScreenshot()
                        },
                        initialOffsetX = toolbarOffsetX,
                        initialOffsetY = toolbarOffsetY,
                        initialOrientation = toolbarOrientation,
                        onToolbarPositionChanged = { x, y ->
                            // 更新工具栏位置
                            toolbarOffsetX = x
                            toolbarOffsetY = y
                            // 立即保存位置
                            saveToolbarPosition(x, y)
                        },
                        onOrientationChanged = { orientation ->
                            // 更新布局方向
                            toolbarOrientation = orientation
                            // 立即保存布局方向
                            saveToolbarOrientation(orientation)
                        },
                        onOverlayLayout = { layout ->
                            if (!annotationToolbarSeenVisible && layout.isVisible()) {
                                annotationToolbarSeenVisible = true
                                android.util.Log.i(
                                    "OverlayService",
                                    "标注工具栏已可见: (${layout.toolbarLeft},${layout.toolbarTop})" +
                                        "-(${layout.toolbarRight},${layout.toolbarBottom}) " +
                                        "container=${layout.containerWidth}x${layout.containerHeight}"
                                )
                            }
                        },
                        toolbarVisible = toolbarVisible,
                        preferencesManager = preferencesManager
                    )
                }
            }
        }
        inputRoot.addView(
            composeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        overlayView = inputRoot

        try {
            windowManager.addView(overlayView, params)
            isAnnotationModeActive = true
            // 兜底复查：标注层会吞掉全部触摸，工具栏没出来就等于把用户锁死在透明画布上。
            // 必须捕获本次创建的 view：若用字段，用户在 3 秒内退出又重进时，
            // 旧任务会拿新窗口做校验，把刚进去的标注模式误杀。
            val createdView = overlayView
            mainHandler.postDelayed(
                { verifyAnnotationUi(createdView) },
                ANNOTATION_UI_TIMEOUT_MS
            )
            android.util.Log.i(
                "OverlayService",
                "showOverlay ok: bounds=${bounds.width()}x${bounds.height()} " +
                    "orientation=$toolbarOrientation toolbarOffset=(${toolbarOffsetX},${toolbarOffsetY})"
            )
        } catch (e: Exception) {
            android.util.Log.e("OverlayService", "添加标注悬浮层失败", e)
            overlayView = null
            overlayParams = null
            isAnnotationModeActive = false
            showFloatingButton()
        }
    }

    /**
     * 移除标注界面
     */
    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: IllegalArgumentException) {
                android.util.Log.w("OverlayService", "标注悬浮层已被系统移除", e)
            }
            overlayView = null
        }
        overlayParams = null
        isAnnotationModeActive = false
        annotationToolbarSeenVisible = false
        stylusInputRouter.dispose()
    }

    /**
     * 标注界面自检。
     *
     * 进入标注模式一小段时间后，如果工具栏**从未**真正出现在可见区域，说明界面没渲染出来
     * （或落到了窗口外）。此时用户既看不到入口、也点不到「退出」，连返回手势都会被画布吃掉，
     * 只能杀进程 —— 主动退出并交还悬浮球，远比把用户锁死在透明画布上要好。
     *
     * 用"曾经可见"而不是"当前可见"，是为了避免用户正常拖拽的中间态被误判成故障。
     */
    private fun verifyAnnotationUi(expected: View?) {
        if (overlayView !== expected || !isAnnotationModeActive) return
        if (annotationToolbarSeenVisible) return

        android.util.Log.w(
            "OverlayService",
            "标注工具栏在 ${ANNOTATION_UI_TIMEOUT_MS}ms 内未出现在可见区域，自动退出标注模式"
        )
        removeOverlay()
        showFloatingButton()
        Toast.makeText(this, "标注界面未能显示，已自动退出", Toast.LENGTH_LONG).show()
    }

    private fun requestStylusKeyBridge() {
        pendingOverlayAfterStylusBridge = true
        Toast.makeText(
            this,
            "请启用“粉笔标注手势服务”，以拦截手写笔厂商原始功能",
            Toast.LENGTH_LONG
        ).show()
        runCatching {
            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { error ->
            pendingOverlayAfterStylusBridge = false
            android.util.Log.e("OverlayService", "无法打开无障碍设置", error)
        }
    }

    private fun handleStylusKeyBridgeConnected() {
        mainHandler.post {
            if (!pendingOverlayAfterStylusBridge) return@post
            pendingOverlayAfterStylusBridge = false
            showOverlay()
        }
    }

    private fun handleStylusKeyBridgeDisconnected() {
        mainHandler.post {
            pendingOverlayAfterStylusBridge = false
            if (!isAnnotationModeActive || !preferencesManager.requiresStylusKeyBridge()) {
                return@post
            }
            removeOverlay()
            showFloatingButton()
            Toast.makeText(
                this,
                "手写笔按键桥接已断开，已退出标注以避免触发厂商原始功能",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateOverlayTouchable(touchable: Boolean) {
        val view = overlayView ?: return
        val params = overlayParams ?: return
        params.flags = if (touchable) {
            params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun executeStylusAction(action: StylusButtonAction) {
        android.util.Log.d("OverlayService", "executeStylusAction action=$action")
        when (action) {
            StylusButtonAction.VENDOR_DEFAULT -> Unit
            StylusButtonAction.NONE -> Unit
            StylusButtonAction.PEN -> drawingEngine.setTool(DrawingTool.PEN)
            StylusButtonAction.HIGHLIGHTER -> drawingEngine.setTool(DrawingTool.HIGHLIGHTER)
            StylusButtonAction.ERASER -> drawingEngine.setTool(DrawingTool.ERASER)
            StylusButtonAction.UNDO -> drawingEngine.undo()
            StylusButtonAction.REDO -> drawingEngine.redo()
            StylusButtonAction.CLEAR -> drawingEngine.clearAll()
            StylusButtonAction.SCREENSHOT -> handleStylusScreenshot()
            StylusButtonAction.EXIT_ANNOTATION -> {
                removeOverlay()
                showFloatingButton()
            }
        }
    }

    /**
     * 移除悬浮按钮
     */
    private fun removeFloatingButton() {
        floatingButton?.let {
            try {
                windowManager.removeView(it)
            } catch (e: IllegalArgumentException) {
                android.util.Log.w("OverlayService", "悬浮按钮已被系统移除", e)
            }
            floatingButton = null
        }
    }

    private var floatingButtonSuppressed = false

    private fun updateFloatingButtonSuppressed(suppressed: Boolean) {
        floatingButtonSuppressed = suppressed
        if (suppressed) {
            removeFloatingButton()
        } else if (!isAnnotationModeActive) {
            showFloatingButton()
        }
    }

    private fun handleStylusScreenshot() {
        toolbarVisibleState.value = false
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (GestureForwardingAccessibilityService.takeSystemScreenshot()) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                    { toolbarVisibleState.value = true },
                    600L
                )
            } else {
                toolbarVisibleState.value = true
                handleScreenshot()
            }
        }, 100L)
    }

    /**
     * 处理截图
     */
    private fun handleScreenshot() {
        android.util.Log.d("OverlayService", "========== 开始处理截图请求 ==========")
        android.util.Log.d("OverlayService", "当前hasPermission(): ${screenCaptureManager.hasPermission()}")
        android.util.Log.d("OverlayService", "当前hasCachedPermissionData(): ${screenCaptureManager.hasCachedPermissionData()}")

        // 检查是否有屏幕捕获权限
        if (!screenCaptureManager.hasPermission()) {
            android.util.Log.w("OverlayService", "MediaProjection未初始化")

            // 检查是否有缓存的权限数据
            val cachedData = screenCaptureManager.getCachedPermissionData()
            if (cachedData != null) {
                android.util.Log.d("OverlayService", "尝试使用缓存的权限数据初始化MediaProjection")
                val (resultCode, data) = cachedData
                promoteToMediaProjectionForegroundService()
                val success = screenCaptureManager.initializeMediaProjection(resultCode, data)

                if (!success) {
                    android.util.Log.e("OverlayService", "初始化MediaProjection失败")
                    Toast.makeText(this, "屏幕捕获权限已过期，请重新授予", Toast.LENGTH_LONG).show()
                    return
                }

                android.util.Log.d("OverlayService", "从缓存成功初始化MediaProjection")
                // 继续执行截图
            } else {
                android.util.Log.e("OverlayService", "没有缓存的权限数据")
                val permissionIntent = Intent(this, ScreenCapturePermissionActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(permissionIntent)
                return
            }
        }

        android.util.Log.d("OverlayService", "MediaProjection已就绪，准备隐藏UI并开始屏幕捕获")

        // 步骤1：隐藏工具栏UI
        toolbarVisibleState.value = false
        android.util.Log.d("OverlayService", "工具栏已隐藏")

        // 步骤2：延迟一小段时间以确保UI更新完成，然后开始截图
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            android.util.Log.d("OverlayService", "开始屏幕捕获")

            // 使用屏幕捕获管理器进行截图
            screenCaptureManager.captureScreenWithAnnotation(
                drawingEngine = drawingEngine,
                onSuccess = { uri ->
                    android.util.Log.d("OverlayService", "========== 截图成功: $uri ==========")

                    // 步骤3：截图成功后，恢复工具栏显示
                    toolbarVisibleState.value = true
                    screenCaptureManager.reset()
                    android.util.Log.d("OverlayService", "工具栏已恢复显示")

                    // 显示成功提示（这个Toast会出现在截图完成后，所以不会被捕获）
                    Toast.makeText(this, "截图已保存到相册", Toast.LENGTH_SHORT).show()
                },
                onError = { exception ->
                    android.util.Log.e("OverlayService", "========== 截图失败 ==========", exception)

                    // 步骤3：截图失败后，也要恢复工具栏显示
                    toolbarVisibleState.value = true
                    screenCaptureManager.reset()
                    android.util.Log.d("OverlayService", "工具栏已恢复显示")

                    Toast.makeText(this, "截图失败: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        }, 100) // 延迟100ms等待UI隐藏动画完成
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "粉笔标注服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持标注功能在后台运行"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("粉笔标注")
            .setContentText("点击悬浮按钮开始标注")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}

@Composable
private fun DraggableFloatingButton(
    onClick: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    isDragging = false
                },
                onDrag = { change, dragAmount ->
                    isDragging = true
                    onDrag(dragAmount.x, dragAmount.y)
                    change.consume()
                },
                onDragEnd = {
                    if (isDragging) {
                        // 调用吸附到边缘的回调
                        onDragEnd()
                    } else {
                        onClick()
                    }
                    isDragging = false
                }
            )
        }
    ) {
        androidx.compose.material3.FloatingActionButton(
            onClick = {
                if (!isDragging) {
                    onClick()
                }
            },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            contentColor = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.size(40.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Outlined.Create,
                contentDescription = "开始标注"
            )
        }
    }
}

@Composable
private fun FloatingButtonContent(onClick: () -> Unit) {
    androidx.compose.material3.FloatingActionButton(
        onClick = onClick,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        contentColor = androidx.compose.ui.graphics.Color.White,
        modifier = Modifier.size(40.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Outlined.Create,
            contentDescription = "开始标注"
        )
    }
}
