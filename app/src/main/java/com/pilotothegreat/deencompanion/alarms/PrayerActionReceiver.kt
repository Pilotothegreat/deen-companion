package com.pilotothegreat.deencompanion.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.data.prayer.PrayerLogRepository
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * The buttons on a prayer notification.
 *
 * "Prayed" is the one that earns its place: it silences the adhan, cancels the iqama reminder that
 * would otherwise arrive for a prayer already prayed, and records the day so the streak means
 * something. Snooze exists because sometimes the answer is "in ten minutes", and an alert with no
 * answer to that is an alert people learn to swipe away.
 */
class PrayerActionReceiver : BroadcastReceiver(), KoinComponent {

    private val settings: SettingsRepository by inject()
    private val log: PrayerLogRepository by inject()
    private val scheduler: PrayerAlarmScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val prayer = Prayer.fromKey(intent.getStringExtra(EXTRA_PRAYER)) ?: return
        val action = intent.action ?: return
        launchAsync {
            val current = settings.current()
            val manager = NotificationManagerCompat.from(context)
            when (action) {
                ACTION_PRAYED -> {
                    AdhanService.stop(context)
                    manager.cancel(Notifications.notificationId(AlarmKind.ADHAN, prayer))
                    manager.cancel(Notifications.notificationId(AlarmKind.IQAMA, prayer))
                    cancelIqama(context, prayer)
                    log.record(prayer, LocalDate.now(current.zone), System.currentTimeMillis())
                }
                ACTION_SNOOZE -> {
                    AdhanService.stop(context)
                    manager.cancel(Notifications.notificationId(AlarmKind.ADHAN, prayer))
                    snooze(context, prayer)
                }
                ACTION_STOP -> {
                    AdhanService.stop(context)
                    manager.cancel(Notifications.notificationId(AlarmKind.ADHAN, prayer))
                }
            }
            scheduler.reschedule()
        }
    }

    /** The iqama alert is for a prayer not yet prayed; once it is, the reminder is noise. */
    private fun cancelIqama(context: Context, prayer: Prayer) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        for (day in 0..1) {
            val pending = PendingIntent.getBroadcast(
                context,
                AlarmRequestCodes.of(AlarmKind.IQAMA, day, prayer),
                PrayerAlarmReceiver.intent(context),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ) ?: continue
            manager.cancel(pending)
            pending.cancel()
        }
    }

    private fun snooze(context: Context, prayer: Prayer) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val at = ZonedDateTime.now().plusMinutes(SNOOZE_MINUTES).toInstant().toEpochMilli()
        val pending = PendingIntent.getBroadcast(
            context,
            AlarmRequestCodes.of(AlarmKind.ADHAN, SNOOZE_DAY, prayer),
            PrayerAlarmReceiver.intent(context, AlarmKind.ADHAN, prayer, at),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
    }

    companion object {
        const val SNOOZE_MINUTES = 10L

        /** Its own slot in the request-code space, so a snooze never overwrites a real alarm. */
        const val SNOOZE_DAY = 9

        const val ACTION_PRAYED = "com.pilotothegreat.deencompanion.action.PRAYED"
        const val ACTION_SNOOZE = "com.pilotothegreat.deencompanion.action.SNOOZE"
        const val ACTION_STOP = "com.pilotothegreat.deencompanion.action.STOP"
        private const val EXTRA_PRAYER = "prayer"

        fun intent(context: Context, action: String, prayer: Prayer): Intent =
            Intent(context, PrayerActionReceiver::class.java).setAction(action).putExtra(EXTRA_PRAYER, prayer.key)
    }
}
