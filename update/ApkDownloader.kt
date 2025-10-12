package com.example.annotation.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * APK下载器
 */
class ApkDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * 下载APK文件
     * @param downloadUrl APK下载地址
     * @param onProgress 下载进度回调 (0-100)
     * @return 下载的APK文件，失败返回null
     */
    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext null
            }

            val body = response.body ?: return@withContext null
            val contentLength = body.contentLength()

            // 创建APK文件
            val apkFile = File(context.getExternalFilesDir(null), "update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            // 写入文件并更新进度
            var downloadedBytes = 0L
            val buffer = ByteArray(8192)
            var lastProgress = 0

            body.byteStream().use { input ->
                apkFile.outputStream().use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        // 计算并更新进度
                        if (contentLength > 0) {
                            val progress = (downloadedBytes * 100 / contentLength).toInt()
                            if (progress != lastProgress) {
                                lastProgress = progress
                                withContext(Dispatchers.Main) {
                                    onProgress(progress)
                                }
                            }
                        }
                    }
                }
            }

            apkFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 安装APK
     * @param apkFile APK文件
     */
    fun installApk(apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0及以上使用FileProvider
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }

            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
