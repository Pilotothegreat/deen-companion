package com.pilotothegreat.deencompanion.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kashida may only lengthen a join the script actually makes. A tatweel after a letter that does not
 * connect, inside لا or the Name of Allah, or between a letter and its own vowel would draw a word no
 * printed mushaf contains.
 */
class KashidaTest {

    private fun isMark(c: Char) = Character.getType(c) == Character.NON_SPACING_MARK.toInt()

    @Test fun aJoinIsStretchedBeforeTheNextLetterAndAfterTheMarks() {
        // ب ِ س ۡ م ِ — the tatweel goes in front of س and of م, after each letter's own vowel or sukun.
        assertEquals(listOf(4, 2), Kashida.positions("بِسۡمِ"))
    }

    @Test fun neverAfterALetterThatDoesNotConnect() {
        assertEquals(emptyList<Int>(), Kashida.positions("دَارَ"))
        // ق joins the alef after it; the alef does not join the lam after it.
        assertEquals(listOf(2), Kashida.positions("قَالَ"))
        assertEquals(emptyList<Int>(), Kashida.positions("وَرَدُوٓاْ"))
    }

    @Test fun neverInsideLamAlef() {
        // ف joins ل; the ل and ا are drawn as one ligature.
        assertEquals(listOf(2), Kashida.positions("فَلَا"))
    }

    @Test fun neverInsideTheNameOfAllah() {
        assertEquals(emptyList<Int>(), Kashida.positions("ٱللَّهِ"))
        assertEquals(emptyList<Int>(), Kashida.positions("لِلَّهِ"))
    }

    @Test fun neverInsideAWordTheFontDrawsAsOneShape() {
        assertEquals(emptyList<Int>(), Kashida.positions("يَسۡجُدُونَۤ"))
        assertEquals(emptyList<Int>(), Kashida.positions("۞ بِسۡمِ"))
    }

    @Test fun neverOnAJoinAlreadyStretched() {
        assertTrue(Kashida.positions("بـسم").none { it == 2 })
    }

    @Test fun everyPositionSitsBetweenAConnectingLetterOrItsMarksAndTheNextLetter() {
        val words = listOf("بِسۡمِ", "ٱلرَّحۡمَٰنِ", "ٱلرَّحِيمِ", "مَٰلِكِ", "يَوۡمِ", "ٱلدِّينِ", "نَسۡتَعِينُ", "ٱلۡمُسۡتَقِيمَ")
        words.forEach { word ->
            Kashida.positions(word).forEach { at ->
                assertTrue("$word at $at: must be before a letter", !isMark(word[at]) && word[at] != ' ')
                assertTrue("$word at $at: must follow a letter or its marks", at > 0 && word[at - 1] != ' ')
            }
        }
    }

    @Test fun justifyingFillsTheTargetWithoutPassingIt() {
        val words = listOf("بِسۡمِ", "ٱللَّهِ", "ٱلرَّحۡمَٰنِ", "ٱلرَّحِيمِ")
        // Each character three units wide, so one tatweel is three units: the result must land within
        // one tatweel of the target and never beyond it.
        val measure = { word: String -> word.length * 3f }
        val natural = words.sumOf { measure(it).toDouble() }.toFloat()
        val target = natural + 14f
        val justified = Kashida.justify(words, target, measure)
        val width = justified.sumOf { measure(it).toDouble() }.toFloat()
        assertTrue("width $width passes the target $target", width <= target)
        assertTrue("width $width falls short of $target by more than a tatweel", target - width < 3f)
        assertEquals("only tatweels are added", words, justified.map { it.replace(Kashida.TATWEEL.toString(), "") })
    }

    @Test fun aWordStretchesAtMostTwoJoinsAndNoJoinBeyondTheCap() {
        val words = listOf("بِسۡمِ", "مَٰلِكِ", "ٱلصَّمَدُ")
        val justified = Kashida.justify(words, target = 10_000f, measure = { it.length.toFloat() })
        val runs = justified.map { word -> Regex("${Kashida.TATWEEL}+").findAll(word).map { it.value.length }.toList() }
        assertEquals("a three-letter word stretches one join", 1, runs[0].size)
        assertEquals("a three-letter word stretches one join", 1, runs[1].size)
        assertEquals("a longer word stretches two", 2, runs[2].size)
        assertTrue("no join beyond ${Kashida.MAX_PER_JOIN}", runs.flatten().all { it <= Kashida.MAX_PER_JOIN })
        // The join before the last letter is always one of them: ٱلصَّمَـدُ.
        assertTrue(justified[2].contains("ـد"))
    }

    @Test fun wordsOfOneOrTwoLettersAreNeverStretched() {
        val words = listOf("قُلۡ", "هُوَ", "ٱللَّهُ", "أَحَدٌ")
        val justified = Kashida.justify(words, target = 10_000f, measure = { it.length.toFloat() })
        assertEquals("قُلۡ", justified[0])
        assertEquals("هُوَ", justified[1])
        assertEquals("the Name of Allah", "ٱللَّهُ", justified[2])
    }

    @Test fun aLineAlreadyFullIsLeftAlone() {
        val words = listOf("بِسۡمِ", "ٱللَّهِ")
        assertEquals(words, Kashida.justify(words, target = 5f, measure = { it.length.toFloat() }))
    }
}
