package com.pilotothegreat.deencompanion.data.settings

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pilotothegreat.deencompanion.core.prayer.AsrSchool
import com.pilotothegreat.deencompanion.core.prayer.CalculationMethod
import com.pilotothegreat.deencompanion.core.prayer.HighLatitudeMode
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.data.quran.Reciter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** The single app DataStore, shared by [SettingsRepository] and the tasbih repository. */
fun createAppDataStore(context: Context): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    migrations = listOf(LegacySettingsMigration),
    produceFile = { context.preferencesDataStoreFile("settings") },
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { it.toAppSettings() }.distinctUntilChanged()

    suspend fun current(): AppSettings = settings.first()

    suspend fun setLocation(
        latitude: Double,
        longitude: Double,
        cityName: String?,
        timezoneId: String,
        countryCode: String? = null,
        source: LocationSource = LocationSource.DEVICE,
        updatedAt: Long = System.currentTimeMillis(),
    ) = edit {
        it[Keys.LATITUDE] = latitude.coerceIn(-90.0, 90.0)
        it[Keys.LONGITUDE] = longitude.coerceIn(-180.0, 180.0)
        if (cityName.isNullOrBlank()) it.remove(Keys.CITY_NAME) else it[Keys.CITY_NAME] = cityName
        if (countryCode.isNullOrBlank()) it.remove(Keys.COUNTRY_CODE) else it[Keys.COUNTRY_CODE] = countryCode.uppercase()
        it[Keys.TIMEZONE_ID] = timezoneId
        it[Keys.LOCATION_SOURCE] = source.name
        it[Keys.LOCATION_UPDATED_AT] = updatedAt
    }

    suspend fun setCityName(name: String) = edit { it[Keys.CITY_NAME] = name }

    suspend fun setTimezone(timezoneId: String) = edit { it[Keys.TIMEZONE_ID] = timezoneId }

    /** Picking a method by hand turns off automatic selection. */
    suspend fun setMethod(value: CalculationMethod) = edit {
        it[Keys.CALC_METHOD] = value.name
        it[Keys.METHOD_AUTO] = false
    }

    suspend fun setMethodAuto() = edit { it[Keys.METHOD_AUTO] = true }

    suspend fun setAsrSchool(value: AsrSchool) = edit { it[Keys.ASR_SCHOOL] = value.name }

    suspend fun setHighLatitude(value: HighLatitudeMode) = edit { it[Keys.HIGH_LATITUDE] = value.name }

    suspend fun setAdjustment(prayer: Prayer, minutes: Int) =
        edit { it[adjustmentKey(prayer)] = minutes.coerceIn(Defaults.ADJUSTMENT_RANGE) }

    suspend fun resetAdjustments() = edit { prefs -> Prayer.entries.forEach { prefs.remove(adjustmentKey(it)) } }

    suspend fun setIqama(prayer: Prayer, value: IqamaSetting) = edit {
        val keys = IqamaKeys(prayer)
        it[keys.fixed] = value.fixed
        it[keys.offset] = value.offsetMinutes.coerceIn(Defaults.IQAMA_OFFSET_RANGE)
        it[keys.time] = value.fixedTime.format(TIME_FORMAT)
    }

    suspend fun setHijriAdjustment(days: Int) =
        edit { it[Keys.HIJRI_ADJUSTMENT] = days.coerceIn(Defaults.HIJRI_ADJUSTMENT_RANGE) }

    suspend fun setNotificationsEnabled(enabled: Boolean) = edit { it[Keys.NOTIFICATIONS] = enabled }

    suspend fun setPrayerMuted(prayer: Prayer, muted: Boolean) = edit {
        val current = it[Keys.MUTED_PRAYERS].orEmpty()
        it[Keys.MUTED_PRAYERS] = if (muted) current + prayer.key else current - prayer.key
    }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = enabled }

    suspend fun setPureBlack(enabled: Boolean) = edit { it[Keys.PURE_BLACK] = enabled }

    suspend fun setQuranFontSize(size: Int) =
        edit { it[Keys.QURAN_FONT_SIZE] = size.coerceIn(Defaults.QURAN_FONT_RANGE) }

    suspend fun setShowTranslation(show: Boolean) = edit { it[Keys.SHOW_TRANSLATION] = show }

    suspend fun setReciter(reciter: Reciter) = edit { it[Keys.RECITER] = reciter.name }

    suspend fun setLastReadPage(page: Int) = edit { it[Keys.LAST_READ_PAGE] = page }

    suspend fun setUseIpLocationFallback(enabled: Boolean) = edit { it[Keys.IP_FALLBACK] = enabled }

    suspend fun setDismissedRamadanYear(hijriYear: Int) = edit { it[Keys.DISMISSED_RAMADAN_YEAR] = hijriYear }

    /** Timestamp and latest release tag of the last update check. */
    suspend fun lastUpdateCheck(): Pair<Long, String> = dataStore.data.first().let {
        (it[Keys.UPDATE_CHECKED_AT] ?: 0L) to (it[Keys.UPDATE_LATEST] ?: "")
    }

    val lastUpdateCheckedAt: Flow<Long> =
        dataStore.data.map { it[Keys.UPDATE_CHECKED_AT] ?: 0L }.distinctUntilChanged()

    suspend fun saveUpdateCheck(timestamp: Long, latestVersion: String) = edit {
        it[Keys.UPDATE_CHECKED_AT] = timestamp
        it[Keys.UPDATE_LATEST] = latestVersion
    }

    suspend fun setAppLanguage(tag: String) = edit { it[Keys.LANGUAGE] = tag }

    /**
     * One-time adoption of the language from earlier versions, which applied it themselves and
     * defaulted to Arabic. Returns the tag to hand to [AppLanguage.apply], or null.
     */
    suspend fun migrateLegacyLanguage(): String? {
        var language: String? = null
        dataStore.edit {
            if (it[Keys.LANGUAGE_MIGRATED] == true) return@edit
            language = it[Keys.LANGUAGE] ?: if (it.asMap().isNotEmpty()) "ar" else null
            language?.let { tag -> it[Keys.LANGUAGE] = tag }
            it[Keys.LANGUAGE_MIGRATED] = true
        }
        return language
    }

    private suspend fun edit(block: suspend (MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }
}

