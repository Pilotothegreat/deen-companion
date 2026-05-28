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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.trafficlight.database.BookmarkedVerse
import com.leekleak.trafficlight.database.BookmarkedVerseDao
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.PageTitle
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun QuranReader(surahNumber: Int, surahName: String) {
    val context = LocalContext.current
    val bookmarkDao: BookmarkedVerseDao = koinInject()
    val scope = rememberCoroutineScope()

    val hazeState = rememberHazeState()
    val surahs = remember { QuranHelper.getSurahs(context) }
    val surah = remember(surahNumber) { surahs.firstOrNull { it.id == surahNumber } }

    val bookmarks by bookmarkDao.getAllFlow().collectAsState(initial = emptyList())
    val bookmarkedIds = remember(bookmarks) { bookmarks.map { it.id }.toSet() }

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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 96.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                            Text(
                                text = "Ayah ${verse.id}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.secondary
                            )
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
                        Text(
                            text = verse.text,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                            lineHeight = 40.sp,
                            color = colorScheme.onSurface
                        )

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
