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
import com.pilotothegreat.deencompanion.data.quran.KhatmaRepository
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
    val khatma: KhatmaRepository by inject()
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

class MomentWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MomentWidget()
}

class QiblaWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QiblaWidget()
}

class KhatmaWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KhatmaWidget()
}

class DateWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DateWidget()
}

/** Redraws the widgets: all of them at a prayer or midnight, and only the next-prayer bar in between. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WidgetUpdater.ACTION_REFRESH) return
        // An alarm booked before this extra existed meant everything.
        val everything = intent.getBooleanExtra(WidgetUpdater.EXTRA_EVERYTHING, true)
        launchAsync { if (everything) WidgetUpdater.updateAll(context) else WidgetUpdater.moveTheBar(context) }
    }
}

object WidgetUpdater {
    internal const val ACTION_REFRESH = "com.pilotothegreat.deencompanion.widget.ACTION_REFRESH"
    internal const val EXTRA_EVERYTHING = "everything"

    private val receivers = listOf(
        PrayerWidgetProvider::class,
        PrayerTimesWidgetProvider::class,
        VerseWidgetProvider::class,
        InspirationWidgetProvider::class,
        AthkarWidgetProvider::class,
        TasbihWidgetProvider::class,
        MomentWidgetProvider::class,
        QiblaWidgetProvider::class,
        KhatmaWidgetProvider::class,
        DateWidgetProvider::class,
    )

    /** Re-renders every placed widget (widgets that aren't on the home screen are skipped) and sets the next redraw. */
    suspend fun updateAll(context: Context) {
        WidgetKind.entries.map { it.widget() }.forEach { widget ->
            runCatching { widget.updateAll(context) }
                .onFailure { Timber.w(it, "%s update failed", widget.javaClass.simpleName) }
        }
        runCatching { scheduleRefresh(context) }.onFailure { Timber.w(it, "Couldn't schedule the widget refresh") }
    }

    /** Redraws only the next-prayer widget, whose bar is the one thing that moves between prayers, and books the next step. */
    internal suspend fun moveTheBar(context: Context) {
        runCatching { NextPrayerWidget().updateAll(context) }.onFailure { Timber.w(it, "NextPrayerWidget update failed") }
        runCatching { scheduleRefresh(context) }.onFailure { Timber.w(it, "Couldn't schedule the widget refresh") }
    }

    /** Hands the widget picker live previews on Android 15+. The system rate-limits this, so failures are ignored. */
    suspend fun publishPreviews(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val manager = GlanceAppWidgetManager(context)
        receivers.forEach { receiver -> runCatching { manager.setWidgetPreviews(receiver) } }
    }

    /** Books the next redraw with [RefreshPlan]. A non-waking alarm: nobody needs a bar moved in a pocket. */
    private suspend fun scheduleRefresh(context: Context) {
        val settings = WidgetDeps.settings.current()
        val now = ZonedDateTime.now(settings.zone)
        val refresh = RefreshPlan.next(
            now = now,
            previousPrayer = previousAdhan(settings, now),
            nextPrayer = DaySchedule.next(now, settings.prayerConfig).adhan,
            midnight = now.toLocalDate().plusDays(1).atStartOfDay(settings.zone),
        )
        context.getSystemService(AlarmManager::class.java)
            ?.set(AlarmManager.RTC, refresh.at.toInstant().toEpochMilli(), refreshIntent(context, refresh.everything))
    }

    private fun refreshIntent(context: Context, everything: Boolean): PendingIntent = PendingIntent.getBroadcast(
        context,
        2002,
        Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION_REFRESH).putExtra(EXTRA_EVERYTHING, everything),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun openApp(context: Context): Intent =
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
