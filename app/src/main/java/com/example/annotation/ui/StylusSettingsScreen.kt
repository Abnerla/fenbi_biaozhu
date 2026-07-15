package com.example.annotation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.annotation.model.*
import com.example.annotation.service.GestureForwardingAccessibilityService
import com.example.annotation.service.OverlayService
import com.example.annotation.ui.theme.iosSwitchColors
import com.example.annotation.utils.PreferencesManager
import com.example.annotation.utils.StylusInputEventKind
import com.example.annotation.utils.StylusInputMonitor
import com.example.annotation.utils.StylusInputReport
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylusSettingsScreen(
    preferencesManager: PreferencesManager,
    onNavigateBack: () -> Unit
) {
    var enabled by remember { mutableStateOf(preferencesManager.getStylusEnabled()) }
    var mode by remember { mutableStateOf(preferencesManager.getStylusMode()) }
    var manualProfile by remember { mutableStateOf(preferencesManager.getManualStylusProfile()) }
    var manualPresetId by remember { mutableStateOf(preferencesManager.getManualStylusPresetId()) }
    var pickerStage by remember { mutableStateOf<StylusPickerStage?>(null) }
    var nextPickerStage by remember { mutableStateOf<StylusPickerStage?>(null) }
    var actionPicker by remember { mutableStateOf<StylusActionPicker?>(null) }
    val detectedProfile = remember { StylusProfile.detectForDevice() }
    val initiallyConnectedStylus = remember { StylusDeviceIdentity.connectedStyluses().firstOrNull() }
    val latestReport by StylusInputMonitor.latestReport.collectAsState()
    val activeIdentity = latestReport?.device ?: initiallyConnectedStylus
    val detectedPreset = activeIdentity?.let(StylusVendorPresetCatalog::detect)
    var learningTarget by remember { mutableStateOf<StylusButton?>(null) }
    var learningStartSequence by remember { mutableLongStateOf(0L) }
    var learningSession by remember { mutableIntStateOf(0) }
    var learnedRevision by remember { mutableIntStateOf(0) }
    var primaryMask by remember { mutableStateOf(preferencesManager.getStylusCustomPrimaryMask().toString()) }
    var secondaryMask by remember { mutableStateOf(preferencesManager.getStylusCustomSecondaryMask().toString()) }
    val mappings = remember {
        val saved = preferencesManager.getStylusButtonMappings()
        mutableStateMapOf(
            PreferencesManager.STYLUS_PRIMARY_SINGLE to saved.primarySingle,
            PreferencesManager.STYLUS_PRIMARY_DOUBLE to saved.primaryDouble,
            PreferencesManager.STYLUS_PRIMARY_LONG to saved.primaryLong,
            PreferencesManager.STYLUS_SECONDARY_SINGLE to saved.secondarySingle,
            PreferencesManager.STYLUS_SECONDARY_DOUBLE to saved.secondaryDouble,
            PreferencesManager.STYLUS_SECONDARY_LONG to saved.secondaryLong
        )
    }

    val learnedBindings = remember(activeIdentity?.stableKey, learnedRevision) {
        activeIdentity?.let { preferencesManager.getStylusLearnedBindings(it.stableKey) }
            ?: StylusLearnedBindings()
    }

    LaunchedEffect(learningSession) {
        if (learningTarget == null) return@LaunchedEffect
        StylusInputMonitor.setLearningActive(true)
        delay(10_000L)
        learningTarget = null
        StylusInputMonitor.setLearningActive(false)
    }

    DisposableEffect(Unit) {
        onDispose { StylusInputMonitor.setLearningActive(false) }
    }

    LaunchedEffect(latestReport?.sequence, learningTarget) {
        val target = learningTarget ?: return@LaunchedEffect
        val report = latestReport ?: return@LaunchedEffect
        if (report.sequence <= learningStartSequence) return@LaunchedEffect

        when (report.kind) {
            StylusInputEventKind.MOTION -> {
                if (report.candidateMotionMask == 0) return@LaunchedEffect
                preferencesManager.setStylusLearnedMotionMask(
                    report.device.stableKey,
                    target,
                    report.candidateMotionMask
                )
            }
            StylusInputEventKind.KEY -> {
                if (report.keyCode == 0) return@LaunchedEffect
                preferencesManager.setStylusLearnedKeyCode(
                    report.device.stableKey,
                    target,
                    report.keyCode
                )
            }
        }
        learningTarget = null
        StylusInputMonitor.setLearningActive(false)
        learnedRevision++
    }

    BackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(44.dp),
                title = { Text("手写笔", fontWeight = FontWeight.SemiBold, fontSize = 20.sp) },
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
            InputSettingsSectionTitle("兼容模式")
            GroupedSettingsCard {
                StylusSwitchRow(
                    title = "启用手写笔适配",
                    description = "识别压力、工具类型和手写笔按键",
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        preferencesManager.setStylusEnabled(it)
                    }
                )
                SettingsInsetDivider()
                StylusProfileRow(
                    mode = mode,
                    detail = when (mode) {
                        StylusMode.AUTO -> detectedPreset?.displayName
                            ?: "${detectedProfile.displayName}，笔型号未知"
                        StylusMode.MANUAL -> StylusVendorPresetCatalog.byId(manualPresetId)?.displayName
                            ?: "${manualProfile.displayName}，型号未知"
                        StylusMode.CUSTOM -> "自定义按键掩码"
                    },
                    onClick = { pickerStage = StylusPickerStage.MODE }
                )
                if (mode != StylusMode.CUSTOM) {
                    SettingsInsetDivider()
                    StylusModelInfoRow(
                        preset = when (mode) {
                            StylusMode.AUTO -> detectedPreset
                            StylusMode.MANUAL -> StylusVendorPresetCatalog.byId(manualPresetId)
                            StylusMode.CUSTOM -> null
                        },
                        onClick = {
                            if (mode == StylusMode.MANUAL) pickerStage = StylusPickerStage.MODEL
                        }
                    )
                }
            }

            if (mode == StylusMode.CUSTOM) {
                InputSettingsSectionTitle("自定义按键掩码")
                GroupedSettingsCard {
                    StylusMaskField(
                        title = "主键 buttonState",
                        value = primaryMask,
                        onValueChange = { value ->
                            primaryMask = value.filter(Char::isDigit)
                            primaryMask.toIntOrNull()?.let(preferencesManager::setStylusCustomPrimaryMask)
                        }
                    )
                    SettingsInsetDivider()
                    StylusMaskField(
                        title = "副键 buttonState",
                        value = secondaryMask,
                        onValueChange = { value ->
                            secondaryMask = value.filter(Char::isDigit)
                            secondaryMask.toIntOrNull()?.let(preferencesManager::setStylusCustomSecondaryMask)
                        }
                    )
                }
            }

            InputSettingsSectionTitle("按键测试与学习")
            GroupedSettingsCard {
                StylusLearningStatusRow(
                    report = latestReport,
                    learningTarget = learningTarget,
                    learnedBindings = learnedBindings,
                    accessibilityReady = GestureForwardingAccessibilityService.isReady
                )
                SettingsInsetDivider()
                StylusCommandRow(
                    title = "学习主键",
                    description = "开始后在 10 秒内按下手写笔主键",
                    icon = Icons.Outlined.Edit,
                    onClick = {
                        learningStartSequence = latestReport?.sequence ?: 0L
                        learningTarget = StylusButton.PRIMARY
                        learningSession++
                    }
                )
                SettingsInsetDivider()
                StylusCommandRow(
                    title = "学习副键",
                    description = "开始后在 10 秒内按下手写笔副键",
                    icon = Icons.Outlined.Edit,
                    onClick = {
                        learningStartSequence = latestReport?.sequence ?: 0L
                        learningTarget = StylusButton.SECONDARY
                        learningSession++
                    }
                )
                SettingsInsetDivider()
                StylusCommandRow(
                    title = "清除当前设备学习结果",
                    description = activeIdentity?.displayName ?: "尚未检测到手写笔设备",
                    icon = Icons.Outlined.Build,
                    enabled = activeIdentity != null,
                    onClick = {
                        activeIdentity?.let {
                            preferencesManager.clearStylusLearnedBindings(it.stableKey)
                            learnedRevision++
                        }
                    }
                )
            }

            InputSettingsSectionTitle("主键功能")
            StylusActionGroup(
                button = StylusButton.PRIMARY,
                buttonName = "主键",
                mappings = mappings,
                items = listOf(
                    "单击" to PreferencesManager.STYLUS_PRIMARY_SINGLE,
                    "双击" to PreferencesManager.STYLUS_PRIMARY_DOUBLE,
                    "长按" to PreferencesManager.STYLUS_PRIMARY_LONG
                ),
                onChooseAction = { pressName, key ->
                    actionPicker = StylusActionPicker("主键", pressName, key)
                }
            )

            InputSettingsSectionTitle("副键功能")
            StylusActionGroup(
                button = StylusButton.SECONDARY,
                buttonName = "副键",
                mappings = mappings,
                items = listOf(
                    "单击" to PreferencesManager.STYLUS_SECONDARY_SINGLE,
                    "双击" to PreferencesManager.STYLUS_SECONDARY_DOUBLE,
                    "长按" to PreferencesManager.STYLUS_SECONDARY_LONG
                ),
                onChooseAction = { pressName, key ->
                    actionPicker = StylusActionPicker("副键", pressName, key)
                }
            )

            InputSettingsSectionTitle("默认行为")
            GroupedSettingsCard {
                StylusCommandRow(
                    title = "恢复当前设备默认",
                    description = "主键和副键全部改为跟随厂商默认",
                    icon = Icons.Outlined.Build,
                    onClick = {
                        preferencesManager.resetStylusButtonMappings()
                        mappings.keys.forEach { key ->
                            mappings[key] = StylusButtonAction.VENDOR_DEFAULT
                        }
                    }
                )
            }
        }
    }

    if (pickerStage != null) {
        DisposableEffect(Unit) {
            OverlayService.setFloatingButtonSuppressed(true)
            onDispose { OverlayService.setFloatingButtonSuppressed(false) }
        }
    }

    when (pickerStage) {
        StylusPickerStage.MODE -> IosSelectionDialog(
            title = "选择设备档案",
            options = StylusMode.entries,
            selectedOption = mode,
            optionText = { it.displayName },
            onSelected = { selectedMode ->
                mode = selectedMode
                preferencesManager.setStylusMode(selectedMode)
                nextPickerStage = if (selectedMode == StylusMode.MANUAL) {
                    StylusPickerStage.BRAND
                } else {
                    null
                }
            },
            onDismiss = {
                pickerStage = nextPickerStage
                nextPickerStage = null
            },
            suppressFloatingButton = false
        )
        StylusPickerStage.BRAND -> IosSelectionDialog(
            title = "选择手写笔品牌",
            options = StylusProfile.entries,
            selectedOption = manualProfile,
            optionText = { it.displayName },
            onSelected = {
                manualProfile = it
                manualPresetId = null
                preferencesManager.setManualStylusProfile(it)
                nextPickerStage = StylusPickerStage.MODEL
            },
            onDismiss = {
                pickerStage = nextPickerStage
                nextPickerStage = null
            },
            suppressFloatingButton = false
        )
        StylusPickerStage.MODEL -> {
            val options = listOf(StylusModelOption(null, "${manualProfile.displayName}（型号未知）")) +
                StylusVendorPresetCatalog.forProfile(manualProfile).map {
                    StylusModelOption(it.id, it.displayName)
                }
            val selected = options.firstOrNull { it.id == manualPresetId } ?: options.first()
            IosSelectionDialog(
                title = "选择手写笔型号",
                options = options,
                selectedOption = selected,
                optionText = { it.displayName },
                onSelected = {
                    manualPresetId = it.id
                    preferencesManager.setManualStylusPresetId(it.id)
                },
                onDismiss = { pickerStage = null },
                suppressFloatingButton = false
            )
        }
        null -> Unit
    }

    actionPicker?.let { picker ->
        IosSelectionDialog(
            title = "${picker.buttonName}${picker.pressName}功能",
            options = StylusButtonAction.entries,
            selectedOption = mappings.getValue(picker.preferenceKey),
            optionText = { it.displayName },
            onSelected = {
                mappings[picker.preferenceKey] = it
                preferencesManager.setStylusButtonAction(picker.preferenceKey, it)
            },
            onDismiss = { actionPicker = null }
        )
    }
}

