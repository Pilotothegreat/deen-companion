package com.pilotothegreat.deencompanion.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.util.PrayerTimeCalculator
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object AdhanAlarmManager {

    suspend fun scheduleAllAdhanAlarms(context: Context, repo: AppPreferenceRepo) {
        val lat = repo.latitude.first()
        val lon = repo.longitude.first()
        val tzId = repo.timezoneId.first()
        val calcMethod = repo.calcMethod.first()
        val asrSchool = repo.asrSchool.first()

        val zoneId = ZoneId.systemDefault()
        val now = LocalDateTime.now(zoneId)
        val today = LocalDate.now(zoneId)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        for (date in listOf(today, today.plusDays(1))) {
            val zonedDateTime = date.atStartOfDay(zoneId)
            val offsetHours = zonedDateTime.offset.totalSeconds / 3600.0

            val times = PrayerTimeCalculator.calculate(
                date = date,
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

            for ((name, time) in prayers) {
                val prayerDateTime = LocalDateTime.of(date, time)
                if (prayerDateTime.isBefore(now)) {
                    // Already passed
                    continue
                }

                val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
                    putExtra("PRAYER_NAME", name)
                }
                
                val requestCode = "${name}_${date}".hashCode()
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
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
                    Timber.d("Batch scheduled Adhan alert for %s at %s on date %s", name, prayerDateTime, date)
                } catch (e: SecurityException) {
                    Timber.e(e, "SecurityException: cannot schedule exact alarm for Adhan %s on date %s", name, date)
                } catch (e: Exception) {
                    Timber.e(e, "Exception scheduling Adhan %s on date %s", name, date)
                }
            }
        }
    }
}
