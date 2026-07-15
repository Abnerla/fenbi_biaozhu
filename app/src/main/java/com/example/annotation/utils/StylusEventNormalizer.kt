package com.example.annotation.utils

import android.view.KeyEvent
import android.view.MotionEvent
import com.example.annotation.model.StylusButton
import com.example.annotation.model.StylusButtonMasks

data class StylusButtonStateSnapshot(
    val primaryPressed: Boolean = false,
    val secondaryPressed: Boolean = false
) {
    fun isPressed(button: StylusButton): Boolean = when (button) {
        StylusButton.PRIMARY -> primaryPressed
        StylusButton.SECONDARY -> secondaryPressed
    }

    fun with(button: StylusButton, pressed: Boolean): StylusButtonStateSnapshot = when (button) {
        StylusButton.PRIMARY -> copy(primaryPressed = pressed)
        StylusButton.SECONDARY -> copy(secondaryPressed = pressed)
    }
}

data class StylusButtonTransition(val button: StylusButton, val pressed: Boolean)

data class NormalizedStylusEvent(
    val state: StylusButtonStateSnapshot,
    val transitions: List<StylusButtonTransition>
)

object StylusEventNormalizer {
    fun normalizeMotion(
        actionMasked: Int,
        actionButton: Int,
        buttonState: Int,
        masks: StylusButtonMasks,
        previous: StylusButtonStateSnapshot
    ): NormalizedStylusEvent {
        val explicitButton = buttonForMask(actionButton, masks)
        if (explicitButton != null &&
            (actionMasked == MotionEvent.ACTION_BUTTON_PRESS ||
                actionMasked == MotionEvent.ACTION_BUTTON_RELEASE)
        ) {
            val pressed = actionMasked == MotionEvent.ACTION_BUTTON_PRESS
            if (previous.isPressed(explicitButton) == pressed) {
                return NormalizedStylusEvent(previous, emptyList())
            }
            return NormalizedStylusEvent(
                previous.with(explicitButton, pressed),
                listOf(StylusButtonTransition(explicitButton, pressed))
            )
        }

        var state = previous
        val transitions = buildList {
            listOf(
                StylusButton.PRIMARY to (buttonState and masks.primary != 0),
                StylusButton.SECONDARY to (buttonState and masks.secondary != 0)
            ).forEach { (button, pressed) ->
                if (state.isPressed(button) != pressed) {
                    state = state.with(button, pressed)
                    add(StylusButtonTransition(button, pressed))
                }
            }
        }
        return NormalizedStylusEvent(state, transitions)
    }

    fun normalizeKey(
        button: StylusButton,
        action: Int,
        previous: StylusButtonStateSnapshot
    ): NormalizedStylusEvent {
        val pressed = when (action) {
            KeyEvent.ACTION_DOWN -> true
            KeyEvent.ACTION_UP -> false
            else -> return NormalizedStylusEvent(previous, emptyList())
        }
        if (previous.isPressed(button) == pressed) {
            return NormalizedStylusEvent(previous, emptyList())
        }
        return NormalizedStylusEvent(
            previous.with(button, pressed),
            listOf(StylusButtonTransition(button, pressed))
        )
    }

    fun buttonForMask(mask: Int, masks: StylusButtonMasks): StylusButton? = when {
        mask != 0 && mask and masks.primary != 0 -> StylusButton.PRIMARY
        mask != 0 && mask and masks.secondary != 0 -> StylusButton.SECONDARY
        else -> null
    }
}
