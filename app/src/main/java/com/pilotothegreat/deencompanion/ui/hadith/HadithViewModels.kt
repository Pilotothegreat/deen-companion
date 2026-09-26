package com.pilotothegreat.deencompanion.ui.hadith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.data.hadith.Hadith
import com.pilotothegreat.deencompanion.data.hadith.HadithBook
import com.pilotothegreat.deencompanion.data.hadith.HadithRepository
import com.pilotothegreat.deencompanion.ui.navigation.HadithBookKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HadithViewModel(private val repository: HadithRepository) : ViewModel() {

    val books: StateFlow<List<HadithBook>?> =
        repository.books.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val downloads: StateFlow<Map<String, Float?>> = repository.downloads
    val downloadFailures: SharedFlow<String> = repository.failures
    val favorites: StateFlow<List<Hadith>> =
        repository.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favoriteIds: StateFlow<Set<String>> =
        repository.favoriteIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Null while the query is blank; re-runs when a download adds more hadiths. */
    val results: StateFlow<List<Hadith>?> = combine(_query.debounce(300).map { it.trim() }, repository.books) { q, books ->
        q to books.sumOf { it.hadithCount }
    }
        .distinctUntilChanged()
        .mapLatest { (q, _) -> if (q.isEmpty()) null else repository.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { repository.ensureSeeded() }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun download(bookId: String) = repository.startDownload(bookId)

    fun cancelDownload(bookId: String) = repository.cancelDownload(bookId)

    fun setFavorite(hadithId: String, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(hadithId, favorite) }
    }
}

class HadithBookViewModel(
    args: HadithBookKey,
    private val repository: HadithRepository,
) : ViewModel() {

    val bookId = args.bookId

    val book: StateFlow<HadithBook?> = repository.books
        .map { books -> books.firstOrNull { it.info.id == bookId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val download: StateFlow<Map<String, Float?>> = repository.downloads
    val downloadFailures: SharedFlow<String> = repository.failures
    val favoriteIds: StateFlow<Set<String>> =
        repository.favoriteIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _hadiths = MutableStateFlow<List<Hadith>>(emptyList())
    val hadiths: StateFlow<List<Hadith>> = _hadiths.asStateFlow()

    private val _endReached = MutableStateFlow(false)
    val endReached: StateFlow<Boolean> = _endReached.asStateFlow()

    private var nextPage = 0
    private var loading = false

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            // Reload from the start whenever the collection grows (e.g. after a download).
            repository.books.map { books -> books.firstOrNull { it.info.id == bookId }?.hadithCount }
                .distinctUntilChanged()
                .collect { reload() }
        }
    }

    fun loadMore() {
        if (loading || _endReached.value) return
        loading = true
        viewModelScope.launch {
            try {
                val page = repository.page(bookId, nextPage, PAGE_SIZE)
                _hadiths.value = _hadiths.value + page
                nextPage++
                _endReached.value = page.size < PAGE_SIZE
            } finally {
                loading = false
            }
        }
    }

    fun startDownload() = repository.startDownload(bookId)

    fun setFavorite(hadithId: String, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(hadithId, favorite) }
    }

    private fun reload() {
        nextPage = 0
        _hadiths.value = emptyList()
        _endReached.value = false
        loading = false
        loadMore()
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}
