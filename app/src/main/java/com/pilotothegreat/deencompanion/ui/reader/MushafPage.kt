package com.pilotothegreat.deencompanion.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.core.text.Kashida
import com.pilotothegreat.deencompanion.core.text.Numerals
import com.pilotothegreat.deencompanion.data.quran.LineKind
import com.pilotothegreat.deencompanion.data.quran.LineWord
import com.pilotothegreat.deencompanion.data.quran.MushafLine
import com.pilotothegreat.deencompanion.data.quran.Quran
import com.pilotothegreat.deencompanion.data.quran.Revelation
import com.pilotothegreat.deencompanion.data.quran.Surah
import com.pilotothegreat.deencompanion.data.quran.Verse
import com.pilotothegreat.deencompanion.ui.theme.UthmanicHafs

/**
 * A page of the mushaf as the mushaf prints it: fifteen lines, breaking where the King Fahd Complex
 * edition breaks them, each stretched to both margins.
 *
 * Like the print, every page is set at one size — the same on page 604 as on page 50 — and its fifteen
 * lines are spread down the page rather than bunched at the top. A line is filled the way the print
 * fills it: by lengthening joins between letters with ـ, then by widening the gaps between words with
 * whatever the kashida leaves. Each word is shaped whole by the font, so its pause marks stay on it and
 * the ayah number after it becomes its rosette; the words are then placed right to left along the line.
 */
