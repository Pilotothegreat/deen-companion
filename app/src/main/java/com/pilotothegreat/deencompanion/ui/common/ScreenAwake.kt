package com.pilotothegreat.deencompanion.ui.common

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Holds the screen on while [enabled], and lets go the moment it is not.
 *
 * Reading a page of the mushaf takes longer than most screen timeouts, and being made to tap the
 * glass every thirty seconds to finish an ayah is the kind of small indignity that makes people
 * read somewhere else. The flag is cleared on dispose as well as when the condition turns false, so
 * leaving the screen — by any route, including the back gesture — always gives the timeout back.
 */
@Composable
fun KeepScreenOn(enabled: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity, enabled) {
        if (enabled) {
            activity.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
