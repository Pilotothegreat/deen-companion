package com.pilotothegreat.deencompanion.data.settings

import com.pilotothegreat.deencompanion.core.prayer.AsrSchool
import com.pilotothegreat.deencompanion.core.prayer.CalculationMethod
import com.pilotothegreat.deencompanion.core.prayer.HighLatitudeMode
import com.pilotothegreat.deencompanion.core.prayer.IqamaRule
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.prayer.PrayerConfig
import com.pilotothegreat.deencompanion.core.quran.RepeatMode
import com.pilotothegreat.deencompanion.data.quran.Reciter
import java.time.LocalTime
import java.time.ZoneId

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Where the saved location came from. Enum names are persisted in settings; do not rename. */
enum class LocationSource {
    /** Nothing saved yet; the app uses [Defaults]. */
    DEFAULT,
    DEVICE,
    IP,
    /** Picked from the city list; automatic updates leave it alone. */
    MANUAL,
}

data class SavedLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String?,
    val timezoneId: String,
    /** ISO 3166 alpha-2 country, used to pick the calculation method automatically. */
    val countryCode: String?,
    /** Epoch millis of the last successful lookup; 0 when never refreshed. */
    val updatedAt: Long,
    /** True until a location has been saved; prayer times then use [Defaults]. */
    val isDefault: Boolean,
    val source: LocationSource,
)

data class IqamaSetting(
    val fixed: Boolean,
    val offsetMinutes: Int,
    val fixedTime: LocalTime,
) {
    val rule: IqamaRule get() = if (fixed) IqamaRule.Fixed(fixedTime) else IqamaRule.Offset(offsetMinutes)
}

/** Whether the app may react to the world, and how far it looks. */
data class SmartSettings(
    /** Rain, wind, storms and extreme temperatures surface the matching dua. */
    val weather: Boolean = true,
    /** Jumu'ah, Ramadan, the two Eids and the rest of the Islamic year. */
    val occasions: Boolean = true,
    /** Offer travel duas and qasr guidance when far from home. */
    val travel: Boolean = true,
    /** Nearby earthquakes and eclipses. */
    val naturalEvents: Boolean = true,
    /** Epoch millis until which "times of calamity" stays on; 0 when off. */
    val calamityUntil: Long = 0L,
    /** The Islamic day turns over at Maghrib, as it does in the calendar itself. */
    val hijriDayStartsAtMaghrib: Boolean = true,
    /** Distance from home past which travel is suspected. */
    val safarKm: Int = Defaults.SAFAR_KM,
) {
    fun calamityActive(nowMillis: Long): Boolean = calamityUntil > nowMillis
}

/** What each prayer sounds like, and what arrives before it. */
data class SoundSettings(
    /** Per prayer: a muezzin id, [SYSTEM_SOUND], [SILENT], or a content URI the user picked. */
    val adhan: Map<Prayer, String> = emptyMap(),
    /** Minutes before the adhan for the early reminder; 0 turns it off. */
    val preReminderMinutes: Int = 0,
    val preReminderPrayers: Set<Prayer> = Prayer.obligatory.toSet(),
    /** Minutes of silence from iqama; 0 turns it off. */
    val silenceMinutes: Int = 0,
) {
    fun adhanFor(prayer: Prayer): String = adhan[prayer] ?: SYSTEM_SOUND

    companion object {
        const val SYSTEM_SOUND = "system"
        const val SILENT = "silent"
    }
}

data class QuranSettings(
    val translationId: String = Defaults.TRANSLATION,
    /** Shown beneath the first when set. */
    val secondTranslationId: String? = null,
    val tajweed: Boolean = false,
    val audioCacheMb: Int = Defaults.AUDIO_CACHE_MB,
    val playbackSpeed: Float = 1f,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val repeatCount: Int = 3,
    /** Carry on into the next surah instead of stopping at the end of this one. */
    val continuousPlayback: Boolean = true,
)

enum class ContrastMode { SYSTEM, MEDIUM, HIGH }

enum class ReduceMotion { SYSTEM, ON, OFF }

/** Everything that makes the app usable for people the default design leaves behind. */
data class AccessibilitySettings(
    /** Bigger text, one card at a time, larger targets, no decorative motion. */
    val simpleMode: Boolean = false,
    /** Multiplies the system font scale, for people who want more than the system slider gives. */
    val textScale: Float = 1f,
    val contrast: ContrastMode = ContrastMode.SYSTEM,
    val reduceMotion: ReduceMotion = ReduceMotion.SYSTEM,
    val haptics: Boolean = true,
    val largeTouchTargets: Boolean = false,
)

