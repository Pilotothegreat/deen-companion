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
import com.pilotothegreat.deencompanion.data.settings.AppSettings
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
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
class MomentRepository(private val settings: SettingsRepository) {

    /** Everything live, best first. Recomputed on the minute, since windows open and close on time. */
    val moments: Flow<List<Moment>> = combine(settings.settings, Ticker.minutes) { s, _ -> s }
        .map { s ->
            val now = ZonedDateTime.now(s.zone)
            MomentEngine.rank(occasions(s, now), now, dismissedToday(s, now))
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
