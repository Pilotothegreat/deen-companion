package com.pilotothegreat.deencompanion.core.prayer

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * The day's times are now held rather than recomputed, which is only safe because the astronomy is
 * deterministic. These check that holding it cannot hand back the wrong day or the wrong city — the
 * two ways a cache like this fails, and both of them silently.
 */
class ScheduleMemoTest {

    private val riyadh = PrayerConfig(
        latitude = 24.71,
        longitude = 46.68,
        zone = ZoneId.of("Asia/Riyadh"),
        method = CalculationMethod.MAKKAH,
        asrSchool = AsrSchool.STANDARD,
    )
    private val date = LocalDate.of(2026, 9, 12)

    @Before fun clear() = DaySchedule.forget()

    @After fun clearAfter() = DaySchedule.forget()

    @Test fun theSameDayAndSettingsGiveBackTheSameAnswer() {
        val first = DaySchedule.forDate(date, riyadh)
        assertSame("the second call should not recompute", first, DaySchedule.forDate(date, riyadh))
    }

    @Test fun anotherDayIsAnotherAnswer() {
        val today = DaySchedule.forDate(date, riyadh)
        val tomorrow = DaySchedule.forDate(date.plusDays(1), riyadh)
        assertNotEquals(today.adhan.getValue(Prayer.FAJR), tomorrow.adhan.getValue(Prayer.FAJR))
        assertEquals(date.plusDays(1), tomorrow.date)
    }

    @Test fun movingCityIsNotServedFromYesterdaysCity() {
        val here = DaySchedule.forDate(date, riyadh)
        val elsewhere = DaySchedule.forDate(date, riyadh.copy(latitude = 51.51, longitude = -0.13, zone = ZoneId.of("Europe/London")))
        assertNotEquals(here.adhan.getValue(Prayer.MAGHRIB), elsewhere.adhan.getValue(Prayer.MAGHRIB))
    }

    @Test fun changingTheIqamaRuleChangesTheAnswer() {
        val without = DaySchedule.forDate(date, riyadh)
        val with = DaySchedule.forDate(date, riyadh.copy(iqama = mapOf(Prayer.FAJR to IqamaRule.Offset(20))))
        assertEquals(emptyMap<Prayer, Any>(), without.iqama)
        assertEquals(1, with.iqama.size)
    }

    @Test fun warmingLeavesTomorrowReadyBeforeAnyoneAsks() {
        DaySchedule.warm(date, riyadh)
        val tomorrow = DaySchedule.forDate(date.plusDays(1), riyadh)
        assertSame(tomorrow, DaySchedule.forDate(date.plusDays(1), riyadh))
    }
}
