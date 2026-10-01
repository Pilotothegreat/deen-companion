package com.pilotothegreat.deencompanion.data.moment

import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.moment.Dismissal
import com.pilotothegreat.deencompanion.core.moment.Moment
import com.pilotothegreat.deencompanion.core.moment.MomentKind
import com.pilotothegreat.deencompanion.core.travel.Travel
import com.pilotothegreat.deencompanion.core.travel.TravelState
import com.pilotothegreat.deencompanion.core.weather.WeatherCondition
import com.pilotothegreat.deencompanion.core.weather.WeatherReading
import com.pilotothegreat.deencompanion.data.nature.Earthquake
import com.pilotothegreat.deencompanion.data.nature.Eclipse
import com.pilotothegreat.deencompanion.data.nature.EclipseKind
import com.pilotothegreat.deencompanion.data.settings.AppSettings
import java.time.ZonedDateTime

/**
 * Turns readings about the world into moments. Each producer is a pure function of its input and the
 * clock, so what appears on Today can be reasoned about without a device in hand.
 */
object Producers {

    /** Weather stays up as long as it lasts, and says nothing at all on an ordinary day. */
    fun weather(reading: WeatherReading?, now: ZonedDateTime): Moment? {
        val condition = reading?.condition ?: return null
        if (condition.priority == 0) return null
        val (title, body) = when (condition) {
            WeatherCondition.RAIN -> R.string.weather_rain to R.string.weather_rain_body
            WeatherCondition.THUNDER -> R.string.weather_thunder to R.string.weather_thunder_body
            WeatherCondition.WIND -> R.string.weather_wind to R.string.weather_wind_body
            WeatherCondition.SNOW -> R.string.weather_snow to R.string.weather_snow_body
            WeatherCondition.HEAT -> R.string.weather_heat to R.string.weather_heat_body
            WeatherCondition.COLD -> R.string.weather_cold to R.string.weather_cold_body
            else -> return null
        }
        return Moment(
            id = "weather-${condition.name.lowercase()}",
            kind = MomentKind.NATURE,
            priority = condition.priority,
            title = title,
            body = body,
            startsAt = now.minusMinutes(1),
            // As long as the reading is good for; a new reading replaces it.
            endsAt = now.plusHours(1),
            athkarCategory = condition.athkarCategory,
        )
    }

    /**
     * Travel, in three cards that never appear together: the question, the duas once you say yes,
     * and the note about shortening prayers. Nothing here changes a computed time — the prayer times
     * where you are are the prayer times where you are.
     */
    fun travel(settings: AppSettings, distanceKm: Double?, now: ZonedDateTime): List<Moment> {
        if (distanceKm == null) return emptyList()
        val dayEnd = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        return when (settings.smart.travelState) {
            TravelState.SUSPECTED -> listOf(
                Moment(
                    id = "travel-ask",
                    kind = MomentKind.TRAVEL,
                    priority = 58,
                    title = R.string.travel_ask,
                    body = R.string.travel_ask_body,
                    count = distanceKm.toInt(),
                    startsAt = now.minusMinutes(1),
                    endsAt = dayEnd,
                    dismissal = Dismissal.TODAY,
                ),
            )
            TravelState.CONFIRMED -> listOf(
                Moment(
                    id = "travel-duas",
                    kind = MomentKind.TRAVEL,
                    priority = 52,
                    title = R.string.travel_duas,
                    body = R.string.travel_duas_body,
                    startsAt = now.minusMinutes(1),
                    endsAt = dayEnd,
                    athkarCategory = "travelling",
                ),
                Moment(
                    id = "travel-qasr",
                    kind = MomentKind.TRAVEL,
                    priority = 50,
                    title = R.string.travel_qasr,
                    body = R.string.travel_qasr_body,
                    startsAt = now.minusMinutes(1),
                    endsAt = dayEnd,
                    dismissal = Dismissal.TODAY,
                ),
            )
            TravelState.HOME -> emptyList()
        }
    }

    /**
     * An eclipse, and what the app is honest about.
     *
     * A lunar eclipse gets a real answer, because whether the moon is above your horizon can be
     * computed. A solar eclipse gets the date and NASA's own list of regions and nothing more: the
     * path of totality is a narrow track this app has no table for, and the prayer follows seeing
     * the eclipse rather than reading about it. Neither card treats an eclipse as an omen; it is a
     * sign, and the sunnah is prayer, remembrance and charity.
     */
    fun eclipse(eclipse: Eclipse?, visibleHere: Boolean, now: ZonedDateTime): Moment? {
        if (eclipse == null) return null
        val at = eclipse.at.atZone(now.zone)
        val solar = eclipse.kind == EclipseKind.SOLAR
        return Moment(
            id = "eclipse-${eclipse.at.epochSecond}",
            kind = MomentKind.NATURE,
            priority = if (visibleHere) 72 else 44,
            title = if (solar) R.string.eclipse_solar else R.string.eclipse_lunar,
            body = when {
                solar -> R.string.eclipse_solar_body
                visibleHere -> R.string.eclipse_lunar_visible_body
                else -> R.string.eclipse_lunar_elsewhere_body
            },
            startsAt = at.minusDays(1),
            endsAt = at.plusHours(4),
            peak = at,
            dismissal = Dismissal.TODAY,
        )
    }

    /**
     * A nearby earthquake, as a card and never as a notification. Android's own earthquake alerts
     * are faster and better placed; a second alarm for the same tremor frightens without helping.
     */
    fun earthquake(quake: Earthquake?, now: ZonedDateTime): Moment? {
        if (quake == null) return null
        return Moment(
            id = "quake-${quake.id}",
            kind = MomentKind.NATURE,
            priority = 62,
            title = R.string.quake_title,
            body = R.string.quake_body,
            count = quake.distanceKm.toInt(),
            startsAt = now.minusMinutes(1),
            endsAt = java.time.Instant.ofEpochMilli(quake.atMillis).atZone(now.zone).plusHours(24),
            athkarCategory = "fear",
            dismissal = Dismissal.PERMANENT,
        )
    }

    /** Offered from a fix taken for another reason; never from watching where you go. */
    fun driving(speedMetresPerSecond: Float?, now: ZonedDateTime): Moment? {
        if (!Travel.looksLikeDriving(speedMetresPerSecond)) return null
        return Moment(
            id = "driving",
            kind = MomentKind.TRAVEL,
            priority = 30,
            title = R.string.travel_driving,
            body = R.string.travel_driving_body,
            startsAt = now.minusMinutes(1),
            endsAt = now.plusHours(2),
            dismissal = Dismissal.TODAY,
        )
    }
}
