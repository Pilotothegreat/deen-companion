package com.pilotothegreat.deencompanion.ui.reader

import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.ui.text.fromHtml
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import com.pilotothegreat.deencompanion.ui.theme.Amiri
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOn
import androidx.compose.material.icons.rounded.RepeatOneOn
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.ui.components.groupedRowColors
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.quran.RepeatMode
import com.pilotothegreat.deencompanion.data.quran.Reciter
import com.pilotothegreat.deencompanion.data.quran.Surah
import com.pilotothegreat.deencompanion.data.quran.TranslationInfo
import com.pilotothegreat.deencompanion.data.quran.Verse
import com.pilotothegreat.deencompanion.playback.PlaybackState
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.Labelled
import com.pilotothegreat.deencompanion.ui.common.SystemIntents
import com.pilotothegreat.deencompanion.ui.common.copyToClipboard
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.common.isArabic
import com.pilotothegreat.deencompanion.ui.common.startSafely
import com.pilotothegreat.deencompanion.ui.quran.verseReference
import com.pilotothegreat.deencompanion.ui.theme.Spacing
import com.pilotothegreat.deencompanion.ui.theme.UthmanicHafs

private val SLEEP_OPTIONS = listOf(0, 10, 15, 30, 60)

/**
 * The recitation bar, docked at the foot of the reader the way Quran for Android docks its audio bar.
 *
 * Everything it does is on its face, in one row: repeat, the ayah before, a large play button whose
 * shape morphs with its state, the ayah after, and stop. The reciter is a chip that opens a short
 * sheet. There is no menu, and no speed — a recitation is not read faster than it was recited.
 */
@Composable
fun PlayerBar(
    state: PlaybackState,
    title: String,
    /** True when the reader has turned away from the page being recited. */
    awayFromRecitation: Boolean,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    onRepeat: () -> Unit,
    onReciter: () -> Unit,
    onBackToRecitation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.navigationBarsPadding().padding(top = Spacing.medium, bottom = Spacing.small)) {
            // How far through the surah, as a thin line — not a seek bar, since ayahs are the steps.
            LinearProgressIndicator(
                progress = { if (state.verseCount > 0) state.ayah.toFloat() / state.verseCount else 0f },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xxlarge),
            )
            Row(
                Modifier.fillMaxWidth().padding(start = Spacing.xxlarge, end = Spacing.large, top = Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Text(
                        stringResource(R.string.player_position, Formatters.number(state.ayah, locale), Formatters.number(state.verseCount, locale)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AssistChip(
                    onClick = onReciter,
                    label = { Text(stringResource(state.reciter.label), maxLines = 1) },
                    leadingIcon = { Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, Modifier.size(18.dp)) },
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.large, vertical = Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small, Alignment.CenterHorizontally),
            ) {
                val repeatLabel = stringResource(R.string.repeat) + ": " + stringResource(state.repeatMode.label)
                Labelled(repeatLabel) {
                    IconButton(onClick = onRepeat, modifier = Modifier.size(IconButtonDefaults.smallContainerSize())) {
                        Icon(
                            when (state.repeatMode) {
                                RepeatMode.OFF -> Icons.Rounded.Repeat
                                RepeatMode.AYAH -> Icons.Rounded.RepeatOneOn
                                RepeatMode.SURAH, RepeatMode.RANGE -> Icons.Rounded.RepeatOn
                            },
                            contentDescription = repeatLabel,
                            tint = if (state.repeatMode == RepeatMode.OFF) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                // Reading is right to left here, so the ayah before is on the right and its arrow points
                // that way. Skip-previous and skip-next are each other's mirror, so the pair is swapped
                // rather than flipped: a mirrored glyph is a drawing of the wrong button.
                val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                SkipButton(
                    icon = if (rtl) Icons.Rounded.SkipNext else Icons.Rounded.SkipPrevious,
                    label = stringResource(R.string.previous_ayah),
                    onClick = onPrevious,
                )
                FilledIconToggleButton(
                    checked = state.isPlaying,
                    onCheckedChange = { onTogglePlay() },
                    shapes = IconButtonDefaults.toggleableShapes(),
                    modifier = Modifier.size(IconButtonDefaults.largeContainerSize()),
                ) {
                    if (state.isBuffering) {
                        LoadingIndicator(Modifier.size(IconButtonDefaults.largeIconSize))
                    } else {
                        Icon(
                            if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.play_pause),
                            modifier = Modifier.size(IconButtonDefaults.largeIconSize),
                        )
                    }
                }
                SkipButton(
                    icon = if (rtl) Icons.Rounded.SkipPrevious else Icons.Rounded.SkipNext,
                    label = stringResource(R.string.next_ayah),
                    onClick = onNext,
                )
                Spacer(Modifier.weight(1f))
                val stopLabel = stringResource(R.string.stop_playback)
                Labelled(stopLabel) {
                    IconButton(onClick = onStop, modifier = Modifier.size(IconButtonDefaults.smallContainerSize())) {
                        Icon(Icons.Rounded.Close, contentDescription = stopLabel)
                    }
                }
            }
            if (awayFromRecitation) {
                TextButton(onClick = onBackToRecitation, modifier = Modifier.padding(start = Spacing.small)) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = null, Modifier.size(18.dp))
                    Text(stringResource(R.string.back_to_recitation), modifier = Modifier.padding(start = Spacing.small))
                }
            }
        }
    }
}

