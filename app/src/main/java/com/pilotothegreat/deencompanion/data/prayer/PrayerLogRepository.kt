package com.pilotothegreat.deencompanion.data.prayer

import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.data.db.PrayerLogDao
import com.pilotothegreat.deencompanion.data.db.PrayerLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Which prayers were marked prayed, and for how many days running.
 *
 * Kept honest on purpose: it records only what the reader taps, never what the app assumes. A
 * streak the app awards itself is worth nothing, and worse, it invites the app to nag.
 */
class PrayerLogRepository(private val dao: PrayerLogDao) {

    fun today(date: LocalDate): Flow<Set<Prayer>> = dao.observeDay(date.toString()).map { rows ->
        rows.mapNotNull { Prayer.fromKey(it.prayer) }.toSet()
    }

    suspend fun record(prayer: Prayer, date: LocalDate, atMillis: Long) {
        dao.upsert(
            PrayerLogEntity(
                id = "${date}_${prayer.key}",
                day = date.toString(),
                prayer = prayer.key,
                prayedAt = atMillis,
            ),
        )
    }

    suspend fun undo(prayer: Prayer, date: LocalDate) = dao.delete("${date}_${prayer.key}")

    /** Days in the last [window] on which anything at all was marked; a gentle measure, deliberately. */
    suspend fun daysObserved(today: LocalDate, window: Long = 30): Int =
        dao.daysWithPrayers(today.minusDays(window - 1).toString(), today.toString())
}
