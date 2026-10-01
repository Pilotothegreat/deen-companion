package com.pilotothegreat.deencompanion.core.quiet

import com.pilotothegreat.deencompanion.core.prayer.Prayer
import java.time.DayOfWeek
import java.time.ZonedDateTime

/**
 * When the app should be quiet, and when it should hold its own tongue.
 *
 * Both rules here are about the same thing: an app that interrupts worship it was built to support
 * is worse than no app. They are pure functions rather than checks scattered through the scheduler
 * so that the awkward cases — a Friday with no iqama configured, an odd night that starts at sunset
 * rather than midnight — are pinned down by tests instead of discovered by someone at prayer.
 */
object QuietTimes {

    /**
     * A khutbah is not a prayer with a few minutes added to it. It begins at the adhan, runs for the
     * better part of an hour, and the prayer follows it, so silencing from the iqama for the usual
     * ten minutes silences a phone that has already rung through the whole sermon.
     */
    const val KHUTBAH_MINUTES = 45

    /** The window during which the phone should be quiet, or null when the reader has not asked for one. */
    fun silenceWindow(
        prayer: Prayer,
        adhan: ZonedDateTime,
        iqama: ZonedDateTime?,
        configuredMinutes: Int,
    ): ClosedRange<ZonedDateTime>? {
        if (configuredMinutes <= 0) return null
        if (isJumuah(prayer, adhan)) {
            // From the adhan, because that is when the khatib stands up.
            return adhan..adhan.plusMinutes(maxOf(configuredMinutes, KHUTBAH_MINUTES).toLong())
        }
        val from = iqama ?: adhan
        return from..from.plusMinutes(configuredMinutes.toLong())
    }

    /** Friday's Dhuhr is the Jumu'ah prayer; every other Dhuhr is an ordinary one. */
    fun isJumuah(prayer: Prayer, at: ZonedDateTime): Boolean =
        prayer == Prayer.DHUHR && at.dayOfWeek == DayOfWeek.FRIDAY

    /**
     * The odd nights of the last ten of Ramadan, when Bilal holds its gentler nudges until morning.
     *
     * Prayer alerts and the athkar reminders still arrive — those are the point of the app — but a
     * reading-plan nudge at eleven at night on the twenty-seventh can wait until the morning, and it
     * is the sort of thing an app should notice without being asked.
     *
     * [hijriDay] is taken from the app's own Hijri clock, which already turns the day over at
     * Maghrib, so "the night of the 27th" is the 27th here rather than the evening of the 26th.
     */
    fun holdsGentleNotices(hijriMonth: Int, hijriDay: Int): Boolean =
        hijriMonth == RAMADAN && hijriDay >= FIRST_OF_LAST_TEN && hijriDay % 2 == 1

    private const val RAMADAN = 9
    private const val FIRST_OF_LAST_TEN = 21
}
