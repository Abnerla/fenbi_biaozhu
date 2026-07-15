package com.example.annotation.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.annotation.utils.AppThemeMode

private val IOSLightColorScheme = lightColorScheme(
    primary = IOSBlue,
    onPrimary = IOSSurface,
    primaryContainer = IOSBlueSurface,
    onPrimaryContainer = IOSLabel,
    inversePrimary = IOSBlue,
    secondary = IOSSecondaryLabel,
    onSecondary = IOSSurface,
    secondaryContainer = IOSSurfaceSecondary,
    onSecondaryContainer = IOSLabel,
    tertiary = IOSGreen,
    onTertiary = IOSSurface,
    tertiaryContainer = IOSGreenSurface,
    onTertiaryContainer = IOSLabel,
    background = IOSBackground,
    onBackground = IOSLabel,
    surface = IOSSurface,
    onSurface = IOSLabel,
    surfaceVariant = IOSSurface,
    onSurfaceVariant = IOSSecondaryLabel,
    surfaceTint = Color.Transparent,
    inverseSurface = IOSLabel,
    inverseOnSurface = IOSSurface,
    error = IOSRed,
    onError = IOSSurface,
    errorContainer = IOSRedSurface,
    onErrorContainer = IOSLabel,
    outline = IOSDivider,
    outlineVariant = IOSDividerSoft,
    scrim = Color(0x52000000)
)

private val IOSDarkColorScheme = darkColorScheme(
    primary = IOSDarkBlue,
    onPrimary = IOSDarkBackground,
    primaryContainer = IOSDarkBlueSurface,
    onPrimaryContainer = IOSDarkLabel,
    secondary = IOSDarkSecondaryLabel,
    onSecondary = IOSDarkBackground,
    secondaryContainer = IOSDarkSurfaceSecondary,
    onSecondaryContainer = IOSDarkLabel,
    tertiary = IOSDarkGreen,
    onTertiary = IOSDarkBackground,
    tertiaryContainer = IOSDarkGreenSurface,
    onTertiaryContainer = IOSDarkLabel,
    background = IOSDarkBackground,
    onBackground = IOSDarkLabel,
    surface = IOSDarkSurface,
    onSurface = IOSDarkLabel,
    surfaceVariant = IOSDarkSurface,
    onSurfaceVariant = IOSDarkSecondaryLabel,
    surfaceTint = Color.Transparent,
    error = IOSDarkRed,
    onError = IOSDarkBackground,
    errorContainer = IOSDarkRedSurface,
    onErrorContainer = IOSDarkLabel,
    outline = IOSDarkDivider,
    outlineVariant = IOSDarkDivider.copy(alpha = 0.7f),
    scrim = Color(0x99000000)
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AnnotationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val colorScheme = if (isDarkTheme) IOSDarkColorScheme else IOSLightColorScheme
    val view = LocalView.current
    val activity = view.context.findActivity()
    if (!view.isInEditMode && activity != null) {
        SideEffect {
            val window = activity.window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDarkTheme
                isAppearanceLightNavigationBars = !isDarkTheme
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
