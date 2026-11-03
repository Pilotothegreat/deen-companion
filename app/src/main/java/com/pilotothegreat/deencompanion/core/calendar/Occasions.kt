package com.pilotothegreat.deencompanion.core.calendar

import androidx.annotation.StringRes
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.prayer.PrayerSchedule
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

/**
 * Something in the Islamic year worth saying today, with the window it is worth saying it in.
 *
 * Occasions are deliberately conservative. Dates that scholars disagree about are left out, and
 * every card that depends on the moon hedges: the app follows Umm al-Qura, which is a calculation,
 * and a local sighting can differ by a day.
 */
data class Occasion(
    val id: String,
    /** Higher wins when several apply at once. */
    val priority: Int,
    @StringRes val title: Int,
    @StringRes val body: Int?,
    /** Formatted into the body by the UI when present, in the reader's own digits. */
    val count: Int? = null,
    val athkarCategory: String? = null,
    val startsAt: ZonedDateTime,
    val endsAt: ZonedDateTime,
    /** The instant the occasion is really about, used to rank imminent things first. */
    val peak: ZonedDateTime? = null,
)

object Occasions {

    const val MUHARRAM = 1
    const val RAJAB = 7
    const val SHABAN = 8
    const val RAMADAN = 9
    const val SHAWWAL = 10
    const val DHUL_HIJJAH = 12

