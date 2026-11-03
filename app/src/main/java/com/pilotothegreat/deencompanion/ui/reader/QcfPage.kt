package com.pilotothegreat.deencompanion.ui.reader

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.data.quran.LineKind
import com.pilotothegreat.deencompanion.data.quran.QcfLine
import com.pilotothegreat.deencompanion.data.quran.Quran
import com.pilotothegreat.deencompanion.data.quran.Verse
import com.pilotothegreat.deencompanion.ui.theme.UthmanicHafs

/**
 * A page of the mushaf in the King Fahd Complex's own QCF V2 drawing of the 1421H print.
 *
 * Every word is a glyph of the page's font, shaped by the Complex's calligrapher, so nothing here
 * shapes, stretches or kashidas a letter: a line's words are placed right to left and its slack goes
 * into the gaps between them, as the print spaces a line. Lines well short of the measure — the two
 * opening pages and the closing lines of the last surahs — are centred, as the print centres them.
 */
@Composable
fun QcfPageLines(
    lines: List<QcfLine>,
    typeface: Typeface,
    names: Typeface?,
    quran: Quran,
    highlight: Pair<Int, Int>?,
    bookmarks: Set<Pair<Int, Int>>,
    onTap: (Verse) -> Unit,
    onLongPress: (Verse) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat() else Float.POSITIVE_INFINITY
        val boxEm = remember(typeface) { lineBoxEm(typeface) }
        val emPx = minOf(widthPx / QCF_LINE_EM, heightPx / (LINES * boxEm))
        val pitchPx = minOf(heightPx / LINES, emPx * MAX_PITCH_EM).coerceAtLeast(emPx * boxEm)
        val measurePx = emPx * QCF_LINE_EM
        val pitch: Dp = with(density) { pitchPx.toDp() }
        // The two opening pages are set apart in the print: a few centred lines in the middle of the page,
        // inside an ornament of their own, rather than fifteen lines from the top.
        val opening = lines.none { it.kind == LineKind.AYAH }
        val ornament = MaterialTheme.colorScheme.secondary
        val shown = if (opening) lines.filter { it.kind != LineKind.BLANK } else lines
        Column(
            Modifier
                .width(with(density) { measurePx.toDp() })
                .then(if (opening) Modifier.openingMedallion(ornament) else Modifier),
        ) {
            shown.forEach { line ->
                when (line.kind) {
                    LineKind.SURAH_HEADER -> line.surah?.let { SurahCartouche(quran.surah(it), names, with(density) { emPx.toSp() }, pitch) }
                    LineKind.BASMALA -> Box(Modifier.fillMaxWidth().height(pitch), contentAlignment = Alignment.Center) {
                        Text(
                            quran.surah(1).verses.first().text,
                            fontFamily = UthmanicHafs,
                            fontSize = with(density) { (emPx * 0.92f).toSp() },
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                    LineKind.BLANK -> Spacer(Modifier.height(pitch))
                    LineKind.AYAH, LineKind.CENTRED -> QcfTextLine(
                        line = line,
                        typeface = typeface,
                        emPx = emPx,
                        measurePx = measurePx,
                        quran = quran,
                        highlight = highlight,
                        bookmarks = bookmarks,
                        onTap = onTap,
                        onLongPress = onLongPress,
                        modifier = Modifier.fillMaxWidth().height(pitch),
                    )
                }
            }
        }
    }
}

/** A line's words, where each one sits, and at what size. */
private class QcfLaid(val left: FloatArray, val right: FloatArray, val textSize: Float, val gap: Float)

@Composable
private fun QcfTextLine(
    line: QcfLine,
    typeface: Typeface,
    emPx: Float,
    measurePx: Float,
    quran: Quran,
    highlight: Pair<Int, Int>?,
    bookmarks: Set<Pair<Int, Int>>,
    onTap: (Verse) -> Unit,
    onLongPress: (Verse) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val laid = remember(line, typeface, emPx, measurePx) { layQcfLine(line, typeface, emPx, measurePx) }
    val paint = remember(typeface) { TextPaint(Paint.ANTI_ALIAS_FLAG).apply { this.typeface = typeface } }
    val tap by rememberUpdatedState(onTap)
    val longPress by rememberUpdatedState(onLongPress)
    val listen = stringResource(R.string.ayah_listen, "%s")
    val options = stringResource(R.string.ayah_options, "%s")
    val actions = remember(line, quran, listen, options) {
        line.ayahs.distinct().flatMap { (surah, ayah) ->
            val ref = "$surah:$ayah"
            listOf(
                CustomAccessibilityAction(listen.format(ref)) { quran.verse(surah, ayah)?.let(tap) != null },
                CustomAccessibilityAction(options.format(ref)) { quran.verse(surah, ayah)?.let(longPress) != null },
            )
        }
    }
    fun ayahAt(x: Float): Pair<Int, Int>? {
        var best = -1
        var distance = Float.MAX_VALUE
        for (i in line.words.indices) {
            val d = if (x in laid.left[i]..laid.right[i]) 0f else minOf(kotlin.math.abs(x - laid.left[i]), kotlin.math.abs(x - laid.right[i]))
            if (d < distance) {
                distance = d
                best = i
            }
        }
        return line.ayahs.getOrNull(best)
    }
    Box(
        modifier
            .semantics {
                text = AnnotatedString(line.text)
                customActions = actions
            }
            .pointerInput(line, laid, quran) {
                detectTapGestures(
                    onTap = { p -> ayahAt(p.x)?.let { (s, a) -> quran.verse(s, a)?.let(tap) } },
                    onLongPress = { p -> ayahAt(p.x)?.let { (s, a) -> quran.verse(s, a)?.let(longPress) } },
                )
            }
            .drawBehind {
                // One band behind the lit ayah's words on this line, the gaps between them included.
                var bandLeft = Float.MAX_VALUE
                var bandRight = -1f
                line.ayahs.forEachIndexed { i, key ->
                    if (key == highlight) {
                        bandLeft = minOf(bandLeft, laid.left[i])
                        bandRight = maxOf(bandRight, laid.right[i])
                    }
                }
                if (bandRight > 0f) {
                    val inset = size.height * 0.06f
                    val l = (bandLeft - laid.gap / 2).coerceAtLeast(0f)
                    val r = (bandRight + laid.gap / 2).coerceAtMost(size.width)
                    drawRoundRect(colors.tertiaryContainer, Offset(l, inset), Size(r - l, size.height - 2 * inset), CornerRadius(emPx * 0.4f))
                }
                paint.textSize = laid.textSize
                val metrics = paint.fontMetrics
                val baseline = size.height / 2 - (metrics.ascent + metrics.descent) / 2
                val canvas = drawContext.canvas.nativeCanvas
                line.words.forEachIndexed { i, word ->
                    val key = line.ayahs[i]
                    paint.color = when {
                        key == highlight -> colors.onTertiaryContainer
                        line.rosettes[i] && key in bookmarks -> colors.primary
                        line.rosettes[i] -> colors.secondary
                        else -> colors.onSurface
                    }.toArgb()
                    canvas.drawText(word, laid.left[i], baseline, paint)
                }
            },
    )
}

/** Measures each word in the page's font and places the words right to left across the measure. */
private fun layQcfLine(line: QcfLine, typeface: Typeface, emPx: Float, measurePx: Float): QcfLaid {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = emPx
    }
    val n = line.words.size
    var widths = FloatArray(n) { paint.measureText(line.words[it]) }
    val natural = widths.sum()
    // The rare line wider than the measure is set a touch smaller, as the print compresses it.
    val scale = if (natural > measurePx) measurePx / natural else 1f
    if (scale < 1f) widths = FloatArray(n) { widths[it] * scale }
    val used = widths.sum()
    val centred = line.kind == LineKind.CENTRED || used < measurePx * CENTRE_BELOW
    val gap = if (centred || n < 2) 0f else (measurePx - used) / (n - 1)
    var right = if (centred) (measurePx + used) / 2 else measurePx
    val left = FloatArray(n)
    val rights = FloatArray(n)
    for (i in 0 until n) {
        rights[i] = right
        left[i] = right - widths[i]
        right = left[i] - gap
    }
    return QcfLaid(left, rights, emPx * scale, gap)
}

