package com.pilotothegreat.deencompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The two string files have to stay in step.
 *
 * A name added to one and forgotten in the other shows up as an English sentence in an Arabic app,
 * and a name left behind after its feature was removed is a line nobody will ever delete on
 * purpose. Neither is visible in review, so both are checked.
 */
class StringsTest {

    private val english = File("src/main/res/values/strings.xml").readText()
    private val arabic = File("src/main/res/values-ar/strings.xml").readText()
    // The manifest counts too: a service's label is a string reference like any other.
    private val sources: String = (
        listOf(File("src/main/java"), File("src/main/res"))
            .flatMap { it.walkTopDown().filter { file -> file.isFile && file.extension in setOf("kt", "xml") } }
            .filterNot { it.path.contains("res/values") } + File("src/main/AndroidManifest.xml")
        ).joinToString("\n") { it.readText() }

    private fun names(xml: String, tag: String, translatableOnly: Boolean = false): Set<String> =
        Regex("""<$tag name="([a-z0-9_]+)"([^>]*)>""").findAll(xml)
            .filterNot { translatableOnly && it.groupValues[2].contains("translatable=\"false\"") }
            .map { it.groupValues[1] }
            .toSet()

    @Test fun everyTranslatableStringIsTranslated() {
        val missing = names(english, "string", translatableOnly = true) - names(arabic, "string")
        assertEquals("strings with no Arabic", emptySet<String>(), missing)
    }

    @Test fun everyPluralIsTranslated() {
        val missing = names(english, "plurals", translatableOnly = true) - names(arabic, "plurals")
        assertEquals("plurals with no Arabic", emptySet<String>(), missing)
    }

    @Test fun arabicCarriesAllSixPluralForms() {
        // Arabic distinguishes zero, one, two, few, many and other; shipping only one/other would
        // read as a translation nobody finished.
        Regex("""<plurals name="([a-z0-9_]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(arabic)
            .forEach { match ->
                val quantities = Regex("""quantity="([a-z]+)"""").findAll(match.groupValues[2])
                    .map { it.groupValues[1] }
                    .toSet()
                assertEquals(
                    "plural ${match.groupValues[1]}",
                    setOf("zero", "one", "two", "few", "many", "other"),
                    quantities,
                )
            }
    }

    @Test fun noArabicStringIsOrphaned() {
        val orphans = names(arabic, "string") - names(english, "string")
        assertEquals("Arabic strings with no English", emptySet<String>(), orphans)
    }

    @Test fun everyStringIsUsedSomewhere() {
        val unused = names(english, "string").filterNot { name ->
            sources.contains("R.string.$name") || sources.contains("@string/$name")
        }
        assertEquals("strings nothing reads", emptyList<String>(), unused)
    }

    @Test fun thereAreStringsToCheck() {
        assertTrue(names(english, "string").size > 400)
    }
}
