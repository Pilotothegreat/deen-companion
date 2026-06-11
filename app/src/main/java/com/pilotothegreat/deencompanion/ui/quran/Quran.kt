package com.pilotothegreat.deencompanion.ui.quran

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import com.pilotothegreat.deencompanion.ui.navigation.Navigator
import com.pilotothegreat.deencompanion.ui.navigation.QuranReaderKey
import com.pilotothegreat.deencompanion.ui.theme.card
import com.pilotothegreat.deencompanion.util.PageTitle
import com.pilotothegreat.deencompanion.util.SearchField
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import androidx.compose.ui.res.stringResource
import com.pilotothegreat.deencompanion.R

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Quran(paddingValues: PaddingValues) {
    val context = LocalContext.current
    val navigator: Navigator = koinInject()
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val lang by appPreferenceRepo.appLanguage.collectAsState(initial = "en")
    val hazeState = rememberHazeState()
    val searchState = rememberTextFieldState("")
    val searchQuery by remember { derivedStateOf { searchState.text.toString().trim() } }
    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(searchQuery) {
        kotlinx.coroutines.delay(300)
        debouncedQuery = searchQuery
    }

    val surahs = remember { QuranHelper.getSurahs(context) }

    var searchResults by remember { mutableStateOf<List<Pair<QuranHelper.Surah, QuranHelper.Verse>>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(debouncedQuery) {
        if (debouncedQuery.isNotEmpty()) {
            isSearching = true
            searchResults = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                val results = mutableListOf<Pair<QuranHelper.Surah, QuranHelper.Verse>>()
                for (surah in surahs) {
                    val surahMatches = surah.transliteration.contains(debouncedQuery, ignoreCase = true) ||
                            surah.name.contains(debouncedQuery)
                    if (surahMatches && surah.verses.isNotEmpty()) {
                        results.add(Pair(surah, surah.verses.first()))
                    }
                    for (verse in surah.verses) {
                        if (verse.translation.contains(debouncedQuery, ignoreCase = true) ||
                            verse.text.contains(debouncedQuery)
                        ) {
                            results.add(Pair(surah, verse))
                        }
                    }
                }
                results.take(50)
            }
            isSearching = false
        } else {
            searchResults = emptyList()
        }
    }

    val quranFontFamily = remember(context) {
        FontFamily(
            Font("UthmanicHafs.ttf", context.assets)
        )
    }

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

        if (debouncedQuery.isNotEmpty()) {
            val direction = if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = colorScheme.primary)
                        }
                    } else if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (lang == "ar") "لا توجد نتائج" else "No results found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = if (lang == "ar") "نتائج البحث (${toArabicNumerals(searchResults.size)})" else "Search Results (${searchResults.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = paddingBottom),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        items(searchResults) { (surah, verse) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navigator.goTo(QuranReaderKey(surah.id, surah.transliteration, scrollToVerse = verse.id))
                                    },
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (lang == "ar") "${surah.name} (آية ${toArabicNumerals(verse.id)})" else "${surah.transliteration} (Ayah ${verse.id})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colorScheme.primary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = verse.text,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = quranFontFamily,
                                            fontSize = 20.sp,
                                            lineHeight = 32.sp
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Start
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = verse.translation,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
            Box(modifier = Modifier.weight(1f)) {
                SurahsList(surahs, navigator, paddingBottom, quranFontFamily)
            }
        }
    }

    PageTitle(false, hazeState, stringResource(R.string.quran_browser))
}

@Composable
fun SurahsList(
    surahs: List<QuranHelper.Surah>,
    navigator: Navigator,
    bottomPadding: androidx.compose.ui.unit.Dp,
    quranFontFamily: FontFamily
) {
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val lang by appPreferenceRepo.appLanguage.collectAsState(initial = "en")

    val direction = if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(surahs) { surah ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .card()
                        .clickable { navigator.goTo(QuranReaderKey(surah.id, surah.transliteration)) }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(colorScheme.primaryContainer, shapes.medium),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (lang == "ar") toArabicNumerals(surah.id) else surah.id.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = surah.transliteration,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            val typeText = if (lang == "ar") {
                                if (surah.type.lowercase() == "meccan") "مكية" else "مدنية"
                            } else {
                                surah.type.replaceFirstChar { it.uppercase() }
                            }
                            val versesLabel = if (lang == "ar") "آياتها" else "Verses"
                            val versesCountText = if (lang == "ar") toArabicNumerals(surah.totalVerses) else surah.totalVerses.toString()
                            Text(
                                text = if (lang == "ar") "$typeText • $versesLabel $versesCountText" else "$typeText • $versesCountText $versesLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.secondary
                            )
                        }
                    }
                    Text(
                        text = surah.name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = quranFontFamily
                        ),
                        color = colorScheme.primary
                    )
                }
            }
        }
    }
}

