package com.pilotothegreat.deencompanion.ui.quran

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.ui.theme.Theme
import com.pilotothegreat.deencompanion.database.AppPreferenceRepo
import com.pilotothegreat.deencompanion.ui.navigation.Navigator
import com.pilotothegreat.deencompanion.services.QuranPlaybackManager
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.koin.compose.koinInject
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlinx.coroutines.withContext

data class JuzBoundary(val surahNumber: Int, val verseNumber: Int, val juzNumber: Int)

val juzData = listOf(
    JuzBoundary(1, 1, 1),
    JuzBoundary(2, 142, 2),
    JuzBoundary(2, 253, 3),
    JuzBoundary(3, 92, 4),
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

sealed interface PageContent {
    data class SurahHeader(val surah: QuranHelper.Surah) : PageContent
    data class Bismillah(val surahId: Int) : PageContent
    data class VerseItem(val surahId: Int, val verse: QuranHelper.Verse) : PageContent
}

sealed interface RenderBlock {
    data class Header(val surah: QuranHelper.Surah) : RenderBlock
    data class BismillahText(val surahId: Int) : RenderBlock
    data class FatihaBismillah(val verseItem: PageContent.VerseItem) : RenderBlock
    data class Verses(val verses: List<PageContent.VerseItem>) : RenderBlock
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuranReader(surahNumber: Int, surahName: String, scrollToVerse: Int? = null, autoPlay: Boolean = false) {
    val context = LocalContext.current
    val navigator: Navigator = koinInject()
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

    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    val screenWidthDp = configuration.screenWidthDp
    val density = LocalDensity.current
    
    val textMeasurer = rememberTextMeasurer()
    val widthPx = remember(screenWidthDp, density) {
        with(density) { (screenWidthDp - 72f).dp.toPx() }
    }
    
    val textStyle = remember(arabicFontSize, quranFontFamily) {
        TextStyle(
            fontSize = arabicFontSize.sp,
            fontFamily = quranFontFamily,
            lineHeight = (arabicFontSize * 2.5f).sp,
            textAlign = TextAlign.Justify
        )
    }

    val globalPages = remember(surahs) {
        val pagesList = mutableListOf<List<PageContent>>()
        for (pageIdx in 0 until 604) {
            val startBoundary = QuranHelper.pageBoundaries.getOrNull(pageIdx) ?: break
            val endBoundary = QuranHelper.pageBoundaries.getOrNull(pageIdx + 1)
            
            val items = mutableListOf<PageContent>()
            for (s in surahs) {
                if (endBoundary != null && s.id > endBoundary.surahNumber) {
                    break
                }
                if (s.id < startBoundary.surahNumber) {
                    continue
                }
                
                for (v in s.verses) {
                    val isAfterStart = when {
                        s.id > startBoundary.surahNumber -> true
                        s.id == startBoundary.surahNumber -> v.id >= startBoundary.verseNumber
                        else -> false
                    }
                    if (!isAfterStart) continue
                    
                    val isBeforeEnd = if (endBoundary != null) {
                        when {
                            s.id < endBoundary.surahNumber -> true
                            s.id == endBoundary.surahNumber -> v.id < endBoundary.verseNumber
                            else -> false
                        }
                    } else {
                        true
                    }
                    if (!isBeforeEnd) break
                    
                    if (v.id == 1) {
                        items.add(PageContent.SurahHeader(s))
                        if (s.id != 9 && s.id != 1) {
                            items.add(PageContent.Bismillah(s.id))
                        }
                    }
                    items.add(PageContent.VerseItem(s.id, v))
                }
            }
            pagesList.add(items)
        }
        pagesList
    }

    val pagerState = rememberPagerState(pageCount = { globalPages.size })

    val activePageIndex = remember(globalPages, surahNumber, scrollToVerse) {
        val targetVerse = scrollToVerse ?: 1
        globalPages.indexOfFirst { page ->
            page.any { item ->
                item is PageContent.VerseItem && item.surahId == surahNumber && item.verse.id == targetVerse
            }
        }.coerceAtLeast(0)
    }

    // Scroll to the active page when first loaded or changed
    var hasScrolledToTarget by remember { mutableStateOf(false) }
    LaunchedEffect(activePageIndex, globalPages) {
        if (activePageIndex >= 0 && globalPages.isNotEmpty() && !hasScrolledToTarget) {
            pagerState.scrollToPage(activePageIndex)
            hasScrolledToTarget = true
        }
    }

    val pageIndexForActiveAyah = remember(currentSurahId, currentAyahId, globalPages) {
        globalPages.indexOfFirst { page ->
            page.any { item ->
                item is PageContent.VerseItem && item.surahId == currentSurahId && item.verse.id == currentAyahId
            }
        }
    }

    var previousActivePage by remember { mutableStateOf(-1) }
    LaunchedEffect(pageIndexForActiveAyah) {
        if (pageIndexForActiveAyah >= 0 && globalPages.isNotEmpty()) {
            if (pagerState.currentPage != pageIndexForActiveAyah && !pagerState.isScrollInProgress) {
                if (previousActivePage == -1 || pagerState.currentPage == previousActivePage) {
                    pagerState.animateScrollToPage(pageIndexForActiveAyah)
                }
            }
            previousActivePage = pageIndexForActiveAyah
        }
    }

    LaunchedEffect(Unit) {
        if (autoPlay && surah != null) {
            playbackManager.playSurah(surah, scrollToVerse ?: 1)
        }
    }

    val themeState by appPreferenceRepo.theme.collectAsState(Theme.AutoMaterial)
    val isDark = themeState.isDark()

    val currentPageItems = globalPages.getOrNull(pagerState.currentPage) ?: emptyList()
    val firstVerseItem = currentPageItems.firstOrNull { it is PageContent.VerseItem } as? PageContent.VerseItem
    val firstHeaderItem = currentPageItems.firstOrNull { it is PageContent.SurahHeader } as? PageContent.SurahHeader
    val currentSurah = remember(firstVerseItem, firstHeaderItem, surahs) {
        val sId = firstVerseItem?.surahId ?: firstHeaderItem?.surah?.id ?: surahNumber
        surahs.firstOrNull { it.id == sId }
    } ?: surah

    val currentJuz = remember(firstVerseItem, currentSurah) {
        if (firstVerseItem != null) {
            getJuzNumber(firstVerseItem.surahId, firstVerseItem.verse.id)
        } else {
            1
        }
    }

    // Mushaf-style colors
    val mushafBgColor = if (isDark) Color(0xFF151412) else Color(0xFFFAF6EE)
    val mushafTextColor = if (isDark) Color(0xFFE8DCC8) else Color(0xFF2C2724)
    val goldAccent = if (isDark) Color(0xFFD4A855) else Color(0xFFC5A059)
    val bismillahColor = if (isDark) colorScheme.primary else Color(0xFF8B0000)

    Box(
        modifier = Modifier
            .background(mushafBgColor)
            .fillMaxSize()
            .hazeSource(hazeState)
    ) {
        if (surah == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.surah_not_found), color = colorScheme.error)
            }
        } else {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 84.dp, bottom = 104.dp)
                ) { pageIdx ->
                    val pageContent = globalPages.getOrNull(pageIdx) ?: emptyList()
                    val blocks = remember(pageContent) {
                        val result = mutableListOf<RenderBlock>()
                        var currentVerses = mutableListOf<PageContent.VerseItem>()
                        
                        for (item in pageContent) {
                            when (item) {
                                is PageContent.SurahHeader -> {
                                    if (currentVerses.isNotEmpty()) {
                                        result.add(RenderBlock.Verses(currentVerses))
                                        currentVerses = mutableListOf()
                                    }
                                    result.add(RenderBlock.Header(item.surah))
                                }
                                is PageContent.Bismillah -> {
                                    if (currentVerses.isNotEmpty()) {
                                        result.add(RenderBlock.Verses(currentVerses))
                                        currentVerses = mutableListOf()
                                    }
                                    result.add(RenderBlock.BismillahText(item.surahId))
                                }
                                is PageContent.VerseItem -> {
                                    if (item.surahId == 1 && item.verse.id == 1) {
                                        if (currentVerses.isNotEmpty()) {
                                            result.add(RenderBlock.Verses(currentVerses))
                                            currentVerses = mutableListOf()
                                        }
                                        result.add(RenderBlock.FatihaBismillah(item))
                                    } else {
                                        currentVerses.add(item)
                                    }
                                }
                            }
                        }
                        if (currentVerses.isNotEmpty()) {
                            result.add(RenderBlock.Verses(currentVerses))
                        }
                        result
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .mushafBorder(goldAccent.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                blocks.forEach { block ->
                                    when (block) {
                                        is RenderBlock.Header -> {
                                            SurahStartBanner(
                                                surah = block.surah,
                                                goldAccent = goldAccent,
                                                textColor = mushafTextColor,
                                                isDark = isDark,
                                                fontFamily = quranFontFamily,
                                                lang = lang
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                        is RenderBlock.BismillahText -> {
                                            Text(
                                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = quranFontFamily,
                                                    fontSize = 32.sp,
                                                    lineHeight = 48.sp
                                                ),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp),
                                                color = bismillahColor
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                        is RenderBlock.FatihaBismillah -> {
                                            val primaryContainerColor = colorScheme.primaryContainer
                                            val onPrimaryContainerColor = colorScheme.onPrimaryContainer
                                            val isCurrentlyPlaying = (block.verseItem.surahId == currentSurahId && block.verseItem.verse.id == currentAyahId)
                                            val isHighlighted = isCurrentlyPlaying || (block.verseItem.surahId == surahNumber && block.verseItem.verse.id == activeAyah)

                                            val annotated = remember(block.verseItem, isHighlighted, primaryContainerColor, onPrimaryContainerColor, isDark, goldAccent, mushafTextColor) {
                                                val builder = AnnotatedString.Builder()
                                                val startOrn = builder.length
                                                val ornament = "﴾${toArabicNumerals(block.verseItem.verse.id)}﴿ "
                                                builder.append(ornament)
                                                val endOrn = builder.length

                                                val startText = builder.length
                                                builder.append(block.verseItem.verse.text)
                                                val endText = builder.length

                                                builder.addStringAnnotation(
                                                    tag = "AYAH_CLICK",
                                                    annotation = "${block.verseItem.surahId}_${block.verseItem.verse.id}",
                                                    start = 0,
                                                    end = builder.length
                                                )

                                                if (isHighlighted) {
                                                    builder.addStyle(
                                                        style = SpanStyle(
                                                            background = primaryContainerColor.copy(alpha = 0.3f),
                                                            color = onPrimaryContainerColor,
                                                            localeList = androidx.compose.ui.text.intl.LocaleList(androidx.compose.ui.text.intl.Locale("ar"))
                                                        ),
                                                        start = 0,
                                                        end = builder.length
                                                    )
                                                } else {
                                                    builder.addStyle(
                                                        style = SpanStyle(
                                                            color = bismillahColor,
                                                            localeList = androidx.compose.ui.text.intl.LocaleList(androidx.compose.ui.text.intl.Locale("ar"))
                                                        ),
                                                        start = startText,
                                                        end = endText
                                                    )
                                                }

                                                builder.addStyle(
                                                    style = SpanStyle(
                                                        color = goldAccent,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    start = startOrn,
                                                    end = endOrn
                                                )
                                                builder.toAnnotatedString()
                                            }

                                            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                            Text(
                                                text = annotated,
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = quranFontFamily,
                                                    fontSize = 32.sp,
                                                    lineHeight = 48.sp
                                                ),
                                                textAlign = TextAlign.Center,
                                                onTextLayout = { layoutResult = it },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp)
                                                    .pointerInput(annotated) {
                                                        detectTapGestures { offset ->
                                                            layoutResult?.let { textLayoutResult ->
                                                                val position = textLayoutResult.getOffsetForPosition(offset)
                                                                annotated.getStringAnnotations(tag = "AYAH_CLICK", start = position, end = position)
                                                                    .firstOrNull()?.let { annotation ->
                                                                        val parts = annotation.item.split("_")
                                                                        if (parts.size == 2) {
                                                                            val clickedSurahId = parts[0].toIntOrNull()
                                                                            val clickedAyahId = parts[1].toIntOrNull()
                                                                            if (clickedSurahId != null && clickedAyahId != null) {
                                                                                val targetSurah = surahs.firstOrNull { it.id == clickedSurahId }
                                                                                if (targetSurah != null) {
                                                                                    playbackManager.jumpToAyah(clickedAyahId)
                                                                                    if (currentSurahId != clickedSurahId) {
                                                                                        playbackManager.playSurah(targetSurah, clickedAyahId)
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                            }
                                                        }
                                                    }
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                        is RenderBlock.Verses -> {
                                            val primaryContainerColor = colorScheme.primaryContainer
                                            val onPrimaryContainerColor = colorScheme.onPrimaryContainer
                                            val segments = remember(block.verses, activeAyah, primaryContainerColor, onPrimaryContainerColor, isDark, goldAccent, mushafTextColor, currentSurahId, currentAyahId) {
                                                val list = mutableListOf<Any>()
                                                var builder = AnnotatedString.Builder()

                                                block.verses.forEach { verseItem ->
                                                    val verse = verseItem.verse
                                                    val surahId = verseItem.surahId
                                                    
                                                    val boundary = juzData.firstOrNull { it.surahNumber == surahId && it.verseNumber == verse.id }
                                                    if (boundary != null) {
                                                        val annotated = builder.toAnnotatedString()
                                                        if (annotated.isNotEmpty()) {
                                                            list.add(annotated)
                                                        }
                                                        list.add(boundary)
                                                        builder = AnnotatedString.Builder()
                                                    }

                                                    val startOrn = builder.length
                                                    
                                                    val startText = builder.length
                                                    builder.append(verse.text)
                                                    builder.append(" ")
                                                    val endText = builder.length
                                                    
                                                    val startSajdah = builder.length
                                                    val isSajdah = QuranHelper.isSajdahVerse(surahId, verse.id)
                                                    if (isSajdah) {
                                                        builder.append("۩ ")
                                                    }
                                                    val endSajdah = builder.length

                                                    val startOrnNum = builder.length
                                                    val ornament = "\u200F﴾${toArabicNumerals(verse.id)}﴿\u200F "
                                                    builder.append(ornament)
                                                    val endOrnNum = builder.length
                                                    
                                                    val end = builder.length

                                                    builder.addStringAnnotation(
                                                        tag = "AYAH_CLICK",
                                                        annotation = "${surahId}_${verse.id}",
                                                        start = startOrn,
                                                        end = end
                                                    )

                                                    val isCurrentlyPlaying = (surahId == currentSurahId && verse.id == currentAyahId)
                                                    val isHighlighted = isCurrentlyPlaying || (surahId == surahNumber && verse.id == activeAyah)

                                                    if (isHighlighted) {
                                                        builder.addStyle(
                                                            style = SpanStyle(
                                                                background = primaryContainerColor.copy(alpha = 0.3f),
                                                                color = onPrimaryContainerColor,
                                                                localeList = androidx.compose.ui.text.intl.LocaleList(androidx.compose.ui.text.intl.Locale("ar"))
                                                            ),
                                                            start = startOrn,
                                                            end = end
                                                        )
                                                    } else {
                                                        builder.addStyle(
                                                            style = SpanStyle(
                                                                color = mushafTextColor,
                                                                localeList = androidx.compose.ui.text.intl.LocaleList(androidx.compose.ui.text.intl.Locale("ar"))
                                                            ),
                                                            start = startText,
                                                            end = endText
                                                        )
                                                    }

                                                    if (isSajdah) {
                                                        builder.addStyle(
                                                            style = SpanStyle(
                                                                color = goldAccent,
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            start = startSajdah,
                                                            end = endSajdah - 1
                                                        )
                                                    }

                                                    builder.addStyle(
                                                        style = SpanStyle(
                                                            color = goldAccent,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        start = startOrnNum,
                                                        end = endOrnNum
                                                    )
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
                                                        JuzHeader(segment.juzNumber, goldAccent, isDark)
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                    }
                                                    is AnnotatedString -> {
                                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                                            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                                            Text(
                                                                text = segment,
                                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                                    fontSize = arabicFontSize.sp,
                                                                    fontFamily = quranFontFamily,
                                                                    lineHeight = (arabicFontSize * 2.5f).sp,
                                                                    textAlign = TextAlign.Justify
                                                                ),
                                                                onTextLayout = { layoutResult = it },
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .pointerInput(segment) {
                                                                        detectTapGestures { offset ->
                                                                            layoutResult?.let { textLayoutResult ->
                                                                                val position = textLayoutResult.getOffsetForPosition(offset)
                                                                                segment.getStringAnnotations(tag = "AYAH_CLICK", start = position, end = position)
                                                                                    .firstOrNull()?.let { annotation ->
                                                                                        val parts = annotation.item.split("_")
                                                                                        if (parts.size == 2) {
                                                                                            val clickedSurahId = parts[0].toIntOrNull()
                                                                                            val clickedAyahId = parts[1].toIntOrNull()
                                                                                            if (clickedSurahId != null && clickedAyahId != null) {
                                                                                                val targetSurah = surahs.firstOrNull { it.id == clickedSurahId }
                                                                                                if (targetSurah != null) {
                                                                                                    playbackManager.jumpToAyah(clickedAyahId)
                                                                                                    if (currentSurahId != clickedSurahId) {
                                                                                                        playbackManager.playSurah(targetSurah, clickedAyahId)
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                            }
                                                                        }
                                                                    }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                val pageVerses = remember(pageContent) {
                                    pageContent.filterIsInstance<PageContent.VerseItem>()
                                }

                                val hasSajdahInPage = remember(pageVerses) {
                                    pageVerses.any { QuranHelper.isSajdahVerse(it.surahId, it.verse.id) }
                                }

                                if (hasSajdahInPage) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(12.dp)),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(Color(0xFFD4AF37).copy(alpha = 0.15f), CircleShape),
                                                contentAlignment = Alignment.Center
                                             ) {
                                                 Text(
                                                     text = "۩",
                                                     color = Color(0xFFD4AF37),
                                                     fontSize = 20.sp,
                                                     fontWeight = FontWeight.Bold
                                                 )
                                             }
                                             Column(modifier = Modifier.weight(1f)) {
                                                 Text(
                                                     text = if (lang == "ar") "سجدة تلاوة" else "Sajdah al-Tilawah",
                                                     fontWeight = FontWeight.Bold,
                                                     style = MaterialTheme.typography.titleSmall,
                                                     color = Color(0xFFD4AF37)
                                                 )
                                                 Spacer(modifier = Modifier.height(2.dp))
                                                 Text(
                                                     text = if (lang == "ar") {
                                                         "تحتوي هذه الصفحة على آية سجود. يُسنّ السجود (سجدة تلاوة) عند قراءتها أو سماعها."
                                                     } else {
                                                         "This page contains a prostration verse. It is recommended to perform prostration when reading or hearing it."
                                                     },
                                                     style = MaterialTheme.typography.bodySmall,
                                                     color = MaterialTheme.colorScheme.onSurfaceVariant
                                                 )
                                             }
                                        }
                                    }
                                }

                                if (pageVerses.isNotEmpty() && lang == "en") {
                                    var showTranslation by remember { mutableStateOf(false) }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    TextButton(onClick = { showTranslation = !showTranslation }) {
                                        Text(
                                            text = if (showTranslation) {
                                                stringResource(R.string.hide_translation)
                                            } else {
                                                stringResource(R.string.show_translation)
                                            },
                                            color = goldAccent,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }

                                    AnimatedVisibility(visible = showTranslation) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            HorizontalDivider(
                                                thickness = 0.5.dp,
                                                color = goldAccent.copy(alpha = 0.3f)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            pageVerses.forEach { verseItem ->
                                                val verse = verseItem.verse
                                                val surahId = verseItem.surahId
                                                val isHighlighted = (surahId == currentSurahId && verse.id == currentAyahId) || (surahId == surahNumber && verse.id == activeAyah)
                                                val isSajdah = QuranHelper.isSajdahVerse(surahId, verse.id)
                                                
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = stringResource(R.string.verse_number_prefix, verse.id).trim(),
                                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                                color = if (isHighlighted) colorScheme.primary else colorScheme.onSurfaceVariant
                                                            )
                                                            if (isSajdah) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .background(Color(0xFFD4AF37).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                                        .border(0.5.dp, Color(0xFFD4AF37), RoundedCornerShape(4.dp))
                                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text(
                                                                        text = if (lang == "ar") "سجدة" else "Sajdah",
                                                                        color = Color(0xFFD4AF37),
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = verse.translation,
                                                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                                            color = if (isHighlighted) colorScheme.primary else colorScheme.onSurfaceVariant,
                                                            textAlign = TextAlign.Start
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            val firstVerse = pageContent.firstOrNull { it is PageContent.VerseItem } as? PageContent.VerseItem
                            val mushafPageNum = if (firstVerse != null) {
                                QuranHelper.getMushafPageNumber(firstVerse.surahId, firstVerse.verse.id)
                            } else {
                                1
                            }
                            val footerText = if (lang == "ar") {
                                "الصفحة ${toArabicNumerals(mushafPageNum)}"
                            } else {
                                "Page $mushafPageNum"
                            }

                            Text(
                                text = "﴾ $footerText ﴿",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = quranFontFamily,
                                    fontSize = 16.sp
                                ),
                                color = goldAccent,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }

            // Audio player at bottom
            if (currentSurah != null) {
                QuranAudioPlayer(
                    surah = currentSurah,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(mushafBgColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(onClick = { navigator.goBack() }) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = stringResource(R.string.go_back),
                        tint = goldAccent
                    )
                }

                // Centered Surah and Juz Name
                if (currentSurah != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "سُورَةُ ${currentSurah.name}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = quranFontFamily,
                                fontSize = 22.sp
                            ),
                            color = goldAccent,
                            maxLines = 1
                        )
                        val subtitleText = if (lang == "ar") {
                            "الجزء ${toArabicNumerals(currentJuz)} • ${currentSurah.type.let { if (it.lowercase() == "meccan") "مكية" else "مدنية" }}"
                        } else {
                            "Juz $currentJuz • ${currentSurah.transliteration}"
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelSmall,
                            color = goldAccent.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }

                // Sleep timer button
                var showSleepMenu by remember { mutableStateOf(false) }
                val sleepTimerRemaining by playbackManager.sleepTimerRemaining.collectAsState()
                val endOfSurahEnabled by playbackManager.endOfSurahEnabled.collectAsState()

                Box(contentAlignment = Alignment.Center) {
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
                                contentDescription = stringResource(R.string.sleep_mode),
                                tint = goldAccent
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

            // Beautiful ornament bottom divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    thickness = 0.5.dp,
                    color = goldAccent.copy(alpha = 0.4f)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer { rotationZ = 45f }
                        .background(goldAccent)
                )
            }
        }
    }
}

@Composable
fun JuzHeader(juzNumber: Int, goldAccent: Color = Color(0xFFC5A059), isDark: Boolean = false) {
    val bgColor = if (isDark) Color(0xFF2A2520) else Color(0xFFF0E6D2)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(0.4f),
            thickness = 0.5.dp,
            color = goldAccent.copy(alpha = 0.5f)
        )

        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .background(
                    color = bgColor.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.small
                )
                .padding(horizontal = 20.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.juz_arabic_label, toArabicNumerals(juzNumber)),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = goldAccent
            )
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(0.4f),
            thickness = 0.5.dp,
            color = goldAccent.copy(alpha = 0.5f)
        )
    }
}

fun toArabicNumerals(n: Int): String =
    n.toString().map { c -> if (c.isDigit()) '٠' + (c - '0') else c }.joinToString("")

fun getJuzNumber(surahId: Int, verseId: Int): Int {
    val match = juzData.lastOrNull { boundary ->
        boundary.surahNumber < surahId || (boundary.surahNumber == surahId && boundary.verseNumber <= verseId)
    }
    return match?.juzNumber ?: 1
}

@Composable
fun SurahStartBanner(
    surah: QuranHelper.Surah,
    goldAccent: Color,
    textColor: Color,
    isDark: Boolean,
    fontFamily: FontFamily,
    lang: String
) {
    val frameBg = if (isDark) Color(0xFF231E1A) else Color(0xFFF3EDE0)
    val typeText = if (lang == "ar") {
        if (surah.type.lowercase() == "meccan") "مكية" else "مدنية"
    } else {
        if (surah.type.lowercase() == "meccan") "Meccan" else "Medinan"
    }
    val versesText = if (lang == "ar") {
        "آياتها ${toArabicNumerals(surah.totalVerses)}"
    } else {
        "${surah.totalVerses} Verses"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = frameBg),
        border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(4.dp).graphicsLayer { rotationZ = 45f }.background(goldAccent))
                HorizontalDivider(modifier = Modifier.width(40.dp), thickness = 0.5.dp, color = goldAccent.copy(alpha = 0.5f))
                Box(modifier = Modifier.size(6.dp).graphicsLayer { rotationZ = 45f }.background(goldAccent))
                HorizontalDivider(modifier = Modifier.width(40.dp), thickness = 0.5.dp, color = goldAccent.copy(alpha = 0.5f))
                Box(modifier = Modifier.size(4.dp).graphicsLayer { rotationZ = 45f }.background(goldAccent))
            }

            Text(
                text = "سُورَةُ ${surah.name}",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    fontSize = 32.sp
                ),
                color = goldAccent,
                textAlign = TextAlign.Center
            )

            Text(
                text = "$typeText • $versesText",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = textColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(4.dp).graphicsLayer { rotationZ = 45f }.background(goldAccent))
                HorizontalDivider(modifier = Modifier.width(40.dp), thickness = 0.5.dp, color = goldAccent.copy(alpha = 0.5f))
                Box(modifier = Modifier.size(6.dp).graphicsLayer { rotationZ = 45f }.background(goldAccent))
                HorizontalDivider(modifier = Modifier.width(40.dp), thickness = 0.5.dp, color = goldAccent.copy(alpha = 0.5f))
                Box(modifier = Modifier.size(4.dp).graphicsLayer { rotationZ = 45f }.background(goldAccent))
            }
        }
    }
}

fun Modifier.mushafBorder(color: Color): Modifier = this.drawBehind {
    val strokeWidth = 1.5.dp.toPx()
    val gap = 4.dp.toPx()
    
    // Outer border
    drawRect(
        color = color,
        topLeft = Offset.Zero,
        size = size,
        style = Stroke(width = strokeWidth)
    )
    
    // Inner border
    drawRect(
        color = color,
        topLeft = Offset(strokeWidth + gap, strokeWidth + gap),
        size = Size(
            size.width - 2 * (strokeWidth + gap),
            size.height - 2 * (strokeWidth + gap)
        ),
        style = Stroke(width = strokeWidth * 0.7f)
    )
    
    // Corner diamond ornaments
    val cornerOffset = strokeWidth + gap
    val diamondSize = 6.dp.toPx()
    val corners = listOf(
        Offset(cornerOffset, cornerOffset),
        Offset(size.width - cornerOffset, cornerOffset),
        Offset(cornerOffset, size.height - cornerOffset),
        Offset(size.width - cornerOffset, size.height - cornerOffset)
    )
    for (corner in corners) {
        val path = Path().apply {
            moveTo(corner.x, corner.y - diamondSize / 2)
            lineTo(corner.x + diamondSize / 2, corner.y)
            lineTo(corner.x, corner.y + diamondSize / 2)
            lineTo(corner.x - diamondSize / 2, corner.y)
            close()
        }
        drawPath(path, color)
    }
}

fun calculateLines(
    items: List<PageContent>,
    widthPx: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle
): Int {
    var lineCount = 0
    val currentVerses = mutableListOf<PageContent.VerseItem>()
    
    fun flushVerses() {
        if (currentVerses.isNotEmpty()) {
            val builder = AnnotatedString.Builder()
            currentVerses.forEach { verseItem ->
                val ornament = "﴾${toArabicNumerals(verseItem.verse.id)}﴿ "
                builder.append(ornament)
                builder.append(verseItem.verse.text)
                builder.append(" ")
                if (QuranHelper.isSajdahVerse(verseItem.surahId, verseItem.verse.id)) {
                    builder.append("۩ ")
                }
            }
            val textLayoutResult = textMeasurer.measure(
                text = builder.toAnnotatedString(),
                style = style,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx.toInt())
            )
            lineCount += textLayoutResult.lineCount
            currentVerses.clear()
        }
    }
    
    for (item in items) {
        when (item) {
            is PageContent.SurahHeader -> {
                flushVerses()
                lineCount += 5
            }
            is PageContent.Bismillah -> {
                flushVerses()
                lineCount += 2
            }
            is PageContent.VerseItem -> {
                val boundary = juzData.firstOrNull { it.surahNumber == item.surahId && it.verseNumber == item.verse.id }
                if (boundary != null) {
                    flushVerses()
                    lineCount += 2
                }
                currentVerses.add(item)
            }
        }
    }
    flushVerses()
    return lineCount
}