/** One ayah back or on: the size M3 gives the controls that flank a large play button. */
@Composable
private fun SkipButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Labelled(label) {
        FilledTonalIconButton(
            onClick = onClick,
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.size(IconButtonDefaults.mediumContainerSize()),
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(IconButtonDefaults.mediumIconSize))
        }
    }
}

/**
 * The player's occasional choices: who recites, when to stop, and the downloaded recitations.
 *
 * The sheet scrolls, because the reciters are now a list of twenty rather than three, and a sleep
 * timer pushed off the bottom of the screen is a setting that does not exist.
 */
@Composable
fun ReciterSheet(
    state: PlaybackState,
    onReciter: (Reciter) -> Unit,
    onSleepTimer: (Int) -> Unit,
    onClearDownloads: () -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = currentLocale()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.large, end = Spacing.large, bottom = Spacing.xxlarge),
        ) {
            Text(
                stringResource(R.string.reciter),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = Spacing.small, bottom = Spacing.small),
            )
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                Reciter.entries.forEachIndexed { index, reciter ->
                    SegmentedListItem(
                        selected = reciter == state.reciter,
                        onClick = { onReciter(reciter) },
                        shapes = ListItemDefaults.segmentedShapes(index, Reciter.entries.size),
                        leadingContent = { RadioButton(selected = reciter == state.reciter, onClick = null) },
                    ) { Text(stringResource(reciter.label)) }
                }
            }
            Text(
                stringResource(R.string.sleep_timer),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = Spacing.small, top = Spacing.xxlarge, bottom = Spacing.small),
            )
            val timerOn = state.sleepTimerEndsAt != null
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                SLEEP_OPTIONS.forEach { minutes ->
                    FilterChip(
                        selected = minutes == 0 && !timerOn,
                        onClick = { onSleepTimer(minutes) },
                        label = {
                            Text(
                                if (minutes == 0) stringResource(R.string.timer_off)
                                else pluralStringResource(R.plurals.minutes, minutes, Formatters.number(minutes, locale)),
                            )
                        },
                    )
                }
            }
            TextButton(onClick = onClearDownloads, modifier = Modifier.padding(top = Spacing.large)) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = null, Modifier.size(18.dp))
                Text(stringResource(R.string.clear_downloaded_recitations), modifier = Modifier.padding(start = Spacing.small))
            }
        }
    }
}

/**
 * What a long press on an ayah opens: its meaning, and what can be done with it.
 *
 * The ayah comes in the language the app is being read in — the Arabic on an Arabic app, the
 * translation on an English one. The page behind the sheet already carries the Arabic, and printing
 * both pushed the play, bookmark, repeat, copy and share rows off the bottom of a phone screen.
 */
