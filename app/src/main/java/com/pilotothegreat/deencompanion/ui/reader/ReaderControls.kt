package com.pilotothegreat.deencompanion.ui.reader

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
 * both pushed the bookmark, repeat, copy and share rows off the bottom of a phone screen.
 */
@Composable
fun AyahMenu(
    verse: Verse,
    surah: Surah,
    translation: TranslationInfo,
    bookmarked: Boolean,
    onRepeat: () -> Unit,
    onBookmark: () -> Unit,
    onDismiss: () -> Unit,
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(start = Spacing.xxlarge, end = Spacing.xxlarge, bottom = Spacing.xxlarge),
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
                        onClick = action,
                        shapes = ListItemDefaults.segmentedShapes(index, actions.size),
                        leadingContent = { Icon(icon, contentDescription = null) },
                    ) { Text(label) }
                }
            }
        }
    }
}
