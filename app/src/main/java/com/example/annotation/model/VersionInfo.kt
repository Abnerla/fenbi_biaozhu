package com.example.annotation.model

import com.google.gson.annotations.SerializedName

/**
 * 版本信息数据模型
 */
data class VersionInfo(
    @SerializedName("latestVersionCode")
    val latestVersionCode: Int,          // 最新版本号（整数，用于比较）

    @SerializedName("latestVersionName")
    val latestVersionName: String,       // 版本名称

    @SerializedName("updateDesc")
    val updateDesc: String,              // 更新说明

    @SerializedName("downloadUrl")
    val downloadUrl: String,             // APK下载地址

    @SerializedName("forceUpdate")
    val forceUpdate: Boolean = false,    // 是否强制更新

    @SerializedName("minSupportVersion")
    val minSupportVersion: Int           // 最低支持版本（低于此版本强制更新）
) {
    fun isValid(): Boolean =
        latestVersionCode > 0 &&
            latestVersionName.isNotBlank() &&
            updateDesc.isNotBlank() &&
            downloadUrl.startsWith("https://") &&
            minSupportVersion in 0..latestVersionCode
}

/**
 * 版本检查结果
 */
sealed class UpdateCheckResult {
    data class HasUpdate(
        val versionInfo: VersionInfo,
        val isForceUpdate: Boolean  // 是否需要强制更新
    ) : UpdateCheckResult()

    object NoUpdate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

fun compareAppVersion(
    currentVersionCode: Int,
    versionInfo: VersionInfo
): UpdateCheckResult {
    if (currentVersionCode >= versionInfo.latestVersionCode) {
        return UpdateCheckResult.NoUpdate
    }

    return UpdateCheckResult.HasUpdate(
        versionInfo = versionInfo,
        isForceUpdate = versionInfo.forceUpdate ||
            currentVersionCode < versionInfo.minSupportVersion
    )
}
