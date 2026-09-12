package com.pilotothegreat.deencompanion.ui.hadith

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.data.hadith.Hadith
import com.pilotothegreat.deencompanion.data.hadith.HadithBook
import com.pilotothegreat.deencompanion.data.hadith.HadithBooks
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.components.EmptyState
import com.pilotothegreat.deencompanion.ui.components.LoadingBox
import com.pilotothegreat.deencompanion.ui.components.SearchField
import com.pilotothegreat.deencompanion.ui.components.ShapeBadge
import com.pilotothegreat.deencompanion.ui.navigation.LocalBottomBarPadding
import com.pilotothegreat.deencompanion.ui.navigation.HadithBookKey
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun HadithScreen(onOpenBook: (String) -> Unit, viewModel: HadithViewModel = koinViewModel()) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val locale = currentLocale()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel.downloadFailures.collect { id ->
            snackbar.showSnackbar(resources.getString(R.string.download_failed, HadithBooks.info(id)?.name(locale) ?: id))
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.hadith)) }) },
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = LocalBottomBarPadding.current)) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = stringResource(R.string.search_hadith_hint),
                modifier = Modifier.padding(horizontal = Spacing.large, vertical = Spacing.small),
            )
            val loadedBooks = books ?: run {
                LoadingBox()
                return@Column
            }
            val bookNames = loadedBooks.associate { it.info.id to it.info.name(locale) }
            if (query.isNotBlank()) {
                SearchResults(results, bookNames, loadedBooks.any { !it.isComplete }, favoriteIds, viewModel::setFavorite)
                return@Column
            }
            SecondaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.collections_tab)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.favorites_tab)) })
            }
            when (tab) {
                0 -> BookList(loadedBooks, downloads, onOpenBook, viewModel::download, viewModel::cancelDownload)
                else -> FavoriteList(favorites, bookNames, viewModel::setFavorite)
            }
        }
    }
}

@Composable
private fun BookList(
    books: List<HadithBook>,
    downloads: Map<String, Float?>,
    onOpen: (String) -> Unit,
    onDownload: (String) -> Unit,
    onCancel: (String) -> Unit,
) {
    val locale = currentLocale()
    LazyColumn(
        contentPadding = PaddingValues(start = Spacing.large, end = Spacing.large, top = Spacing.medium, bottom = Spacing.xxlarge + LocalBottomBarPadding.current),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        itemsIndexed(books, key = { _, book -> book.info.id }) { index, book ->
            val id = book.info.id
            val downloading = id in downloads
            val progress = downloads[id]
            SegmentedListItem(
                // A collection with nothing stored yet has nothing to open, so a tap downloads it.
                onClick = { if (book.hadithCount > 0) onOpen(id) else if (!downloading) onDownload(id) },
                shapes = ListItemDefaults.segmentedShapes(index, books.size),
                modifier = Modifier.animateItem(),
                leadingContent = {
                    ShapeBadge(
                        text = Formatters.number(index + 1, locale),
                        shape = MaterialShapes.Cookie6Sided.toShape(),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(book.info.compiler(locale))
                        when {
                            downloading && progress != null ->
                                LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                            downloading -> LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                            book.isComplete ->
                                Text(pluralStringResource(R.plurals.hadith_count, book.hadithCount, Formatters.number(book.hadithCount, locale)))
                            book.hadithCount == 0 -> Text(stringResource(R.string.not_downloaded))
                            else -> Text(stringResource(R.string.sample_hadiths, Formatters.number(book.hadithCount, locale)))
                        }
                    }
                },
                trailingContent = {
                    when {
                        downloading -> IconButton(onClick = { onCancel(id) }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cancel_download))
                        }
                        !book.isComplete -> IconButton(onClick = { onDownload(id) }) {
                            Icon(Icons.Rounded.Download, contentDescription = stringResource(R.string.download_collection))
                        }
                        else -> Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                    }
                },
            ) {
                Text(book.info.name(locale), style = MaterialTheme.typography.titleMedium)
            }
        }
        item(key = "note") {
            Text(
                stringResource(R.string.downloads_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.hair, end = Spacing.hair, top = Spacing.large),
            )
        }
    }
}

