package com.pilotothegreat.deencompanion.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.time.ZonedDateTime

/**
 * Keeps adhan and iqama alarms for today and tomorrow in sync with settings. Every call cancels
 * and rebuilds the full set, so it is safe to call after any change, boot or alarm.
 */
class PrayerAlarmScheduler(private val context: Context, private val settings: SettingsRepository) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val lock = Mutex()

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager?.canScheduleExactAlarms() == true

    suspend fun reschedule() = lock.withLock {
        val manager = alarmManager ?: return@withLock
        cancelAll(manager)
        val current = settings.current()
        if (!current.notificationsEnabled) return@withLock

        val now = ZonedDateTime.now(current.zone)
        for (day in 0..1) {
            val schedule = DaySchedule.forDate(now.toLocalDate().plusDays(day.toLong()), current.prayerConfig)
            for (prayer in Prayer.obligatory) {
                if (prayer in current.mutedPrayers) continue
                val adhan = schedule.adhan.getValue(prayer)
                if (adhan.isAfter(now)) set(manager, AlarmKind.ADHAN, day, prayer, adhan)
                val iqama = schedule.iqama[prayer]
                if (iqama != null && iqama.isAfter(now) && iqama != adhan) set(manager, AlarmKind.IQAMA, day, prayer, iqama)
            }
        }
    }

    private fun set(manager: AlarmManager, kind: AlarmKind, day: Int, prayer: Prayer, at: ZonedDateTime) {
        val millis = at.toInstant().toEpochMilli()
        val intent = PrayerAlarmReceiver.intent(context, kind, prayer, millis)
        val pending = PendingIntent.getBroadcast(
            context, requestCode(kind, day, prayer), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            if (canScheduleExact()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
            }
        } catch (e: SecurityException) {
            Timber.w(e, "Exact alarm permission revoked; scheduling inexact %s %s", kind, prayer)
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }

    private fun cancelAll(manager: AlarmManager) {
        for (kind in AlarmKind.entries) for (day in 0..1) for (prayer in Prayer.entries) {
            val pending = PendingIntent.getBroadcast(
                context, requestCode(kind, day, prayer), PrayerAlarmReceiver.intent(context),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ) ?: continue
            manager.cancel(pending)
            pending.cancel()
        }
    }

    private fun requestCode(kind: AlarmKind, day: Int, prayer: Prayer) = 1000 + kind.ordinal * 100 + day * 10 + prayer.ordinal
}
