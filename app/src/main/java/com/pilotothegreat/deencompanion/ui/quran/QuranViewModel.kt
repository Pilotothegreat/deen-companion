package com.pilotothegreat.deencompanion.ui.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.data.quran.Bookmark
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class QuranViewModel(
    private val repository: QuranRepository,
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
        .mapLatest { if (it.isEmpty()) null else repository.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val bookmarks: StateFlow<List<Bookmark>> =
        repository.bookmarks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Last page opened in the reader, or 0. */
    val lastReadPage: StateFlow<Int> = settings.settings.map { it.lastReadPage }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun removeBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.quran().verse(bookmark.surah, bookmark.ayah)?.let { repository.setBookmark(it, false) }
        }
    }
}
