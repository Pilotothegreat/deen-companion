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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import com.pilotothegreat.deencompanion.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.util.PageTitle
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.koin.compose.koinInject

data class JuzBoundary(val surahNumber: Int, val verseNumber: Int, val juzNumber: Int)

val juzData = listOf(
    JuzBoundary(1, 1, 1),
    JuzBoundary(2, 142, 2),
    JuzBoundary(2, 253, 3),
    JuzBoundary(3, 93, 4),
    JuzBoundary(4, 24, 5),
    JuzBoundary(4, 148, 6),
    JuzBoundary(5, 82, 7),
    JuzBoundary(6, 111, 8),
    JuzBoundary(7, 88, 9),
    JuzBoundary(8, 41, 10),
    JuzBoundary(9, 93, 11),
    JuzBoundary(11, 6, 12),
    JuzBoundary(12, 53, 13),
    JuzBoundary(15, 1, 14),
    JuzBoundary(17, 1, 15),
    JuzBoundary(18, 75, 16),
    JuzBoundary(21, 1, 17),
    JuzBoundary(23, 1, 18),
    JuzBoundary(25, 21, 19),
    JuzBoundary(27, 56, 20),
    JuzBoundary(29, 46, 21),
    JuzBoundary(33, 31, 22),
    JuzBoundary(36, 28, 23),
    JuzBoundary(39, 32, 24),
    JuzBoundary(41, 47, 25),
    JuzBoundary(46, 1, 26),
    JuzBoundary(51, 31, 27),
    JuzBoundary(58, 1, 28),
    JuzBoundary(67, 1, 29),
    JuzBoundary(78, 1, 30)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuranReader(surahNumber: Int, surahName: String, scrollToVerse: Int? = null, autoPlay: Boolean = false) {
    val context = LocalContext.current
    val appPreferenceRepo: AppPreferenceRepo = koinInject()

    val hazeState = rememberHazeState()
    val surahs = remember { QuranHelper.getSurahs(context) }
    val surah = remember(surahNumber) { surahs.firstOrNull { it.id == surahNumber } }

    val arabicFontSize by appPreferenceRepo.quranArabicFontSize.collectAsState(initial = 24)
    val scheherazadeFont = remember { FontFamily(Font(R.font.scheherazade_new)) }
    val listState = rememberLazyListState()

    val lang by appPreferenceRepo.appLanguage.collectAsState(initial = "en")

    var currentAyah by remember { mutableStateOf(scrollToVerse ?: 1) }

    val juzBoundariesForSurah = remember(surahNumber) {
        juzData.filter { it.surahNumber == surahNumber }
    }

    LaunchedEffect(scrollToVerse, surah) {
        if (scrollToVerse != null) {
            currentAyah = scrollToVerse
        }
    }

    LaunchedEffect(currentAyah) {
        if (surah != null) {
            val index = surah.verses.indexOfFirst { it.id == currentAyah }
            if (index >= 0) {
                val bismillahOffset = if (surah.id != 9 && surah.id != 1) 1 else 0
                val juzOffset = juzBoundariesForSurah.count { it.verseNumber <= currentAyah }
                val targetIndex = index + bismillahOffset + juzOffset
                listState.animateScrollToItem(targetIndex)
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
                contentPadding = PaddingValues(top = 96.dp, bottom = 160.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

                surah.verses.forEach { verse ->
                    val boundary = juzBoundariesForSurah.firstOrNull { it.verseNumber == verse.id }
                    if (boundary != null) {
                        stickyHeader(key = "juz_${boundary.juzNumber}") {
                            JuzHeader(juzNumber = boundary.juzNumber)
                        }
                    }

                    item(key = "verse_${verse.id}") {
                        val isHighlighted = verse.id == currentAyah
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isHighlighted) colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    else Color.Transparent,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .clickable {
                                    currentAyah = verse.id
                                }
                                .padding(12.dp)
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = "${verse.text} \u06DD${toArabicNumerals(verse.id)}",
                                    fontFamily = scheherazadeFont,
                                    fontSize = arabicFontSize.sp,
                                    color = if (isHighlighted) colorScheme.primary else colorScheme.onSurface,
                                    lineHeight = (arabicFontSize * 1.8f).sp,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (lang == "en") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = verse.translation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isHighlighted) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            QuranAudioPlayer(
                surah = surah,
                currentAyah = currentAyah,
                onAyahChanged = { currentAyah = it },
                autoPlay = autoPlay,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        PageTitle(backButton = true, hazeState = hazeState, text = surahName)
    }
}

@Composable
fun JuzHeader(juzNumber: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.secondaryContainer.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Juz $juzNumber",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onSecondaryContainer
            )
            Text(
                text = "الجزء ${toArabicNumerals(juzNumber)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onSecondaryContainer
            )
        }
    }
}

fun toArabicNumerals(n: Int): String =
    n.toString().map { c -> if (c.isDigit()) '٠' + (c - '0') else c }.joinToString("")
