package com.pilotothegreat.deencompanion.alarms

import android.app.NotificationManager
import android.content.Context
import timber.log.Timber

/**
 * Optional silence while you pray, off by default.
 *
 * Two things make this safe rather than alarming. The restore alarm is scheduled at the same instant
 * as the mute, so a crash in between can never leave a phone silent for good. And it uses Do Not
 * Disturb rather than the ringer, so alarms still sound and the system's own indicator tells the
 * reader plainly what is going on — a phone silenced invisibly by an app is a phone people stop
 * trusting.
 */
object QuietDuringPrayer {

    /** Do Not Disturb can only be changed with the reader's explicit grant in system settings. */
    fun isAllowed(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true

    fun silence(context: Context) = apply(context, NotificationManager.INTERRUPTION_FILTER_PRIORITY)

    fun restore(context: Context) = apply(context, NotificationManager.INTERRUPTION_FILTER_ALL)

    private fun apply(context: Context, filter: Int) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!manager.isNotificationPolicyAccessGranted) return
        runCatching { manager.setInterruptionFilter(filter) }
            .onFailure { Timber.w(it, "Could not change Do Not Disturb") }
    }
}
