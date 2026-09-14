package com.pilotothegreat.deencompanion.core.calendar

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate

/**
 * The Islamic day begins at sunset, not at midnight. Every night-anchored occasion depends on it:
 * the odd nights of Ramadan, Laylat al-Qadr, the takbir of Eid, reading al-Kahf from Thursday
 * evening. Anchoring the Hijri date to the civil day instead puts all of them out by one.
 *
 * The flip is a setting because a mushaf date printed on a phone is also read as "today's date",
 * and some people would rather it matched the calendar on the wall.
 */
object HijriClock {

    /**
     * The Hijri date in force at [now]. After [maghrib] the next Islamic day has begun, so the date
     * is taken from tomorrow.
     */
    fun dateAt(now: ZonedDateTime, maghrib: ZonedDateTime?, adjustmentDays: Int, flipAtMaghrib: Boolean): HijrahDate? {
        val civil = if (flipAtMaghrib && maghrib != null && !now.isBefore(maghrib)) {
            now.toLocalDate().plusDays(1)
        } else {
            now.toLocalDate()
        }
        return HijriCalendar.date(civil, adjustmentDays)
    }

    /** True once the Islamic day has turned over, which is what makes a card say "tonight". */
    fun isAfterMaghrib(now: ZonedDateTime, maghrib: ZonedDateTime?): Boolean =
        maghrib != null && !now.isBefore(maghrib)

    /** The civil date the Islamic day beginning at [nightOf]'s sunset belongs to. */
    fun civilDateOfNight(nightOf: LocalDate, afterMaghrib: Boolean): LocalDate =
        if (afterMaghrib) nightOf.plusDays(1) else nightOf
}
