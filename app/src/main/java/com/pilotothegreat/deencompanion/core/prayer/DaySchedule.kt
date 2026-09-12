package com.pilotothegreat.deencompanion.core.prayer

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class Prayer {
    FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA;

    val isObligatory: Boolean get() = this != SUNRISE

    /** Stable key persisted in settings (muted prayers) and alarm extras. */
    val key: String get() = name.lowercase().replaceFirstChar(Char::uppercase)

    companion object {
        val obligatory: List<Prayer> = entries.filter { it.isObligatory }

        fun fromKey(key: String?): Prayer? = entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
    }
}

sealed interface IqamaRule {
    data class Offset(val minutes: Int) : IqamaRule
    data class Fixed(val time: LocalTime) : IqamaRule
}

/** Everything needed to compute a day's prayer and iqama times. */
data class PrayerConfig(
    val latitude: Double,
    val longitude: Double,
    val zone: ZoneId,
    val method: CalculationMethod,
    val asrSchool: AsrSchool,
    val iqama: Map<Prayer, IqamaRule> = emptyMap(),
    val highLatitude: HighLatitudeMode = HighLatitudeMode.AUTO,
    /** Minutes added to each computed time to match the local mosque. */
    val adjustments: Map<Prayer, Int> = emptyMap(),
)

data class PrayerSchedule(
    val date: LocalDate,
    val adhan: Map<Prayer, ZonedDateTime>,
    val iqama: Map<Prayer, ZonedDateTime>,
    val middleOfNight: ZonedDateTime,
    val lastThirdOfNight: ZonedDateTime,
)

data class NextPrayer(
    val prayer: Prayer,
    val adhan: ZonedDateTime,
    val iqama: ZonedDateTime?,
)

object DaySchedule {

    /**
     * The last few days computed, so the same day is not solved over and over.
     *
     * The astronomy is deterministic — the same date and the same configuration always give the same
     * answer — so holding it costs nothing in correctness. It buys two things: the home screen and
     * the widgets recompute today and tomorrow every minute between them, and the first draw after
     * midnight is instant because tomorrow was already worked out yesterday.
     */
    private val memo = java.util.concurrent.ConcurrentHashMap<Pair<LocalDate, PrayerConfig>, PrayerSchedule>()

    /** Four is today, tomorrow, yesterday and one spare; beyond that the cache is simply dropped. */
    private const val MEMO_LIMIT = 8

    fun forDate(date: LocalDate, config: PrayerConfig): PrayerSchedule {
        val key = date to config
        memo[key]?.let { return it }
        return compute(date, config).also {
            if (memo.size >= MEMO_LIMIT) memo.clear()
            memo[key] = it
        }
    }

    /** Works tomorrow out now, while nobody is waiting for it. */
    fun warm(date: LocalDate, config: PrayerConfig) {
        forDate(date, config)
        forDate(date.plusDays(1), config)
    }

    /** Only for tests, which must not inherit another test's configuration. */
    internal fun forget() = memo.clear()

    private fun compute(date: LocalDate, config: PrayerConfig): PrayerSchedule {
        val times = PrayerEngine.calculate(
            date, config.latitude, config.longitude, config.zone, config.method, config.asrSchool,
            config.highLatitude, config.adjustments,
        )
        val adhan = times.adhan
        val iqama = buildMap {
            for (prayer in Prayer.obligatory) {
                val rule = config.iqama[prayer] ?: continue
                put(prayer, iqamaTime(adhan.getValue(prayer), rule))
            }
        }
        return PrayerSchedule(date, adhan, iqama, times.middleOfNight, times.lastThirdOfNight)
    }

    /** A fixed iqama earlier than the adhan (e.g. long summer days) is moved to the adhan. */
    fun iqamaTime(adhan: ZonedDateTime, rule: IqamaRule): ZonedDateTime = when (rule) {
        is IqamaRule.Offset -> adhan.plusMinutes(rule.minutes.toLong())
        is IqamaRule.Fixed -> {
            val fixed = adhan.with(rule.time)
            if (fixed.isBefore(adhan)) adhan else fixed
        }
    }

    /** The next obligatory prayer after [now]; after Isha this is tomorrow's Fajr. */
    fun next(now: ZonedDateTime, config: PrayerConfig): NextPrayer {
        val local = now.withZoneSameInstant(config.zone)
        val today = forDate(local.toLocalDate(), config)
        Prayer.obligatory.firstOrNull { today.adhan.getValue(it).isAfter(local) }?.let {
            return NextPrayer(it, today.adhan.getValue(it), today.iqama[it])
        }
        val tomorrow = forDate(local.toLocalDate().plusDays(1), config)
        return NextPrayer(Prayer.FAJR, tomorrow.adhan.getValue(Prayer.FAJR), tomorrow.iqama[Prayer.FAJR])
    }
}
