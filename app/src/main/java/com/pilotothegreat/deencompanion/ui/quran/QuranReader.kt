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
import androidx.compose.ui.graphics.drawscope.DrawScope
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

    val arabicFontSize by appPreferenceRepo.quranArabicFontSize.collectAsState(initial = 32)
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

    val activePageIndex = remember(globalPages, surahNumber, scrollToVerse) {
        val targetVerse = scrollToVerse ?: 1
        globalPages.indexOfFirst { page ->
            page.any { item ->
                item is PageContent.VerseItem && item.surahId == surahNumber && item.verse.id == targetVerse
            }
        }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(initialPage = activePageIndex, pageCount = { globalPages.size })

    // Scroll to the active page when first loaded or changed
    var hasScrolledToTarget by remember(surahNumber, scrollToVerse) { mutableStateOf(false) }
    LaunchedEffect(activePageIndex, globalPages) {
        if (activePageIndex >= 0 && globalPages.isNotEmpty() && !hasScrolledToTarget) {
            pagerState.scrollToPage(activePageIndex)
            hasScrolledToTarget = true
        }
    }

    val pageIndexForActiveAyah = remember(currentSurahId, currentAyahId, globalPages, surahNumber) {
        if (currentSurahId == surahNumber) {
            globalPages.indexOfFirst { page ->
                page.any { item ->
                    item is PageContent.VerseItem && item.surahId == currentSurahId && item.verse.id == currentAyahId
                }
            }
        } else {
            -1
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
                    val pageVerses = remember(pageContent) {
                        pageContent.filterIsInstance<PageContent.VerseItem>()
                    }
                    var showTranslation by remember { mutableStateOf(false) }
                    val isScrollable = showTranslation

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

                    // Dynamically calculate adjusted font size if not scrollable (translation hidden)
                    val adjustedFontSize = remember(pageContent, arabicFontSize, screenHeightDp, widthPx, isScrollable) {
                        var size = arabicFontSize.toFloat()
                        if (!isScrollable) {
                            val maxContentHeightPx = with(density) { (screenHeightDp - 228f).coerceAtLeast(300f).dp.toPx() }
                            val hasSajdah = pageVerses.any { QuranHelper.isSajdahVerse(it.surahId, it.verse.id) }
                            
                            while (size > 14f) {
                                val estimatedHeight = calculatePageHeight(
                                    blocks = blocks,
                                    fontSizeSp = size,
                                    fontFamily = quranFontFamily,
                                    widthPx = widthPx.toInt(),
                                    textMeasurer = textMeasurer,
                                    density = density,
                                    lang = lang,
                                    hasSajdah = hasSajdah
                                )
                                if (estimatedHeight <= maxContentHeightPx) {
                                    break
                                }
                                size -= 1f
                            }
                        }
                        size
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
                            // Top Header inside the Mus'haf border frame
                            if (currentSurah != null) {
                                InnerPageHeader(
                                    surahName = currentSurah.name,
                                    juzNumber = currentJuz,
                                    goldAccent = goldAccent,
                                    textColor = mushafTextColor,
                                    fontFamily = quranFontFamily,
                                    lang = lang
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            val scrollState = rememberScrollState()
                            Column(
                                modifier = if (isScrollable) {
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .verticalScroll(scrollState)
                                } else {
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                },
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
                                            val bFontSize = adjustedFontSize * 1.3f
                                            val bLineHeight = bFontSize * 1.5f
                                            Text(
                                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = quranFontFamily,
                                                    fontSize = bFontSize.sp,
                                                    lineHeight = bLineHeight.sp
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
                                                val ornament = "\u200F﴿${toArabicNumerals(block.verseItem.verse.id)}﴾\u200F "
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
                                                    fontSize = (adjustedFontSize * 1.3f).sp,
                                                    lineHeight = ((adjustedFontSize * 1.3f) * 1.5f).sp
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
                                                                                    playbackManager.playOrJumpToAyah(targetSurah, clickedAyahId)
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
                                                    val ornament = "\u200F﴿${toArabicNumerals(verse.id)}﴾\u200F "
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
                                                key(segment) {
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
                                                                        fontSize = adjustedFontSize.sp,
                                                                        fontFamily = quranFontFamily,
                                                                        lineHeight = (adjustedFontSize * 1.8f).sp,
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
                                                                                                        playbackManager.playOrJumpToAyah(targetSurah, clickedAyahId)
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
                                "﴿ الصفحة ${toArabicNumerals(mushafPageNum)} ﴾"
                            } else {
                                "﴾ Page $mushafPageNum ﴿"
                            }

                            Text(
                                text = footerText,
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

                Spacer(modifier = Modifier.weight(1f))

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
fun InnerPageHeader(
    surahName: String,
    juzNumber: Int,
    goldAccent: Color,
    textColor: Color,
    fontFamily: FontFamily,
    lang: String
) {
    val surahText = if (lang == "ar") "سُورَةُ $surahName" else "Surah $surahName"
    val juzText = if (lang == "ar") "الجُزْءُ ${toArabicNumerals(juzNumber)}" else "Juz $juzNumber"
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = surahText,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    fontSize = 12.sp,
                    color = goldAccent
                )
            )
            
            Text(
                text = juzText,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    fontSize = 12.sp,
                    color = goldAccent
                )
            )
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = goldAccent.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
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
    val earsBgColor = if (isDark) Color(0xFF0F2620) else Color(0xFF144B3E)
    val middleBgColor = if (isDark) Color(0xFF22201C) else Color(0xFFFAF5E8)

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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .drawBehind {
                val size = this.size
                // 1. Draw whole middle background
                drawRect(middleBgColor, topLeft = Offset.Zero, size = size)
                
                // 2. Define left ear path
                val leftEarPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width * 0.18f, 0f)
                    cubicTo(
                        size.width * 0.18f, 0f,
                        size.width * 0.15f, size.height * 0.15f,
                        size.width * 0.15f, size.height * 0.25f
                    )
                    lineTo(size.width * 0.15f, size.height * 0.38f)
                    cubicTo(
                        size.width * 0.15f, size.height * 0.38f,
                        size.width * 0.12f, size.height * 0.5f,
                        size.width * 0.15f, size.height * 0.62f
                    )
                    lineTo(size.width * 0.15f, size.height * 0.75f)
                    cubicTo(
                        size.width * 0.15f, size.height * 0.75f,
                        size.width * 0.15f, size.height * 0.85f,
                        size.width * 0.18f, size.height
                    )
                    lineTo(0f, size.height)
                    close()
                }

                // 3. Define right ear path
                val rightEarPath = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width * 0.82f, 0f)
                    cubicTo(
                        size.width * 0.82f, 0f,
                        size.width * 0.85f, size.height * 0.15f,
                        size.width * 0.85f, size.height * 0.25f
                    )
                    lineTo(size.width * 0.85f, size.height * 0.38f)
                    cubicTo(
                        size.width * 0.85f, size.height * 0.38f,
                        size.width * 0.88f, size.height * 0.5f,
                        size.width * 0.85f, size.height * 0.62f
                    )
                    lineTo(size.width * 0.85f, size.height * 0.75f)
                    cubicTo(
                        size.width * 0.85f, size.height * 0.75f,
                        size.width * 0.85f, size.height * 0.85f,
                        size.width * 0.82f, size.height
                    )
                    lineTo(size.width, size.height)
                    close()
                }

                // 4. Fill ears
                drawPath(leftEarPath, earsBgColor)
                drawPath(rightEarPath, earsBgColor)

                // 5. Draw scalloped inner borders
                val leftBorderPath = Path().apply {
                    moveTo(size.width * 0.18f, 0f)
                    cubicTo(
                        size.width * 0.18f, 0f,
                        size.width * 0.15f, size.height * 0.15f,
                        size.width * 0.15f, size.height * 0.25f
                    )
                    lineTo(size.width * 0.15f, size.height * 0.38f)
                    cubicTo(
                        size.width * 0.15f, size.height * 0.38f,
                        size.width * 0.12f, size.height * 0.5f,
                        size.width * 0.15f, size.height * 0.62f
                    )
                    lineTo(size.width * 0.15f, size.height * 0.75f)
                    cubicTo(
                        size.width * 0.15f, size.height * 0.75f,
                        size.width * 0.15f, size.height * 0.85f,
                        size.width * 0.18f, size.height
                    )
                }
                val rightBorderPath = Path().apply {
                    moveTo(size.width * 0.82f, 0f)
                    cubicTo(
                        size.width * 0.82f, 0f,
                        size.width * 0.85f, size.height * 0.15f,
                        size.width * 0.85f, size.height * 0.25f
                    )
                    lineTo(size.width * 0.85f, size.height * 0.38f)
                    cubicTo(
                        size.width * 0.85f, size.height * 0.38f,
                        size.width * 0.88f, size.height * 0.5f,
                        size.width * 0.85f, size.height * 0.62f
                    )
                    lineTo(size.width * 0.85f, size.height * 0.75f)
                    cubicTo(
                        size.width * 0.85f, size.height * 0.75f,
                        size.width * 0.85f, size.height * 0.85f,
                        size.width * 0.82f, size.height
                    )
                }
                drawPath(leftBorderPath, goldAccent, style = Stroke(width = 1.5.dp.toPx()))
                drawPath(rightBorderPath, goldAccent, style = Stroke(width = 1.5.dp.toPx()))

                // 6. Draw outer rectangular border
                drawRect(goldAccent, topLeft = Offset.Zero, size = size, style = Stroke(width = 1.5.dp.toPx()))

                // 7. Draw Islamic stars and dots on ears
                val starSize = size.height * 0.35f
                drawIslamicStar(Offset(size.width * 0.08f, size.height / 2), starSize, goldAccent)
                drawIslamicStar(Offset(size.width * 0.92f, size.height / 2), starSize, goldAccent)

                drawCircle(goldAccent, radius = 2.5.dp.toPx(), center = Offset(size.width * 0.08f, size.height * 0.22f))
                drawCircle(goldAccent, radius = 2.5.dp.toPx(), center = Offset(size.width * 0.08f, size.height * 0.78f))
                drawCircle(goldAccent, radius = 2.5.dp.toPx(), center = Offset(size.width * 0.92f, size.height * 0.22f))
                drawCircle(goldAccent, radius = 2.5.dp.toPx(), center = Offset(size.width * 0.92f, size.height * 0.78f))
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "سُورَةُ ${surah.name}",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    fontSize = 24.sp
                ),
                color = goldAccent,
                textAlign = TextAlign.Center
            )

            Text(
                text = "$typeText • $versesText",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    fontSize = 11.sp
                ),
                color = textColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun DrawScope.drawIslamicStar(center: Offset, size: Float, color: Color) {
    val r = size / 2
    val path = Path().apply {
        // First square
        moveTo(center.x - r, center.y)
        lineTo(center.x, center.y - r)
        lineTo(center.x + r, center.y)
        lineTo(center.x, center.y + r)
        close()
        // Second square rotated 45 degrees
        val rRot = r * 0.7071f
        moveTo(center.x - rRot, center.y - rRot)
        lineTo(center.x + rRot, center.y - rRot)
        lineTo(center.x + rRot, center.y + rRot)
        lineTo(center.x - rRot, center.y + rRot)
        close()
    }
    drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
    drawCircle(color, radius = r * 0.3f, center = center)
}

fun Modifier.mushafBorder(color: Color): Modifier = this.drawBehind {
    val strokeWidth = 2.dp.toPx()
    val gap = 4.dp.toPx()
    
    // Outer border
    drawRect(
        color = color,
        topLeft = Offset.Zero,
        size = size,
        style = Stroke(width = strokeWidth)
    )
    
    // Inner border
    val innerOffset = strokeWidth + gap
    drawRect(
        color = color,
        topLeft = Offset(innerOffset, innerOffset),
        size = Size(
            size.width - 2 * innerOffset,
            size.height - 2 * innerOffset
        ),
        style = Stroke(width = strokeWidth * 0.5f)
    )
    
    // Diagonal lines at the four corners connecting outer and inner corners
    drawLine(
        color = color,
        start = Offset.Zero,
        end = Offset(innerOffset, innerOffset),
        strokeWidth = strokeWidth * 0.5f
    )
    drawLine(
        color = color,
        start = Offset(size.width, 0f),
        end = Offset(size.width - innerOffset, innerOffset),
        strokeWidth = strokeWidth * 0.5f
    )
    drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(innerOffset, size.height - innerOffset),
        strokeWidth = strokeWidth * 0.5f
    )
    drawLine(
        color = color,
        start = Offset(size.width, size.height),
        end = Offset(size.width - innerOffset, size.height - innerOffset),
        strokeWidth = strokeWidth * 0.5f
    )

    // Draw solid diamonds at the four inner corners
    val dSize = 4.dp.toPx()
    drawDiamond(Offset(innerOffset, innerOffset), dSize, color)
    drawDiamond(Offset(size.width - innerOffset, innerOffset), dSize, color)
    drawDiamond(Offset(innerOffset, size.height - innerOffset), dSize, color)
    drawDiamond(Offset(size.width - innerOffset, size.height - innerOffset), dSize, color)
}

private fun DrawScope.drawDiamond(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - size)
        lineTo(center.x + size, center.y)
        lineTo(center.x, center.y + size)
        lineTo(center.x - size, center.y)
        close()
    }
    drawPath(path, color)
}

