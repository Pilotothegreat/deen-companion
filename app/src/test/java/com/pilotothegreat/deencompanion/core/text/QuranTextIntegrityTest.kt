package com.pilotothegreat.deencompanion.core.text

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * The Quran text must ship exactly as the King Fahd Complex publishes it.
 *
 * Since 2.1 it is the Complex's own QPC Hafs text, because that is what its font is built to draw: with
 * Tanzil's text the same font drew the silent-letter circle as a dotted circle inside the word. These
 * checks keep the text complete, untouched and paired with its line table.
 */
class QuranTextIntegrityTest {

    private val arabic = Json.parseToJsonElement(File("src/main/assets/quran-ar.json").readText()).jsonObject
    private val surahs = arabic.getValue("surahs").jsonArray
    private val hashes = Json.parseToJsonElement(File("src/test/resources/quran-hashes.json").readText()).jsonObject
    private val whole: String = surahs.joinToString("\n") { surah ->
        surah.jsonObject.getValue("verses").jsonArray.joinToString("\n") { it.jsonPrimitive.content }
    }

    @Test fun everySurahMatchesItsRecordedChecksum() {
        assertEquals(114, surahs.size)
        surahs.forEachIndexed { index, surah ->
            val verses = surah.jsonObject.getValue("verses").jsonArray.map { it.jsonPrimitive.content }
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(verses.joinToString("\n").toByteArray())
                .joinToString("") { "%02x".format(it) }
            val number = (index + 1).toString()
            assertEquals("surah $number has changed", hashes.getValue(number).jsonPrimitive.content, digest)
        }
    }

    @Test fun theQuranIsComplete() {
        val counts = surahs.map { it.jsonObject.getValue("verses").jsonArray.size }
        assertEquals("total ayahs", 6236, counts.sum())
        assertEquals("Al-Fatihah", 7, counts[0])
        assertEquals("Al-Baqarah", 286, counts[1])
        assertEquals("An-Nas", 6, counts[113])
        assertTrue("no empty ayah", surahs.all { s -> s.jsonObject.getValue("verses").jsonArray.all { it.jsonPrimitive.content.isNotBlank() } })
    }

    /** The printed sukun is the Complex's own jazm mark; it is what distinguishes this text from Tanzil's. */
    @Test fun theTextIsTheKingFahdComplexEncoding() {
        assertTrue("the jazm sukun (U+06E1) carries the text", whole.count { it == 'ۡ' } > 10_000)
        assertTrue("harakat are present", whole.count { it == 'َ' } > 100_000)
    }

    /**
     * The ayah numbers are stored apart from the words, so search, copy and share get words; the page
     * puts each number back and the font draws the rosette around it.
     */
    @Test fun theAyahNumbersAreNotInTheWords() {
        assertEquals("no Arabic-Indic digits in the text", 0, whole.count { it in '٠'..'٩' })
        assertEquals("no end-of-ayah marks in the text", 0, whole.count { it == '۝' })
    }

    /**
     * The mushaf writes the Basmala as a heading, not as part of ayah 1, and the reader draws it from
     * this field; printing it from both would show it twice.
     */
    @Test fun theBasmalaIsAHeadingAndNotPartOfAyahOne() {
        val basmala = ArabicText.normalize("بسم الله الرحمن الرحيم")
        val withHeading = surahs.mapIndexedNotNull { index, surah -> (index + 1).takeIf { surah.jsonObject["bismillah"] != null } }
        assertEquals("every surah but al-Fatihah and at-Tawbah", (2..114).toList() - 9, withHeading)
        surahs.forEach { surah ->
            val heading = surah.jsonObject["bismillah"]?.jsonPrimitive?.content
            if (heading != null) assertEquals("the heading is the Basmala", basmala, ArabicText.normalize(heading))
        }
        val openings = surahs.drop(1).map { it.jsonObject.getValue("verses").jsonArray[0].jsonPrimitive.content }
        assertTrue("ayah 1 never repeats the Basmala", openings.none { ArabicText.normalize(it).startsWith(basmala) })
    }

    /**
     * The line table is generated against this exact text. Regenerated one without the other, every word
     * after the first difference lands on the wrong line while the page still looks plausible.
     */
    @Test fun theLineTableCoversThisTextAndNoOther() {
        val table = Json.parseToJsonElement(File("src/main/assets/mushaf-lines.json").readText()).jsonObject
        val tokens = surahs.sumOf { surah ->
            surah.jsonObject.getValue("verses").jsonArray.sumOf { it.jsonPrimitive.content.split(' ').size }
        }
        assertEquals("the table was built against a different text", tokens, table.getValue("tokens").jsonPrimitive.content.toInt())
        val ends = table.getValue("ends").jsonArray.flatMap { page -> page.jsonArray.map { it.jsonPrimitive.content.toInt() } }
        assertEquals(604 * 15, ends.size)
        assertEquals(tokens - 1, ends.last())
        assertTrue("the lines run backwards somewhere", ends.zipWithNext().all { (a, b) -> b >= a })
    }

    @Test fun theTextAndTranslationAreCredited() {
        val source = arabic.getValue("source").jsonObject
        assertTrue(source.getValue("name").jsonPrimitive.content.contains("King Fahd"))
        assertTrue(source.getValue("source").jsonPrimitive.content.startsWith("https://"))
        assertTrue("the terms forbid changing the text", source.getValue("terms").jsonPrimitive.content.contains("not allowed"))

        val translation = Json.parseToJsonElement(File("src/main/assets/quran-tr-clearquran.json").readText()).jsonObject
        assertEquals("clearquran", translation.getValue("id").jsonPrimitive.content)
        assertEquals("Talal Itani", translation.getValue("translator").jsonPrimitive.content)
        assertEquals("CC BY-ND 4.0", translation.getValue("license").jsonPrimitive.content)
        assertFalse(translation.getValue("attribution").jsonPrimitive.content.isBlank())
        assertEquals(6236, translation.getValue("verses").jsonArray.sumOf { it.jsonArray.size })
    }
}
