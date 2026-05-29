// FIXED: Confirm try-catch and KoinComponent implementation
package com.leekleak.trafficlight.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.leekleak.trafficlight.database.AppPreferenceRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import androidx.core.app.NotificationManagerCompat
import com.leekleak.trafficlight.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class IqamaAlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val repo: AppPreferenceRepo by inject()

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"

            showPrayerNotification(context, prayerName, isIqama = true)

            // Play notification sound with custom volume and reschedule next alarm
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                try {
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
                                prepare()
                                start()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    IqamaAlarmManager.scheduleNextIqamaAlarm(context, repo)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showPrayerNotification(context: Context, prayerName: String, isIqama: Boolean) {
        val title = if (isIqama) "Iqama — $prayerName" else "Prayer Time — $prayerName"
        val body  = if (isIqama) "Iqama for $prayerName has begun" else "Time for $prayerName prayer"

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
