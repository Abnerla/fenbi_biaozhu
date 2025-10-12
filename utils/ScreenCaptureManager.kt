package com.example.annotation.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.example.annotation.drawing.DrawingEngine
import java.nio.ByteBuffer

/**
 * 屏幕捕获管理器 - 完全重写版本
 *
 * 关键特性：
 * 1. MediaProjection 必须在前台服务中初始化
 * 2. 支持权限数据持久化和恢复
 * 3. 改进的错误处理和资源管理
 * 4. 线程安全的单例模式
 */
class ScreenCaptureManager private constructor(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private val handler = Handler(Looper.getMainLooper())

    // 权限数据缓存
    private var cachedResultCode: Int = 0
    private var cachedData: Intent? = null

    companion object {
        private const val TAG = "ScreenCaptureManager"
        private const val PREFS_NAME = "screen_capture_prefs"
        private const val KEY_HAS_PERMISSION = "has_permission"

        @Volatile
        private var instance: ScreenCaptureManager? = null

        /**
         * 获取单例实例
         */
        fun getInstance(context: Context): ScreenCaptureManager {
            return instance ?: synchronized(this) {
                instance ?: ScreenCaptureManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    /**
     * 创建屏幕捕获权限请求Intent
     */
    fun createScreenCaptureIntent(): Intent {
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
            as MediaProjectionManager
        return projectionManager.createScreenCaptureIntent()
    }

    /**
     * 保存屏幕捕获权限数据（在Activity中调用）
     *
     * @param resultCode 权限请求结果码
     * @param data 权限Intent数据
     */
    fun savePermissionData(resultCode: Int, data: Intent) {
        Log.d(TAG, "========== 保存权限数据 ==========")
        Log.d(TAG, "resultCode=$resultCode")
        Log.d(TAG, "data=$data")

        // 缓存权限数据
        cachedResultCode = resultCode
        cachedData = data

        Log.d(TAG, "已缓存到内存: cachedResultCode=$cachedResultCode")
        Log.d(TAG, "已缓存到内存: cachedData=$cachedData")

        // 保存权限状态到SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HAS_PERMISSION, true).apply()

        Log.d(TAG, "已保存到SharedPreferences: has_permission=true")
        Log.d(TAG, "========== 权限数据保存完成 ==========")
    }

    /**
     * 初始化MediaProjection（必须在前台服务中调用）
     *
     * @param resultCode 权限请求结果码
     * @param data 权限Intent数据
     * @return 是否初始化成功
     */
    fun initializeMediaProjection(resultCode: Int, data: Intent): Boolean {
        try {
            Log.d(TAG, "在前台服务中初始化MediaProjection: resultCode=$resultCode")

            // 先停止之前的MediaProjection
            stopMediaProjection()

            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager

            mediaProjection = projectionManager.getMediaProjection(resultCode, data)

            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection初始化失败")
                return false
            }

            // 注册回调监听
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection已停止")
                    cleanup()
                }
            }, handler)

            // 缓存权限数据以便后续恢复
            cachedResultCode = resultCode
            cachedData = data

            Log.d(TAG, "MediaProjection初始化成功")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "初始化MediaProjection失败", e)
            return false
        }
    }

    /**
     * 检查是否有屏幕捕获权限
     */
    fun hasPermission(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasStoredPermission = prefs.getBoolean(KEY_HAS_PERMISSION, false)
        return hasStoredPermission && mediaProjection != null
    }

    /**
     * 检查是否有缓存的权限数据
     */
    fun hasCachedPermissionData(): Boolean {
        val hasData = cachedResultCode != 0 && cachedData != null
        Log.d(TAG, "hasCachedPermissionData(): $hasData (cachedResultCode=$cachedResultCode, cachedData=$cachedData)")
        return hasData
    }

    /**
     * 获取缓存的权限数据
     */
    fun getCachedPermissionData(): Pair<Int, Intent>? {
        return if (hasCachedPermissionData()) {
            Pair(cachedResultCode, cachedData!!)
        } else {
            null
        }
    }

    /**
     * 捕获屏幕并与标注合成
     *
     * @param drawingEngine 绘图引擎（包含标注路径）
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    fun captureScreenWithAnnotation(
        drawingEngine: DrawingEngine,
        onSuccess: (Uri) -> Unit,
        onError: (Exception) -> Unit
    ) {
        Log.d(TAG, "开始屏幕捕获")

        if (mediaProjection == null) {
            val error = IllegalStateException("MediaProjection未初始化，请先请求权限")
            Log.e(TAG, error.message ?: "")
            onError(error)
            return
        }

        try {
            // 获取屏幕尺寸 - 使用WindowManager而不是Context.display
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val displayMetrics = DisplayMetrics()

            // Service的Context不支持context.display，必须通过WindowManager获取
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)

            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            val screenDensity = displayMetrics.densityDpi

            Log.d(TAG, "屏幕尺寸: ${screenWidth}x${screenHeight}, DPI: $screenDensity")

            // 创建ImageReader
            imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            )

            // 创建VirtualDisplay
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                handler
            )

            if (virtualDisplay == null) {
                throw IllegalStateException("VirtualDisplay创建失败")
            }

            Log.d(TAG, "VirtualDisplay创建成功，等待渲染...")

            // 延迟捕获，确保VirtualDisplay已经渲染完成
            handler.postDelayed({
                captureImage(screenWidth, screenHeight, drawingEngine, onSuccess, onError)
            }, 300) // 增加延迟时间以确保渲染完成

        } catch (e: Exception) {
            Log.e(TAG, "屏幕捕获失败", e)
            cleanup()
            onError(e)
        }
    }

    /**
     * 捕获图像
     */
    private fun captureImage(
        width: Int,
        height: Int,
        drawingEngine: DrawingEngine,
        onSuccess: (Uri) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val image = imageReader?.acquireLatestImage()

            if (image == null) {
                Log.e(TAG, "无法获取图像")
                cleanup()
                onError(IllegalStateException("无法获取屏幕图像，请重试"))
                return
            }

            Log.d(TAG, "成功获取图像，开始转换为Bitmap")

            // 转换为Bitmap
            val screenBitmap = imageToBitmap(image, width, height)
            image.close()

            // 在Bitmap上绘制标注
            val canvas = Canvas(screenBitmap)
            val paths = drawingEngine.paths

            Log.d(TAG, "绘制 ${paths.size} 条标注路径")

            paths.forEach { path ->
                ScreenshotHelper.drawPathOnCanvas(canvas, path, drawingEngine)
            }

            // 保存到相册
            Log.d(TAG, "保存图像到相册")
            val uri = ScreenshotHelper.saveBitmapToGallery(context, screenBitmap)

            // 回收Bitmap
            screenBitmap.recycle()

            // 清理资源
            cleanup()

            Log.d(TAG, "截图保存成功: $uri")
            onSuccess(uri)

        } catch (e: Exception) {
            Log.e(TAG, "图像捕获失败", e)
            cleanup()
            onError(e)
        }
    }

    /**
     * 将Image转换为Bitmap
     */
    private fun imageToBitmap(image: Image, width: Int, height: Int): Bitmap {
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        // 创建Bitmap（考虑行填充）
        val bitmapWidth = width + rowPadding / pixelStride
        val bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)

        // 如果有行填充，裁剪到实际屏幕尺寸
        return if (rowPadding > 0 && bitmapWidth > width) {
            val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
            bitmap.recycle()
            croppedBitmap
        } else {
            bitmap
        }
    }

    /**
     * 清理VirtualDisplay和ImageReader资源
     */
    private fun cleanup() {
        Log.d(TAG, "清理屏幕捕获资源")

        try {
            virtualDisplay?.release()
            virtualDisplay = null

            imageReader?.close()
            imageReader = null
        } catch (e: Exception) {
            Log.e(TAG, "清理资源时出错", e)
        }
    }

    /**
     * 停止MediaProjection
     */
    fun stopMediaProjection() {
        Log.d(TAG, "停止MediaProjection")

        cleanup()

        try {
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            Log.e(TAG, "停止MediaProjection时出错", e)
        }
    }

    /**
     * 重置所有状态（包括清除权限信息）
     */
    fun reset() {
        Log.d(TAG, "重置ScreenCaptureManager")

        stopMediaProjection()

        cachedResultCode = 0
        cachedData = null

        // 清除SharedPreferences中的权限信息
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
