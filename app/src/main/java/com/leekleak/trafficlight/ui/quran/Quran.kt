package com.leekleak.trafficlight.ui.quran

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
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.trafficlight.database.BookmarkedVerse
import com.leekleak.trafficlight.database.BookmarkedVerseDao
import com.leekleak.trafficlight.ui.navigation.Navigator
import com.leekleak.trafficlight.ui.navigation.QuranReaderKey
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.PageTitle
import com.leekleak.trafficlight.util.SearchField
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Quran(paddingValues: PaddingValues) {
    val context = LocalContext.current
    val navigator: Navigator = koinInject()
    val hazeState = rememberHazeState()
    val scope = rememberCoroutineScope()
    val searchState = rememberTextFieldState("")
    val searchQuery by remember { derivedStateOf { searchState.text.toString().trim() } }

    val surahs = remember { QuranHelper.getSurahs(context) }

    val tabs = listOf("Surahs", "Juz'")
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
            // Show Search Results instead of tabs
            val searchResults = remember(searchQuery) {
                val results = mutableListOf<Pair<QuranHelper.Surah, QuranHelper.Verse>>()
                for (surah in surahs) {
                    if (surah.transliteration.contains(searchQuery, ignoreCase = true) ||
                        surah.name.contains(searchQuery)
                    ) {
                        // Match entire surah -> add its first verse
                        if (surah.verses.isNotEmpty()) {
                            results.add(Pair(surah, surah.verses.first()))
                        }
                    }
                    for (verse in surah.verses) {
                        if (verse.translation.contains(searchQuery, ignoreCase = true) ||
                            verse.text.contains(searchQuery)
                        ) {
                            results.add(Pair(surah, verse))
                        }
                    }
                }
                results.take(50) // Cap results
            }

            Text(
                text = "Search Results (${searchResults.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

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
                                text = "${surah.transliteration} (Ayah ${verse.id})",
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = verse.text,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
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
                    0 -> SurahsList(surahs, navigator, paddingBottom)
                    1 -> JuzList(surahs, navigator, paddingBottom)
                }
            }
        }
    }

    PageTitle(false, hazeState, "Quran Browser")
}

@Composable
fun SurahsList(surahs: List<QuranHelper.Surah>, navigator: Navigator, bottomPadding: androidx.compose.ui.unit.Dp) {
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
                            text = surah.id.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onPrimaryContainer
                        )
                    }
                    Column {
                        Text(surah.transliteration, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("${surah.type.replaceFirstChar { it.uppercase() }} • ${surah.totalVerses} Verses", style = MaterialTheme.typography.labelSmall, color = colorScheme.secondary)
                    }
                }
                Text(
                    text = surah.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.primary
                )
            }
        }
    }
}

val juzStartSurah = mapOf(
    1 to listOf(1,2), 2 to listOf(2), 3 to listOf(2,3), 4 to listOf(3,4),
    5 to listOf(4), 6 to listOf(4,5), 7 to listOf(5,6), 8 to listOf(6,7),
    9 to listOf(7,8), 10 to listOf(8,9), 11 to listOf(9,10,11),
    12 to listOf(11,12), 13 to listOf(12,13,14), 14 to listOf(15,16),
    15 to listOf(17,18), 16 to listOf(18,19,20), 17 to listOf(21,22),
    18 to listOf(23,24,25), 19 to listOf(25,26,27), 20 to listOf(27,28,29),
    21 to listOf(29,30,31,32,33), 22 to listOf(33,34,35,36),
    23 to listOf(36,37,38,39), 24 to listOf(39,40,41),
    25 to listOf(41,42,43,44,45), 26 to listOf(46,47,48,49,50,51),
    27 to listOf(51,52,53,54,55,56,57), 28 to listOf(58,59,60,61,62,63,64,65,66),
    29 to listOf(67,68,69,70,71,72,73,74,75,76,77),
    30 to listOf(78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114)
)

@Composable
fun JuzList(surahs: List<QuranHelper.Surah>, navigator: Navigator, bottomPadding: androidx.compose.ui.unit.Dp) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items((1..30).toList()) { juzNum ->
            val surahIds = juzStartSurah[juzNum] ?: emptyList()
            val juzSurahs = surahs.filter { it.id in surahIds }
            var expanded by remember { mutableStateOf(false) }
            Column(modifier = Modifier.fillMaxWidth().card()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("الجزء $juzNum · Juz $juzNum",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("${juzSurahs.size} Surahs",
                            style = MaterialTheme.typography.labelSmall, color = colorScheme.secondary)
                    }
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null, tint = colorScheme.primary)
                }
                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        juzSurahs.forEach { surah ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { navigator.goTo(QuranReaderKey(surah.id, surah.transliteration)) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(surah.transliteration, style = MaterialTheme.typography.bodyMedium)
                                Text(surah.name, style = MaterialTheme.typography.titleMedium, color = colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
