package com.pilotothegreat.deencompanion.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.core.analytics.UsageEvent
import com.pilotothegreat.deencompanion.core.analytics.UsageReport
import com.pilotothegreat.deencompanion.ui.common.Formatters
import com.pilotothegreat.deencompanion.ui.common.SystemIntents
import com.pilotothegreat.deencompanion.ui.common.currentLocale
import com.pilotothegreat.deencompanion.ui.common.startSafely
import com.pilotothegreat.deencompanion.ui.theme.Spacing

/**
 * Everything the app has counted, in the same words the report is sent in.
 *
 * A privacy policy is a promise; this is the evidence. Anyone can open it and read the whole tally —
 * the device it describes, the days it covers, and each counter with its number — and share the exact
 * JSON that would be sent. Nothing is summarised away, because a summary is where a thing would hide.
 */
@Composable
fun UsageSheet(report: UsageReport?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val locale = currentLocale()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.large, end = Spacing.large, bottom = Spacing.xxlarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Text(
                stringResource(R.string.usage_show),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = Spacing.small),
            )
            if (report == null) {
                Row(Modifier.fillMaxWidth().padding(Spacing.xxlarge), horizontalArrangement = Arrangement.Center) {
                    LoadingIndicator()
                }
                return@Column
            }
            Text(
                stringResource(
                    R.string.usage_summary,
                    Formatters.number(report.daysActive, locale),
                    Formatters.number(report.sessions, locale),
                    report.firstSeen,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.small, bottom = Spacing.small),
            )
            val rows = report.totals.entries.sortedByDescending { it.value }
            if (rows.isEmpty()) {
                Text(
                    stringResource(R.string.usage_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = Spacing.small),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                rows.forEachIndexed { index, (event, count) ->
                    SegmentedListItem(
                        shapes = ListItemDefaults.segmentedShapes(index, rows.size),
                        trailingContent = { Text(Formatters.number(count, locale), style = MaterialTheme.typography.titleMedium) },
                    ) { Text(label(event)) }
                }
            }
            TextButton(
                onClick = { context.startSafely(SystemIntents.shareText(report.toJson().toString(2))) },
                modifier = Modifier.padding(top = Spacing.medium),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, Modifier.size(18.dp))
                Text(stringResource(R.string.usage_share), modifier = Modifier.padding(start = Spacing.small))
            }
        }
    }
}

/**
 * The counter's own name, readable.
 *
 * Deliberately the wire id turned into words rather than a translated string per event: a list of
 * forty translations would drift from the events themselves, and the point of this sheet is that what
 * it shows is exactly what is recorded.
 */
private fun label(event: String): String = (UsageEvent.byId(event)?.id ?: event)
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }
