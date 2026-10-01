package com.pilotothegreat.deencompanion.data.quran

import com.pilotothegreat.deencompanion.core.quran.Khatma
import com.pilotothegreat.deencompanion.core.quran.KhatmaPlan
import com.pilotothegreat.deencompanion.core.quran.KhatmaProgress
import com.pilotothegreat.deencompanion.data.db.ReadingPlanDao
import com.pilotothegreat.deencompanion.data.db.ReadingPlanEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * The khatma: one plan at a time, its progress taken from the page actually reached rather than
 * from a button someone has to remember to press.
 */
class KhatmaRepository(private val dao: ReadingPlanDao) {

    val plan: Flow<Khatma?> = dao.observe().map { it?.toKhatma() }

    fun progress(today: LocalDate): Flow<KhatmaProgress?> =
        plan.map { it?.let { plan -> KhatmaPlan.progress(plan, today) } }

    suspend fun start(targetDays: Int, fromPage: Int, today: LocalDate) {
        val start = fromPage.coerceIn(1, KhatmaPlan.PAGE_COUNT)
        dao.upsert(
            ReadingPlanEntity(
                startedOn = today.toString(),
                targetDays = targetDays.coerceAtLeast(1),
                startPage = start,
                // Nothing read yet: the plan sits one page before where it begins.
                lastPage = start - 1,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    /** Called as the reader turns pages. A plan only ever moves forward. */
    suspend fun onPageRead(page: Int) {
        val current = dao.observe().first() ?: return
        if (page <= current.lastPage) return
        dao.upsert(current.copy(lastPage = page.coerceAtMost(KhatmaPlan.PAGE_COUNT), updatedAt = System.currentTimeMillis()))
    }

    suspend fun cancel() = dao.clear()

    private fun ReadingPlanEntity.toKhatma() = Khatma(
        startedOn = LocalDate.parse(startedOn),
        targetDays = targetDays,
        startPage = startPage,
        lastPage = lastPage,
    )
}
