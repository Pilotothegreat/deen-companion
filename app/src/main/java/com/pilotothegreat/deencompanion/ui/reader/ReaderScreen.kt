package com.pilotothegreat.deencompanion.ui.reader

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import com.pilotothegreat.deencompanion.ui.common.rememberHaptics
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.text.Numerals
import com.pilotothegreat.deencompanion.data.net.NetError
import com.pilotothegreat.deencompanion.data.quran.MushafPage
import com.pilotothegreat.deencompanion.data.quran.Quran
import com.pilotothegreat.deencompanion.data.quran.Revelation
import com.pilotothegreat.deencompanion.data.quran.Surah
import com.pilotothegreat.deencompanion.data.quran.Verse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.pilotothegreat.deencompanion.ui.common.KeepScreenOn
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.components.LoadingBox
import com.pilotothegreat.deencompanion.ui.navigation.ReaderKey
import com.pilotothegreat.deencompanion.ui.quran.surahName
import com.pilotothegreat.deencompanion.ui.theme.UthmanicHafs
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

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
    val nightDim by viewModel.nightDim.collectAsStateWithLifecycle()
    // A fifteen-line page cannot be read at twice the system text size, so the reader falls back to
    // flowing it. The reader can also ask for that directly, from the top bar.
    val density = LocalDensity.current
    var flowingOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val flowing = flowingOverride ?: (density.fontScale > FLOW_ABOVE_FONT_SCALE)
    val resources = LocalResources.current
    val locale = currentLocale()
    val snackbar = remember { SnackbarHostState() }
    val haptics = rememberHaptics()
    var selected by remember { mutableStateOf<Verse?>(null) }
    var jumping by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { jumping = true }) {
                        Icon(Icons.AutoMirrored.Rounded.List, contentDescription = stringResource(R.string.jump_to))
                    }
                    IconToggleButton(checked = !flowing, onCheckedChange = { flowingOverride = !it }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = stringResource(R.string.reader_mushaf_page),
                        )
                    }
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
            // A few percent off the page after Isha, eased so it is never a visible step, and full
            // brightness again from Fajr. The toolbar and the snackbar keep their contrast.
            val dim by animateFloatAsState(if (nightDim) NIGHT_DIM else 1f, label = "nightDim")
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    key = { it },
                    modifier = Modifier.graphicsLayer { alpha = dim },
                ) { index ->
                    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                        MushafPageView(
                            page = loaded.pages[index],
                            quran = loaded,
                            fontSize = prefs.fontSize,
                            showTranslation = prefs.showTranslation,
                            highlight = highlight,
                            // Whatever is highlighted is worth scrolling to: the recited ayah as
                            // it moves, and the one a search or a bookmark opened, which used to be
                            // tinted somewhere below the fold and left there.
                            follow = highlight,
                            bookmarks = bookmarks,
                            bottomSpace = playback.isActive,
                            flowing = flowing,
                            onZoom = viewModel::zoom,
                            onAyahClick = {
                                haptics.click()
                                selected = it
                            },
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
                    onRepeat = viewModel::setRepeat,
                    onSpeed = viewModel::setSpeed,
                    onClearRange = viewModel::clearRange,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.large),
                )
            }
        }
    }

    if (jumping) {
        val scope = rememberCoroutineScope()
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

    selected?.let { verse ->
        val bookmarked = (verse.surah to verse.number) in bookmarks
        val anchor by viewModel.rangeAnchor.collectAsStateWithLifecycle()
        AyahSheet(
            verse = verse,
            surah = loaded.surah(verse.surah),
            translation = loaded.translation,
            bookmarked = bookmarked,
            rangeAnchor = anchor,
            onPlay = {
                viewModel.play(verse)
                selected = null
            },
            onRepeatRange = { viewModel.repeatFrom(verse) },
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
    /** The ayah being recited right now, which the page scrolls to keep in view. Null when idle. */
    follow: Pair<Int, Int>?,
    bookmarks: Set<Pair<Int, Int>>,
    bottomSpace: Boolean,
    /** True to flow the page as a paragraph instead of setting it line for line. */
    flowing: Boolean,
    onZoom: (Float) -> Unit,
    onAyahClick: (Verse) -> Unit,
) {
    val locale = currentLocale()
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    // Where the page begins on screen, and where the followed ayah sits, both in root coordinates.
    // On a set page that is the line the ayah opens on; on a flowing one the ayah lives inside a
    // single justified paragraph, so its position has to come from the text layout itself.
    var pageTop by remember { mutableFloatStateOf(0f) }
    var ayahTop by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(follow, ayahTop, pageTop) {
        val top = ayahTop ?: return@LaunchedEffect
        if (follow == null) return@LaunchedEffect
        // Parked a little way down rather than at the very edge: an ayah pinned to the top of the
        // screen reads like the page is about to run out. In dp, because these used to be raw
        // pixels and so sat three times further down a dense screen than a coarse one.
        val margin = with(density) { FOLLOW_MARGIN.toPx() }
        val slack = with(density) { FOLLOW_SLACK.toPx() }
        val target = (scroll.value + (top - pageTop) - margin).toInt().coerceAtLeast(0)
        if (kotlin.math.abs(target - scroll.value) > slack) scroll.animateScrollTo(target)
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .onGloballyPositioned { pageTop = it.positionInRoot().y }
            // Pinch the page to resize the Arabic, as one would a photograph. The gesture reports
            // continuously, so only a pinch that has actually changed the size by a noticeable
            // amount is passed on, and the setting is written at most once per step.
            .pointerInput(Unit) {
                var pending = 1f
                detectTransformGestures { _, _, zoom, _ ->
                    pending *= zoom
                    if (pending > ZOOM_STEP || pending < 1f / ZOOM_STEP) {
                        onZoom(pending)
                        pending = 1f
                    }
                }
            }
            .padding(horizontal = Spacing.large, vertical = Spacing.small),
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = Spacing.large, vertical = Spacing.xlarge), verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                if (page.lines.isNotEmpty() && !flowing) {
                    MushafPageLines(
                        lines = page.lines,
                        quran = quran,
                        fontSize = fontSize,
                        highlight = highlight,
                        follow = follow,
                        bookmarks = bookmarks,
                        onAyahClick = onAyahClick,
                        onFollowedLinePositioned = { ayahTop = it },
                    )
                } else {
                    page.verses.groupBy { it.surah }.forEach { (surahNumber, verses) ->
                    val surah = quran.surah(surahNumber)
                    if (verses.first().number == 1) {
                        SurahBanner(surah)
                        // From the text itself, so it matches the mushaf's own script and is absent
                        // exactly where the mushaf omits it.
                        surah.bismillah?.let { basmala ->
                            Text(
                                basmala,
                                fontFamily = UthmanicHafs,
                                fontSize = (fontSize * 1.05f).sp,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    AyahText(verses, fontSize, highlight, follow, bookmarks, onAyahClick) { ayahTop = it }
                    }
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
        // Says what the ۩ on the page means, and leaves the ruling to the reader's school.
        page.verses.filter { it.isSajdah }.takeIf { it.isNotEmpty() }?.let { prostrations ->
            val references = prostrations
                .joinToString(", ") { "${Formatters.number(it.surah, locale)}:${Formatters.number(it.number, locale)}" }
            Text(
                stringResource(R.string.sajdah_note, references),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showTranslation) {
            // Each ayah with its meaning directly under it, rather than the page's Arabic followed
            // by a list of numbered sentences the reader has to match up by eye.
            page.verses.forEach { verse ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.hair), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        verse.text,
                        fontFamily = UthmanicHafs,
                        fontSize = (fontSize * 0.8f).sp,
                        lineHeight = (fontSize * 1.5f).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = TextStyle(textDirection = TextDirection.Rtl),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            verseReference(verse, locale),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            verse.standaloneTranslation,
                            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
    follow: Pair<Int, Int>?,
    bookmarks: Set<Pair<Int, Int>>,
    onAyahClick: (Verse) -> Unit,
    onFollowedAyahPositioned: (Float) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // Ayahs are tappable, but must not look like hyperlinks.
    val plainLink = TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.None))
    val markers = mutableMapOf<String, InlineTextContent>()
    /** Where each ayah starts in the finished string, so the recited one can be found again. */
    val starts = mutableMapOf<Pair<Int, Int>, Int>()
    val text = buildAnnotatedString {
        verses.forEach { verse ->
            val key = verse.surah to verse.number
            starts[key] = length
            val emphasis = when (key) {
                highlight -> SpanStyle(background = colors.tertiaryContainer, color = colors.onTertiaryContainer)
                else -> SpanStyle()
            }
            // ۞ opens a rub' al-hizb. Unlike the sajdah sign, this one is not in the text, so the
            // app draws it.
            if (verse.quarterStart != null) {
                withStyle(SpanStyle(color = colors.tertiary)) { append("۞ ") }
            }
            withLink(LinkAnnotation.Clickable(tag = "${verse.surah}:${verse.number}", styles = plainLink) { onAyahClick(verse) }) {
                // The text already carries its own sajdah sign where there is one; appending
                // another printed it twice on all fifteen of them.
                withStyle(emphasis) { append(verse.text) }
                val id = "$AYAH_MARKER${verse.surah}:${verse.number}"
                val digits = Numerals.toArabicIndic(verse.number.toString())
                markers[id] = ayahMarker(
                    number = verse.number,
                    fontSize = fontSize,
                    color = if (key in bookmarks) colors.primary else colors.secondary,
                )
                appendInlineContent(id, digits)
            }
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var textTop by remember { mutableFloatStateOf(0f) }
    val followedStart = follow?.let { starts[it] }
    LaunchedEffect(followedStart, layout, textTop) {
        val start = followedStart ?: return@LaunchedEffect
        val measured = layout ?: return@LaunchedEffect
        if (start >= measured.layoutInput.text.length) return@LaunchedEffect
        onFollowedAyahPositioned(textTop + measured.getLineTop(measured.getLineForOffset(start)))
    }
    Text(
        text = text,
        inlineContent = markers,
        onTextLayout = { layout = it },
        style = TextStyle(
            fontFamily = UthmanicHafs,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.9f).sp,
            textAlign = TextAlign.Justify,
            textDirection = TextDirection.Rtl,
            color = colors.onSurface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { textTop = it.positionInRoot().y },
    )
}

/** A pinch has to change the size by this much before it counts, so the text does not flicker. */
private const val ZOOM_STEP = 1.08f

/** How far below the top of the page the followed ayah is parked. */
private val FOLLOW_MARGIN = 64.dp

/** Below this, the ayah is near enough already and scrolling would only be a twitch. */
private val FOLLOW_SLACK = 16.dp

/** After Isha the page comes down to this, and returns to full at Fajr. */
private const val NIGHT_DIM = 0.88f

/**
 * Above this system text scale the page is flowed rather than set in fifteen lines.
 *
 * The printed page is a fixed shape; at twice the text size it would either run off the screen or
 * shrink until it was no longer readable, which defeats the point of asking for larger text.
 */
private const val FLOW_ABOVE_FONT_SCALE = 1.3f

/** "2:255", in the reader's own numerals. */
private fun verseReference(verse: Verse, locale: java.util.Locale): String =
    "${Formatters.number(verse.surah, locale)}:${Formatters.number(verse.number, locale)}"

private const val AYAH_MARKER = "ayah:"

/**
 * The end-of-ayah rosette with its number inside it, as the mushaf prints it.
 *
 * It has to be drawn rather than typed. U+06DD is meant to enclose the digits that follow it, but
 * that composition is a font feature, and the bundled Uthmanic Hafs declares only calt, fina, init,
 * liga and medi — there is no rule to compose it. Typing "۝٢" therefore produced an empty rosette
 * followed by loose digits: two marks where the mushaf has one.
 */
@Composable
fun AyahRosette(number: Int, size: Float, color: Color, drop: Float = 0f) {
    Box(contentAlignment = Alignment.Center) {
        Text(AYAH_ROSETTE, fontFamily = UthmanicHafs, fontSize = (size * ROSETTE_SIZE).sp, color = color)
        Text(
            Numerals.toArabicIndic(number.toString()),
            fontFamily = UthmanicHafs,
            fontSize = (size * DIGIT_SIZE).sp,
            color = color,
            modifier = Modifier.offset { IntOffset(0, (size * drop).sp.roundToPx()) },
        )
    }
}

private const val ROSETTE_SIZE = 1.15f
private const val DIGIT_SIZE = 0.62f

/**
 * How far the digit drops inside the rosette when the rosette is set inline in a paragraph.
 *
 * On a line of its own the ring's opening falls where the Box centres it and no correction is
 * wanted. Inline, the glyph sits on the paragraph's baseline inside a placeholder box, so its
 * opening lands above centre and the digit would otherwise sit on the ornamental crown. In sp
 * rather than dp so it grows with the glyph it corrects — as dp it stayed put while the rosette
 * grew, and at a large accessibility text scale the digit climbed back onto the crown.
 */
private const val INLINE_DIGIT_DROP = 0.33f

private fun ayahMarker(number: Int, fontSize: Int, color: Color): InlineTextContent =
    InlineTextContent(
        Placeholder(width = 1.95.em, height = 1.35.em, placeholderVerticalAlign = PlaceholderVerticalAlign.Center),
    ) {
        AyahRosette(number = number, size = fontSize.toFloat(), color = color, drop = INLINE_DIGIT_DROP)
    }

/** U+06DD on its own: the rosette, with no digits for the font to fail to compose. */
private const val AYAH_ROSETTE = "۝"

/** The ornamental band the print sets a surah's name in. */
@Composable
fun SurahBanner(surah: Surah) {
    val locale = currentLocale()
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(Spacing.medium), verticalAlignment = Alignment.CenterVertically) {
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

/** Says what actually went wrong, so "check your connection" isn't the answer to a missing file. */
@StringRes
private fun NetError.playbackMessage(): Int = when (this) {
    NetError.OFFLINE -> R.string.playback_error_offline
    NetError.NOT_FOUND -> R.string.playback_error_missing
    NetError.RATE_LIMITED, NetError.FAILED -> R.string.playback_error_failed
}
