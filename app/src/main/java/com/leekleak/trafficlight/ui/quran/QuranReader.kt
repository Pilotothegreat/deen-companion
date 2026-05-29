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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.leekleak.trafficlight.R
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.PageTitle
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun QuranReader(surahNumber: Int, surahName: String, scrollToVerse: Int? = null, autoPlay: Boolean = false) {
    val context = LocalContext.current
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val scope = rememberCoroutineScope()

    val hazeState = rememberHazeState()
    val surahs = remember { QuranHelper.getSurahs(context) }
    val surah = remember(surahNumber) { surahs.firstOrNull { it.id == surahNumber } }

    val arabicFontSize by appPreferenceRepo.quranArabicFontSize.collectAsState(initial = 24)
    val scheherazadeFont = remember { FontFamily(Font(R.font.scheherazade_new)) }
    val listState = rememberLazyListState()

    val lang by appPreferenceRepo.appLanguage.collectAsState(initial = "en")

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
                contentPadding = PaddingValues(top = 96.dp, bottom = 120.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
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

                item {
                    val fullText = buildAnnotatedString {
                        surah.verses.forEach { verse ->
                            withStyle(SpanStyle(
                                fontFamily = scheherazadeFont,
                                fontSize = arabicFontSize.sp,
                                color = colorScheme.onSurface
                            )) { append(verse.text) }
                            append(" ")
                            withStyle(SpanStyle(
                                fontFamily = scheherazadeFont,
                                fontSize = (arabicFontSize * 0.8f).sp,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )) { append("\u06DD${toArabicNumerals(verse.id)}") }
                            append("  ")
                        }
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = fullText,
                            modifier = Modifier.fillMaxWidth().card().padding(16.dp),
                            textAlign = TextAlign.Start,
                            lineHeight = (arabicFontSize * 2.0f).sp
                        )
                    }
                }

                if (lang == "en") {
                    items(surah.verses) { verse ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${verse.id}.",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                modifier = Modifier.width(28.dp)
                            )
                            Text(
                                text = verse.translation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            QuranAudioPlayer(surah, autoPlay = autoPlay, modifier = Modifier.align(Alignment.BottomCenter))
        }

        PageTitle(backButton = true, hazeState = hazeState, text = surahName)
    }
}

fun toArabicNumerals(n: Int): String =
    n.toString().map { c -> if (c.isDigit()) '٠' + (c - '0') else c }.joinToString("")
