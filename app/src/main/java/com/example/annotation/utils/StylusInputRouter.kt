package com.example.annotation.utils

import android.view.KeyEvent
import android.view.MotionEvent
import android.util.Log
import com.example.annotation.model.StylusButton
import com.example.annotation.model.StylusButtonAction
import com.example.annotation.model.StylusButtonMasks
import com.example.annotation.model.StylusDeviceIdentity
import com.example.annotation.model.StylusGesture
import com.example.annotation.model.standardStylusButtonForKeyCode

class StylusInputRouter(
    private val preferencesManager: PreferencesManager,
    private val onAction: (StylusButtonAction) -> Unit
) {
    private var motionState = StylusButtonStateSnapshot()
    private var keyState = StylusButtonStateSnapshot()
    private var deliveredState = StylusButtonStateSnapshot()
    private var currentIdentity: StylusDeviceIdentity? = null
    private val detector = StylusButtonGestureDetector { button, pressType ->
        val mappings = preferencesManager.getStylusButtonMappings()
        val configuredAction = mappings.actionFor(button, pressType)
        val action = if (configuredAction == StylusButtonAction.VENDOR_DEFAULT) {
            preferencesManager.resolveStylusPreset(currentIdentity)
                ?.emulatedActions
                ?.get(StylusGesture(button, pressType))
        } else {
            configuredAction
        }
        Log.d(TAG, "gesture button=$button type=$pressType action=$action")
        action?.let(onAction)
    }

    fun processMotionEvent(event: MotionEvent): Boolean {
        if (!preferencesManager.getStylusEnabled()) return false

        val identity = StylusDeviceIdentity.fromInputDevice(event.device)
        val learned = preferencesManager.getStylusLearnedBindings(identity.stableKey)
        val hasLearnedMotionBinding = learned.primaryMask != 0 || learned.secondaryMask != 0
        val report = StylusInputMonitor.publishMotion(
            event,
            force = hasLearnedMotionBinding
        ) ?: return false

        currentIdentity = report.device
        val configured = preferencesManager.getStylusButtonMasks()
        val masks = StylusButtonMasks(
            primary = learned.primaryMask.takeIf { it != 0 } ?: configured.primary,
            secondary = learned.secondaryMask.takeIf { it != 0 } ?: configured.secondary
        )
        val normalized = StylusEventNormalizer.normalizeMotion(
            actionMasked = event.actionMasked,
            actionButton = event.actionButton,
            buttonState = event.buttonState,
            masks = masks,
            previous = motionState
        )
        motionState = normalized.state
        synchronizeDetector(event.eventTime)

        val mappings = preferencesManager.getStylusButtonMappings()
        val explicitButton = StylusEventNormalizer.buttonForMask(event.actionButton, masks)
        val ownsTransition = normalized.transitions.any { mappings.owns(it.button) }
        return explicitButton?.let(mappings::owns) == true || ownsTransition
    }

    fun processKeyEvent(event: KeyEvent): Boolean {
        if (!preferencesManager.getStylusEnabled()) return false

        val identity = StylusDeviceIdentity.fromInputDevice(event.device)
        val learned = preferencesManager.getStylusLearnedBindings(identity.stableKey)
        val button = when (event.keyCode) {
            learned.primaryKeyCode -> StylusButton.PRIMARY.takeIf { learned.primaryKeyCode != 0 }
            learned.secondaryKeyCode -> StylusButton.SECONDARY.takeIf { learned.secondaryKeyCode != 0 }
            else -> standardStylusButtonForKeyCode(event.keyCode)
        } ?: return false

        Log.d(
            TAG,
            "key action=${event.action} code=${event.keyCode} device=${identity.stableKey} button=$button"
        )
        StylusInputMonitor.publishKey(event, force = true)
        currentIdentity = identity

        val mappings = preferencesManager.getStylusButtonMappings()
        if (!mappings.owns(button)) return false

        val normalized = StylusEventNormalizer.normalizeKey(button, event.action, keyState)
        keyState = normalized.state
        synchronizeDetector(event.eventTime)
        return true
    }

    private fun synchronizeDetector(eventTime: Long) {
        val mappings = preferencesManager.getStylusButtonMappings()
        StylusButton.entries.forEach { button ->
            val pressed = motionState.isPressed(button) || keyState.isPressed(button)
            if (deliveredState.isPressed(button) != pressed) {
                deliveredState = deliveredState.with(button, pressed)
                if (mappings.owns(button)) {
                    detector.processTransition(button, pressed, eventTime)
                }
            }
        }
    }

    fun dispose() {
        detector.dispose()
        motionState = StylusButtonStateSnapshot()
        keyState = StylusButtonStateSnapshot()
        deliveredState = StylusButtonStateSnapshot()
        currentIdentity = null
    }

    private companion object {
        const val TAG = "StylusInputRouter"
    }
}
