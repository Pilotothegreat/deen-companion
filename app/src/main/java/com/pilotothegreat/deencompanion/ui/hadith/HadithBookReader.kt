package com.pilotothegreat.deencompanion.ui.hadith

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.ui.navigation.Navigator
import com.pilotothegreat.deencompanion.util.PageTitle
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HadithBookReader(bookId: String) {
    val viewModel: HadithVM = koinViewModel()
    val navigator: Navigator = koinInject()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val lang by appPreferenceRepo.appLanguage.collectAsState(initial = "en")

    LaunchedEffect(bookId) {
        viewModel.selectBook(bookId)
    }

    val books by viewModel.books.collectAsState()
    val loadedHadiths by viewModel.loadedHadiths.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val book = remember(bookId, books) { books.firstOrNull { it.id == bookId } }
    val hazeState = rememberHazeState()

    val paddingTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 52.dp
    val paddingBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val direction = if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Column(
            modifier = Modifier
                .background(colorScheme.surface)
                .fillMaxSize()
                .hazeSource(hazeState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.height(paddingTop + 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(
                        text = getLocalizedBookName(book?.id ?: "", book?.name ?: "", lang),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = getLocalizedCompiler(book?.id ?: "", book?.compiler ?: "", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.secondary
                    )
                }

                IconButton(onClick = { book?.id?.let { viewModel.forceSyncBook(it) } }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.sync_full_book),
                        tint = colorScheme.primary
                    )
                }
            }

            if (isSyncing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = colorScheme.primary
                )
            }

            var isRefreshing by remember { mutableStateOf(false) }

            val listState = rememberLazyListState()
            val shouldLoadMore = remember {
                derivedStateOf {
                    val totalItemsCount = listState.layoutInfo.totalItemsCount
                    val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    lastVisibleItemIndex >= totalItemsCount - 5
                }
            }

            LaunchedEffect(shouldLoadMore.value) {
                if (shouldLoadMore.value && viewModel.hasMoreToLoad) {
                    viewModel.loadNextPage()
                }
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    scope.launch {
                        book?.id?.let { viewModel.forceSyncBook(it) }
                        kotlinx.coroutines.delay(1200)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = paddingBottom + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(loadedHadiths, key = { it.id }) { hadith ->
                        HadithCard(
                            collectionName = getLocalizedBookName(hadith.bookId, book?.name ?: "", lang),
                            hadith = hadith,
                            isFavorite = hadith.isFavorite,
                            onFavoriteToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleFavorite(hadith.id, hadith.isFavorite)
                            },
                            lang = lang,
                            showCollectionName = false
                        )
                    }

                    if (viewModel.hasMoreToLoad) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    PageTitle(true, hazeState, "")
}
