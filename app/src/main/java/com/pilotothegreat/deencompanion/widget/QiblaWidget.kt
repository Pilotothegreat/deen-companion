package com.pilotothegreat.deencompanion.widget

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.qibla.QiblaMath
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.navigation.DeepLinks
import kotlin.math.roundToInt

internal data class QiblaWidgetState(
    val dynamic: Boolean,
    val label: String,
    /** Degrees clockwise from true north, which is what the arrow is turned by. */
    val bearing: Float,
    /** Just the degrees, "294°", which is the one thing this widget exists to say. */
    val degrees: String,
    val fromNorth: String,
    val distance: String,
) {
    companion object {
        suspend fun load(context: Context): QiblaWidgetState {
            val settings = WidgetDeps.settings.current()
            val res = AppLanguage.localizedContext(context, settings.appLanguage)
            val locale = AppLanguage.locale(settings.appLanguage)
            val bearing = QiblaMath.bearing(settings.location.latitude, settings.location.longitude)
            val km = QiblaMath.distanceKm(settings.location.latitude, settings.location.longitude)
            return QiblaWidgetState(
                dynamic = settings.dynamicColor,
                label = res.getString(R.string.qibla_compass),
                bearing = bearing.toFloat(),
                degrees = res.getString(R.string.degrees, Formatters.number(bearing.roundToInt(), locale)),
                fromNorth = res.getString(R.string.widget_qibla_from_north),
                distance = res.getString(R.string.widget_qibla_distance, Formatters.number(km.roundToInt(), locale)),
            )
        }
    }
}

/**
 * The direction of the Kaaba from where you are, and how far away it is.
 *
 * The arrow is drawn against north rather than against the phone, because a widget is not told which
 * way the phone is pointing; the line under it says so in words. A tap opens the live compass, which
 * is the screen that can follow the phone.
 */
class QiblaWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(WidgetKind.QIBLA.previewSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideFresh(context, id, ::loadQibla)

    override suspend fun providePreview(context: Context, widgetCategory: Int) = provideContent(loadQibla(context, WidgetConfig()))
}

internal suspend fun loadQibla(context: Context, config: WidgetConfig): @Composable () -> Unit {
    val state = QiblaWidgetState.load(context)
    return { BilalWidgetTheme(config.dynamicColor ?: state.dynamic) { QiblaContent(state, config) } }
}

@Composable
internal fun QiblaContent(state: QiblaWidgetState, config: WidgetConfig = WidgetConfig()) {
    val colors = GlanceTheme.colors
    val content = colors.onSecondaryContainer
    val size = LocalSize.current
    val open = GlanceModifier.clickable(actionStartActivity(DeepLinks.screen(LocalContext.current, DeepLinks.QIBLA)))
    val budget = rememberBudget()
    // The degrees are the widget. The words under them go while they fit, the dial only where there is
    // width to spare: a dial that squeezes "294°" into "29…" has taken the place of what it illustrates.
    // One row tall, the bearing shrinks to the height rather than being cut in half under its label.
    val bearingSp = minOf(BEARING_SP, budget.left.value / budget.line(1f).value)
    budget.spend(budget.line(bearingSp))
    val showLabel = budget.takeLine(LABEL_SP)
    val showFromNorth = budget.takeLine(BODY_SP)
    val showDistance = budget.takeLine(BODY_SP)
    val dial = when {
        size.width < DIAL_FROM || size.height < DIAL_FROM -> 0.dp
        size.height >= 150.dp -> 64.dp
        else -> 52.dp
    }
    WidgetSurface(colors.secondaryContainer, open, transparency = config.transparency) {
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (dial > 0.dp) {
                QiblaDial(state.bearing, dial)
                Spacer(GlanceModifier.width(12.dp))
            }
            Column(GlanceModifier.defaultWeight()) {
                if (showLabel) Text(state.label, style = textStyle(content, LABEL_SP.sp, FontWeight.Medium), maxLines = 1)
                Text(state.degrees, style = textStyle(content, bearingSp.sp, FontWeight.Bold), maxLines = 1)
                if (showFromNorth) Text(state.fromNorth, style = textStyle(content, BODY_SP.sp), maxLines = 1)
                if (showDistance) Text(state.distance, style = textStyle(content, BODY_SP.sp), maxLines = 1)
            }
        }
    }
}

/** The arrow on its dial, turned to the bearing: north is up, as on a map. */
@Composable
private fun QiblaDial(bearing: Float, size: Dp) {
    val colors = GlanceTheme.colors
    val density = LocalContext.current.resources.displayMetrics.density
    val px = (size.value * density).roundToInt().coerceAtLeast(1)
    val arrow = remember(bearing, px) { WidgetArt.rotatedIcon(Icons.Rounded.Navigation, bearing, px) }
    Box(
        modifier = GlanceModifier.size(size)
            .background(ImageProvider(R.drawable.widget_dial), colorFilter = ColorFilter.tint(colors.secondary)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(arrow),
            contentDescription = null,
            modifier = GlanceModifier.size(size * ARROW_SCALE),
            colorFilter = ColorFilter.tint(colors.onSecondary),
        )
    }
}

/** The label takes its own line, so the bearing below it is the biggest thing on the card. */
private const val LABEL_SP = 11f
private const val BEARING_SP = 22f
private const val BODY_SP = 12f
private const val ARROW_SCALE = 0.55f
private val DIAL_FROM = 110.dp
