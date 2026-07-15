package com.example.annotation.network

import com.example.annotation.model.VersionInfo
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 更新服务 - 负责从GitHub获取版本信息
 */
class UpdateService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        // GitHub Raw URL - 直接访问文件内容
        private const val VERSION_JSON_URL =
            "https://raw.githubusercontent.com/Abnerla/fenbi_biaozhu/main/version.json"
    }

    /**
     * 从GitHub获取版本信息
     * @return VersionInfo 版本信息对象，失败返回null
     */
    suspend fun fetchVersionInfo(): VersionInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$VERSION_JSON_URL?t=${System.currentTimeMillis()}")
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonString = response.body?.string()
                    if (!jsonString.isNullOrBlank()) {
                        gson.fromJson(jsonString, VersionInfo::class.java)
                            ?.takeIf(VersionInfo::isValid)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
