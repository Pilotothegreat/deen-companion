package com.pilotothegreat.deencompanion.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.prayer.PrayerSchedule
import com.pilotothegreat.deencompanion.ui.theme.rememberReducedMotion
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The sky at one moment of the day of prayers: four colours from the top of the sky to the horizon,
 * how bright the stars are, and how much cloud shows.
 */
@Immutable
data class SkyPalette(
    val top: Color,
    val upper: Color,
    val lower: Color,
    val horizon: Color,
    val stars: Float,
    val clouds: Float,
)

/** Where the sky is: its colours, and the sun's way along its arc (0 at sunrise, 1 at Maghrib), if it is up. */
@Immutable
data class SkyState(val palette: SkyPalette, val sun: Float?, val dawnGlow: Float) {
    val night: Boolean get() = palette.stars > 0.5f
}

private val Night = SkyPalette(Color(0xFF0D1430), Color(0xFF17204A), Color(0xFF242E62), Color(0xFF323A72), 1f, 0.06f)
private val FirstLight = SkyPalette(Color(0xFF1B2552), Color(0xFF3A3C76), Color(0xFF7A5F8E), Color(0xFFB9808C), 0.6f, 0.12f)
private val Dawn = SkyPalette(Color(0xFF2E3A72), Color(0xFF7A659E), Color(0xFFD9918D), Color(0xFFF8C79C), 0.15f, 0.22f)
private val Morning = SkyPalette(Color(0xFF3F79B8), Color(0xFF7FAEDB), Color(0xFFC9DFF0), Color(0xFFF7E3C4), 0f, 0.32f)
private val Noon = SkyPalette(Color(0xFF2F72B6), Color(0xFF5E9BD6), Color(0xFFA7CDEE), Color(0xFFE4F1FA), 0f, 0.36f)
private val Afternoon = SkyPalette(Color(0xFF3B6FA8), Color(0xFF7A9FC8), Color(0xFFDDBF94), Color(0xFFF4D6A2), 0f, 0.3f)
private val Sunset = SkyPalette(Color(0xFF473B7B), Color(0xFF9E5A86), Color(0xFFEA8C6B), Color(0xFFFFCE8C), 0f, 0.3f)
private val Dusk = SkyPalette(Color(0xFF252760), Color(0xFF4F4482), Color(0xFF905D7E), Color(0xFFCB8577), 0.35f, 0.16f)

private fun SkyPalette.lerpTo(other: SkyPalette, f: Float) = SkyPalette(
    lerp(top, other.top, f), lerp(upper, other.upper, f), lerp(lower, other.lower, f), lerp(horizon, other.horizon, f),
    stars + (other.stars - stars) * f, clouds + (other.clouds - clouds) * f,
)

/**
 * The sky for [now] over [schedule]'s day, drawn from the prayer times themselves: first light at Fajr,
 * dawn at sunrise, noon blue by Dhuhr, gold by Asr, the sunset glow around Maghrib, night by Isha. Between
 * two of those moments the colours blend, so the sky never changes in a step.
 */
fun skyAt(now: ZonedDateTime, schedule: PrayerSchedule): SkyState {
    val a = schedule.adhan
    val fajr = a[Prayer.FAJR] ?: return SkyState(Noon, 0.5f, 0f)
    val sunrise = a[Prayer.SUNRISE] ?: fajr.plusMinutes(80)
    val dhuhr = a[Prayer.DHUHR] ?: sunrise.plusHours(6)
    val asr = a[Prayer.ASR] ?: dhuhr.plusHours(3)
    val maghrib = a[Prayer.MAGHRIB] ?: asr.plusHours(3)
    val isha = a[Prayer.ISHA] ?: maghrib.plusMinutes(80)
    val keys = listOf(
        fajr.minusMinutes(50) to Night,
        fajr to FirstLight,
        sunrise.minusMinutes(10) to Dawn,
        sunrise.plusMinutes(70) to Morning,
        dhuhr to Noon,
        asr to Afternoon,
        maghrib.minusMinutes(35) to Sunset,
        maghrib.plusMinutes(15) to Dusk,
        isha to Night,
    )
    val palette = when {
        !now.isAfter(keys.first().first) || !now.isBefore(keys.last().first) -> Night
        else -> {
            val i = keys.indexOfLast { !now.isBefore(it.first) }
            val (from, a0) = keys[i]
            val (to, a1) = keys[i + 1]
            val span = Duration.between(from, to).toMillis().coerceAtLeast(1)
            a0.lerpTo(a1, (Duration.between(from, now).toMillis().toFloat() / span).coerceIn(0f, 1f))
        }
    }
    val day = Duration.between(sunrise, maghrib).toMillis().coerceAtLeast(1)
    val sun = Duration.between(sunrise, now).toMillis().toFloat() / day
    val glow = if (now.isAfter(fajr) && now.isBefore(sunrise)) {
        Duration.between(fajr, now).toMillis().toFloat() / Duration.between(fajr, sunrise).toMillis().coerceAtLeast(1)
    } else {
        0f
    }
    return SkyState(palette, sun.takeIf { it in -0.02f..1.02f }, glow)
}

