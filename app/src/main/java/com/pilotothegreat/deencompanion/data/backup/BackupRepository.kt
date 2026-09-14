package com.pilotothegreat.deencompanion.data.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.pilotothegreat.deencompanion.data.db.BookmarkDao
import com.pilotothegreat.deencompanion.data.db.BookmarkEntity
import com.pilotothegreat.deencompanion.data.db.ReadingPlanDao
import com.pilotothegreat.deencompanion.data.db.ReadingPlanEntity
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/** What went wrong with a restore, so the reader is told rather than left guessing. */
enum class RestoreError { UNREADABLE, WRONG_FILE, TOO_NEW }

/**
 * Export and restore by file.
 *
 * Cloud backup stays off — the app holds where you live and what you read, and that is nobody
 * else's. But "off" used to mean "no way out at all", and that is its own problem: the Play build
 * and the GitHub build are different packages, so moving between them lost every setting, every
 * bookmark and a khatma halfway done. A file the reader keeps solves that without anything leaving
 * the device except by their own hand.
 */
class BackupRepository(
    private val dataStore: DataStore<Preferences>,
    private val bookmarks: BookmarkDao,
    private val readingPlan: ReadingPlanDao,
) {

    companion object {
        const val VERSION = 1
        const val MAGIC = "bilal-backup"
        const val FILENAME = "bilal-backup.json"
    }

    /** Everything worth carrying to another phone, as pretty JSON so it is readable and diffable. */
    suspend fun export(): String {
        val preferences = dataStore.data.first()
        val settings = JSONObject()
        preferences.asMap().forEach { (key, value) ->
            val encoded = when (value) {
                is Boolean, is Int, is Long, is Float, is Double, is String -> value
                is Set<*> -> JSONArray(value.map { it.toString() })
                else -> return@forEach
            }
            settings.put("${typeTag(value)}:${key.name}", encoded)
        }
        val plan = readingPlan.observe().first()
        return JSONObject()
            .put("format", MAGIC)
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("settings", settings)
            .put(
                "bookmarks",
                JSONArray(
                    bookmarks.observeAll().first().map {
                        JSONObject()
                            .put("surah", it.surahNumber)
                            .put("ayah", it.ayahNumber)
                            .put("surahName", it.surahName)
                            .put("timestamp", it.timestamp)
                    },
                ),
            )
            .put(
                "khatma",
                plan?.let {
                    JSONObject()
                        .put("startedOn", it.startedOn)
                        .put("targetDays", it.targetDays)
                        .put("startPage", it.startPage)
                        .put("lastPage", it.lastPage)
                } ?: JSONObject.NULL,
            )
            .toString(2)
    }

    /**
     * Replaces settings, bookmarks and the khatma with what the file holds. A file from a newer
     * version is refused rather than half-applied: a partial restore is worse than none, because the
     * reader cannot tell which half took.
     */
    suspend fun restore(json: String): RestoreError? {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return RestoreError.UNREADABLE
        if (root.optString("format") != MAGIC) return RestoreError.WRONG_FILE
        if (root.optInt("version", Int.MAX_VALUE) > VERSION) return RestoreError.TOO_NEW

        val settings = root.optJSONObject("settings")
        if (settings != null) {
            dataStore.edit { preferences ->
                preferences.clear()
                settings.keys().forEach { tagged ->
                    val tag = tagged.substringBefore(':')
                    val name = tagged.substringAfter(':')
                    when (tag) {
                        "b" -> preferences[booleanPreferencesKey(name)] = settings.getBoolean(tagged)
                        "i" -> preferences[intPreferencesKey(name)] = settings.getInt(tagged)
                        "l" -> preferences[longPreferencesKey(name)] = settings.getLong(tagged)
                        "f" -> preferences[floatPreferencesKey(name)] = settings.getDouble(tagged).toFloat()
                        "d" -> preferences[doublePreferencesKey(name)] = settings.getDouble(tagged)
                        "s" -> preferences[stringPreferencesKey(name)] = settings.getString(tagged)
                        "set" -> preferences[stringSetPreferencesKey(name)] = settings.getJSONArray(tagged).toStringSet()
                    }
                }
            }
        }

        root.optJSONArray("bookmarks")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val surah = item.getInt("surah")
                val ayah = item.getInt("ayah")
                bookmarks.upsert(
                    BookmarkEntity(
                        id = BookmarkEntity.idFor(surah, ayah),
                        surahNumber = surah,
                        ayahNumber = ayah,
                        surahName = item.optString("surahName"),
                        timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                    ),
                )
            }
        }

        root.optJSONObject("khatma")?.let { plan ->
            readingPlan.upsert(
                ReadingPlanEntity(
                    startedOn = plan.getString("startedOn"),
                    targetDays = plan.getInt("targetDays"),
                    startPage = plan.getInt("startPage"),
                    lastPage = plan.getInt("lastPage"),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
        return null
    }

    /** Back to a fresh install, without touching the Quran text or the downloaded recitation. */
    suspend fun reset() {
        dataStore.edit { it.clear() }
        readingPlan.clear()
    }

    /** DataStore is typed, so the type travels with the key rather than being guessed on the way back. */
    private fun typeTag(value: Any): String = when (value) {
        is Boolean -> "b"
        is Int -> "i"
        is Long -> "l"
        is Float -> "f"
        is Double -> "d"
        is Set<*> -> "set"
        else -> "s"
    }

    private fun JSONArray.toStringSet(): Set<String> = (0 until length()).map { getString(it) }.toSet()
}
