package com.pilotothegreat.deencompanion.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.core.analytics.UsageEvent
import com.pilotothegreat.deencompanion.core.athkar.AthkarCategory
import com.pilotothegreat.deencompanion.core.athkar.AthkarSchedule
import com.pilotothegreat.deencompanion.core.athkar.DayProgress
import com.pilotothegreat.deencompanion.core.calendar.HijriCalendar
import com.pilotothegreat.deencompanion.core.moment.Moment
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
import com.pilotothegreat.deencompanion.core.prayer.NextPrayer
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.prayer.PrayerSchedule
import com.pilotothegreat.deencompanion.core.text.DailyVerse
import com.pilotothegreat.deencompanion.core.text.Inspiration
import com.pilotothegreat.deencompanion.core.text.Inspirations
import com.pilotothegreat.deencompanion.core.time.Ticker
import com.pilotothegreat.deencompanion.data.analytics.Analytics
import com.pilotothegreat.deencompanion.data.athkar.AthkarRepository
import com.pilotothegreat.deencompanion.data.location.LocationRepository
import com.pilotothegreat.deencompanion.data.moment.MomentRepository
import com.pilotothegreat.deencompanion.data.prayer.PrayerLogRepository
import com.pilotothegreat.deencompanion.data.quran.QuranRepository
import com.pilotothegreat.deencompanion.data.quran.Surah
import com.pilotothegreat.deencompanion.data.quran.Verse
import com.pilotothegreat.deencompanion.data.settings.AppSettings
import com.pilotothegreat.deencompanion.data.settings.LocationSource
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.core.update.UpdateUrgency
import com.pilotothegreat.deencompanion.data.update.UpdateChecker
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Content that changes only with settings or the date. */
data class HomeContent(
    val settings: AppSettings,
    val today: PrayerSchedule,
    val hijri: HijrahDate?,
)

data class Countdown(val next: NextPrayer, val remaining: Duration)

/** The athkar that fit the time of day, with today's progress. */
data class AthkarNow(val category: AthkarCategory, val progress: DayProgress)

sealed interface HomeEvent {
    data object LocationUnavailable : HomeEvent
    data object LocationPermissionMissing : HomeEvent
    /** An update worth a pop-up; [insistent] once it has waited long enough or matters enough. */
    data class UpdateAvailable(val insistent: Boolean) : HomeEvent
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val settings: SettingsRepository,
    private val location: LocationRepository,
    private val athkar: AthkarRepository,
    private val moments: MomentRepository,
    private val updates: UpdateChecker,
    private val prayerLog: PrayerLogRepository,
) : ViewModel() {

    private val seconds = Ticker.seconds
    private val minutes = Ticker.minutes

    /**
     * Which prayers have been marked prayed today, and how many of the last thirty days had any
     * mark at all.
     *
     * Every "Prayed" tap has been written to the database since 1.8.0 and read by nothing, so the
     * record existed and the reader could never see it. The count is deliberately gentle: days on
     * which anything was marked, not a tally of five-out-of-five, because an app that grades
     * someone's prayers has stopped being useful and started being a nag.
     */
    val prayedToday: StateFlow<Set<Prayer>> = settings.settings
        .map { LocalDate.now(it.zone) }
        .distinctUntilChanged()
        .flatMapLatest { prayerLog.today(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val daysObserved: StateFlow<Int> = combine(settings.settings, Ticker.days) { s, _ -> LocalDate.now(s.zone) }
        .distinctUntilChanged()
        .map { prayerLog.daysObserved(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setPrayed(prayer: Prayer, prayed: Boolean) {
        if (prayed) Analytics.record(UsageEvent.PRAYER_MARKED)
        viewModelScope.launch {
            val date = LocalDate.now(settings.current().zone)
            if (prayed) prayerLog.record(prayer, date, System.currentTimeMillis()) else prayerLog.undo(prayer, date)
        }
    }

    val content: StateFlow<HomeContent?> = combine(settings.settings, seconds) { s, _ -> s to LocalDate.now(s.zone) }
        .distinctUntilChanged()
        .map { (s, date) ->
            HomeContent(
                settings = s,
                today = DaySchedule.forDate(date, s.prayerConfig),
                hijri = HijriCalendar.date(date, s.hijriAdjustment),
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

    val athkarNow: StateFlow<AthkarNow?> = combine(settings.settings, athkar.progress, moments.suggestedAthkar) { s, progress, suggested ->
        val now = ZonedDateTime.now(s.zone)
        athkar.library().category(suggested)?.let { AthkarNow(it, progress.on(now.toLocalDate())) }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * What Today shows above the prayer times: whatever the engine ranks highest right now, capped
     * so the screen can never turn into a column of cards.
     */
    val cards: StateFlow<List<Moment>> = moments.todayCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            location.onAppOpened()
            moments.onAppOpened()
        }
        viewModelScope.launch { offerUpdate() }
    }

    /** Picks up travel and weather since the app was last in front (both throttled). */
    fun onResume() {
        viewModelScope.launch {
            location.onAppOpened()
            moments.onAppOpened()
            offerUpdate()
        }
    }

    /** Whether this launch has already said something about the update; it says it once. */
    private var offeredThisLaunch = false

    /**
     * Checks for an update and says so as loudly as it has earned.
     *
     * A Play update that has already downloaded is finished here rather than waiting for the app to
     * be killed, which on a phone that keeps it open for weeks may be never.
     */
    private suspend fun offerUpdate() {
        updates.check()
        if (updates.completeDownloadedPlayUpdate()) return
        if (offeredThisLaunch) return
        val urgency = updates.urgencyForLaunch()
        if (urgency == UpdateUrgency.QUIET) return
        offeredThisLaunch = true
        Analytics.record(UsageEvent.UPDATE_OFFERED)
        _events.emit(HomeEvent.UpdateAvailable(insistent = urgency.promptsOnLaunch))
    }

    fun setTravelling(travelling: Boolean) {
        viewModelScope.launch { moments.setTravelling(travelling) }
    }

    fun refreshLocation() {
        Analytics.record(UsageEvent.REFRESH_PRESSED)
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

    fun dismiss(moment: Moment) {
        viewModelScope.launch { moments.dismiss(moment) }
    }

}
