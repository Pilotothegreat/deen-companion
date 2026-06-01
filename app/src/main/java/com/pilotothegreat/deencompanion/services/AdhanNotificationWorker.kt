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
            batchScheduleAdhans(applicationContext)
            // Keep Iqama alerts updated as well
            IqamaAlarmManager.scheduleNextIqamaAlarm(applicationContext, repo)
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "AdhanNotificationWorker: failed to batch schedule Adhans")
            Result.retry()
        }
    }

    private suspend fun batchScheduleAdhans(context: Context) {
        val lat = repo.latitude.first()
        val lon = repo.longitude.first()
        val tzId = repo.timezoneId.first()
        val calcMethod = repo.calcMethod.first()
        val asrSchool = repo.asrSchool.first()

        val zoneId = try { ZoneId.of(tzId) } catch (e: Exception) { ZoneId.systemDefault() }
        val now = LocalDateTime.now(zoneId)
        val today = LocalDate.now(zoneId)

        // Calculate today's prayer times
        val zonedDateTime = today.atStartOfDay(zoneId)
        val offsetHours = zonedDateTime.offset.totalSeconds / 3600.0

        val times = PrayerTimeCalculator.calculate(
            date = today,
            latitude = lat,
            longitude = lon,
            timezoneOffsetHours = offsetHours,
            method = calcMethod,
            asrSchool = asrSchool
        )

        val prayers = listOf(
            Pair("Fajr", times.fajr),
            Pair("Dhuhr", times.dhuhr),
            Pair("Asr", times.asr),
            Pair("Maghrib", times.maghrib),
            Pair("Isha", times.isha)
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        for ((name, time) in prayers) {
            val prayerDateTime = LocalDateTime.of(today, time)
            if (prayerDateTime.isBefore(now)) {
                // Already passed for today
                continue
            }

            val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
                putExtra("PRAYER_NAME", name)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                name.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val epochMillis = prayerDateTime.atZone(zoneId).toInstant().toEpochMilli()

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            epochMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            epochMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        epochMillis,
                        pendingIntent
                    )
                }
                Timber.d("Batch scheduled Adhan alert for %s at %s", name, prayerDateTime)
            } catch (e: SecurityException) {
                Timber.e(e, "SecurityException: cannot schedule exact alarm for Adhan %s", name)
            } catch (e: Exception) {
                Timber.e(e, "Exception scheduling Adhan %s", name)
            }
        }
    }
}
