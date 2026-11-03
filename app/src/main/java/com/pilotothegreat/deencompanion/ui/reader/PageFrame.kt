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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
 * The page's border: two fine rules in the ornament's gold with a small rosette at each corner, as the
 * printed mushaf frames its text. Drawn behind the page, so it costs one draw and no layout.
 */
fun Modifier.mushafFrame(ornament: Color, paper: Color): Modifier = drawBehind {
    val outer = 1.2.dp.toPx()
    val inner = 0.6.dp.toPx()
    val gap = 3.dp.toPx()
    val radius = 10.dp.toPx()
    drawRoundRect(paper, cornerRadius = CornerRadius(radius))
    drawRoundRect(ornament, style = Stroke(outer), cornerRadius = CornerRadius(radius))
    drawRoundRect(
        ornament.copy(alpha = 0.55f),
        topLeft = Offset(gap, gap),
        size = Size(size.width - 2 * gap, size.height - 2 * gap),
        style = Stroke(inner),
        cornerRadius = CornerRadius(radius - gap),
    )
    val r = 2.4.dp.toPx()
    val inset = gap + radius * 0.45f
    listOf(
        Offset(inset, inset),
        Offset(size.width - inset, inset),
        Offset(inset, size.height - inset),
        Offset(size.width - inset, size.height - inset),
    ).forEach { corner -> drawRosette(corner, r, ornament) }
}

/** A four-petalled rosette: the small ornament the print sets in corners and between bands. */
private fun DrawScope.drawRosette(center: Offset, r: Float, color: Color) {
    for (i in 0 until 4) {
        val angle = Math.toRadians(i * 90.0 + 45.0)
        drawCircle(color.copy(alpha = 0.8f), r * 0.55f, Offset(center.x + (r * 0.7f * kotlin.math.cos(angle)).toFloat(), center.y + (r * 0.7f * kotlin.math.sin(angle)).toFloat()))
    }
    drawCircle(color, r * 0.45f, center)
}

/**
 * The band a surah opens with: a cartouche of two gold rules with pointed ends and a rosette at each,
 * the surah's name in the middle. With the King Fahd surah-name font the name is the Complex's own
 * calligraphy; without it, the name is set in the mushaf's font.
 */
@Composable
fun SurahCartouche(surah: Surah, names: Typeface?, fontSize: TextUnit, height: Dp) {
    val colors = MaterialTheme.colorScheme
    val ornament = colors.secondary
    val ink = colors.onSurface
    val fill = colors.secondaryContainer.copy(alpha = 0.45f)
    val label = SURAH_PREFIX + surah.nameArabic
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) { drawCartouche(ornament, fill) }
        if (names != null) {
            val nameText = remember(surah.number) { "%03d".format(surah.number) }
            Canvas(Modifier.fillMaxSize()) {
                val paint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = names
                    color = ink.toArgb()
                    textSize = size.height * 0.62f
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

private fun DrawScope.drawCartouche(ornament: Color, fill: Color) {
    val h = size.height
    val w = size.width
    val pad = h * 0.12f
    val top = pad
    val bottom = h - pad
    val mid = h / 2
    val point = h * 0.42f
    fun band(inset: Float): Path = Path().apply {
        moveTo(inset, mid)
        lineTo(inset + point, top + inset)
        lineTo(w - inset - point, top + inset)
        lineTo(w - inset, mid)
        lineTo(w - inset - point, bottom - inset)
        lineTo(inset + point, bottom - inset)
        close()
    }
    drawPath(band(0f), fill)
    drawPath(band(0f), ornament, style = Stroke(1.2.dp.toPx()))
    drawPath(band(3.dp.toPx()), ornament.copy(alpha = 0.55f), style = Stroke(0.6.dp.toPx()))
    val r = (h * 0.09f).coerceAtMost(3.dp.toPx())
    drawRosette(Offset(point * 0.62f, mid), r, ornament)
    drawRosette(Offset(w - point * 0.62f, mid), r, ornament)
}

private const val SURAH_PREFIX = "سُورَةُ "
