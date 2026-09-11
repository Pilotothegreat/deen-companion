package com.pilotothegreat.deencompanion.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.text.Numerals
import com.pilotothegreat.deencompanion.data.quran.MushafPage
import com.pilotothegreat.deencompanion.data.quran.Quran
import com.pilotothegreat.deencompanion.data.quran.Revelation
import com.pilotothegreat.deencompanion.data.quran.Surah
import com.pilotothegreat.deencompanion.data.quran.Verse
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.components.LoadingBox
import com.pilotothegreat.deencompanion.ui.navigation.ReaderKey
import com.pilotothegreat.deencompanion.ui.quran.surahName
import com.pilotothegreat.deencompanion.ui.theme.UthmanicHafs
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val BISMILLAH = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"

@Composable
fun ReaderScreen(
    key: ReaderKey,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = koinViewModel { parametersOf(key) },
) {
    val quran by viewModel.quran.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val locale = currentLocale()
    val snackbar = remember { SnackbarHostState() }
    var selected by remember { mutableStateOf<Verse?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.playbackErrors.collect { snackbar.showSnackbar(resources.getString(R.string.playback_error)) }
    }

    val loaded = quran ?: return LoadingBox()
    val pagerState = rememberPagerState(initialPage = viewModel.initialPage - 1) { loaded.pages.size }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { viewModel.onPageSettled(it + 1) }
    }
    // Follow the recitation onto the next page unless the reader is swiping.
    LaunchedEffect(playback.surah, playback.ayah, playback.isPlaying) {
        if (!playback.isActive || !playback.isPlaying) return@LaunchedEffect
        val page = loaded.pageOf(playback.surah, playback.ayah) - 1
        if (page != pagerState.currentPage && !pagerState.isScrollInProgress) pagerState.animateScrollToPage(page)
    }

    val highlight = if (playback.isActive) playback.surah to playback.ayah else viewModel.targetAyah
    val current = loaded.pages[pagerState.currentPage]
    val firstVerse = current.verses.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(surahName(loaded.surah(firstVerse.surah), locale)) },
                subtitle = {
                    Text(stringResource(R.string.page_juz, Formatters.number(current.number, locale), Formatters.number(current.juz, locale)))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                },
                actions = {
                    IconToggleButton(checked = prefs.showTranslation, onCheckedChange = { viewModel.toggleTranslation() }) {
                        Icon(Icons.Rounded.Translate, contentDescription = stringResource(R.string.show_translation))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val layoutDirection = LocalLayoutDirection.current
            // Pages turn right-to-left like a printed mushaf.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalPager(state = pagerState, beyondViewportPageCount = 1, key = { it }) { index ->
                    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                        MushafPageView(
                            page = loaded.pages[index],
                            quran = loaded,
                            fontSize = prefs.fontSize,
                            showTranslation = prefs.showTranslation,
                            highlight = highlight,
                            bookmarks = bookmarks,
                            bottomSpace = playback.isActive,
                            onAyahClick = { selected = it },
                        )
                    }
                }
            }
            if (playback.isActive) {
                PlayerToolbar(
                    state = playback,
                    title = surahName(loaded.surah(playback.surah), locale),
                    onTogglePlay = viewModel::togglePlayPause,
                    onPrevious = viewModel::previous,
                    onNext = viewModel::next,
                    onStop = viewModel::stop,
                    onReciter = viewModel::setReciter,
                    onSleepTimer = viewModel::setSleepTimer,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                )
            }
        }
    }

    selected?.let { verse ->
        val bookmarked = (verse.surah to verse.number) in bookmarks
        AyahSheet(
            verse = verse,
            surah = loaded.surah(verse.surah),
            bookmarked = bookmarked,
            onPlay = {
                viewModel.play(verse)
                selected = null
            },
            onBookmark = { viewModel.setBookmark(verse, !bookmarked) },
            onDismiss = { selected = null },
        )
    }
}

@Composable
private fun MushafPageView(
    page: MushafPage,
    quran: Quran,
    fontSize: Int,
    showTranslation: Boolean,
    highlight: Pair<Int, Int>?,
    bookmarks: Set<Pair<Int, Int>>,
    bottomSpace: Boolean,
    onAyahClick: (Verse) -> Unit,
) {
    val locale = currentLocale()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                page.verses.groupBy { it.surah }.forEach { (surahNumber, verses) ->
                    val surah = quran.surah(surahNumber)
                    if (verses.first().number == 1) {
                        SurahBanner(surah)
                        if (surahNumber != 1 && surahNumber != 9) {
                            Text(
                                BISMILLAH,
                                fontFamily = UthmanicHafs,
                                fontSize = (fontSize * 1.05f).sp,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    AyahText(verses, fontSize, highlight, bookmarks, onAyahClick)
                }
                Text(
                    Numerals.toArabicIndic(page.number.toString()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (showTranslation) {
            page.verses.forEach { verse ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "${Formatters.number(verse.surah, locale)}:${Formatters.number(verse.number, locale)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(verse.translation, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (bottomSpace) Spacer(Modifier.size(96.dp))
    }
}

@Composable
private fun AyahText(
    verses: List<Verse>,
    fontSize: Int,
    highlight: Pair<Int, Int>?,
    bookmarks: Set<Pair<Int, Int>>,
    onAyahClick: (Verse) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // Ayahs are tappable, but must not look like hyperlinks.
    val plainLink = TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.None))
    val text = buildAnnotatedString {
        verses.forEach { verse ->
            val key = verse.surah to verse.number
            val emphasis = when (key) {
                highlight -> SpanStyle(background = colors.tertiaryContainer, color = colors.onTertiaryContainer)
                else -> SpanStyle()
            }
            withLink(LinkAnnotation.Clickable(tag = "${verse.surah}:${verse.number}", styles = plainLink) { onAyahClick(verse) }) {
                withStyle(emphasis) {
                    append(verse.text)
                    if (verse.isSajdah) withStyle(SpanStyle(color = colors.tertiary)) { append(" ۩") }
                }
                withStyle(
                    SpanStyle(
                        color = if (key in bookmarks) colors.primary else colors.secondary,
                        fontWeight = FontWeight.Bold,
                    ),
                ) {
                    append(" ${Numerals.toArabicIndic(verse.number.toString())} ")
                }
            }
        }
    }
    Text(
        text = text,
        style = TextStyle(
            fontFamily = UthmanicHafs,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.9f).sp,
            textAlign = TextAlign.Justify,
            textDirection = TextDirection.Rtl,
            color = colors.onSurface,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SurahBanner(surah: Surah) {
    val locale = currentLocale()
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = MaterialShapes.Flower.toShape(),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(Numerals.toArabicIndic(surah.number.toString()), style = MaterialTheme.typography.labelLarge)
                }
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("سُورَةُ ${surah.nameArabic}", fontFamily = UthmanicHafs, fontSize = 24.sp)
                val revelation = stringResource(if (surah.revelation == Revelation.MECCAN) R.string.meccan else R.string.medinan)
                val verses = pluralStringResource(R.plurals.verse_count, surah.verses.size, Formatters.number(surah.verses.size, locale))
                Text("$revelation · $verses", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.size(40.dp))
        }
    }
}
