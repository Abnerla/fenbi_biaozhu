package com.example.annotation.utils

import android.view.KeyEvent
import android.view.MotionEvent
import com.example.annotation.model.StylusButton
import com.example.annotation.model.StylusButtonMasks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StylusEventNormalizerTest {
    private val masks = StylusButtonMasks(
        MotionEvent.BUTTON_STYLUS_PRIMARY,
        MotionEvent.BUTTON_STYLUS_SECONDARY
    )

    @Test
    fun explicitButtonReleaseWinsOverStaleButtonState() {
        val pressed = StylusEventNormalizer.normalizeMotion(
            MotionEvent.ACTION_BUTTON_PRESS,
            MotionEvent.BUTTON_STYLUS_SECONDARY,
            MotionEvent.BUTTON_STYLUS_SECONDARY,
            masks,
            StylusButtonStateSnapshot()
        )
        val released = StylusEventNormalizer.normalizeMotion(
            MotionEvent.ACTION_BUTTON_RELEASE,
            MotionEvent.BUTTON_STYLUS_SECONDARY,
            MotionEvent.BUTTON_STYLUS_SECONDARY,
            masks,
            pressed.state
        )

        assertTrue(pressed.state.secondaryPressed)
        assertFalse(released.state.secondaryPressed)
        assertEquals(
            listOf(StylusButtonTransition(StylusButton.SECONDARY, false)),
            released.transitions
        )
    }

    @Test
    fun buttonStateSequenceProducesOnePressAndOneRelease() {
        val pressed = StylusEventNormalizer.normalizeMotion(
            MotionEvent.ACTION_MOVE,
            0,
            MotionEvent.BUTTON_STYLUS_PRIMARY,
            masks,
            StylusButtonStateSnapshot()
        )
        val duplicate = StylusEventNormalizer.normalizeMotion(
            MotionEvent.ACTION_MOVE,
            0,
            MotionEvent.BUTTON_STYLUS_PRIMARY,
            masks,
            pressed.state
        )
        val released = StylusEventNormalizer.normalizeMotion(
            MotionEvent.ACTION_UP,
            0,
            0,
            masks,
            duplicate.state
        )

        assertEquals(1, pressed.transitions.size)
        assertTrue(duplicate.transitions.isEmpty())
        assertEquals(1, released.transitions.size)
        assertFalse(released.state.primaryPressed)
    }

    @Test
    fun repeatedKeyDownIsDeduplicated() {
        val pressed = StylusEventNormalizer.normalizeKey(
            StylusButton.PRIMARY,
            KeyEvent.ACTION_DOWN,
            StylusButtonStateSnapshot()
        )
        val repeated = StylusEventNormalizer.normalizeKey(
            StylusButton.PRIMARY,
            KeyEvent.ACTION_DOWN,
            pressed.state
        )

        assertEquals(1, pressed.transitions.size)
        assertTrue(repeated.transitions.isEmpty())
    }
}
