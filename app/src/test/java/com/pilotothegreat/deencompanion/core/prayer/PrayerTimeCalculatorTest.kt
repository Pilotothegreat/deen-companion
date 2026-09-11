package com.pilotothegreat.deencompanion.core.prayer

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.min

/** Reference times from api.aladhan.com (angle-based high-latitude rule). */
class PrayerTimeCalculatorTest {

    private fun calculate(
        date: String,
        lat: Double,
        lon: Double,
        zone: String,
        method: CalculationMethod,
        school: AsrSchool = AsrSchool.STANDARD,
    ) = PrayerTimeCalculator.calculate(LocalDate.parse(date), lat, lon, ZoneId.of(zone), method, school)

    /** [expected] is Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha. */
    private fun assertTimes(expected: List<String>, actual: PrayerTimes, toleranceMinutes: Int = 2) {
        Prayer.entries.forEachIndexed { i, prayer ->
            val want = LocalTime.parse(expected[i])
            val got = actual[prayer]
            val diff = abs(want.toSecondOfDay() - got.toSecondOfDay()) / 60
            assertTrue("$prayer: expected $want, got $got", min(diff, 24 * 60 - diff) <= toleranceMinutes)
        }
    }

    /** Official Muscat timetable from mara.gov.om. */
    @Test fun omanMatchesMinistryTimetable() {
        fun oman(date: String) = calculate(date, 23.5880, 58.3829, "Asia/Muscat", CalculationMethod.OMAN)
        assertTimes(listOf("03:54", "05:20", "12:10", "15:29", "18:54", "20:15"), oman("2026-06-01"), 1)
        assertTimes(listOf("04:36", "05:52", "12:09", "15:36", "18:20", "19:31"), oman("2026-09-11"), 1)
        assertTimes(listOf("05:12", "06:32", "12:01", "15:04", "17:25", "18:39"), oman("2026-12-01"), 1)
    }

    @Test fun muscatMwl() = assertTimes(
        listOf("04:36", "05:52", "12:03", "15:31", "18:14", "19:26"),
        calculate("2026-09-11", 23.5880, 58.3829, "Asia/Muscat", CalculationMethod.MWL),
    )

    @Test fun muscatUmmAlQura() = assertTimes(
        listOf("04:33", "05:52", "12:03", "15:31", "18:14", "19:44"),
        calculate("2026-09-11", 23.5880, 58.3829, "Asia/Muscat", CalculationMethod.MAKKAH),
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

    @Test fun osloMidsummerFallsBackToAngleBasedPortions() = assertTimes(
        listOf("02:21", "03:54", "13:19", "18:00", "22:44", "00:12"),
        calculate("2026-06-21", 59.9139, 10.7522, "Europe/Oslo", CalculationMethod.MWL),
        toleranceMinutes = 3,
    )
}
