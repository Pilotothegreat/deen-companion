package com.pilotothegreat.deencompanion.alarms

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import com.pilotothegreat.deencompanion.data.quran.KhatmaRepository
import com.pilotothegreat.deencompanion.core.quran.KhatmaPlan
import androidx.work.WorkerParameters
import com.pilotothegreat.deencompanion.core.calendar.HijriCalendar
import com.pilotothegreat.deencompanion.core.calendar.HijriClock
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
import com.pilotothegreat.deencompanion.core.quiet.QuietTimes
import com.pilotothegreat.deencompanion.data.settings.AppSettings
import java.time.ZonedDateTime
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.data.location.LocationRepository
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.data.settings.SoundSettings
import com.pilotothegreat.deencompanion.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/** Runs [block] off the main thread while keeping the broadcast alive until it finishes. */
internal fun BroadcastReceiver.launchAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            block()
        } catch (e: Exception) {
            Timber.e(e, "Background work failed in %s", this@launchAsync::class.simpleName)
        } finally {
            pending.finish()
        }
    }
}

class PrayerAlarmReceiver : BroadcastReceiver(), KoinComponent {
    private val settings: SettingsRepository by inject()
    private val scheduler: PrayerAlarmScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val prayer = Prayer.fromKey(intent.getStringExtra(EXTRA_PRAYER))
        val kind = intent.getStringExtra(EXTRA_KIND)?.let { runCatching { AlarmKind.valueOf(it) }.getOrNull() }
        val scheduledAt = intent.getLongExtra(EXTRA_AT, 0L)
        launchAsync {
            val current = settings.current()
            // Alarms delivered long after their time (e.g. held while the device was off) are skipped.
            // A progress nudge can be a few minutes late without harm; an adhan cannot.
            val window = if (kind != null && kind in AlarmKind.progressCheckpoints) PROGRESS_STALE_MILLIS else STALE_AFTER_MILLIS
            val onTime = System.currentTimeMillis() - scheduledAt < window
            if (prayer != null && kind != null && onTime) {
                when {
                    kind.isAthkar -> if (current.athkarReminders) Notifications.showAthkar(context, current.appLanguage, kind)
                    kind == AlarmKind.SILENCE_START -> QuietDuringPrayer.silence(context)
                    kind == AlarmKind.SILENCE_END -> QuietDuringPrayer.restore(context)
                    current.notificationsEnabled && prayer !in current.mutedPrayers ->
                        onPrayerAlarm(context, current, kind, prayer)
                }
            }
            scheduler.reschedule()
            WidgetUpdater.updateAll(context)
        }
    }

    /**
     * The adhan, its countdown and the iqama, as one notification that changes rather than three
     * that pile up.
     */
    private fun onPrayerAlarm(context: Context, current: AppSettings, kind: AlarmKind, prayer: Prayer) {
        if (kind == AlarmKind.PRE_PRAYER) {
            Notifications.showPreReminder(context, current.appLanguage, prayer)
            return
        }
        val now = ZonedDateTime.now(current.zone)
        val schedule = DaySchedule.forDate(now.toLocalDate(), current.prayerConfig)
        val adhanAt = schedule.adhan[prayer]?.toInstant()?.toEpochMilli() ?: return
        val iqamaAt = schedule.iqama[prayer]
            ?.takeUnless { current.smart.isTravelling }
            ?.toInstant()?.toEpochMilli()
            ?.takeIf { it > adhanAt }

        val sound = current.sounds.adhanFor(prayer)
        val playing = kind == AlarmKind.ADHAN && sound != SoundSettings.SILENT
        if (playing) {
            // The service owns the notification while it owns the audio, so the two cannot diverge.
            AdhanService.start(
                context = context,
                prayer = prayer,
                languageTag = current.appLanguage,
                sound = sound.takeUnless { it == SoundSettings.SYSTEM_SOUND },
                iqamaAt = iqamaAt ?: 0L,
            )
            return
        }
        val stage = PrayerWindow.stageAt(
            now = System.currentTimeMillis(),
            adhanAt = adhanAt,
            iqamaAt = iqamaAt,
            adhanPlaying = false,
        )
        Notifications.showPrayerWindow(context, current.appLanguage, prayer, stage, iqamaAt)
    }

    companion object {
        private const val ACTION = "com.pilotothegreat.deencompanion.action.PRAYER_ALARM"
        private const val EXTRA_KIND = "kind"
        private const val EXTRA_PRAYER = "prayer"
        private const val EXTRA_AT = "at"
        private const val STALE_AFTER_MILLIS = 30 * 60 * 1000L
        private const val PROGRESS_STALE_MILLIS = 5 * 60 * 1000L

        /** Extras don't affect PendingIntent identity, so the bare intent also matches for cancelling. */
        internal fun intent(context: Context, kind: AlarmKind? = null, prayer: Prayer? = null, at: Long = 0L): Intent =
            Intent(context, PrayerAlarmReceiver::class.java).setAction(ACTION).apply {
                kind?.let { putExtra(EXTRA_KIND, it.name) }
                prayer?.let { putExtra(EXTRA_PRAYER, it.key) }
                putExtra(EXTRA_AT, at)
            }
    }
}

