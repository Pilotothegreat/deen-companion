package com.pilotothegreat.deencompanion.data.settings

import com.pilotothegreat.deencompanion.core.prayer.AsrSchool
import com.pilotothegreat.deencompanion.core.prayer.CalculationMethod
import com.pilotothegreat.deencompanion.core.prayer.IqamaRule
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.prayer.PrayerConfig
import com.pilotothegreat.deencompanion.data.quran.Reciter
import java.time.LocalTime
import java.time.ZoneId

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class SavedLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String?,
    val timezoneId: String,
    /** Epoch millis of the last successful lookup; 0 when never refreshed. */
    val updatedAt: Long,
    /** True until a location has been saved; prayer times then use [Defaults]. */
    val isDefault: Boolean,
)

data class IqamaSetting(
    val fixed: Boolean,
    val offsetMinutes: Int,
    val fixedTime: LocalTime,
) {
    val rule: IqamaRule get() = if (fixed) IqamaRule.Fixed(fixedTime) else IqamaRule.Offset(offsetMinutes)
}

data class AppSettings(
    val location: SavedLocation,
    val method: CalculationMethod,
    val asrSchool: AsrSchool,
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
    val dismissedRamadanYear: Int,
    /** BCP 47 tag of the UI language, or [AppLanguage.SYSTEM]. */
    val appLanguage: String,
) {
    val zone: ZoneId
        get() = runCatching { ZoneId.of(location.timezoneId) }.getOrElse { ZoneId.systemDefault() }

    val prayerConfig: PrayerConfig
        get() = PrayerConfig(
            latitude = location.latitude,
            longitude = location.longitude,
            zone = zone,
            method = method,
            asrSchool = asrSchool,
            iqama = iqama.mapValues { it.value.rule },
        )
}

/** Defaults target the app's primary audience in Oman. */
object Defaults {
    const val LATITUDE = 23.5880
    const val LONGITUDE = 58.3829
    const val TIMEZONE = "Asia/Muscat"
    val METHOD = CalculationMethod.OMAN
    const val QURAN_FONT_SIZE = 28
    val QURAN_FONT_RANGE = 20..44
    val HIJRI_ADJUSTMENT_RANGE = -2..2
    val IQAMA_OFFSET_RANGE = 0..60

    val iqama: Map<Prayer, IqamaSetting> = mapOf(
        Prayer.FAJR to IqamaSetting(fixed = false, offsetMinutes = 25, fixedTime = LocalTime.of(5, 15)),
        Prayer.DHUHR to IqamaSetting(fixed = false, offsetMinutes = 25, fixedTime = LocalTime.of(12, 50)),
        Prayer.ASR to IqamaSetting(fixed = false, offsetMinutes = 20, fixedTime = LocalTime.of(15, 45)),
        Prayer.MAGHRIB to IqamaSetting(fixed = false, offsetMinutes = 10, fixedTime = LocalTime.of(18, 45)),
        Prayer.ISHA to IqamaSetting(fixed = false, offsetMinutes = 20, fixedTime = LocalTime.of(20, 15)),
    )
}
