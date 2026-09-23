package com.pilotothegreat.deencompanion.widget

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.SystemClock
import android.text.format.DateFormat
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.DynamicThemeColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.graphics.shapes.RoundedPolygon
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.icon
import com.pilotothegreat.deencompanion.ui.theme.DeenDarkColors
import com.pilotothegreat.deencompanion.ui.theme.DeenLightColors
import com.pilotothegreat.deencompanion.ui.theme.shape
import java.time.LocalTime
import java.util.Locale
import kotlin.math.roundToInt

private val BrandColors = ColorProviders(light = DeenLightColors, dark = DeenDarkColors)

/** The padding inside every widget, the same on all seven so they line up side by side. */
internal val WidgetPadding = 14.dp

/**
 * The padding above and below the content: [WidgetPadding], or less on a widget one row tall, where
 * 14dp top and bottom left too little of the height for a line of text.
 */
@Composable
internal fun verticalPadding(): Dp = if (LocalSize.current.height < SHORT_WIDGET) 8.dp else WidgetPadding

private val SHORT_WIDGET = 90.dp

/**
 * A progress bar and the 6dp above it. Glance draws the platform's horizontal bar, which is 16dp tall
 * whatever it is given; budgeting 10dp for it pushed the last athkar meter off the bottom.
 */
internal val WidgetBar = 6.dp + 16.dp

/** The app's colours, or the wallpaper-based scheme when dynamic colour is on (Android 12+), as in the app. */
@Composable
internal fun BilalWidgetTheme(dynamic: Boolean, content: @Composable () -> Unit) {
    val colors = if (dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) DynamicThemeColorProviders else BrandColors
    GlanceTheme(colors = colors, content = content)
}

/**
 * The rounded widget body: the shape drawn as an image under the content, tinted with the theme's
 * colour and faded by [transparency].
 *
 * It used to be the box's background with a tint, and transparency did nothing: a tint is drawn
 * SRC_ATOP, keeping the opacity of the white shape underneath, so a see-through colour only made the
 * card paler. An image's own alpha fades the whole thing, and the colour stays a provider rather than
 * one resolved colour, so the widget still follows the phone from light to dark.
 */
@Composable
internal fun WidgetSurface(
    color: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    /** 0f is opaque; the widget's own setting, so two copies can sit differently on two screens. */
    transparency: Float = 0f,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = GlanceModifier.fillMaxSize()
            .appWidgetBackground()
            // The launcher's own radius, so the widget sits flush with everything beside it instead
            // of carrying this app's idea of a corner onto someone else's home screen.
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    GlanceModifier.cornerRadius(android.R.dimen.system_app_widget_background_radius)
                } else {
                    GlanceModifier.cornerRadius(28.dp)
                },
            ),
    ) {
        Image(
            provider = ImageProvider(R.drawable.widget_shape),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(color),
            alpha = (1f - transparency).coerceIn(0f, 1f),
        )
        Box(GlanceModifier.fillMaxSize().then(modifier).padding(horizontal = WidgetPadding, vertical = verticalPadding()), content = content)
    }
}

/**
 * The height a widget's content has, spent in order of importance.
 *
 * Each widget lays out for the size it is actually given, which in a One UI stack is less than it asked
 * for. Nothing scrolls in a widget, so a line drawn past the bottom is simply lost — that is how Isha
 * disappeared. Instead each widget takes its essential lines first and adds the rest only while they
 * fit, measured at the phone's own text size.
 */
internal class HeightBudget(total: Dp, private val fontScale: Float) {
    var left: Dp = total
        private set

    /** The height one line of [sp] text takes. */
    fun line(sp: Float): Dp = (sp * LINE_HEIGHT * fontScale).dp

    /**
     * Spends [height] whether or not it is there, for what the widget cannot leave out. A [take] that failed
     * spent nothing, so the optional lines after it were still "fitted" into room the essentials had used.
     */
    fun spend(height: Dp) {
        left -= height
    }

    /** Spends [height] if there is that much left, and says whether it did. */
    fun take(height: Dp): Boolean = (height <= left).also { fits -> if (fits) left -= height }

    fun takeLine(sp: Float): Boolean = take(line(sp))

    private companion object {
        /** A line of Android text is about a third taller than its size. */
        const val LINE_HEIGHT = 1.32f
    }
}

/** The budget of the widget being drawn: its height less the padding. Create it where it is spent. */
@Composable
internal fun rememberBudget(): HeightBudget =
    HeightBudget(LocalSize.current.height - verticalPadding() * 2, LocalContext.current.resources.configuration.fontScale)

