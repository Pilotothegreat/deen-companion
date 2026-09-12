package com.pilotothegreat.deencompanion.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import com.pilotothegreat.deencompanion.MainActivity
import com.pilotothegreat.deencompanion.alarms.launchAsync
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
import com.pilotothegreat.deencompanion.data.athkar.AthkarRepository
import com.pilotothegreat.deencompanion.data.moment.MomentRepository
import com.pilotothegreat.deencompanion.data.quran.QuranRepository
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.data.tasbih.TasbihRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.ZonedDateTime

internal object WidgetDeps : KoinComponent {
    val settings: SettingsRepository by inject()
    val tasbih: TasbihRepository by inject()
    val athkar: AthkarRepository by inject()
    val quran: QuranRepository by inject()
    val moments: MomentRepository by inject()
}

// The first three receivers keep their original class names so widgets placed before the update stay put.
class PrayerWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextPrayerWidget()
}

class TasbihWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TasbihWidget()
}

class InspirationWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = InspirationWidget()
}

class PrayerTimesWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerTimesWidget()
}

class VerseWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VerseWidget()
}

class AthkarWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AthkarWidget()
}

/** Redraws the widgets when the next prayer arrives or the day changes. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WidgetUpdater.ACTION_REFRESH) launchAsync { WidgetUpdater.updateAll(context) }
    }
}

object WidgetUpdater {
    internal const val ACTION_REFRESH = "com.pilotothegreat.deencompanion.widget.ACTION_REFRESH"

    private val receivers = listOf(
        PrayerWidgetProvider::class,
        PrayerTimesWidgetProvider::class,
        VerseWidgetProvider::class,
        InspirationWidgetProvider::class,
        AthkarWidgetProvider::class,
        TasbihWidgetProvider::class,
    )

    /** Re-renders every placed widget (widgets that aren't on the home screen are skipped) and sets the next redraw. */
    suspend fun updateAll(context: Context) {
        listOf(NextPrayerWidget(), PrayerTimesWidget(), VerseWidget(), InspirationWidget(), AthkarWidget(), TasbihWidget())
            .forEach { widget ->
                runCatching { widget.updateAll(context) }
                    .onFailure { Timber.w(it, "%s update failed", widget.javaClass.simpleName) }
            }
        runCatching { scheduleRefresh(context) }.onFailure { Timber.w(it, "Couldn't schedule the widget refresh") }
    }

    /** Hands the widget picker live previews on Android 15+. The system rate-limits this, so failures are ignored. */
    suspend fun publishPreviews(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val manager = GlanceAppWidgetManager(context)
        receivers.forEach { receiver -> runCatching { manager.setWidgetPreviews(receiver) } }
    }

    /**
     * Wakes the widgets at the next prayer (the countdown and the suggested athkar move on) or at
     * midnight (the daily verse, hadith and table), whichever comes first. A non-waking alarm is enough.
     */
    private suspend fun scheduleRefresh(context: Context) {
        val settings = WidgetDeps.settings.current()
        val now = ZonedDateTime.now(settings.zone)
        val nextPrayer = DaySchedule.next(now, settings.prayerConfig).adhan
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay(settings.zone)
        val at = minOf(nextPrayer, midnight).toInstant().toEpochMilli() + 1_000
        context.getSystemService(AlarmManager::class.java)?.set(AlarmManager.RTC, at, refreshIntent(context))
    }

    private fun refreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        2002,
        Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION_REFRESH),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun openApp(context: Context): Intent =
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
