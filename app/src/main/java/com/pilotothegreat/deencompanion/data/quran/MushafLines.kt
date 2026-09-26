package com.pilotothegreat.deencompanion.data.quran

import org.json.JSONObject

/** What a line of the printed page carries. */
enum class LineKind {
    /** Words, stretched to both margins. */
    AYAH,

    /** Words, centred: a surah's closing line, and every line of the two opening pages. */
    CENTRED,

    /** The band carrying the surah's name. */
    SURAH_HEADER,

    /** The Basmala, centred under the band. */
    BASMALA,

    /** The page ran out of text. */
    BLANK,
}

/**
 * One printed word, exactly as the King Fahd Complex text writes it — pause marks, the rub' al-hizb
 * sign and the sajdah overline included, since those are part of the word in the print.
 */
data class LineWord(
    val surah: Int,
    val ayah: Int,
    val text: String,
    /** True when this word closes its ayah, so the ayah's number follows it and the font draws the rosette. */
    val endsAyah: Boolean,
)

data class MushafLine(
    val kind: LineKind,
    val words: List<LineWord>,
    /** The surah a band or Basmala line announces; 0 on a line of words. */
    val owner: Int = 0,
) {
    /** The surah this line belongs to: the one it announces, or the one its first word is from. */
    val surah: Int? get() = owner.takeIf { it > 0 } ?: words.firstOrNull()?.surah
}

/**
 * Where the printed mushaf breaks its fifteen lines.
 *
 * The words run in one global order — surah 1 to 114, ayah 1 to n, word 1 to m — so the table stores,
 * for each line, only the index of its last word and what kind of line it is. The words themselves
 * are the ayahs' own text split on their spaces; the build script refuses to write a table unless
 * those are character for character the print's words.
 */
object MushafLines {

    const val ASSET = "mushaf-lines.json"
    const val LINES_PER_PAGE = 15

    /**
     * Reads the table and cuts [surahs] into pages of lines, or returns null when the asset is missing
     * or does not fit the text. The build script and a unit test both assert the fit, so null means a
     * broken build rather than a normal path.
     */
    fun parse(json: String, surahs: List<Surah>): List<List<MushafLine>>? {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val ends = root.optJSONArray("ends") ?: return null
        val kinds = root.optJSONArray("kinds") ?: return null
        val owners = root.optJSONArray("owners") ?: return null
        val joinArray = root.optJSONArray("joins") ?: return null
        if (ends.length() != MushafLayout.PAGE_COUNT || kinds.length() != ends.length()) return null

        val tokens = tokenStream(surahs)
        if (root.optInt("tokens") != tokens.size) return null
        val joins = HashSet<Int>(joinArray.length() * 2).apply {
            for (i in 0 until joinArray.length()) add(joinArray.getInt(i))
        }

        val pages = ArrayList<List<MushafLine>>(ends.length())
        var cursor = 0
        for (page in 0 until ends.length()) {
            val pageEnds = ends.optJSONArray(page) ?: return null
            val pageKinds = kinds.optJSONArray(page) ?: return null
            val pageOwners = owners.optJSONArray(page) ?: return null
            if (pageEnds.length() != LINES_PER_PAGE) return null
            val lines = ArrayList<MushafLine>(LINES_PER_PAGE)
            for (line in 0 until LINES_PER_PAGE) {
                val end = pageEnds.getInt(line)
                val kind = LineKind.entries.getOrNull(pageKinds.getInt(line)) ?: LineKind.BLANK
                val onLine = if (end >= cursor) printedWords(tokens, joins, cursor, end) else emptyList()
                if (end >= cursor) cursor = end + 1
                lines += MushafLine(kind, onLine, pageOwners.optInt(line))
            }
            pages += lines
        }
        return pages.takeIf { cursor == tokens.size }
    }

    /** Every token of the text in mushaf order: each ayah split on its ordinary spaces. */
    private fun tokenStream(surahs: List<Surah>): List<LineWord> {
        val out = ArrayList<LineWord>(80_000)
        for (surah in surahs) {
            for (verse in surah.verses) {
                val parts = verse.text.split(' ')
                parts.forEachIndexed { index, text ->
                    out += LineWord(surah.number, verse.number, text, endsAyah = index == parts.lastIndex)
                }
            }
        }
        return out
    }

    /**
     * Tokens [from]..[to] as the print's words. A token in [joins] continues the word before it — the
     * print sets بَعۡدَ مَا as one word — and keeps the space the text has between them.
     */
    private fun printedWords(tokens: List<LineWord>, joins: Set<Int>, from: Int, to: Int): List<LineWord> {
        val out = ArrayList<LineWord>(to - from + 1)
        for (index in from..to) {
            val token = tokens[index]
            if (index in joins && out.isNotEmpty()) {
                val previous = out.removeAt(out.lastIndex)
                out += previous.copy(text = previous.text + " " + token.text, endsAyah = token.endsAyah)
            } else {
                out += token
            }
        }
        return out
    }
}
