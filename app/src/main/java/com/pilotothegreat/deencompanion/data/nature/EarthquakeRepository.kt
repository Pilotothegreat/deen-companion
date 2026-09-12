package com.pilotothegreat.deencompanion.data.nature

import com.pilotothegreat.deencompanion.data.db.NaturalEventDao
import com.pilotothegreat.deencompanion.data.db.NaturalEventEntity
import com.pilotothegreat.deencompanion.data.location.CityIndex
import com.pilotothegreat.deencompanion.data.net.Http
import com.pilotothegreat.deencompanion.data.settings.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import timber.log.Timber

data class Earthquake(
    val id: String,
    val magnitude: Double,
    val distanceKm: Double,
    val place: String,
    val atMillis: Long,
)

/**
 * Significant earthquakes near you, from the USGS public feed.
 *
 * This never raises a notification. Android already ships an earthquake alert system that is faster
 * and better placed than any app, and a second alarm for the same tremor is alarming without being
 * useful. What the app adds is what Android does not: the dua, and a settled place to put it.
 */
class EarthquakeRepository(
    private val dao: NaturalEventDao,
    private val fetch: suspend (String) -> String = { Http.getText(it) },
) {

    companion object {
        const val KIND = "quake"
        const val NEARBY_KM = 800.0
        const val MIN_MAGNITUDE = 4.5
        const val WINDOW_MILLIS = 48 * 60 * 60 * 1000L
        private const val CACHE_MILLIS = 60 * 60 * 1000L

        /** The 4.5+ feed for the last day: small enough to parse on a phone, wide enough to matter. */
        const val FEED = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/4.5_day.geojson"
    }

    @Volatile private var lastFetch = 0L

    /** Nearby quakes still inside the window, strongest first. */
    fun recent(nowMillis: Long): Flow<List<Earthquake>> =
        dao.observeSince(nowMillis - WINDOW_MILLIS).map { rows ->
            rows.filter { it.kind == KIND }
                .map { Earthquake(it.id, it.magnitude ?: 0.0, it.distanceKm ?: 0.0, it.place, it.at) }
                .sortedByDescending { it.magnitude }
        }

    /**
     * Fetched alongside the weather, while the app is open, at most once an hour. Filtering happens
     * here rather than in the query, so the service is never told where you are.
     */
    suspend fun refresh(settings: AppSettings, nowMillis: Long) {
        if (!settings.smart.reactToTheWorld || settings.location.isDefault) return
        if (nowMillis - lastFetch < CACHE_MILLIS) return
        lastFetch = nowMillis
        runCatching {
            val events = parse(fetch(FEED), settings.location.latitude, settings.location.longitude, nowMillis)
            if (events.isNotEmpty()) dao.upsert(events)
            dao.deleteBefore(nowMillis - WINDOW_MILLIS)
        }.onFailure { Timber.d(it, "Earthquake feed unavailable") }
    }

    private fun parse(body: String, latitude: Double, longitude: Double, nowMillis: Long): List<NaturalEventEntity> {
        val features = JSONObject(body).getJSONArray("features")
        return (0 until features.length()).mapNotNull { i ->
            val feature = features.getJSONObject(i)
            val properties = feature.getJSONObject("properties")
            val magnitude = properties.optDouble("mag", 0.0)
            if (magnitude < MIN_MAGNITUDE) return@mapNotNull null
            val coordinates = feature.getJSONObject("geometry").getJSONArray("coordinates")
            val distance = CityIndex.distanceKm(latitude, longitude, coordinates.getDouble(1), coordinates.getDouble(0))
            if (distance > NEARBY_KM) return@mapNotNull null
            NaturalEventEntity(
                id = "$KIND:${feature.getString("id")}",
                kind = KIND,
                at = properties.optLong("time", nowMillis),
                magnitude = magnitude,
                distanceKm = distance,
                place = properties.optString("place").ifBlank { "" },
                fetchedAt = nowMillis,
            )
        }
    }
}
