package com.pilotothegreat.deencompanion.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.alarms.launchAsync
import com.pilotothegreat.deencompanion.core.text.Inspirations
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import java.time.LocalDate

/** Shows the same verse or hadith of the day as the home screen. */
class InspirationWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) =
        launchAsync { render(context) }

    companion object {
        suspend fun render(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, InspirationWidgetProvider::class.java))
            if (ids.isEmpty()) return

            val settings = WidgetDeps.settings.current()
            val res = AppLanguage.localizedContext(context, settings.appLanguage)
            val locale = AppLanguage.locale(settings.appLanguage)
            val inspiration = Inspirations.forDate(LocalDate.now(settings.zone))

            val views = RemoteViews(context.packageName, R.layout.inspiration_widget_layout).apply {
                setTextViewText(R.id.widget_title, res.getString(R.string.daily_inspiration))
                setTextViewText(R.id.widget_inspiration_text, inspiration.text(locale))
                setTextViewText(R.id.widget_inspiration_ref, inspiration.source(locale))
                setOnClickPendingIntent(android.R.id.background, openAppIntent(context))
            }
            manager.updateAppWidget(ids, views)
        }
    }
}
