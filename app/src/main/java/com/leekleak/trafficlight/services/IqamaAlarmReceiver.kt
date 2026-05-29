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

import androidx.core.app.NotificationManagerCompat
import com.leekleak.trafficlight.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class IqamaAlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val repo: AppPreferenceRepo by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"

        showPrayerNotification(context, prayerName, isIqama = true)

        // Reschedule next alarm
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                IqamaAlarmManager.scheduleNextIqamaAlarm(context, repo)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showPrayerNotification(context: Context, prayerName: String, isIqama: Boolean) {
        val title = if (isIqama) "Iqama — $prayerName" else "Prayer Time — $prayerName"
        val body  = if (isIqama) "Iqama for $prayerName has begun" else "Time for $prayerName prayer"

        val notification = NotificationCompat.Builder(context, "prayer_times")
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
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
