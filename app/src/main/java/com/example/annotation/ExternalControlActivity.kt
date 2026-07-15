package com.example.annotation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import com.example.annotation.service.GestureForwardingAccessibilityService
import com.example.annotation.service.OverlayService
import com.example.annotation.utils.PreferencesManager

/** Receives explicit cross-app commands and exits without showing application UI. */
class ExternalControlActivity : ComponentActivity() {
    companion object {
        const val ACTION_CONTROL = "com.example.annotation.action.CONTROL"
        const val EXTRA_ANNOTATION_MODE = "annotation_mode"
        const val EXTRA_FLOATING_WINDOW = "floating_window"
        const val EXTRA_RESULT_CODE = "result_code"

        const val VALUE_ON = "on"
        const val VALUE_OFF = "off"
        const val VALUE_TOGGLE = "toggle"

        const val RESULT_OK = "ok"
        const val RESULT_NO_COMMAND = "no_command"
        const val RESULT_INVALID_VALUE = "invalid_value"
        const val RESULT_ANNOTATION_CONTROL_DISABLED = "annotation_control_disabled"
        const val RESULT_FLOATING_CONTROL_DISABLED = "floating_control_disabled"
        const val RESULT_OVERLAY_PERMISSION_REQUIRED = "overlay_permission_required"
        const val RESULT_STYLUS_KEY_BRIDGE_REQUIRED = "stylus_key_bridge_required"
    }

    private enum class Command { ON, OFF, TOGGLE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleControlIntent(intent)
        finish()
    }

    private fun handleControlIntent(controlIntent: Intent) {
        val annotationValue = controlIntent.getStringExtra(EXTRA_ANNOTATION_MODE)
        val floatingValue = controlIntent.getStringExtra(EXTRA_FLOATING_WINDOW)
        if (annotationValue == null && floatingValue == null) {
            finishWithResult(Activity.RESULT_CANCELED, RESULT_NO_COMMAND)
            return
        }

        val annotationCommand = annotationValue?.let(::parseCommand)
        val floatingCommand = floatingValue?.let(::parseCommand)
        if ((annotationValue != null && annotationCommand == null) ||
            (floatingValue != null && floatingCommand == null)
        ) {
            finishWithResult(Activity.RESULT_CANCELED, RESULT_INVALID_VALUE)
            return
        }

        val preferences = PreferencesManager(this)
        if (annotationCommand != null && !preferences.getAllowExternalAnnotationControl()) {
            finishWithResult(Activity.RESULT_CANCELED, RESULT_ANNOTATION_CONTROL_DISABLED)
            return
        }
        if (floatingCommand != null && !preferences.getAllowExternalFloatingWindowControl()) {
            finishWithResult(Activity.RESULT_CANCELED, RESULT_FLOATING_CONTROL_DISABLED)
            return
        }

        val serviceWasRunning = OverlayService.isRunning
        val targetServiceRunning = when (floatingCommand) {
            Command.ON -> true
            Command.OFF -> false
            Command.TOGGLE -> !serviceWasRunning
            null -> serviceWasRunning || annotationCommand == Command.ON ||
                (annotationCommand == Command.TOGGLE && !OverlayService.isAnnotationModeActive)
        }

        if (targetServiceRunning && !Settings.canDrawOverlays(this)) {
            finishWithResult(Activity.RESULT_CANCELED, RESULT_OVERLAY_PERMISSION_REQUIRED)
            return
        }

        if (!targetServiceRunning) {
            OverlayService.stop(this)
            finishWithResult(Activity.RESULT_OK, RESULT_OK)
            return
        }

        val targetAnnotationMode = when (annotationCommand) {
            Command.ON -> true
            Command.OFF -> false
            Command.TOGGLE -> !OverlayService.isAnnotationModeActive
            null -> null
        }

        if (targetAnnotationMode == true && preferences.requiresStylusKeyBridge() &&
            !GestureForwardingAccessibilityService.isReady
        ) {
            finishWithResult(Activity.RESULT_CANCELED, RESULT_STYLUS_KEY_BRIDGE_REQUIRED)
            return
        }

        when {
            targetAnnotationMode != null -> {
                if (!serviceWasRunning && !targetAnnotationMode) {
                    OverlayService.start(this)
                } else {
                    OverlayService.setAnnotationMode(this, targetAnnotationMode)
                }
            }
            !serviceWasRunning -> OverlayService.start(this)
        }

        finishWithResult(Activity.RESULT_OK, RESULT_OK)
    }

    private fun parseCommand(value: String): Command? = when (value.lowercase()) {
        VALUE_ON -> Command.ON
        VALUE_OFF -> Command.OFF
        VALUE_TOGGLE -> Command.TOGGLE
        else -> null
    }

    private fun finishWithResult(resultCode: Int, code: String) {
        setResult(resultCode, Intent().putExtra(EXTRA_RESULT_CODE, code))
    }
}
