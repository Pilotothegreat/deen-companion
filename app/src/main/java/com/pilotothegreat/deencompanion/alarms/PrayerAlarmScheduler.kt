package com.pilotothegreat.deencompanion.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.prayer.PrayerSchedule
import com.pilotothegreat.deencompanion.core.quiet.QuietTimes
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
        if (!current.notificationsEnabled && !current.athkarReminders) return@withLock

        val now = ZonedDateTime.now(current.zone)
        for (day in 0..1) {
            val schedule = DaySchedule.forDate(now.toLocalDate().plusDays(day.toLong()), current.prayerConfig)
            if (current.notificationsEnabled) {
                val sounds = current.sounds
                for (prayer in Prayer.obligatory) {
                    if (prayer in current.mutedPrayers) continue
                    val adhan = schedule.adhan.getValue(prayer)
                    if (sounds.preReminderMinutes > 0 && prayer in sounds.preReminderPrayers) {
                        val early = adhan.minusMinutes(sounds.preReminderMinutes.toLong())
                        if (early.isAfter(now)) set(manager, AlarmKind.PRE_PRAYER, day, prayer, early)
                    }
                    if (adhan.isAfter(now)) set(manager, AlarmKind.ADHAN, day, prayer, adhan)
                    // The iqama is a local mosque's, so it means nothing in another city. The adhan
                    // still sounds: the prayer time is the prayer time wherever you are.
                    val iqama = schedule.iqama[prayer].takeUnless { current.smart.isTravelling }
                    if (iqama != null && iqama.isAfter(now) && iqama != adhan) {
                        set(manager, AlarmKind.IQAMA, day, prayer, iqama)
                        // A handful of nudges to advance the progress bar between the two. The
                        // countdown beside it is a Chronometer and wakes nothing.
                        val checkpoints = PrayerWindow.checkpoints(
                            adhanAt = adhan.toInstant().toEpochMilli(),
                            iqamaAt = iqama.toInstant().toEpochMilli(),
                        )
                        checkpoints.forEachIndexed { index, at ->
                            val kind = AlarmKind.progressCheckpoints[index]
                            val time = java.time.Instant.ofEpochMilli(at).atZone(current.zone)
                            if (time.isAfter(now)) set(manager, kind, day, prayer, time)
                        }
                    }

                    // The restore is scheduled in the same breath as the mute, so a crash between
                    // the two can never leave someone's phone silent for good. On Friday the window
                    // covers the khutbah too, which begins at the adhan rather than the iqama.
                    val quiet = QuietTimes.silenceWindow(prayer, adhan, iqama, sounds.silenceMinutes)
                    if (quiet != null && quiet.start.isAfter(now)) {
                        set(manager, AlarmKind.SILENCE_START, day, prayer, quiet.start)
                        set(manager, AlarmKind.SILENCE_END, day, prayer, quiet.endInclusive)
                    }
                }
            }
            if (current.athkarReminders) {
                athkarTime(schedule, Prayer.FAJR).takeIf { it.isAfter(now) }
                    ?.let { set(manager, AlarmKind.ATHKAR_MORNING, day, Prayer.FAJR, it) }
                athkarTime(schedule, Prayer.ASR).takeIf { it.isAfter(now) }
                    ?.let { set(manager, AlarmKind.ATHKAR_EVENING, day, Prayer.ASR, it) }
            }
        }
    }

    /** Twenty minutes after the prayer has been prayed: the iqama, or twenty minutes after the adhan without one. */
    private fun athkarTime(schedule: PrayerSchedule, prayer: Prayer): ZonedDateTime =
        (schedule.iqama[prayer] ?: schedule.adhan.getValue(prayer).plusMinutes(20)).plusMinutes(20)

    /** Reminders don't need to be to-the-minute, so they use inexact alarms and spare the battery. */
    private fun set(manager: AlarmManager, kind: AlarmKind, day: Int, prayer: Prayer, at: ZonedDateTime) {
        val millis = at.toInstant().toEpochMilli()
        val intent = PrayerAlarmReceiver.intent(context, kind, prayer, millis)
        val pending = PendingIntent.getBroadcast(
            context, requestCode(kind, day, prayer), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            if (canScheduleExact() && kind.needsExactTime) {
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

    /**
     * A distinct PendingIntent id per (kind, day, prayer). Two alarms sharing an id silently
     * overwrite each other, and the symptom — one prayer alert quietly missing — is nearly
     * impossible to spot by hand, so AlarmRequestCodes is exhaustively tested instead.
     */
    private fun requestCode(kind: AlarmKind, day: Int, prayer: Prayer) =
        AlarmRequestCodes.of(kind, day, prayer)
}

/**
 * Ids for the alarms. Kept apart from the scheduler so every combination can be enumerated in a
 * test: this release takes the alarm kinds from four to seven, and a clash would show up only as an
 * alert that never arrived.
 */
object AlarmRequestCodes {
    const val BASE = 1000
    const val DAYS = 10
    val PRAYERS = Prayer.entries.size

    fun of(kind: AlarmKind, day: Int, prayer: Prayer): Int =
        BASE + ((kind.ordinal * DAYS) + day) * PRAYERS + prayer.ordinal
}
