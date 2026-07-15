package com.example.annotation.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.annotation.service.GestureForwardingAccessibilityService
import com.example.annotation.ui.theme.iosSwitchColors
import com.example.annotation.utils.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureSettingsScreen(
    preferencesManager: PreferencesManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var twoFingerUndo by remember { mutableStateOf(preferencesManager.getTwoFingerTapUndoEnabled()) }
    var threeFingerRedo by remember { mutableStateOf(preferencesManager.getThreeFingerTapRedoEnabled()) }
    val initialBridgeReady = GestureForwardingAccessibilityService.isReady
    var pageMove by remember {
        mutableStateOf(preferencesManager.getTwoFingerPageMoveEnabled() && initialBridgeReady)
    }
    var bridgeReady by remember { mutableStateOf(initialBridgeReady) }
    var enablePageMoveAfterAuthorization by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                bridgeReady = GestureForwardingAccessibilityService.isReady
                if (bridgeReady && enablePageMoveAfterAuthorization) {
                    pageMove = true
                    preferencesManager.setTwoFingerPageMoveEnabled(true)
                    enablePageMoveAfterAuthorization = false
                } else if (!bridgeReady) {
                    pageMove = false
                    preferencesManager.setTwoFingerPageMoveEnabled(false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(44.dp),
                title = { Text("手势设置", fontWeight = FontWeight.SemiBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                windowInsets = WindowInsets(0)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                )
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InputSettingsSectionTitle("批注手势")
            GroupedSettingsCard {
                GestureSwitchRow(
                    icon = Icons.Outlined.Refresh,
                    title = "双指单击撤销",
                    description = "两根手指同时轻触画布时撤销一步",
                    checked = twoFingerUndo,
                    onCheckedChange = {
                        twoFingerUndo = it
                        preferencesManager.setTwoFingerTapUndoEnabled(it)
                    }
                )
                SettingsInsetDivider()
                GestureSwitchRow(
                    icon = Icons.Outlined.Check,
                    title = "三指单击重做",
                    description = "三根手指同时轻触画布时恢复一步",
                    checked = threeFingerRedo,
                    onCheckedChange = {
                        threeFingerRedo = it
                        preferencesManager.setThreeFingerTapRedoEnabled(it)
                    }
                )
                SettingsInsetDivider()
                GestureSwitchRow(
                    icon = Icons.Outlined.KeyboardArrowDown,
                    title = "双指移动底层页面",
                    description = if (bridgeReady) "无障碍手势桥接已连接" else "需要启用粉笔标注手势服务",
                    checked = pageMove,
                    onCheckedChange = { enabled ->
                        if (enabled && !bridgeReady) {
                            enablePageMoveAfterAuthorization = true
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        } else {
                            enablePageMoveAfterAuthorization = false
                            pageMove = enabled
                            preferencesManager.setTwoFingerPageMoveEnabled(enabled)
                        }
                    }
                )
            }

            if (!bridgeReady) {
                TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                    Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("打开无障碍设置")
                }
            }
        }
    }
}

@Composable
private fun GestureSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InputSettingsRowIcon(icon)
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = iosSwitchColors())
    }
}
