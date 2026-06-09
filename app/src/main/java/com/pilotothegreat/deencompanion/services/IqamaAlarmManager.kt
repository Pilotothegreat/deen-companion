// FIXED: Add canScheduleExactAlarms() check before scheduling exact alarm
package com.pilotothegreat.deencompanion.services

import android.app.AlarmManager
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

import android.media.AudioAttributes
import android.media.RingtoneManager
import android.app.NotificationChannel
import android.app.NotificationManager

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

    suspend fun scheduleNextIqamaAlarm(context: Context, appPreferenceRepo: AppPreferenceRepo) {
        val lat = appPreferenceRepo.latitude.first()
        val lon = appPreferenceRepo.longitude.first()
        val tzId = appPreferenceRepo.timezoneId.first()
        val calcMethod = appPreferenceRepo.calcMethod.first()
        val asrSchool = appPreferenceRepo.asrSchool.first()

        // Iqama offsets and modes
        val fajrOffset = appPreferenceRepo.fajrIqamaOffset.first()
        val dhuhrOffset = appPreferenceRepo.dhuhrIqamaOffset.first()
        val asrOffset = appPreferenceRepo.asrIqamaOffset.first()
        val maghribOffset = appPreferenceRepo.maghribIqamaOffset.first()
        val ishaOffset = appPreferenceRepo.ishaIqamaOffset.first()

        val fajrIsFixed = appPreferenceRepo.fajrIqamaIsFixed.first()
        val dhuhrIsFixed = appPreferenceRepo.dhuhrIqamaIsFixed.first()
        val asrIsFixed = appPreferenceRepo.asrIqamaIsFixed.first()
        val maghribIsFixed = appPreferenceRepo.maghribIqamaIsFixed.first()
        val ishaIsFixed = appPreferenceRepo.ishaIqamaIsFixed.first()

        val fajrIqamaTimeVal = appPreferenceRepo.fajrIqamaTime.first()
        val dhuhrIqamaTimeVal = appPreferenceRepo.dhuhrIqamaTime.first()
        val asrIqamaTimeVal = appPreferenceRepo.asrIqamaTime.first()
        val maghribIqamaTimeVal = appPreferenceRepo.maghribIqamaTime.first()
        val ishaIqamaTimeVal = appPreferenceRepo.ishaIqamaTime.first()

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

            val fajrIqamaTime = if (fajrIsFixed) {
                parseFixedTime(fajrIqamaTimeVal) ?: times.fajr.plusMinutes(fajrOffset.toLong())
            } else {
                times.fajr.plusMinutes(fajrOffset.toLong())
            }
            val dhuhrIqamaTime = if (dhuhrIsFixed) {
                parseFixedTime(dhuhrIqamaTimeVal) ?: times.dhuhr.plusMinutes(dhuhrOffset.toLong())
            } else {
                times.dhuhr.plusMinutes(dhuhrOffset.toLong())
            }
            val asrIqamaTime = if (asrIsFixed) {
                parseFixedTime(asrIqamaTimeVal) ?: times.asr.plusMinutes(asrOffset.toLong())
            } else {
                times.asr.plusMinutes(asrOffset.toLong())
            }
            val maghribIqamaTime = if (maghribIsFixed) {
                parseFixedTime(maghribIqamaTimeVal) ?: times.maghrib.plusMinutes(maghribOffset.toLong())
            } else {
                times.maghrib.plusMinutes(maghribOffset.toLong())
            }
            val ishaIqamaTime = if (ishaIsFixed) {
                parseFixedTime(ishaIqamaTimeVal) ?: times.isha.plusMinutes(ishaOffset.toLong())
            } else {
                times.isha.plusMinutes(ishaOffset.toLong())
            }

            val prayerList = listOf(
                Pair("Fajr", fajrIqamaTime),
                Pair("Dhuhr", dhuhrIqamaTime),
                Pair("Asr", asrIqamaTime),
                Pair("Maghrib", maghribIqamaTime),
                Pair("Isha", ishaIqamaTime)
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
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, IqamaAlarmReceiver::class.java).apply {
                    putExtra("PRAYER_NAME", nextPrayerName)
                }

                val requestCode = "Iqama_$nextPrayerName".hashCode()
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val epochMillis = alarmDateTime.atZone(zoneId).toInstant().toEpochMilli()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            epochMillis,
                            pendingIntent
                        )
                    } else {
                        // Fallback to inexact alarm
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
            } catch (e: SecurityException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

