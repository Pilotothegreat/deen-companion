package com.pilotothegreat.deencompanion.ui.quran

import com.pilotothegreat.deencompanion.core.analytics.UsageEvent
import com.pilotothegreat.deencompanion.data.analytics.Analytics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.core.quran.KhatmaProgress
import com.pilotothegreat.deencompanion.core.time.Ticker
import com.pilotothegreat.deencompanion.data.quran.Bookmark
import com.pilotothegreat.deencompanion.data.quran.KhatmaRepository
import com.pilotothegreat.deencompanion.data.quran.Quran
import com.pilotothegreat.deencompanion.data.quran.QuranRepository
import com.pilotothegreat.deencompanion.data.quran.QuranSearchResults
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class QuranViewModel(
    private val repository: QuranRepository,
    private val khatma: KhatmaRepository,
    settings: SettingsRepository,
) : ViewModel() {

    val quran: StateFlow<Quran?> = flow { emit(repository.quran()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Null while the query is blank. */
    val results: StateFlow<QuranSearchResults?> = _query
        .debounce(250)
        .map { it.trim() }
        .distinctUntilChanged()
        .mapLatest {
            if (it.isEmpty()) return@mapLatest null
            Analytics.record(UsageEvent.QURAN_SEARCHED)
            repository.search(it).also { results ->
                // Counted apart from the plain search: whether people name what they are after is
                // the question the smarter search field was built to answer.
                if (results.destinations.isNotEmpty()) Analytics.record(UsageEvent.QURAN_SEARCH_JUMPED)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val bookmarks: StateFlow<List<Bookmark>> =
        repository.bookmarks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Last page opened in the reader, or 0. */
    val lastReadPage: StateFlow<Int> = settings.settings.map { it.lastReadPage }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Recomputed each day, so the plan's target moves on without the screen being reopened. */
    val khatmaProgress: StateFlow<KhatmaProgress?> = Ticker.days
        .flatMapLatest { khatma.progress(LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun startKhatma(days: Int, fromPage: Int) {
        Analytics.record(UsageEvent.KHATMA_STARTED)
        viewModelScope.launch { khatma.start(days, fromPage, LocalDate.now()) }
    }

    fun cancelKhatma() {
        viewModelScope.launch { khatma.cancel() }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun removeBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.quran().verse(bookmark.surah, bookmark.ayah)?.let { repository.setBookmark(it, false) }
        }
    }
}
