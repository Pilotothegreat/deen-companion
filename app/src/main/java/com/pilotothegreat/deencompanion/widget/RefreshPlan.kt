package com.pilotothegreat.deencompanion.widget

import java.time.Duration
import java.time.ZonedDateTime

/**
 * When the widgets are drawn again.
 *
 * A widget is a picture: its progress bar moves only when it is redrawn. Redrawing at each prayer alone
 * left the bar frozen for hours between Dhuhr and Asr, so in between the next-prayer widget is redrawn in
 * small steps — a twenty-fourth of the gap, never more often than every five minutes nor less often than
 * every twenty — and everything is redrawn at the prayer itself and at midnight. The alarm does not wake
 * the phone, so the steps only happen while someone could be looking.
 */
internal object RefreshPlan {

    /** The next redraw: at [at], of every widget or only of the next-prayer widget's moving parts. */
    data class Refresh(val at: ZonedDateTime, val everything: Boolean)

    fun next(now: ZonedDateTime, previousPrayer: ZonedDateTime?, nextPrayer: ZonedDateTime, midnight: ZonedDateTime): Refresh {
        val boundary = if (nextPrayer.isBefore(midnight)) nextPrayer else midnight
        val gap = Duration.between(previousPrayer ?: now, nextPrayer)
        val step = gap.dividedBy(STEPS).coerceIn(SHORTEST, LONGEST)
        val tick = now.plus(step)
        // A second past the boundary, so the redraw sees the new prayer rather than the last instant of the old.
        return if (tick.isBefore(boundary)) Refresh(tick, everything = false) else Refresh(boundary.plusSeconds(1), everything = true)
    }

    private const val STEPS = 24L
    private val SHORTEST: Duration = Duration.ofMinutes(5)
    private val LONGEST: Duration = Duration.ofMinutes(20)
}
