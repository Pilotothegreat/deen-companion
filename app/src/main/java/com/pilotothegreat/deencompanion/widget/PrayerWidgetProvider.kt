package com.pilotothegreat.deencompanion.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.SystemClock
import android.widget.RemoteViews
import com.pilotothegreat.deencompanion.MainActivity
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.ui.overview.calculateNextPrayer
import com.pilotothegreat.deencompanion.util.PrayerTimeCalculator
import com.pilotothegreat.deencompanion.util.toLocaleHourString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class PrayerWidgetProvider : AppWidgetProvider(), KoinComponent {

    private val repo: AppPreferenceRepo by inject()

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.pilotothegreat.deencompanion.widget.ACTION_UPDATE_WIDGET"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PrayerWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                try {
                    updateWidgets(context, appWidgetManager, appWidgetIds)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
            scheduleNextUpdate(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
        scheduleNextUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleNextUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelUpdateAlarm(context)
    }

    private suspend fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        try {
            val lastUpdated = repo.lastPrayerTimeUpdate.first()
            val hasOpened = lastUpdated != 0L
            val lang = repo.appLanguage.first()
            val locale = Locale(lang)
            val config = Configuration(context.resources.configuration).apply {
                setLocale(locale)
            }
            val localizedContext = context.createConfigurationContext(config)

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.prayer_widget_layout)

                if (hasOpened) {
                    val lat = repo.latitude.first()
                    val lon = repo.longitude.first()
                    val tzId = repo.timezoneId.first()
                    val calcMethod = repo.calcMethod.first()
                    val asrSchool = repo.asrSchool.first()

                    val zoneId = try { ZoneId.of(tzId) } catch (e: Exception) { ZoneId.systemDefault() }
                    val today = LocalDate.now(zoneId)
                    val zonedDateTime = today.atStartOfDay(zoneId)
                    val offsetHours = zonedDateTime.offset.totalSeconds / 3600.0

                    val times = PrayerTimeCalculator.calculate(
                        date = today,
                        latitude = lat,
                        longitude = lon,
                        timezoneOffsetHours = offsetHours,
                        method = calcMethod,
                        asrSchool = asrSchool
                    )

                    // Get next prayer in localized context
                    val next = calculateNextPrayer(times, tzId, localizedContext)

                    val countdownPrefix = if (lang == "ar") "خلال" else "in"
                    val localizedPrayerName = when (next.name) {
                        "Fajr" -> localizedContext.getString(R.string.fajr)
                        "Sunrise" -> localizedContext.getString(R.string.sunrise)
                        "Dhuhr" -> localizedContext.getString(R.string.dhuhr)
                        "Asr" -> localizedContext.getString(R.string.asr)
                        "Maghrib" -> localizedContext.getString(R.string.maghrib)
                        "Isha" -> localizedContext.getString(R.string.isha)
                        else -> next.name
                    }
                    views.setTextViewText(R.id.widget_prayer_name, localizedPrayerName)
                    views.setTextViewText(R.id.widget_countdown, "$countdownPrefix ${next.remainingTimeStr}")
                    views.setTextViewText(R.id.widget_prayer_time, next.timeStr)
                } else {
                    views.setTextViewText(R.id.widget_prayer_name, localizedContext.getString(R.string.app_name))
                    views.setTextViewText(R.id.widget_countdown, localizedContext.getString(R.string.widget_placeholder_initialize))
                    views.setTextViewText(R.id.widget_prayer_time, "--:--")
                }

                views.setTextViewText(R.id.widget_title, localizedContext.getString(R.string.app_name))

                // Set click intent to open main application on the background ID
                val mainIntent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(android.R.id.background, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleNextUpdate(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, PrayerWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                2002,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 1 minute interval (60,000 ms)
            val triggerTime = SystemClock.elapsedRealtime() + 60 * 1000L
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelUpdateAlarm(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, PrayerWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                2002,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
