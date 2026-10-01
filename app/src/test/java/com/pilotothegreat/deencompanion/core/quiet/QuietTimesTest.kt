package com.pilotothegreat.deencompanion.core.quiet

import com.pilotothegreat.deencompanion.core.prayer.Prayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The two moments the app has to notice without being told: a Friday, and an odd night of the last
 * ten. Both are the kind of rule that is obvious to state and easy to get wrong by one prayer or one
 * day, so each edge is pinned here rather than discovered by someone at the mosque.
 */
class QuietTimesTest {

    private val zone = ZoneId.of("Asia/Riyadh")
    private fun at(day: Int, hour: Int, minute: Int = 0) =
        ZonedDateTime.of(2026, 9, day, hour, minute, 0, 0, zone)

    // 2026-09-11 is a Friday; 2026-09-12 a Saturday.
    private val fridayDhuhr = at(11, 12, 5)
    private val saturdayDhuhr = at(12, 12, 5)

    @Test fun noWindowUntilTheReaderAsksForOne() {
        assertNull(QuietTimes.silenceWindow(Prayer.DHUHR, fridayDhuhr, fridayDhuhr.plusMinutes(20), 0))
    }

    @Test fun anOrdinaryPrayerIsSilencedFromItsIqama() {
        val window = QuietTimes.silenceWindow(Prayer.DHUHR, saturdayDhuhr, saturdayDhuhr.plusMinutes(20), 10)!!
        assertEquals(saturdayDhuhr.plusMinutes(20), window.start)
        assertEquals(saturdayDhuhr.plusMinutes(30), window.endInclusive)
    }

    @Test fun withoutAnIqamaItIsSilencedFromTheAdhan() {
        val window = QuietTimes.silenceWindow(Prayer.ASR, saturdayDhuhr, null, 10)!!
        assertEquals(saturdayDhuhr, window.start)
        assertEquals(saturdayDhuhr.plusMinutes(10), window.endInclusive)
    }

    @Test fun theKhutbahIsCoveredFromTheAdhanAndNotFromTheIqama() {
        val window = QuietTimes.silenceWindow(Prayer.DHUHR, fridayDhuhr, fridayDhuhr.plusMinutes(20), 10)!!
        assertEquals("the khatib stands up at the adhan", fridayDhuhr, window.start)
        assertEquals(fridayDhuhr.plusMinutes(QuietTimes.KHUTBAH_MINUTES.toLong()), window.endInclusive)
    }

    @Test fun aLongerChosenWindowIsRespectedOnFridayToo() {
        val window = QuietTimes.silenceWindow(Prayer.DHUHR, fridayDhuhr, null, 90)!!
        assertEquals(fridayDhuhr.plusMinutes(90), window.endInclusive)
    }

    @Test fun onlyFridayDhuhrIsJumuah() {
        assertTrue(QuietTimes.isJumuah(Prayer.DHUHR, fridayDhuhr))
        assertFalse("Friday Asr is an ordinary prayer", QuietTimes.isJumuah(Prayer.ASR, fridayDhuhr))
        assertFalse("Saturday Dhuhr is an ordinary prayer", QuietTimes.isJumuah(Prayer.DHUHR, saturdayDhuhr))
    }

    @Test fun theGentleNoticesAreHeldOnlyOnTheOddNightsOfTheLastTen() {
        listOf(21, 23, 25, 27, 29).forEach {
            assertTrue("night $it of Ramadan", QuietTimes.holdsGentleNotices(9, it))
        }
        listOf(22, 24, 26, 28, 30).forEach {
            assertFalse("the even $it is not one of them", QuietTimes.holdsGentleNotices(9, it))
        }
        assertFalse("the 19th is not in the last ten", QuietTimes.holdsGentleNotices(9, 19))
        assertFalse("Shaban has no last ten of Ramadan", QuietTimes.holdsGentleNotices(8, 27))
        assertFalse("nor does Shawwal", QuietTimes.holdsGentleNotices(10, 27))
    }
}
