package com.pilotothegreat.deencompanion.ui.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.data.quran.Bookmark
import com.pilotothegreat.deencompanion.data.quran.Quran
import com.pilotothegreat.deencompanion.data.quran.QuranSearchResults
import com.pilotothegreat.deencompanion.data.quran.Revelation
import com.pilotothegreat.deencompanion.data.quran.Surah
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.common.isArabic
import com.pilotothegreat.deencompanion.ui.components.EmptyState
import com.pilotothegreat.deencompanion.ui.components.LoadingBox
import com.pilotothegreat.deencompanion.ui.components.SearchField
import com.pilotothegreat.deencompanion.ui.components.SectionHeader
import com.pilotothegreat.deencompanion.ui.components.ShapeBadge
import com.pilotothegreat.deencompanion.ui.navigation.LocalBottomBarPadding
import com.pilotothegreat.deencompanion.ui.navigation.ReaderKey
import com.pilotothegreat.deencompanion.ui.theme.UthmanicHafs
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun QuranScreen(onOpenReader: (ReaderKey) -> Unit, viewModel: QuranViewModel = koinViewModel()) {
    val quran by viewModel.quran.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val lastReadPage by viewModel.lastReadPage.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.quran)) }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = stringResource(R.string.search_quran_hint),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            val loaded = quran ?: run {
                LoadingBox()
                return@Column
            }
            if (query.isNotBlank()) {
                SearchResults(results, loaded, onOpenReader)
                return@Column
            }
            SecondaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.surahs)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.bookmarks)) })
            }
            when (tab) {
                0 -> SurahList(loaded, lastReadPage, onOpenReader)
                else -> BookmarkList(bookmarks, loaded, onOpenReader, viewModel::removeBookmark)
            }
        }
    }
}

@Composable
private fun SurahList(quran: Quran, lastReadPage: Int, onOpenReader: (ReaderKey) -> Unit) {
    val locale = currentLocale()
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp + LocalBottomBarPadding.current),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        if (lastReadPage > 0) {
            item(key = "continue") {
                val firstVerse = quran.pages[lastReadPage - 1].verses.first()
                ContinueReadingCard(
                    page = lastReadPage,
                    surahName = surahName(quran.surah(firstVerse.surah), locale),
                    locale = locale,
                    onClick = { onOpenReader(ReaderKey(lastReadPage)) },
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
        itemsIndexed(quran.surahs, key = { _, surah -> surah.number }) { index, surah ->
            SegmentedListItem(
                onClick = { onOpenReader(ReaderKey(quran.pageOf(surah.number, 1))) },
                shapes = ListItemDefaults.segmentedShapes(index, quran.surahs.size),
                leadingContent = { ShapeBadge(Formatters.number(surah.number, locale)) },
                supportingContent = { Text(surahDetails(surah, locale)) },
                trailingContent = if (locale.isArabic) {
                    null
                } else {
                    {
                        Text(
                            surah.nameArabic,
                            fontFamily = UthmanicHafs,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            ) {
                Text(surahName(surah, locale), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(page: Int, surahName: String, locale: Locale, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.continue_reading), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.page_of_surah, Formatters.number(page, locale), surahName), style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun BookmarkList(
    bookmarks: List<Bookmark>,
    quran: Quran,
    onOpenReader: (ReaderKey) -> Unit,
    onRemove: (Bookmark) -> Unit,
) {
    if (bookmarks.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.BookmarkBorder,
            title = stringResource(R.string.no_bookmarks),
            body = stringResource(R.string.no_bookmarks_body),
        )
        return
    }
    val locale = currentLocale()
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp + LocalBottomBarPadding.current),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        itemsIndexed(bookmarks, key = { _, b -> "${b.surah}:${b.ayah}" }) { index, bookmark ->
            val surah = quran.surah(bookmark.surah)
            SegmentedListItem(
                onClick = { onOpenReader(ReaderKey(quran.pageOf(bookmark.surah, bookmark.ayah), bookmark.surah, bookmark.ayah)) },
                shapes = ListItemDefaults.segmentedShapes(index, bookmarks.size),
                supportingContent = {
                    Text(
                        quran.verse(bookmark.surah, bookmark.ayah)?.text.orEmpty(),
                        fontFamily = UthmanicHafs,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl),
                    )
                },
                trailingContent = {
                    IconButton(onClick = { onRemove(bookmark) }) {
                        Icon(Icons.Rounded.BookmarkRemove, contentDescription = stringResource(R.string.remove_bookmark))
                    }
                },
            ) {
                Text(verseReference(surah, bookmark.ayah, locale), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SearchResults(results: QuranSearchResults?, quran: Quran, onOpenReader: (ReaderKey) -> Unit) {
    when {
        results == null -> LoadingBox()
        results.isEmpty -> EmptyState(icon = Icons.Rounded.SearchOff, title = stringResource(R.string.no_results))
        else -> {
            val locale = currentLocale()
            val highlight = SpanStyle(
                background = MaterialTheme.colorScheme.tertiaryContainer,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp + LocalBottomBarPadding.current),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (results.surahs.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.surahs), Modifier.padding(start = 0.dp)) }
                    itemsIndexed(results.surahs, key = { _, s -> "surah-${s.number}" }) { index, surah ->
                        SegmentedListItem(
                            onClick = { onOpenReader(ReaderKey(quran.pageOf(surah.number, 1))) },
                            shapes = ListItemDefaults.segmentedShapes(index, results.surahs.size),
                            leadingContent = { ShapeBadge(Formatters.number(surah.number, locale)) },
                            supportingContent = { Text(surahDetails(surah, locale)) },
                        ) { Text(surahName(surah, locale)) }
                    }
                }
                if (results.verses.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.ayahs), Modifier.padding(start = 0.dp)) }
                    itemsIndexed(results.verses, key = { _, m -> "verse-${m.verse.surah}:${m.verse.number}" }) { _, match ->
                        val verse = match.verse
                        Card(
                            onClick = { onOpenReader(ReaderKey(quran.pageOf(verse.surah, verse.number), verse.surah, verse.number)) },
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    verseReference(quran.surah(verse.surah), verse.number, locale),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    highlighted(verse.text, match.arabicMatch, highlight),
                                    fontFamily = UthmanicHafs,
                                    fontSize = 22.sp,
                                    lineHeight = 40.sp,
                                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (!locale.isArabic || match.translationMatch != null) {
                                    Text(
                                        highlighted(verse.translation, match.translationMatch, highlight),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun highlighted(text: String, range: IntRange?, style: SpanStyle): AnnotatedString = buildAnnotatedString {
    append(text)
    if (range != null) addStyle(style, range.first, range.last + 1)
}

@Composable
internal fun surahName(surah: Surah, locale: Locale): String =
    if (locale.isArabic) stringResource(R.string.surah_title, surah.nameArabic) else surah.nameEnglish

@Composable
private fun surahDetails(surah: Surah, locale: Locale): String {
    val revelation = stringResource(if (surah.revelation == Revelation.MECCAN) R.string.meccan else R.string.medinan)
    val verses = pluralStringResource(R.plurals.verse_count, surah.verses.size, Formatters.number(surah.verses.size, locale))
    return "$revelation · $verses"
}

@Composable
internal fun verseReference(surah: Surah, ayah: Int, locale: Locale): String =
    "${surahName(surah, locale)} ${Formatters.number(surah.number, locale)}:${Formatters.number(ayah, locale)}"
