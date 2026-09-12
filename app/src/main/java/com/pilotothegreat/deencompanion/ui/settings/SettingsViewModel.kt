package com.pilotothegreat.deencompanion.ui.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.alarms.PrayerAlarmScheduler
import com.pilotothegreat.deencompanion.core.prayer.AsrSchool
import com.pilotothegreat.deencompanion.core.prayer.CalculationMethod
import com.pilotothegreat.deencompanion.core.prayer.HighLatitudeMode
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.data.location.LocationRepository
import com.pilotothegreat.deencompanion.data.quran.QuranRepository
import com.pilotothegreat.deencompanion.data.quran.Reciter
import com.pilotothegreat.deencompanion.data.quran.TextSource
import com.pilotothegreat.deencompanion.data.quran.TranslationInfo
import com.pilotothegreat.deencompanion.data.settings.AppLanguage
import com.pilotothegreat.deencompanion.data.settings.AppSettings
import com.pilotothegreat.deencompanion.data.settings.IqamaSetting
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.data.settings.ThemeMode
import com.pilotothegreat.deencompanion.data.update.UpdateChecker
import com.pilotothegreat.deencompanion.playback.AudioCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the Quran text and its translation must be credited as, read from the assets themselves. */
data class QuranCredits(val text: TextSource, val translation: TranslationInfo, val edition: String)

class SettingsViewModel(
    private val context: Context,
    private val repository: SettingsRepository,
    private val location: LocationRepository,
    private val updates: UpdateChecker,
    private val scheduler: PrayerAlarmScheduler,
    quran: QuranRepository,
) : ViewModel() {

    val quranCredits: StateFlow<QuranCredits?> = flow { quran.quran().let { emit(QuranCredits(it.textSource, it.translation, it.edition)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val settings: StateFlow<AppSettings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val updateState: StateFlow<UpdateChecker.State> = updates.state
    val lastUpdateCheck: StateFlow<Long> =
        updates.lastCheckedAt.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val isPlayInstall: Boolean get() = updates.isPlayInstall

    private val _audioCacheBytes = MutableStateFlow(0L)
    /** What the recitation cache is holding, refreshed whenever settings are shown. */
    val audioCacheBytes: StateFlow<Long> = _audioCacheBytes.asStateFlow()

    fun canScheduleExactAlarms(): Boolean = scheduler.canScheduleExact()

    fun setAudioCacheMb(mb: Int) = launch {
        repository.setAudioCacheMb(mb)
        AudioCache.setBudgetMb(mb)
    }

fun setPreReminder(minutes: Int) = launch { repository.setPreReminderMinutes(minutes) }

    fun setSilenceMinutes(minutes: Int) = launch { repository.setSilenceMinutes(minutes) }

        fun setNaturalEvents(on: Boolean) = launch { repository.setNaturalEventsEnabled(on) }

    /** Zero turns it off; otherwise it runs for this many days and then lapses on its own. */
    fun setCalamityDays(days: Int) = launch {
        repository.setCalamityUntil(if (days <= 0) 0L else System.currentTimeMillis() + days * 86_400_000L)
    }

    fun setSmartWeather(on: Boolean) = launch { repository.setWeatherEnabled(on) }

    fun setSmartTravel(on: Boolean) = launch { repository.setTravelEnabled(on) }

    /** Home is wherever you are when you say so; travel is measured from it. */
    fun anchorHomeHere() = launch {
        val current = repository.current()
        if (!current.location.isDefault) repository.setHome(current.location.latitude, current.location.longitude)
    }

        fun setContinuousPlayback(on: Boolean) = launch { repository.setContinuousPlayback(on) }

    fun clearAudioCache() = launch {
        withContext(Dispatchers.IO) { AudioCache.clear() }
        refreshAudioCacheSize()
    }

    fun refreshAudioCacheSize() = launch {
        _audioCacheBytes.value = withContext(Dispatchers.IO) { AudioCache.sizeBytes(context) }
    }

    fun setUseIpLocationFallback(enabled: Boolean) = launch { repository.setUseIpLocationFallback(enabled) }
    fun setMethod(method: CalculationMethod) = launch { repository.setMethod(method) }
    fun setMethodAuto() = launch { repository.setMethodAuto() }
    fun setAsrSchool(school: AsrSchool) = launch { repository.setAsrSchool(school) }
    fun setHighLatitude(mode: HighLatitudeMode) = launch { repository.setHighLatitude(mode) }
    fun setAdjustment(prayer: Prayer, minutes: Int) = launch { repository.setAdjustment(prayer, minutes) }
    fun resetAdjustments() = launch { repository.resetAdjustments() }
    fun setIqama(prayer: Prayer, value: IqamaSetting) = launch { repository.setIqama(prayer, value) }
fun setHijriDayStartsAtMaghrib(on: Boolean) = launch { repository.setHijriDayStartsAtMaghrib(on) }

    fun setHijriAdjustment(days: Int) = launch { repository.setHijriAdjustment(days) }
    fun setNotificationsEnabled(enabled: Boolean) = launch { repository.setNotificationsEnabled(enabled) }
    fun setThemeMode(mode: ThemeMode) = launch { repository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = launch { repository.setDynamicColor(enabled) }
    fun setPureBlack(enabled: Boolean) = launch { repository.setPureBlack(enabled) }
    fun setQuranFontSize(size: Int) = launch { repository.setQuranFontSize(size) }
    fun setReciter(reciter: Reciter) = launch { repository.setReciter(reciter) }
    fun setAthkarReminders(enabled: Boolean) = launch { repository.setAthkarReminders(enabled) }
    fun setAthkarFontSize(size: Int) = launch { repository.setAthkarFontSize(size) }
    fun setAthkarShowTranslation(show: Boolean) = launch { repository.setAthkarShowTranslation(show) }
    fun setAthkarShowTransliteration(show: Boolean) = launch { repository.setAthkarShowTransliteration(show) }

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
