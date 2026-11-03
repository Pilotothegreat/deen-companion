package com.pilotothegreat.deencompanion.ui.theme

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.pilotothegreat.deencompanion.data.settings.AccessibilitySettings
import com.pilotothegreat.deencompanion.data.settings.ContrastMode
import com.pilotothegreat.deencompanion.data.settings.ReduceMotion

/**
 * The resolved accessibility state for the whole app.
 *
 * It is resolved once, at the theme, because the alternative is what this app had: each composable
 * asking the system for itself, in an un-keyed `remember` that read the setting a single time and
 * then ignored it forever. Turning animations off in system settings did nothing until the app was
 * restarted, and only two composables ever asked at all.
 */
data class Accessibility(
    val reduceMotion: Boolean = false,
    val simpleMode: Boolean = false,
    val largeTouchTargets: Boolean = false,
    val haptics: Boolean = true,
    val highContrast: Boolean = false,
    val textScale: Float = 1f,
) {
    /** Simple mode implies larger targets and no decorative motion, whatever the individual toggles say. */
    companion object {
        fun from(settings: AccessibilitySettings, systemReducesMotion: Boolean): Accessibility {
            val simple = settings.simpleMode
            return Accessibility(
                reduceMotion = simple || when (settings.reduceMotion) {
                    ReduceMotion.ON -> true
                    ReduceMotion.OFF -> false
                    ReduceMotion.SYSTEM -> systemReducesMotion
                },
                simpleMode = simple,
                largeTouchTargets = simple || settings.largeTouchTargets,
                haptics = settings.haptics,
                highContrast = settings.contrast == ContrastMode.HIGH || (simple && settings.contrast == ContrastMode.SYSTEM),
                textScale = if (simple) maxOf(settings.textScale, SIMPLE_TEXT_SCALE) else settings.textScale,
            )
        }

        /** Simple mode's floor: large enough to read at arm's length without being a separate design. */
        const val SIMPLE_TEXT_SCALE = 1.3f
    }
}

val LocalAccessibility = staticCompositionLocalOf { Accessibility() }

/**
 * Whether the system has animations turned off, kept current.
 *
 * Watched rather than read once: someone who turns animations off to make an app usable should not
 * have to restart it to find out whether that worked.
 */
@Composable
fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    var reduced by remember {
        mutableStateOf(Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f)
    }
    DisposableEffect(context) {
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
            }
        }
        runCatching { context.contentResolver.registerContentObserver(uri, false, observer) }
        onDispose { runCatching { context.contentResolver.unregisterContentObserver(observer) } }
    }
    return reduced
}
