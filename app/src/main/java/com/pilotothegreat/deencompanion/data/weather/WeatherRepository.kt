package com.pilotothegreat.deencompanion.data.weather

import com.pilotothegreat.deencompanion.core.weather.WeatherReading
import com.pilotothegreat.deencompanion.core.weather.WmoCodes
import com.pilotothegreat.deencompanion.data.net.Http
import com.pilotothegreat.deencompanion.data.settings.AppSettings
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import kotlin.math.round

/**
 * Current conditions from Open-Meteo, which needs no account and no key.
 *
 * This is the first time the app sends anything about where you are to anyone, so it is deliberately
 * careful: coordinates are rounded to two decimal places — about a kilometre, enough for weather and
 * not enough to place a house — the call is made only while the app is in front of you, at most once
 * an hour, and the feature can be switched off. It is declared in PRIVACY.md and in Play's data
 * safety form.
 */
class WeatherRepository(private val fetch: suspend (String) -> String = { Http.getText(it) }) {

    companion object {
        const val CACHE_MILLIS = 60 * 60 * 1000L
        private const val ENDPOINT = "https://api.open-meteo.com/v1/forecast"

        /** Two decimals is roughly a kilometre; the app never sends a finer position than that. */
        fun blunt(value: Double): Double = round(value * 100) / 100

        internal fun url(latitude: Double, longitude: Double): String =
            "$ENDPOINT?latitude=${blunt(latitude)}&longitude=${blunt(longitude)}" +
                "&current=temperature_2m,wind_speed_10m,weather_code&wind_speed_unit=kmh"
    }

    private val lock = Mutex()
    @Volatile private var cached: WeatherReading? = null
    @Volatile private var cachedFor: Pair<Double, Double>? = null

    /** The last reading, without going to the network. */
    fun current(): WeatherReading? = cached

    /**
     * Conditions where the settings say you are, or null when weather is off, the location is still
     * the default, or the call fails. A failure is never surfaced: weather is a nicety, and an error
     * banner for it would be worse than saying nothing.
     */
    suspend fun refresh(settings: AppSettings, nowMillis: Long): WeatherReading? {
        if (!settings.smart.weather || settings.location.isDefault) return null
        val here = blunt(settings.location.latitude) to blunt(settings.location.longitude)
        cached?.let { if (cachedFor == here && nowMillis - it.observedAt < CACHE_MILLIS) return it }
        return lock.withLock {
            cached?.let { if (cachedFor == here && nowMillis - it.observedAt < CACHE_MILLIS) return it }
            runCatching { parse(fetch(url(here.first, here.second)), nowMillis) }
                .onFailure { Timber.d(it, "Weather unavailable") }
                .getOrNull()
                ?.also {
                    cached = it
                    cachedFor = here
                }
        }
    }

    private fun parse(body: String, nowMillis: Long): WeatherReading {
        val current = org.json.JSONObject(body).getJSONObject("current")
        val temperature = current.getDouble("temperature_2m")
        val wind = current.getDouble("wind_speed_10m")
        return WeatherReading(
            condition = WmoCodes.condition(current.getInt("weather_code"), temperature, wind),
            temperatureC = temperature,
            windKph = wind,
            observedAt = nowMillis,
        )
    }
}