internal object Keys {
    val LATITUDE = doublePreferencesKey("latitude")
    val LONGITUDE = doublePreferencesKey("longitude")
    val CITY_NAME = stringPreferencesKey("city_name")
    val TIMEZONE_ID = stringPreferencesKey("timezone_id")
    val LOCATION_UPDATED_AT = longPreferencesKey("location_updated_at")
    val CALC_METHOD = stringPreferencesKey("calc_method")
    val ASR_SCHOOL = stringPreferencesKey("asr_school")
    val HIJRI_ADJUSTMENT = intPreferencesKey("hijri_adjustment")
    val NOTIFICATIONS = booleanPreferencesKey("notification_enabled")
    val MUTED_PRAYERS = stringSetPreferencesKey("muted_prayers")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val PURE_BLACK = booleanPreferencesKey("amoled_black_mode")
    val QURAN_FONT_SIZE = intPreferencesKey("quran_arabic_font_size")
    val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
    val RECITER = stringPreferencesKey("reciter")
    val LAST_READ_PAGE = intPreferencesKey("quran_last_page")
    val IP_FALLBACK = booleanPreferencesKey("use_ip_location_fallback")
    val DISMISSED_RAMADAN_YEAR = intPreferencesKey("dismissed_ramadan_hilal_year")
    val UPDATE_CHECKED_AT = longPreferencesKey("github_check_timestamp")
    val UPDATE_LATEST = stringPreferencesKey("github_check_latest_version")
    val LANGUAGE_MIGRATED = booleanPreferencesKey("language_migrated")
    val LANGUAGE = stringPreferencesKey("app_language")
    val COUNTRY_CODE = stringPreferencesKey("country_code")
    val LOCATION_SOURCE = stringPreferencesKey("location_source")
    val METHOD_AUTO = booleanPreferencesKey("calc_method_auto")
    val HIGH_LATITUDE = stringPreferencesKey("high_latitude_rule")
}

internal fun adjustmentKey(prayer: Prayer) = intPreferencesKey("${prayer.key.lowercase()}_adjustment")

