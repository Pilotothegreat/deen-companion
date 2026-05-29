// FIXED: Home Screen widget provider updating every 10 minutes via AlarmManager
package com.leekleak.trafficlight.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.ui.overview.calculateNextPrayer
import com.leekleak.trafficlight.util.PrayerTimeCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.time.ZoneId

class PrayerWidgetProvider : AppWidgetProvider(), KoinComponent {

    private val repo: AppPreferenceRepo by inject()

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.leekleak.trafficlight.widget.ACTION_UPDATE_WIDGET"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, PrayerWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            }
            scheduleNextUpdate(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            updateWidgets(context, appWidgetManager, appWidgetIds)
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

            val next = calculateNextPrayer(times, tzId, context)

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.prayer_widget_layout)
                
                // Show prayer name and countdown (e.g. "Dhuhr in 2h 15m")
                views.setTextViewText(R.id.widget_title, "Deen Companion")
                views.setTextViewText(R.id.widget_prayer_name, next.name)
                views.setTextViewText(R.id.widget_countdown, "in ${next.remainingTimeStr}")
                views.setTextViewText(R.id.widget_prayer_time, next.timeStr)

                // Set click intent to open main application
                val mainIntent = Intent(context, Class.forName("com.leekleak.trafficlight.MainActivity"))
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

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

            // 10 minutes interval (600,000 ms)
            val triggerTime = SystemClock.elapsedRealtime() + 10 * 60 * 1000L
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
