// FIXED: Confirm try-catch and KoinComponent implementation
package com.pilotothegreat.deencompanion.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import androidx.core.app.NotificationManagerCompat
import com.pilotothegreat.deencompanion.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class IqamaAlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val repo: AppPreferenceRepo by inject()

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"

            // Play notification sound with custom volume and reschedule next alarm
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.Default)
            var launched = false
            try {
                scope.launch {
                    try {
                        val lang = repo.appLanguage.first()
                        val localizedPrayerName = when (prayerName) {
                            "Fajr" -> if (lang == "ar") "الفجر" else "Fajr"
                            "Dhuhr" -> if (lang == "ar") "الظهر" else "Dhuhr"
                            "Asr" -> if (lang == "ar") "العصر" else "Asr"
                            "Maghrib" -> if (lang == "ar") "المغرب" else "Maghrib"
                            "Isha" -> if (lang == "ar") "العشاء" else "Isha"
                            else -> prayerName
                        }

                        showPrayerNotification(context, localizedPrayerName, isIqama = true, lang = lang)

                        val volume = repo.notificationVolume.first()
                        if (volume > 0) {
                            try {
                                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                val mediaPlayer = android.media.MediaPlayer().apply {
                                    setDataSource(context, ringtoneUri)
                                    setAudioAttributes(
                                        android.media.AudioAttributes.Builder()
                                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                            .build()
                                    )
                                    val vol = volume / 100.0f
                                    setVolume(vol, vol)
                                    setOnCompletionListener { mp ->
                                        mp.release()
                                    }
                                    setOnErrorListener { mp, _, _ ->
                                        mp.release()
                                        true
                                    }
                                    prepare() // synchronous – we are already on Dispatchers.Default
                                    start()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        IqamaAlarmManager.scheduleAllIqamaAlarms(context, repo)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            pendingResult.finish()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                launched = true
            } finally {
                if (!launched) {
                    try {
                        pendingResult.finish()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showPrayerNotification(context: Context, prayerName: String, isIqama: Boolean, lang: String) {
        val title = if (lang == "ar") {
            if (isIqama) "الإقامة — $prayerName" else "حان وقت صلاة — $prayerName"
        } else {
            if (isIqama) "Iqama — $prayerName" else "Prayer Time — $prayerName"
        }

        val body = if (lang == "ar") {
            if (isIqama) "إقامة صلاة $prayerName قد بدأت" else "حان الآن وقت صلاة $prayerName"
        } else {
            if (isIqama) "Iqama for $prayerName has begun" else "Time for $prayerName prayer"
        }

        // Build notification without default sound so that our MediaPlayer sound plays cleanly at the customized volume
        val notification = NotificationCompat.Builder(context, "prayer_times")
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(null) // Silent notification builder sound to avoid double play
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(prayerName.hashCode(), notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}

