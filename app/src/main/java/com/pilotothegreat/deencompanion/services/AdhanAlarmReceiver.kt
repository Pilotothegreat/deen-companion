package com.pilotothegreat.deencompanion.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class AdhanAlarmReceiver : BroadcastReceiver(), KoinComponent {

    private val repo: AppPreferenceRepo by inject()

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"
            Timber.d("AdhanAlarmReceiver: Received alert for %s", prayerName)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val lang = repo.appLanguage.first()
                    val config = android.content.res.Configuration(context.resources.configuration).apply {
                        setLocale(java.util.Locale(lang))
                    }
                    val localizedContext = context.createConfigurationContext(config)
                    val localizedPrayerName = when (prayerName) {
                        "Fajr" -> localizedContext.getString(R.string.fajr)
                        "Dhuhr" -> localizedContext.getString(R.string.dhuhr)
                        "Asr" -> localizedContext.getString(R.string.asr)
                        "Maghrib" -> localizedContext.getString(R.string.maghrib)
                        "Isha" -> localizedContext.getString(R.string.isha)
                        else -> prayerName
                    }
                    showAdhanNotification(localizedContext, localizedPrayerName)
                    // Reschedule to maintain the rolling 2-day queue
                    AdhanAlarmManager.scheduleAllAdhanAlarms(context, repo)
                } catch (e: Exception) {
                    Timber.e(e, "Error displaying Adhan notification")
                } finally {
                    try { pendingResult.finish() } catch (e: Exception) { Timber.e(e, "Error finishing pendingResult") }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in AdhanAlarmReceiver.onReceive")
        }
    }

    private fun showAdhanNotification(context: Context, prayerName: String) {
        val title = context.getString(R.string.adhan_notification_title, prayerName)
        val body = context.getString(R.string.adhan_notification_body, prayerName)

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
