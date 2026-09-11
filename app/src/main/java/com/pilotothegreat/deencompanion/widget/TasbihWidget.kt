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
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.compose.ui.unit.Dp
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
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(NARROW, WIDE))
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) = show(context)

    override suspend fun providePreview(context: Context, widgetCategory: Int) = show(context)

    private suspend fun show(context: Context): Nothing {
        val state = TasbihWidgetState.load(context)
        provideContent { BilalWidgetTheme(state.dynamic) { TasbihContent(state) } }
    }

    internal companion object {
        val NARROW = DpSize(110.dp, 100.dp)
        val WIDE = DpSize(170.dp, 100.dp)
    }
}

/** The +1 button: counts, then redraws this widget. */
class TasbihIncrementAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetDeps.tasbih.increment()
        TasbihWidget().update(context, glanceId)
    }
}

@Composable
internal fun TasbihContent(state: TasbihWidgetState) {
    val colors = GlanceTheme.colors
    val content = colors.onSecondaryContainer
    val wide = LocalSize.current.width >= TasbihWidget.WIDE.width
    val open = GlanceModifier.clickable(actionStartActivity(WidgetUpdater.openApp(LocalContext.current)))
    if (wide) {
        WidgetSurface(colors.secondaryContainer, open) {
            Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    Text(state.dhikr, style = textStyle(content, 13.sp, FontWeight.Medium), maxLines = 1)
                    Text(state.count, style = textStyle(content, 34.sp, FontWeight.Bold), maxLines = 1)
                    Text(state.target, style = textStyle(content, 12.sp), maxLines = 1)
                }
                CountButton(state.countAction, 56.dp)
            }
        }
    } else {
        WidgetSurface(colors.secondaryContainer, open, padding = 12.dp) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.count, style = textStyle(content, 24.sp, FontWeight.Bold, TextAlign.Center), maxLines = 1)
                Spacer(GlanceModifier.height(4.dp))
                CountButton(state.countAction, 40.dp)
            }
        }
    }
}

@Composable
private fun CountButton(description: String, size: Dp) {
    val colors = GlanceTheme.colors
    Box(
        modifier = GlanceModifier.size(size)
            .background(ImageProvider(R.drawable.widget_pill), colorFilter = ColorFilter.tint(colors.primary))
            .cornerRadius(size / 2)
            .clickable(actionRunCallback<TasbihIncrementAction>())
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            LocalContext.current.getString(R.string.tasbih_plus_one),
            style = textStyle(colors.onPrimary, if (size >= 56.dp) 18.sp else 15.sp, FontWeight.Bold, TextAlign.Center),
        )
    }
}
