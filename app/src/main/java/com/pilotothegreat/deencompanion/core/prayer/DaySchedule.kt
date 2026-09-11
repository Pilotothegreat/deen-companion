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
)

data class PrayerSchedule(
    val date: LocalDate,
    val adhan: Map<Prayer, ZonedDateTime>,
    val iqama: Map<Prayer, ZonedDateTime>,
)

data class NextPrayer(
    val prayer: Prayer,
    val adhan: ZonedDateTime,
    val iqama: ZonedDateTime?,
)

object DaySchedule {

    fun forDate(date: LocalDate, config: PrayerConfig): PrayerSchedule {
        val times = PrayerTimeCalculator.calculate(
            date, config.latitude, config.longitude, config.zone, config.method, config.asrSchool,
        )
        val adhan = Prayer.entries.associateWith { ZonedDateTime.of(date, times[it], config.zone) }
        val iqama = buildMap {
            for (prayer in Prayer.obligatory) {
                val rule = config.iqama[prayer] ?: continue
                put(prayer, iqamaTime(adhan.getValue(prayer), rule))
            }
        }
        return PrayerSchedule(date, adhan, iqama)
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
