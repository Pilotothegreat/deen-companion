package com.pilotothegreat.deencompanion.widget

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
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
import com.pilotothegreat.deencompanion.core.quran.KhatmaPlan
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.navigation.DeepLinks
import kotlinx.coroutines.flow.first
import java.time.LocalDate

internal data class KhatmaWidgetState(
    val dynamic: Boolean,
    val label: String,
    /** What is left to do today, or the offer to begin when there is no plan. */
    val headline: String,
    val detail: String,
    val fraction: Float,
    /** False when no khatma is running: no bar, and the tap goes to the Quran rather than a page. */
    val hasPlan: Boolean,
    val open: Intent,
) {
    companion object {
        suspend fun load(context: Context): KhatmaWidgetState {
            val settings = WidgetDeps.settings.current()
            val res = AppLanguage.localizedContext(context, settings.appLanguage)
            val locale = AppLanguage.locale(settings.appLanguage)
            val today = LocalDate.now(settings.zone)
            val progress = WidgetDeps.khatma.progress(today).first()
                ?: return KhatmaWidgetState(
                    dynamic = settings.dynamicColor,
                    label = res.getString(R.string.khatma),
                    headline = res.getString(R.string.khatma_start),
                    detail = res.getString(R.string.khatma_start_desc),
                    fraction = 0f,
                    hasPlan = false,
                    open = DeepLinks.screen(context, DeepLinks.QURAN),
                )
            val number = { value: Int -> Formatters.number(value, locale) }
            return KhatmaWidgetState(
                dynamic = settings.dynamicColor,
                label = res.getString(R.string.khatma),
                headline = when {
                    progress.isComplete -> res.getString(R.string.khatma_complete)
                    progress.isOnTrack -> res.getString(R.string.khatma_on_track, number(progress.pagesPerDay))
                    else -> res.resources.getQuantityString(R.plurals.khatma_pages_due, progress.pagesDueToday, number(progress.pagesDueToday))
                },
                detail = res.getString(
                    R.string.khatma_progress,
                    number(progress.pagesRead),
                    number(progress.pagesTotal),
                    number(progress.daysLeft),
                ),
                fraction = progress.fraction,
                hasPlan = true,
                // The next page to read, which is where someone opening this widget means to go.
                open = DeepLinks.reader(context, (progress.currentPage + 1).coerceIn(1, KhatmaPlan.PAGE_COUNT)),
            )
        }
    }
}

/** The reading plan: what today still asks for, and how far the mushaf has come. */
class KhatmaWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(WidgetKind.KHATMA.previewSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent(loadKhatma(context, configOf(context, id)))

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent(loadKhatma(context, WidgetConfig()))
}

internal suspend fun loadKhatma(context: Context, config: WidgetConfig): @Composable () -> Unit {
    val state = KhatmaWidgetState.load(context)
    return { BilalWidgetTheme(config.dynamicColor ?: state.dynamic) { KhatmaContent(state, config) } }
}

@Composable
internal fun KhatmaContent(state: KhatmaWidgetState, config: WidgetConfig = WidgetConfig()) {
    val colors = GlanceTheme.colors
    val content = colors.onPrimaryContainer
    val width = LocalSize.current.width
    val open = GlanceModifier.clickable(actionStartActivity(state.open))
    val budget = rememberBudget()
    val headlineSp = if (budget.left >= 90.dp) 18f else 16f
    // What is due today always; then the bar, then the count of pages behind it.
    budget.spend(budget.line(headlineSp))
    val showBar = state.hasPlan && budget.take(WidgetBar)
    val detailLines = (if (budget.takeLine(BODY_SP)) 1 else 0) + if (budget.takeLine(BODY_SP)) 1 else 0
    val showLabel = budget.takeLine(LABEL_SP)
    WidgetSurface(colors.primaryContainer, open, transparency = config.transparency) {
        Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    if (showLabel) Text(state.label, style = textStyle(content, LABEL_SP.sp, FontWeight.Medium), maxLines = 1)
                    Text(state.headline, style = textStyle(content, headlineSp.sp, FontWeight.Bold), maxLines = 2)
                    // Two lines where there is room: "Read the whole mushaf by a d…" is not an offer.
                    if (detailLines > 0) Text(state.detail, style = textStyle(content, BODY_SP.sp), maxLines = detailLines)
                }
                if (width >= BADGE_FROM) {
                    Spacer(GlanceModifier.width(10.dp))
                    ShapeIcon(MaterialShapes.Cookie9Sided, Icons.AutoMirrored.Rounded.MenuBook, 40.dp, colors.primary, colors.onPrimary)
                }
            }
            if (showBar) {
                Spacer(GlanceModifier.height(6.dp))
                LinearProgressIndicator(
                    progress = state.fraction,
                    modifier = GlanceModifier.fillMaxWidth(),
                    color = colors.primary,
                    backgroundColor = trackColor(content),
                )
            }
        }
    }
}

private const val LABEL_SP = 11f
private const val BODY_SP = 12f
private val BADGE_FROM = 170.dp
