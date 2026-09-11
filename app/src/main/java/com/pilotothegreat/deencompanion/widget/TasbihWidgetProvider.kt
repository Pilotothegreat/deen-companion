package com.pilotothegreat.deencompanion.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.alarms.launchAsync
import com.pilotothegreat.deencompanion.core.tasbih.TasbihEngine
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.labelRes
import kotlinx.coroutines.flow.first

/** Counts dhikr from the home screen using the same rules and storage as the app. */
class TasbihWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) =
        launchAsync { render(context) }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_INCREMENT) {
            launchAsync {
                WidgetDeps.tasbih.increment()
                render(context)
            }
        }
    }

    companion object {
        private const val ACTION_INCREMENT = "com.pilotothegreat.deencompanion.widget.ACTION_INCREMENT_TASBIH"

        suspend fun render(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TasbihWidgetProvider::class.java))
            if (ids.isEmpty()) return

            val languageTag = WidgetDeps.settings.current().appLanguage
            val res = AppLanguage.localizedContext(context, languageTag)
            val locale = AppLanguage.locale(languageTag)
            val state = WidgetDeps.tasbih.state.first()
            val target = Formatters.number(TasbihEngine.roundTarget(state), locale)

            val increment = PendingIntent.getBroadcast(
                context,
                3001,
                Intent(context, TasbihWidgetProvider::class.java).setAction(ACTION_INCREMENT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val views = RemoteViews(context.packageName, R.layout.tasbih_widget_layout).apply {
                setTextViewText(R.id.widget_title, res.getString(R.string.tasbih_counter))
                setTextViewText(R.id.widget_tasbih_count, Formatters.number(state.count, locale))
                setTextViewText(R.id.widget_dhikr_name, "${res.getString(state.dhikr.labelRes)} · $target")
                setOnClickPendingIntent(R.id.widget_btn_container, increment)
                setOnClickPendingIntent(android.R.id.background, openAppIntent(context))
            }
            manager.updateAppWidget(ids, views)
        }
    }
}