@Composable
fun MushafPageLines(
    lines: List<MushafLine>,
    quran: Quran,
    highlight: Pair<Int, Int>?,
    bookmarks: Set<Pair<Int, Int>>,
    onTap: (Verse) -> Unit,
    onLongPress: (Verse) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val heightPx = if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat() else Float.POSITIVE_INFINITY
        val lineBoxEm = remember(measurer, density) { lineBoxEm(measurer, density) }
        val fit = remember(constraints.maxWidth, heightPx, lineBoxEm) { PageFit.of(constraints.maxWidth.toFloat(), heightPx, lineBoxEm) }
        val lineWidth: Dp = with(density) { fit.lineWidthPx.toDp() }
        val pitch: Dp = with(density) { fit.pitchPx.toDp() }
        val fontSize: TextUnit = with(density) { fit.emPx.toSp() }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.width(lineWidth)) {
                lines.forEach { line ->
                    when (line.kind) {
                        LineKind.SURAH_HEADER -> line.surah?.let { SurahBand(quran.surah(it), fontSize, pitch) }
                        LineKind.BASMALA -> BasmalaLine(line.surah?.let { quran.surah(it) }, fontSize, pitch)
                        LineKind.BLANK -> Spacer(Modifier.height(pitch))
                        LineKind.AYAH, LineKind.CENTRED -> TextLine(
                            line = line,
                            quran = quran,
                            fit = fit,
                            justified = line.kind == LineKind.AYAH,
                            highlight = highlight,
                            bookmarks = bookmarks,
                            onTap = onTap,
                            onLongPress = onLongPress,
                            measurer = measurer,
                            modifier = Modifier.fillMaxWidth().height(pitch),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The page's measurements, in pixels.
 *
 * [emPx] is the font size. [lineWidthPx] is the measure every justified line is stretched to — the
 * screen's width, or less where the height is what limits the size. [pitchPx] is the height of each of
 * the fifteen line slots.
 */
private class PageFit(val emPx: Float, val lineWidthPx: Float, val pitchPx: Float) {
    companion object {
        fun of(widthPx: Float, heightPx: Float, lineBoxEm: Float): PageFit {
            val em = minOf(widthPx / LINE_EM, heightPx / (LINES * lineBoxEm))
            // Spread down the page, but not further apart than a little beyond the print's own spacing, so
            // a tall phone keeps a page rather than a list of lines.
            val pitch = minOf(heightPx / LINES, em * MAX_PITCH_EM).coerceAtLeast(em * lineBoxEm)
            return PageFit(em, em * LINE_EM, pitch)
        }
    }
}

/** A word, with the ayah number it closes if it closes one, and where it sits on its line. */
private class Piece(val layout: TextLayoutResult, val right: Float, val width: Float, val surah: Int, val ayah: Int) {
    val left: Float get() = right - width
}

private class LaidLine(val pieces: List<Piece>, val gap: Float, val text: String) {
    /** The ayah under a touch at [x]: the word touched, or the nearest one when the touch is in a gap. */
    fun ayahAt(x: Float): Pair<Int, Int>? = pieces
        .minByOrNull { piece -> if (x in piece.left..piece.right) 0f else minOf(kotlin.math.abs(x - piece.left), kotlin.math.abs(x - piece.right)) }
        ?.let { it.surah to it.ayah }
}

@Composable
private fun TextLine(
    line: MushafLine,
    quran: Quran,
    fit: PageFit,
    justified: Boolean,
    highlight: Pair<Int, Int>?,
    bookmarks: Set<Pair<Int, Int>>,
    onTap: (Verse) -> Unit,
    onLongPress: (Verse) -> Unit,
    measurer: TextMeasurer,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val marked = line.words.filter { it.endsAyah && (it.surah to it.ayah) in bookmarks }.map { it.surah to it.ayah }.toSet()
    val laid = remember(line, fit, justified, marked, colors, density) {
        layLine(line, fit, justified, marked, colors, measurer, density)
    }
    val tap by rememberUpdatedState(onTap)
    val longPress by rememberUpdatedState(onLongPress)
    Box(
        modifier
            .semantics { text = AnnotatedString(laid.text) }
            // A tap brings up the player on this ayah, a long press its menu. detectTapGestures waits for
            // the finger to lift and lets a drag go by, so a swipe to the next page is never taken for
            // either — the pinch detector 2.0 put here swallowed every swipe.
            .pointerInput(laid, quran) {
                detectTapGestures(
                    onTap = { position -> laid.ayahAt(position.x)?.let { (surah, ayah) -> quran.verse(surah, ayah)?.let(tap) } },
                    onLongPress = { position -> laid.ayahAt(position.x)?.let { (surah, ayah) -> quran.verse(surah, ayah)?.let(longPress) } },
                )
            }
            .drawBehind { drawLine(laid, highlight, colors, fit.emPx) },
    )
}

private fun DrawScope.drawLine(laid: LaidLine, highlight: Pair<Int, Int>?, colors: ColorScheme, emPx: Float) {
    val lit = laid.pieces.filter { (it.surah to it.ayah) == highlight }
    if (lit.isNotEmpty()) {
        // One band behind the highlighted ayah's words on this line, gaps included.
        val left = lit.minOf { it.left } - laid.gap / 2
        val right = lit.maxOf { it.right } + laid.gap / 2
        val inset = size.height * 0.08f
        drawRoundRect(
            color = colors.tertiaryContainer,
            topLeft = Offset(left.coerceAtLeast(0f), inset),
            size = Size((right.coerceAtMost(size.width) - left.coerceAtLeast(0f)), size.height - 2 * inset),
            cornerRadius = CornerRadius(emPx * 0.4f),
        )
    }
    laid.pieces.forEach { piece ->
        val topLeft = Offset(piece.right - piece.layout.size.width, (size.height - piece.layout.size.height) / 2)
        if ((piece.surah to piece.ayah) == highlight) {
            drawText(piece.layout, color = colors.onTertiaryContainer, topLeft = topLeft)
        } else {
            drawText(piece.layout, topLeft = topLeft)
        }
    }
}

/** Shapes a line's words at the page's size, fills the line with kashida and gaps, and places the words. */
private fun layLine(
    line: MushafLine,
    fit: PageFit,
    justified: Boolean,
    marked: Set<Pair<Int, Int>>,
    colors: ColorScheme,
    measurer: TextMeasurer,
    density: Density,
): LaidLine {
    val baseSize = with(density) { fit.emPx.toSp() }
    val display = line.words.map { word -> if (word.endsAyah) word.text + numberOf(word.ayah) else word.text }
    val gaps = (display.size - 1).coerceAtLeast(0)

    fun styleAt(scale: Float) = TextStyle(
        fontFamily = UthmanicHafs,
        fontSize = baseSize * scale,
        color = colors.onSurface,
        textDirection = TextDirection.Rtl,
    )
    val widths = HashMap<String, Float>()
    fun widthOf(text: String): Float = widths.getOrPut(text) {
        measurer.measure(text, styleAt(1f), maxLines = 1, softWrap = false).multiParagraph.intrinsics.maxIntrinsicWidth
    }

    val space = SPACE_EM * fit.emPx
    val natural = display.fold(0f) { sum, word -> sum + widthOf(word) } + gaps * space
    // The one line in a hundred wider than the measure is set a touch smaller, as the print compresses it.
    val scale = if (natural > fit.lineWidthPx) fit.lineWidthPx / natural else 1f
    val words = if (justified && scale == 1f && gaps > 0) {
        // Kashida takes most of the shortfall and the word gaps the rest, as in the print: joins stretched
        // everywhere look ruled, and wide gaps alone leave holes. Whatever kashida cannot take, the gaps do.
        val shortfall = fit.lineWidthPx - natural
        Kashida.justify(display, natural - gaps * space + shortfall * KASHIDA_SHARE, ::widthOf)
    } else {
        display
    }

    val style = styleAt(scale)
    val layouts = words.mapIndexed { index, text -> measurer.measure(colouredWord(text, line.words[index], marked, colors), style, maxLines = 1, softWrap = false) }
    val pieceWidths = layouts.map { it.multiParagraph.intrinsics.maxIntrinsicWidth }
    val used = pieceWidths.sum()
    val stretch = justified || scale < 1f
    val gap = when {
        gaps == 0 -> 0f
        stretch -> ((fit.lineWidthPx - used) / gaps).coerceAtLeast(0f)
        else -> space
    }
    // A justified line runs from the right margin to the left; a centred one sits in the middle.
    var right = if (stretch && gaps > 0) fit.lineWidthPx else (fit.lineWidthPx + used + gaps * gap) / 2
    val pieces = layouts.mapIndexed { index, layout ->
        val word = line.words[index]
        Piece(layout, right, pieceWidths[index], word.surah, word.ayah).also { right -= pieceWidths[index] + gap }
    }
    return LaidLine(pieces, gap, display.joinToString(" "))
}

/** The word in the page's ink, and its ayah number — which the font draws as the rosette — in the ornament colour. */
private fun colouredWord(text: String, word: LineWord, marked: Set<Pair<Int, Int>>, colors: ColorScheme): AnnotatedString =
    buildAnnotatedString {
        append(text)
        if (word.endsAyah) {
            val colour = if ((word.surah to word.ayah) in marked) colors.primary else colors.secondary
            addStyle(SpanStyle(color = colour), text.length - numberOf(word.ayah).length, text.length)
        }
    }

/**
 * The number goes back after a no-break space: that is what the font turns into a rosette, and what
 * keeps it with the word it closes.
 */
private fun numberOf(ayah: Int): String = "\u00a0" + Numerals.toArabicIndic(ayah.toString())

/** The height of the font's line box, in ems: its tallest marks above and deepest below. */
private fun lineBoxEm(measurer: TextMeasurer, density: Density): Float {
    val style = TextStyle(fontFamily = UthmanicHafs, fontSize = REFERENCE_SP.sp)
    val px = with(density) { REFERENCE_SP.sp.toPx() }
    return measurer.measure(REFERENCE_WORD, style, maxLines = 1, softWrap = false).size.height / px
}

@Composable
private fun BasmalaLine(surah: Surah?, size: TextUnit, height: Dp) {
    Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        Text(
            text = surah?.bismillah.orEmpty(),
            fontFamily = UthmanicHafs,
            fontSize = size,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/**
 * The band the print sets a surah's name in: the name in the mushaf's script inside a framed band, the
 * number of ayahs on one side and where it was revealed on the other, in small medallions. It is
 * Arabic whatever the app's language, as everything on a page of the mushaf is.
 */
@Composable
private fun SurahBand(surah: Surah, size: TextUnit, height: Dp) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = colors.surfaceContainerHigh,
        border = BorderStroke(1.dp, colors.outline),
        modifier = Modifier.fillMaxWidth().height(height).padding(vertical = 3.dp),
    ) {
        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Medallion(Numerals.toArabicIndic(surah.verses.size.toString()), height)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = SURAH_PREFIX + surah.nameArabic,
                    fontFamily = UthmanicHafs,
                    fontSize = size * 0.9f,
                    color = colors.onSurface,
                    maxLines = 1,
                )
            }
            Medallion(if (surah.revelation == Revelation.MECCAN) MECCAN else MEDINAN, height)
        }
    }
}

@Composable
private fun Medallion(label: String, lineHeight: Dp) {
    Surface(
        shape = MaterialShapes.Cookie9Sided.toShape(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(lineHeight * 0.7f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

private const val LINES = 15

/**
 * The measure, in ems: 99 of every 100 justified lines of the print are no wider than this set in this
 * font with plain spaces (scripts/quran/measure_lines.py measures all 8,694). One size for every page
 * follows from it, and the rare wider line is set slightly smaller rather than shrinking every page.
 */
private const val LINE_EM = 20.5f

/** The print's line spacing is about 2.5 ems; a tall screen may open it a little beyond that, no further. */
private const val MAX_PITCH_EM = 3.0f

/** The font's space, 450 of its 2,048 units. */
private const val SPACE_EM = 450f / 2048f

/** The share of a short line's shortfall that kashida fills; the gaps between words take the rest. */
private const val KASHIDA_SHARE = 0.7f

private const val REFERENCE_SP = 100f

/** Measured for the height of a line: letters with marks above and below. */
private const val REFERENCE_WORD = "بِسۡمِ ٱللَّهِ"
private const val SURAH_PREFIX = "سُورَةُ "
private const val MECCAN = "مكية"
private const val MEDINAN = "مدنية"
