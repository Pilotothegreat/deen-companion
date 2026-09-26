package com.pilotothegreat.deencompanion.widget

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import com.pilotothegreat.deencompanion.core.calendar.HijriCalendar
import com.pilotothegreat.deencompanion.core.moment.MomentEngine
import com.pilotothegreat.deencompanion.core.moment.MomentKind
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.navigation.DeepLinks
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
    /** What kind of moment, for its icon; null for the date. */
    val kind: MomentKind? = null,
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
                kind = moment.kind,
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
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(WidgetKind.MOMENT.previewSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent(loadMoment(context, configOf(context, id)))

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent(loadMoment(context, WidgetConfig()))
}

internal suspend fun loadMoment(context: Context, config: WidgetConfig): @Composable () -> Unit {
    val state = MomentWidgetState.load(context)
    return { BilalWidgetTheme(config.dynamicColor ?: state.dynamic) { MomentContent(state, config) } }
}

@Composable
internal fun MomentContent(state: MomentWidgetState, config: WidgetConfig = WidgetConfig()) {
    val colors = GlanceTheme.colors
    val context = LocalContext.current
    val content = if (state.hasMoment) colors.onPrimaryContainer else colors.onSurfaceVariant
    val surface = if (state.hasMoment) colors.primaryContainer else colors.surfaceVariant
    val width = LocalSize.current.width
    val titleSp = if (width >= WIDE_FROM) 18f else 16f
    val budget = rememberBudget()
    // The title always; a second line of it only if the body still gets one; the body with whatever is left.
    val titleLines = if (state.body.isNotBlank() && budget.left >= budget.line(titleSp) * 2 + GAP + budget.line(BODY_SP)) 2 else 1
    budget.spend(budget.line(titleSp) * titleLines)
    val bodyLines = if (state.body.isBlank()) 0 else ((budget.left - GAP) / budget.line(BODY_SP)).toInt().coerceIn(0, 4)
    val open = GlanceModifier.clickable(
        actionStartActivity(state.athkarCategory?.let { DeepLinks.athkar(context, it) } ?: WidgetUpdater.openApp(context)),
    )
    WidgetSurface(surface, open, transparency = config.transparency) {
        // Centred, so a short moment in a tall widget does not sit at the top above an empty half.
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (width >= BADGE_FROM) {
                ShapeIcon(
                    MaterialShapes.Clover4Leaf,
                    state.kind.icon,
                    36.dp,
                    if (state.hasMoment) colors.primary else colors.secondary,
                    if (state.hasMoment) colors.onPrimary else colors.onSecondary,
                )
                Spacer(GlanceModifier.width(10.dp))
            }
            Column(GlanceModifier.defaultWeight()) {
                Text(state.title, style = textStyle(content, titleSp.sp, FontWeight.Bold), maxLines = titleLines)
                if (bodyLines > 0) {
                    Spacer(GlanceModifier.height(GAP))
                    Text(state.body, style = textStyle(content, BODY_SP.sp), maxLines = bodyLines)
                }
            }
        }
    }
}

private val MomentKind?.icon: ImageVector
    get() = when (this) {
        MomentKind.OCCASION -> Icons.Rounded.NightsStay
        MomentKind.NATURE -> Icons.Rounded.Cloud
        MomentKind.TRAVEL -> Icons.Rounded.Flight
        MomentKind.PLAN -> Icons.AutoMirrored.Rounded.MenuBook
        MomentKind.MAINTENANCE -> Icons.Rounded.Build
        null -> Icons.Rounded.CalendarMonth
    }

private val WIDE_FROM = 250.dp
private val BADGE_FROM = 150.dp
private const val BODY_SP = 12f
private val GAP = 4.dp
