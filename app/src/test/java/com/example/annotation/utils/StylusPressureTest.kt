package com.example.annotation.utils

import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class StylusPressureTest {
    @Test
    fun stylusPressureIsUsedOnlyWhenStylusAdaptationIsEnabled() {
        assertEquals(
            0.25f,
            StylusPressure.resolve(true, MotionEvent.TOOL_TYPE_STYLUS, 0.25f),
            0f
        )
        assertEquals(
            1f,
            StylusPressure.resolve(false, MotionEvent.TOOL_TYPE_STYLUS, 0.25f),
            0f
        )
    }

    @Test
    fun eraserTipSupportsPressureButTouchAndMouseKeepConfiguredSize() {
        assertEquals(
            0.4f,
            StylusPressure.resolve(true, MotionEvent.TOOL_TYPE_ERASER, 0.4f),
            0f
        )
        assertEquals(
            1f,
            StylusPressure.resolve(true, MotionEvent.TOOL_TYPE_FINGER, 0.4f),
            0f
        )
        assertEquals(
            1f,
            StylusPressure.resolve(true, MotionEvent.TOOL_TYPE_MOUSE, 0.4f),
            0f
        )
    }

    @Test
    fun reportedPressureIsClampedAndInvalidValuesFallBackToFullSize() {
        assertEquals(
            1f,
            StylusPressure.resolve(true, MotionEvent.TOOL_TYPE_STYLUS, 1.5f),
            0f
        )
        assertEquals(
            0f,
            StylusPressure.resolve(true, MotionEvent.TOOL_TYPE_STYLUS, -0.2f),
            0f
        )
        assertEquals(
            1f,
            StylusPressure.resolve(true, MotionEvent.TOOL_TYPE_STYLUS, Float.NaN),
            0f
        )
    }
}
