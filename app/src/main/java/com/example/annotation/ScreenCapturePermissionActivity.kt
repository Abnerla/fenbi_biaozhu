package com.example.annotation

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.annotation.service.OverlayService
import com.example.annotation.utils.ScreenCaptureManager

/** Requests one screen capture session without bringing the main app UI to the foreground. */
class ScreenCapturePermissionActivity : ComponentActivity() {
    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            ScreenCaptureManager.getInstance(this).savePermissionData(result.resultCode, data)

            val serviceIntent = Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_SET_MEDIA_PROJECTION
                putExtra(OverlayService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(OverlayService.EXTRA_DATA, data)
                putExtra(OverlayService.EXTRA_CAPTURE_AFTER_PERMISSION, true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            Toast.makeText(this, "已取消本次截图", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val manager = ScreenCaptureManager.getInstance(this)
            captureLauncher.launch(manager.createScreenCaptureIntent())
        }
    }
}
