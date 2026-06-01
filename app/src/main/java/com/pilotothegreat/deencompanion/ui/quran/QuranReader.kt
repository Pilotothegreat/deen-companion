package com.pilotothegreat.deencompanion.ui.quran

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
    val pages = remember(surah, arabicFontSize, screenHeightDp) {
        if (surah == null) return@remember emptyList<List<QuranHelper.Verse>>()
        
        val availableHeight = (screenHeightDp - 260f).coerceAtLeast(100f)
        val approximateLineHeight = arabicFontSize * 2.3f
        val maxLines = (availableHeight / approximateLineHeight).toInt().coerceAtLeast(2)
        val charsPerLine = (320f / (arabicFontSize * 0.55f)).toInt().coerceAtLeast(20)
        val maxCharsPerPage = maxLines * charsPerLine
        
        val pageList = mutableListOf<List<QuranHelper.Verse>>()
        var currentPageVerses = mutableListOf<QuranHelper.Verse>()
        var currentPageCharCount = 0
        
        for (verse in surah.verses) {
            val verseLength = verse.text.length
            if (currentPageCharCount + verseLength > maxCharsPerPage && currentPageVerses.isNotEmpty()) {
                pageList.add(currentPageVerses)
                currentPageVerses = mutableListOf()
                currentPageCharCount = 0
            }
            currentPageVerses.add(verse)
            currentPageCharCount += verseLength
        }
        if (currentPageVerses.isNotEmpty()) {
            pageList.add(currentPageVerses)
        }
        pageList
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

    val themeState by appPreferenceRepo.theme.collectAsState(Theme.AutoMaterial)
    val isDark = themeState.isDark()

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
                    val pageVerses = pages.getOrNull(pageIdx) ?: emptyList()

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
                                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Surah banner and Bismillah on first page
                                if (pageIdx == 0) {
                                    SurahStartBanner(
                                        surah = surah,
                                        goldAccent = goldAccent,
                                        textColor = mushafTextColor,
                                        isDark = isDark,
                                        fontFamily = quranFontFamily,
                                        lang = lang
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (surah.id != 9 && surah.id != 1) {
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
                                }

                                // Build annotated string with inline juz headers
                                val primaryContainerColor = colorScheme.primaryContainer
                                val onPrimaryContainerColor = colorScheme.onPrimaryContainer
                                val segments = remember(pageVerses, activeAyah, primaryContainerColor, onPrimaryContainerColor, isDark, goldAccent, mushafTextColor) {
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
                                        val verseText = "${verse.text} "
                                        builder.append(verseText)

                                        // Style the verse text
                                        if (verse.id != activeAyah) {
                                            builder.addStyle(
                                                style = SpanStyle(
                                                    color = mushafTextColor,
                                                    localeList = androidx.compose.ui.text.intl.LocaleList(androidx.compose.ui.text.intl.Locale("ar"))
                                                ),
                                                start = start,
                                                end = builder.length
                                            )
                                        }

                                        val startOrn = builder.length
                                        val ornament = "﴾${toArabicNumerals(verse.id)}﴿ "
                                        builder.append(ornament)
                                        val endOrn = builder.length

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
                                                    background = primaryContainerColor.copy(alpha = 0.3f),
                                                    color = onPrimaryContainerColor,
                                                    localeList = androidx.compose.ui.text.intl.LocaleList(androidx.compose.ui.text.intl.Locale("ar"))
                                                ),
                                                start = start,
                                                end = end
                                            )
                                        }

                                        // Ayah number ornament always gold
                                        builder.addStyle(
                                            style = SpanStyle(
                                                color = goldAccent,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            start = startOrn,
                                            end = endOrn
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
                                                ClickableText(
                                                    text = segment,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontSize = arabicFontSize.sp,
                                                        fontFamily = quranFontFamily,
                                                        lineHeight = (arabicFontSize * 2.5f).sp,
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

                                // Collapsible translation
                                if (pageVerses.any { it.translation.isNotEmpty() }) {
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

                                            pageVerses.forEach { verse ->
                                                val isHighlighted = verse.id == activeAyah
                                                Text(
                                                    text = stringResource(R.string.verse_number_prefix, verse.id) + verse.translation,
                                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
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

                            // Page Number ornament
                            Text(
                                text = "﴾ ${toArabicNumerals(pageIdx + 1)} ﴿",
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
            QuranAudioPlayer(
                surah = surah,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Mushaf-style header
        val currentPageVerses = pages.getOrNull(pagerState.currentPage) ?: emptyList()
        val firstVerseOnPage = currentPageVerses.firstOrNull()
        val currentJuz = remember(firstVerseOnPage, surah) {
            if (firstVerseOnPage != null && surah != null) {
                getJuzNumber(surah.id, firstVerseOnPage.id)
            } else {
                1
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
                if (surah != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "سُورَةُ ${surah.name}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = quranFontFamily,
                                fontSize = 22.sp
                            ),
                            color = goldAccent,
                            maxLines = 1
                        )
                        val subtitleText = if (lang == "ar") {
                            "الجزء ${toArabicNumerals(currentJuz)} • ${surah.type.let { if (it.lowercase() == "meccan") "مكية" else "مدنية" }}"
                        } else {
                            "Juz $currentJuz • ${surah.transliteration}"
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
        surah.type.replaceFirstChar { it.uppercase() }
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
