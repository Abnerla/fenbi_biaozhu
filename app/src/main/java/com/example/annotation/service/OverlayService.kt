package com.example.annotation.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
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
import com.example.annotation.model.StylusProfile
import com.example.annotation.ui.OverlayContent
import com.example.annotation.ui.theme.AnnotationTheme
import com.example.annotation.utils.AppThemeMode
import com.example.annotation.utils.PreferencesManager
import com.example.annotation.utils.ScreenshotHelper
import com.example.annotation.utils.ScreenCaptureManager
import com.example.annotation.utils.StylusButtonGestureDetector
import android.widget.Toast
import com.example.annotation.ScreenCapturePermissionActivity

/**
 * 悬浮窗服务
 */
class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var floatingButton: ComposeView? = null
    private val drawingEngine = DrawingEngine()
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var screenCaptureManager: ScreenCaptureManager
    private val stylusButtonDetector by lazy {
        StylusButtonGestureDetector { button, pressType ->
            val action = preferencesManager.getStylusButtonMappings().actionFor(button, pressType)
            executeStylusAction(action)
        }
    }
    private val themeModeState = mutableStateOf(AppThemeMode.SYSTEM)

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
        private const val KEY_BUTTON_X = "button_x"
        private const val KEY_BUTTON_Y = "button_y"
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
        stylusButtonDetector.dispose()

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
     * 保存悬浮按钮位置到SharedPreferences
     */
    private fun saveButtonPosition(x: Int, y: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(KEY_BUTTON_X, x)
            putInt(KEY_BUTTON_Y, y)
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
     * 显示悬浮按钮
     */
    private fun showFloatingButton() {
        if (floatingButton != null) return
        if (!Settings.canDrawOverlays(this)) {
            android.util.Log.w("OverlayService", "showFloatingButton: 悬浮窗权限不可用")
            return
        }

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // 获取SharedPreferences
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true)

        // 计算初始位置
        val initialX: Int
        val initialY: Int

        if (isFirstLaunch) {
            // 第一次启动：位于屏幕右边中间位置
            // 注意：需要在按钮创建后才能获取其实际宽高，这里先用估计值
            val estimatedButtonSize = 100 // FAB的估计大小（像素）- 减少后约40dp
            initialX = screenWidth - estimatedButtonSize
            initialY = (screenHeight - estimatedButtonSize) / 2

            // 标记已经不是第一次启动
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
        } else {
            // 读取上次保存的位置
            initialX = prefs.getInt(KEY_BUTTON_X, screenWidth - 100)
            initialY = prefs.getInt(KEY_BUTTON_Y, screenHeight / 2)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }

        // 保存params引用
        floatingButtonParams = params

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
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // 计算悬浮按钮的中心位置
        val centerX = params.x + view.width / 2
        val centerY = params.y + view.height / 2

        // 确定最近的边缘（左边或右边）
        val targetX = if (centerX < screenWidth / 2) {
            // 靠左边
            0
        } else {
            // 靠右边
            screenWidth - view.width
        }

        // Y轴保持在屏幕范围内
        val targetY = params.y.coerceIn(0, screenHeight - view.height)

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

        // 隐藏悬浮按钮
        removeFloatingButton()

        // 获取屏幕尺寸
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()

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
            // offsetX为靠右的标记值（screenWidth - 16），实际会对齐到右侧
            toolbarOffsetX = screenWidth - 16f
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        overlayParams = params

        overlayView = ComposeView(this).apply {
            // 设置Lifecycle和SavedStateRegistry
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setOnGenericMotionListener { _, event ->
                processStylusGenericMotion(event)
                false
            }

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
                        toolbarVisible = toolbarVisible,
                        preferencesManager = preferencesManager
                    )
                }
            }
        }

        try {
            windowManager.addView(overlayView, params)
            isAnnotationModeActive = true
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

    private fun processStylusGenericMotion(event: MotionEvent) {
        if (!preferencesManager.getStylusEnabled()) return
        val configured = preferencesManager.getStylusProfile()
        val resolved = configured.resolvedForDevice()
        val primaryMask = if (configured == StylusProfile.CUSTOM) {
            preferencesManager.getStylusCustomPrimaryMask()
        } else {
            resolved.primaryButtonMask
        }
        val secondaryMask = if (configured == StylusProfile.CUSTOM) {
            preferencesManager.getStylusCustomSecondaryMask()
        } else {
            resolved.secondaryButtonMask
        }
        stylusButtonDetector.process(event.buttonState, primaryMask, secondaryMask, event.eventTime)
    }

    private fun executeStylusAction(action: StylusButtonAction) {
        when (action) {
            StylusButtonAction.NONE -> Unit
            StylusButtonAction.PEN -> drawingEngine.setTool(DrawingTool.PEN)
            StylusButtonAction.HIGHLIGHTER -> drawingEngine.setTool(DrawingTool.HIGHLIGHTER)
            StylusButtonAction.ERASER -> drawingEngine.setTool(DrawingTool.ERASER)
            StylusButtonAction.UNDO -> drawingEngine.undo()
            StylusButtonAction.REDO -> drawingEngine.redo()
            StylusButtonAction.CLEAR -> drawingEngine.clearAll()
            StylusButtonAction.SCREENSHOT -> handleScreenshot()
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
