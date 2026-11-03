package com.pilotothegreat.deencompanion.ui.reader

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.pilotothegreat.deencompanion.data.quran.Surah
import com.pilotothegreat.deencompanion.ui.theme.UthmanicHafs

/**
 * The band a surah opens with: a soft capsule of the ornament's tint, lit a shade lighter along its top,
 * two fine rules following its curve, a small star near each end, and the surah's name in the middle. With the King Fahd surah-name font the name is the Complex's own
 * calligraphy; without it, the name is set in the mushaf's font.
 */
@Composable
fun SurahCartouche(surah: Surah, names: Typeface?, fontSize: TextUnit, height: Dp) {
    val colors = MaterialTheme.colorScheme
    val ornament = colors.secondary
    val ink = colors.onSurface
    val fill = colors.secondaryContainer.copy(alpha = 0.55f)
    val label = SURAH_PREFIX + surah.nameArabic
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) { drawCartouche(ornament, fill, colors.surface) }
        if (names != null) {
            val nameText = remember(surah.number) { "%03d".format(surah.number) }
            Canvas(Modifier.fillMaxSize()) {
                val paint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = names
                    color = ink.toArgb()
                    textSize = size.height * 0.78f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                val metrics = paint.fontMetrics
                val baseline = size.height / 2 - (metrics.ascent + metrics.descent) / 2
                drawContext.canvas.nativeCanvas.drawText(nameText, size.width / 2, baseline, paint)
            }
        } else {
            Text(label, fontFamily = UthmanicHafs, fontSize = fontSize * 0.9f, color = ink, maxLines = 1)
        }
    }
}

private fun DrawScope.drawCartouche(ornament: Color, fill: Color, glow: Color) {
    val h = size.height
    val w = size.width
    val band = h * 0.72f
    val inset = w * 0.035f
    val top = (h - band) / 2
    val radius = CornerRadius(band / 2)
    val bandSize = Size(w - 2 * inset, band)
    val topLeft = Offset(inset, top)
    // A soft capsule, a shade lighter along its top edge, as if lit from above.
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to androidx.compose.ui.graphics.lerp(fill, glow, 0.35f),
            1f to fill,
            startY = top,
            endY = top + band,
        ),
        topLeft = topLeft,
        size = bandSize,
        cornerRadius = radius,
    )
    drawRoundRect(ornament.copy(alpha = 0.35f), topLeft, bandSize, radius, style = Stroke(1.dp.toPx()))
    val gap = 3.dp.toPx()
    drawRoundRect(
        ornament.copy(alpha = 0.22f),
        Offset(inset + gap, top + gap),
        Size(bandSize.width - 2 * gap, band - 2 * gap),
        CornerRadius(band / 2 - gap),
        style = Stroke(0.7.dp.toPx()),
    )
    // A small four-pointed star near each end, the print's sparkle between band and name.
    val r = band * 0.2f
    listOf(Offset(inset + band * 0.62f, h / 2), Offset(w - inset - band * 0.62f, h / 2)).forEach { c ->
        drawPath(star(c, r), ornament.copy(alpha = 0.75f))
        drawCircle(glow.copy(alpha = 0.9f), r * 0.16f, c)
    }
}

/** A four-pointed star with softly pinched sides. */
private fun star(c: Offset, r: Float): androidx.compose.ui.graphics.Path = androidx.compose.ui.graphics.Path().apply {
    val pinch = r * 0.22f
    moveTo(c.x, c.y - r)
    quadraticTo(c.x + pinch, c.y - pinch, c.x + r, c.y)
    quadraticTo(c.x + pinch, c.y + pinch, c.x, c.y + r)
    quadraticTo(c.x - pinch, c.y + pinch, c.x - r, c.y)
    quadraticTo(c.x - pinch, c.y - pinch, c.x, c.y - r)
    close()
}

private const val SURAH_PREFIX = "سُورَةُ "
