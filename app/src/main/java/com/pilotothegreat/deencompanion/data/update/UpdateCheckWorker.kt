package com.pilotothegreat.deencompanion.data.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pilotothegreat.deencompanion.alarms.Notifications
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Looks for a new release once a day whether or not the app is opened, and says so once.
 *
 * This is the part that actually reaches people. Checking only while someone is in the app finds the
 * update for the people who were already coming back; a sideloaded copy on a phone that opens the
 * widget and nothing else never hears about a fix at all. Play installs are told too: Play's own
 * auto-update is often off or waits for Wi-Fi and a charger, and the app's notification opens the
 * in-place update rather than the store.
 */
class UpdateCheckWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val updates: UpdateChecker by inject()
    private val settings: SettingsRepository by inject()

    override suspend fun doWork(): Result {
        return runCatching {
            updates.check(force = true)
            val available = updates.state.value as? UpdateChecker.State.Available ?: return Result.success()
            // Play reports a version code rather than a name; either identifies the release.
            val release = available.version ?: updates.playVersionCode.takeIf { it > 0 }?.let { "play-$it" } ?: return Result.success()
            // Once per version: the notification is a message, not a reminder that repeats daily.
            if (settings.notifiedUpdateVersion() == release) return Result.success()
            settings.setNotifiedUpdateVersion(release)
            Notifications.showUpdate(applicationContext, settings.current().appLanguage, available.version)
            Result.success()
        }.getOrElse {
            Timber.w(it, "Background update check failed")
            Result.success()
        }
    }

    companion object {
        private const val WORK_NAME = "update_check"

        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .setRequiresBatteryNotLow(true)
                            .build(),
                    )
                    .build(),
            )
        }
    }
}
