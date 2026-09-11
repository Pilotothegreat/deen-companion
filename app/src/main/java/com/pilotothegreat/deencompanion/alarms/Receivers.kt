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
import androidx.work.WorkerParameters
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
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
            val onTime = System.currentTimeMillis() - scheduledAt < STALE_AFTER_MILLIS
            if (prayer != null && kind != null && onTime && current.notificationsEnabled && prayer !in current.mutedPrayers) {
                Notifications.showPrayer(context, current.appLanguage, kind, prayer)
            }
            scheduler.reschedule()
            WidgetUpdater.updateAll(context)
        }
    }

    companion object {
        private const val ACTION = "com.pilotothegreat.deencompanion.action.PRAYER_ALARM"
        private const val EXTRA_KIND = "kind"
        private const val EXTRA_PRAYER = "prayer"
        private const val EXTRA_AT = "at"
        private const val STALE_AFTER_MILLIS = 30 * 60 * 1000L

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

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        launchAsync {
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
