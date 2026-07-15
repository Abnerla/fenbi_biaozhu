package com.example.annotation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 设置页面 - 入口列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToToolbarSettings: () -> Unit = {},
    onNavigateToHighlighterSettings: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToOtherSettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToDeveloper: () -> Unit = {},
    onNavigateToFeedback: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    // 处理返回手势
    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(44.dp),
                title = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "设置",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "返回",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding(), start = 16.dp, end = 16.dp, bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 应用设置分组
            SettingGroupTitle(title = "应用设置")

            // 工具栏设置入口
            SettingEntryCard(
                title = "工具栏设置",
                description = "调整工具栏的属性",
                icon = Icons.Outlined.Build,
                onClick = onNavigateToToolbarSettings
            )

            // 笔设置入口
            SettingEntryCard(
                title = "笔设置",
                description = "调整笔的细节",
                icon = Icons.Outlined.Edit,
                onClick = onNavigateToHighlighterSettings
            )

            // 系统权限管理入口
            SettingEntryCard(
                title = "系统权限",
                description = "管理应用所需权限",
                icon = Icons.Outlined.Lock,
                onClick = onNavigateToPermissions
            )

            // 其他设置入口
            SettingEntryCard(
                title = "其他",
                description = "其他杂项设置",
                icon = Icons.Outlined.Settings,
                onClick = onNavigateToOtherSettings
            )

            // 关于与帮助分组
            SettingGroupTitle(title = "关于与帮助")

            // 版本信息入口
            SettingEntryCard(
                title = "版本信息",
                description = "版本信息与更新",
                icon = Icons.Outlined.Info,
                onClick = onNavigateToAbout
            )

            // 开发人员信息入口
            SettingEntryCard(
                title = "开发人员",
                description = "查看开发团队信息",
                icon = Icons.Outlined.Person,
                onClick = onNavigateToDeveloper
            )

            // 反馈入口
            SettingEntryCard(
                title = "反馈",
                description = "提交问题或建议",
                icon = Icons.Outlined.Email,
                onClick = onNavigateToFeedback
            )

            // 帮助入口
            SettingEntryCard(
                title = "帮助",
                description = "使用指南与常见问题",
                icon = Icons.Outlined.Home,
                onClick = onNavigateToHelp
            )
        }
    }
}

/**
 * 设置分组标题
 */
@Composable
private fun SettingGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

/**
 * 设置入口卡片组件
 */
@Composable
private fun SettingEntryCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
