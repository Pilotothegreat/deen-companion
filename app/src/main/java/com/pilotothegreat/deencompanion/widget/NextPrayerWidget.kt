package com.pilotothegreat.deencompanion.widget

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.nameRes
import java.time.Duration
import java.time.ZonedDateTime

/** One prayer in the strip of the day's times. */
internal data class TimeCell(val name: String, val time: String, val isNext: Boolean)

internal data class NextPrayerState(
    val dynamic: Boolean,
    val prayer: Prayer,
    val label: String,
    val name: String,
    /** elapsedRealtime at which the countdown reaches zero. */
    val countdownTarget: Long,
    /** How far the day has come between the last prayer and the next, 0f..1f. */
    val elapsed: Float,
    val adhan: String,
    val iqama: String?,
    val day: List<TimeCell>,
) {
    companion object {
        suspend fun load(context: Context): NextPrayerState {
            val settings = WidgetDeps.settings.current()
            val res = AppLanguage.localizedContext(context, settings.appLanguage)
            val locale = AppLanguage.locale(settings.appLanguage)
            val now = ZonedDateTime.now(settings.zone)
            val next = DaySchedule.next(now, settings.prayerConfig)
            val schedule = DaySchedule.forDate(next.adhan.toLocalDate(), settings.prayerConfig)
            val iqama = next.iqama?.takeIf { it != next.adhan }
            // The arc fills from one prayer to the next. It advances on every redraw, and the
            // widget is redrawn at each prayer and on unlock, so it is never far behind.
            val today = DaySchedule.forDate(now.toLocalDate(), settings.prayerConfig)
            val yesterday = DaySchedule.forDate(now.toLocalDate().minusDays(1), settings.prayerConfig)
            val previous = (Prayer.obligatory.mapNotNull { today.adhan[it] } + listOfNotNull(yesterday.adhan[Prayer.ISHA]))
                .filter { !it.isAfter(now) }
                .maxOrNull()
            return NextPrayerState(
                dynamic = settings.dynamicColor,
                prayer = next.prayer,
                label = res.getString(R.string.widget_next_prayer_title),
                name = res.getString(next.prayer.nameRes),
                countdownTarget = SystemClock.elapsedRealtime() + Duration.between(now, next.adhan).toMillis(),
                elapsed = previous?.let {
                    val whole = Duration.between(it, next.adhan).toMillis().toFloat()
                    if (whole <= 0f) 0f else (Duration.between(it, now).toMillis() / whole).coerceIn(0f, 1f)
                } ?: 0f,
                adhan = res.getString(R.string.adhan_at, Formatters.time(context, next.adhan.toLocalTime(), locale)),
                iqama = iqama?.let { res.getString(R.string.iqama_at, Formatters.time(context, it.toLocalTime(), locale)) },
                day = Prayer.obligatory.map { prayer ->
                    TimeCell(
                        name = res.getString(prayer.nameRes),
                        time = shortTime(context, schedule.adhan.getValue(prayer).toLocalTime(), locale),
                        isNext = prayer == next.prayer,
                    )
                },
            )
        }
    }
}

/**
 * The next prayer, designed like the Today card: label, name, live countdown, adhan and iqama, and
 * the prayer's shape. Larger sizes add the day's five times.
 */
class NextPrayerWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT, SMALL, CARD, WIDE, FULL))
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) = show(context, configOf(context, id))

    override suspend fun providePreview(context: Context, widgetCategory: Int) = show(context, WidgetConfig())

    private suspend fun show(context: Context, config: WidgetConfig): Nothing {
        val state = NextPrayerState.load(context)
        provideContent { BilalWidgetTheme(config.dynamicColor ?: state.dynamic) { NextPrayerContent(state, config) } }
    }

    internal companion object {
        val COMPACT = DpSize(100.dp, 48.dp)
        val SMALL = DpSize(120.dp, 100.dp)
        val CARD = DpSize(130.dp, 140.dp)
        val WIDE = DpSize(200.dp, 140.dp)
        val FULL = DpSize(250.dp, 200.dp)
    }
}

