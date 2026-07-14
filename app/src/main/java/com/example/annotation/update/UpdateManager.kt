package com.example.annotation.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.annotation.model.UpdateCheckResult
import com.example.annotation.model.VersionInfo
import com.example.annotation.network.UpdateService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 更新管理器 - 负责版本检查和更新逻辑
 */
class UpdateManager(private val context: Context) {

    private val updateService = UpdateService()

    /**
     * 获取当前应用的版本号
     */
    private fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * 获取当前应用的版本名称
     */
    fun getCurrentVersionName(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "未知版本"
        } catch (e: Exception) {
            e.printStackTrace()
            "未知版本"
        }
    }

    /**
     * 检查是否有更新
     * @return UpdateCheckResult 更新检查结果
     */
    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            // 获取服务器版本信息
            val versionInfo = updateService.fetchVersionInfo()
                ?: return@withContext UpdateCheckResult.Error("无法获取版本信息")

            // 获取当前版本号
            val currentVersionCode = getCurrentVersionCode()

            // 判断是否需要更新
            when {
                // 当前版本低于最低支持版本，强制更新
                currentVersionCode < versionInfo.minSupportVersion -> {
                    UpdateCheckResult.HasUpdate(
                        versionInfo = versionInfo,
                        isForceUpdate = true
                    )
                }
                // 有新版本且设置了强制更新
                currentVersionCode < versionInfo.latestVersionCode && versionInfo.forceUpdate -> {
                    UpdateCheckResult.HasUpdate(
                        versionInfo = versionInfo,
                        isForceUpdate = true
                    )
                }
                // 有新版本但不强制更新
                currentVersionCode < versionInfo.latestVersionCode -> {
                    UpdateCheckResult.HasUpdate(
                        versionInfo = versionInfo,
                        isForceUpdate = false
                    )
                }
                // 已是最新版本
                else -> {
                    UpdateCheckResult.NoUpdate
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateCheckResult.Error("检查更新失败: ${e.message}")
        }
    }

    /**
     * 比较版本号
     * @return true 如果服务器版本更新
     */
    private fun isServerVersionNewer(
        currentVersion: Int,
        serverVersion: Int
    ): Boolean {
        return serverVersion > currentVersion
    }
}
