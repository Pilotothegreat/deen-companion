package com.pilotothegreat.deencompanion.data

import com.pilotothegreat.deencompanion.data.quran.LineKind
import com.pilotothegreat.deencompanion.data.quran.MushafLines
import com.pilotothegreat.deencompanion.data.quran.Revelation
import com.pilotothegreat.deencompanion.data.quran.Surah
import com.pilotothegreat.deencompanion.data.quran.Verse
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.robolectric.annotation.Config
import java.io.File

/**
 * The line table is the difference between a page of the mushaf and a paragraph that happens to
 * contain the same words. It is generated, so what matters is that it still fits the text it was
 * generated against — a table one token out of step would misplace every word after it and nothing
 * about the page would look obviously wrong.
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
        parsed.forEachIndexed { index, lines ->
            assertEquals("page ${index + 1}", MushafLines.LINES_PER_PAGE, lines.size)
        }
    }

    @Test fun everyTokenOfTheTextAppearsOnExactlyOneLineInOrder() {
        val parsed = requireNotNull(pages)
        val printed = parsed.flatten().flatMap { line -> line.words.map { it.text } }
        // A printed word may join several of Tanzil's tokens, so the comparison is on the tokens.
        val fromLines = printed.flatMap { it.split(' ') }
        val fromText = surahs.flatMap { surah -> surah.verses.flatMap { it.text.split(' ') } }
        assertEquals("the table covers a different number of tokens", fromText.size, fromLines.size)
        assertEquals("the table puts the tokens in a different order", fromText, fromLines)
    }

    @Test fun everyAyahEndsExactlyOnce() {
        val parsed = requireNotNull(pages)
        val ends = parsed.flatten().flatMap { line -> line.words.filter { it.endsAyah } }
        assertEquals(6236, ends.size)
        assertEquals("an ayah ends twice", ends.size, ends.map { it.surah to it.ayah }.distinct().size)
    }

    @Test fun everySurahIsAnnouncedAndOnlyTwoOpenWithoutABasmala() {
        val parsed = requireNotNull(pages)
        val flat = parsed.flatten()
        val banners = flat.filter { it.kind == LineKind.SURAH_HEADER }
        val basmalas = flat.filter { it.kind == LineKind.BASMALA }
        assertEquals("every surah gets its name in a band", 114, banners.size)
        assertEquals(114, banners.mapNotNull { it.surah }.distinct().size)
        // al-Fatihah's Basmala is its first ayah; at-Tawbah opens without one.
        assertEquals(112, basmalas.size)
        assertTrue(1 !in basmalas.mapNotNull { it.surah })
        assertTrue(9 !in basmalas.mapNotNull { it.surah })
    }

    @Test fun theFamiliarPagesLookThePartTheyShould() {
        val parsed = requireNotNull(pages)
        val page1 = parsed[0]
        assertEquals("page 1 opens with al-Fatihah's band", LineKind.SURAH_HEADER, page1[0].kind)
        assertEquals(1, page1[0].surah)
        assertEquals("its first line of words is the Basmala as ayah 1", "بِسْمِ", page1[1].words.first().text)
        assertTrue("al-Fatihah has no separate Basmala line", page1.none { it.kind == LineKind.BASMALA })

        val page2 = parsed[1]
        assertEquals(LineKind.SURAH_HEADER, page2[0].kind)
        assertEquals(LineKind.BASMALA, page2[1].kind)
        assertEquals(2, page2[1].surah)

        val last = parsed[603]
        assertEquals("the mushaf ends on an-Nas", 114, last.last { it.words.isNotEmpty() }.words.last().surah)
        assertEquals(6, last.last { it.words.isNotEmpty() }.words.last().ayah)
    }

    @Test fun theSajdahSignRidesWithItsWordRatherThanStandingAlone() {
        val parsed = requireNotNull(pages)
        val lone = parsed.flatten().flatMap { it.words }.filter { it.text.trim() == SAJDAH }
        assertEquals("the sajdah sign is never a word of its own", emptyList<Any>(), lone)
        val carrying = parsed.flatten().flatMap { it.words }.count { it.text.contains(SAJDAH) }
        assertEquals("one prostration mark per sajdah ayah", 15, carrying)
    }

    @Test fun aPauseMarkIsNeverStrandedBetweenTwoWords() {
        val parsed = requireNotNull(pages)
        val stranded = parsed.flatten()
            .flatMap { it.words }
            .filter { word -> word.text.isNotEmpty() && word.text.all { it.code in MARKS || it == ' ' } }
        assertEquals("a pause mark must belong to the word before it", emptyList<Any>(), stranded)
    }

    @Test fun aTableThatDoesNotFitIsRefusedRatherThanDrawnWrong() {
        val broken = JSONObject(asset).put("tokens", 1).toString()
        assertEquals(null, MushafLines.parse(broken, surahs))
        assertEquals(null, MushafLines.parse("{", surahs))
        assertNotNull(MushafLines.parse(asset, surahs))
    }

    private fun readSurahs(): List<Surah> {
        val root = JSONObject(File("src/main/assets/quran-ar.json").readText())
        val array = root.getJSONArray("surahs")
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
        val MARKS = (0x06D6..0x06ED).toSet()
    }
}

