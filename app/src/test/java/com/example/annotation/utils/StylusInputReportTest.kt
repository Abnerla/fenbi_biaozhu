package com.example.annotation.utils

import com.example.annotation.model.StylusDeviceIdentity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StylusInputReportTest {
    private val device = StylusDeviceIdentity(
        manufacturer = "test",
        name = "test stylus",
        descriptor = "test-descriptor",
        vendorId = 1,
        productId = 2
    )

    @Test
    fun ordinaryWritingMotionIsNotTreatedAsButtonInput() {
        val report = motionReport(buttonState = 0, actionButton = 0)

        assertFalse(report.hasButtonInput)
    }

    @Test
    fun motionButtonStateIsTreatedAsButtonInput() {
        val report = motionReport(buttonState = 32, actionButton = 0)

        assertTrue(report.hasButtonInput)
    }

    @Test
    fun keyCodeIsTreatedAsButtonInput() {
        val report = StylusInputReport(
            sequence = 1,
            kind = StylusInputEventKind.KEY,
            device = device,
            action = 0,
            source = 0,
            toolType = 0,
            keyCode = 308,
            eventTime = 1
        )

        assertTrue(report.hasButtonInput)
    }

    private fun motionReport(buttonState: Int, actionButton: Int) = StylusInputReport(
        sequence = 1,
        kind = StylusInputEventKind.MOTION,
        device = device,
        action = 2,
        source = 0,
        toolType = 2,
        buttonState = buttonState,
        actionButton = actionButton,
        eventTime = 1
    )
}
