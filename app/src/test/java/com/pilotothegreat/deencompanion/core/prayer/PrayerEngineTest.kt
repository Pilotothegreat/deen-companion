package com.pilotothegreat.deencompanion.core.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.min

/** Reference times from mara.gov.om and api.aladhan.com. */
class PrayerEngineTest {

    private fun calculate(
        date: String,
        lat: Double,
        lon: Double,
        zone: String,
        method: CalculationMethod,
        school: AsrSchool = AsrSchool.STANDARD,
        highLatitude: HighLatitudeMode = HighLatitudeMode.AUTO,
        adjustments: Map<Prayer, Int> = emptyMap(),
    ) = PrayerEngine.calculate(LocalDate.parse(date), lat, lon, ZoneId.of(zone), method, school, highLatitude, adjustments)

    private fun muscat(date: String, method: CalculationMethod, adjustments: Map<Prayer, Int> = emptyMap()) =
        calculate(date, 23.5880, 58.3829, "Asia/Muscat", method, adjustments = adjustments)

    /** [expected] is Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha. */
    private fun assertTimes(expected: List<String>, actual: DayTimes, toleranceMinutes: Int = 2) {
        Prayer.entries.forEachIndexed { i, prayer ->
            val want = LocalTime.parse(expected[i])
            val got = actual.adhan.getValue(prayer).toLocalTime()
            val diff = abs(want.toSecondOfDay() - got.toSecondOfDay()) / 60
            assertTrue("$prayer: expected $want, got $got", min(diff, 24 * 60 - diff) <= toleranceMinutes)
        }
    }

    /** Official Muscat timetable from mara.gov.om. */
    @Test fun omanMatchesMinistryTimetable() {
        assertTimes(listOf("03:54", "05:20", "12:10", "15:29", "18:54", "20:15"), muscat("2026-06-01", CalculationMethod.OMAN), 1)
        assertTimes(listOf("04:36", "05:52", "12:09", "15:36", "18:20", "19:31"), muscat("2026-09-11", CalculationMethod.OMAN), 1)
        assertTimes(listOf("05:12", "06:32", "12:01", "15:04", "17:25", "18:39"), muscat("2026-12-01", CalculationMethod.OMAN), 1)
    }

    @Test fun muscatMwl() = assertTimes(
        listOf("04:36", "05:52", "12:03", "15:31", "18:14", "19:26"),
        muscat("2026-09-11", CalculationMethod.MWL),
    )

    @Test fun muscatUmmAlQura() = assertTimes(
        listOf("04:33", "05:52", "12:03", "15:31", "18:14", "19:44"),
        muscat("2026-09-11", CalculationMethod.MAKKAH),
    )

    @Test fun muscatKarachiHanafi() = assertTimes(
        listOf("04:36", "05:52", "12:03", "16:30", "18:14", "19:30"),
        calculate("2026-09-11", 23.5880, 58.3829, "Asia/Muscat", CalculationMethod.KARACHI, AsrSchool.HANAFI),
    )

    @Test fun makkahRamadanIshaIsTwoHoursAfterMaghrib() = assertTimes(
        listOf("05:25", "06:41", "12:33", "15:54", "18:25", "20:25"),
        calculate("2026-03-01", 21.4225, 39.8262, "Asia/Riyadh", CalculationMethod.MAKKAH),
    )

    @Test fun newYorkOnDaylightSavingDay() = assertTimes(
        listOf("06:04", "07:19", "13:07", "16:21", "18:55", "20:10"),
        calculate("2026-03-08", 40.7128, -74.0060, "America/New_York", CalculationMethod.ISNA),
    )

    @Test fun londonWinter() = assertTimes(
        listOf("05:59", "08:04", "11:59", "13:38", "15:53", "17:51"),
        calculate("2026-12-21", 51.5074, -0.1278, "Europe/London", CalculationMethod.MWL),
    )

    @Test fun osloMidsummerWithTwilightAngleRule() = assertTimes(
        listOf("02:21", "03:54", "13:19", "18:00", "22:44", "00:12"),
        calculate("2026-06-21", 59.9139, 10.7522, "Europe/Oslo", CalculationMethod.MWL, highLatitude = HighLatitudeMode.TWILIGHT_ANGLE),
        toleranceMinutes = 3,
    )

    @Test fun ishaAfterMidnightStaysAfterMaghrib() {
        val times = calculate("2026-06-21", 59.9139, 10.7522, "Europe/Oslo", CalculationMethod.MWL)
        val ordered = Prayer.entries.map { times.adhan.getValue(it) }
        assertTrue(ordered.zipWithNext().all { (a, b) -> a.isBefore(b) })
    }

    @Test fun adjustmentsShiftOnlyTheirPrayer() {
        val base = muscat("2026-09-11", CalculationMethod.OMAN)
        val shifted = muscat("2026-09-11", CalculationMethod.OMAN, mapOf(Prayer.ASR to 3, Prayer.ISHA to -2))
        Prayer.entries.forEach { prayer ->
            val expected = when (prayer) {
                Prayer.ASR -> 3L
                Prayer.ISHA -> -2L
                else -> 0L
            }
            assertEquals(prayer.name, expected, Duration.between(base.adhan[prayer], shifted.adhan[prayer]).toMinutes())
        }
    }

    @Test fun nightTimesFallBetweenMaghribAndNextFajr() {
        val today = muscat("2026-09-11", CalculationMethod.OMAN)
        val tomorrow = muscat("2026-09-12", CalculationMethod.OMAN)
        assertTrue(today.middleOfNight.isAfter(today.adhan[Prayer.ISHA]))
        assertTrue(today.lastThirdOfNight.isAfter(today.middleOfNight))
        assertTrue(today.lastThirdOfNight.isBefore(tomorrow.adhan[Prayer.FAJR]))
    }

    @Test fun jafariMaghribWaitsForTheRednessToPass() {
        val tehran = calculate("2026-09-11", 35.6892, 51.3890, "Asia/Tehran", CalculationMethod.JAFARI)
        val sunset = calculate("2026-09-11", 35.6892, 51.3890, "Asia/Tehran", CalculationMethod.MWL).adhan.getValue(Prayer.MAGHRIB)
        val wait = Duration.between(sunset, tehran.adhan[Prayer.MAGHRIB]).toMinutes()
        assertTrue("Maghrib $wait min after sunset", wait in 12..25)
    }

    @Test fun methodFollowsCountry() {
        assertEquals(CalculationMethod.OMAN, CalculationMethod.forCountry("OM"))
        assertEquals(CalculationMethod.MAKKAH, CalculationMethod.forCountry("sa"))
        assertEquals(CalculationMethod.MOONSIGHTING, CalculationMethod.forCountry("GB"))
        assertEquals(CalculationMethod.SINGAPORE, CalculationMethod.forCountry("MY"))
        assertEquals(CalculationMethod.MWL, CalculationMethod.forCountry("FR"))
        assertEquals(CalculationMethod.MWL, CalculationMethod.forCountry(null))
    }
}
