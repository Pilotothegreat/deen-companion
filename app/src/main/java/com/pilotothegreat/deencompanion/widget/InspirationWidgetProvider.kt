package com.pilotothegreat.deencompanion.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
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
import java.time.LocalDate
import java.util.Locale

class InspirationWidgetProvider : AppWidgetProvider(), KoinComponent {

    private val repo: AppPreferenceRepo by inject()

    private val inspirations = listOf(
        Pair("So verily, with hardship, there is ease.", "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا"),
        Pair("Indeed, Allah is with the patient.", "إِنَّ اللَّهَ مَعَ الصَّابِرِينَ"),
        Pair("And He found you lost and guided you.", "وَوَجَدَكَ ضَالًّا فَهَدَىٰ"),
        Pair("Call upon Me; I will answer you.", "ادْعُونِي أَسْتَجِبْ لَكُمْ"),
        Pair("My mercy encompasses all things.", "وَرَحْمَتِي وَسِعَتْ كُلَّ شَيْءٍ"),
        Pair("Remember Me; I will remember you.", "فَاذْكُرُونِي أَذْكُرْكُمْ"),
        Pair("Allah does not burden a soul beyond that it can bear.", "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا"),
        Pair("Indeed, actions are but by intentions.", "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ")
    )
    
    private val refs = listOf(
        "Quran 94:5",
        "Quran 2:153",
        "Quran 93:7",
        "Quran 40:60",
        "Quran 7:156",
        "Quran 2:152",
        "Quran 2:286",
        "Bukhari & Muslim"
    )

    private fun getLocalizedInspirationRef(ref: String, lang: String): String {
        if (lang != "ar") return ref
        val arabicDigits = mapOf('0' to '٠', '1' to '١', '2' to '٢', '3' to '٣', '4' to '٤',
            '5' to '٥', '6' to '٦', '7' to '٧', '8' to '٨', '9' to '٩')
        return ref
            .replace("Quran", "القرآن")
            .replace("Bukhari & Muslim", "البخاري ومسلم")
            .replace("Bukhari", "البخاري")
            .map { arabicDigits[it] ?: it }
            .joinToString("")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, InspirationWidgetProvider::class.java)
            )
            
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                try {
                    updateWidgets(context, appWidgetManager, appWidgetIds)
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Error updating inspiration widget on ACTION_APPWIDGET_UPDATE")
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
                timber.log.Timber.e(e, "Error updating inspiration widget on onUpdate")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        try {
            val lang = repo.appLanguage.first()
            val locale = Locale.forLanguageTag(lang)
            val config = Configuration(context.resources.configuration).apply {
                setLocale(locale)
            }
            val localizedContext = context.createConfigurationContext(config)

            val tzId = try { repo.timezoneId.first() } catch (e: Exception) { java.util.TimeZone.getDefault().id }
            val zoneId = try { java.time.ZoneId.of(tzId) } catch (e: Exception) { java.time.ZoneId.systemDefault() }
            val dayIndex = LocalDate.now(zoneId).dayOfYear % inspirations.size
            val inspiration = inspirations[dayIndex]
            val ref = refs[dayIndex]
            
            val text = if (lang == "ar") inspiration.second else inspiration.first

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.inspiration_widget_layout)
                
                views.setTextViewText(R.id.widget_title, localizedContext.getString(R.string.daily_inspiration))
                views.setTextViewText(R.id.widget_inspiration_text, "\"$text\"")
                views.setTextViewText(R.id.widget_inspiration_ref, "— ${getLocalizedInspirationRef(ref, lang)}")

                // Open app on widget click
                val mainIntent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(android.R.id.background, pendingIntent)
                views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error in InspirationWidgetProvider.updateWidgets")
        }
    }
}
