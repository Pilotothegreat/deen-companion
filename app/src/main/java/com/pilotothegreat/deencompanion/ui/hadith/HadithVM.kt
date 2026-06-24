package com.pilotothegreat.deencompanion.ui.hadith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pilotothegreat.deencompanion.database.HadithBookEntity
import com.pilotothegreat.deencompanion.database.HadithEntity
import com.pilotothegreat.deencompanion.database.HadithRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(FlowPreview::class)
class HadithVM(
    private val repository: HadithRepository
) : ViewModel() {

    val books: StateFlow<List<HadithBookEntity>> = repository.getHadithBooks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val undownloadedBooks: StateFlow<List<HadithBookEntity>> = repository.getHadithBooks()
        .map { list -> list.filter { it.hadithCount <= 20 } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favorites: StateFlow<List<HadithEntity>> = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<HadithEntity>>(emptyList())
    val searchResults: StateFlow<List<HadithEntity>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Pagination state for active book
    private val _activeBookId = MutableStateFlow<String?>(null)
    val activeBookId: StateFlow<String?> = _activeBookId.asStateFlow()

    private val _loadedHadiths = MutableStateFlow<List<HadithEntity>>(emptyList())
    val loadedHadiths: StateFlow<List<HadithEntity>> = _loadedHadiths.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20
    var hasMoreToLoad = true
        private set

    private suspend fun executeSearch(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        val rankedResults = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val results = repository.searchHadiths(query)
            val normalizedQuery = normalizeArabic(query)
            val lowerQuery = query.lowercase().trim()

            results.mapNotNull { entity ->
                var score = 0
                val normEnglish = entity.english.lowercase()
                val normArabic = normalizeArabic(entity.arabic)
                val normNarrator = entity.narrator.lowercase()

                if (normArabic.contains(normalizedQuery)) score += 5
                if (normEnglish.contains(lowerQuery)) score += 3
                if (normNarrator.contains(lowerQuery)) score += 2

                if (score > 0) Pair(entity, score) else null
            }.sortedByDescending { it.second }.map { it.first }
        }
        _searchResults.value = rankedResults
        _isSearching.value = false
    }

    init {
        // Debounced search with cancellation of stale queries
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isBlank()) {
                    _searchResults.value = emptyList()
                    _isSearching.value = false
                } else {
                    _isSearching.value = true
                    executeSearch(query)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun selectBook(bookId: String?) {
        _activeBookId.value = bookId
        _loadedHadiths.value = emptyList()
        currentPage = 0
        hasMoreToLoad = true
        if (bookId != null) {
            viewModelScope.launch {
                // 1. Load the preloaded content instantly
                loadNextPage()

                // 2. Check if we need to sync the full collection
                try {
                    val count = repository.getHadithCount(bookId)
                    if (count <= 20) {
                        _isSyncing.value = true
                        // Launch sync as a background job so we don't block the UI
                        val job = launch(kotlinx.coroutines.Dispatchers.IO) {
                            val success = repository.syncFullBook(bookId)
                            if (success) {
                                // Reset pagination and reload the full synced list
                                currentPage = 0
                                hasMoreToLoad = true
                                _loadedHadiths.value = emptyList()
                                loadNextPage()
                            }
                        }
                        job.invokeOnCompletion {
                            _isSyncing.value = false
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error during selectBook background sync: $bookId")
                    _isSyncing.value = false
                }
            }
        }
    }

    fun loadNextPage() {
        val bookId = _activeBookId.value ?: return
        if (!hasMoreToLoad) return

        viewModelScope.launch {
            try {
                val newItems = repository.getHadithsPaged(bookId, currentPage, pageSize)
                if (newItems.isEmpty()) {
                    // Try to fetch full book from CDN if we scroll past preloaded and have none
                    if (currentPage == 0) {
                        val synced = repository.syncFullBook(bookId)
                        if (synced) {
                            val refetched = repository.getHadithsPaged(bookId, 0, pageSize)
                            _loadedHadiths.value = refetched
                            currentPage = 1
                            hasMoreToLoad = refetched.size >= pageSize
                        } else {
                            hasMoreToLoad = false
                        }
                    } else {
                        hasMoreToLoad = false
                    }
                } else {
                    _loadedHadiths.value = _loadedHadiths.value + newItems
                    currentPage++
                    hasMoreToLoad = newItems.size >= pageSize
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading next page for book: $bookId")
            }
        }
    }

    fun toggleFavorite(hadithId: String, currentFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(hadithId, !currentFav)
            
            // Map over _loadedHadiths and replace the toggled item in place
            _loadedHadiths.value = _loadedHadiths.value.map {
                if (it.id == hadithId) it.copy(isFavorite = !currentFav) else it
            }
            
            // Map over _searchResults and replace the toggled item in place
            _searchResults.value = _searchResults.value.map {
                if (it.id == hadithId) it.copy(isFavorite = !currentFav) else it
            }
        }
    }

    fun forceSyncBook(bookId: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.syncFullBook(bookId)
            } catch (e: Exception) {
                Timber.e(e, "Error manually syncing book: $bookId")
            } finally {
                _isSyncing.value = false
                _loadedHadiths.value = emptyList()
                currentPage = 0
                hasMoreToLoad = true
                loadNextPage()
            }
        }
    }

    fun syncAllUndownloaded() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val list = undownloadedBooks.value
                for (book in list) {
                    val success = repository.syncFullBook(book.id)
                    if (success) {
                        val currentQuery = _searchQuery.value
                        if (currentQuery.isNotBlank()) {
                            executeSearch(currentQuery)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error syncing all undownloaded books")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /**
     * Cleans up Arabic text by removing diacritics and normalizing key letters
     */
    private fun normalizeArabic(text: String): String {
        var normalized = text
        // Remove diacritics (tashkeel)
        val diacritics = "[\\u064B-\\u065F\\u0670]".toRegex()
        normalized = normalized.replace(diacritics, "")
        // Normalize Alefs (أ, إ, آ -> ا)
        normalized = normalized.replace("[أإآ]".toRegex(), "ا")
        // Normalize Yeh/Alif Maksura (ى -> ي)
        normalized = normalized.replace("ى".toRegex(), "ي")
        // Normalize Teh Marbuta (ة -> ه)
        normalized = normalized.replace("ة".toRegex(), "ه")
        return normalized
    }
}
