package com.pilotothegreat.deencompanion.data.analytics

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pilotothegreat.deencompanion.BuildConfig
import com.pilotothegreat.deencompanion.data.net.Http
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Sends the day's tally to the collector this build was given, if there is one and if the reader
 * agreed to it.
 *
 * Once a day, on unmetered network, and never on a low battery: a usage report is the least urgent
 * thing this app does and must not cost anyone a megabyte of mobile data. A failed send is not
 * retried — tomorrow's report contains the same window anyway.
 */
class UsageUploadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val analytics: AnalyticsRepository by inject()

    override suspend fun doWork(): Result {
        if (!analytics.hasCollector) return Result.success()
        return runCatching {
            val report = analytics.report()
            Http.postJson(BuildConfig.ANALYTICS_ENDPOINT, report.toJson().toString())
            Result.success()
        }.getOrElse {
            Timber.w(it, "Usage report not sent")
            Result.success()
        }
    }

    companion object {
        private const val WORK_NAME = "usage_upload"

        /** Started when counting is turned on, cancelled the moment it is turned off. */
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<UsageUploadWorker>(1, TimeUnit.DAYS)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.UNMETERED)
                            .setRequiresBatteryNotLow(true)
                            .build(),
                    )
                    .build(),
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
