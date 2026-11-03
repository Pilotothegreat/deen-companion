package com.pilotothegreat.deencompanion.ui

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.pilotothegreat.deencompanion.ui.common.Haptics
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The haptics switch has to actually stop the buzzing.
 *
 * Until 2.0 it did not: the setting was resolved at the theme and every call site went straight to
 * the platform. The failure was invisible in review — the code all looked like it was asking — so
 * it is checked here instead.
 */
class HapticsTest {

    private class Recorder : HapticFeedback {
        val performed = mutableListOf<HapticFeedbackType>()
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performed += hapticFeedbackType
        }
    }

    @Test fun everyKindIsFeltWhenHapticsAreOn() {
        val recorder = Recorder()
        val haptics = Haptics(recorder, enabled = true)
        haptics.tick()
        haptics.confirm()
        haptics.click()
        haptics.toggle(true)
        haptics.toggle(false)
        haptics.reject()
        assertEquals(
            listOf(
                HapticFeedbackType.SegmentTick, HapticFeedbackType.Confirm, HapticFeedbackType.ContextClick,
                HapticFeedbackType.ToggleOn, HapticFeedbackType.ToggleOff, HapticFeedbackType.Reject,
            ),
            recorder.performed,
        )
    }

    @Test fun nothingIsFeltWhenTheyAreOff() {
        val recorder = Recorder()
        val haptics = Haptics(recorder, enabled = false)
        haptics.tick()
        haptics.confirm()
        haptics.click()
        haptics.toggle(true)
        haptics.reject()
        haptics.perform(HapticFeedbackType.LongPress)
        assertEquals(emptyList<HapticFeedbackType>(), recorder.performed)
    }
}
