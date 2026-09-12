package com.pilotothegreat.deencompanion.data.quran

import org.json.JSONArray
import org.json.JSONObject

/** What a line of the printed page carries. */
enum class LineKind {
    /** Words, set justified to both margins. */
    AYAH,

    /** Words, centred: the closing line of a surah, which the print does not stretch. */
    CENTRED,

    /** The ornamental band carrying the surah's name. */
    SURAH_HEADER,

    /** The Basmala, centred under the band. */
    BASMALA,

    /** The page ran out of text. */
    BLANK,
}

/**
 * One printed word.
 *
 * Usually one of Tanzil's space-separated tokens. Sometimes more than one, because the print sets
 * them as a single unit — a pause mark sits tight above the word it follows, and a few pairs such
 * as بَعْدَ مَا are written joined — and justification must not open a gap inside one.
 */
data class LineWord(
    val surah: Int,
    val ayah: Int,
    val text: String,
    /** True when this word opens its ayah, which is where a rub' al-hizb mark goes. */
    val startsAyah: Boolean,
    /** True when this word closes its ayah, so the numbered rosette follows it. */
    val endsAyah: Boolean,
)

data class MushafLine(
    val kind: LineKind,
    val words: List<LineWord>,
    /** The surah a banner or Basmala line announces; 0 on a line of words. */
    val owner: Int = 0,
) {
    val isOrnament: Boolean get() = words.isEmpty()

    /** The surah this line belongs to: the one it announces, or the one its first word is from. */
    val surah: Int? get() = owner.takeIf { it > 0 } ?: words.firstOrNull()?.surah
}

/**
 * Where the printed mushaf breaks its lines.
 *
 * The app has always known which ayahs belong to which page. Without this it did not know where the
 * page breaks its fifteen lines, so the reader poured a page into one justified paragraph and let
 * the text engine break it wherever the phone's width ran out: a different shape on every device,
 * half-empty lines stretched to the margins, and nothing a memoriser could hold a picture of.
 *
 * The table is deliberately small. Tanzil's tokens run in one global order, so a line needs only the
 * index of its last token and what kind of line it is; the print's own grouping of tokens into words
 * is the short list of tokens that join the one before them.
 */
object MushafLines {

    const val ASSET = "mushaf-lines.json"
    const val LINES_PER_PAGE = 15

    /**
     * Reads the table and cuts [surahs] into pages of lines.
     *
     * Returns null when the asset is missing or does not fit the text, which is all the reader needs
     * to fall back to flowing the page. The build script asserts the fit, and a unit test asserts it
     * again, so this is a belt for a brace rather than an expected path.
     */
    fun parse(json: String, surahs: List<Surah>): List<List<MushafLine>>? {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val ends = root.optJSONArray("ends") ?: return null
        val kinds = root.optJSONArray("kinds") ?: return null
        val owners = root.optJSONArray("owners")
        if (ends.length() != MushafLayout.PAGE_COUNT || kinds.length() != ends.length()) return null

        val tokens = tokenStream(surahs)
        if (root.optInt("tokens") != tokens.size) return null
        val glued = glueSet(root.optJSONArray("glue"))

        val pages = ArrayList<List<MushafLine>>(ends.length())
        var cursor = 0
        for (page in 0 until ends.length()) {
            val pageEnds = ends.optJSONArray(page) ?: return null
            val pageKinds = kinds.optJSONArray(page) ?: return null
            val pageOwners = owners?.optJSONArray(page)
            if (pageEnds.length() != LINES_PER_PAGE) return null
            val lines = ArrayList<MushafLine>(LINES_PER_PAGE)
            for (line in 0 until LINES_PER_PAGE) {
                val end = pageEnds.getInt(line)
                val kind = LineKind.entries.getOrNull(pageKinds.getInt(line)) ?: LineKind.BLANK
                val words = if (end >= cursor) wordsBetween(tokens, glued, cursor, end) else emptyList()
                if (end >= cursor) cursor = end + 1
                lines += MushafLine(kind, words, pageOwners?.optInt(line) ?: 0)
            }
            pages += lines
        }
        return pages.takeIf { cursor == tokens.size }
    }

    /** One entry per Tanzil token, in mushaf order, remembering which ayah it closes. */
    private fun tokenStream(surahs: List<Surah>): List<Token> {
        val out = ArrayList<Token>(82_000)
        for (surah in surahs) {
            for (verse in surah.verses) {
                val parts = verse.text.split(' ').filter { it.isNotEmpty() }
                parts.forEachIndexed { index, text ->
                    out += Token(surah.number, verse.number, text, index == 0, index == parts.lastIndex)
                }
            }
        }
        return out
    }

    private fun glueSet(array: JSONArray?): Set<Int> {
        if (array == null) return emptySet()
        val out = HashSet<Int>(array.length() * 2)
        for (i in 0 until array.length()) out += array.getInt(i)
        return out
    }

    /** Tokens [from]..[to] gathered into printed words, joining each glued token to the one before. */
    private fun wordsBetween(tokens: List<Token>, glued: Set<Int>, from: Int, to: Int): List<LineWord> {
        val out = ArrayList<LineWord>((to - from + 1).coerceAtLeast(1))
        val builder = StringBuilder()
        var start = from
        var index = from
        while (index <= to) {
            val token = tokens[index]
            // A glued token continues the word already being built; anything else begins a new one.
            if (index > start && index !in glued) {
                out += finish(tokens, start, index - 1, builder)
                builder.setLength(0)
                start = index
            }
            if (builder.isNotEmpty()) builder.append(' ')
            builder.append(token.text)
            index++
        }
        if (builder.isNotEmpty()) out += finish(tokens, start, to, builder)
        return out
    }

    private fun finish(tokens: List<Token>, start: Int, end: Int, builder: StringBuilder): LineWord {
        val first = tokens[start]
        return LineWord(
            surah = first.surah,
            ayah = first.ayah,
            text = builder.toString(),
            startsAyah = first.startsAyah,
            endsAyah = tokens[end].endsAyah,
        )
    }

    private data class Token(
        val surah: Int,
        val ayah: Int,
        val text: String,
        val startsAyah: Boolean,
        val endsAyah: Boolean,
    )
}
