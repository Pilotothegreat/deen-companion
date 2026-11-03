package com.pilotothegreat.deencompanion.ui.theme

import android.graphics.Matrix
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.pilotothegreat.deencompanion.core.prayer.Prayer

/** A [Morph] between two polygons at a fixed [progress], scaled to fill the layout bounds. */
class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val bounds = morph.calculateMaxBounds()
        val width = bounds[2] - bounds[0]
        val height = bounds[3] - bounds[1]
        val path = morph.toPath(progress)
        path.transform(
            Matrix().apply {
                setTranslate(-bounds[0], -bounds[1])
                postScale(size.width / width, size.height / height)
            },
        )
        return Outline.Generic(path.asComposePath())
    }
}

/** Shape that springs from its previous polygon to [target] whenever [target] changes. */
@Composable
fun animatedPolygonShape(
    target: RoundedPolygon,
    spec: FiniteAnimationSpec<Float> = MaterialTheme.motionScheme.slowSpatialSpec(),
): Shape {
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(target) {
        if (target == to) return@LaunchedEffect
        from = to
        to = target
        progress.snapTo(0f)
        progress.animateTo(1f, spec)
    }
    val morph = remember(from, to) { Morph(from, to) }
    return MorphShape(morph, progress.value)
}

/** Each prayer has its own expressive shape; the home hero morphs between them. */
val Prayer.shape: RoundedPolygon
    get() = when (this) {
        Prayer.FAJR -> MaterialShapes.SoftBurst
        Prayer.SUNRISE, Prayer.DHUHR -> MaterialShapes.Sunny
        Prayer.ASR -> MaterialShapes.Cookie9Sided
        Prayer.MAGHRIB -> MaterialShapes.Clover8Leaf
        Prayer.ISHA -> MaterialShapes.Cookie12Sided
    }
