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
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pilotothegreat.deencompanion.core.prayer.AsrSchool
import com.pilotothegreat.deencompanion.core.prayer.CalculationMethod
import com.pilotothegreat.deencompanion.core.prayer.HighLatitudeMode
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.quran.RepeatMode
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

    suspend fun setAthkarReminders(enabled: Boolean) = edit { it[Keys.ATHKAR_REMINDERS] = enabled }

    suspend fun setAthkarFontSize(size: Int) =
        edit { it[Keys.ATHKAR_FONT_SIZE] = size.coerceIn(Defaults.ATHKAR_FONT_RANGE) }

    suspend fun setAthkarShowTranslation(show: Boolean) = edit { it[Keys.ATHKAR_TRANSLATION] = show }

    suspend fun setAthkarShowTransliteration(show: Boolean) = edit { it[Keys.ATHKAR_TRANSLITERATION] = show }

    // Smart features
    suspend fun setWeatherEnabled(on: Boolean) = edit { it[Keys.SMART_WEATHER] = on }

    suspend fun setOccasionsEnabled(on: Boolean) = edit { it[Keys.SMART_OCCASIONS] = on }

    suspend fun setTravelEnabled(on: Boolean) = edit { it[Keys.SMART_TRAVEL] = on }

    suspend fun setNaturalEventsEnabled(on: Boolean) = edit { it[Keys.SMART_EVENTS] = on }

    /** Pass 0 to turn "times of calamity" off. */
    suspend fun setCalamityUntil(epochMillis: Long) = edit { it[Keys.CALAMITY_UNTIL] = epochMillis }

    suspend fun setHijriDayStartsAtMaghrib(on: Boolean) = edit { it[Keys.HIJRI_MAGHRIB] = on }

    suspend fun setSafarKm(km: Int) = edit { it[Keys.SAFAR_KM] = km.coerceIn(Defaults.SAFAR_RANGE) }

    // Sounds and early reminders
    suspend fun setAdhanSound(prayer: Prayer, sound: String) = edit { it[adhanSoundKey(prayer)] = sound }

    suspend fun setPreReminderMinutes(minutes: Int) = edit { it[Keys.PRE_REMINDER_MINUTES] = minutes.coerceAtLeast(0) }

    suspend fun setPreReminderPrayers(prayers: Set<Prayer>) = edit {
        it[Keys.PRE_REMINDER_PRAYERS] = prayers.map(Prayer::key).toSet()
    }

    suspend fun setSilenceMinutes(minutes: Int) = edit { it[Keys.SILENCE_MINUTES] = minutes.coerceAtLeast(0) }

    // Quran
    suspend fun setTranslation(id: String) = edit { it[Keys.QURAN_TRANSLATION] = id }

    suspend fun setSecondTranslation(id: String?) = edit {
        if (id.isNullOrBlank()) it.remove(Keys.QURAN_TRANSLATION_SECOND) else it[Keys.QURAN_TRANSLATION_SECOND] = id
    }

    suspend fun setTajweed(on: Boolean) = edit { it[Keys.QURAN_TAJWEED] = on }

    suspend fun setAudioCacheMb(mb: Int) = edit { it[Keys.AUDIO_CACHE_MB] = mb.coerceIn(Defaults.AUDIO_CACHE_RANGE) }

    suspend fun setPlaybackSpeed(speed: Float) = edit { it[Keys.PLAYBACK_SPEED] = speed.coerceIn(0.5f, 2f) }

    suspend fun setContinuousPlayback(on: Boolean) = edit { it[Keys.CONTINUOUS_PLAYBACK] = on }

    suspend fun setRepeat(mode: RepeatMode, count: Int) = edit {
        it[Keys.REPEAT_MODE] = mode.name
        it[Keys.REPEAT_COUNT] = count.coerceIn(1, 20)
    }

    // Accessibility
    suspend fun setSimpleMode(on: Boolean) = edit { it[Keys.SIMPLE_MODE] = on }

    suspend fun setTextScale(scale: Float) = edit { it[Keys.TEXT_SCALE] = scale.coerceIn(Defaults.TEXT_SCALE_RANGE) }

    suspend fun setContrast(mode: ContrastMode) = edit { it[Keys.CONTRAST] = mode.name }

    suspend fun setReduceMotion(mode: ReduceMotion) = edit { it[Keys.REDUCE_MOTION] = mode.name }

    suspend fun setHaptics(on: Boolean) = edit { it[Keys.HAPTICS] = on }

    suspend fun setLargeTouchTargets(on: Boolean) = edit { it[Keys.LARGE_TARGETS] = on }

    // First run and what's new
    suspend fun setOnboardingCompleted(done: Boolean) = edit { it[Keys.ONBOARDING_DONE] = done }

    suspend fun setLastSeenVersionCode(code: Int) = edit { it[Keys.LAST_SEEN_VERSION] = code }

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
    val ATHKAR_REMINDERS = booleanPreferencesKey("athkar_reminders")
    val ATHKAR_FONT_SIZE = intPreferencesKey("athkar_font_size")
    val ATHKAR_TRANSLATION = booleanPreferencesKey("athkar_show_translation")
    val ATHKAR_TRANSLITERATION = booleanPreferencesKey("athkar_show_transliteration")
    val METHOD_AUTO = booleanPreferencesKey("calc_method_auto")
    val HIGH_LATITUDE = stringPreferencesKey("high_latitude_rule")

    // Smart features
    val SMART_WEATHER = booleanPreferencesKey("smart_weather")
    val SMART_OCCASIONS = booleanPreferencesKey("smart_occasions")
    val SMART_TRAVEL = booleanPreferencesKey("smart_travel")
    val SMART_EVENTS = booleanPreferencesKey("smart_natural_events")
    val CALAMITY_UNTIL = longPreferencesKey("calamity_until")
    val HIJRI_MAGHRIB = booleanPreferencesKey("hijri_day_starts_at_maghrib")
    val SAFAR_KM = intPreferencesKey("safar_km")

    // Sounds and early reminders
    val PRE_REMINDER_MINUTES = intPreferencesKey("pre_reminder_minutes")
    val PRE_REMINDER_PRAYERS = stringSetPreferencesKey("pre_reminder_prayers")
    val SILENCE_MINUTES = intPreferencesKey("silence_during_prayer_minutes")

    // Quran
    val QURAN_TRANSLATION = stringPreferencesKey("quran_translation")
    val QURAN_TRANSLATION_SECOND = stringPreferencesKey("quran_translation_secondary")
    val QURAN_TAJWEED = booleanPreferencesKey("quran_tajweed")
    val AUDIO_CACHE_MB = intPreferencesKey("quran_audio_cache_mb")
    val PLAYBACK_SPEED = floatPreferencesKey("quran_playback_speed")
    val REPEAT_MODE = stringPreferencesKey("quran_repeat_mode")
    val REPEAT_COUNT = intPreferencesKey("quran_repeat_count")
    val CONTINUOUS_PLAYBACK = booleanPreferencesKey("quran_continuous_playback")

    // Accessibility
    val SIMPLE_MODE = booleanPreferencesKey("simple_mode")
    val TEXT_SCALE = floatPreferencesKey("text_scale")
    val CONTRAST = stringPreferencesKey("contrast_mode")
    val REDUCE_MOTION = stringPreferencesKey("reduce_motion")
    val HAPTICS = booleanPreferencesKey("haptics_enabled")
    val LARGE_TARGETS = booleanPreferencesKey("large_touch_targets")

    // First run and what's new
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
    val LAST_SEEN_VERSION = intPreferencesKey("last_seen_version_code")
}

