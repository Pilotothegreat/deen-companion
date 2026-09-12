package com.pilotothegreat.deencompanion.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
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
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import com.pilotothegreat.deencompanion.core.calendar.HijriCalendar
import com.pilotothegreat.deencompanion.core.moment.MomentEngine
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.common.Formatters
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZonedDateTime

internal data class MomentWidgetState(
    val dynamic: Boolean,
    val title: String,
    val body: String,
    /** False when nothing is happening, and the widget falls back to today's date. */
    val hasMoment: Boolean,
    val athkarCategory: String?,
) {
    companion object {
        suspend fun load(context: Context): MomentWidgetState {
            val settings = WidgetDeps.settings.current()
            val res = AppLanguage.localizedContext(context, settings.appLanguage)
            val locale = AppLanguage.locale(settings.appLanguage)
            val moment = MomentEngine.forWidget(WidgetDeps.moments.moments.first())
            if (moment == null) {
                // Nothing to say, so it says the date rather than a placeholder. A widget that
                // announces its own emptiness is worse than one that is quietly useful.
                val now = ZonedDateTime.now(settings.zone)
                val hijri = HijriCalendar.date(LocalDate.now(settings.zone), settings.hijriAdjustment)
                return MomentWidgetState(
                    dynamic = settings.dynamicColor,
                    title = hijri?.let { Formatters.hijri(it, locale) }.orEmpty(),
                    body = Formatters.date(now.toInstant().toEpochMilli(), settings.zone, locale),
                    hasMoment = false,
                    athkarCategory = null,
                )
            }
            val body = moment.body?.let { body ->
                if (moment.count != null) res.getString(body, Formatters.number(moment.count, locale)) else res.getString(body)
            }.orEmpty()
            return MomentWidgetState(
                dynamic = settings.dynamicColor,
                title = res.getString(moment.title),
                body = body,
                hasMoment = true,
                athkarCategory = moment.athkarCategory,
            )
        }
    }
}

/**
 * Whatever the engine ranks first: the odd night, iftar in an hour, the dua for the rain now
 * falling. It is the visible payoff of the context engine — one widget that becomes a different
 * widget as the day and the world move, instead of six more that each need placing.
 */
class MomentWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) = show(context, configOf(context, id))

    override suspend fun providePreview(context: Context, widgetCategory: Int) = show(context, WidgetConfig())

    private suspend fun show(context: Context, config: WidgetConfig): Nothing {
        val state = MomentWidgetState.load(context)
        provideContent { BilalWidgetTheme(config.dynamicColor ?: state.dynamic) { MomentContent(state, config) } }
    }

    internal companion object {
        val SMALL = DpSize(150.dp, 100.dp)
        val WIDE = DpSize(250.dp, 100.dp)
    }
}

@Composable
internal fun MomentContent(state: MomentWidgetState, config: WidgetConfig = WidgetConfig()) {
    val colors = GlanceTheme.colors
    val context = LocalContext.current
    val content = if (state.hasMoment) colors.onPrimaryContainer else colors.onSurfaceVariant
    val surface = if (state.hasMoment) colors.primaryContainer else colors.surfaceVariant
    val wide = LocalSize.current.width >= MomentWidget.WIDE.width
    val open = GlanceModifier.clickable(
        actionStartActivity(
            state.athkarCategory
                ?.let { com.pilotothegreat.deencompanion.ui.navigation.DeepLinks.athkar(context, it) }
                ?: WidgetUpdater.openApp(context),
        ),
    )
    WidgetSurface(surface, open, transparency = config.transparency) {
        Column(GlanceModifier.fillMaxSize()) {
            Text(state.title, style = textStyle(content, if (wide) 18.sp else 16.sp, FontWeight.Bold), maxLines = 2)
            if (state.body.isNotBlank()) {
                Spacer(GlanceModifier.height(4.dp))
                Text(state.body, style = textStyle(content, 12.sp), maxLines = if (wide) 3 else 4)
            }
        }
    }
}
