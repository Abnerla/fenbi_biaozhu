package com.example.annotation.ui.theme

import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun iosSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = IOSSurface,
    checkedTrackColor = IOSGreen,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = IOSSurface,
    uncheckedTrackColor = IOSDivider,
    uncheckedBorderColor = Color.Transparent,
    disabledCheckedThumbColor = IOSSurface.copy(alpha = 0.8f),
    disabledCheckedTrackColor = IOSGreen.copy(alpha = 0.35f),
    disabledCheckedBorderColor = Color.Transparent,
    disabledUncheckedThumbColor = IOSSurface.copy(alpha = 0.8f),
    disabledUncheckedTrackColor = IOSDividerSoft,
    disabledUncheckedBorderColor = Color.Transparent
)

@Composable
fun iosTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = IOSSurface,
    unfocusedContainerColor = IOSSurface,
    disabledContainerColor = IOSSurfaceSecondary,
    focusedBorderColor = IOSBlue,
    unfocusedBorderColor = IOSDivider,
    disabledBorderColor = IOSDividerSoft,
    cursorColor = IOSBlue,
    focusedLabelColor = IOSBlue,
    unfocusedLabelColor = IOSSecondaryLabel
)
