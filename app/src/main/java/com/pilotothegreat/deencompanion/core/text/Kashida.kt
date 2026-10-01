package com.pilotothegreat.deencompanion.core.text

/**
 * Kashida justification: filling a line by lengthening the joins between letters with ـ, the way the
 * printed mushaf does, instead of widening the gaps between words.
 *
 * Android's text stack justifies only by stretching spaces, which leaves holes in a Quran line. The
 * app therefore inserts tatweel itself, for display only — search, copy and share keep the untouched
 * text. Where a tatweel may go is decided by the letters, not by the font:
 *
 * - only after a letter that connects to the next one, so never after ا د ذ ر ز و ة ى ء and their
 *   hamza forms;
 * - just before the next letter, after every mark the previous letter carries, so no vowel or pause
 *   mark is carried off its letter;
 * - never inside لا, which the font draws as one ligature, nor inside the ligature of the Name of
 *   Allah;
 * - never inside a word that carries the sajdah overline, the rub' al-hizb sign or the sajdah sign,
 *   which the font also draws as single shapes that a tatweel would break apart;
 * - never on a join the text has already stretched.
 *
 * As a calligrapher would, it lengthens the join before a word's last letter first, and at most one more
 * in a longer word; it leaves words of one or two letters alone, and spreads the stretch along the line
 * rather than piling it into one word.
 */
object Kashida {

    const val TATWEEL = 'ـ'

    /** Letters that do not connect to the letter after them. */
    private val NON_JOINING: Set<Char> = setOf(
        'ء', // ء
        'آ', 'أ', 'إ', 'ا', 'ٱ', // آ أ إ ا ٱ
        'د', 'ذ', 'ر', 'ز', // د ذ ر ز
        'ؤ', 'و', // ؤ و
        'ة', 'ى', // ة ى
    )

    private val ALEFS: Set<Char> = setOf('آ', 'أ', 'إ', 'ا', 'ٱ')

    /** Characters that mean the word is drawn as a single shape a tatweel would break. */
    private val WHOLE_WORD_SHAPES: Set<Char> = setOf('ۤ', '۞', '۩')

    private const val LAM = 'ل'
    private const val HEH = 'ه'

    private fun isLetter(c: Char): Boolean = (c in 'ء'..'ي' && c != TATWEEL) || c == 'ٱ'

    private fun isMark(c: Char): Boolean = Character.getType(c) == Character.NON_SPACING_MARK.toInt()

    /**
     * Where a tatweel may be inserted in [word], best first. Each value is the index of the letter the
     * tatweel goes in front of.
     */
    fun positions(word: String): List<Int> {
        if (word.any { it in WHOLE_WORD_SHAPES }) return emptyList()
        val letters = word.indices.filter { isLetter(word[it]) }
        // The Name of Allah is lam, lam, heh with marks between; its letters are drawn as one ligature.
        val protected = HashSet<Int>()
        for (k in 0 until letters.size - 2) {
            if (word[letters[k]] == LAM && word[letters[k + 1]] == LAM && word[letters[k + 2]] == HEH) {
                protected += letters[k]
                protected += letters[k + 1]
                protected += letters[k + 2]
            }
        }
        val out = ArrayList<Int>()
        for (k in 0 until letters.size - 1) {
            val before = letters[k]
            val after = letters[k + 1]
            val previous = word[before]
            val next = word[after]
            if (previous in NON_JOINING) continue
            if (previous == LAM && next in ALEFS) continue
            if (before in protected || after in protected) continue
            // Only marks may lie between the two letters: a space, a digit or an existing tatweel
            // means they are not joined, or are stretched already.
            if ((before + 1 until after).any { !isMark(word[it]) }) continue
            out += after
        }
        // The join before the last letter first, then the earlier ones from the end of the word.
        return out.sortedDescending()
    }

    /**
     * Stretches [words] so that their widths together come as close to [target] as a whole number of
     * tatweels allows without passing it. [measure] gives one word's width in the same units as
     * [target]; the gaps between words are the caller's, who gives them whatever kashida leaves.
     *
     * Returns the words with tatweels inserted, or the words unchanged if they already fill the target
     * or nothing in them can be stretched.
     */
    fun justify(words: List<String>, target: Float, measure: (String) -> Float, maxPerJoin: Int = MAX_PER_JOIN): List<String> {
        fun width(candidate: List<String>): Float = candidate.fold(0f) { sum, word -> sum + measure(word) }
        if (words.isEmpty() || width(words) >= target) return words
        val perWord = words.map { word ->
            val letters = word.count(::isLetter)
            if (letters < MIN_LETTERS) emptyList() else positions(word).take(if (letters >= TWO_JOIN_LETTERS) 2 else 1)
        }
        // Round-robin across the words, so the stretch is spread along the line.
        val slots = ArrayList<Pair<Int, Int>>()
        val deepest = perWord.maxOfOrNull { it.size } ?: 0
        for (rank in 0 until deepest) {
            perWord.forEachIndexed { wordIndex, positions -> positions.getOrNull(rank)?.let { slots += wordIndex to it } }
        }
        if (slots.isEmpty()) return words

        fun build(count: Int): List<String> {
            val added = IntArray(slots.size)
            for (n in 0 until count) added[n % slots.size]++
            return words.mapIndexed { wordIndex, word ->
                val inserts = slots.indices
                    .filter { slots[it].first == wordIndex && added[it] > 0 }
                    .map { slots[it].second to added[it] }
                    .sortedByDescending { it.first }
                val builder = StringBuilder(word)
                inserts.forEach { (at, times) -> builder.insert(at, TATWEEL.toString().repeat(times)) }
                builder.toString()
            }
        }

        var low = 0
        var high = slots.size * maxPerJoin
        while (low < high) {
            val middle = (low + high + 1) / 2
            if (width(build(middle)) <= target) low = middle else high = middle - 1
        }
        return build(low)
    }

    /** More than this on one join starts to look like a line drawn with a ruler. */
    const val MAX_PER_JOIN = 4

    /** Words shorter than this are never stretched: قُلۡ and هُوَ drawn long read as a different word. */
    private const val MIN_LETTERS = 3

    /** A word this long may lengthen a second join, as ٱلصَّمَدُ does on the last page of the print. */
    private const val TWO_JOIN_LETTERS = 4
}