/** Rebuilds alarms and widgets after reboot, clock or timezone changes, updates and permission grants. */
class SystemEventsReceiver : BroadcastReceiver(), KoinComponent {
    private val scheduler: PrayerAlarmScheduler by inject()
    private val location: LocationRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        launchAsync {
            if (intent.action == Intent.ACTION_TIMEZONE_CHANGED) location.onTimezoneChanged()
            scheduler.reschedule()
            WidgetUpdater.updateAll(context)
        }
    }

    private companion object {
        // The exact-alarm action is only broadcast on API 31+; on older versions it simply never arrives.
        @SuppressLint("InlinedApi")
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}

/** Safety net that rolls the two-day alarm window forward even if no alarm fires. */
class RescheduleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {
    private val scheduler: PrayerAlarmScheduler by inject()

    override suspend fun doWork(): Result {
        scheduler.reschedule()
        WidgetUpdater.updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "prayer_alarms"
        private val LEGACY_WORK = listOf("adhan_scheduler", "adhan_scheduler_one_time")

        fun enqueue(context: Context) {
            val workManager = WorkManager.getInstance(context)
            LEGACY_WORK.forEach(workManager::cancelUniqueWork)
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<RescheduleWorker>(12, TimeUnit.HOURS).build(),
            )
        }
    }
}

/**
 * Checks the khatma once a day. It is a worker rather than an exact alarm because being nudged
 * about a reading plan a few minutes late costs nothing, and an exact alarm for it would compete
 * with the ones that must arrive on time.
 */
class KhatmaReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {
    private val khatma: KhatmaRepository by inject()
    private val settings: SettingsRepository by inject()

    override suspend fun doWork(): Result {
        val plan = khatma.plan.first() ?: return Result.success()
        val today = LocalDate.now()
        if (!KhatmaPlan.needsReminder(plan, today)) return Result.success()
        val current = settings.current()
        // On an odd night of the last ten, a nudge about a reading plan can wait until morning. The
        // adhan still sounds and the athkar still arrive; this is the one notice that can afford to.
        if (holdsTonight(current)) return Result.success()
        val progress = KhatmaPlan.progress(plan, today)
        Notifications.showKhatma(
            context = applicationContext,
            languageTag = current.appLanguage,
            pagesDue = progress.pagesDueToday,
            page = progress.currentPage + 1,
        )
        return Result.success()
    }

    private fun holdsTonight(current: AppSettings): Boolean {
        val now = ZonedDateTime.now(current.zone)
        val maghrib = DaySchedule.forDate(now.toLocalDate(), current.prayerConfig).adhan[Prayer.MAGHRIB]
        val hijri = HijriClock.dateAt(now, maghrib, current.hijriAdjustment, current.smart.hijriDayStartsAtMaghrib)
            ?: return false
        return QuietTimes.holdsGentleNotices(HijriCalendar.month(hijri), HijriCalendar.day(hijri))
    }

    companion object {
        private const val WORK_NAME = "khatma_reminder"

        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<KhatmaReminderWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(hoursUntilEvening(), TimeUnit.HOURS)
                    .build(),
            )
        }

        /** Aims for the evening, when there is still time to read but the day is mostly spent. */
        private fun hoursUntilEvening(): Long {
            val hour = java.time.LocalTime.now().hour
            return ((19 - hour) + 24) % 24L
        }
    }
}
