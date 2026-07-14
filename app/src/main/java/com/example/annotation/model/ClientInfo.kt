package com.example.annotation.model

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONObject
import java.net.URLEncoder

/**
 * 客户端信息数据类
 * 用于向兔小巢反馈系统传递应用和设备信息
 */
data class ClientInfo(
    val appName: String,              // 应用名称
    val appVersion: String,            // 应用版本
    val osVersion: String,             // 操作系统版本
    val device: String,                // 设备型号
    val customInfo: Map<String, String> = emptyMap()  // 自定义参数
) {
    companion object {
        /**
         * 从 Context 创建客户端信息
         */
        fun fromContext(context: Context, customInfo: Map<String, String> = emptyMap()): ClientInfo {
            val packageManager = context.packageManager
            val packageInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(context.packageName, 0)
                }
            } catch (e: Exception) {
                null
            }

            val appName = try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(context.packageName, 0)
                ).toString()
            } catch (e: Exception) {
                "未知应用"
            }

            val appVersion = packageInfo?.versionName ?: "未知版本"
            val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            val device = "${Build.MANUFACTURER} ${Build.MODEL}"

            return ClientInfo(
                appName = appName,
                appVersion = appVersion,
                osVersion = osVersion,
                device = device,
                customInfo = customInfo
            )
        }
    }

    /**
     * 转换为 JSON 字符串
     */
    fun toJson(): String {
        val jsonObject = JSONObject()
        jsonObject.put("appName", appName)
        jsonObject.put("appVersion", appVersion)
        jsonObject.put("osVersion", osVersion)
        jsonObject.put("device", device)

        // 添加自定义参数
        if (customInfo.isNotEmpty()) {
            val customInfoJson = JSONObject()
            customInfo.forEach { (key, value) ->
                customInfoJson.put(key, value)
            }
            jsonObject.put("customInfo", customInfoJson)
        }

        return jsonObject.toString()
    }

    /**
     * 转换为 URL 编码的字符串，用于 POST 请求
     */
    fun toUrlEncodedString(): String {
        return "clientInfo=${URLEncoder.encode(toJson(), "UTF-8")}"
    }

    /**
     * 获取 POST 请求的字节数组
     */
    fun toPostData(): ByteArray {
        return toUrlEncodedString().toByteArray(Charsets.UTF_8)
    }
}
