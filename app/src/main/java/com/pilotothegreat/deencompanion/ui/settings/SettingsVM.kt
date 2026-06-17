package com.pilotothegreat.deencompanion.ui.settings

import android.app.Activity
import android.content.Context
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsVM(
    private val context: Context,
    private val appPreferenceRepo: AppPreferenceRepo
) : ViewModel() {

    enum class UpdateState { IDLE, CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, FAILED }

    val theme: StateFlow<Theme> = appPreferenceRepo.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Theme.AutoMaterial)

    val notification: StateFlow<Boolean> = appPreferenceRepo.notification
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val calcMethod: StateFlow<PrayerTimeCalculator.CalculationMethod> = appPreferenceRepo.calcMethod
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrayerTimeCalculator.CalculationMethod.MWL)

    val asrSchool: StateFlow<PrayerTimeCalculator.AsrSchool> = appPreferenceRepo.asrSchool
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrayerTimeCalculator.AsrSchool.STANDARD)

    val fajrIqamaOffset: StateFlow<Int> = appPreferenceRepo.fajrIqamaOffset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)

    val dhuhrIqamaOffset: StateFlow<Int> = appPreferenceRepo.dhuhrIqamaOffset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)

    val asrIqamaOffset: StateFlow<Int> = appPreferenceRepo.asrIqamaOffset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20)

    val maghribIqamaOffset: StateFlow<Int> = appPreferenceRepo.maghribIqamaOffset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val ishaIqamaOffset: StateFlow<Int> = appPreferenceRepo.ishaIqamaOffset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20)

    val fajrIqamaIsFixed: StateFlow<Boolean> = appPreferenceRepo.fajrIqamaIsFixed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val dhuhrIqamaIsFixed: StateFlow<Boolean> = appPreferenceRepo.dhuhrIqamaIsFixed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val asrIqamaIsFixed: StateFlow<Boolean> = appPreferenceRepo.asrIqamaIsFixed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val maghribIqamaIsFixed: StateFlow<Boolean> = appPreferenceRepo.maghribIqamaIsFixed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val ishaIqamaIsFixed: StateFlow<Boolean> = appPreferenceRepo.ishaIqamaIsFixed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val fajrIqamaTime: StateFlow<String> = appPreferenceRepo.fajrIqamaTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "05:15")

    val dhuhrIqamaTime: StateFlow<String> = appPreferenceRepo.dhuhrIqamaTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "12:50")

    val asrIqamaTime: StateFlow<String> = appPreferenceRepo.asrIqamaTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "15:45")

    val maghribIqamaTime: StateFlow<String> = appPreferenceRepo.maghribIqamaTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "18:45")

    val ishaIqamaTime: StateFlow<String> = appPreferenceRepo.ishaIqamaTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "20:15")

    val quranArabicFontSize: StateFlow<Int> = appPreferenceRepo.quranArabicFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 24)

    val appLanguage: StateFlow<String> = appPreferenceRepo.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ar")


    val useIpLocationFallback: StateFlow<Boolean> = appPreferenceRepo.useIpLocationFallback
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val amoledBlackMode: StateFlow<Boolean> = appPreferenceRepo.amoledBlackMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _updateAvailable = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val updateAvailable: StateFlow<String?> = _updateAvailable

    private val _updateState = kotlinx.coroutines.flow.MutableStateFlow(UpdateState.IDLE)
    val updateState: StateFlow<UpdateState> = _updateState

    val lastCheckedTimestamp: StateFlow<Long> = appPreferenceRepo.githubCheckTimestamp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        checkForUpdates(force = false)
    }

    fun checkForUpdates(force: Boolean = false) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val lastChecked = appPreferenceRepo.githubCheckTimestamp.first()
            val cachedLatest = appPreferenceRepo.githubCheckLatestVersion.first()
            val currentVersion = com.pilotothegreat.deencompanion.BuildConfig.VERSION_NAME.replace("v", "").trim()

            // If not forcing and checked recently (less than 1 day ago), use cache
            if (!force && lastChecked != 0L && (now - lastChecked) < 24L * 60L * 60L * 1000L) {
                if (cachedLatest.isNotEmpty() && isNewerVersion(currentVersion, cachedLatest)) {
                    _updateAvailable.value = cachedLatest
                    _updateState.value = UpdateState.UPDATE_AVAILABLE
                } else {
                    _updateState.value = UpdateState.UP_TO_DATE
                }
                return@launch
            }

            _updateState.value = UpdateState.CHECKING
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
                    
                    // Update cache
                    appPreferenceRepo.setGithubCheckTimestamp(now)
                    appPreferenceRepo.setGithubCheckLatestVersion(cleanTag)

                    if (isNewerVersion(currentVersion, cleanTag)) {
                        _updateAvailable.value = tagName
                        _updateState.value = UpdateState.UPDATE_AVAILABLE
                    } else {
                        _updateAvailable.value = null
                        _updateState.value = UpdateState.UP_TO_DATE
                    }
                } else {
                    _updateState.value = UpdateState.FAILED
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _updateState.value = UpdateState.FAILED
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

    fun setAmoledBlackMode(value: Boolean) {
        viewModelScope.launch {
            appPreferenceRepo.setAmoledBlackMode(value)
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
            com.pilotothegreat.deencompanion.services.IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
        }
    }

    fun setDhuhrIqamaOffset(value: Int) {
        viewModelScope.launch {
            appPreferenceRepo.setDhuhrIqamaOffset(value)
            com.pilotothegreat.deencompanion.services.IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
        }
    }

    fun setAsrIqamaOffset(value: Int) {
        viewModelScope.launch {
            appPreferenceRepo.setAsrIqamaOffset(value)
            com.pilotothegreat.deencompanion.services.IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
        }
    }

    fun setMaghribIqamaOffset(value: Int) {
        viewModelScope.launch {
            appPreferenceRepo.setMaghribIqamaOffset(value)
            com.pilotothegreat.deencompanion.services.IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
        }
    }

    fun setIshaIqamaOffset(value: Int) {
        viewModelScope.launch {
            appPreferenceRepo.setIshaIqamaOffset(value)
            com.pilotothegreat.deencompanion.services.IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
        }
    }

    fun setFajrIqamaIsFixed(value: Boolean) {
        viewModelScope.launch {
            appPreferenceRepo.setFajrIqamaIsFixed(value)
            com.pilotothegreat.deencompanion.services.IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
        }
    }

    fun setDhuhrIqamaIsFixed(value: Boolean) {
        viewModelScope.launch {
            appPreferenceRepo.setDhuhrIqamaIsFixed(value)
            com.pilotothegreat.deencompanion.services.IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
        }
    }

    fun setAsrIqamaIsFixed(value: Boolean) {
        viewModelScope.launch {
            appPreferenceRepo.setAsrIqamaIsFixed(value)
            com.pilotothegreat.deencompanion.services.IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
        }
    }

    fun setMaghribIqamaIsFixed(value: Boolean) {
        viewModelScope.launch {
            appPreferenceRepo.setMaghribIqamaIsFixed(value)
            com.pilotothegreat.deencompanion.services.IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
        }
    }

    fun setIshaIqamaIsFixed(value: Boolean) {
        viewModelScope.launch {
            appPreferenceRepo.setIshaIqamaIsFixed(value)
            com.pilotothegreat.deencompanion.services.IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
        }
    }

    fun setFajrIqamaTime(value: String) {
        viewModelScope.launch {
            appPreferenceRepo.setFajrIqamaTime(value)
            com.pilotothegreat.deencompanion.services.IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
        }
    }

    fun setDhuhrIqamaTime(value: String) {
        viewModelScope.launch {
            appPreferenceRepo.setDhuhrIqamaTime(value)
            com.pilotothegreat.deencompanion.services.IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
        }
    }

    fun setAsrIqamaTime(value: String) {
        viewModelScope.launch {
            appPreferenceRepo.setAsrIqamaTime(value)
            com.pilotothegreat.deencompanion.services.IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
        }
    }

    fun setMaghribIqamaTime(value: String) {
        viewModelScope.launch {
            appPreferenceRepo.setMaghribIqamaTime(value)
            com.pilotothegreat.deencompanion.services.IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
        }
    }

    fun setIshaIqamaTime(value: String) {
        viewModelScope.launch {
            appPreferenceRepo.setIshaIqamaTime(value)
            com.pilotothegreat.deencompanion.services.IqamaAlarmManager.scheduleNextIqamaAlarm(context, appPreferenceRepo)
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


    fun setUseIpLocationFallback(value: Boolean) {
        viewModelScope.launch {
            appPreferenceRepo.setUseIpLocationFallback(value)
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