@Composable
fun AyahMenu(
    verse: Verse,
    surah: Surah,
    translation: TranslationInfo,
    bookmarked: Boolean,
    onPlay: () -> Unit,
    onRepeat: () -> Unit,
    onBookmark: () -> Unit,
    onDismiss: () -> Unit,
    onTafsir: () -> Unit = {},
) {
    val context = LocalContext.current
    val locale = currentLocale()
    val reference = verseReference(surah, verse.number, locale)
    val shareText = buildString {
        append(verse.text).append("\n\n").append(verse.standaloneTranslation)
        append("\n— ").append(surah.nameEnglish).append(' ').append(verse.surah).append(':').append(verse.number)
        // The translation's licence asks for its translator to be named wherever it goes.
        if (verse.translation.isNotBlank()) append('\n').append(translation.attribution)
    }

    // Opened to its own height: half open, the share row sat below the fold.
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberBottomSheetState(SheetValue.Hidden, setOf(SheetValue.Hidden, SheetValue.Expanded))) {
        // Scrolls: 2:282 alone is over 1,200 characters of translation, which pushed every action off a phone.
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.xxlarge, end = Spacing.xxlarge, bottom = Spacing.xxlarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(reference, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            // An ayah with no translation falls back to the Arabic: a sheet with nothing in it is worse.
            if (locale.isArabic || verse.translation.isBlank()) {
                Text(
                    verse.text,
                    fontFamily = UthmanicHafs,
                    fontSize = 24.sp,
                    lineHeight = 46.sp,
                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(verse.standaloneTranslation, style = MaterialTheme.typography.bodyLarge)
                Text(translation.attribution, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (verse.isSajdah) {
                Text(
                    stringResource(R.string.sajdah_note, "${Formatters.number(verse.surah, locale)}:${Formatters.number(verse.number, locale)}"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            val actions = listOf(
                Triple(Icons.Rounded.PlayArrow, stringResource(R.string.play_from_here)) {
                    onPlay()
                    onDismiss()
                },
                Triple(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.tafsir)) {
                    onTafsir()
                    onDismiss()
                },
                Triple(
                    if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    stringResource(if (bookmarked) R.string.remove_bookmark else R.string.bookmark),
                    onBookmark,
                ),
                Triple(Icons.Rounded.RepeatOneOn, stringResource(R.string.repeat_this_ayah)) {
                    onRepeat()
                    onDismiss()
                },
                Triple(Icons.Rounded.ContentCopy, stringResource(R.string.copy)) {
                    context.copyToClipboard(reference, shareText)
                    onDismiss()
                },
                Triple(Icons.Rounded.Share, stringResource(R.string.share)) {
                    context.startSafely(SystemIntents.shareText(shareText))
                },
            )
            Column(Modifier.padding(top = Spacing.small), verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                actions.forEachIndexed { index, (icon, label, action) ->
                    SegmentedListItem(
                        colors = groupedRowColors(),
                        onClick = action,
                        shapes = ListItemDefaults.segmentedShapes(index, actions.size),
                        leadingContent = { Icon(icon, contentDescription = null) },
                    ) { Text(label) }
                }
            }
        }
    }
}

/**
 * How the mushaf is set: the King Fahd Complex's printed page, fetched page by page, or the bundled
 * font. The whole set can be fetched ahead for reading offline, and removed again.
 */
@Composable
fun MushafSheet(
    printedPage: Boolean,
    pageWaiting: Boolean,
    bulkProgress: Float?,
    totalBytes: Long,
    storedBytes: Long,
    onPrintedPage: (Boolean) -> Unit,
    onDownloadAll: () -> Unit,
    onCancelDownload: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    fun size(bytes: Long) = android.text.format.Formatter.formatShortFileSize(context, bytes)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.large, end = Spacing.large, bottom = Spacing.xxlarge),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(
                stringResource(R.string.mushaf_sheet),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = Spacing.small, bottom = Spacing.medium),
            )
            val rows = buildList<@Composable (Int, Int) -> Unit> {
                add { index, count ->
                    SegmentedListItem(
                        colors = groupedRowColors(),
                        onClick = { onPrintedPage(!printedPage) },
                        shapes = ListItemDefaults.segmentedShapes(index, count),
                        supportingContent = {
                            Text(stringResource(if (printedPage && pageWaiting) R.string.printed_page_waiting else R.string.printed_page_desc))
                        },
                        trailingContent = { androidx.compose.material3.Switch(checked = printedPage, onCheckedChange = null) },
                    ) { Text(stringResource(R.string.printed_page)) }
                }
                if (printedPage) add { index, count ->
                    SegmentedListItem(
                        colors = groupedRowColors(),
                        onClick = { if (bulkProgress == null) onDownloadAll() else onCancelDownload() },
                        shapes = ListItemDefaults.segmentedShapes(index, count),
                        supportingContent = {
                            if (bulkProgress != null) {
                                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                                    Text(stringResource(R.string.printed_page_downloading, "${(bulkProgress * 100).toInt()}%"))
                                    androidx.compose.material3.LinearWavyProgressIndicator(progress = { bulkProgress }, modifier = Modifier.fillMaxWidth())
                                }
                            } else {
                                Text(stringResource(R.string.printed_page_download_desc, size(totalBytes)))
                            }
                        },
                        trailingContent = {
                            Icon(
                                if (bulkProgress == null) Icons.Rounded.Download else Icons.Rounded.Close,
                                contentDescription = if (bulkProgress == null) null else stringResource(R.string.cancel),
                            )
                        },
                    ) { Text(stringResource(R.string.printed_page_download)) }
                }
                if (storedBytes > 0) add { index, count ->
                    SegmentedListItem(
                        colors = groupedRowColors(),
                        onClick = onDelete,
                        shapes = ListItemDefaults.segmentedShapes(index, count),
                        supportingContent = { Text(stringResource(R.string.printed_page_stored, size(storedBytes))) },
                        trailingContent = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                    ) { Text(stringResource(R.string.printed_page_delete)) }
                }
            }
            rows.forEachIndexed { index, row -> row(index, rows.size) }
        }
    }
}

