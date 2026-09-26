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
 * widget and nothing else never hears about a fix at all. Play installs are left alone — Play updates
 * itself, and a second notification for the same thing is noise.
 */
class UpdateCheckWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val updates: UpdateChecker by inject()
    private val settings: SettingsRepository by inject()

    override suspend fun doWork(): Result {
        if (updates.isPlayInstall) return Result.success()
        return runCatching {
            updates.check(force = true)
            val available = updates.state.value as? UpdateChecker.State.Available ?: return Result.success()
            val version = available.version ?: return Result.success()
            // Once per version: the notification is a message, not a reminder that repeats daily.
            if (settings.notifiedUpdateVersion() == version) return Result.success()
            settings.setNotifiedUpdateVersion(version)
            Notifications.showUpdate(applicationContext, settings.current().appLanguage, version)
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
