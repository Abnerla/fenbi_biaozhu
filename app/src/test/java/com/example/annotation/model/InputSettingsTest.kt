package com.example.annotation.model

import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputSettingsTest {
    @Test
    fun detectsSupportedManufacturersAndAliases() {
        val cases = mapOf(
            "HUAWEI" to StylusProfile.HUAWEI,
            "HONOR" to StylusProfile.HONOR,
            "Xiaomi" to StylusProfile.XIAOMI,
            "Redmi" to StylusProfile.XIAOMI,
            "samsung" to StylusProfile.SAMSUNG,
            "OPPO" to StylusProfile.OPPO,
            "OnePlus" to StylusProfile.OPPO,
            "vivo" to StylusProfile.VIVO,
            "iQOO" to StylusProfile.VIVO
        )

        cases.forEach { (manufacturer, expected) ->
            val detection = StylusProfile.detect(manufacturer)
            assertEquals(expected, detection.profile)
            assertTrue(detection.knownBrand)
            assertEquals(expected.displayName, detection.displayName)
        }
    }

    @Test
    fun unknownManufacturerFallsBackToAndroidAndKeepsSystemName() {
        val detection = StylusProfile.detect("Google")

        assertEquals(StylusProfile.ANDROID, detection.profile)
        assertFalse(detection.knownBrand)
        assertEquals("Google（安卓原生）", detection.displayName)
    }

    @Test
    fun migratesLegacyProfilesToModeAndRememberedBrand() {
        assertEquals(
            LegacyStylusSettings(StylusMode.AUTO, StylusProfile.ANDROID),
            migrateLegacyStylusProfile(null)
        )
        assertEquals(
            LegacyStylusSettings(StylusMode.AUTO, StylusProfile.ANDROID),
            migrateLegacyStylusProfile("AUTO")
        )
        assertEquals(
            LegacyStylusSettings(StylusMode.CUSTOM, StylusProfile.ANDROID),
            migrateLegacyStylusProfile("CUSTOM")
        )
        assertEquals(
            LegacyStylusSettings(StylusMode.MANUAL, StylusProfile.SAMSUNG),
            migrateLegacyStylusProfile("SAMSUNG")
        )
    }

    @Test
    fun resolvesMasksForAllThreeModes() {
        val custom = StylusButtonMasks(123, 456)

        assertEquals(
            StylusButtonMasks(MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
            resolveStylusButtonMasks(
                StylusMode.AUTO,
                StylusProfile.XIAOMI,
                StylusProfile.SAMSUNG,
                custom
            )
        )
        assertEquals(
            StylusButtonMasks(MotionEvent.BUTTON_STYLUS_PRIMARY, MotionEvent.BUTTON_STYLUS_SECONDARY),
            resolveStylusButtonMasks(
                StylusMode.MANUAL,
                StylusProfile.XIAOMI,
                StylusProfile.SAMSUNG,
                custom
            )
        )
        assertEquals(
            custom,
            resolveStylusButtonMasks(
                StylusMode.CUSTOM,
                StylusProfile.XIAOMI,
                StylusProfile.SAMSUNG,
                custom
            )
        )
    }

    @Test
    fun vendorDefaultsDoNotOwnButtonsUntilUserOverridesOneGesture() {
        val defaults = StylusButtonMappings()
        assertFalse(defaults.owns(StylusButton.PRIMARY))
        assertFalse(defaults.owns(StylusButton.SECONDARY))

        val overridden = defaults.copy(secondaryLong = StylusButtonAction.SCREENSHOT)
        assertTrue(overridden.owns(StylusButton.SECONDARY))
        assertTrue(overridden.isMixed(StylusButton.SECONDARY))
        assertFalse(overridden.owns(StylusButton.PRIMARY))
    }

    @Test
    fun keyBridgeIsRequiredOnlyForOwnedKeyBoundButtons() {
        val bindings = StylusLearnedBindings(primaryKeyCode = 92, secondaryKeyCode = 93)
        val mappings = StylusButtonMappings(secondaryLong = StylusButtonAction.PEN)

        assertEquals(
            setOf(StylusButton.SECONDARY),
            requiredStylusKeyBridgeButtons(mappings, bindings)
        )
        assertTrue(
            requiredStylusKeyBridgeButtons(
                StylusButtonMappings(secondaryLong = StylusButtonAction.NONE),
                bindings
            ).contains(StylusButton.SECONDARY)
        )
        assertTrue(requiredStylusKeyBridgeButtons(StylusButtonMappings(), bindings).isEmpty())
    }

    @Test
    fun ownedButtonDoesNotReplayVendorDefaultForUnconfiguredGestures() {
        val mappings = StylusButtonMappings(secondaryLong = StylusButtonAction.PEN)

        assertEquals(
            null,
            mappings.customActionFor(StylusButton.SECONDARY, StylusPressType.SINGLE)
        )
        assertEquals(
            StylusButtonAction.PEN,
            mappings.customActionFor(StylusButton.SECONDARY, StylusPressType.LONG)
        )
    }

    @Test
    fun vendorPresetMatchingIsModelSpecific() {
        val focusPen = StylusDeviceIdentity(
            manufacturer = "Xiaomi",
            name = "Xiaomi Focus Pen",
            descriptor = "focus",
            vendorId = 1,
            productId = 2
        )
        val unknownRedmiPen = focusPen.copy(name = "Generic Pen", descriptor = "generic")

        assertEquals(
            "xiaomi_focus_pen",
            StylusVendorPresetCatalog.detect(focusPen)?.id
        )
        assertEquals(null, StylusVendorPresetCatalog.detect(unknownRedmiPen))
    }
}
