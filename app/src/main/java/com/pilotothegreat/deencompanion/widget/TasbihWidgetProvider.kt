package com.pilotothegreat.deencompanion.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews
import com.pilotothegreat.deencompanion.MainActivity
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

class TasbihWidgetProvider : AppWidgetProvider(), KoinComponent {

    private val repo: AppPreferenceRepo by inject()

    companion object {
        const val ACTION_INCREMENT_TASBIH = "com.pilotothegreat.deencompanion.widget.ACTION_INCREMENT_TASBIH"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_INCREMENT_TASBIH) {
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                try {
                    repo.incrementAndCycleTasbih()
                    
                    // Update widget immediately
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val componentName = ComponentName(context, TasbihWidgetProvider::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                    updateWidgets(context, appWidgetManager, appWidgetIds)
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Error processing ACTION_INCREMENT_TASBIH")
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TasbihWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                try {
                    updateWidgets(context, appWidgetManager, appWidgetIds)
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Error updating Tasbih widgets on ACTION_APPWIDGET_UPDATE")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            try {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Error in TasbihWidgetProvider.onUpdate")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        try {
            val lastUpdated = repo.lastPrayerTimeUpdate.first()
            val hasOpened = lastUpdated != 0L

            val lang = repo.appLanguage.first()
            val locale = Locale.forLanguageTag(lang)
            val config = Configuration(context.resources.configuration).apply {
                setLocale(locale)
            }
            val localizedContext = context.createConfigurationContext(config)

            val count = repo.tasbihCount.first()
            val dhikr = repo.tasbihDhikr.first()

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.tasbih_widget_layout)
                
                views.setTextViewText(R.id.widget_title, localizedContext.getString(R.string.tasbih_counter))
                views.setTextViewText(R.id.widget_tasbih_count, count.toString())

                val localizedDhikr = when (dhikr) {
                    "سبحان الله" -> localizedContext.getString(R.string.tasbih_dhikr_subhanallah)
                    "الحمد لله" -> localizedContext.getString(R.string.tasbih_dhikr_alhamdulillah)
                    "لا إله إلا الله" -> localizedContext.getString(R.string.tasbih_dhikr_lailahaillallah)
                    "الله أكبر" -> localizedContext.getString(R.string.tasbih_dhikr_allahuakbar)
                    else -> dhikr
                }
                views.setTextViewText(R.id.widget_dhikr_name, localizedDhikr)

                // Increment button broadcast intent (FLAG_IMMUTABLE prevents redirection)
                val incrementIntent = Intent(context, TasbihWidgetProvider::class.java).apply {
                    action = ACTION_INCREMENT_TASBIH
                }
                val incrementPendingIntent = PendingIntent.getBroadcast(
                    context,
                    3001,
                    incrementIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_container, incrementPendingIntent)

                // Open app on background container click
                val mainIntent = Intent(context, MainActivity::class.java)
                val mainPendingIntent = PendingIntent.getActivity(
                    context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(android.R.id.background, mainPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_title, mainPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error updating Tasbih widgets")
        }
    }
}
