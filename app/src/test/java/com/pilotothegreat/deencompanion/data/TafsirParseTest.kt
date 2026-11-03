package com.pilotothegreat.deencompanion.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pilotothegreat.deencompanion.data.quran.Tafsir
import com.pilotothegreat.deencompanion.data.quran.TafsirPassage
import com.pilotothegreat.deencompanion.data.quran.TafsirText
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * Commentaries explain a run of ayahs together more often than not, and the two feeds write it
 * differently. What the reader must never do is show "nothing on this ayah" for an ayah a passage
 * covers, or show the passage for the wrong ayahs.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class TafsirParseTest {

    @Test fun anAyahInARunGetsTheRunsPassage() {
        val passages = listOf("one", "two and three", "", "four", "", "")
        assertEquals(TafsirPassage("one", 1, 1), TafsirText.passage(passages, 1))
        assertEquals(TafsirPassage("two and three", 2, 3), TafsirText.passage(passages, 3))
        assertEquals(TafsirPassage("four", 4, 6), TafsirText.passage(passages, 6))
    }

    @Test fun outsideTheSurahThereIsNoPassage() {
        assertNull(TafsirText.passage(listOf("one"), 0))
        assertNull(TafsirText.passage(listOf("one"), 2))
        assertNull(TafsirText.passage(listOf("", ""), 2))
    }

    @Test fun quranComEntriesLandOnTheirAyahs() {
        val json = """{"tafsirs":[{"verse_key":"2:1","text":"<p>a</p>"},{"verse_key":"2:3","text":"c"},{"verse_key":"3:1","text":"x"}]}"""
        assertEquals(listOf("<p>a</p>", "", "c"), TafsirText.fromQuranCom(json, surah = 2, ayahs = 3))
    }

    @Test fun mirrorEntriesLandOnTheirAyahs() {
        val json = """[{"surah":2,"ayah":2,"text":"b"},{"surah":2,"ayah":9,"text":"beyond"}]"""
        assertEquals(listOf("", "b"), TafsirText.fromMirror(json, surah = 2, ayahs = 2))
    }

    @Test fun theBundledMuyassarCoversEverySurah() {
        val root = JSONObject(File("src/main/assets/tafsir-muyassar.json").readText())
        val surahs = root.getJSONArray("surahs")
        val counts = JSONObject(File("src/main/assets/quran-ar.json").readText()).getJSONArray("surahs")
        assertEquals(114, surahs.length())
        for (i in 0 until 114) {
            val passages = surahs.getJSONArray(i)
            assertEquals("surah ${i + 1}", counts.getJSONObject(i).getJSONArray("verses").length(), passages.length())
            assertTrue("surah ${i + 1} opens with nothing", passages.getString(0).isNotBlank())
        }
    }

    @Test fun eachLanguageOpensOnACommentaryInIt() {
        assertTrue(Tafsir.defaultFor(arabicUi = true).arabic)
        assertTrue(!Tafsir.defaultFor(arabicUi = false).arabic)
        assertEquals(Tafsir.entries.size, Tafsir.entries.map { it.id }.distinct().size)
    }
}