/**
 * The unfilled part of a progress bar: the card's own text colour, faint.
 *
 * surfaceVariant was a colour from another part of the palette, and on a green or blue card it landed
 * as a brown stain rather than the rest of the bar.
 */
@Composable
internal fun trackColor(content: ColorProvider): ColorProvider =
    ColorProvider(content.getColor(LocalContext.current).copy(alpha = TRACK_ALPHA))

private const val TRACK_ALPHA = 0.24f

internal fun textStyle(
    color: ColorProvider,
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Start,
) = TextStyle(color = color, fontSize = size, fontWeight = weight, textAlign = align)

/** "4:36" or "16:36" without the AM/PM marker, for the narrow columns of the times strip and table. */
internal fun shortTime(context: Context, time: LocalTime, locale: Locale): String =
    Formatters.pattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm", locale).format(time)

/**
 * A live countdown that reaches zero at [target] (an elapsedRealtime). RemoteViews' Chronometer ticks
 * by itself, so the widget only has to redraw when the next prayer changes.
 */
@Composable
internal fun Countdown(
    target: Long,
    textSizeSp: Float,
    dynamic: Boolean,
    modifier: GlanceModifier = GlanceModifier,
    /** Words around the time, "%s" standing for it, so a name and its countdown can be one line of text. */
    format: String? = null,
) {
    val context = LocalContext.current
    // A Chronometer counting down past its target counts back up again, so a widget the launcher
    // redraws a moment late used to show time since the adhan as though it were time until it.
    val clamped = target.coerceAtLeast(SystemClock.elapsedRealtime())
    val views = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
        setChronometer(R.id.countdown, clamped, format, true)
        setChronometerCountDown(R.id.countdown, true)
        setTextViewTextSize(R.id.countdown, TypedValue.COMPLEX_UNIT_SP, textSizeSp)
        val (day, night) = countdownColors(context, dynamic)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setColorInt(R.id.countdown, "setTextColor", day, night)
        } else {
            val isNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            setTextColor(R.id.countdown, if (isNight) night else day)
        }
    }
    AndroidRemoteViews(views, modifier)
}

/** onPrimaryContainer for day and night; the chronometer is a plain view outside Glance's theming. */
private fun countdownColors(context: Context, dynamic: Boolean): Pair<Int, Int> =
    if (dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getColor(android.R.color.system_accent1_900) to context.getColor(android.R.color.system_accent1_100)
    } else {
        DeenLightColors.onPrimaryContainer.toArgb() to DeenDarkColors.onPrimaryContainer.toArgb()
    }

/** The prayer's expressive shape with its icon, as on the Today card. */
@Composable
internal fun PrayerBadge(prayer: Prayer, size: Dp) =
    ShapeIcon(prayer.shape, prayer.icon, size, GlanceTheme.colors.primary, GlanceTheme.colors.onPrimary)

/**
 * An icon set in a MaterialShapes shape, the badge the app puts beside its cards. Glance cannot draw
 * either, so both are white bitmaps tinted by the theme.
 */
@Composable
internal fun ShapeIcon(polygon: RoundedPolygon, icon: ImageVector, size: Dp, container: ColorProvider, content: ColorProvider) {
    val px = pixels(size)
    val shape = remember(polygon, px) { WidgetArt.shape(polygon, px) }
    val glyph = remember(icon, px) { WidgetArt.icon(icon, (px * ICON_SCALE).roundToInt().coerceAtLeast(1)) }
    Box(GlanceModifier.size(size), contentAlignment = Alignment.Center) {
        Image(ImageProvider(shape), contentDescription = null, modifier = GlanceModifier.size(size), colorFilter = ColorFilter.tint(container))
        Image(ImageProvider(glyph), contentDescription = null, modifier = GlanceModifier.size(size * ICON_SCALE), colorFilter = ColorFilter.tint(content))
    }
}

/** A MaterialShapes shape as a container, for a button or a number. */
@Composable
internal fun ShapeBox(
    polygon: RoundedPolygon,
    size: Dp,
    color: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    content: @Composable () -> Unit,
) {
    val px = pixels(size)
    val shape = remember(polygon, px) { WidgetArt.shape(polygon, px) }
    Box(
        modifier = modifier.size(size).background(ImageProvider(shape), colorFilter = ColorFilter.tint(color)),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun pixels(size: Dp): Int = (size.value * LocalContext.current.resources.displayMetrics.density).roundToInt().coerceAtLeast(1)

private const val ICON_SCALE = 0.42f
