package com.pilotothegreat.deencompanion.ui.quran

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.foundation.BorderStroke
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.services.QuranPlaybackManager
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
    val playbackManager: QuranPlaybackManager = koinInject()

    val hazeState = rememberHazeState()
    val surahs = remember { QuranHelper.getSurahs(context) }
    val surah = remember(surahNumber) { surahs.firstOrNull { it.id == surahNumber } }

    val arabicFontSize by appPreferenceRepo.quranArabicFontSize.collectAsState(initial = 24)
    val quranFontFamily = remember(context) {
        FontFamily(
            Font("UthmanicHafs.ttf", context.assets)
        )
    }

    val lang by appPreferenceRepo.appLanguage.collectAsState(initial = "en")

    val currentSurahId by playbackManager.currentSurahId.collectAsState()
    val currentAyahId by playbackManager.currentAyahId.collectAsState()

    val activeAyah = if (currentSurahId == surahNumber) currentAyahId else scrollToVerse ?: 1

    val pages = remember(surah) {
        surah?.verses?.chunked(8) ?: emptyList()
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    val pageIndexForActiveAyah = remember(activeAyah, pages) {
        pages.indexOfFirst { page -> page.any { it.id == activeAyah } }
    }

    LaunchedEffect(pageIndexForActiveAyah) {
        if (pageIndexForActiveAyah >= 0 && pagerState.currentPage != pageIndexForActiveAyah) {
            pagerState.animateScrollToPage(pageIndexForActiveAyah)
        }
    }

    LaunchedEffect(Unit) {
        if (autoPlay && surah != null) {
            playbackManager.playSurah(surah, scrollToVerse ?: 1)
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
                Text(stringResource(R.string.surah_not_found), color = colorScheme.error)
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp, bottom = 120.dp)
            ) { pageIdx ->
                val pageVerses = pages.getOrNull(pageIdx) ?: emptyList()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBF7)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (pageIdx == 0 && surah.id != 9 && surah.id != 1) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer.copy(alpha = 0.15f)),
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = quranFontFamily
                                        ),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        color = colorScheme.primary
                                    )
                                }
                            }

                            val primaryContainerColor = colorScheme.primaryContainer
                            val onPrimaryContainerColor = colorScheme.onPrimaryContainer
                            val segments = remember(pageVerses, activeAyah, primaryContainerColor, onPrimaryContainerColor) {
                                val list = mutableListOf<Any>()
                                var builder = AnnotatedString.Builder()
                                
                                pageVerses.forEach { verse ->
                                    val boundary = juzData.firstOrNull { it.surahNumber == surah.id && it.verseNumber == verse.id }
                                    if (boundary != null) {
                                        val annotated = builder.toAnnotatedString()
                                        if (annotated.isNotEmpty()) {
                                            list.add(annotated)
                                        }
                                        list.add(boundary)
                                        builder = AnnotatedString.Builder()
                                    }
                                    
                                    val start = builder.length
                                    val verseText = "${verse.text} ﴾${toArabicNumerals(verse.id)}﴿ "
                                    builder.append(verseText)
                                    val end = builder.length
                                    
                                    builder.addStringAnnotation(
                                        tag = "AYAH_CLICK",
                                        annotation = verse.id.toString(),
                                        start = start,
                                        end = end
                                    )
                                    
                                    if (verse.id == activeAyah) {
                                        builder.addStyle(
                                            style = SpanStyle(
                                                background = primaryContainerColor,
                                                color = onPrimaryContainerColor
                                            ),
                                            start = start,
                                            end = end
                                        )
                                    }
                                }
                                val finalAnnotated = builder.toAnnotatedString()
                                if (finalAnnotated.isNotEmpty()) {
                                    list.add(finalAnnotated)
                                }
                                list
                            }

                            segments.forEach { segment ->
                                when (segment) {
                                    is JuzBoundary -> {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        JuzHeader(segment.juzNumber)
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                    is AnnotatedString -> {
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                            ClickableText(
                                                text = segment,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontSize = arabicFontSize.sp,
                                                    fontFamily = quranFontFamily,
                                                    lineHeight = (arabicFontSize * 2.3f).sp,
                                                    textAlign = TextAlign.Justify
                                                ),
                                                onClick = { offset ->
                                                    segment.getStringAnnotations(tag = "AYAH_CLICK", start = offset, end = offset)
                                                        .firstOrNull()?.let { annotation ->
                                                            val clickedAyahId = annotation.item.toIntOrNull()
                                                            if (clickedAyahId != null) {
                                                                playbackManager.jumpToAyah(clickedAyahId)
                                                                if (currentSurahId != surah.id) {
                                                                    playbackManager.playSurah(surah, clickedAyahId)
                                                                }
                                                            }
                                                        }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }

                            if (lang == "en") {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                pageVerses.forEach { verse ->
                                    val isHighlighted = verse.id == activeAyah
                                    Text(
                                        text = stringResource(R.string.verse_number_prefix, verse.id) + verse.translation,
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                        color = if (isHighlighted) colorScheme.primary else colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }
                    }
                }
            }

            QuranAudioPlayer(
                surah = surah,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        PageTitle(
            backButton = true,
            hazeState = hazeState,
            text = surahName,
            customElement = {
                var showSleepMenu by remember { mutableStateOf(false) }
                val sleepTimerRemaining by playbackManager.sleepTimerRemaining.collectAsState()
                val endOfSurahEnabled by playbackManager.endOfSurahEnabled.collectAsState()

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box {
                        IconButton(onClick = { showSleepMenu = true }) {
                            BadgedBox(
                                badge = {
                                    if (sleepTimerRemaining > 0) {
                                        val mins = (sleepTimerRemaining + 59) / 60
                                        Badge { Text(mins.toString()) }
                                    } else if (endOfSurahEnabled) {
                                        Badge { Text("S") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = "Sleep Mode"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSleepMenu,
                            onDismissRequest = { showSleepMenu = false }
                        ) {
                            listOf(
                                0 to stringResource(R.string.timer_off),
                                10 to stringResource(R.string.timer_10m),
                                15 to stringResource(R.string.timer_15m),
                                30 to stringResource(R.string.timer_30m),
                                45 to stringResource(R.string.timer_45m),
                                60 to stringResource(R.string.timer_60m)
                            ).forEach { (minutes, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        playbackManager.setSleepTimer(minutes)
                                        showSleepMenu = false
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.timer_end_of_surah)) },
                                onClick = {
                                    playbackManager.setEndOfSurahEnabled(true)
                                    showSleepMenu = false
                                }
                            )
                        }
                    }
                }
            }
        )
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
                text = stringResource(R.string.juz_number, juzNumber),
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
