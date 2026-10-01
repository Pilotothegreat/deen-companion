package com.pilotothegreat.deencompanion.widget

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
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
import androidx.glance.appwidget.LinearProgressIndicator
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
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.athkar.AthkarIds
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.navigation.DeepLinks
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime

internal data class AthkarMeter(val title: String, val fraction: Float)

internal data class AthkarWidgetState(
    val dynamic: Boolean,
    val label: String,
    val categoryId: String,
    val title: String,
    val status: String,
    val fraction: Float,
    val meters: List<AthkarMeter>,
) {
    companion object {
        suspend fun load(context: Context, config: WidgetConfig = WidgetConfig()): AthkarWidgetState {
            val settings = WidgetDeps.settings.current()
            val res = AppLanguage.localizedContext(context, settings.appLanguage)
            val locale = AppLanguage.locale(settings.appLanguage)
            val today = ZonedDateTime.now(settings.zone).toLocalDate()
            val library = WidgetDeps.athkar.library()
            val progress = WidgetDeps.athkar.progress.first().on(today)
            // A pinned category wins: someone who placed this beside their bed wants the sleep
            // athkar at noon too.
            val suggested = config.pinnedAthkar ?: WidgetDeps.moments.suggestedAthkarNow()
            val category = library.category(suggested) ?: library.core.first()
            return AthkarWidgetState(
                dynamic = settings.dynamicColor,
                label = res.getString(R.string.athkar_now),
                categoryId = category.id,
                title = category.title(locale),
                status = if (progress.isComplete(category)) {
                    res.getString(R.string.athkar_done_today)
                } else {
                    res.getString(
                        R.string.athkar_progress,
                        Formatters.number(progress.completedItems(category), locale),
                        Formatters.number(category.items.size, locale),
                    )
                },
                fraction = progress.fraction(category),
                // Not the category already named above: the widget showed "Evening" twice, once as the
                // heading and once as a meter under it.
                meters = listOf(AthkarIds.MORNING, AthkarIds.EVENING).filterNot { it == category.id }.mapNotNull { id ->
                    library.category(id)?.let { AthkarMeter(it.title(locale), progress.fraction(it)) }
                },
            )
        }
    }
}

/** The athkar for this time of day with today's progress; tapping starts the session. */
class AthkarWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(WidgetKind.ATHKAR_NOW.previewSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideFresh(context, id, ::loadAthkar)

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent(loadAthkar(context, WidgetConfig()))
}

internal suspend fun loadAthkar(context: Context, config: WidgetConfig): @Composable () -> Unit {
    val state = AthkarWidgetState.load(context, config)
    return { BilalWidgetTheme(config.dynamicColor ?: state.dynamic) { AthkarContent(state, config) } }
}

@Composable
internal fun AthkarContent(state: AthkarWidgetState, config: WidgetConfig = WidgetConfig()) {
    val colors = GlanceTheme.colors
    val content = colors.onTertiaryContainer
    val width = LocalSize.current.width
    val open = GlanceModifier.clickable(actionStartActivity(DeepLinks.athkar(LocalContext.current, state.categoryId)))
    val budget = rememberBudget()
    // The category always; its bar, its progress, the label and the morning and evening meters while they fit.
    budget.spend(budget.line(18f))
    val showBar = budget.take(WidgetBar)
    val showStatus = budget.takeLine(12f)
    val showLabel = budget.takeLine(11f)
    val meters = state.meters.takeWhile { budget.take(METER_GAP + budget.line(12f) + WidgetBar) }
    WidgetSurface(colors.tertiaryContainer, open, transparency = config.transparency) {
        Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    if (showLabel) Text(state.label, style = textStyle(content, 11.sp, FontWeight.Medium), maxLines = 1)
                    Text(state.title, style = textStyle(content, 18.sp, FontWeight.Bold), maxLines = 1)
                    if (showStatus) Text(state.status, style = textStyle(content, 12.sp), maxLines = 1)
                }
                if (width >= BADGE_FROM) {
                    Spacer(GlanceModifier.width(8.dp))
                    ShapeIcon(
                        MaterialShapes.Sunny,
                        athkarIcon(state.categoryId),
                        if (showLabel && showStatus) 40.dp else 28.dp,
                        colors.tertiary,
                        colors.onTertiary,
                    )
                }
            }
            if (showBar) {
                Spacer(GlanceModifier.height(6.dp))
                LinearProgressIndicator(
                    progress = state.fraction,
                    modifier = GlanceModifier.fillMaxWidth(),
                    color = colors.tertiary,
                    backgroundColor = trackColor(content),
                )
            }
            meters.forEach { meter ->
                Spacer(GlanceModifier.height(METER_GAP))
                Text(meter.title, style = textStyle(content, 12.sp), maxLines = 1)
                Spacer(GlanceModifier.height(6.dp))
                LinearProgressIndicator(
                    progress = meter.fraction,
                    modifier = GlanceModifier.fillMaxWidth(),
                    color = colors.tertiary,
                    backgroundColor = trackColor(content),
                )
            }
        }
    }
}

private fun athkarIcon(categoryId: String): ImageVector = when (categoryId) {
    AthkarIds.MORNING -> Icons.Rounded.WbSunny
    AthkarIds.EVENING -> Icons.Rounded.NightsStay
    AthkarIds.SLEEP -> Icons.Rounded.Bedtime
    else -> Icons.Rounded.AutoAwesome
}

private val BADGE_FROM = 150.dp
private val METER_GAP = 8.dp
