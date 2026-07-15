package com.example.annotation.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun outlinedThemeIcon(
    name: String,
    content: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = content
    )
}.build()

val SystemModeIcon: ImageVector = outlinedThemeIcon("SystemMode") {
    moveTo(3f, 4.5f)
    lineTo(21f, 4.5f)
    lineTo(21f, 17f)
    lineTo(3f, 17f)
    close()
    moveTo(8f, 21f)
    lineTo(16f, 21f)
    moveTo(12f, 17f)
    lineTo(12f, 21f)
    moveTo(12f, 8f)
    arcToRelative(2.75f, 2.75f, 0f, false, true, 0f, 5.5f)
    arcToRelative(2.75f, 2.75f, 0f, false, true, 0f, -5.5f)
}

val LightModeIcon: ImageVector = outlinedThemeIcon("LightMode") {
    moveTo(12f, 8f)
    arcToRelative(4f, 4f, 0f, true, true, 0f, 8f)
    arcToRelative(4f, 4f, 0f, true, true, 0f, -8f)
    moveTo(12f, 2f)
    lineTo(12f, 4f)
    moveTo(12f, 20f)
    lineTo(12f, 22f)
    moveTo(4.93f, 4.93f)
    lineTo(6.34f, 6.34f)
    moveTo(17.66f, 17.66f)
    lineTo(19.07f, 19.07f)
    moveTo(2f, 12f)
    lineTo(4f, 12f)
    moveTo(20f, 12f)
    lineTo(22f, 12f)
    moveTo(4.93f, 19.07f)
    lineTo(6.34f, 17.66f)
    moveTo(17.66f, 6.34f)
    lineTo(19.07f, 4.93f)
}

val DarkModeIcon: ImageVector = outlinedThemeIcon("DarkMode") {
    moveTo(20.5f, 15.1f)
    curveTo(19.2f, 15.8f, 17.8f, 16.2f, 16.2f, 16.2f)
    curveTo(11.6f, 16.2f, 7.8f, 12.4f, 7.8f, 7.8f)
    curveTo(7.8f, 6.2f, 8.2f, 4.8f, 8.9f, 3.5f)
    curveTo(5.1f, 4.8f, 2.5f, 8.4f, 2.5f, 12.5f)
    curveTo(2.5f, 17.5f, 6.5f, 21.5f, 11.5f, 21.5f)
    curveTo(15.6f, 21.5f, 19.2f, 18.9f, 20.5f, 15.1f)
    close()
}