/** The sky over the app right now, for whatever wants to take its colour from the time of day. */
val LocalSky = androidx.compose.runtime.compositionLocalOf<SkyState?> { null }

/** The sky now, refreshed each minute: the colours move through the day with the clock. */
@Composable
fun rememberSky(schedule: PrayerSchedule?, zone: ZoneId): State<SkyState?> =
    produceState(initialValue = schedule?.let { skyAt(ZonedDateTime.now(zone), it) }, schedule, zone) {
        while (true) {
            value = schedule?.let { skyAt(ZonedDateTime.now(zone), it) }
            kotlinx.coroutines.delay(60_000)
        }
    }

/**
 * The sky, alive: clouds drift across it, stars twinkle at night, and the sun breathes a little on its
 * arc. Everything moves in the draw phase from one slow clock, so nothing recomposes while it moves, and
 * it stops altogether under reduced motion. When it leaves the screen, its clock stops with it.
 */
@Composable
fun SkyCanvas(sky: SkyState, modifier: Modifier = Modifier) {
    val still = rememberReducedMotion()
    val clock = if (still) null else rememberInfiniteTransition(label = "sky").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SKY_LOOP_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "skyClock",
    )
    val stars = remember { List(STARS) { i -> Star(i) } }
    // In Arabic the words sit on the right, so the sky turns about: the moon and the sun's setting side
    // move to the left, clear of the text.
    val mirrored = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    Canvas(modifier) {
        if (mirrored) {
            scale(-1f, 1f) { drawSky(sky, clock?.value ?: 0f, stars) }
        } else {
            drawSky(sky, clock?.value ?: 0f, stars)
        }
    }
}

private class Star(i: Int) {
    private val r = java.util.Random(i * 7919L + 13)
    val x = r.nextFloat()
    val y = r.nextFloat() * 0.62f
    val size = if (r.nextFloat() < 0.2f) 1.6f else 1f
    val phase = r.nextFloat()
    val rate = 3 + r.nextInt(5)
}

private fun DrawScope.drawSky(sky: SkyState, t: Float, stars: List<Star>) {
    val p = sky.palette
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(0f to p.top, 0.45f to p.upper, 0.8f to p.lower, 1f to p.horizon))

    // Stars, each twinkling at its own pace.
    if (p.stars > 0.02f) {
        stars.forEach { s ->
            val twinkle = 0.55f + 0.45f * sin(2 * PI.toFloat() * (t * s.rate + s.phase))
            drawCircle(Color.White.copy(alpha = (p.stars * twinkle * 0.9f).coerceIn(0f, 1f)), s.size.dp.toPx(), Offset(s.x * w, s.y * h))
        }
    }

    // First light gathering at the horizon before sunrise.
    if (sky.dawnGlow > 0f) {
        val c = Offset(w * 0.3f, h * 1.05f)
        drawCircle(Brush.radialGradient(listOf(p.horizon.copy(alpha = 0.9f * sky.dawnGlow), Color.Transparent), c, h * 0.9f), h * 0.9f, c)
    }

    // The sun on the day's arc, or the crescent at night.
    val cx = w / 2
    val cy = h * 0.98f
    val rx = w * 0.42f
    val ry = h * 0.66f
    val breathe = 1f + 0.05f * sin(2 * PI.toFloat() * t * 4)
    val sunAt = sky.sun
    if (sunAt != null) {
        val angle = PI * sunAt.coerceIn(0f, 1f)
        val sun = Offset((cx - rx * cos(angle)).toFloat(), (cy - ry * sin(angle)).toFloat())
        val r = h * 0.075f
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF4D6).copy(alpha = 0.7f), Color.Transparent), sun, r * 5f * breathe), r * 5f * breathe, sun)
        drawCircle(Color(0xFFFFF6DE), r, sun)
    } else if (sky.night) {
        val moon = Offset(w * 0.8f, h * (0.24f + 0.01f * sin(2 * PI.toFloat() * t)))
        val r = h * 0.085f
        drawCircle(Brush.radialGradient(listOf(Color(0xFFF4ECD2).copy(alpha = 0.28f), Color.Transparent), moon, r * 4f * breathe), r * 4f * breathe, moon)
        drawCircle(Color(0xFFF4ECD2), r, moon)
        drawCircle(lerp(p.top, p.upper, 0.25f), r * 0.92f, Offset(moon.x + r * 0.42f, moon.y - r * 0.28f))
    }

    // Soft clouds drifting west, each a few blurred puffs.
    CLOUDS.forEach { c ->
        val cw = c.width * w
        val span = w + cw
        val x = ((c.x + t * c.speed) % 1f) * span - cw
        val y = c.y * h
        val ch = cw * 0.32f
        for (k in 0 until 3) {
            val puff = Offset(x + cw * (0.2f + 0.3f * k), y + ch * (if (k == 1) -0.25f else 0.05f))
            val pr = ch * (if (k == 1) 0.95f else 0.75f) * 1.6f
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = p.clouds), Color.Transparent), puff, pr), pr, puff)
        }
    }
}

