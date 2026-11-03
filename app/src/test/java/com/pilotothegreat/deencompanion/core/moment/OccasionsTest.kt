package com.pilotothegreat.deencompanion.core.moment

import com.pilotothegreat.deencompanion.core.calendar.HijriCalendar
import com.pilotothegreat.deencompanion.core.calendar.HijriClock
import com.pilotothegreat.deencompanion.core.calendar.Occasions
import com.pilotothegreat.deencompanion.core.prayer.AsrSchool
import com.pilotothegreat.deencompanion.core.prayer.CalculationMethod
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.prayer.PrayerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField

/**
 * The calendar used to know one thing: how many days until Ramadan, and only during Sha'ban. These
 * check what replaced it, including the Maghrib flip that every night-anchored occasion depends on.
 */
class OccasionsTest {

    private val zone: ZoneId = ZoneId.of("Asia/Muscat")
    private val config = PrayerConfig(23.5880, 58.3829, zone, CalculationMethod.OMAN, AsrSchool.STANDARD)

    private fun ids(at: ZonedDateTime, flipAtMaghrib: Boolean = true): List<String> {
        val today = DaySchedule.forDate(at.toLocalDate(), config)
        val tomorrow = DaySchedule.forDate(at.toLocalDate().plusDays(1), config)
        val hijri = HijriClock.dateAt(at, today.adhan[Prayer.MAGHRIB], 0, flipAtMaghrib) ?: return emptyList()
        return Occasions.at(at, hijri, today, tomorrow).map { it.id }
    }

    /** Finds the civil date whose Maghrib-anchored Hijri date is the one asked for. */
    private fun civilDateOf(hijriMonth: Int, hijriDay: Int, from: LocalDate = LocalDate.of(2026, 1, 1)): LocalDate {
        var date = from
        repeat(1200) {
            val hijri = HijriCalendar.date(date)
            if (hijri != null &&
                hijri.get(ChronoField.MONTH_OF_YEAR) == hijriMonth &&
                hijri.get(ChronoField.DAY_OF_MONTH) == hijriDay
            ) {
                return date
            }
            date = date.plusDays(1)
        }
        throw AssertionError("no civil date for $hijriMonth/$hijriDay")
    }

    private fun at(date: LocalDate, time: String) = ZonedDateTime.of(date, LocalTime.parse(time), zone)

    @Test fun theIslamicDayTurnsOverAtMaghrib() {
        val date = LocalDate.of(2026, 6, 10)
        val maghrib = DaySchedule.forDate(date, config).adhan.getValue(Prayer.MAGHRIB)
        val before = HijriClock.dateAt(maghrib.minusMinutes(1), maghrib, 0, flipAtMaghrib = true)!!
        val after = HijriClock.dateAt(maghrib.plusMinutes(1), maghrib, 0, flipAtMaghrib = true)!!
        assertEquals("one day apart", 1, after.toEpochDay() - before.toEpochDay())

        val unflipped = HijriClock.dateAt(maghrib.plusMinutes(1), maghrib, 0, flipAtMaghrib = false)!!
        assertEquals("the setting is honoured", before, unflipped)
    }

    @Test fun alKahfRunsFromThursdaySunsetToFridaySunset() {
        // 2026-06-11 is a Thursday.
        val thursday = LocalDate.of(2026, 6, 11)
        val maghrib = DaySchedule.forDate(thursday, config).adhan.getValue(Prayer.MAGHRIB)
        assertFalse("not in the afternoon", "kahf" in ids(maghrib.minusHours(1)))
        assertTrue("from sunset", "kahf" in ids(maghrib.plusMinutes(5)))
        assertTrue("and through Friday", "kahf" in ids(at(thursday.plusDays(1), "10:00")))
        assertFalse("but not on Saturday", "kahf" in ids(at(thursday.plusDays(2), "10:00")))
    }

    @Test fun theHourOfResponseIsTheLastHourOfFriday() {
        val friday = LocalDate.of(2026, 6, 12)
        val maghrib = DaySchedule.forDate(friday, config).adhan.getValue(Prayer.MAGHRIB)
        assertTrue("hour-of-response" in ids(maghrib.minusMinutes(20)))
        assertFalse("hour-of-response" in ids(maghrib.minusHours(3)))
        assertFalse("hour-of-response" in ids(maghrib.plusMinutes(5)))
    }

