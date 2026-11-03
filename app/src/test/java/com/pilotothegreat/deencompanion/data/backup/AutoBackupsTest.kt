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

/**
 * The same rules against a real files directory: what is written can be found again and read back,
 * and the directory cannot grow past three copies however many weeks pass.
 */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
@org.robolectric.annotation.Config(sdk = [34], application = android.app.Application::class)
class AutoBackupsOnDiskTest {

    private val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()

    @org.junit.After fun clean() {
        AutoBackups.directory(context).deleteRecursively()
    }

    @Test fun whatIsWrittenIsWhatComesBack() {
        val json = """{"format":"bilal-backup","version":1}"""
        val written = AutoBackups.write(context, json, atMillis = 1_757_000_000_000)
        assertEquals(json, written.readText())
        assertEquals(listOf(written.name), AutoBackups.list(context).map { it.name })
    }

    @Test fun aYearOfWeeklyBackupsStillLeavesThree() {
        val week = 7L * 24 * 60 * 60 * 1000
        var at = 1_700_000_000_000
        repeat(52) {
            AutoBackups.write(context, """{"n":$it}""", atMillis = at)
            at += week
        }
        val kept = AutoBackups.list(context)
        assertEquals(AutoBackups.KEEP, kept.size)
        assertTrue("the newest must survive", kept.first().readText().contains("51"))
    }

    @Test fun anEmptyDirectoryIsNotAnError() {
        assertEquals(emptyList<File>(), AutoBackups.list(context))
    }
}
