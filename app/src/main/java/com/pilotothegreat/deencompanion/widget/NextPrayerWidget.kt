package com.pilotothegreat.deencompanion.widget

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.Composable
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
import com.pilotothegreat.deencompanion.data.settings.AppSettings
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
            // The bar fills from one prayer to the next; RefreshPlan redraws it through the gap.
            val previous = previousAdhan(settings, now)
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

/** The last obligatory prayer's adhan at or before [now], reaching back to yesterday's Isha after midnight. */
internal fun previousAdhan(settings: AppSettings, now: ZonedDateTime): ZonedDateTime? {
    val today = DaySchedule.forDate(now.toLocalDate(), settings.prayerConfig)
    val yesterday = DaySchedule.forDate(now.toLocalDate().minusDays(1), settings.prayerConfig)
    return (Prayer.obligatory.mapNotNull { today.adhan[it] } + listOfNotNull(yesterday.adhan[Prayer.ISHA]))
        .filter { !it.isAfter(now) }
        .maxOrNull()
}

/**
 * The next prayer, designed like the Today card: label, name, live countdown, adhan and iqama, and
 * the prayer's shape. With room, the day's five times.
 */
class NextPrayerWidget : GlanceAppWidget() {
    // Laid out for the size it is given, not the nearest of a few declared sizes: a One UI stack
    // gives less height than any bucket, and the bucket's layout was cropped from the bottom.
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(WidgetKind.NEXT_PRAYER.previewSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent(loadNextPrayer(context, configOf(context, id)))

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent(loadNextPrayer(context, WidgetConfig()))

    internal companion object {
        /** Too short for the card, from this width the name and the countdown share one line; narrower, the name goes above. */
        val ONE_LINE_FROM = 180.dp

        /** From this width the prayer's shape sits beside the countdown. */
        val BADGE_FROM = 150.dp

        /** From this width the day's five times fit in a strip. */
        val STRIP_FROM = 220.dp
    }
}

internal suspend fun loadNextPrayer(context: Context, config: WidgetConfig): @Composable () -> Unit {
    val state = NextPrayerState.load(context)
    return { BilalWidgetTheme(config.dynamicColor ?: state.dynamic) { NextPrayerContent(state, config) } }
}

@Composable
internal fun NextPrayerContent(state: NextPrayerState, config: WidgetConfig = WidgetConfig()) {
    val size = LocalSize.current
    val colors = GlanceTheme.colors
    val content = colors.onPrimaryContainer
    val open = GlanceModifier.clickable(actionStartActivity(WidgetUpdater.openApp(LocalContext.current)))
    WidgetSurface(colors.primaryContainer, open, transparency = config.transparency) {
        val budget = rememberBudget()
        val roomy = budget.left >= 150.dp
        val nameSp = if (roomy) 20f else 18f
        val countdownSp = if (roomy) 30f else 24f
        // The name, the countdown and the bar make the card; when even those three do not fit, a one-line form.
        val essentials = budget.line(nameSp) + budget.line(countdownSp) + WidgetBar
        if (budget.left < essentials && size.width >= NextPrayerWidget.ONE_LINE_FROM) {
            // The name inside the chronometer's own text: side by side, the countdown's view took the whole row
            // and the name was squeezed to nothing. One line of text shares one baseline and cannot lose half.
            Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Countdown(state.countdownTarget, 20f, state.dynamic, format = "${state.name.replace("%", "%%")}  %s")
            }
        } else if (budget.left < essentials) {
            // Too narrow for "Maghrib 2:59:59" on a line: the name small above a countdown as large as the height allows.
            val showName = budget.takeLine(11f)
            val fontScale = LocalContext.current.resources.configuration.fontScale
            val countdownSp = (budget.left.value / (COUNTDOWN_LINE * fontScale)).coerceIn(12f, 20f)
            Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                if (showName) Text(state.name, style = textStyle(content, 11.sp, FontWeight.Bold), maxLines = 1)
                Countdown(state.countdownTarget, countdownSp, state.dynamic)
            }
        } else {
            // The card's three essentials, then the rest, most useful first, while it fits.
            budget.spend(essentials)
            val showAdhan = budget.takeLine(12f)
            val showStrip = size.width >= NextPrayerWidget.STRIP_FROM &&
                budget.take(STRIP_GAP + budget.line(11f) + budget.line(13f) + CELL_PADDING)
            val showIqama = config.showIqama && state.iqama != null && budget.takeLine(12f)
            val showLabel = budget.takeLine(11f)
            Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(GlanceModifier.defaultWeight()) {
                        if (showLabel) Text(state.label, style = textStyle(content, 11.sp, FontWeight.Medium), maxLines = 1)
                        Text(state.name, style = textStyle(content, nameSp.sp, FontWeight.Bold), maxLines = 1)
                        Countdown(state.countdownTarget, countdownSp, state.dynamic)
                        if (showAdhan) Text(state.adhan, style = textStyle(content, 12.sp), maxLines = 1)
                        if (showIqama) Text(state.iqama, style = textStyle(content, 12.sp), maxLines = 1)
                        Spacer(GlanceModifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = state.elapsed,
                            modifier = GlanceModifier.fillMaxWidth(),
                            color = colors.primary,
                            backgroundColor = trackColor(content),
                        )
                    }
                    // Each prayer has its own shape, the same language as the Today card.
                    if (size.width >= NextPrayerWidget.BADGE_FROM) {
                        Spacer(GlanceModifier.width(8.dp))
                        PrayerBadge(state.prayer, if (roomy && size.width >= 200.dp) 56.dp else 44.dp)
                    }
                }
                if (showStrip) {
                    Spacer(GlanceModifier.height(STRIP_GAP))
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
                GlanceModifier.background(ImageProvider(R.drawable.widget_cell), colorFilter = ColorFilter.tint(colors.primary))
            } else {
                GlanceModifier
            }
            val color = if (cell.isNext) colors.onPrimary else content
            Column(
                modifier = GlanceModifier.defaultWeight().then(pill).padding(vertical = CELL_PADDING / 2),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(cell.name, style = textStyle(color, 11.sp, align = TextAlign.Center), maxLines = 1)
                Text(cell.time, style = textStyle(color, 13.sp, FontWeight.Bold, TextAlign.Center), maxLines = 1)
            }
        }
    }
}

private val STRIP_GAP = 10.dp

/** The countdown's line height to its size; it has no font padding, so barely taller than the text. */
private const val COUNTDOWN_LINE = 1.17f
private val CELL_PADDING = 10.dp
