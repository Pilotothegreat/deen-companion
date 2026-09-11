package com.pilotothegreat.deencompanion.ui.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.alarms.PrayerAlarmScheduler
import com.pilotothegreat.deencompanion.core.prayer.AsrSchool
import com.pilotothegreat.deencompanion.core.prayer.CalculationMethod
import com.pilotothegreat.deencompanion.core.prayer.HighLatitudeMode
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.data.location.LocationRepository
import com.pilotothegreat.deencompanion.data.quran.Reciter
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.data.settings.AppSettings
import com.pilotothegreat.deencompanion.data.settings.IqamaSetting
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.data.settings.ThemeMode
import com.pilotothegreat.deencompanion.data.update.UpdateChecker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val location: LocationRepository,
    private val updates: UpdateChecker,
    private val scheduler: PrayerAlarmScheduler,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val updateState: StateFlow<UpdateChecker.State> = updates.state
    val lastUpdateCheck: StateFlow<Long> =
        updates.lastCheckedAt.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val _isRefreshingLocation = MutableStateFlow(false)
    val isRefreshingLocation: StateFlow<Boolean> = _isRefreshingLocation.asStateFlow()

    private val _locationResults = MutableSharedFlow<LocationRepository.Result>(extraBufferCapacity = 1)
    val locationResults: SharedFlow<LocationRepository.Result> = _locationResults.asSharedFlow()

    val isPlayInstall: Boolean get() = updates.isPlayInstall

    fun canScheduleExactAlarms(): Boolean = scheduler.canScheduleExact()

    fun refreshLocation() {
        if (_isRefreshingLocation.value) return
        viewModelScope.launch {
            _isRefreshingLocation.value = true
            try {
                _locationResults.emit(location.refresh())
            } finally {
                _isRefreshingLocation.value = false
            }
        }
    }

    fun setUseIpLocationFallback(enabled: Boolean) = launch { repository.setUseIpLocationFallback(enabled) }
    fun setMethod(method: CalculationMethod) = launch { repository.setMethod(method) }
    fun setMethodAuto() = launch { repository.setMethodAuto() }
    fun setAsrSchool(school: AsrSchool) = launch { repository.setAsrSchool(school) }
    fun setHighLatitude(mode: HighLatitudeMode) = launch { repository.setHighLatitude(mode) }
    fun setAdjustment(prayer: Prayer, minutes: Int) = launch { repository.setAdjustment(prayer, minutes) }
    fun resetAdjustments() = launch { repository.resetAdjustments() }
    fun setIqama(prayer: Prayer, value: IqamaSetting) = launch { repository.setIqama(prayer, value) }
    fun setHijriAdjustment(days: Int) = launch { repository.setHijriAdjustment(days) }
    fun setNotificationsEnabled(enabled: Boolean) = launch { repository.setNotificationsEnabled(enabled) }
    fun setThemeMode(mode: ThemeMode) = launch { repository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = launch { repository.setDynamicColor(enabled) }
    fun setPureBlack(enabled: Boolean) = launch { repository.setPureBlack(enabled) }
    fun setQuranFontSize(size: Int) = launch { repository.setQuranFontSize(size) }
    fun setReciter(reciter: Reciter) = launch { repository.setReciter(reciter) }

    fun setLanguage(tag: String) = launch {
        repository.setAppLanguage(tag)
        AppLanguage.apply(tag)
        location.relocalizeCity()
    }

    fun checkForUpdates() = launch { updates.check(force = true) }

    fun updateIntent(): Intent = updates.updateIntent()

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
