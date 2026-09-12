package com.pilotothegreat.deencompanion.playback

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The recitation cache's budget.
 *
 * A cache that honours a smaller number than the reader asked for wastes their storage, and one
 * that honours a larger number eats it. The floor matters most: an evictor built with a few
 * kilobytes would throw every ayah away the moment after it arrived, so a nonsense setting is
 * clamped rather than obeyed.
 */
class AudioCacheTest {

    @After fun restore() = AudioCache.setBudgetMb(256)

    @Test fun theBudgetIsWhatWasAskedFor() {
        AudioCache.setBudgetMb(64)
        assertEquals(64L * 1024 * 1024, AudioCache.budgetBytes)
    }

    @Test fun anAbsurdlySmallBudgetIsLiftedToSomethingUsable() {
        AudioCache.setBudgetMb(0)
        assertEquals(16L * 1024 * 1024, AudioCache.budgetBytes)
        AudioCache.setBudgetMb(-100)
        assertEquals(16L * 1024 * 1024, AudioCache.budgetBytes)
    }

    @Test fun aLargeBudgetIsTakenAtItsWord() {
        AudioCache.setBudgetMb(4096)
        assertTrue(AudioCache.budgetBytes > Int.MAX_VALUE)
    }
}
