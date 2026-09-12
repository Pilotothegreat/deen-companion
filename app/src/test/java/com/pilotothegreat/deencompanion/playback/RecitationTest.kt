package com.pilotothegreat.deencompanion.playback

import com.pilotothegreat.deencompanion.core.quran.Khatma
import com.pilotothegreat.deencompanion.core.quran.KhatmaPlan
import com.pilotothegreat.deencompanion.core.quran.PlaybackStep
import com.pilotothegreat.deencompanion.core.quran.RepeatMode
import com.pilotothegreat.deencompanion.core.quran.RepeatPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The first tests this package has ever had. */
class RepeatPlanTest {

    private fun step(
        finished: Int,
        mode: RepeatMode,
        repeatsDone: Int = 0,
        count: Int = 3,
        lastIndex: Int = 6,
        range: IntRange = 0..6,
        continueToNextSurah: Boolean = false,
    ) = RepeatPlan.onAyahFinished(finished, lastIndex, mode, count, repeatsDone, range, continueToNextSurah)

    @Test fun offPlaysStraightThroughAndStops() {
        assertEquals(PlaybackStep.Advance, step(finished = 0, mode = RepeatMode.OFF))
        assertEquals(PlaybackStep.Advance, step(finished = 5, mode = RepeatMode.OFF))
        assertEquals(PlaybackStep.Stop, step(finished = 6, mode = RepeatMode.OFF))
    }

    @Test fun offCarriesOnWhenContinuousPlaybackIsOn() {
        assertEquals(PlaybackStep.NextSurah, step(finished = 6, mode = RepeatMode.OFF, continueToNextSurah = true))
    }

    @Test fun anAyahRepeatsExactlyTheRequestedNumberOfTimes() {
        // Three plays means the ayah is replayed twice, then the surah moves on.
        assertEquals(PlaybackStep.SeekTo(2), step(finished = 2, mode = RepeatMode.AYAH, repeatsDone = 0))
        assertEquals(PlaybackStep.SeekTo(2), step(finished = 2, mode = RepeatMode.AYAH, repeatsDone = 1))
        assertEquals(PlaybackStep.Advance, step(finished = 2, mode = RepeatMode.AYAH, repeatsDone = 2))
    }

    @Test fun aCountOfZeroRepeatsWithoutLimit() {
        assertEquals(PlaybackStep.SeekTo(2), step(finished = 2, mode = RepeatMode.AYAH, repeatsDone = 99, count = 0))
    }

    @Test fun aRangeLoopsAtItsEndAndIsTransparentEverywhereElse() {
        val range = 2..4
        assertEquals(PlaybackStep.Advance, step(finished = 0, mode = RepeatMode.RANGE, range = range))
        assertEquals(PlaybackStep.Advance, step(finished = 3, mode = RepeatMode.RANGE, range = range))
        assertEquals(PlaybackStep.SeekTo(2), step(finished = 4, mode = RepeatMode.RANGE, range = range))
        assertEquals(PlaybackStep.Advance, step(finished = 4, mode = RepeatMode.RANGE, range = range, repeatsDone = 2))
        // Past the range, the surah plays out normally rather than being dragged back.
        assertEquals(PlaybackStep.Advance, step(finished = 5, mode = RepeatMode.RANGE, range = range))
        assertEquals(PlaybackStep.Stop, step(finished = 6, mode = RepeatMode.RANGE, range = range))
    }

    @Test fun aSurahRepeatRestartsFromItsFirstAyah() {
        assertEquals(PlaybackStep.Advance, step(finished = 3, mode = RepeatMode.SURAH))
        assertEquals(PlaybackStep.SeekTo(0), step(finished = 6, mode = RepeatMode.SURAH))
        assertEquals(PlaybackStep.Stop, step(finished = 6, mode = RepeatMode.SURAH, repeatsDone = 2))
    }

    @Test fun onlyASeekCountsAsARepeat() {
        assertTrue(RepeatPlan.unitRestarted(PlaybackStep.SeekTo(0)))
        assertFalse(RepeatPlan.unitRestarted(PlaybackStep.Advance))
        assertFalse(RepeatPlan.unitRestarted(PlaybackStep.Stop))
        assertFalse(RepeatPlan.unitRestarted(PlaybackStep.NextSurah))
    }
}

class KhatmaPlanTest {

