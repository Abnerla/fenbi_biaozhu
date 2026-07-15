package com.example.annotation.utils

import android.os.Handler
import android.os.Looper
import com.example.annotation.model.StylusButton
import com.example.annotation.model.StylusPressType

class StylusButtonGestureDetector(
    private val onGesture: (StylusButton, StylusPressType) -> Unit
) {
    private data class ButtonState(
        var pressed: Boolean = false,
        var longTriggered: Boolean = false,
        var lastReleaseTime: Long = 0L,
        var pendingSingle: Runnable? = null,
        var pendingLong: Runnable? = null
    )

    private val handler = Handler(Looper.getMainLooper())
    private val states = mapOf(
        StylusButton.PRIMARY to ButtonState(),
        StylusButton.SECONDARY to ButtonState()
    )

    fun process(buttonState: Int, primaryMask: Int, secondaryMask: Int, eventTime: Long) {
        update(StylusButton.PRIMARY, buttonState and primaryMask != 0, eventTime)
        update(StylusButton.SECONDARY, buttonState and secondaryMask != 0, eventTime)
    }

    fun dispose() {
        states.values.forEach { state ->
            state.pendingSingle?.let(handler::removeCallbacks)
            state.pendingLong?.let(handler::removeCallbacks)
        }
    }

    private fun update(button: StylusButton, pressed: Boolean, eventTime: Long) {
        val state = states.getValue(button)
        if (pressed == state.pressed) return
        state.pressed = pressed

        if (pressed) {
            state.longTriggered = false
            val longAction = Runnable {
                if (state.pressed) {
                    state.longTriggered = true
                    state.pendingSingle?.let(handler::removeCallbacks)
                    state.pendingSingle = null
                    onGesture(button, StylusPressType.LONG)
                }
            }
            state.pendingLong = longAction
            handler.postDelayed(longAction, LONG_PRESS_MILLIS)
            return
        }

        state.pendingLong?.let(handler::removeCallbacks)
        state.pendingLong = null
        if (state.longTriggered) return

        val isDouble = state.pendingSingle != null &&
            eventTime - state.lastReleaseTime <= DOUBLE_CLICK_MILLIS
        if (isDouble) {
            state.pendingSingle?.let(handler::removeCallbacks)
            state.pendingSingle = null
            state.lastReleaseTime = 0L
            onGesture(button, StylusPressType.DOUBLE)
        } else {
            state.lastReleaseTime = eventTime
            val singleAction = Runnable {
                state.pendingSingle = null
                onGesture(button, StylusPressType.SINGLE)
            }
            state.pendingSingle = singleAction
            handler.postDelayed(singleAction, DOUBLE_CLICK_MILLIS)
        }
    }

    private companion object {
        const val DOUBLE_CLICK_MILLIS = 280L
        const val LONG_PRESS_MILLIS = 520L
    }
}
