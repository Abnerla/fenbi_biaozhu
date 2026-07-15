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
import com.example.annotation.model.StylusButtonAction
import com.example.annotation.model.StylusMode
import com.example.annotation.model.StylusProfile
import com.example.annotation.service.OverlayService
import com.example.annotation.ui.theme.iosSwitchColors
import com.example.annotation.utils.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylusSettingsScreen(
    preferencesManager: PreferencesManager,
    onNavigateBack: () -> Unit
) {
    var enabled by remember { mutableStateOf(preferencesManager.getStylusEnabled()) }
    var mode by remember { mutableStateOf(preferencesManager.getStylusMode()) }
    var manualProfile by remember { mutableStateOf(preferencesManager.getManualStylusProfile()) }
    var pickerStage by remember { mutableStateOf<StylusPickerStage?>(null) }
    var nextPickerStage by remember { mutableStateOf<StylusPickerStage?>(null) }
    var actionPicker by remember { mutableStateOf<StylusActionPicker?>(null) }
    val detectedProfile = remember { StylusProfile.detectForDevice() }
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
                        StylusMode.AUTO -> detectedProfile.displayName
                        StylusMode.MANUAL -> manualProfile.displayName
                        StylusMode.CUSTOM -> "自定义按键掩码"
                    },
                    onClick = { pickerStage = StylusPickerStage.MODE }
                )
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

            InputSettingsSectionTitle("主键功能")
            StylusActionGroup(
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
                preferencesManager.setManualStylusProfile(it)
            },
            onDismiss = { pickerStage = null },
            suppressFloatingButton = false
        )
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

private enum class StylusPickerStage { MODE, BRAND }

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
    buttonName: String,
    mappings: MutableMap<String, StylusButtonAction>,
    items: List<Pair<String, String>>,
    onChooseAction: (pressName: String, preferenceKey: String) -> Unit
) {
    GroupedSettingsCard {
        items.forEachIndexed { index, (label, key) ->
            StylusActionRow(
                title = label,
                description = "手写笔${buttonName}${label}",
                selectedText = mappings.getValue(key).displayName,
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
