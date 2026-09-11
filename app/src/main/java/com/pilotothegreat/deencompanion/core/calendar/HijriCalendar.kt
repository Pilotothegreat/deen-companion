package com.pilotothegreat.deencompanion.core.calendar

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Umm al-Qura calendar with a user adjustment for local moon sighting. */
object HijriCalendar {
    const val SHABAN = 8
    const val RAMADAN = 9

    /** Null when the date is outside the range covered by the Umm al-Qura tables. */
    fun date(on: LocalDate, adjustmentDays: Int = 0): HijrahDate? =
        runCatching { HijrahDate.from(on.plusDays(adjustmentDays.toLong())) }.getOrNull()

    fun year(date: HijrahDate): Int = date.get(ChronoField.YEAR)
    fun month(date: HijrahDate): Int = date.get(ChronoField.MONTH_OF_YEAR)
    fun day(date: HijrahDate): Int = date.get(ChronoField.DAY_OF_MONTH)

    /** Days until 1 Ramadan while in Sha'ban, otherwise null. */
    fun daysUntilRamadan(on: LocalDate, adjustmentDays: Int = 0): Int? {
        val hijri = date(on, adjustmentDays) ?: return null
        if (month(hijri) != SHABAN) return null
        val firstOfRamadan = hijri.with(ChronoField.DAY_OF_MONTH, 1).plus(1, ChronoUnit.MONTHS)
        return ChronoUnit.DAYS.between(hijri, firstOfRamadan).toInt()
    }

    fun format(date: HijrahDate, locale: Locale): String =
        DateTimeFormatter.ofPattern("d MMMM y", locale).format(date)
}
