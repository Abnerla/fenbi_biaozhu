package com.example.annotation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.annotation.service.OverlayService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IosDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(0.86f).widthIn(max = 360.dp),
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    suppressFloatingButton: Boolean = true,
    content: @Composable (requestDismiss: () -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    var contentVisible by remember { mutableStateOf(false) }
    var dismissing by remember { mutableStateOf(false) }
    val dimAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 0.22f else 0f,
        animationSpec = tween(IOS_DIALOG_ANIMATION_MILLIS.toInt()),
        label = "iosDialogDim"
    )

    fun dismissSmoothly() {
        if (dismissing) return
        dismissing = true
        contentVisible = false
        scope.launch {
            delay(IOS_DIALOG_ANIMATION_MILLIS)
            latestOnDismiss()
        }
    }

    LaunchedEffect(Unit) { contentVisible = true }

    if (suppressFloatingButton) {
        DisposableEffect(Unit) {
            OverlayService.setFloatingButtonSuppressed(true)
            onDispose { OverlayService.setFloatingButtonSuppressed(false) }
        }
    }

    Dialog(
        onDismissRequest = {
            if (dismissOnBackPress || dismissOnClickOutside) dismissSmoothly()
        },
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (dismissOnClickOutside) dismissSmoothly()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.94f),
                exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.96f)
            ) {
                Surface(
                    modifier = modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    tonalElevation = 1.dp,
                    shadowElevation = 4.dp
                ) {
                    content(::dismissSmoothly)
                }
            }
        }
    }
}

@Composable
fun <T> IosSelectionDialog(
    title: String,
    options: List<T>,
    selectedOption: T,
    optionText: (T) -> String,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    suppressFloatingButton: Boolean = true,
    maxListHeight: Dp = 460.dp
) {
    IosDialog(
        onDismiss = onDismiss,
        suppressFloatingButton = suppressFloatingButton
    ) { dismiss ->
        Column {
            IosDialogTitle(title)
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier
                    .heightIn(max = maxListHeight)
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clickable {
                                onSelected(option)
                                dismiss()
                            }
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = optionText(option),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (option == selectedOption) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (option == selectedOption) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "已选择",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    if (index != options.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 20.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IosMessageDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    IosDialog(onDismiss = onDismiss) { dismiss ->
        Column {
            IosDialogTitle(title)
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = message,
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            TextButton(
                onClick = dismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text("确定", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun IosDialogTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

private const val IOS_DIALOG_ANIMATION_MILLIS = 180L
