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
import com.example.annotation.model.StylusProfile
import com.example.annotation.ui.theme.iosSwitchColors
import com.example.annotation.utils.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylusSettingsScreen(
    preferencesManager: PreferencesManager,
    onNavigateBack: () -> Unit
) {
    var enabled by remember { mutableStateOf(preferencesManager.getStylusEnabled()) }
    var profile by remember { mutableStateOf(preferencesManager.getStylusProfile()) }
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
                StylusChoiceRow(
                    title = "设备档案",
                    description = profile.description,
                    selectedText = profile.displayName,
                    options = StylusProfile.entries,
                    optionText = { it.displayName },
                    onSelected = {
                        profile = it
                        preferencesManager.setStylusProfile(it)
                    }
                )
            }

            if (profile == StylusProfile.CUSTOM) {
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
                mappings = mappings,
                preferencesManager = preferencesManager,
                items = listOf(
                    "单击" to PreferencesManager.STYLUS_PRIMARY_SINGLE,
                    "双击" to PreferencesManager.STYLUS_PRIMARY_DOUBLE,
                    "长按" to PreferencesManager.STYLUS_PRIMARY_LONG
                )
            )

            InputSettingsSectionTitle("副键功能")
            StylusActionGroup(
                mappings = mappings,
                preferencesManager = preferencesManager,
                items = listOf(
                    "单击" to PreferencesManager.STYLUS_SECONDARY_SINGLE,
                    "双击" to PreferencesManager.STYLUS_SECONDARY_DOUBLE,
                    "长按" to PreferencesManager.STYLUS_SECONDARY_LONG
                )
            )
        }
    }
}

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
private fun <T> StylusChoiceRow(
    title: String,
    description: String,
    selectedText: String,
    options: List<T>,
    optionText: (T) -> String,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
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
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionText(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
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
    mappings: MutableMap<String, StylusButtonAction>,
    preferencesManager: PreferencesManager,
    items: List<Pair<String, String>>
) {
    GroupedSettingsCard {
        items.forEachIndexed { index, (label, key) ->
            StylusChoiceRow(
                title = label,
                description = "手写笔按键$label",
                selectedText = mappings.getValue(key).displayName,
                options = StylusButtonAction.entries,
                optionText = { it.displayName },
                onSelected = {
                    mappings[key] = it
                    preferencesManager.setStylusButtonAction(key, it)
                }
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
