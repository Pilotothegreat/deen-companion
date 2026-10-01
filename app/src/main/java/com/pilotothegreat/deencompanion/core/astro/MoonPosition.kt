package com.pilotothegreat.deencompanion.core.astro

import java.time.Instant
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Where the moon is, to about a twentieth of a degree.
 *
 * This exists for one question: at the moment of a lunar eclipse, is the moon above the horizon
 * where you are? A card that announces an eclipse nobody in the country can see is worse than no
 * card, and the prayer follows witnessing the eclipse, not the almanac.
 *
 * The series is the abridged lunar theory from Meeus, *Astronomical Algorithms*, chapter 47 — the
 * largest few terms only. That is far more precision than a horizon check needs and small enough to
 * read in one sitting.
 */
object MoonPosition {

    private const val DEG = Math.PI / 180.0

    /** Altitude of the moon's centre above the horizon, in degrees; negative means below it. */
    fun altitudeDegrees(at: Instant, latitude: Double, longitude: Double): Double {
        val jd = julianDay(at)
        val t = (jd - 2451545.0) / 36525.0

        val meanLongitude = 218.316 + 481267.8813 * t
        val meanAnomaly = (134.963 + 477198.8676 * t) * DEG
        val argumentOfLatitude = (93.272 + 483202.0175 * t) * DEG
        val elongation = (297.850 + 445267.1115 * t) * DEG
        val sunAnomaly = (357.529 + 35999.0503 * t) * DEG

        val lambda = (
            meanLongitude +
                6.289 * sin(meanAnomaly) +
                1.274 * sin(2 * elongation - meanAnomaly) +
                0.658 * sin(2 * elongation) +
                0.214 * sin(2 * meanAnomaly) -
                0.186 * sin(sunAnomaly) -
                0.114 * sin(2 * argumentOfLatitude)
            ) * DEG
        val beta = (
            5.128 * sin(argumentOfLatitude) +
                0.281 * sin(meanAnomaly + argumentOfLatitude) -
                0.278 * sin(argumentOfLatitude - meanAnomaly) -
                0.173 * sin(2 * elongation - argumentOfLatitude)
            ) * DEG

        val obliquity = (23.439 - 0.0000004 * (jd - 2451545.0)) * DEG
        val rightAscension = atan2(
            sin(lambda) * cos(obliquity) - kotlin.math.tan(beta) * sin(obliquity),
            cos(lambda),
        )
        val declination = asin(sin(beta) * cos(obliquity) + cos(beta) * sin(obliquity) * sin(lambda))

        val siderealDegrees = 280.46061837 + 360.98564736629 * (jd - 2451545.0) + longitude
        val hourAngle = siderealDegrees * DEG - rightAscension
        val phi = latitude * DEG
        val altitude = asin(sin(phi) * sin(declination) + cos(phi) * cos(declination) * cos(hourAngle))
        return altitude / DEG
    }

    /**
     * True when the moon is high enough to actually be seen. Six degrees allows for the horizon
     * being a building or a hill rather than a mathematical line.
     */
    fun isUp(at: Instant, latitude: Double, longitude: Double, minimumAltitude: Double = 6.0): Boolean =
        altitudeDegrees(at, latitude, longitude) >= minimumAltitude

    /** Julian Day, counted from noon UT on 1 January 4713 BC, as every ephemeris does. */
    fun julianDay(at: Instant): Double = at.toEpochMilli() / 86_400_000.0 + 2440587.5
}
