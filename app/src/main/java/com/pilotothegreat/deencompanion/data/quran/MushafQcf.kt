package com.pilotothegreat.deencompanion.data.quran

import android.content.Context
import androidx.compose.runtime.Immutable
import com.pilotothegreat.deencompanion.core.text.Numerals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * One line of the print-exact page: the King Fahd Complex's QCF V2 glyphs for that line's words, and
 * which ayah each word belongs to.
 *
 * The glyphs are the page font's own drawing of each word, already shaped as the 1421H print has it;
 * they mean nothing as text. What the line says, for a screen reader, copy or search, is [text]: the
 * same words in the verified KFGQPC text.
 */
@Immutable
class QcfLine internal constructor(
    val kind: LineKind,
    /** The surah a band or Basmala line announces; 0 on a line of words. */
    val owner: Int,
    /** The words' glyphs, one entry per printed word, each to be drawn in the page's own font. */
    val words: List<String>,
    /** For each of [words]: the ayah it belongs to, and whether it is that ayah's closing rosette. */
    val ayahs: List<Pair<Int, Int>>,
    val rosettes: List<Boolean>,
    /** The line's words in the KFGQPC text, each ayah closed by its number. */
    val text: String,
) {
    val surah: Int? get() = owner.takeIf { it > 0 } ?: ayahs.firstOrNull()?.first
}

/** The 604 pages of the print-exact mushaf, fifteen [QcfLine]s each. */
@Immutable
class MushafQcf internal constructor(val pages: List<List<QcfLine>>) {
    fun page(number: Int): List<QcfLine> = pages.getOrNull(number - 1).orEmpty()

    private val firstPage: Map<Pair<Int, Int>, Int> by lazy {
        val map = HashMap<Pair<Int, Int>, Int>(6236)
        pages.forEachIndexed { index, lines -> lines.forEach { line -> line.ayahs.forEach { map.putIfAbsent(it, index + 1) } } }
        map
    }

    /**
     * The page the 1421H print starts the ayah on. For 56 ayahs that is a page earlier than the 1405H
     * page table the rest of the app follows, so a recitation turns to the page the ayah is drawn on.
     */
    fun pageOf(surah: Int, ayah: Int): Int? = firstPage[surah to ayah]

    companion object {
        const val ASSET = "mushaf-qcf2.json"

        /** Between words in a line's glyphs, as build_qcf.py writes them; a word may itself hold a space. */
        private const val SEPARATOR = '|'

        /**
         * Reads the table against [surahs], or returns null when it is missing or does not fit the
         * text: the reader then keeps the bundled font, which needs nothing from this table.
         */
        fun parse(json: String, surahs: List<Surah>): MushafQcf? = runCatching {
            val root = JSONObject(json)
            val keys = ArrayList<Pair<Int, Int>>(6236)
            surahs.forEach { surah -> surah.verses.forEach { keys += surah.number to it.number } }
            // The text's words in one global order, as build_qcf.py counts them.
            val tokens = ArrayList<String>(80_000)
            val tokenEnds = ArrayList<Boolean>(80_000)
            val tokenAyah = ArrayList<Int>(80_000)
            surahs.forEach { surah ->
                surah.verses.forEach { verse ->
                    val parts = verse.text.split(' ')
                    parts.forEachIndexed { i, part ->
                        tokens += part
                        tokenEnds += i == parts.lastIndex
                        tokenAyah += verse.number
                    }
                }
            }
            if (root.getInt("tokens") != tokens.size || keys.size != root.getInt("ayahs")) return null
            val lines = root.getJSONArray("lines")
            val pages = (0 until lines.length()).map { p ->
                val page = lines.getJSONArray(p)
                (0 until page.length()).map { l -> line(page.getJSONArray(l), keys, tokens, tokenEnds, tokenAyah) }
            }
            MushafQcf(pages).takeIf { pages.size == MushafLayout.PAGE_COUNT }
        }.getOrNull()

        private fun line(
            row: JSONArray,
            keys: List<Pair<Int, Int>>,
            tokens: List<String>,
            tokenEnds: List<Boolean>,
            tokenAyah: List<Int>,
        ): QcfLine {
            val kind = LineKind.entries[row.getInt(0)]
            val glyphs = row.getString(2)
            val words = if (glyphs.isEmpty()) emptyList() else glyphs.split(SEPARATOR)
            val runs = row.getJSONArray(5)
            val ayahs = ArrayList<Pair<Int, Int>>(words.size)
            val rosettes = ArrayList<Boolean>(words.size)
            var i = 0
            while (i < runs.length()) {
                val key = keys[runs.getInt(i)]
                val count = runs.getInt(i + 1)
                val ends = runs.getInt(i + 2) == 1
                repeat(count) { k ->
                    ayahs += key
                    rosettes += ends && k == count - 1
                }
                i += 3
            }
            val first = row.getInt(3)
            val last = row.getInt(4)
            val text = if (first < 0) "" else buildString {
                for (t in first..last) {
                    if (t > first) append(' ')
                    append(tokens[t])
                    if (tokenEnds[t]) append(' ').append(Numerals.toArabicIndic(tokenAyah[t].toString()))
                }
            }
            return QcfLine(kind, row.getInt(1), words, ayahs, rosettes, text)
        }
    }
}

/** Loads the print-exact table once, off the main thread. */
class MushafQcfRepository(private val context: Context, private val quran: QuranRepository) {
    private val lock = Mutex()
    @Volatile private var cached: MushafQcf? = null
    @Volatile private var failed = false

    suspend fun table(): MushafQcf? {
        cached?.let { return it }
        if (failed) return null
        return lock.withLock {
            cached ?: if (failed) null else withContext(Dispatchers.IO) {
                val surahs = quran.quran().surahs
                runCatching { context.assets.open(MushafQcf.ASSET).bufferedReader().use { it.readText() } }
                    .getOrNull()
                    ?.let { MushafQcf.parse(it, surahs) }
            }.also { if (it == null) failed = true else cached = it }
        }
    }
}