/** Per-prayer iqama keys, named as in earlier versions. */
internal class IqamaKeys(prayer: Prayer) {
    private val base = prayer.key.lowercase()
    val offset = intPreferencesKey("${base}_iqama")
    val fixed = booleanPreferencesKey("${base}_iqama_is_fixed")
    val time = stringPreferencesKey("${base}_iqama_time")
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

internal fun Preferences.toAppSettings(): AppSettings = AppSettings(
    location = SavedLocation(
        latitude = this[Keys.LATITUDE] ?: Defaults.LATITUDE,
        longitude = this[Keys.LONGITUDE] ?: Defaults.LONGITUDE,
        cityName = this[Keys.CITY_NAME]?.takeIf { it.isNotBlank() },
        timezoneId = this[Keys.TIMEZONE_ID] ?: Defaults.TIMEZONE,
        countryCode = this[Keys.COUNTRY_CODE] ?: Defaults.COUNTRY.takeIf { this[Keys.LATITUDE] == null },
        updatedAt = this[Keys.LOCATION_UPDATED_AT] ?: 0L,
        isDefault = this[Keys.LATITUDE] == null,
        // Earlier versions only saved device or IP locations.
        source = enumOrNull<LocationSource>(this[Keys.LOCATION_SOURCE])
            ?: if (this[Keys.LATITUDE] == null) LocationSource.DEFAULT else LocationSource.DEVICE,
    ),
    method = enumOrNull<CalculationMethod>(this[Keys.CALC_METHOD]) ?: Defaults.METHOD,
    // Installs that already chose a method keep it; everyone else follows their location.
    methodAuto = this[Keys.METHOD_AUTO] ?: (this[Keys.CALC_METHOD] == null),
    asrSchool = enumOrNull<AsrSchool>(this[Keys.ASR_SCHOOL]) ?: AsrSchool.STANDARD,
    highLatitude = enumOrNull<HighLatitudeMode>(this[Keys.HIGH_LATITUDE]) ?: HighLatitudeMode.AUTO,
    adjustments = Prayer.entries.associateWith { this[adjustmentKey(it)] ?: 0 }.filterValues { it != 0 },
    iqama = Prayer.obligatory.associateWith { prayer ->
        val keys = IqamaKeys(prayer)
        val default = Defaults.iqama.getValue(prayer)
        IqamaSetting(
            fixed = this[keys.fixed] ?: default.fixed,
            offsetMinutes = this[keys.offset] ?: default.offsetMinutes,
            fixedTime = parseTime(this[keys.time]) ?: default.fixedTime,
        )
    },
    hijriAdjustment = this[Keys.HIJRI_ADJUSTMENT] ?: 0,
    notificationsEnabled = this[Keys.NOTIFICATIONS] ?: true,
    mutedPrayers = this[Keys.MUTED_PRAYERS].orEmpty().mapNotNull(Prayer::fromKey).toSet(),
    themeMode = enumOrNull<ThemeMode>(this[Keys.THEME_MODE]) ?: ThemeMode.SYSTEM,
    dynamicColor = this[Keys.DYNAMIC_COLOR] ?: true,
    pureBlack = this[Keys.PURE_BLACK] ?: false,
    quranFontSize = (this[Keys.QURAN_FONT_SIZE] ?: Defaults.QURAN_FONT_SIZE).coerceIn(Defaults.QURAN_FONT_RANGE),
    showTranslation = this[Keys.SHOW_TRANSLATION] ?: false,
    reciter = enumOrNull<Reciter>(this[Keys.RECITER]) ?: Reciter.MISHARY,
    lastReadPage = this[Keys.LAST_READ_PAGE] ?: 0,
    useIpLocationFallback = this[Keys.IP_FALLBACK] ?: false,
    dismissedRamadanYear = this[Keys.DISMISSED_RAMADAN_YEAR] ?: 0,
    appLanguage = this[Keys.LANGUAGE] ?: AppLanguage.SYSTEM,
)

private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
    name?.let { n -> enumValues<T>().firstOrNull { it.name.equals(n, ignoreCase = true) } }

private fun parseTime(value: String?): LocalTime? =
    value?.let { runCatching { LocalTime.parse(it.trim(), TIME_FORMAT) }.getOrNull() }

/** Converts values written by earlier versions and drops keys no longer used. */
internal object LegacySettingsMigration : DataMigration<Preferences> {
    private val THEME = stringPreferencesKey("theme")
    private val HIJRI_METHOD = stringPreferencesKey("hijri_method")
    private val obsolete = listOf(
        intPreferencesKey("app_launch_count"),
        booleanPreferencesKey("donation_prompt_dismissed"),
        longPreferencesKey("last_donation_prompt_show_time"),
        intPreferencesKey("donation_prompt_show_count"),
        intPreferencesKey("notification_volume"),
        longPreferencesKey("last_prayer_time_update"),
        stringPreferencesKey("tasbih_history"),
        intPreferencesKey("tasbih_mode"),
        intPreferencesKey("tasbih_bead_size"),
        intPreferencesKey("tasbih_bead_preset"),
        stringSetPreferencesKey("favorited_hadiths"),
    )

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        THEME in currentData || HIJRI_METHOD in currentData || obsolete.any { it in currentData }

    override suspend fun migrate(currentData: Preferences): Preferences = currentData.toMutablePreferences().apply {
        this[THEME]?.let { legacy ->
            val (mode, dynamic) = when (legacy) {
                "LightMaterial" -> ThemeMode.LIGHT to true
                "DarkMaterial" -> ThemeMode.DARK to true
                "Auto" -> ThemeMode.SYSTEM to false
                "Light" -> ThemeMode.LIGHT to false
                "Dark" -> ThemeMode.DARK to false
                else -> ThemeMode.SYSTEM to true
            }
            this[Keys.THEME_MODE] = mode.name
            this[Keys.DYNAMIC_COLOR] = dynamic
            remove(THEME)
        }
        this[HIJRI_METHOD]?.let { legacy ->
            if (legacy == "REGIONAL") this[Keys.HIJRI_ADJUSTMENT] = 1
            remove(HIJRI_METHOD)
        }
        obsolete.forEach { remove(it) }
    }.toPreferences()

    override suspend fun cleanUp() = Unit
}
