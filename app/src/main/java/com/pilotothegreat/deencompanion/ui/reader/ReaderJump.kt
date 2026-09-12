package com.pilotothegreat.deencompanion.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.data.quran.MushafLayout
import com.pilotothegreat.deencompanion.data.quran.Quran
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.components.ShapeBadge
import com.pilotothegreat.deencompanion.ui.quran.surahName

/**
 * Jump to a surah, a juz or a page without leaving the reader. The mushaf's own divisions are the
 * way people navigate it, and swiping 604 pages is not navigation.
 */
@Composable
fun JumpSheet(quran: Quran, currentPage: Int, onJump: (Int) -> Unit, onDismiss: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val locale = currentLocale()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = Spacing.xxlarge), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            SecondaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.surahs)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.juz_tab)) })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.page_tab)) })
            }
            when (tab) {
                0 -> {
                    val surahs = quran.surahs
                    val current = quran.pages[currentPage - 1].verses.first().surah
                    ScrollingList(surahs.size, current - 1) { state ->
                        LazyColumn(
                            state = state,
                            modifier = Modifier.heightIn(max = 420.dp),
                            contentPadding = PaddingValues(horizontal = Spacing.large),
                            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                        ) {
                            itemsIndexed(surahs, key = { _, s -> s.number }) { index, surah ->
                                SegmentedListItem(
                                    onClick = { onJump(quran.pageOf(surah.number, 1)) },
                                    shapes = ListItemDefaults.segmentedShapes(index, surahs.size),
                                    leadingContent = { ShapeBadge(Formatters.number(surah.number, locale)) },
                                    supportingContent = {
                                        Text(
                                            stringResource(
                                                R.string.page_juz,
                                                Formatters.number(quran.pageOf(surah.number, 1), locale),
                                                Formatters.number(quran.juzOf(surah.number, 1), locale),
                                            ),
                                        )
                                    },
                                ) { Text(surahName(surah, locale)) }
                            }
                        }
                    }
                }
                1 -> {
                    val juzs = MushafLayout.juzStarts.let { starts ->
                        (0 until starts.size / 2).map { starts[it * 2] to starts[it * 2 + 1] }
                    }
                    val current = quran.juzOf(
                        quran.pages[currentPage - 1].verses.first().surah,
                        quran.pages[currentPage - 1].verses.first().number,
                    )
                    ScrollingList(juzs.size, current - 1) { state ->
                        LazyColumn(
                            state = state,
                            modifier = Modifier.heightIn(max = 420.dp),
                            contentPadding = PaddingValues(horizontal = Spacing.large),
                            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                        ) {
                            itemsIndexed(juzs) { index, (surah, ayah) ->
                                SegmentedListItem(
                                    onClick = { onJump(quran.pageOf(surah, ayah)) },
                                    shapes = ListItemDefaults.segmentedShapes(index, juzs.size),
                                    leadingContent = { ShapeBadge(Formatters.number(index + 1, locale)) },
                                    supportingContent = {
                                        Text(
                                            "${surahName(quran.surah(surah), locale)} " +
                                                "${Formatters.number(surah, locale)}:${Formatters.number(ayah, locale)}",
                                        )
                                    },
                                ) { Text(stringResource(R.string.juz_number, Formatters.number(index + 1, locale))) }
                            }
                        }
                    }
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(72.dp),
                    modifier = Modifier.heightIn(max = 420.dp),
                    contentPadding = PaddingValues(Spacing.large),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    items((1..quran.pages.size).toList()) { page ->
                        val selected = page == currentPage
                        Surface(
                            onClick = { onJump(page) },
                            shape = MaterialTheme.shapes.medium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        ) {
                            Text(
                                Formatters.number(page, locale),
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Opens the list already scrolled to where the reader is, so the current entry is one glance away. */
@Composable
private fun ScrollingList(
    size: Int,
    initialIndex: Int,
    content: @Composable (androidx.compose.foundation.lazy.LazyListState) -> Unit,
) {
    val state = rememberLazyListState()
    LaunchedEffect(size, initialIndex) {
        if (initialIndex in 0 until size) state.scrollToItem(initialIndex)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) { content(state) }
}
