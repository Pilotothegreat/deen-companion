package com.pilotothegreat.deencompanion.ui.home

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.core.athkar.AthkarCategory
import com.pilotothegreat.deencompanion.core.athkar.AthkarSchedule
import com.pilotothegreat.deencompanion.core.athkar.DayProgress
import com.pilotothegreat.deencompanion.core.calendar.HijriCalendar
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
import com.pilotothegreat.deencompanion.core.prayer.NextPrayer
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.prayer.PrayerSchedule
import com.pilotothegreat.deencompanion.core.text.Inspiration
import com.pilotothegreat.deencompanion.core.text.Inspirations
import com.pilotothegreat.deencompanion.data.athkar.AthkarRepository
import com.pilotothegreat.deencompanion.data.location.LocationRepository
import com.pilotothegreat.deencompanion.data.settings.AppSettings
import com.pilotothegreat.deencompanion.data.settings.LocationSource
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.data.update.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate

/** Content that changes only with settings or the date. */
data class HomeContent(
    val settings: AppSettings,
    val today: PrayerSchedule,
    val hijri: HijrahDate?,
    val daysUntilRamadan: Int?,
    val inspiration: Inspiration,
) {
    val showRamadanCard: Boolean
        get() = daysUntilRamadan != null && hijri != null && HijriCalendar.year(hijri) != settings.dismissedRamadanYear
}

data class Countdown(val next: NextPrayer, val remaining: Duration)

/** The athkar that fit the time of day, with today's progress. */
data class AthkarNow(val category: AthkarCategory, val progress: DayProgress)

sealed interface HomeEvent {
    data object LocationUnavailable : HomeEvent
    data object LocationPermissionMissing : HomeEvent
    data object UpdateAvailable : HomeEvent
}

class HomeViewModel(
    private val settings: SettingsRepository,
    private val location: LocationRepository,
    private val athkar: AthkarRepository,
    private val updates: UpdateChecker,
) : ViewModel() {

    private val seconds = flow {
        while (true) {
            emit(Unit)
            delay(1_000 - System.currentTimeMillis() % 1_000)
        }
    }

    private val minutes = seconds.map { System.currentTimeMillis() / 60_000 }.distinctUntilChanged()

    val content: StateFlow<HomeContent?> = combine(settings.settings, seconds) { s, _ -> s to LocalDate.now(s.zone) }
        .distinctUntilChanged()
        .map { (s, date) ->
            HomeContent(
                settings = s,
                today = DaySchedule.forDate(date, s.prayerConfig),
                hijri = HijriCalendar.date(date, s.hijriAdjustment),
                daysUntilRamadan = HijriCalendar.daysUntilRamadan(date, s.hijriAdjustment),
                inspiration = Inspirations.forDate(date),
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val countdown: StateFlow<Countdown?> = combine(settings.settings, seconds) { s, _ ->
        val now = ZonedDateTime.now(s.zone)
        val next = DaySchedule.next(now, s.prayerConfig)
        Countdown(next, Duration.between(now, next.adhan))
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val athkarNow: StateFlow<AthkarNow?> = combine(settings.settings, athkar.progress, minutes) { s, progress, _ ->
        val now = ZonedDateTime.now(s.zone)
        val suggested = AthkarSchedule.suggest(now, DaySchedule.forDate(now.toLocalDate(), s.prayerConfig))
        athkar.library().category(suggested)?.let { AthkarNow(it, progress.on(now.toLocalDate())) }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch { location.onAppOpened() }
        viewModelScope.launch {
            updates.check()
            if (updates.state.value is UpdateChecker.State.Available) _events.emit(HomeEvent.UpdateAvailable)
        }
    }

    /** Picks up travel since the app was last in front (throttled in the repository). */
    fun onResume() {
        viewModelScope.launch { location.onAppOpened() }
    }

    fun refreshLocation() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            // A manually chosen city stays put; pull-to-refresh only redraws.
            if (settings.current().location.source == LocationSource.MANUAL) return@launch
            _isRefreshing.value = true
            try {
                when (location.refresh()) {
                    LocationRepository.Result.UPDATED -> Unit
                    LocationRepository.Result.PERMISSION_MISSING -> _events.emit(HomeEvent.LocationPermissionMissing)
                    LocationRepository.Result.UNAVAILABLE -> _events.emit(HomeEvent.LocationUnavailable)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun setMuted(prayer: Prayer, muted: Boolean) {
        viewModelScope.launch { settings.setPrayerMuted(prayer, muted) }
    }

    fun dismissRamadan(hijriYear: Int) {
        viewModelScope.launch { settings.setDismissedRamadanYear(hijriYear) }
    }

    fun updateIntent(): Intent = updates.updateIntent()
}
