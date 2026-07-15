package com.example.annotation

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.annotation.model.UpdateCheckResult
import com.example.annotation.model.VersionInfo
import com.example.annotation.service.OverlayService
import com.example.annotation.ui.*
import com.example.annotation.ui.theme.AnnotationTheme
import com.example.annotation.ui.theme.SystemModeIcon
import com.example.annotation.ui.theme.LightModeIcon
import com.example.annotation.ui.theme.DarkModeIcon
import com.example.annotation.ui.theme.iosSwitchColors
import com.example.annotation.update.ApkDownloader
import com.example.annotation.update.UpdateManager
import com.example.annotation.utils.PermissionHelper
import com.example.annotation.utils.PreferencesManager
import com.example.annotation.utils.AppThemeMode
import com.example.annotation.utils.ScreenCaptureManager
import com.example.annotation.utils.StylusInputMonitor
import kotlinx.coroutines.launch
import android.app.Activity

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var updateManager: UpdateManager
    private lateinit var apkDownloader: ApkDownloader
    private lateinit var screenCaptureManager: ScreenCaptureManager

    private var currentScreen by mutableStateOf("main")
    private var showUpdateDialog by mutableStateOf(false)
    private var updateInfo by mutableStateOf<Pair<VersionInfo, Boolean>?>(null)
    private var downloadProgress by mutableStateOf<Int?>(null)
    private var showNoUpdateSnackbar by mutableStateOf(false)
    private var updateCheckMessage by mutableStateOf("")

    private var permissionStatus by mutableStateOf(
        com.example.annotation.utils.PermissionStatus(
            hasOverlay = false,
            hasNotification = false,
            hasStorage = false,
            hasScreenCapture = false,
            hasForegroundServiceMediaProjection = false
        )
    )

    // 服务运行状态
    private var isServiceRunning by mutableStateOf(false)
    private var themeMode by mutableStateOf(AppThemeMode.SYSTEM)

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        StylusInputMonitor.publishMotion(event)
        return super.dispatchGenericMotionEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        StylusInputMonitor.publishMotion(event)
        return super.dispatchTouchEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        StylusInputMonitor.publishKey(event)
        return super.dispatchKeyEvent(event)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        updatePermissionStatus()
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        updatePermissionStatus()
    }

    private val foregroundServicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        updatePermissionStatus()
        if (!isGranted) {
            android.widget.Toast.makeText(
                this,
                "需要前台服务权限才能使用标注功能",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("MainActivity", "屏幕捕获权限结果: resultCode=${result.resultCode}, data=${result.data}")

        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            android.util.Log.d("MainActivity", "屏幕捕获权限已授予")

            // 1. 保存权限数据到ScreenCaptureManager
            screenCaptureManager.savePermissionData(result.resultCode, result.data!!)

            // 仅缓存授权令牌；真正的屏幕捕获在用户点击截图时才初始化。
            updatePermissionStatus()

            android.widget.Toast.makeText(
                this,
                "截图权限已准备，将在截图时按需启用",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else {
            android.util.Log.w("MainActivity", "屏幕捕获权限被拒绝")
            android.widget.Toast.makeText(
                this,
                "已取消截图授权，不影响标注功能使用",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化 PreferencesManager
        preferencesManager = PreferencesManager(this)
        themeMode = preferencesManager.getThemeMode()

        // 初始化更新管理器
        updateManager = UpdateManager(this)
        apkDownloader = ApkDownloader(this)

        // 获取ScreenCaptureManager单例实例
        screenCaptureManager = ScreenCaptureManager.getInstance(this)

        // 在onCreate中初始化权限状态
        updatePermissionStatus()

        // 检查更新（根据设置决定是否自动检查）
        if (preferencesManager.getAutoCheckUpdate()) {
            checkForUpdate(isManual = false)
        }

        setContent {
            AnnotationTheme(themeMode = themeMode) {
                val snackbarHostState = remember { SnackbarHostState() }

                // 显示提示消息
                LaunchedEffect(showNoUpdateSnackbar) {
                    if (showNoUpdateSnackbar) {
                        snackbarHostState.showSnackbar(
                            message = updateCheckMessage,
                            duration = SnackbarDuration.Short
                        )
                        showNoUpdateSnackbar = false
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { paddingValues ->
                    // 处理返回事件
                    BackHandler(enabled = currentScreen != "main") {
                        when (currentScreen) {
                            "settings" -> currentScreen = "main"
                            "toolbar_settings" -> currentScreen = "settings"
                            "highlighter_settings" -> currentScreen = "settings"
                            "stylus_settings" -> currentScreen = "settings"
                            "gesture_settings" -> currentScreen = "settings"
                            "permissions" -> currentScreen = "settings"
                            "other_settings" -> currentScreen = "settings"
                            "coming_soon" -> currentScreen = "main"
                            "about" -> currentScreen = "settings"
                            "developer" -> currentScreen = "settings"
                            "feedback" -> currentScreen = "settings"
                            "help" -> currentScreen = "settings"
                            else -> currentScreen = "main"
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (currentScreen) {
                            "main" -> MainScreen(
                                permissionStatus = permissionStatus,
                                isServiceRunning = isServiceRunning,
                                preferencesManager = preferencesManager,
                                themeMode = themeMode,
                                onThemeModeChange = { mode ->
                                    themeMode = mode
                                    preferencesManager.setThemeMode(mode)
                                },
                                onRequestOverlayPermission = {
                                    PermissionHelper.requestOverlayPermission(this)
                                },
                                onRequestNotificationPermission = {
                                    PermissionHelper.requestNotificationPermission(this)
                                },
                                onRequestStoragePermission = {
                                    PermissionHelper.requestStoragePermission(this)
                                },
                                onRequestScreenCapturePermission = {
                                    requestScreenCapturePermission()
                                },
                                onRequestForegroundServicePermission = {
                                    requestForegroundServicePermission()
                                },
                                onStartService = {
                                    OverlayService.start(this)
                                    isServiceRunning = true
                                },
                                onStopService = {
                                    OverlayService.stop(this)
                                    isServiceRunning = false
                                },
                                onOpenSettings = {
                                    currentScreen = "settings"
                                },
                                onOpenComingSoon = {
                                    currentScreen = "coming_soon"
                                }
                            )
                            "settings" -> SettingsScreen(
                                onNavigateToToolbarSettings = {
                                    currentScreen = "toolbar_settings"
                                },
                                onNavigateToHighlighterSettings = {
                                    currentScreen = "highlighter_settings"
                                },
                                onNavigateToStylusSettings = {
                                    currentScreen = "stylus_settings"
                                },
                                onNavigateToGestureSettings = {
                                    currentScreen = "gesture_settings"
                                },
                                onNavigateToPermissions = {
                                    currentScreen = "permissions"
                                },
                                onNavigateToOtherSettings = {
                                    currentScreen = "other_settings"
                                },
                                onNavigateToAbout = {
                                    currentScreen = "about"
                                },
                                onNavigateToDeveloper = {
                                    currentScreen = "developer"
                                },
                                onNavigateToFeedback = {
                                    currentScreen = "feedback"
                                },
                                onNavigateToHelp = {
                                    currentScreen = "help"
                                },
                                onNavigateBack = {
                                    currentScreen = "main"
                                }
                            )
                            "toolbar_settings" -> ToolbarSettingsScreen(
                                preferencesManager = preferencesManager,
                                onNavigateBack = {
                                    currentScreen = "settings"
                                }
                            )
                            "highlighter_settings" -> HighlighterSettingsScreen(
                                preferencesManager = preferencesManager,
                                onNavigateBack = {
                                    currentScreen = "settings"
                                }
                            )
                            "stylus_settings" -> StylusSettingsScreen(
                                preferencesManager = preferencesManager,
                                onNavigateBack = { currentScreen = "settings" }
                            )
                            "gesture_settings" -> GestureSettingsScreen(
                                preferencesManager = preferencesManager,
                                onNavigateBack = { currentScreen = "settings" }
                            )
                            "permissions" -> PermissionsScreen(
                                permissionStatus = permissionStatus,
                                onRequestOverlayPermission = {
                                    PermissionHelper.requestOverlayPermission(this)
                                },
                                onRequestNotificationPermission = {
                                    PermissionHelper.requestNotificationPermission(this)
                                },
                                onRequestStoragePermission = {
                                    PermissionHelper.requestStoragePermission(this)
                                },
                                onRequestScreenCapturePermission = {
                                    requestScreenCapturePermission()
                                },
                                onRequestForegroundServicePermission = {
                                    requestForegroundServicePermission()
                                },
                                onNavigateBack = {
                                    currentScreen = "settings"
                                }
                            )
                            "about" -> AboutScreen(
                                currentVersion = updateManager.getCurrentVersionName(),
                                preferencesManager = preferencesManager,
                                onCheckUpdate = { checkForUpdate(isManual = true) },
                                onNavigateBack = {
                                    currentScreen = "settings"
                                }
                            )
                            "developer" -> DeveloperScreen(
                                onNavigateBack = {
                                    currentScreen = "settings"
                                }
                            )
                            "feedback" -> FeedbackScreen(
                                onNavigateBack = {
                                    currentScreen = "settings"
                                },
                                customInfo = buildCustomInfo()
                            )
                            "help" -> HelpScreen(
                                onNavigateBack = {
                                    currentScreen = "settings"
                                }
                            )
                            "other_settings" -> OtherSettingsScreen(
                                preferencesManager = preferencesManager,
                                onNavigateBack = {
                                    currentScreen = "settings"
                                }
                            )
                            "coming_soon" -> ComingSoonScreen(
                                onNavigateBack = {
                                    currentScreen = "main"
                                }
                            )
                        }

                        // 显示更新对话框
                        if (showUpdateDialog && updateInfo != null) {
                            val (versionInfo, isForceUpdate) = updateInfo!!
                            UpdateDialog(
                                versionInfo = versionInfo,
                                isForceUpdate = isForceUpdate,
                                downloadProgress = downloadProgress,
                                onUpdate = {
                                    downloadAndInstallUpdate(versionInfo)
                                },
                                onDismiss = {
                                    showUpdateDialog = false
                                    updateInfo = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    /**
     * 请求屏幕捕获权限
     */
    private fun requestScreenCapturePermission() {
        try {
            android.util.Log.d("MainActivity", "请求屏幕捕获权限")
            val captureIntent = screenCaptureManager.createScreenCaptureIntent()
            screenCaptureLauncher.launch(captureIntent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "请求屏幕捕获权限失败", e)
            android.widget.Toast.makeText(
                this,
                "请求权限失败: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * 请求前台服务媒体投影权限
     */
    private fun requestForegroundServicePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            android.util.Log.d("MainActivity", "请求前台服务媒体投影权限")
            foregroundServicePermissionLauncher.launch(android.Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION)
        }
    }

    /**
     * 检查服务是否正在运行
     */
    private fun checkServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (OverlayService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    /**
     * 更新权限状态
     */
    private fun updatePermissionStatus() {
        android.util.Log.d("MainActivity", "更新权限状态")

        // 检查屏幕捕获权限状态 - 需要检查实际的缓存数据，而不仅仅是标记
        val hasScreenCapture = screenCaptureManager.hasCachedPermissionData()

        android.util.Log.d("MainActivity", "屏幕捕获权限状态: $hasScreenCapture")

        val previousStatus = permissionStatus
        permissionStatus = PermissionHelper.checkAllPermissions(
            this,
            hasScreenCapture = hasScreenCapture
        )

        android.util.Log.d("MainActivity", "权限状态已更新: overlay=${permissionStatus.hasOverlay}, " +
                "notification=${permissionStatus.hasNotification}, storage=${permissionStatus.hasStorage}, " +
                "screenCapture=${permissionStatus.hasScreenCapture}")

        // 检查服务运行状态
        isServiceRunning = checkServiceRunning()
        android.util.Log.d("MainActivity", "服务运行状态: $isServiceRunning")

        // 屏幕捕获是按需权限，不阻止基础标注服务启动。
        if (permissionStatus.canRunAnnotationService() &&
            !previousStatus.canRunAnnotationService() &&
            !isServiceRunning
        ) {
            android.util.Log.d("MainActivity", "基础权限已授予，自动启动服务")
            OverlayService.start(this)
            isServiceRunning = true
        }
    }

    /**
     * 检查更新
     * @param isManual 是否为手动检查（true: 用户手动点击检查，false: 自动检查）
     */
    private fun checkForUpdate(isManual: Boolean = false) {
        lifecycleScope.launch {
            when (val result = updateManager.checkForUpdate()) {
                is UpdateCheckResult.HasUpdate -> {
                    updateInfo = Pair(result.versionInfo, result.isForceUpdate)
                    showUpdateDialog = true
                }
                is UpdateCheckResult.NoUpdate -> {
                    // 如果是手动检查，显示提示
                    if (isManual) {
                        updateCheckMessage = "已是最新版本"
                        showNoUpdateSnackbar = true
                    }
                }
                is UpdateCheckResult.Error -> {
                    // 如果是手动检查，显示错误提示
                    if (isManual) {
                        updateCheckMessage = "检查更新失败，请检查网络连接"
                        showNoUpdateSnackbar = true
                    }
                }
            }
        }
    }

    /**
     * 下载并安装更新
     */
    private fun downloadAndInstallUpdate(versionInfo: VersionInfo) {
        lifecycleScope.launch {
            // 开始下载
            downloadProgress = 0

            val apkFile = apkDownloader.downloadApk(versionInfo.downloadUrl) { progress ->
                downloadProgress = progress
            }

            if (apkFile != null) {
                // 下载成功，安装APK
                apkDownloader.installApk(apkFile)
                // 安装后关闭对话框
                showUpdateDialog = false
                updateInfo = null
                downloadProgress = null
            } else {
                // 下载失败，重置进度
                downloadProgress = null
            }
        }
    }

    /**
     * 构建自定义参数
     * 您可以在这里添加更多自定义信息
     * 注意：根据国家个人信息保护相关法律法规，禁止传递唯一设备识别码等敏感信息
     */
    private fun buildCustomInfo(): Map<String, String> {
        return mapOf(
            // 应用相关信息
            "buildType" to if (BuildConfig.DEBUG) "Debug" else "Release",
            "isServiceRunning" to isServiceRunning.toString(),

            // 权限状态信息（帮助排查问题）
            "hasOverlayPermission" to permissionStatus.hasOverlay.toString(),
            "hasNotificationPermission" to permissionStatus.hasNotification.toString(),
            "hasStoragePermission" to permissionStatus.hasStorage.toString(),
            "hasScreenCapturePermission" to permissionStatus.hasScreenCapture.toString(),
            "hasForegroundServicePermission" to permissionStatus.hasForegroundServiceMediaProjection.toString(),

            // 设备信息（非敏感）
            "screenDensity" to resources.displayMetrics.density.toString(),
            "screenWidth" to resources.displayMetrics.widthPixels.toString(),
            "screenHeight" to resources.displayMetrics.heightPixels.toString(),

            // 您可以在这里添加更多自定义参数
            // 例如：用户设置、功能使用情况等
            // "customParam1" to "value1",
            // "customParam2" to "value2",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    permissionStatus: com.example.annotation.utils.PermissionStatus,
    isServiceRunning: Boolean,
    preferencesManager: PreferencesManager,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onRequestScreenCapturePermission: () -> Unit,
    onRequestForegroundServicePermission: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenComingSoon: () -> Unit = {}
) {
    val showUserEntry = remember { mutableStateOf(preferencesManager.getShowUserEntry()) }
    var themeMenuExpanded by remember { mutableStateOf(false) }

    // 监听设置变化
    LaunchedEffect(Unit) {
        // 定期检查设置是否变化
        kotlinx.coroutines.delay(500)
        showUserEntry.value = preferencesManager.getShowUserEntry()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(44.dp),
                title = {
                    Text(
                        text = "粉笔标注",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { themeMenuExpanded = true }) {
                            Icon(
                                imageVector = when (themeMode) {
                                    AppThemeMode.SYSTEM -> SystemModeIcon
                                    AppThemeMode.LIGHT -> LightModeIcon
                                    AppThemeMode.DARK -> DarkModeIcon
                                },
                                contentDescription = "外观模式",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = themeMenuExpanded,
                            onDismissRequest = { themeMenuExpanded = false }
                        ) {
                            ThemeModeMenuItem(
                                label = "跟随系统",
                                icon = SystemModeIcon,
                                selected = themeMode == AppThemeMode.SYSTEM,
                                onClick = {
                                    onThemeModeChange(AppThemeMode.SYSTEM)
                                    themeMenuExpanded = false
                                }
                            )
                            ThemeModeMenuItem(
                                label = "浅色模式",
                                icon = LightModeIcon,
                                selected = themeMode == AppThemeMode.LIGHT,
                                onClick = {
                                    onThemeModeChange(AppThemeMode.LIGHT)
                                    themeMenuExpanded = false
                                }
                            )
                            ThemeModeMenuItem(
                                label = "深色模式",
                                icon = DarkModeIcon,
                                selected = themeMode == AppThemeMode.DARK,
                                onClick = {
                                    onThemeModeChange(AppThemeMode.DARK)
                                    themeMenuExpanded = false
                                }
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "设置",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (showUserEntry.value) {
                        IconButton(onClick = onOpenComingSoon) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.cue),
                                contentDescription = "用户",
                                tint = MaterialTheme.colorScheme.onSurface,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Hero区域 - 渐变背景卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Create,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "全局屏幕标注工具",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "随时随页，自由标注",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 服务控制卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (permissionStatus.canRunAnnotationService())
                            MaterialTheme.colorScheme.tertiaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (permissionStatus.canRunAnnotationService())
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.outline
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Create,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "标注服务",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (permissionStatus.canRunAnnotationService())
                                            "开启后显示悬浮按钮"
                                        else
                                            "需要授予基础权限",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // 开关按钮
                            Switch(
                                checked = isServiceRunning,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        onStartService()
                                    } else {
                                        onStopService()
                                    }
                                },
                                enabled = permissionStatus.canRunAnnotationService(),
                                colors = iosSwitchColors()
                            )
                        }
                    }
                }

                // 权限状态卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = if (permissionStatus.canRunAnnotationService())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "权限检查",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        PermissionItemModern(
                            icon = Icons.Outlined.Info,
                            name = "屏幕捕获权限（可选）",
                            description = "仅在应用内截图时按需启用",
                            isGranted = permissionStatus.hasScreenCapture,
                            onRequest = onRequestScreenCapturePermission
                        )
                        SettingsInsetDivider()

                        PermissionItemModern(
                            icon = Icons.Outlined.Star,
                            name = "悬浮窗权限",
                            description = "允许应用在其他应用上层显示",
                            isGranted = permissionStatus.hasOverlay,
                            onRequest = onRequestOverlayPermission
                        )
                        SettingsInsetDivider()

                        PermissionItemModern(
                            icon = Icons.Outlined.Notifications,
                            name = "通知权限",
                            description = "保持服务在后台运行",
                            isGranted = permissionStatus.hasNotification,
                            onRequest = onRequestNotificationPermission
                        )
                        SettingsInsetDivider()

                        PermissionItemModern(
                            icon = Icons.Outlined.Build,
                            name = "存储权限",
                            description = "保存标注截图到相册",
                            isGranted = permissionStatus.hasStorage,
                            onRequest = onRequestStoragePermission
                        )

                        // Only show on Android 14+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            SettingsInsetDivider()
                            PermissionItemModern(
                                icon = Icons.Outlined.DateRange,
                                name = "前台服务权限",
                                description = "允许应用在前台运行媒体投影服务",
                                isGranted = permissionStatus.hasForegroundServiceMediaProjection,
                                onRequest = onRequestForegroundServicePermission
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 提示文本
                AnimatedVisibility(
                    visible = permissionStatus.canRunAnnotationService(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "启动后将显示悬浮按钮，点击即可开始标注",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        leadingIcon = { Icon(imageVector = icon, contentDescription = null) },
        trailingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@Composable
fun PermissionItemModern(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGranted)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isGranted) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "已授权",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                FilledTonalButton(
                    onClick = onRequest,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("授权")
                }
            }
        }
    }
}

@Composable
fun PermissionItem(
    name: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) {
                    Icons.Outlined.Check
                } else {
                    Icons.Outlined.Close
                },
                contentDescription = null,
                tint = if (isGranted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            Text(text = name)
        }

        if (!isGranted) {
            TextButton(onClick = onRequest) {
                Text("授权")
            }
        }
    }
}
