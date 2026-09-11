package com.pilotothegreat.deencompanion.widget

import android.content.Context
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.calendar.HijriCalendar
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.text.Numerals
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.common.nameRes
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal data class TimesRow(val name: String, val time: String, val iqama: String?, val isNext: Boolean)

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
                    ?: DateTimeFormatter.ofPattern("EEEE d MMMM", locale).format(date),
                place = settings.location.cityName ?: res.getString(R.string.default_location),
                rows = Prayer.obligatory.map { prayer ->
                    val adhan = schedule.adhan.getValue(prayer)
                    TimesRow(
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

/** The day's prayer times as a table, the next prayer on a pill; wide sizes lay them out as columns. */
class PrayerTimesWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COLUMNS, LIST, LIST_WIDE))
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(LIST_WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) = show(context)

    override suspend fun providePreview(context: Context, widgetCategory: Int) = show(context)

    private suspend fun show(context: Context): Nothing {
        val state = PrayerTimesState.load(context)
        provideContent { BilalWidgetTheme(state.dynamic) { PrayerTimesContent(state) } }
    }

    internal companion object {
        val COLUMNS = DpSize(250.dp, 100.dp)
        val LIST = DpSize(150.dp, 190.dp)
        val LIST_WIDE = DpSize(250.dp, 200.dp)
    }
}

@Composable
internal fun PrayerTimesContent(state: PrayerTimesState) {
    val size = LocalSize.current
    val colors = GlanceTheme.colors
    val open = GlanceModifier.clickable(actionStartActivity(WidgetUpdater.openApp(LocalContext.current)))
    WidgetSurface(colors.widgetBackground, open) {
        if (size.height < PrayerTimesWidget.LIST.height) {
            Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Text("${state.title} · ${state.place}", style = textStyle(colors.onSurfaceVariant, 12.sp, FontWeight.Medium), maxLines = 1)
                Spacer(GlanceModifier.height(8.dp))
                Row(GlanceModifier.fillMaxWidth()) {
                    state.rows.forEach { row ->
                        val next = row.isNext
                        val color = if (next) colors.onSecondaryContainer else colors.onSurface
                        Column(
                            modifier = GlanceModifier.defaultWeight().then(pill(next)).padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(row.name, style = textStyle(color, 11.sp, align = TextAlign.Center), maxLines = 1)
                            Text(row.time, style = textStyle(color, 14.sp, FontWeight.Bold, TextAlign.Center), maxLines = 1)
                        }
                    }
                }
            }
        } else {
            val showIqama = size.width >= PrayerTimesWidget.LIST_WIDE.width
            Column(GlanceModifier.fillMaxSize()) {
                Text(state.title, style = textStyle(colors.onSurface, 14.sp, FontWeight.Bold), maxLines = 1)
                Text(state.place, style = textStyle(colors.onSurfaceVariant, 11.sp), maxLines = 1)
                Spacer(GlanceModifier.height(6.dp))
                state.rows.forEach { row ->
                    val next = row.isNext
                    val color = if (next) colors.onSecondaryContainer else colors.onSurface
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().then(pill(next)).padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            row.name,
                            GlanceModifier.defaultWeight(),
                            textStyle(color, 13.sp, if (next) FontWeight.Bold else FontWeight.Normal),
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
                        Text(row.time, style = textStyle(color, 13.sp, FontWeight.Bold), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun pill(selected: Boolean): GlanceModifier = if (selected) {
    GlanceModifier.background(ImageProvider(R.drawable.widget_pill), colorFilter = ColorFilter.tint(GlanceTheme.colors.secondaryContainer))
} else {
    GlanceModifier
}
