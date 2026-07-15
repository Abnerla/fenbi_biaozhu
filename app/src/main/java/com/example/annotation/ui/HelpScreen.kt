package com.example.annotation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 帮助页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
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
                            text = "帮助",
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
            // 快速入门指南
            Text(
                text = "快速入门",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            QuickStartCard(
                step = "1",
                title = "授予权限",
                description = "首次使用需要授予悬浮窗、通知和存储权限",
                icon = Icons.Outlined.Lock
            )

            QuickStartCard(
                step = "2",
                title = "启动服务",
                description = "在主页面开启标注服务，屏幕上会显示悬浮按钮",
                icon = Icons.Outlined.PlayArrow
            )

            QuickStartCard(
                step = "3",
                title = "开始标注",
                description = "点击悬浮按钮展开工具栏，选择画笔或荧光笔开始标注",
                icon = Icons.Outlined.Create
            )

            QuickStartCard(
                step = "4",
                title = "保存截图",
                description = "标注完成后点击截图按钮，图片将保存到相册",
                icon = Icons.Outlined.Star
            )

            // 常见问题
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "常见问题",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            FAQItem(
                question = "软件要收费吗？",
                answer = "软件永远不会考虑收费的问题，因为软件本就属于本地运行，不会对作者产生任何负担！"
            )
            FAQItem(
                question = "为什么悬浮按钮不显示？",
                answer = "请确保已授予悬浮窗权限并在主页面开启了标注服务。如果仍然不显示，请尝试重启应用。"
            )

            FAQItem(
                question = "如何调整画笔粗细和颜色？",
                answer = "点击悬浮按钮展开工具栏后，选择画笔工具，会显示二级菜单包含颜色和粗细选项。"
            )

            FAQItem(
                question = "标注的内容可以撤销吗？",
                answer = "可以，工具栏中有撤销按钮，可以撤销上一步操作。也可以使用橡皮擦工具擦除指定内容。"
            )

            FAQItem(
                question = "截图保存在哪里？",
                answer = "截图会自动保存到设备相册中，通常在 Pictures/粉笔标注 文件夹下。"
            )

            FAQItem(
                question = "如何完全关闭标注功能？",
                answer = "在主页面关闭标注服务开关即可。悬浮按钮会立即消失，应用会停止后台运行。"
            )

            FAQItem(
                question = "应用会消耗很多电量吗？",
                answer = "不会。应用在后台运行时几乎不消耗电量，只有在使用标注功能时才会有少量耗电。"
            )

            // 功能说明
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "工具说明",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            ToolDescriptionCard(
                icon = Icons.Outlined.Edit,
                title = "画笔",
                description = "自由绘制线条，支持多种颜色和粗细选择"
            )

            ToolDescriptionCard(
                icon = Icons.Outlined.Build,
                title = "荧光笔",
                description = "半透明标记工具，适合突出重点内容"
            )

            ToolDescriptionCard(
                icon = Icons.Outlined.Close,
                title = "橡皮擦",
                description = "擦除已绘制的内容，支持调节擦除范围"
            )

            ToolDescriptionCard(
                icon = Icons.Outlined.Delete,
                title = "清空",
                description = "一键清除画布上的所有标注内容"
            )

            ToolDescriptionCard(
                icon = Icons.Outlined.Star,
                title = "截图",
                description = "将当前屏幕和标注内容保存为图片"
            )
        }
    }
}

/**
 * 快速入门卡片
 */
@Composable
private fun QuickStartCard(
    step: String,
    title: String,
    description: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * FAQ展开项
 */
@Composable
private fun FAQItem(
    question: String,
    answer: String
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(if (expanded) 180f else 0f)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

/**
 * 工具说明卡片
 */
@Composable
private fun ToolDescriptionCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
