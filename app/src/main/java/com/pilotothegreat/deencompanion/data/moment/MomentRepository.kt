package com.pilotothegreat.deencompanion.data.moment

import com.pilotothegreat.deencompanion.core.athkar.AthkarSchedule
import com.pilotothegreat.deencompanion.core.calendar.HijriClock
import com.pilotothegreat.deencompanion.core.calendar.Occasion
import com.pilotothegreat.deencompanion.core.calendar.Occasions
import com.pilotothegreat.deencompanion.core.moment.Dismissal
import com.pilotothegreat.deencompanion.core.moment.Moment
import com.pilotothegreat.deencompanion.core.moment.MomentEngine
import com.pilotothegreat.deencompanion.core.moment.MomentKind
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.time.Ticker
import com.pilotothegreat.deencompanion.core.travel.Travel
import com.pilotothegreat.deencompanion.core.travel.TravelState
import com.pilotothegreat.deencompanion.data.location.LocationRepository
import com.pilotothegreat.deencompanion.data.nature.EarthquakeRepository
import com.pilotothegreat.deencompanion.data.nature.Eclipse
import com.pilotothegreat.deencompanion.data.nature.EclipseRepository
import com.pilotothegreat.deencompanion.data.settings.AppSettings
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.data.weather.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.ZonedDateTime

/**
 * Assembles what matters now and keeps it ranked.
 *
 * Producers are added here as later phases land — weather, travel, eclipses, earthquakes — and each
 * one becomes visible on every surface at once, because every surface reads this list rather than
 * deciding for itself what to show.
 */
