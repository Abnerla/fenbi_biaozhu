package com.example.annotation.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {

    const val REQUEST_CODE_OVERLAY = 1001
    const val REQUEST_CODE_NOTIFICATION = 1002
    const val REQUEST_CODE_STORAGE = 1003

    /**
     * 检查是否有悬浮窗权限
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * 请求悬浮窗权限
     */
    fun requestOverlayPermission(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY)
    }

    /**
     * 检查通知权限
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 请求通知权限
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_NOTIFICATION
            )
        }
    }

    /**
     * 检查存储权限
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 请求存储权限
     */
    fun requestStoragePermission(activity: Activity) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE_STORAGE)
    }

    /**
     * 检查前台服务媒体投影权限
     */
    fun hasForegroundServiceMediaProjectionPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required before Android 14
        }
    }

    /**
     * 请求前台服务媒体投影权限
     */
    fun requestForegroundServiceMediaProjectionPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION),
                REQUEST_CODE_STORAGE
            )
        }
    }

    /**
     * 检查所有必要权限
     */
    fun checkAllPermissions(context: Context, hasScreenCapture: Boolean = false): PermissionStatus {
        return PermissionStatus(
            hasOverlay = hasOverlayPermission(context),
            hasNotification = hasNotificationPermission(context),
            hasStorage = hasStoragePermission(context),
            hasScreenCapture = hasScreenCapture,
            hasForegroundServiceMediaProjection = hasForegroundServiceMediaProjectionPermission(context)
        )
    }
}

data class PermissionStatus(
    val hasOverlay: Boolean,
    val hasNotification: Boolean,
    val hasStorage: Boolean,
    val hasScreenCapture: Boolean = false,
    val hasForegroundServiceMediaProjection: Boolean = true
) {
    fun isAllGranted(): Boolean = hasOverlay && hasNotification && hasStorage && hasScreenCapture && hasForegroundServiceMediaProjection
}
