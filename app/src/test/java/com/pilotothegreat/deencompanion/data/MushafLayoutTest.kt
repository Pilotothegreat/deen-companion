package com.pilotothegreat.deencompanion.data

import com.pilotothegreat.deencompanion.data.quran.MushafLayout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * MushafLayout.kt is generated from Tanzil's quran-data.xml, and mushaf-layout.json is the same
 * data written out independently by the same script. Comparing them catches a hand edit to the
 * generated file; the rest of this class checks the invariants the divisions must satisfy, across
 * every one of the 6,236 ayahs rather than the three spot checks that stood here before.
 */
class MushafLayoutTest {

    private val golden = Json.parseToJsonElement(File("src/test/resources/mushaf-layout.json").readText()).jsonObject
    private val ayahCounts = Json.parseToJsonElement(File("src/main/assets/quran-ar.json").readText())
        .jsonObject.getValue("surahs").jsonArray.map { it.jsonObject.getValue("verses").jsonArray.size }

    private fun goldenPairs(name: String): List<Pair<Int, Int>> =
        golden.getValue(name).jsonArray.map { it.jsonArray.let { p -> p[0].jsonPrimitive.int to p[1].jsonPrimitive.int } }

    private fun IntArray.pairs(): List<Pair<Int, Int>> = (indices step 2).map { this[it] to this[it + 1] }

    private val allAyahs: List<Pair<Int, Int>> =
        ayahCounts.flatMapIndexed { index, count -> (1..count).map { (index + 1) to it } }

    @Test fun theGeneratedTablesMatchTheirSource() {
        assertEquals(goldenPairs("pages"), MushafLayout.pageStarts.pairs())
        assertEquals(goldenPairs("juzs"), MushafLayout.juzStarts.pairs())
        assertEquals(goldenPairs("quarters"), MushafLayout.quarterStarts.pairs())
        assertEquals(goldenPairs("manzils"), MushafLayout.manzilStarts.pairs())
        assertEquals(goldenPairs("rukus"), MushafLayout.rukuStarts.pairs())

        assertEquals(604, MushafLayout.PAGE_COUNT)
        assertEquals(30, MushafLayout.JUZ_COUNT)
        assertEquals(60, MushafLayout.HIZB_COUNT)
        assertEquals(7, MushafLayout.MANZIL_COUNT)
        assertEquals(556, MushafLayout.RUKU_COUNT)
    }

    @Test fun theSajdahsMatchTheirSourceWithTheirType() {
        val expected = golden.getValue("sajdas").jsonArray.map {
            val o = it.jsonObject
            Triple(o.getValue("surah").jsonPrimitive.int, o.getValue("ayah").jsonPrimitive.int, o.getValue("obligatory").jsonPrimitive.boolean)
        }
        assertEquals(expected, MushafLayout.sajdahList.map { Triple(it.surah, it.ayah, it.obligatory) })
        assertEquals(15, MushafLayout.sajdahs.size)
        assertEquals("the four the mushaf marks as obligatory", 4, MushafLayout.sajdahList.count { it.obligatory })
    }

    @Test fun everyDivisionCoversEveryAyahInOrder() {
        var page = 0
        var juz = 0
        var quarter = 0
        var manzil = 0
        var ruku = 0
        allAyahs.forEach { (surah, ayah) ->
            val where = "$surah:$ayah"
            val p = MushafLayout.pageOf(surah, ayah)
            val j = MushafLayout.juzOf(surah, ayah)
            val q = MushafLayout.quarterOf(surah, ayah)
            val m = MushafLayout.manzilOf(surah, ayah)
            val r = MushafLayout.rukuOf(surah, ayah)
            // Each division only ever moves forward, and never by more than one at a time.
            assertTrue("page goes backwards at $where", p == page || p == page + 1)
            assertTrue("juz goes backwards at $where", j == juz || j == juz + 1)
            assertTrue("quarter goes backwards at $where", q == quarter || q == quarter + 1)
            assertTrue("manzil goes backwards at $where", m == manzil || m == manzil + 1)
            assertTrue("ruku goes backwards at $where", r == ruku || r == ruku + 1)
            assertEquals("hizb of $where", (q - 1) / 4 + 1, MushafLayout.hizbOf(surah, ayah))
            assertEquals("rub of $where", (q - 1) % 4 + 1, MushafLayout.rubOf(surah, ayah))
            page = p
            juz = j
            quarter = q
            manzil = m
            ruku = r
        }
        // Reaching the last index means no division was left empty along the way.
        assertEquals("every page is used", 604, page)
        assertEquals("every juz is used", 30, juz)
        assertEquals("every quarter is used", 240, quarter)
        assertEquals("every manzil is used", 7, manzil)
        assertEquals("every ruku is used", 556, ruku)
        assertEquals(6236, allAyahs.size)
    }

    @Test fun eachJuzIsEightQuartersAndEachHizbIsFour() {
        val juzs = MushafLayout.juzStarts.pairs()
        val quarters = MushafLayout.quarterStarts.pairs()
        juzs.forEachIndexed { index, start ->
            assertEquals("juz ${index + 1} opens a hizb", start, quarters[index * 8])
        }
        (1..60).forEach { hizb ->
            val start = quarters[(hizb - 1) * 4]
            assertEquals("hizb $hizb", hizb, MushafLayout.hizbOf(start.first, start.second))
            assertEquals("hizb $hizb opens on its first quarter", 1, MushafLayout.rubOf(start.first, start.second))
        }
    }

    @Test fun everyStartIsReportedAsAStart() {
        MushafLayout.juzStarts.pairs().forEachIndexed { index, (surah, ayah) ->
            assertEquals(index + 1, MushafLayout.juzStartingAt(surah, ayah))
        }
        MushafLayout.quarterStarts.pairs().forEachIndexed { index, (surah, ayah) ->
            assertEquals(index + 1, MushafLayout.quarterStartingAt(surah, ayah))
        }
        // An ayah in the middle of a division opens nothing: 2:143 is well inside juz 2.
        assertEquals(null, MushafLayout.juzStartingAt(2, 143))
        assertEquals(null, MushafLayout.quarterStartingAt(2, 143))
    }

    @Test fun theFamiliarBoundariesAreWhereTheyBelong() {
        assertEquals("juz 4 begins at Al Imran 93", 4, MushafLayout.juzOf(3, 93))
        assertEquals(3, MushafLayout.juzOf(3, 92))
        assertEquals("Ayat al-Kursi", 42, MushafLayout.pageOf(2, 255))
        assertEquals(1, MushafLayout.pageOf(1, 7))
        assertEquals(604, MushafLayout.pageOf(114, 6))
        assertEquals(30, MushafLayout.juzOf(114, 6))
        assertEquals("Al-Kahf opens manzil 4", 4, MushafLayout.manzilOf(18, 1))
    }
}
