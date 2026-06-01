package com.pilotothegreat.deencompanion.ui.hadith

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import org.koin.compose.koinInject
import com.pilotothegreat.deencompanion.ui.quran.toArabicNumerals
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.database.HadithBookEntity
import com.pilotothegreat.deencompanion.database.HadithEntity
import com.pilotothegreat.deencompanion.util.PageTitle
import com.pilotothegreat.deencompanion.util.SearchField
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Hadith(paddingValues: PaddingValues) {
    val viewModel: HadithVM = koinViewModel()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val lang by appPreferenceRepo.appLanguage.collectAsState(initial = "en")

    val hazeState = rememberHazeState()
    val searchState = rememberTextFieldState("")
    val searchQueryText = searchState.text.toString()

    // Sync Search query to viewmodel
    LaunchedEffect(searchQueryText) {
        viewModel.onSearchQueryChanged(searchQueryText)
    }

    val books by viewModel.books.collectAsState()
    val loadedHadiths by viewModel.loadedHadiths.collectAsState()
    val activeBookId by viewModel.activeBookId.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val tabs = listOf(stringResource(R.string.collections_tab), stringResource(R.string.favorites_tab))
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    val paddingSide = paddingValues.calculateLeftPadding(LayoutDirection.Ltr)
    val paddingTop = paddingValues.calculateTopPadding()
    val paddingBottom = paddingValues.calculateBottomPadding()

    Column(
        modifier = Modifier
            .background(colorScheme.surface)
            .fillMaxSize()
            .hazeSource(hazeState)
            .padding(horizontal = paddingSide),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.height(paddingTop - 8.dp))

        // Unified M3 Search Field
        SearchField(textFieldState = searchState)

        if (searchQueryText.trim().isNotEmpty()) {
            // Show Search Results
            Text(
                text = stringResource(R.string.search_results_count, searchResults.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            if (isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.search_empty_state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = paddingBottom + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(searchResults, key = { it.id }) { hadith ->
                        val bookName = books.firstOrNull { it.id == hadith.bookId }?.name ?: hadith.bookId
                        HadithCard(
                            collectionName = getLocalizedBookName(hadith.bookId, bookName, lang),
                            hadith = hadith,
                            isFavorite = hadith.isFavorite,
                            onFavoriteToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleFavorite(hadith.id, hadith.isFavorite)
                            },
                            lang = lang,
                            showCollectionName = true
                        )
                    }
                }
            }
        } else if (activeBookId != null) {
            // Active collection with paged/infinite scroll
            val book = remember(activeBookId, books) { books.firstOrNull { it.id == activeBookId } }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.selectBook(null) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                    Column(modifier = Modifier.padding(start = 8.dp)) {
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
                }

                // Manual pull/download full book
                IconButton(onClick = { book?.id?.let { viewModel.forceSyncBook(it) } }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = stringResource(R.string.sync_full_book), tint = colorScheme.primary)
                }
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

            if (isSyncing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = colorScheme.primary)
                        Text(
                            text = if (lang == "ar") "جاري تحميل المجموعة كاملة..." else "Downloading entire collection...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.outline
                        )
                    }
                }
            } else {
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
        } else {
            // Book list / favorites tabs
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> CollectionsTab(books, lang) { viewModel.selectBook(it.id) }
                    1 -> FavoritesTab(favorites, books, viewModel, paddingBottom, lang)
                }
            }
        }
    }

    PageTitle(false, hazeState, stringResource(R.string.hadith_library_title))
}

