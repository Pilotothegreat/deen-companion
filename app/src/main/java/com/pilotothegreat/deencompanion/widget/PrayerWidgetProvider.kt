package com.pilotothegreat.deencompanion.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.alarms.launchAsync
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.nameRes
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Shows the next prayer with a live countdown. The countdown is a RemoteViews Chronometer, so
 * the widget only needs to be redrawn when the next prayer changes.
 */
class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) =
        launchAsync { render(context) }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) launchAsync { render(context) }
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(refreshIntent(context))
    }

    companion object {
        private const val ACTION_REFRESH = "com.pilotothegreat.deencompanion.widget.ACTION_UPDATE_WIDGET"

        suspend fun render(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PrayerWidgetProvider::class.java))
            if (ids.isEmpty()) return

            val settings = WidgetDeps.settings.current()
            val res = AppLanguage.localizedContext(context, settings.appLanguage)
            val locale = AppLanguage.locale(settings.appLanguage)
            val now = ZonedDateTime.now(settings.zone)
            val next = DaySchedule.next(now, settings.prayerConfig)
            val remainingMillis = Duration.between(now, next.adhan).toMillis()

            val views = RemoteViews(context.packageName, R.layout.prayer_widget_layout).apply {
                setTextViewText(R.id.widget_title, res.getString(R.string.widget_next_prayer_title))
                setTextViewText(R.id.widget_prayer_name, res.getString(next.prayer.nameRes))
                setTextViewText(R.id.widget_prayer_time, Formatters.time(context, next.adhan.toLocalTime(), locale))
                setChronometer(R.id.widget_countdown, SystemClock.elapsedRealtime() + remainingMillis, null, true)
                setChronometerCountDown(R.id.widget_countdown, true)
                setOnClickPendingIntent(android.R.id.background, openAppIntent(context))
            }
            manager.updateAppWidget(ids, views)

            // Redraw once the next prayer arrives; a non-waking alarm is enough for a widget.
            context.getSystemService(AlarmManager::class.java)
                ?.set(AlarmManager.RTC, next.adhan.toInstant().toEpochMilli() + 1_000, refreshIntent(context))
        }

        private fun refreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            2002,
            Intent(context, PrayerWidgetProvider::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
