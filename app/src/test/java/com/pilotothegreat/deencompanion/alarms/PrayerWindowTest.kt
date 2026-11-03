package com.pilotothegreat.deencompanion.alarms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One notification per prayer, from the adhan to the iqama.
 *
 * Before this, the adhan raised one notification from the receiver and a second from the service
 * playing the audio, and the iqama added a third. These pin down which single thing is on screen at
 * any instant, which is the part worth testing — the rest is Android.
 */
class PrayerWindowTest {

    private val adhan = 1_800_000_000_000L
    private val minute = 60_000L
    private val iqama = adhan + 20 * minute

    private fun at(offsetMinutes: Long, playing: Boolean = false) =
        PrayerWindow.stageAt(adhan + offsetMinutes * minute, adhan, iqama, playing)

    @Test fun nothingIsShownBeforeTheAdhan() {
        assertEquals(PrayerStage.Done, PrayerWindow.stageAt(adhan - minute, adhan, iqama, adhanPlaying = false))
    }

    @Test fun theAdhanStageLastsAsLongAsTheAudioDoes() {
        assertEquals(PrayerStage.Adhan, at(0, playing = true))
        assertEquals("still sounding two minutes in", PrayerStage.Adhan, at(2, playing = true))
    }

    @Test fun theCountdownBeginsWhenTheAudioEnds() {
        val stage = at(2)
        assertTrue(stage is PrayerStage.Waiting)
        assertEquals(0.1f, (stage as PrayerStage.Waiting).fraction, 0.001f)
    }

    @Test fun theBarFillsAcrossTheWait() {
        assertEquals(0f, (at(0) as PrayerStage.Waiting).fraction, 0.001f)
        assertEquals(0.5f, (at(10) as PrayerStage.Waiting).fraction, 0.001f)
        assertEquals(0.95f, (at(19) as PrayerStage.Waiting).fraction, 0.001f)
    }

    @Test fun theIqamaTakesOverAtItsTimeAndThenLetsGo() {
        assertEquals(PrayerStage.Iqama, at(20))
        assertEquals(PrayerStage.Iqama, at(25))
        assertEquals("and removes itself once it has been seen", PrayerStage.Done, at(31))
    }

    @Test fun aPrayerWithNoIqamaShowsTheAdhanAndThenGoes() {
        // Travelling suppresses the iqama, and some prayers simply have none configured.
        assertEquals(PrayerStage.Adhan, PrayerWindow.stageAt(adhan + minute, adhan, null, false))
        assertEquals(PrayerStage.Done, PrayerWindow.stageAt(adhan + 25 * minute, adhan, null, false))
    }

    @Test fun anIqamaAtTheAdhanIsNoIqamaAtAll() {
        assertEquals(PrayerStage.Adhan, PrayerWindow.stageAt(adhan + minute, adhan, adhan, false))
        assertTrue(PrayerWindow.checkpoints(adhan, adhan).isEmpty())
    }

    @Test fun theProgressNudgesAreFewAndInsideTheGap() {
        val checkpoints = PrayerWindow.checkpoints(adhan, iqama)
        assertEquals(PrayerWindow.MAX_CHECKPOINTS, checkpoints.size)
        assertEquals(listOf(adhan + 5 * minute, adhan + 10 * minute, adhan + 15 * minute), checkpoints)
        checkpoints.forEach { assertTrue("$it is inside the gap", it in (adhan + 1) until iqama) }
        assertEquals("one alarm kind per checkpoint", checkpoints.size, AlarmKind.progressCheckpoints.size)
    }

    @Test fun thereAreNoNudgesWhenThereIsNothingToCountTowards() {
        assertTrue(PrayerWindow.checkpoints(adhan, null).isEmpty())
    }

    @Test fun theFractionNeverLeavesItsRange() {
        assertEquals(0f, PrayerWindow.fractionElapsed(adhan - minute, adhan, iqama), 0f)
        assertEquals(1f, PrayerWindow.fractionElapsed(iqama + minute, adhan, iqama), 0f)
        assertEquals("a zero-length gap is simply over", 1f, PrayerWindow.fractionElapsed(adhan, adhan, adhan), 0f)
    }
}
