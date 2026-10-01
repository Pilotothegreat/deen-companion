package com.pilotothegreat.deencompanion.core.calendar

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
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

    fun format(date: HijrahDate, locale: Locale): String =
        DateTimeFormatter.ofPattern("d MMMM y", locale).format(date)
}
