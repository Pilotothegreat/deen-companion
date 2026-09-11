package com.pilotothegreat.deencompanion.core.text

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class DailyContentTest {

    @Test
    fun dailyVersesExistAndFitACard() {
        val quran = Json.parseToJsonElement(File("src/main/assets/quran.json").readText()).jsonArray
        assertTrue(DailyVerse.all.size >= 100)
        assertEquals("duplicate ayah", DailyVerse.all.size, DailyVerse.all.toSet().size)
        DailyVerse.all.forEach { ref ->
            val verse = quran.getOrNull(ref.surah - 1)?.jsonObject?.get("verses")?.jsonArray?.getOrNull(ref.ayah - 1)?.jsonObject
            assertNotNull("$ref is not in the Quran", verse)
            assertTrue("$ref is too long for a card", verse!!.getValue("text").jsonPrimitive.content.length <= 220)
        }
    }

    @Test
    fun inspirationsAreComplete() {
        assertTrue(Inspirations.all.size >= 60)
        assertEquals("duplicate text", Inspirations.all.size, Inspirations.all.map { it.arabic }.toSet().size)
        Inspirations.all.forEach {
            assertTrue(it.toString(), listOf(it.arabic, it.english, it.sourceArabic, it.sourceEnglish).none(String::isBlank))
        }
    }

    @Test
    fun consecutiveDaysVisitEveryEntryWithoutRepeating() {
        val start = LocalDate.of(2026, 12, 20)
        val days = (0L until 400L).map { start.plusDays(it) }
        assertEquals(DailyVerse.all.toSet(), days.take(DailyVerse.all.size).map(DailyVerse::forDate).toSet())
        assertEquals(Inspirations.all.toSet(), days.take(Inspirations.all.size).map(Inspirations::forDate).toSet())
        days.zipWithNext().forEach { (a, b) ->
            assertNotEquals(DailyVerse.forDate(a), DailyVerse.forDate(b))
            assertNotEquals(Inspirations.forDate(a), Inspirations.forDate(b))
        }
    }
}
