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
 * The Quran text must ship exactly as Tanzil publishes it. Their licence forbids altering it, and
 * until 1.8.0 the app rewrote hamza sequences at load time, deleting more than fifteen thousand
 * vowel marks across 82% of the ayahs. These checks make that impossible to reintroduce quietly.
 */
class QuranTextIntegrityTest {

    private val arabic = Json.parseToJsonElement(File("src/main/assets/quran-ar.json").readText()).jsonObject
    private val surahs = arabic.getValue("surahs").jsonArray
    private val hashes = Json.parseToJsonElement(File("src/test/resources/quran-hashes.json").readText()).jsonObject

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
        assertTrue("no empty surah", counts.all { it > 0 })
    }

    @Test fun vowelMarksSurvive() {
        // 1:5 is the ayah the old "font fix" damaged first: إِيَّاكَ lost its kasra.
        val fatihah = surahs[0].jsonObject.getValue("verses").jsonArray.map { it.jsonPrimitive.content }
        assertTrue("kasra under the hamza of إِيَّاكَ", fatihah[4].contains("إِ"))

        val kasraAfterHamza = surahs.sumOf { surah ->
            surah.jsonObject.getValue("verses").jsonArray.count { it.jsonPrimitive.content.contains("إِ") }
        }
        assertTrue("the text still carries its harakat ($kasraAfterHamza ayahs)", kasraAfterHamza > 1_000)
    }

    /**
     * The mushaf writes the Basmala as a heading, not as part of ayah 1, and the reader draws it
     * from this field. Tanzil's plain-text export glues it onto the first ayah instead, which both
     * corrupts the ayah and prints the Basmala twice on screen.
     */
    @Test fun theBasmalaIsAHeadingAndNotPartOfAyahOne() {
        // Compared on letters alone: Tanzil marks the ba' of the heading with a shadda in Al-Tin and
        // Al-Qadr and not elsewhere, and the text ships exactly as they publish it.
        val basmala = ArabicText.normalize("\u0628\u0633\u0645 \u0627\u0644\u0644\u0647 \u0627\u0644\u0631\u062d\u0645\u0646 \u0627\u0644\u0631\u062d\u064a\u0645")
        val withHeading = surahs.mapIndexedNotNull { index, surah ->
            (index + 1).takeIf { surah.jsonObject["bismillah"] != null }
        }
        assertEquals("every surah but Al-Fatihah and At-Tawbah", (2..114).toList() - 9, withHeading)
        surahs.forEach { surah ->
            val heading = surah.jsonObject["bismillah"]?.jsonPrimitive?.content
            if (heading != null) assertEquals("the heading is the Basmala", basmala, ArabicText.normalize(heading))
        }

        val openings = surahs.drop(1).map { it.jsonObject.getValue("verses").jsonArray[0].jsonPrimitive.content }
        assertTrue("ayah 1 never repeats the Basmala", openings.none { ArabicText.normalize(it).startsWith(basmala) })
    }

    @Test fun theTextAndTranslationAreCredited() {
        val source = arabic.getValue("source").jsonObject
        assertFalse(source.getValue("name").jsonPrimitive.content.isBlank())
        assertTrue(source.getValue("source").jsonPrimitive.content.startsWith("https://"))
        assertEquals("CC BY 3.0", source.getValue("license").jsonPrimitive.content)
        assertTrue("the terms forbid changing the text", source.getValue("terms").jsonPrimitive.content.contains("not allowed"))

        val translation = Json.parseToJsonElement(File("src/main/assets/quran-tr-clearquran.json").readText()).jsonObject
        assertEquals("clearquran", translation.getValue("id").jsonPrimitive.content)
        assertEquals("Talal Itani", translation.getValue("translator").jsonPrimitive.content)
        assertEquals("CC BY-ND 4.0", translation.getValue("license").jsonPrimitive.content)
        assertFalse(translation.getValue("attribution").jsonPrimitive.content.isBlank())
        assertEquals(114, translation.getValue("verses").jsonArray.size)
        assertEquals(6236, translation.getValue("verses").jsonArray.sumOf { it.jsonArray.size })
    }
}
