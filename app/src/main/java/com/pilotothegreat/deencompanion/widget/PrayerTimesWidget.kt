package com.pilotothegreat.deencompanion.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
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
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.calendar.HijriCalendar
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.text.Numerals
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.nameRes
import java.time.ZonedDateTime

internal data class TimesRow(val prayer: Prayer, val name: String, val time: String, val iqama: String?, val isNext: Boolean)

internal data class PrayerTimesState(val dynamic: Boolean, val title: String, val place: String, val rows: List<TimesRow>) {
    companion object {
        suspend fun load(context: Context): PrayerTimesState {
            val settings = WidgetDeps.settings.current()
            val res = AppLanguage.localizedContext(context, settings.appLanguage)
            val locale = AppLanguage.locale(settings.appLanguage)
            val next = DaySchedule.next(ZonedDateTime.now(settings.zone), settings.prayerConfig)
            // After Isha this shows tomorrow, with Fajr highlighted.
            val date = next.adhan.toLocalDate()
            val schedule = DaySchedule.forDate(date, settings.prayerConfig)
            val hijri = HijriCalendar.date(date, settings.hijriAdjustment)
            return PrayerTimesState(
                dynamic = settings.dynamicColor,
                title = hijri?.let { res.getString(R.string.hijri_date, Numerals.localize(HijriCalendar.format(it, locale), locale)) }
                    // Through Formatters so the widget's fallback date carries the locale's own
                    // digits, which ofPattern alone does not do.
                    ?: Formatters.pattern("EEEE d MMMM", locale).format(date),
                place = settings.location.cityName ?: res.getString(R.string.default_location),
                rows = Prayer.obligatory.map { prayer ->
                    val adhan = schedule.adhan.getValue(prayer)
                    TimesRow(
                        prayer = prayer,
                        name = res.getString(prayer.nameRes),
                        time = shortTime(context, adhan.toLocalTime(), locale),
                        iqama = schedule.iqama[prayer]?.takeIf { it != adhan }?.let { shortTime(context, it.toLocalTime(), locale) },
                        isNext = prayer == next.prayer,
                    )
                },
            )
        }
    }
}

/**
 * The day's five prayers, the next on a pill. Five prayers are five prayers at every size: a tall widget
 * lists them, a short one sets them side by side, and a small one keeps the times without the names —
 * never a list with Isha cut off the bottom.
 */
class PrayerTimesWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(WidgetKind.PRAYER_TIMES.previewSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent(loadPrayerTimes(context, configOf(context, id)))

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent(loadPrayerTimes(context, WidgetConfig()))

    internal companion object {
        /** From this width a list has room for each prayer's iqama beside its adhan. */
        val IQAMA_FROM = 250.dp

        /** A strip cell narrower than this cannot hold "Maghrib"; the names then take a second row. */
        val NAMED_CELL = 44.dp
    }
}

internal suspend fun loadPrayerTimes(context: Context, config: WidgetConfig): @Composable () -> Unit {
    val state = PrayerTimesState.load(context)
    return { BilalWidgetTheme(config.dynamicColor ?: state.dynamic) { PrayerTimesContent(state, config) } }
}

@Composable
internal fun PrayerTimesContent(state: PrayerTimesState, config: WidgetConfig = WidgetConfig()) {
    val size = LocalSize.current
    val colors = GlanceTheme.colors
    val open = GlanceModifier.clickable(actionStartActivity(WidgetUpdater.openApp(LocalContext.current)))
    WidgetSurface(colors.widgetBackground, open, transparency = config.transparency) {
        val budget = rememberBudget()
        val list = budget.line(TITLE_SP) + (budget.line(ROW_SP) + ROW_PADDING) * 5
        if (budget.take(list)) {
            TimesList(
                state = state,
                showPlace = budget.takeLine(PLACE_SP),
                showIqama = config.showIqama && size.width >= PrayerTimesWidget.IQAMA_FROM,
            )
        } else {
            TimesStrip(state, budget, size.width)
        }
    }
}

