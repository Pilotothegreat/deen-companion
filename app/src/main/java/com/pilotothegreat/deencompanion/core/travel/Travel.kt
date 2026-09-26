package com.pilotothegreat.deencompanion.core.travel

import com.pilotothegreat.deencompanion.data.location.CityIndex

/** Where the app thinks you are, relative to home. */
enum class TravelState {
    /** Near home, or no home set yet. */
    HOME,

    /** Far enough that travel is likely, but the app has not been told so. */
    SUSPECTED,

    /** You told it you are travelling. */
    CONFIRMED,
}

/**
 * Whether you are away, and never a guess acted on alone.
 *
 * The app suspects travel from distance and then asks. It does not decide, because the consequences
 * of deciding wrongly all fall on the reader: suppressed iqama alerts, a qasr card telling someone
 * at home they may shorten their prayers. A question costs a tap; a wrong answer costs a prayer.
 */
object Travel {

    /** Coming back inside this share of the threshold ends the trip, so a boundary can't flap. */
    private const val RETURN_FRACTION = 0.8

    fun distanceKm(homeLatitude: Double, homeLongitude: Double, latitude: Double, longitude: Double): Double =
        CityIndex.distanceKm(homeLatitude, homeLongitude, latitude, longitude)

    /**
     * @param previous what the app last concluded, so a confirmed trip is not cancelled by a step
     *   back towards the threshold.
     */
    fun state(distanceKm: Double, safarKm: Int, previous: TravelState): TravelState {
        val threshold = safarKm.toDouble()
        val returned = distanceKm < threshold * RETURN_FRACTION
        return when {
            returned -> TravelState.HOME
            distanceKm >= threshold && previous == TravelState.CONFIRMED -> TravelState.CONFIRMED
            distanceKm >= threshold -> TravelState.SUSPECTED
            // Between the return line and the threshold: whatever it was, it stays.
            else -> previous
        }
    }

    /**
     * Speed that suggests a vehicle, from a fix taken for another reason. It is only ever an offer
     * of a chip; the app never starts watching location to find out, and never hides an alert
     * because it thinks you are driving.
     */
    const val DRIVING_KPH = 25.0

    fun looksLikeDriving(speedMetresPerSecond: Float?): Boolean =
        speedMetresPerSecond != null && speedMetresPerSecond * 3.6 >= DRIVING_KPH
}
