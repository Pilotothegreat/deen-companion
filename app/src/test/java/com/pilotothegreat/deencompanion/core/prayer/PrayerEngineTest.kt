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

    /**
     * The Ministry of Endowments' own table (mara.gov.om, 2026), the 1st and 15th of each month in
     * Muscat and Salalah, as the city lists place them. Over the whole year most prayers match to the
     * minute and 99% within one; a rare Asr is two out. What must not happen again is a drift: +6
     * precautionary minutes made Dhuhr and Maghrib a minute late on most days, and the total catches it.
     */
    @Test fun omanMatchesTheMinistryTable() {
        val places = mapOf("Muscat" to (23.5841 to 58.4078), "Salalah" to (17.015 to 54.0924))
        val bias = IntArray(6)
        MINISTRY_2026.forEach { row ->
            val cells = row.split(",")
            val (lat, lon) = places.getValue(cells[0])
            val day = calculate(cells[1], lat, lon, "Asia/Muscat", CalculationMethod.OMAN)
            Prayer.entries.forEachIndexed { i, prayer ->
                val got = day.adhan.getValue(prayer).toLocalTime()
                val diff = Duration.between(LocalTime.parse(cells[i + 2]), got).toMinutes().toInt()
                assertTrue("${cells[0]} ${cells[1]} $prayer: $got, ministry ${cells[i + 2]}", abs(diff) <= 2)
                bias[i] += diff
            }
        }
        bias.forEachIndexed { i, total ->
            assertTrue("${Prayer.entries[i]} runs ${total} minutes off over ${MINISTRY_2026.size} days", abs(total) <= MINISTRY_2026.size / 3)
        }
    }

    private companion object {
        /** City, date, then Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha as the ministry publishes them. */
        val MINISTRY_2026 = listOf(
        "Muscat,2026-01-01,05:29,06:49,12:15,15:16,17:36,18:52",
        "Muscat,2026-01-15,05:32,06:51,12:21,15:25,17:46,19:01",
        "Muscat,2026-02-01,05:30,06:48,12:25,15:35,17:58,19:11",
        "Muscat,2026-02-15,05:24,06:40,12:26,15:42,18:07,19:18",
        "Muscat,2026-03-01,05:14,06:29,12:24,15:45,18:14,19:25",
        "Muscat,2026-03-15,05:01,06:16,12:21,15:46,18:21,19:31",
        "Muscat,2026-04-01,04:43,05:59,12:16,15:43,18:27,19:38",
        "Muscat,2026-04-15,04:29,05:46,12:12,15:39,18:33,19:46",
        "Muscat,2026-05-01,04:13,05:33,12:09,15:35,18:40,19:55",
        "Muscat,2026-05-15,04:02,05:25,12:08,15:31,18:46,20:05",
        "Muscat,2026-06-01,03:54,05:20,12:10,15:29,18:54,20:15",
        "Muscat,2026-06-15,03:52,05:20,12:12,15:30,19:00,20:22",
        "Muscat,2026-07-01,03:56,05:24,12:16,15:34,19:03,20:25",
        "Muscat,2026-07-15,04:03,05:29,12:18,15:38,19:02,20:22",
        "Muscat,2026-08-01,04:14,05:37,12:18,15:42,18:55,20:12",
        "Muscat,2026-08-15,04:23,05:43,12:16,15:43,18:45,20:00",
        "Muscat,2026-09-01,04:32,05:49,12:12,15:40,18:30,19:43",
        "Muscat,2026-09-15,04:38,05:53,12:07,15:34,18:16,19:27",
        "Muscat,2026-10-01,04:43,05:58,12:02,15:26,18:00,19:10",
        "Muscat,2026-10-15,04:49,06:04,11:58,15:18,17:47,18:57",
        "Muscat,2026-11-01,04:56,06:12,11:55,15:09,17:34,18:45",
        "Muscat,2026-11-15,05:03,06:21,11:56,15:05,17:27,18:40",
        "Muscat,2026-12-01,05:12,06:32,12:01,15:04,17:25,18:39",
        "Muscat,2026-12-15,05:20,06:41,12:07,15:07,17:28,18:43",
        "Salalah,2026-01-01,05:36,06:53,12:32,15:43,18:06,19:18",
        "Salalah,2026-01-15,05:40,06:56,12:38,15:51,18:14,19:26",
        "Salalah,2026-02-01,05:41,06:55,12:42,15:59,18:24,19:34",
        "Salalah,2026-02-15,05:37,06:50,12:43,16:02,18:30,19:39",
        "Salalah,2026-03-01,05:30,06:42,12:41,16:03,18:35,19:42",
        "Salalah,2026-03-15,05:20,06:32,12:38,16:00,18:38,19:45",
        "Salalah,2026-04-01,05:06,06:19,12:33,15:53,18:42,19:49",
        "Salalah,2026-04-15,04:54,06:08,12:29,15:46,18:44,19:53",
        "Salalah,2026-05-01,04:42,05:58,12:26,15:38,18:48,19:59",
        "Salalah,2026-05-15,04:34,05:52,12:25,15:38,18:53,20:06",
        "Salalah,2026-06-01,04:29,05:49,12:26,15:47,18:59,20:14",
        "Salalah,2026-06-15,04:28,05:50,12:29,15:53,19:03,20:20",
        "Salalah,2026-07-01,04:32,05:54,12:32,15:56,19:06,20:23",
        "Salalah,2026-07-15,04:38,05:58,12:35,15:54,19:06,20:21",
        "Salalah,2026-08-01,04:46,06:03,12:35,15:46,19:02,20:14",
        "Salalah,2026-08-15,04:51,06:07,12:33,15:46,18:55,20:05",
        "Salalah,2026-09-01,04:57,06:10,12:29,15:47,18:43,19:51",
        "Salalah,2026-09-15,04:59,06:12,12:24,15:45,18:31,19:39",
        "Salalah,2026-10-01,05:02,06:14,12:18,15:41,18:18,19:25",
        "Salalah,2026-10-15,05:04,06:16,12:15,15:36,18:08,19:15",
        "Salalah,2026-11-01,05:08,06:22,12:12,15:31,17:58,19:06",
        "Salalah,2026-11-15,05:13,06:28,12:13,15:29,17:53,19:03",
        "Salalah,2026-12-01,05:20,06:37,12:18,15:30,17:53,19:05",
        "Salalah,2026-12-15,05:28,06:45,12:24,15:34,17:57,19:09",
        )
    }
}
