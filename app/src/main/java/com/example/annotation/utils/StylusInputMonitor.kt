package com.example.annotation.utils

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.example.annotation.model.StylusDeviceIdentity
import com.example.annotation.model.StylusVendorPresetCatalog
import com.example.annotation.model.standardStylusButtonForKeyCode
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class StylusInputEventKind { MOTION, KEY }

data class StylusInputReport(
    val sequence: Long,
    val kind: StylusInputEventKind,
    val device: StylusDeviceIdentity,
    val action: Int,
    val source: Int,
    val toolType: Int,
    val buttonState: Int = 0,
    val actionButton: Int = 0,
    val keyCode: Int = 0,
    val repeatCount: Int = 0,
    val eventTime: Long
) {
    val hasButtonInput: Boolean
        get() = when (kind) {
            StylusInputEventKind.MOTION -> candidateMotionMask != 0
            StylusInputEventKind.KEY -> keyCode != 0
        }

    val candidateMotionMask: Int
        get() = when {
            actionButton != 0 -> actionButton
            buttonState != 0 -> Integer.lowestOneBit(buttonState)
            else -> 0
        }

    val summary: String
        get() = when (kind) {
            StylusInputEventKind.MOTION ->
                "Motion action=$action buttonState=$buttonState actionButton=$actionButton"
            StylusInputEventKind.KEY ->
                "Key action=$action keyCode=$keyCode repeat=$repeatCount"
        }
}

object StylusInputMonitor {
    private val nextSequence = AtomicLong(0L)
    private val learning = AtomicBoolean(false)
    private val _latestReport = MutableStateFlow<StylusInputReport?>(null)
    val latestReport = _latestReport.asStateFlow()
    private val _latestButtonReport = MutableStateFlow<StylusInputReport?>(null)
    val latestButtonReport = _latestButtonReport.asStateFlow()

    val isLearning: Boolean
        get() = learning.get()

    fun setLearningActive(active: Boolean) {
        learning.set(active)
    }

    fun publishMotion(event: MotionEvent, force: Boolean = false): StylusInputReport? {
        val identity = StylusDeviceIdentity.fromInputDevice(event.device)
        val toolType = if (event.pointerCount > 0) event.getToolType(0) else MotionEvent.TOOL_TYPE_UNKNOWN
        val relevant = force || event.isFromSource(InputDevice.SOURCE_STYLUS) ||
            toolType == MotionEvent.TOOL_TYPE_STYLUS ||
            toolType == MotionEvent.TOOL_TYPE_ERASER ||
            StylusVendorPresetCatalog.looksLikeStylus(identity.name) ||
            (learning.get() && (event.actionButton != 0 || event.buttonState != 0))
        if (!relevant) return null

        return StylusInputReport(
            sequence = nextSequence.incrementAndGet(),
            kind = StylusInputEventKind.MOTION,
            device = identity,
            action = event.actionMasked,
            source = event.source,
            toolType = toolType,
            buttonState = event.buttonState,
            actionButton = event.actionButton,
            eventTime = event.eventTime
        ).also { report ->
            _latestReport.value = report
            if (report.hasButtonInput) _latestButtonReport.value = report
        }
    }

    fun publishKey(event: KeyEvent, force: Boolean = false): StylusInputReport? {
        val identity = StylusDeviceIdentity.fromInputDevice(event.device)
        val relevant = force || learning.get() ||
            standardStylusButtonForKeyCode(event.keyCode) != null ||
            StylusVendorPresetCatalog.looksLikeStylus(identity.name)
        if (!relevant) return null
        return StylusInputReport(
            sequence = nextSequence.incrementAndGet(),
            kind = StylusInputEventKind.KEY,
            device = identity,
            action = event.action,
            source = event.source,
            toolType = MotionEvent.TOOL_TYPE_UNKNOWN,
            keyCode = event.keyCode,
            repeatCount = event.repeatCount,
            eventTime = event.eventTime
        ).also { report ->
            _latestReport.value = report
            if (report.hasButtonInput) _latestButtonReport.value = report
        }
    }
}
