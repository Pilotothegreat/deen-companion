package com.pilotothegreat.deencompanion.widget

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.calendar.HijriCalendar
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.common.Formatters
import java.time.LocalDate

internal data class DateWidgetState(
    val dynamic: Boolean,
    /** "29 Rabi' I 1448 AH", the date this app is keeping. */
    val hijri: String,
    val weekday: String,
    val gregorian: String,
) {
    companion object {
        suspend fun load(context: Context): DateWidgetState {
            val settings = WidgetDeps.settings.current()
            val res = AppLanguage.localizedContext(context, settings.appLanguage)
            val locale = AppLanguage.locale(settings.appLanguage)
            val today = LocalDate.now(settings.zone)
            val hijri = HijriCalendar.date(today, settings.hijriAdjustment)
            return DateWidgetState(
                dynamic = settings.dynamicColor,
                hijri = hijri?.let { res.getString(R.string.hijri_date, Formatters.hijri(it, locale)) }.orEmpty(),
                weekday = Formatters.pattern("EEEE", locale).format(today),
                gregorian = Formatters.pattern("d MMMM y", locale).format(today),
            )
        }
    }
}

/**
 * Today in both calendars, the Hijri date first.
 *
 * The Hijri day turns over at Maghrib in this app, so the date on the home screen and the date in
 * the app are the same date all evening, which is the whole reason to place it.
 */
class DateWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(WidgetKind.DATE.previewSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent(loadDate(context, configOf(context, id)))

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent(loadDate(context, WidgetConfig()))
}

internal suspend fun loadDate(context: Context, config: WidgetConfig): @Composable () -> Unit {
    val state = DateWidgetState.load(context)
    return { BilalWidgetTheme(config.dynamicColor ?: state.dynamic) { DateContent(state, config) } }
}

@Composable
internal fun DateContent(state: DateWidgetState, config: WidgetConfig = WidgetConfig()) {
    val colors = GlanceTheme.colors
    val content = colors.onTertiaryContainer
    val width = LocalSize.current.width
    val open = GlanceModifier.clickable(actionStartActivity(WidgetUpdater.openApp(LocalContext.current)))
    val budget = rememberBudget()
    // Below this width the Hijri date wraps and leaves "AH" alone on a line of its own.
    val hijriSp = if (budget.left >= 80.dp && width >= 230.dp) 20f else 16f
    budget.spend(budget.line(hijriSp))
    val showGregorian = budget.takeLine(BODY_SP)
    val showWeekday = budget.takeLine(BODY_SP)
    WidgetSurface(colors.tertiaryContainer, open, transparency = config.transparency) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                if (showWeekday) Text(state.weekday, style = textStyle(content, BODY_SP.sp, FontWeight.Medium), maxLines = 1)
                Text(state.hijri, style = textStyle(content, hijriSp.sp, FontWeight.Bold), maxLines = 2)
                if (showGregorian) Text(state.gregorian, style = textStyle(content, BODY_SP.sp), maxLines = 1)
            }
            if (width >= BADGE_FROM) {
                Spacer(GlanceModifier.width(10.dp))
                ShapeIcon(MaterialShapes.Clover4Leaf, Icons.Rounded.CalendarMonth, 40.dp, colors.tertiary, colors.onTertiary)
            }
        }
    }
}

private const val BODY_SP = 12f
private val BADGE_FROM = 190.dp
