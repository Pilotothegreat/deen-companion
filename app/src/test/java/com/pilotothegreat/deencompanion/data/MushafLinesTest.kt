package com.pilotothegreat.deencompanion.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.data.quran.LineKind
import com.pilotothegreat.deencompanion.data.quran.MushafLayout
import com.pilotothegreat.deencompanion.data.quran.MushafLines
import com.pilotothegreat.deencompanion.data.quran.Revelation
import com.pilotothegreat.deencompanion.data.quran.Surah
import com.pilotothegreat.deencompanion.data.quran.Verse
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * The line table is the difference between a page of the mushaf and a paragraph that happens to
 * contain the same words. It is generated, so what matters is that it still fits the text it was
 * generated against: one token out of step misplaces every word after it and the page still looks
 * plausible.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class MushafLinesTest {

    private val asset = File("src/main/assets/mushaf-lines.json").readText()
    private val golden = File("src/test/resources/mushaf-lines.json").readText()
    private val surahs: List<Surah> = readSurahs()
    private val pages = MushafLines.parse(asset, surahs)

    @Test fun theShippedTableIsTheGeneratedOne() {
        assertEquals("mushaf-lines.json was edited by hand", golden, asset)
    }

    @Test fun everyPageHasFifteenLines() {
        val parsed = requireNotNull(pages) { "the table does not fit the text" }
        assertEquals(604, parsed.size)
        parsed.forEachIndexed { index, lines -> assertEquals("page ${index + 1}", MushafLines.LINES_PER_PAGE, lines.size) }
    }

    @Test fun everyTokenOfTheTextSitsOnExactlyOneLineInOrder() {
        val parsed = requireNotNull(pages)
        val fromLines = parsed.flatten().flatMap { line -> line.words.flatMap { it.text.split(' ') } }
        val fromText = surahs.flatMap { surah -> surah.verses.flatMap { it.text.split(' ') } }
        assertEquals("the table puts the words in a different order", fromText, fromLines)
    }

    @Test fun everyAyahEndsExactlyOnce() {
        val ends = requireNotNull(pages).flatten().flatMap { line -> line.words.filter { it.endsAyah } }
        assertEquals(6236, ends.size)
        assertEquals("an ayah ends twice", ends.size, ends.map { it.surah to it.ayah }.distinct().size)
    }

    /**
     * The check that would have caught 2.0. A word that begins with a combining mark has nothing for
     * the mark to sit on, and Android draws it detached, over a dotted circle — 4,361 such words shipped
     * because the old test only looked for words made entirely of marks.
     */
    @Test fun noPrintedWordBeginsWithAMark() {
        val orphaned = requireNotNull(pages).flatten()
            .flatMap { it.words }
            .filter { word -> word.text.isEmpty() || Character.getType(word.text[0]) == Character.NON_SPACING_MARK.toInt() }
            .map { "${it.surah}:${it.ayah} ${it.text}" }
        assertEquals("words with nothing under their first mark", emptyList<String>(), orphaned)
    }

    @Test fun everySurahIsAnnouncedAndOnlyTwoOpenWithoutABasmala() {
        val flat = requireNotNull(pages).flatten()
        val bands = flat.filter { it.kind == LineKind.SURAH_HEADER }
        val basmalas = flat.filter { it.kind == LineKind.BASMALA }
        assertEquals("every surah is announced by its band", 114, bands.mapNotNull { it.surah }.distinct().size)
        assertEquals(114, bands.size)
        // al-Fatihah's Basmala is its first ayah; at-Tawbah opens without one.
        assertEquals(112, basmalas.size)
        assertTrue(basmalas.none { it.surah == 1 || it.surah == 9 })
    }

    @Test fun theFamiliarPagesLookThePartTheyShould() {
        val parsed = requireNotNull(pages)
        val page1 = parsed[0]
        assertEquals("page 1 opens with al-Fatihah's band", LineKind.SURAH_HEADER, page1[0].kind)
        assertEquals(1, page1[0].surah)
        assertEquals("its first words are the Basmala, as ayah 1", 1, page1[1].words.first().ayah)
        assertTrue("al-Fatihah has no separate Basmala line", page1.none { it.kind == LineKind.BASMALA })
        assertTrue("the opening pages are centred", page1.filter { it.words.isNotEmpty() }.all { it.kind == LineKind.CENTRED })

        val page2 = parsed[1]
        assertEquals(LineKind.SURAH_HEADER, page2[0].kind)
        assertEquals(LineKind.BASMALA, page2[1].kind)
        assertEquals(2, page2[1].surah)

        val lastWord = parsed[603].last { it.words.isNotEmpty() }.words.last()
        assertEquals("the mushaf ends on an-Nas 6", 114 to 6, lastWord.surah to lastWord.ayah)
    }

    @Test fun theSajdahSignIsOnTheFifteenSajdahsAndNowhereElse() {
        val carrying = requireNotNull(pages).flatten()
            .flatMap { it.words }
            .filter { SAJDAH in it.text }
            .map { it.surah to it.ayah }
            .toSet()
        assertEquals(MushafLayout.sajdahs.keys, carrying)
    }

    @Test fun aTableThatDoesNotFitIsRefusedRatherThanDrawnWrong() {
        assertNull(MushafLines.parse(JSONObject(asset).put("tokens", 1).toString(), surahs))
        assertNull(MushafLines.parse("{", surahs))
        assertNotNull(MushafLines.parse(asset, surahs))
    }

    private fun readSurahs(): List<Surah> {
        val array = JSONObject(File("src/main/assets/quran-ar.json").readText()).getJSONArray("surahs")
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val number = obj.getInt("id")
            val verses = obj.getJSONArray("verses")
            Surah(
                number = number,
                nameArabic = obj.getString("name"),
                nameEnglish = obj.getString("transliteration"),
                revelation = Revelation.MECCAN,
                verses = (0 until verses.length()).map { j -> Verse(number, j + 1, verses.getString(j), "") },
                bismillah = obj.optString("bismillah").takeIf { it.isNotEmpty() },
            )
        }
    }

    private companion object {
        const val SAJDAH = "۩"
    }
}
