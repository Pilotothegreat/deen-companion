package com.pilotothegreat.deencompanion.core.analytics

import org.json.JSONArray
import org.json.JSONObject

/**
 * Everything the app is allowed to count.
 *
 * A closed list rather than free-form event names, because free-form names are how a query string, a
 * search term or an ayah someone bookmarked ends up in a report. Each entry is a counter and nothing
 * else: how many times it happened, on which day. There is no text, no identifier, no location and no
 * order of events — the report cannot say what one person did, only what a day looked like.
 *
 * [id] is the wire name and never changes once shipped; the enum constant can be renamed freely.
 */
enum class UsageEvent(val id: String) {
    APP_OPENED("app_opened"),

    SCREEN_TODAY("screen_today"),
    SCREEN_QURAN("screen_quran"),
    SCREEN_READER("screen_reader"),
    SCREEN_HADITH("screen_hadith"),
    SCREEN_ATHKAR("screen_athkar"),
    SCREEN_QIBLA("screen_qibla"),
    SCREEN_SETTINGS("screen_settings"),

    QURAN_SEARCHED("quran_searched"),
    /** A search that named a place — a passage, a reference, a juz or a page — rather than text. */
    QURAN_SEARCH_JUMPED("quran_search_jumped"),
    READER_PAGE_TURNED("reader_page_turned"),
    RECITATION_PLAYED("recitation_played"),
    RECITER_CHANGED("reciter_changed"),
    AYAH_BOOKMARKED("ayah_bookmarked"),
    AYAH_SHARED("ayah_shared"),
    AYAH_REPEATED("ayah_repeated"),
    KHATMA_STARTED("khatma_started"),

    HADITH_SEARCHED("hadith_searched"),
    HADITH_DOWNLOADED("hadith_downloaded"),
    HADITH_FAVOURITED("hadith_favourited"),

    ATHKAR_SESSION_STARTED("athkar_session_started"),
    ATHKAR_COUNTED("athkar_counted"),
    ATHKAR_SESSION_FINISHED("athkar_session_finished"),
    TASBIH_COUNTED("tasbih_counted"),

    PRAYER_MARKED("prayer_marked"),
    QIBLA_ALIGNED("qibla_aligned"),
    NOTIFICATION_OPENED("notification_opened"),

    WIDGET_CONFIGURED("widget_configured"),
    THEME_CHANGED("theme_changed"),
    LANGUAGE_CHANGED("language_changed"),
    REFRESH_PRESSED("refresh_pressed"),
    BACKUP_EXPORTED("backup_exported"),
    BACKUP_RESTORED("backup_restored"),

    UPDATE_OFFERED("update_offered"),
    UPDATE_STARTED("update_started"),
    UPDATE_INSTALLED("update_installed"),
    ;

    companion object {
        fun byId(id: String): UsageEvent? = entries.firstOrNull { it.id == id }
    }
}

/** One day's count of one event. */
data class UsageCount(val day: String, val event: String, val count: Int)

/** The build and device a report came from, which is what turns counts into a userbase. */
data class UsageEnvironment(
    val appVersion: String,
    val appVersionCode: Int,
    /** "play", "github" or "other": which build these numbers describe. */
    val installSource: String,
    val androidSdk: Int,
    val deviceModel: String,
    val language: String,
    val country: String,
)

/**
 * What one device sends, or exports: how long it has been used, and what was pressed on which day.
 *
 * There is no device id in it. Two reports from the same phone cannot be told apart from two reports
 * from two phones with the same model and the same first day, which is deliberate: this answers
 * "which features get used" and cannot answer "what does this person do".
 */
data class UsageReport(
    val environment: UsageEnvironment,
    /** ISO day this device first opened the app, for cohorting; no time of day. */
    val firstSeen: String,
    val daysActive: Int,
    val sessions: Int,
    val counts: List<UsageCount>,
) {
    /** Totals per event over the whole window, which is what a reader of the report looks at first. */
    val totals: Map<String, Int>
        get() = counts.groupBy { it.event }.mapValues { (_, list) -> list.sumOf { it.count } }

    fun toJson(): JSONObject = JSONObject().apply {
        put("schema", SCHEMA)
        put("app_version", environment.appVersion)
        put("app_version_code", environment.appVersionCode)
        put("install_source", environment.installSource)
        put("android_sdk", environment.androidSdk)
        put("device_model", environment.deviceModel)
        put("language", environment.language)
        put("country", environment.country)
        put("first_seen", firstSeen)
        put("days_active", daysActive)
        put("sessions", sessions)
        put("totals", JSONObject(totals.toSortedMap().mapValues { it.value as Any }))
        put(
            "days",
            JSONArray().apply {
                counts.groupBy { it.day }.toSortedMap().forEach { (day, list) ->
                    put(
                        JSONObject().apply {
                            put("day", day)
                            put("events", JSONObject(list.associate { it.event to it.count as Any }.toSortedMap()))
                        },
                    )
                }
            },
        )
    }

    companion object {
        /** Bumped when the shape changes, so a collector can tell old reports from new ones. */
        const val SCHEMA = 1
    }
}
