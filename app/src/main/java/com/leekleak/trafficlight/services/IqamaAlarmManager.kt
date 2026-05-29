package com.leekleak.trafficlight.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.util.PrayerTimeCalculator
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

import android.media.AudioAttributes
import android.media.RingtoneManager
import android.app.NotificationChannel
import android.app.NotificationManager

object IqamaAlarmManager {

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "prayer_times",
                "Prayer Times",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Adhan and Iqama time notifications"
                enableVibration(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    suspend fun scheduleNextIqamaAlarm(context: Context, appPreferenceRepo: AppPreferenceRepo) {
        val lat = appPreferenceRepo.latitude.first()
        val lon = appPreferenceRepo.longitude.first()
        val tzId = appPreferenceRepo.timezoneId.first()
        val calcMethod = appPreferenceRepo.calcMethod.first()
        val asrSchool = appPreferenceRepo.asrSchool.first()

        // Iqama offsets
        val fajrOffset = appPreferenceRepo.fajrIqamaOffset.first()
        val dhuhrOffset = appPreferenceRepo.dhuhrIqamaOffset.first()
        val asrOffset = appPreferenceRepo.asrIqamaOffset.first()
        val maghribOffset = appPreferenceRepo.maghribIqamaOffset.first()
        val ishaOffset = appPreferenceRepo.ishaIqamaOffset.first()

        val zoneId = try { ZoneId.of(tzId) } catch (e: Exception) { ZoneId.systemDefault() }
        val now = LocalDateTime.now(zoneId)

        // Find next prayer time (checking today and tomorrow)
        var alarmDateTime: LocalDateTime? = null
        var nextPrayerName = ""

        val daysToCheck = listOf(LocalDate.now(zoneId), LocalDate.now(zoneId).plusDays(1))

        outer@ for (date in daysToCheck) {
            // Get timezone offset in hours for this date
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

            val prayerList = listOf(
                Pair("Fajr", times.fajr.plusMinutes(fajrOffset.toLong())),
                Pair("Dhuhr", times.dhuhr.plusMinutes(dhuhrOffset.toLong())),
                Pair("Asr", times.asr.plusMinutes(asrOffset.toLong())),
                Pair("Maghrib", times.maghrib.plusMinutes(maghribOffset.toLong())),
                Pair("Isha", times.isha.plusMinutes(ishaOffset.toLong()))
            )

            for (p in prayerList) {
                val pDateTime = LocalDateTime.of(date, p.second)
                if (pDateTime.isAfter(now)) {
                    alarmDateTime = pDateTime
                    nextPrayerName = p.first
                    break@outer
                }
            }
        }

        if (alarmDateTime != null) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, IqamaAlarmReceiver::class.java).apply {
                    putExtra("PRAYER_NAME", nextPrayerName)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    1001,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val epochMillis = alarmDateTime.atZone(zoneId).toInstant().toEpochMilli()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
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
            } catch (e: SecurityException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