data class AppSettings(
    val location: SavedLocation,
    /** The method picked in settings; also the fallback while [methodAuto] has no country to go on. */
    val method: CalculationMethod,
    val methodAuto: Boolean,
    val asrSchool: AsrSchool,
    val highLatitude: HighLatitudeMode,
    /** Minutes added to each computed time to match the local mosque. */
    val adjustments: Map<Prayer, Int>,
    val iqama: Map<Prayer, IqamaSetting>,
    val hijriAdjustment: Int,
    val notificationsEnabled: Boolean,
    val mutedPrayers: Set<Prayer>,
    val themeMode: ThemeMode,
    val dynamicColor: Boolean,
    val pureBlack: Boolean,
    val quranFontSize: Int,
    val showTranslation: Boolean,
    val reciter: Reciter,
    /** Last Quran page opened (1..604), or 0 if none. */
    val lastReadPage: Int,
    val useIpLocationFallback: Boolean,
    /** Moment ids sent away, each stamped "id@epochDay". */
    val dismissedMoments: Set<String>,
    /** BCP 47 tag of the UI language, or [AppLanguage.SYSTEM]. */
    val appLanguage: String,
    /** Morning and evening athkar reminders, after Fajr and Asr. */
    val athkarReminders: Boolean,
    val athkarFontSize: Int,
    /** Null until chosen: the English meaning is shown by default, except in the Arabic UI. */
    val athkarShowTranslation: Boolean?,
    val athkarShowTransliteration: Boolean,
    val smart: SmartSettings = SmartSettings(),
    val sounds: SoundSettings = SoundSettings(),
    val quran: QuranSettings = QuranSettings(),
    val accessibility: AccessibilitySettings = AccessibilitySettings(),
    /** False until the first-run flow has been seen. */
    val onboardingCompleted: Boolean = false,
    /** The version whose "what's new" has already been shown. */
    val lastSeenVersionCode: Int = 0,
) {
    val zone: ZoneId
        get() = runCatching { ZoneId.of(location.timezoneId) }.getOrElse { ZoneId.systemDefault() }

    /** The method actually used for prayer times. */
    val effectiveMethod: CalculationMethod
        get() = location.countryCode?.takeIf { methodAuto }?.let(CalculationMethod::forCountry) ?: method

    val prayerConfig: PrayerConfig
        get() = PrayerConfig(
            latitude = location.latitude,
            longitude = location.longitude,
            zone = zone,
            method = effectiveMethod,
            asrSchool = asrSchool,
            iqama = iqama.mapValues { it.value.rule },
            highLatitude = highLatitude,
            adjustments = adjustments,
        )
}

/** Defaults target the app's primary audience in Oman. */
object Defaults {
    const val LATITUDE = 23.5880
    const val LONGITUDE = 58.3829
    const val TIMEZONE = "Asia/Muscat"
    const val COUNTRY = "OM"
    val METHOD = CalculationMethod.OMAN
    const val QURAN_FONT_SIZE = 28
    val QURAN_FONT_RANGE = 20..44
    val HIJRI_ADJUSTMENT_RANGE = -2..2
    val IQAMA_OFFSET_RANGE = 0..60
    val ADJUSTMENT_RANGE = -30..30
    const val ATHKAR_FONT_SIZE = 26
    val ATHKAR_FONT_RANGE = 20..40
    /** Roughly the classical four burud; adjustable because scholars differ. */
    const val SAFAR_KM = 80
    val SAFAR_RANGE = 60..120
    const val TRANSLATION = "clearquran"
    const val AUDIO_CACHE_MB = 256
    val AUDIO_CACHE_RANGE = 64..2048
    val PRE_REMINDER_CHOICES = listOf(0, 5, 10, 15, 20, 30)
    val SILENCE_CHOICES = listOf(0, 10, 15, 20, 30)
    val TEXT_SCALE_RANGE = 0.9f..1.6f

    val iqama: Map<Prayer, IqamaSetting> = mapOf(
        Prayer.FAJR to IqamaSetting(fixed = false, offsetMinutes = 25, fixedTime = LocalTime.of(5, 15)),
        Prayer.DHUHR to IqamaSetting(fixed = false, offsetMinutes = 25, fixedTime = LocalTime.of(12, 50)),
        Prayer.ASR to IqamaSetting(fixed = false, offsetMinutes = 20, fixedTime = LocalTime.of(15, 45)),
        Prayer.MAGHRIB to IqamaSetting(fixed = false, offsetMinutes = 10, fixedTime = LocalTime.of(18, 45)),
        Prayer.ISHA to IqamaSetting(fixed = false, offsetMinutes = 20, fixedTime = LocalTime.of(20, 15)),
    )
}