private class Cloud(val x: Float, val y: Float, val width: Float, val speed: Float)

private val CLOUDS = listOf(Cloud(0.1f, 0.52f, 0.42f, 1f), Cloud(0.6f, 0.34f, 0.3f, 2f), Cloud(0.35f, 0.72f, 0.36f, 1f))

private const val STARS = 28

/** One full drift of the clouds; the stars and the sun's breath run on multiples of it. */
private const val SKY_LOOP_MS = 90_000

/**
 * A small mark for each prayer: where the sun stands at that time of day, over the horizon. A dawn
 * glow for Fajr, the sun clearing the horizon at sunrise, high at Dhuhr, lower at Asr, setting at
 * Maghrib, and the crescent at Isha. Drawn with clipping, not paint, so it sits on any surface.
 */
@Composable
fun PrayerGlyph(prayer: Prayer, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    val tone = GLYPH_TONES.getValue(prayer).copy(alpha = if (dimmed) 0.5f else 1f)
    Canvas(modifier) {
        val s = size.minDimension
        val horizon = s * 0.86f
        if (prayer == Prayer.ISHA) {
            val moon = androidx.compose.ui.graphics.Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(Offset(s * 0.5f, s * 0.42f), s * 0.25f))
            }
            val bite = androidx.compose.ui.graphics.Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(Offset(s * 0.62f, s * 0.33f), s * 0.22f))
            }
            drawPath(androidx.compose.ui.graphics.Path.combine(androidx.compose.ui.graphics.PathOperation.Difference, moon, bite), tone)
        } else {
            val (x, y, r) = when (prayer) {
                Prayer.FAJR -> Triple(0.5f, 0.86f, 0.16f)
                Prayer.SUNRISE -> Triple(0.5f, 0.72f, 0.2f)
                Prayer.DHUHR -> Triple(0.5f, 0.32f, 0.22f)
                Prayer.ASR -> Triple(0.72f, 0.46f, 0.2f)
                else -> Triple(0.62f, 0.8f, 0.2f)
            }
            val c = Offset(s * x, s * y)
            clipRect(bottom = horizon) {
                drawCircle(tone.copy(alpha = tone.alpha * 0.2f), s * r * 1.55f, c)
                drawCircle(tone, s * r, c)
            }
        }
        drawLine(tone.copy(alpha = 0.55f), Offset(s * 0.1f, horizon), Offset(s * 0.9f, horizon), s * 0.05f, cap = StrokeCap.Round)
    }
}

private val GLYPH_TONES = mapOf(
    Prayer.FAJR to Color(0xFFE9A27B),
    Prayer.SUNRISE to Color(0xFFF2B36B),
    Prayer.DHUHR to Color(0xFFE8B54A),
    Prayer.ASR to Color(0xFFE1A452),
    Prayer.MAGHRIB to Color(0xFFE07A5F),
    Prayer.ISHA to Color(0xFF6A6FB0),
)

/**
 * The page behind the tabs: the app's paper, washed from the top with the colour of the sky right now
 * and two soft glows that drift very slowly. It is what the frosted cards and the floating bar blur, so
 * the whole app takes on the time of day without any card turning loud.
 */
@Composable
fun SkyWash(sky: SkyState?, paper: Color, dark: Boolean, modifier: Modifier = Modifier) {
    val still = rememberReducedMotion()
    val drift = if (still) null else rememberInfiniteTransition(label = "wash").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(WASH_LOOP_MS, easing = LinearEasing), RepeatMode.Reverse),
        label = "washDrift",
    )
    val strength = if (dark) 0.42f else 0.3f
    Box(
        modifier.drawBehind {
            drawRect(paper)
            val p = sky?.palette ?: return@drawBehind
            val d = drift?.value ?: 0.5f
            drawRect(
                Brush.verticalGradient(
                    0f to p.upper.copy(alpha = strength),
                    0.28f to p.lower.copy(alpha = strength * 0.55f),
                    0.55f to Color.Transparent,
                ),
            )
            val a = Offset(size.width * (0.15f + 0.25f * d), size.height * 0.16f)
            drawCircle(Brush.radialGradient(listOf(p.horizon.copy(alpha = strength * 0.9f), Color.Transparent), a, size.width * 0.55f), size.width * 0.55f, a)
            val b = Offset(size.width * (0.9f - 0.2f * d), size.height * 0.48f)
            drawCircle(Brush.radialGradient(listOf(p.lower.copy(alpha = strength * 0.5f), Color.Transparent), b, size.width * 0.5f), size.width * 0.5f, b)
        },
    )
}

private const val WASH_LOOP_MS = 40_000
