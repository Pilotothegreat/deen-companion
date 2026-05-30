package com.pilotothegreat.deencompanion.ui.settings

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.ui.theme.Theme
import com.pilotothegreat.deencompanion.util.PrayerTimeCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsVM(
    private val appPreferenceRepo: AppPreferenceRepo
) : ViewModel() {

    val theme: StateFlow<Theme> = appPreferenceRepo.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Theme.AutoMaterial)

    val notification: StateFlow<Boolean> = appPreferenceRepo.notification
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val calcMethod: StateFlow<PrayerTimeCalculator.CalculationMethod> = appPreferenceRepo.calcMethod
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrayerTimeCalculator.CalculationMethod.MWL)

    val asrSchool: StateFlow<PrayerTimeCalculator.AsrSchool> = appPreferenceRepo.asrSchool
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrayerTimeCalculator.AsrSchool.STANDARD)

    val fajrIqamaOffset: StateFlow<Int> = appPreferenceRepo.fajrIqamaOffset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)

    val dhuhrIqamaOffset: StateFlow<Int> = appPreferenceRepo.dhuhrIqamaOffset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)

    val asrIqamaOffset: StateFlow<Int> = appPreferenceRepo.asrIqamaOffset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)

    val maghribIqamaOffset: StateFlow<Int> = appPreferenceRepo.maghribIqamaOffset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val ishaIqamaOffset: StateFlow<Int> = appPreferenceRepo.ishaIqamaOffset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)

    val quranArabicFontSize: StateFlow<Int> = appPreferenceRepo.quranArabicFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 24)

    val appLanguage: StateFlow<String> = appPreferenceRepo.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ar")

    private val _updateAvailable = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val updateAvailable: StateFlow<String?> = _updateAvailable

    init {
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("https://api.github.com/repos/Pilotothegreat/deen-companion/releases/latest")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", "Deen-Companion-App")

                if (connection.responseCode == 200) {
                    val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(jsonStr)
                    val tagName = json.getString("tag_name")
                    val cleanTag = tagName.replace("v", "").trim()
                    val currentVersion = com.pilotothegreat.deencompanion.BuildConfig.VERSION_NAME.replace("v", "").trim()
                    if (isNewerVersion(currentVersion, cleanTag)) {
                        _updateAvailable.value = tagName
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val length = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until length) {
            val curr = currentParts.getOrElse(i) { 0 }
            val lat = latestParts.getOrElse(i) { 0 }
            if (lat > curr) return true
            if (curr > lat) return false
        }
        return false
    }

    fun setTheme(value: Theme) {
        viewModelScope.launch {
            appPreferenceRepo.setTheme(value)
        }
    }

    fun setNotification(enabled: Boolean) {
        viewModelScope.launch {
            appPreferenceRepo.setNotification(enabled)
        }
    }

    fun setCalcMethod(value: PrayerTimeCalculator.CalculationMethod) {
        viewModelScope.launch {
            appPreferenceRepo.setCalcMethod(value)
        }
    }

    fun setAsrSchool(value: PrayerTimeCalculator.AsrSchool) {
        viewModelScope.launch {
            appPreferenceRepo.setAsrSchool(value)
        }
    }

    fun setFajrIqamaOffset(value: Int) {
        viewModelScope.launch {
            appPreferenceRepo.setFajrIqamaOffset(value)
        }
    }

    fun setDhuhrIqamaOffset(value: Int) {
        viewModelScope.launch {
            appPreferenceRepo.setDhuhrIqamaOffset(value)
        }
    }

    fun setAsrIqamaOffset(value: Int) {
        viewModelScope.launch {
            appPreferenceRepo.setAsrIqamaOffset(value)
        }
    }

    fun setMaghribIqamaOffset(value: Int) {
        viewModelScope.launch {
            appPreferenceRepo.setMaghribIqamaOffset(value)
        }
    }

    fun setIshaIqamaOffset(value: Int) {
        viewModelScope.launch {
            appPreferenceRepo.setIshaIqamaOffset(value)
        }
    }

    fun setQuranArabicFontSize(value: Int) {
        viewModelScope.launch {
            appPreferenceRepo.setQuranArabicFontSize(value)
        }
    }

    fun setAppLanguage(value: String) {
        viewModelScope.launch {
            appPreferenceRepo.setAppLanguage(value)
        }
    }

    fun openAppSettings(activity: Activity?) {
        activity?.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:${activity.packageName}".toUri()
            )
        )
    }

    fun openNotificationChannelSettings(activity: Activity?, channel: String) {
        activity?.startActivity(
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channel)
            }
        )
    }
}
