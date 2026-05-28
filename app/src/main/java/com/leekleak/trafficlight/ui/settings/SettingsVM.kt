package com.leekleak.trafficlight.ui.settings

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.ui.theme.Theme
import com.leekleak.trafficlight.util.PrayerTimeCalculator
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

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