fun calculatePageHeight(
    blocks: List<RenderBlock>,
    fontSizeSp: Float,
    fontFamily: FontFamily,
    widthPx: Int,
    textMeasurer: TextMeasurer,
    density: androidx.compose.ui.unit.Density,
    lang: String,
    hasSajdah: Boolean
): Float {
    var totalHeightPx = 0f
    
    val spacingPx = with(density) { 8.dp.toPx() }
    
    blocks.forEach { block ->
        when (block) {
            is RenderBlock.Header -> {
                // SurahStartBanner: ~100.dp
                totalHeightPx += with(density) { 100.dp.toPx() }
            }
            is RenderBlock.BismillahText -> {
                val bFontSize = fontSizeSp * 1.3f
                val bLineHeight = bFontSize * 1.5f
                val paddingDp = 24f
                totalHeightPx += with(density) { (bLineHeight.sp.toPx() + paddingDp.dp.toPx()) }
            }
            is RenderBlock.FatihaBismillah -> {
                val bFontSize = fontSizeSp * 1.3f
                val bLineHeight = bFontSize * 1.5f
                val paddingDp = 24f
                totalHeightPx += with(density) { (bLineHeight.sp.toPx() + paddingDp.dp.toPx()) }
            }
            is RenderBlock.Verses -> {
                val builder = AnnotatedString.Builder()
                var hasJuzHeader = false
                block.verses.forEach { verseItem ->
                    val verse = verseItem.verse
                    val surahId = verseItem.surahId
                    
                    val boundary = juzData.firstOrNull { it.surahNumber == surahId && it.verseNumber == verse.id }
                    if (boundary != null) {
                        hasJuzHeader = true
                    }
                    builder.append(verse.text)
                    builder.append(" ")
                    if (QuranHelper.isSajdahVerse(surahId, verse.id)) {
                        builder.append("۩ ")
                    }
                    val ornament = "\u200F﴿${toArabicNumerals(verse.id)}﴾\u200F "
                    builder.append(ornament)
                }
                
                val annotatedString = builder.toAnnotatedString()
                val textStyle = TextStyle(
                    fontSize = fontSizeSp.sp,
                    fontFamily = fontFamily,
                    lineHeight = (fontSizeSp * 1.8f).sp,
                    textAlign = TextAlign.Justify
                )
                
                val textLayoutResult = textMeasurer.measure(
                    text = annotatedString,
                    style = textStyle,
                    constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx)
                )
                totalHeightPx += textLayoutResult.size.height.toFloat()
                
                if (hasJuzHeader) {
                    totalHeightPx += with(density) { 50.dp.toPx() }
                }
            }
        }
        totalHeightPx += spacingPx
    }
    
    if (hasSajdah) {
        totalHeightPx += with(density) { 90.dp.toPx() }
    }
    
    totalHeightPx += with(density) { 68.dp.toPx() }
    
    return totalHeightPx
}