/**
 * The commentary on an ayah, from whichever of the five commentaries the reader picks.
 *
 * The ayah sits at the top on a soft card in the mushaf's own font; the commentaries are chips under
 * it, the one in the app's language first; the passage follows, selectable for copying a line. A
 * commentary that explains several ayahs together says which. Arrows step to the ayah before or after
 * without closing the sheet, so a surah can be read through with its commentary.
 */
@Composable
fun TafsirSheet(
    quran: com.pilotothegreat.deencompanion.data.quran.Quran,
    start: Verse,
    onDismiss: () -> Unit,
) {
    val repository: com.pilotothegreat.deencompanion.data.quran.TafsirRepository = org.koin.compose.koinInject()
    val settings: com.pilotothegreat.deencompanion.data.settings.SettingsRepository = org.koin.compose.koinInject()
    val context = LocalContext.current
    val locale = currentLocale()
    val arabicUi = locale.isArabic
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val chosen by androidx.compose.runtime.produceState<String?>(null) { value = settings.current().quran.tafsir }
    var picked by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val tafsir = com.pilotothegreat.deencompanion.data.quran.Tafsir.byId(picked ?: chosen)
        ?: com.pilotothegreat.deencompanion.data.quran.Tafsir.defaultFor(arabicUi)
    var ayah by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(start.number) }
    var attempt by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val surah = quran.surah(start.surah)
    val verse = surah.verses[ayah - 1]
    val result by androidx.compose.runtime.produceState<com.pilotothegreat.deencompanion.data.quran.TafsirResult?>(null, tafsir, ayah, attempt) {
        value = null
        value = repository.passage(tafsir, start.surah, ayah)
    }
    val ordered = androidx.compose.runtime.remember(arabicUi) {
        com.pilotothegreat.deencompanion.data.quran.Tafsir.entries.sortedBy { if (it.arabic == arabicUi) 0 else 1 }
    }
    val name = tafsir.name(arabicUi)
    val colors = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = Spacing.large)) {
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xxlarge),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                Text(
                    verseReference(surah, ayah, locale),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primary,
                )
                androidx.compose.material3.Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = colors.secondaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        verse.text + " " + com.pilotothegreat.deencompanion.core.text.Numerals.toArabicIndic(ayah.toString()),
                        fontFamily = UthmanicHafs,
                        fontSize = 22.sp,
                        lineHeight = 44.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl),
                        modifier = Modifier.padding(horizontal = Spacing.large, vertical = Spacing.medium),
                    )
                }
                // Wrapped rather than scrolled: five commentaries, every one of them in sight.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    ordered.forEach { option ->
                        androidx.compose.material3.FilterChip(
                            selected = option == tafsir,
                            onClick = {
                                picked = option.id
                                scope.launch { settings.setTafsir(option.id) }
                            },
                            label = { Text(option.name(arabicUi)) },
                        )
                    }
                }
                androidx.compose.animation.AnimatedContent(
                    targetState = result,
                    transitionSpec = {
                        androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) togetherWith
                            androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120))
                    },
                    contentKey = { it?.javaClass },
                    label = "tafsir",
                ) { state ->
                    when (state) {
                        null -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier.padding(vertical = Spacing.large),
                        ) {
                            androidx.compose.material3.LoadingIndicator(Modifier.size(36.dp))
                            Text(stringResource(R.string.tafsir_loading, name), color = colors.onSurfaceVariant)
                        }
                        is com.pilotothegreat.deencompanion.data.quran.TafsirResult.Ready -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                            val passage = state.passage
                            if (passage.fromAyah != passage.toAyah) {
                                Text(
                                    stringResource(R.string.tafsir_range, Formatters.number(passage.fromAyah, locale), Formatters.number(passage.toAyah, locale)),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.tertiary,
                                )
                            }
                            val body = androidx.compose.runtime.remember(passage.text, colors.primary) { tafsirBody(passage.text, colors.primary) }
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                if (tafsir.arabic) {
                                    Text(
                                        body,
                                        fontFamily = Amiri,
                                        fontSize = 19.sp,
                                        lineHeight = 36.sp,
                                        style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl),
                                    )
                                } else {
                                    Text(body, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr, lineHeight = 26.sp))
                                }
                            }
                            Text(
                                tafsir.name(arabicUi) + " · " + tafsir.authorEn,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        com.pilotothegreat.deencompanion.data.quran.TafsirResult.Empty ->
                            Text(stringResource(R.string.tafsir_empty, name), color = colors.onSurfaceVariant, modifier = Modifier.padding(vertical = Spacing.large))
                        is com.pilotothegreat.deencompanion.data.quran.TafsirResult.Failed -> Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                            modifier = Modifier.padding(vertical = Spacing.medium),
                        ) {
                            Text(
                                stringResource(
                                    if (state.error == com.pilotothegreat.deencompanion.data.net.NetError.OFFLINE) R.string.tafsir_offline else R.string.tafsir_failed,
                                    name,
                                ),
                                color = colors.onSurfaceVariant,
                            )
                            androidx.compose.material3.FilledTonalButton(onClick = { attempt++ }) { Text(stringResource(R.string.retry)) }
                        }
                    }
                }
            }
            // The steps between ayahs and what to do with a passage stay in reach however long it is.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.large, vertical = Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.IconButton(onClick = { if (ayah > 1) ayah-- }, enabled = ayah > 1) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.tafsir_previous))
                }
                androidx.compose.material3.IconButton(onClick = { if (ayah < surah.verses.size) ayah++ }, enabled = ayah < surah.verses.size) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = stringResource(R.string.tafsir_next))
                }
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                val ready = result as? com.pilotothegreat.deencompanion.data.quran.TafsirResult.Ready
                val shareText = ready?.let {
                    verseReference(surah, ayah, locale) + "\n" + verse.text + "\n\n" +
                        tafsirBody(it.passage.text, colors.primary).text + "\n— " + tafsir.name(arabicUi)
                }
                androidx.compose.material3.IconButton(onClick = { shareText?.let { context.copyToClipboard(name, it) } }, enabled = shareText != null) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.copy))
                }
                androidx.compose.material3.IconButton(onClick = { shareText?.let { context.startSafely(SystemIntents.shareText(it)) } }, enabled = shareText != null) {
                    Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.share))
                }
            }
        }
    }
}

/**
 * A passage as the commentary writes it, markup and all, made readable: paragraphs and headings kept,
 * and every quotation of the Quran — between ﴿ ﴾ or { } — set in the mushaf's font and the accent colour.
 */
private fun tafsirBody(raw: String, quote: androidx.compose.ui.graphics.Color): androidx.compose.ui.text.AnnotatedString {
    val html = androidx.compose.ui.text.AnnotatedString.fromHtml(raw.replace("\n", "<br>"))
    val trimmed = html.subSequence(0, html.text.trimEnd().length)
    return androidx.compose.ui.text.buildAnnotatedString {
        append(trimmed)
        QURAN_QUOTE.findAll(trimmed.text).forEach { match ->
            addStyle(androidx.compose.ui.text.SpanStyle(fontFamily = UthmanicHafs, color = quote), match.range.first, match.range.last + 1)
        }
    }
}

private val QURAN_QUOTE = Regex("[﴿{][^﴾}]{1,600}[﴾}]")
