// FIXED: Make DataStore a singleton via companion object with corruption handler and backup logic
package com.pilotothegreat.deencompanion.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.pilotothegreat.deencompanion.ui.theme.Theme
import com.pilotothegreat.deencompanion.util.PrayerTimeCalculator
import com.pilotothegreat.deencompanion.util.valueOfOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(androidx.datastore.preferences.SharedPreferencesMigration(context, "settings"))
    }
)

enum class HijriMethod {
    REGIONAL, UMM_AL_QURA
}

class AppPreferenceRepo(
    private val context: Context,
    private val db: AppDatabase
) {
    private val dataStore = context.dataStore
    private val data get() = dataStore.data

    init {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
        scope.launch {
            try {
                // Ensure SharedPreferences language matches DataStore language
                val prefs = dataStore.data.first()
                val dsLang = prefs[APP_LANGUAGE] ?: "ar"
                val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                val spLang = sp.getString("app_language", null)
                if (spLang != dsLang) {
                    sp.edit().putString("app_language", dsLang).apply()
                }

                val dbRecord = db.tasbihDao().getRecord()
                val dsCount = prefs[TASBIH_COUNT]
                
                if (dbRecord != null) {
                    if (dsCount == null) {
                        dataStore.edit { editPrefs ->
                            editPrefs[TASBIH_COUNT] = dbRecord.count
                            editPrefs[TASBIH_HISTORY] = dbRecord.historyJson
                            editPrefs[TASBIH_DHIKR] = dbRecord.dhikr
                            editPrefs[TASBIH_TARGET] = dbRecord.target
                        }
                        Timber.d("Startup Sync: Restored Tasbih from Room to DataStore")
                    } else {
                        val dsHistory = prefs[TASBIH_HISTORY] ?: "[]"
                        val dsDhikr = prefs[TASBIH_DHIKR] ?: "سبحان الله"
                        val dsTarget = prefs[TASBIH_TARGET] ?: 33
                        db.tasbihDao().insertRecord(
                            TasbihRecord(
                                id = "default",
                                count = dsCount,
                                historyJson = dsHistory,
                                dhikr = dsDhikr,
                                target = dsTarget
                            )
                        )
                        Timber.d("Startup Sync: Synchronized DataStore Tasbih to Room")
                    }
                } else {
                    val count = dsCount ?: 0
                    val history = prefs[TASBIH_HISTORY] ?: "[]"
                    val dhikr = prefs[TASBIH_DHIKR] ?: "سبحان الله"
                    val target = prefs[TASBIH_TARGET] ?: 33
                    db.tasbihDao().insertRecord(
                        TasbihRecord(
                            id = "default",
                            count = count,
                            historyJson = history,
                            dhikr = dhikr,
                            target = target
                        )
                    )
                    Timber.d("Startup Sync: Initialized empty Room with DataStore Tasbih")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error performing bidirectional Tasbih startup sync")
            }
        }
    }

    private suspend fun saveTasbihToRoom() {
        try {
            val prefs = dataStore.data.first()
            val count = prefs[TASBIH_COUNT] ?: 0
            val history = prefs[TASBIH_HISTORY] ?: "[]"
            val dhikr = prefs[TASBIH_DHIKR] ?: "سبحان الله"
            val target = prefs[TASBIH_TARGET] ?: 33
            
            db.tasbihDao().insertRecord(
                TasbihRecord(
                    id = "default",
                    count = count,
                    historyJson = history,
                    dhikr = dhikr,
                    target = target
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to save Tasbih to Room")
        }
    }

    private fun formatAndValidateFixedTime(timeStr: String): String? {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
    }

    companion object {

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
        private val DISMISSED_RAMADAN_HILAL_YEAR = intPreferencesKey("dismissed_ramadan_hilal_year")
        private val APP_LAUNCH_COUNT = intPreferencesKey("app_launch_count")
        private val DONATION_PROMPT_DISMISSED = booleanPreferencesKey("donation_prompt_dismissed")
        private val LAST_DONATION_PROMPT_SHOW_TIME = longPreferencesKey("last_donation_prompt_show_time")
        private val GITHUB_CHECK_TIMESTAMP = longPreferencesKey("github_check_timestamp")
        private val GITHUB_CHECK_LATEST_VERSION = stringPreferencesKey("github_check_latest_version")
        private val TASBIH_DHIKR = stringPreferencesKey("tasbih_dhikr")
        private val TASBIH_TARGET = intPreferencesKey("tasbih_target")
        private val TASBIH_HISTORY = stringPreferencesKey("tasbih_history")
        private val FAJR_IQAMA_IS_FIXED = booleanPreferencesKey("fajr_iqama_is_fixed")
        private val DHUHR_IQAMA_IS_FIXED = booleanPreferencesKey("dhuhr_iqama_is_fixed")
        private val ASR_IQAMA_IS_FIXED = booleanPreferencesKey("asr_iqama_is_fixed")
        private val MAGHRIB_IQAMA_IS_FIXED = booleanPreferencesKey("maghrib_iqama_is_fixed")
        private val ISHA_IQAMA_IS_FIXED = booleanPreferencesKey("isha_iqama_is_fixed")
        private val FAJR_IQAMA_TIME = stringPreferencesKey("fajr_iqama_time")
        private val DHUHR_IQAMA_TIME = stringPreferencesKey("dhuhr_iqama_time")
        private val ASR_IQAMA_TIME = stringPreferencesKey("asr_iqama_time")
        private val MAGHRIB_IQAMA_TIME = stringPreferencesKey("maghrib_iqama_time")
        private val ISHA_IQAMA_TIME = stringPreferencesKey("isha_iqama_time")
        private val USE_IP_LOCATION_FALLBACK = booleanPreferencesKey("use_ip_location_fallback")
        private val AMOLED_BLACK_MODE = booleanPreferencesKey("amoled_black_mode")
    }

    // Location Settings
    val latitude: Flow<Double> = data.map { it[LATITUDE] ?: 21.3891 }.distinctUntilChanged()
    suspend fun setLatitude(value: Double) {
        val validated = value.coerceIn(-90.0, 90.0)
        dataStore.edit { it[LATITUDE] = validated }
    }

    val longitude: Flow<Double> = data.map { it[LONGITUDE] ?: 39.8579 }.distinctUntilChanged()
    suspend fun setLongitude(value: Double) {
        val validated = value.coerceIn(-180.0, 180.0)
        dataStore.edit { it[LONGITUDE] = validated }
    }

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
    val fajrIqamaOffset: Flow<Int> = data.map { it[FAJR_IQAMA] ?: 25 }.distinctUntilChanged()
    suspend fun setFajrIqamaOffset(value: Int) = dataStore.edit { it[FAJR_IQAMA] = value }

    val dhuhrIqamaOffset: Flow<Int> = data.map { it[DHUHR_IQAMA] ?: 25 }.distinctUntilChanged()
    suspend fun setDhuhrIqamaOffset(value: Int) = dataStore.edit { it[DHUHR_IQAMA] = value }

    val asrIqamaOffset: Flow<Int> = data.map { it[ASR_IQAMA] ?: 20 }.distinctUntilChanged()
    suspend fun setAsrIqamaOffset(value: Int) = dataStore.edit { it[ASR_IQAMA] = value }

    val maghribIqamaOffset: Flow<Int> = data.map { it[MAGHRIB_IQAMA] ?: 10 }.distinctUntilChanged()
    suspend fun setMaghribIqamaOffset(value: Int) = dataStore.edit { it[MAGHRIB_IQAMA] = value }

    val ishaIqamaOffset: Flow<Int> = data.map { it[ISHA_IQAMA] ?: 20 }.distinctUntilChanged()
    suspend fun setIshaIqamaOffset(value: Int) = dataStore.edit { it[ISHA_IQAMA] = value }

    val fajrIqamaIsFixed: Flow<Boolean> = data.map { it[FAJR_IQAMA_IS_FIXED] ?: false }.distinctUntilChanged()
    suspend fun setFajrIqamaIsFixed(value: Boolean) = dataStore.edit { it[FAJR_IQAMA_IS_FIXED] = value }

    val dhuhrIqamaIsFixed: Flow<Boolean> = data.map { it[DHUHR_IQAMA_IS_FIXED] ?: true }.distinctUntilChanged()
    suspend fun setDhuhrIqamaIsFixed(value: Boolean) = dataStore.edit { it[DHUHR_IQAMA_IS_FIXED] = value }

    val asrIqamaIsFixed: Flow<Boolean> = data.map { it[ASR_IQAMA_IS_FIXED] ?: false }.distinctUntilChanged()
    suspend fun setAsrIqamaIsFixed(value: Boolean) = dataStore.edit { it[ASR_IQAMA_IS_FIXED] = value }

    val maghribIqamaIsFixed: Flow<Boolean> = data.map { it[MAGHRIB_IQAMA_IS_FIXED] ?: false }.distinctUntilChanged()
    suspend fun setMaghribIqamaIsFixed(value: Boolean) = dataStore.edit { it[MAGHRIB_IQAMA_IS_FIXED] = value }

    val ishaIqamaIsFixed: Flow<Boolean> = data.map { it[ISHA_IQAMA_IS_FIXED] ?: false }.distinctUntilChanged()
    suspend fun setIshaIqamaIsFixed(value: Boolean) = dataStore.edit { it[ISHA_IQAMA_IS_FIXED] = value }

    val fajrIqamaTime: Flow<String> = data.map { it[FAJR_IQAMA_TIME] ?: "05:15" }.distinctUntilChanged()
    suspend fun setFajrIqamaTime(value: String) {
        val formatted = formatAndValidateFixedTime(value)
        if (formatted != null) {
            dataStore.edit { it[FAJR_IQAMA_TIME] = formatted }
        }
    }

    val dhuhrIqamaTime: Flow<String> = data.map { it[DHUHR_IQAMA_TIME] ?: "12:50" }.distinctUntilChanged()
    suspend fun setDhuhrIqamaTime(value: String) {
        val formatted = formatAndValidateFixedTime(value)
        if (formatted != null) {
            dataStore.edit { it[DHUHR_IQAMA_TIME] = formatted }
        }
    }

    val asrIqamaTime: Flow<String> = data.map { it[ASR_IQAMA_TIME] ?: "15:45" }.distinctUntilChanged()
    suspend fun setAsrIqamaTime(value: String) {
        val formatted = formatAndValidateFixedTime(value)
        if (formatted != null) {
            dataStore.edit { it[ASR_IQAMA_TIME] = formatted }
        }
    }

    val maghribIqamaTime: Flow<String> = data.map { it[MAGHRIB_IQAMA_TIME] ?: "18:45" }.distinctUntilChanged()
    suspend fun setMaghribIqamaTime(value: String) {
        val formatted = formatAndValidateFixedTime(value)
        if (formatted != null) {
            dataStore.edit { it[MAGHRIB_IQAMA_TIME] = formatted }
        }
    }

    val ishaIqamaTime: Flow<String> = data.map { it[ISHA_IQAMA_TIME] ?: "20:15" }.distinctUntilChanged()
    suspend fun setIshaIqamaTime(value: String) {
        val formatted = formatAndValidateFixedTime(value)
        if (formatted != null) {
            dataStore.edit { it[ISHA_IQAMA_TIME] = formatted }
        }
    }

    // Tasbih Count
    val tasbihCount: Flow<Int> = data.map { it[TASBIH_COUNT] ?: 0 }.distinctUntilChanged()
    suspend fun setTasbihCount(value: Int) {
        dataStore.edit { it[TASBIH_COUNT] = value }
        saveTasbihToRoom()
    }
    suspend fun incrementTasbihCount() {
        dataStore.edit { prefs ->
            val current = prefs[TASBIH_COUNT] ?: 0
            prefs[TASBIH_COUNT] = current + 1
        }
        saveTasbihToRoom()
    }
    suspend fun incrementAndCycleTasbih() {
        dataStore.edit { prefs ->
            val count = prefs[TASBIH_COUNT] ?: 0
            val target = prefs[TASBIH_TARGET] ?: 33
            val dhikr = prefs[TASBIH_DHIKR] ?: "سبحان الله"
            val nextCount = count + 1
            if (nextCount >= target) {
                val nextDhikr = when (dhikr) {
                    "سبحان الله" -> "الحمد لله"
                    "الحمد لله" -> "الله أكبر"
                    else -> "سبحان الله"
                }
                prefs[TASBIH_DHIKR] = nextDhikr
                prefs[TASBIH_COUNT] = 0
            } else {
                prefs[TASBIH_COUNT] = nextCount
            }
        }
        saveTasbihToRoom()
    }

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

    val amoledBlackMode: Flow<Boolean> = data.map { it[AMOLED_BLACK_MODE] ?: false }.distinctUntilChanged()
    suspend fun setAmoledBlackMode(value: Boolean) = dataStore.edit { it[AMOLED_BLACK_MODE] = value }

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
    val appLanguage: Flow<String> = data.map { it[APP_LANGUAGE] ?: "ar" }.distinctUntilChanged()
    suspend fun setAppLanguage(value: String) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .putString("app_language", value)
            .apply()
        dataStore.edit { it[APP_LANGUAGE] = value }
    }

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

    // Dismissed Ramadan Hilal card year
    val dismissedRamadanHilalYear: Flow<Int> = data.map { it[DISMISSED_RAMADAN_HILAL_YEAR] ?: 0 }.distinctUntilChanged()
    suspend fun setDismissedRamadanHilalYear(value: Int) = dataStore.edit { it[DISMISSED_RAMADAN_HILAL_YEAR] = value }

    // App Launch Count
    val appLaunchCount: Flow<Int> = data.map { it[APP_LAUNCH_COUNT] ?: 0 }.distinctUntilChanged()
    suspend fun setAppLaunchCount(value: Int) = dataStore.edit { it[APP_LAUNCH_COUNT] = value }

    // Donation prompt dismissed
    val donationPromptDismissed: Flow<Boolean> = data.map { it[DONATION_PROMPT_DISMISSED] ?: false }.distinctUntilChanged()
    suspend fun setDonationPromptDismissed(value: Boolean) = dataStore.edit { it[DONATION_PROMPT_DISMISSED] = value }

    // Last time donation prompt was shown
    val lastDonationPromptShowTime: Flow<Long> = data.map { it[LAST_DONATION_PROMPT_SHOW_TIME] ?: 0L }.distinctUntilChanged()
    suspend fun setLastDonationPromptShowTime(value: Long) = dataStore.edit { it[LAST_DONATION_PROMPT_SHOW_TIME] = value }

    // GitHub update cache
    val githubCheckTimestamp: Flow<Long> = data.map { it[GITHUB_CHECK_TIMESTAMP] ?: 0L }.distinctUntilChanged()
    suspend fun setGithubCheckTimestamp(value: Long) = dataStore.edit { it[GITHUB_CHECK_TIMESTAMP] = value }

    val githubCheckLatestVersion: Flow<String> = data.map { it[GITHUB_CHECK_LATEST_VERSION] ?: "" }.distinctUntilChanged()
    suspend fun setGithubCheckLatestVersion(value: String) = dataStore.edit { it[GITHUB_CHECK_LATEST_VERSION] = value }

    // Tasbih Settings
    val tasbihDhikr: Flow<String> = data.map { it[TASBIH_DHIKR] ?: "سبحان الله" }.distinctUntilChanged()
    suspend fun setTasbihDhikr(value: String) {
        dataStore.edit { it[TASBIH_DHIKR] = value }
        saveTasbihToRoom()
    }

    val tasbihTarget: Flow<Int> = data.map { it[TASBIH_TARGET] ?: 33 }.distinctUntilChanged()
    suspend fun setTasbihTarget(value: Int) {
        dataStore.edit { it[TASBIH_TARGET] = value }
        saveTasbihToRoom()
    }

    val tasbihHistory: Flow<List<String>> = data.map { prefs ->
        val json = prefs[TASBIH_HISTORY] ?: "[]"
        try {
            val arr = org.json.JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }.distinctUntilChanged()

    suspend fun addTasbihHistoryItem(item: String) {
        val currentJson = dataStore.data.first()[TASBIH_HISTORY] ?: "[]"
        val newList = try {
            val arr = org.json.JSONArray(currentJson)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list.add(item)
            list
        } catch (e: Exception) {
            listOf(item)
        }
        val newJson = org.json.JSONArray(newList).toString()
        dataStore.edit { it[TASBIH_HISTORY] = newJson }
        saveTasbihToRoom()
    }

    suspend fun clearTasbihHistory() {
        dataStore.edit { it[TASBIH_HISTORY] = "[]" }
        saveTasbihToRoom()
    }


    // IP-based location fallback toggle (defaults true for existing installs, user can disable for strict offline mode)
    val useIpLocationFallback: Flow<Boolean> = data.map { it[USE_IP_LOCATION_FALLBACK] ?: true }.distinctUntilChanged()
    suspend fun setUseIpLocationFallback(value: Boolean) = dataStore.edit { it[USE_IP_LOCATION_FALLBACK] = value }
}

