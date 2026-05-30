package com.pilotothegreat.deencompanion.ui.hadith

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.R
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.ui.theme.card
import com.pilotothegreat.deencompanion.util.PageTitle
import com.pilotothegreat.deencompanion.util.SearchField
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

fun hadithFavKey(collectionName: String, hadithNumber: Int): String =
    "${collectionName.trim().lowercase().replace(" ", "_")}:$hadithNumber"

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Hadith(paddingValues: PaddingValues) {
    val context = LocalContext.current
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val favoritedHadiths by appPreferenceRepo.favoritedHadiths.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    val hazeState = rememberHazeState()
    val searchState = rememberTextFieldState("")
    val searchQuery by remember { derivedStateOf { searchState.text.toString().trim() } }

    val collections = remember { HadithHelper.getCollections(context) }
    var selectedCollection by remember { mutableStateOf<HadithHelper.HadithCollection?>(null) }

    val tabs = listOf("Collections", "Favorites")
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

        // Unified Search Field
        SearchField(textFieldState = searchState)

        if (searchQuery.isNotEmpty()) {
            // Show Search Results from Hadiths
            val searchResults = remember(searchQuery) {
                HadithHelper.searchHadiths(context, searchQuery).take(50)
            }

            Text(
                text = "Search Results (${searchResults.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = paddingBottom + 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(searchResults) { (collectionName, hadith) ->
                    val favKey = hadithFavKey(collectionName, hadith.number)
                    val isFav = favoritedHadiths.contains(favKey)

                    HadithCard(
                        collectionName = collectionName,
                        hadith = hadith,
                        isFavorite = isFav,
                        onFavoriteToggle = {
                            scope.launch {
                                appPreferenceRepo.toggleHadithFavorite(favKey)
                            }
                        }
                    )
                }
            }
        } else if (selectedCollection != null) {
            // Show Hadiths in the selected collection
            val currentCollection = selectedCollection!!

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedCollection = null }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = currentCollection.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = currentCollection.compiler,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.secondary
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = paddingBottom + 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(currentCollection.hadiths) { hadith ->
                    val favKey = hadithFavKey(currentCollection.name, hadith.number)
                    val isFav = favoritedHadiths.contains(favKey)

                    HadithCard(
                        collectionName = currentCollection.name,
                        hadith = hadith,
                        isFavorite = isFav,
                        onFavoriteToggle = {
                            scope.launch {
                                appPreferenceRepo.toggleHadithFavorite(favKey)
                            }
                        }
                    )
                }
            }
        } else {
            // Show Tabs
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
                    0 -> CollectionsTab(collections) { selectedCollection = it }
                    1 -> FavoritesTab(collections, favoritedHadiths, appPreferenceRepo, paddingBottom)
                }
            }
        }
    }

    PageTitle(false, hazeState, "Hadith Library")
}

@Composable
fun CollectionsTab(
    collections: List<HadithHelper.HadithCollection>,
    onCollectionClick: (HadithHelper.HadithCollection) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(collections) { col ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCollectionClick(col) },
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
                        Text(
                            text = col.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Compiled by: ${col.compiler}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.secondary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${col.hadiths.size} Selected Hadiths",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesTab(
    collections: List<HadithHelper.HadithCollection>,
    favorites: Set<String>,
    appPreferenceRepo: AppPreferenceRepo,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    val scope = rememberCoroutineScope()
    val favList = remember(favorites, collections) {
        val result = mutableListOf<Pair<String, HadithHelper.Hadith>>()
        for (col in collections) {
            for (hadith in col.hadiths) {
                val key = hadithFavKey(col.name, hadith.number)
                if (favorites.contains(key)) {
                    result.add(Pair(col.name, hadith))
                }
            }
        }
        result
    }

    if (favList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No favorite hadiths yet.\nTap the heart icon on any hadith to save it here.",
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
            items(favList) { (collectionName, hadith) ->
                val favKey = hadithFavKey(collectionName, hadith.number)
                HadithCard(
                    collectionName = collectionName,
                    hadith = hadith,
                    isFavorite = true,
                    onFavoriteToggle = {
                        scope.launch {
                            appPreferenceRepo.toggleHadithFavorite(favKey)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun HadithCard(
    collectionName: String,
    hadith: HadithHelper.Hadith,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val arabicFontFamily = remember { FontFamily(Font(R.font.scheherazade_new)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = collectionName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary
                    )
                    Text(
                        text = "Hadith #${hadith.number}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.secondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Grade Badge
                    val isSahih = hadith.grade.contains("Sahih", ignoreCase = true)
                    val isHasan = hadith.grade.contains("Hasan", ignoreCase = true)
                    val isDaif = hadith.grade.contains("Da'if", ignoreCase = true) ||
                                 hadith.grade.contains("Daif", ignoreCase = true)
                    val isMawdu = hadith.grade.contains("Mawdu", ignoreCase = true) ||
                                  hadith.grade.contains("Fabricated", ignoreCase = true)

                    val badgeColor = when {
                        isSahih  -> colorScheme.primaryContainer
                        isHasan  -> colorScheme.tertiaryContainer
                        isDaif   -> colorScheme.secondaryContainer   // yellow/muted warning
                        isMawdu  -> colorScheme.errorContainer       // red — fabricated
                        else     -> colorScheme.surfaceVariant       // unknown — neutral grey
                    }
                    val badgeTextColor = when {
                        isSahih  -> colorScheme.onPrimaryContainer
                        isHasan  -> colorScheme.onTertiaryContainer
                        isDaif   -> colorScheme.onSecondaryContainer
                        isMawdu  -> colorScheme.onErrorContainer
                        else     -> colorScheme.onSurfaceVariant
                    }

                    Surface(
                        color = badgeColor,
                        shape = shapes.extraSmall,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = hadith.grade,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeTextColor
                        )
                    }

                    // Favorite Button
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.Red else colorScheme.outline
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Narrator
            Text(
                text = "Narrated by: ${hadith.narrator}",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = colorScheme.secondary
            )

            Spacer(Modifier.height(12.dp))

            if (!expanded) {
                val previewText = if (hadith.english.length > 120) {
                    hadith.english.take(120) + "…"
                } else {
                    hadith.english
                }
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            } else {
                // Arabic Text
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = hadith.arabic,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 20.sp,
                            lineHeight = 32.sp,         // increased for Arabic readability
                            fontWeight = FontWeight.Medium,
                            fontFamily = arabicFontFamily
                        ),
                        textAlign = TextAlign.Start,    // Start = Right in RTL context
                        modifier = Modifier.fillMaxWidth(),
                        color = colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(12.dp))

                // English Text
                Text(
                    text = hadith.english,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

