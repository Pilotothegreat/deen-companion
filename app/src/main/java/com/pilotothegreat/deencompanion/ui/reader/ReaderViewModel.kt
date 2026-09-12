package com.pilotothegreat.deencompanion.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.core.prayer.DaySchedule
import com.pilotothegreat.deencompanion.core.prayer.Prayer
import com.pilotothegreat.deencompanion.core.quran.RepeatMode
import com.pilotothegreat.deencompanion.core.time.Ticker
import com.pilotothegreat.deencompanion.data.net.NetError
import com.pilotothegreat.deencompanion.data.quran.KhatmaRepository
import com.pilotothegreat.deencompanion.data.quran.Quran
import com.pilotothegreat.deencompanion.data.quran.QuranRepository
import com.pilotothegreat.deencompanion.data.quran.Reciter
import com.pilotothegreat.deencompanion.data.quran.Verse
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.playback.PlaybackState
import com.pilotothegreat.deencompanion.playback.QuranPlayer
import com.pilotothegreat.deencompanion.ui.navigation.ReaderKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReaderPrefs(
    val fontSize: Int = 28,
    val showTranslation: Boolean = false,
    val reciter: Reciter = Reciter.MISHARY,
)

class ReaderViewModel(
    private val args: ReaderKey,
    private val repository: QuranRepository,
    private val settings: SettingsRepository,
    private val khatma: KhatmaRepository,
    private val player: QuranPlayer,
) : ViewModel() {

    val initialPage: Int = args.page.coerceIn(1, 604)

    /** Ayah highlighted when the reader was opened from search or a bookmark. */
    val targetAyah: Pair<Int, Int>? = if (args.surah > 0 && args.ayah > 0) args.surah to args.ayah else null

    val quran: StateFlow<Quran?> = flow { emit(repository.quran()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val prefs: StateFlow<ReaderPrefs> = settings.settings
        .map { ReaderPrefs(it.quranFontSize, it.showTranslation, it.reciter) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderPrefs())

    val bookmarks: StateFlow<Set<Pair<Int, Int>>> = repository.bookmarks
        .map { list -> list.map { it.surah to it.ayah }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /**
     * True between Isha and Fajr, which the reader uses to take the page down a shade.
     *
     * Reading at night on a screen set for daylight is the most common reason someone puts the
     * mushaf down, and reaching for the system brightness slider mid-ayah is the other. This is
     * deliberately small — a few percent, not a night filter — and it is not a setting, because
     * asking someone whether they would like the screen to stop hurting is not a real question.
     */
    val nightDim: StateFlow<Boolean> = combine(settings.settings, Ticker.minutes) { s, _ ->
        val now = java.time.ZonedDateTime.now(s.zone)
        val today = DaySchedule.forDate(now.toLocalDate(), s.prayerConfig)
        val isha = today.adhan[Prayer.ISHA]
        val fajr = today.adhan[Prayer.FAJR]
        (isha != null && !now.isBefore(isha)) || (fajr != null && now.isBefore(fajr))
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val playback: StateFlow<PlaybackState> = player.state
    val playbackErrors: SharedFlow<NetError> = player.errors

    private var saveJob: Job? = null

    fun onPageSettled(page: Int) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            settings.setLastReadPage(page)
            // The khatma follows the pages actually reached, not a button someone has to remember.
            khatma.onPageRead(page)
        }
    }

    fun toggleTranslation() {
        viewModelScope.launch { settings.setShowTranslation(!prefs.value.showTranslation) }
    }

    fun setBookmark(verse: Verse, bookmarked: Boolean) {
        viewModelScope.launch { repository.setBookmark(verse, bookmarked) }
    }

    fun play(verse: Verse) {
        val quran = quran.value ?: return
        player.play(quran.surah(verse.surah), verse.number, prefs.value.reciter)
    }

    fun togglePlayPause() = player.togglePlayPause()
    fun next() = player.next()
    fun previous() = player.previous()
    fun stop() = player.stop()
    fun setSleepTimer(minutes: Int) = player.setSleepTimer(minutes)

    /** Replays the ayah that failed, which is what the snackbar's retry offers. */
    fun retry() {
        val quran = quran.value ?: return
        val state = player.state.value
        if (!state.isActive) return
        player.play(quran.surah(state.surah), state.ayah.coerceAtLeast(1), state.reciter)
    }

    fun setRepeat(mode: RepeatMode, count: Int) {
        viewModelScope.launch { settings.setRepeat(mode, count) }
    }

    fun setSpeed(speed: Float) {
        viewModelScope.launch { settings.setPlaybackSpeed(speed) }
    }

    fun setReciter(reciter: Reciter) {
        viewModelScope.launch {
            settings.setReciter(reciter)
            if (player.state.value.isActive) player.changeReciter(reciter)
        }
    }
}
