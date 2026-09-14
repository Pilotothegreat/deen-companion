package com.pilotothegreat.deencompanion.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

/**
 * An icon in a MaterialShapes shape that morphs into a second shape while its row is pressed: the badge
 * beside every row in Settings, so a press is felt in the shape as well as the ripple.
 */
@Composable
fun MorphBadge(
    icon: ImageVector,
    interactionSource: InteractionSource,
    modifier: Modifier = Modifier,
    rest: RoundedPolygon = MaterialShapes.Cookie9Sided,
    pressed: RoundedPolygon = MaterialShapes.Cookie4Sided,
    size: Dp = 40.dp,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    val morph = remember(rest, pressed) { Morph(rest, pressed) }
    val isPressed by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "badge",
    )
    val path = remember { Path() }
    Box(
        modifier
            .size(size)
            .drawBehind { drawPath(morph.toComposePath(progress, this.size, path), containerColor) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(size * 0.55f))
    }
}

/** The morph at [progress] as a path filling [size]; MaterialShapes are drawn in a unit square. */
internal fun Morph.toComposePath(progress: Float, size: Size, into: Path = Path()): Path {
    into.reset()
    var first = true
    forEachCubic(progress) { cubic ->
        if (first) {
            into.moveTo(cubic.anchor0X * size.width, cubic.anchor0Y * size.height)
            first = false
        }
        into.cubicTo(
            cubic.control0X * size.width, cubic.control0Y * size.height,
            cubic.control1X * size.width, cubic.control1Y * size.height,
            cubic.anchor1X * size.width, cubic.anchor1Y * size.height,
        )
    }
    into.close()
    return into
}

/** The shape as a path filling [size]. */
internal fun RoundedPolygon.toComposePath(size: Size, into: Path = Path()): Path {
    into.reset()
    cubics.forEachIndexed { index, cubic ->
        if (index == 0) into.moveTo(cubic.anchor0X * size.width, cubic.anchor0Y * size.height)
        into.cubicTo(
            cubic.control0X * size.width, cubic.control0Y * size.height,
            cubic.control1X * size.width, cubic.control1Y * size.height,
            cubic.anchor1X * size.width, cubic.anchor1Y * size.height,
        )
    }
    into.close()
    return into
}