    /**
     * Everything in force at [now]. [hijri] must already be the Maghrib-anchored date, so an
     * occasion that begins at sunset is reported on the evening it begins, not the next morning.
     */
    fun at(now: ZonedDateTime, hijri: HijrahDate, today: PrayerSchedule, tomorrow: PrayerSchedule): List<Occasion> {
        val month = hijri.get(ChronoField.MONTH_OF_YEAR)
        val day = hijri.get(ChronoField.DAY_OF_MONTH)
        val fajr = today.adhan.getValue(Prayer.FAJR)
        val maghrib = today.adhan.getValue(Prayer.MAGHRIB)
        val asr = today.adhan.getValue(Prayer.ASR)
        val dayStart = now.toLocalDate().atStartOfDay(now.zone)
        val dayEnd = dayStart.plusDays(1)

        val out = mutableListOf<Occasion>()

        // --- Jumu'ah -------------------------------------------------------------------------
        // Al-Kahf is read from Thursday's sunset; the hour of response is the last hour before Maghrib.
        if (now.dayOfWeek == DayOfWeek.THURSDAY && !now.isBefore(maghrib)) {
            out += Occasion(
                id = "kahf",
                priority = 60,
                title = R.string.occasion_kahf,
                body = R.string.occasion_kahf_body,
                startsAt = maghrib,
                endsAt = dayEnd,
            )
        }
        if (now.dayOfWeek == DayOfWeek.FRIDAY) {
            if (now.isBefore(maghrib)) {
                out += Occasion(
                    id = "kahf",
                    priority = 60,
                    title = R.string.occasion_kahf,
                    body = R.string.occasion_kahf_body,
                    startsAt = dayStart,
                    endsAt = maghrib,
                )
            }
            out += Occasion(
                id = "jumuah-salawat",
                priority = 55,
                title = R.string.occasion_jumuah,
                body = R.string.occasion_jumuah_body,
                startsAt = dayStart,
                endsAt = maghrib,
            )
            out += Occasion(
                id = "hour-of-response",
                priority = 70,
                title = R.string.occasion_hour_of_response,
                body = R.string.occasion_hour_of_response_body,
                startsAt = maxOf(asr, maghrib.minusHours(1)),
                endsAt = maghrib,
                peak = maghrib,
            )
        }

        // --- Ramadan -------------------------------------------------------------------------
        if (month == SHABAN) {
            val until = daysUntil(hijri, RAMADAN)
            if (until in 1..30) {
                out += Occasion(
                    id = "ramadan-countdown",
                    priority = 40,
                    title = R.string.occasion_ramadan_soon,
                    body = R.string.occasion_ramadan_soon_body,
                    count = until,
                    startsAt = dayStart,
                    endsAt = dayEnd,
                )
            }
        }
        if (month == RAMADAN) {
            // Suhoor is the last two hours before whichever Fajr is next, whether that is this
            // morning's (in the small hours) or tomorrow's (after sunset).
            val nextFajr = if (now.isBefore(fajr)) fajr else tomorrow.adhan.getValue(Prayer.FAJR)
            out += Occasion(
                id = "suhoor",
                priority = 85,
                title = R.string.occasion_suhoor,
                body = R.string.occasion_suhoor_body,
                startsAt = nextFajr.minusHours(2),
                endsAt = nextFajr,
                peak = nextFajr,
            )
            out += Occasion(
                id = "iftar",
                priority = 90,
                title = R.string.occasion_iftar,
                body = R.string.occasion_iftar_body,
                athkarCategory = "iftar",
                startsAt = maghrib.minusHours(1),
                endsAt = maghrib.plusMinutes(30),
                peak = maghrib,
            )
            if (day >= 21) {
                out += Occasion(
                    id = "last-ten",
                    priority = 75,
                    title = R.string.occasion_last_ten,
                    body = R.string.occasion_last_ten_body,
                    count = day,
                    startsAt = dayStart,
                    endsAt = dayEnd,
                )
                if (day % 2 == 1) {
                    out += Occasion(
                        id = "odd-night",
                        priority = 95,
                        title = R.string.occasion_odd_night,
                        body = R.string.occasion_odd_night_body,
                        count = day,
                        startsAt = maghrib.minusHours(12).coerceAtLeast(dayStart),
                        endsAt = tomorrow.adhan.getValue(Prayer.FAJR),
                        peak = tomorrow.adhan.getValue(Prayer.FAJR).minusHours(2),
                    )
                }
            }
            if (day >= 28) {
                out += Occasion(
                    id = "zakat-al-fitr",
                    priority = 80,
                    title = R.string.occasion_zakat_al_fitr,
                    body = R.string.occasion_zakat_al_fitr_body,
                    startsAt = dayStart,
                    endsAt = dayEnd,
                )
            }
        }

        // --- The two Eids --------------------------------------------------------------------
        if (month == SHAWWAL && day == 1) {
            out += Occasion(
                id = "eid-al-fitr",
                priority = 100,
                title = R.string.occasion_eid_al_fitr,
                body = R.string.occasion_eid_body,
                startsAt = dayStart,
                endsAt = dayEnd,
            )
        }
        if (month == SHAWWAL && day in 2..29) {
            out += Occasion(
                id = "six-of-shawwal",
                priority = 30,
                title = R.string.occasion_six_of_shawwal,
                body = R.string.occasion_six_of_shawwal_body,
                startsAt = dayStart,
                endsAt = dayEnd,
            )
        }
        if (month == DHUL_HIJJAH && day in 1..10) {
            out += Occasion(
                id = "ten-of-dhul-hijjah",
                priority = 70,
                title = R.string.occasion_ten_days,
                body = R.string.occasion_ten_days_body,
                count = day,
                startsAt = dayStart,
                endsAt = dayEnd,
            )
        }
        if (month == DHUL_HIJJAH && day == 9) {
            out += Occasion(
                id = "arafah",
                priority = 100,
                title = R.string.occasion_arafah,
                body = R.string.occasion_arafah_body,
                startsAt = dayStart,
                endsAt = maghrib,
                peak = asr,
            )
        }
        if (month == DHUL_HIJJAH && day in 10..13) {
            out += Occasion(
                id = "eid-al-adha",
                priority = 100,
                title = R.string.occasion_eid_al_adha,
                body = R.string.occasion_eid_body,
                startsAt = dayStart,
                endsAt = dayEnd,
            )
        }
        // The takbir of tashriq runs from Fajr of Arafah to Asr of the last day of tashriq.
        if (month == DHUL_HIJJAH && day in 9..13) {
            out += Occasion(
                id = "takbir",
                priority = 65,
                title = R.string.occasion_takbir,
                body = R.string.occasion_takbir_body,
                startsAt = if (day == 9) fajr else dayStart,
                endsAt = if (day == 13) asr else dayEnd,
            )
        }

        // --- Muharram ------------------------------------------------------------------------
        if (month == MUHARRAM && day in 9..10) {
            out += Occasion(
                id = "ashura",
                priority = 80,
                title = if (day == 9) R.string.occasion_tasua else R.string.occasion_ashura,
                body = R.string.occasion_ashura_body,
                startsAt = dayStart,
                endsAt = dayEnd,
            )
        }

        // --- Every month ---------------------------------------------------------------------
        if (day in 1..2) {
            out += Occasion(
                id = "new-moon",
                priority = 25,
                title = R.string.occasion_new_moon,
                body = R.string.occasion_new_moon_body,
                athkarCategory = "new-moon",
                startsAt = dayStart,
                endsAt = dayEnd,
            )
        }
        if (day in 13..15 && month != RAMADAN) {
            out += Occasion(
                id = "white-days",
                priority = 35,
                title = R.string.occasion_white_days,
                body = R.string.occasion_white_days_body,
                count = day,
                startsAt = dayStart,
                endsAt = dayEnd,
            )
        }
        if (now.dayOfWeek in setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY) && month != RAMADAN && !isEid(month, day)) {
            out += Occasion(
                id = "sunnah-fast",
                priority = 20,
                title = R.string.occasion_sunnah_fast,
                body = R.string.occasion_sunnah_fast_body,
                startsAt = dayStart,
                endsAt = maghrib,
                peak = maghrib,
            )
        }

        return out.filter { !now.isBefore(it.startsAt) && now.isBefore(it.endsAt) }
            .distinctBy { it.id }
            .sortedByDescending { it.priority }
    }

    /** The athkar category the top occasion suggests, or null to fall back to the time of day. */
    fun athkarFor(occasions: List<Occasion>): String? = occasions.firstNotNullOfOrNull { it.athkarCategory }

    private fun isEid(month: Int, day: Int) =
        (month == SHAWWAL && day == 1) || (month == DHUL_HIJJAH && day in 10..13)

    private fun daysUntil(hijri: HijrahDate, month: Int): Int {
        val firstOfMonth = hijri.with(ChronoField.DAY_OF_MONTH, 1).let {
            if (hijri.get(ChronoField.MONTH_OF_YEAR) < month) it.plus((month - hijri.get(ChronoField.MONTH_OF_YEAR)).toLong(), ChronoUnit.MONTHS) else it
        }
        return ChronoUnit.DAYS.between(hijri, firstOfMonth).toInt()
    }
}

private fun ZonedDateTime.coerceAtLeast(other: ZonedDateTime) = if (isBefore(other)) other else this
