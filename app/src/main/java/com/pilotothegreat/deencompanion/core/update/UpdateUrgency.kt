package com.pilotothegreat.deencompanion.core.update

/**
 * How hard the app should push an update that is waiting.
 *
 * An update nobody installs is a bug nobody's fix reaches, and until 2.1 the whole of the nudging was
 * one snackbar on the day someone happened to open the app. It escalates with age rather than shouting
 * from the first minute: a release published an hour ago can wait for the next launch, one that has
 * been out a fortnight is why people are still reporting a fixed crash.
 */
enum class UpdateUrgency {
    /** Nothing waiting, or it has just been offered. The version row in Settings still shows it. */
    QUIET,

    /** A line on Today, once a day. */
    NUDGE,

    /** The sheet, on every launch, until it is installed or the version changes. */
    INSIST,
    ;

    val promptsOnLaunch: Boolean get() = this == INSIST
}

object UpdatePlan {

    /** After this long with an update available, the sheet opens itself. */
    const val INSIST_AFTER_DAYS = 14L

    /** A nudge is not repeated inside this window, however many times the app is opened. */
    const val NUDGE_EVERY_HOURS = 24L

    /** Play's own priority, 0..5; from this value up, an update is treated as important. */
    const val HIGH_PRIORITY = 4

    private const val HOUR = 60 * 60 * 1000L
    private const val DAY = 24 * HOUR

    /**
     * @param availableSince when this version was first seen to be available, 0 if nothing is waiting
     * @param lastPromptedAt when the reader was last shown something about it
     * @param priority Play's update priority, or 0 for a GitHub release
     * @param stalenessDays how long Play says the installed version has been out of date, or 0
     */
    fun urgency(
        now: Long,
        availableSince: Long,
        lastPromptedAt: Long,
        priority: Int = 0,
        stalenessDays: Int = 0,
    ): UpdateUrgency {
        if (availableSince <= 0L) return UpdateUrgency.QUIET
        val waited = now - availableSince
        val important = priority >= HIGH_PRIORITY || stalenessDays >= INSIST_AFTER_DAYS
        if (important || waited >= INSIST_AFTER_DAYS * DAY) return UpdateUrgency.INSIST
        // Shown once, then held back for the day: the second snackbar in an hour teaches people to
        // dismiss without reading, which costs the release that actually matters.
        return if (now - lastPromptedAt >= NUDGE_EVERY_HOURS * HOUR) UpdateUrgency.NUDGE else UpdateUrgency.QUIET
    }

    /** True when Play's own full-screen flow is the right one: an important update, not a routine one. */
    fun useImmediateFlow(priority: Int, stalenessDays: Int): Boolean =
        priority >= HIGH_PRIORITY || stalenessDays >= INSIST_AFTER_DAYS
}