internal fun adjustmentKey(prayer: Prayer) = intPreferencesKey("${prayer.key.lowercase()}_adjustment")

/** The sound chosen for one prayer: a muezzin id, "system", "silent", or a URI the user picked. */
internal fun adhanSoundKey(prayer: Prayer) = stringPreferencesKey("${prayer.key.lowercase()}_adhan_sound")

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
    athkarReminders = this[Keys.ATHKAR_REMINDERS] ?: false,
    athkarFontSize = (this[Keys.ATHKAR_FONT_SIZE] ?: Defaults.ATHKAR_FONT_SIZE).coerceIn(Defaults.ATHKAR_FONT_RANGE),
    athkarShowTranslation = this[Keys.ATHKAR_TRANSLATION],
    athkarShowTransliteration = this[Keys.ATHKAR_TRANSLITERATION] ?: false,
    smart = SmartSettings(
        weather = this[Keys.SMART_WEATHER] ?: true,
        occasions = this[Keys.SMART_OCCASIONS] ?: true,
        travel = this[Keys.SMART_TRAVEL] ?: true,
        naturalEvents = this[Keys.SMART_EVENTS] ?: true,
        calamityUntil = this[Keys.CALAMITY_UNTIL] ?: 0L,
        hijriDayStartsAtMaghrib = this[Keys.HIJRI_MAGHRIB] ?: true,
        safarKm = (this[Keys.SAFAR_KM] ?: Defaults.SAFAR_KM).coerceIn(Defaults.SAFAR_RANGE),
    ),
    sounds = SoundSettings(
        adhan = Prayer.obligatory.mapNotNull { prayer -> this[adhanSoundKey(prayer)]?.let { prayer to it } }.toMap(),
        preReminderMinutes = this[Keys.PRE_REMINDER_MINUTES] ?: 0,
        preReminderPrayers = this[Keys.PRE_REMINDER_PRAYERS]?.mapNotNull(Prayer::fromKey)?.toSet()
            ?: Prayer.obligatory.toSet(),
        silenceMinutes = this[Keys.SILENCE_MINUTES] ?: 0,
    ),
    quran = QuranSettings(
        translationId = this[Keys.QURAN_TRANSLATION] ?: Defaults.TRANSLATION,
        secondTranslationId = this[Keys.QURAN_TRANSLATION_SECOND]?.takeIf { it.isNotBlank() },
        tajweed = this[Keys.QURAN_TAJWEED] ?: false,
        audioCacheMb = (this[Keys.AUDIO_CACHE_MB] ?: Defaults.AUDIO_CACHE_MB).coerceIn(Defaults.AUDIO_CACHE_RANGE),
        playbackSpeed = (this[Keys.PLAYBACK_SPEED] ?: 1f).coerceIn(0.5f, 2f),
        repeatMode = enumOrNull<RepeatMode>(this[Keys.REPEAT_MODE]) ?: RepeatMode.OFF,
        repeatCount = (this[Keys.REPEAT_COUNT] ?: 3).coerceIn(1, 20),
        continuousPlayback = this[Keys.CONTINUOUS_PLAYBACK] ?: true,
    ),
    accessibility = AccessibilitySettings(
        simpleMode = this[Keys.SIMPLE_MODE] ?: false,
        textScale = (this[Keys.TEXT_SCALE] ?: 1f).coerceIn(Defaults.TEXT_SCALE_RANGE),
        contrast = enumOrNull<ContrastMode>(this[Keys.CONTRAST]) ?: ContrastMode.SYSTEM,
        reduceMotion = enumOrNull<ReduceMotion>(this[Keys.REDUCE_MOTION]) ?: ReduceMotion.SYSTEM,
        haptics = this[Keys.HAPTICS] ?: true,
        largeTouchTargets = this[Keys.LARGE_TARGETS] ?: false,
    ),
    onboardingCompleted = this[Keys.ONBOARDING_DONE] ?: false,
    lastSeenVersionCode = this[Keys.LAST_SEEN_VERSION] ?: 0,
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
