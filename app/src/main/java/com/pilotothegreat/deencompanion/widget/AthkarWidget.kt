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
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.athkar.AthkarIds
import com.pilotothegreat.deencompanion.core.athkar.AthkarSchedule
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
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
            val now = ZonedDateTime.now(settings.zone)
            val today = now.toLocalDate()
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
                meters = listOf(AthkarIds.MORNING, AthkarIds.EVENING).mapNotNull { id ->
                    library.category(id)?.let { AthkarMeter(it.title(locale), progress.fraction(it)) }
                },
            )
        }
    }
}

/** The athkar for this time of day with today's progress; tapping starts the session. */
class AthkarWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(TINY, SMALL, TALL, LARGE))
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) = show(context, configOf(context, id))

    override suspend fun providePreview(context: Context, widgetCategory: Int) = show(context, WidgetConfig())

    private suspend fun show(context: Context, config: WidgetConfig): Nothing {
        val state = AthkarWidgetState.load(context, config)
        provideContent { BilalWidgetTheme(config.dynamicColor ?: state.dynamic) { AthkarContent(state, config) } }
    }

    internal companion object {
        val TINY = DpSize(110.dp, 70.dp)
        val SMALL = DpSize(150.dp, 100.dp)
        val TALL = DpSize(180.dp, 180.dp)
        val LARGE = DpSize(280.dp, 260.dp)
    }
}

@Composable
internal fun AthkarContent(state: AthkarWidgetState, config: WidgetConfig = WidgetConfig()) {
    val colors = GlanceTheme.colors
    val content = colors.onTertiaryContainer
    val tall = LocalSize.current.height >= AthkarWidget.TALL.height
    val open = GlanceModifier.clickable(actionStartActivity(DeepLinks.athkar(LocalContext.current, state.categoryId)))
    WidgetSurface(colors.tertiaryContainer, open, transparency = config.transparency) {
        Column(GlanceModifier.fillMaxSize()) {
            Text(state.label, style = textStyle(content, 11.sp, FontWeight.Medium), maxLines = 1)
            Text(state.title, style = textStyle(content, 18.sp, FontWeight.Bold), maxLines = 1)
            Text(state.status, style = textStyle(content, 12.sp), maxLines = 1)
            Spacer(GlanceModifier.height(8.dp))
            LinearProgressIndicator(
                progress = state.fraction,
                modifier = GlanceModifier.fillMaxWidth(),
                color = colors.tertiary,
                backgroundColor = colors.surfaceVariant,
            )
            if (tall) {
                state.meters.forEach { meter ->
                    Spacer(GlanceModifier.height(10.dp))
                    Text(meter.title, style = textStyle(content, 12.sp), maxLines = 1)
                    Spacer(GlanceModifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = meter.fraction,
                        modifier = GlanceModifier.fillMaxWidth(),
                        color = colors.tertiary,
                        backgroundColor = colors.surfaceVariant,
                    )
                }
            }
        }
    }
}
