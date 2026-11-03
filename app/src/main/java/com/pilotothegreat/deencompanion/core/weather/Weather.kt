package com.pilotothegreat.deencompanion.core.weather

/**
 * What the sky is doing, reduced to the handful of states the app has something to say about.
 *
 * Weather earns its place here only where a dua exists for it. Fog, cloud and an ordinary clear day
 * produce nothing at all, because a card that appears every day is a card nobody reads.
 */
enum class WeatherCondition {
    CLEAR,
    CLOUDY,
    FOG,
    RAIN,
    SNOW,
    THUNDER,
    WIND,
    HEAT,
    COLD;

    /** The athkar category this routes to, or null when the app has nothing to add. */
    val athkarCategory: String?
        get() = when (this) {
            RAIN -> "rain"
            THUNDER -> "thunder"
            WIND -> "wind"
            SNOW -> "rain"
            CLEAR, CLOUDY, FOG, HEAT, COLD -> null
        }

    /** Higher conditions displace lower ones when several are true at once. */
    val priority: Int
        get() = when (this) {
            THUNDER -> 50
            SNOW -> 46
            RAIN -> 45
            WIND -> 42
            HEAT, COLD -> 38
            CLEAR, CLOUDY, FOG -> 0
        }
}

data class WeatherReading(
    val condition: WeatherCondition,
    val temperatureC: Double,
    val windKph: Double,
    /** Epoch millis the reading was taken. */
    val observedAt: Long,
)

object WmoCodes {

    /** Gusts at or above this are worth a dua whatever else the sky is doing. */
    const val WINDY_KPH = 45.0
    const val HOT_C = 43.0
    const val COLD_C = 2.0

    /**
     * The WMO code Open-Meteo reports, and the temperature and wind alongside it, reduced to one
     * condition. Thunder beats precipitation, precipitation beats wind, and wind beats temperature,
     * so a storm is never reported as merely "hot".
     */
    fun condition(code: Int, temperatureC: Double, windKph: Double): WeatherCondition {
        val fromCode = when (code) {
            0 -> WeatherCondition.CLEAR
            1, 2, 3 -> WeatherCondition.CLOUDY
            45, 48 -> WeatherCondition.FOG
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherCondition.RAIN
            71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW
            95, 96, 99 -> WeatherCondition.THUNDER
            else -> WeatherCondition.CLOUDY
        }
        return when {
            fromCode.priority > 0 -> fromCode
            windKph >= WINDY_KPH -> WeatherCondition.WIND
            temperatureC >= HOT_C -> WeatherCondition.HEAT
            temperatureC <= COLD_C -> WeatherCondition.COLD
            else -> fromCode
        }
    }
}
