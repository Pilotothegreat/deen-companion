package com.pilotothegreat.deencompanion.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.pilotothegreat.deencompanion.ui.theme.LocalAccessibility

/**
 * The app's haptics, which can actually be turned off.
 *
 * Every tap counter, tab and aligned compass called `LocalHapticFeedback` straight through, and not
 * one of them consulted the setting that claims to govern them — so the switch in Simple mode was a
 * switch that did nothing. Going through here means the next one cannot forget either: there is no
 * other handle in the app to reach for.
 */
@Composable
fun rememberHaptics(): Haptics {
    val platform = LocalHapticFeedback.current
    val enabled = LocalAccessibility.current.haptics
    return remember(platform, enabled) { Haptics(platform, enabled) }
}

class Haptics internal constructor(
    private val platform: HapticFeedback,
    private val enabled: Boolean,
) {
    fun perform(type: HapticFeedbackType) {
        if (enabled) platform.performHapticFeedback(type)
    }

    /** One step of a count: the tasbih, a tab, an ayah. */
    fun tick() = perform(HapticFeedbackType.SegmentTick)

    /** Something finished: a round of dhikr, the compass coming to rest on the Qibla. */
    fun confirm() = perform(HapticFeedbackType.Confirm)

    /** A press that opens something. */
    fun click() = perform(HapticFeedbackType.ContextClick)
}
