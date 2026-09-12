package com.pilotothegreat.deencompanion.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.AssistChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
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
import com.pilotothegreat.deencompanion.ui.common.SystemIntents
import com.pilotothegreat.deencompanion.ui.common.copyToClipboard
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.common.startSafely
import com.pilotothegreat.deencompanion.ui.quran.verseReference
import com.pilotothegreat.deencompanion.ui.theme.UthmanicHafs

private val SLEEP_OPTIONS = listOf(0, 10, 15, 30, 60)
private val SPEED_OPTIONS = listOf(0.75f, 1f, 1.25f, 1.5f)
private val REPEAT_COUNTS = listOf(3, 5, 10)

/** Floating recitation controls; the play button's shape morphs with its state. */
@Composable
fun PlayerToolbar(
    state: PlaybackState,
    title: String,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    onReciter: (Reciter) -> Unit,
    onSleepTimer: (Int) -> Unit,
    onRepeat: (RepeatMode, Int) -> Unit,
    onSpeed: (Float) -> Unit,
    onClearRange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    var menuOpen by remember { mutableStateOf(false) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // A chosen range is the one piece of playback state that is invisible otherwise, and the
        // one a reader most needs a way out of.
        state.range?.let { range ->
            AssistChip(
                onClick = onClearRange,
                label = {
                    Text(
                        stringResource(
                            R.string.repeat_range_set,
                            Formatters.number(range.first, locale),
                            Formatters.number(range.last, locale),
                        ),
                    )
                },
                trailingIcon = {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.repeat_range_clear))
                },
            )
        }
        Text(
            text = buildString {
                append(title)
                append(" · ")
                append(stringResource(R.string.player_position, Formatters.number(state.ayah, locale), Formatters.number(state.verseCount, locale)))
                if (state.repeatMode != RepeatMode.OFF) {
                    append(" · ")
                    append(stringResource(R.string.repeat_progress, Formatters.number(state.repeatsDone + 1, locale)))
                }
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalFloatingToolbar(
            expanded = true,
            leadingContent = {
                IconButton(onClick = onStop) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.stop_playback)) }
            },
            trailingContent = {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            if (state.sleepTimerEndsAt != null) Icons.Rounded.Bedtime else Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.playback_options),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        Text(
                            stringResource(R.string.reciter),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        Reciter.entries.forEach { reciter ->
                            DropdownMenuItem(
                                text = { Text(stringResource(reciter.label)) },
                                leadingIcon = { RadioButton(selected = reciter == state.reciter, onClick = null) },
                                onClick = {
                                    onReciter(reciter)
                                    menuOpen = false
                                },
                            )
                        }
                        HorizontalDivider()
                        Text(
                            stringResource(R.string.repeat),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        RepeatMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(stringResource(mode.label)) },
                                leadingIcon = { RadioButton(selected = mode == state.repeatMode, onClick = null) },
                                onClick = {
                                    // Keeping the count on OFF means turning repeat back on resumes what was set.
                                    onRepeat(mode, state.repeatCount)
                                    menuOpen = false
                                },
                            )
                        }
                        if (state.repeatMode != RepeatMode.OFF) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                REPEAT_COUNTS.forEach { count ->
                                    FilterChip(
                                        selected = count == state.repeatCount,
                                        onClick = { onRepeat(state.repeatMode, count) },
                                        label = { Text(Formatters.number(count, locale)) },
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                        Text(
                            stringResource(R.string.playback_speed),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SPEED_OPTIONS.forEach { speed ->
                                FilterChip(
                                    selected = speed == state.speed,
                                    onClick = { onSpeed(speed) },
                                    label = { Text(stringResource(R.string.speed_multiplier, Formatters.decimal(speed, locale))) },
                                )
                            }
                        }
                        HorizontalDivider()
                        Text(
                            stringResource(R.string.sleep_timer),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        SLEEP_OPTIONS.forEach { minutes ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (minutes == 0) stringResource(R.string.timer_off)
                                        else pluralStringResource(R.plurals.minutes, minutes, Formatters.number(minutes, locale)),
                                    )
                                },
                                leadingIcon = { Icon(Icons.Outlined.Bedtime, contentDescription = null) },
                                onClick = {
                                    onSleepTimer(minutes)
                                    menuOpen = false
                                },
                            )
                        }
                    }
                }
            },
        ) {
            IconButton(onClick = onPrevious) { Icon(Icons.Rounded.SkipPrevious, contentDescription = stringResource(R.string.previous_ayah)) }
            FilledIconToggleButton(
                checked = state.isPlaying,
                onCheckedChange = { onTogglePlay() },
                shapes = IconButtonDefaults.toggleableShapes(),
            ) {
                if (state.isBuffering) {
                    LoadingIndicator(Modifier.size(24.dp))
                } else {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.play_pause),
                    )
                }
            }
            IconButton(onClick = onNext) { Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.next_ayah)) }
        }
    }
}

@Composable
fun AyahSheet(
    verse: Verse,
    surah: Surah,
    translation: TranslationInfo,
    bookmarked: Boolean,
    /** The ayah already chosen as one end of a repeat, if the reader is part-way through choosing. */
    rangeAnchor: Pair<Int, Int>?,
    onPlay: () -> Unit,
    onRepeatRange: () -> Unit,
    onBookmark: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val locale = currentLocale()
    val reference = verseReference(surah, verse.number, locale)
    val shareText = "${verse.text}\n\n${verse.standaloneTranslation}\n— ${surah.nameEnglish} ${verse.surah}:${verse.number}"
        .let { if (verse.translation.isBlank()) it else "$it\n${translation.attribution}" }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(reference, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                verse.text,
                fontFamily = UthmanicHafs,
                fontSize = 24.sp,
                lineHeight = 44.sp,
                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(verse.standaloneTranslation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // The licence asks for the translator to be named wherever the translation is read.
            Text(
                translation.attribution,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val waiting = rangeAnchor != null && rangeAnchor.first == verse.surah
            val actions = listOf(
                Triple(Icons.Rounded.PlayArrow, stringResource(R.string.play_from_here), onPlay),
                Triple(
                    Icons.Rounded.Repeat,
                    stringResource(if (waiting) R.string.repeat_to_here else R.string.repeat_from_here),
                ) {
                    onRepeatRange()
                    onDismiss()
                },
                Triple(
                    if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    stringResource(if (bookmarked) R.string.remove_bookmark else R.string.bookmark),
                    onBookmark,
                ),
                Triple(Icons.Rounded.ContentCopy, stringResource(R.string.copy)) {
                    context.copyToClipboard(reference, shareText)
                    onDismiss()
                },
                Triple(Icons.Rounded.Share, stringResource(R.string.share)) {
                    context.startSafely(SystemIntents.shareText(shareText))
                },
            )
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
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
