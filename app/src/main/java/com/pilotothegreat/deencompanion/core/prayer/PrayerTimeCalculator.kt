package com.pilotothegreat.deencompanion.core.prayer

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

sealed interface IshaRule {
    /** Isha when the sun is [degrees] below the horizon. */
    data class Angle(val degrees: Double) : IshaRule

    /** Isha a fixed interval after Maghrib, longer during Ramadan. */
    data class Interval(val minutes: Int, val ramadanMinutes: Int = minutes) : IshaRule
}

/** Enum names are persisted in settings; do not rename. */
enum class CalculationMethod(
    val fajrAngle: Double,
    val isha: IshaRule,
    val maghribAngle: Double? = null,
    val maghribOffsetMinutes: Int = 0,
    val dhuhrOffsetMinutes: Int = 1,
    val asrOffsetMinutes: Int = 0,
) {
    /** Oman Ministry of Endowments: 18°/18° with precautionary minutes, matching mara.gov.om tables. */
    OMAN(18.0, IshaRule.Angle(18.0), maghribOffsetMinutes = 6, dhuhrOffsetMinutes = 6, asrOffsetMinutes = 5),
    MWL(18.0, IshaRule.Angle(17.0)),
    ISNA(15.0, IshaRule.Angle(15.0)),
    EGYPT(19.5, IshaRule.Angle(17.5)),
    MAKKAH(18.5, IshaRule.Interval(90, 120)),
    KARACHI(18.0, IshaRule.Angle(18.0)),
    JAFARI(16.0, IshaRule.Angle(14.0), maghribAngle = 4.0),
    TEHRAN(17.7, IshaRule.Angle(14.0), maghribAngle = 4.5),
}

/** Enum names are persisted in settings; do not rename. */
enum class AsrSchool(val shadowFactor: Double) {
    STANDARD(1.0),
    HANAFI(2.0),
}

data class PrayerTimes(
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
) {
    operator fun get(prayer: Prayer): LocalTime = when (prayer) {
        Prayer.FAJR -> fajr
        Prayer.SUNRISE -> sunrise
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }
}

/**
 * Astronomical prayer times following the praytimes.org algorithm: the sun's position is
 * evaluated near each prayer's approximate time, and prayers whose angle is never reached
 * (high latitudes in summer) fall back to the angle-based portion of the night.
 */
object PrayerTimeCalculator {