class MomentRepository(
    private val settings: SettingsRepository,
    private val weather: WeatherRepository,
    private val location: LocationRepository,
    private val eclipses: EclipseRepository,
    private val earthquakes: EarthquakeRepository,
) {

    /** Recomputed with the weather, not on the minute: the table is fixed and the feed is hourly. */
    @Volatile private var nextEclipse: Eclipse? = null
    @Volatile private var eclipseVisibleHere = false

    /** Everything live, best first. Recomputed on the minute, since windows open and close on time. */
    val moments: Flow<List<Moment>> = combine(
        settings.settings,
        Ticker.minutes,
        earthquakes.recent(System.currentTimeMillis()),
    ) { s, _, quakes -> s to quakes }
        .map { (s, quakes) ->
            val now = ZonedDateTime.now(s.zone)
            val live = occasions(s, now) +
                listOfNotNull(
                    Producers.calamity(s, now),
                    Producers.weather(weather.current(), now),
                    Producers.eclipse(nextEclipse.takeIf { s.smart.naturalEvents }, eclipseVisibleHere, now),
                    Producers.earthquake(quakes.firstOrNull().takeIf { s.smart.naturalEvents }, now),
                    Producers.driving(location.lastFixSpeed, now),
                ) +
                Producers.travel(s, distanceFromHome(s), now)
            MomentEngine.rank(live, now, dismissedToday(s, now))
        }
        .flowOn(Dispatchers.Default)

    val todayCards: Flow<List<Moment>> = moments.map(MomentEngine::forToday)

    /**
     * Which athkar to offer. The top moment wins when it names one — the rain dua while it rains,
     * the iftar dua at Maghrib in Ramadan — and the time of day is the fallback, as before. This is
     * what finally surfaces the rain, thunder, wind, new-moon, travel, fear and calamity categories
     * that have been sitting unreachable in athkar.json since they were bundled.
     */
    val suggestedAthkar: Flow<String> = combine(settings.settings, moments) { s, live ->
        live.firstNotNullOfOrNull { it.athkarCategory } ?: timeOfDayAthkar(s)
    }.flowOn(Dispatchers.Default)

    /** The same choice, for callers outside composition such as the widgets. */
    suspend fun suggestedAthkarNow(): String = suggestedAthkar.first()

    /**
     * Fetches the weather, at most once an hour and only while the app is in front of the reader.
     * There is no background poll: knowing it rained while the phone was in a pocket is worth
     * nothing, and a wake-up for it would cost battery for nobody.
     */
    suspend fun onAppOpened() {
        val s = settings.current()
        val nowMillis = System.currentTimeMillis()
        weather.refresh(s, nowMillis)
        earthquakes.refresh(s, nowMillis)
        refreshEclipse(s, nowMillis)
        anchorHomeIfNeeded(s)
        updateTravelState(s)
    }

    private suspend fun refreshEclipse(s: AppSettings, nowMillis: Long) {
        if (!s.smart.naturalEvents) {
            nextEclipse = null
            return
        }
        val next = eclipses.upcoming(java.time.Instant.ofEpochMilli(nowMillis)).firstOrNull()
        nextEclipse = next
        eclipseVisibleHere = next != null && !s.location.isDefault &&
            eclipses.isLunarEclipseVisible(next, s.location.latitude, s.location.longitude)
    }

    /** Turns "times of calamity" on for a while, or off when given zero. */
    suspend fun setCalamity(untilMillis: Long) = settings.setCalamityUntil(untilMillis)

    suspend fun setTravelling(travelling: Boolean) {
        settings.setTravelState(if (travelling) TravelState.CONFIRMED else TravelState.HOME)
        if (!travelling) {
            // Coming home while far away would ask again immediately, so home moves to here.
            val s = settings.current()
            if (!s.location.isDefault) settings.setHome(s.location.latitude, s.location.longitude)
        }
    }

    /** The first real location becomes home; there is nothing to measure travel against before that. */
    private suspend fun anchorHomeIfNeeded(s: AppSettings) {
        if (s.smart.hasHome || s.location.isDefault) return
        settings.setHome(s.location.latitude, s.location.longitude)
    }

    private suspend fun updateTravelState(s: AppSettings) {
        if (!s.smart.travel) return
        val distance = distanceFromHome(s) ?: return
        val next = Travel.state(distance, s.smart.safarKm, s.smart.travelState)
        if (next != s.smart.travelState) settings.setTravelState(next)
    }

    private fun distanceFromHome(s: AppSettings): Double? {
        val latitude = s.smart.homeLatitude ?: return null
        val longitude = s.smart.homeLongitude ?: return null
        if (s.location.isDefault) return null
        return Travel.distanceKm(latitude, longitude, s.location.latitude, s.location.longitude)
    }

    suspend fun dismiss(moment: Moment) {
        if (moment.dismissal == Dismissal.NONE) return
        val zone = settings.current().zone
        settings.dismissMoment(moment.id, ZonedDateTime.now(zone).toLocalDate().toEpochDay())
    }

    private fun timeOfDayAthkar(s: AppSettings): String {
        val now = ZonedDateTime.now(s.zone)
        return AthkarSchedule.suggest(now, DaySchedule.forDate(now.toLocalDate(), s.prayerConfig))
    }

    private fun occasions(s: AppSettings, now: ZonedDateTime): List<Moment> {
        val today = DaySchedule.forDate(now.toLocalDate(), s.prayerConfig)
        val tomorrow = DaySchedule.forDate(now.toLocalDate().plusDays(1), s.prayerConfig)
        val hijri = HijriClock.dateAt(
            now = now,
            maghrib = today.adhan[Prayer.MAGHRIB],
            adjustmentDays = s.hijriAdjustment,
            flipAtMaghrib = s.smart.hijriDayStartsAtMaghrib,
        ) ?: return emptyList()
        return Occasions.at(now, hijri, today, tomorrow).map(Occasion::toMoment)
    }

    /**
     * A dismissal stamped with today's date still holds; anything older has lapsed, which is what
     * brings a daily card back tomorrow.
     */
    private fun dismissedToday(s: AppSettings, now: ZonedDateTime): Set<String> {
        val today = now.toLocalDate().toEpochDay()
        return s.dismissedMoments
            .filter { it.substringAfterLast('@').toLongOrNull() == today }
            .map { it.substringBeforeLast('@') }
            .toSet()
    }
}

private fun Occasion.toMoment() = Moment(
    id = id,
    kind = MomentKind.OCCASION,
    priority = priority,
    title = title,
    body = body,
    count = count,
    startsAt = startsAt,
    endsAt = endsAt,
    peak = peak,
    athkarCategory = athkarCategory,
)
