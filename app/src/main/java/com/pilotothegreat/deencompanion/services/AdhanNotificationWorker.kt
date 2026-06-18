package com.pilotothegreat.deencompanion.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.util.PrayerTimeCalculator
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AdhanNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val repo: AppPreferenceRepo by inject()

    override suspend fun doWork(): Result {
        Timber.d("AdhanNotificationWorker: doWork triggered to batch queue daily Adhan alerts.")
        return try {
            AdhanAlarmManager.scheduleAllAdhanAlarms(applicationContext, repo)
            // Keep Iqama alerts updated as well
            IqamaAlarmManager.scheduleNextIqamaAlarm(applicationContext, repo)
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "AdhanNotificationWorker: failed to batch schedule Adhans")
            Result.retry()
        }
    }
}
