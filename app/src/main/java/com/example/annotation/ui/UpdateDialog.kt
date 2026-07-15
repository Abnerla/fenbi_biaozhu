package com.example.annotation.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.annotation.model.VersionInfo
import com.example.annotation.ui.theme.IOSBlue
import com.example.annotation.ui.theme.IOSBlueSurface
import com.example.annotation.ui.theme.IOSRed
import com.example.annotation.ui.theme.IOSRedSurface
import kotlinx.coroutines.delay

/**
 * 现代化更新对话框
 * @param versionInfo 版本信息
 * @param isForceUpdate 是否强制更新
 * @param downloadProgress 下载进度 (0-100)，null表示未开始下载
 * @param onUpdate 点击更新按钮的回调
 * @param onDismiss 点击取消按钮的回调（强制更新时不可用）
 */
@Composable
fun UpdateDialog(
    versionInfo: VersionInfo,
    isForceUpdate: Boolean,
    downloadProgress: Int? = null,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    // 入场动画状态
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }

    // 缩放动画
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dialog_scale"
    )

    // 透明度动画
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(250),
        label = "dialog_alpha"
    )

    Dialog(
        onDismissRequest = {
            if (!isForceUpdate) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isForceUpdate,
            dismissOnClickOutside = !isForceUpdate
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .alpha(alpha),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                tonalElevation = 0.dp,
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 顶部渐变区域（固定）
                    HeaderSection(isForceUpdate = isForceUpdate)

                    // 固定内容区域（不可滚动）
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // 标题
                        TitleSection(
                            isForceUpdate = isForceUpdate
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 更新内容
                        UpdateContentCard(
                            updateDesc = versionInfo.updateDesc,
                            version = versionInfo.latestVersionName
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 下载进度
                        AnimatedVisibility(
                            visible = downloadProgress != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            if (downloadProgress != null) {
                                ProgressSection(progress = downloadProgress)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // 强制更新警告
                        if (isForceUpdate) {
                            ForceUpdateWarning()
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // 底部按钮区域（固定）
                    BottomButtonSection(
                        isForceUpdate = isForceUpdate,
                        downloadProgress = downloadProgress,
                        onUpdate = onUpdate,
                        onDismiss = onDismiss
                    )
                }
            }

        }
    }
}

/**
 * 顶部动态图标区域
 */
@Composable
private fun HeaderSection(isForceUpdate: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(
                color = if (isForceUpdate) IOSRedSurface else IOSBlueSurface,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedIcon(isForceUpdate = isForceUpdate)
    }
}

/**
 * 动态图标组件 - 增强版
 */
@Composable
private fun AnimatedIcon(isForceUpdate: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_animation")

    // 主旋转动画
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // 反向旋转动画
    val reverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverse_rotation"
    )

    // 脉动动画
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // 光圈透明度动画
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha1"
    )

    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha2"
    )

    // 图标旋转动画
    val iconRotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_rotation"
    )

    // 外圈缩放动画
    val outerScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outer_scale"
    )

    Box(
        contentAlignment = Alignment.Center
    ) {
        // 最外层光圈（扩散效果）
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(outerScale)
                .alpha(alpha1)
                .background(
                    color = Color.White,
                    shape = CircleShape
                )
        )

        // 第二层光圈（反向旋转）
        Box(
            modifier = Modifier
                .size(72.dp)
                .rotate(reverseRotation)
                .alpha(alpha2)
                .background(
                    color = Color.White,
                    shape = CircleShape
                )
        )

        // 第三层光圈（正向旋转）
        Box(
            modifier = Modifier
                .size(64.dp)
                .rotate(rotation)
                .alpha(0.2f)
                .background(
                    color = Color.White,
                    shape = CircleShape
                )
        )

        // 中心图标背景（脉动）
        Surface(
            modifier = Modifier
                .size(52.dp)
                .scale(scale),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isForceUpdate) Icons.Outlined.Warning else Icons.Outlined.Star,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(iconRotation),
                    tint = if (isForceUpdate) IOSRed else IOSBlue
                )
            }
        }

        // 装饰粒子效果
        ParticleEffect(isForceUpdate = isForceUpdate)
    }
}

