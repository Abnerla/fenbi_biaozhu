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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 开发人员信息页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(
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
                            text = "开发人员",
                            fontWeight = FontWeight.Bold,
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
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                .padding(top = paddingValues.calculateTopPadding())
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 开发团队介绍
            DeveloperHeader()

            // 开发者列表
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "开发团队",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                DeveloperCard(
                    name = "主程序员：Abner",
                    role = "核心开发 & 架构设计",
                    icon = Icons.Default.AccountCircle,
                    description = "负责应用整体架构设计与核心功能开发"
                )

                DeveloperCard(
                    name = "UI/UX设计师：Abner",
                    role = "界面设计 & 用户体验",
                    icon = Icons.Default.Star,
                    description = "打造简洁美观的用户界面和流畅的交互体验"
                )

                DeveloperCard(
                    name = "测试工程师：Abner",
                    role = "质量保证 & 性能优化",
                    icon = Icons.Default.Build,
                    description = "确保应用稳定性和高性能运行"
                )

                // 联系方式
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "联系我们",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ContactCard(
                    icon = Icons.Default.Email,
                    title = "电子邮箱",
                    content = "1444170707@qq.com",
                    onClick = { /* TODO: 打开邮件客户端 */ }
                )

                ContactCard(
                    icon = Icons.Default.Face,
                    title = "GitHub",
                    content = "https://github.com/Abnerla",
                    onClick = { /* TODO: 打开GitHub */ }
                )

                // 致谢
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "特别感谢",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "感谢所有使用和支持粉笔标注的用户，你们的反馈和建议是我们持续改进的动力！",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 开发团队头部
 */
@Composable
private fun DeveloperHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6366F1),
                            Color(0xFF8B5CF6)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Text(
            text = "粉笔标注开发团队",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "致力于打造最好用的屏幕标注工具",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 开发者卡片
 */
@Composable
private fun DeveloperCard(
    name: String,
    role: String,
    icon: ImageVector,
    description: String
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
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = role,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * 联系方式卡片
 */
@Composable
private fun ContactCard(
    icon: ImageVector,
    title: String,
    content: String,
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
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
