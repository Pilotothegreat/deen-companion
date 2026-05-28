package com.leekleak.trafficlight.ui.quran

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import com.leekleak.trafficlight.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.BookmarkedVerse
import com.leekleak.trafficlight.database.BookmarkedVerseDao
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.PageTitle
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun QuranReader(surahNumber: Int, surahName: String, scrollToVerse: Int? = null) {
    val context = LocalContext.current
    val bookmarkDao: BookmarkedVerseDao = koinInject()
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val scope = rememberCoroutineScope()

    val hazeState = rememberHazeState()
    val surahs = remember { QuranHelper.getSurahs(context) }
    val surah = remember(surahNumber) { surahs.firstOrNull { it.id == surahNumber } }

    val bookmarks by bookmarkDao.getAllFlow().collectAsState(initial = emptyList())
    val bookmarkedIds = remember(bookmarks) { bookmarks.map { it.id }.toSet() }

    val arabicFontSize by appPreferenceRepo.quranArabicFontSize.collectAsState(initial = 24)
    val scheherazadeFont = remember { FontFamily(Font(R.font.scheherazade_new)) }
    val listState = rememberLazyListState()

    LaunchedEffect(scrollToVerse, surah) {
        if (scrollToVerse != null && surah != null) {
            val index = surah.verses.indexOfFirst { it.id == scrollToVerse }
            if (index >= 0) {
                val targetIndex = if (surah.id != 9 && surah.id != 1) index + 1 else index
                listState.scrollToItem(targetIndex)
            }
        }
    }

    Box(
        modifier = Modifier
            .background(colorScheme.surface)
            .fillMaxSize()
            .hazeSource(hazeState)
    ) {
        if (surah == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Surah not found", color = colorScheme.error)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 96.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TODO: Verify JSON source — if Al-Fatihah verse 1 IS the Bismillah,
                //       then skip the header AND start verse display from verse 1 (don't skip verse 1).
                // suppress Bismillah for Surah 9 (At-Tawbah) and Surah 1 (Al-Fatihah, where it is verse 1)
                if (surah.id != 9 && surah.id != 1) {
                    item {
                        Text(
                            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            color = colorScheme.primary
                        )
                    }
                }

                items(surah.verses) { verse ->
                    val isBookmarked = bookmarkedIds.contains("${surah.id}:${verse.id}")

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .card()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Ayah ${verse.id}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.secondary
                                )
                                if (QuranHelper.isSajdahVerse(surah.id, verse.id)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .background(colorScheme.primaryContainer, MaterialTheme.shapes.small)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.magic),
                                            contentDescription = "Sajdah",
                                            tint = colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Sajdah",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    val id = "${surah.id}:${verse.id}"
                                    if (isBookmarked) {
                                        bookmarkDao.delete(id)
                                    } else {
                                        bookmarkDao.add(
                                            BookmarkedVerse(
                                                id = id,
                                                surahNumber = surah.id,
                                                ayahNumber = verse.id,
                                                surahName = surah.transliteration,
                                                timestamp = System.currentTimeMillis()
                                            )
                                        )
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = colorScheme.primary
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Arabic Verse
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = verse.text,
                                fontSize = arabicFontSize.sp,
                                fontFamily = scheherazadeFont,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth(),
                                lineHeight = (arabicFontSize * 1.6f).sp,
                                color = colorScheme.onSurface
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // English Translation
                        Text(
                            text = verse.translation,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        PageTitle(backButton = true, hazeState = hazeState, text = surahName)
    }
}