/**
 * 粒子效果组件 - 增强版
 */
@Composable
private fun ParticleEffect(isForceUpdate: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    // 内圈粒子（快速）
    val particle1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle1"
    )

    val particle2Offset by infiniteTransition.animateFloat(
        initialValue = 90f,
        targetValue = 450f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle2"
    )

    val particle3Offset by infiniteTransition.animateFloat(
        initialValue = 180f,
        targetValue = 540f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle3"
    )

    val particle4Offset by infiniteTransition.animateFloat(
        initialValue = 270f,
        targetValue = 630f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle4"
    )

    // 外圈粒子（慢速反向）
    val particle5Offset by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle5"
    )

    val particle6Offset by infiniteTransition.animateFloat(
        initialValue = 300f,
        targetValue = -60f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle6"
    )

    val particle7Offset by infiniteTransition.animateFloat(
        initialValue = 240f,
        targetValue = -120f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle7"
    )

    val particle8Offset by infiniteTransition.animateFloat(
        initialValue = 180f,
        targetValue = -180f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle8"
    )

    val particle9Offset by infiniteTransition.animateFloat(
        initialValue = 120f,
        targetValue = -240f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle9"
    )

    val particle10Offset by infiniteTransition.animateFloat(
        initialValue = 60f,
        targetValue = -300f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle10"
    )

    // 透明度动画1
    val particleAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particle_alpha1"
    )

    // 透明度动画2（反相）
    val particleAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particle_alpha2"
    )

    // 粒子大小动画
    val particleSize by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particle_size"
    )

    Box(contentAlignment = Alignment.Center) {
        // 内圈粒子（半径35dp，4个粒子）
        ParticleDot(
            angle = particle1Offset,
            radius = 35.dp,
            color = Color.White,
            alpha = particleAlpha1,
            size = particleSize.dp
        )

        ParticleDot(
            angle = particle2Offset,
            radius = 35.dp,
            color = Color.White,
            alpha = particleAlpha2,
            size = particleSize.dp
        )

        ParticleDot(
            angle = particle3Offset,
            radius = 35.dp,
            color = Color.White,
            alpha = particleAlpha1 * 0.8f,
            size = particleSize.dp
        )

        ParticleDot(
            angle = particle4Offset,
            radius = 35.dp,
            color = Color.White,
            alpha = particleAlpha2 * 0.8f,
            size = particleSize.dp
        )

        // 外圈粒子（半径48dp，6个粒子）
        ParticleDot(
            angle = particle5Offset,
            radius = 48.dp,
            color = Color.White,
            alpha = particleAlpha1 * 0.6f,
            size = (particleSize * 0.8f).dp
        )

        ParticleDot(
            angle = particle6Offset,
            radius = 48.dp,
            color = Color.White,
            alpha = particleAlpha2 * 0.6f,
            size = (particleSize * 0.8f).dp
        )

        ParticleDot(
            angle = particle7Offset,
            radius = 48.dp,
            color = Color.White,
            alpha = particleAlpha1 * 0.5f,
            size = (particleSize * 0.8f).dp
        )

        ParticleDot(
            angle = particle8Offset,
            radius = 48.dp,
            color = Color.White,
            alpha = particleAlpha2 * 0.5f,
            size = (particleSize * 0.8f).dp
        )

        ParticleDot(
            angle = particle9Offset,
            radius = 48.dp,
            color = Color.White,
            alpha = particleAlpha1 * 0.7f,
            size = (particleSize * 0.8f).dp
        )

        ParticleDot(
            angle = particle10Offset,
            radius = 48.dp,
            color = Color.White,
            alpha = particleAlpha2 * 0.7f,
            size = (particleSize * 0.8f).dp
        )

        // 随机漂浮粒子
        FloatingParticles(isForceUpdate = isForceUpdate)
    }
}

