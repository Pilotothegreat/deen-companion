package com.pilotothegreat.deencompanion.core.qibla

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object QiblaMath {
    const val KAABA_LATITUDE = 21.4225
    const val KAABA_LONGITUDE = 39.8262
    private const val EARTH_RADIUS_KM = 6371.0

    /** Initial great-circle bearing to the Kaaba, in degrees clockwise from true north. */
    fun bearing(latitude: Double, longitude: Double): Double {
        val phi = Math.toRadians(latitude)
        val phiKaaba = Math.toRadians(KAABA_LATITUDE)
        val deltaLambda = Math.toRadians(KAABA_LONGITUDE - longitude)
        val y = sin(deltaLambda) * cos(phiKaaba)
        val x = cos(phi) * sin(phiKaaba) - sin(phi) * cos(phiKaaba) * cos(deltaLambda)
        return normalize(Math.toDegrees(atan2(y, x)))
    }

    /** Great-circle (haversine) distance to the Kaaba. */
    fun distanceKm(latitude: Double, longitude: Double): Double {
        val dPhi = Math.toRadians(KAABA_LATITUDE - latitude)
        val dLambda = Math.toRadians(KAABA_LONGITUDE - longitude)
        val a = sin(dPhi / 2) * sin(dPhi / 2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(KAABA_LATITUDE)) *
            sin(dLambda / 2) * sin(dLambda / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }

    fun normalize(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0

    /** True when [heading] is within [toleranceDegrees] of [bearing]. */
    fun isAligned(heading: Double, bearing: Double, toleranceDegrees: Double): Boolean {
        val diff = abs(normalize(bearing - heading))
        return diff <= toleranceDegrees || diff >= 360.0 - toleranceDegrees
    }

    /** Value near [current] equivalent to [target] modulo 360, so rotations take the short way round. */
    fun unwrap(target: Float, current: Float): Float {
        var diff = (target - current) % 360f
        if (diff < -180f) diff += 360f
        if (diff > 180f) diff -= 360f
        return current + diff
    }
}
