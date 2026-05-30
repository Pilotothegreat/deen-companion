package com.pilotothegreat.deencompanion.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
                    val currentCount = repo.tasbihCount.first()
                    repo.setTasbihCount(currentCount + 1)
                    
                    // Update widget immediately
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val componentName = ComponentName(context, TasbihWidgetProvider::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                    updateWidgets(context, appWidgetManager, appWidgetIds)
                } catch (e: Exception) {
                    e.printStackTrace()
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
                    e.printStackTrace()
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
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        try {
            val count = repo.tasbihCount.first()
            val dhikr = repo.tasbihDhikr.first()

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.tasbih_widget_layout)
                
                views.setTextViewText(R.id.widget_tasbih_count, count.toString())
                views.setTextViewText(R.id.widget_dhikr_name, dhikr)

                // Increment button broadcast intent
                val incrementIntent = Intent(context, TasbihWidgetProvider::class.java).apply {
                    action = ACTION_INCREMENT_TASBIH
                }
                val incrementPendingIntent = PendingIntent.getBroadcast(
                    context,
                    3001,
                    incrementIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_container, incrementPendingIntent)

                // Open app on background container click
                val mainIntent = Intent(context, MainActivity::class.java)
                val mainPendingIntent = PendingIntent.getActivity(
                    context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_container, mainPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
