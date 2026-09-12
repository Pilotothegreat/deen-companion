package com.pilotothegreat.deencompanion.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.data.quran.LineKind
import com.pilotothegreat.deencompanion.data.quran.MushafLine
import com.pilotothegreat.deencompanion.data.quran.Quran
import com.pilotothegreat.deencompanion.data.quran.Surah
import com.pilotothegreat.deencompanion.data.quran.Verse
import com.pilotothegreat.deencompanion.ui.theme.UthmanicHafs

/**
 * A page of the mushaf as the mushaf prints it: fifteen lines, breaking where the King Fahd Complex
 * edition breaks them.
 *
 * The reader used to pour a page into one justified paragraph and let the text engine choose the
 * breaks. That reflowed differently on every screen and at every font size, and stretched a
 * half-empty line to the margins because it had no idea the line was meant to be short. Here each
 * line is laid out on its own, at the break the print chose, which is what keeps the gaps small:
 * those breaks were picked so that the line very nearly fills.
 *
 * One size is used for the whole page, found by measuring its widest line. A page whose lines were
 * each shrunk to fit would be a different thing entirely.
 */
@Composable
fun MushafPageLines(
    lines: List<MushafLine>,
    quran: Quran,
    fontSize: Int,
    highlight: Pair<Int, Int>?,
    bookmarks: Set<Pair<Int, Int>>,
    onAyahClick: (Verse) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val widthPx = constraints.maxWidth
        val fit = remember(lines, fontSize, widthPx) { fitSize(lines, fontSize, widthPx, measurer) }
        val size = fit.size
        val height = (size * LINE_HEIGHT).dp
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.fillMaxWidth()) {
                lines.forEach { line ->
                    when (line.kind) {
                        LineKind.SURAH_HEADER -> line.surah?.let { SurahBanner(quran.surah(it)) }
                        LineKind.BASMALA -> BasmalaLine(line.surah?.let { quran.surah(it) }, size, height)
                        LineKind.BLANK -> Box(Modifier.height(height))
                        LineKind.AYAH, LineKind.CENTRED -> WordLine(
                            line = line,
                            quran = quran,
                            size = size,
                            height = height,
                            // The print justifies with kashida, which no Android text stack can do,
                            // so a line that does not nearly fill is centred instead of having its
                            // word gaps blown open to reach both margins.
                            centred = line.kind == LineKind.CENTRED || line in fit.short,
                            highlight = highlight,
                            bookmarks = bookmarks,
                            onAyahClick = onAyahClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordLine(
    line: MushafLine,
    quran: Quran,
    size: Float,
    height: Dp,
    centred: Boolean,
    highlight: Pair<Int, Int>?,
    bookmarks: Set<Pair<Int, Int>>,
    onAyahClick: (Verse) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().height(height),
        // Justified to both margins, because the break was chosen to make that look right. A
        // surah's closing line is centred instead, as the print sets it.
        horizontalArrangement = if (centred) CENTRED_WORDS else Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        line.words.forEach { word ->
            val key = word.surah to word.ayah
            val verse = quran.verse(word.surah, word.ayah)
            val interaction = remember(key) { MutableInteractionSource() }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // ۞ opens a rub' al-hizb. Unlike the sajdah sign it is not in Tanzil's text, so the
                // app draws it — on the first word of the ayah that opens the quarter, which is
                // where the print sets it.
                if (word.startsAyah && verse?.quarterStart != null) {
                    Text(
                        QUARTER,
                        fontFamily = UthmanicHafs,
                        fontSize = size.sp,
                        // The same ornamental colour as the rosettes: in print they are one family
                        // of marks, and picking the accent colour made the quarter mark shout.
                        color = colors.secondary,
                    )
                }
                Text(
                    text = word.text,
                    fontFamily = UthmanicHafs,
                    fontSize = size.sp,
                    color = if (key == highlight) colors.onTertiaryContainer else colors.onSurface,
                    modifier = Modifier
                        .background(if (key == highlight) colors.tertiaryContainer else Color.Transparent)
                        .then(
                            if (verse == null) {
                                Modifier
                            } else {
                                // No ripple: a wash of colour over one word of scripture on every
                                // tap is louder than the tap deserves.
                                Modifier.selectable(
                                    selected = key == highlight,
                                    interactionSource = interaction,
                                    indication = null,
                                    onClick = { onAyahClick(verse) },
                                )
                            },
                        ),
                )
                if (word.endsAyah) {
                    AyahRosette(
                        number = word.ayah,
                        size = size,
                        color = if (key in bookmarks) colors.primary else colors.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun BasmalaLine(surah: Surah?, size: Float, height: Dp) {
    Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        Text(
            text = surah?.bismillah ?: BASMALA,
            fontFamily = UthmanicHafs,
            fontSize = (size * 0.95f).sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The widest line decides the size for the whole page.
 *
 * Measuring costs little: only the page in view and its two neighbours are ever composed, and the
 * answer is remembered until the width or the reader's chosen size changes.
 */
/** The size the whole page is set at, and the lines too short to justify. */
private data class PageFit(val size: Float, val short: Set<MushafLine>)

private fun fitSize(lines: List<MushafLine>, fontSize: Int, widthPx: Int, measurer: TextMeasurer): PageFit {
    if (widthPx <= 0) return PageFit(fontSize.toFloat(), emptySet())
    val style = TextStyle(fontFamily = UthmanicHafs, fontSize = fontSize.sp)
    val widths = HashMap<MushafLine, Int>(lines.size)
    lines.forEach { line ->
        if (line.words.isEmpty()) return@forEach
        val text = line.words.joinToString(WORD_GAP) { it.text }
        val measured = measurer.measure(text, style, maxLines = 1, softWrap = false).size.width
        // Every rosette on the line takes room the measured string does not include.
        val rosettes = line.words.count { it.endsAyah }
        val marks = if (rosettes == 0) 0 else {
            measurer.measure(ROSETTE_RULER.repeat(rosettes), style, maxLines = 1, softWrap = false).size.width
        }
        widths[line] = measured + marks
    }
    val widest = widths.values.maxOrNull() ?: return PageFit(fontSize.toFloat(), emptySet())
    val ratio = (widthPx.toFloat() / widest).coerceAtMost(1f)
    val short = widths.filterValues { it < widest * JUSTIFY_ABOVE }.keys
    return PageFit((fontSize * ratio).coerceAtLeast(MIN_SIZE), short)
}

/** U+06DE, the mark that opens a quarter of a hizb. */
private const val QUARTER = "\u06de"

/** A centred line keeps a normal word gap rather than being pushed to the margins. */
private val CENTRED_WORDS = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)

/** The measured string joins words with a single space, which is the gap a full line ends up with. */
private const val WORD_GAP = " "

/** Stands in for the rosette when measuring: the glyph plus the space either side of it. */
private const val ROSETTE_RULER = " ۝ "

private const val LINE_HEIGHT = 2.1f
private const val MIN_SIZE = 10f

/** A line narrower than this share of the page's widest is centred rather than justified. */
private const val JUSTIFY_ABOVE = 0.86f
private const val BASMALA = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
