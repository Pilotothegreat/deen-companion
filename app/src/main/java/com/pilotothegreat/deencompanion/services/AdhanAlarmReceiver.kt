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
                    val localizedPrayerName = when (prayerName) {
                        "Fajr" -> if (lang == "ar") "الفجر" else "Fajr"
                        "Dhuhr" -> if (lang == "ar") "الظهر" else "Dhuhr"
                        "Asr" -> if (lang == "ar") "العصر" else "Asr"
                        "Maghrib" -> if (lang == "ar") "المغرب" else "Maghrib"
                        "Isha" -> if (lang == "ar") "العشاء" else "Isha"
                        else -> prayerName
                    }
                    showAdhanNotification(context, localizedPrayerName, lang)
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

    private fun showAdhanNotification(context: Context, prayerName: String, lang: String) {
        val title = if (lang == "ar") "الأذان — $prayerName" else "Adhan — $prayerName"
        val body = if (lang == "ar") "حان الآن وقت صلاة $prayerName" else "It is time for the $prayerName prayer"

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
