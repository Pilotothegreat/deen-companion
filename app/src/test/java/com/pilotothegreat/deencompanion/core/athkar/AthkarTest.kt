package com.pilotothegreat.deencompanion.core.athkar

import com.pilotothegreat.deencompanion.core.prayer.AsrSchool
import com.pilotothegreat.deencompanion.core.prayer.CalculationMethod
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.prayer.PrayerConfig
import com.pilotothegreat.deencompanion.core.text.ArabicText
import com.pilotothegreat.deencompanion.data.athkar.AthkarRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Runs against the bundled asset, so dataset regressions fail the build. */
class AthkarTest {

    private val library = AthkarRepository.parse(File("src/main/assets/athkar.json").readText())
    private val morning = requireNotNull(library.category(AthkarIds.MORNING))
    private val date = LocalDate.of(2026, 9, 11)

    @Test fun coreCategoriesComeFirst() = assertEquals(
        listOf(AthkarIds.MORNING, AthkarIds.EVENING, AthkarIds.AFTER_PRAYER, AthkarIds.WAKING, AthkarIds.SLEEP),
        library.core.map { it.id },
    )

    @Test fun everyItemHasTextAndACount() {
        library.all.forEach { category ->
            assertTrue(category.id, category.items.isNotEmpty())
            assertEquals(category.id, category.items.size, category.items.map { it.id }.toSet().size)
            category.items.forEach { item ->
                assertTrue(item.id, item.arabic.isNotBlank())
                assertTrue(item.id, item.count >= 1)
            }
        }
    }

    @Test fun afterPrayerCountsEachTasbihSeparately() {
        val afterPrayer = requireNotNull(library.category(AthkarIds.AFTER_PRAYER))
        assertEquals(3, afterPrayer.items.count { it.count == 33 })
    }

    @Test fun morningIncludesAyatAlKursi() =
        assertTrue(morning.items.any { "القيوم" in ArabicText.normalize(it.arabic) })

    @Test fun incrementStopsAtTheCount() {
        val item = morning.items.first { it.count == 3 }
        var progress = DayProgress(date)
        repeat(5) { progress = progress.increment(morning.id, item) }
        assertEquals(3, progress.count(morning.id, item.id))
    }

    @Test fun finishingEveryItemCompletesTheCategory() {
        var progress = DayProgress(date)
        morning.items.forEach { item -> repeat(item.count) { progress = progress.increment(morning.id, item) } }
        assertTrue(progress.isComplete(morning))
        assertEquals(1f, progress.fraction(morning), 0.0001f)
        assertEquals(0f, progress.reset(morning.id).fraction(morning), 0.0001f)
    }

    @Test fun progressStartsAfreshTheNextDay() {
        val item = morning.items.first()
        val progress = DayProgress(date).increment(morning.id, item)
        assertEquals(1, progress.on(date).count(morning.id, item.id))
        assertEquals(0, progress.on(date.plusDays(1)).count(morning.id, item.id))
    }

    @Test fun progressSurvivesStorage() {
        val progress = DayProgress(date).increment(morning.id, morning.items.first())
        assertEquals(progress, AthkarRepository.decode(AthkarRepository.encode(progress)))
        assertEquals(LocalDate.MIN, AthkarRepository.decode("not json").date)
    }

    @Test fun streakGrowsDailyAndBreaksAfterAMissedDay() {
        var streak = Streak().completed(date)
        assertEquals(1, streak.days)
        streak = streak.completed(date)
        assertEquals(1, streak.days)
        streak = streak.completed(date.plusDays(1))
        assertEquals(2, streak.current(date.plusDays(2)))
        assertEquals(0, streak.current(date.plusDays(3)))
        assertEquals(1, streak.completed(date.plusDays(5)).days)
    }

    @Test fun suggestionFollowsThePrayerDay() {
        val config = PrayerConfig(23.5880, 58.3829, ZoneId.of("Asia/Muscat"), CalculationMethod.OMAN, AsrSchool.STANDARD)
        val day = DaySchedule.forDate(date, config)
        fun at(time: String) = ZonedDateTime.of(date, LocalTime.parse(time), config.zone)
        assertEquals(AthkarIds.SLEEP, AthkarSchedule.suggest(at("02:00"), day))
        assertEquals(AthkarIds.MORNING, AthkarSchedule.suggest(at("07:30"), day))
        assertEquals(AthkarIds.EVENING, AthkarSchedule.suggest(at("16:40"), day))
        assertEquals(AthkarIds.SLEEP, AthkarSchedule.suggest(at("22:30"), day))
        val afterDhuhr = day.adhan.getValue(Prayer.DHUHR).plusMinutes(20)
        assertEquals(AthkarIds.AFTER_PRAYER, AthkarSchedule.suggest(afterDhuhr, day))
    }
}
