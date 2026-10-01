package com.pilotothegreat.deencompanion.core.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.chrono.HijrahDate

class DayScheduleTest {
    private val date = LocalDate.of(2026, 9, 11)
    private val config = PrayerConfig(
        latitude = 23.5880,
        longitude = 58.3829,
        zone = ZoneId.of("Asia/Muscat"),
        method = CalculationMethod.MWL,
        asrSchool = AsrSchool.STANDARD,
        iqama = mapOf(
            Prayer.FAJR to IqamaRule.Offset(25),
            Prayer.DHUHR to IqamaRule.Fixed(LocalTime.of(12, 50)),
            Prayer.ASR to IqamaRule.Offset(20),
            Prayer.MAGHRIB to IqamaRule.Offset(10),
            Prayer.ISHA to IqamaRule.Fixed(LocalTime.of(18, 0)),
        ),
    )
    private val schedule = DaySchedule.forDate(date, config)

    @Test fun timesAreInOrder() {
        val times = Prayer.entries.map { schedule.adhan.getValue(it) }
        assertTrue(times.zipWithNext().all { (a, b) -> a.isBefore(b) })
    }

    @Test fun offsetIqama() =
        assertEquals(schedule.adhan.getValue(Prayer.FAJR).plusMinutes(25), schedule.iqama[Prayer.FAJR])

    @Test fun fixedIqama() = assertEquals(LocalTime.of(12, 50), schedule.iqama.getValue(Prayer.DHUHR).toLocalTime())

    @Test fun fixedIqamaNeverPrecedesAdhan() =
        assertEquals(schedule.adhan[Prayer.ISHA], schedule.iqama[Prayer.ISHA])

    @Test fun sunriseHasNoIqama() = assertTrue(Prayer.SUNRISE !in schedule.iqama)

    @Test fun nextSkipsSunrise() {
        val next = DaySchedule.next(schedule.adhan.getValue(Prayer.FAJR).plusMinutes(1), config)
        assertEquals(Prayer.DHUHR, next.prayer)
    }

    @Test fun nextAfterIshaIsTomorrowsFajr() {
        val next = DaySchedule.next(schedule.adhan.getValue(Prayer.ISHA).plusMinutes(1), config)
        assertEquals(Prayer.FAJR, next.prayer)
        assertEquals(date.plusDays(1), next.adhan.toLocalDate())
        assertEquals(next.adhan.plusMinutes(25), next.iqama)
    }

    @Test fun keysAreBackwardCompatible() {
        assertEquals("Fajr", Prayer.FAJR.key)
        assertEquals(Prayer.MAGHRIB, Prayer.fromKey("Maghrib"))
    }

    /**
     * The Makkah method's longer Ramadan Isha follows the Hijri date the reader sees. With the date
     * moved a day earlier, the last day of Sha'ban already reads as 1 Ramadan, so its Isha is later.
     */
    @Test fun ramadanIshaFollowsTheHijriAdjustment() {
        val firstOfRamadan = LocalDate.from(HijrahDate.of(1448, 9, 1))
        val eve = firstOfRamadan.minusDays(1)
        val makkah = config.copy(method = CalculationMethod.MAKKAH, iqama = emptyMap())
        val plain = DaySchedule.forDate(eve, makkah).adhan.getValue(Prayer.ISHA)
        val adjusted = DaySchedule.forDate(eve, makkah.copy(hijriAdjustment = 1)).adhan.getValue(Prayer.ISHA)
        assertEquals(plain.plusMinutes(30), adjusted)
    }
}
