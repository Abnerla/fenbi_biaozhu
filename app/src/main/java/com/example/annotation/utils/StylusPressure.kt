package com.example.annotation.utils

import android.view.MotionEvent

object StylusPressure {
    fun resolve(stylusEnabled: Boolean, toolType: Int, reportedPressure: Float): Float {
        val isStylusTool = toolType == MotionEvent.TOOL_TYPE_STYLUS ||
            toolType == MotionEvent.TOOL_TYPE_ERASER
        if (!stylusEnabled || !isStylusTool || !reportedPressure.isFinite()) return 1f
        return reportedPressure.coerceIn(0f, 1f)
    }
}