@Composable
fun CollectionsTab(
    books: List<HadithBookEntity>,
    lang: String,
    onBookClick: (HadithBookEntity) -> Unit
) {
    if (books.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(books, key = { it.id }) { book ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBookClick(book) },
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val localizedBookName = getLocalizedBookName(book.id, book.name, lang)
                            val localizedCompiler = getLocalizedCompiler(book.id, book.compiler, lang)
                            Text(
                                text = localizedBookName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.compiled_by_prefix, localizedCompiler),
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.secondary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.hadiths_count_suffix, book.hadithCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesTab(
    favorites: List<HadithEntity>,
    books: List<HadithBookEntity>,
    viewModel: HadithVM,
    bottomPadding: androidx.compose.ui.unit.Dp,
    lang: String
) {
    val haptic = LocalHapticFeedback.current

    if (favorites.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.favorites_empty_state),
                textAlign = TextAlign.Center,
                color = colorScheme.secondary,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(favorites, key = { it.id }) { hadith ->
                val bookName = books.firstOrNull { it.id == hadith.bookId }?.name ?: hadith.bookId
                HadithCard(
                    collectionName = getLocalizedBookName(hadith.bookId, bookName, lang),
                    hadith = hadith,
                    isFavorite = true,
                    onFavoriteToggle = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleFavorite(hadith.id, true)
                    },
                    lang = lang,
                    showCollectionName = true
                )
            }
        }
    }
}

@Composable
fun HadithCard(
    collectionName: String,
    hadith: HadithEntity,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    lang: String,
    showCollectionName: Boolean = false
) {
    val arabicFontFamily = remember { FontFamily(Font(R.font.scheherazade_new)) }
    val favScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Arabic Text (always)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = hadith.arabic,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 20.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = arabicFontFamily,
                        localeList = androidx.compose.ui.text.intl.LocaleList(androidx.compose.ui.text.intl.Locale("ar"))
                    ),
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.fillMaxWidth(),
                    color = colorScheme.onSurface
                )
            }

            if (lang == "en") {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = hadith.english,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            favScale.animateTo(0.7f, spring(stiffness = Spring.StiffnessHigh))
                            onFavoriteToggle()
                            favScale.animateTo(1f, spring(bouncyFlow()))
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer {
                            scaleX = favScale.value
                            scaleY = favScale.value
                        }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(R.string.favorite),
                        tint = if (isFavorite) Color.Red else colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun bouncyFlow() = Spring.DampingRatioHighBouncy

fun getLocalizedBookName(bookId: String, defaultName: String, lang: String): String {
    if (lang != "ar") return defaultName
    return when (bookId) {
        "bukhari" -> "صحيح البخاري"
        "muslim" -> "صحيح مسلم"
        "tirmidhi" -> "سنن الترمذي"
        "abudawud" -> "سنن أبي داود"
        "nasai" -> "سنن النسائي"
        "ibnmajah" -> "سنن ابن ماجه"
        "malik" -> "موطأ مالك"
        "ahmad" -> "مسند أحمد"
        "nawawi" -> "الأربعون النووية"
        "riyad" -> "رياض الصالحين"
        else -> defaultName
    }
}

fun getLocalizedCompiler(bookId: String, defaultCompiler: String, lang: String): String {
    if (lang != "ar") return defaultCompiler
    return when (bookId) {
        "bukhari" -> "الإمام البخاري"
        "muslim" -> "الإمام مسلم"
        "tirmidhi" -> "الإمام الترمذي"
        "abudawud" -> "الإمام أبو داود"
        "nasai" -> "الإمام النسائي"
        "ibnmajah" -> "الإمام ابن ماجه"
        "malik" -> "الإمام مالك"
        "ahmad" -> "الإمام أحمد بن حنبل"
        "nawawi" -> "الإمام النووي"
        else -> defaultCompiler
    }
}

fun getLocalizedGrade(grade: String, lang: String): String {
    if (lang != "ar") return grade
    return when {
        grade.contains("Sahih", ignoreCase = true) -> "صحيح"
        grade.contains("Hasan", ignoreCase = true) -> "حسن"
        grade.contains("Da'if", ignoreCase = true) || grade.contains("Daif", ignoreCase = true) -> "ضعيف"
        grade.contains("Mawdu", ignoreCase = true) || grade.contains("Fabricated", ignoreCase = true) -> "موضوع"
        else -> grade
    }
}
