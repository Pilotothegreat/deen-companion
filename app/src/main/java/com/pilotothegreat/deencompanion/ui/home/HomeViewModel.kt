package com.pilotothegreat.deencompanion.ui.home

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.pilotothegreat.deencompanion.data.update.UpdateChecker
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.filterNotNull
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
    val inspiration: Inspiration,
)

data class Countdown(val next: NextPrayer, val remaining: Duration)

/** The athkar that fit the time of day, with today's progress. */
data class AthkarNow(val category: AthkarCategory, val progress: DayProgress)

/** Today's ayah with its surah and the mushaf page it sits on. */
data class VerseOfDay(val verse: Verse, val surah: Surah, val page: Int)

sealed interface HomeEvent {
    data object LocationUnavailable : HomeEvent
    data object LocationPermissionMissing : HomeEvent
    data object UpdateAvailable : HomeEvent
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val settings: SettingsRepository,
    private val location: LocationRepository,
    private val athkar: AthkarRepository,
    private val moments: MomentRepository,
    private val quran: QuranRepository,
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
                inspiration = Inspirations.forDate(date),
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Loaded separately so the prayer times never wait for the Quran text. */
    val verseOfDay: StateFlow<VerseOfDay?> = content.filterNotNull()
        .map { it.today.date }
        .distinctUntilChanged()
        .map { date ->
            val ref = DailyVerse.forDate(date)
            val book = quran.quran()
            book.verse(ref.surah, ref.ayah)?.let { VerseOfDay(it, book.surah(ref.surah), book.pageOf(ref.surah, ref.ayah)) }
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
        viewModelScope.launch {
            updates.check()
            if (updates.state.value is UpdateChecker.State.Available) _events.emit(HomeEvent.UpdateAvailable)
        }
    }

    /** Picks up travel and weather since the app was last in front (both throttled). */
    fun onResume() {
        viewModelScope.launch {
            location.onAppOpened()
            moments.onAppOpened()
        }
    }

    fun setTravelling(travelling: Boolean) {
        viewModelScope.launch { moments.setTravelling(travelling) }
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

    fun dismiss(moment: Moment) {
        viewModelScope.launch { moments.dismiss(moment) }
    }

    fun updateIntent(): Intent = updates.updateIntent()
}
