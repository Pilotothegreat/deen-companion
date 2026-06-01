package com.pilotothegreat.deencompanion.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pilotothegreat.deencompanion.R
import timber.log.Timber

class AdhanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"
            Timber.d("AdhanAlarmReceiver: Received alert for %s", prayerName)
            showAdhanNotification(context, prayerName)
        } catch (e: Exception) {
            Timber.e(e, "Error displaying Adhan notification")
        }
    }

    private fun showAdhanNotification(context: Context, prayerName: String) {
        val title = "Adhan — $prayerName"
        val body = "It is time for the $prayerName prayer"

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
                .notify(prayerName.hashCode() + 20000, notification)
        } catch (e: SecurityException) {
            Timber.e(e, "Permission error showing Adhan notification")
        }
    }
}
