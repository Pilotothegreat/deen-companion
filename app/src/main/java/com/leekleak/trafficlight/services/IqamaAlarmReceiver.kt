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
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class IqamaAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "iqama_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Iqama Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when it is time for Iqama"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Iqama Time")
            .setContentText("It is time for the $prayerName Iqama prayer.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(2002, notification)

        // Reschedule next alarm
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                val repo: AppPreferenceRepo = GlobalContext.get().get()
                IqamaAlarmManager.scheduleNextIqamaAlarm(context, repo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