    @Test fun ramadanCountsDownThenGivesWayToIftarAndTheLastTen() {
        val shaban20 = civilDateOf(Occasions.SHABAN, 20)
        assertTrue("ramadan-countdown" in ids(at(shaban20, "10:00")))

        val ramadan3 = civilDateOf(Occasions.RAMADAN, 3)
        val ids = ids(at(ramadan3, "10:00"))
        assertFalse("the countdown stops once it starts", "ramadan-countdown" in ids)
        assertFalse("and the last ten are not yet", "last-ten" in ids)

        val maghrib = DaySchedule.forDate(ramadan3, config).adhan.getValue(Prayer.MAGHRIB)
        assertTrue("iftar" in ids(maghrib.minusMinutes(10)))
    }

    @Test fun theOddNightsAreOddNightsAndNotOddDays() {
        val ramadan26 = civilDateOf(Occasions.RAMADAN, 26)
        val maghrib = DaySchedule.forDate(ramadan26, config).adhan.getValue(Prayer.MAGHRIB)
        // The night of the 27th begins at the sunset that ends the 26th.
        val night = ids(maghrib.plusMinutes(30))
        assertTrue("last-ten" in night)
        assertTrue("odd-night" in night)
        assertFalse("an even night says nothing", "odd-night" in ids(at(ramadan26, "12:00")))
    }

    @Test fun arafahAndTheEidsAreWhereTheyBelong() {
        assertTrue("arafah" in ids(at(civilDateOf(Occasions.DHUL_HIJJAH, 9), "10:00")))
        assertTrue("takbir" in ids(at(civilDateOf(Occasions.DHUL_HIJJAH, 11), "10:00")))
        assertTrue("eid-al-adha" in ids(at(civilDateOf(Occasions.DHUL_HIJJAH, 10), "08:00")))
        assertTrue("eid-al-fitr" in ids(at(civilDateOf(Occasions.SHAWWAL, 1), "08:00")))
        assertTrue("ashura" in ids(at(civilDateOf(Occasions.MUHARRAM, 10), "10:00")))
    }

    @Test fun aFastIsNotSuggestedOnADayFastingIsForbidden() {
        val eid = civilDateOf(Occasions.SHAWWAL, 1)
        assertFalse("sunnah-fast" in ids(at(eid, "10:00")))
    }

    @Test fun theWhiteDaysAndTheNewMoonComeRoundEveryMonth() {
        assertTrue("white-days" in ids(at(civilDateOf(Occasions.RAJAB, 14), "10:00")))
        assertTrue("new-moon" in ids(at(civilDateOf(Occasions.RAJAB, 1), "10:00")))
    }

    @Test fun theNewMoonSurfacesTheDhikrThatWasUnreachable() {
        val date = civilDateOf(Occasions.RAJAB, 1)
        val today = DaySchedule.forDate(date, config)
        val tomorrow = DaySchedule.forDate(date.plusDays(1), config)
        val now = at(date, "10:00")
        val hijri = HijriClock.dateAt(now, today.adhan[Prayer.MAGHRIB], 0, true)!!
        val occasions = Occasions.at(now, hijri, today, tomorrow)
        assertEquals("new-moon", Occasions.athkarFor(occasions))
    }

    @Test fun anOrdinaryTuesdayInAnOrdinaryMonthSaysNothing() {
        // 8 Rajab: no white days, no new moon, and a Tuesday.
        var date = civilDateOf(Occasions.RAJAB, 8)
        while (date.dayOfWeek.value in setOf(1, 4)) date = civilDateOf(Occasions.RAJAB, 8, date.plusDays(1))
        assertTrue(ids(at(date, "10:00")).isEmpty())
    }

    @Test fun occasionsComeBackHighestPriorityFirst() {
        val ramadan26 = civilDateOf(Occasions.RAMADAN, 26)
        val maghrib = DaySchedule.forDate(ramadan26, config).adhan.getValue(Prayer.MAGHRIB)
        val list = ids(maghrib.plusHours(2))
        assertNotNull(list.firstOrNull())
        assertEquals("the night of the 27th outranks the rest", "odd-night", list.first())
    }

    @Test fun zakatAlFitrArrivesInTheLastNights() {
        val ramadan29 = civilDateOf(Occasions.RAMADAN, 29)
        val maghrib = DaySchedule.forDate(ramadan29, config).adhan.getValue(Prayer.MAGHRIB)
        // After sunset the 30th has begun: an even night, so no odd-night card, but zakat is due.
        val list = ids(maghrib.plusHours(2))
        assertTrue("zakat-al-fitr" in list)
        assertFalse("odd-night" in list)
    }

    @Test fun anImpossibleHijriDateYieldsNothingRatherThanCrashing() {
        assertNull(HijriClock.dateAt(at(LocalDate.of(1200, 1, 1), "10:00"), null, 0, true))
    }
}
