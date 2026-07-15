package com.example.annotation.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AnnotationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val activity = view.context.findActivity()
    if (!view.isInEditMode && activity != null) {
        SideEffect {
            val window = activity.window
            window.statusBarColor = IOSSurface.toArgb()
            window.navigationBarColor = IOSBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    MaterialTheme(
        colorScheme = IOSLightColorScheme,
        typography = Typography,
        content = content
    )
}
