package com.pilotothegreat.deencompanion.core.prayer

import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.HighLatitudeRule
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerAdjustments
import com.batoulapps.adhan2.SunnahTimes
import com.batoulapps.adhan2.data.DateComponents
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.time.ExperimentalTime
import com.batoulapps.adhan2.CalculationMethod as AdhanMethod
import com.batoulapps.adhan2.PrayerTimes as AdhanTimes

/** Parameters for methods Adhan has no preset for. */
private class CustomMethod(
    val fajrAngle: Double,
    val ishaAngle: Double,
    val adjustments: PrayerAdjustments = PrayerAdjustments(),
    /** Sun depression for Maghrib used by Shia methods; null means sunset. */
    val maghribAngle: Double? = null,
)

/**
 * Enum names are persisted in settings; do not rename. Methods with an Adhan preset use it as-is
 * so Adhan's own rules (Moonsighting seasonal adjustments, Singapore rounding) stay intact.
 */
enum class CalculationMethod(private val preset: AdhanMethod?, private val custom: CustomMethod? = null) {
    /** Oman Ministry of Endowments: 18°/18° with precautionary minutes, matching mara.gov.om tables. */
    OMAN(null, CustomMethod(18.0, 18.0, PrayerAdjustments(dhuhr = 6, asr = 5, maghrib = 6))),
    MWL(AdhanMethod.MUSLIM_WORLD_LEAGUE),
    ISNA(AdhanMethod.NORTH_AMERICA),
    EGYPT(AdhanMethod.EGYPTIAN),
    /** Umm al-Qura: Isha 90 minutes after Maghrib, 120 in Ramadan. */
    MAKKAH(AdhanMethod.UMM_AL_QURA),
    KARACHI(AdhanMethod.KARACHI),
    JAFARI(null, CustomMethod(16.0, 14.0, maghribAngle = 4.0)),
    TEHRAN(null, CustomMethod(17.7, 14.0, maghribAngle = 4.5)),
    DUBAI(AdhanMethod.DUBAI),
    KUWAIT(AdhanMethod.KUWAIT),
    QATAR(AdhanMethod.QATAR),
    SINGAPORE(AdhanMethod.SINGAPORE),
    TURKEY(AdhanMethod.TURKEY),
    MOONSIGHTING(AdhanMethod.MOON_SIGHTING_COMMITTEE);

    internal val maghribAngle: Double? get() = custom?.maghribAngle

    internal fun parameters(): CalculationParameters = preset?.parameters ?: requireNotNull(custom).let {
        CalculationParameters(
            fajrAngle = it.fajrAngle,
            ishaAngle = it.ishaAngle,
            method = AdhanMethod.OTHER,
            methodAdjustments = it.adjustments,
        )
    }

    companion object {
        /** The authority most commonly followed in a country (ISO 3166 alpha-2), or MWL. */
        fun forCountry(countryCode: String?): CalculationMethod = when (countryCode?.uppercase()) {
            "OM" -> OMAN
            "SA" -> MAKKAH
            "AE" -> DUBAI
            "KW" -> KUWAIT
            "QA" -> QATAR
            "EG" -> EGYPT
            "PK", "IN", "BD", "AF" -> KARACHI
            "TR" -> TURKEY
            "SG", "MY", "ID", "BN" -> SINGAPORE
            "IR" -> TEHRAN
            "US", "CA", "GB" -> MOONSIGHTING
            else -> MWL
        }
    }
}

/** Enum names are persisted in settings; do not rename. */
enum class AsrSchool { STANDARD, HANAFI }

/** Enum names are persisted in settings; do not rename. */
enum class HighLatitudeMode {
    /** Seventh of the night above 48° latitude, where twilight can last all night; middle of the night elsewhere. */
    AUTO,
    MIDDLE_OF_NIGHT,
    SEVENTH_OF_NIGHT,
    TWILIGHT_ANGLE,
}

/** A day's adhan times plus the night times used for qiyam. */
data class DayTimes(
    val adhan: Map<Prayer, ZonedDateTime>,
    val middleOfNight: ZonedDateTime,
    val lastThirdOfNight: ZonedDateTime,
)

