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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.DynamicThemeColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.ui.common.icon
import com.pilotothegreat.deencompanion.ui.theme.DeenDarkColors
import com.pilotothegreat.deencompanion.ui.theme.DeenLightColors
import com.pilotothegreat.deencompanion.ui.theme.shape
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.util.Locale
import kotlin.math.roundToInt

private val BrandColors = ColorProviders(light = DeenLightColors, dark = DeenDarkColors)

/** The app's colours, or the wallpaper-based scheme when dynamic colour is on (Android 12+), as in the app. */
@Composable
internal fun BilalWidgetTheme(dynamic: Boolean, content: @Composable () -> Unit) {
    val colors = if (dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) DynamicThemeColorProviders else BrandColors
    GlanceTheme(colors = colors, content = content)
}

/** The rounded widget body. It's a tinted shape drawable, so corners are round on every Android version. */
@Composable
internal fun WidgetSurface(
    color: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    padding: Dp = 14.dp,
    /** 0f is opaque; the widget's own setting, so two copies can sit differently on two screens. */
    transparency: Float = 0f,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val tint = if (transparency <= 0f) {
        color
    } else {
        ColorProvider(color.getColor(context).copy(alpha = (1f - transparency).coerceIn(0.05f, 1f)))
    }
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
            )
            .background(ImageProvider(R.drawable.widget_shape), colorFilter = ColorFilter.tint(tint))
            .then(modifier)
            .padding(padding),
        content = content,
    )
}

internal fun textStyle(
    color: ColorProvider,
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Start,
) = TextStyle(color = color, fontSize = size, fontWeight = weight, textAlign = align)

/** "4:36" or "16:36" without the AM/PM marker, for the narrow columns of the times strip and table. */
internal fun shortTime(context: Context, time: LocalTime, locale: Locale): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    return DateTimeFormatter.ofPattern(pattern, locale).withDecimalStyle(DecimalStyle.of(locale)).format(time)
}

/**
 * A live countdown that reaches zero at [target] (an elapsedRealtime). RemoteViews' Chronometer ticks
 * by itself, so the widget only has to redraw when the next prayer changes.
 */
@Composable
internal fun Countdown(target: Long, textSizeSp: Float, dynamic: Boolean, modifier: GlanceModifier = GlanceModifier) {
    val context = LocalContext.current
    // A Chronometer counting down past its target counts back up again, so a widget the launcher
    // redraws a moment late used to show time since the adhan as though it were time until it.
    val clamped = target.coerceAtLeast(SystemClock.elapsedRealtime())
    val views = RemoteViews(context.packageName, R.layout.widget_countdown).apply {
        setChronometer(R.id.countdown, clamped, null, true)
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

/** The prayer's expressive shape with its icon, as on the Today card. Both are white bitmaps tinted by the theme. */
@Composable
internal fun PrayerBadge(prayer: Prayer, size: Dp) {
    val density = LocalContext.current.resources.displayMetrics.density
    val px = (size.value * density).roundToInt()
    val shape = remember(prayer, px) { WidgetArt.shape(prayer.shape, px) }
    val icon = remember(prayer, px) { WidgetArt.icon(prayer.icon, (px * ICON_SCALE).roundToInt()) }
    Box(GlanceModifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(shape),
            contentDescription = null,
            modifier = GlanceModifier.size(size),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
        )
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            modifier = GlanceModifier.size(size * ICON_SCALE),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
        )
    }
}

private const val ICON_SCALE = 0.42f