@Composable
internal fun NextPrayerContent(state: NextPrayerState, config: WidgetConfig = WidgetConfig()) {
    val size = LocalSize.current
    val colors = GlanceTheme.colors
    val content = colors.onPrimaryContainer
    val open = GlanceModifier.clickable(actionStartActivity(WidgetUpdater.openApp(LocalContext.current)))
    WidgetSurface(colors.primaryContainer, open, transparency = config.transparency) {
        when {
            size.height < NextPrayerWidget.SMALL.height -> Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(state.name, GlanceModifier.defaultWeight(), textStyle(content, 17.sp, FontWeight.Bold), maxLines = 1)
                Countdown(state.countdownTarget, 20f, state.dynamic)
            }
            size.height < NextPrayerWidget.CARD.height -> Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(state.name, style = textStyle(content, 18.sp, FontWeight.Bold), maxLines = 1)
                Countdown(state.countdownTarget, 24f, state.dynamic)
                Text(state.adhan, style = textStyle(content, 12.sp), maxLines = 1)
                Spacer(GlanceModifier.height(6.dp))
                LinearProgressIndicator(
                    progress = state.elapsed,
                    modifier = GlanceModifier.fillMaxWidth(),
                    color = colors.primary,
                    backgroundColor = colors.surfaceVariant,
                )
            }
            else -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(GlanceModifier.defaultWeight()) {
                        Text(state.label, style = textStyle(content, 11.sp, FontWeight.Medium), maxLines = 1)
                        Text(state.name, style = textStyle(content, 20.sp, FontWeight.Bold), maxLines = 1)
                        Countdown(state.countdownTarget, if (size.width >= NextPrayerWidget.WIDE.width) 30f else 26f, state.dynamic)
                        Text(state.adhan, style = textStyle(content, 12.sp), maxLines = 1)
                        if (config.showIqama) state.iqama?.let { Text(it, style = textStyle(content, 12.sp), maxLines = 1) }
                        Spacer(GlanceModifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = state.elapsed,
                            modifier = GlanceModifier.fillMaxWidth(),
                            color = colors.primary,
                            backgroundColor = colors.surfaceVariant,
                        )
                    }
                    // Each prayer has its own shape, and it changes as the day moves — the same
                    // language as the Today card, drawn as a bitmap so it looks identical on One UI,
                    // Pixel and everything else.
                    if (size.width >= NextPrayerWidget.CARD.width) {
                        Spacer(GlanceModifier.width(8.dp))
                        PrayerBadge(state.prayer, if (size.width >= NextPrayerWidget.WIDE.width) 64.dp else 44.dp)
                    }
                }
                if (size.height >= NextPrayerWidget.FULL.height && size.width >= NextPrayerWidget.FULL.width) {
                    Spacer(GlanceModifier.height(12.dp))
                    TimeStrip(state.day, content)
                }
            }
        }
    }
}

/** The day's five times; the next one sits on a pill. */
@Composable
private fun TimeStrip(cells: List<TimeCell>, content: ColorProvider) {
    val colors = GlanceTheme.colors
    Row(GlanceModifier.fillMaxWidth()) {
        cells.forEach { cell ->
            val pill = if (cell.isNext) {
                GlanceModifier.background(ImageProvider(R.drawable.widget_pill), colorFilter = ColorFilter.tint(colors.primary))
            } else {
                GlanceModifier
            }
            val color = if (cell.isNext) colors.onPrimary else content
            Column(
                modifier = GlanceModifier.defaultWeight().then(pill).padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(cell.name, style = textStyle(color, 11.sp, align = TextAlign.Center), maxLines = 1)
                Text(cell.time, style = textStyle(color, 13.sp, FontWeight.Bold, TextAlign.Center), maxLines = 1)
            }
        }
    }
}