private enum class StylusPickerStage { MODE, BRAND, MODEL }

private data class StylusModelOption(val id: String?, val displayName: String)

private data class StylusActionPicker(
    val buttonName: String,
    val pressName: String,
    val preferenceKey: String
)

@Composable
fun InputSettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun StylusSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InputSettingsRowIcon(Icons.Outlined.Edit)
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = iosSwitchColors())
    }
}

@Composable
private fun StylusProfileRow(
    mode: StylusMode,
    detail: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InputSettingsRowIcon(Icons.Outlined.Build)
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text("设备档案", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    mode.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StylusModelInfoRow(
    preset: StylusVendorPreset?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InputSettingsRowIcon(Icons.Outlined.Info)
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                preset?.displayName ?: "未识别精确笔型号",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                preset?.verifiedBehavior ?: "不会根据手机品牌自动套用手写笔动作",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StylusLearningStatusRow(
    report: StylusInputReport?,
    learningTarget: StylusButton?,
    learnedBindings: StylusLearnedBindings,
    accessibilityReady: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InputSettingsRowIcon(Icons.Outlined.Info)
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                when (learningTarget) {
                    StylusButton.PRIMARY -> "等待主键输入"
                    StylusButton.SECONDARY -> "等待副键输入"
                    null -> report?.device?.displayName ?: "尚未检测到手写笔输入"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                report?.summary ?: "按键桥接${if (accessibilityReady) "已连接" else "未连接"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val learned = buildList {
                if (learnedBindings.primaryMask != 0) add("主键 mask=${learnedBindings.primaryMask}")
                if (learnedBindings.primaryKeyCode != 0) add("主键 key=${learnedBindings.primaryKeyCode}")
                if (learnedBindings.secondaryMask != 0) add("副键 mask=${learnedBindings.secondaryMask}")
                if (learnedBindings.secondaryKeyCode != 0) add("副键 key=${learnedBindings.secondaryKeyCode}")
            }
            if (learned.isNotEmpty()) {
                Text(
                    learned.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StylusCommandRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InputSettingsRowIcon(icon)
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
private fun StylusActionRow(
    title: String,
    description: String,
    selectedText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InputSettingsRowIcon(Icons.Outlined.Build)
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(selectedText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null)
    }
}

@Composable
private fun StylusMaskField(title: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(120.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun StylusActionGroup(
    button: StylusButton,
    buttonName: String,
    mappings: MutableMap<String, StylusButtonAction>,
    items: List<Pair<String, String>>,
    onChooseAction: (pressName: String, preferenceKey: String) -> Unit
) {
    val isMixed = when (button) {
        StylusButton.PRIMARY -> listOf(
            PreferencesManager.STYLUS_PRIMARY_SINGLE,
            PreferencesManager.STYLUS_PRIMARY_DOUBLE,
            PreferencesManager.STYLUS_PRIMARY_LONG
        )
        StylusButton.SECONDARY -> listOf(
            PreferencesManager.STYLUS_SECONDARY_SINGLE,
            PreferencesManager.STYLUS_SECONDARY_DOUBLE,
            PreferencesManager.STYLUS_SECONDARY_LONG
        )
    }.map(mappings::getValue).let { actions ->
        actions.any { it != StylusButtonAction.VENDOR_DEFAULT } &&
            actions.any { it == StylusButtonAction.VENDOR_DEFAULT }
    }
    GroupedSettingsCard {
        items.forEachIndexed { index, (label, key) ->
            val selected = mappings.getValue(key)
            StylusActionRow(
                title = label,
                description = if (isMixed && selected == StylusButtonAction.VENDOR_DEFAULT) {
                    "按键已接管，未确认的默认动作无法保留"
                } else {
                    "手写笔${buttonName}${label}"
                },
                selectedText = selected.displayName,
                onClick = { onChooseAction(label, key) }
            )
            if (index != items.lastIndex) SettingsInsetDivider()
        }
    }
}

@Composable
fun InputSettingsRowIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}