    private val start = LocalDate.of(2026, 3, 1)

    private fun plan(days: Int = 30, startPage: Int = 1, lastPage: Int = 0) =
        Khatma(startedOn = start, targetDays = days, startPage = startPage, lastPage = lastPage)

    @Test fun aThirtyDayPlanIsTwentyOnePagesADay() {
        val progress = KhatmaPlan.progress(plan(), start)
        assertEquals(604, progress.pagesTotal)
        assertEquals(21, progress.pagesPerDay)
        assertEquals("day one already owes its share", 21, progress.targetPage)
        assertEquals(29, progress.daysLeft)
        assertEquals(0, progress.pagesRead)
    }

    @Test fun theTargetMovesWithTheDays() {
        assertEquals(21, KhatmaPlan.progress(plan(), start).targetPage)
        assertEquals(42, KhatmaPlan.progress(plan(), start.plusDays(1)).targetPage)
        assertEquals(604, KhatmaPlan.progress(plan(), start.plusDays(60)).targetPage)
    }

    @Test fun aPlanStartedPartWayThroughCountsOnlyWhatIsLeft() {
        val progress = KhatmaPlan.progress(plan(days = 10, startPage = 405, lastPage = 410), start)
        assertEquals(200, progress.pagesTotal)
        assertEquals(20, progress.pagesPerDay)
        assertEquals(6, progress.pagesRead)
    }

    @Test fun beingAheadIsOnTrackAndOwesNothingMoreToday() {
        val ahead = KhatmaPlan.progress(plan(lastPage = 100), start)
        assertTrue(ahead.isOnTrack)
        assertEquals(0, ahead.pagesBehind)
        assertFalse(KhatmaPlan.needsReminder(plan(lastPage = 100), start))
    }

    @Test fun fallingBehindAsksForTodaysShareAndTheShortfall() {
        // Day three, nothing read since day one's 21 pages: 21 owed for today plus 21 missed.
        val behind = plan(lastPage = 21)
        val progress = KhatmaPlan.progress(behind, start.plusDays(2))
        assertEquals(63, progress.targetPage)
        assertEquals(42, progress.pagesBehind)
        assertEquals(63, progress.pagesDueToday)
        assertTrue(KhatmaPlan.needsReminder(behind, start.plusDays(2)))
    }

    @Test fun aFinishedKhatmaIsDoneAndSaysNothingMore() {
        val done = plan(lastPage = 604)
        val progress = KhatmaPlan.progress(done, start.plusDays(5))
        assertTrue(progress.isComplete)
        assertEquals(1f, progress.fraction, 0.0001f)
        assertEquals(0, progress.pagesDueToday)
        assertFalse(KhatmaPlan.needsReminder(done, start.plusDays(5)))
    }

    @Test fun aPlanThatHasNotStartedYetIsSilent() {
        assertFalse(KhatmaPlan.needsReminder(plan(), start.minusDays(1)))
    }
}

/**
 * The range repeat was fully implemented in QuranPlayer and reachable from nothing: RepeatMode.RANGE
 * silently repeated "from where you pressed play to the end of the surah". These pin down the plan
 * the range drives, so the UI that now sets it has something to be right against.
 */
class RepeatRangeTest {

    private val lastIndex = 19

    private fun step(finished: Int, range: IntRange, done: Int = 0) = RepeatPlan.onAyahFinished(
        finished = finished,
        lastIndex = lastIndex,
        mode = RepeatMode.RANGE,
        repeatCount = 3,
        repeatsDone = done,
        range = range,
        continueToNextSurah = true,
    )

    @Test fun theRangeRestartsAtItsOwnBeginningAndNotTheSurahs() {
        assertEquals(PlaybackStep.SeekTo(4), step(finished = 8, range = 4..8))
    }

    @Test fun insideTheRangePlaybackSimplyCarriesOn() {
        assertEquals(PlaybackStep.Advance, step(finished = 5, range = 4..8))
    }

    @Test fun theRangeStopsRepeatingOnceItHasBeenHeardEnoughTimes() {
        assertEquals(PlaybackStep.Advance, step(finished = 8, range = 4..8, done = 3))
    }

    @Test fun aSingleAyahIsAValidRange() {
        assertEquals(PlaybackStep.SeekTo(7), step(finished = 7, range = 7..7))
    }
}
