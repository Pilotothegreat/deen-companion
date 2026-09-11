package com.pilotothegreat.deencompanion.data.athkar

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pilotothegreat.deencompanion.core.athkar.AthkarCategory
import com.pilotothegreat.deencompanion.core.athkar.AthkarGroup
import com.pilotothegreat.deencompanion.core.athkar.AthkarIds
import com.pilotothegreat.deencompanion.core.athkar.AthkarItem
import com.pilotothegreat.deencompanion.core.athkar.AthkarLibrary
import com.pilotothegreat.deencompanion.core.athkar.DayProgress
import com.pilotothegreat.deencompanion.core.athkar.Streak
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.time.LocalDate

/** The bundled athkar (assets/athkar.json) plus today's progress and the streak, kept in DataStore. */
class AthkarRepository(
    private val readJson: () -> String,
    private val dataStore: DataStore<Preferences>,
) {
    private val mutex = Mutex()

    @Volatile
    private var cached: AthkarLibrary? = null

    suspend fun library(): AthkarLibrary = cached ?: mutex.withLock {
        cached ?: withContext(Dispatchers.IO) { parse(readJson()) }.also { cached = it }
    }

    /** The last saved progress; call [DayProgress.on] with today's date before reading it. */
    val progress: Flow<DayProgress> = dataStore.data.map { decode(it[PROGRESS]) }.distinctUntilChanged()

    val streak: Flow<Streak> = dataStore.data.map { it.streak() }.distinctUntilChanged()

    /**
     * Counts one repetition and returns the new progress, or null when [item] was already done (so
     * rapid taps can't report a completion twice). Finishing both the morning and the evening athkar
     * extends the streak.
     */
    suspend fun increment(category: AthkarCategory, item: AthkarItem, today: LocalDate): DayProgress? {
        val library = library()
        val morning = library.category(AthkarIds.MORNING)
        val evening = library.category(AthkarIds.EVENING)
        var result: DayProgress? = null
        dataStore.edit { prefs ->
            val before = decode(prefs[PROGRESS]).on(today)
            if (before.isDone(category, item)) return@edit
            val updated = before.increment(category.id, item)
            prefs[PROGRESS] = encode(updated)
            if (morning != null && evening != null && updated.isComplete(morning) && updated.isComplete(evening)) {
                val streak = prefs.streak().completed(today)
                prefs[STREAK_DAYS] = streak.days
                prefs[STREAK_LAST] = today.toString()
            }
            result = updated
        }
        return result
    }

    suspend fun reset(categoryId: String, today: LocalDate) {
        dataStore.edit { it[PROGRESS] = encode(decode(it[PROGRESS]).on(today).reset(categoryId)) }
    }

    private fun Preferences.streak() =
        Streak(this[STREAK_DAYS] ?: 0, this[STREAK_LAST]?.let { runCatching { LocalDate.parse(it) }.getOrNull() })

    companion object {
        private val PROGRESS = stringPreferencesKey("athkar_progress")
        private val STREAK_DAYS = intPreferencesKey("athkar_streak_days")
        private val STREAK_LAST = stringPreferencesKey("athkar_streak_last")

        fun parse(json: String): AthkarLibrary {
            val root = Json.parseToJsonElement(json).jsonObject
            return AthkarLibrary(
                core = root.getValue("core").jsonArray.map { category(it.jsonObject) },
                groups = root.getValue("groups").jsonArray.map { element ->
                    val group = element.jsonObject
                    AthkarGroup(
                        id = group.text("id"),
                        titleEnglish = group.text("titleEn"),
                        titleArabic = group.text("titleAr"),
                        categories = group.getValue("categories").jsonArray.map { category(it.jsonObject) },
                    )
                },
            )
        }

        private fun category(json: JsonObject) = AthkarCategory(
            id = json.text("id"),
            titleEnglish = json.text("titleEn"),
            titleArabic = json.text("titleAr"),
            items = json.getValue("items").jsonArray.map { element ->
                val item = element.jsonObject
                AthkarItem(
                    id = item.text("id"),
                    arabic = item.text("ar"),
                    translation = item.text("en"),
                    transliteration = item.text("tr"),
                    count = item.getValue("count").jsonPrimitive.int,
                    virtueEnglish = item.text("virtueEn"),
                    virtueArabic = item.text("virtueAr"),
                    sourceEnglish = item.text("sourceEn"),
                    sourceArabic = item.text("sourceAr"),
                    noteEnglish = item.text("noteEn"),
                    noteArabic = item.text("noteAr"),
                )
            },
        )

        private fun JsonObject.text(key: String): String = this[key]?.jsonPrimitive?.content.orEmpty()

        internal fun encode(progress: DayProgress): String = buildJsonObject {
            put("date", progress.date.toString())
            putJsonObject("counts") {
                progress.counts.forEach { (categoryId, items) ->
                    putJsonObject(categoryId) { items.forEach { (itemId, count) -> put(itemId, count) } }
                }
            }
        }.toString()

        /** Unreadable or missing data counts as no progress. */
        internal fun decode(value: String?): DayProgress = runCatching {
            val json = Json.parseToJsonElement(requireNotNull(value)).jsonObject
            DayProgress(
                date = LocalDate.parse(json.text("date")),
                counts = json.getValue("counts").jsonObject.mapValues { (_, items) ->
                    items.jsonObject.mapValues { it.value.jsonPrimitive.int }
                },
            )
        }.getOrElse { DayProgress(LocalDate.MIN) }
    }
}
