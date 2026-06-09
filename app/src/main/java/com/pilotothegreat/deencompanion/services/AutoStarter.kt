package com.pilotothegreat.deencompanion.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class AutoStarter : BroadcastReceiver(), KoinComponent {
    private val appPreferenceRepo: AppPreferenceRepo by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)

                    val workManager = WorkManager.getInstance(context)
                    val oneTimeRequest = OneTimeWorkRequestBuilder<AdhanNotificationWorker>().build()
                    workManager.enqueueUniqueWork(
                        "adhan_scheduler_one_time",
                        ExistingWorkPolicy.REPLACE,
                        oneTimeRequest
                    )

                    val periodicRequest = PeriodicWorkRequestBuilder<AdhanNotificationWorker>(
                        24, TimeUnit.HOURS
                    ).build()
                    workManager.enqueueUniquePeriodicWork(
                        "adhan_scheduler",
                        ExistingPeriodicWorkPolicy.KEEP,
                        periodicRequest
                    )
                } catch (e: Exception) {
                    Timber.e(e, "AutoStarter: Failed to restore scheduled alerts on boot")
                } finally {
                    try {
                        pendingResult.finish()
                    } catch (e: Exception) {
                        Timber.e(e, "AutoStarter: Error finishing pendingResult")
                    }
                }
            }
        }
    }
}

