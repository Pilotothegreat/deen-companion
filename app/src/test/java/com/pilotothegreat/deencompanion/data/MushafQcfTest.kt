package com.pilotothegreat.deencompanion.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.data.quran.LineKind
import com.pilotothegreat.deencompanion.data.quran.MushafQcf
import com.pilotothegreat.deencompanion.data.quran.Revelation
import com.pilotothegreat.deencompanion.data.quran.Surah
import com.pilotothegreat.deencompanion.data.quran.Verse
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * The print-exact table pairs the King Fahd Complex's QCF V2 glyphs with the verified text. The glyphs
 * cannot be checked by eye, so what is checked is that every line still holds the text's words in order
 * and every ayah closes exactly once: a table one word out of step still draws a plausible page.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class MushafQcfTest {

    private val asset = File("src/main/assets/mushaf-qcf2.json").readText()
    private val golden = File("src/test/resources/mushaf-qcf2.json").readText()
    private val surahs: List<Surah> = readSurahs()
    private val table = requireNotNull(MushafQcf.parse(asset, surahs)) { "the QCF table does not fit the text" }

    @Test fun theShippedTableIsTheGeneratedOne() {
        assertEquals("mushaf-qcf2.json was edited by hand", golden, asset)
    }

    @Test fun everyPageHasFifteenLines() {
        assertEquals(604, table.pages.size)
        table.pages.forEachIndexed { index, lines -> assertEquals("page ${index + 1}", 15, lines.size) }
    }

    @Test fun theLinesSayTheTextInOrder() {
        val number = Regex(" [٠-٩]+$")
        val fromLines = table.pages.flatten().filter { it.text.isNotEmpty() }
            .flatMap { line -> line.text.split(' ') }
            .map { it.replace(number, "") }
        val fromText = surahs.flatMap { surah -> surah.verses.flatMap { it.text.split(' ') } }
        assertEquals("the table holds the words in a different order", fromText, fromLines)
    }

    @Test fun everyAyahClosesOnceOnItsRosette() {
        val closing = table.pages.flatten().flatMap { line -> line.ayahs.filterIndexed { i, _ -> line.rosettes[i] } }
        assertEquals(6236, closing.size)
        assertEquals("an ayah closes twice", closing.size, closing.distinct().size)
    }

    @Test fun everyWordHasAnAyahAndAGlyph() {
        table.pages.flatten().forEach { line ->
            assertEquals(line.words.size, line.ayahs.size)
            assertTrue("an empty glyph", line.words.all { it.isNotEmpty() })
        }
    }

    @Test fun everySurahHasItsBandAndAllButTwoTheirBasmala() {
        val kinds = table.pages.flatten().map { it.kind }
        assertEquals(114, kinds.count { it == LineKind.SURAH_HEADER })
        assertEquals(112, kinds.count { it == LineKind.BASMALA })
    }

    @Test fun theOpeningPagesAreCentred() {
        for (page in 1..2) {
            assertTrue(table.page(page).none { it.kind == LineKind.AYAH })
        }
    }

    @Test fun aRecitationFindsThePageItsAyahIsDrawnOn() {
        assertEquals(1, table.pageOf(1, 1))
        assertEquals(604, table.pageOf(114, 6))
        // The 1421H print starts 5:77 on page 120, a page earlier than the 1405H table.
        assertEquals(120, table.pageOf(5, 77))
    }

    @Test fun aTableThatDoesNotFitTheTextIsRefused() {
        assertNull(MushafQcf.parse(asset, surahs.dropLast(1)))
        assertNull(MushafQcf.parse("{}", surahs))
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
}
