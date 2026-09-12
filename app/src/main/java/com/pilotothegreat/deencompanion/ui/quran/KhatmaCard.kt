package com.pilotothegreat.deencompanion.ui.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.quran.KhatmaProgress
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.currentLocale

/** The lengths people actually commit to: a month, Ramadan, a fortnight, a week. */
internal val KHATMA_LENGTHS = listOf(30, 29, 14, 7)

/**
 * Shows the khatma only while one is running, and says the one thing that matters today: how many
 * pages are due. It stays quiet about being behind until it has to, because a plan that scolds is
 * a plan people cancel.
 */
@Composable
fun KhatmaCard(progress: KhatmaProgress, onOpen: (Int) -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val locale = currentLocale()
    Card(
        onClick = { onOpen(progress.currentPage.coerceAtLeast(1)) },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (progress.isComplete) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(40.dp))
            } else {
                CircularProgressIndicator(progress = { progress.fraction }, modifier = Modifier.size(40.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.khatma), style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        progress.isComplete -> stringResource(R.string.khatma_complete)
                        progress.isOnTrack -> stringResource(
                            R.string.khatma_on_track,
                            pluralStringResource(R.plurals.pages, progress.pagesPerDay, Formatters.number(progress.pagesPerDay, locale)),
                        )
                        else -> pluralStringResource(
                            R.plurals.khatma_pages_due,
                            progress.pagesDueToday,
                            Formatters.number(progress.pagesDueToday, locale),
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(
                        R.string.khatma_progress,
                        Formatters.number(progress.pagesRead, locale),
                        Formatters.number(progress.pagesTotal, locale),
                        Formatters.number(progress.daysLeft, locale),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onCancel) { Text(stringResource(R.string.khatma_end)) }
        }
    }
}

/** Offered only when no plan is running, so the list isn't carrying a dead row. */
@Composable
fun StartKhatmaRow(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onStart,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.khatma_start), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.khatma_start_desc), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun StartKhatmaDialog(currentPage: Int, onStart: (days: Int, fromPage: Int) -> Unit, onDismiss: () -> Unit) {
    var days by rememberSaveable { mutableIntStateOf(KHATMA_LENGTHS.first()) }
    var fromStart by rememberSaveable { mutableIntStateOf(1) }
    val locale = currentLocale()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.khatma_start)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.khatma_length), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KHATMA_LENGTHS.forEach { length ->
                        FilterChip(
                            selected = length == days,
                            onClick = { days = length },
                            label = { Text(pluralStringResource(R.plurals.days, length, Formatters.number(length, locale))) },
                        )
                    }
                }
                if (currentPage > 1) {
                    Text(stringResource(R.string.khatma_from), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = fromStart == 1,
                            onClick = { fromStart = 1 },
                            label = { Text(stringResource(R.string.khatma_from_beginning)) },
                        )
                        FilterChip(
                            selected = fromStart != 1,
                            onClick = { fromStart = currentPage },
                            label = { Text(stringResource(R.string.khatma_from_here, Formatters.number(currentPage, locale))) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onStart(days, fromStart) }) { Text(stringResource(R.string.khatma_begin)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
