package com.example.annotation.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.example.annotation.drawing.DrawingEngine
import com.example.annotation.model.DrawingTool
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object ScreenshotHelper {

    /**
     * 保存标注内容为图片
     */
    fun saveAnnotationToImage(
        context: Context,
        drawingEngine: DrawingEngine,
        width: Int,
        height: Int,
        includeBackground: Boolean = false,
        backgroundColor: Int = android.graphics.Color.WHITE
    ): Result<Uri> {
        return try {
            val bitmap = createAnnotationBitmap(drawingEngine, width, height, includeBackground, backgroundColor)
            val uri = saveBitmapToGallery(context, bitmap)
            bitmap.recycle()
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 捕获View并与标注内容合成保存
     */
    fun captureViewWithAnnotation(
        context: Context,
        view: View,
        drawingEngine: DrawingEngine
    ): Result<Uri> {
        return try {
            // 捕获底层视图内容
            val viewBitmap = captureView(view)

            // 在视图位图上绘制标注
            val canvas = Canvas(viewBitmap)
            drawingEngine.paths.forEach { path ->
                drawPathOnCanvas(canvas, path, drawingEngine)
            }

            // 保存到相册
            val uri = saveBitmapToGallery(context, viewBitmap)
            viewBitmap.recycle()
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 捕获View的位图
     */
    private fun captureView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    /**
     * 创建标注内容的位图
     */
    private fun createAnnotationBitmap(
        drawingEngine: DrawingEngine,
        width: Int,
        height: Int,
        includeBackground: Boolean,
        backgroundColor: Int = android.graphics.Color.WHITE
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 设置背景
        if (includeBackground) {
            canvas.drawColor(backgroundColor)
        } else {
            canvas.drawColor(android.graphics.Color.TRANSPARENT)
        }

        // 绘制所有路径
        drawingEngine.paths.forEach { path ->
            drawPathOnCanvas(canvas, path, drawingEngine)
        }

        return bitmap
    }

    /**
     * 在Canvas上绘制路径
     */
    fun drawPathOnCanvas(
        canvas: Canvas,
        path: com.example.annotation.model.DrawingPath,
        drawingEngine: DrawingEngine
    ) {
        if (path.points.size < 2) return

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        when (path.tool) {
            DrawingTool.PEN -> {
                path.penConfig?.let { config ->
                    paint.color = colorToAndroidColor(config.color)
                    paint.alpha = 255 // 画笔完全不透明
                    val avgPressure = path.points.map { it.pressure }.average().toFloat()
                    paint.strokeWidth = drawingEngine.calculatePressureAdjustedWidth(
                        config.strokeWidth,
                        avgPressure
                    )
                }
            }
            DrawingTool.HIGHLIGHTER -> {
                path.highlighterConfig?.let { config ->
                    paint.color = colorToAndroidColor(config.color)
                    paint.alpha = (config.alpha * 255).toInt()
                    val avgPressure = path.points.map { it.pressure }.average().toFloat()
                    paint.strokeWidth = drawingEngine.calculatePressureAdjustedWidth(
                        config.strokeWidth,
                        avgPressure
                    )
                }
            }
            DrawingTool.ERASER -> {
                // 橡皮擦在保存时不绘制
                return
            }
        }

        // 绘制平滑路径
        val androidPath = android.graphics.Path()
        val firstPoint = path.points.first()
        androidPath.moveTo(firstPoint.offset.x, firstPoint.offset.y)

        for (i in 1 until path.points.size) {
            val prevPoint = path.points[i - 1]
            val currentPoint = path.points[i]

            if (i < path.points.size - 1) {
                val controlX = (prevPoint.offset.x + currentPoint.offset.x) / 2
                val controlY = (prevPoint.offset.y + currentPoint.offset.y) / 2
                androidPath.quadTo(
                    prevPoint.offset.x,
                    prevPoint.offset.y,
                    controlX,
                    controlY
                )
            } else {
                androidPath.lineTo(currentPoint.offset.x, currentPoint.offset.y)
            }
        }

        canvas.drawPath(androidPath, paint)
    }

    /**
     * 将Compose Color转换为Android Color
     */
    private fun colorToAndroidColor(color: Color): Int {
        return android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
    }

    /**
     * 保存位图到相册
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "粉笔标注_$timestamp.png"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用MediaStore
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/粉笔标注")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: throw IOException("Failed to create MediaStore entry")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            } ?: throw IOException("Failed to open output stream")

            uri
        } else {
            // Android 9 及以下使用传统文件存储
            val picturesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            val appDir = File(picturesDir, "粉笔标注")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }

            val file = File(appDir, fileName)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            Uri.fromFile(file)
        }
    }
}
