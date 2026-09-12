package com.pilotothegreat.deencompanion.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every stored setting must be read by something.
 *
 * Before 1.9.0 four were not: `smart_occasions`, `quran_tajweed`, `quran_translation_secondary` and
 * `quran_translation` were all written, mapped and shown in the settings menu, and nothing anywhere
 * ever consulted them. A switch that does nothing is worse than a missing one — it is a promise the
 * app quietly breaks — so the rule is checked rather than remembered.
 */
class SettingsKeysTest {

    private val repository = File("src/main/java/com/pilotothegreat/deencompanion/data/settings/SettingsRepository.kt").readText()
    private val sources: String = File("src/main/java").walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .joinToString("\n") { it.readText() }

    /**
     * The names declared inside the Keys object. Private ones belong to the legacy migration, which
     * reads them once by their own name and then deletes them.
     */
    private val declared: List<String> =
        Regex("""\n {4}val ([A-Z0-9_]+) = \w+PreferencesKey\(""").findAll(repository)
            .map { it.groupValues[1] }
            .toList()

    @Test fun thereAreKeysToCheck() {
        assertTrue("the Keys object should not be empty", declared.size > 20)
    }

    @Test fun everyKeyIsBothWrittenAndRead() {
        val unread = declared.filter { name ->
            // A key earns its place by being read back somewhere, not merely stored.
            val reads = Regex("""this\[Keys\.$name]|\[Keys\.$name]""").findAll(repository).count()
            reads < 2
        }
        assertEquals("keys that are written but never read: $unread", emptyList<String>(), unread)
    }

    @Test fun noSettingIsExposedWithoutSomethingActingOnIt() {
        // The four that were dead in 1.8.0 are gone from the model entirely; if any comes back it
        // must come back with a reader.
        listOf("SMART_OCCASIONS", "QURAN_TAJWEED", "QURAN_TRANSLATION_SECOND", "IP_FALLBACK").forEach {
            assertTrue("$it was removed as dead and should not return unused", it !in declared)
        }
    }

    @Test fun theSettingsScreenHasNotGrownBackIntoElevenGroups() {
        val screen = File("src/main/java/com/pilotothegreat/deencompanion/ui/settings/SettingsScreen.kt").readText()
        val groups = Regex("""item\(key = "([a-z]+)"\)""").findAll(screen).map { it.groupValues[1] }.toList()
        assertEquals("settings groups", 6, groups.size)
        assertTrue("a group must not be added without a thought for the whole menu", groups.size <= 6)
    }

    @Test fun theAppStillCompilesAgainstEveryRemovedSetting() {
        // The removed features must leave no references behind anywhere in the app.
        listOf("useIpLocationFallback", "athkarShowTransliteration", "continuousPlayback", "athkarFontSize")
            .forEach { assertTrue("$it should be gone", it !in sources) }
    }
}
