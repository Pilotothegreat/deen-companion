// FIXED v1.4.0: Schedule ALL 5 Iqama alarms per day (today + tomorrow) instead of only the next one.
// This prevents missed Iqamas when the process dies between prayers.
package com.pilotothegreat.deencompanion.services

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.util.PrayerTimeCalculator
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object IqamaAlarmManager {

    private fun parseFixedTime(timeStr: String): java.time.LocalTime? {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return java.time.LocalTime.of(hour, minute)
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "prayer_times",
                "Prayer Times",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Adhan and Iqama time notifications"
                enableVibration(true)
                setSound(null, null)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    /**
     * Schedules exact alarms for ALL 5 Iqama times for today AND tomorrow.
     * Past alarms are silently skipped. Future alarms are (re)set atomically.
     * Call this on: app boot, after settings change, and after each alarm fires.
     */
    suspend fun scheduleAllIqamaAlarms(context: Context, appPreferenceRepo: AppPreferenceRepo) {
        val lat = appPreferenceRepo.latitude.first()
        val lon = appPreferenceRepo.longitude.first()
        val tzId = appPreferenceRepo.timezoneId.first()
        val calcMethod = appPreferenceRepo.calcMethod.first()
        val asrSchool = appPreferenceRepo.asrSchool.first()

        val fajrOffset    = appPreferenceRepo.fajrIqamaOffset.first()
        val dhuhrOffset   = appPreferenceRepo.dhuhrIqamaOffset.first()
        val asrOffset     = appPreferenceRepo.asrIqamaOffset.first()
        val maghribOffset = appPreferenceRepo.maghribIqamaOffset.first()
        val ishaOffset    = appPreferenceRepo.ishaIqamaOffset.first()

        val fajrIsFixed    = appPreferenceRepo.fajrIqamaIsFixed.first()
        val dhuhrIsFixed   = appPreferenceRepo.dhuhrIqamaIsFixed.first()
        val asrIsFixed     = appPreferenceRepo.asrIqamaIsFixed.first()
        val maghribIsFixed = appPreferenceRepo.maghribIqamaIsFixed.first()
        val ishaIsFixed    = appPreferenceRepo.ishaIqamaIsFixed.first()

        val fajrFixedTimeVal    = appPreferenceRepo.fajrIqamaTime.first()
        val dhuhrFixedTimeVal   = appPreferenceRepo.dhuhrIqamaTime.first()
        val asrFixedTimeVal     = appPreferenceRepo.asrIqamaTime.first()
        val maghribFixedTimeVal = appPreferenceRepo.maghribIqamaTime.first()
        val ishaFixedTimeVal    = appPreferenceRepo.ishaIqamaTime.first()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val zoneId = try { ZoneId.of(tzId) } catch (e: Exception) { ZoneId.systemDefault() }
        val now = LocalDateTime.now(zoneId)

        // Schedule today (dayOffset=0) and tomorrow (dayOffset=1)
        for (dayOffset in 0..1) {
            val date = LocalDate.now(zoneId).plusDays(dayOffset.toLong())
            val zonedDate = date.atStartOfDay(zoneId)
            val offsetHours = zonedDate.offset.totalSeconds / 3600.0

            val times = PrayerTimeCalculator.calculate(
                date = date,
                latitude = lat,
                longitude = lon,
                timezoneOffsetHours = offsetHours,
                method = calcMethod,
                asrSchool = asrSchool
            )

            val fajrTime    = if (fajrIsFixed)    parseFixedTime(fajrFixedTimeVal)    ?: times.fajr.plusMinutes(fajrOffset.toLong())    else times.fajr.plusMinutes(fajrOffset.toLong())
            val dhuhrTime   = if (dhuhrIsFixed)   parseFixedTime(dhuhrFixedTimeVal)   ?: times.dhuhr.plusMinutes(dhuhrOffset.toLong())   else times.dhuhr.plusMinutes(dhuhrOffset.toLong())
            val asrTime     = if (asrIsFixed)     parseFixedTime(asrFixedTimeVal)     ?: times.asr.plusMinutes(asrOffset.toLong())       else times.asr.plusMinutes(asrOffset.toLong())
            val maghribTime = if (maghribIsFixed) parseFixedTime(maghribFixedTimeVal) ?: times.maghrib.plusMinutes(maghribOffset.toLong()) else times.maghrib.plusMinutes(maghribOffset.toLong())
            val ishaTime    = if (ishaIsFixed)    parseFixedTime(ishaFixedTimeVal)    ?: times.isha.plusMinutes(ishaOffset.toLong())     else times.isha.plusMinutes(ishaOffset.toLong())

            // Unique request codes: prayer 1001-1005 for today, 1011-1015 for tomorrow
            val prayers = listOf(
                Triple("Fajr",    fajrTime,    1001 + dayOffset * 10),
                Triple("Dhuhr",   dhuhrTime,   1002 + dayOffset * 10),
                Triple("Asr",     asrTime,     1003 + dayOffset * 10),
                Triple("Maghrib", maghribTime, 1004 + dayOffset * 10),
                Triple("Isha",    ishaTime,    1005 + dayOffset * 10)
            )

            for ((name, localTime, requestCode) in prayers) {
                val alarmDT = LocalDateTime.of(date, localTime)
                if (alarmDT.isAfter(now)) {
                    scheduleExactAlarm(context, alarmManager, name, alarmDT, zoneId, requestCode)
                }
            }
        }
    }

    private fun scheduleExactAlarm(
        context: Context,
        alarmManager: AlarmManager,
        prayerName: String,
        alarmDT: LocalDateTime,
        zoneId: ZoneId,
        requestCode: Int
    ) {
        try {
            val intent = Intent(context, IqamaAlarmReceiver::class.java).apply {
                putExtra("PRAYER_NAME", prayerName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val epochMillis = alarmDT.atZone(zoneId).toInstant().toEpochMilli()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Backward-compat alias kept so callers don't break before being updated. */
    suspend fun scheduleNextIqamaAlarm(context: Context, appPreferenceRepo: AppPreferenceRepo) {
        scheduleAllIqamaAlarms(context, appPreferenceRepo)
    }
}
