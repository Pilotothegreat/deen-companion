package com.pilotothegreat.deencompanion.core.moment

import androidx.annotation.StringRes
import java.time.Duration
import java.time.ZonedDateTime

/** What a moment is about, which is also how surfaces decide whether they want it. */
enum class MomentKind {
    /** A day or night of the Islamic year. */
    OCCASION,

    /** Weather, an eclipse, an earthquake: the world outside the phone. */
    NATURE,

    /** Travelling, and what changes while you are. */
    TRAVEL,

    /** A reading plan or a streak. */
    PLAN,

    /** Something is wrong with the app's setup and needs the reader's hand. */
    MAINTENANCE,
}

/** How a card goes away once the reader has dealt with it. */
enum class Dismissal {
    /** It cannot be dismissed; it leaves when its window closes. */
    NONE,

    /** Gone for the rest of the day. */
    TODAY,

    /** Gone until the thing itself changes (a new Ramadan, a new earthquake). */
    PERMANENT,
}

/**
 * One thing worth saying, with the window it is worth saying it in.
 *
 * Every surface — Today, the athkar tab, the widgets, the notifications — reads the same list, so a
 * feature appears and disappears everywhere at once, and nothing has to be wired up screen by
 * screen. That is what keeps the interface from silting up.
 */
data class Moment(
    val id: String,
    val kind: MomentKind,
    /** Higher wins. Roughly: 100 Eid, 90 iftar, 70 Arafah and Jumu'ah, 40 weather, 10 hints. */
    val priority: Int,
    @StringRes val title: Int,
    @StringRes val body: Int?,
    /** Formatted into the body by the UI, in the reader's own digits. */
    val count: Int? = null,
    val startsAt: ZonedDateTime,
    val endsAt: ZonedDateTime,
    /** The instant it is really about; imminence breaks ties. */
    val peak: ZonedDateTime? = null,
    /** Opens this athkar category when the card is tapped. */
    val athkarCategory: String? = null,
    val deepLink: String? = null,
    val dismissal: Dismissal = Dismissal.TODAY,
) {
    fun isLiveAt(now: ZonedDateTime): Boolean = !now.isBefore(startsAt) && now.isBefore(endsAt)
}

/**
 * Ranks moments and caps what each surface shows.
 *
 * Before this, Today could stack three permission cards above the prayer times and everything else
 * was hard-coded to be always visible. The cap is the point: at most a handful of cards, and only
 * ever one asking the reader to go and fix something.
 */
object MomentEngine {

    const val TODAY_CARDS = 3
    const val WIDGET_CARDS = 1

    /**
     * @param dismissed ids the reader has sent away and that are still dismissed.
     */
    fun rank(moments: List<Moment>, now: ZonedDateTime, dismissed: Set<String> = emptySet()): List<Moment> =
        moments.asSequence()
            .filter { it.isLiveAt(now) }
            .filter { it.id !in dismissed }
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<Moment> { it.priority }
                    .thenBy { imminence(it, now) }
                    .thenBy { Duration.between(it.startsAt, it.endsAt) }
                    .thenBy { it.id },
            )
            .toList()

    /**
     * What Today shows: the best few, and never more than one maintenance prompt, because a column
     * of things to go and fix is what makes people stop reading the screen at all.
     */
    fun forToday(ranked: List<Moment>, limit: Int = TODAY_CARDS): List<Moment> {
        var maintenance = 0
        return ranked.filter { moment ->
            if (moment.kind != MomentKind.MAINTENANCE) true else maintenance++ == 0
        }.take(limit)
    }

    /** The one the widget shows, or null so it can hide itself rather than print a placeholder. */
    fun forWidget(ranked: List<Moment>): Moment? =
        ranked.firstOrNull { it.kind != MomentKind.MAINTENANCE }

    /** How soon the moment's peak is; a moment with no peak sorts after those that have one. */
    private fun imminence(moment: Moment, now: ZonedDateTime): Long {
        val peak = moment.peak ?: return Long.MAX_VALUE
        return Duration.between(now, peak).abs().toMinutes()
    }
}
