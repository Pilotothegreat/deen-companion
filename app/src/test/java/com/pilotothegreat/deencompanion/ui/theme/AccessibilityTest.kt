package com.pilotothegreat.deencompanion.ui.theme

import com.pilotothegreat.deencompanion.data.settings.AccessibilitySettings
import com.pilotothegreat.deencompanion.data.settings.ContrastMode
import com.pilotothegreat.deencompanion.data.settings.ReduceMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reduced motion used to be read once into an un-keyed `remember`, so the system setting did
 * nothing until the app restarted, and only two composables asked at all. It is now one resolved
 * value, and these pin down what it resolves to.
 */
class AccessibilityTest {

    @Test fun theSystemSettingIsFollowedByDefault() {
        assertTrue(Accessibility.from(AccessibilitySettings(), systemReducesMotion = true).reduceMotion)
        assertFalse(Accessibility.from(AccessibilitySettings(), systemReducesMotion = false).reduceMotion)
    }

    @Test fun anExplicitChoiceOverridesTheSystem() {
        val forcedOn = AccessibilitySettings(reduceMotion = ReduceMotion.ON)
        val forcedOff = AccessibilitySettings(reduceMotion = ReduceMotion.OFF)
        assertTrue(Accessibility.from(forcedOn, systemReducesMotion = false).reduceMotion)
        assertFalse(Accessibility.from(forcedOff, systemReducesMotion = true).reduceMotion)
    }

    @Test fun simpleModeIsOneSwitchThatMeansSixThings() {
        val simple = Accessibility.from(AccessibilitySettings(simpleMode = true), systemReducesMotion = false)
        assertTrue("no decorative motion", simple.reduceMotion)
        assertTrue("larger targets", simple.largeTouchTargets)
        assertTrue("stronger contrast", simple.highContrast)
        assertTrue("and bigger text", simple.textScale >= Accessibility.SIMPLE_TEXT_SCALE)
    }

    @Test fun simpleModeNeverShrinksWhatTheReaderAlreadyChose() {
        val larger = AccessibilitySettings(simpleMode = true, textScale = 1.5f)
        assertEquals(1.5f, Accessibility.from(larger, systemReducesMotion = false).textScale, 0.001f)
    }

    @Test fun anExplicitContrastChoiceSurvivesSimpleMode() {
        val chosen = AccessibilitySettings(simpleMode = true, contrast = ContrastMode.MEDIUM)
        assertFalse("the reader asked for medium, not high", Accessibility.from(chosen, false).highContrast)
    }

    @Test fun nothingIsTurnedOnForSomeoneWhoAskedForNothing() {
        val plain = Accessibility.from(AccessibilitySettings(), systemReducesMotion = false)
        assertFalse(plain.simpleMode)
        assertFalse(plain.largeTouchTargets)
        assertFalse(plain.highContrast)
        assertEquals(1f, plain.textScale, 0f)
        assertTrue("haptics stay on, as they were", plain.haptics)
    }
}