/** The height of the page font's line box, in ems. */
private fun lineBoxEm(typeface: Typeface): Float {
    val paint = TextPaint().apply {
        this.typeface = typeface
        textSize = 100f
    }
    val m = paint.fontMetrics
    return ((m.descent - m.ascent) / 100f).coerceIn(1.2f, 3f)
}

private const val LINES = 15

/**
 * The measure, in ems of the page fonts: a full line of the print measures 15.2 to 15.9 ems in its
 * own font (build-time measurement of every line of six sample pages), so 16.2 leaves every full line
 * a little room to spread into, as the print's lines have.
 */
private const val QCF_LINE_EM = 16.2f

/** A tall screen may open the lines a little beyond the print's spacing, no further. */
private const val MAX_PITCH_EM = 2.2f

/** A line shorter than this share of the measure is one the print centres rather than spreads. */
private const val CENTRE_BELOW = 0.78f

/**
 * Where the print sets al-Fatihah and the opening of al-Baqarah apart: a soft rounded glow of the
 * ornament's tint behind the lines, with a little air above and below them.
 */
private fun Modifier.openingMedallion(ornament: androidx.compose.ui.graphics.Color): Modifier = this
    .drawBehind {
        val pad = 14.dp.toPx()
        val outer = androidx.compose.ui.geometry.Rect(-pad * 0.6f, -pad, size.width + pad * 0.6f, size.height + pad)
        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                0f to ornament.copy(alpha = 0.12f),
                1f to ornament.copy(alpha = 0.03f),
                center = outer.center,
                radius = outer.maxDimension * 0.6f,
            ),
            topLeft = outer.topLeft,
            size = outer.size,
            cornerRadius = CornerRadius(size.width * 0.18f),
        )
    }
    .padding(vertical = 6.dp)
