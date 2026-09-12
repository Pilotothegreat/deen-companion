package com.pilotothegreat.deencompanion.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.ZoneId

/**
 * The automatic backup is only worth having if it cannot grow without bound and cannot lose the
 * newest copy. Both are one-line rules and both are the sort of one-line rule that is written
 * backwards, so they are checked.
 */
class AutoBackupsTest {

    private fun files(vararg names: String) = names.map { File(it) }

    @Test fun nothingIsDeletedWhileThereIsRoom() {
        assertEquals(emptyList<File>(), AutoBackups.prune(files("c", "b", "a"), keep = 3))
        assertEquals(emptyList<File>(), AutoBackups.prune(emptyList(), keep = 3))
    }

    @Test fun theOldestGoFirstAndTheNewestAlwaysStay() {
        val newestFirst = files("bilal-auto-2026-09-12-0300.json", "bilal-auto-2026-09-05-0300.json", "bilal-auto-2026-08-29-0300.json", "bilal-auto-2026-08-22-0300.json")
        val doomed = AutoBackups.prune(newestFirst, keep = 3)
        assertEquals(1, doomed.size)
        assertEquals("bilal-auto-2026-08-22-0300.json", doomed.single().name)
    }

    @Test fun theNameSortsIntoTheOrderItWasWrittenIn() {
        val zone = ZoneId.of("UTC")
        val earlier = AutoBackups.nameFor(1_757_000_000_000, zone)
        val later = AutoBackups.nameFor(1_758_000_000_000, zone)
        assertTrue("$earlier should sort before $later", earlier < later)
        assertTrue(earlier.endsWith(".json"))
    }

    @Test fun threeWeeksIsWhatIsKept() {
        assertEquals(3, AutoBackups.KEEP)
    }
}
