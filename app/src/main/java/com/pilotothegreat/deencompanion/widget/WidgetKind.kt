package com.pilotothegreat.deencompanion.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidget
import com.pilotothegreat.deencompanion.widget.WidgetOption.ATHKAR
import com.pilotothegreat.deencompanion.widget.WidgetOption.COLOURS
import com.pilotothegreat.deencompanion.widget.WidgetOption.IQAMA
import com.pilotothegreat.deencompanion.widget.WidgetOption.TRANSPARENCY

/** Something a widget can be told on its configuration screen. */
internal enum class WidgetOption { COLOURS, TRANSPARENCY, IQAMA, ATHKAR }

/**
 * The seven widgets. Each lists only the options it actually uses — the configuration screen used to show
 * all four to every widget, and three of them ignored every one — and builds its content in one place, for
 * the home screen and for the configuration screen's live preview alike.
 */
internal enum class WidgetKind(val receiver: Class<*>, val options: Set<WidgetOption>, val previewSize: DpSize) {
    NEXT_PRAYER(PrayerWidgetProvider::class.java, setOf(COLOURS, TRANSPARENCY, IQAMA), DpSize(250.dp, 200.dp)),
    PRAYER_TIMES(PrayerTimesWidgetProvider::class.java, setOf(COLOURS, TRANSPARENCY, IQAMA), DpSize(250.dp, 220.dp)),
    ATHKAR_NOW(AthkarWidgetProvider::class.java, setOf(COLOURS, TRANSPARENCY, ATHKAR), DpSize(200.dp, 180.dp)),
    MOMENT(MomentWidgetProvider::class.java, setOf(COLOURS, TRANSPARENCY), DpSize(250.dp, 120.dp)),
    TASBIH(TasbihWidgetProvider::class.java, setOf(COLOURS, TRANSPARENCY), DpSize(200.dp, 110.dp)),
    VERSE(VerseWidgetProvider::class.java, setOf(COLOURS, TRANSPARENCY), DpSize(250.dp, 180.dp)),
    INSPIRATION(InspirationWidgetProvider::class.java, setOf(COLOURS, TRANSPARENCY), DpSize(250.dp, 180.dp)),
    ;

    fun widget(): GlanceAppWidget = when (this) {
        NEXT_PRAYER -> NextPrayerWidget()
        PRAYER_TIMES -> PrayerTimesWidget()
        ATHKAR_NOW -> AthkarWidget()
        MOMENT -> MomentWidget()
        TASBIH -> TasbihWidget()
        VERSE -> VerseWidget()
        INSPIRATION -> InspirationWidget()
    }

    /** Loads what the widget shows and returns it drawn with [config]. */
    suspend fun content(context: Context, config: WidgetConfig): @Composable () -> Unit = when (this) {
        NEXT_PRAYER -> loadNextPrayer(context, config)
        PRAYER_TIMES -> loadPrayerTimes(context, config)
        ATHKAR_NOW -> loadAthkar(context, config)
        MOMENT -> loadMoment(context, config)
        TASBIH -> loadTasbih(context, config)
        VERSE -> loadVerse(context, config)
        INSPIRATION -> loadInspiration(context, config)
    }

    companion object {
        /** The kind of widget a launcher's provider class name belongs to. */
        fun ofProvider(className: String?): WidgetKind? = entries.firstOrNull { it.receiver.name == className }
    }
}
