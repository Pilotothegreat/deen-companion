package com.pilotothegreat.deencompanion.core.quran

import java.time.LocalDate
import kotlin.math.ceil

/** A commitment to finish the mushaf by a date, and how it is going. */
data class Khatma(
    val startedOn: LocalDate,
    val targetDays: Int,
    /** The page the plan began from, so a khatma started mid-mushaf still adds up. */
    val startPage: Int,
    /** The furthest page reached. */
    val lastPage: Int,
) {
    val endsOn: LocalDate get() = startedOn.plusDays((targetDays - 1).toLong())
}

/**
 * A khatma is a page quota, not a streak. It is deliberately quiet when you are ahead: a reminder
 * that fires on a day you have already covered is noise, and noise is what makes people turn
 * reminders off.
 */
data class KhatmaProgress(
    val pagesRead: Int,
    val pagesTotal: Int,
    val pagesPerDay: Int,
    /** Where the plan expects you to be today. */
    val targetPage: Int,
    val currentPage: Int,
    val daysLeft: Int,
    val isComplete: Boolean,
) {
    val pagesBehind: Int get() = (targetPage - currentPage).coerceAtLeast(0)
    val isOnTrack: Boolean get() = pagesBehind == 0
    /** 0f..1f, for the progress ring. */
    val fraction: Float get() = if (pagesTotal <= 0) 0f else (pagesRead.toFloat() / pagesTotal).coerceIn(0f, 1f)
    /** Pages still to read today to catch up, including today's own share. */
    val pagesDueToday: Int get() = if (isComplete) 0 else (targetPage - currentPage + pagesPerDay).coerceIn(0, pagesTotal)
}

object KhatmaPlan {

    const val PAGE_COUNT = 604

    fun progress(plan: Khatma, today: LocalDate, totalPages: Int = PAGE_COUNT): KhatmaProgress {
        val pagesTotal = (totalPages - plan.startPage + 1).coerceAtLeast(1)
        val pagesRead = (plan.lastPage - plan.startPage + 1).coerceIn(0, pagesTotal)
        val days = plan.targetDays.coerceAtLeast(1)
        val perDay = ceil(pagesTotal.toDouble() / days).toInt().coerceAtLeast(1)
        // Day 1 is the start date, so a plan read on its first day is already due its first share.
        val dayNumber = (today.toEpochDay() - plan.startedOn.toEpochDay() + 1).coerceIn(1, days.toLong()).toInt()
        val targetPage = (plan.startPage + dayNumber * perDay - 1).coerceAtMost(totalPages)
        return KhatmaProgress(
            pagesRead = pagesRead,
            pagesTotal = pagesTotal,
            pagesPerDay = perDay,
            targetPage = targetPage,
            currentPage = plan.lastPage.coerceAtLeast(plan.startPage - 1),
            daysLeft = (days - dayNumber).coerceAtLeast(0),
            isComplete = plan.lastPage >= totalPages,
        )
    }

    /** True when a reminder is worth sending: the plan is live and today's pages are not done. */
    fun needsReminder(plan: Khatma, today: LocalDate, totalPages: Int = PAGE_COUNT): Boolean {
        if (today.isBefore(plan.startedOn)) return false
        val progress = progress(plan, today, totalPages)
        return !progress.isComplete && !progress.isOnTrack
    }
}
