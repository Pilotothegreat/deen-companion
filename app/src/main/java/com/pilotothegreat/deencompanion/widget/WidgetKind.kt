package com.pilotothegreat.deencompanion.widget

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidget
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.widget.WidgetOption.ATHKAR
import com.pilotothegreat.deencompanion.widget.WidgetOption.COLOURS
import com.pilotothegreat.deencompanion.widget.WidgetOption.IQAMA
import com.pilotothegreat.deencompanion.widget.WidgetOption.TRANSPARENCY

/** Something a widget can be told on its configuration screen. */
internal enum class WidgetOption { COLOURS, TRANSPARENCY, IQAMA, ATHKAR }

/**
 * The ten widgets. Each lists only the options it actually uses — the configuration screen used to show
 * all four to every widget, and three of them ignored every one — and builds its content in one place, for
 * the home screen and for the configuration screen's live preview alike.
 *
 * [previewSize] is the size the launcher's own grid gives the widget at the cells it asks for in its
 * appwidget-provider (70n - 30 dp, the measurement AOSP's grid uses), so what the picker shows and what
 * lands on the home screen are the same widget rather than two different layouts.
 */
internal enum class WidgetKind(
    val receiver: Class<*>,
    val options: Set<WidgetOption>,
    val previewSize: DpSize,
    /** What this widget is called in the picker, so its configuration screen can say which one it is. */
    @get:StringRes val label: Int,
) {
    NEXT_PRAYER(PrayerWidgetProvider::class.java, setOf(COLOURS, TRANSPARENCY, IQAMA), DpSize(250.dp, 180.dp), R.string.widget_next_prayer_title),
    PRAYER_TIMES(PrayerTimesWidgetProvider::class.java, setOf(COLOURS, TRANSPARENCY, IQAMA), DpSize(250.dp, 180.dp), R.string.widget_prayer_times_title),
    ATHKAR_NOW(AthkarWidgetProvider::class.java, setOf(COLOURS, TRANSPARENCY, ATHKAR), DpSize(180.dp, 180.dp), R.string.athkar),
    MOMENT(MomentWidgetProvider::class.java, setOf(COLOURS, TRANSPARENCY), DpSize(250.dp, 110.dp), R.string.widget_moment),
    TASBIH(TasbihWidgetProvider::class.java, setOf(COLOURS, TRANSPARENCY), DpSize(180.dp, 110.dp), R.string.tasbih_counter),
    VERSE(VerseWidgetProvider::class.java, setOf(COLOURS, TRANSPARENCY), DpSize(250.dp, 180.dp), R.string.verse_of_the_day),
    INSPIRATION(InspirationWidgetProvider::class.java, setOf(COLOURS, TRANSPARENCY), DpSize(250.dp, 180.dp), R.string.daily_inspiration),
    QIBLA(QiblaWidgetProvider::class.java, setOf(COLOURS, TRANSPARENCY), DpSize(250.dp, 110.dp), R.string.qibla_compass),
    KHATMA(KhatmaWidgetProvider::class.java, setOf(COLOURS, TRANSPARENCY), DpSize(250.dp, 110.dp), R.string.khatma),
    DATE(DateWidgetProvider::class.java, setOf(COLOURS, TRANSPARENCY), DpSize(180.dp, 110.dp), R.string.widget_date_title),
    ;

    fun widget(): GlanceAppWidget = when (this) {
        NEXT_PRAYER -> NextPrayerWidget()
        PRAYER_TIMES -> PrayerTimesWidget()
        ATHKAR_NOW -> AthkarWidget()
        MOMENT -> MomentWidget()
        TASBIH -> TasbihWidget()
        VERSE -> VerseWidget()
        INSPIRATION -> InspirationWidget()
        QIBLA -> QiblaWidget()
        KHATMA -> KhatmaWidget()
        DATE -> DateWidget()
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
        QIBLA -> loadQibla(context, config)
        KHATMA -> loadKhatma(context, config)
        DATE -> loadDate(context, config)
    }

    companion object {
        /** The kind of widget a launcher's provider class name belongs to. */
        fun ofProvider(className: String?): WidgetKind? = entries.firstOrNull { it.receiver.name == className }
    }
}
