package com.pilotothegreat.deencompanion.widget

import android.content.Context
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.tasbih.TasbihEngine
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.labelRes
import kotlinx.coroutines.flow.first

internal data class TasbihWidgetState(
    val dynamic: Boolean,
    val dhikr: String,
    val count: String,
    val target: String,
    val countAction: String,
) {
    companion object {
        suspend fun load(context: Context): TasbihWidgetState {
            val settings = WidgetDeps.settings.current()
            val res = AppLanguage.localizedContext(context, settings.appLanguage)
            val locale = AppLanguage.locale(settings.appLanguage)
            val state = WidgetDeps.tasbih.state.first()
            return TasbihWidgetState(
                dynamic = settings.dynamicColor,
                dhikr = res.getString(state.dhikr.labelRes),
                count = Formatters.number(state.count, locale),
                target = res.getString(R.string.tasbih_of_target, Formatters.number(TasbihEngine.roundTarget(state), locale)),
                countAction = res.getString(R.string.cd_tasbih_button),
            )
        }
    }
}

/** Counts dhikr from the home screen with the same rules and storage as the app. */
class TasbihWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(WidgetKind.TASBIH.previewSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent(loadTasbih(context, configOf(context, id)))

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent(loadTasbih(context, WidgetConfig()))

    internal companion object {
        /** From this width the dhikr and its target sit beside the count. */
        val WIDE_FROM = 170.dp
    }
}

internal suspend fun loadTasbih(context: Context, config: WidgetConfig): @Composable () -> Unit {
    val state = TasbihWidgetState.load(context)
    return { BilalWidgetTheme(config.dynamicColor ?: state.dynamic) { TasbihContent(state, config) } }
}

/** The +1 button: counts, then redraws this widget. */
class TasbihIncrementAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetDeps.tasbih.increment()
        TasbihWidget().update(context, glanceId)
    }
}

@Composable
internal fun TasbihContent(state: TasbihWidgetState, config: WidgetConfig = WidgetConfig()) {
    val colors = GlanceTheme.colors
    val content = colors.onSecondaryContainer
    val size = LocalSize.current
    val inner = size.height - WidgetPadding * 2
    val open = GlanceModifier.clickable(actionStartActivity(WidgetUpdater.openApp(LocalContext.current)))
    WidgetSurface(colors.secondaryContainer, open, transparency = config.transparency) {
        val budget = rememberBudget()
        if (size.width >= TasbihWidget.WIDE_FROM) {
            val countSp = if (budget.left >= budget.line(34f) + budget.line(13f)) 34f else 26f
            budget.spend(budget.line(countSp))
            val showDhikr = budget.takeLine(13f)
            val showTarget = budget.takeLine(12f)
            Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    if (showDhikr) Text(state.dhikr, style = textStyle(content, 13.sp, FontWeight.Medium), maxLines = 1)
                    Text(state.count, style = textStyle(content, countSp.sp, FontWeight.Bold), maxLines = 1)
                    if (showTarget) Text(state.target, style = textStyle(content, 12.sp), maxLines = 1)
                }
                CountButton(state.countAction, min(56.dp, inner))
            }
        } else if (budget.take(budget.line(24f) + 4.dp + 40.dp)) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.count, style = textStyle(content, 24.sp, FontWeight.Bold, TextAlign.Center), maxLines = 1)
                Spacer(GlanceModifier.height(4.dp))
                CountButton(state.countAction, 40.dp)
            }
        } else {
            // Too short to stack them: the count and the button side by side.
            Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Text(state.count, GlanceModifier.defaultWeight(), textStyle(content, 20.sp, FontWeight.Bold), maxLines = 1)
                Spacer(GlanceModifier.width(4.dp))
                CountButton(state.countAction, min(36.dp, inner))
            }
        }
    }
}

/** The count button, in a scalloped shape that reads as something to press. */
@Composable
private fun CountButton(description: String, size: Dp) {
    val colors = GlanceTheme.colors
    ShapeBox(
        polygon = MaterialShapes.Cookie12Sided,
        size = size,
        color = colors.primary,
        modifier = GlanceModifier.clickable(actionRunCallback<TasbihIncrementAction>()).semantics { contentDescription = description },
    ) {
        Text(
            LocalContext.current.getString(R.string.tasbih_plus_one),
            style = textStyle(colors.onPrimary, if (size >= 56.dp) 18.sp else 14.sp, FontWeight.Bold, TextAlign.Center),
        )
    }
}
