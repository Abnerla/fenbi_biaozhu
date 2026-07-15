package com.example.annotation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val IOSFontFamily = FontFamily.SansSerif

private fun iosTextStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal
) = TextStyle(
    fontFamily = IOSFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp
)

val Typography = Typography(
    displayLarge = iosTextStyle(57, 64, FontWeight.Light),
    displayMedium = iosTextStyle(45, 52, FontWeight.Light),
    displaySmall = iosTextStyle(36, 44, FontWeight.Normal),
    headlineLarge = iosTextStyle(32, 40, FontWeight.SemiBold),
    headlineMedium = iosTextStyle(28, 36, FontWeight.SemiBold),
    headlineSmall = iosTextStyle(24, 32, FontWeight.SemiBold),
    titleLarge = iosTextStyle(22, 28, FontWeight.SemiBold),
    titleMedium = iosTextStyle(16, 22, FontWeight.Medium),
    titleSmall = iosTextStyle(14, 20, FontWeight.Medium),
    bodyLarge = iosTextStyle(17, 24),
    bodyMedium = iosTextStyle(15, 21),
    bodySmall = iosTextStyle(13, 18),
    labelLarge = iosTextStyle(15, 20, FontWeight.Medium),
    labelMedium = iosTextStyle(13, 18, FontWeight.Medium),
    labelSmall = iosTextStyle(11, 16, FontWeight.Normal)
)