    private const val SUNRISE_DEPRESSION = 0.833

    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zone: ZoneId,
        method: CalculationMethod,
        asrSchool: AsrSchool = AsrSchool.STANDARD,
        hijriMonth: Int? = null,
    ): PrayerTimes {
        // Offset at local noon so DST transition days use the offset in force during the day.
        val offsetHours = zone.rules.getOffset(date.atTime(LocalTime.NOON)).totalSeconds / 3600.0
        return calculate(date, latitude, longitude, offsetHours, method, asrSchool, hijriMonth)
    }

    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        timezoneOffsetHours: Double,
        method: CalculationMethod,
        asrSchool: AsrSchool = AsrSchool.STANDARD,
        hijriMonth: Int? = null,
    ): PrayerTimes {
        val sky = Sky(julianDay(date) - longitude / (15.0 * 24.0), latitude)
        val shift = timezoneOffsetHours - longitude / 15.0

        val sunrise = sky.angleTime(SUNRISE_DEPRESSION, 6.0, rising = true, clamp = true)!! + shift
        val sunset = sky.angleTime(SUNRISE_DEPRESSION, 18.0, rising = false, clamp = true)!! + shift
        val night = 24.0 - (sunset - sunrise)
        val dhuhr = sky.midDay(12.0) + shift + method.dhuhrOffsetMinutes / 60.0
        val asr = sky.asrTime(asrSchool.shadowFactor, 13.0) + shift + method.asrOffsetMinutes / 60.0

        val fajrPortion = method.fajrAngle / 60.0 * night
        val fajrByAngle = sky.angleTime(method.fajrAngle, 5.0, rising = true)?.plus(shift)
        val fajr = if (fajrByAngle == null || sunrise - fajrByAngle > fajrPortion) {
            sunrise - fajrPortion
        } else {
            fajrByAngle
        }

        val maghribBase = method.maghribAngle
            ?.let { sky.angleTime(it, 18.0, rising = false)?.plus(shift) }
            ?: sunset
        val maghrib = maghribBase + method.maghribOffsetMinutes / 60.0

        val isha = when (val rule = method.isha) {
            is IshaRule.Interval -> {
                val ramadan = (hijriMonth ?: hijriMonthOf(date)) == 9
                maghrib + (if (ramadan) rule.ramadanMinutes else rule.minutes) / 60.0
            }
            is IshaRule.Angle -> {
                val portion = rule.degrees / 60.0 * night
                val byAngle = sky.angleTime(rule.degrees, 18.0, rising = false)?.plus(shift)
                if (byAngle == null || byAngle - sunset > portion) sunset + portion else byAngle
            }
        }

        return PrayerTimes(
            fajr = toTime(fajr),
            sunrise = toTime(sunrise),
            dhuhr = toTime(dhuhr),
            asr = toTime(asr),
            maghrib = toTime(maghrib),
            isha = toTime(isha),
        )
    }

    private class Sky(private val jDate: Double, private val latitude: Double) {

        fun midDay(t: Double): Double = fixHour(12.0 - sun(t).equation)

        /** Time the sun reaches [angle] degrees below the horizon; null if it never does. */
        fun angleTime(angle: Double, t: Double, rising: Boolean, clamp: Boolean = false): Double? {
            val s = sun(t)
            var cosV = (-dsin(angle) - dsin(s.declination) * dsin(latitude)) /
                (dcos(s.declination) * dcos(latitude))
            if (cosV < -1.0 || cosV > 1.0) {
                if (!clamp) return null
                cosV = cosV.coerceIn(-1.0, 1.0)
            }
            val v = darccos(cosV) / 15.0
            val noon = midDay(t)
            return if (rising) noon - v else noon + v
        }

        fun asrTime(shadowFactor: Double, t: Double): Double {
            val declination = sun(t).declination
            val angle = -darccot(shadowFactor + dtan(abs(latitude - declination)))
            return angleTime(angle, t, rising = false, clamp = true)!!
        }

        private fun sun(t: Double) = sunPosition(jDate + t / 24.0)
    }

    private class SunPosition(val declination: Double, val equation: Double)

    private fun sunPosition(jd: Double): SunPosition {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = fixHour(darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0)
        var equation = q / 15.0 - ra
        if (equation > 12) equation -= 24.0 else if (equation < -12) equation += 24.0
        return SunPosition(darcsin(dsin(e) * dsin(l)), equation)
    }

    private fun julianDay(date: LocalDate): Double {
        var year = date.year
        var month = date.monthValue
        if (month <= 2) {
            year -= 1
            month += 12
        }
        val a = floor(year / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + date.dayOfMonth + b - 1524.5
    }

    private fun hijriMonthOf(date: LocalDate): Int =
        runCatching { HijrahDate.from(date).get(ChronoField.MONTH_OF_YEAR) }.getOrDefault(0)

    private fun toTime(hours: Double): LocalTime {
        val minutes = (fixHour(hours) * 60).roundToInt() % (24 * 60)
        return LocalTime.of(minutes / 60, minutes % 60)
    }

    private fun fixAngle(a: Double) = ((a % 360.0) + 360.0) % 360.0
    private fun fixHour(h: Double) = ((h % 24.0) + 24.0) % 24.0
    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
    private fun dtan(d: Double) = tan(Math.toRadians(d))
    private fun darcsin(x: Double) = Math.toDegrees(asin(x))
    private fun darccos(x: Double) = Math.toDegrees(acos(x))
    private fun darctan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun darccot(x: Double) = Math.toDegrees(atan(1.0 / x))
}