/**
 * 随机漂浮粒子效果
 */
@Composable
private fun FloatingParticles(isForceUpdate: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating_particles")

    // 漂浮粒子动画
    val float1Y by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float1_y"
    )

    val float2Y by infiniteTransition.animateFloat(
        initialValue = 15f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float2_y"
    )

    val float3Y by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float3_y"
    )

    val floatAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_alpha"
    )

    // 漂浮粒子
    Box(
        modifier = Modifier
            .offset(x = (-25).dp, y = float1Y.dp)
            .size(4.dp)
            .alpha(floatAlpha)
            .background(color = Color.White, shape = CircleShape)
    )

    Box(
        modifier = Modifier
            .offset(x = 25.dp, y = float2Y.dp)
            .size(5.dp)
            .alpha(floatAlpha * 0.8f)
            .background(color = Color.White, shape = CircleShape)
    )

    Box(
        modifier = Modifier
            .offset(x = 0.dp, y = (30 + float3Y).dp)
            .size(3.dp)
            .alpha(floatAlpha * 0.6f)
            .background(color = Color.White, shape = CircleShape)
    )

    Box(
        modifier = Modifier
            .offset(x = 0.dp, y = (-30 + float1Y).dp)
            .size(4.dp)
            .alpha(floatAlpha * 0.7f)
            .background(color = Color.White, shape = CircleShape)
    )
}

/**
 * 单个粒子点
 */
@Composable
private fun ParticleDot(
    angle: Float,
    radius: androidx.compose.ui.unit.Dp,
    color: Color,
    alpha: Float,
    size: androidx.compose.ui.unit.Dp = 6.dp
) {
    val radiusPx = with(androidx.compose.ui.platform.LocalDensity.current) { radius.toPx() }
    val radians = Math.toRadians(angle.toDouble())
    val offsetX = (radiusPx * kotlin.math.cos(radians)).toFloat()
    val offsetY = (radiusPx * kotlin.math.sin(radians)).toFloat()

    Box(
        modifier = Modifier
            .offset(x = offsetX.dp, y = offsetY.dp)
            .size(size)
            .alpha(alpha)
            .background(color = color, shape = CircleShape)
    )
}

/**
 * 标题区域
 */
@Composable
private fun TitleSection(isForceUpdate: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isForceUpdate) "强制更新" else "发现新版本",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp
        )
    }
}

/**
 * 更新内容卡片
 */
@Composable
private fun UpdateContentCard(updateDesc: String, version: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "更新内容",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "v$version",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = updateDesc,
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * 下载进度区域
 */
@Composable
private fun ProgressSection(progress: Int) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 进度信息
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 旋转的下载图标
                val infiniteTransition = rememberInfiniteTransition(label = "download")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "download_rotation"
                )

                Icon(
                    imageVector = Icons.Outlined.Build,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotation),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "正在下载",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "$progress%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 渐变进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress / 100f)
                    .fillMaxHeight()
                    .background(
                        color = IOSBlue,
                        shape = RoundedCornerShape(5.dp)
                    )
            )
        }
    }
}

/**
 * 强制更新警告
 */
@Composable
private fun ForceUpdateWarning() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "当前版本过低，必须更新后才能继续使用",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 底部按钮区域 - 现代化设计
 */
@Composable
private fun BottomButtonSection(
    isForceUpdate: Boolean,
    downloadProgress: Int?,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 分隔线
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        // 按钮区域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 稍后再说按钮（文本样式）
            if (!isForceUpdate) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = downloadProgress == null,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "稍后再说",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // 立即更新按钮
            Button(
                onClick = onUpdate,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = downloadProgress == null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp,
                    disabledElevation = 0.dp
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (downloadProgress != null) "下载中..." else "立即更新",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
