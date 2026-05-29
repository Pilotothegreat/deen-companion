// FIXED: Add hijriCalendarMethod, notificationVolume, and lastPrayerTimeUpdate preferences
package com.leekleak.trafficlight.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.leekleak.trafficlight.ui.theme.Theme
import com.leekleak.trafficlight.util.PrayerTimeCalculator
import com.leekleak.trafficlight.util.valueOfOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class HijriMethod {
    REGIONAL, UMM_AL_QURA
}

val Context.appPreferences: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppPreferenceRepo (
    private val context: Context,
) {
    private val dataStore get() = context.appPreferences
    private val data get() = dataStore.data

    // Location Settings
    val latitude: Flow<Double> = data.map { it[LATITUDE] ?: 21.3891 }.distinctUntilChanged() // Default Makkah lat
    suspend fun setLatitude(value: Double) = dataStore.edit { it[LATITUDE] = value }

    val longitude: Flow<Double> = data.map { it[LONGITUDE] ?: 39.8579 }.distinctUntilChanged() // Default Makkah lon
    suspend fun setLongitude(value: Double) = dataStore.edit { it[LONGITUDE] = value }

    val cityName: Flow<String> = data.map { it[CITY_NAME] ?: "Makkah, Saudi Arabia" }.distinctUntilChanged()
    suspend fun setCityName(value: String) = dataStore.edit { it[CITY_NAME] = value }

    val timezoneId: Flow<String> = data.map { it[TIMEZONE_ID] ?: "Asia/Riyadh" }.distinctUntilChanged()
    suspend fun setTimezoneId(value: String) = dataStore.edit { it[TIMEZONE_ID] = value }

    // Prayer Time Settings
    val calcMethod: Flow<PrayerTimeCalculator.CalculationMethod> = data.map { prefs ->
        prefs[CALC_METHOD]?.let { valueOfOrNull<PrayerTimeCalculator.CalculationMethod>(it) } ?: PrayerTimeCalculator.CalculationMethod.MWL
    }.distinctUntilChanged()
    suspend fun setCalcMethod(value: PrayerTimeCalculator.CalculationMethod) = dataStore.edit { it[CALC_METHOD] = value.name }

    val asrSchool: Flow<PrayerTimeCalculator.AsrSchool> = data.map { prefs ->
        prefs[ASR_SCHOOL]?.let { valueOfOrNull<PrayerTimeCalculator.AsrSchool>(it) } ?: PrayerTimeCalculator.AsrSchool.STANDARD
    }.distinctUntilChanged()
    suspend fun setAsrSchool(value: PrayerTimeCalculator.AsrSchool) = dataStore.edit { it[ASR_SCHOOL] = value.name }

    // Iqama Offsets (Minutes)
    val fajrIqamaOffset: Flow<Int> = data.map { it[FAJR_IQAMA] ?: 15 }.distinctUntilChanged()
    suspend fun setFajrIqamaOffset(value: Int) = dataStore.edit { it[FAJR_IQAMA] = value }

    val dhuhrIqamaOffset: Flow<Int> = data.map { it[DHUHR_IQAMA] ?: 15 }.distinctUntilChanged()
    suspend fun setDhuhrIqamaOffset(value: Int) = dataStore.edit { it[DHUHR_IQAMA] = value }

    val asrIqamaOffset: Flow<Int> = data.map { it[ASR_IQAMA] ?: 15 }.distinctUntilChanged()
    suspend fun setAsrIqamaOffset(value: Int) = dataStore.edit { it[ASR_IQAMA] = value }

    val maghribIqamaOffset: Flow<Int> = data.map { it[MAGHRIB_IQAMA] ?: 10 }.distinctUntilChanged()
    suspend fun setMaghribIqamaOffset(value: Int) = dataStore.edit { it[MAGHRIB_IQAMA] = value }

    val ishaIqamaOffset: Flow<Int> = data.map { it[ISHA_IQAMA] ?: 15 }.distinctUntilChanged()
    suspend fun setIshaIqamaOffset(value: Int) = dataStore.edit { it[ISHA_IQAMA] = value }

    // Tasbih Count
    val tasbihCount: Flow<Int> = data.map { it[TASBIH_COUNT] ?: 0 }.distinctUntilChanged()
    suspend fun setTasbihCount(value: Int) = dataStore.edit { it[TASBIH_COUNT] = value }

    // Notifications Enabled
    val notification: Flow<Boolean> = data.map { it[NOTIFICATION_ENABLED] ?: true }.distinctUntilChanged()
    suspend fun setNotification(value: Boolean) = dataStore.edit { it[NOTIFICATION_ENABLED] = value }

    // Quran Arabic Font Size
    val quranArabicFontSize: Flow<Int> = data.map { it[QURAN_ARABIC_FONT_SIZE] ?: 24 }.distinctUntilChanged()
    suspend fun setQuranArabicFontSize(value: Int) = dataStore.edit { it[QURAN_ARABIC_FONT_SIZE] = value }

    // Theme Settings
    val theme: Flow<Theme> = data.map { prefs ->
        prefs[THEME]?.let { valueOfOrNull<Theme>(it) } ?: Theme.AutoMaterial
    }.distinctUntilChanged()
    suspend fun setTheme(value: Theme) = dataStore.edit { it[THEME] = value.name }

    // Hadith Favorites
    val favoritedHadiths: Flow<Set<String>> = data.map { it[FAVORITED_HADITHS] ?: emptySet() }.distinctUntilChanged()
    suspend fun toggleHadithFavorite(key: String) = dataStore.edit { prefs ->
        val current = prefs[FAVORITED_HADITHS] ?: emptySet()
        if (current.contains(key)) {
            prefs[FAVORITED_HADITHS] = current - key
        } else {
            prefs[FAVORITED_HADITHS] = current + key
        }
    }

    // App Language ("en" or "ar")
    val appLanguage: Flow<String> = data.map { it[APP_LANGUAGE] ?: "en" }.distinctUntilChanged()
    suspend fun setAppLanguage(value: String) = dataStore.edit { it[APP_LANGUAGE] = value }

    // Hijri Calendar Method
    val hijriCalendarMethod: Flow<HijriMethod> = data.map { prefs ->
        prefs[HIJRI_METHOD]?.let { valueOfOrNull<HijriMethod>(it) } ?: HijriMethod.UMM_AL_QURA
    }.distinctUntilChanged()
    suspend fun setHijriCalendarMethod(value: HijriMethod) = dataStore.edit { it[HIJRI_METHOD] = value.name }

    // Notification Volume (0-100)
    val notificationVolume: Flow<Int> = data.map { it[NOTIFICATION_VOLUME] ?: 80 }.distinctUntilChanged()
    suspend fun setNotificationVolume(value: Int) = dataStore.edit { it[NOTIFICATION_VOLUME] = value }

    // Last Prayer Time Update (epoch millis)
    val lastPrayerTimeUpdate: Flow<Long> = data.map { it[LAST_PRAYER_TIME_UPDATE] ?: 0L }.distinctUntilChanged()
    suspend fun setLastPrayerTimeUpdate(value: Long) = dataStore.edit { it[LAST_PRAYER_TIME_UPDATE] = value }

    private companion object {
        private val LATITUDE = doublePreferencesKey("latitude")
        private val LONGITUDE = doublePreferencesKey("longitude")
        private val CITY_NAME = stringPreferencesKey("city_name")
        private val TIMEZONE_ID = stringPreferencesKey("timezone_id")
        private val CALC_METHOD = stringPreferencesKey("calc_method")
        private val ASR_SCHOOL = stringPreferencesKey("asr_school")
        private val FAJR_IQAMA = intPreferencesKey("fajr_iqama")
        private val DHUHR_IQAMA = intPreferencesKey("dhuhr_iqama")
        private val ASR_IQAMA = intPreferencesKey("asr_iqama")
        private val MAGHRIB_IQAMA = intPreferencesKey("maghrib_iqama")
        private val ISHA_IQAMA = intPreferencesKey("isha_iqama")
        private val TASBIH_COUNT = intPreferencesKey("tasbih_count")
        private val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        private val THEME = stringPreferencesKey("theme")
        private val FAVORITED_HADITHS = stringSetPreferencesKey("favorited_hadiths")
        private val QURAN_ARABIC_FONT_SIZE = intPreferencesKey("quran_arabic_font_size")
        private val APP_LANGUAGE = stringPreferencesKey("app_language")
        private val HIJRI_METHOD = stringPreferencesKey("hijri_method")
        private val NOTIFICATION_VOLUME = intPreferencesKey("notification_volume")
        private val LAST_PRAYER_TIME_UPDATE = longPreferencesKey("last_prayer_time_update")
    }
}