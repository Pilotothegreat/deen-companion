package com.pilotothegreat.deencompanion.core.update

import org.junit.Assert.assertEquals
import org.junit.Test

/** When the app is allowed to raise its voice about an update, and when it has to keep quiet. */
class UpdateUrgencyTest {

    private val now = 1_800_000_000_000L
    private val hour = 60 * 60 * 1000L
    private val day = 24 * hour

    @Test fun nothingWaitingIsQuiet() {
        assertEquals(UpdateUrgency.QUIET, UpdatePlan.urgency(now, availableSince = 0L, lastPromptedAt = 0L))
    }

    @Test fun aFreshReleaseIsMentionedOnceAndThenLeftAlone() {
        val since = now - hour
        assertEquals(UpdateUrgency.NUDGE, UpdatePlan.urgency(now, since, lastPromptedAt = 0L))
        assertEquals(UpdateUrgency.QUIET, UpdatePlan.urgency(now, since, lastPromptedAt = now - hour))
        assertEquals(UpdateUrgency.NUDGE, UpdatePlan.urgency(now, since, lastPromptedAt = now - 25 * hour))
    }

    @Test fun anUpdateIgnoredForAFortnightStopsSlidingAway() {
        val since = now - UpdatePlan.INSIST_AFTER_DAYS * day
        assertEquals(UpdateUrgency.INSIST, UpdatePlan.urgency(now, since, lastPromptedAt = now - 1))
        assertEquals(true, UpdateUrgency.INSIST.promptsOnLaunch)
        assertEquals(false, UpdateUrgency.NUDGE.promptsOnLaunch)
    }

    @Test fun playsOwnPriorityOutranksTheClock() {
        val since = now - hour
        assertEquals(
            UpdateUrgency.INSIST,
            UpdatePlan.urgency(now, since, lastPromptedAt = now - 1, priority = UpdatePlan.HIGH_PRIORITY),
        )
        assertEquals(
            UpdateUrgency.INSIST,
            UpdatePlan.urgency(now, since, lastPromptedAt = now - 1, stalenessDays = 30),
        )
    }

    @Test fun onlyAnImportantUpdateTakesTheScreen() {
        assertEquals(false, UpdatePlan.useImmediateFlow(priority = 1, stalenessDays = 0))
        assertEquals(true, UpdatePlan.useImmediateFlow(priority = 5, stalenessDays = 0))
        assertEquals(true, UpdatePlan.useImmediateFlow(priority = 0, stalenessDays = 20))
    }
}
