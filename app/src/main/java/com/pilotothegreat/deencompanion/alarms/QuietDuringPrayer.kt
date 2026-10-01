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

    /**
     * Turns on Do Not Disturb only if it is off. When the reader already has it on, for a meeting or
     * for sleep, it is theirs and is left alone, and so is not ours to turn off afterwards either.
     */
    fun silence(context: Context) {
        val manager = manager(context) ?: return
        if (manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return
        if (set(manager, NotificationManager.INTERRUPTION_FILTER_PRIORITY)) markSilenced(context, true)
    }

    /**
     * Ends a silence this app began, and nothing else. If the reader changed Do Not Disturb in the
     * meantime, their choice stands. Safe to call at any time: without a silence of ours it does nothing.
     */
    fun restore(context: Context) {
        if (!wasSilenced(context)) return
        markSilenced(context, false)
        val manager = manager(context) ?: return
        if (manager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
            set(manager, NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }

    private fun manager(context: Context): NotificationManager? =
        context.getSystemService(NotificationManager::class.java)?.takeIf { it.isNotificationPolicyAccessGranted }

    private fun set(manager: NotificationManager, filter: Int): Boolean =
        runCatching { manager.setInterruptionFilter(filter) }
            .onFailure { Timber.w(it, "Could not change Do Not Disturb") }
            .isSuccess

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun wasSilenced(context: Context) = prefs(context).getBoolean(KEY_SILENCED, false)

    private fun markSilenced(context: Context, silenced: Boolean) =
        prefs(context).edit().putBoolean(KEY_SILENCED, silenced).commit()

    private const val PREFS = "quiet_during_prayer"
    private const val KEY_SILENCED = "silenced_by_bilal"
}
