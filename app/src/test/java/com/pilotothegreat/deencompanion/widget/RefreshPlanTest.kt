package com.pilotothegreat.deencompanion.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The next-prayer bar used to be drawn at Dhuhr and not again until Asr. These pin down that it now
 * moves through the gap, and that a prayer or midnight still redraws everything.
 */
class RefreshPlanTest {

    private val zone = ZoneId.of("Asia/Muscat")
    private fun at(hour: Int, minute: Int, day: Int = 14) = ZonedDateTime.of(2026, 9, day, hour, minute, 0, 0, zone)
    private val midnight = at(0, 0, day = 15)

    @Test fun betweenDhuhrAndAsrTheBarMovesEveryTwentyFourthOfTheGap() {
        val refresh = RefreshPlan.next(now = at(13, 0), previousPrayer = at(12, 0), nextPrayer = at(15, 30), midnight = midnight)
        // Three and a half hours in twenty-four steps: eight minutes and three quarters.
        assertEquals(at(13, 0).plus(Duration.ofSeconds(525)), refresh.at)
        assertFalse("between prayers only the bar is redrawn", refresh.everything)
    }

    @Test fun aLongNightStillMovesAtLeastEveryTwentyMinutes() {
        val refresh = RefreshPlan.next(now = at(21, 0), previousPrayer = at(19, 30), nextPrayer = at(4, 30, day = 15), midnight = midnight)
        assertEquals(at(21, 20), refresh.at)
    }

    @Test fun aShortGapIsNotRedrawnMoreThanEveryFiveMinutes() {
        val refresh = RefreshPlan.next(now = at(18, 10), previousPrayer = at(18, 0), nextPrayer = at(19, 0), midnight = midnight)
        assertEquals(at(18, 15), refresh.at)
    }

    @Test fun thePrayerItselfRedrawsEverything() {
        val refresh = RefreshPlan.next(now = at(15, 28), previousPrayer = at(12, 0), nextPrayer = at(15, 30), midnight = midnight)
        assertEquals(at(15, 30).plusSeconds(1), refresh.at)
        assertTrue(refresh.everything)
    }

    @Test fun midnightBeforeFajrRedrawsEverythingForTheNewDay() {
        val refresh = RefreshPlan.next(now = at(23, 55), previousPrayer = at(19, 30), nextPrayer = at(4, 30, day = 15), midnight = midnight)
        assertEquals(midnight.plusSeconds(1), refresh.at)
        assertTrue(refresh.everything)
    }

    @Test fun withNoEarlierPrayerTheGapIsMeasuredFromNow() {
        val refresh = RefreshPlan.next(now = at(3, 0, day = 15), previousPrayer = null, nextPrayer = at(4, 30, day = 15), midnight = at(0, 0, day = 16))
        assertEquals(at(3, 5, day = 15), refresh.at)
    }
}