@Composable
private fun FavoriteList(favorites: List<Hadith>, bookNames: Map<String, String>, onFavorite: (String, Boolean) -> Unit) {
    if (favorites.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.FavoriteBorder,
            title = stringResource(R.string.favorites_empty),
            body = stringResource(R.string.favorites_empty_body),
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = Spacing.large, end = Spacing.large, top = Spacing.medium, bottom = Spacing.xxlarge + LocalBottomBarPadding.current),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        items(favorites, key = { it.id }) { hadith ->
            HadithCard(hadith, bookNames[hadith.bookId], isFavorite = true, onFavoriteChange = { onFavorite(hadith.id, it) }, Modifier.animateItem())
        }
    }
}

@Composable
private fun SearchResults(
    results: List<Hadith>?,
    bookNames: Map<String, String>,
    someIncomplete: Boolean,
    favoriteIds: Set<String>,
    onFavorite: (String, Boolean) -> Unit,
) {
    when {
        results == null -> LoadingBox()
        results.isEmpty() -> EmptyState(
            icon = Icons.Rounded.SearchOff,
            title = stringResource(R.string.no_results),
            body = if (someIncomplete) stringResource(R.string.search_incomplete) else null,
        )
        else -> LazyColumn(
            contentPadding = PaddingValues(start = Spacing.large, end = Spacing.large, top = Spacing.small, bottom = Spacing.xxlarge + LocalBottomBarPadding.current),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            if (someIncomplete) {
                item(key = "hint") {
                    Text(
                        stringResource(R.string.search_incomplete),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.hair),
                    )
                }
            }
            items(results, key = { it.id }) { hadith ->
                HadithCard(hadith, bookNames[hadith.bookId], hadith.id in favoriteIds, onFavoriteChange = { onFavorite(hadith.id, it) }, Modifier.animateItem())
            }
        }
    }
}

@Composable
fun HadithBookScreen(
    key: HadithBookKey,
    onBack: () -> Unit,
    viewModel: HadithBookViewModel = koinViewModel { parametersOf(key) },
) {
    val book by viewModel.book.collectAsStateWithLifecycle()
    val hadiths by viewModel.hadiths.collectAsStateWithLifecycle()
    val endReached by viewModel.endReached.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val downloads by viewModel.download.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val locale = currentLocale()
    val snackbar = remember { SnackbarHostState() }
    val info = book?.info ?: HadithBooks.info(key.bookId)

    LaunchedEffect(viewModel) {
        viewModel.downloadFailures.collect { id ->
            if (id == key.bookId) snackbar.showSnackbar(resources.getString(R.string.download_failed, info?.name(locale) ?: id))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(info?.name(locale).orEmpty()) },
                subtitle = { Text(info?.compiler(locale).orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = LocalBottomBarPadding.current)) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = Spacing.large, end = Spacing.large, top = Spacing.small, bottom = Spacing.xxlarge + LocalBottomBarPadding.current),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            val current = book
            if (current != null && !current.isComplete) {
                item(key = "sample") {
                    SampleBanner(
                        downloading = key.bookId in downloads,
                        progress = downloads[key.bookId],
                        onDownload = viewModel::startDownload,
                    )
                }
            }
            items(hadiths, key = { it.id }) { hadith ->
                HadithCard(hadith, bookName = null, isFavorite = hadith.id in favoriteIds, onFavoriteChange = { viewModel.setFavorite(hadith.id, it) }, modifier = Modifier.animateItem())
            }
            if (!endReached) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(Spacing.xxlarge), contentAlignment = Alignment.Center) { LoadingIndicator() }
                    LaunchedEffect(hadiths.size) { viewModel.loadMore() }
                }
            }
        }
    }
}

@Composable
private fun SampleBanner(downloading: Boolean, progress: Float?, onDownload: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(Spacing.xlarge), verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            Text(stringResource(R.string.sample_banner), style = MaterialTheme.typography.bodyMedium)
            when {
                downloading && progress != null -> LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                downloading -> LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                else -> FilledTonalButton(onClick = onDownload, modifier = Modifier.align(Alignment.End)) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                    Text(stringResource(R.string.download_collection), modifier = Modifier.padding(start = Spacing.small))
                }
            }
        }
    }
}
