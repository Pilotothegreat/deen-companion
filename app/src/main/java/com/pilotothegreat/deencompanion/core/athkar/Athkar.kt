package com.pilotothegreat.deencompanion.core.athkar

import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.prayer.PrayerSchedule
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import kotlin.math.min

data class AthkarItem(
    val id: String,
    val arabic: String,
    val translation: String,
    val transliteration: String,
    /** How many times to say it. */
    val count: Int,
    val virtueEnglish: String = "",
    val virtueArabic: String = "",
    val sourceEnglish: String = "",
    val sourceArabic: String = "",
    /** When or how to say it, e.g. "After Fajr and Maghrib". */
    val noteEnglish: String = "",
    val noteArabic: String = "",
) {
    fun virtue(locale: Locale): String? = pick(locale, virtueEnglish, virtueArabic)
    fun source(locale: Locale): String? = pick(locale, sourceEnglish, sourceArabic)
    fun note(locale: Locale): String? = pick(locale, noteEnglish, noteArabic)
}

/** Text in the reader's language, falling back to the other one; null when neither exists. */
private fun pick(locale: Locale, english: String, arabic: String): String? =
    (if (locale.language == "ar") arabic.ifBlank { english } else english.ifBlank { arabic }).ifBlank { null }

data class AthkarCategory(
    val id: String,
    val titleEnglish: String,
    val titleArabic: String,
    val items: List<AthkarItem>,
) {
    fun title(locale: Locale): String = if (locale.language == "ar") titleArabic else titleEnglish

    /** Every repetition in the category, e.g. 33 + 33 + 34 + … */
    val totalCount: Int get() = items.sumOf { it.count }
}

data class AthkarGroup(
    val id: String,
    val titleEnglish: String,
    val titleArabic: String,
    val categories: List<AthkarCategory>,
) {
    fun title(locale: Locale): String = if (locale.language == "ar") titleArabic else titleEnglish
}

/** The bundled athkar, plus the lists the reader wrote for themselves in [custom]. */
data class AthkarLibrary(
    val core: List<AthkarCategory>,
    val groups: List<AthkarGroup>,
    val custom: List<AthkarCategory> = emptyList(),
) {
    val all: List<AthkarCategory> = core + groups.flatMap { it.categories } + custom

    fun category(id: String): AthkarCategory? = all.firstOrNull { it.id == id }
}

object AthkarIds {
    const val MORNING = "morning"
    const val EVENING = "evening"
    const val AFTER_PRAYER = "after-prayer"
    const val WAKING = "waking"
    const val SLEEP = "sleep"

    /** Lists the reader made are kept apart from the bundled ids by this prefix. */
    private const val CUSTOM_PREFIX = "custom-"

    fun isCustom(id: String): Boolean = id.startsWith(CUSTOM_PREFIX)

    fun newCustom(): String = CUSTOM_PREFIX + UUID.randomUUID()
}

/** How many times each item has been said on [date], per category. */
data class DayProgress(val date: LocalDate, val counts: Map<String, Map<String, Int>> = emptyMap()) {

    fun count(categoryId: String, itemId: String): Int = counts[categoryId]?.get(itemId) ?: 0

    fun isDone(category: AthkarCategory, item: AthkarItem): Boolean = count(category.id, item.id) >= item.count

    fun completedItems(category: AthkarCategory): Int = category.items.count { isDone(category, it) }

    fun isComplete(category: AthkarCategory): Boolean =
        category.items.isNotEmpty() && category.items.all { isDone(category, it) }

    /** Share of the category's repetitions said so far, 0..1. */
    fun fraction(category: AthkarCategory): Float {
        val total = category.totalCount
        if (total == 0) return 0f
        return category.items.sumOf { min(count(category.id, it.id), it.count) } / total.toFloat()
    }

    /** One more repetition of [item]; never beyond its count. */
    fun increment(categoryId: String, item: AthkarItem): DayProgress {
        val current = count(categoryId, item.id)
        if (current >= item.count) return this
        val category = counts[categoryId].orEmpty() + (item.id to current + 1)
        return copy(counts = counts + (categoryId to category))
    }

    fun reset(categoryId: String): DayProgress = copy(counts = counts - categoryId)

    /** Progress for [today]; counts from an earlier day don't carry over. */
    fun on(today: LocalDate): DayProgress = if (date == today) this else DayProgress(today)
}

/** Consecutive days on which both the morning and the evening athkar were completed. */
data class Streak(val days: Int = 0, val lastDay: LocalDate? = null) {

    fun completed(day: LocalDate): Streak = when (lastDay) {
        day -> this
        day.minusDays(1) -> Streak(days + 1, day)
        else -> Streak(1, day)
    }

    /** The streak holds until a whole day is missed. */
    fun current(today: LocalDate): Int = if (lastDay != null && !lastDay.isBefore(today.minusDays(1))) days else 0
}

/** Which athkar fit the moment, so the Athkar tab can offer them first. */
object AthkarSchedule {
    private const val AFTER_PRAYER_MINUTES = 30L
    private const val NO_IQAMA_DELAY_MINUTES = 15L

    /**
     * After-prayer athkar for half an hour after each prayer, morning from Fajr to Dhuhr, evening from
     * Asr to Isha, and the sleep athkar at night.
     */
    fun suggest(now: ZonedDateTime, today: PrayerSchedule): String {
        val adhan = today.adhan
        for (prayer in Prayer.obligatory) {
            val start = today.iqama[prayer] ?: adhan.getValue(prayer).plusMinutes(NO_IQAMA_DELAY_MINUTES)
            if (!now.isBefore(start) && now.isBefore(start.plusMinutes(AFTER_PRAYER_MINUTES))) return AthkarIds.AFTER_PRAYER
        }
        return when {
            now.isBefore(adhan.getValue(Prayer.FAJR)) -> AthkarIds.SLEEP
            now.isBefore(adhan.getValue(Prayer.DHUHR)) -> AthkarIds.MORNING
            now.isBefore(adhan.getValue(Prayer.ASR)) -> AthkarIds.AFTER_PRAYER
            now.isBefore(adhan.getValue(Prayer.ISHA)) -> AthkarIds.EVENING
            else -> AthkarIds.SLEEP
        }
    }
}
