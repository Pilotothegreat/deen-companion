package com.pilotothegreat.deencompanion.core.astro

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * The bundled table and the lunar-position series that decides whether an eclipse can actually be
 * seen from where the reader is. A card announcing an eclipse nobody in the country can see is
 * worse than no card, since the prayer follows witnessing it.
 */
class EclipseTest {

    private val table = Json.parseToJsonElement(File("src/main/assets/eclipses.json").readText()).jsonObject
    private val eclipses = table.getValue("eclipses").jsonArray.map { it.jsonObject }

    private fun at(entry: Map<String, kotlinx.serialization.json.JsonElement>) =
        Instant.parse(entry.getValue("at").jsonPrimitive.content)

    @Test fun theTableCoversTwoDecadesAndIsInOrder() {
        assertEquals(2026, table.getValue("from").jsonPrimitive.content.toInt())
        assertEquals(2045, table.getValue("to").jsonPrimitive.content.toInt())
        assertTrue("enough eclipses to be plausible", eclipses.size > 60)
        val times = eclipses.map { at(it) }
        assertEquals("sorted", times.sortedBy { it }, times)
        times.forEach {
            val year = it.atZone(ZoneOffset.UTC).year
            assertTrue("$year is inside the range", year in 2026..2045)
        }
    }

    @Test fun everyEntryIsUsable() {
        eclipses.forEach { entry ->
            val kind = entry.getValue("kind").jsonPrimitive.content
            assertTrue(kind in setOf("solar", "lunar"))
            assertTrue(
                entry.getValue("type").jsonPrimitive.content in
                    setOf("total", "annular", "partial", "penumbral", "hybrid"),
            )
            assertFalse("NASA's regions are what a solar card can honestly say", entry.getValue("regions").jsonPrimitive.content.isBlank())
        }
    }

    @Test fun bothKindsArePresentInRoughlyTheExpectedNumbers() {
        val solar = eclipses.count { it.getValue("kind").jsonPrimitive.content == "solar" }
        val lunar = eclipses.size - solar
        // Two to five of each a year; anything far outside means the parse drifted.
        assertTrue("$solar solar", solar in 40..100)
        assertTrue("$lunar lunar", lunar in 40..100)
    }

    @Test fun theMoonIsUpWhereTheEclipseIsVisibleAndDownWhereItIsNot() {
        // 3 March 2026, total lunar eclipse at 11:34 UTC. NASA: east Asia, Australia, the Pacific
        // and the Americas — so it is up over Sydney and below the horizon over Cairo and London.
        val greatest = Instant.parse("2026-03-03T11:34:52Z")
        assertTrue("Sydney", MoonPosition.isUp(greatest, -33.87, 151.21))
        assertFalse("Cairo", MoonPosition.isUp(greatest, 30.04, 31.24))
        assertFalse("London", MoonPosition.isUp(greatest, 51.51, -0.13))
    }

    @Test fun theOtherSideOfTheWorldSeesTheOtherEclipse() {
        // 28 August 2026, partial lunar eclipse at 04:14 UTC. NASA: east Pacific, the Americas,
        // Europe and Africa — up over London, below the horizon over Sydney.
        val greatest = Instant.parse("2026-08-28T04:14:04Z")
        assertTrue("London", MoonPosition.isUp(greatest, 51.51, -0.13))
        assertFalse("Sydney", MoonPosition.isUp(greatest, -33.87, 151.21))
    }

    @Test fun theMoonRisesAndSetsRatherThanSittingStill() {
        val start = ZonedDateTime.of(2026, 6, 10, 0, 0, 0, 0, ZoneOffset.UTC)
        val altitudes = (0 until 24).map { MoonPosition.altitudeDegrees(start.plusHours(it.toLong()).toInstant(), 23.59, 58.38) }
        assertTrue("it is up at some point", altitudes.any { it > 10 })
        assertTrue("and down at another", altitudes.any { it < -10 })
        altitudes.forEach { assertTrue("$it is a real altitude", it in -90.0..90.0) }
    }

    @Test fun julianDayMatchesTheEpochEveryEphemerisUses() {
        // 1 January 2000, 12:00 UTC is JD 2451545.0 by definition.
        assertEquals(2451545.0, MoonPosition.julianDay(Instant.parse("2000-01-01T12:00:00Z")), 1e-6)
    }
}
