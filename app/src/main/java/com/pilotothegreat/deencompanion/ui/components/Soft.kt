package com.pilotothegreat.deencompanion.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.pilotothegreat.deencompanion.ui.theme.rememberReducedMotion
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The sky behind the tabs, for frosted surfaces to blur; null where there is none, as on pushed screens. */
val LocalFrost = staticCompositionLocalOf<HazeState?> { null }

/** The sky and the screen over it together, for the floating bar to blur, as a phone's dock blurs its wallpaper. */
val LocalChrome = staticCompositionLocalOf<HazeState?> { null }

/**
 * Frosted glass: the sky behind blurred, a pale veil of the page's own colour over it, and a fine light
 * edge along the top, as light catches the rim of a pane. Below Android 12, and anywhere without a sky
 * behind, it is the same veil, opaque enough to read on.
 */
@Composable
fun Modifier.frost(shape: Shape, veil: Float = 0.62f): Modifier {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val surface = colors.surfaceContainerLow
    val rim = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = if (dark) 0.16f else 0.7f), Color.White.copy(alpha = if (dark) 0.04f else 0.18f)),
    )
    val state = LocalFrost.current
    val glass = if (state == null) {
        Modifier.clip(shape).background(surface.copy(alpha = 0.94f))
    } else {
        val style = remember(surface, veil) {
            HazeBlurStyle {
                blurRadius(28.dp)
                backgroundColor(surface)
                noiseFactor(0.06f)
                colorEffects(listOf(HazeColorEffect.tint(surface.copy(alpha = veil))))
                fallbackColorEffect(HazeColorEffect.tint(surface.copy(alpha = 0.92f)))
            }
        }
        Modifier
            .clip(shape)
            .hazeBlur(input = HazeInput.Backdrop(state), style = style, performanceMode = HazePerformanceMode.Adaptive)
    }
    return this.then(glass).border(1.dp, rim, shape)
}

/**
 * A soft bloom: a ring of rounded petals that opens out from the middle and fades, for the small
 * completions worth a moment — a prayer marked, a round of tasbih, a list of athkar finished. It plays
 * once each time [key] changes after the first, and not at all under reduced motion.
 */
@Composable
fun Bloom(key: Any?, color: Color, modifier: Modifier = Modifier, play: Boolean = true, onArrival: Boolean = false) {
    val still = rememberReducedMotion()
    val progress = remember { Animatable(1f) }
    val first = remember { mutableListOf(true) }
    LaunchedEffect(key) {
        if (first[0]) {
            first[0] = false
            if (!onArrival) return@LaunchedEffect
        }
        if (still || !play) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(BLOOM_MS, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val fade = 1f - p
        // Drawn past its own bounds: the petals open out over the row around it.
        val reach = size.minDimension * (0.3f + 0.75f * p)
        rotate(p * 40f) {
            for (i in 0 until PETALS) {
                val angle = 2 * PI * i / PETALS
                val at = Offset(center.x + (reach * cos(angle)).toFloat(), center.y + (reach * sin(angle)).toFloat())
                val r = size.minDimension * (if (i % 2 == 0) 0.09f else 0.06f) * (1f - 0.4f * p)
                drawCircle(color.copy(alpha = 0.85f * fade), r, at)
            }
        }
        drawCircle(color.copy(alpha = 0.22f * fade), reach * 0.8f, center)
    }
}

private const val PETALS = 10
private const val BLOOM_MS = 720

/**
 * Glass for the app's chrome — the floating bar, a header — in the manner of a phone's dock: whatever
 * passes behind is blurred deep and soft, a translucent tint of the page lies over it so labels stay
 * readable, and a fine rim of light runs round the edge, brightest along the top.
 */
@Composable
fun Modifier.glass(state: HazeState?, shape: Shape, tint: Float = 0.5f, rim: Boolean = true): Modifier {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val base = if (dark) colors.surfaceContainerHigh else colors.surfaceContainerLow
    val edge = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = if (dark) 0.22f else 0.85f), Color.White.copy(alpha = if (dark) 0.05f else 0.25f)),
    )
    val surface = if (state == null) {
        Modifier.clip(shape).background(base.copy(alpha = 0.95f))
    } else {
        val style = remember(base, tint) {
            HazeBlurStyle {
                blurRadius(34.dp)
                backgroundColor(base)
                noiseFactor(0.08f)
                colorEffects(listOf(HazeColorEffect.tint(base.copy(alpha = tint))))
                fallbackColorEffect(HazeColorEffect.tint(base.copy(alpha = 0.94f)))
            }
        }
        Modifier.clip(shape).hazeBlur(input = HazeInput.Backdrop(state), style = style, performanceMode = HazePerformanceMode.Adaptive)
    }
    return if (rim) this.then(surface).border(1.dp, edge, shape) else this.then(surface)
}

/**
 * The frost behind a tab's header, as leekleak's Traffic Light draws it: a layer over the list and under
 * the title that runs from the very top of the screen, status bar and all, to a little past the bar.
 * What scrolls beneath it is blurred fully at the top and less and less further down, and the veil over
 * the blur thins with it, so the header melts into the page with no edge at all.
 *
 * Lay it over the page's content, after it, with the height the scaffold gives the top bar.
 */
@Composable
fun HeaderFrost(state: HazeState, barHeight: androidx.compose.ui.unit.Dp) {
    val total = barHeight + HEADER_FADE
    // Full frost behind the title, so nothing shows through the words; the fade is all below the bar.
    val solid = if (total.value > 0f) (barHeight / total).coerceIn(0f, 1f) else 0f
    androidx.compose.foundation.layout.Box(
        Modifier
            .fillMaxWidth()
            .height(total)
            .headerFrost(state, solid),
    )
}

@Composable
private fun Modifier.headerFrost(state: HazeState, solid: Float): Modifier {
    val page = MaterialTheme.colorScheme.background
    val style = remember(page, solid) {
        val veil = Brush.verticalGradient(
            0f to page.copy(alpha = 0.82f),
            solid to page.copy(alpha = 0.7f),
            1f to page.copy(alpha = 0f),
        )
        HazeBlurStyle {
            blurRadius(24.dp)
            backgroundColor(page)
            noiseFactor(0.05f)
            colorEffects(listOf(HazeColorEffect.tint(veil)))
            fallbackColorEffect(HazeColorEffect.tint(veil))
            if (progressiveBlurAvailable) {
                // Traffic Light's progressive blur, held at full strength through the bar.
                progressive(
                    dev.chrisbanes.haze.HazeProgressive.Brush(
                        Brush.verticalGradient(0f to Color.Black, solid to Color.Black, 1f to Color.Transparent),
                    ),
                )
            } else {
                // Without runtime shaders (Android 12) the whole effect fades instead, which reads the same.
                mask(Brush.verticalGradient(0f to Color.Black, solid to Color.Black, 1f to Color.Transparent))
            }
        }
    }
    return hazeBlur(input = HazeInput.Sources(state), style = style)
}

/** How far the frost runs past the bar, fading out. */
private val HEADER_FADE = 40.dp

/**
 * Haze's progressive blur is a runtime shader, which Android has from 13 on. Robolectric's renderer
 * cannot build one either, and an exception there would end the render tests.
 */
private val progressiveBlurAvailable: Boolean =
    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && android.os.Build.FINGERPRINT != "robolectric"
