package com.pilotothegreat.deencompanion.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.ui.common.SystemIntents
import com.pilotothegreat.deencompanion.ui.common.startSafely
import com.pilotothegreat.deencompanion.ui.theme.Spacing

/** The developer's GitHub Sponsors page: a card, once or monthly, from anywhere. */
internal const val SPONSORS_URL = "https://github.com/sponsors/Pilotothegreat"

@Composable
fun SupportSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) { SupportContent() }
}

/** The sheet's contents, apart from the sheet, so they can be rendered and looked at. */
@Composable
internal fun SupportContent() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(start = Spacing.xxlarge, end = Spacing.xxlarge, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            Icon(Icons.Rounded.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.support_title), style = MaterialTheme.typography.headlineSmallEmphasized)
        }
        Text(stringResource(R.string.support_body), style = MaterialTheme.typography.bodyLarge)
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(
                stringResource(R.string.palestine_note),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(Spacing.large),
            )
        }
        Card(
            onClick = { context.startSafely(SystemIntents.url(SPONSORS_URL)) },
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Row(Modifier.fillMaxWidth().padding(Spacing.xlarge), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.hair)) {
                    Text("GitHub Sponsors", style = MaterialTheme.typography.headlineSmallEmphasized)
                    Text(stringResource(R.string.sponsors_desc), style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
            }
        }
    }
}