/** Prayer times from the Adhan library (Meeus astronomical algorithms). */
@OptIn(ExperimentalTime::class)
object PrayerEngine {

    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zone: ZoneId,
        method: CalculationMethod,
        asrSchool: AsrSchool = AsrSchool.STANDARD,
        highLatitude: HighLatitudeMode = HighLatitudeMode.AUTO,
        adjustments: Map<Prayer, Int> = emptyMap(),
        hijriMonth: Int? = null,
    ): DayTimes {
        val ramadan = (hijriMonth ?: hijriMonthOf(date)) == 9
        val ishaExtra = if (method == CalculationMethod.MAKKAH && ramadan) 30 else 0
        val params = method.parameters().copy(
            madhab = if (asrSchool == AsrSchool.HANAFI) Madhab.HANAFI else Madhab.SHAFI,
            highLatitudeRule = highLatitudeRule(highLatitude, latitude),
            prayerAdjustments = PrayerAdjustments(
                fajr = adjustments[Prayer.FAJR] ?: 0,
                sunrise = adjustments[Prayer.SUNRISE] ?: 0,
                dhuhr = adjustments[Prayer.DHUHR] ?: 0,
                asr = adjustments[Prayer.ASR] ?: 0,
                maghrib = adjustments[Prayer.MAGHRIB] ?: 0,
                isha = (adjustments[Prayer.ISHA] ?: 0) + ishaExtra,
            ),
        )
        val times = AdhanTimes(Coordinates(latitude, longitude), DateComponents(date.year, date.monthValue, date.dayOfMonth), params)
        val sunnah = SunnahTimes(times)

        val maghrib = method.maghribAngle
            ?.let { SolarAngle.eveningTime(date, latitude, longitude, it) }
            ?.plusMinutes((adjustments[Prayer.MAGHRIB] ?: 0).toLong())
            ?.withZoneSameInstant(zone)
            ?: times.maghrib.at(zone)

        return DayTimes(
            adhan = mapOf(
                Prayer.FAJR to times.fajr.at(zone),
                Prayer.SUNRISE to times.sunrise.at(zone),
                Prayer.DHUHR to times.dhuhr.at(zone),
                Prayer.ASR to times.asr.at(zone),
                Prayer.MAGHRIB to maghrib,
                Prayer.ISHA to times.isha.at(zone),
            ),
            middleOfNight = sunnah.middleOfTheNight.at(zone),
            lastThirdOfNight = sunnah.lastThirdOfTheNight.at(zone),
        )
    }

    private fun highLatitudeRule(mode: HighLatitudeMode, latitude: Double): HighLatitudeRule = when (mode) {
        HighLatitudeMode.AUTO -> if (abs(latitude) > 48) HighLatitudeRule.SEVENTH_OF_THE_NIGHT else HighLatitudeRule.MIDDLE_OF_THE_NIGHT
        HighLatitudeMode.MIDDLE_OF_NIGHT -> HighLatitudeRule.MIDDLE_OF_THE_NIGHT
        HighLatitudeMode.SEVENTH_OF_NIGHT -> HighLatitudeRule.SEVENTH_OF_THE_NIGHT
        HighLatitudeMode.TWILIGHT_ANGLE -> HighLatitudeRule.TWILIGHT_ANGLE
    }

    private fun kotlin.time.Instant.at(zone: ZoneId): ZonedDateTime =
        java.time.Instant.ofEpochMilli(toEpochMilliseconds()).atZone(zone).truncatedTo(ChronoUnit.MINUTES)

    private fun hijriMonthOf(date: LocalDate): Int =
        runCatching { HijrahDate.from(date).get(ChronoField.MONTH_OF_YEAR) }.getOrDefault(0)
}

/** Solar depression times for the Maghrib angle Adhan doesn't model (praytimes.org formulas). */
internal object SolarAngle {

    /** When the sun sinks [angle]° below the horizon in the evening, rounded to the minute; null if it never does. */
    fun eveningTime(date: LocalDate, latitude: Double, longitude: Double, angle: Double): ZonedDateTime? {
        val jDate = julianDay(date) - longitude / (15.0 * 24.0)
        var hours = 18.0
        repeat(2) {
            val sun = sunPosition(jDate + hours / 24.0)
            val cosV = (-dsin(angle) - dsin(sun.declination) * dsin(latitude)) / (dcos(sun.declination) * dcos(latitude))
            if (cosV < -1.0 || cosV > 1.0) return null
            hours = 12.0 - sun.equation + Math.toDegrees(acos(cosV)) / 15.0
        }
        val utcSeconds = ((hours - longitude / 15.0) * 3600).roundToLong()
        return date.atStartOfDay(ZoneOffset.UTC).plusSeconds(utcSeconds + 30).truncatedTo(ChronoUnit.MINUTES)
    }

    private class Sun(val declination: Double, val equation: Double)

    private fun sunPosition(jd: Double): Sun {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = fixHour(Math.toDegrees(atan2(dcos(e) * dsin(l), dcos(l))) / 15.0)
        var equation = q / 15.0 - ra
        if (equation > 12) equation -= 24.0 else if (equation < -12) equation += 24.0
        return Sun(Math.toDegrees(asin(dsin(e) * dsin(l))), equation)
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

    private fun fixAngle(a: Double) = ((a % 360.0) + 360.0) % 360.0
    private fun fixHour(h: Double) = ((h % 24.0) + 24.0) % 24.0
    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
}
