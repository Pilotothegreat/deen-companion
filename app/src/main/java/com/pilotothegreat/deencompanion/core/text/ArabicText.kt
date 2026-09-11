package com.pilotothegreat.deencompanion.core.text

import java.util.Locale

/** Search-oriented text folding for Arabic (diacritics, letter variants) and Latin (case). */
object ArabicText {

    fun normalize(text: String): String = buildString(text.length) {
        for (c in text) if (!isMark(c)) append(fold(c))
    }

    /**
     * Range of [text] matching [query] once both are normalized, or null. The range covers the
     * original characters, including diacritics that trail the last matched letter.
     */
    fun findMatch(text: String, query: String): IntRange? {
        val normalizedQuery = normalize(query).trim()
        if (normalizedQuery.isEmpty()) return null
        val folded = StringBuilder(text.length)
        val sourceIndex = IntArray(text.length)
        text.forEachIndexed { i, c ->
            if (!isMark(c)) {
                sourceIndex[folded.length] = i
                folded.append(fold(c))
            }
        }
        val idx = folded.indexOf(normalizedQuery)
        if (idx < 0) return null
        val start = sourceIndex[idx]
        var end = sourceIndex[idx + normalizedQuery.length - 1] + 1
        while (end < text.length && isMark(text[end])) end++
        return start until end
    }

    fun containsArabic(text: String): Boolean = text.any { it in '؀'..'ۿ' }

    private fun isMark(c: Char): Boolean =
        c in 'ؐ'..'ؚ' || c in 'ً'..'ٟ' || c == 'ٰ' ||
            c in 'ۖ'..'ۭ' || c == 'ـ'

    private fun fold(c: Char): Char = when (c) {
        'أ', 'إ', 'آ', 'ٱ' -> 'ا'
        'ة' -> 'ه'
        'ى' -> 'ي'
        else -> c.lowercaseChar()
    }
}

object Numerals {
    fun toArabicIndic(text: String): String = buildString(text.length) {
        for (c in text) append(if (c in '0'..'9') '٠' + (c - '0') else c)
    }

    /** Arabic-Indic digits for Arabic UI, unchanged otherwise. */
    fun localize(text: String, locale: Locale): String =
        if (locale.language == "ar") toArabicIndic(text) else text

    fun format(number: Int, locale: Locale): String = localize(number.toString(), locale)
}
