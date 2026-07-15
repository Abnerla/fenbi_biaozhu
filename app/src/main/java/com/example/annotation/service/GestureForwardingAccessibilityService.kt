package com.example.annotation.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.example.annotation.utils.StylusInputMonitor

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

        fun takeSystemScreenshot(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) ?: false
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        instance = this
        OverlayService.onStylusKeyBridgeConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (StylusInputMonitor.isLearning) StylusInputMonitor.publishKey(event, force = true)
        if (!OverlayService.isAnnotationModeActive) return false
        if (!OverlayService.shouldConsumeStylusKeyEvent(event)) return false
        OverlayService.processStylusKeyEvent(event)
        return true
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        detachBridge()
        OverlayService.setOverlayTouchable(true)
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        detachBridge()
        return super.onUnbind(intent)
    }

    private fun detachBridge() {
        if (instance !== this) return
        instance = null
        OverlayService.onStylusKeyBridgeDisconnected()
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
