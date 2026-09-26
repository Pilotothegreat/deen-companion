package com.pilotothegreat.deencompanion.ui.reader

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.data.net.NetError
import com.pilotothegreat.deencompanion.data.quran.Verse
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.KeepScreenOn
import com.pilotothegreat.deencompanion.ui.common.Labelled
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.common.rememberHaptics
import com.pilotothegreat.deencompanion.ui.components.LoadingBox
import com.pilotothegreat.deencompanion.ui.navigation.ReaderKey
import com.pilotothegreat.deencompanion.ui.quran.surahName
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The mushaf, a page at a time, turned right to left like a printed copy.
 *
 * One look and no modes: each page fits the screen as a printed page does. A tap on an ayah brings the
 * player up on it, a long press opens its menu, and a swipe turns the page. Nothing on the page listens
 * for a pinch or a drag — a pinch detector here is what swallowed every swipe in 2.0.
 */
@Composable
fun ReaderScreen(
    key: ReaderKey,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = koinViewModel { parametersOf(key) },
) {
    val quran by viewModel.quran.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val nightDim by viewModel.nightDim.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val locale = currentLocale()
    val snackbar = remember { SnackbarHostState() }
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    var menuFor by remember { mutableStateOf<Verse?>(null) }
    var jumping by remember { mutableStateOf(false) }
    var choosingReciter by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.playbackErrors.collect { error ->
            val result = snackbar.showSnackbar(
                message = resources.getString(error.playbackMessage()),
                actionLabel = resources.getString(R.string.retry),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.retry()
        }
    }

    // Reading a page takes longer than most screen timeouts, and so does listening to a surah.
    KeepScreenOn(enabled = true)

    val loaded = quran ?: return LoadingBox()
    val pagerState = rememberPagerState(initialPage = viewModel.initialPage - 1) { loaded.pages.size }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { viewModel.onPageSettled(it + 1) }
    }

    // The recitation turns the page only for someone still on the page it was reading. 2.0 turned back
    // at every ayah, so a reader who swiped ahead during a recitation was thrown back a few seconds later.
    var recitedPage by remember { mutableIntStateOf(-1) }
    LaunchedEffect(playback.isActive, playback.surah, playback.ayah) {
        if (!playback.isActive) {
            recitedPage = -1
            return@LaunchedEffect
        }
        val page = loaded.pageOf(playback.surah, playback.ayah) - 1
        val watching = recitedPage == -1 || pagerState.settledPage == recitedPage
        recitedPage = page
        if (watching && pagerState.currentPage != page && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(page)
        }
    }

    val highlight = if (playback.isActive) playback.surah to playback.ayah else viewModel.targetAyah
    val current = loaded.pages[pagerState.currentPage]
    val firstVerse = current.verses.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(surahName(loaded.surah(firstVerse.surah), locale)) },
                subtitle = {
                    Text(
                        stringResource(
                            R.string.page_juz_hizb,
                            Formatters.number(current.number, locale),
                            Formatters.number(current.juz, locale),
                            Formatters.number(loaded.hizbOf(firstVerse.surah, firstVerse.number), locale),
                            Formatters.number(loaded.rubOf(firstVerse.surah, firstVerse.number), locale),
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                },
                actions = {
                    val jumpLabel = stringResource(R.string.jump_to)
                    Labelled(jumpLabel) {
                        IconButton(onClick = { jumping = true }) {
                            Icon(Icons.AutoMirrored.Rounded.List, contentDescription = jumpLabel)
                        }
                    }
                },
            )
        },
        bottomBar = {
            // Docked under the page, not floating over it: the page is sized for the space left, so no
            // line hides behind the bar and nothing needs a guessed spacer.
            if (playback.isActive) {
                PlayerBar(
                    state = playback,
                    title = surahName(loaded.surah(playback.surah), locale),
                    awayFromRecitation = recitedPage >= 0 && pagerState.settledPage != recitedPage,
                    onTogglePlay = viewModel::togglePlayPause,
                    onPrevious = viewModel::previous,
                    onNext = viewModel::next,
                    onStop = viewModel::stop,
                    onRepeat = viewModel::cycleRepeat,
                    onReciter = { choosingReciter = true },
                    onBackToRecitation = { scope.launch { pagerState.animateScrollToPage(recitedPage) } },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        // A few percent off the page after Isha, eased in so it is never a visible step.
        val dim by animateFloatAsState(if (nightDim) NIGHT_DIM else 1f, label = "nightDim")
        val layoutDirection = LocalLayoutDirection.current
        // Page 1 is on the right and the next page comes in from the left, as in a printed mushaf.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                key = { it },
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .graphicsLayer { alpha = dim },
            ) { index ->
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    val page = loaded.pages[index]
                    Column(Modifier.fillMaxSize().padding(horizontal = Spacing.medium, vertical = Spacing.small)) {
                        MushafPageLines(
                            lines = page.lines,
                            quran = loaded,
                            highlight = highlight,
                            bookmarks = bookmarks,
                            onTap = { verse ->
                                haptics.click()
                                viewModel.cue(verse)
                            },
                            onLongPress = { verse ->
                                haptics.confirm()
                                menuFor = verse
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                        Text(
                            Formatters.number(page.number, locale),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (jumping) {
        JumpSheet(
            quran = loaded,
            currentPage = current.number,
            onJump = { page ->
                jumping = false
                scope.launch { pagerState.scrollToPage(page - 1) }
            },
            onDismiss = { jumping = false },
        )
    }

    if (choosingReciter) {
        ReciterSheet(
            state = playback,
            onReciter = viewModel::setReciter,
            onSleepTimer = viewModel::setSleepTimer,
            onClearDownloads = viewModel::clearDownloads,
            onDismiss = { choosingReciter = false },
        )
    }

    menuFor?.let { verse ->
        val bookmarked = (verse.surah to verse.number) in bookmarks
        AyahMenu(
            verse = verse,
            surah = loaded.surah(verse.surah),
            translation = loaded.translation,
            bookmarked = bookmarked,
            onRepeat = { viewModel.repeatAyah(verse) },
            onBookmark = { viewModel.setBookmark(verse, !bookmarked) },
            onDismiss = { menuFor = null },
        )
    }
}

/** After Isha the page comes down to this, and returns to full at Fajr. */
private const val NIGHT_DIM = 0.88f

/** Says what actually went wrong, so "check your connection" isn't the answer to a missing file. */
@StringRes
private fun NetError.playbackMessage(): Int = when (this) {
    NetError.OFFLINE -> R.string.playback_error_offline
    NetError.NOT_FOUND -> R.string.playback_error_missing
    NetError.RATE_LIMITED, NetError.FAILED -> R.string.playback_error_failed
}
