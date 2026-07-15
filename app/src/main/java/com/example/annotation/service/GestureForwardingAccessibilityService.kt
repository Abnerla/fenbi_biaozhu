package com.example.annotation.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class GestureForwardingAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile
        private var instance: GestureForwardingAccessibilityService? = null

        val isReady: Boolean
            get() = instance != null

        fun forwardSwipe(
            startX: Float,
            startY: Float,
            endX: Float,
            endY: Float,
            durationMillis: Long
        ): Boolean = instance?.dispatchForwardedSwipe(
            startX,
            startY,
            endX,
            endY,
            durationMillis
        ) ?: false
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        OverlayService.setOverlayTouchable(true)
        super.onDestroy()
    }

    private fun dispatchForwardedSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMillis: Long
    ): Boolean {
        OverlayService.setOverlayTouchable(false)
        handler.postDelayed({
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        0,
                        durationMillis.coerceIn(120L, 1000L)
                    )
                )
                .build()
            val dispatched = dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        OverlayService.setOverlayTouchable(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        OverlayService.setOverlayTouchable(true)
                    }
                },
                handler
            )
            if (!dispatched) OverlayService.setOverlayTouchable(true)
        }, 40L)
        return true
    }
}
