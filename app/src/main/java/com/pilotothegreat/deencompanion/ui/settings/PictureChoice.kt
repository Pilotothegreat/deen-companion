package com.pilotothegreat.deencompanion.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.core.prayer.AsrSchool
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.ui.theme.Spacing

/**
 * A choice set out the way the theme picker sets out its own: a card per option with a small
 * picture of what it means, and the chosen card rounding its corners and turning and growing its
 * picture. It stands in for rows of segmented buttons, which said the same thing in words alone.
 *
 * Up to [FITS] options share the width; more scroll, and the chosen one is brought into view.
 */
@Composable
internal fun <T> PictureChoice(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    picture: @Composable (option: T, chosen: Boolean) -> Unit,
) {
    if (options.size <= FITS) {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            options.forEach { option ->
                PictureCard(option == selected, label(option), { onSelect(option) }, Modifier.weight(1f)) { picture(option, option == selected) }
            }
        }
    } else {
        val scroll = rememberScrollState()
        val step = with(LocalDensity.current) { (SCROLLING_WIDTH + Spacing.small).roundToPx() }
        val index = options.indexOf(selected).coerceAtLeast(0)
        // maxValue is a key so this runs again once the row is measured and can scroll.
        LaunchedEffect(index, scroll.maxValue) { scroll.animateScrollTo((step * (index - 1)).coerceIn(0, scroll.maxValue)) }
        Row(modifier.fillMaxWidth().horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            options.forEach { option ->
                PictureCard(option == selected, label(option), { onSelect(option) }, Modifier.width(SCROLLING_WIDTH)) { picture(option, option == selected) }
            }
        }
    }
}

@Composable
private fun PictureCard(selected: Boolean, label: String, onClick: () -> Unit, modifier: Modifier, picture: @Composable () -> Unit) {
    val motion = cardMotion(selected, restTurn = 0f, chosenTurn = -8f)
    val tile = MaterialTheme.colorScheme.surface
    Column(
        modifier.then(cardModifier(selected, Role.RadioButton, onClick)).padding(Spacing.hair),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(4.dp)
                .drawBehind { rotate(-motion.turn()) { drawRoundRect(tile, cornerRadius = CornerRadius(motion.corner().dp.toPx())) } },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.graphicsLayer {
                    val grow = motion.shape() / REST_SHAPE
                    scaleX = grow
                    scaleY = grow
                    rotationZ = motion.turn() / 2
                },
                contentAlignment = Alignment.Center,
            ) { picture() }
        }
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = Spacing.hair, vertical = 6.dp),
        )
    }
}

/** The language written in itself; the phone's own setting is a phone. */
@Composable
internal fun LanguagePicture(tag: String, chosen: Boolean) {
    val color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    when (tag) {
        AppLanguage.SYSTEM -> Icon(Icons.Rounded.Smartphone, contentDescription = null, tint = color, modifier = Modifier.size(30.dp))
        "en" -> Text("Aa", style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Ltr), color = color)
        else -> Text("أب", style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Rtl), color = color)
    }
}

/** "Aa" at the size it would be read at. */
@Composable
internal fun TextSizePicture(scale: Float, chosen: Boolean) {
    val color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Text("Aa", fontSize = (15 * scale).sp, color = color, style = MaterialTheme.typography.titleMedium)
}

/**
 * A stick and its shadow, which is the whole difference between the two: Asr begins when the
 * shadow is once the stick's length (the majority) or twice it (the Hanafi school).
 */
@Composable
internal fun AsrPicture(school: AsrSchool, chosen: Boolean) {
    val stick = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val sun = MaterialTheme.colorScheme.tertiary
    val shade = MaterialTheme.colorScheme.outline
    val times = if (school == AsrSchool.HANAFI) 2f else 1f
    Canvas(Modifier.size(width = 64.dp, height = 44.dp)) {
        val ground = size.height * 0.86f
        val height = size.height * 0.5f
        val base = Offset(size.width * 0.28f, ground)
        val line = 3.dp.toPx()
        drawLine(shade.copy(alpha = 0.5f), Offset(0f, ground), Offset(size.width, ground), strokeWidth = 1.dp.toPx())
        drawCircle(sun, radius = 5.dp.toPx(), center = Offset(size.width * 0.08f, size.height * 0.16f))
        drawLine(shade, base, base.copy(x = base.x + height * times), strokeWidth = line * 1.4f, cap = StrokeCap.Round)
        drawLine(stick, base, base.copy(y = ground - height), strokeWidth = line, cap = StrokeCap.Round)
    }
}

/** Minutes as the part of a clock face they take up, around what they are for. */
@Composable
internal fun MinutesPicture(minutes: Int, icon: ImageVector, chosen: Boolean) {
    val arc = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(44.dp)) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2
            val arcSize = size.copy(width = size.width - stroke, height = size.height - stroke)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke))
            if (minutes > 0) {
                drawArc(
                    arc, -90f, 360f * minutes / 60f, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Icon(icon, contentDescription = null, tint = arc, modifier = Modifier.size(20.dp))
    }
}

/** Beyond this many options, cards keep their width and the row scrolls. */
private const val FITS = 4
private val SCROLLING_WIDTH = 84.dp

/** The rest size [cardMotion] reports for a shape; the picture is scaled by the ratio to it. */
private const val REST_SHAPE = 42f