@Composable
private fun TimesList(state: PrayerTimesState, showPlace: Boolean, showIqama: Boolean) {
    val colors = GlanceTheme.colors
    Column(GlanceModifier.fillMaxSize()) {
        Text(state.title, style = textStyle(colors.onSurface, TITLE_SP.sp, FontWeight.Bold), maxLines = 1)
        if (showPlace) Text(state.place, style = textStyle(colors.onSurfaceVariant, PLACE_SP.sp), maxLines = 1)
        // The rows share whatever height there is, so a short widget tightens them rather than losing one.
        state.rows.forEach { row ->
            val next = row.isNext
            val color = if (next) colors.onSecondaryContainer else colors.onSurface
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight().then(pill(next)).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (next) {
                    PrayerBadge(row.prayer, 16.dp)
                    Spacer(GlanceModifier.width(8.dp))
                }
                Text(
                    row.name,
                    GlanceModifier.defaultWeight(),
                    textStyle(color, ROW_SP.sp, if (next) FontWeight.Bold else FontWeight.Normal),
                    maxLines = 1,
                )
                if (showIqama && row.iqama != null) {
                    Text(
                        row.iqama,
                        GlanceModifier.padding(end = 10.dp),
                        textStyle(if (next) color else colors.onSurfaceVariant, 11.sp),
                        maxLines = 1,
                    )
                }
                Text(row.time, style = textStyle(color, ROW_SP.sp, FontWeight.Bold), maxLines = 1)
            }
        }
    }
}

/** The five prayers across: under a one-line header when there is room, in two rows when it is narrow. */
@Composable
private fun TimesStrip(state: PrayerTimesState, budget: HeightBudget, width: Dp) {
    val colors = GlanceTheme.colors
    val named = (width - WidgetPadding * 2) / 5 >= PrayerTimesWidget.NAMED_CELL
    val namedCell = budget.line(NAME_SP) + budget.line(TIME_SP) + CELL_PADDING
    Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        when {
            named && budget.take(namedCell) -> {
                if (budget.take(budget.line(HEADER_SP) + STRIP_GAP)) {
                    Text("${state.title} · ${state.place}", style = textStyle(colors.onSurfaceVariant, HEADER_SP.sp, FontWeight.Medium), maxLines = 1)
                    Spacer(GlanceModifier.height(STRIP_GAP))
                }
                Row(GlanceModifier.fillMaxWidth()) { state.rows.forEach { StripCell(it, named = true) } }
            }
            !named && budget.take(namedCell * 2 + ROW_GAP) -> {
                Row(GlanceModifier.fillMaxWidth()) { state.rows.take(3).forEach { StripCell(it, named = true) } }
                Spacer(GlanceModifier.height(ROW_GAP))
                Row(GlanceModifier.fillMaxWidth()) {
                    state.rows.drop(3).forEach { StripCell(it, named = true) }
                    Spacer(GlanceModifier.defaultWeight())
                }
            }
            else -> {
                // Five times in a small widget: the size follows the cell, so "12:09" is never cut to "12:…".
                val cell = (width - WidgetPadding * 2) / 5
                val fontScale = LocalContext.current.resources.configuration.fontScale
                val timeSp = minOf(TIME_SP, cell.value / (TIME_EMS * fontScale))
                Row(GlanceModifier.fillMaxWidth()) { state.rows.forEach { StripCell(it, named = false, timeSp) } }
            }
        }
    }
}

@Composable
private fun RowScope.StripCell(row: TimesRow, named: Boolean, timeSp: Float = TIME_SP) {
    val colors = GlanceTheme.colors
    val color = if (row.isNext) colors.onSecondaryContainer else colors.onSurface
    Column(
        modifier = GlanceModifier.defaultWeight().then(pill(row.isNext)).padding(vertical = CELL_PADDING / 2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (named) Text(row.name, style = textStyle(color, NAME_SP.sp, align = TextAlign.Center), maxLines = 1)
        Text(row.time, style = textStyle(color, timeSp.sp, FontWeight.Bold, TextAlign.Center), maxLines = 1)
    }
}

@Composable
private fun pill(selected: Boolean): GlanceModifier = if (selected) {
    GlanceModifier.background(ImageProvider(R.drawable.widget_pill), colorFilter = ColorFilter.tint(GlanceTheme.colors.secondaryContainer))
} else {
    GlanceModifier
}

private const val TITLE_SP = 14f
private const val PLACE_SP = 11f
private const val ROW_SP = 13f
private const val HEADER_SP = 12f
private const val NAME_SP = 11f
private const val TIME_SP = 14f

/** How many ems the widest short time, a bold "12:09", takes, with a little room to spare. */
private const val TIME_EMS = 2.7f
private val ROW_PADDING = 4.dp
private val CELL_PADDING = 8.dp
private val STRIP_GAP = 6.dp
private val ROW_GAP = 4.dp
