package com.pilotothegreat.deencompanion.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * A copy of everything worth keeping, written quietly once a week.
 *
 * Exporting by hand already existed and almost nobody does it, which means the people who lose a
 * khatma halfway through are exactly the people who were never going to press the button. This costs
 * a few kilobytes and no network — it stays in the app's own files directory and leaves the device
 * only if the reader chooses to share it — and it turns "I reinstalled and lost everything" into a
 * row on the restore sheet.
 */
class AutoBackupWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {

    private val backup: BackupRepository by inject()

    override suspend fun doWork(): Result {
        return runCatching {
            AutoBackups.write(applicationContext, backup.export())
            Result.success()
        }.getOrElse {
            Timber.w(it, "Automatic backup failed")
            // A backup that could not be written is worth one retry and no more; the next weekly
            // run is never far away and a retry storm for a nicety helps nobody.
            Result.success()
        }
    }

    companion object {
        private const val WORK_NAME = "auto_backup"

        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiresBatteryNotLow(true)
                            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                            .build(),
                    )
                    .build(),
            )
        }
    }
}

/** The files themselves, kept apart from the worker so the rules can be tested without Android. */
object AutoBackups {

    /** Three is a month of history: enough to go back past a mistake, small enough to ignore. */
    const val KEEP = 3
    const val DIRECTORY = "backups"
    private const val PREFIX = "bilal-auto-"
    private const val SUFFIX = ".json"

    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")

    fun directory(context: Context): File = File(context.filesDir, DIRECTORY)

    fun nameFor(atMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        PREFIX + stamp.format(Instant.ofEpochMilli(atMillis).atZone(zone)) + SUFFIX

    /** Newest first, which is the order the restore sheet offers them in. */
    fun list(context: Context): List<File> = directory(context)
        .listFiles { file -> file.isFile && file.name.startsWith(PREFIX) && file.name.endsWith(SUFFIX) }
        ?.sortedByDescending { it.name }
        .orEmpty()

    fun write(context: Context, json: String, atMillis: Long = System.currentTimeMillis()): File {
        val directory = directory(context).apply { mkdirs() }
        val file = File(directory, nameFor(atMillis))
        file.writeText(json)
        prune(list(context)).forEach { it.delete() }
        return file
    }

    /** What to delete: everything past the most recent [keep], oldest first out. */
    fun prune(existing: List<File>, keep: Int = KEEP): List<File> =
        if (existing.size <= keep) emptyList() else existing.drop(keep)
}
