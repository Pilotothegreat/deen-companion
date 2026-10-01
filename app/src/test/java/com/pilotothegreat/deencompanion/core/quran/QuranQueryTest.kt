package com.pilotothegreat.deencompanion.core.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the search field understands.
 *
 * Ayat al-Kursi is the case that started this: its name is nowhere in its words, so a search that only
 * looks through the text answers "no results" to the most searched-for ayah in the Quran.
 */
class QuranQueryTest {

    /** Enough of a mushaf to check a reference against: al-Fatihah's 7, al-Baqarah's 286, an-Nas's 6. */
    private val verseCount: (Int) -> Int = { surah ->
        when (surah) {
            1 -> 7
            2 -> 286
            24 -> 64
            36 -> 83
            113 -> 5
            114 -> 6
            else -> 100
        }
    }

    private val surahNamed: (String) -> Int? = { name ->
        when (QuranQuery.fold(name).replace(" ", "")) {
            "albaqarah", "baqarah" -> 2
            "alkahf", "kahf" -> 18
            "yasin", "yaseen" -> 36
            "البقره" -> 2
            else -> null
        }
    }

    private fun parse(raw: String) = QuranQuery.parse(raw, verseCount, surahNamed)

    @Test fun aPassageIsFoundByTheNameItIsKnownBy() {
        listOf("ayat al kursi", "Ayatul Kursi", "ayat-ul-kursi", "آية الكرسي", "the throne verse").forEach { spelling ->
            val destinations = parse(spelling).destinations
            assertEquals(
                "\"$spelling\" should open Ayat al-Kursi",
                QuranDestination.Ayah(2, 255, NamedPassage.AYAT_AL_KURSI),
                destinations.firstOrNull(),
            )
        }
    }

    @Test fun aPassageIsOfferedWhileItIsStillBeingTyped() {
        assertEquals(
            QuranDestination.Ayah(2, 255, NamedPassage.AYAT_AL_KURSI),
            parse("ayat al ku").destinations.firstOrNull(),
        )
    }

    @Test fun aPassageOfMoreThanOneAyahOpensAtItsFirst() {
        val passage = NamedPassage.KHAWATIM_AL_BAQARAH
        assertEquals(QuranDestination.Ayah(2, 285, passage), parse("khawatim al baqarah").destinations.first())
        assertEquals(286, passage.lastAyah)
        assertTrue(!passage.isSingleAyah)
    }

    @Test fun aReferenceIsReadInEitherScript() {
        assertEquals(QuranDestination.Ayah(2, 255), parse("2:255").destinations.first())
        assertEquals(QuranDestination.Ayah(2, 255), parse("٢:٢٥٥").destinations.first())
        assertEquals(QuranDestination.Ayah(2, 255), parse("surah 2 ayah 255").destinations.first())
        assertEquals(QuranDestination.Ayah(2, 255), parse("al baqarah 255").destinations.first())
        assertEquals(QuranDestination.Ayah(2, 255), parse("البقرة ٢٥٥").destinations.first())
    }

    @Test fun anAyahThatDoesNotExistIsNotOffered() {
        assertTrue(parse("114:99").destinations.none { it is QuranDestination.Ayah })
        assertTrue(parse("200:1").destinations.isEmpty())
    }

    @Test fun juzAndPageAreTheirOwnPlaces() {
        assertEquals(QuranDestination.Juz(30), parse("juz 30").destinations.first())
        assertEquals(QuranDestination.Juz(30), parse("الجزء ٣٠").destinations.first())
        assertEquals(QuranDestination.Page(604), parse("page 604").destinations.first())
        assertEquals(QuranDestination.Page(604), parse("صفحة ٦٠٤").destinations.first())
        assertTrue("there is no juz 31", parse("juz 31").destinations.none { it is QuranDestination.Juz })
    }

    @Test fun aSurahIsFoundByNameAndBySpelling() {
        assertEquals(QuranDestination.SurahStart(36), parse("yaseen").destinations.first())
        assertEquals(QuranDestination.SurahStart(18), parse("surah al kahf").destinations.first())
    }

    @Test fun aBareNumberIsASurahFirstAndAPageSecond() {
        assertEquals(
            listOf(QuranDestination.SurahStart(36), QuranDestination.Page(36)),
            parse("36").destinations,
        )
        assertEquals(listOf(QuranDestination.Page(300)), parse("300").destinations)
    }

    @Test fun aSentenceIsReducedToWhatItIsAsking() {
        assertEquals("patience", parse("show me the verse about patience").text)
        assertEquals("الصبر", parse("ابحث عن آية الصبر").text)
        // Nothing but framing words: the query stands as it was typed rather than becoming nothing.
        assertEquals("the", parse("the").text)
    }

    @Test fun theTextIsStillSearchedWhenTheQueryNamedAPlace() {
        val parsed = parse("ayat al kursi")
        assertTrue("a named passage still leaves something to look for", parsed.text.isNotEmpty())
        assertEquals(QuranDestination.Ayah(2, 255, NamedPassage.AYAT_AL_KURSI), parsed.destinations.first())
    }

    @Test fun everyPassagePointsAtAnAyahThatExists() {
        NamedPassage.entries.forEach { passage ->
            assertTrue("${passage.name} names a surah", passage.surah in 1..114)
            assertTrue("${passage.name} names an ayah", passage.ayah >= 1 && passage.lastAyah >= passage.ayah)
            assertTrue("${passage.name} has spellings to match", passage.aliases.isNotEmpty())
        }
    }
}
