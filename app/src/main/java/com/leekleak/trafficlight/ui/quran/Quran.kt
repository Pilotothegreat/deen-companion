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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.Color
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
    val bookmarkDao: BookmarkedVerseDao = koinInject()

    val hazeState = rememberHazeState()
    val scope = rememberCoroutineScope()

    val searchState = rememberTextFieldState("")
    val searchQuery by remember { derivedStateOf { searchState.text.toString().trim() } }

    val surahs = remember { QuranHelper.getSurahs(context) }
    val bookmarks by bookmarkDao.getAllFlow().collectAsState(initial = emptyList())

    val tabs = listOf("Surahs", "Juz'", "Bookmarks", "Guidelines")
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
                    1 -> JuzList(navigator, paddingBottom)
                    2 -> BookmarksList(bookmarks, navigator, paddingBottom)
                    3 -> GuidelinesView(paddingBottom)
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

@Composable
fun JuzList(navigator: Navigator, bottomPadding: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    var expandedJuz by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items((1..30).toList()) { juzNum ->
            val isExpanded = expandedJuz == juzNum
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .card()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedJuz = if (isExpanded) null else juzNum }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Juz $juzNum",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (isExpanded) "Hide" else "Show Verses",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary
                    )
                }

                if (isExpanded) {
                    val verses = remember(juzNum) { QuranHelper.getVersesForJuz(context, juzNum) }
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        verses.forEach { (surah, verse) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navigator.goTo(QuranReaderKey(surah.id, surah.transliteration, scrollToVerse = verse.id)) }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${surah.transliteration} (${verse.id})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.secondary,
                                    modifier = Modifier.weight(0.4f)
                                )
                                Text(
                                    text = verse.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarksList(bookmarks: List<BookmarkedVerse>, navigator: Navigator, bottomPadding: androidx.compose.ui.unit.Dp) {
    if (bookmarks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No bookmarked verses yet.\nBookmark verses during reading to view them here.",
                textAlign = TextAlign.Center,
                color = colorScheme.secondary
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(bookmarks) { bookmark ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .card()
                        .clickable { navigator.goTo(QuranReaderKey(bookmark.surahNumber, bookmark.surahName, scrollToVerse = bookmark.ayahNumber)) }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${bookmark.surahName} (Ayah ${bookmark.ayahNumber})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Tap to read",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun GuidelinesView(bottomPadding: androidx.compose.ui.unit.Dp) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recitation Etiquettes (Adab)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("1. Cleanliness: It is highly recommended to perform Wudu (ablution) before reciting.", style = MaterialTheme.typography.bodyMedium)
                    Text("2. Focus: Sit in a clean, quiet place facing the Qiblah (direction of prayer) if possible.", style = MaterialTheme.typography.bodyMedium)
                    Text("3. Attentiveness: Recite slowly (Tarteel) and contemplate the meanings.", style = MaterialTheme.typography.bodyMedium)
                    Text("4. Silence: When the Quran is recited, listen to it attentively and be silent so you may receive mercy.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tajweed Basics", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Tajweed refers to the rules governing pronunciation during recitation. It ensures that letters are pronounced from their correct articulation points with proper characteristics.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("• Ghunnah: A nasal sound produced from the nose, applied to letters Meem (م) and Noon (ن) when they have a Shaddah.", style = MaterialTheme.typography.bodyMedium)
                    Text("• Qalqalah: An echoing or bouncing sound made when pronouncing certain consonants (ق, ط, ب, ج, د) when they carry a Sukoon.", style = MaterialTheme.typography.bodyMedium)
                    Text("• Madd: Prolongation of vowel sounds when followed by letters of elongation (Alif, Waw, Ya).", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
